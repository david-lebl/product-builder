package mpbuilder.admin.state

import kyo.*
import mpbuilder.admin.port.BackofficeClient
import mpbuilder.artwork.*
import mpbuilder.backoffice.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*
import mpbuilder.ui.components.{ArtworkComponents, BrowserPickedFile}
import mpbuilder.ui.i18n.AppI18n

enum AdminRoute:
  case Dashboard
  case Orders
  case OrderDetail(orderId: OrderId)
  case Catalog
  case Pricing
  case Customers
  case StaffAndSettings

object AdminRoute:
  def toHash(route: AdminRoute, prefix: String = "#/admin"): String =
    val base = prefix.stripSuffix("/")
    route match
      case Dashboard             => s"$base/dashboard"
      case Orders                => s"$base/orders"
      case OrderDetail(orderId)  => s"$base/orders/${orderId.value}"
      case Catalog               => s"$base/catalog"
      case Pricing               => s"$base/pricing"
      case Customers             => s"$base/customers"
      case StaffAndSettings      => s"$base/settings"

  /** Parses both prefixed (`#/admin/orders/ord-1`) and standalone (`#/orders/ord-1`) hashes. */
  def fromHash(hash: String, requireAdminPrefix: Boolean = false): Option[AdminRoute] =
    val clean    = hash.stripPrefix("#").stripPrefix("/")
    val segments = clean.split("/").toList.filter(_.nonEmpty)
    val rest: Option[List[String]] =
      if requireAdminPrefix then
        segments match
          case "admin" :: tail => Some(tail)
          case _               => None
      else
        segments match
          case "admin" :: tail => Some(tail)
          case other           => Some(other)

    rest.flatMap {
      case Nil | List("dashboard")     => Some(Dashboard)
      case List("orders")              => Some(Orders)
      case List("orders", orderId)     => Some(OrderDetail(OrderId.unsafe(orderId)))
      case List("catalog")             => Some(Catalog)
      case List("pricing")             => Some(Pricing)
      case List("customers")           => Some(Customers)
      case List("settings")            => Some(StaffAndSettings)
      case _                           => None
    }

