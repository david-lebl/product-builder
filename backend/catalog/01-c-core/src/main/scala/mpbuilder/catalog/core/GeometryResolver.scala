package mpbuilder.catalog
package core

import mpbuilder.shared.*

/** Pure geometry, imposition, and weight engine.
  *
  * Computes flat vs finished size, units per product, optimal sheet/roll imposition,
  * and physical weight ONCE per configured product — eliminating duplicated saddle-stitch
  * and flat-size math across pricing, weight, and manufacturing.
  */
private[catalog] object GeometryResolver:

  def resolveComponentGeometry(
      key: ComponentKey,
      material: Material,
      printable: Boolean,
      specs: ProductSpecs,
  ): Validation[CatalogError, ComponentGeometry] =
    val finishedSize    = specs.dimensions.finishedBoundingBox
    val flatSize        = deriveFlatSize(key, printable, specs)
    val unitsPerProduct = deriveUnitsPerProduct(key, printable, specs)
    val totalFlatUnits  = unitsPerProduct * specs.quantity.value

    computeImposition(key, material, flatSize, totalFlatUnits, specs.bleed).map { imp =>
      ComponentGeometry(
        finishedSize = finishedSize,
        flatSize = flatSize,
        unitsPerProduct = unitsPerProduct,
        totalFlatUnits = totalFlatUnits,
        areaPerUnitSqM = flatSize.areaSqM,
        perimeterPerUnitM = flatSize.perimeterMeters,
        imposition = imp,
      )
    }

  /** Derives the unfolded flat sheet size for a component:
    *   - Non-printable hardware blanks keep their finished bounding box.
    *   - Folded products unfold along `foldType.panelLayout` (e.g. Tri-fold DL 99×210 -> 297×210 A4!).
    *   - Saddle-stitch cover & body sheets are folded spreads (2× finished page width).
    */
  private[catalog] def deriveFlatSize(
      key: ComponentKey,
      printable: Boolean,
      specs: ProductSpecs,
  ): Dimension2D =
    if !printable then specs.dimensions.finishedBoundingBox
    else
      specs.dimensions match
        case ProductDimensions.Folded2D(finished, foldType) =>
          val (cols, rows) = foldType.panelLayout
          Dimension2D(finished.width * cols, finished.height * rows)
        case other =>
          val base = other.finishedBoundingBox
          val isSaddleStitchSpread =
            specs.binding.exists(_.method == BindingMethod.SaddleStitch) &&
              (key == ComponentKey.Cover || key == ComponentKey.Body)
          if isSaddleStitchSpread then Dimension2D(base.width * 2, base.height)
          else base

  /** Derives how many flat sheets/units of this component are required for 1 finished product. */
  private[catalog] def deriveUnitsPerProduct(
      key: ComponentKey,
      printable: Boolean,
      specs: ProductSpecs,
  ): Int =
    if !printable then 1
    else if key != ComponentKey.Body then 1
    else
      specs.binding match
        case None => 1
        case Some(BindingSpec(method, totalPages, selfCover)) =>
          val coverPages =
            if selfCover then 0
            else
              method match
                case BindingMethod.SaddleStitch | BindingMethod.PerfectBinding | BindingMethod.CaseBinding => 4
                case BindingMethod.SpiralBinding | BindingMethod.WireOBinding                              => 2
          val bodyPages = math.max(0, totalPages - coverPages)
          val pagesPerSheet = method match
            case BindingMethod.SaddleStitch => 4
            case _                          => 2
          math.max(1, math.ceil(bodyPages.toDouble / pagesPerSheet).toInt)

  /** Picks the best stock format on `material` (minimizing press sheets or fitting roll width). */
  private[catalog] def computeImposition(
      key: ComponentKey,
      material: Material,
      flatSize: Dimension2D,
      totalFlatUnits: Int,
      bleedOverride: Option[NonNegativeMm],
  ): Validation[CatalogError, ImpositionResult] =
    val formats = material.stockFormats.toList

    val sheetCandidates = formats.collect { case s: MediaFormat.SheetStock => s }
    if sheetCandidates.nonEmpty then
      val evaluated = sheetCandidates.flatMap { sheet =>
        val bleed  = bleedOverride.getOrElse(sheet.defaultBleed)
        val gutter = sheet.defaultGutter
        val pps    = nestOnSheet(sheet.sheetSize, flatSize, bleed, gutter)
        if pps > 0 then
          val sheetsNeeded = math.ceil(totalFlatUnits.toDouble / pps).toInt
          val usedArea     = flatSize.areaSqM.value * BigDecimal(pps)
          val sheetArea    = sheet.sheetSize.areaSqM.value
          val wasteRatio   =
            if sheetArea > BigDecimal(0) then
              (BigDecimal(1) - (usedArea / sheetArea)).max(BigDecimal(0)).setScale(4, BigDecimal.RoundingMode.HALF_UP)
            else BigDecimal(0)
          Some(
            new ImpositionResult.SheetImposition(
              stockFormatId = sheet.formatId,
              sheetSize = sheet.sheetSize,
              piecesPerSheet = pps,
              sheetsNeeded = sheetsNeeded,
              guillotineCutsPerSheet = math.max(0, pps - 1),
              wasteRatio = wasteRatio,
            )
          )
        else None
      }
      evaluated.minByOption(i => (i.sheetsNeeded, i.wasteRatio)) match
        case Some(best) => Validation.succeed(best)
        case None       => Validation.fail(CatalogError.FlatSizeExceedsAllStockFormats(key, material.id, flatSize))
    else
      val rollCandidates = formats.collect { case r: MediaFormat.RollStock => r }
      if rollCandidates.nonEmpty then
        val fitting = rollCandidates.flatMap { roll =>
          val pw = roll.printableWidth.value
          val (across, stepMm) =
            if flatSize.width.value <= pw && flatSize.height.value <= pw then
              val a1 = (pw / flatSize.width.value).toBigInt.toInt
              val a2 = (pw / flatSize.height.value).toBigInt.toInt
              if a1 >= a2 then (a1, flatSize.height.value) else (a2, flatSize.width.value)
            else if flatSize.width.value <= pw then
              ((pw / flatSize.width.value).toBigInt.toInt, flatSize.height.value)
            else if flatSize.height.value <= pw then
              ((pw / flatSize.height.value).toBigInt.toInt, flatSize.width.value)
            else (0, BigDecimal(0))

          if across > 0 then
            val rowsNeeded     = math.ceil(totalFlatUnits.toDouble / across).toInt
            val linearMeters   = ((BigDecimal(rowsNeeded) * stepMm) / BigDecimal(1000)).setScale(3, BigDecimal.RoundingMode.HALF_UP)
            val totalAreaSqM   = flatSize.areaSqM * totalFlatUnits
            Some(
              new ImpositionResult.RollImposition(
                stockFormatId = roll.formatId,
                rollWidth = roll.rollWidth,
                piecesAcrossRoll = across,
                linearMetersUsed = linearMeters,
                totalAreaSqM = totalAreaSqM,
              )
            )
          else None
        }
        fitting.minByOption(_.linearMetersUsed) match
          case Some(best) => Validation.succeed(best)
          case None       => Validation.fail(CatalogError.FlatSizeExceedsAllStockFormats(key, material.id, flatSize))
      else
        Validation.succeed(ImpositionResult.DiscreteUnit(totalFlatUnits))

  private def nestOnSheet(
      sheet: Dimension2D,
      item: Dimension2D,
      bleed: NonNegativeMm,
      gutter: NonNegativeMm,
  ): Int =
    val ew = item.width.value + BigDecimal(2) * bleed.value
    val eh = item.height.value + BigDecimal(2) * bleed.value
    val g  = gutter.value

    def count(w: BigDecimal, h: BigDecimal): Int =
      val cols = ((sheet.width.value + g) / (w + g)).toBigInt.toInt
      val rows = ((sheet.height.value + g) / (h + g)).toBigInt.toInt
      if cols > 0 && rows > 0 then cols * rows else 0

    math.max(count(ew, eh), count(eh, ew))

  /** Computes physical weight for all material weight models (grammage, rigid density, and discrete unit weight). */
  def computeWeight(
      components: NonEmptyChunk[ConfiguredComponent],
      quantity: Quantity,
  ): Option[WeightSummary] =
    val weightsOpt: Option[List[ComponentWeight]] =
      components.toList.foldLeft(Option(List.empty[ComponentWeight])) { (accOpt, comp) =>
        for
          acc <- accOpt
          perProduct <- comp.material.weight match
            case MaterialWeight.Grammage(w) =>
              val grams = comp.geometry.areaPerUnitSqM.value * BigDecimal(w.gsm) * BigDecimal(comp.geometry.unitsPerProduct)
              Some(Grams.unsafe(grams.setScale(2, BigDecimal.RoundingMode.HALF_UP)))
            case MaterialWeight.Thickness(_, Some(w)) =>
              val grams = comp.geometry.areaPerUnitSqM.value * BigDecimal(w.gsm) * BigDecimal(comp.geometry.unitsPerProduct)
              Some(Grams.unsafe(grams.setScale(2, BigDecimal.RoundingMode.HALF_UP)))
            case MaterialWeight.Thickness(_, None) =>
              None
            case MaterialWeight.UnitWeight(unitG) =>
              val grams = unitG.value * BigDecimal(comp.geometry.unitsPerProduct)
              Some(Grams.unsafe(grams.setScale(2, BigDecimal.RoundingMode.HALF_UP)))
        yield acc :+ ComponentWeight(
          key = comp.key,
          weightPerProduct = perProduct,
          totalWeight = perProduct * quantity.value,
        )
      }

    weightsOpt.flatMap(NonEmptyChunk.fromIterableOption).map { cw =>
      val perProd = cw.map(_.weightPerProduct).reduceLeft(_ + _)
      WeightSummary(
        components = cw,
        weightPerProduct = perProd,
        totalWeight = perProd * quantity.value,
      )
    }
