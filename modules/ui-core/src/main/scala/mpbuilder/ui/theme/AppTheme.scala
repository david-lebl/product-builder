package mpbuilder.ui.theme

import kyo.*
import kyo.Style.*

/** Cohesive, responsive design system built entirely with typed `kyo.Stylesheet` & `kyo.Style`.
  *
  * Mobile-first breakpoints:
  *   - Desktop: > 960px (2-column configurator/checkout with sticky side summary, 3-col catalog cards)
  *   - Tablet:  <= 960px (stacked configurator/checkout, 2-col catalog cards, compact header)
  *   - Mobile:  <= 640px (single-column cards, full-width drawer/modal, touch-friendly >= 40px targets)
  */
object AppTheme:

  // ── Typed Color Palette ────────────────────────────────────────────────────

  object Palette:
    private[theme] def hexColor(raw: String, fallback: Color = Color.slate): Color =
      Color.hex(raw).getOrElse(fallback)

    val canvas: Color         = hexColor("#F8FAFC")
    val surface: Color        = Color.white
    val surfaceAlt: Color     = hexColor("#F1F5F9")
    val surfaceHover: Color   = hexColor("#EEF2F7")
    val border: Color         = hexColor("#E2E8F0")
    val borderStrong: Color   = hexColor("#CBD5E1")
    val ink: Color            = hexColor("#0F172A")
    val inkSecondary: Color   = hexColor("#475569")
    val inkMuted: Color       = hexColor("#64748B")
    val brand: Color          = hexColor("#4F46E5", Color.indigo)
    val brandHover: Color     = hexColor("#4338CA", Color.indigo)
    val brandSoft: Color      = hexColor("#EEF2FF")
    val brandBorder: Color    = hexColor("#C7D2FE")
    val brandDark: Color      = hexColor("#312E81")
    val emerald: Color        = hexColor("#059669", Color.green)
    val emeraldSoft: Color    = hexColor("#ECFDF5")
    val emeraldBorder: Color  = hexColor("#A7F3D0")
    val emeraldInk: Color     = hexColor("#065F46")
    val amber: Color          = hexColor("#D97706", Color.orange)
    val amberSoft: Color      = hexColor("#FFFBEB")
    val amberBorder: Color    = hexColor("#FDE68A")
    val amberInk: Color       = hexColor("#92400E")
    val rose: Color           = hexColor("#DC2626", Color.red)
    val roseSoft: Color       = hexColor("#FEF2F2")
    val roseBorder: Color     = hexColor("#FECACA")
    val roseInk: Color        = hexColor("#991B1B")
    val backdrop: Color       = Color.rgba(15, 23, 42, 0.48)
    val glassHeader: Color    = Color.rgba(255, 255, 255, 0.92)
    val shadowSubtle: Color   = Color.rgba(15, 23, 42, 0.06)
    val shadowMedium: Color   = Color.rgba(15, 23, 42, 0.12)
    val shadowStrong: Color   = Color.rgba(15, 23, 42, 0.22)

  import Palette.*

  // ── Shared Inline Style Helpers ────────────────────────────────────────────

  val stackXs: Style   = Style.column.gap(4.px)
  val stackSm: Style   = Style.column.gap(8.px)
  val stackMd: Style   = Style.column.gap(16.px)
  val stackLg: Style   = Style.column.gap(24.px)
  val rowXs: Style     = Style.row.gap(6.px).align(Alignment.center)
  val rowSm: Style     = Style.row.gap(8.px).align(Alignment.center)
  val rowMd: Style     = Style.row.gap(12.px).align(Alignment.center)
  val rowWrap: Style   = Style.row.gap(8.px).flexWrap(FlexWrap.wrap).align(Alignment.center)
  val rowBetween: Style = Style.row.justify(Justification.spaceBetween).align(Alignment.center).gap(12.px)

  val labelStyle: Style =
    Style.fontSize(13.px).fontWeight(FontWeight.w600).color(inkSecondary)

  val mutedText: Style =
    Style.fontSize(13.px).color(inkMuted).lineHeight(1.5)

  val smallText: Style =
    Style.fontSize(12.px).color(inkMuted).lineHeight(1.4)

  val sectionTitle: Style =
    Style.fontSize(18.px).fontWeight(FontWeight.w700).color(ink).letterSpacing((-0.2).px)

  val dividerStyle: Style =
    Style.height(1.px).width(100.pct).bg(border).border(0.px, BorderStyle.none, Color.transparent)

  // ── Core Responsive Stylesheet (Shared by Storefront SPA + Calculator) ─────

  val coreStylesheet: Stylesheet =
    Stylesheet
      .vars(
        "mp-canvas"  -> "#F8FAFC",
        "mp-surface" -> "#FFFFFF",
        "mp-ink"     -> "#0F172A",
        "mp-brand"   -> "#4F46E5",
      )
      // Root containers
      .rule(
        "mp-app",
        Style.column
          .minHeight(100.pct)
          .bg(canvas)
          .color(ink)
          .fontFamily(FontFamily.SystemUi)
          .fontSize(15.px)
          .lineHeight(1.5),
      )
      .rule(
        "mp-calc-root",
        Style.column
          .bg(canvas)
          .color(ink)
          .fontFamily(FontFamily.SystemUi)
          .fontSize(15.px)
          .lineHeight(1.5)
          .rounded(16.px)
          .border(1.px, border)
          .padding(20.px)
          .gap(20.px),
      )
      .rule(
        "mp-container",
        Style.column
          .width(100.pct)
          .maxWidth(1240.px)
          .margin(0.px, Length.Auto)
          .padding(0.px, 24.px),
      )
      // Sticky Header
      .rule(
        "mp-header",
        Style.position(Position.sticky)
          .top(0.px)
          .zIndex(40)
          .width(100.pct)
          .bg(glassHeader)
          .borderBottom(1.px, border)
          .shadow(0.px, 1.px, 8.px, 0.px, shadowSubtle),
      )
      .rule(
        "mp-header-inner",
        Style.row
          .width(100.pct)
          .maxWidth(1240.px)
          .margin(0.px, Length.Auto)
          .padding(12.px, 24.px)
          .align(Alignment.center)
          .justify(Justification.spaceBetween)
          .gap(16.px),
      )
      .rule(
        "mp-desktop-nav",
        Style.row.gap(4.px).align(Alignment.center),
      )
      .rule(
        "mp-mobile-only",
        Style.displayNone,
      )
      .rule(
        "mp-mobile-menu",
        Style.column
          .width(100.pct)
          .bg(surface)
          .borderBottom(1.px, border)
          .padding(12.px, 20.px)
          .gap(8.px),
      )
      // Hero Banner
      .rule(
        "mp-hero",
        Style.column
          .width(100.pct)
          .padding(40.px, 32.px)
          .rounded(20.px)
          .bgGradient(GradientDirection.toBottomRight, (brandDark, 0.pct), (brand, 65.pct), (hexColor("#7C3AED"), 100.pct))
          .color(Color.white)
          .gap(16.px)
          .shadow(0.px, 12.px, 32.px, 0.px, Color.rgba(79, 70, 229, 0.22)),
      )
      .rule(
        "mp-hero-title",
        Style.fontSize(36.px).fontWeight(FontWeight.w800).lineHeight(1.15).letterSpacing((-0.6).px).color(Color.white),
      )
      // Cards & Grids (responsive flex grids)
      .rule(
        "mp-card",
        Style.column
          .bg(surface)
          .border(1.px, border)
          .rounded(14.px)
          .padding(20.px)
          .gap(14.px)
          .shadow(0.px, 1.px, 3.px, 0.px, shadowSubtle),
      )
      .rule(
        "mp-card-product",
        Style.column
          .bg(surface)
          .border(1.px, border)
          .rounded(16.px)
          .padding(20.px)
          .gap(12.px)
          .flexGrow(1.0)
          .flexShrink(1.0)
          .flexBasis(320.px)
          .maxWidth(420.px)
          .shadow(0.px, 1.px, 4.px, 0.px, shadowSubtle)
          .transition(TransitionProperty.all, 160, Easing.easeOut)
          .hover(Style.borderColor(brandBorder).shadow(0.px, 8.px, 24.px, 0.px, shadowMedium)),
      )
      .rule(
        "mp-card-grid",
        Style.row
          .flexWrap(FlexWrap.wrap)
          .gap(18.px)
          .align(Alignment.stretch),
      )
      .rule(
        "mp-option-grid",
        Style.row
          .flexWrap(FlexWrap.wrap)
          .gap(10.px)
          .align(Alignment.stretch),
      )
      .rule(
        "mp-option-card",
        Style.column
          .bg(surface)
          .border(1.px, border)
          .rounded(12.px)
          .padding(12.px, 14.px)
          .gap(4.px)
          .flexGrow(1.0)
          .flexBasis(200.px)
          .cursor(Cursor.pointer)
          .textAlign(TextAlign.left)
          .transition(TransitionProperty.all, 140, Easing.easeOut)
          .hover(Style.borderColor(brandBorder).bg(surfaceAlt)),
      )
      .rule(
        "mp-option-card-active",
        Style.column
          .bg(brandSoft)
          .border(2.px, brand)
          .rounded(12.px)
          .padding(11.px, 13.px)
          .gap(4.px)
          .flexGrow(1.0)
          .flexBasis(200.px)
          .cursor(Cursor.pointer)
          .textAlign(TextAlign.left)
          .shadow(0.px, 2.px, 8.px, 0.px, Color.rgba(79, 70, 229, 0.14)),
      )
      // 2-Column Responsive Split (Configurator & Checkout)
      .rule(
        "mp-split-layout",
        Style.row
          .width(100.pct)
          .gap(24.px)
          .align(Alignment.start),
      )
      .rule(
        "mp-split-main",
        Style.column
          .flexGrow(1.0)
          .flexShrink(1.0)
          .minWidth(0.px)
          .gap(20.px),
      )
      .rule(
        "mp-split-aside",
        Style.column
          .width(380.px)
          .flexShrink(0.0)
          .position(Position.sticky)
          .top(82.px)
          .gap(16.px),
      )
      // Buttons
      .rule(
        "mp-btn",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .gap(6.px)
          .padding(9.px, 16.px)
          .rounded(10.px)
          .fontSize(14.px)
          .fontWeight(FontWeight.w600)
          .cursor(Cursor.pointer)
          .border(1.px, Color.transparent)
          .transition(TransitionProperty.all, 140, Easing.easeOut),
      )
      .rule(
        "mp-btn-primary",
        Style.bg(brand)
          .color(Color.white)
          .hover(Style.bg(brandHover))
          .disabled(Style.opacity(0.5).cursor(Cursor.notAllowed)),
      )
      .rule(
        "mp-btn-secondary",
        Style.bg(surface)
          .color(ink)
          .border(1.px, borderStrong)
          .hover(Style.bg(surfaceAlt).borderColor(brandBorder))
          .disabled(Style.opacity(0.5).cursor(Cursor.notAllowed)),
      )
      .rule(
        "mp-btn-ghost",
        Style.bg(Color.transparent)
          .color(inkSecondary)
          .hover(Style.bg(surfaceAlt).color(ink)),
      )
      .rule(
        "mp-btn-nav-active",
        Style.bg(brandSoft).color(brand),
      )
      .rule(
        "mp-btn-danger",
        Style.bg(roseSoft)
          .color(roseInk)
          .border(1.px, roseBorder)
          .hover(Style.bg(rose).color(Color.white)),
      )
      .rule(
        "mp-btn-sm",
        Style.padding(6.px, 12.px).fontSize(13.px).rounded(8.px),
      )
      .rule(
        "mp-btn-lg",
        Style.padding(12.px, 20.px).fontSize(15.px).rounded(12.px).width(100.pct),
      )
      // Pills / Chips
      .rule(
        "mp-pill",
        Style.row
          .align(Alignment.center)
          .gap(6.px)
          .padding(7.px, 14.px)
          .rounded(999.px)
          .fontSize(13.px)
          .fontWeight(FontWeight.w600)
          .bg(surface)
          .color(inkSecondary)
          .border(1.px, border)
          .cursor(Cursor.pointer)
          .transition(TransitionProperty.all, 140, Easing.easeOut)
          .hover(Style.borderColor(brandBorder).color(brand).bg(brandSoft)),
      )
      .rule(
        "mp-pill-active",
        Style.row
          .align(Alignment.center)
          .gap(6.px)
          .padding(7.px, 14.px)
          .rounded(999.px)
          .fontSize(13.px)
          .fontWeight(FontWeight.w600)
          .bg(brand)
          .color(Color.white)
          .border(1.px, brand)
          .cursor(Cursor.pointer),
      )
      // Form Inputs
      .rule(
        "mp-input",
        Style.width(100.pct)
          .padding(10.px, 12.px)
          .rounded(10.px)
          .border(1.px, borderStrong)
          .bg(surface)
          .color(ink)
          .fontSize(14.px)
          .lineHeight(1.4)
          .focus(Style.borderColor(brand).shadow(0.px, 0.px, 0.px, 3.px, Color.rgba(79, 70, 229, 0.16))),
      )
      .rule(
        "mp-field-row",
        Style.row.flexWrap(FlexWrap.wrap).gap(12.px).width(100.pct),
      )
      .rule(
        "mp-field",
        Style.column.gap(5.px).flexGrow(1.0).flexBasis(180.px).minWidth(0.px),
      )
      // Drawer & Modal Overlays
      .rule(
        "mp-backdrop",
        Style.position(Position.fixed)
          .top(0.px)
          .left(0.px)
          .right(0.px)
          .bottom(0.px)
          .zIndex(80)
          .bg(backdrop)
          .row
          .justify(Justification.end),
      )
      .rule(
        "mp-drawer",
        Style.column
          .width(440.px)
          .maxWidth(100.pct)
          .height(100.pct)
          .bg(surface)
          .shadow((-8).px, 0.px, 32.px, 0.px, shadowStrong)
          .overflowY(Overflow.auto),
      )
      .rule(
        "mp-modal-backdrop",
        Style.position(Position.fixed)
          .top(0.px)
          .left(0.px)
          .right(0.px)
          .bottom(0.px)
          .zIndex(90)
          .bg(backdrop)
          .row
          .align(Alignment.center)
          .justify(Justification.center)
          .padding(16.px),
      )
      .rule(
        "mp-modal",
        Style.column
          .width(520.px)
          .maxWidth(100.pct)
          .maxHeight(92.pct)
          .bg(surface)
          .rounded(18.px)
          .padding(24.px)
          .gap(16.px)
          .overflowY(Overflow.auto)
          .shadow(0.px, 20.px, 48.px, 0.px, shadowStrong),
      )
      // Toast
      .rule(
        "mp-toast",
        Style.position(Position.fixed)
          .bottom(20.px)
          .right(20.px)
          .zIndex(100)
          .padding(12.px, 18.px)
          .rounded(12.px)
          .bg(ink)
          .color(Color.white)
          .fontSize(14.px)
          .fontWeight(FontWeight.w600)
          .shadow(0.px, 8.px, 24.px, 0.px, shadowStrong),
      )
      // Tablet breakpoint (<= 960px)
      .media(Stylesheet.MediaQuery.maxWidth(960.px))(
        Stylesheet
          .rule("mp-split-layout", Style.column.gap(20.px))
          .rule("mp-split-aside", Style.width(100.pct).position(Position.relative).top(0.px))
          .rule("mp-desktop-nav", Style.displayNone)
          .rule("mp-mobile-only", Style.row.align(Alignment.center).gap(6.px))
          .rule("mp-hero-title", Style.fontSize(28.px)),
      )
      // Mobile breakpoint (<= 640px)
      .media(Stylesheet.MediaQuery.maxWidth(640.px))(
        Stylesheet
          .rule("mp-container", Style.padding(0.px, 14.px))
          .rule("mp-header-inner", Style.padding(10.px, 14.px).gap(8.px))
          .rule("mp-hero", Style.padding(24.px, 18.px).rounded(14.px))
          .rule("mp-hero-title", Style.fontSize(24.px))
          .rule("mp-card", Style.padding(14.px))
          .rule("mp-card-product", Style.flexBasis(100.pct).maxWidth(100.pct))
          .rule("mp-option-card", Style.flexBasis(140.px))
          .rule("mp-option-card-active", Style.flexBasis(140.px))
          .rule("mp-calc-root", Style.padding(12.px).rounded(12.px))
          .rule("mp-modal", Style.padding(18.px).rounded(14.px)),
      )

  val storefrontStylesheet: Stylesheet =
    Stylesheet.rule(
      Selector.tag("body"),
      Style.margin(0.px).padding(0.px).bg(canvas).color(ink).fontFamily(FontFamily.SystemUi),
    ) ++ coreStylesheet

  val calculatorStylesheet: Stylesheet = coreStylesheet
