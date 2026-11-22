package mpbuilder.customers

import mpbuilder.customers.core.*
import mpbuilder.shared.*
import zio.*
import zio.test.*
import java.time.Instant

object CustomerDomainSpec extends ZIOSpecDefault:

  private val now = Instant.parse("2026-03-10T10:00:00Z")

  private val sampleAddress = Address(
    street = NonEmptyString.unsafe("Václavské náměstí 1"),
    city = NonEmptyString.unsafe("Praha"),
    postalCode = NonEmptyString.unsafe("11000"),
    countryCode = NonEmptyString.unsafe("CZ"),
  )

  private val sampleCompany = CompanyProfile(
    legalName = NonEmptyString.unsafe("PrintStudio s.r.o."),
    registrationNumber = NonEmptyString.unsafe("12345678"),
    vatId = Some(NonEmptyString.unsafe("CZ12345678")),
    contactPerson = NonEmptyString.unsafe("Jan Novák"),
  )

  private def contact(email: String) = ContactPerson(
    firstName = NonEmptyString.unsafe("Jan"),
    lastName = NonEmptyString.unsafe("Novák"),
    email = EmailAddress.unsafe(email),
  )

  private def inMemoryRepoLayer: ULayer[CustomerRepository] =
    ZLayer.fromZIO {
      Ref.make(Map.empty[CustomerId, Customer]).map { ref =>
        new CustomerRepository:
          override def findById(id: CustomerId): UIO[Option[Customer]] =
            ref.get.map(_.get(id))
          override def findByEmail(email: EmailAddress): UIO[Option[Customer]] =
            ref.get.map(_.values.find(_.contact.email == email))
          override def findByRegistrationNumber(regNo: NonEmptyString): UIO[Option[Customer]] =
            ref.get.map(_.values.find(_.company.exists(_.registrationNumber == regNo)))
          override def findByVatId(vatId: NonEmptyString): UIO[Option[Customer]] =
            ref.get.map(_.values.find(_.company.flatMap(_.vatId).contains(vatId)))
          override def save(customer: Customer): UIO[Unit] =
            ref.update(_ + (customer.id -> customer))
      }
    }

  private def serviceLayer: ULayer[CustomerService & EventBus] =
    val busLayer = EventBus.live
    busLayer ++ ((inMemoryRepoLayer ++ busLayer) >>> CustomerServiceLive.layer)

  def spec = suite("CustomerDomainSpec")(
    test("B2B registration requires CompanyProfile and starts in PendingApproval; Individual starts Active") {
      val b2bNoCompany = RegisterCustomerCmd(
        id = CustomerId.unsafe("c-bad-b2b"),
        segment = CustomerSegment.Business,
        company = None,
        contact = contact("b2b@example.com"),
        billingAddress = sampleAddress,
      )
      val b2bOk = b2bNoCompany.copy(id = CustomerId.unsafe("c-b2b"), company = Some(sampleCompany))
      val indiv = RegisterCustomerCmd(
        id = CustomerId.unsafe("c-ind"),
        segment = CustomerSegment.Individual,
        company = None,
        contact = contact("ind@example.com"),
        billingAddress = sampleAddress,
      )

      assertTrue(
        Customer.register(b2bNoCompany, now) == Left(CustomerError.CompanyProfileRequired(CustomerSegment.Business)),
        Customer.register(b2bOk, now).map(_.status) == Right(CustomerStatus.PendingApproval),
        Customer.register(indiv, now).map(_.status) == Right(CustomerStatus.Active),
      )
    },
    test("NetInvoice terms are blocked for Individual or PendingApproval, granted to Active Business, and revoked on Suspend") {
      val net30 = PaymentTerms.NetInvoice(dueDays = 30, creditLimit = Price(Money(50000), Currency.CZK))
      val b2b   = Customer.register(
        RegisterCustomerCmd(CustomerId.unsafe("c-b2b"), CustomerSegment.Business, Some(sampleCompany), contact("b2b@co.cz"), sampleAddress),
        now,
      ).toOption.get
      val indiv = Customer.register(
        RegisterCustomerCmd(CustomerId.unsafe("c-ind"), CustomerSegment.Individual, None, contact("ind@co.cz"), sampleAddress),
        now,
      ).toOption.get

      val pendingFail = b2b.withPaymentTerms(net30, CustomerActor.System, now)
      val indivFail   = indiv.withPaymentTerms(net30, CustomerActor.System, now)
      val activeB2b   = b2b.approve(CustomerActor.System, now).toOption.get
      val granted     = activeB2b.withPaymentTerms(net30, CustomerActor.System, now)
      val suspended   = granted.toOption.get.suspend(NonEmptyString.unsafe("Overdue invoice"), None, None, now)

      assertTrue(
        pendingFail.isLeft,
        indivFail.isLeft,
        granted.map(_.paymentTerms) == Right(net30),
        suspended.map(_.status) == Right(CustomerStatus.Suspended),
        suspended.map(_.paymentTerms) == Right(PaymentTerms.PrepaidOnly),
      )
    },
    test("CustomerServiceLive enforces duplicate email check, supports Email/IČO/DIČ lookup, and emits domain events") {
      ZIO.scoped {
        for
          svc     <- ZIO.service[CustomerService]
          bus     <- ZIO.service[EventBus]
          queue   <- bus.subscribe
          cmd      = RegisterCustomerCmd(
                       CustomerId.unsafe("c-1"),
                       CustomerSegment.Agency,
                       Some(sampleCompany),
                       contact("agency@studio.cz"),
                       sampleAddress,
                     )
          v1      <- svc.register(cmd)
          dup     <- svc.register(cmd.copy(id = CustomerId.unsafe("c-2"))).either
          byEmail <- svc.findByIdentifier(CustomerIdentifier.Email(cmd.contact.email))
          byIco   <- svc.findByIdentifier(CustomerIdentifier.CompanyRegNo(sampleCompany.registrationNumber))
          byDic   <- svc.findByIdentifier(CustomerIdentifier.VatId(sampleCompany.vatId.get))
          missing <- svc.findByIdentifier(CustomerIdentifier.CompanyRegNo(NonEmptyString.unsafe("99999999"))).either
          v2      <- svc.approve(v1.id)
          ev1     <- queue.take
          ev2     <- queue.take
        yield assertTrue(
          v1.status == CustomerStatus.PendingApproval,
          dup == Left(CustomerError.EmailAlreadyRegistered(cmd.contact.email)),
          byEmail.id == v1.id,
          byIco.id == v1.id,
          byDic.id == v1.id,
          missing.isLeft,
          v2.status == CustomerStatus.Active,
          ev1.isInstanceOf[CustomerRegistered],
          ev2.isInstanceOf[CustomerApproved],
        )
      }.provide(serviceLayer)
    },
  )
