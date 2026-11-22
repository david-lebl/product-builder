package mpbuilder.backoffice

import mpbuilder.artwork.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*

// ── Errors ───────────────────────────────────────────────────────────────────

enum BackofficeError(val message: String):
  case Unauthenticated(reason: String = "Authentication required")
      extends BackofficeError(reason)
  case Forbidden(requiredRoles: Set[EmployeeRole], actualRoles: Set[EmployeeRole])
      extends BackofficeError(
        s"Access denied: requires one of [${requiredRoles.mkString(", ")}], " +
          s"but caller has [${actualRoles.mkString(", ")}]"
      )
  case NotAnEmployeeSession
      extends BackofficeError("Backoffice requires an employee account")
  case CatalogFailure(error: CatalogError)   extends BackofficeError(error.message)
  case PricingFailure(error: PricingError)   extends BackofficeError(error.message)
  case CustomerFailure(error: CustomerError) extends BackofficeError(error.message)
  case IdentityFailure(error: IdentityError) extends BackofficeError(error.message)
  case OrderFailure(error: OrderError)       extends BackofficeError(error.message)
  case ArtworkFailure(error: ArtworkError)   extends BackofficeError(error.message)
  case PresetNotFound(categoryId: CategoryId, presetId: PresetId)
      extends BackofficeError(s"Preset '${presetId.value}' not found in category '${categoryId.value}'")

// ── Staff Auth & Dashboard Models ────────────────────────────────────────────

final case class StaffAuthResult(
    session: AuthSession,
    employee: Principal.Employee,
)

final case class BackofficeDashboardSummary(
    totalOrders: Int,
    ordersAwaitingIntake: Int,
    ordersAwaitingPayment: Int,
    ordersAccepted: Int,
    ordersInProduction: Int,
    ordersReadyForDispatch: Int,
    ordersOnHold: Int,
    ordersCompleted: Int,
    pendingB2BCustomers: Int,
    totalCustomers: Int,
    activePromoCodes: Int,
    catalogCategoryCount: Int,
    catalogMaterialCount: Int,
    recentOrders: List[OrderView],
    pendingCustomers: List[CustomerView],
)

final case class CustomerDetailView(
    customer: CustomerView,
    orders: List[OrderView],
    agreements: List[CustomerPriceAgreement],
)

final case class MarginSimulationCmd(
    categoryId: CategoryId,
    presetId: PresetId,
    quantity: Quantity,
    currency: Currency,
    speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
    customerId: Option[CustomerId] = None,
    customerTier: Option[CustomerTier] = None,
    promoCode: Option[String] = None,
)

final case class MarginSimulationResult(
    quote: PriceQuote,
    margin: MarginAnalysis,
    sheetsUsedTotal: Int,
)

final case class StoreDeliverySettingsView(
    pickupLocations: List[ShopPickupLocation],
    couriersByCurrency: Map[Currency, List[CourierServiceOption]],
    bankAccounts: Map[Currency, BankAccountConfig],
)
