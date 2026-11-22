package mpbuilder.customers.sample

import mpbuilder.customers.*
import mpbuilder.customers.core.{Customer, CustomerRepository, CustomerServiceLive}
import mpbuilder.shared.*
import zio.*
import java.time.Instant

/** All 10 sample customer accounts replicated from legacy `SampleCustomers`
  * (strictly clean account entities — commercial pricing agreements live in `pricing.sample.SampleAgreements`).
  */
object SampleCustomers:

  val printShopProId: CustomerId    = CustomerId.unsafe("cust-print-shop-pro")
  val graphicDesignCoId: CustomerId = CustomerId.unsafe("cust-graphic-design-co")
  val megaCorpId: CustomerId        = CustomerId.unsafe("cust-mega-corp")
  val localPrintId: CustomerId      = CustomerId.unsafe("cust-local-print")
  val eventAgencyId: CustomerId     = CustomerId.unsafe("cust-event-agency")
  val adStudioId: CustomerId        = CustomerId.unsafe("cust-ad-studio")
  val startupHubId: CustomerId      = CustomerId.unsafe("cust-startup-hub")
  val packagingLtdId: CustomerId    = CustomerId.unsafe("cust-packaging-ltd")
  val suspendedCoId: CustomerId     = CustomerId.unsafe("cust-suspended-co")
  val pendingCoId: CustomerId       = CustomerId.unsafe("cust-pending-co")

  private def mkCustomer(
      id: CustomerId,
      status: CustomerStatus,
      tier: CustomerTier,
      companyName: String,
      regNo: String,
      vatId: Option[String],
      firstName: String,
      lastName: String,
      email: String,
      phone: String,
      street: String,
      city: String,
      postal: String,
      paymentTerms: PaymentTerms = PaymentTerms.PrepaidOnly,
      notes: List[AccountNote] = Nil,
      tags: Set[String] = Set.empty,
      createdAtMs: Long,
      additionalContacts: List[ContactPerson] = Nil,
      shippingAddresses: List[Address] = Nil,
      defaultShippingIndex: Option[Int] = None,
  ): Customer =
    val created = Instant.ofEpochMilli(createdAtMs)
    Customer(
      id = id,
      segment = CustomerSegment.Agency,
      status = status,
      tier = tier,
      company = Some(
        CompanyProfile(
          legalName = NonEmptyString.unsafe(companyName),
          registrationNumber = NonEmptyString.unsafe(regNo),
          vatId = vatId.map(NonEmptyString.unsafe),
          contactPerson = NonEmptyString.unsafe(s"$firstName $lastName"),
        )
      ),
      contact = ContactPerson(
        firstName = NonEmptyString.unsafe(firstName),
        lastName = NonEmptyString.unsafe(lastName),
        email = EmailAddress.unsafe(email),
        phone = Some(NonEmptyString.unsafe(phone)),
        roleTitle = Some(NonEmptyString.unsafe("Primary Contact")),
      ),
      billingAddress = Address(
        street = NonEmptyString.unsafe(street),
        city = NonEmptyString.unsafe(city),
        postalCode = NonEmptyString.unsafe(postal),
        countryCode = NonEmptyString.unsafe("CZ"),
      ),
      shippingAddresses = shippingAddresses,
      paymentTerms = paymentTerms,
      notes = notes,
      tags = tags,
      createdAt = created,
      additionalContacts = additionalContacts,
      defaultShippingIndex = defaultShippingIndex.orElse(if shippingAddresses.nonEmpty then Some(0) else None),
      auditLog = List(
        CustomerAuditEntry(
          action = "Registered",
          summary = s"Account created ($tier, $status)",
          actor = CustomerActor.System,
          occurredAt = created,
        )
      ),
    )

  private[customers] val printShopPro: Customer = mkCustomer(
    id = printShopProId,
    status = CustomerStatus.Active,
    tier = CustomerTier.Gold,
    companyName = "Print Shop Pro s.r.o.",
    regNo = "12345678",
    vatId = Some("CZ12345678"),
    firstName = "Jan",
    lastName = "Novák",
    email = "jan@printshoppro.cz",
    phone = "+420 123 456 789",
    street = "Vinohradská 10",
    city = "Praha",
    postal = "12000",
    paymentTerms = PaymentTerms.NetInvoice(dueDays = 14, creditLimit = Price(Money("100000"), Currency.CZK)),
    notes = List(AccountNote(NonEmptyString.unsafe("VIP customer, priority handling"), Instant.ofEpochMilli(1700000000000L), Some(EmployeeId.unsafe("emp-1")))),
    tags = Set("vip", "agency"),
    createdAtMs = 1690000000000L,
    additionalContacts = List(
      ContactPerson(
        firstName = NonEmptyString.unsafe("Lucie"),
        lastName = NonEmptyString.unsafe("Procházková"),
        email = EmailAddress.unsafe("lucie.billing@printshoppro.cz"),
        phone = Some(NonEmptyString.unsafe("+420 123 456 790")),
        roleTitle = Some(NonEmptyString.unsafe("Billing & Invoicing")),
      ),
      ContactPerson(
        firstName = NonEmptyString.unsafe("Tomáš"),
        lastName = NonEmptyString.unsafe("Beneš"),
        email = EmailAddress.unsafe("tomas.production@printshoppro.cz"),
        phone = Some(NonEmptyString.unsafe("+420 123 456 791")),
        roleTitle = Some(NonEmptyString.unsafe("Production Buyer")),
      ),
    ),
    shippingAddresses = List(
      Address(
        street = NonEmptyString.unsafe("Vinohradská 10"),
        city = NonEmptyString.unsafe("Praha"),
        postalCode = NonEmptyString.unsafe("12000"),
        countryCode = NonEmptyString.unsafe("CZ"),
      ),
      Address(
        street = NonEmptyString.unsafe("Průmyslová 45, Sklad B"),
        city = NonEmptyString.unsafe("Praha 10"),
        postalCode = NonEmptyString.unsafe("10200"),
        countryCode = NonEmptyString.unsafe("CZ"),
      ),
    ),
    defaultShippingIndex = Some(1),
  )

  private[customers] val graphicDesignCo: Customer = mkCustomer(
    id = graphicDesignCoId,
    status = CustomerStatus.Active,
    tier = CustomerTier.Silver,
    companyName = "Graphic Design Co.",
    regNo = "87654321",
    vatId = Some("CZ87654321"),
    firstName = "Marie",
    lastName = "Svobodová",
    email = "marie@graphicdesign.cz",
    phone = "+420 987 654 321",
    street = "Národní 5",
    city = "Praha",
    postal = "11000",
    tags = Set("design"),
    createdAtMs = 1692000000000L,
  )

  private[customers] val megaCorp: Customer = mkCustomer(
    id = megaCorpId,
    status = CustomerStatus.Active,
    tier = CustomerTier.Platinum,
    companyName = "MegaCorp a.s.",
    regNo = "11223344",
    vatId = Some("CZ11223344"),
    firstName = "Petr",
    lastName = "Dvořák",
    email = "petr@megacorp.cz",
    phone = "+420 111 222 333",
    street = "Wenceslas Square 1",
    city = "Praha",
    postal = "11000",
    paymentTerms = PaymentTerms.NetInvoice(dueDays = 30, creditLimit = Price(Money("500000"), Currency.CZK)),
    notes = List(
      AccountNote(NonEmptyString.unsafe("Largest corporate account"), Instant.ofEpochMilli(1691000000000L), Some(EmployeeId.unsafe("emp-1"))),
      AccountNote(NonEmptyString.unsafe("Renewed annual contract"), Instant.ofEpochMilli(1698000000000L), Some(EmployeeId.unsafe("emp-2"))),
    ),
    tags = Set("enterprise", "vip"),
    createdAtMs = 1685000000000L,
    additionalContacts = List(
      ContactPerson(
        firstName = NonEmptyString.unsafe("Klára"),
        lastName = NonEmptyString.unsafe("Horáková"),
        email = EmailAddress.unsafe("klara.marketing@megacorp.cz"),
        phone = Some(NonEmptyString.unsafe("+420 111 222 340")),
        roleTitle = Some(NonEmptyString.unsafe("Head of Marketing")),
      )
    ),
    shippingAddresses = List(
      Address(
        street = NonEmptyString.unsafe("Wenceslas Square 1, Reception"),
        city = NonEmptyString.unsafe("Praha"),
        postalCode = NonEmptyString.unsafe("11000"),
        countryCode = NonEmptyString.unsafe("CZ"),
      )
    ),
    defaultShippingIndex = Some(0),
  )

  private[customers] val localPrint: Customer = mkCustomer(
    id = localPrintId,
    status = CustomerStatus.Active,
    tier = CustomerTier.Standard,
    companyName = "Local Print",
    regNo = "55667788",
    vatId = None,
    firstName = "Eva",
    lastName = "Černá",
    email = "eva@localprint.cz",
    phone = "+420 555 666 777",
    street = "Masarykova 22",
    city = "Brno",
    postal = "60200",
    createdAtMs = 1695000000000L,
  )

  private[customers] val eventAgency: Customer = mkCustomer(
    id = eventAgencyId,
    status = CustomerStatus.Active,
    tier = CustomerTier.Silver,
    companyName = "Event Agency s.r.o.",
    regNo = "99887766",
    vatId = Some("CZ99887766"),
    firstName = "Tomáš",
    lastName = "Král",
    email = "tomas@eventagency.cz",
    phone = "+420 999 888 777",
    street = "Dlouhá 15",
    city = "Praha",
    postal = "11000",
    tags = Set("events", "seasonal"),
    createdAtMs = 1693000000000L,
  )

  private[customers] val adStudio: Customer = mkCustomer(
    id = adStudioId,
    status = CustomerStatus.Active,
    tier = CustomerTier.Gold,
    companyName = "Ad Studio s.r.o.",
    regNo = "33445566",
    vatId = Some("CZ33445566"),
    firstName = "Lucie",
    lastName = "Procházková",
    email = "lucie@adstudio.cz",
    phone = "+420 333 444 555",
    street = "Karlova 8",
    city = "Praha",
    postal = "11000",
    tags = Set("advertising"),
    createdAtMs = 1694000000000L,
  )

  private[customers] val startupHub: Customer = mkCustomer(
    id = startupHubId,
    status = CustomerStatus.Active,
    tier = CustomerTier.Standard,
    companyName = "StartupHub z.s.",
    regNo = "77889900",
    vatId = None,
    firstName = "David",
    lastName = "Horák",
    email = "david@startuphub.cz",
    phone = "+420 777 888 999",
    street = "Technická 2",
    city = "Praha",
    postal = "16000",
    tags = Set("startup"),
    createdAtMs = 1696000000000L,
  )

  private[customers] val packagingLtd: Customer = mkCustomer(
    id = packagingLtdId,
    status = CustomerStatus.Deactivated,
    tier = CustomerTier.Silver,
    companyName = "Packaging Ltd.",
    regNo = "44556677",
    vatId = Some("CZ44556677"),
    firstName = "Anna",
    lastName = "Veselá",
    email = "anna@packagingltd.cz",
    phone = "+420 444 555 666",
    street = "Průmyslová 30",
    city = "Ostrava",
    postal = "70200",
    tags = Set("packaging"),
    createdAtMs = 1688000000000L,
  )

  private[customers] val suspendedCo: Customer = mkCustomer(
    id = suspendedCoId,
    status = CustomerStatus.Suspended,
    tier = CustomerTier.Standard,
    companyName = "Suspended Co.",
    regNo = "66778899",
    vatId = None,
    firstName = "Karel",
    lastName = "Malý",
    email = "karel@suspendedco.cz",
    phone = "+420 666 777 888",
    street = "Hlavní 1",
    city = "Plzeň",
    postal = "30100",
    notes = List(AccountNote(NonEmptyString.unsafe("Suspended due to unpaid invoices"), Instant.ofEpochMilli(1698000000000L), Some(EmployeeId.unsafe("emp-1")))),
    createdAtMs = 1691000000000L,
  )

  private[customers] val pendingCo: Customer = mkCustomer(
    id = pendingCoId,
    status = CustomerStatus.PendingApproval,
    tier = CustomerTier.Standard,
    companyName = "Pending Co.",
    regNo = "11009988",
    vatId = Some("CZ11009988"),
    firstName = "Jiří",
    lastName = "Procházka",
    email = "jiri@pendingco.cz",
    phone = "+420 110 099 888",
    street = "Nová 42",
    city = "Liberec",
    postal = "46001",
    createdAtMs = 1700000000000L,
  )

  private[customers] val all: List[Customer] = List(
    printShopPro, graphicDesignCo, megaCorp, localPrint, eventAgency,
    adStudio, startupHub, packagingLtd, suspendedCo, pendingCo,
  )

  val allViews: List[CustomerView] = all.map(_.toView)

