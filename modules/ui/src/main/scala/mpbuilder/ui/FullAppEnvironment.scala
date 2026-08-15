package mpbuilder.ui

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.{BuilderEnvironment, BuilderState, BasketPrimaryAction, QuickOrderAction, ProductBuilderViewModel}
import mpbuilder.domain.model.*
import mpbuilder.domain.manufacturing.StationUtilisation
import mpbuilder.domain.pricing.PricingContext
import mpbuilder.domain.sample.{SampleCatalog, SampleManufacturing}
import mpbuilder.domain.service.{BusyPeriodFilter, CompletionEstimator, UtilisationCalculator}

/** The product configurator as hosted by the full SPA: simulated shop-floor
  * queue data drives concrete completion dates, surge pricing and the Express
  * availability gate, artwork goes through the visual editor, and the basket
  * leads into the checkout wizard.
  */
object FullAppEnvironment:

  // ── Simulated station utilisation ─────────────────────────────────────
  // Stands in for a real shop-floor feed until the manufacturing backend exists.

  private val simulatedStationUtilisation: List[StationUtilisation] = List(
    StationUtilisation(StationType.Prepress,        queueDepth = 3, inProgressCount = 1, machineCount = 2, avgProcessingTimeMs = 1800000L, estimatedClearTimeMs = 3600000L),
    StationUtilisation(StationType.DigitalPrinter,   queueDepth = 5, inProgressCount = 2, machineCount = 3, avgProcessingTimeMs = 900000L,  estimatedClearTimeMs = 2700000L),
    StationUtilisation(StationType.OffsetPress,      queueDepth = 4, inProgressCount = 1, machineCount = 2, avgProcessingTimeMs = 2700000L, estimatedClearTimeMs = 5400000L),
    StationUtilisation(StationType.LargeFormatPrinter, queueDepth = 2, inProgressCount = 1, machineCount = 1, avgProcessingTimeMs = 1200000L, estimatedClearTimeMs = 3600000L),
    StationUtilisation(StationType.Cutter,           queueDepth = 6, inProgressCount = 2, machineCount = 2, avgProcessingTimeMs = 300000L,  estimatedClearTimeMs = 1200000L),
    StationUtilisation(StationType.Laminator,        queueDepth = 2, inProgressCount = 1, machineCount = 1, avgProcessingTimeMs = 600000L,  estimatedClearTimeMs = 1800000L),
    StationUtilisation(StationType.Folder,           queueDepth = 3, inProgressCount = 1, machineCount = 1, avgProcessingTimeMs = 300000L,  estimatedClearTimeMs = 1200000L),
    StationUtilisation(StationType.Binder,           queueDepth = 2, inProgressCount = 1, machineCount = 1, avgProcessingTimeMs = 600000L,  estimatedClearTimeMs = 1800000L),
    StationUtilisation(StationType.QualityControl,   queueDepth = 4, inProgressCount = 1, machineCount = 2, avgProcessingTimeMs = 900000L,  estimatedClearTimeMs = 2700000L),
    StationUtilisation(StationType.Packaging,        queueDepth = 3, inProgressCount = 1, machineCount = 2, avgProcessingTimeMs = 600000L,  estimatedClearTimeMs = 1800000L),
  )

  private val simulatedQueueState: Map[StationType, CompletionEstimator.StationQueueState] = Map(
    StationType.Prepress         -> CompletionEstimator.StationQueueState(queueDepth = 3,  avgProcessingTimeMinutes = 30, activeMachineCount = 2),
    StationType.DigitalPrinter   -> CompletionEstimator.StationQueueState(queueDepth = 5,  avgProcessingTimeMinutes = 15, activeMachineCount = 3),
    StationType.OffsetPress      -> CompletionEstimator.StationQueueState(queueDepth = 4,  avgProcessingTimeMinutes = 45, activeMachineCount = 2),
    StationType.LargeFormatPrinter -> CompletionEstimator.StationQueueState(queueDepth = 2, avgProcessingTimeMinutes = 20, activeMachineCount = 1),
    StationType.Cutter           -> CompletionEstimator.StationQueueState(queueDepth = 6,  avgProcessingTimeMinutes = 5,  activeMachineCount = 2),
    StationType.Laminator        -> CompletionEstimator.StationQueueState(queueDepth = 2,  avgProcessingTimeMinutes = 10, activeMachineCount = 1),
    StationType.Folder           -> CompletionEstimator.StationQueueState(queueDepth = 3,  avgProcessingTimeMinutes = 5,  activeMachineCount = 1),
    StationType.Binder           -> CompletionEstimator.StationQueueState(queueDepth = 2,  avgProcessingTimeMinutes = 10, activeMachineCount = 1),
    StationType.QualityControl   -> CompletionEstimator.StationQueueState(queueDepth = 4,  avgProcessingTimeMinutes = 15, activeMachineCount = 2),
    StationType.Packaging        -> CompletionEstimator.StationQueueState(queueDepth = 3,  avgProcessingTimeMinutes = 10, activeMachineCount = 2),
  )

  /** Derive the station types a product configuration passes through.
    *
    * Station sequence: Prepress → Printer → [Laminator] → Cutter → [Folder] → [Binder] → QC → Packaging.
    * Banners and roll-ups use LargeFormatPrinter; all other categories use DigitalPrinter.
    * Laminator is added when any component has a lamination or overlamination finish.
    * Folder is added for folded products (FoldTypeSpec present).
    * Binder is added for bound products (BindingMethodSpec present).
    */
  def deriveStepTypes(config: ProductConfiguration): List[StationType] =
    val steps = List.newBuilder[StationType]
    steps += StationType.Prepress

    // Printing station: use large format for banners and roll-ups
    val largeFormatCategories = Set(
      SampleCatalog.bannersId,
      SampleCatalog.rollUpsId,
    )
    if largeFormatCategories.contains(config.category.id) then
      steps += StationType.LargeFormatPrinter
    else
      steps += StationType.DigitalPrinter

    // Finishes
    val hasLamination = config.components.exists(_.finishes.exists(f =>
      f.finishType == FinishType.Lamination || f.finishType == FinishType.Overlamination
    ))
    if hasLamination then steps += StationType.Laminator

    // Cutting
    steps += StationType.Cutter

    // Folding / Binding
    val hasFold = config.specifications.specs.values.exists {
      case SpecValue.FoldTypeSpec(_) => true
      case _ => false
    }
    if hasFold then steps += StationType.Folder

    val hasBinding = config.specifications.specs.values.exists {
      case SpecValue.BindingMethodSpec(_) => true
      case _ => false
    }
    if hasBinding then steps += StationType.Binder

    // QC + Packaging
    steps += StationType.QualityControl
    steps += StationType.Packaging
    steps.result()

  /** Global utilisation from simulated stations. */
  val globalUtilisation: BigDecimal =
    UtilisationCalculator.computeGlobalUtilisation(simulatedStationUtilisation)

  private def completionText(speed: ManufacturingSpeed, s: BuilderState, lang: Language): Option[String] =
    s.configuration.map { config =>
      val quantity = s.specifications.collectFirst { case SpecValue.QuantitySpec(q) => q.value }.getOrElse(1)
      val now = ProductBuilderViewModel.currentLocalDateTime
      CompletionEstimator
        .estimate(
          steps = deriveStepTypes(config),
          quantity = quantity,
          speed = speed,
          stationEstimates = SampleManufacturing.stationTimeEstimates,
          stationQueues = simulatedQueueState,
          schedule = SampleManufacturing.shopSchedule,
          orderTime = now,
        )
        .formatEarliest(now, lang)
    }

  /** Build a dynamic PricingContext from current utilisation and active busy periods. */
  private def pricingContext(): PricingContext =
    val now = ProductBuilderViewModel.currentLocalDateTime
    val activeBusyPeriods = BusyPeriodFilter.filterActive(SampleManufacturing.busyPeriodMultipliers, now)
    UtilisationCalculator.buildPricingContext(
      stations = simulatedStationUtilisation,
      activeBusyPeriods = activeBusyPeriods,
      currentTimeMillis = System.currentTimeMillis(),
    )

  val environment: BuilderEnvironment = BuilderEnvironment(
    pricingContext = () => pricingContext(),
    completionText = completionText,
    expressAvailable = Val(UtilisationCalculator.isExpressAvailable(globalUtilisation)),
    artwork = Some(VisualEditorArtwork),
    basketOpen = AppRouter.basketOpen,
    basketPrimaryAction = BasketPrimaryAction(
      label = {
        case Language.En => "Proceed to Checkout →"
        case Language.Cs => "Přejít k pokladně →"
      },
      onClick = () => {
        AppRouter.basketOpen.set(false)
        ProductBuilderViewModel.startCheckout()
        AppRouter.navigateTo(AppRoute.Checkout)
      },
    ),
    quickOrderAction = QuickOrderAction(
      label = {
        case Language.En => "Order Now →"
        case Language.Cs => "Objednat nyní →"
      },
      enabled = ProductBuilderViewModel.state.map(_.configuration.isDefined),
      onClick = quantity => {
        ProductBuilderViewModel.startQuickCheckout(quantity)
        AppRouter.navigateTo(AppRoute.Checkout)
      },
    ),
  )
