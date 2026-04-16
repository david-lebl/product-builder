package mpbuilder.ui

import com.raquo.waypoint.*
import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.CategoryId

/** Page hierarchy representing all routable pages in the application.
  *
  * This sealed trait and its subtypes define all possible pages that can be
  * navigated to in the application. Waypoint uses these types for type-safe
  * routing with the browser's History API.
  */
sealed trait Page

object Page:
  // Top-level pages
  case object ProductCatalog extends Page
  case object ProductBuilder extends Page
  case class VisualEditor(artworkId: Option[String] = None) extends Page
  case object Checkout extends Page
  case object OrderHistory extends Page
  case object CustomerPortal extends Page
  case class ProductDetail(categoryId: String) extends Page

  // Manufacturing sub-pages
  sealed trait ManufacturingPage extends Page
  case object ManufacturingDashboard extends ManufacturingPage
  case object ManufacturingStationQueue extends ManufacturingPage
  case object ManufacturingOrderApproval extends ManufacturingPage
  case object ManufacturingOrderProgress extends ManufacturingPage
  case object ManufacturingEmployees extends ManufacturingPage
  case object ManufacturingMachines extends ManufacturingPage
  case object ManufacturingAnalytics extends ManufacturingPage
  case object ManufacturingSettings extends ManufacturingPage

  // Catalog editor sub-pages
  sealed trait CatalogPage extends Page
  case object CatalogCategories extends CatalogPage
  case object CatalogMaterials extends CatalogPage
  case object CatalogFinishes extends CatalogPage
  case object CatalogPrintingMethods extends CatalogPage
  case object CatalogRules extends CatalogPage
  case object CatalogPricelist extends CatalogPage
  case object CatalogExport extends CatalogPage

  // Customer management sub-pages
  sealed trait CustomerPage extends Page
  case object CustomersList extends CustomerPage
  case object CustomerPricing extends CustomerPage
  case object DiscountCodes extends CustomerPage

