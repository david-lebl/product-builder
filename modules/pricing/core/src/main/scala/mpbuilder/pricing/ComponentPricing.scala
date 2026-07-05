package mpbuilder.pricing

import mpbuilder.catalog.*
import mpbuilder.kernel.*
import zio.prelude.*

/** Per-component material, ink and cutting lines (area / sheet / unit pricing). */
private[pricing] object ComponentPricing:

  def calculateComponentBreakdown(
      comp: ProductComponent,
      specs: ProductSpecifications,
      rules: List[PricingRule],
      quantity: Int,
      lang: Language,
      printingMethodId: PrintingMethodId,
  ): Validation[PricingError, ComponentBreakdown] =
    val effectiveQuantity = comp.sheetCount * quantity

    val areaTierRule = rules.collectFirst {
      case r: PricingRule.MaterialAreaTier if r.materialId == comp.material.id => r
    }
    val areaRule = rules.collectFirst {
      case r: PricingRule.MaterialAreaPrice if r.materialId == comp.material.id => r
    }
    val sheetRule = rules.collectFirst {
      case r: PricingRule.MaterialSheetPrice if r.materialId == comp.material.id => r
    }
    val baseRule = rules.collectFirst {
      case r: PricingRule.MaterialBasePrice if r.materialId == comp.material.id => r
    }

    // MaterialAreaTier takes precedence over MaterialAreaPrice
    areaTierRule match
      case Some(tierRule) =>
        specs.get(SpecKind.Size) match
          case Some(SpecValue.SizeSpec(dim)) =>
            val areaSqM = BigDecimal(dim.widthMm) * BigDecimal(dim.heightMm) / BigDecimal(1_000_000)
            val selectedTier = tierRule.tiers
              .filter(_.minSqm <= areaSqM)
              .maxByOption(_.minSqm)
            selectedTier match
              case None =>
                Validation.fail(PricingError.NoBasePriceForMaterial(comp.material.id, comp.role))
              case Some(tier) =>
                val unitPrice = tier.pricePerSqMeter * areaSqM
                val materialLineTotal = unitPrice * effectiveQuantity
                val tierLabel = s"Material: ${comp.material.name(lang)} (${tier.minSqm} m² tier)"
                val materialLine = LineItem(
                  label = tierLabel,
                  unitPrice = unitPrice,
                  quantity = effectiveQuantity,
                  lineTotal = materialLineTotal,
                )
                val inkConfigLine = computeInkConfigLine(comp.inkConfiguration, rules, printingMethodId, InkPricingBasis.Area(areaSqM, effectiveQuantity))
                FinishPricing.computeFinishLines(comp.finishes, rules, quantity, FinishPricingBasis.PerArea(areaSqM, effectiveQuantity), lang, Some(dim)).map { finishLines =>
                  ComponentBreakdown(
                    role = comp.role,
                    materialLine = materialLine,
                    cuttingLine = None,
                    inkConfigLine = inkConfigLine,
                    finishLines = finishLines,
                    sheetsUsed = 0,
                  )
                }
          case _ =>
            Validation.fail(PricingError.NoSizeForAreaPricing(comp.material.id, comp.role))

      case None =>
        areaRule match
          case Some(areaPrice) =>
            specs.get(SpecKind.Size) match
              case Some(SpecValue.SizeSpec(dim)) =>
                val areaSqM = BigDecimal(dim.widthMm) * BigDecimal(dim.heightMm) / BigDecimal(1_000_000)
                val unitPrice = areaPrice.pricePerSqMeter * areaSqM
                val materialLineTotal = unitPrice * effectiveQuantity
                val materialLine = LineItem(
                  label = s"Material: ${comp.material.name(lang)}",
                  unitPrice = unitPrice,
                  quantity = effectiveQuantity,
                  lineTotal = materialLineTotal,
                )
                val inkConfigLine = computeInkConfigLine(comp.inkConfiguration, rules, printingMethodId, InkPricingBasis.Area(areaSqM, effectiveQuantity))
                FinishPricing.computeFinishLines(comp.finishes, rules, quantity, FinishPricingBasis.PerArea(areaSqM, effectiveQuantity), lang, Some(dim)).map { finishLines =>
                  ComponentBreakdown(
                    role = comp.role,
                    materialLine = materialLine,
                    cuttingLine = None,
                    inkConfigLine = inkConfigLine,
                    finishLines = finishLines,
                    sheetsUsed = 0,
                  )
                }
              case _ =>
                Validation.fail(PricingError.NoSizeForAreaPricing(comp.material.id, comp.role))

          case None =>
            sheetRule match
              case Some(sp) =>
                specs.get(SpecKind.Size) match
                  case Some(SpecValue.SizeSpec(dim)) =>
                    // For saddle-stitch booklets, each folded sheet has a flat (unfolded) width that
                    // is twice the finished page width.  Using the finished page width would overestimate
                    // piecesPerSheet by 2×, halving the reported sheets used.
                    val isSaddleStitchFolded =
                      specs.get(SpecKind.BindingMethod).contains(SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch)) &&
                        (comp.role == ComponentRole.Cover || comp.role == ComponentRole.Body)
                    val flatItemW = if isSaddleStitchFolded then dim.widthMm.toDouble * 2 else dim.widthMm.toDouble
                    val pps = SheetNesting.piecesPerSheet(
                      sp.sheetWidthMm, sp.sheetHeightMm,
                      flatItemW, dim.heightMm.toDouble,
                      sp.bleedMm, sp.gutterMm,
                    )
                    val sheetsUsed = math.ceil(effectiveQuantity.toDouble / pps).toInt
                    val materialLineTotal = sp.pricePerSheet * sheetsUsed
                    val materialLine = LineItem(
                      label = s"Material: ${comp.material.name(lang)}",
                      unitPrice = sp.pricePerSheet,
                      quantity = sheetsUsed,
                      lineTotal = materialLineTotal,
                    )

                    val inkConfigLine = computeInkConfigLine(comp.inkConfiguration, rules, printingMethodId, InkPricingBasis.SheetOrUnit(sheetsUsed))

                    val cuttingRule = rules.collectFirst { case r: PricingRule.CuttingSurcharge => r }
                    val numCuts = pps - 1
                    val cuttingLine =
                      if numCuts > 0 then
                        cuttingRule.map { cr =>
                          val costPerSheet = cr.costPerCut * numCuts
                          LineItem(
                            label = "Cutting surcharge",
                            unitPrice = costPerSheet,
                            quantity = sheetsUsed,
                            lineTotal = (costPerSheet * sheetsUsed).rounded,
                          )
                        }
                      else None

                    val finishLines = FinishPricing.computeFinishLines(comp.finishes, rules, quantity, FinishPricingBasis.PerSheet(sheetsUsed), lang, specs.get(SpecKind.Size).collect { case SpecValue.SizeSpec(d) => d })
                    finishLines.map { fl =>
                      ComponentBreakdown(
                        role = comp.role,
                        materialLine = materialLine,
                        cuttingLine = cuttingLine,
                        inkConfigLine = inkConfigLine,
                        finishLines = fl,
                        sheetsUsed = sheetsUsed,
                      )
                    }
                  case _ =>
                    Validation.fail(PricingError.NoSizeForSheetPricing(comp.material.id, comp.role))

              case None =>
                baseRule match
                  case Some(bp) =>
                    val materialLineTotal = bp.unitPrice * effectiveQuantity
                    val materialLine = LineItem(
                      label = s"Material: ${comp.material.name(lang)}",
                      unitPrice = bp.unitPrice,
                      quantity = effectiveQuantity,
                      lineTotal = materialLineTotal,
                    )
                    val inkConfigLine = computeInkConfigLine(comp.inkConfiguration, rules, printingMethodId, InkPricingBasis.SheetOrUnit(effectiveQuantity))
                    FinishPricing.computeFinishLines(comp.finishes, rules, quantity, FinishPricingBasis.PerItem(quantity), lang, specs.get(SpecKind.Size).collect { case SpecValue.SizeSpec(d) => d }).map { finishLines =>
                      ComponentBreakdown(
                        role = comp.role,
                        materialLine = materialLine,
                        cuttingLine = None,
                        inkConfigLine = inkConfigLine,
                        finishLines = finishLines,
                        sheetsUsed = 0,
                      )
                    }
                  case None =>
                    Validation.fail(PricingError.NoBasePriceForMaterial(comp.material.id, comp.role))

  private enum InkPricingBasis:
    case SheetOrUnit(count: Int)
    case Area(sqM: BigDecimal, quantity: Int)

  private def computeInkConfigLine(
      inkConfig: InkConfiguration,
      rules: List[PricingRule],
      printingMethodId: PrintingMethodId,
      basis: InkPricingBasis,
  ): Option[LineItem] =
    val label = s"Ink configuration: ${inkConfig.notation}"
    basis match
      case InkPricingBasis.SheetOrUnit(count) =>
        // unitPrice is taken directly from the rule (already Money precision); only lineTotal is rounded.
        rules.collectFirst {
          case r: PricingRule.InkConfigurationSheetPrice
              if r.printingMethodId == printingMethodId
                && r.frontColorCount == inkConfig.front.colorCount
                && r.backColorCount == inkConfig.back.colorCount =>
            r.pricePerSheet
        }.map { pricePerSheet =>
          LineItem(
            label = label,
            unitPrice = pricePerSheet,
            quantity = count,
            lineTotal = (pricePerSheet * count).rounded,
          )
        }
      case InkPricingBasis.Area(sqM, qty) =>
        // unitPrice is rounded first (pricePerSqM × area can have arbitrary precision), then lineTotal is rounded.
        rules.collectFirst {
          case r: PricingRule.InkConfigurationAreaPrice
              if r.printingMethodId == printingMethodId
                && r.frontColorCount == inkConfig.front.colorCount
                && r.backColorCount == inkConfig.back.colorCount =>
            r.pricePerSqM
        }.map { pricePerSqM =>
          val unitPrice = (pricePerSqM * sqM).rounded
          LineItem(
            label = label,
            unitPrice = unitPrice,
            quantity = qty,
            lineTotal = (unitPrice * qty).rounded,
          )
        }

  private object SheetNesting:
    def piecesPerSheet(
        sheetW: Double,
        sheetH: Double,
        itemW: Double,
        itemH: Double,
        bleedMm: Double,
        gutterMm: Double,
    ): Int =
      val effectiveW = itemW + 2 * bleedMm
      val effectiveH = itemH + 2 * bleedMm

      def countOrientation(ew: Double, eh: Double): Int =
        val cols = math.floor((sheetW + gutterMm) / (ew + gutterMm)).toInt
        val rows = math.floor((sheetH + gutterMm) / (eh + gutterMm)).toInt
        cols * rows

      val normal = countOrientation(effectiveW, effectiveH)
      val rotated = countOrientation(effectiveH, effectiveW)
      math.max(math.max(normal, rotated), 1)
