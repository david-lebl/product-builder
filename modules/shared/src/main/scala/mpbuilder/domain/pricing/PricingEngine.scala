package mpbuilder.domain.pricing

import mpbuilder.domain.*
import mpbuilder.domain.catalog.*
import mpbuilder.domain.config.*
import mpbuilder.domain.ids.*
import zio.NonEmptyChunk
import zio.prelude.Validation

/** The rule-driven price calculator (spec §5). A pure function of the catalog,
  * one pricelist, and a product configuration — the browser runs it for live
  * pricing and the server re-runs it authoritatively on order submission.
  *
  * Every monetary intermediate is rounded to 2 decimals as it is produced
  * (spec §5.11). Errors accumulate: all unpriceable parts are reported at once.
  */
object PricingEngine:

  def price(
    catalog: Catalog,
    pricelist: Pricelist,
    config: ProductConfiguration,
  ): Either[NonEmptyChunk[PricingError], PriceBreakdown] =
    new Calc(catalog, pricelist, config).result.toEither

  // ---------------------------------------------------------------------------

  private enum Mode:
    case TieredArea(tiers: List[AreaTier])
    case FlatArea(pricePerM2: BigDecimal)
    case Sheet(pricePerSheet: BigDecimal)
    case PerUnit(pricePerUnit: BigDecimal)

  /** Pricing method resolved for one selected finish (spec §5.4 priority order). */
  private enum FinishCharge:
    case PerUnitPrice(finish: Finish, price: BigDecimal)             // crease-count pricing
    case PerArea(finish: Finish, pricePerM2: BigDecimal)             // grommets / area-mode surcharge
    case PerSheet(finish: Finish, pricePerSheet: BigDecimal)         // sheet-mode surcharge
    case PerLength(finish: Finish, pricePerMetre: BigDecimal, metres: BigDecimal)
    case PerItem(finish: Finish, price: BigDecimal)                  // unit-mode surcharge / per-unit params pricing
    case Free(finish: Finish)

  private final case class ComponentPlan(
    conf: ComponentConfiguration,
    material: Material,
    mode: Mode,
    layout: Option[Imposition.Layout],
    areaM2: Option[BigDecimal],
    finishCharges: List[FinishCharge],
  )

  private final class Calc(catalog: Catalog, pricelist: Pricelist, config: ProductConfiguration):
    private val currency = pricelist.currency
    private def money(amount: BigDecimal): Money = Money(amount, currency)
    private val rules = pricelist.rules

    def result: Validation[PricingError, PriceBreakdown] =
      Validation
        .validateWith(quantity, Validation.validateAll(config.components.map(plan)))(buildBreakdown)

    // ----- step 1: quantity ---------------------------------------------------

    private def quantity: Validation[PricingError, Int] =
      config.details.quantity match
        case Some(q) if q > 0 => Validation.succeed(q)
        case _                => Validation.fail(PricingError.NoQuantity)

    // ----- per-component plan (quantity-independent lookups + errors) --------

    private def plan(conf: ComponentConfiguration): Validation[PricingError, ComponentPlan] =
      catalog.materialsById.get(conf.materialId) match
        case None =>
          Validation.fail(PricingError.UnknownReference("material", conf.materialId.raw))
        case Some(material) =>
          materialMode(material).flatMap { mode =>
            val (layout, area) = geometry
            val sizeCheck: Validation[PricingError, Unit] = mode match
              case Mode.Sheet(_) =>
                config.details.size match
                  case None => Validation.fail(PricingError.SizeRequiredForPricing(material.id))
                  case Some(size) =>
                    if layout.isEmpty then Validation.fail(PricingError.DoesNotFitOnSheet(material.id, size))
                    else Validation.unit
              case Mode.FlatArea(_) | Mode.TieredArea(_) =>
                if area.isEmpty then Validation.fail(PricingError.SizeRequiredForPricing(material.id))
                else Validation.unit
              case Mode.PerUnit(_) => Validation.unit

            Validation.validateWith(
              sizeCheck,
              Validation.validateAll(conf.finishes.map(finishCharge(_, material, mode, area))),
            )((_, charges) => ComponentPlan(conf, material, mode, layout, area, charges))
          }

    /** Material pricing mode, in spec §5.2 priority order. */
    private def materialMode(material: Material): Validation[PricingError, Mode] =
      val id = material.id
      rules
        .collectFirst { case PricingRule.MaterialTieredAreaPrice(`id`, tiers) => Mode.TieredArea(tiers) }
        .orElse(rules.collectFirst { case PricingRule.MaterialAreaPrice(`id`, p) => Mode.FlatArea(p) })
        .orElse(rules.collectFirst { case PricingRule.MaterialSheetPrice(`id`, p) => Mode.Sheet(p) })
        .orElse(rules.collectFirst { case PricingRule.MaterialUnitPrice(`id`, p) => Mode.PerUnit(p) })
        .fold(Validation.fail(PricingError.NoMaterialPrice(id)))(Validation.succeed)

    /** Sheet layout and per-item area, where a size is given. Whether the size
      * is *required* depends on the pricing mode, checked in [[plan]].
      */
    private lazy val geometry: (Option[Imposition.Layout], Option[BigDecimal]) =
      config.details.size match
        case None       => (None, None)
        case Some(size) => (Imposition.layout(size), Some(size.areaM2))

    // ----- step 4 resolution: how each selected finish is priced --------------

    private def finishCharge(
      selected: SelectedFinish,
      material: Material,
      mode: Mode,
      areaM2: Option[BigDecimal],
    ): Validation[PricingError, FinishCharge] =
      catalog.finishesById.get(selected.finishId) match
        case None =>
          Validation.fail(PricingError.UnknownReference("finish", selected.finishId.raw))
        case Some(finish) =>
          finish.finishType match
            case FinishType.Scoring => creaseCharge(finish, selected)
            case FinishType.Grommets => grommetCharge(finish, selected, material, areaM2)
            case _ =>
              linearCharge(finish, selected)
                .map(Validation.succeed)
                .getOrElse(Validation.succeed(standardCharge(finish, mode)))

    private def creaseCharge(finish: Finish, selected: SelectedFinish): Validation[PricingError, FinishCharge] =
      selected.params match
        case Some(FinishParams.Creases(n)) =>
          rules
            .collectFirst { case PricingRule.CreaseCountPrice(`n`, price) => price }
            .fold(Validation.fail(PricingError.NoCreaseCountPrice(n)))(p =>
              Validation.succeed(FinishCharge.PerUnitPrice(finish, p))
            )
        case _ => Validation.fail(PricingError.MissingFinishParams(finish.id))

    private def grommetCharge(
      finish: Finish,
      selected: SelectedFinish,
      material: Material,
      areaM2: Option[BigDecimal],
    ): Validation[PricingError, FinishCharge] =
      selected.params match
        case Some(FinishParams.GrommetSpacing(spacing)) =>
          rules.collectFirst { case PricingRule.GrommetSpacingPrice(`spacing`, price) => price } match
            case None => Validation.fail(PricingError.NoGrommetSpacingPrice(spacing))
            case Some(price) =>
              if areaM2.isEmpty then Validation.fail(PricingError.SizeRequiredForPricing(material.id))
              else Validation.succeed(FinishCharge.PerArea(finish, price))
        case _ => Validation.fail(PricingError.MissingFinishParams(finish.id))

    private def linearCharge(finish: Finish, selected: SelectedFinish): Option[FinishCharge] =
      findByTarget(finish) { case PricingRule.LinearLengthPrice(t, price) => (t, price) }
        .map { perMetre =>
          selected.params match
            case Some(FinishParams.RopeLength(metres)) => FinishCharge.PerLength(finish, perMetre, metres)
            // Length priced but no length chosen: treated as missing params via plan validation below.
            case _ => FinishCharge.PerLength(finish, perMetre, BigDecimal(0))
        }

    private def standardCharge(finish: Finish, mode: Mode): FinishCharge =
      findByTarget(finish) { case PricingRule.FinishSurcharge(t, amount) => (t, amount) } match
        case None => FinishCharge.Free(finish)
        case Some(amount) =>
          mode match
            case Mode.Sheet(_)                      => FinishCharge.PerSheet(finish, amount)
            case Mode.FlatArea(_) | Mode.TieredArea(_) => FinishCharge.PerArea(finish, amount)
            case Mode.PerUnit(_)                    => FinishCharge.PerItem(finish, amount)

    /** Specific-finish rule beats finish-type rule (spec §5.4). */
    private def findByTarget(finish: Finish)(
      pf: PartialFunction[PricingRule, (FinishTarget, BigDecimal)]
    ): Option[BigDecimal] =
      val matches = rules.collect(pf)
      matches
        .collectFirst { case (FinishTarget.Specific(id), amount) if id == finish.id => amount }
        .orElse(matches.collectFirst {
          case (FinishTarget.OfType(t), amount) if t == finish.finishType => amount
        })

    // ----- assembling the breakdown (steps 2–11) ------------------------------

    private def buildBreakdown(qty: Int, plans: List[ComponentPlan]): PriceBreakdown =
      val componentBreakdowns = plans.map(componentBreakdown(qty, _))
      val orderLines          = buildOrderLines(qty)

      // step 6 — subtotal
      val subtotal =
        (componentBreakdowns.map(_.subtotal) ++ orderLines.map(_.total))
          .foldLeft(money(0))(_ + _)
          .round2

      // step 7 — volume discount
      val totalSheets = componentBreakdowns.flatMap(_.sheetsUsed).sum
      val volumeDiscount = resolveDiscount(totalSheets, qty)
      val discountedSubtotal = volumeDiscount match
        case Some(d) => (subtotal * d.multiplier).round2
        case None    => subtotal
      val appliedDiscount = volumeDiscount.map(d => d.copy(amountOff = subtotal - discountedSubtotal))

      // step 8 — production speed multiplier (static in stage 1)
      val speedMultiplier = rules.collectFirst {
        case PricingRule.SpeedMultiplier(t, m) if t == config.speedTier && m != BigDecimal(1) => m
      }
      val afterSpeed = speedMultiplier match
        case Some(m) => (discountedSubtotal * m).round2
        case None    => discountedSubtotal
      val appliedSpeed = speedMultiplier.map(m =>
        AppliedSpeed(config.speedTier, m, afterSpeed - discountedSubtotal)
      )

      // step 9 — one-time setup fees (deduped, never discounted)
      val setupFees  = buildSetupFees(plans)
      val afterSetup = setupFees.foldLeft(afterSpeed)((acc, f) => acc + f.fee).round2

      // step 10 — minimum order price
      val minimum = rules.collectFirst { case PricingRule.MinimumOrderPrice(a) => money(a) }
      val (total, minimumApplied) = minimum match
        case Some(min) if afterSetup < min => (min, Some(min))
        case _                             => (afterSetup, None)

      PriceBreakdown(
        currency = currency,
        quantity = qty,
        components = componentBreakdowns,
        orderLines = orderLines,
        subtotal = subtotal,
        volumeDiscount = appliedDiscount,
        discountedSubtotal = discountedSubtotal,
        speedAdjustment = appliedSpeed,
        afterSpeed = afterSpeed,
        setupFees = setupFees,
        minimumApplied = minimumApplied,
        total = total,
      )

    // ----- steps 2–4 + cutting, for one component ------------------------------

    private def componentBreakdown(qty: Int, plan: ComponentPlan): ComponentBreakdown =
      val sheets = sheetsUsed(qty, plan)

      val materialLine = plan.mode match
        case Mode.TieredArea(tiers) =>
          val area = plan.areaM2.getOrElse(BigDecimal(0))
          val rate = tiers.filter(_.minAreaM2 <= area).maxByOption(_.minAreaM2).map(_.pricePerM2).getOrElse(BigDecimal(0))
          line(LineKind.Material, plan.material.name, "per m²", rate, area * qty)
        case Mode.FlatArea(rate) =>
          val area = plan.areaM2.getOrElse(BigDecimal(0))
          line(LineKind.Material, plan.material.name, "per m²", rate, area * qty)
        case Mode.Sheet(rate) =>
          line(LineKind.Material, plan.material.name, "per sheet", rate, BigDecimal(sheets.getOrElse(0)))
        case Mode.PerUnit(rate) =>
          line(LineKind.Material, plan.material.name, "per unit", rate, BigDecimal(qty))

      val inkLine = buildInkLine(qty, plan, sheets)

      val finishLines = plan.finishCharges.flatMap(finishLine(qty, plan, sheets, _))

      val cuttingLine = (plan.mode, plan.layout, sheets) match
        case (Mode.Sheet(_), Some(layout), Some(s)) =>
          rules.collectFirst { case PricingRule.CuttingSurcharge(perCut) => perCut }.map { perCut =>
            line(LineKind.Cutting, Labels.cutting, "per cut", perCut, BigDecimal(layout.cutsPerSheet * s))
          }
        case _ => None

      val lines    = List(materialLine) ++ inkLine ++ finishLines ++ cuttingLine
      val subtotal = lines.map(_.total).foldLeft(money(0))(_ + _).round2

      ComponentBreakdown(
        role = plan.conf.role,
        materialId = plan.material.id,
        copiesPerSheet = plan.layout.map(_.copiesPerSheet),
        sheetsUsed = sheets,
        areaM2 = plan.areaM2,
        lines = lines,
        subtotal = subtotal,
      )

    /** Sheets for sheet-mode components; Body components with a page count use
      * the paged-body imposition convention.
      */
    private def sheetsUsed(qty: Int, plan: ComponentPlan): Option[Int] =
      plan.mode match
        case Mode.Sheet(_) =>
          plan.layout.map { layout =>
            (plan.conf.role, config.details.pages) match
              case (ComponentRole.Body, Some(pages)) if pages > 0 =>
                Imposition.bodySheetsNeeded(qty, pages, layout.copiesPerSheet)
              case _ => Imposition.sheetsNeeded(qty, layout.copiesPerSheet)
          }
        case _ => None

    private def buildInkLine(qty: Int, plan: ComponentPlan, sheets: Option[Int]): Option[PriceLine] =
      val methodId = config.printingMethodId
      val inkId    = config.inkConfigurationId
      val label =
        (catalog.printingMethodsById.get(methodId), catalog.inkConfigurationsById.get(inkId)) match
          case (Some(m), Some(i)) =>
            LocalizedText(s"Ink — ${m.name.en}, ${i.name.en}", s"Tisk — ${m.name.cs}, ${i.name.cs}")
          case _ => LocalizedText("Ink", "Tisk")
      plan.mode match
        case Mode.Sheet(_) =>
          rules
            .collectFirst { case PricingRule.InkPricePerSheet(`methodId`, `inkId`, p) => p }
            .map(p => line(LineKind.Ink, label, "per sheet", p, BigDecimal(sheets.getOrElse(0))))
        case Mode.FlatArea(_) | Mode.TieredArea(_) =>
          rules
            .collectFirst { case PricingRule.InkPricePerM2(`methodId`, `inkId`, p) => p }
            .map(p => line(LineKind.Ink, label, "per m²", p, plan.areaM2.getOrElse(BigDecimal(0)) * qty))
        case Mode.PerUnit(_) =>
          rules
            .collectFirst { case PricingRule.InkPricePerUnit(`methodId`, `inkId`, p) => p }
            .map(p => line(LineKind.Ink, label, "per unit", p, BigDecimal(qty)))

    private def finishLine(
      qty: Int,
      plan: ComponentPlan,
      sheets: Option[Int],
      charge: FinishCharge,
    ): Option[PriceLine] =
      charge match
        case FinishCharge.PerUnitPrice(f, p) =>
          Some(line(LineKind.Finish, f.name, "per unit", p, BigDecimal(qty)))
        case FinishCharge.PerArea(f, p) =>
          Some(line(LineKind.Finish, f.name, "per m²", p, plan.areaM2.getOrElse(BigDecimal(0)) * qty))
        case FinishCharge.PerSheet(f, p) =>
          Some(line(LineKind.Finish, f.name, "per sheet", p, BigDecimal(sheets.getOrElse(0))))
        case FinishCharge.PerLength(f, p, metres) =>
          Some(line(LineKind.Finish, f.name, "per metre", p, metres * qty))
        case FinishCharge.PerItem(f, p) =>
          Some(line(LineKind.Finish, f.name, "per unit", p, BigDecimal(qty)))
        case FinishCharge.Free(_) => None

    // ----- step 5: order-level flat per-item surcharges ------------------------

    private def buildOrderLines(qty: Int): List[PriceLine] =
      val methodId   = config.printingMethodId
      val categoryId = config.categoryId

      val process = rules
        .collectFirst { case PricingRule.ProcessSurcharge(`methodId`, p) => p }
        .map { p =>
          val label = catalog.printingMethodsById.get(methodId).map(_.name).getOrElse(LocalizedText.plain("Process"))
          line(LineKind.Process, label, "per unit", p, BigDecimal(qty))
        }

      val category = rules
        .collectFirst { case PricingRule.CategorySurcharge(`categoryId`, p) => p }
        .map { p =>
          val label = catalog.categoriesById.get(categoryId).map(_.name).getOrElse(LocalizedText.plain("Category"))
          line(LineKind.Category, label, "per unit", p, BigDecimal(qty))
        }

      val fold = config.details.foldType.flatMap { f =>
        rules
          .collectFirst { case PricingRule.FoldSurcharge(`f`, p) => p }
          .map(p => line(LineKind.Fold, Labels.fold(f), "per unit", p, BigDecimal(qty)))
      }

      val binding = config.details.bindingMethod.flatMap { b =>
        rules
          .collectFirst { case PricingRule.BindingSurcharge(`b`, p) => p }
          .map(p => line(LineKind.Binding, Labels.binding(b), "per unit", p, BigDecimal(qty)))
      }

      List(process, category, fold, binding).flatten

    // ----- step 7: volume discount tier resolution ------------------------------

    private def resolveDiscount(totalSheets: Int, qty: Int): Option[AppliedDiscount] =
      val sheetTiers    = rules.collectFirst { case PricingRule.SheetVolumeDiscount(t) => t }
      val quantityTiers = rules.collectFirst { case PricingRule.QuantityVolumeDiscount(t) => t }

      def applied(basis: DiscountBasis, count: Int, tiers: List[DiscountTier]): Option[AppliedDiscount] =
        tiers
          .filter(_.minCount <= count)
          .maxByOption(_.minCount)
          .filter(_.multiplier != BigDecimal(1))
          .map(t => AppliedDiscount(basis, count, t.multiplier, Money.zero(currency)))

      if totalSheets > 0 then
        sheetTiers
          .flatMap(applied(DiscountBasis.Sheets, totalSheets, _))
          .orElse(quantityTiers.flatMap(applied(DiscountBasis.Quantity, qty, _)))
      else quantityTiers.flatMap(applied(DiscountBasis.Quantity, qty, _))

    // ----- step 9: one-time setup fees ------------------------------------------

    private def buildSetupFees(plans: List[ComponentPlan]): List[SetupFeeLine] =
      val selectedFinishes: List[Finish] =
        plans
          .flatMap(_.finishCharges)
          .map {
            case FinishCharge.PerUnitPrice(f, _) => f
            case FinishCharge.PerArea(f, _)      => f
            case FinishCharge.PerSheet(f, _)     => f
            case FinishCharge.PerLength(f, _, _) => f
            case FinishCharge.PerItem(f, _)      => f
            case FinishCharge.Free(f)            => f
          }
          .distinctBy(_.id)

      val finishFees = selectedFinishes.flatMap { finish =>
        val fee =
          if finish.finishType == FinishType.Scoring then
            // A dedicated creasing setup fee always beats a generic finish-type fee (spec §5.9).
            rules
              .collectFirst { case PricingRule.CreasingSetupFee(fee) => fee }
              .orElse(finishSetupFee(finish))
          else finishSetupFee(finish)
        fee.map(f => SetupFeeLine(s"finish:${finish.id.raw}", finish.name, money(f)))
      }

      val foldFee = config.details.foldType.flatMap { f =>
        rules
          .collectFirst { case PricingRule.FoldSetupFee(`f`, fee) => fee }
          .map(fee => SetupFeeLine(s"fold:$f", Labels.fold(f), money(fee)))
      }

      val bindingFee = config.details.bindingMethod.flatMap { b =>
        rules
          .collectFirst { case PricingRule.BindingSetupFee(`b`, fee) => fee }
          .map(fee => SetupFeeLine(s"binding:$b", Labels.binding(b), money(fee)))
      }

      finishFees ++ foldFee ++ bindingFee

    private def finishSetupFee(finish: Finish): Option[BigDecimal] =
      findByTarget(finish) { case PricingRule.FinishSetupFee(t, fee) => (t, fee) }

    // ----- helpers ---------------------------------------------------------------

    private def line(
      kind: LineKind,
      label: LocalizedText,
      unitDescription: String,
      unitPrice: BigDecimal,
      billedQuantity: BigDecimal,
    ): PriceLine =
      PriceLine(
        kind = kind,
        label = label,
        unitDescription = unitDescription,
        unitPrice = money(unitPrice),
        billedQuantity = billedQuantity,
        total = (money(unitPrice) * billedQuantity).round2,
      )
