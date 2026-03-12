package mpbuilder.domain.model

/** Status of a customer account */
enum CustomerStatus:
  case Active, Inactive, Suspended, PendingApproval

  def displayName: LocalizedString = this match
    case Active          => LocalizedString("Active", "Aktivní")
    case Inactive        => LocalizedString("Inactive", "Neaktivní")
    case Suspended       => LocalizedString("Suspended", "Pozastavený")
    case PendingApproval => LocalizedString("Pending Approval", "Čeká na schválení")

/** Tier of a customer — drives default discount levels */
enum CustomerTier:
  case Standard, Silver, Gold, Platinum

  def displayName: LocalizedString = this match
    case Standard => LocalizedString("Standard", "Standardní")
    case Silver   => LocalizedString("Silver", "Stříbrný")
    case Gold     => LocalizedString("Gold", "Zlatý")
    case Platinum => LocalizedString("Platinum", "Platinový")

/** Company-specific information for agency (B2B) customers */
final case class CompanyInfo(
    companyName: String,
    businessId: String,
    vatId: Option[String],
    contactPerson: String,
)

/** An internal note attached to a customer by an operator */
final case class CustomerNote(
    text: String,
    createdAt: Long,
    createdBy: Option[EmployeeId],
)

/** A customer entity — represents both agency (B2B) and future regular (B2C) customers */
final case class Customer(
    id: CustomerId,
    customerType: CustomerType,
    status: CustomerStatus,
    tier: CustomerTier,
    companyInfo: Option[CompanyInfo],
    contactInfo: ContactInfo,
    address: Address,
    internalNotes: List[CustomerNote],
    createdAt: Long,
    lastOrderAt: Option[Long],
    tags: Set[String],
)
