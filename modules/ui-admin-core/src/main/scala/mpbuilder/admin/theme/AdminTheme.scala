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

    val sidebarBg: Color          = hexColor("#0E1424")
    val sidebarHover: Color       = Color.rgba(148, 163, 184, 0.10)
    val sidebarActive: Color      = Color.rgba(129, 140, 248, 0.18)
    val sidebarBorder: Color      = Color.rgba(148, 163, 184, 0.12)
    val sidebarText: Color        = hexColor("#B6C0CF")
    val sidebarTextActive: Color  = Color.white
    val sidebarMuted: Color       = hexColor("#7D8AA0")
    val sidebarAccent: Color      = hexColor("#A5B4FC")

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
          .fontFamily(AppTheme.fontStack)
          .fontSize(14.px)
          .lineHeight(1.5),
      )
      .rule(
        "mp-admin-sidebar",
        Style.column
          .position(Position.sticky)
          .top(0.px)
          .height(100.vh)
          .overflowY(Overflow.auto)
          .width(256.px)
          .flexShrink(0.0)
          .bg(sidebarBg)
          .color(sidebarText)
          .padding(18.px, 12.px)
          .gap(18.px)
          .borderRight(1.px, sidebarBorder),
      )
      .rule(
        "mp-admin-nav-btn",
        Style.row
          .align(Alignment.center)
          .gap(11.px)
          .width(100.pct)
          .padding(9.px, 12.px)
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
          .gap(11.px)
          .width(100.pct)
          .padding(9.px, 12.px)
          .rounded(10.px)
          .bg(sidebarActive)
          .color(sidebarTextActive)
          .fontSize(14.px)
          .fontWeight(FontWeight.w700)
          .cursor(Cursor.pointer)
          .border(1.px, Color.rgba(129, 140, 248, 0.35))
          .textAlign(TextAlign.left)
          .shadow(0.px, 0.px, 0.px, 0.px, Color.transparent),
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
        "mp-admin-topbar-wrap",
        Style.column
          .position(Position.sticky)
          .top(0.px)
          .zIndex(35)
          .width(100.pct),
      )
      .rule(
        "mp-admin-topbar",
        Style.row
          .width(100.pct)
          .minHeight(60.px)
          .bg(glassHeader)
          .borderBottom(1.px, AppTheme.Palette.border)
          .padding(10.px, 24.px)
          .align(Alignment.center)
          .justify(Justification.spaceBetween)
          .gap(12.px),
      )
      .rule(
        "mp-admin-content",
        Style.column
          .width(100.pct)
          .maxWidth(1560.px)
          .padding(28.px)
          .gap(22.px)
          .flexGrow(1.0)
          .animation("mp-fade-in", 180, Easing.easeOut),
      )
      .rule(
        "mp-admin-mobile-nav",
        Style.column
          .width(100.pct)
          .bg(surface)
          .borderBottom(1.px, AppTheme.Palette.border)
          .padding(10.px, 12.px)
          .gap(2.px)
          .maxHeight(80.vh)
          .overflowY(Overflow.auto)
          .animation("mp-drop-in", 160, Easing.easeOut),
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
          .align(Alignment.stretch)
          .bg(surface)
          .color(ink)
          .border(1.px, AppTheme.Palette.border)
          .rounded(16.px)
          .padding(16.px, 18.px)
          .gap(10.px)
          .flexGrow(1.0)
          .flexBasis(160.px)
          .textAlign(TextAlign.left)
          .cursor(Cursor.pointer)
          .shadow(0.px, 1.px, 2.px, 0.px, shadowSubtle)
          .transition(TransitionProperty.all, 160, Easing.easeOut)
          .hover(Style.borderColor(brandBorder).translate(0.px, (-2).px).shadow(0.px, 12.px, 24.px, (-10).px, shadowMedium)),
      )
      // Responsive Table Wrapper
      .rule(
        "mp-table-wrap",
        Style.width(100.pct)
          .overflowX(Overflow.auto)
          .bg(surface)
          .border(1.px, AppTheme.Palette.border)
          .rounded(16.px),
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
          .color(ink)
          .fontFamily(AppTheme.fontStack)
          .align(Alignment.center)
          .justify(Justification.center)
          .padding(32.px, 24.px),
      )
      .rule(
        "mp-admin-login-card",
        Style.column
          .width(480.px)
          .maxWidth(100.pct)
          .bg(surface)
          .border(1.px, AppTheme.Palette.border)
          .rounded(22.px)
          .padding(32.px)
          .gap(18.px)
          .animation("mp-rise-in", 260, Easing.easeOut)
          .shadow(0.px, 24.px, 60.px, (-16).px, shadowStrong),
      )
      // Timeline
      .rule(
        "mp-timeline",
        Style.column
          .gap(12.px)
          .padding(4.px, 0.px, 4.px, 14.px)
          .borderLeft(2.px, brandBorder),
      )
      // KPI anatomy
      .rule(
        "mp-kpi-icon",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .width(34.px)
          .height(34.px)
          .rounded(10.px)
          .flexShrink(0.0),
      )
      .rule(
        "mp-kpi-value",
        Style.fontSize(28.px)
          .fontWeight(FontWeight.w800)
          .letterSpacing((-0.6).px)
          .lineHeight(1.05)
          .color(ink),
      )
      // Sidebar sections & profile
      .rule(
        "mp-admin-nav-label",
        Style.padding(0.px, 12.px)
          .margin(6.px, 0.px, 2.px, 0.px)
          .fontSize(11.px)
          .fontWeight(FontWeight.w700)
          .letterSpacing(0.8.px)
          .textTransform(TextTransform.uppercase)
          .color(sidebarMuted),
      )
      .rule(
        "mp-admin-nav-count",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .minWidth(20.px)
          .height(20.px)
          .padding(0.px, 6.px)
          .margin(0.px, 0.px, 0.px, Length.Auto)
          .rounded(999.px)
          .bg(amber)
          .color(Color.white)
          .fontSize(11.px)
          .fontWeight(FontWeight.w800)
          .lineHeight(1.0),
      )
      .rule(
        "mp-admin-profile",
        Style.column
          .gap(10.px)
          .padding(12.px)
          .rounded(14.px)
          .bg(Color.rgba(148, 163, 184, 0.07))
          .border(1.px, sidebarBorder),
      )
      .rule(
        "mp-admin-side-btn",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .gap(8.px)
          .flexGrow(1.0)
          .padding(8.px, 10.px)
          .rounded(9.px)
          .bg(sidebarHover)
          .color(sidebarText)
          .border(1.px, sidebarBorder)
          .fontSize(13.px)
          .fontWeight(FontWeight.w600)
          .cursor(Cursor.pointer)
          .transition(TransitionProperty.all, 140, Easing.easeOut)
          .hover(Style.bg(Color.rgba(148, 163, 184, 0.18)).color(Color.white)),
      )
      // Dashboard order rows
      .rule(
        "mp-list-row",
        Style.row
          .flexWrap(FlexWrap.wrap)
          .justify(Justification.spaceBetween)
          .align(Alignment.center)
          .gap(12.px)
          .padding(12.px, 14.px)
          .rounded(12.px)
          .bg(surface)
          .border(1.px, AppTheme.Palette.border)
          .transition(TransitionProperty.all, 140, Easing.easeOut)
          .hover(Style.borderColor(brandBorder).bg(surfaceAlt)),
      )
      .rule(Selector.cls("mp-admin-nav-btn").pseudo("focus-visible"), AppTheme.focusVisibleRing)
      .rule(Selector.cls("mp-admin-nav-btn-active").pseudo("focus-visible"), AppTheme.focusVisibleRing)
      .rule(Selector.cls("mp-kpi-card").pseudo("focus-visible"), AppTheme.focusVisibleRing)
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
          .rule("mp-admin-content", Style.padding(14.px, 12.px))
          .rule("mp-kpi-value", Style.fontSize(22.px)),
      )

  val adminStylesheet: Stylesheet =
    AppTheme.storefrontStylesheet ++ adminOnlyStylesheet

  /** Full-height "one big table" layout, applied automatically whenever the routed view contains a `DataTable`
    * (`.mp-dt-root`) — Orders, Catalog, Customers, and the Pricing table tabs.
    *
    * The workspace is pinned to the viewport so the page itself never scrolls; every wrapper between
    * `.mp-admin-content` and the table becomes a flexible column, and only the table body
    * (`.mp-dt-table-scroll`, with sticky column headers) and the detail pane body scroll. The page header,
    * search / create buttons, filters and pager therefore stay visible, as does the top of the split
    * detail pane while browsing the last rows. The content width cap is raised from 1560px to 2400px
    * (centred) so only ultra-wide screens are limited.
    *
    * Raw CSS because typed `kyo.Style` cannot emit `max-height: none` (`Length.Auto` renders invalid
    * `max-height: auto`), an inset box-shadow (sticky header border under `border-collapse`), or the
    * `100vh` → `100dvh` fallback pair — see `docs/analysis/kyo-ecosystem-findings.md` #21.
    * Phones (<= 640px) keep the regular page-scroll layout.
    * Injected by the full-page backoffice hosts next to `AppTheme.documentCss`.
    */
  val documentCss: String =
    """@media (min-width: 641px){
      |.mp-admin-workspace:has(.mp-dt-root){height:100vh;height:100dvh;min-height:0}
      |.mp-admin-content:has(.mp-dt-root){flex:1 1 0;min-height:0;max-width:2400px;margin-left:auto;margin-right:auto;overflow-y:auto}
      |.mp-admin-content :has(.mp-dt-root){display:flex;flex-direction:column;flex:1 1 0;min-height:0;align-self:stretch}
      |.mp-admin-content .mp-dt-root{flex:1 1 0;min-height:0}
      |.mp-admin-content .mp-dt-header-top{flex-shrink:0}
      |.mp-admin-content .mp-dt-split{flex:1 1 0;min-height:320px}
      |.mp-admin-content .mp-dt-list-pane{min-height:0}
      |.mp-admin-content .mp-dt-header-filters,.mp-admin-content .mp-dt-pager{flex-shrink:0}
      |.mp-admin-content .mp-dt-table-scroll{flex:1 1 0;min-height:0;overflow:auto}
      |.mp-admin-content .mp-dt-th{position:sticky;top:0;z-index:2;box-shadow:inset 0 -1px 0 var(--mp-border)}
      |.mp-admin-content .mp-dt-detail-pane{flex:1 1 0;min-height:0;max-height:none}
      |}
      |""".stripMargin