final class InMemoryCustomerRepository(
    ref: Ref[Map[CustomerId, Customer]],
) extends CustomerRepository:

  override def findById(id: CustomerId): UIO[Option[Customer]] =
    ref.get.map(_.get(id))

  override def findByEmail(email: EmailAddress): UIO[Option[Customer]] =
    ref.get.map(_.values.find(_.contact.email == email))

  override def findByRegistrationNumber(regNo: NonEmptyString): UIO[Option[Customer]] =
    val normalized = regNo.value.trim.replaceAll("\\s+", "")
    ref.get.map(_.values.find(_.company.exists(_.registrationNumber.value.trim.replaceAll("\\s+", "") == normalized)))

  override def findByVatId(vatId: NonEmptyString): UIO[Option[Customer]] =
    val normalized = vatId.value.trim.replaceAll("\\s+", "").toUpperCase
    ref.get.map(_.values.find(_.company.flatMap(_.vatId).exists(_.value.trim.replaceAll("\\s+", "").toUpperCase == normalized)))

  override def save(customer: Customer): UIO[Unit] =
    ref.update(_ + (customer.id -> customer))

  override def listAll: UIO[List[Customer]] =
    ref.get.map(_.values.toList.sortBy(_.id.value))

  def listAllViews: UIO[List[CustomerView]] =
    listAll.map(_.map(_.toView))

object CustomerSampleLayer:

  def repositoryLayer(
      initial: List[Customer] = SampleCustomers.all,
  ): ULayer[InMemoryCustomerRepository] =
    ZLayer.fromZIO(Ref.make(initial.map(c => c.id -> c).toMap).map(new InMemoryCustomerRepository(_)))

  def live(
      initial: List[Customer] = SampleCustomers.all,
  ): ZLayer[EventBus, Nothing, CustomerService] =
    (repositoryLayer(initial) ++ ZLayer.service[EventBus]) >>> CustomerServiceLive.layer
