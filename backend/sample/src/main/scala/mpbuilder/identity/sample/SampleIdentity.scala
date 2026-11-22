package mpbuilder.identity.sample

import mpbuilder.customers.{CustomerIdentifier, CustomerService, CustomerStatus}
import mpbuilder.identity.*
import mpbuilder.identity.core.*
import mpbuilder.shared.*
import zio.*

/** In-memory OTP inbox so dev UI / tests can retrieve the 6-digit code sent for a challenge. */
final class InMemoryOtpInbox(ref: Ref[Map[OtpChallengeId, OtpCode]]) extends OtpDeliveryPort:
  override def sendOtp(challengeId: OtpChallengeId, principal: Principal, code: OtpCode): UIO[Unit] =
    ref.update(_ + (challengeId -> code))

  def codeFor(challengeId: OtpChallengeId): UIO[Option[OtpCode]] =
    ref.get.map(_.get(challengeId))

/** ACL adapter bridging `identity`'s `CustomerAccountLookupPort` to `customers`'s `CustomerService`. */
final class CustomerServiceLookupAdapter(
    customerService: CustomerService,
) extends CustomerAccountLookupPort:

  override def resolveCustomer(identifier: LoginIdentifier): UIO[Option[ResolvedAccount]] =
    val custId = identifier match
      case LoginIdentifier.Email(e)        => CustomerIdentifier.Email(e)
      case LoginIdentifier.CompanyRegNo(r) => CustomerIdentifier.CompanyRegNo(r)
      case LoginIdentifier.VatId(v)        => CustomerIdentifier.VatId(v)

    customerService.findByIdentifier(custId).either.map {
      case Left(_) => None
      case Right(view) =>
        val display = NonEmptyString.unsafe(
          view.company.map(_.legalName.value).getOrElse(s"${view.contact.firstName.value} ${view.contact.lastName.value}")
        )
        val principal = Principal.Customer(view.id, display, view.contact.email)
        val isActive  = view.status == CustomerStatus.Active
        val reason    = if isActive then None else Some(s"Customer account is ${view.status}")
        Some(ResolvedAccount(principal, isActive, reason))
    }

object SampleIdentity:

  val demoCustomerPassword: NonEmptyString = NonEmptyString.unsafe("demo1234")
  val demoEmployeePassword: NonEmptyString = NonEmptyString.unsafe("staff1234")

  val adminEmployeePrincipal: Principal.Employee = Principal.Employee(
    employeeId = EmployeeId.unsafe("emp-1"),
    displayName = NonEmptyString.unsafe("Admin Operator"),
    email = EmailAddress.unsafe("admin@printshop.cz"),
    roles = Set(EmployeeRole.Admin, EmployeeRole.Manager, EmployeeRole.Operator),
  )

  val prepressEmployeePrincipal: Principal.Employee = Principal.Employee(
    employeeId = EmployeeId.unsafe("emp-2"),
    displayName = NonEmptyString.unsafe("Prepress Specialist"),
    email = EmailAddress.unsafe("prepress@printshop.cz"),
    roles = Set(EmployeeRole.Prepress),
  )

  val managerEmployeePrincipal: Principal.Employee = Principal.Employee(
    employeeId = EmployeeId.unsafe("emp-3"),
    displayName = NonEmptyString.unsafe("Operations Manager"),
    email = EmailAddress.unsafe("manager@printshop.cz"),
    roles = Set(EmployeeRole.Manager, EmployeeRole.Operator),
  )

