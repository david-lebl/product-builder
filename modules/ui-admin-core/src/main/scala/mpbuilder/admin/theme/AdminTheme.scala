package mpbuilder.admin.theme

import kyo.*
import kyo.Style.*
import mpbuilder.ui.theme.AppTheme
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Responsive Backoffice Stylesheet extending `AppTheme.storefrontStylesheet`.
  *
  * Layout:
  *   - Desktop (> 960px): Fixed 252px dark slate sidebar + scrollable fluid content workspace
  *   - Tablet/Mobile (<= 960px): Collapsible slide-down nav bar + single-column cards & scrollable tables
  */
object AdminTheme:

  object AdminPalette:
    private def hexColor(raw: String, fallback: Color = Color.slate): Color =
      Color.hex(raw).getOrElse(fallback)

    val sidebarBg: Color          = hexColor("#0F172A")
    val sidebarHover: Color       = hexColor("#1E293B")
    val sidebarActive: Color      = hexColor("#312E81")
    val sidebarBorder: Color      = hexColor("#1E293B")
    val sidebarText: Color        = hexColor("#CBD5E1")
    val sidebarTextActive: Color  = Color.white
    val sidebarMuted: Color       = hexColor("#94A3B8")

  import AdminPalette.*

  val adminOnlyStylesheet: Stylesheet =
    Stylesheet
      .rule(
        "mp-admin-shell",
        Style.row
          .minHeight(100.vh)
          .width(100.pct)
          .bg(canvas)
          .color(ink)
          .fontFamily(FontFamily.SystemUi)
          .fontSize(14.px)
          .lineHeight(1.5),
      )
      .rule(
        "mp-admin-sidebar",
        Style.column
          .width(256.px)
          .flexShrink(0.0)
          .bg(sidebarBg)
          .color(sidebarText)
          .padding(20.px, 14.px)
          .gap(16.px)
          .borderRight(1.px, sidebarBorder),
      )
      .rule(
        "mp-admin-nav-btn",
        Style.row
          .align(Alignment.center)
          .justify(Justification.spaceBetween)
          .width(100.pct)
          .padding(10.px, 12.px)
          .rounded(10.px)
          .bg(Color.transparent)
          .color(sidebarText)
          .fontSize(14.px)
          .fontWeight(FontWeight.w600)
          .cursor(Cursor.pointer)
          .border(1.px, Color.transparent)
          .textAlign(TextAlign.left)
          .transition(TransitionProperty.all, 140, Easing.easeOut)
          .hover(Style.bg(sidebarHover).color(Color.white)),
      )
      .rule(
        "mp-admin-nav-btn-active",
        Style.row
          .align(Alignment.center)
          .justify(Justification.spaceBetween)
          .width(100.pct)
          .padding(10.px, 12.px)
          .rounded(10.px)
          .bg(sidebarActive)
          .color(sidebarTextActive)
          .fontSize(14.px)
          .fontWeight(FontWeight.w700)
          .cursor(Cursor.pointer)
          .border(1.px, brand)
          .textAlign(TextAlign.left),
      )
      .rule(
        "mp-admin-workspace",
        Style.column
          .flexGrow(1.0)
          .minWidth(0.px)
          .minHeight(100.vh)
          .bg(canvas),
      )
      .rule(
        "mp-admin-topbar",
        Style.row
          .position(Position.sticky)
          .top(0.px)
          .zIndex(35)
          .width(100.pct)
          .bg(glassHeader)
          .borderBottom(1.px, AppTheme.Palette.border)
          .padding(12.px, 24.px)
          .align(Alignment.center)
          .justify(Justification.spaceBetween)
          .gap(12.px),
      )
      .rule(
        "mp-admin-content",
        Style.column
          .width(100.pct)
          .maxWidth(1560.px)
          .padding(24.px)
          .gap(20.px)
          .flexGrow(1.0),
      )
      .rule(
        "mp-admin-mobile-nav",
        Style.column
          .width(100.pct)
          .bg(sidebarBg)
          .color(sidebarText)
          .padding(12.px, 16.px)
          .gap(8.px),
      )
      // KPI Grid
      .rule(
        "mp-kpi-grid",
        Style.row
          .flexWrap(FlexWrap.wrap)
          .gap(14.px)
          .width(100.pct),
      )
      .rule(
        "mp-kpi-card",
        Style.column
          .bg(surface)
          .border(1.px, AppTheme.Palette.border)
          .rounded(14.px)
          .padding(16.px, 18.px)
          .gap(6.px)
          .flexGrow(1.0)
          .flexBasis(175.px)
          .shadow(0.px, 1.px, 3.px, 0.px, shadowSubtle),
      )
      // Responsive Table Wrapper
      .rule(
        "mp-table-wrap",
        Style.width(100.pct)
          .overflowX(Overflow.auto)
          .bg(surface)
          .border(1.px, AppTheme.Palette.border)
          .rounded(14.px),
      )
      .rule(
        "mp-table",
        Style.width(100.pct).fontSize(13.px),
      )
      // Two-pane master-detail (Customers / Order Detail)
      .rule(
        "mp-admin-split",
        Style.row
          .width(100.pct)
          .gap(20.px)
          .align(Alignment.start),
      )
      .rule(
        "mp-admin-split-left",
        Style.column
          .flexGrow(1.0)
          .flexBasis(360.px)
          .minWidth(0.px)
          .gap(12.px),
      )
      .rule(
        "mp-admin-split-right",
        Style.column
          .flexGrow(1.3)
          .flexBasis(420.px)
          .minWidth(0.px)
          .gap(16.px),
      )
      // Login shell
      .rule(
        "mp-admin-login-shell",
        Style.column
          .minHeight(100.vh)
          .width(100.pct)
          .bg(canvas)
          .align(Alignment.center)
          .justify(Justification.center)
          .padding(24.px),
      )
      .rule(
        "mp-admin-login-card",
        Style.column
          .width(500.px)
          .maxWidth(100.pct)
          .bg(surface)
          .border(1.px, AppTheme.Palette.border)
          .rounded(18.px)
          .padding(28.px)
          .gap(18.px)
          .shadow(0.px, 12.px, 36.px, 0.px, shadowMedium),
      )
      // Timeline
      .rule(
        "mp-timeline",
        Style.column
          .gap(12.px)
          .padding(4.px, 0.px, 4.px, 14.px)
          .borderLeft(2.px, brandBorder),
      )
      // Tablet breakpoint (<= 960px)
      .media(Stylesheet.MediaQuery.maxWidth(960.px))(
        Stylesheet
          .rule("mp-admin-sidebar", Style.displayNone)
          .rule("mp-admin-split", Style.column.gap(16.px))
          .rule("mp-admin-split-left", Style.width(100.pct).flexBasis(Length.Auto))
          .rule("mp-admin-split-right", Style.width(100.pct).flexBasis(Length.Auto))
          .rule("mp-admin-topbar", Style.padding(10.px, 16.px))
          .rule("mp-admin-content", Style.padding(16.px)),
      )
      // Mobile breakpoint (<= 640px)
      .media(Stylesheet.MediaQuery.maxWidth(640.px))(
        Stylesheet
          .rule("mp-kpi-card", Style.flexBasis(140.px).padding(12.px, 14.px))
          .rule("mp-admin-login-card", Style.padding(20.px).rounded(14.px))
          .rule("mp-admin-content", Style.padding(12.px)),
      )

  val adminStylesheet: Stylesheet =
    AppTheme.storefrontStylesheet ++ adminOnlyStylesheet
