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

  def formatQuantity(qty: Int, lang: Language): String =
    val sep = if lang == Language.Cs then "\u00A0" else ","
    qty.toString.reverse.grouped(3).mkString(sep).reverse

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

  def materialFamilyLabel(f: MaterialFamily, lang: Language): String = (f, lang) match
    case (MaterialFamily.Paper, Language.Cs)      => "Papír"
    case (MaterialFamily.Paper, _)                => "Paper"
    case (MaterialFamily.Synthetic, Language.Cs)  => "Syntetika"
    case (MaterialFamily.Synthetic, _)            => "Synthetic"
    case (MaterialFamily.Adhesive, Language.Cs)   => "Samolepicí"
    case (MaterialFamily.Adhesive, _)             => "Adhesive"
    case (MaterialFamily.Vinyl, Language.Cs)      => "Vinyl & PVC"
    case (MaterialFamily.Vinyl, _)                => "Vinyl & PVC"
    case (MaterialFamily.Cardboard, Language.Cs)  => "Lepenka"
    case (MaterialFamily.Cardboard, _)            => "Cardboard"
    case (MaterialFamily.RigidBoard, Language.Cs) => "Tuhé desky"
    case (MaterialFamily.RigidBoard, _)           => "Rigid Board"
    case (MaterialFamily.Fabric, Language.Cs)     => "Textil"
    case (MaterialFamily.Fabric, _)               => "Fabric"
    case (MaterialFamily.Ceramic, Language.Cs)    => "Keramika"
    case (MaterialFamily.Ceramic, _)              => "Ceramic"
    case (MaterialFamily.Metal, Language.Cs)      => "Kov"
    case (MaterialFamily.Metal, _)                => "Metal"
    case (MaterialFamily.Wood, Language.Cs)       => "Dřevo"
    case (MaterialFamily.Wood, _)                 => "Wood"
    case (MaterialFamily.Hardware, Language.Cs)   => "Hardware"
    case (MaterialFamily.Hardware, _)             => "Hardware"

  def surfaceCoatingLabel(c: SurfaceCoating, lang: Language): String = (c, lang) match
    case (SurfaceCoating.Uncoated, Language.Cs)        => "Nenatíraný"
    case (SurfaceCoating.Uncoated, _)                  => "Uncoated"
    case (SurfaceCoating.CoatedGloss, Language.Cs)     => "Lesklý"
    case (SurfaceCoating.CoatedGloss, _)               => "Gloss"
    case (SurfaceCoating.CoatedSilk, Language.Cs)      => "Hedvábný"
    case (SurfaceCoating.CoatedSilk, _)                => "Silk"
    case (SurfaceCoating.CoatedMatte, Language.Cs)     => "Matný"
    case (SurfaceCoating.CoatedMatte, _)               => "Matte"
    case (SurfaceCoating.NaturalTextured, Language.Cs) => "Přírodní struktura"
    case (SurfaceCoating.NaturalTextured, _)           => "Natural textured"
    case (SurfaceCoating.TransparentFilm, Language.Cs) => "Průhledná fólie"
    case (SurfaceCoating.TransparentFilm, _)           => "Transparent film"
    case (SurfaceCoating.Metallic, Language.Cs)        => "Metalický"
    case (SurfaceCoating.Metallic, _)                  => "Metallic"
    case (SurfaceCoating.Glazed, Language.Cs)          => "Glazovaný"
    case (SurfaceCoating.Glazed, _)                    => "Glazed"

  def materialPropertyLabel(p: MaterialProperty, lang: Language): String = (p, lang) match
    case (MaterialProperty.Recyclable, Language.Cs)     => "Recyklovatelný"
    case (MaterialProperty.Recyclable, _)               => "Recyclable"
    case (MaterialProperty.WaterResistant, Language.Cs) => "Voděodolný"
    case (MaterialProperty.WaterResistant, _)           => "Water resistant"
    case (MaterialProperty.TearResistant, Language.Cs)  => "Neroztrhnutelný"
    case (MaterialProperty.TearResistant, _)            => "Tear resistant"
    case (MaterialProperty.SelfAdhesive, Language.Cs)   => "Samolepicí"
    case (MaterialProperty.SelfAdhesive, _)             => "Self-adhesive"
    case (MaterialProperty.Transparent, Language.Cs)    => "Průhledný"
    case (MaterialProperty.Transparent, _)              => "Transparent"
    case (MaterialProperty.SmoothSurface, Language.Cs)  => "Hladký povrch"
    case (MaterialProperty.SmoothSurface, _)            => "Smooth surface"
    case (MaterialProperty.Textured, Language.Cs)       => "Strukturovaný"
    case (MaterialProperty.Textured, _)                 => "Textured"
    case (MaterialProperty.Writable, Language.Cs)       => "Popisovatelný"
    case (MaterialProperty.Writable, _)                 => "Writable"
    case (MaterialProperty.OutdoorRated, Language.Cs)   => "Do exteriéru"
    case (MaterialProperty.OutdoorRated, _)             => "Outdoor rated"
    case (MaterialProperty.FoodSafe, Language.Cs)       => "Zdravotně nezávadný"
    case (MaterialProperty.FoodSafe, _)                 => "Food safe"
    case (MaterialProperty.Carbonless, Language.Cs)     => "Samopropisovací"
    case (MaterialProperty.Carbonless, _)               => "Carbonless"

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
    case (OrderStatus.Placed, Language.Cs)           => "Odeslána"
    case (OrderStatus.Placed, _)                     => "Placed"
    case (OrderStatus.AwaitingPayment, Language.Cs)  => "Čeká na platbu"
    case (OrderStatus.AwaitingPayment, _)            => "Awaiting Payment"
    case (OrderStatus.Confirmed, Language.Cs)        => "Potvrzena"
    case (OrderStatus.Confirmed, _)                  => "Confirmed"
    case (OrderStatus.Accepted, Language.Cs)         => "Přijata"
    case (OrderStatus.Accepted, _)                   => "Accepted"
    case (OrderStatus.InProduction, Language.Cs)     => "Ve výrobě"
    case (OrderStatus.InProduction, _)               => "In Progress"
    case (OrderStatus.ReadyForDispatch, Language.Cs) => "Připravena k expedici"
    case (OrderStatus.ReadyForDispatch, _)           => "Ready for Dispatch"
    case (OrderStatus.Dispatched, Language.Cs)       => "Expedována"
    case (OrderStatus.Dispatched, _)                 => "Dispatched"
    case (OrderStatus.Completed, Language.Cs)        => "Dokončena"
    case (OrderStatus.Completed, _)                  => "Completed"
    case (OrderStatus.OnHold(_), Language.Cs)        => "Pozastavena"
    case (OrderStatus.OnHold(_), _)                  => "On Hold"
    case (OrderStatus.Cancelled(_), Language.Cs)     => "Zrušena"
    case (OrderStatus.Cancelled(_), _)               => "Cancelled"

  def orderStatusTone(s: OrderStatus): BadgeTone = s match
    case OrderStatus.Confirmed | OrderStatus.Accepted | OrderStatus.Completed | OrderStatus.Dispatched =>
      BadgeTone.Emerald
    case OrderStatus.AwaitingPayment | OrderStatus.OnHold(_) =>
      BadgeTone.Amber
    case OrderStatus.Cancelled(_) =>
      BadgeTone.Rose
    case OrderStatus.Placed | OrderStatus.InProduction | OrderStatus.ReadyForDispatch =>
      BadgeTone.Brand

  def tierLabel(t: CustomerTier): String = t.toString

  /** Color-coded customer tier pill (Standard = Slate, Silver = Steel Blue, Gold = Warm Gold, Platinum = Royal Violet). */
  def tierBadge(t: CustomerTier, compact: Boolean = false): UI =
    val (bgColor, fgColor, bdColor, icon) = t match
      case CustomerTier.Standard => (surfaceAlt, inkSecondary, Palette.border, "")
      case CustomerTier.Silver   => (silverBg, silverInk, silverBorder, "◆ ")
      case CustomerTier.Gold     => (goldBg, goldInk, goldBorder, "★ ")
      case CustomerTier.Platinum => (platinumBg, platinumInk, platinumBorder, "✦ ")
    val padV = if compact then 1.px else 3.px
    val padH = if compact then 7.px else 9.px
    val fSz  = if compact then 10.px else 12.px
    span(s"$icon${tierLabel(t)}").style(
      Style
        .display(Display.inlineFlex)
        .align(Alignment.center)
        .padding(padV, padH)
        .rounded(999.px)
        .bg(bgColor)
        .color(fgColor)
        .border(1.px, bdColor)
        .fontSize(fSz)
        .fontWeight(FontWeight.w700)
        .lineHeight(1.2),
    )

  def priorityLabel(p: OrderPriority, lang: Language): String = (p, lang) match
    case (OrderPriority.Urgent, Language.Cs)   => "⚡ Urgentní"
    case (OrderPriority.Urgent, _)             => "⚡ Urgent"
    case (OrderPriority.Standard, Language.Cs) => "Standardní"
    case (OrderPriority.Standard, _)           => "Standard"
    case (OrderPriority.Economy, Language.Cs)  => "Ekonomická"
    case (OrderPriority.Economy, _)            => "Economy"

  def priorityBadge(p: OrderPriority, lang: Language): UI =
    val tone = p match
      case OrderPriority.Urgent   => BadgeTone.Rose
      case OrderPriority.Standard => BadgeTone.Brand
      case OrderPriority.Economy  => BadgeTone.Neutral
    badge(priorityLabel(p, lang), tone)

  def paymentStatusLabel(s: PaymentStatus, lang: Language): String = (s, lang) match
    case (PaymentStatus.Confirmed, Language.Cs)  => "Zaplaceno"
    case (PaymentStatus.Confirmed, _)            => "Paid"
    case (PaymentStatus.Pending, Language.Cs)    => "Nezaplaceno"
    case (PaymentStatus.Pending, _)              => "Unpaid"
    case (PaymentStatus.Authorized, Language.Cs) => "Autorizováno"
    case (PaymentStatus.Authorized, _)           => "Authorized"
    case (PaymentStatus.Failed, Language.Cs)     => "Platba selhala"
    case (PaymentStatus.Failed, _)               => "Failed"
    case (PaymentStatus.Refunded, Language.Cs)   => "Vráceno"
    case (PaymentStatus.Refunded, _)             => "Refunded"

  def paymentMethodLabel(m: PaymentMethod, lang: Language): String = (m, lang) match
    case (PaymentMethod.BankTransferQR, Language.Cs)   => "Bankovní převod (QR)"
    case (PaymentMethod.BankTransferQR, _)             => "Bank transfer (QR)"
    case (PaymentMethod.Card, Language.Cs)             => "Platební karta"
    case (PaymentMethod.Card, _)                       => "Card"
    case (PaymentMethod.InvoiceOnAccount, Language.Cs) => "Faktura na účet"
    case (PaymentMethod.InvoiceOnAccount, _)           => "Invoice on account"

  def paymentStatusBadge(s: PaymentStatus, lang: Language): UI =
    val tone = s match
      case PaymentStatus.Confirmed  => BadgeTone.Emerald
      case PaymentStatus.Pending    => BadgeTone.Amber
      case PaymentStatus.Authorized => BadgeTone.Brand
      case PaymentStatus.Failed     => BadgeTone.Rose
      case PaymentStatus.Refunded   => BadgeTone.Neutral
    badge(paymentStatusLabel(s, lang), tone)

  def artworkStatusLabel(s: mpbuilder.artwork.ArtworkStatus, lang: Language): String =
    import mpbuilder.artwork.ArtworkStatus
    (s, lang) match
      case (ArtworkStatus.Uploaded, Language.Cs)            => "Nahráno"
      case (ArtworkStatus.Uploaded, _)                      => "Uploaded"
      case (ArtworkStatus.PreflightPassed, Language.Cs)     => "Preflight OK"
      case (ArtworkStatus.PreflightPassed, _)               => "Preflight Passed"
      case (ArtworkStatus.PreflightFailed, Language.Cs)     => "Preflight chyby"
      case (ArtworkStatus.PreflightFailed, _)               => "Preflight Issues"
      case (ArtworkStatus.ChangesRequested(_), Language.Cs) => "Vyžaduje opravu"
      case (ArtworkStatus.ChangesRequested(_), _)           => "Re-upload Requested"
      case (ArtworkStatus.OperatorApproved(_, _), Language.Cs) => "Schváleno"
      case (ArtworkStatus.OperatorApproved(_, _), _)        => "Approved"
      case (ArtworkStatus.Rejected(_), Language.Cs)         => "Zamítnuto"
      case (ArtworkStatus.Rejected(_), _)                   => "Rejected"

  def artworkStatusBadge(s: mpbuilder.artwork.ArtworkStatus, lang: Language): UI =
    import mpbuilder.artwork.ArtworkStatus
    val tone = s match
      case ArtworkStatus.OperatorApproved(_, _) | ArtworkStatus.PreflightPassed => BadgeTone.Emerald
      case ArtworkStatus.ChangesRequested(_)                                    => BadgeTone.Amber
      case ArtworkStatus.PreflightFailed | ArtworkStatus.Rejected(_)            => BadgeTone.Rose
      case ArtworkStatus.Uploaded                                               => BadgeTone.Neutral
    badge(artworkStatusLabel(s, lang), tone)

  /** Visual 5-step customer-friendly order progress bar:
    *   0: Placed -> 1: Quality Check -> 2: In Production -> 3: Packaged -> 4: Delivery / Completed
    */
  def orderProgressStepper(status: OrderStatus, lang: Language): UI =
    val isCs = lang == Language.Cs
    val steps = List(
      if isCs then "Objednáno" else "Placed",
      if isCs then "Kontrola dat" else "Quality Check",
      if isCs then "Ve výrobě" else "In Production",
      if isCs then "Zabaleno" else "Packaged",
      if isCs then "Doručení" else "Delivery",
    )

    val (activeStep, isHold, isCancelled, noteOpt) = status match
      case OrderStatus.Placed | OrderStatus.AwaitingPayment => (0, false, false, None)
      case OrderStatus.Confirmed | OrderStatus.Accepted     => (1, false, false, None)
      case OrderStatus.InProduction                         => (2, false, false, None)
      case OrderStatus.ReadyForDispatch                     => (3, false, false, None)
      case OrderStatus.Dispatched | OrderStatus.Completed   => (4, false, false, None)
      case OrderStatus.OnHold(reason)                       => (1, true, false, Some(reason.value))
      case OrderStatus.Cancelled(reason)                    => (-1, false, true, Some(reason.value))

    val isFullyComplete = status == OrderStatus.Completed

    div.style(stackSm.width(100.pct))(
      if isCancelled then
        div.style(
          Style.padding(8.px, 12.px).rounded(10.px).bg(roseSoft).color(roseInk).border(1.px, roseBorder).fontSize(12.px).fontWeight(FontWeight.w600),
        )(
          s"✕ ${if isCs then "Objednávka zrušena" else "Order cancelled"}${noteOpt.map(r => s": $r").getOrElse("")}",
        )
      else
        div.cssClass("mp-stepper")(
          steps.zipWithIndex.map { case (lbl, idx) =>
            val done   = isFullyComplete || idx < activeStep
            val active = !isFullyComplete && idx == activeStep
            val dotBg =
              if done then emerald
              else if active && isHold then amber
              else if active then brand
              else surface
            val dotBorder =
              if done then emerald
              else if active && isHold then amber
              else if active then brand
              else borderStrong
            val dotColor =
              if done || active then Color.white else inkMuted
            val dotSymbol =
              if done then "✓"
              else if active && isHold then "!"
              else (idx + 1).toString
            val textColor =
              if done then emeraldInk
              else if active && isHold then amberInk
              else if active then brand
              else inkMuted

            div.cssClass("mp-stepper-step")(
              div.style(
                Style.row.align(Alignment.center).justify(Justification.center).width(100.pct),
              )(
                div.style(
                  Style
                    .flexGrow(1.0)
                    .height(2.px)
                    .bg(if idx == 0 then Color.transparent else if idx <= activeStep || isFullyComplete then emerald else Palette.border),
                ),
                div.style(
                  Style.row
                    .align(Alignment.center)
                    .justify(Justification.center)
                    .width(24.px)
                    .height(24.px)
                    .rounded(999.px)
                    .bg(dotBg)
                    .color(dotColor)
                    .border(2.px, dotBorder)
                    .fontSize(11.px)
                    .fontWeight(FontWeight.w800)
                    .flexShrink(0.0),
                )(dotSymbol),
                div.style(
                  Style
                    .flexGrow(1.0)
                    .height(2.px)
                    .bg(if idx == steps.size - 1 then Color.transparent else if idx < activeStep || isFullyComplete then emerald else Palette.border),
                ),
              ),
              span(lbl).style(
                Style.fontSize(11.px).fontWeight(if active || done then FontWeight.w700 else FontWeight.w500).color(textColor),
              ),
            )
          }*
        ),
      if isHold then
        div.style(
          Style.padding(6.px, 10.px).rounded(8.px).bg(amberSoft).color(amberInk).border(1.px, amberBorder).fontSize(12.px).fontWeight(FontWeight.w600),
        )(
          s"⏸ ${if isCs then "Pozastaveno" else "On hold"}${noteOpt.map(r => s": $r").getOrElse("")}",
        )
      else UI.empty,
    )

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

  // ── Catalog Group Glyphs ───────────────────────────────────────────────────

  /** Accent (foreground) and soft (tint) colors per catalog group; theme-aware via CSS variables. */
  def groupAccent(group: CatalogGroup): (Color, Color) = group match
    case CatalogGroup.Sheet       => (brand, brandSoft)
    case CatalogGroup.LargeFormat => (emerald, emeraldSoft)
    case CatalogGroup.Bound       => (violet, violetSoft)
    case CatalogGroup.Specialty   => (amber, amberSoft)
    case CatalogGroup.Promotional => (sky, skySoft)

  def groupGlyph(group: CatalogGroup): Icons.Icon = group match
    case CatalogGroup.Sheet       => Icons.Icon.FileText
    case CatalogGroup.LargeFormat => Icons.Icon.Image
    case CatalogGroup.Bound       => Icons.Icon.Book
    case CatalogGroup.Specialty   => Icons.Icon.Box
    case CatalogGroup.Promotional => Icons.Icon.Star

  /** Tinted rounded tile holding the group glyph (product lists, detail headers). */
  def groupIcon(group: CatalogGroup, size: Int = 42): UI =
    val (accent, soft) = groupAccent(group)
    div.style(
      Style.row
        .align(Alignment.center)
        .justify(Justification.center)
        .width(size.px)
        .height(size.px)
        .rounded((size * 0.3).toInt.px)
        .bg(soft)
        .color(accent)
        .flexShrink(0.0),
    )(Icons(groupGlyph(group), (size * 0.5).toInt))

  // ── Pluralization ──────────────────────────────────────────────────────────

  /** Picks `<base>-one` / `<base>-few` / `<base>-count` following EN & CS plural rules (CS: 1 | 2–4 | 0, 5+). */
  def pluralKey(base: String, count: Int, lang: Language): String =
    if count == 1 then s"$base-one"
    else if lang == Language.Cs && count >= 2 && count <= 4 then s"$base-few"
    else s"$base-count"

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
      .fill(Svg.Paint.Color(Color.hex("#0F172A").getOrElse(Color.black)))

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
