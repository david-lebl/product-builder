package mpbuilder.domain

import zio.test.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.CustomerPricing
import mpbuilder.domain.service.*
import mpbuilder.domain.sample.SampleCustomers

object CustomerManagementServiceSpec extends ZIOSpecDefault:

  private val sampleCustomers = SampleCustomers.all

  private def makeCustomer(
      id: String,
      email: String,
      firstName: String = "Test",
      lastName: String = "User",
      businessId: String = "99999999",
      companyName: String = "Test Company",
  ): Customer = Customer(
    id = CustomerId.unsafe(id),
    customerType = CustomerType.Agency,
    status = CustomerStatus.Active,
    tier = CustomerTier.Standard,
    companyInfo = Some(CompanyInfo(companyName, businessId, None, "Contact Person")),
    contactInfo = ContactInfo(firstName, lastName, email, "+420 000 000 000", Some(companyName), Some(businessId), None),
    address = Address("Street 1", "City", "10000", "CZ"),
    pricing = CustomerPricing.empty,
    internalNotes = Nil,
    createdAt = 1700000000000L,
    lastOrderAt = None,
    tags = Set.empty,
  )

  def spec = suite("CustomerManagementService")(
    suite("addCustomer")(
      test("adds a new customer to the list") {
        val newCustomer = makeCustomer("cust-new", "new@test.cz")
        val result = CustomerManagementService.addCustomer(sampleCustomers, newCustomer)
        val updated = result.toEither.toOption.get
        assertTrue(
          updated.size == sampleCustomers.size + 1,
          updated.last.id == CustomerId.unsafe("cust-new"),
        )
      },
      test("fails if business ID already exists") {
        val duplicate = makeCustomer("cust-dup", "unique@test.cz", businessId = "12345678")
        val result = CustomerManagementService.addCustomer(sampleCustomers, duplicate)
        assertTrue(result.toEither.isLeft)
      },
      test("fails if email already exists") {
        val duplicate = makeCustomer("cust-dup", "jan@printshoppro.cz", businessId = "unique-id")
        val result = CustomerManagementService.addCustomer(sampleCustomers, duplicate)
        assertTrue(result.toEither.isLeft)
      },
      test("fails if email is empty") {
        val noEmail = makeCustomer("cust-no-email", "", businessId = "unique-id-2")
        val result = CustomerManagementService.addCustomer(sampleCustomers, noEmail)
        assertTrue(result.toEither.isLeft)
      },
      test("fails if first name is empty") {
        val noName = makeCustomer("cust-no-name", "test@unique.cz", firstName = "", businessId = "unique-id-3")
        val result = CustomerManagementService.addCustomer(sampleCustomers, noName)
        assertTrue(result.toEither.isLeft)
      },
      test("fails if last name is empty") {
        val noName = makeCustomer("cust-no-name", "test@unique2.cz", lastName = "", businessId = "unique-id-4")
        val result = CustomerManagementService.addCustomer(sampleCustomers, noName)
        assertTrue(result.toEither.isLeft)
      },
      test("fails if agency customer has no company info") {
        val noCompany = Customer(
          id = CustomerId.unsafe("cust-no-company"),
          customerType = CustomerType.Agency,
          status = CustomerStatus.Active,
          tier = CustomerTier.Standard,
          companyInfo = None,
          contactInfo = ContactInfo("Test", "User", "nocompany@test.cz", "+420 000 000 000", None, None, None),
          address = Address("Street 1", "City", "10000", "CZ"),
          pricing = CustomerPricing.empty,
          internalNotes = Nil,
          createdAt = 1700000000000L,
          lastOrderAt = None,
          tags = Set.empty,
        )
        val result = CustomerManagementService.addCustomer(sampleCustomers, noCompany)
        assertTrue(result.toEither.isLeft)
      },
      test("fails if agency customer has empty company name") {
        val emptyCompanyName = makeCustomer("cust-empty-co", "emptyco@test.cz", businessId = "unique-id-5", companyName = "  ")
        val result = CustomerManagementService.addCustomer(sampleCustomers, emptyCompanyName)
        assertTrue(result.toEither.isLeft)
      },
      test("fails if agency customer has empty business ID") {
        val emptyBizId = makeCustomer("cust-empty-biz", "emptybiz@test.cz", businessId = "  ")
        // need to construct manually since makeCustomer puts businessId in companyInfo
        val customer = Customer(
          id = CustomerId.unsafe("cust-empty-biz"),
          customerType = CustomerType.Agency,
          status = CustomerStatus.Active,
          tier = CustomerTier.Standard,
          companyInfo = Some(CompanyInfo("Valid Co", "  ", None, "Contact")),
          contactInfo = ContactInfo("Test", "User", "emptybiz@test.cz", "+420 000 000 000", None, None, None),
          address = Address("Street 1", "City", "10000", "CZ"),
          pricing = CustomerPricing.empty,
          internalNotes = Nil,
          createdAt = 1700000000000L,
          lastOrderAt = None,
          tags = Set.empty,
        )
        val result = CustomerManagementService.addCustomer(sampleCustomers, customer)
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("updateCustomer")(
      test("updates company info, contact info, and address") {
        val newContact = ContactInfo("Updated", "Name", "updated@test.cz", "+420 111 111 111", None, None, None)
        val newAddress = Address("New Street 1", "New City", "20000", "CZ")
        val newCompany = Some(CompanyInfo("Updated Company", "12345678", Some("CZ12345678"), "New Contact"))
        val result = CustomerManagementService.updateCustomer(
          sampleCustomers,
          SampleCustomers.printShopProId,
          newCompany,
          newContact,
          newAddress,
        )
        val updated = result.toEither.toOption.get
        val customer = updated.find(_.id == SampleCustomers.printShopProId).get
        assertTrue(
          customer.contactInfo.firstName == "Updated",
          customer.address.city == "New City",
          customer.companyInfo.get.companyName == "Updated Company",
        )
      },
      test("fails if customer not found") {
        val newContact = ContactInfo("Test", "User", "test@test.cz", "+420 000 000 000", None, None, None)
        val result = CustomerManagementService.updateCustomer(
          sampleCustomers,
          CustomerId.unsafe("nonexistent"),
          None,
          newContact,
          Address("", "", "", ""),
        )
        assertTrue(result.toEither.isLeft)
      },
      test("fails if updated email is empty") {
        val emptyEmailContact = ContactInfo("Test", "User", "", "+420 000 000 000", None, None, None)
        val result = CustomerManagementService.updateCustomer(
          sampleCustomers,
          SampleCustomers.printShopProId,
          None,
          emptyEmailContact,
          Address("Street", "City", "10000", "CZ"),
        )
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("updateStatus")(
      test("updates customer status") {
        val result = CustomerManagementService.updateStatus(
          sampleCustomers,
          SampleCustomers.printShopProId,
          CustomerStatus.Suspended,
        )
        val updated = result.toEither.toOption.get
        val customer = updated.find(_.id == SampleCustomers.printShopProId).get
        assertTrue(customer.status == CustomerStatus.Suspended)
      },
      test("fails if customer not found") {
        val result = CustomerManagementService.updateStatus(
          sampleCustomers,
          CustomerId.unsafe("nonexistent"),
          CustomerStatus.Active,
        )
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("updateTier")(
      test("updates customer tier") {
        val result = CustomerManagementService.updateTier(
          sampleCustomers,
          SampleCustomers.localPrintId,
          CustomerTier.Gold,
        )
        val updated = result.toEither.toOption.get
        val customer = updated.find(_.id == SampleCustomers.localPrintId).get
        assertTrue(customer.tier == CustomerTier.Gold)
      },
      test("fails if customer not found") {
        val result = CustomerManagementService.updateTier(
          sampleCustomers,
          CustomerId.unsafe("nonexistent"),
          CustomerTier.Gold,
        )
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("addNote")(
      test("appends a note to the customer") {
        val note = CustomerNote("Test note", 1701000000000L, Some(EmployeeId.unsafe("emp-1")))
        val result = CustomerManagementService.addNote(
          sampleCustomers,
          SampleCustomers.graphicDesignCoId,
          note,
        )
        val updated = result.toEither.toOption.get
        val customer = updated.find(_.id == SampleCustomers.graphicDesignCoId).get
        assertTrue(
          customer.internalNotes.size == 1,
          customer.internalNotes.last.text == "Test note",
        )
      },
      test("fails if customer not found") {
        val note = CustomerNote("Note", 1701000000000L, None)
        val result = CustomerManagementService.addNote(
          sampleCustomers,
          CustomerId.unsafe("nonexistent"),
          note,
        )
        assertTrue(result.toEither.isLeft)
      },
      test("fails if note text is empty") {
        val note = CustomerNote("  ", 1701000000000L, None)
        val result = CustomerManagementService.addNote(
          sampleCustomers,
          SampleCustomers.printShopProId,
          note,
        )
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("removeCustomer")(
      test("removes a customer from the list") {
        val result = CustomerManagementService.removeCustomer(
          sampleCustomers,
          SampleCustomers.suspendedCoId,
        )
        val updated = result.toEither.toOption.get
        assertTrue(
          updated.size == sampleCustomers.size - 1,
          !updated.exists(_.id == SampleCustomers.suspendedCoId),
        )
      },
      test("fails if customer not found") {
        val result = CustomerManagementService.removeCustomer(
          sampleCustomers,
          CustomerId.unsafe("nonexistent"),
        )
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("error messages")(
      test("all errors have English and Czech messages") {
        val errors: List[CustomerManagementError] = List(
          CustomerManagementError.DuplicateBusinessId("12345"),
          CustomerManagementError.DuplicateEmail("test@test.cz"),
          CustomerManagementError.CustomerNotFound(CustomerId.unsafe("x")),
          CustomerManagementError.InvalidStatus(CustomerStatus.Active, CustomerStatus.Suspended),
          CustomerManagementError.MissingRequiredField("email"),
        )
        assertTrue(errors.forall { e =>
          e.message(Language.En).nonEmpty && e.message(Language.Cs).nonEmpty
        })
      },
    ),
    suite("enum display names")(
      test("CustomerStatus has bilingual display names") {
        assertTrue(
          CustomerStatus.values.forall(s =>
            s.displayName(Language.En).nonEmpty && s.displayName(Language.Cs).nonEmpty
          )
        )
      },
      test("CustomerTier has bilingual display names") {
        assertTrue(
          CustomerTier.values.forall(t =>
            t.displayName(Language.En).nonEmpty && t.displayName(Language.Cs).nonEmpty
          )
        )
      },
    ),
  )
