package mpbuilder.sample

import mpbuilder.artwork.{ColorSpace, PreflightRequirement}
import mpbuilder.catalog.*
import mpbuilder.manufacturing.*
import mpbuilder.ordering.{LinePriceSnapshot, ProductSnapshot}
import mpbuilder.pricing.*
import mpbuilder.shared.*

/** Pure Anti-Corruption Layer (ACL) translators between bounded contexts.
  *
  * Each bounded context (`catalog`, `pricing`, `artwork`, `ordering`, `manufacturing`)
  * owns its own input/snapshot types and has ZERO compile-time dependency on any other
  * context. `DomainBridges` maps public outputs of one context to public inputs of another.
  */
object DomainBridges:

  /** Converts a `CategoryPreset` into a ready-to-execute `ConfigureProductCmd`. */
  def presetToConfigureCmd(
      configId: ConfigurationId,
      categoryId: CategoryId,
      preset: CategoryPreset,
      specsOverride: Option[ProductSpecs] = None,
  ): ConfigureProductCmd =
    ConfigureProductCmd(
      configurationId = configId,
      categoryId = categoryId,
      components = preset.components.map { cp =>
        ComponentSelection(
          key = cp.key,
          materialId = cp.materialId,
          printingMethodId = cp.printingMethodId,
          inkConfiguration = cp.inkConfiguration,
          finishes = cp.finishes,
        )
      },
      specs = specsOverride.getOrElse(preset.defaultSpecs),
    )

  /** Translates a validated `ConfiguredProduct` from `catalog` into a `PricingSubject` for `pricing`. */
  def toPricingSubject(
      product: ConfiguredProduct,
      speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
  ): PricingSubject =
    val pricingComponents = product.components.map { comp =>
      val (stockFormatId, sheetsUsed, cutsPerSheet) = comp.geometry.imposition match
        case ImpositionResult.SheetImposition(fmtId, _, _, sheets, cuts, _) =>
          (Some(fmtId), sheets, cuts)
        case ImpositionResult.RollImposition(fmtId, _, _, _, _) =>
          (Some(fmtId), 0, 0)
        case ImpositionResult.DiscreteUnit(_) =>
          (None, 0, 0)

      val pricingFinishes = comp.finishes.map { sf =>
        val (discreteParam, linearMeters) = sf.params match
          case Some(FinishParams.Scoring(cnt))      => (Some(cnt), None)
          case Some(FinishParams.Grommets(sp))      => (Some(sp), None)
          case Some(FinishParams.Perforation(p))    => (Some(p), None)
          case Some(FinishParams.RoundCorners(c, _)) => (Some(c), None)
          case Some(FinishParams.LinearLength(m))   => (None, Some(m))
          case _                                    => (None, None)

        PricingFinish(
          finishId = sf.id,
          finishTypeKey = sf.finishType.toString,
          sideMultiplier = sf.sideMultiplier,
          discreteParam = discreteParam,
          linearMeters = linearMeters,
        )
      }

      PricingComponent(
        key = comp.key,
        materialId = comp.material.id,
        stockFormatId = stockFormatId,
        printingMethodId = comp.print.map(_.printingMethod.id),
        printingProcessKey = comp.print.map(_.printingMethod.processType.toString),
        inkKey = comp.print.map(p => InkBillingKey(p.ink.front.notation, p.ink.back.notation)),
        unitsPerProduct = comp.geometry.unitsPerProduct,
        totalFlatUnits = comp.geometry.totalFlatUnits,
        sheetsUsed = sheetsUsed,
        guillotineCutsPerSheet = cutsPerSheet,
        areaPerUnitSqM = comp.geometry.areaPerUnitSqM,
        perimeterPerUnitM = comp.geometry.perimeterPerUnitM,
        finishes = pricingFinishes,
      )
    }

    PricingSubject(
      categoryId = product.categoryId,
      quantity = product.specs.quantity,
      versionCount = product.specs.versionCount,
      components = pricingComponents,
      foldKey = product.specs.dimensions.foldTypeOpt.map(_.toString),
      bindingKey = product.specs.binding.map(_.method.toString),
      speed = speed,
    )

  /** Freezes a `ConfiguredProduct` into an immutable `ProductSnapshot` for `ordering`. */
  def toProductSnapshot(
      product: ConfiguredProduct,
      summaryNotes: Option[String] = None,
  ): ProductSnapshot =
    mpbuilder.storefront.StorefrontTranslators.toProductSnapshot(product, summaryNotes)

  /** Freezes a `PriceQuote` into a `LinePriceSnapshot` for `ordering`. */
  def toLinePriceSnapshot(quote: PriceQuote): LinePriceSnapshot =
    LinePriceSnapshot(
      quoteId = quote.quoteId,
      pricelistId = quote.pricelistId,
      pricelistVersion = quote.pricelistVersion,
      currency = quote.currency,
      netAmount = quote.netTotal,
      taxRate = quote.tax.rate,
      taxAmount = quote.tax.taxAmount,
      grossAmount = quote.grossTotal,
    )

  /** Derives an automated `PreflightRequirement` for a printable component of a `ConfiguredProduct`. */
  def toPreflightRequirement(
      product: ConfiguredProduct,
      componentKey: ComponentKey,
  ): Option[PreflightRequirement] =
    product.components.find(_.key == componentKey).flatMap { comp =>
      comp.print.map { ps =>
        val isLargeFormat = ps.printingMethod.processType.isLargeFormatInkjet
        val bleed = product.specs.bleed.getOrElse(mpbuilder.storefront.StorefrontTranslators.defaultBleed(product))
        val minDpi = if isLargeFormat then 150 else 300
        val expectedPages = product.specs.binding match
          case Some(b) if componentKey == ComponentKey.Body =>
            val coverPages = if b.selfCover then 0 else 4
            Some(math.max(1, b.totalPages - coverPages))
          case Some(_) if componentKey == ComponentKey.Cover =>
            Some(if ps.ink.isDoubleSided then 2 else 1)
          case _ =>
            Some(if ps.ink.isDoubleSided then 2 else 1)

        val allowedColors =
          if ps.ink.allSpecialInks.nonEmpty then Set(ColorSpace.CMYK, ColorSpace.CMYKWithSpot, ColorSpace.SpotPantone)
          else Set(ColorSpace.CMYK, ColorSpace.Grayscale, ColorSpace.CMYKWithSpot)

        PreflightRequirement(
          expectedTrimSize = comp.geometry.finishedSize,
          dimensionToleranceMm = NonNegativeMm.unsafe(1),
          requiredBleed = bleed,
          minResolutionDpi = minDpi,
          expectedPageCount = expectedPages,
          allowedColorSpaces = allowedColors,
        )
      }
    }

  /** Translates a `ConfiguredProduct` + order reference into a `ProductionSpec` for `manufacturing`. */
  def toProductionSpec(
      orderId: OrderId,
      orderLineIndex: Int,
      product: ConfiguredProduct,
      speed: ManufacturingSpeed,
      lang: Language = Language.En,
      customerInstructions: Option[String] = None,
  ): ProductionSpec =
    val prodComponents = product.components.map { comp =>
      val printJobOpt = comp.print.map { ps =>
        val stationKind = ps.printingMethod.processType match
          case PrintingProcessType.Digital           => PrintStationKind.Digital
          case PrintingProcessType.Offset            => PrintStationKind.Offset
          case PrintingProcessType.Letterpress       => PrintStationKind.Letterpress
          case PrintingProcessType.ScreenPrint       => PrintStationKind.ScreenPrint
          case PrintingProcessType.DTG               => PrintStationKind.DTG
          case PrintingProcessType.Sublimation       => PrintStationKind.Sublimation
          case PrintingProcessType.HeatTransfer      => PrintStationKind.DTG
          case PrintingProcessType.UVCurableInkjet |
              PrintingProcessType.LatexInkjet |
              PrintingProcessType.SolventInkjet      => PrintStationKind.LargeFormatInkjet

        val (sheetsToPrint, needsCut) = comp.geometry.imposition match
          case ImpositionResult.SheetImposition(_, _, pps, sheets, _, _) => (sheets, pps > 1)
          case ImpositionResult.RollImposition(_, _, _, _, _)            => (comp.geometry.totalFlatUnits, false)
          case ImpositionResult.DiscreteUnit(units)                      => (units, false)

        ComponentPrintJob(
          stationKind = stationKind,
          inkNotation = ps.ink.notation,
          sheetsToPrint = sheetsToPrint,
          requiresGuillotineCut = needsCut,
        )
      }

      val finOps = comp.finishes.flatMap(sf => toFinishingOperation(sf.finishType)).distinct

      ProductionComponentSpec(
        key = comp.key,
        materialName = comp.material.name(lang),
        printJob = printJobOpt,
        finishingOperations = finOps,
      )
    }

    ProductionSpec(
      orderId = orderId,
      orderLineIndex = orderLineIndex,
      categoryId = product.categoryId,
      productLabel = product.categoryName(lang),
      quantity = product.specs.quantity,
      speed = speed,
      components = prodComponents,
      hasFolding = product.specs.dimensions.foldTypeOpt.isDefined,
      bindingLabel = product.specs.binding.map(b => s"${b.method} (${b.totalPages}pp)"),
      customerInstructions = customerInstructions.map(_.trim).filter(_.nonEmpty),
    )

  /** Maps a catalog `FinishType` into a manufacturing `FinishingOperation`. */
  def toFinishingOperation(finishType: FinishType): Option[FinishingOperation] =
    finishType match
      case FinishType.Lamination | FinishType.Overlamination | FinishType.SoftTouchCoating =>
        Some(FinishingOperation.Lamination)
      case FinishType.UVCoating | FinishType.AqueousCoating | FinishType.Varnish =>
        Some(FinishingOperation.UVOrAqueousCoating)
      case FinishType.Embossing | FinishType.Debossing | FinishType.FoilStamping | FinishType.Thermography =>
        Some(FinishingOperation.EmbossingOrFoil)
      case FinishType.Embroidery =>
        Some(FinishingOperation.Embroidery)
      case FinishType.DieCut | FinishType.ContourCut | FinishType.KissCut | FinishType.RoundCorners =>
        Some(FinishingOperation.DieOrContourCut)
      case FinishType.Scoring | FinishType.Perforation =>
        Some(FinishingOperation.ScoringOrPerforation)
      case FinishType.Grommets | FinishType.Hem | FinishType.PolePocket | FinishType.RopeAccessory =>
        Some(FinishingOperation.LargeFormatHardware)
      case FinishType.HardwareAttachment | FinishType.HeatTransfer | FinishType.Mounting | FinishType.Drilling =>
        Some(FinishingOperation.HardwareAssembly)
      case FinishType.EdgePainting | FinishType.Numbering | FinishType.ProtectivePackaging =>
        None

  /** Translates `manufacturing`'s live `CapacitySnapshot` into `pricing`'s `DynamicPricingContext`. */
  def toDynamicPricingContext(
      snapshot: CapacitySnapshot,
      busyPeriods: List[BusyPeriodMultiplier] = Nil,
      expressMultiplierCap: BigDecimal = BigDecimal(2),
  ): DynamicPricingContext =
    DynamicPricingContext(
      globalUtilisation = snapshot.globalUtilisation,
      busyPeriods = busyPeriods,
      expressMultiplierCap = expressMultiplierCap,
    )