/** Reactive Backoffice coordinator holding staff auth, routing, order intake, catalog, pricing, customers, staff & store settings. */
final class BackofficeState(
    val client: BackofficeClient,
    val i18n: AppI18n,
    val route: SignalRef[AdminRoute],
    val auth: SignalRef[Option[StaffAuthResult]],
    val dashboard: SignalRef[Option[BackofficeDashboardSummary]],
    val orders: SignalRef[List[OrderView]],
    val orderStatusFilter: SignalRef[Option[OrderStatusKind]],
    val orderSearch: SignalRef[String],
    val selectedOrder: SignalRef[Option[OrderView]],
    val selectedOrderArtwork: SignalRef[List[ArtworkSubmissionView]],
    val artworkPreview: ArtworkComponents.PreviewModal.Handle,
    val catalog: SignalRef[Option[ProductCatalog]],
    val ruleset: SignalRef[Option[CompatibilityRuleset]],
    val pricelists: SignalRef[List[Pricelist]],
    val agreements: SignalRef[List[CustomerPriceAgreement]],
    val promoCodes: SignalRef[List[PromoCode]],
    val costSheets: SignalRef[List[ProductionCostSheet]],
    val customers: SignalRef[List[CustomerView]],
    val customerStatusFilter: SignalRef[Option[CustomerStatus]],
    val customerSearch: SignalRef[String],
    val selectedCustomer: SignalRef[Option[CustomerDetailView]],
    val staffMembers: SignalRef[List[StaffMemberView]],
    val deliverySettings: SignalRef[Option[StoreDeliverySettingsView]],
    val pricingFocusTab: SignalRef[String],
    val pricingFocusCustomerId: SignalRef[Option[CustomerId]],
    val mobileNavOpen: SignalRef[Boolean],
    val toast: SignalRef[Option[String]],
    val errorBanner: SignalRef[Option[String]],
    val busy: SignalRef[Boolean],
    val onRouteChange: AdminRoute => Unit < Sync,
    val onLanguagePersist: Language => Unit < Sync,
    val onDataMutated: () => Unit < Async,
):

  // ── Role Helpers ───────────────────────────────────────────────────────────

  def currentRoles(authOpt: Option[StaffAuthResult]): Set[EmployeeRole] =
    authOpt.map(_.employee.roles).getOrElse(Set.empty)

  def canIntake(authOpt: Option[StaffAuthResult]): Boolean =
    val r = currentRoles(authOpt)
    r.contains(EmployeeRole.Operator) || r.contains(EmployeeRole.Manager) || r.contains(EmployeeRole.Admin)

  def canManage(authOpt: Option[StaffAuthResult]): Boolean =
    val r = currentRoles(authOpt)
    r.contains(EmployeeRole.Manager) || r.contains(EmployeeRole.Admin)

  def isAdmin(authOpt: Option[StaffAuthResult]): Boolean =
    currentRoles(authOpt).contains(EmployeeRole.Admin)

  // ── Internal Auth-Scoped Call Helper ───────────────────────────────────────

  private def withToken[A](f: SessionToken => A < (Abort[BackofficeError] & Async)): Option[A] < Async =
    auth.get.flatMap {
      case None => None
      case Some(staffAuth) =>
        Abort.run[BackofficeError](f(staffAuth.session.token)).flatMap {
          case Result.Success(v) => Some(v)
          case Result.Failure(err) =>
            errorBanner.set(Some(err.message)).andThen(None)
          case Result.Panic(ex) =>
            errorBanner.set(Some(Option(ex.getMessage).getOrElse("Unexpected backoffice error"))).andThen(None)
        }
    }

  def showToast(msg: String): Unit < Sync = toast.set(Some(msg))
  def dismissToast: Unit < Sync           = toast.set(None)
  def dismissError: Unit < Sync           = errorBanner.set(None)

  // ── Language ───────────────────────────────────────────────────────────────

  def setLanguage(lang: Language): Unit < Async =
    for
      _ <- i18n.setLanguage(lang)
      _ <- onLanguagePersist(lang)
    yield ()

  // ── Authentication ─────────────────────────────────────────────────────────

  def login(emailRaw: String, passwordRaw: String): Either[String, StaffAuthResult] < Async =
    for
      _ <- errorBanner.set(None)
      _ <- busy.set(true)
      validated = (EmailAddress(emailRaw.trim).toEither, NonEmptyString(passwordRaw).toEither) match
                    case (Left(e), _) => Left(e.mkString(", "))
                    case (_, Left(e)) => Left(e.mkString(", "))
                    case (Right(em), Right(pw)) => Right((em, pw))
      res <- (validated match
               case Left(msg) => Kyo.lift(Left(msg): Either[String, StaffAuthResult]): Either[String, StaffAuthResult] < Async
               case Right((em, pw)) =>
                 Abort.run[BackofficeError](client.loginStaff(em, pw)).flatMap {
                   case Result.Success(staffAuth) =>
                     for
                       _   <- auth.set(Some(staffAuth))
                       _   <- refreshCurrentRoute
                       msg <- i18n.now("adm-toast-signed-in", Dict("name" -> staffAuth.employee.displayName.value))
                       _   <- showToast(msg)
                     yield Right(staffAuth): Either[String, StaffAuthResult]
                   case Result.Failure(err) => Kyo.lift(Left(err.message): Either[String, StaffAuthResult])
                   case Result.Panic(ex)    => Kyo.lift(Left(Option(ex.getMessage).getOrElse("Login error")): Either[String, StaffAuthResult])
                 })
      _ <- busy.set(false)
    yield res

  def logout: Unit < Async =
    for
      curr <- auth.get
      _ <- curr match
             case Some(sa) => client.logoutStaff(sa.session.token)
             case None     => Kyo.unit
      _   <- auth.set(None)
      _   <- dashboard.set(None)
      _   <- orders.set(Nil)
      _   <- selectedOrder.set(None)
      _   <- selectedOrderArtwork.set(Nil)
      _   <- customers.set(Nil)
      _   <- selectedCustomer.set(None)
      _   <- staffMembers.set(Nil)
      _   <- errorBanner.set(None)
      msg <- i18n.now("adm-toast-signed-out")
      _   <- showToast(msg)
    yield ()

  // ── Routing & Data Loading ─────────────────────────────────────────────────

  def navigate(next: AdminRoute, syncHash: Boolean = true): Unit < Async =
    for
      _ <- mobileNavOpen.set(false)
      _ <- errorBanner.set(None)
      _ <- route.set(next)
      _ <- if syncHash then onRouteChange(next) else Kyo.unit
      _ <- refreshRoute(next)
    yield ()

  def refreshCurrentRoute: Unit < Async =
    route.get.flatMap(refreshRoute)

  def refreshRoute(r: AdminRoute): Unit < Async =
    auth.get.flatMap {
      case None => Kyo.unit
      case Some(_) =>
        r match
          case AdminRoute.Dashboard            => refreshDashboard
          case AdminRoute.Orders               => refreshOrders
          case AdminRoute.OrderDetail(orderId) => refreshOrderDetail(orderId)
          case AdminRoute.Catalog              => refreshCatalog
          case AdminRoute.Pricing              => refreshPricing
          case AdminRoute.Customers            => refreshCustomers
          case AdminRoute.StaffAndSettings     => refreshStaffAndSettings
    }

  def refreshDashboard: Unit < Async =
    for
      res <- withToken(client.getDashboardSummary)
      _ <- res match
             case Some(d) => dashboard.set(Some(d))
             case None    => Kyo.unit
    yield ()

  def refreshOrders: Unit < Async =
    for
      res <- withToken(tok => client.listOrders(tok, OrderFilter()))
      _ <- res match
             case Some(list) => orders.set(list)
             case None       => Kyo.unit
    yield ()

  def refreshOrderDetail(orderId: OrderId): Unit < Async =
    for
      ordOpt <- withToken(tok => client.getOrder(tok, orderId))
      artOpt <- withToken(tok => client.listOrderArtwork(tok, orderId))
      _      <- selectedOrder.set(ordOpt)
      _      <- selectedOrderArtwork.set(artOpt.getOrElse(Nil))
    yield ()

  def refreshCatalog: Unit < Async =
    for
      catOpt   <- withToken(client.getCatalog)
      rulesOpt <- withToken(client.getRuleset)
      _        <- catOpt.map(c => catalog.set(Some(c))).getOrElse(Kyo.unit)
      _        <- rulesOpt.map(r => ruleset.set(Some(r))).getOrElse(Kyo.unit)
    yield ()

  def refreshPricing: Unit < Async =
    for
      catOpt  <- withToken(client.getCatalog)
      plOpt   <- withToken(client.listPricelists)
      agrOpt  <- withToken(client.listAgreements)
      prOpt   <- withToken(client.listPromoCodes)
      csOpt   <- withToken(client.listCostSheets)
      custOpt <- withToken(tok => client.listCustomers(tok, CustomerFilter()))
      _       <- catOpt.map(c => catalog.set(Some(c))).getOrElse(Kyo.unit)
      _       <- plOpt.map(pricelists.set).getOrElse(Kyo.unit)
      _       <- agrOpt.map(agreements.set).getOrElse(Kyo.unit)
      _       <- prOpt.map(promoCodes.set).getOrElse(Kyo.unit)
      _       <- csOpt.map(costSheets.set).getOrElse(Kyo.unit)
      _       <- custOpt.map(customers.set).getOrElse(Kyo.unit)
    yield ()

  def refreshCustomers: Unit < Async =
    for
      res    <- withToken(tok => client.listCustomers(tok, CustomerFilter()))
      agrOpt <- withToken(client.listAgreements)
      ordOpt <- withToken(tok => client.listOrders(tok, OrderFilter()))
      _      <- res.map(customers.set).getOrElse(Kyo.unit)
      _      <- agrOpt.map(agreements.set).getOrElse(Kyo.unit)
      _      <- ordOpt.map(orders.set).getOrElse(Kyo.unit)
      sel    <- selectedCustomer.get
      _ <- sel match
             case Some(d) => selectCustomer(d.customer.id)
             case None    => Kyo.unit
    yield ()

  def openCustomerPricing(customerId: CustomerId): Unit < Async =
    for
      _ <- pricingFocusTab.set("agreements")
      _ <- pricingFocusCustomerId.set(Some(customerId))
      _ <- navigate(AdminRoute.Pricing)
    yield ()

  def selectCustomer(id: CustomerId): Unit < Async =
    for
      res <- withToken(tok => client.getCustomerDetail(tok, id))
      _   <- res.map(d => selectedCustomer.set(Some(d))).getOrElse(Kyo.unit)
    yield ()

  def refreshStaffAndSettings: Unit < Async =
    for
      curr <- auth.get
      _ <-
        if canManage(curr) then
          for
            sOpt <- withToken(client.listStaffMembers)
            _    <- sOpt.map(staffMembers.set).getOrElse(Kyo.unit)
          yield ()
        else staffMembers.set(Nil)
      dOpt <- withToken(client.getStoreDeliverySettings)
      _    <- dOpt.map(d => deliverySettings.set(Some(d))).getOrElse(Kyo.unit)
    yield ()

  // ── Order Intake Mutations ─────────────────────────────────────────────────

  private def applyOrderUpdate(resOpt: Option[OrderView]): Unit < Async =
    resOpt match
      case None => Kyo.unit
      case Some(updated) =>
        for
          _   <- selectedOrder.set(Some(updated))
          _   <- refreshOrders
          _   <- refreshDashboard
          _   <- onDataMutated()
          msg <- i18n.now("adm-toast-order-updated")
          _   <- showToast(msg)
        yield ()

  def acceptOrder(orderId: OrderId, noteRaw: String = ""): Unit < Async =
    val noteOpt = NonEmptyString(noteRaw.trim).toOption
    withToken(tok => client.acceptOrder(tok, orderId, noteOpt)).flatMap(applyOrderUpdate)

  def confirmOrderPayment(orderId: OrderId, noteRaw: String = ""): Unit < Async =
    val noteOpt = NonEmptyString(noteRaw.trim).toOption
    withToken(tok => client.confirmOrderPayment(tok, orderId, noteOpt)).flatMap(applyOrderUpdate)

  def markOrderPaymentStatus(orderId: OrderId, status: PaymentStatus, noteRaw: String = ""): Unit < Async =
    val noteOpt = NonEmptyString(noteRaw.trim).toOption
    withToken(tok => client.markOrderPaymentStatus(tok, orderId, status, noteOpt)).flatMap(applyOrderUpdate)

  def editOrderLine(cmd: EditOrderLineCmd): Unit < Async =
    withToken(tok => client.editOrderLine(tok, cmd)).flatMap(applyOrderUpdate)

  def putOrderOnHold(orderId: OrderId, reasonRaw: String): Unit < Async =
    NonEmptyString(reasonRaw.trim).toEither match
      case Left(_)       => errorBanner.set(Some("Please enter a reason to put this order on hold."))
      case Right(reason) => withToken(tok => client.putOrderOnHold(tok, orderId, reason)).flatMap(applyOrderUpdate)

  def resumeOrderFromHold(orderId: OrderId, noteRaw: String = ""): Unit < Async =
    val noteOpt = NonEmptyString(noteRaw.trim).toOption
    withToken(tok => client.resumeOrderFromHold(tok, orderId, noteOpt)).flatMap(applyOrderUpdate)

  def advanceOrderStage(orderId: OrderId, target: OrderStatus, noteRaw: String = ""): Unit < Async =
    val noteOpt = NonEmptyString(noteRaw.trim).toOption
    withToken(tok => client.advanceOrderStage(tok, orderId, target, noteOpt)).flatMap(applyOrderUpdate)

  def cancelOrder(orderId: OrderId, reasonRaw: String): Unit < Async =
    NonEmptyString(reasonRaw.trim).toEither match
      case Left(_)       => errorBanner.set(Some("Please enter a cancellation reason."))
      case Right(reason) => withToken(tok => client.cancelOrder(tok, orderId, reason)).flatMap(applyOrderUpdate)

  def addOrderNote(orderId: OrderId, textRaw: String): Unit < Async =
    NonEmptyString(textRaw.trim).toEither match
      case Left(_)     => errorBanner.set(Some("Note text must not be empty."))
      case Right(text) => withToken(tok => client.addOrderInternalNote(tok, orderId, text)).flatMap(applyOrderUpdate)

  def approveArtwork(orderId: OrderId, artworkId: ArtworkId, overrideFailures: Boolean): Unit < Async =
    for
      res <- withToken(tok => client.approveArtwork(tok, artworkId, overrideFailures))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshOrderDetail(orderId)
                 msg <- i18n.now("adm-toast-saved")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def requestArtworkChanges(orderId: OrderId, artworkId: ArtworkId, notesRaw: String): Unit < Async =
    NonEmptyString(notesRaw.trim).toEither match
      case Left(_) => errorBanner.set(Some("Please describe the required artwork changes."))
      case Right(notes) =>
        for
          res <- withToken(tok => client.requestArtworkChanges(tok, artworkId, notes))
          _ <- res match
                 case Some(_) =>
                   for
                     _   <- refreshOrderDetail(orderId)
                     msg <- i18n.now("adm-toast-saved")
                     _   <- showToast(msg)
                   yield ()
                 case None => Kyo.unit
        yield ()

  def rejectArtwork(orderId: OrderId, artworkId: ArtworkId, reasonRaw: String): Unit < Async =
    NonEmptyString(reasonRaw.trim).toEither match
      case Left(_) => errorBanner.set(Some("Please provide a rejection reason."))
      case Right(reason) =>
        for
          res <- withToken(tok => client.rejectArtwork(tok, artworkId, reason))
          _ <- res match
                 case Some(_) =>
                   for
                     _   <- refreshOrderDetail(orderId)
                     msg <- i18n.now("adm-toast-saved")
                     _   <- showToast(msg)
                   yield ()
                 case None => Kyo.unit
        yield ()

  private def toStaffPayload(file: BrowserPickedFile, expectedTrim: Dimension2D): StaffArtworkUploadPayload =
    val sf = ArtworkComponents.toUploadPayload(file, expectedTrim, None)
    StaffArtworkUploadPayload(
      fileName = sf.fileName,
      mimeType = sf.mimeType,
      sizeBytes = sf.sizeBytes,
      downloadUrl = sf.downloadUrl,
      previewUrl = sf.previewUrl,
      resolutionDpi = sf.resolutionDpi,
      bleedMm = sf.bleedMm,
      colorSpace = sf.colorSpace,
      pageCount = sf.pageCount,
      fontsEmbedded = sf.fontsEmbedded,
    )

  def uploadStaffArtworkRevision(
      orderId: OrderId,
      artworkId: ArtworkId,
      expectedTrim: Dimension2D,
      file: BrowserPickedFile,
  ): Unit < Async =
    val payload = toStaffPayload(file, expectedTrim)
    for
      res <- withToken(tok => client.uploadStaffArtworkRevision(tok, orderId, artworkId, payload))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshOrderDetail(orderId)
                 msg <- i18n.now("adm-od-artwork-uploaded-toast")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def uploadStaffOrderLineArtwork(
      orderId: OrderId,
      lineIndex: Int,
      componentKey: ComponentKey,
      expectedTrim: Dimension2D,
      file: BrowserPickedFile,
  ): Unit < Async =
    val payload = toStaffPayload(file, expectedTrim)
    for
      res <- withToken(tok => client.uploadStaffOrderLineArtwork(tok, orderId, lineIndex, componentKey, payload))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshOrderDetail(orderId)
                 msg <- i18n.now("adm-od-artwork-uploaded-toast")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def rerunArtworkPreflight(orderId: OrderId, artworkId: ArtworkId): Unit < Async =
    for
      res <- withToken(tok => client.rerunArtworkPreflight(tok, orderId, artworkId))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshOrderDetail(orderId)
                 msg <- i18n.now("adm-od-artwork-preflight-toast")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def notifyLinkCopied(url: String): Unit < Async =
    i18n.now("adm-od-artwork-link-copied").flatMap(showToast)

  // ── Catalog Mutations ──────────────────────────────────────────────────────

  private def applyCatalogUpdate(resOpt: Option[ProductCatalog], deleted: Boolean = false): Unit < Async =
    resOpt match
      case None => Kyo.unit
      case Some(updated) =>
        for
          _   <- catalog.set(Some(updated))
          _   <- onDataMutated()
          msg <- i18n.now(if deleted then "adm-toast-deleted" else "adm-toast-saved")
          _   <- showToast(msg)
        yield ()

  def upsertCategory(cat: ProductCategory): Unit < Async =
    withToken(tok => client.upsertCategory(tok, cat)).flatMap(applyCatalogUpdate(_))

  def deleteCategory(id: CategoryId): Unit < Async =
    withToken(tok => client.deleteCategory(tok, id)).flatMap(applyCatalogUpdate(_, deleted = true))

  def upsertMaterial(m: Material): Unit < Async =
    withToken(tok => client.upsertMaterial(tok, m)).flatMap(applyCatalogUpdate(_))

  def deleteMaterial(id: MaterialId): Unit < Async =
    withToken(tok => client.deleteMaterial(tok, id)).flatMap(applyCatalogUpdate(_, deleted = true))

  def upsertFinish(f: Finish): Unit < Async =
    withToken(tok => client.upsertFinish(tok, f)).flatMap(applyCatalogUpdate(_))

  def deleteFinish(id: FinishId): Unit < Async =
    withToken(tok => client.deleteFinish(tok, id)).flatMap(applyCatalogUpdate(_, deleted = true))

  def upsertPrintingMethod(pm: PrintingMethod): Unit < Async =
    withToken(tok => client.upsertPrintingMethod(tok, pm)).flatMap(applyCatalogUpdate(_))

  def deletePrintingMethod(id: PrintingMethodId): Unit < Async =
    withToken(tok => client.deletePrintingMethod(tok, id)).flatMap(applyCatalogUpdate(_, deleted = true))

  def upsertShowcaseListing(sl: ShowcaseListing): Unit < Async =
    withToken(tok => client.upsertShowcaseListing(tok, sl)).flatMap(applyCatalogUpdate(_))

  def deleteShowcaseListing(catId: CategoryId): Unit < Async =
    withToken(tok => client.deleteShowcaseListing(tok, catId)).flatMap(applyCatalogUpdate(_, deleted = true))

  def upsertRule(rule: CompatibilityRule): Unit < Async =
    for
      res <- withToken(tok => client.upsertRule(tok, rule))
      _ <- res match
             case Some(rs) =>
               for
                 _   <- ruleset.set(Some(rs))
                 _   <- onDataMutated()
                 msg <- i18n.now("adm-toast-saved")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def deleteRule(id: RuleId): Unit < Async =
    for
      res <- withToken(tok => client.deleteRule(tok, id))
      _ <- res match
             case Some(rs) =>
               for
                 _   <- ruleset.set(Some(rs))
                 _   <- onDataMutated()
                 msg <- i18n.now("adm-toast-deleted")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  // ── Pricing Mutations & Simulation ─────────────────────────────────────────

  def simulateMargin(cmd: MarginSimulationCmd): Option[MarginSimulationResult] < Async =
    withToken(tok => client.simulateMargin(tok, cmd))

  def savePromoCode(promo: PromoCode): Unit < Async =
    for
      res <- withToken(tok => client.savePromoCode(tok, promo))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshPricing
                 _   <- onDataMutated()
                 msg <- i18n.now("adm-toast-saved")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def togglePromoActive(id: DiscountCodeId, active: Boolean): Unit < Async =
    for
      res <- withToken(tok => client.setPromoCodeActive(tok, id, active))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshPricing
                 _   <- onDataMutated()
                 msg <- i18n.now("adm-toast-saved")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def deletePromoCode(id: DiscountCodeId): Unit < Async =
    for
      res <- withToken(tok => client.deletePromoCode(tok, id))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshPricing
                 _   <- onDataMutated()
                 msg <- i18n.now("adm-toast-deleted")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def saveAgreement(agr: CustomerPriceAgreement): Unit < Async =
    for
      res <- withToken(tok => client.saveAgreement(tok, agr))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshPricing
                 _   <- onDataMutated()
                 msg <- i18n.now("adm-toast-saved")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def deleteAgreement(target: AgreementTarget, currency: Currency): Unit < Async =
    for
      res <- withToken(tok => client.deleteAgreement(tok, target, currency))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshPricing
                 _   <- onDataMutated()
                 msg <- i18n.now("adm-toast-deleted")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def savePricelist(pl: Pricelist): Unit < Async =
    for
      res <- withToken(tok => client.savePricelist(tok, pl))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshPricing
                 _   <- onDataMutated()
                 msg <- i18n.now("adm-toast-saved")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def saveCostSheet(sheet: ProductionCostSheet): Unit < Async =
    for
      res <- withToken(tok => client.saveCostSheet(tok, sheet))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshPricing
                 msg <- i18n.now("adm-toast-saved")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  // ── Customer Mutations ─────────────────────────────────────────────────────

  private def afterCustomerMutation(id: CustomerId): Unit < Async =
    for
      _   <- refreshCustomers
      _   <- selectCustomer(id)
      _   <- refreshDashboard
      _   <- onDataMutated()
      msg <- i18n.now("adm-toast-saved")
      _   <- showToast(msg)
    yield ()

  def registerCustomer(cmd: RegisterCustomerCmd): Unit < Async =
    for
      res <- withToken(tok => client.registerCustomer(tok, cmd))
      _   <- res.map(c => afterCustomerMutation(c.id)).getOrElse(Kyo.unit)
    yield ()

  def approveCustomer(id: CustomerId): Unit < Async =
    for
      res <- withToken(tok => client.approveCustomer(tok, id))
      _   <- res.map(_ => afterCustomerMutation(id)).getOrElse(Kyo.unit)
    yield ()

  def suspendCustomer(id: CustomerId, reasonRaw: String): Unit < Async =
    NonEmptyString(reasonRaw.trim).toEither match
      case Left(_) => errorBanner.set(Some("Please enter a suspension reason."))
      case Right(reason) =>
        withToken(tok => client.suspendCustomer(tok, id, reason)).flatMap(
          _.map(_ => afterCustomerMutation(id)).getOrElse(Kyo.unit)
        )

  def deactivateCustomer(id: CustomerId, reasonRaw: String): Unit < Async =
    NonEmptyString(reasonRaw.trim).toEither match
      case Left(_) => errorBanner.set(Some("Please enter a deactivation reason."))
      case Right(reason) =>
        withToken(tok => client.deactivateCustomer(tok, id, reason)).flatMap(
          _.map(_ => afterCustomerMutation(id)).getOrElse(Kyo.unit)
        )

  def changeCustomerTier(id: CustomerId, tier: CustomerTier): Unit < Async =
    withToken(tok => client.changeCustomerTier(tok, id, tier)).flatMap(
      _.map(_ => afterCustomerMutation(id)).getOrElse(Kyo.unit)
    )

  def grantCustomerPaymentTerms(id: CustomerId, terms: PaymentTerms): Unit < Async =
    withToken(tok => client.grantCustomerPaymentTerms(tok, id, terms)).flatMap(
      _.map(_ => afterCustomerMutation(id)).getOrElse(Kyo.unit)
    )

  def addCustomerNote(id: CustomerId, textRaw: String): Unit < Async =
    NonEmptyString(textRaw.trim).toEither match
      case Left(_) => errorBanner.set(Some("Note text must not be empty."))
      case Right(text) =>
        withToken(tok => client.addCustomerNote(tok, id, text)).flatMap(
          _.map(_ => afterCustomerMutation(id)).getOrElse(Kyo.unit)
        )

  def updateCustomerProfile(cmd: UpdateCustomerProfileCmd): Unit < Async =
    withToken(tok => client.updateCustomerProfile(tok, cmd)).flatMap(
      _.map(_ => afterCustomerMutation(cmd.id)).getOrElse(Kyo.unit)
    )

  // ── Staff & Store Settings Mutations ───────────────────────────────────────

  def createStaffMember(cmd: CreateStaffCmd): Unit < Async =
    for
      res <- withToken(tok => client.createStaffMember(tok, cmd))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshStaffAndSettings
                 msg <- i18n.now("adm-toast-saved")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def updateStaffRoles(id: EmployeeId, roles: Set[EmployeeRole]): Unit < Async =
    if roles.isEmpty then errorBanner.set(Some("A staff member must have at least one role."))
    else
      for
        res <- withToken(tok => client.updateStaffRoles(tok, id, roles))
        _ <- res match
               case Some(_) =>
                 for
                   _   <- refreshStaffAndSettings
                   msg <- i18n.now("adm-toast-saved")
                   _   <- showToast(msg)
                 yield ()
               case None => Kyo.unit
      yield ()

  def setStaffActive(id: EmployeeId, active: Boolean): Unit < Async =
    for
      res <- withToken(tok => client.setStaffActive(tok, id, active))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshStaffAndSettings
                 msg <- i18n.now("adm-toast-saved")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def upsertPickupLocation(loc: ShopPickupLocation): Unit < Async =
    for
      res <- withToken(tok => client.upsertPickupLocation(tok, loc))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshStaffAndSettings
                 _   <- onDataMutated()
                 msg <- i18n.now("adm-toast-saved")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def deletePickupLocation(id: DeliveryOptionId): Unit < Async =
    for
      res <- withToken(tok => client.deletePickupLocation(tok, id))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshStaffAndSettings
                 _   <- onDataMutated()
                 msg <- i18n.now("adm-toast-deleted")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def upsertCourierOption(opt: CourierServiceOption): Unit < Async =
    for
      res <- withToken(tok => client.upsertCourierOption(tok, opt))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshStaffAndSettings
                 _   <- onDataMutated()
                 msg <- i18n.now("adm-toast-saved")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def deleteCourierOption(currency: Currency, id: DeliveryOptionId): Unit < Async =
    for
      res <- withToken(tok => client.deleteCourierOption(tok, currency, id))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshStaffAndSettings
                 _   <- onDataMutated()
                 msg <- i18n.now("adm-toast-deleted")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

  def upsertBankAccount(currency: Currency, account: BankAccountConfig): Unit < Async =
    for
      res <- withToken(tok => client.upsertBankAccount(tok, currency, account))
      _ <- res match
             case Some(_) =>
               for
                 _   <- refreshStaffAndSettings
                 _   <- onDataMutated()
                 msg <- i18n.now("adm-toast-saved")
                 _   <- showToast(msg)
               yield ()
             case None => Kyo.unit
    yield ()

