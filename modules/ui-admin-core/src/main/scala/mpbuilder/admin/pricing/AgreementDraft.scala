package mpbuilder.admin.pricing

import mpbuilder.pricing.*
import mpbuilder.shared.*

/** Editable, string-backed form state for a `CustomerPriceAgreement` (backoffice agreement editor).
  *
  * Inputs keep the raw text the staff member typed; `toAgreement` parses and validates everything at
  * once so the form can report every problem together. Parts of the agreement the form cannot edit
  * structurally (custom volume curves, tiered / discrete fixed rates) are carried through untouched
  * and are editable via the JSON mode (`AgreementJson`).
  */
final case class AgreementDraft(
    targetKind: AgreementDraft.TargetKind = AgreementDraft.TargetKind.Tier,
    tier: CustomerTier = CustomerTier.Gold,
    customerId: String = "",
    currency: Currency = Currency.CZK,
    globalDiscount: String = "",
    minimumOrder: String = "",
    categoryDiscounts: Vector[AgreementDraft.DiscountRow] = Vector.empty,
    materialDiscounts: Vector[AgreementDraft.DiscountRow] = Vector.empty,
    finishDiscounts: Vector[AgreementDraft.DiscountRow] = Vector.empty,
    fixedMaterialRates: Vector[AgreementDraft.FixedRateRow] = Vector.empty,
    customVolumeCurves: Option[List[VolumeCurve]] = None,
):
  import AgreementDraft.*

  def target: Option[AgreementTarget] = targetKind match
    case TargetKind.Tier     => Some(AgreementTarget.TierDefault(tier))
    case TargetKind.Customer => Option(customerId.trim).filter(_.nonEmpty).map(id => AgreementTarget.SpecificCustomer(CustomerId.unsafe(id)))

  def section(s: Section): Vector[DiscountRow] = s match
    case Section.Category => categoryDiscounts
    case Section.Material => materialDiscounts
    case Section.Finish   => finishDiscounts

  def updateSection(s: Section)(f: Vector[DiscountRow] => Vector[DiscountRow]): AgreementDraft = s match
    case Section.Category => copy(categoryDiscounts = f(categoryDiscounts))
    case Section.Material => copy(materialDiscounts = f(materialDiscounts))
    case Section.Finish   => copy(finishDiscounts = f(finishDiscounts))

  /** Parses & validates the whole draft, accumulating every issue. */
  def toAgreement: Either[List[Issue], CustomerPriceAgreement] =
    val issues = List.newBuilder[Issue]

    val tgt = target
    if tgt.isEmpty then issues += Issue("Select a customer for the agreement.", "Vyberte zákazníka pro smlouvu.")

    def percent(raw: String, en: String, cs: String): Option[Percentage] =
      parseDecimal(raw) match
        case None => issues += Issue(s"$en: '$raw' is not a number.", s"$cs: '$raw' není číslo."); None
        case Some(v) =>
          Percentage(v).toEither match
            case Right(p) => Some(p)
            case Left(_)  => issues += Issue(s"$en: discount must be between 0 and 100 %.", s"$cs: sleva musí být mezi 0 a 100 %."); None

    def money(raw: String, en: String, cs: String): Option[Money] =
      parseDecimal(raw) match
        case None => issues += Issue(s"$en: '$raw' is not a number.", s"$cs: '$raw' není číslo."); None
        case Some(v) if v < 0 => issues += Issue(s"$en: amount must not be negative.", s"$cs: částka nesmí být záporná."); None
        case Some(v) => Some(Money(v))

    val global =
      if globalDiscount.trim.isEmpty then None
      else percent(globalDiscount, "Global discount", "Globální sleva").filter(_.value > 0)

    val minOrder =
      if minimumOrder.trim.isEmpty then None
      else money(minimumOrder, "Minimum order", "Minimální objednávka").filter(_.value > 0)

    def discounts[K](s: Section, mkKey: String => K): Map[K, Percentage] =
      val rows = section(s)
      val (en, cs) = s.labels
      rows.zipWithIndex.foreach { case (r, i) =>
        if r.key.trim.isEmpty then issues += Issue(s"$en row ${i + 1}: select an item.", s"$cs řádek ${i + 1}: vyberte položku.")
      }
      rows.map(_.key.trim).filter(_.nonEmpty).groupBy(identity).collect { case (k, vs) if vs.size > 1 => k }.toList.sorted.foreach { k =>
        issues += Issue(s"$en: '$k' is listed more than once.", s"$cs: '$k' je uvedeno vícekrát.")
      }
      rows.filter(_.key.trim.nonEmpty).flatMap { r =>
        percent(r.percent, s"$en '${r.key.trim}'", s"$cs '${r.key.trim}'").map(p => mkKey(r.key.trim) -> p)
      }.toMap

    val catD = discounts(Section.Category, CategoryId.unsafe)
    val matD = discounts(Section.Material, MaterialId.unsafe)
    val finD = discounts(Section.Finish, FinishId.unsafe)

    fixedMaterialRates.zipWithIndex.foreach { case (r, i) =>
      if r.materialId.trim.isEmpty then issues += Issue(s"Fixed rate row ${i + 1}: select a material.", s"Fixní sazba řádek ${i + 1}: vyberte materiál.")
    }
    fixedMaterialRates.map(_.materialId.trim).filter(_.nonEmpty).groupBy(identity).collect { case (k, vs) if vs.size > 1 => k }.toList.sorted.foreach { k =>
      issues += Issue(s"Fixed rates: material '$k' is listed more than once.", s"Fixní sazby: materiál '$k' je uveden vícekrát.")
    }
    val fixed: Map[MaterialId, RateModel] =
      fixedMaterialRates.filter(_.materialId.trim.nonEmpty).flatMap { r =>
        val mid = r.materialId.trim
        r.kind match
          case RateKind.Advanced => r.advanced.map(MaterialId.unsafe(mid) -> _)
          case k =>
            money(r.amount, s"Fixed rate '$mid'", s"Fixní sazba '$mid'").map(m => MaterialId.unsafe(mid) -> k.build(m))
      }.toMap

    val all = issues.result()
    if all.nonEmpty then Left(all)
    else
      Right(
        CustomerPriceAgreement(
          target = tgt.get,
          currency = currency,
          globalDiscount = global,
          categoryDiscounts = catD,
          materialDiscounts = matD,
          fixedMaterialRates = fixed,
          finishDiscounts = finD,
          customVolumeCurves = customVolumeCurves,
          minimumOrderOverride = minOrder,
        )
      )

