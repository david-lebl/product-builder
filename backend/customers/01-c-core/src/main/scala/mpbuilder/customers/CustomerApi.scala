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
    roleTitle: Option[NonEmptyString] = None,
)

enum PaymentTerms:
  case PrepaidOnly
  /** Net invoice credit terms — allowed only for Active Business/Agency customers. */
  case NetInvoice(dueDays: Int, creditLimit: Price)

enum CustomerActor:
  case System
  case CustomerSelf(customerId: CustomerId, name: NonEmptyString)
  case Employee(employeeId: EmployeeId, name: Option[NonEmptyString] = None)

final case class CustomerAuditEntry(
    action: String,
    summary: String,
    actor: CustomerActor,
    occurredAt: Instant,
)

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
    notes: List[AccountNote] = Nil,
    additionalContacts: List[ContactPerson] = Nil,
    defaultShippingIndex: Option[Int] = None,
    auditLog: List[CustomerAuditEntry] = Nil,
):
  def defaultShippingAddress: Option[Address] =
    defaultShippingIndex.flatMap(shippingAddresses.lift).orElse(shippingAddresses.headOption)

final case class CustomerFilter(
    status: Option[CustomerStatus] = None,
    segment: Option[CustomerSegment] = None,
    tier: Option[CustomerTier] = None,
    query: Option[String] = None,
)

final case class RegisterCustomerCmd(
    id: CustomerId,
    segment: CustomerSegment,
    company: Option[CompanyProfile],
    contact: ContactPerson,
    billingAddress: Address,
    additionalContacts: List[ContactPerson] = Nil,
    shippingAddresses: List[Address] = Nil,
    defaultShippingIndex: Option[Int] = None,
)

final case class UpdateCustomerProfileCmd(
    id: CustomerId,
    company: Option[CompanyProfile],
    contact: ContactPerson,
    billingAddress: Address,
    tags: Set[String],
    additionalContacts: Option[List[ContactPerson]] = None,
    shippingAddresses: Option[List[Address]] = None,
    defaultShippingIndex: Option[Int] = None,
    actor: CustomerActor = CustomerActor.System,
)

final case class UpdateCustomerAddressesCmd(
    id: CustomerId,
    billingAddress: Address,
    shippingAddresses: List[Address],
    defaultShippingIndex: Option[Int],
    actor: CustomerActor = CustomerActor.System,
)

enum CustomerIdentifier:
  case Email(email: EmailAddress)
  case CompanyRegNo(registrationNumber: NonEmptyString)
  case VatId(vatId: NonEmptyString)

enum CustomerError(val message: String):
  case NotFound(id: CustomerId)
      extends CustomerError(s"Customer '${id.value}' not found")
  case NotFoundByIdentifier(identifier: CustomerIdentifier)
      extends CustomerError(s"Customer not found for identifier $identifier")
  case EmailAlreadyRegistered(email: EmailAddress)
      extends CustomerError(s"Email '${email.value}' is already registered")
  case CompanyProfileRequired(segment: CustomerSegment)
      extends CustomerError(s"Company profile is required for $segment customers")
  case MultipleContactsNotAllowed(segment: CustomerSegment)
      extends CustomerError(s"Additional contact persons are only supported for Business and Agency accounts (got $segment)")
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
  def findByIdentifier(identifier: CustomerIdentifier): IO[CustomerError.NotFoundByIdentifier, CustomerView]
  def listCustomers(filter: CustomerFilter = CustomerFilter()): UIO[List[CustomerView]]
  def approve(id: CustomerId, actor: CustomerActor = CustomerActor.System): IO[CustomerError, CustomerView]
  def suspend(id: CustomerId, reason: NonEmptyString, by: Option[EmployeeId], byName: Option[NonEmptyString] = None): IO[CustomerError, CustomerView]
  def deactivate(id: CustomerId, reason: NonEmptyString, by: Option[EmployeeId], byName: Option[NonEmptyString] = None): IO[CustomerError, CustomerView]
  def changeTier(id: CustomerId, newTier: CustomerTier, actor: CustomerActor = CustomerActor.System): IO[CustomerError, CustomerView]
  def grantPaymentTerms(id: CustomerId, terms: PaymentTerms, actor: CustomerActor = CustomerActor.System): IO[CustomerError, CustomerView]
  def updateProfile(cmd: UpdateCustomerProfileCmd): IO[CustomerError, CustomerView]
  def updateAddresses(cmd: UpdateCustomerAddressesCmd): IO[CustomerError, CustomerView]
  def addNote(id: CustomerId, text: NonEmptyString, by: Option[EmployeeId], byName: Option[NonEmptyString] = None): IO[CustomerError, CustomerView]

object CustomerService:
  def register(cmd: RegisterCustomerCmd): ZIO[CustomerService, CustomerError, CustomerView] =
    ZIO.serviceWithZIO[CustomerService](_.register(cmd))
