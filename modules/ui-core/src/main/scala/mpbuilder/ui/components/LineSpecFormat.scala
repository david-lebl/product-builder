package mpbuilder.ui.components

import mpbuilder.catalog.{BindingMethod, DeliveryFormat, FinishSide, FoilColor, FoldType}
import mpbuilder.ordering.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*

/** Localized, human-readable rendering of the product configuration frozen on a cart / order line
  * (`ProductSpecSnapshot`, `FinishSnapshot`). Reuses the catalog enum labels so the cart, My orders and
  * the backoffice describe a line with the same words the configurator used.
  */
object LineSpecFormat:

  private def cs(lang: Language): Boolean = lang == Language.Cs

  private def decimal(v: BigDecimal): String = v.bigDecimal.stripTrailingZeros.toPlainString

  def orientationLabel(o: ProductSpecSnapshot.Orientation, lang: Language): String = (o, cs(lang)) match
    case (ProductSpecSnapshot.Orientation.Portrait, true)   => "Na výšku"
    case (ProductSpecSnapshot.Orientation.Portrait, false)  => "Portrait"
    case (ProductSpecSnapshot.Orientation.Landscape, true)  => "Na šířku"
    case (ProductSpecSnapshot.Orientation.Landscape, false) => "Landscape"

  def foldLabel(ft: ProductSpecSnapshot.FoldType, lang: Language): String =
    val catalogFold = ft match
      case ProductSpecSnapshot.FoldType.Half       => FoldType.Half
      case ProductSpecSnapshot.FoldType.Tri        => FoldType.Tri
      case ProductSpecSnapshot.FoldType.Gate       => FoldType.Gate
      case ProductSpecSnapshot.FoldType.Accordion  => FoldType.Accordion
      case ProductSpecSnapshot.FoldType.ZFold      => FoldType.ZFold
      case ProductSpecSnapshot.FoldType.RollFold   => FoldType.RollFold
      case ProductSpecSnapshot.FoldType.FrenchFold => FoldType.FrenchFold
      case ProductSpecSnapshot.FoldType.CrossFold  => FoldType.CrossFold
    foldTypeLabel(catalogFold, lang)

  def bindingLabel(b: ProductSpecSnapshot.Binding, lang: Language): String =
    val method = b.method match
      case ProductSpecSnapshot.BindingMethod.SaddleStitch   => BindingMethod.SaddleStitch
      case ProductSpecSnapshot.BindingMethod.PerfectBinding => BindingMethod.PerfectBinding
      case ProductSpecSnapshot.BindingMethod.SpiralBinding  => BindingMethod.SpiralBinding
      case ProductSpecSnapshot.BindingMethod.WireOBinding   => BindingMethod.WireOBinding
      case ProductSpecSnapshot.BindingMethod.CaseBinding    => BindingMethod.CaseBinding
    val pages     = if cs(lang) then s"${b.totalPages} stran" else s"${b.totalPages} pp."
    val selfCover = if !b.selfCover then "" else if cs(lang) then " · obálka ze stejného papíru" else " · self-cover"
    s"${bindingMethodLabel(method, lang)} · $pages$selfCover"

  def deliveryLabel(df: ProductSpecSnapshot.DeliveryFormat, lang: Language): String =
    val catalogFormat = df match
      case ProductSpecSnapshot.DeliveryFormat.CutSingles            => DeliveryFormat.CutSingles
      case ProductSpecSnapshot.DeliveryFormat.KissCutSheets         => DeliveryFormat.KissCutSheets
      case ProductSpecSnapshot.DeliveryFormat.RollLabels(core, dir) => DeliveryFormat.RollLabels(core, dir)
      case ProductSpecSnapshot.DeliveryFormat.PackedUnits           => DeliveryFormat.PackedUnits
    val unwind = df match
      case ProductSpecSnapshot.DeliveryFormat.RollLabels(_, dir) =>
        if cs(lang) then s", odvin $dir" else s", unwind $dir"
      case _ => ""
    deliveryFormatLabel(catalogFormat, lang) + unwind

  def dimensionsLabel(d: ProductSpecSnapshot.Dimensions, lang: Language): String = d match
    case ProductSpecSnapshot.Dimensions.Flat2D(size)        => formatDimension(size)
    case ProductSpecSnapshot.Dimensions.Folded2D(size, ft)  => s"${formatDimension(size)} · ${foldLabel(ft, lang)}"
    case ProductSpecSnapshot.Dimensions.Box3D(box, blank) =>
      val base = s"${decimal(box.length.value)} × ${decimal(box.width.value)} × ${decimal(box.height.value)} mm"
      blank.fold(base)(b => s"$base (${if cs(lang) then "výsek" else "blank"} ${formatDimension(b)})")
    case ProductSpecSnapshot.Dimensions.Circular(diameter) => s"⌀ ${decimal(diameter.value)} mm"
    case ProductSpecSnapshot.Dimensions.PrintArea(area, label) =>
      val base = s"${if cs(lang) then "Tisková plocha" else "Print area"} ${formatDimension(area)}"
      label.fold(base)(l => s"$base · $l")

  /** Ordered (label, value) rows describing the physical spec. */
  def specRows(specs: ProductSpecSnapshot, lang: Language): List[(String, String)] =
    val isCs = cs(lang)
    List(
      Some((if isCs then "Formát" else "Format") -> dimensionsLabel(specs.dimensions, lang)),
      specs.orientation.map(o => (if isCs then "Orientace" else "Orientation") -> orientationLabel(o, lang)),
      Some((if isCs then "Spadávka" else "Bleed") -> s"${decimal(specs.bleed.value)} mm"),
      specs.binding.map(b => (if isCs then "Vazba" else "Binding") -> bindingLabel(b, lang)),
      Some((if isCs then "Dodání" else "Delivery") -> deliveryLabel(specs.deliveryFormat, lang)),
    ).flatten

  /** One-line spec summary for compact places (cart drawer, My orders). Skips the format, which is shown elsewhere. */
  def compactSummary(specs: ProductSpecSnapshot, lang: Language): String =
    List(
      specs.orientation.map(orientationLabel(_, lang)),
      Some(s"${if cs(lang) then "spadávka" else "bleed"} ${decimal(specs.bleed.value)} mm"),
      specs.binding.map(bindingLabel(_, lang)),
      Some(specs.deliveryFormat).filter(_ != ProductSpecSnapshot.DeliveryFormat.CutSingles).map(deliveryLabel(_, lang)),
    ).flatten.mkString(" · ")

  def sideLabel(s: FinishSnapshot.Side, lang: Language): String =
    finishSideLabel(
      s match
        case FinishSnapshot.Side.Front => FinishSide.Front
        case FinishSnapshot.Side.Back  => FinishSide.Back
        case FinishSnapshot.Side.Both  => FinishSide.Both
      ,
      lang,
    )

  def paramsLabel(p: FinishSnapshot.Params, lang: Language): String =
    val isCs = cs(lang)
    p match
      case FinishSnapshot.Params.RoundCorners(n, r) =>
        if isCs then s"$n rohy, r $r mm" else s"$n corners, r $r mm"
      case FinishSnapshot.Params.FoilStamping(c) =>
        foilColorLabel(
          c match
            case FinishSnapshot.FoilColor.Gold        => FoilColor.Gold
            case FinishSnapshot.FoilColor.Silver      => FoilColor.Silver
            case FinishSnapshot.FoilColor.Copper      => FoilColor.Copper
            case FinishSnapshot.FoilColor.RoseGold    => FoilColor.RoseGold
            case FinishSnapshot.FoilColor.Holographic => FoilColor.Holographic
          ,
          lang,
        )
      case FinishSnapshot.Params.Grommets(s)     => if isCs then s"rozteč $s mm" else s"every $s mm"
      case FinishSnapshot.Params.Perforation(p)  => if isCs then s"rozteč $p mm" else s"pitch $p mm"
      case FinishSnapshot.Params.LinearLength(m) => s"${decimal(m)} m"
      case FinishSnapshot.Params.Scoring(n)      => if isCs then s"$n bigů" else s"$n creases"

  /** "Matte Lamination (Both sides)", "Rounded Corners (4 corners, r 3 mm)". */
  def finishLabel(f: FinishSnapshot, lang: Language): String =
    val details = f.side.map(sideLabel(_, lang)).toList ++ f.params.map(paramsLabel(_, lang)).toList
    if details.isEmpty then f.name(lang) else s"${f.name(lang)} (${details.mkString(", ")})"

  def componentFinishes(comp: ComponentSnapshot, lang: Language): List[String] =
    comp.finishes.map(finishLabel(_, lang))

  /** Compact spec + finishes line for a cart / order line. */
  def lineSummary(product: ProductSnapshot, lang: Language): String =
    val finishes = product.components.toList.flatMap(_.finishes.map(finishLabel(_, lang))).distinct
    (compactSummary(product.specs, lang) :: finishes).mkString(" · ")
