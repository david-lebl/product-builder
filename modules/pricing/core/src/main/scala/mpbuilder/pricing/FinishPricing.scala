package mpbuilder.pricing

import mpbuilder.catalog.*
import mpbuilder.kernel.*
import zio.prelude.*

/** Pricing basis for finish surcharges, derived from how the material is priced. */
private[pricing] enum FinishPricingBasis:
  case PerItem(quantity: Int)
  case PerSheet(sheetsUsed: Int)
  case PerArea(areaSqM: BigDecimal, quantity: Int)

/** Per-finish surcharge lines (grommets, ropes, scoring, lamination, …). */
private[pricing] object FinishPricing:

  def computeFinishLines(
      finishes: List[SelectedFinish],
      rules: List[PricingRule],
      quantity: Int,
      basis: FinishPricingBasis,
      lang: Language,
      dimOpt: Option[Dimension] = None,
  ): Validation[PricingError, List[LineItem]] =
    finishes
      .map(finish => computeSingleFinishLine(finish, rules, quantity, basis, lang, dimOpt))
      .foldLeft(Validation.succeed(List.empty[LineItem]): Validation[PricingError, List[LineItem]]) {
        (accV, itemV) => accV.zipWith(itemV)(_ ++ _)
      }

  private def computeSingleFinishLine(
      finish: SelectedFinish,
      rules: List[PricingRule],
      quantity: Int,
      basis: FinishPricingBasis,
      lang: Language,
      dimOpt: Option[Dimension],
  ): Validation[PricingError, List[LineItem]] =
    // Parameterized Scoring → ScoringCountSurcharge (fails if rule is missing)
    finish.params match
      case Some(FinishParameters.ScoringParams(creaseCount)) if finish.finishType == FinishType.Scoring =>
        rules.collectFirst {
          case r: PricingRule.ScoringCountSurcharge if r.creaseCount == creaseCount => r.surchargePerUnit
        } match
          case Some(surcharge) =>
            val label = lang match
              case Language.En => if creaseCount == 1 then "Creasing: 1 crease" else s"Creasing: $creaseCount creases"
              case Language.Cs => creaseCount match
                case 1            => "Bigování: 1 linka"
                case n if n <= 4  => s"Bigování: $n linky"
                case n            => s"Bigování: $n linek"
            Validation.succeed(List(LineItem(label, surcharge, quantity, surcharge * quantity)))
          case None =>
            Validation.fail(PricingError.MissingScoringPrice(creaseCount))

      case _ =>
        // 1. GrommetSpacingAreaPrice: area-based grommet surcharge driven by spacing
        val grommetAreaRule = rules.collectFirst {
          case r: PricingRule.GrommetSpacingAreaPrice if r.finishId == finish.id => r
        }
        val grommetAreaItem = grommetAreaRule.flatMap { rule =>
          finish.params match
            case Some(FinishParameters.GrommetParams(spacingMm)) =>
              dimOpt.flatMap { dim =>
                val areaSqM = BigDecimal(dim.widthMm) * BigDecimal(dim.heightMm) / BigDecimal(1_000_000)
                val selectedTier = rule.tiers
                  .filter(_.spacingMm <= spacingMm)
                  .maxByOption(_.spacingMm)
                selectedTier.map { tier =>
                  val perimeterMm = 2.0 * (dim.widthMm + dim.heightMm)
                  val approxCount = math.ceil(perimeterMm / spacingMm).toInt + 4 // + 4 corner grommets
                  val surchargePerUnit = tier.pricePerSqMeter * areaSqM
                  val lineTotal = surchargePerUnit * quantity
                  LineItem(
                    label = s"Finish: ${finish.name(lang)} (≈$approxCount pcs @ ${spacingMm} mm)",
                    unitPrice = surchargePerUnit,
                    quantity = quantity,
                    lineTotal = lineTotal,
                  )
                }
              }
            case _ => None
        }

        // 2. FinishLinearMeterPrice: price per metre for rope/accessory finishes
        val linearMeterItem = if grommetAreaItem.isDefined then None else {
          val linearMeterRule = rules.collectFirst {
            case r: PricingRule.FinishLinearMeterPrice if r.finishId == finish.id => r
          }
          linearMeterRule.flatMap { rule =>
            finish.params match
              case Some(FinishParameters.RopeParams(lengthMeters)) =>
                val surchargePerUnit = rule.pricePerMeter * lengthMeters
                val lineTotal = surchargePerUnit * quantity
                Some(LineItem(
                  label = s"Finish: ${finish.name(lang)} (${lengthMeters} m)",
                  unitPrice = surchargePerUnit,
                  quantity = quantity,
                  lineTotal = lineTotal,
                ))
              case _ => None
          }
        }

        // 3. Fall through to existing FinishSurcharge / FinishTypeSurcharge
        val fallbackItem = if grommetAreaItem.isDefined || linearMeterItem.isDefined then None else {
          val byId = rules.collectFirst {
            case r: PricingRule.FinishSurcharge if r.finishId == finish.id => r.surchargePerUnit
          }
          val byType = rules.collectFirst {
            case r: PricingRule.FinishTypeSurcharge if r.finishType == finish.finishType => r.surchargePerUnit
          }
          // ID-level takes precedence over type-level
          byId.orElse(byType).map { surcharge =>
            // Lamination (and overlamination / soft-touch coating) applied to both sides
            // costs twice as much: each side is an independent pass on press.
            val sideFactor = finish.params match
              case Some(FinishParameters.LaminationParams(FinishSide.Both)) => BigDecimal(2)
              case _                                                         => BigDecimal(1)
            val effectiveSurcharge = surcharge * sideFactor
            basis match
              case FinishPricingBasis.PerItem(qty) =>
                LineItem(s"Finish: ${finish.name(lang)}", effectiveSurcharge, qty, effectiveSurcharge * qty)
              case FinishPricingBasis.PerSheet(sheets) =>
                LineItem(s"Finish: ${finish.name(lang)}", effectiveSurcharge, sheets, effectiveSurcharge * sheets)
              case FinishPricingBasis.PerArea(areaSqM, qty) =>
                val unitPrice = (effectiveSurcharge * areaSqM).rounded
                LineItem(s"Finish: ${finish.name(lang)}", unitPrice, qty, (unitPrice * qty).rounded)
          }
        }

        Validation.succeed(grommetAreaItem.orElse(linearMeterItem).orElse(fallbackItem).toList)
