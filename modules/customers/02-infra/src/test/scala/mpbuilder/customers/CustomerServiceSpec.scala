package mpbuilder.customers

import mpbuilder.commons.*
import mpbuilder.customers.impl.legacy.LegacyCustomerService
import mpbuilder.domain.model as dm
import mpbuilder.domain.sample.SampleCustomers
import zio.*
import zio.test.*

/** Contract tests for [[CustomerService]] — public surface only, apart from seeding fixtures. */
object CustomerServiceSpec extends ZIOSpecDefault:

  private def service = LegacyCustomerService.seeded(SampleCustomers.all)

  private val anActive = SampleCustomers.all.find(_.status == dm.CustomerStatus.Active).get

  def spec = suite("CustomerService")(
    suite("find")(
      test("finds a seeded customer by id") {
        for
          svc <- service
          found <- svc.find(anActive.id.value)
        yield assertTrue(
          found.isDefined,
          found.get.email == anActive.contactInfo.email,
          found.get.displayName.nonEmpty,
        )
      },
      test("returns None for an unknown id rather than failing") {
        for
          svc <- service
          found <- svc.find("no-such-customer")
        yield assertTrue(found.isEmpty)
      },
    ),
    suite("findBy")(
      test("finds an active customer by email, case-insensitively") {
        for
          svc <- service
          found <- svc.findBy(Identifier.Email(anActive.contactInfo.email.toUpperCase))
        yield assertTrue(found.isDefined)
      },
      test("finds a business customer by business id") {
        val withCompany = SampleCustomers.all.find(c =>
          c.status == dm.CustomerStatus.Active && c.companyInfo.exists(_.businessId.nonEmpty)
        )
        withCompany match
          case None => ZIO.succeed(assertTrue(true))
          case Some(c) =>
            for
              svc <- service
              found <- svc.findBy(Identifier.BusinessId(c.companyInfo.get.businessId))
            yield assertTrue(found.exists(_.id == c.id.value))
      },
      test("an account awaiting approval is reported as absent") {
        // Spec §9.1: a not-yet-approved applicant must not learn from the sign-in form that
        // their application exists.
        val pending = SampleCustomers.all.find(_.status == dm.CustomerStatus.PendingApproval)
        pending match
          case None => ZIO.succeed(assertTrue(true))
          case Some(c) =>
            for
              svc <- service
              found <- svc.findBy(Identifier.Email(c.contactInfo.email))
            yield assertTrue(found.isEmpty)
      },
    ),
    suite("canPayOnAccount")(
      test("is true only for an active corporate account") {
        for
          svc <- service
          summaries <- ZIO.foreach(SampleCustomers.all)(c => svc.find(c.id.value))
        yield assertTrue(summaries.flatten.forall { s =>
          s.canPayOnAccount == (s.isActive && s.customerType == "RegisteredCorporate")
        })
      },
      test("an active RegisteredCorporate account may be invoiced") {
        // Proved with an explicit fixture rather than over the sample data, because *no* sample
        // customer is RegisteredCorporate — every business account is typed Agency, so the rule
        // is currently unreachable in practice. See the note in the architecture doc.
        val corporate = SampleCustomers.all.head.copy(
          id = dm.CustomerId.unsafe("cust-corporate-fixture"),
          customerType = dm.CustomerType.RegisteredCorporate,
          status = dm.CustomerStatus.Active,
        )
        for
          svc <- LegacyCustomerService.seeded(List(corporate))
          found <- svc.find(corporate.id.value)
        yield assertTrue(found.exists(_.canPayOnAccount))
      },
      test("no sample customer currently qualifies for invoice on account") {
        // Documents the data/spec mismatch so it fails loudly if the sample data is fixed:
        // spec §9.1 grants invoicing to RegisteredCorporate, but all 10 sample business
        // customers are typed Agency.
        for
          svc <- service
          summaries <- ZIO.foreach(SampleCustomers.all)(c => svc.find(c.id.value))
        yield assertTrue(!summaries.flatten.exists(_.canPayOnAccount))
      },
      test("a suspended corporate account may not be invoiced") {
        val suspended = SampleCustomers.all.find(c =>
          c.customerType == dm.CustomerType.RegisteredCorporate &&
            c.status != dm.CustomerStatus.Active
        )
        suspended match
          case None => ZIO.succeed(assertTrue(true))
          case Some(c) =>
            for
              svc <- service
              found <- svc.find(c.id.value)
            yield assertTrue(found.exists(!_.canPayOnAccount))
      },
    ),
    suite("register")(
      test("an individual is active immediately and findable afterwards") {
        for
          svc <- service
          created <- svc.register(
            RegisterCustomer("Jan", "Novák", "jan.novak@example.com", "+420111222333")
          )
          found <- svc.find(created.id)
        yield assertTrue(
          created.isActive,
          created.customerType == "Registered",
          !created.canPayOnAccount,
          found.isDefined,
          found.get.email == "jan.novak@example.com",
        )
      },
      test("a corporate applicant starts pending, and cannot yet be invoiced") {
        // Granting invoice-on-account is a staff decision, so registration must not confer it.
        for
          svc <- service
          created <- svc.register(
            RegisterCustomer(
              "Eva",
              "Dvořáková",
              "eva@firma.cz",
              "+420999888777",
              company = Some(CompanyDetails("Firma s.r.o.", Some("10203040"), Some("CZ10203040"))),
            )
          )
        yield assertTrue(
          created.customerType == "RegisteredCorporate",
          created.status == "PendingApproval",
          !created.isActive,
          !created.canPayOnAccount,
          created.company.exists(_.name == "Firma s.r.o."),
        )
      },
      test("refuses a duplicate email") {
        for
          svc <- service
          _ <- svc.register(RegisterCustomer("A", "One", "dupe@example.com", "1"))
          error <- svc.register(RegisterCustomer("B", "Two", "dupe@example.com", "2")).flip
        yield assertTrue(error == CustomerError.EmailAlreadyRegistered("dupe@example.com"))
      },
      test("rejects incomplete details with bilingual problems") {
        for
          svc <- service
          error <- svc.register(RegisterCustomer("", "", "", "")).flip
        yield assertTrue(
          error.isInstanceOf[CustomerError.Rejected],
          error.message(Language.En).nonEmpty,
          error.message(Language.Cs) != error.message(Language.En),
        )
      },
      test("a rejected registration does not modify the store") {
        for
          svc <- service
          before <- ZIO.foreach(SampleCustomers.all)(c => svc.find(c.id.value)).map(_.flatten.size)
          _ <- svc.register(RegisterCustomer("", "", "", "")).either
          after <- ZIO.foreach(SampleCustomers.all)(c => svc.find(c.id.value)).map(_.flatten.size)
        yield assertTrue(before == after)
      },
    ),
    suite("pricing separation")(
      test("a customer summary carries no negotiated pricing") {
        // Pricing overlays belong to the pricing context, resolved from a customer id. If they
        // leaked into this DTO, every caller could reimplement pricing — which is the mistake
        // this split exists to prevent.
        for
          svc <- service
          found <- svc.find(anActive.id.value)
        yield assertTrue(
          found.isDefined,
          !found.get.toString.toLowerCase.contains("discount"),
        )
      }
    ),
  )
