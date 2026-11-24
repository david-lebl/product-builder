package mpbuilder.backoffice
package http

import api.*
import api.BackofficeApi.Tags
import api.BackofficeApiCodecs.given
import kyo.*
import mpbuilder.api.ApiConventions
import mpbuilder.api.server.ApiServer.*
import mpbuilder.artwork.ArtworkSubmissionView
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*

/** Inbound HTTP adapter: kyo-http routes for `/api/v1/admin`, bound to `BackofficeFacade`.
  * Every route except login requires `Authorization: Bearer <staff SessionToken>`; role checks
  * (Operator / Prepress / Manager / Admin) are enforced by the facade.
  */
final class BackofficeRoutes private[backoffice] (f: BackofficeFacade):
  import BackofficeRoutes.{given, *}

  private def run[A](v: => A < (Async & Abort[BackofficeError]))(using Frame): A < (Async & Abort[Failure]) =
    Abort.runPartial[BackofficeError](v).map {
      case Result.Success(a) => a
      case Result.Failure(e) => Abort.fail(Failure(e))
    }

  private def ok[A: Schema](v: => A < (Async & Abort[BackofficeError]))(using Frame) =
    run(v).map(HttpResponse.ok(_))

  /** Resolves the bearer token (401 `Unauthenticated` when absent) before running `f`. */
  private def authed[A, S](header: Maybe[String])(f: SessionToken => A < (S & Abort[Failure]))(using Frame): A < (S & Abort[Failure]) =
    bearerToken(header) match
      case Some(token) => f(token)
      case None        => Abort.fail(Failure(BackofficeError.Unauthenticated()))

  val handlers: Seq[HttpHandler[?, ?, ?]] =
    authRoutes ++ dashboardRoutes ++ ordersRoutes ++ artworkRoutes ++ catalogRoutes ++ pricingRoutes ++ customersRoutes ++ staffRoutes ++ settingsRoutes

  private def authRoutes: Seq[HttpHandler[?, ?, ?]] = Seq(
    HttpRoute.postRaw(base / "auth" / "login")
      .request(_.bodyJson[StaffLoginRequest])
      .response(_.bodyJson[StaffAuthResult]).apiErrors
      .doc(Tags.Auth, "Staff password login")
      .handler(req => ok(f.loginStaff(req.fields.body.email, req.fields.body.password))),
    HttpRoute.getRaw(base / "auth" / "session")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[StaffAuthResult]).apiErrors
      .doc(Tags.Auth, "Validate the staff session")
      .handler(req => authed(req.fields.authorization)(t => ok(f.validateStaffSession(t)))),
    HttpRoute.postRaw(base / "auth" / "logout")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.status(HttpStatus.NoContent)).apiErrors
      .doc(Tags.Auth, "Revoke the staff session")
      .handler(req => authed(req.fields.authorization)(t => run(f.logoutStaff(t)).andThen(HttpResponse.noContent))),
  )

  private def dashboardRoutes: Seq[HttpHandler[?, ?, ?]] = Seq(
    HttpRoute.getRaw(base / "dashboard")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[BackofficeDashboardSummary]).apiErrors
      .doc(Tags.Dashboard, "Operations KPIs")
      .handler(req => authed(req.fields.authorization)(t => ok(f.getDashboardSummary(t)))),
  )

  private def ordersRoutes: Seq[HttpHandler[?, ?, ?]] = Seq(
    HttpRoute.postRaw(base / "orders" / "search")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[OrderFilter])
      .response(_.bodyJson[List[OrderView]]).apiErrors
      .doc(Tags.Orders, "Order intake queue (filtered)")
      .handler(req => authed(req.fields.authorization)(t => ok(f.listOrders(t, req.fields.body)))),
    HttpRoute.getRaw(base / "orders" / Capture[OrderId]("orderId"))
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[OrderView]).apiErrors
      .doc(Tags.Orders, "Order detail")
      .handler(req => authed(req.fields.authorization)(t => ok(f.getOrder(t, req.fields.orderId)))),
    HttpRoute.postRaw(base / "orders" / Capture[OrderId]("orderId") / "accept")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[NoteRequest])
      .response(_.bodyJson[OrderView]).apiErrors
      .doc(Tags.Orders, "Accept an order for production")
      .handler(req => authed(req.fields.authorization)(t => ok(f.acceptOrder(t, req.fields.orderId, req.fields.body.note)))),
    HttpRoute.postRaw(base / "orders" / Capture[OrderId]("orderId") / "hold")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ReasonRequest])
      .response(_.bodyJson[OrderView]).apiErrors
      .doc(Tags.Orders, "Put an order on hold")
      .handler(req => authed(req.fields.authorization)(t => ok(f.putOrderOnHold(t, req.fields.orderId, req.fields.body.reason)))),
    HttpRoute.postRaw(base / "orders" / Capture[OrderId]("orderId") / "resume")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[NoteRequest])
      .response(_.bodyJson[OrderView]).apiErrors
      .doc(Tags.Orders, "Resume an order from hold")
      .handler(req => authed(req.fields.authorization)(t => ok(f.resumeOrderFromHold(t, req.fields.orderId, req.fields.body.note)))),
    HttpRoute.postRaw(base / "orders" / Capture[OrderId]("orderId") / "advance")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[AdvanceStageRequest])
      .response(_.bodyJson[OrderView]).apiErrors
      .doc(Tags.Orders, "Advance the manual stage")
      .handler(req => authed(req.fields.authorization)(t => ok(f.advanceOrderStage(t, req.fields.orderId, req.fields.body.target, req.fields.body.note)))),
    HttpRoute.postRaw(base / "orders" / Capture[OrderId]("orderId") / "payment" / "confirm")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[NoteRequest])
      .response(_.bodyJson[OrderView]).apiErrors
      .doc(Tags.Orders, "Confirm bank transfer")
      .handler(req => authed(req.fields.authorization)(t => ok(f.confirmOrderPayment(t, req.fields.orderId, req.fields.body.note)))),
    HttpRoute.postRaw(base / "orders" / Capture[OrderId]("orderId") / "payment" / "status")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[PaymentStatusRequest])
      .response(_.bodyJson[OrderView]).apiErrors
      .doc(Tags.Orders, "Set payment status")
      .handler(req => authed(req.fields.authorization)(t => ok(f.markOrderPaymentStatus(t, req.fields.orderId, req.fields.body.status, req.fields.body.note)))),
    HttpRoute.postRaw(base / "orders" / "line-edits")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[EditOrderLineCmd])
      .response(_.bodyJson[OrderView]).apiErrors
      .doc(Tags.Orders, "Edit an order line")
      .handler(req => authed(req.fields.authorization)(t => ok(f.editOrderLine(t, req.fields.body)))),
    HttpRoute.postRaw(base / "orders" / Capture[OrderId]("orderId") / "cancel")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ReasonRequest])
      .response(_.bodyJson[OrderView]).apiErrors
      .doc(Tags.Orders, "Cancel an order")
      .handler(req => authed(req.fields.authorization)(t => ok(f.cancelOrder(t, req.fields.orderId, req.fields.body.reason)))),
    HttpRoute.postRaw(base / "orders" / Capture[OrderId]("orderId") / "notes")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[TextRequest])
      .response(_.bodyJson[OrderView]).apiErrors
      .doc(Tags.Orders, "Add an internal staff note")
      .handler(req => authed(req.fields.authorization)(t => ok(f.addOrderInternalNote(t, req.fields.orderId, req.fields.body.text)))),
  )

  private def artworkRoutes: Seq[HttpHandler[?, ?, ?]] = Seq(
    HttpRoute.getRaw(base / "orders" / Capture[OrderId]("orderId") / "artwork")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[List[ArtworkSubmissionView]]).apiErrors
      .doc(Tags.Artwork, "Artwork of an order")
      .handler(req => authed(req.fields.authorization)(t => ok(f.listOrderArtwork(t, req.fields.orderId)))),
    HttpRoute.postRaw(base / "artwork" / Capture[ArtworkId]("artworkId") / "approve")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ApproveArtworkRequest])
      .response(_.bodyJson[ArtworkSubmissionView]).apiErrors
      .doc(Tags.Artwork, "Approve artwork (optionally overriding preflight failures)")
      .handler(req => authed(req.fields.authorization)(t => ok(f.approveArtwork(t, req.fields.artworkId, req.fields.body.overrideFailures)))),
    HttpRoute.postRaw(base / "artwork" / Capture[ArtworkId]("artworkId") / "request-changes")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[TextRequest])
      .response(_.bodyJson[ArtworkSubmissionView]).apiErrors
      .doc(Tags.Artwork, "Request changes from the customer")
      .handler(req => authed(req.fields.authorization)(t => ok(f.requestArtworkChanges(t, req.fields.artworkId, req.fields.body.text)))),
    HttpRoute.postRaw(base / "artwork" / Capture[ArtworkId]("artworkId") / "reject")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ReasonRequest])
      .response(_.bodyJson[ArtworkSubmissionView]).apiErrors
      .doc(Tags.Artwork, "Reject artwork")
      .handler(req => authed(req.fields.authorization)(t => ok(f.rejectArtwork(t, req.fields.artworkId, req.fields.body.reason)))),
    HttpRoute.postRaw(base / "orders" / Capture[OrderId]("orderId") / "artwork" / Capture[ArtworkId]("artworkId") / "revisions")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[StaffArtworkUploadPayload])
      .response(_.bodyJson[ArtworkSubmissionView]).apiErrors
      .doc(Tags.Artwork, "Upload a corrected revision on behalf of the customer")
      .handler(req => authed(req.fields.authorization)(t => ok(f.uploadStaffArtworkRevision(t, req.fields.orderId, req.fields.artworkId, req.fields.body)))),
    HttpRoute.postRaw(base / "orders" / Capture[OrderId]("orderId") / "artwork")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[StaffOrderLineArtworkRequest])
      .response(_.bodyJson[ArtworkSubmissionView]).apiErrors
      .doc(Tags.Artwork, "Upload artwork for an order line")
      .handler(req => authed(req.fields.authorization)(t => ok(f.uploadStaffOrderLineArtwork(t, req.fields.orderId, req.fields.body.lineIndex, req.fields.body.componentKey, req.fields.body.payload)))),
    HttpRoute.postRaw(base / "orders" / Capture[OrderId]("orderId") / "artwork" / Capture[ArtworkId]("artworkId") / "preflight")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[ArtworkSubmissionView]).apiErrors
      .doc(Tags.Artwork, "Re-run automated preflight")
      .handler(req => authed(req.fields.authorization)(t => ok(f.rerunArtworkPreflight(t, req.fields.orderId, req.fields.artworkId)))),
  )

  private def catalogRoutes: Seq[HttpHandler[?, ?, ?]] = Seq(
    HttpRoute.getRaw(base / "catalog")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[ProductCatalog]).apiErrors
      .doc(Tags.Catalog, "Catalog (staff view)")
      .handler(req => authed(req.fields.authorization)(t => ok(f.getCatalog(t)))),
    HttpRoute.getRaw(base / "catalog" / "ruleset")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[CompatibilityRuleset]).apiErrors
      .doc(Tags.Catalog, "Compatibility ruleset")
      .handler(req => authed(req.fields.authorization)(t => ok(f.getRuleset(t)))),
    HttpRoute.putRaw(base / "catalog" / "categories")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ProductCategory])
      .response(_.bodyJson[ProductCatalog]).apiErrors
      .doc(Tags.Catalog, "Create/update a category")
      .handler(req => authed(req.fields.authorization)(t => ok(f.upsertCategory(t, req.fields.body)))),
    HttpRoute.deleteRaw(base / "catalog" / "categories" / Capture[CategoryId]("id"))
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[ProductCatalog]).apiErrors
      .doc(Tags.Catalog, "Delete a category")
      .handler(req => authed(req.fields.authorization)(t => ok(f.deleteCategory(t, req.fields.id)))),
    HttpRoute.putRaw(base / "catalog" / "materials")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[Material])
      .response(_.bodyJson[ProductCatalog]).apiErrors
      .doc(Tags.Catalog, "Create/update a material")
      .handler(req => authed(req.fields.authorization)(t => ok(f.upsertMaterial(t, req.fields.body)))),
    HttpRoute.deleteRaw(base / "catalog" / "materials" / Capture[MaterialId]("id"))
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[ProductCatalog]).apiErrors
      .doc(Tags.Catalog, "Delete a material")
      .handler(req => authed(req.fields.authorization)(t => ok(f.deleteMaterial(t, req.fields.id)))),
    HttpRoute.putRaw(base / "catalog" / "finishes")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[Finish])
      .response(_.bodyJson[ProductCatalog]).apiErrors
      .doc(Tags.Catalog, "Create/update a finish")
      .handler(req => authed(req.fields.authorization)(t => ok(f.upsertFinish(t, req.fields.body)))),
    HttpRoute.deleteRaw(base / "catalog" / "finishes" / Capture[FinishId]("id"))
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[ProductCatalog]).apiErrors
      .doc(Tags.Catalog, "Delete a finish")
      .handler(req => authed(req.fields.authorization)(t => ok(f.deleteFinish(t, req.fields.id)))),
    HttpRoute.putRaw(base / "catalog" / "printing-methods")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[PrintingMethod])
      .response(_.bodyJson[ProductCatalog]).apiErrors
      .doc(Tags.Catalog, "Create/update a printing method")
      .handler(req => authed(req.fields.authorization)(t => ok(f.upsertPrintingMethod(t, req.fields.body)))),
    HttpRoute.deleteRaw(base / "catalog" / "printing-methods" / Capture[PrintingMethodId]("id"))
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[ProductCatalog]).apiErrors
      .doc(Tags.Catalog, "Delete a printing method")
      .handler(req => authed(req.fields.authorization)(t => ok(f.deletePrintingMethod(t, req.fields.id)))),
    HttpRoute.putRaw(base / "catalog" / "showcases")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ShowcaseListing])
      .response(_.bodyJson[ProductCatalog]).apiErrors
      .doc(Tags.Catalog, "Create/update a showcase listing")
      .handler(req => authed(req.fields.authorization)(t => ok(f.upsertShowcaseListing(t, req.fields.body)))),
    HttpRoute.deleteRaw(base / "catalog" / "showcases" / Capture[CategoryId]("categoryId"))
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[ProductCatalog]).apiErrors
      .doc(Tags.Catalog, "Delete a showcase listing")
      .handler(req => authed(req.fields.authorization)(t => ok(f.deleteShowcaseListing(t, req.fields.categoryId)))),
    HttpRoute.putRaw(base / "catalog" / "rules")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[CompatibilityRule])
      .response(_.bodyJson[CompatibilityRuleset]).apiErrors
      .doc(Tags.Catalog, "Create/update a rule")
      .handler(req => authed(req.fields.authorization)(t => ok(f.upsertRule(t, req.fields.body)))),
    HttpRoute.deleteRaw(base / "catalog" / "rules" / Capture[RuleId]("id"))
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[CompatibilityRuleset]).apiErrors
      .doc(Tags.Catalog, "Delete a rule")
      .handler(req => authed(req.fields.authorization)(t => ok(f.deleteRule(t, req.fields.id)))),
  )

  private def pricingRoutes: Seq[HttpHandler[?, ?, ?]] = Seq(
    HttpRoute.getRaw(base / "pricing" / "pricelists")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[List[Pricelist]]).apiErrors
      .doc(Tags.Pricing, "Pricelists")
      .handler(req => authed(req.fields.authorization)(t => ok(f.listPricelists(t)))),
    HttpRoute.putRaw(base / "pricing" / "pricelists")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[Pricelist])
      .response(_.bodyJson[Pricelist]).apiErrors
      .doc(Tags.Pricing, "Save a pricelist")
      .handler(req => authed(req.fields.authorization)(t => ok(f.savePricelist(t, req.fields.body)))),
    HttpRoute.getRaw(base / "pricing" / "agreements")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[List[CustomerPriceAgreement]]).apiErrors
      .doc(Tags.Pricing, "B2B agreements")
      .handler(req => authed(req.fields.authorization)(t => ok(f.listAgreements(t)))),
    HttpRoute.putRaw(base / "pricing" / "agreements")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[CustomerPriceAgreement])
      .response(_.bodyJson[CustomerPriceAgreement]).apiErrors
      .doc(Tags.Pricing, "Save an agreement")
      .handler(req => authed(req.fields.authorization)(t => ok(f.saveAgreement(t, req.fields.body)))),
    HttpRoute.postRaw(base / "pricing" / "agreements" / "delete")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[DeleteAgreementRequest])
      .response(_.status(HttpStatus.NoContent)).apiErrors
      .doc(Tags.Pricing, "Delete an agreement")
      .handler(req => authed(req.fields.authorization)(t => run(f.deleteAgreement(t, req.fields.body.target, req.fields.body.currency)).andThen(HttpResponse.noContent))),
    HttpRoute.getRaw(base / "pricing" / "promo-codes")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[List[PromoCode]]).apiErrors
      .doc(Tags.Pricing, "Promo codes")
      .handler(req => authed(req.fields.authorization)(t => ok(f.listPromoCodes(t)))),
    HttpRoute.putRaw(base / "pricing" / "promo-codes")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[PromoCode])
      .response(_.bodyJson[PromoCode]).apiErrors
      .doc(Tags.Pricing, "Save a promo code")
      .handler(req => authed(req.fields.authorization)(t => ok(f.savePromoCode(t, req.fields.body)))),
    HttpRoute.postRaw(base / "pricing" / "promo-codes" / Capture[DiscountCodeId]("id") / "active")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ActiveRequest])
      .response(_.bodyJson[PromoCode]).apiErrors
      .doc(Tags.Pricing, "Activate/deactivate a promo code")
      .handler(req => authed(req.fields.authorization)(t => ok(f.setPromoCodeActive(t, req.fields.id, req.fields.body.active)))),
    HttpRoute.deleteRaw(base / "pricing" / "promo-codes" / Capture[DiscountCodeId]("id"))
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.status(HttpStatus.NoContent)).apiErrors
      .doc(Tags.Pricing, "Delete a promo code")
      .handler(req => authed(req.fields.authorization)(t => run(f.deletePromoCode(t, req.fields.id)).andThen(HttpResponse.noContent))),
    HttpRoute.getRaw(base / "pricing" / "cost-sheets")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[List[ProductionCostSheet]]).apiErrors
      .doc(Tags.Pricing, "Production cost sheets")
      .handler(req => authed(req.fields.authorization)(t => ok(f.listCostSheets(t)))),
    HttpRoute.putRaw(base / "pricing" / "cost-sheets")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ProductionCostSheet])
      .response(_.bodyJson[ProductionCostSheet]).apiErrors
      .doc(Tags.Pricing, "Save a cost sheet")
      .handler(req => authed(req.fields.authorization)(t => ok(f.saveCostSheet(t, req.fields.body)))),
    HttpRoute.postRaw(base / "pricing" / "margin-simulation")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[MarginSimulationCmd])
      .response(_.bodyJson[MarginSimulationResult]).apiErrors
      .doc(Tags.Pricing, "Live margin simulation")
      .handler(req => authed(req.fields.authorization)(t => ok(f.simulateMargin(t, req.fields.body)))),
  )

  private def customersRoutes: Seq[HttpHandler[?, ?, ?]] = Seq(
    HttpRoute.postRaw(base / "customers" / "search")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[CustomerFilter])
      .response(_.bodyJson[List[CustomerView]]).apiErrors
      .doc(Tags.Customers, "Customer directory (filtered)")
      .handler(req => authed(req.fields.authorization)(t => ok(f.listCustomers(t, req.fields.body)))),
    HttpRoute.getRaw(base / "customers" / Capture[CustomerId]("customerId"))
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[CustomerDetailView]).apiErrors
      .doc(Tags.Customers, "Customer detail with orders & artwork")
      .handler(req => authed(req.fields.authorization)(t => ok(f.getCustomerDetail(t, req.fields.customerId)))),
    HttpRoute.postRaw(base / "customers")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[RegisterCustomerCmd])
      .response(_.bodyJson[CustomerView]).apiErrors
      .doc(Tags.Customers, "Register a customer")
      .handler(req => authed(req.fields.authorization)(t => ok(f.registerCustomer(t, req.fields.body)))),
    HttpRoute.postRaw(base / "customers" / Capture[CustomerId]("customerId") / "approve")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[CustomerView]).apiErrors
      .doc(Tags.Customers, "Approve a pending B2B account")
      .handler(req => authed(req.fields.authorization)(t => ok(f.approveCustomer(t, req.fields.customerId)))),
    HttpRoute.postRaw(base / "customers" / Capture[CustomerId]("customerId") / "suspend")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ReasonRequest])
      .response(_.bodyJson[CustomerView]).apiErrors
      .doc(Tags.Customers, "Suspend an account")
      .handler(req => authed(req.fields.authorization)(t => ok(f.suspendCustomer(t, req.fields.customerId, req.fields.body.reason)))),
    HttpRoute.postRaw(base / "customers" / Capture[CustomerId]("customerId") / "deactivate")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ReasonRequest])
      .response(_.bodyJson[CustomerView]).apiErrors
      .doc(Tags.Customers, "Deactivate an account")
      .handler(req => authed(req.fields.authorization)(t => ok(f.deactivateCustomer(t, req.fields.customerId, req.fields.body.reason)))),
    HttpRoute.postRaw(base / "customers" / Capture[CustomerId]("customerId") / "tier")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[TierRequest])
      .response(_.bodyJson[CustomerView]).apiErrors
      .doc(Tags.Customers, "Change the commercial tier")
      .handler(req => authed(req.fields.authorization)(t => ok(f.changeCustomerTier(t, req.fields.customerId, req.fields.body.tier)))),
    HttpRoute.postRaw(base / "customers" / Capture[CustomerId]("customerId") / "payment-terms")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[PaymentTerms])
      .response(_.bodyJson[CustomerView]).apiErrors
      .doc(Tags.Customers, "Grant net-invoice credit terms")
      .handler(req => authed(req.fields.authorization)(t => ok(f.grantCustomerPaymentTerms(t, req.fields.customerId, req.fields.body)))),
    HttpRoute.putRaw(base / "customers" / "profile")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[UpdateCustomerProfileCmd])
      .response(_.bodyJson[CustomerView]).apiErrors
      .doc(Tags.Customers, "Update a customer profile")
      .handler(req => authed(req.fields.authorization)(t => ok(f.updateCustomerProfile(t, req.fields.body)))),
    HttpRoute.postRaw(base / "customers" / Capture[CustomerId]("customerId") / "notes")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[TextRequest])
      .response(_.bodyJson[CustomerView]).apiErrors
      .doc(Tags.Customers, "Add an account note")
      .handler(req => authed(req.fields.authorization)(t => ok(f.addCustomerNote(t, req.fields.customerId, req.fields.body.text)))),
  )

  private def staffRoutes: Seq[HttpHandler[?, ?, ?]] = Seq(
    HttpRoute.getRaw(base / "staff")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[List[StaffMemberView]]).apiErrors
      .doc(Tags.Staff, "Staff directory")
      .handler(req => authed(req.fields.authorization)(t => ok(f.listStaffMembers(t)))),
    HttpRoute.postRaw(base / "staff")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[CreateStaffCmd])
      .response(_.bodyJson[StaffMemberView]).apiErrors
      .doc(Tags.Staff, "Create a staff account")
      .handler(req => authed(req.fields.authorization)(t => ok(f.createStaffMember(t, req.fields.body)))),
    HttpRoute.putRaw(base / "staff" / Capture[EmployeeId]("employeeId") / "roles")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[RolesRequest])
      .response(_.bodyJson[StaffMemberView]).apiErrors
      .doc(Tags.Staff, "Change staff roles")
      .handler(req => authed(req.fields.authorization)(t => ok(f.updateStaffRoles(t, req.fields.employeeId, req.fields.body.roles)))),
    HttpRoute.postRaw(base / "staff" / Capture[EmployeeId]("employeeId") / "active")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ActiveRequest])
      .response(_.bodyJson[StaffMemberView]).apiErrors
      .doc(Tags.Staff, "Activate/deactivate staff")
      .handler(req => authed(req.fields.authorization)(t => ok(f.setStaffActive(t, req.fields.employeeId, req.fields.body.active)))),
    HttpRoute.postRaw(base / "staff" / "password-reset")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[PasswordResetRequest])
      .response(_.status(HttpStatus.NoContent)).apiErrors
      .doc(Tags.Staff, "Reset a staff password")
      .handler(req => authed(req.fields.authorization)(t => run(f.resetStaffPassword(t, req.fields.body.email, req.fields.body.newPassword)).andThen(HttpResponse.noContent))),
  )

  private def settingsRoutes: Seq[HttpHandler[?, ?, ?]] = Seq(
    HttpRoute.getRaw(base / "settings" / "delivery")
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[StoreDeliverySettingsView]).apiErrors
      .doc(Tags.Settings, "Delivery & bank settings")
      .handler(req => authed(req.fields.authorization)(t => ok(f.getStoreDeliverySettings(t)))),
    HttpRoute.putRaw(base / "settings" / "pickup-locations")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[ShopPickupLocation])
      .response(_.bodyJson[List[ShopPickupLocation]]).apiErrors
      .doc(Tags.Settings, "Save a pickup location")
      .handler(req => authed(req.fields.authorization)(t => ok(f.upsertPickupLocation(t, req.fields.body)))),
    HttpRoute.deleteRaw(base / "settings" / "pickup-locations" / Capture[DeliveryOptionId]("id"))
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[List[ShopPickupLocation]]).apiErrors
      .doc(Tags.Settings, "Delete a pickup location")
      .handler(req => authed(req.fields.authorization)(t => ok(f.deletePickupLocation(t, req.fields.id)))),
    HttpRoute.putRaw(base / "settings" / "couriers")
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[CourierServiceOption])
      .response(_.bodyJson[List[CourierServiceOption]]).apiErrors
      .doc(Tags.Settings, "Save a courier option")
      .handler(req => authed(req.fields.authorization)(t => ok(f.upsertCourierOption(t, req.fields.body)))),
    HttpRoute.deleteRaw(base / "settings" / "couriers" / Capture[Currency]("currency") / Capture[DeliveryOptionId]("id"))
      .request(_.headerOpt[String]("authorization", "Authorization"))
      .response(_.bodyJson[List[CourierServiceOption]]).apiErrors
      .doc(Tags.Settings, "Delete a courier option")
      .handler(req => authed(req.fields.authorization)(t => ok(f.deleteCourierOption(t, req.fields.currency, req.fields.id)))),
    HttpRoute.putRaw(base / "settings" / "bank-accounts" / Capture[Currency]("currency"))
      .request(_.headerOpt[String]("authorization", "Authorization").bodyJson[BankAccountConfig])
      .response(_.bodyJson[Map[Currency, BankAccountConfig]]).apiErrors
      .doc(Tags.Settings, "Save the bank account for a currency")
      .handler(req => authed(req.fields.authorization)(t => ok(f.upsertBankAccount(t, req.fields.currency, req.fields.body)))),
  )

