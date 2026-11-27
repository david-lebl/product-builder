package mpbuilder.storefront

import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.manufacturing.FinishingOperation
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*

/** Pure Anti-Corruption translators used by `StorefrontFacade` to map between public
  * bounded-context APIs without any context depending directly on another.
  */
object StorefrontTranslators:

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

  def toProductSnapshot(
      product: ConfiguredProduct,
      summaryNotes: Option[String] = None,
  ): ProductSnapshot =
    ProductSnapshot(
      categoryId = product.categoryId,
      categoryName = product.categoryName,
      catalogVersion = product.catalogVersion,
      components = product.components.map { c =>
        val sheets = c.geometry.imposition match
          case ImpositionResult.SheetImposition(_, _, _, s, _, _) => s
          case _                                                  => 0
        ComponentSnapshot(
          key = c.key,
          materialId = c.material.id,
          materialName = c.material.name,
          printingMethodId = c.print.map(_.printingMethod.id),
          inkNotation = c.print.map(_.ink.notation),
          finishes = c.finishes.map(toFinishSnapshot),
          flatSize = c.geometry.flatSize,
          finishedSize = c.geometry.finishedSize,
          sheetsUsed = sheets,
        )
      },
      specs = toProductSpecSnapshot(product),
      summaryNotes = summaryNotes,
    )

  /** Bleed when the customer did not choose one: 5 mm for large-format inkjet, 3 mm otherwise. */
  def defaultBleed(product: ConfiguredProduct): NonNegativeMm =
    val largeFormat = product.components.exists(_.print.exists(_.printingMethod.processType.isLargeFormatInkjet))
    NonNegativeMm.unsafe(if largeFormat then 5 else 3)

  def toFinishSnapshot(sf: SelectedFinish): FinishSnapshot =
    FinishSnapshot(
      finishId = sf.id,
      name = sf.finish.name,
      finishType = sf.finishType.toString,
      side = sf.appliedSide.map {
        case FinishSide.Front => FinishSnapshot.Side.Front
        case FinishSide.Back  => FinishSnapshot.Side.Back
        case FinishSide.Both  => FinishSnapshot.Side.Both
      },
      params = sf.params.map {
        case FinishParams.RoundCorners(n, r) => FinishSnapshot.Params.RoundCorners(n, r)
        case FinishParams.FoilStamping(c)    => FinishSnapshot.Params.FoilStamping(toFoilColor(c))
        case FinishParams.Grommets(s)        => FinishSnapshot.Params.Grommets(s)
        case FinishParams.Perforation(p)     => FinishSnapshot.Params.Perforation(p)
        case FinishParams.LinearLength(m)    => FinishSnapshot.Params.LinearLength(m)
        case FinishParams.Scoring(n)         => FinishSnapshot.Params.Scoring(n)
      },
    )

  private def toFoilColor(c: FoilColor): FinishSnapshot.FoilColor = c match
    case FoilColor.Gold        => FinishSnapshot.FoilColor.Gold
    case FoilColor.Silver      => FinishSnapshot.FoilColor.Silver
    case FoilColor.Copper      => FinishSnapshot.FoilColor.Copper
    case FoilColor.RoseGold    => FinishSnapshot.FoilColor.RoseGold
    case FoilColor.Holographic => FinishSnapshot.FoilColor.Holographic

  def toProductSpecSnapshot(product: ConfiguredProduct): ProductSpecSnapshot =
    import ProductSpecSnapshot as S
    val specs = product.specs
    def fold(ft: FoldType): S.FoldType = ft match
      case FoldType.Half       => S.FoldType.Half
      case FoldType.Tri        => S.FoldType.Tri
      case FoldType.Gate       => S.FoldType.Gate
      case FoldType.Accordion  => S.FoldType.Accordion
      case FoldType.ZFold      => S.FoldType.ZFold
      case FoldType.RollFold   => S.FoldType.RollFold
      case FoldType.FrenchFold => S.FoldType.FrenchFold
      case FoldType.CrossFold  => S.FoldType.CrossFold
    def binding(m: BindingMethod): S.BindingMethod = m match
      case BindingMethod.SaddleStitch   => S.BindingMethod.SaddleStitch
      case BindingMethod.PerfectBinding => S.BindingMethod.PerfectBinding
      case BindingMethod.SpiralBinding  => S.BindingMethod.SpiralBinding
      case BindingMethod.WireOBinding   => S.BindingMethod.WireOBinding
      case BindingMethod.CaseBinding    => S.BindingMethod.CaseBinding
    ProductSpecSnapshot(
      dimensions = specs.dimensions match
        case ProductDimensions.Flat2D(size)            => S.Dimensions.Flat2D(size)
        case ProductDimensions.Folded2D(size, ft)      => S.Dimensions.Folded2D(size, fold(ft))
        case ProductDimensions.Box3D(box, blank)       => S.Dimensions.Box3D(box, blank)
        case ProductDimensions.Circular(d)             => S.Dimensions.Circular(d)
        case ProductDimensions.PrintArea(area, label)  => S.Dimensions.PrintArea(area, label)
      ,
      orientation = specs.orientation.map {
        case Orientation.Portrait  => S.Orientation.Portrait
        case Orientation.Landscape => S.Orientation.Landscape
      },
      bleed = specs.bleed.getOrElse(defaultBleed(product)),
      binding = specs.binding.map(b => S.Binding(binding(b.method), b.totalPages, b.selfCover)),
      deliveryFormat = specs.deliveryFormat match
        case DeliveryFormat.CutSingles            => S.DeliveryFormat.CutSingles
        case DeliveryFormat.KissCutSheets         => S.DeliveryFormat.KissCutSheets
        case DeliveryFormat.RollLabels(core, dir) => S.DeliveryFormat.RollLabels(core, dir)
        case DeliveryFormat.PackedUnits           => S.DeliveryFormat.PackedUnits
      ,
    )

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

  def toCartLine(
      lineId: OrderLineId,
      product: ConfiguredProduct,
      speed: ManufacturingSpeed,
      quote: PriceQuote,
      copies: Quantity = Quantity.one,
      artworkIds: Map[ComponentKey, ArtworkId] = Map.empty,
      productionNote: Option[String] = None,
  ): CartLine =
    CartLine(
      lineId = lineId,
      product = toProductSnapshot(product),
      runQuantity = product.specs.quantity,
      versionCount = product.specs.versionCount,
      speed = speed,
      price = toLinePriceSnapshot(quote),
      artworkIds = artworkIds,
      copies = copies,
      weightGrams = product.weight.map(_.totalWeight.value),
      productionNote = ProductionNote.normalize(productionNote),
    )

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

  def toOrderCustomerSnapshot(customer: CustomerView): OrderCustomerSnapshot =
    OrderCustomerSnapshot(
      customerId = Some(customer.id),
      segment = customer.segment,
      fullName = NonEmptyString.unsafe(s"${customer.contact.firstName.value} ${customer.contact.lastName.value}"),
      email = customer.contact.email,
      phone = customer.contact.phone,
      companyName = customer.company.map(_.legalName),
      companyRegNo = customer.company.map(_.registrationNumber),
      vatId = customer.company.flatMap(_.vatId),
    )
