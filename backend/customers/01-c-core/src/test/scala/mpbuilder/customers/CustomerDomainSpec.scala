package mpbuilder.customers

import mpbuilder.customers.core.*
import mpbuilder.shared.*
import kyo.*
import kyo.test.*
import java.time.Instant

class CustomerDomainSpec extends Test[Any]:

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

  private def inMemoryRepo: CustomerRepository < Sync =
    AtomicRef.init(Map.empty[CustomerId, Customer]).map { ref =>
        new CustomerRepository:
          override def findById(id: CustomerId): Option[Customer] < Async =
            ref.get.map(_.get(id))
          override def findByEmail(email: EmailAddress): Option[Customer] < Async =
            ref.get.map(_.values.find(_.contact.email == email))
          override def findByRegistrationNumber(regNo: NonEmptyString): Option[Customer] < Async =
            ref.get.map(_.values.find(_.company.exists(_.registrationNumber == regNo)))
          override def findByVatId(vatId: NonEmptyString): Option[Customer] < Async =
            ref.get.map(_.values.find(_.company.flatMap(_.vatId).contains(vatId)))
          override def save(customer: Customer): Unit < Async =
            ref.update(_ + (customer.id -> customer))
    }

  private def services: (CustomerService, EventBus) < Sync =
    for
      bus  <- EventBus.init
      repo <- inMemoryRepo
    yield (CustomerServiceLive.make(repo, bus), bus)

  "CustomerDomainSpec" - {
    "B2B registration requires CompanyProfile and starts in PendingApproval; Individual starts Active" in {
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

      assert(Customer.register(b2bNoCompany, now) == Left(CustomerError.CompanyProfileRequired(CustomerSegment.Business)))
      assert(Customer.register(b2bOk, now).map(_.status) == Right(CustomerStatus.PendingApproval))
      assert(Customer.register(indiv, now).map(_.status) == Right(CustomerStatus.Active))
    }

    "NetInvoice terms are blocked for Individual or PendingApproval, granted to Active Business, and revoked on Suspend" in {
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

      assert(pendingFail.isLeft)
      assert(indivFail.isLeft)
      assert(granted.map(_.paymentTerms) == Right(net30))
      assert(suspended.map(_.status) == Right(CustomerStatus.Suspended))
      assert(suspended.map(_.paymentTerms) == Right(PaymentTerms.PrepaidOnly))
    }

    "CustomerServiceLive enforces duplicate email check, supports Email/IČO/DIČ lookup, and emits domain events" in {
      locally {
        for
          (svc, bus) <- services
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
          dupId   <- svc.register(cmd.copy(contact = contact("other@studio.cz"))).either
          byEmail <- svc.findByIdentifier(CustomerIdentifier.Email(cmd.contact.email))
          byIco   <- svc.findByIdentifier(CustomerIdentifier.CompanyRegNo(sampleCompany.registrationNumber))
          byDic   <- svc.findByIdentifier(CustomerIdentifier.VatId(sampleCompany.vatId.get))
          missing <- svc.findByIdentifier(CustomerIdentifier.CompanyRegNo(NonEmptyString.unsafe("99999999"))).either
          v2      <- svc.approve(v1.id)
          events  <- queue.drain
          ev1      = events(0)
          ev2      = events(1)
        yield {
          assert(v1.status == CustomerStatus.PendingApproval)
          assert(dup == Left(CustomerError.EmailAlreadyRegistered(cmd.contact.email)))
          assert(dupId == Left(CustomerError.CustomerIdAlreadyExists(cmd.id)))
          assert(byEmail.id == v1.id)
          assert(byIco.id == v1.id)
          assert(byDic.id == v1.id)
          assert(missing.isLeft)
          assert(v2.status == CustomerStatus.Active)
          assert(ev1.isInstanceOf[CustomerRegistered])
          assert(ev2.isInstanceOf[CustomerApproved])
        }
      }
    }
  }
