package mpbuilder.ui.configurator

import kyo.*
import mpbuilder.artwork.*
import mpbuilder.catalog.*
import mpbuilder.identity.SessionToken
import mpbuilder.shared.*
import mpbuilder.storefront.*
import mpbuilder.ui.components.{ArtworkComponents, BrowserPickedFile}
import mpbuilder.ui.i18n.AppI18n
import mpbuilder.ui.port.StorefrontClient

/** Mutable-friendly draft state for a single component inside the product configurator. */
final case class DraftComponent(
    key: ComponentKey,
    materialId: MaterialId,
    printingMethodId: Option[PrintingMethodId],
    inkConfiguration: Option[InkConfiguration],
    finishes: List[FinishSelection],
)

/** Complete draft state of the product configurator. */
final case class DraftConfig(
    categoryId: CategoryId,
    activePresetId: Option[PresetId],
    activeComponentKey: ComponentKey,
    components: List[DraftComponent],
    specs: ProductSpecs,
    speed: ManufacturingSpeed,
    artworks: Map[ComponentKey, ArtworkSubmissionView] = Map.empty,
    /** Artworks that were designed in the Visual Editor (re-openable with "Edit design"). */
    designs: Map[ComponentKey, DesignId] = Map.empty,
)

/** Request from the configurator to open the (lazily loaded) Visual Editor for one printable component. */
final case class DesignOnlineRequest(
    categoryId: CategoryId,
    categoryName: String,
    componentKey: ComponentKey,
    componentLabel: String,
    trimSize: Dimension2D,
    pageCount: Option[Int],
    designId: Option[DesignId],
)

/** A design confirmed in the Visual Editor, attached to the line as its artwork (proof until the PDF pipeline exists). */
final case class DesignArtwork(
    designId: DesignId,
    name: String,
    proofDataUrl: String,
    minimumDpi: Option[Int],
    bleedMm: Double,
    pageCount: Int,
    trimSize: Dimension2D,
)

enum QuoteStatus:
  case Loading
  case Ready(result: ProductBuilderQuoteResult)
  case Failed(error: StorefrontError)

/** Feedback phase of the "Add to cart" CTA (idle → adding → added ✓ → idle). */
enum CartFeedback derives CanEqual:
  case Idle, Adding, Added

