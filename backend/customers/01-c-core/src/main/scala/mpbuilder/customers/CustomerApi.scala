package mpbuilder.customers

import mpbuilder.shared.*
import zio.*
import java.time.Instant

// ── Public Models, Commands, Errors & Events ─────────────────────────────────

enum CustomerStatus:
  case PendingApproval, Active, Suspended, Deactivated

final case class CompanyProfile(
    legalName: NonEmptyString,
    registrationNumber: NonEmptyString,
    vatId: Option[NonEmptyString] = None,
    contactPerson: NonEmptyString,
)

final case class ContactPerson(
    firstName: NonEmptyString,
    lastName: NonEmptyString,
    email: EmailAddress,
    phone: Option[NonEmptyString] = None,
)

enum PaymentTerms:
  case PrepaidOnly
  /** Net invoice credit terms — allowed only for Active Business/Agency customers. */
  case NetInvoice(dueDays: Int, creditLimit: Price)

final case class AccountNote(
    text: NonEmptyString,
    createdAt: Instant,
    authorId: Option[EmployeeId],
)

/** Public read view of a customer account (internal `Customer` aggregate lives in `core`).
  *
  * Note: Contains ZERO pricing rules (owned by `pricing`) and ZERO auth credentials
  * (owned by identity/auth).
  */
final case class CustomerView(
    id: CustomerId,
    segment: CustomerSegment,
    status: CustomerStatus,
    tier: CustomerTier,
    company: Option[CompanyProfile],
    contact: ContactPerson,
    billingAddress: Address,
    shippingAddresses: List[Address],
    paymentTerms: PaymentTerms,
    tags: Set[String],
    createdAt: Instant,
)

final case class RegisterCustomerCmd(
    id: CustomerId,
    segment: CustomerSegment,
    company: Option[CompanyProfile],
    contact: ContactPerson,
    billingAddress: Address,
)

enum CustomerError(val message: String):
  case NotFound(id: CustomerId)
      extends CustomerError(s"Customer '${id.value}' not found")
  case EmailAlreadyRegistered(email: EmailAddress)
      extends CustomerError(s"Email '${email.value}' is already registered")
  case CompanyProfileRequired(segment: CustomerSegment)
      extends CustomerError(s"Company profile is required for $segment customers")
  case InvalidStatusTransition(id: CustomerId, from: CustomerStatus, to: CustomerStatus)
      extends CustomerError(s"Cannot transition customer '${id.value}' from $from to $to")
  case NetInvoiceNotEligible(id: CustomerId, reason: String)
      extends CustomerError(s"Customer '${id.value}' is not eligible for NetInvoice terms: $reason")
  case InvalidCreditTerms(detail: String)
      extends CustomerError(s"Invalid credit terms: $detail")

// ── Domain Events ────────────────────────────────────────────────────────────

final case class CustomerRegistered(
    customerId: CustomerId,
    segment: CustomerSegment,
    tier: CustomerTier,
    occurredAt: Instant,
) extends DomainEvent

final case class CustomerApproved(
    customerId: CustomerId,
    occurredAt: Instant,
) extends DomainEvent

final case class CustomerTierChanged(
    customerId: CustomerId,
    previousTier: CustomerTier,
    newTier: CustomerTier,
    occurredAt: Instant,
) extends DomainEvent

// ── Public Inbound Port (Module API) ─────────────────────────────────────────

trait CustomerService:
  def register(cmd: RegisterCustomerCmd): IO[CustomerError, CustomerView]
  def getById(id: CustomerId): IO[CustomerError.NotFound, CustomerView]
  def approve(id: CustomerId): IO[CustomerError, CustomerView]
  def suspend(id: CustomerId, reason: NonEmptyString, by: Option[EmployeeId]): IO[CustomerError, CustomerView]
  def changeTier(id: CustomerId, newTier: CustomerTier): IO[CustomerError, CustomerView]
  def grantPaymentTerms(id: CustomerId, terms: PaymentTerms): IO[CustomerError, CustomerView]

object CustomerService:
  def register(cmd: RegisterCustomerCmd): ZIO[CustomerService, CustomerError, CustomerView] =
    ZIO.serviceWithZIO[CustomerService](_.register(cmd))
