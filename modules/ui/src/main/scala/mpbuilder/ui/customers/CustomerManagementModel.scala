package mpbuilder.ui.customers

import mpbuilder.domain.model.*

/** Which section of the customer management UI is currently active. */
enum CustomerSection:
  case Customers, DiscountCodes

/** Edit state for the customer management UI — tracks which entity is being edited. */
enum CustomerEditState:
  case None
  case EditingCustomer(id: CustomerId)
  case CreatingCustomer
  case EditingDiscountCode(id: DiscountCodeId)
  case CreatingDiscountCode

/** Full state of the customer management UI. */
final case class CustomerManagementState(
    customers: List[Customer],
    discountCodes: List[DiscountCode],
    activeSection: CustomerSection,
    editState: CustomerEditState,
    statusMessage: Option[String],
)