/** Router configuration using Waypoint for URL-based navigation with browser History API. */
object Router extends com.raquo.waypoint.Router[Page](
  routes = Router.allRoutes,
  getPageTitle = Router.getPageTitle,
  serializePage = Router.serializePage,
  deserializePage = Router.deserializePage
):

  // Top-level routes
  private val catalogRoute = Route.static(Page.ProductCatalog, root / "catalog" / endOfSegments)
  private val builderRoute = Route.static(Page.ProductBuilder, root / "builder" / endOfSegments)

  // Visual editor routes - with and without artwork ID
  private val visualEditorWithArtworkRoute = Route[Page.VisualEditor, String](
    encode = ve => ve.artworkId.getOrElse(""),
    decode = artId => Page.VisualEditor(if artId.isEmpty then None else Some(artId)),
    pattern = root / "editor" / segment[String] / endOfSegments
  )

  private val visualEditorRoute = Route(
    encode = (_: Page.VisualEditor) => (),
    decode = (_: Unit) => Page.VisualEditor(None),
    pattern = root / "editor" / endOfSegments
  )

  private val checkoutRoute = Route.static(Page.Checkout, root / "checkout" / endOfSegments)
  private val orderHistoryRoute = Route.static(Page.OrderHistory, root / "order-history" / endOfSegments)
  private val customerPortalRoute = Route.static(Page.CustomerPortal, root / "my-orders" / endOfSegments)

  private val productDetailRoute = Route[Page.ProductDetail, String](
    encode = _.categoryId,
    decode = id => Page.ProductDetail(id),
    pattern = root / "product" / segment[String] / endOfSegments
  )

  // Manufacturing routes
  private val manufacturingDashboardRoute = Route.static(Page.ManufacturingDashboard, root / "manufacturing" / endOfSegments)
  private val manufacturingStationQueueRoute = Route.static(Page.ManufacturingStationQueue, root / "manufacturing" / "station-queue" / endOfSegments)
  private val manufacturingOrderApprovalRoute = Route.static(Page.ManufacturingOrderApproval, root / "manufacturing" / "order-approval" / endOfSegments)
  private val manufacturingOrderProgressRoute = Route.static(Page.ManufacturingOrderProgress, root / "manufacturing" / "order-progress" / endOfSegments)
  private val manufacturingEmployeesRoute = Route.static(Page.ManufacturingEmployees, root / "manufacturing" / "employees" / endOfSegments)
  private val manufacturingMachinesRoute = Route.static(Page.ManufacturingMachines, root / "manufacturing" / "machines" / endOfSegments)
  private val manufacturingAnalyticsRoute = Route.static(Page.ManufacturingAnalytics, root / "manufacturing" / "analytics" / endOfSegments)
  private val manufacturingSettingsRoute = Route.static(Page.ManufacturingSettings, root / "manufacturing" / "settings" / endOfSegments)

  // Catalog routes
  private val catalogCategoriesRoute = Route.static(Page.CatalogCategories, root / "catalog-editor" / "categories" / endOfSegments)
  private val catalogMaterialsRoute = Route.static(Page.CatalogMaterials, root / "catalog-editor" / "materials" / endOfSegments)
  private val catalogFinishesRoute = Route.static(Page.CatalogFinishes, root / "catalog-editor" / "finishes" / endOfSegments)
  private val catalogPrintingMethodsRoute = Route.static(Page.CatalogPrintingMethods, root / "catalog-editor" / "printing-methods" / endOfSegments)
  private val catalogRulesRoute = Route.static(Page.CatalogRules, root / "catalog-editor" / "rules" / endOfSegments)
  private val catalogPricelistRoute = Route.static(Page.CatalogPricelist, root / "catalog-editor" / "pricelist" / endOfSegments)
  private val catalogExportRoute = Route.static(Page.CatalogExport, root / "catalog-editor" / "export" / endOfSegments)

  // Customer management routes
  private val customersListRoute = Route.static(Page.CustomersList, root / "customers" / endOfSegments)
  private val customerPricingRoute = Route.static(Page.CustomerPricing, root / "customers" / "pricing" / endOfSegments)
  private val discountCodesRoute = Route.static(Page.DiscountCodes, root / "customers" / "discount-codes" / endOfSegments)

  // All routes combined (order matters - more specific routes first)
  private val allRoutes: List[Route[? <: Page, ?]] = List(
    // Manufacturing sub-routes (before dashboard)
    manufacturingStationQueueRoute,
    manufacturingOrderApprovalRoute,
    manufacturingOrderProgressRoute,
    manufacturingEmployeesRoute,
    manufacturingMachinesRoute,
    manufacturingAnalyticsRoute,
    manufacturingSettingsRoute,
    manufacturingDashboardRoute,

    // Catalog sub-routes
    catalogCategoriesRoute,
    catalogMaterialsRoute,
    catalogFinishesRoute,
    catalogPrintingMethodsRoute,
    catalogRulesRoute,
    catalogPricelistRoute,
    catalogExportRoute,

    // Customer sub-routes (before main)
    customerPricingRoute,
    discountCodesRoute,
    customersListRoute,

    // Visual editor with artwork ID (before generic editor)
    visualEditorWithArtworkRoute,
    visualEditorRoute,

    // Top-level routes
    productDetailRoute,
    catalogRoute,
    builderRoute,
    checkoutRoute,
    orderHistoryRoute,
    customerPortalRoute,
  )

  /** Simple page serialization using page class names. */
  private def serializePage(page: Page): String = page match
    case Page.ProductCatalog => "catalog"
    case Page.ProductBuilder => "builder"
    case Page.VisualEditor(artId) => s"editor:${artId.getOrElse("")}"
    case Page.Checkout => "checkout"
    case Page.OrderHistory => "order-history"
    case Page.CustomerPortal => "my-orders"
    case Page.ProductDetail(id) => s"product:$id"
    case Page.ManufacturingDashboard => "manufacturing"
    case Page.ManufacturingStationQueue => "manufacturing:station-queue"
    case Page.ManufacturingOrderApproval => "manufacturing:order-approval"
    case Page.ManufacturingOrderProgress => "manufacturing:order-progress"
    case Page.ManufacturingEmployees => "manufacturing:employees"
    case Page.ManufacturingMachines => "manufacturing:machines"
    case Page.ManufacturingAnalytics => "manufacturing:analytics"
    case Page.ManufacturingSettings => "manufacturing:settings"
    case Page.CatalogCategories => "catalog-editor:categories"
    case Page.CatalogMaterials => "catalog-editor:materials"
    case Page.CatalogFinishes => "catalog-editor:finishes"
    case Page.CatalogPrintingMethods => "catalog-editor:printing-methods"
    case Page.CatalogRules => "catalog-editor:rules"
    case Page.CatalogPricelist => "catalog-editor:pricelist"
    case Page.CatalogExport => "catalog-editor:export"
    case Page.CustomersList => "customers"
    case Page.CustomerPricing => "customers:pricing"
    case Page.DiscountCodes => "customers:discount-codes"

  private def deserializePage(str: String): Page = str match
    case "catalog" => Page.ProductCatalog
    case "builder" => Page.ProductBuilder
    case s if s.startsWith("editor:") =>
      val artId = s.stripPrefix("editor:")
      Page.VisualEditor(if artId.isEmpty then None else Some(artId))
    case "checkout" => Page.Checkout
    case "order-history" => Page.OrderHistory
    case "my-orders" => Page.CustomerPortal
    case s if s.startsWith("product:") => Page.ProductDetail(s.stripPrefix("product:"))
    case "manufacturing" => Page.ManufacturingDashboard
    case "manufacturing:station-queue" => Page.ManufacturingStationQueue
    case "manufacturing:order-approval" => Page.ManufacturingOrderApproval
    case "manufacturing:order-progress" => Page.ManufacturingOrderProgress
    case "manufacturing:employees" => Page.ManufacturingEmployees
    case "manufacturing:machines" => Page.ManufacturingMachines
    case "manufacturing:analytics" => Page.ManufacturingAnalytics
    case "manufacturing:settings" => Page.ManufacturingSettings
    case "catalog-editor:categories" => Page.CatalogCategories
    case "catalog-editor:materials" => Page.CatalogMaterials
    case "catalog-editor:finishes" => Page.CatalogFinishes
    case "catalog-editor:printing-methods" => Page.CatalogPrintingMethods
    case "catalog-editor:rules" => Page.CatalogRules
    case "catalog-editor:pricelist" => Page.CatalogPricelist
    case "catalog-editor:export" => Page.CatalogExport
    case "customers" => Page.CustomersList
    case "customers:pricing" => Page.CustomerPricing
    case "customers:discount-codes" => Page.DiscountCodes
    case _ => Page.ProductCatalog // Default fallback

  /** Page title generation for browser tab/history. */
  private def getPageTitle(page: Page): String = page match
    case Page.ProductCatalog => "Products | Product Builder"
    case Page.ProductBuilder => "Product Parameters | Product Builder"
    case Page.VisualEditor(_) => "Visual Editor | Product Builder"
    case Page.Checkout => "Checkout | Product Builder"
    case Page.OrderHistory => "Order History | Product Builder"
    case Page.CustomerPortal => "My Orders | Product Builder"
    case Page.ProductDetail(_) => "Product Details | Product Builder"
    case Page.ManufacturingDashboard => "Manufacturing Dashboard | Product Builder"
    case Page.ManufacturingStationQueue => "Station Queue | Manufacturing"
    case Page.ManufacturingOrderApproval => "Order Approval | Manufacturing"
    case Page.ManufacturingOrderProgress => "Order Progress | Manufacturing"
    case Page.ManufacturingEmployees => "Employees | Manufacturing"
    case Page.ManufacturingMachines => "Machines | Manufacturing"
    case Page.ManufacturingAnalytics => "Analytics | Manufacturing"
    case Page.ManufacturingSettings => "Settings | Manufacturing"
    case Page.CatalogCategories => "Categories | Catalog Editor"
    case Page.CatalogMaterials => "Materials | Catalog Editor"
    case Page.CatalogFinishes => "Finishes | Catalog Editor"
    case Page.CatalogPrintingMethods => "Printing Methods | Catalog Editor"
    case Page.CatalogRules => "Rules | Catalog Editor"
    case Page.CatalogPricelist => "Pricelist | Catalog Editor"
    case Page.CatalogExport => "Export/Import | Catalog Editor"
    case Page.CustomersList => "Customers | Customer Management"
    case Page.CustomerPricing => "Customer Pricing | Customer Management"
    case Page.DiscountCodes => "Discount Codes | Customer Management"

  // Helper navigation methods for common patterns
  def navigateToProductCatalog(): Unit = pushState(Page.ProductCatalog)
  def navigateToProductBuilder(): Unit = pushState(Page.ProductBuilder)
  def navigateToVisualEditor(artworkId: Option[String] = None): Unit = pushState(Page.VisualEditor(artworkId))
  def navigateToCheckout(): Unit = pushState(Page.Checkout)
  def navigateToOrderHistory(): Unit = pushState(Page.OrderHistory)
  def navigateToCustomerPortal(): Unit = pushState(Page.CustomerPortal)
  def navigateToProductDetail(categoryId: CategoryId): Unit = pushState(Page.ProductDetail(categoryId.value))

  // Manufacturing navigation
  def navigateToManufacturingDashboard(): Unit = pushState(Page.ManufacturingDashboard)
  def navigateToManufacturingStationQueue(): Unit = pushState(Page.ManufacturingStationQueue)
  def navigateToManufacturingOrderApproval(): Unit = pushState(Page.ManufacturingOrderApproval)
  def navigateToManufacturingOrderProgress(): Unit = pushState(Page.ManufacturingOrderProgress)
  def navigateToManufacturingEmployees(): Unit = pushState(Page.ManufacturingEmployees)
  def navigateToManufacturingMachines(): Unit = pushState(Page.ManufacturingMachines)
  def navigateToManufacturingAnalytics(): Unit = pushState(Page.ManufacturingAnalytics)
  def navigateToManufacturingSettings(): Unit = pushState(Page.ManufacturingSettings)

  // Catalog editor navigation
  def navigateToCatalogCategories(): Unit = pushState(Page.CatalogCategories)
  def navigateToCatalogMaterials(): Unit = pushState(Page.CatalogMaterials)
  def navigateToCatalogFinishes(): Unit = pushState(Page.CatalogFinishes)
  def navigateToCatalogPrintingMethods(): Unit = pushState(Page.CatalogPrintingMethods)
  def navigateToCatalogRules(): Unit = pushState(Page.CatalogRules)
  def navigateToCatalogPricelist(): Unit = pushState(Page.CatalogPricelist)
  def navigateToCatalogExport(): Unit = pushState(Page.CatalogExport)

  // Customer management navigation
  def navigateToCustomersList(): Unit = pushState(Page.CustomersList)
  def navigateToCustomerPricing(): Unit = pushState(Page.CustomerPricing)
  def navigateToDiscountCodes(): Unit = pushState(Page.DiscountCodes)

  /** Check if the current page is a manufacturing page. */
  def isManufacturingPage(page: Page): Boolean = page.isInstanceOf[Page.ManufacturingPage]

  /** Check if the current page is a catalog page. */
  def isCatalogPage(page: Page): Boolean = page.isInstanceOf[Page.CatalogPage]

  /** Check if the current page is a customer page. */
  def isCustomerPage(page: Page): Boolean = page.isInstanceOf[Page.CustomerPage]