/** Reactive controller driving the shared Product Configurator (for both Storefront SPA and Calculator Widget). */
final class ConfiguratorController(
    val client: StorefrontClient,
    val catalogRef: SignalRef[ProductCatalog],
    val i18n: AppI18n,
    val currency: Signal[Currency],
    val displayCurrency: SignalRef[Currency],
    val sessionToken: Signal[Option[SessionToken]],
    val draft: SignalRef[DraftConfig],
    val quoteStatus: SignalRef[QuoteStatus],
    val compatibleFinishesMap: SignalRef[Map[ComponentKey, List[Finish]]],
    val finishLimitsMap: SignalRef[Map[ComponentKey, Map[FinishType, BigDecimal]]],
    private val seqRef: SignalRef[Long],
    val materialPicker: mpbuilder.ui.components.ComboBox.Handle,
    /** Mobile/tablet price dock: expanded into the full price-detail sheet. */
    val dockExpanded: SignalRef[Boolean],
    /** Add-to-cart button feedback shared by the desktop sidebar and the mobile dock. */
    val cartFeedback: SignalRef[CartFeedback],
    val draftSessionId: NonEmptyString = NonEmptyString.unsafe("draft-session-ui"),
):

  def setDisplayCurrency(cur: Currency): Unit < Async =
    for
      prev <- displayCurrency.get
      _ <- if prev == cur then Kyo.unit
           else displayCurrency.set(cur).andThen(recompute)
    yield ()

  def toggleDock: Unit < Async = dockExpanded.updateAndGet(!_).unit

  def collapseDock: Unit < Async = dockExpanded.set(false)

  /** Runs the host's add-to-cart action with button feedback; a success collapses the dock and briefly shows "Added ✓". */
  def runAddToCart(action: => Boolean < Async): Unit < Async =
    cartFeedback.get.flatMap {
      case CartFeedback.Adding => Kyo.unit
      case _ =>
        for
          _  <- cartFeedback.set(CartFeedback.Adding)
          ok <- action
          _  <- if ok then onAdded else cartFeedback.set(CartFeedback.Idle)
        yield ()
    }

  private def onAdded: Unit < Async =
    for
      _ <- cartFeedback.set(CartFeedback.Added)
      _ <- dockExpanded.set(false)
      _ <- Fiber.initUnscoped(Async.sleep(ConfiguratorController.AddedFeedback).andThen(resetAddedFeedback))
    yield ()

  private def resetAddedFeedback: Unit < Async =
    cartFeedback.get.flatMap(s => if s == CartFeedback.Added then cartFeedback.set(CartFeedback.Idle) else Kyo.unit)

  def currentCategory(d: DraftConfig, catalog: ProductCatalog): Option[ProductCategory] =
    catalog.categories.get(d.categoryId).filter(_.enabled)

  def updateCatalog(next: ProductCatalog): Unit < Async =
    for
      _ <- catalogRef.set(next)
      d <- draft.get
      adjusted =
        next.categories.get(d.categoryId).filter(_.enabled) match
          case None =>
            ConfiguratorController
              .firstEnabledCategory(next)
              .map(cat => ConfiguratorController.initialDraftForCategory(cat, next))
              .getOrElse(d)
          case Some(cat) =>
            val updatedComps = d.components.map { comp =>
              val tplOpt = cat.templateFor(comp.key)
              val nextMatId =
                if next.materials.get(comp.materialId).exists(_.enabled) then comp.materialId
                else
                  tplOpt
                    .map(_.allowedMaterialIds)
                    .getOrElse(next.materials.keySet)
                    .find(id => next.materials.get(id).exists(_.enabled))
                    .getOrElse(comp.materialId)
              val nextPmId =
                if tplOpt.forall(_.printable) then
                  comp.printingMethodId.filter(id => next.printingMethods.get(id).exists(_.enabled)).orElse {
                    val allowed = tplOpt.flatMap(_.allowedPrintingMethodIds).getOrElse(cat.allowedPrintingMethodIds)
                    val pool    = if allowed.nonEmpty then allowed else next.printingMethods.keySet
                    pool.find(id => next.printingMethods.get(id).exists(_.enabled))
                  }
                else None
              val nextFinishes = comp.finishes.filter(f => next.finishes.get(f.finishId).exists(_.enabled))
              comp.copy(materialId = nextMatId, printingMethodId = nextPmId, finishes = nextFinishes)
            }
            val nextPreset = d.activePresetId.filter(pid =>
              cat.presets.find(_.id == pid).exists(ConfiguratorController.isPresetAvailable(_, next))
            )
            d.copy(activePresetId = nextPreset, components = updatedComps)
      _ <- if adjusted != d then draft.set(adjusted) else Kyo.unit
      _ <- recompute
    yield ()

  def selectCategory(catId: CategoryId): Unit < Async =
    catalogRef.get.flatMap { catalog =>
      catalog.categories.get(catId).filter(_.enabled) match
        case None => Kyo.unit
        case Some(cat) =>
          val next = ConfiguratorController.initialDraftForCategory(cat, catalog)
          draft.set(next).andThen(recompute)
    }

  def applyPreset(catId: CategoryId, presetId: PresetId): Unit < Async =
    catalogRef.get.flatMap { catalog =>
      catalog.categories.get(catId).filter(_.enabled) match
        case None => Kyo.unit
        case Some(cat) =>
          cat.presets.find(p => p.id == presetId && ConfiguratorController.isPresetAvailable(p, catalog)) match
            case None => selectCategory(catId)
            case Some(preset) =>
              val next = ConfiguratorController.draftFromPreset(cat, preset)
              draft.set(next).andThen(recompute)
    }

  def selectComponentTab(key: ComponentKey): Unit < Sync =
    draft.updateAndGet(_.copy(activeComponentKey = key)).unit

  def updateComponent(key: ComponentKey)(f: DraftComponent => DraftComponent): Unit < Async =
    for
      _ <- draft.updateAndGet { d =>
             d.copy(
               activePresetId = None,
               components = d.components.map(c => if c.key == key then f(c) else c),
             )
           }
      _ <- recompute
    yield ()

  def updateMaterial(key: ComponentKey, materialId: MaterialId): Unit < Async =
    updateComponent(key)(c => c.copy(materialId = materialId))

  def updatePrintingMethod(key: ComponentKey, methodId: PrintingMethodId): Unit < Async =
    catalogRef.get.flatMap { catalog =>
      val methodOpt = catalog.printingMethods.get(methodId)
      updateComponent(key) { c =>
        val adjustedInk = (c.inkConfiguration, methodOpt) match
          case (Some(ink), Some(m)) if !m.supportsDoubleSided && ink.isDoubleSided =>
            Some(ink.copy(back = SideInk.blank))
          case (None, Some(_)) =>
            Some(InkConfiguration.cmyk4_0)
          case (existing, _) =>
            existing
        c.copy(printingMethodId = Some(methodId), inkConfiguration = adjustedInk)
      }
    }

  def updateInk(key: ComponentKey, ink: InkConfiguration): Unit < Async =
    updateComponent(key)(_.copy(inkConfiguration = Some(ink)))

  def toggleFinish(key: ComponentKey, finish: Finish): Unit < Async =
    updateComponent(key) { c =>
      if c.finishes.exists(_.finishId == finish.id) then
        c.copy(finishes = c.finishes.filterNot(_.finishId == finish.id))
      else
        val defaultSide = finish.sidePolicy match
          case FinishSidePolicy.NotApplicable      => None
          case FinishSidePolicy.Fixed(s)           => Some(s)
          case FinishSidePolicy.Selectable(_, defS) => Some(defS)
        val defaultParams = ConfiguratorController.defaultParamsFor(finish.paramSchema)
        c.copy(finishes = c.finishes :+ FinishSelection(finish.id, defaultSide, defaultParams))
    }

  def updateFinishSide(key: ComponentKey, finishId: FinishId, side: FinishSide): Unit < Async =
    updateComponent(key) { c =>
      c.copy(finishes = c.finishes.map(f => if f.finishId == finishId then f.copy(requestedSide = Some(side)) else f))
    }

  def updateFinishParams(key: ComponentKey, finishId: FinishId, params: FinishParams): Unit < Async =
    updateComponent(key) { c =>
      c.copy(finishes = c.finishes.map(f => if f.finishId == finishId then f.copy(params = Some(params)) else f))
    }

  def updateSpecs(f: ProductSpecs => ProductSpecs): Unit < Async =
    for
      _ <- draft.updateAndGet(d => d.copy(activePresetId = None, specs = f(d.specs)))
      _ <- recompute
    yield ()

  def updateSpeed(speed: ManufacturingSpeed): Unit < Async =
    for
      _ <- draft.updateAndGet(_.copy(speed = speed))
      _ <- recompute
    yield ()

  def uploadDraftArtwork(
      key: ComponentKey,
      file: BrowserPickedFile,
  ): Option[ArtworkSubmissionView] < Async =
    for
      d   <- draft.get
      tok <- sessionToken.current
      expectedTrim = d.specs.dimensions.finishedBoundingBox
      expectedPages = d.specs.binding.map(_.totalPages)
      payload = ArtworkComponents.toUploadPayload(file, expectedTrim, expectedPages)
      // Delete previous draft artwork for this component if replacing
      _ <- d.artworks.get(key) match
             case Some(prev) => Abort.run[StorefrontError](client.deleteDraftArtwork(prev.id)).unit
             case None       => Kyo.unit
      res <- Abort.run[StorefrontError](
               client.uploadDraftArtwork(
                 sessionId = draftSessionId,
                 componentKey = key,
                 expectedTrim = expectedTrim,
                 expectedPages = expectedPages,
                 payload = payload,
                 sessionToken = tok,
               )
             )
      out <- res match
               case Result.Success(view) =>
                 draft.updateAndGet(cur => cur.copy(artworks = cur.artworks.updated(key, view), designs = cur.designs - key)).map(_ => Some(view))
               case _ =>
                 Kyo.lift(None)
    yield out

  /** Attaches a Visual Editor design as the component's draft artwork (replacing any previous file). */
  def attachDesignArtwork(key: ComponentKey, design: DesignArtwork): Option[ArtworkSubmissionView] < Async =
    for
      d   <- draft.get
      tok <- sessionToken.current
      expectedTrim  = d.specs.dimensions.finishedBoundingBox
      expectedPages = d.specs.binding.map(_.totalPages)
      safeName      = design.name.trim.filter(c => c.isLetterOrDigit || c == ' ' || c == '_' || c == '.' || c == '-').take(60)
      payload = ArtworkUploadPayload(
                  fileName = NonEmptyString.unsafe(s"${if safeName.isEmpty then "design" else safeName}.design.svg"),
                  mimeType = NonEmptyString.unsafe("image/svg+xml"),
                  sizeBytes = design.proofDataUrl.length.toLong.max(1L),
                  downloadUrl = NonEmptyString(design.proofDataUrl).toOption,
                  previewUrl = NonEmptyString(design.proofDataUrl).toOption,
                  resolutionDpi = design.minimumDpi.getOrElse(300).max(72).min(1200),
                  bleedMm = math.round(design.bleedMm).toInt,
                  // Browser designs are sRGB; the server PDF pipeline converts to the press CMYK profile.
                  colorSpace = ColorSpace.RGB,
                  pageCount = expectedPages.map(_ => design.pageCount),
                  fontsEmbedded = true,
                  trimSizeOverride = Some(design.trimSize),
                )
      _ <- d.artworks.get(key) match
             case Some(prev) => Abort.run[StorefrontError](client.deleteDraftArtwork(prev.id)).unit
             case None       => Kyo.unit
      res <- Abort.run[StorefrontError](
               client.uploadDraftArtwork(
                 sessionId = draftSessionId,
                 componentKey = key,
                 expectedTrim = expectedTrim,
                 expectedPages = expectedPages,
                 payload = payload,
                 sessionToken = tok,
               )
             )
      out <- res match
               case Result.Success(view) =>
                 draft
                   .updateAndGet(cur => cur.copy(artworks = cur.artworks.updated(key, view), designs = cur.designs.updated(key, design.designId)))
                   .map(_ => Some(view))
               case _ => Kyo.lift(None)
    yield out

  def removeDraftArtwork(key: ComponentKey): Unit < Async =
    for
      d <- draft.get
      _ <- d.artworks.get(key) match
             case Some(existing) => Abort.run[StorefrontError](client.deleteDraftArtwork(existing.id)).unit
             case None           => Kyo.unit
      _ <- draft.updateAndGet(cur => cur.copy(artworks = cur.artworks - key, designs = cur.designs - key)).unit
    yield ()

  def clearDraftArtworks: Unit < Sync =
    draft.updateAndGet(_.copy(artworks = Map.empty, designs = Map.empty)).unit

  def buildConfigureCmd(d: DraftConfig): ConfigureProductCmd < Sync =
    for
      catalog <- catalogRef.get
      seq     <- seqRef.updateAndGet(_ + 1)
    yield
      val nonEmptyComps = NonEmptyChunk.fromIterableOption(
        d.components.map { c =>
          ComponentSelection(
            key = c.key,
            materialId = c.materialId,
            printingMethodId = c.printingMethodId,
            inkConfiguration = c.inkConfiguration,
            finishes = c.finishes,
          )
        }
      ).getOrElse(
        NonEmptyChunk.single(
          ComponentSelection(
            ComponentKey.Main,
            catalog.materials.values.find(_.enabled).map(_.id).getOrElse(catalog.materials.keys.head),
          )
        )
      )
      ConfigureProductCmd(
        configurationId = ConfigurationId.unsafe(s"cfg-ui-$seq"),
        categoryId = d.categoryId,
        components = nonEmptyComps,
        specs = d.specs,
      )

  /** Refreshes compatible finishes, parameter limits, and live dual-price quote via `StorefrontClient`. */
  def recompute: Unit < Async =
    for
      d     <- draft.get
      cur   <- displayCurrency.get
      tok   <- sessionToken.current
      lang  <- i18n.currentLanguage
      _     <- refreshComponentCapabilities(d)
      // Prune finishes that became incompatible after material/method change
      finMap <- compatibleFinishesMap.get
      pruned = d.copy(
                 components = d.components.map { c =>
                   val allowed = finMap.getOrElse(c.key, Nil).map(_.id).toSet
                   c.copy(finishes = c.finishes.filter(f => allowed.contains(f.finishId)))
                 }
               )
      _     <- if pruned.components != d.components then draft.set(pruned) else Kyo.unit
      cmd   <- buildConfigureCmd(pruned)
      res   <- Abort.run[StorefrontError](
                 client.configureAndQuote(
                   cmd = cmd,
                   currency = cur,
                   speed = pruned.speed,
                   sessionToken = tok,
                   promoCode = None,
                   lang = lang,
                 )
               )
      _     <- res match
                 case Result.Success(quoted) => quoteStatus.set(QuoteStatus.Ready(quoted))
                 case Result.Failure(err)    => quoteStatus.set(QuoteStatus.Failed(err))
                 case Result.Panic(ex)       =>
                   quoteStatus.set(
                     QuoteStatus.Failed(
                       StorefrontError.CatalogFailure(
                         CatalogError.RuleViolation(
                           RuleId.unsafe("ui-runtime"),
                           LocalizedString(Option(ex.getMessage).getOrElse("Unexpected error")),
                         )
                       )
                     )
                   )
    yield ()

  private def refreshComponentCapabilities(d: DraftConfig): Unit < Async =
    for
      entries <- Kyo.foreach(d.components) { comp =>
                   val selectedIds = comp.finishes.map(_.finishId).toSet
                   for
                     finRes <- Abort.run[StorefrontError](
                                 client.compatibleFinishes(
                                   categoryId = d.categoryId,
                                   componentKey = comp.key,
                                   materialId = comp.materialId,
                                   printingMethodId = comp.printingMethodId,
                                   alreadySelectedFinishIds = selectedIds,
                                 )
                               )
                     limRes <- Abort.run[StorefrontError](
                                 client.finishParameterLimits(
                                   categoryId = d.categoryId,
                                   componentKey = comp.key,
                                   materialId = comp.materialId,
                                   printingMethodId = comp.printingMethodId,
                                 )
                               )
                   yield (
                     comp.key -> finRes.getOrElse(Nil),
                     comp.key -> limRes.getOrElse(Map.empty),
                   )
                 }
      _ <- compatibleFinishesMap.set(entries.map(_._1).toMap)
      _ <- finishLimitsMap.set(entries.map(_._2).toMap)
    yield ()

