package mpbuilder.backoffice
package http.api

import BackofficeApiCodecs.given
import mpbuilder.api.ApiSchemas.given
import mpbuilder.api.ApiSupport.*
import mpbuilder.artwork.ArtworkSubmissionView
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*
import sttp.model.StatusCode
import sttp.tapir.*

/** Pure Tapir contracts for the role-based Staff Backoffice BFF (`/api/v1/admin`).
  * Every endpoint except login requires `Authorization: Bearer <staff SessionToken>`; role
  * checks (Operator / Prepress / Manager / Admin) are enforced by `BackofficeFacade`.
  */
object BackofficeEndpoints:

  given Codec[String, OrderId, CodecFormat.TextPlain]          = opaqueCodec(OrderId(_))(_.value)
  given Codec[String, ArtworkId, CodecFormat.TextPlain]        = opaqueCodec(ArtworkId(_))(_.value)
  given Codec[String, CategoryId, CodecFormat.TextPlain]       = opaqueCodec(CategoryId(_))(_.value)
  given Codec[String, MaterialId, CodecFormat.TextPlain]       = opaqueCodec(MaterialId(_))(_.value)
  given Codec[String, FinishId, CodecFormat.TextPlain]         = opaqueCodec(FinishId(_))(_.value)
  given Codec[String, PrintingMethodId, CodecFormat.TextPlain] = opaqueCodec(PrintingMethodId(_))(_.value)
  given Codec[String, RuleId, CodecFormat.TextPlain]           = opaqueCodec(RuleId(_))(_.value)
  given Codec[String, DiscountCodeId, CodecFormat.TextPlain]   = opaqueCodec(DiscountCodeId(_))(_.value)
  given Codec[String, CustomerId, CodecFormat.TextPlain]       = opaqueCodec(CustomerId(_))(_.value)
  given Codec[String, EmployeeId, CodecFormat.TextPlain]       = opaqueCodec(EmployeeId(_))(_.value)
  given Codec[String, DeliveryOptionId, CodecFormat.TextPlain] = opaqueCodec(DeliveryOptionId(_))(_.value)
  given Codec[String, Currency, CodecFormat.TextPlain]         = enumCodec(Currency.values)(using Schema.string[Currency])

  private val publicBase: PublicEndpoint[Unit, BackofficeError, Unit, Any] =
    endpoint.in("api" / apiVersion / "admin").errorOut(typedErrorOut[BackofficeError](_.message, "BackofficeError"))

  /** Authenticated base: the session token is the first input of every endpoint. */
  private val base = publicBase.in(bearer)

  private val orders    = base.in("orders").tag("Backoffice · Order Intake")
  private val artwork   = base.tag("Backoffice · Artwork Review")
  private val catalog   = base.in("catalog").tag("Backoffice · Catalog Manager")
  private val pricing   = base.in("pricing").tag("Backoffice · Pricing Manager")
  private val customers = base.in("customers").tag("Backoffice · Customers")
  private val staff     = base.in("staff").tag("Backoffice · Staff Directory")
  private val settings  = base.in("settings").tag("Backoffice · Store Settings")

  private val orderId   = path[OrderId]("orderId")
  private val artworkId = path[ArtworkId]("artworkId")
  private val noContent = statusCode(StatusCode.NoContent)

  // ── 1. Staff auth & dashboard ──────────────────────────────────────────────

  val login =
    publicBase.post.in("auth" / "login").in(body[StaffLoginRequest]).out(body[StaffAuthResult])
      .tag("Backoffice · Auth").summary("Staff password login")

  val validateSession =
    base.get.in("auth" / "session").out(body[StaffAuthResult]).tag("Backoffice · Auth").summary("Validate the staff session")

  val logout =
    base.post.in("auth" / "logout").out(noContent).tag("Backoffice · Auth").summary("Revoke the staff session")

  val dashboard =
    base.get.in("dashboard").out(body[BackofficeDashboardSummary]).tag("Backoffice · Dashboard").summary("Operations KPIs")

  // ── 2–3. Order intake ──────────────────────────────────────────────────────

  val listOrders =
    orders.post.in("search").in(body[OrderFilter]).out(jsonList[OrderView]).summary("Order intake queue (filtered)")

  val getOrder =
    orders.get.in(orderId).out(body[OrderView]).summary("Order detail")

  val acceptOrder =
    orders.post.in(orderId / "accept").in(body[NoteRequest]).out(body[OrderView]).summary("Accept an order for production")

  val putOnHold =
    orders.post.in(orderId / "hold").in(body[ReasonRequest]).out(body[OrderView]).summary("Put an order on hold")

  val resumeOrder =
    orders.post.in(orderId / "resume").in(body[NoteRequest]).out(body[OrderView]).summary("Resume an order from hold")

  val advanceStage =
    orders.post.in(orderId / "advance").in(body[AdvanceStageRequest]).out(body[OrderView]).summary("Advance the manual stage")

  val confirmPayment =
    orders.post.in(orderId / "payment" / "confirm").in(body[NoteRequest]).out(body[OrderView]).summary("Confirm bank transfer")

  val markPaymentStatus =
    orders.post.in(orderId / "payment" / "status").in(body[PaymentStatusRequest]).out(body[OrderView]).summary("Set payment status")

  val editOrderLine =
    orders.post.in("line-edits").in(body[EditOrderLineCmd]).out(body[OrderView]).summary("Edit an order line")

  val cancelOrder =
    orders.post.in(orderId / "cancel").in(body[ReasonRequest]).out(body[OrderView]).summary("Cancel an order")

  val addInternalNote =
    orders.post.in(orderId / "notes").in(body[TextRequest]).out(body[OrderView]).summary("Add an internal staff note")

  // ── 4. Artwork review ──────────────────────────────────────────────────────

  val listOrderArtwork =
    artwork.get.in("orders" / orderId / "artwork").out(jsonList[ArtworkSubmissionView]).summary("Artwork of an order")

  val approveArtwork =
    artwork.post.in("artwork" / artworkId / "approve").in(body[ApproveArtworkRequest])
      .out(body[ArtworkSubmissionView]).summary("Approve artwork (optionally overriding preflight failures)")

  val requestArtworkChanges =
    artwork.post.in("artwork" / artworkId / "request-changes").in(body[TextRequest])
      .out(body[ArtworkSubmissionView]).summary("Request changes from the customer")

  val rejectArtwork =
    artwork.post.in("artwork" / artworkId / "reject").in(body[ReasonRequest])
      .out(body[ArtworkSubmissionView]).summary("Reject artwork")

  val uploadStaffRevision =
    artwork.post.in("orders" / orderId / "artwork" / artworkId / "revisions").in(body[StaffArtworkUploadPayload])
      .out(body[ArtworkSubmissionView]).summary("Upload a corrected revision on behalf of the customer")

  val uploadStaffOrderLineArtwork =
    artwork.post.in("orders" / orderId / "artwork").in(body[StaffOrderLineArtworkRequest])
      .out(body[ArtworkSubmissionView]).summary("Upload artwork for an order line")

  val rerunPreflight =
    artwork.post.in("orders" / orderId / "artwork" / artworkId / "preflight")
      .out(body[ArtworkSubmissionView]).summary("Re-run automated preflight")

  // ── 5. Catalog management ──────────────────────────────────────────────────

  val getCatalog       = catalog.get.out(body[ProductCatalog]).summary("Catalog (staff view)")
  val getRuleset       = catalog.get.in("ruleset").out(body[CompatibilityRuleset]).summary("Compatibility ruleset")
  val upsertCategory   = catalog.put.in("categories").in(body[ProductCategory]).out(body[ProductCatalog]).summary("Create/update a category")
  val deleteCategory   = catalog.delete.in("categories" / path[CategoryId]("id")).out(body[ProductCatalog]).summary("Delete a category")
  val upsertMaterial   = catalog.put.in("materials").in(body[Material]).out(body[ProductCatalog]).summary("Create/update a material")
  val deleteMaterial   = catalog.delete.in("materials" / path[MaterialId]("id")).out(body[ProductCatalog]).summary("Delete a material")
  val upsertFinish     = catalog.put.in("finishes").in(body[Finish]).out(body[ProductCatalog]).summary("Create/update a finish")
  val deleteFinish     = catalog.delete.in("finishes" / path[FinishId]("id")).out(body[ProductCatalog]).summary("Delete a finish")
  val upsertMethod     = catalog.put.in("printing-methods").in(body[PrintingMethod]).out(body[ProductCatalog]).summary("Create/update a printing method")
  val deleteMethod     = catalog.delete.in("printing-methods" / path[PrintingMethodId]("id")).out(body[ProductCatalog]).summary("Delete a printing method")
  val upsertShowcase   = catalog.put.in("showcases").in(body[ShowcaseListing]).out(body[ProductCatalog]).summary("Create/update a showcase listing")
  val deleteShowcase   = catalog.delete.in("showcases" / path[CategoryId]("categoryId")).out(body[ProductCatalog]).summary("Delete a showcase listing")
  val upsertRule       = catalog.put.in("rules").in(body[CompatibilityRule]).out(body[CompatibilityRuleset]).summary("Create/update a rule")
  val deleteRule       = catalog.delete.in("rules" / path[RuleId]("id")).out(body[CompatibilityRuleset]).summary("Delete a rule")

  // ── 6. Pricing management ──────────────────────────────────────────────────

  val listPricelists     = pricing.get.in("pricelists").out(jsonList[Pricelist]).summary("Pricelists")
  val savePricelist      = pricing.put.in("pricelists").in(body[Pricelist]).out(body[Pricelist]).summary("Save a pricelist")
  val listAgreements     = pricing.get.in("agreements").out(jsonList[CustomerPriceAgreement]).summary("B2B agreements")
  val saveAgreement      = pricing.put.in("agreements").in(body[CustomerPriceAgreement]).out(body[CustomerPriceAgreement]).summary("Save an agreement")
  val deleteAgreement    = pricing.post.in("agreements" / "delete").in(body[DeleteAgreementRequest]).out(noContent).summary("Delete an agreement")
  val listPromoCodes     = pricing.get.in("promo-codes").out(jsonList[PromoCode]).summary("Promo codes")
  val savePromoCode      = pricing.put.in("promo-codes").in(body[PromoCode]).out(body[PromoCode]).summary("Save a promo code")
  val setPromoCodeActive = pricing.post.in("promo-codes" / path[DiscountCodeId]("id") / "active").in(body[ActiveRequest]).out(body[PromoCode]).summary("Activate/deactivate a promo code")
  val deletePromoCode    = pricing.delete.in("promo-codes" / path[DiscountCodeId]("id")).out(noContent).summary("Delete a promo code")
  val listCostSheets     = pricing.get.in("cost-sheets").out(jsonList[ProductionCostSheet]).summary("Production cost sheets")
  val saveCostSheet      = pricing.put.in("cost-sheets").in(body[ProductionCostSheet]).out(body[ProductionCostSheet]).summary("Save a cost sheet")
  val simulateMargin     = pricing.post.in("margin-simulation").in(body[MarginSimulationCmd]).out(body[MarginSimulationResult]).summary("Live margin simulation")

  // ── 7. Customer management ─────────────────────────────────────────────────

  private val customerId = path[CustomerId]("customerId")

  val listCustomers      = customers.post.in("search").in(body[CustomerFilter]).out(jsonList[CustomerView]).summary("Customer directory (filtered)")
  val getCustomer        = customers.get.in(customerId).out(body[CustomerDetailView]).summary("Customer detail with orders & artwork")
  val registerCustomer   = customers.post.in(body[RegisterCustomerCmd]).out(body[CustomerView]).summary("Register a customer")
  val approveCustomer    = customers.post.in(customerId / "approve").out(body[CustomerView]).summary("Approve a pending B2B account")
  val suspendCustomer    = customers.post.in(customerId / "suspend").in(body[ReasonRequest]).out(body[CustomerView]).summary("Suspend an account")
  val deactivateCustomer = customers.post.in(customerId / "deactivate").in(body[ReasonRequest]).out(body[CustomerView]).summary("Deactivate an account")
  val changeTier         = customers.post.in(customerId / "tier").in(body[TierRequest]).out(body[CustomerView]).summary("Change the commercial tier")
  val grantPaymentTerms  = customers.post.in(customerId / "payment-terms").in(body[PaymentTerms]).out(body[CustomerView]).summary("Grant net-invoice credit terms")
  val updateProfile      = customers.put.in("profile").in(body[UpdateCustomerProfileCmd]).out(body[CustomerView]).summary("Update a customer profile")
  val addCustomerNote    = customers.post.in(customerId / "notes").in(body[TextRequest]).out(body[CustomerView]).summary("Add an account note")

  // ── 8. Staff & store settings ──────────────────────────────────────────────

  private val employeeId = path[EmployeeId]("employeeId")

  val listStaff          = staff.get.out(jsonList[StaffMemberView]).summary("Staff directory")
  val createStaff        = staff.post.in(body[CreateStaffCmd]).out(body[StaffMemberView]).summary("Create a staff account")
  val updateStaffRoles   = staff.put.in(employeeId / "roles").in(body[RolesRequest]).out(body[StaffMemberView]).summary("Change staff roles")
  val setStaffActive     = staff.post.in(employeeId / "active").in(body[ActiveRequest]).out(body[StaffMemberView]).summary("Activate/deactivate staff")
  val resetStaffPassword = staff.post.in("password-reset").in(body[PasswordResetRequest]).out(noContent).summary("Reset a staff password")

  val getDeliverySettings  = settings.get.in("delivery").out(body[StoreDeliverySettingsView]).summary("Delivery & bank settings")
  val upsertPickupLocation = settings.put.in("pickup-locations").in(body[ShopPickupLocation]).out(jsonList[ShopPickupLocation]).summary("Save a pickup location")
  val deletePickupLocation = settings.delete.in("pickup-locations" / path[DeliveryOptionId]("id")).out(jsonList[ShopPickupLocation]).summary("Delete a pickup location")
  val upsertCourierOption  = settings.put.in("couriers").in(body[CourierServiceOption]).out(jsonList[CourierServiceOption]).summary("Save a courier option")
  val deleteCourierOption  = settings.delete.in("couriers" / path[Currency]("currency") / path[DeliveryOptionId]("id")).out(jsonList[CourierServiceOption]).summary("Delete a courier option")
  val upsertBankAccount    = settings.put.in("bank-accounts" / path[Currency]("currency")).in(body[BankAccountConfig]).out(jsonNamed[Map[Currency, BankAccountConfig]]("Map_Currency_BankAccountConfig")).summary("Save the bank account for a currency")

  val all: List[AnyEndpoint] = List(
    login, validateSession, logout, dashboard,
    listOrders, getOrder, acceptOrder, putOnHold, resumeOrder, advanceStage, confirmPayment, markPaymentStatus,
    editOrderLine, cancelOrder, addInternalNote,
    listOrderArtwork, approveArtwork, requestArtworkChanges, rejectArtwork, uploadStaffRevision, uploadStaffOrderLineArtwork, rerunPreflight,
    getCatalog, getRuleset, upsertCategory, deleteCategory, upsertMaterial, deleteMaterial, upsertFinish, deleteFinish,
    upsertMethod, deleteMethod, upsertShowcase, deleteShowcase, upsertRule, deleteRule,
    listPricelists, savePricelist, listAgreements, saveAgreement, deleteAgreement, listPromoCodes, savePromoCode,
    setPromoCodeActive, deletePromoCode, listCostSheets, saveCostSheet, simulateMargin,
    listCustomers, getCustomer, registerCustomer, approveCustomer, suspendCustomer, deactivateCustomer, changeTier,
    grantPaymentTerms, updateProfile, addCustomerNote,
    listStaff, createStaff, updateStaffRoles, setStaffActive, resetStaffPassword,
    getDeliverySettings, upsertPickupLocation, deletePickupLocation, upsertCourierOption, deleteCourierOption, upsertBankAccount,
  )
