package mpbuilder.ui.storefront

import kyo.*
import mpbuilder.artwork.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.shared.*
import mpbuilder.storefront.*
import mpbuilder.ui.components.{ArtworkComponents, BrowserPickedFile, ProductionNoteEditor}
import mpbuilder.ui.configurator.*
import mpbuilder.ui.i18n.AppI18n
import mpbuilder.ui.port.StorefrontClient

enum AppRoute:
  case Catalog
  case ProductDetail(categoryId: CategoryId)
  case Builder(categoryId: Option[CategoryId] = None, presetId: Option[PresetId] = None)
  case Checkout(source: CheckoutSource)
  case Account

object AppRoute:
  def toHash(route: AppRoute): String = route match
    case Catalog               => "#/catalog"
    case ProductDetail(catId)  => s"#/product/${catId.value}"
    case Builder(Some(c), Some(p)) => s"#/builder/${c.value}/${p.value}"
    case Builder(Some(c), None)    => s"#/builder/${c.value}"
    case Builder(None, _)          => "#/builder"
    case Checkout(_)           => "#/checkout"
    case Account               => "#/account"

  def fromHash(hash: String): Option[AppRoute] =
    val clean = hash.stripPrefix("#").stripPrefix("/")
    clean.split("/").toList.filter(_.nonEmpty) match
      case Nil | List("catalog")            => Some(Catalog)
      case List("product", catId)           => Some(ProductDetail(CategoryId.unsafe(catId)))
      case List("builder")                  => Some(Builder(None, None))
      case List("builder", catId)           => Some(Builder(Some(CategoryId.unsafe(catId)), None))
      case List("builder", catId, presetId) => Some(Builder(Some(CategoryId.unsafe(catId)), Some(PresetId.unsafe(presetId))))
      case List("account")                  => Some(Account)
      case _                                => None