object BackofficeState:

  def init(
      client: BackofficeClient,
      i18n: AppI18n,
      initialRoute: AdminRoute = AdminRoute.Dashboard,
      onRouteChange: AdminRoute => Unit < Sync = _ => Kyo.unit,
      onLanguagePersist: Language => Unit < Sync = _ => Kyo.unit,
      onDataMutated: () => Unit < Async = () => Kyo.unit,
  ): BackofficeState < Sync =
    for
      routeRef    <- Signal.initRef[AdminRoute](initialRoute)
      authRef     <- Signal.initRef[Option[StaffAuthResult]](None)
      dashRef     <- Signal.initRef[Option[BackofficeDashboardSummary]](None)
      ordersRef   <- Signal.initRef[List[OrderView]](Nil)
      ordFiltRef  <- Signal.initRef[Option[OrderStatusKind]](None)
      ordSearch   <- Signal.initRef("")
      selOrdRef   <- Signal.initRef[Option[OrderView]](None)
      selArtRef   <- Signal.initRef[List[ArtworkSubmissionView]](Nil)
      artPrevH    <- ArtworkComponents.PreviewModal.Handle.make
      catRef      <- Signal.initRef[Option[ProductCatalog]](None)
      rulesRef    <- Signal.initRef[Option[CompatibilityRuleset]](None)
      plRef       <- Signal.initRef[List[Pricelist]](Nil)
      agrRef      <- Signal.initRef[List[CustomerPriceAgreement]](Nil)
      promoRef    <- Signal.initRef[List[PromoCode]](Nil)
      costRef     <- Signal.initRef[List[ProductionCostSheet]](Nil)
      custRef     <- Signal.initRef[List[CustomerView]](Nil)
      custFiltRef <- Signal.initRef[Option[CustomerStatus]](None)
      custSearch  <- Signal.initRef("")
      selCustRef  <- Signal.initRef[Option[CustomerDetailView]](None)
      staffRef    <- Signal.initRef[List[StaffMemberView]](Nil)
      delivRef    <- Signal.initRef[Option[StoreDeliverySettingsView]](None)
      priTabRef   <- Signal.initRef("simulator")
      priCustRef  <- Signal.initRef[Option[CustomerId]](None)
      mobNavRef   <- Signal.initRef(false)
      toastRef    <- Signal.initRef[Option[String]](None)
      errRef      <- Signal.initRef[Option[String]](None)
      busyRef     <- Signal.initRef(false)
    yield new BackofficeState(
      client,
      i18n,
      routeRef,
      authRef,
      dashRef,
      ordersRef,
      ordFiltRef,
      ordSearch,
      selOrdRef,
      selArtRef,
      artPrevH,
      catRef,
      rulesRef,
      plRef,
      agrRef,
      promoRef,
      costRef,
      custRef,
      custFiltRef,
      custSearch,
      selCustRef,
      staffRef,
      delivRef,
      priTabRef,
      priCustRef,
      mobNavRef,
      toastRef,
      errRef,
      busyRef,
      onRouteChange,
      onLanguagePersist,
      onDataMutated,
    )
