package mpbuilder.customers

import mpbuilder.commons.*
import zio.{IO, NonEmptyChunk, ZIO}

/** How a customer identifies themselves at sign-in. */
enum Identifier:
  case Email(value: String)
  case BusinessId(value: String)
  case VatId(value: String)

final case class CompanyDetails(
    name: String,
    businessId: Option[String] = None,
    vatId: Option[String] = None,
)

/** What other contexts are allowed to know about a customer.
  *
  * Note what is *not* here: negotiated pricing. That belongs to pricing, which resolves it from a
  * customer id. Order-intake asking "what does this cost for this customer" goes to pricing, not
  * here — so a customer record and a price list can evolve independently.
  */
final case class CustomerSummary(
    id: String,
    displayName: String,
    email: String,
    company: Option[CompanyDetails],
    customerType: String,
    status: String,
    tier: String,
    /** Only an active account may sign in or place an order. */
    isActive: Boolean,
    /** Whether "invoice on account" may be offered at checkout.
      *
      * Derived here, once, rather than re-deriving the corporate-and-active rule in every context
      * that renders a payment step.
      */
    canPayOnAccount: Boolean,
)

final case class RegisterCustomer(
    firstName: String,
    lastName: String,
    email: String,
    phone: String,
    company: Option[CompanyDetails] = None,
)

enum CustomerError extends DomainError:
  case Rejected(problems: NonEmptyChunk[Problem])
  case NotFound(id: String)
  case EmailAlreadyRegistered(email: String)

  def message(lang: Language): String = this match
    case Rejected(problems) => problems.map(_.message(lang)).mkString("; ")
    case NotFound(id) =>
      lang match
        case Language.En => s"No customer with id '$id'"
        case Language.Cs => s"Zákazník s id '$id' neexistuje"
    case EmailAlreadyRegistered(email) =>
      lang match
        case Language.En => s"An account already exists for $email"
        case Language.Cs => s"Účet pro $email již existuje"

/** The customers context's public contract: who we sell to. */
trait CustomerService:

  def find(id: String): IO[CustomerError, Option[CustomerSummary]]

  /** Look up by any of the identifiers a business customer may sign in with.
    *
    * Accounts awaiting approval are reported as absent rather than as a distinct state, so an
    * applicant cannot learn from the sign-in form that their application exists yet.
    */
  def findBy(identifier: Identifier): IO[CustomerError, Option[CustomerSummary]]

  /** Create a customer from details collected during checkout. */
  def register(input: RegisterCustomer): IO[CustomerError, CustomerSummary]

object CustomerService:
  def find(id: String): ZIO[CustomerService, CustomerError, Option[CustomerSummary]] =
    ZIO.serviceWithZIO(_.find(id))

  def findBy(identifier: Identifier): ZIO[CustomerService, CustomerError, Option[CustomerSummary]] =
    ZIO.serviceWithZIO(_.findBy(identifier))

  def register(input: RegisterCustomer): ZIO[CustomerService, CustomerError, CustomerSummary] =
    ZIO.serviceWithZIO(_.register(input))
