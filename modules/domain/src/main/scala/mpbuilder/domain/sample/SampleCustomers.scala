package mpbuilder.domain.sample

import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*

object SampleCustomers:

  // --- Customer IDs ---
  val printShopProId: CustomerId   = CustomerId.unsafe("cust-print-shop-pro")
  val graphicDesignCoId: CustomerId = CustomerId.unsafe("cust-graphic-design-co")
  val megaCorpId: CustomerId       = CustomerId.unsafe("cust-mega-corp")
  val localPrintId: CustomerId     = CustomerId.unsafe("cust-local-print")
  val eventAgencyId: CustomerId    = CustomerId.unsafe("cust-event-agency")
  val adStudioId: CustomerId       = CustomerId.unsafe("cust-ad-studio")
  val startupHubId: CustomerId     = CustomerId.unsafe("cust-startup-hub")
  val packagingLtdId: CustomerId   = CustomerId.unsafe("cust-packaging-ltd")
  val suspendedCoId: CustomerId    = CustomerId.unsafe("cust-suspended-co")
  val pendingCoId: CustomerId      = CustomerId.unsafe("cust-pending-co")

  val printShopPro: Customer = Customer(
    id = printShopProId,
    customerType = CustomerType.Agency,
    status = CustomerStatus.Active,
    tier = CustomerTier.Gold,
    companyInfo = Some(CompanyInfo("Print Shop Pro s.r.o.", "12345678", Some("CZ12345678"), "Jan Novák")),
    contactInfo = ContactInfo("Jan", "Novák", "jan@printshoppro.cz", "+420 123 456 789", Some("Print Shop Pro s.r.o."), Some("12345678"), Some("CZ12345678")),
    address = Address("Vinohradská 10", "Praha", "12000", "CZ"),
    pricing = CustomerPricing(
      globalDiscount = Some(Percentage.unsafe(BigDecimal("10"))),
      materialDiscounts = Map(
        SampleCatalog.coated300gsmId -> Percentage.unsafe(BigDecimal("15")),
      ),
    ),
    internalNotes = List(CustomerNote("VIP customer, priority handling", 1700000000000L, Some(EmployeeId.unsafe("emp-1")))),
    createdAt = 1690000000000L,
    lastOrderAt = Some(1700000000000L),
    tags = Set("vip", "agency"),
  )

  val graphicDesignCo: Customer = Customer(
    id = graphicDesignCoId,
    customerType = CustomerType.Agency,
    status = CustomerStatus.Active,
    tier = CustomerTier.Silver,
    companyInfo = Some(CompanyInfo("Graphic Design Co.", "87654321", Some("CZ87654321"), "Marie Svobodová")),
    contactInfo = ContactInfo("Marie", "Svobodová", "marie@graphicdesign.cz", "+420 987 654 321", Some("Graphic Design Co."), Some("87654321"), Some("CZ87654321")),
    address = Address("Národní 5", "Praha", "11000", "CZ"),
    pricing = CustomerPricing(
      globalDiscount = Some(Percentage.unsafe(BigDecimal("5"))),
      finishDiscounts = Map(
        SampleCatalog.matteLaminationId -> Percentage.unsafe(BigDecimal("20")),
      ),
    ),
    internalNotes = Nil,
    createdAt = 1692000000000L,
    lastOrderAt = Some(1699000000000L),
    tags = Set("design"),
  )

  val megaCorp: Customer = Customer(
    id = megaCorpId,
    customerType = CustomerType.Agency,
    status = CustomerStatus.Active,
    tier = CustomerTier.Platinum,
    companyInfo = Some(CompanyInfo("MegaCorp a.s.", "11223344", Some("CZ11223344"), "Petr Dvořák")),
    contactInfo = ContactInfo("Petr", "Dvořák", "petr@megacorp.cz", "+420 111 222 333", Some("MegaCorp a.s."), Some("11223344"), Some("CZ11223344")),
    address = Address("Wenceslas Square 1", "Praha", "11000", "CZ"),
    pricing = CustomerPricing(
      globalDiscount = Some(Percentage.unsafe(BigDecimal("15"))),
      categoryDiscounts = Map(
        SampleCatalog.businessCardsId -> Percentage.unsafe(BigDecimal("20")),
        SampleCatalog.flyersId -> Percentage.unsafe(BigDecimal("18")),
      ),
      fixedMaterialPrices = Map(
        SampleCatalog.coated300gsmId -> Price(Money("0.09"), Currency.USD),
      ),
      customQuantityTiers = Some(List(
        PricingRule.QuantityTier(1, Some(99), BigDecimal("1.0")),
        PricingRule.QuantityTier(100, Some(499), BigDecimal("0.85")),
        PricingRule.QuantityTier(500, Some(1999), BigDecimal("0.70")),
        PricingRule.QuantityTier(2000, None, BigDecimal("0.60")),
      )),
    ),
    internalNotes = List(
      CustomerNote("Largest corporate account", 1691000000000L, Some(EmployeeId.unsafe("emp-1"))),
      CustomerNote("Renewed annual contract", 1698000000000L, Some(EmployeeId.unsafe("emp-2"))),
    ),
    createdAt = 1685000000000L,
    lastOrderAt = Some(1700500000000L),
    tags = Set("enterprise", "vip"),
  )

  val localPrint: Customer = Customer(
    id = localPrintId,
    customerType = CustomerType.Agency,
    status = CustomerStatus.Active,
    tier = CustomerTier.Standard,
    companyInfo = Some(CompanyInfo("Local Print", "55667788", None, "Eva Černá")),
    contactInfo = ContactInfo("Eva", "Černá", "eva@localprint.cz", "+420 555 666 777", Some("Local Print"), Some("55667788"), None),
    address = Address("Masarykova 22", "Brno", "60200", "CZ"),
    pricing = CustomerPricing.empty,
    internalNotes = Nil,
    createdAt = 1695000000000L,
    lastOrderAt = None,
    tags = Set.empty,
  )

  val eventAgency: Customer = Customer(
    id = eventAgencyId,
    customerType = CustomerType.Agency,
    status = CustomerStatus.Active,
    tier = CustomerTier.Silver,
    companyInfo = Some(CompanyInfo("Event Agency s.r.o.", "99887766", Some("CZ99887766"), "Tomáš Král")),
    contactInfo = ContactInfo("Tomáš", "Král", "tomas@eventagency.cz", "+420 999 888 777", Some("Event Agency s.r.o."), Some("99887766"), Some("CZ99887766")),
    address = Address("Dlouhá 15", "Praha", "11000", "CZ"),
    pricing = CustomerPricing(
      categoryDiscounts = Map(
        SampleCatalog.bannersId -> Percentage.unsafe(BigDecimal("12")),
      ),
      materialDiscounts = Map(
        SampleCatalog.vinylId -> Percentage.unsafe(BigDecimal("10")),
      ),
    ),
    internalNotes = List(CustomerNote("Seasonal high-volume orders during events", 1697000000000L, None)),
    createdAt = 1693000000000L,
    lastOrderAt = Some(1698500000000L),
    tags = Set("events", "seasonal"),
  )

  val adStudio: Customer = Customer(
    id = adStudioId,
    customerType = CustomerType.Agency,
    status = CustomerStatus.Active,
    tier = CustomerTier.Gold,
    companyInfo = Some(CompanyInfo("Ad Studio s.r.o.", "33445566", Some("CZ33445566"), "Lucie Procházková")),
    contactInfo = ContactInfo("Lucie", "Procházková", "lucie@adstudio.cz", "+420 333 444 555", Some("Ad Studio s.r.o."), Some("33445566"), Some("CZ33445566")),
    address = Address("Karlova 8", "Praha", "11000", "CZ"),
    pricing = CustomerPricing(
      globalDiscount = Some(Percentage.unsafe(BigDecimal("8"))),
      finishDiscounts = Map(
        SampleCatalog.foilStampingId -> Percentage.unsafe(BigDecimal("15")),
        SampleCatalog.embossingId -> Percentage.unsafe(BigDecimal("10")),
      ),
    ),
    internalNotes = Nil,
    createdAt = 1694000000000L,
    lastOrderAt = Some(1699500000000L),
    tags = Set("advertising"),
  )

  val startupHub: Customer = Customer(
    id = startupHubId,
    customerType = CustomerType.Agency,
    status = CustomerStatus.Active,
    tier = CustomerTier.Standard,
    companyInfo = Some(CompanyInfo("StartupHub z.s.", "77889900", None, "David Horák")),
    contactInfo = ContactInfo("David", "Horák", "david@startuphub.cz", "+420 777 888 999", Some("StartupHub z.s."), Some("77889900"), None),
    address = Address("Technická 2", "Praha", "16000", "CZ"),
    pricing = CustomerPricing(
      globalDiscount = Some(Percentage.unsafe(BigDecimal("3"))),
    ),
    internalNotes = Nil,
    createdAt = 1696000000000L,
    lastOrderAt = Some(1697000000000L),
    tags = Set("startup"),
  )

  val packagingLtd: Customer = Customer(
    id = packagingLtdId,
    customerType = CustomerType.Agency,
    status = CustomerStatus.Inactive,
    tier = CustomerTier.Silver,
    companyInfo = Some(CompanyInfo("Packaging Ltd.", "44556677", Some("CZ44556677"), "Anna Veselá")),
    contactInfo = ContactInfo("Anna", "Veselá", "anna@packagingltd.cz", "+420 444 555 666", Some("Packaging Ltd."), Some("44556677"), Some("CZ44556677")),
    address = Address("Průmyslová 30", "Ostrava", "70200", "CZ"),
    pricing = CustomerPricing(
      globalDiscount = Some(Percentage.unsafe(BigDecimal("5"))),
      categoryDiscounts = Map(
        SampleCatalog.packagingId -> Percentage.unsafe(BigDecimal("10")),
      ),
    ),
    internalNotes = List(CustomerNote("Account deactivated — no orders for 6 months", 1699000000000L, Some(EmployeeId.unsafe("emp-1")))),
    createdAt = 1688000000000L,
    lastOrderAt = Some(1693000000000L),
    tags = Set("packaging"),
  )

  val suspendedCo: Customer = Customer(
    id = suspendedCoId,
    customerType = CustomerType.Agency,
    status = CustomerStatus.Suspended,
    tier = CustomerTier.Standard,
    companyInfo = Some(CompanyInfo("Suspended Co.", "66778899", None, "Karel Malý")),
    contactInfo = ContactInfo("Karel", "Malý", "karel@suspendedco.cz", "+420 666 777 888", Some("Suspended Co."), Some("66778899"), None),
    address = Address("Hlavní 1", "Plzeň", "30100", "CZ"),
    pricing = CustomerPricing.empty,
    internalNotes = List(CustomerNote("Suspended due to unpaid invoices", 1698000000000L, Some(EmployeeId.unsafe("emp-1")))),
    createdAt = 1691000000000L,
    lastOrderAt = Some(1696000000000L),
    tags = Set.empty,
  )

  val pendingCo: Customer = Customer(
    id = pendingCoId,
    customerType = CustomerType.Agency,
    status = CustomerStatus.PendingApproval,
    tier = CustomerTier.Standard,
    companyInfo = Some(CompanyInfo("Pending Co.", "11009988", Some("CZ11009988"), "Jiří Procházka")),
    contactInfo = ContactInfo("Jiří", "Procházka", "jiri@pendingco.cz", "+420 110 099 888", Some("Pending Co."), Some("11009988"), Some("CZ11009988")),
    address = Address("Nová 42", "Liberec", "46001", "CZ"),
    pricing = CustomerPricing.empty,
    internalNotes = Nil,
    createdAt = 1700000000000L,
    lastOrderAt = None,
    tags = Set.empty,
  )

  /** All sample customers */
  val all: List[Customer] = List(
    printShopPro,
    graphicDesignCo,
    megaCorp,
    localPrint,
    eventAgency,
    adStudio,
    startupHub,
    packagingLtd,
    suspendedCo,
    pendingCo,
  )