/** Reactive storefront coordinator holding navigation, currency, auth, cart, configurator & account state. */
final class StorefrontState(
    val client: StorefrontClient,
    val catalogRef: SignalRef[ProductCatalog],
    val listingsRef: SignalRef[List[ShowcaseListing]],
    val i18n: AppI18n,
    val route: SignalRef[AppRoute],
    val currency: SignalRef[Currency],
    val authSession: SignalRef[Option[AuthSession]],
    val currentCustomer: SignalRef[Option[CustomerView]],
    val cart: SignalRef[Option[CartView]],
    val cartDrawerOpen: SignalRef[Boolean],
    val authModalOpen: SignalRef[Boolean],
    val mobileMenuOpen: SignalRef[Boolean],
    val toast: SignalRef[Option[String]],
    val configurator: ConfiguratorController,
    val presetQuotes: SignalRef[Map[PresetId, QuoteStatus]],
    val orders: SignalRef[List[OrderView]],
    val orderArtworks: SignalRef[Map[OrderId, List[mpbuilder.artwork.ArtworkSubmissionView]]],
    val paymentModal: SignalRef[Option[(OrderView, PaymentInstructions)]],
    val artworkPreview: ArtworkComponents.PreviewModal.Handle,
    val cartError: SignalRef[Option[String]],
    val checkoutSession: SignalRef[Option[CheckoutSession]],
    /** Inline editor for cart / order line production instructions (one line edited at a time). */
    val noteEditor: ProductionNoteEditor.Handle,
    private val idSeq: SignalRef[Long],
    val onRouteChange: AppRoute => Unit < Sync,
    val onLanguagePersist: Language => Unit < Sync,
    val theme: mpbuilder.ui.theme.ThemeController,
):

  def sessionTokenSignal: Signal[Option[SessionToken]] =
    authSession.map(_.map(_.token))

  def cartItemCountSignal: Signal[Int] =
    cart.map(_.map(_.lines.map(_.copies.value).sum).getOrElse(0))

  // ── Navigation ─────────────────────────────────────────────────────────────

  def navigate(next: AppRoute, syncHash: Boolean = true): Unit < Async =
    for
      _ <- mobileMenuOpen.set(false)
      _ <- next match
             case AppRoute.Checkout(source) =>
               CheckoutSession.init(this, source).flatMap(s => checkoutSession.set(Some(s)))
             case _ => Kyo.unit
      _ <- route.set(next)
      _ <- if syncHash then onRouteChange(next) else Kyo.unit
      _ <- next match
             case AppRoute.ProductDetail(catId) => refreshPresetQuotes(catId)
             case AppRoute.Builder(Some(catId), Some(presetId)) =>
               configurator.applyPreset(catId, presetId)
             case AppRoute.Builder(Some(catId), None) =>
               configurator.selectCategory(catId)
             case AppRoute.Account => refreshOrders
             case _                => Kyo.unit
    yield ()

  def openCatalog: Unit < Async = navigate(AppRoute.Catalog)

  def openProductDetail(categoryId: CategoryId): Unit < Async =
    navigate(AppRoute.ProductDetail(categoryId))

  def openBuilder(categoryId: Option[CategoryId] = None, presetId: Option[PresetId] = None): Unit < Async =
    navigate(AppRoute.Builder(categoryId, presetId))

  def openAccount: Unit < Async =
    navigate(AppRoute.Account)

  // ── Language & Currency ────────────────────────────────────────────────────

  def setLanguage(lang: Language): Unit < Async =
    for
      _ <- i18n.setLanguage(lang)
      _ <- onLanguagePersist(lang)
      _ <- configurator.recompute
      r <- route.get
      _ <- r match
             case AppRoute.ProductDetail(catId) => refreshPresetQuotes(catId)
             case _                             => Kyo.unit
    yield ()

  def setCurrency(cur: Currency): Unit < Async =
    for
      prev <- currency.get
      _ <- if prev == cur then Kyo.unit
           else
             for
               _ <- currency.set(cur)
               // Reset local cart handle when switching currencies (carts are single-currency in backend)
               _ <- cart.set(None)
               _ <- configurator.recompute
               r <- route.get
               _ <- r match
                      case AppRoute.ProductDetail(catId) => refreshPresetQuotes(catId)
                      case _                             => Kyo.unit
             yield ()
    yield ()

  // ── Live Catalog & Preset Quotes (Product Detail View) ─────────────────────

  def refreshCatalog: Unit < Async =
    for
      cat  <- client.getCatalog
      list <- client.listShowcaseListings
      _    <- catalogRef.set(cat)
      _    <- listingsRef.set(list.sortBy(_.sortOrder))
      _    <- configurator.updateCatalog(cat)
      r    <- route.get
      _ <- r match
             case AppRoute.ProductDetail(catId) => refreshPresetQuotes(catId)
             case _                             => Kyo.unit
    yield ()

  def refreshPresetQuotes(categoryId: CategoryId): Unit < Async =
    catalogRef.get.flatMap { catalog =>
      catalog.categories.get(categoryId).filter(_.enabled) match
        case None => presetQuotes.set(Map.empty)
        case Some(cat) =>
          val availablePresets = cat.presets.filter(ConfiguratorController.isPresetAvailable(_, catalog))
          for
            cur  <- currency.get
            tok  <- authSession.get.map(_.map(_.token))
            lang <- i18n.currentLanguage
            _    <- presetQuotes.set(availablePresets.map(p => p.id -> QuoteStatus.Loading).toMap)
            results <- Kyo.foreach(availablePresets) { preset =>
                         Abort
                           .run[StorefrontError](
                             client.pricePreset(
                               categoryId = categoryId,
                               presetId = preset.id,
                               currency = cur,
                               speed = ManufacturingSpeed.Standard,
                               specsOverride = None,
                               sessionToken = tok,
                               lang = lang,
                             )
                           )
                           .map {
                             case Result.Success(q)   => preset.id -> QuoteStatus.Ready(q)
                             case Result.Failure(err) => preset.id -> QuoteStatus.Failed(err)
                             case Result.Panic(ex) =>
                               preset.id -> QuoteStatus.Failed(
                                 StorefrontError.CatalogFailure(
                                   CatalogError.RuleViolation(
                                     RuleId.unsafe("preset-quote"),
                                     LocalizedString(Option(ex.getMessage).getOrElse("Error")),
                                   )
                                 )
                               )
                           }
                       }
            _ <- presetQuotes.set(results.toMap)
          yield ()
    }

  // ── Auth & Account ─────────────────────────────────────────────────────────

  def completeAuth(result: AuthResult): Unit < Async =
    for
      _ <- authSession.set(Some(result.session))
      _ <- currentCustomer.set(result.customer)
      _ <- authModalOpen.set(false)
      _ <- configurator.recompute
      r <- route.get
      _ <- r match
             case AppRoute.ProductDetail(catId) => refreshPresetQuotes(catId)
             case AppRoute.Account              => refreshOrders
             case _                             => Kyo.unit
      msg <- i18n.now("auth-welcome-toast", Dict("name" -> result.session.principal.displayName.value))
      _   <- showToast(msg)
    yield ()

  def signOut: Unit < Async =
    for
      sessOpt <- authSession.get
      _ <- sessOpt match
             case Some(s) => client.logout(s.token)
             case None    => Kyo.unit
      _ <- authSession.set(None)
      _ <- currentCustomer.set(None)
      _ <- orders.set(Nil)
      _ <- orderArtworks.set(Map.empty)
      _ <- paymentModal.set(None)
      _ <- configurator.recompute
      r <- route.get
      _ <- r match
             case AppRoute.ProductDetail(catId) => refreshPresetQuotes(catId)
             case _                             => Kyo.unit
      msg <- i18n.now("auth-signed-out-toast")
      _   <- showToast(msg)
    yield ()

  def refreshOrders: Unit < Async =
    for
      sessOpt <- authSession.get
      _ <- sessOpt match
             case None =>
               orders.set(Nil).andThen(orderArtworks.set(Map.empty))
             case Some(sess) =>
               Abort.run[StorefrontError](client.listMyOrders(sess.token)).flatMap {
                 case Result.Success(list) =>
                   for
                     _ <- orders.set(list)
                     artPairs <- Kyo.foreach(list) { ord =>
                                   Abort.run[StorefrontError](client.listOrderArtworks(sess.token, ord.id)).map {
                                     case Result.Success(arts) => ord.id -> arts
                                     case _                    => ord.id -> Nil
                                   }
                                 }
                     _ <- orderArtworks.set(artPairs.toMap)
                   yield ()
                 case _ =>
                   orders.set(Nil).andThen(orderArtworks.set(Map.empty))
               }
    yield ()

  def updateMyAddresses(
      billingAddress: Address,
      shippingAddresses: List[Address],
      defaultShippingIndex: Option[Int],
  ): Unit < Async =
    for
      sessOpt <- authSession.get
      _ <- sessOpt match
             case None => Kyo.unit
             case Some(sess) =>
               Abort
                 .run[StorefrontError](
                   client.updateMyAddresses(sess.token, billingAddress, shippingAddresses, defaultShippingIndex)
                 )
                 .flatMap {
                   case Result.Success(updated) =>
                     for
                       _   <- currentCustomer.set(Some(updated))
                       msg <- i18n.now("account-addresses-saved")
                       _   <- showToast(msg)
                     yield ()
                   case Result.Failure(err) => showToast(err.message)
                   case Result.Panic(ex)    => showToast(Option(ex.getMessage).getOrElse("Error"))
                 }
    yield ()

  def openPaymentInstructions(order: OrderView): Unit < Async =
    for
      sessOpt <- authSession.get
      _ <- sessOpt match
             case None => Kyo.unit
             case Some(sess) =>
               Abort.run[StorefrontError](client.getOrderPaymentInstructions(sess.token, order.id)).flatMap {
                 case Result.Success(instr) => paymentModal.set(Some((order, instr)))
                 case Result.Failure(err)   => showToast(err.message)
                 case Result.Panic(ex)      => showToast(Option(ex.getMessage).getOrElse("Error"))
               }
    yield ()

  def closePaymentInstructions: Unit < Sync =
    paymentModal.set(None)

  def reuploadOrderArtwork(
      orderId: OrderId,
      artworkId: ArtworkId,
      fileName: NonEmptyString,
  ): Unit < Async =
    for
      sessOpt <- authSession.get
      _ <- sessOpt match
             case None => Kyo.unit
             case Some(sess) =>
               Abort
                 .run[StorefrontError](
                   client.reuploadOrderArtwork(
                     sessionToken = sess.token,
                     orderId = orderId,
                     artworkId = artworkId,
                     fileName = fileName,
                     resolutionDpi = 300,
                     bleedMm = 3,
                     colorSpace = mpbuilder.artwork.ColorSpace.CMYK,
                   )
                 )
                 .flatMap {
                   case Result.Success(_) =>
                     for
                       _   <- refreshOrders
                       msg <- i18n.now("account-artwork-reuploaded")
                       _   <- showToast(msg)
                     yield ()
                   case Result.Failure(err) => showToast(err.message)
                   case Result.Panic(ex)    => showToast(Option(ex.getMessage).getOrElse("Error"))
                 }
    yield ()

  def uploadOrderLineArtwork(
      orderId: OrderId,
      lineIndex: Int,
      componentKey: ComponentKey,
      expectedTrim: Dimension2D,
      expectedPages: Option[Int],
      file: BrowserPickedFile,
  ): Unit < Async =
    for
      sessOpt <- authSession.get
      _ <- sessOpt match
             case None => Kyo.unit
             case Some(sess) =>
               val payload = ArtworkComponents.toUploadPayload(file, expectedTrim, expectedPages)
               Abort
                 .run[StorefrontError](
                   client.uploadOrderLineArtwork(sess.token, orderId, lineIndex, componentKey, payload)
                 )
                 .flatMap {
                   case Result.Success(_) =>
                     for
                       _   <- refreshOrders
                       msg <- i18n.now("account-artwork-uploaded")
                       _   <- showToast(msg)
                     yield ()
                   case Result.Failure(err) => showToast(err.message)
                   case Result.Panic(ex)    => showToast(Option(ex.getMessage).getOrElse("Error"))
                 }
    yield ()

  def reuploadOrderArtworkFile(
      orderId: OrderId,
      artworkId: ArtworkId,
      expectedTrim: Dimension2D,
      expectedPages: Option[Int],
      file: BrowserPickedFile,
  ): Unit < Async =
    for
      sessOpt <- authSession.get
      _ <- sessOpt match
             case None => Kyo.unit
             case Some(sess) =>
               val payload = ArtworkComponents.toUploadPayload(file, expectedTrim, expectedPages)
               Abort
                 .run[StorefrontError](
                   client.reuploadOrderArtworkPayload(sess.token, orderId, artworkId, payload)
                 )
                 .flatMap {
                   case Result.Success(_) =>
                     for
                       _   <- refreshOrders
                       msg <- i18n.now("account-artwork-reuploaded")
                       _   <- showToast(msg)
                     yield ()
                   case Result.Failure(err) => showToast(err.message)
                   case Result.Panic(ex)    => showToast(Option(ex.getMessage).getOrElse("Error"))
                 }
    yield ()

  def notifyLinkCopied(url: String): Unit < Async =
    i18n.now("cfg-artwork-link-copied").flatMap(showToast)

  // ── Shopping Cart ──────────────────────────────────────────────────────────

  private def ensureCartId: CartId < (Abort[StorefrontError] & Async) =
    cart.get.flatMap {
      case Some(existing) => existing.id
      case None =>
        for
          seq <- idSeq.updateAndGet(_ + 1)
          cur <- currency.get
          tok <- authSession.get.map(_.map(_.token))
          cid  = CartId.unsafe(s"cart-ui-$seq")
          c   <- client.createCart(cid, cur, tok)
          _   <- cart.set(Some(c))
        yield c.id
    }

  /** Adds the draft to the cart (opening the cart drawer); returns whether it succeeded. */
  def addDraftToCart(d: DraftConfig): Boolean < Async =
    for
      _   <- cartError.set(None)
      cmd <- configurator.buildConfigureCmd(d)
      cur <- currency.get
      tok <- authSession.get.map(_.map(_.token))
      artIds = d.artworks.view.mapValues(_.id).toMap
      note <- configurator.currentProductionNote
      res <- Abort.run[StorefrontError] {
               for
                 cid     <- ensureCartId
                 updated <- client.addConfiguredProductToCart(cid, cmd, cur, d.speed, Quantity.one, tok, artIds, note)
               yield updated
             }
      ok <- res match
             case Result.Success(updated) =>
               for
                 _   <- cart.set(Some(updated))
                 _   <- configurator.clearDraftArtworks
                 _   <- cartDrawerOpen.set(true)
                 msg <- i18n.now("cfg-added-toast")
                 _   <- showToast(msg)
               yield true
             case Result.Failure(err) =>
               showToast(err.message).andThen(false)
             case Result.Panic(ex) =>
               showToast(Option(ex.getMessage).getOrElse("Cart error")).andThen(false)
    yield ok

  def addPresetToCart(categoryId: CategoryId, presetId: PresetId): Unit < Async =
    catalogRef.get.flatMap { catalog =>
      catalog.categories
        .get(categoryId)
        .filter(_.enabled)
        .flatMap(cat => cat.presets.find(p => p.id == presetId && ConfiguratorController.isPresetAvailable(p, catalog)).map(cat -> _)) match
        case None => Kyo.unit
        case Some((cat, preset)) =>
          val draft = ConfiguratorController.draftFromPreset(cat, preset)
          addDraftToCart(draft).unit
    }

  /** Starts a "buy now" checkout. Sends only the configuration; the backend re-prices it on every checkout call. */
  def startQuickOrder(d: DraftConfig): Unit < Async =
    for
      cmd  <- configurator.buildConfigureCmd(d)
      cur  <- currency.get
      note <- configurator.currentProductionNote
      item  = QuickOrderItem(
                cmd = cmd,
                currency = cur,
                speed = d.speed,
                copies = Quantity.one,
                artworkIds = d.artworks.view.mapValues(_.id).toMap,
                productionNote = note,
              )
      _ <- configurator.clearDraftArtworks
      _ <- navigate(AppRoute.Checkout(CheckoutSource.QuickSingleLine(item)))
    yield ()

  def startPresetQuickOrder(categoryId: CategoryId, presetId: PresetId): Unit < Async =
    catalogRef.get.flatMap { catalog =>
      catalog.categories
        .get(categoryId)
        .filter(_.enabled)
        .flatMap(cat => cat.presets.find(p => p.id == presetId && ConfiguratorController.isPresetAvailable(p, catalog)).map(cat -> _)) match
        case None => Kyo.unit
        case Some((cat, preset)) =>
          startQuickOrder(ConfiguratorController.draftFromPreset(cat, preset))
    }

  def startCartCheckout: Unit < Async =
    cart.get.flatMap {
      case Some(c) if c.lines.nonEmpty =>
        for
          _ <- cartDrawerOpen.set(false)
          _ <- navigate(AppRoute.Checkout(CheckoutSource.FromCart(c.id)))
        yield ()
      case _ => Kyo.unit
    }

  def updateCartLineCopies(lineId: OrderLineId, copies: Int): Unit < Async =
    if copies < 1 then removeCartLine(lineId)
    else
      cart.get.flatMap {
        case None => Kyo.unit
        case Some(c) =>
          Abort.run[StorefrontError](client.updateCartLineCopies(c.id, lineId, Quantity.unsafe(copies))).flatMap {
            case Result.Success(updated) => cart.set(Some(updated))
            case Result.Failure(err)     => cartError.set(Some(err.message))
            case _                       => Kyo.unit
          }
      }

  /** Saves a cart line's production instructions; returns whether it succeeded. */
  def updateCartLineProductionNote(lineId: OrderLineId, note: Option[String]): Boolean < Async =
    cart.get.flatMap {
      case None => false
      case Some(c) =>
        Abort.run[StorefrontError](client.updateCartLineProductionNote(c.id, lineId, note)).flatMap {
          case Result.Success(updated) => cart.set(Some(updated)).andThen(true)
          case Result.Failure(err)     => cartError.set(Some(err.message)).andThen(false)
          case Result.Panic(ex)        => cartError.set(Some(Option(ex.getMessage).getOrElse("Cart error"))).andThen(false)
        }
    }

  /** Saves an order line's production instructions (allowed until the order is accepted). */
  def updateOrderLineProductionNote(orderId: OrderId, lineId: OrderLineId, note: Option[String]): Boolean < Async =
    authSession.get.flatMap {
      case None => false
      case Some(sess) =>
        Abort.run[StorefrontError](client.updateMyOrderLineProductionNote(sess.token, orderId, lineId, note)).flatMap {
          case Result.Success(updated) =>
            for
              _   <- orders.updateAndGet(_.map(o => if o.id == updated.id then updated else o)).unit
              msg <- i18n.now("note-production-saved")
              _   <- showToast(msg)
            yield true
          case Result.Failure(err) => showToast(err.message).andThen(false)
          case Result.Panic(ex)    => showToast(Option(ex.getMessage).getOrElse("Error")).andThen(false)
        }
    }

  def removeCartLine(lineId: OrderLineId): Unit < Async =
    cart.get.flatMap {
      case None => Kyo.unit
      case Some(c) =>
        Abort.run[StorefrontError](client.removeCartLine(c.id, lineId)).flatMap {
          case Result.Success(updated) => cart.set(Some(updated))
          case Result.Failure(err)     => cartError.set(Some(err.message))
          case _                       => Kyo.unit
        }
    }

  def clearCart: Unit < Async =
    cart.get.flatMap {
      case None => Kyo.unit
      case Some(c) =>
        Abort.run[StorefrontError](client.clearCart(c.id)).flatMap {
          case Result.Success(updated) => cart.set(Some(updated)).andThen(cartError.set(None))
          case Result.Failure(err)     => cartError.set(Some(err.message))
          case _                       => Kyo.unit
        }
    }

  def applyCartPromo(code: String): Unit < Async =
    val trimmed = code.trim
    if trimmed.isEmpty then Kyo.unit
    else
      cart.get.flatMap {
        case None => Kyo.unit
        case Some(c) =>
          for
            _   <- cartError.set(None)
            tok <- authSession.get.map(_.map(_.token))
            res <- Abort.run[StorefrontError](client.applyCartPromo(c.id, trimmed, tok))
            _ <- res match
                   case Result.Success(updated) => cart.set(Some(updated))
                   case Result.Failure(err)     => cartError.set(Some(err.message))
                   case _                       => Kyo.unit
          yield ()
      }

  def removeCartPromo: Unit < Async =
    cart.get.flatMap {
      case None => Kyo.unit
      case Some(c) =>
        Abort.run[StorefrontError](client.removeCartPromo(c.id)).flatMap {
          case Result.Success(updated) => cart.set(Some(updated)).andThen(cartError.set(None))
          case Result.Failure(err)     => cartError.set(Some(err.message))
          case _                       => Kyo.unit
        }
    }

  def onOrderPlaced(source: CheckoutSource): Unit < Async =
    for
      _ <- source match
             case CheckoutSource.FromCart(_) => cart.set(None)
             case _                          => Kyo.unit
      _ <- refreshOrders
    yield ()

  def showToast(msg: String): Unit < Sync =
    toast.set(Some(msg))

  def dismissToast: Unit < Sync =
    toast.set(None)