object IdentitySampleLayer:

  val otpInboxLayer: ULayer[InMemoryOtpInbox] =
    ZLayer.fromZIO(Ref.make(Map.empty[OtpChallengeId, OtpCode]).map(new InMemoryOtpInbox(_)))

  def live(
      seedCustomerEmails: List[(CustomerId, String, EmailAddress)] = List(
        (CustomerId.unsafe("cust-print-shop-pro"), "Print Shop Pro s.r.o.", EmailAddress.unsafe("jan@printshoppro.cz")),
        (CustomerId.unsafe("cust-graphic-design-co"), "Graphic Design Co.", EmailAddress.unsafe("marie@graphicdesign.cz")),
        (CustomerId.unsafe("cust-mega-corp"), "MegaCorp a.s.", EmailAddress.unsafe("petr@megacorp.cz")),
        (CustomerId.unsafe("cust-local-print"), "Local Print", EmailAddress.unsafe("eva@localprint.cz")),
      ),
      config: IdentityConfig = IdentityConfig(),
  ): ZLayer[CustomerService & EventBus, Nothing, IdentityService & InMemoryOtpInbox] =
    ZLayer.fromZIOEnvironment {
      for
        custSvc  <- ZIO.service[CustomerService]
        eventBus <- ZIO.service[EventBus]
        inboxRef <- Ref.make(Map.empty[OtpChallengeId, OtpCode])
        inbox     = new InMemoryOtpInbox(inboxRef)

        credRef  <- Ref.make(Map.empty[EmailAddress, PasswordCredential])
        otpRef   <- Ref.make(Map.empty[OtpChallengeId, OtpChallenge])
        sessRef  <- Ref.make(Map.empty[SessionToken, StoredSession])
        seqRef   <- Ref.make(1000L)

        credRepo = new CredentialRepository:
          override def findByEmail(email: EmailAddress): UIO[Option[PasswordCredential]] = credRef.get.map(_.get(email))
          override def listAll: UIO[List[PasswordCredential]]                          = credRef.get.map(_.values.toList)
          override def save(credential: PasswordCredential): UIO[Unit]                 = credRef.update(_ + (credential.email -> credential))

        otpRepo = new OtpRepository:
          override def findById(id: OtpChallengeId): UIO[Option[OtpChallenge]] = otpRef.get.map(_.get(id))
          override def save(challenge: OtpChallenge): UIO[Unit]                = otpRef.update(_ + (challenge.id -> challenge))

        sessRepo = new SessionRepository:
          override def findByToken(token: SessionToken): UIO[Option[StoredSession]] = sessRef.get.map(_.get(token))
          override def save(session: StoredSession): UIO[Unit]                      = sessRef.update(_ + (session.token -> session))
          override def revoke(token: SessionToken): UIO[Unit] =
            sessRef.update(_.updatedWith(token)(_.map(_.copy(revoked = true))))

        tokenGen = new TokenGenerator:
          override def generateSessionToken: UIO[SessionToken] =
            seqRef.updateAndGet(_ + 1).map(n => SessionToken.unsafe(s"sess-token-$n"))
          override def generateOtpChallengeId: UIO[OtpChallengeId] =
            seqRef.updateAndGet(_ + 1).map(n => OtpChallengeId.unsafe(s"otp-ch-$n"))
          override def generateOtpCode: UIO[OtpCode] =
            seqRef.updateAndGet(_ + 1).map(n => OtpCode.unsafe(f"${(123456 + n) % 900000 + 100000}%06d"))
          override def generateSalt: UIO[String] =
            seqRef.updateAndGet(_ + 1).map(n => s"sample-salt-$n")

        lookup = new CustomerServiceLookupAdapter(custSvc)
        svc    = new IdentityServiceLive(credRepo, otpRepo, sessRepo, lookup, inbox, tokenGen, eventBus, config)

        // Seed password credentials for demo customers + employees
        _ <- ZIO.foreachDiscard(seedCustomerEmails) { case (cid, name, email) =>
               svc
                 .registerPasswordCredential(
                   RegisterPasswordCmd(
                     principal = Principal.Customer(cid, NonEmptyString.unsafe(name), email),
                     email = email,
                     rawPassword = SampleIdentity.demoCustomerPassword,
                   )
                 )
                 .ignore
             }
        _ <- ZIO.foreachDiscard(
               List(
                 SampleIdentity.adminEmployeePrincipal,
                 SampleIdentity.prepressEmployeePrincipal,
                 SampleIdentity.managerEmployeePrincipal,
               )
             ) { emp =>
               svc
                 .registerPasswordCredential(
                   RegisterPasswordCmd(
                     principal = emp,
                     email = emp.email,
                     rawPassword = SampleIdentity.demoEmployeePassword,
                   )
                 )
                 .ignore
             }
      yield ZEnvironment[IdentityService, InMemoryOtpInbox](svc, inbox)
    }