object BackofficeRoutes:
  private[http] val base: HttpPath[Any] = api / "admin"

  private given HttpCodec[OrderId]          = opaqueCodec(OrderId(_), _.value)
  private given HttpCodec[ArtworkId]        = opaqueCodec(ArtworkId(_), _.value)
  private given HttpCodec[CategoryId]       = opaqueCodec(CategoryId(_), _.value)
  private given HttpCodec[MaterialId]       = opaqueCodec(MaterialId(_), _.value)
  private given HttpCodec[FinishId]         = opaqueCodec(FinishId(_), _.value)
  private given HttpCodec[PrintingMethodId] = opaqueCodec(PrintingMethodId(_), _.value)
  private given HttpCodec[RuleId]           = opaqueCodec(RuleId(_), _.value)
  private given HttpCodec[DiscountCodeId]   = opaqueCodec(DiscountCodeId(_), _.value)
  private given HttpCodec[CustomerId]       = opaqueCodec(CustomerId(_), _.value)
  private given HttpCodec[EmployeeId]       = opaqueCodec(EmployeeId(_), _.value)
  private given HttpCodec[DeliveryOptionId] = opaqueCodec(DeliveryOptionId(_), _.value)
  private given HttpCodec[Currency]         = enumCodec(Currency.values)

  /** Error envelopes — one concrete type per HTTP status (kyo-http maps error types to statuses). */
  sealed trait Failure
  final case class BadRequest(message: String, error: BackofficeError)   extends Failure derives Schema
  final case class Unauthorized(message: String, error: BackofficeError) extends Failure derives Schema
  final case class Forbidden(message: String, error: BackofficeError)    extends Failure derives Schema
  final case class NotFound(message: String, error: BackofficeError)     extends Failure derives Schema
  final case class Conflict(message: String, error: BackofficeError)     extends Failure derives Schema

  object Failure:
    def apply(e: BackofficeError): Failure =
      ApiConventions.statusOf(e) match
        case 401 => Unauthorized(e.message, e)
        case 403 => Forbidden(e.message, e)
        case 404 => NotFound(e.message, e)
        case 409 => Conflict(e.message, e)
        case _   => BadRequest(e.message, e)

  extension [In, Out, E](route: HttpRoute[In, Out, E])
    private inline def apiErrors: HttpRoute[In, Out, E | Failure] =
      route
        .error[BadRequest](HttpStatus.BadRequest)
        .error[Unauthorized](HttpStatus.Unauthorized)
        .error[Forbidden](HttpStatus.Forbidden)
        .error[NotFound](HttpStatus.NotFound)
        .error[Conflict](HttpStatus.Conflict)

  /** Public constructor for composition roots: routes over any `BackofficeFacade`. */
  def apply(facade: BackofficeFacade): BackofficeRoutes = new BackofficeRoutes(facade)