object ConfiguratorController:

  /** How long the add-to-cart button shows its "Added ✓" state. */
  val AddedFeedback: Duration = 1800.millis

  def firstEnabledCategory(catalog: ProductCatalog): Option[ProductCategory] =
    catalog.showcases.values.toList
      .filter(_.enabled)
      .sortBy(_.sortOrder)
      .flatMap(s => catalog.categories.get(s.categoryId).filter(_.enabled))
      .headOption
      .orElse(catalog.categories.values.find(_.enabled))

  def isPresetAvailable(preset: CategoryPreset, catalog: ProductCatalog): Boolean =
    preset.components.forall { cp =>
      catalog.materials.get(cp.materialId).exists(_.enabled) &&
      cp.printingMethodId.forall(id => catalog.printingMethods.get(id).exists(_.enabled)) &&
      cp.finishes.forall(f => catalog.finishes.get(f.finishId).exists(_.enabled))
    }

  def init(
      client: StorefrontClient,
      catalog: ProductCatalog,
      i18n: AppI18n,
      currency: Signal[Currency],
      sessionToken: Signal[Option[SessionToken]],
      initialCategoryId: Option[CategoryId] = None,
      initialPresetId: Option[PresetId] = None,
  ): ConfiguratorController < Async =
    val defaultCat =
      initialCategoryId
        .flatMap(catalog.categories.get)
        .filter(_.enabled)
        .orElse(firstEnabledCategory(catalog))
        .orElse(catalog.categories.values.headOption)
        .get

    val initDraft = initialPresetId
      .flatMap(pid => defaultCat.presets.find(p => p.id == pid && isPresetAvailable(p, catalog)))
      .map(p => draftFromPreset(defaultCat, p))
      .getOrElse(initialDraftForCategory(defaultCat, catalog))

    for
      catalogRef <- Signal.initRef(catalog)
      initCur    <- currency.current
      dispCurRef <- Signal.initRef[Currency](if initCur == Currency.EUR then Currency.EUR else Currency.CZK)
      draftRef   <- Signal.initRef(initDraft)
      quoteRef   <- Signal.initRef[QuoteStatus](QuoteStatus.Loading)
      finMapRef  <- Signal.initRef(Map.empty[ComponentKey, List[Finish]])
      limMapRef  <- Signal.initRef(Map.empty[ComponentKey, Map[FinishType, BigDecimal]])
      seqRef     <- Signal.initRef(0L)
      matPicker  <- mpbuilder.ui.components.ComboBox.Handle.make
      dockRef    <- Signal.initRef(false)
      feedbackRef <- Signal.initRef[CartFeedback](CartFeedback.Idle)
      ctrl        = new ConfiguratorController(
                      client,
                      catalogRef,
                      i18n,
                      currency,
                      dispCurRef,
                      sessionToken,
                      draftRef,
                      quoteRef,
                      finMapRef,
                      limMapRef,
                      seqRef,
                      matPicker,
                      dockRef,
                      feedbackRef,
                    )
      _          <- ctrl.recompute
    yield ctrl

  def draftFromPreset(cat: ProductCategory, preset: CategoryPreset): DraftConfig =
    val comps = preset.components.toList.map { cp =>
      DraftComponent(
        key = cp.key,
        materialId = cp.materialId,
        printingMethodId = cp.printingMethodId,
        inkConfiguration = cp.inkConfiguration,
        finishes = cp.finishes,
      )
    }
    DraftConfig(
      categoryId = cat.id,
      activePresetId = Some(preset.id),
      activeComponentKey = comps.headOption.map(_.key).getOrElse(ComponentKey.Main),
      components = comps,
      specs = preset.defaultSpecs,
      speed = ManufacturingSpeed.Standard,
    )

  def initialDraftForCategory(cat: ProductCategory, catalog: ProductCatalog): DraftConfig =
    cat.presets.find(isPresetAvailable(_, catalog)) match
      case Some(preset) => draftFromPreset(cat, preset)
      case None =>
        val comps = cat.components.toList.map { tpl =>
          val matId = tpl.allowedMaterialIds
            .find(id => catalog.materials.get(id).exists(_.enabled))
            .orElse(tpl.allowedMaterialIds.headOption)
            .getOrElse(catalog.materials.keys.head)
          val pmOpt =
            if tpl.printable then
              val allowed = tpl.allowedPrintingMethodIds.getOrElse(cat.allowedPrintingMethodIds)
              val pool    = if allowed.nonEmpty then allowed else catalog.printingMethods.keySet
              pool
                .find(id => catalog.printingMethods.get(id).exists(_.enabled))
                .orElse(pool.headOption)
            else None
          val inkOpt = if tpl.printable then Some(InkConfiguration.cmyk4_0) else None
          DraftComponent(tpl.key, matId, pmOpt, inkOpt, Nil)
        }
        val dims: ProductDimensions =
          if cat.specRequirements.requiresFoldedDimensions then
            ProductDimensions.Folded2D(Dimension2D.A5, FoldType.Half)
          else ProductDimensions.Flat2D(Dimension2D.A5)
        val bindingOpt =
          if cat.specRequirements.requiresBinding then Some(BindingSpec(BindingMethod.SaddleStitch, 16))
          else None
        DraftConfig(
          categoryId = cat.id,
          activePresetId = None,
          activeComponentKey = comps.headOption.map(_.key).getOrElse(ComponentKey.Main),
          components = comps,
          specs = ProductSpecs(
            dimensions = dims,
            quantity = Quantity.unsafe(100),
            orientation = if cat.specRequirements.requiresOrientation then Some(Orientation.Portrait) else None,
            binding = bindingOpt,
          ),
          speed = ManufacturingSpeed.Standard,
        )

  def defaultParamsFor(schema: FinishParamSchema): Option[FinishParams] = schema match
    case FinishParamSchema.NoParams => None
    case FinishParamSchema.RoundCornersSchema(counts, minR, _) =>
      Some(FinishParams.RoundCorners(counts.toList.sorted.lastOption.getOrElse(4), math.max(minR, 3)))
    case FinishParamSchema.FoilColorSchema(colors) =>
      Some(FinishParams.FoilStamping(colors.headOption.getOrElse(FoilColor.Gold)))
    case FinishParamSchema.GrommetSpacingSchema(spacings) =>
      Some(FinishParams.Grommets(spacings.toList.sorted.headOption.getOrElse(500)))
    case FinishParamSchema.PerforationSchema(minP, _) =>
      Some(FinishParams.Perforation(math.max(minP, 50)))
    case FinishParamSchema.LinearLengthSchema(minM, _) =>
      Some(FinishParams.LinearLength(minM.max(BigDecimal(1))))
    case FinishParamSchema.CreaseCountSchema(minC, _) =>
      Some(FinishParams.Scoring(math.max(minC, 1)))
