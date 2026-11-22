package mpbuilder.ui.components

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.ordering.*
import mpbuilder.shared.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*
import java.time.{Instant, ZoneOffset}

/** Reusable formatting utilities, domain-enum localization, badges, icons & SVG QR visual. */
object CommonComponents:

  // ── Formatting Helpers ─────────────────────────────────────────────────────

  def formatMoney(money: Money, currency: Currency, lang: Language): String =
    val rounded = money.rounded.value
    val neg     = rounded < BigDecimal(0)
    val absVal  = rounded.abs
    val raw     = absVal.setScale(2, BigDecimal.RoundingMode.HALF_UP).toString
    val parts   = raw.split('.')
    val intPart = parts(0)
    val decPart = if parts.length > 1 then parts(1) else "00"
    val sep     = if lang == Language.Cs then "\u00A0" else ","
    val decSep  = if lang == Language.Cs then "," else "."
    val grouped = intPart.reverse.grouped(3).mkString(sep).reverse
    val sign    = if neg then "−" else ""
    val num     = s"$sign$grouped$decSep$decPart"
    currency match
      case Currency.CZK => if lang == Language.Cs then s"$num Kč" else s"$num CZK"
      case Currency.USD => s"$sign$$$grouped$decSep$decPart"
      case Currency.EUR => s"$num €"
      case Currency.GBP => s"${sign}£$grouped$decSep$decPart"

  def formatWeight(grams: BigDecimal): String =
    if grams >= BigDecimal(1000) then
      val kg = (grams / BigDecimal(1000)).setScale(2, BigDecimal.RoundingMode.HALF_UP)
      s"$kg kg"
    else
      val g = grams.setScale(0, BigDecimal.RoundingMode.HALF_UP)
      s"$g g"

  private def formatDecimal(v: BigDecimal): String =
    v.bigDecimal.stripTrailingZeros.toPlainString

  def formatDimension(d: Dimension2D): String =
    s"${formatDecimal(d.width.value)} × ${formatDecimal(d.height.value)} mm"

  def formatMaterialWeight(w: MaterialWeight): String = w match
    case MaterialWeight.Grammage(pw)      => s"${pw.gsm} g/m²"
    case MaterialWeight.Thickness(mm, _)  => s"${formatDecimal(mm.value)} mm"
    case MaterialWeight.UnitWeight(grams) => formatWeight(grams.value)

  def formatInstant(instant: Instant): String =
    val dt = instant.atZone(ZoneOffset.UTC)
    f"${dt.getYear}%04d-${dt.getMonthValue}%02d-${dt.getDayOfMonth}%02d ${dt.getHour}%02d:${dt.getMinute}%02d UTC"

  // ── Localized Domain Enum Labels ───────────────────────────────────────────

  def catalogGroupLabel(g: CatalogGroup, lang: Language): String = (g, lang) match
    case (CatalogGroup.Sheet, Language.Cs)       => "Archový tisk"
    case (CatalogGroup.Sheet, _)                 => "Sheet Print"
    case (CatalogGroup.LargeFormat, Language.Cs) => "Velkoformát"
    case (CatalogGroup.LargeFormat, _)           => "Large Format"
    case (CatalogGroup.Bound, Language.Cs)       => "Vazby a brožury"
    case (CatalogGroup.Bound, _)                 => "Bound & Books"
    case (CatalogGroup.Specialty, Language.Cs)   => "Speciální a obaly"
    case (CatalogGroup.Specialty, _)             => "Specialty & Packaging"
    case (CatalogGroup.Promotional, Language.Cs) => "Reklamní předměty"
    case (CatalogGroup.Promotional, _)           => "Promotional"

  def speedLabel(s: ManufacturingSpeed, lang: Language): String = (s, lang) match
    case (ManufacturingSpeed.Express, Language.Cs)  => "Expresní"
    case (ManufacturingSpeed.Express, _)            => "Express"
    case (ManufacturingSpeed.Standard, Language.Cs) => "Standardní"
    case (ManufacturingSpeed.Standard, _)           => "Standard"
    case (ManufacturingSpeed.Economy, Language.Cs)  => "Ekonomická"
    case (ManufacturingSpeed.Economy, _)            => "Economy"

  def foldTypeLabel(ft: FoldType, lang: Language): String = (ft, lang) match
    case (FoldType.Half, Language.Cs)       => "Na půl (V-sklad)"
    case (FoldType.Half, _)                 => "Half Fold"
    case (FoldType.Tri, Language.Cs)        => "Zavinovací (Tri-fold)"
    case (FoldType.Tri, _)                  => "Tri-Fold"
    case (FoldType.Gate, Language.Cs)       => "Okenicový (Gate)"
    case (FoldType.Gate, _)                 => "Gate Fold"
    case (FoldType.Accordion, Language.Cs)  => "Harmonika"
    case (FoldType.Accordion, _)            => "Accordion Fold"
    case (FoldType.ZFold, Language.Cs)      => "Z-sklad"
    case (FoldType.ZFold, _)                => "Z-Fold"
    case (FoldType.RollFold, Language.Cs)   => "Rolovací sklad"
    case (FoldType.RollFold, _)             => "Roll Fold"
    case (FoldType.FrenchFold, Language.Cs) => "Francouzský sklad"
    case (FoldType.FrenchFold, _)           => "French Fold"
    case (FoldType.CrossFold, Language.Cs)  => "Křížový sklad"
    case (FoldType.CrossFold, _)            => "Cross Fold"

  def bindingMethodLabel(b: BindingMethod, lang: Language): String = (b, lang) match
    case (BindingMethod.SaddleStitch, Language.Cs)   => "Vazba V1 (sešitá skobkami)"
    case (BindingMethod.SaddleStitch, _)             => "Saddle Stitch (V1)"
    case (BindingMethod.PerfectBinding, Language.Cs) => "Vazba V2 (lepená)"
    case (BindingMethod.PerfectBinding, _)           => "Perfect Binding (V2)"
    case (BindingMethod.SpiralBinding, Language.Cs)  => "Kroužková spirála"
    case (BindingMethod.SpiralBinding, _)            => "Spiral Binding"
    case (BindingMethod.WireOBinding, Language.Cs)   => "Kovová vazba Twin-Wire (Wire-O)"
    case (BindingMethod.WireOBinding, _)             => "Wire-O Binding"
    case (BindingMethod.CaseBinding, Language.Cs)    => "Pevná knižní vazba (V8)"
    case (BindingMethod.CaseBinding, _)              => "Hardcover Case Binding (V8)"

  def deliveryFormatLabel(df: DeliveryFormat, lang: Language): String = (df, lang) match
    case (DeliveryFormat.CutSingles, Language.Cs)      => "Jednotlivě oříznuté kusy"
    case (DeliveryFormat.CutSingles, _)                => "Cut Singles"
    case (DeliveryFormat.KissCutSheets, Language.Cs)   => "Archy s násekem (Kiss-Cut)"
    case (DeliveryFormat.KissCutSheets, _)             => "Kiss-Cut Sheets"
    case (DeliveryFormat.RollLabels(c, _), Language.Cs) => s"Etikety na roli (dutinka ${c} mm)"
    case (DeliveryFormat.RollLabels(c, _), _)           => s"Roll Labels (${c}mm core)"
    case (DeliveryFormat.PackedUnits, Language.Cs)     => "Balené kusy"
    case (DeliveryFormat.PackedUnits, _)               => "Packed Units"

  def finishCategoryLabel(fc: FinishCategory, lang: Language): String = (fc, lang) match
    case (FinishCategory.Surface, Language.Cs)     => "Povrchová úprava"
    case (FinishCategory.Surface, _)               => "Surface"
    case (FinishCategory.Decorative, Language.Cs)  => "Zušlechtění"
    case (FinishCategory.Decorative, _)            => "Decorative"
    case (FinishCategory.Structural, Language.Cs)  => "Konstrukční"
    case (FinishCategory.Structural, _)            => "Structural"
    case (FinishCategory.LargeFormat, Language.Cs) => "Velkoformátové prvky"
    case (FinishCategory.LargeFormat, _)           => "Large Format"
    case (FinishCategory.Assembly, Language.Cs)    => "Kompletace"
    case (FinishCategory.Assembly, _)              => "Assembly"
    case (FinishCategory.Packaging, Language.Cs)   => "Balení"
    case (FinishCategory.Packaging, _)             => "Packaging"

  def finishSideLabel(fs: FinishSide, lang: Language): String = (fs, lang) match
    case (FinishSide.Front, Language.Cs) => "Přední"
    case (FinishSide.Front, _)           => "Front"
    case (FinishSide.Back, Language.Cs)  => "Zadní"
    case (FinishSide.Back, _)            => "Back"
    case (FinishSide.Both, Language.Cs)  => "Obě strany"
    case (FinishSide.Both, _)            => "Both sides"

  def foilColorLabel(fc: FoilColor, lang: Language): String = (fc, lang) match
    case (FoilColor.Gold, Language.Cs)        => "Zlatá"
    case (FoilColor.Gold, _)                  => "Gold"
    case (FoilColor.Silver, Language.Cs)      => "Stříbrná"
    case (FoilColor.Silver, _)                => "Silver"
    case (FoilColor.Copper, Language.Cs)      => "Měděná"
    case (FoilColor.Copper, _)                => "Copper"
    case (FoilColor.RoseGold, Language.Cs)    => "Růžové zlato"
    case (FoilColor.RoseGold, _)              => "Rose Gold"
    case (FoilColor.Holographic, Language.Cs) => "Holografická"
    case (FoilColor.Holographic, _)           => "Holographic"

  def orderStatusLabel(s: OrderStatus, lang: Language): String = (s, lang) match
    case (OrderStatus.Placed, Language.Cs)          => "Přijata"
    case (OrderStatus.Placed, _)                    => "Placed"
    case (OrderStatus.AwaitingPayment, Language.Cs) => "Čeká na platbu"
    case (OrderStatus.AwaitingPayment, _)           => "Awaiting Payment"
    case (OrderStatus.Confirmed, Language.Cs)       => "Potvrzena"
    case (OrderStatus.Confirmed, _)                 => "Confirmed"
    case (OrderStatus.InProduction, Language.Cs)    => "Ve výrobě"
    case (OrderStatus.InProduction, _)              => "In Production"
    case (OrderStatus.ReadyForDispatch, Language.Cs)=> "Připravena k expedici"
    case (OrderStatus.ReadyForDispatch, _)          => "Ready for Dispatch"
    case (OrderStatus.Dispatched, Language.Cs)      => "Odeslána"
    case (OrderStatus.Dispatched, _)                => "Dispatched"
    case (OrderStatus.Completed, Language.Cs)       => "Dokončena"
    case (OrderStatus.Completed, _)                 => "Completed"
    case (OrderStatus.Cancelled(_), Language.Cs)    => "Zrušena"
    case (OrderStatus.Cancelled(_), _)              => "Cancelled"

  def tierLabel(t: CustomerTier): String = t.toString

  // ── UI Badges & Alerts ─────────────────────────────────────────────────────

  enum BadgeTone:
    case Brand, Emerald, Amber, Rose, Neutral

  def badge(text: String, tone: BadgeTone = BadgeTone.Neutral): UI =
    val (bgColor, fgColor, bdColor) = tone match
      case BadgeTone.Brand   => (brandSoft, brand, brandBorder)
      case BadgeTone.Emerald => (emeraldSoft, emeraldInk, emeraldBorder)
      case BadgeTone.Amber   => (amberSoft, amberInk, amberBorder)
      case BadgeTone.Rose    => (roseSoft, roseInk, roseBorder)
      case BadgeTone.Neutral => (surfaceAlt, inkSecondary, Palette.border)
    span(text).style(
      Style
        .display(Display.inlineFlex)
        .align(Alignment.center)
        .padding(3.px, 9.px)
        .rounded(999.px)
        .bg(bgColor)
        .color(fgColor)
        .border(1.px, bdColor)
        .fontSize(12.px)
        .fontWeight(FontWeight.w600)
        .lineHeight(1.2),
    )

  def reactiveBadge(text: Signal[String], tone: BadgeTone = BadgeTone.Neutral): UI =
    val (bgColor, fgColor, bdColor) = tone match
      case BadgeTone.Brand   => (brandSoft, brand, brandBorder)
      case BadgeTone.Emerald => (emeraldSoft, emeraldInk, emeraldBorder)
      case BadgeTone.Amber   => (amberSoft, amberInk, amberBorder)
      case BadgeTone.Rose    => (roseSoft, roseInk, roseBorder)
      case BadgeTone.Neutral => (surfaceAlt, inkSecondary, Palette.border)
    span(text).style(
      Style
        .display(Display.inlineFlex)
        .align(Alignment.center)
        .padding(3.px, 9.px)
        .rounded(999.px)
        .bg(bgColor)
        .color(fgColor)
        .border(1.px, bdColor)
        .fontSize(12.px)
        .fontWeight(FontWeight.w600)
        .lineHeight(1.2),
    )

  def alertBanner(tone: BadgeTone, title: UI, lines: List[String]): UI =
    val (bgColor, fgColor, bdColor) = tone match
      case BadgeTone.Rose    => (roseSoft, roseInk, roseBorder)
      case BadgeTone.Amber   => (amberSoft, amberInk, amberBorder)
      case BadgeTone.Emerald => (emeraldSoft, emeraldInk, emeraldBorder)
      case _                 => (brandSoft, brandDark, brandBorder)
    div.style(
      Style.column
        .gap(6.px)
        .padding(12.px, 14.px)
        .rounded(12.px)
        .bg(bgColor)
        .color(fgColor)
        .border(1.px, bdColor)
        .fontSize(13.px),
    )(
      div.style(Style.fontWeight(FontWeight.w700))(title),
      UI.fragment(lines.map(msg => div.style(Style.lineHeight(1.4))(s"• $msg"))*),
    )

  // ── Typed SVG Category Icons ───────────────────────────────────────────────

  def groupIcon(group: CatalogGroup): UI =
    val accent = group match
      case CatalogGroup.Sheet       => brand
      case CatalogGroup.LargeFormat => emerald
      case CatalogGroup.Bound       => Color.hex("#7C3AED").getOrElse(Color.purple)
      case CatalogGroup.Specialty   => amber
      case CatalogGroup.Promotional => Color.hex("#0284C7").getOrElse(Color.blue)

    val shapes = group match
      case CatalogGroup.Sheet =>
        Seq(
          Svg.rect.x(6).y(4).width(14).height(18).rx(2).fill(Svg.Paint.None).stroke(Svg.Paint.Color(accent)).strokeWidth(1.8),
          Svg.line.x1(9).y1(9).x2(17).y2(9).stroke(Svg.Paint.Color(accent)).strokeWidth(1.6).strokeLinecap(Svg.StrokeLinecap.Round),
          Svg.line.x1(9).y1(13).x2(17).y2(13).stroke(Svg.Paint.Color(accent)).strokeWidth(1.6).strokeLinecap(Svg.StrokeLinecap.Round),
          Svg.line.x1(9).y1(17).x2(14).y2(17).stroke(Svg.Paint.Color(accent)).strokeWidth(1.6).strokeLinecap(Svg.StrokeLinecap.Round),
        )
      case CatalogGroup.LargeFormat =>
        Seq(
          Svg.rect.x(3).y(5).width(20).height(13).rx(2).fill(Svg.Paint.None).stroke(Svg.Paint.Color(accent)).strokeWidth(1.8),
          Svg.circle.cx(8.5).cy(10).r(1.6).fill(Svg.Paint.Color(accent)),
          Svg.path.d(Svg.PathData.from(5, 16).lineTo(11, 11).lineTo(15, 14).lineTo(18, 11.5).lineTo(21, 16)).fill(Svg.Paint.None).stroke(Svg.Paint.Color(accent)).strokeWidth(1.6),
        )
      case CatalogGroup.Bound =>
        Seq(
          Svg.rect.x(5).y(4).width(16).height(18).rx(2).fill(Svg.Paint.None).stroke(Svg.Paint.Color(accent)).strokeWidth(1.8),
          Svg.line.x1(9).y1(4).x2(9).y2(22).stroke(Svg.Paint.Color(accent)).strokeWidth(1.8),
          Svg.line.x1(12).y1(9).x2(18).y2(9).stroke(Svg.Paint.Color(accent)).strokeWidth(1.5).strokeLinecap(Svg.StrokeLinecap.Round),
        )
      case CatalogGroup.Specialty =>
        Seq(
          Svg.path.d(Svg.PathData.from(13, 3).lineTo(22, 8).lineTo(22, 18).lineTo(13, 23).lineTo(4, 18).lineTo(4, 8).close).fill(Svg.Paint.None).stroke(Svg.Paint.Color(accent)).strokeWidth(1.8),
          Svg.path.d(Svg.PathData.from(4, 8).lineTo(13, 13).lineTo(22, 8)).fill(Svg.Paint.None).stroke(Svg.Paint.Color(accent)).strokeWidth(1.6),
          Svg.line.x1(13).y1(13).x2(13).y2(23).stroke(Svg.Paint.Color(accent)).strokeWidth(1.6),
        )
      case CatalogGroup.Promotional =>
        Seq(
          Svg.circle.cx(13).cy(13).r(8.5).fill(Svg.Paint.None).stroke(Svg.Paint.Color(accent)).strokeWidth(1.8),
          Svg.path.d(Svg.PathData.from(13, 7.5).lineTo(14.7, 11).lineTo(18.5, 11.5).lineTo(15.7, 14.2).lineTo(16.4, 18).lineTo(13, 16.1).lineTo(9.6, 18).lineTo(10.3, 14.2).lineTo(7.5, 11.5).lineTo(11.3, 11).close).fill(Svg.Paint.Color(accent)),
        )

    div.style(
      Style.row
        .align(Alignment.center)
        .justify(Justification.center)
        .width(42.px)
        .height(42.px)
        .rounded(12.px)
        .bg(surfaceAlt)
        .border(1.px, Palette.border)
        .flexShrink(0.0),
    )(
      Svg.svg.width(26).height(26).viewBox(Svg.ViewBox(0, 0, 26, 26))(shapes*),
    )

  /** Crisp deterministic QR-style vector visual rendered with typed `kyo.Svg` for SPD 1.0 bank transfer payload. */
  def qrCodeVisual(payload: String, sizePx: Int = 152): UI =
    val modules = 21
    val cell    = sizePx.toDouble / modules
    val bytes   = payload.getBytes("UTF-8")

    def isFinder(r: Int, c: Int): Boolean =
      def inBox(r0: Int, c0: Int): Boolean =
        val dr = r - r0
        val dc = c - c0
        dr >= 0 && dr < 7 && dc >= 0 && dc < 7
      inBox(0, 0) || inBox(0, modules - 7) || inBox(modules - 7, 0)

    def finderDark(r: Int, c: Int): Boolean =
      val lr = if r >= modules - 7 then r - (modules - 7) else r
      val lc = if c >= modules - 7 then c - (modules - 7) else c
      lr == 0 || lr == 6 || lc == 0 || lc == 6 || (lr >= 2 && lr <= 4 && lc >= 2 && lc <= 4)

    val rects = for
      r <- 0 until modules
      c <- 0 until modules
      dark =
        if isFinder(r, c) then finderDark(r, c)
        else if r == 7 || c == 7 then false
        else
          val idx = (r * modules + c)
          val b   = if bytes.nonEmpty then bytes(idx % bytes.length) & 0xff else 0
          val mix = ((b * 31 + r * 17 + c * 13) ^ (idx * 7)) & 0xff
          (mix % 3) != 0
      if dark
    yield Svg.rect
      .x(c * cell)
      .y(r * cell)
      .width(cell + 0.3)
      .height(cell + 0.3)
      .fill(Svg.Paint.Color(ink))

    div.style(
      Style.padding(10.px)
        .bg(Color.white)
        .border(1.px, Palette.border)
        .rounded(12.px)
        .display(Display.inlineFlex),
    )(
      Svg.svg.width(sizePx).height(sizePx).viewBox(Svg.ViewBox(0, 0, sizePx, sizePx))(
        (Svg.rect.x(0).y(0).width(sizePx).height(sizePx).fill(Svg.Paint.Color(Color.white)) +: rects)*
      ),
    )