object AgreementDraft:

  enum TargetKind:
    case Tier, Customer

  enum Section:
    case Category, Material, Finish

    def labels: (String, String) = this match
      case Category => ("Category discount", "Sleva na kategorii")
      case Material => ("Material discount", "Sleva na materiál")
      case Finish   => ("Finish discount", "Sleva na zušlechtění")

  final case class DiscountRow(key: String = "", percent: String = "")

  /** Simple single-amount rate models are editable in the form; the rest are `Advanced` (JSON only). */
  enum RateKind:
    case PerUnit, PerSheet, PerSqMeter, PerLinearMeter, PerPerimeterMeter, FlatFee, Advanced

    def build(m: Money): RateModel = this match
      case PerUnit           => RateModel.PerUnit(m)
      case PerSheet          => RateModel.PerSheet(m)
      case PerSqMeter        => RateModel.PerSqMeter(m)
      case PerLinearMeter    => RateModel.PerLinearMeter(m)
      case PerPerimeterMeter => RateModel.PerPerimeterMeter(m)
      case FlatFee           => RateModel.FlatFee(m)
      case Advanced          => RateModel.PerUnit(m)

  object RateKind:
    val editable: List[RateKind] = List(PerUnit, PerSheet, PerSqMeter, PerLinearMeter, PerPerimeterMeter, FlatFee)

    def label(k: RateKind, isCs: Boolean): String = k match
      case PerUnit           => if isCs then "za kus" else "per unit"
      case PerSheet          => if isCs then "za arch" else "per sheet"
      case PerSqMeter        => if isCs then "za m²" else "per m²"
      case PerLinearMeter    => if isCs then "za běžný metr" else "per linear m"
      case PerPerimeterMeter => if isCs then "za metr obvodu" else "per perimeter m"
      case FlatFee           => if isCs then "paušál" else "flat fee"
      case Advanced          => if isCs then "pokročilá (JSON)" else "advanced (JSON)"

  final case class FixedRateRow(
      materialId: String = "",
      kind: RateKind = RateKind.PerSheet,
      amount: String = "",
      advanced: Option[RateModel] = None,
  )

  object FixedRateRow:
    def from(materialId: MaterialId, rm: RateModel): FixedRateRow =
      val id = materialId.value
      rm match
        case RateModel.PerUnit(r)               => FixedRateRow(id, RateKind.PerUnit, plain(r.value))
        case RateModel.PerSheet(r)              => FixedRateRow(id, RateKind.PerSheet, plain(r.value))
        case RateModel.PerSqMeter(r, None)      => FixedRateRow(id, RateKind.PerSqMeter, plain(r.value))
        case RateModel.PerLinearMeter(r)        => FixedRateRow(id, RateKind.PerLinearMeter, plain(r.value))
        case RateModel.PerPerimeterMeter(r)     => FixedRateRow(id, RateKind.PerPerimeterMeter, plain(r.value))
        case RateModel.FlatFee(r)               => FixedRateRow(id, RateKind.FlatFee, plain(r.value))
        case other                              => FixedRateRow(id, RateKind.Advanced, "", Some(other))

  /** A validation problem, carried in both UI languages. */
  final case class Issue(en: String, cs: String):
    def text(isCs: Boolean): String = if isCs then cs else en

  def empty(target: AgreementTarget, currency: Currency): AgreementDraft =
    target match
      case AgreementTarget.TierDefault(t)        => AgreementDraft(TargetKind.Tier, tier = t, currency = currency)
      case AgreementTarget.SpecificCustomer(cid) => AgreementDraft(TargetKind.Customer, customerId = cid.value, currency = currency)

  def from(a: CustomerPriceAgreement): AgreementDraft =
    val base = empty(a.target, a.currency)
    base.copy(
      globalDiscount = a.globalDiscount.map(p => plain(p.value)).getOrElse(""),
      minimumOrder = a.minimumOrderOverride.map(m => plain(m.value)).getOrElse(""),
      categoryDiscounts = a.categoryDiscounts.toVector.sortBy(_._1.value).map((k, p) => DiscountRow(k.value, plain(p.value))),
      materialDiscounts = a.materialDiscounts.toVector.sortBy(_._1.value).map((k, p) => DiscountRow(k.value, plain(p.value))),
      finishDiscounts = a.finishDiscounts.toVector.sortBy(_._1.value).map((k, p) => DiscountRow(k.value, plain(p.value))),
      fixedMaterialRates = a.fixedMaterialRates.toVector.sortBy(_._1.value).map((k, rm) => FixedRateRow.from(k, rm)),
      customVolumeCurves = a.customVolumeCurves,
    )

  /** Accepts both `.` and `,` as the decimal separator. */
  def parseDecimal(raw: String): Option[BigDecimal] =
    val s = raw.trim.replace(',', '.')
    if s.isEmpty then None else scala.util.Try(BigDecimal(s)).toOption

  private def plain(v: BigDecimal): String = v.bigDecimal.stripTrailingZeros.toPlainString

  /** Stable identity of an agreement (target + currency), used as row key and to detect re-targeting. */
  def keyOf(target: AgreementTarget, currency: Currency): String =
    val t = target match
      case AgreementTarget.SpecificCustomer(cid) => s"cust:${cid.value}"
      case AgreementTarget.TierDefault(tier)     => s"tier:$tier"
    s"$t:$currency"

  def keyOf(a: CustomerPriceAgreement): String = keyOf(a.target, a.currency)
