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
    val silverBg: Color       = hexColor("#E0F2FE")
    val silverBorder: Color   = hexColor("#7DD3FC")
    val silverInk: Color      = hexColor("#0369A1")
    val goldBg: Color         = hexColor("#FEF3C7")
    val goldBorder: Color     = hexColor("#FCD34D")
    val goldInk: Color        = hexColor("#B45309")
    val platinumBg: Color     = hexColor("#F3E8FF")
    val platinumBorder: Color = hexColor("#C4B5FD")
    val platinumInk: Color    = hexColor("#6D28D9")
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
      // Refined Balanced Tab Bar (full-width bottom divider line clearly scoping content below)
      .rule(
        "mp-tabs",
        Style.row
          .flexWrap(FlexWrap.wrap)
          .align(Alignment.end)
          .gap(4.px)
          .width(100.pct)
          .borderBottom(1.px, borderStrong),
      )
      .rule(
        "mp-tabs-sm",
        Style.gap(2.px)
          .borderBottom(1.px, border),
      )
      .rule(
        "mp-tab",
        Style.row
          .align(Alignment.center)
          .gap(7.px)
          .padding(9.px, 15.px)
          .rounded(8.px, 8.px, 0.px, 0.px)
          .bg(Color.transparent)
          .color(inkSecondary)
          .fontSize(13.px)
          .fontWeight(FontWeight.w600)
          .border(0.px, BorderStyle.none, Color.transparent)
          .borderBottom(2.px, Color.transparent)
          .margin(0.px, 0.px, (-1).px, 0.px)
          .cursor(Cursor.pointer)
          .transition(TransitionProperty.all, 140, Easing.easeOut)
          .hover(Style.bg(surfaceHover).color(ink)),
      )
      .rule(
        "mp-tab-sm",
        Style.padding(7.px, 12.px)
          .fontSize(12.px)
          .gap(5.px),
      )
      .rule(
        "mp-tab-active",
        Style.bg(brandSoft)
          .color(brandDark)
          .fontWeight(FontWeight.w700)
          .borderBottom(2.px, brand)
          .hover(Style.bg(brandSoft).color(brandDark)),
      )
      // Corner Action Bar & ActionMenu Dropdown
      .rule(
        "mp-action-bar",
        Style.row
          .flexWrap(FlexWrap.wrap)
          .align(Alignment.center)
          .justify(Justification.end)
          .gap(8.px)
          .flexShrink(0.0),
      )
      .rule(
        "mp-action-menu-wrap",
        Style.position(Position.relative)
          .flexShrink(0.0),
      )
      .rule(
        "mp-action-menu-backdrop",
        Style.position(Position.fixed)
          .top(0.px)
          .left(0.px)
          .right(0.px)
          .bottom(0.px)
          .zIndex(55)
          .bg(Color.transparent),
      )
      .rule(
        "mp-action-menu-panel",
        Style.position(Position.absolute)
          .top(100.pct)
          .right(0.px)
          .zIndex(60)
          .minWidth(190.px)
          .margin(5.px, 0.px, 0.px, 0.px)
          .bg(surface)
          .border(1.px, borderStrong)
          .rounded(10.px)
          .padding(5.px)
          .shadow(0.px, 10.px, 28.px, 0.px, shadowMedium)
          .column
          .gap(2.px),
      )
      .rule(
        "mp-action-menu-item",
        Style.row
          .align(Alignment.center)
          .gap(8.px)
          .width(100.pct)
          .padding(8.px, 10.px)
          .rounded(7.px)
          .bg(Color.transparent)
          .border(0.px, BorderStyle.none, Color.transparent)
          .color(ink)
          .fontSize(13.px)
          .fontWeight(FontWeight.w600)
          .cursor(Cursor.pointer)
          .textAlign(TextAlign.left)
          .whiteSpace(WhiteSpace.noWrap)
          .transition(TransitionProperty.all, 110, Easing.easeOut)
          .hover(Style.bg(surfaceAlt)),
      )
      .rule(
        "mp-action-menu-item-primary",
        Style.color(brandDark).hover(Style.bg(brandSoft)),
      )
      .rule(
        "mp-action-menu-item-amber",
        Style.color(amberInk).hover(Style.bg(amberSoft)),
      )
      .rule(
        "mp-action-menu-item-danger",
        Style.color(roseInk).hover(Style.bg(roseSoft)),
      )
      .rule(
        "mp-action-menu-divider",
        Style.width(100.pct)
          .height(1.px)
          .bg(border)
          .margin(3.px, 0.px),
      )
      // Detail Header Helpers (Headline + Actions on 1 line; Tags cleanly below)
      .rule(
        "mp-detail-head",
        Style.column
          .gap(8.px)
          .width(100.pct)
          .padding(0.px, 0.px, 12.px, 0.px)
          .borderBottom(1.px, border),
      )
      .rule(
        "mp-detail-head-row",
        Style.row
          .align(Alignment.center)
          .justify(Justification.spaceBetween)
          .gap(12.px)
          .width(100.pct),
      )
      .rule(
        "mp-detail-tags-row",
        Style.row
          .flexWrap(FlexWrap.wrap)
          .align(Alignment.center)
          .gap(6.px)
          .width(100.pct),
      )
      // Universal DataTable — Vaadin-Style Master-Detail Layout with External Header & Resizable Splitter
      .rule(
        "mp-dt-root",
        Style.column
          .width(100.pct)
          .gap(12.px),
      )
      .rule(
        "mp-dt-header-top",
        Style.row
          .align(Alignment.center)
          .justify(Justification.spaceBetween)
          .gap(14.px)
          .width(100.pct),
      )
      .rule(
        "mp-dt-title-block",
        Style.column
          .gap(2.px)
          .flexGrow(1.0)
          .flexShrink(1.0)
          .flexBasis(0.px)
          .minWidth(0.px),
      )
      .rule(
        "mp-dt-controls",
        Style.row
          .align(Alignment.center)
          .justify(Justification.end)
          .gap(8.px)
          .flexShrink(0.0),
      )
      .rule(
        "mp-dt-split",
        Style.row
          .width(100.pct)
          .bg(surface)
          .border(1.px, border)
          .rounded(14.px)
          .shadow(0.px, 1.px, 4.px, 0.px, shadowSubtle)
          .align(Alignment.stretch)
          .minHeight(440.px),
      )
      .rule(
        "mp-dt-list-pane",
        Style.column
          .flexGrow(1.0)
          .flexShrink(1.0)
          .flexBasis(0.px)
          .minWidth(0.px)
          .width(100.pct),
      )
      .rule(
        "mp-dt-list-pane-split",
        Style.flexGrow(0.0)
          .flexShrink(0.0),
      )
      .rule(
        "mp-dt-splitter",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .width(10.px)
          .flexShrink(0.0)
          .bg(surfaceAlt)
          .borderLeft(1.px, border)
          .borderRight(1.px, border)
          .cursor(Cursor.grab)
          .transition(TransitionProperty.all, 120, Easing.easeOut)
          .hover(Style.bg(brandSoft).borderColor(brandBorder)),
      )
      .rule(
        "mp-dt-splitter-grip",
        Style.width(3.px)
          .height(34.px)
          .rounded(999.px)
          .bg(borderStrong),
      )
      .rule(
        "mp-dt-detail-pane",
        Style.column
          .flexGrow(1.0)
          .flexShrink(1.0)
          .flexBasis(0.px)
          .minWidth(0.px)
          .bg(surface)
          .maxHeight(78.vh),
      )
      .rule(
        "mp-dt-detail-header",
        Style.row
          .justify(Justification.spaceBetween)
          .align(Alignment.center)
          .gap(10.px)
          .padding(9.px, 16.px)
          .bg(surfaceAlt)
          .borderBottom(1.px, border)
          .flexShrink(0.0),
      )
      .rule(
        "mp-dt-detail-body",
        Style.column
          .flexGrow(1.0)
          .minHeight(0.px)
          .overflowY(Overflow.auto)
          .padding(18.px, 20.px)
          .gap(14.px),
      )
      .rule(
        "mp-dt-mobile-back",
        Style.displayNone,
      )
      .rule(
        "mp-dt-header-filters",
        Style.row
          .flexWrap(FlexWrap.wrap)
          .align(Alignment.center)
          .gap(10.px)
          .padding(10.px, 14.px)
          .bg(surfaceAlt)
          .borderBottom(1.px, border)
          .width(100.pct),
      )
      .rule(
        "mp-dt-filter-group",
        Style.row
          .flexWrap(FlexWrap.wrap)
          .align(Alignment.center)
          .gap(6.px),
      )
      .rule(
        "mp-dt-filter-label",
        Style.fontSize(11.px)
          .fontWeight(FontWeight.w800)
          .color(inkMuted)
          .textTransform(TextTransform.uppercase)
          .letterSpacing(0.4.px)
          .margin(0.px, 2.px, 0.px, 0.px),
      )
      .rule(
        "mp-dt-check",
        Style.row
          .align(Alignment.center)
          .gap(6.px)
          .padding(4.px, 9.px)
          .rounded(7.px)
          .bg(surface)
          .border(1.px, borderStrong)
          .color(inkSecondary)
          .fontSize(12.px)
          .fontWeight(FontWeight.w600)
          .cursor(Cursor.pointer)
          .transition(TransitionProperty.all, 120, Easing.easeOut)
          .hover(Style.borderColor(brandBorder).color(ink)),
      )
      .rule(
        "mp-dt-check-active",
        Style.bg(brandSoft)
          .borderColor(brand)
          .color(brandDark)
          .fontWeight(FontWeight.w700)
          .hover(Style.bg(brandSoft).borderColor(brand)),
      )
      .rule(
        "mp-dt-check-box",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .width(14.px)
          .height(14.px)
          .rounded(4.px)
          .border(1.px, borderStrong)
          .bg(surface)
          .fontSize(10.px)
          .fontWeight(FontWeight.w800)
          .lineHeight(1.0)
          .flexShrink(0.0),
      )
      .rule(
        "mp-dt-check-box-checked",
        Style.bg(brand).borderColor(brand).color(Color.white),
      )
      .rule(
        "mp-dt-ms-wrap",
        Style.position(Position.relative)
          .minWidth(210.px)
          .maxWidth(340.px)
          .flexGrow(1.0),
      )
      .rule(
        "mp-dt-ms-box",
        Style.row
          .flexWrap(FlexWrap.wrap)
          .align(Alignment.center)
          .gap(4.px)
          .padding(4.px, 8.px)
          .rounded(8.px)
          .bg(surface)
          .border(1.px, borderStrong)
          .minHeight(34.px),
      )
      .rule(
        "mp-dt-ms-tag",
        Style.row
          .align(Alignment.center)
          .gap(4.px)
          .padding(2.px, 7.px)
          .rounded(6.px)
          .bg(brandSoft)
          .color(brandDark)
          .border(1.px, brandBorder)
          .fontSize(11.px)
          .fontWeight(FontWeight.w700),
      )
      .rule(
        "mp-dt-ms-input",
        Style.border(0.px, BorderStyle.none, Color.transparent)
          .bg(Color.transparent)
          .color(ink)
          .fontSize(12.px)
          .flexGrow(1.0)
          .minWidth(85.px)
          .padding(3.px, 4.px),
      )
      .rule(
        "mp-dt-ms-menu",
        Style.position(Position.absolute)
          .top(100.pct)
          .left(0.px)
          .right(0.px)
          .zIndex(45)
          .margin(4.px, 0.px, 0.px, 0.px)
          .bg(surface)
          .border(1.px, borderStrong)
          .rounded(10.px)
          .padding(6.px)
          .shadow(0.px, 10.px, 28.px, 0.px, shadowMedium)
          .column
          .gap(2.px)
          .maxHeight(240.px)
          .overflowY(Overflow.auto),
      )
      .rule(
        "mp-dt-ms-option",
        Style.row
          .align(Alignment.center)
          .gap(8.px)
          .width(100.pct)
          .padding(6.px, 8.px)
          .rounded(7.px)
          .bg(Color.transparent)
          .border(0.px, BorderStyle.none, Color.transparent)
          .color(ink)
          .fontSize(12.px)
          .fontWeight(FontWeight.w600)
          .cursor(Cursor.pointer)
          .textAlign(TextAlign.left)
          .hover(Style.bg(surfaceAlt)),
      )
      .rule(
        "mp-dt-table-scroll",
        Style.width(100.pct)
          .overflowX(Overflow.auto),
      )
      .rule(
        "mp-dt-table",
        Style.width(100.pct)
          .borderCollapse(BorderCollapse.collapse)
          .fontSize(13.px),
      )
      .rule(
        "mp-dt-th",
        Style.padding(10.px, 12.px)
          .bg(surfaceAlt)
          .color(inkSecondary)
          .fontSize(11.px)
          .fontWeight(FontWeight.w700)
          .textTransform(TextTransform.uppercase)
          .letterSpacing(0.3.px)
          .borderBottom(1.px, border),
      )
      .rule(
        "mp-dt-td",
        Style.padding(11.px, 12.px)
          .borderBottom(1.px, border)
          .color(ink),
      )
      .rule(
        "mp-dt-th-btn",
        Style.row
          .align(Alignment.center)
          .gap(4.px)
          .width(100.pct)
          .bg(Color.transparent)
          .border(0.px, BorderStyle.none, Color.transparent)
          .padding(0.px)
          .color(inkSecondary)
          .fontSize(11.px)
          .fontWeight(FontWeight.w700)
          .textTransform(TextTransform.uppercase)
          .letterSpacing(0.3.px)
          .cursor(Cursor.pointer)
          .hover(Style.color(brand)),
      )
      .rule(
        "mp-dt-row",
        Style.cursor(Cursor.pointer)
          .transition(TransitionProperty.all, 120, Easing.easeOut)
          .hover(Style.bg(surfaceAlt)),
      )
      .rule(
        "mp-dt-row-selected",
        Style.bg(brandSoft).hover(Style.bg(brandSoft)),
      )
      .rule(
        "mp-dt-pager",
        Style.row
          .flexWrap(FlexWrap.wrap)
          .justify(Justification.spaceBetween)
          .align(Alignment.center)
          .gap(10.px)
          .padding(10.px, 16.px)
          .borderTop(1.px, border),
      )
      // Order Progress Stepper
      .rule(
        "mp-stepper",
        Style.row
          .align(Alignment.start)
          .justify(Justification.spaceBetween)
          .width(100.pct)
          .gap(4.px)
          .padding(10.px, 4.px, 6.px, 4.px),
      )
      .rule(
        "mp-stepper-step",
        Style.column
          .align(Alignment.center)
          .gap(6.px)
          .flexGrow(1.0)
          .flexBasis(0.px)
          .textAlign(TextAlign.center),
      )
      // Artwork Drag & Drop Upload Zones, Thumbnails & Lightbox Preview
      .rule(
        "mp-dropzone",
        Style.column
          .align(Alignment.center)
          .justify(Justification.center)
          .width(100.pct)
          .padding(18.px, 16.px)
          .rounded(12.px)
          .bg(surfaceAlt)
          .border(2.px, BorderStyle.dashed, borderStrong)
          .cursor(Cursor.pointer)
          .transition(TransitionProperty.all, 140, Easing.easeOut)
          .hover(Style.borderColor(brand).bg(brandSoft)),
      )
      .rule(
        "mp-dropzone-compact",
        Style.padding(11.px, 12.px)
          .rounded(10.px),
      )
      .rule(
        "mp-dropzone-active",
        Style.borderColor(brand)
          .bg(brandSoft)
          .shadow(0.px, 0.px, 0.px, 3.px, brandBorder),
      )
      .rule(
        "mp-art-thumb",
        Style.transition(TransitionProperty.all, 140, Easing.easeOut)
          .hover(Style.borderColor(brand).shadow(0.px, 2.px, 8.px, 0.px, shadowSubtle)),
      )
      .rule(
        "mp-art-preview-modal",
        Style.maxWidth(780.px)
          .width(100.pct),
      )
      .rule(
        "mp-art-preview-frame",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .width(100.pct)
          .minHeight(240.px)
          .padding(16.px)
          .rounded(12.px)
          .bg(canvas)
          .border(1.px, border),
      )
      // Tablet breakpoint (<= 960px)
      .media(Stylesheet.MediaQuery.maxWidth(960.px))(
        Stylesheet
          .rule("mp-split-layout", Style.column.gap(20.px))
          .rule("mp-split-aside", Style.width(100.pct).position(Position.relative).top(0.px))
          .rule("mp-desktop-nav", Style.displayNone)
          .rule("mp-mobile-only", Style.row.align(Alignment.center).gap(6.px))
          .rule("mp-hero-title", Style.fontSize(28.px))
          .rule("mp-dt-split", Style.column.minHeight(0.px))
          .rule("mp-dt-list-pane-split", Style.displayNone)
          .rule("mp-dt-splitter", Style.displayNone)
          .rule("mp-dt-detail-pane", Style.width(100.pct).flexBasis(Length.Auto).maxHeight(Length.Auto))
          .rule("mp-dt-mobile-back", Style.row.align(Alignment.center).gap(6.px)),
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
          .rule("mp-modal", Style.padding(18.px).rounded(14.px))
          .rule("mp-dt-header-top", Style.column.align(Alignment.stretch).gap(10.px))
          .rule("mp-dt-controls", Style.flexWrap(FlexWrap.wrap).justify(Justification.start).width(100.pct)),
      )

  val storefrontStylesheet: Stylesheet =
    Stylesheet.rule(
      Selector.tag("body"),
      Style.margin(0.px).padding(0.px).bg(canvas).color(ink).fontFamily(FontFamily.SystemUi),
    ) ++ coreStylesheet

  val calculatorStylesheet: Stylesheet = coreStylesheet
