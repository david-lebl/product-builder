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
import zio.NonEmptyChunk

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
)

enum QuoteStatus:
  case Loading
  case Ready(result: ProductBuilderQuoteResult)
  case Failed(error: StorefrontError)

/** Reactive controller driving the shared Product Configurator (for both Storefront SPA and Calculator Widget). */
final class ConfiguratorController(
    val client: StorefrontClient,
    val catalog: ProductCatalog,
    val i18n: AppI18n,
    val currency: Signal[Currency],
    val sessionToken: Signal[Option[SessionToken]],
    val draft: SignalRef[DraftConfig],
    val quoteStatus: SignalRef[QuoteStatus],
    val compatibleFinishesMap: SignalRef[Map[ComponentKey, List[Finish]]],
    val finishLimitsMap: SignalRef[Map[ComponentKey, Map[FinishType, BigDecimal]]],
    private val seqRef: SignalRef[Long],
    val draftSessionId: NonEmptyString = NonEmptyString.unsafe("draft-session-ui"),
):

  def currentCategory(d: DraftConfig): Option[ProductCategory] =
    catalog.categories.get(d.categoryId)

  def selectCategory(catId: CategoryId): Unit < Async =
    catalog.categories.get(catId) match
      case None => Kyo.unit
      case Some(cat) =>
        val next = ConfiguratorController.initialDraftForCategory(cat, catalog)
        draft.set(next).andThen(recompute)

  def applyPreset(catId: CategoryId, presetId: PresetId): Unit < Async =
    catalog.categories.get(catId) match
      case None => Kyo.unit
      case Some(cat) =>
        cat.presets.find(_.id == presetId) match
          case None => selectCategory(catId)
          case Some(preset) =>
            val next = ConfiguratorController.draftFromPreset(cat, preset)
            draft.set(next).andThen(recompute)

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
                 draft.updateAndGet(cur => cur.copy(artworks = cur.artworks.updated(key, view))).map(_ => Some(view))
               case _ =>
                 Kyo.lift(None)
    yield out

  def removeDraftArtwork(key: ComponentKey): Unit < Async =
    for
      d <- draft.get
      _ <- d.artworks.get(key) match
             case Some(existing) => Abort.run[StorefrontError](client.deleteDraftArtwork(existing.id)).unit
             case None           => Kyo.unit
      _ <- draft.updateAndGet(cur => cur.copy(artworks = cur.artworks - key)).unit
    yield ()

  def clearDraftArtworks: Unit < Sync =
    draft.updateAndGet(_.copy(artworks = Map.empty)).unit

  def buildConfigureCmd(d: DraftConfig): ConfigureProductCmd < Sync =
    seqRef.updateAndGet(_ + 1).map { seq =>
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
          ComponentSelection(ComponentKey.Main, catalog.materials.keys.head)
        )
      )
      ConfigureProductCmd(
        configurationId = ConfigurationId.unsafe(s"cfg-ui-$seq"),
        categoryId = d.categoryId,
        components = nonEmptyComps,
        specs = d.specs,
      )
    }

  /** Refreshes compatible finishes, parameter limits, and live dual-price quote via `StorefrontClient`. */
  def recompute: Unit < Async =
    for
      d     <- draft.get
      cur   <- currency.current
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
        .orElse(catalog.showcases.values.toList.sortBy(_.sortOrder).headOption.flatMap(s => catalog.categories.get(s.categoryId)))
        .orElse(catalog.categories.values.headOption)
        .get

    val initDraft = initialPresetId
      .flatMap(pid => defaultCat.presets.find(_.id == pid))
      .map(p => draftFromPreset(defaultCat, p))
      .getOrElse(initialDraftForCategory(defaultCat, catalog))

    for
      draftRef   <- Signal.initRef(initDraft)
      quoteRef   <- Signal.initRef[QuoteStatus](QuoteStatus.Loading)
      finMapRef  <- Signal.initRef(Map.empty[ComponentKey, List[Finish]])
      limMapRef  <- Signal.initRef(Map.empty[ComponentKey, Map[FinishType, BigDecimal]])
      seqRef     <- Signal.initRef(0L)
      ctrl        = new ConfiguratorController(
                      client,
                      catalog,
                      i18n,
                      currency,
                      sessionToken,
                      draftRef,
                      quoteRef,
                      finMapRef,
                      limMapRef,
                      seqRef,
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
    cat.presets.headOption match
      case Some(preset) => draftFromPreset(cat, preset)
      case None =>
        val comps = cat.components.toList.map { tpl =>
          val matId = tpl.allowedMaterialIds.headOption.getOrElse(catalog.materials.keys.head)
          val pmOpt =
            if tpl.printable then
              tpl.allowedPrintingMethodIds
                .flatMap(_.headOption)
                .orElse(cat.allowedPrintingMethodIds.headOption)
                .orElse(catalog.printingMethods.keys.headOption)
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