object StorefrontState:

  def init(
      client: StorefrontClient,
      i18n: AppI18n,
      initialCurrency: Currency = Currency.CZK,
      initialRoute: AppRoute = AppRoute.Catalog,
      onRouteChange: AppRoute => Unit < Sync = _ => Kyo.unit,
      onLanguagePersist: Language => Unit < Sync = _ => Kyo.unit,
      theme: Option[mpbuilder.ui.theme.ThemeController] = None,
  ): StorefrontState < Async =
    for
      themeCtl    <- theme.fold(mpbuilder.ui.theme.ThemeController.init())(t => Sync.defer(t))
      catalog     <- client.getCatalog
      listings    <- client.listShowcaseListings
      catalogRef  <- Signal.initRef(catalog)
      listingsRef <- Signal.initRef(listings.sortBy(_.sortOrder))
      routeRef    <- Signal.initRef[AppRoute](initialRoute)
      curRef      <- Signal.initRef[Currency](initialCurrency)
      authRef     <- Signal.initRef[Option[AuthSession]](None)
      custRef     <- Signal.initRef[Option[CustomerView]](None)
      cartRef     <- Signal.initRef[Option[CartView]](None)
      drawerRef   <- Signal.initRef(false)
      authModal   <- Signal.initRef(false)
      mobileMenu  <- Signal.initRef(false)
      toastRef    <- Signal.initRef[Option[String]](None)
      presetsRef  <- Signal.initRef(Map.empty[PresetId, QuoteStatus])
      ordersRef   <- Signal.initRef(List.empty[OrderView])
      artRef      <- Signal.initRef(Map.empty[OrderId, List[mpbuilder.artwork.ArtworkSubmissionView]])
      payModalRef <- Signal.initRef[Option[(OrderView, PaymentInstructions)]](None)
      artPrevH    <- ArtworkComponents.PreviewModal.Handle.make
      cartErrRef  <- Signal.initRef[Option[String]](None)
      checkoutRef <- Signal.initRef[Option[CheckoutSession]](None)
      noteEdH     <- ProductionNoteEditor.Handle.make
      seqRef      <- Signal.initRef(0L)
      initCatOpt   = initialRoute match
                       case AppRoute.Builder(c, _) => c
                       case AppRoute.ProductDetail(c) => Some(c)
                       case _ => None
      initPreset   = initialRoute match
                       case AppRoute.Builder(_, p) => p
                       case _ => None
      ctrl        <- ConfiguratorController.init(
                       client = client,
                       catalog = catalog,
                       i18n = i18n,
                       currency = curRef,
                       sessionToken = authRef.map(_.map(_.token)),
                       initialCategoryId = initCatOpt,
                       initialPresetId = initPreset,
                     )
      state        = new StorefrontState(
                       client,
                       catalogRef,
                       listingsRef,
                       i18n,
                       routeRef,
                       curRef,
                       authRef,
                       custRef,
                       cartRef,
                       drawerRef,
                       authModal,
                       mobileMenu,
                       toastRef,
                       ctrl,
                       presetsRef,
                       ordersRef,
                       artRef,
                       payModalRef,
                       artPrevH,
                       cartErrRef,
                       checkoutRef,
                       noteEdH,
                       seqRef,
                       onRouteChange,
                       onLanguagePersist,
                       themeCtl,
                     )
      _ <- initialRoute match
             case AppRoute.ProductDetail(catId) => state.refreshPresetQuotes(catId)
             case AppRoute.Checkout(source)     => CheckoutSession.init(state, source).flatMap(s => checkoutRef.set(Some(s)))
             case _                             => Kyo.unit
    yield state
