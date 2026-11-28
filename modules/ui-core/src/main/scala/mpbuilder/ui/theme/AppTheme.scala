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

  // ── Design Tokens (CSS custom properties, light + dark) ────────────────────

  /** One themable color token, emitted as `--mp-<name>` with a light and a dark value. */
  final case class Token(name: String, light: String, dark: String):
    val cssVar: String = s"mp-$name"
    val color: Color   = Color.variable(cssVar)

  /** Typed palette. Every member resolves to `var(--mp-…)`, so the whole UI re-themes (light / dark /
    * system) by swapping the variable block — no call site needs to know which theme is active.
    */
  object Palette:
    private[theme] def hexColor(raw: String, fallback: Color = Color.slate): Color =
      Color.hex(raw).getOrElse(fallback)

    private val registry = scala.collection.mutable.ListBuffer.empty[Token]
    private def token(name: String, light: String, dark: String): Color =
      val t = Token(name, light, dark)
      registry += t
      t.color

    // Surfaces
    val canvas: Color         = token("canvas", "#F6F7FB", "#0B0F19")
    val surface: Color        = token("surface", "#FFFFFF", "#111827")
    val surfaceAlt: Color     = token("surface-alt", "#F3F5F9", "#161F31")
    val surfaceHover: Color   = token("surface-hover", "#ECEFF5", "#1E293B")
    val border: Color         = token("border", "#E3E7EE", "#232E42")
    val borderStrong: Color   = token("border-strong", "#CBD2DD", "#334155")
    // Text
    val ink: Color            = token("ink", "#0F172A", "#E6EAF1")
    val inkSecondary: Color   = token("ink-secondary", "#475569", "#B3BECD")
    val inkMuted: Color       = token("ink-muted", "#64748B", "#8592A6")
    // Brand (indigo)
    val brand: Color          = token("brand", "#4F46E5", "#6366F1")
    val brandHover: Color     = token("brand-hover", "#4338CA", "#7C83F7")
    val brandSoft: Color      = token("brand-soft", "#EEF2FF", "rgba(99, 102, 241, 0.16)")
    val brandBorder: Color    = token("brand-border", "#C7D2FE", "rgba(129, 140, 248, 0.40)")
    val brandDark: Color      = token("brand-ink", "#3730A3", "#C7D2FE")
    // Semantic
    val emerald: Color        = token("emerald", "#059669", "#10B981")
    val emeraldSoft: Color    = token("emerald-soft", "#ECFDF5", "rgba(16, 185, 129, 0.14)")
    val emeraldBorder: Color  = token("emerald-border", "#A7F3D0", "rgba(16, 185, 129, 0.36)")
    val emeraldInk: Color     = token("emerald-ink", "#065F46", "#6EE7B7")
    val amber: Color          = token("amber", "#D97706", "#F59E0B")
    val amberSoft: Color      = token("amber-soft", "#FFFBEB", "rgba(245, 158, 11, 0.14)")
    val amberBorder: Color    = token("amber-border", "#FDE68A", "rgba(245, 158, 11, 0.36)")
    val amberInk: Color       = token("amber-ink", "#92400E", "#FCD34D")
    val rose: Color           = token("rose", "#DC2626", "#EF4444")
    val roseSoft: Color       = token("rose-soft", "#FEF2F2", "rgba(239, 68, 68, 0.14)")
    val roseBorder: Color     = token("rose-border", "#FECACA", "rgba(239, 68, 68, 0.38)")
    val roseInk: Color        = token("rose-ink", "#991B1B", "#FCA5A5")
    val violet: Color         = token("violet", "#7C3AED", "#A78BFA")
    val violetSoft: Color     = token("violet-soft", "#F5F3FF", "rgba(139, 92, 246, 0.16)")
    val sky: Color            = token("sky", "#0284C7", "#38BDF8")
    val skySoft: Color        = token("sky-soft", "#F0F9FF", "rgba(14, 165, 233, 0.14)")
    // Customer tiers
    val silverBg: Color       = token("silver-bg", "#E0F2FE", "rgba(14, 165, 233, 0.16)")
    val silverBorder: Color   = token("silver-border", "#7DD3FC", "rgba(56, 189, 248, 0.42)")
    val silverInk: Color      = token("silver-ink", "#0369A1", "#7DD3FC")
    val goldBg: Color         = token("gold-bg", "#FEF3C7", "rgba(234, 179, 8, 0.16)")
    val goldBorder: Color     = token("gold-border", "#FCD34D", "rgba(250, 204, 21, 0.42)")
    val goldInk: Color        = token("gold-ink", "#B45309", "#FDE047")
    val platinumBg: Color     = token("platinum-bg", "#F3E8FF", "rgba(168, 85, 247, 0.16)")
    val platinumBorder: Color = token("platinum-border", "#C4B5FD", "rgba(192, 132, 252, 0.42)")
    val platinumInk: Color    = token("platinum-ink", "#6D28D9", "#D8B4FE")
    // Overlays, glass & elevation
    val backdrop: Color       = token("backdrop", "rgba(15, 23, 42, 0.42)", "rgba(2, 6, 23, 0.66)")
    val glassHeader: Color    = token("glass", "rgba(255, 255, 255, 0.82)", "rgba(11, 15, 25, 0.78)")
    val shadowSubtle: Color   = token("shadow-sm", "rgba(15, 23, 42, 0.06)", "rgba(0, 0, 0, 0.30)")
    val shadowMedium: Color   = token("shadow-md", "rgba(15, 23, 42, 0.10)", "rgba(0, 0, 0, 0.42)")
    val shadowStrong: Color   = token("shadow-lg", "rgba(15, 23, 42, 0.18)", "rgba(0, 0, 0, 0.60)")
    val focusRing: Color      = token("focus-ring", "rgba(79, 70, 229, 0.28)", "rgba(129, 140, 248, 0.45)")
    // Inverse surfaces (toasts, tooltips) stay dark-on-light in both themes
    val inverseBg: Color      = token("inverse-bg", "#0F172A", "#1E293B")
    val inverseInk: Color     = token("inverse-ink", "#F8FAFC", "#F8FAFC")

    /** All themable tokens, in declaration order. */
    lazy val tokens: List[Token] = registry.toList

    // Static (non-themed) brand literals for gradients & illustrations that look right on both themes.
    val brandStatic: Color  = hexColor("#4F46E5", Color.indigo)
    val brandDeep: Color    = hexColor("#312E81", Color.indigo)
    val violetStatic: Color = hexColor("#7C3AED", Color.purple)
    val skyStatic: Color    = hexColor("#0284C7", Color.blue)
    val onBrand: Color      = Color.white

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

  /** Accessible keyboard focus ring (applied via `:focus-visible`, so mouse clicks stay clean). */
  val focusVisibleRing: Style =
    Style.shadow(0.px, 0.px, 0.px, 3.px, focusRing).borderColor(brand)

  /** Hides content visually while keeping it in the accessibility tree (screen-reader label). */
  val visuallyHidden: Style =
    Style.position(Position.absolute)
      .width(1.px)
      .height(1.px)
      .padding(0.px)
      .margin((-1).px)
      .overflow(Overflow.hidden)
      .whiteSpace(WhiteSpace.noWrap)
      .border(0.px, BorderStyle.none, Color.transparent)
      .opacity(0.0)

  val dividerStyle: Style =
    Style.height(1.px).width(100.pct).bg(border).border(0.px, BorderStyle.none, Color.transparent)

  // ── Core Responsive Stylesheet (Shared by Storefront SPA + Calculator) ─────

  // ── Typography ─────────────────────────────────────────────────────────────

  /** Inter (loaded by the host page) with a native system-UI fallback, so embeds without the web font still look crisp. */
  val fontStack: FontFamily =
    FontFamily.Custom("\"Inter\", \"InterVariable\", ui-sans-serif, system-ui, -apple-system, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif")

  val monoStack: FontFamily =
    FontFamily.Custom("ui-monospace, \"SF Mono\", SFMono-Regular, Menlo, Consolas, monospace")

  // ── Theme Variables (light default, `[data-theme=dark]`, and `[data-theme=system]` + OS dark) ──

  private def darkPairs: Seq[(String, String)] = Palette.tokens.map(t => t.cssVar -> t.dark)

  val themeStylesheet: Stylesheet =
    Stylesheet
      .vars(Palette.tokens.map(t => t.cssVar -> t.light)*)
      .scopedVars(Selector.data("theme", "dark"), darkPairs*)
      .media(Stylesheet.MediaQuery.prefersDark)(
        Stylesheet.scopedVars(Selector.data("theme", "system"), darkPairs*),
      )

  // ── Motion ─────────────────────────────────────────────────────────────────

  val motionStylesheet: Stylesheet =
    Stylesheet
      .keyframes("mp-fade-in", Stylesheet.Keyframe.from -> Style.opacity(0.0), Stylesheet.Keyframe.to -> Style.opacity(1.0))
      .keyframes(
        "mp-rise-in",
        Stylesheet.Keyframe.from -> Style.opacity(0.0).translate(0.px, 10.px),
        Stylesheet.Keyframe.to   -> Style.opacity(1.0).translate(0.px, 0.px),
      )
      .keyframes(
        "mp-slide-in-right",
        Stylesheet.Keyframe.from -> Style.opacity(0.6).translate(32.px, 0.px),
        Stylesheet.Keyframe.to   -> Style.opacity(1.0).translate(0.px, 0.px),
      )
      .keyframes(
        "mp-slide-up",
        Stylesheet.Keyframe.from -> Style.opacity(0.4).translate(0.px, 48.px),
        Stylesheet.Keyframe.to   -> Style.opacity(1.0).translate(0.px, 0.px),
      )
      .keyframes(
        "mp-drop-in",
        Stylesheet.Keyframe.from -> Style.opacity(0.0).translate(0.px, (-6).px),
        Stylesheet.Keyframe.to   -> Style.opacity(1.0).translate(0.px, 0.px),
      )

  val coreStylesheet: Stylesheet =
    themeStylesheet ++ motionStylesheet ++ Stylesheet
      // Root containers
      .rule(
        "mp-app",
        Style.column
          .minHeight(100.vh)
          .bg(canvas)
          .color(ink)
          .fontFamily(fontStack)
          .fontSize(15.px)
          .lineHeight(1.55),
      )
      .rule(
        "mp-calc-root",
        Style.column
          .bg(canvas)
          .color(ink)
          .fontFamily(fontStack)
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
      .rule(
        "mp-main",
        Style.padding(28.px, 24.px, 56.px, 24.px).flexGrow(1.0),
      )
      // Sticky Header
      .rule(
        "mp-header",
        Style.position(Position.sticky)
          .top(0.px)
          .zIndex(40)
          .width(100.pct)
          .bg(glassHeader)
          .borderBottom(1.px, border),
      )
      .rule(
        "mp-header-inner",
        Style.row
          .width(100.pct)
          .maxWidth(1240.px)
          .margin(0.px, Length.Auto)
          .padding(10.px, 24.px)
          .minHeight(64.px)
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
          .borderTop(1.px, border)
          .padding(12.px, 20.px)
          .gap(8.px)
          .maxHeight(80.vh)
          .overflowY(Overflow.auto),
      )
      // Hero Banner
      .rule(
        "mp-hero",
        Style.column
          .width(100.pct)
          .padding(44.px, 40.px)
          .rounded(24.px)
          .bgGradient(GradientDirection.toBottomRight, (brandDeep, 0.pct), (brandStatic, 55.pct), (violetStatic, 100.pct))
          .color(Color.white)
          .gap(16.px)
          .overflow(Overflow.hidden)
          .shadow(0.px, 18.px, 40.px, (-12).px, Color.rgba(79, 70, 229, 0.45)),
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
          .rounded(16.px)
          .padding(20.px)
          .gap(14.px)
          .shadow(0.px, 1.px, 2.px, 0.px, shadowSubtle),
      )
      .rule(
        "mp-card-product",
        Style.column
          .bg(surface)
          .border(1.px, border)
          .rounded(18.px)
          .padding(0.px)
          .gap(0.px)
          .flexGrow(1.0)
          .flexShrink(1.0)
          .flexBasis(300.px)
          .maxWidth(420.px)
          .overflow(Overflow.hidden)
          .shadow(0.px, 1.px, 2.px, 0.px, shadowSubtle)
          .cursor(Cursor.pointer)
          .transition(TransitionProperty.all, 180, Easing.easeOut)
          .hover(Style.borderColor(brandBorder).translate(0.px, (-3).px).shadow(0.px, 14.px, 30.px, (-8).px, shadowMedium)),
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
          .align(Alignment.stretch)
          .cursor(Cursor.pointer)
          .textAlign(TextAlign.left)
          .color(ink)
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
          .align(Alignment.stretch)
          .cursor(Cursor.pointer)
          .textAlign(TextAlign.left)
          .color(ink)
          .shadow(0.px, 0.px, 0.px, 3.px, focusRing),
      )
      // Rich searchable Combo Box (material picker)
      .rule("mp-combo", Style.position(Position.relative).width(100.pct))
      .rule(
        "mp-combo-trigger",
        Style.row
          .align(Alignment.center)
          .gap(12.px)
          .width(100.pct)
          .padding(10.px, 12.px)
          .rounded(12.px)
          .border(1.px, borderStrong)
          .bg(surface)
          .color(ink)
          .cursor(Cursor.pointer)
          .textAlign(TextAlign.left)
          .transition(TransitionProperty.all, 120, Easing.easeOut)
          .hover(Style.borderColor(brandBorder).bg(surfaceAlt)),
      )
      .rule(
        "mp-combo-open",
        Style.position(Position.relative)
          .zIndex(60)
          .column
          .width(100.pct)
          .rounded(12.px)
          .border(1.px, brand)
          .bg(surface)
          .shadow(0.px, 0.px, 0.px, 3.px, focusRing),
      )
      .rule(
        "mp-combo-search",
        Style.row.align(Alignment.center).gap(8.px).padding(4.px, 6.px, 4.px, 12.px),
      )
      .rule(
        "mp-combo-input",
        Style.flexGrow(1.0)
          .minWidth(0.px)
          .padding(8.px, 0.px)
          .border(0.px, BorderStyle.none, Color.transparent)
          .bg(Color.transparent)
          .color(ink)
          .fontSize(14.px),
      )
      .rule(
        "mp-combo-panel",
        Style.position(Position.absolute)
          .top(100.pct)
          .left(0.px)
          .right(0.px)
          .zIndex(60)
          .margin(6.px, 0.px, 0.px, 0.px)
          .column
          .bg(surface)
          .border(1.px, border)
          .rounded(12.px)
          .overflow(Overflow.hidden)
          .animation("mp-drop-in", 140, Easing.easeOut)
          .shadow(0.px, 18.px, 40.px, (-8).px, shadowStrong),
      )
      .rule("mp-combo-scroll", Style.column.maxHeight(360.px).overflowY(Overflow.auto).padding(4.px))
      .rule(
        "mp-combo-group",
        Style.padding(10.px, 10.px, 4.px, 10.px)
          .fontSize(11.px)
          .fontWeight(FontWeight.w800)
          .letterSpacing(0.5.px)
          .textTransform(TextTransform.uppercase)
          .color(inkMuted),
      )
      .rule(
        "mp-combo-option",
        Style.row
          .align(Alignment.center)
          .gap(12.px)
          .width(100.pct)
          .padding(8.px, 10.px)
          .rounded(8.px)
          .bg(Color.transparent)
          .border(0.px, BorderStyle.none, Color.transparent)
          .color(ink)
          .cursor(Cursor.pointer)
          .textAlign(TextAlign.left)
          .hover(Style.bg(surfaceAlt)),
      )
      .rule("mp-combo-option-active", Style.bg(surfaceHover).hover(Style.bg(surfaceHover)))
      .rule(Selector.cls("mp-combo-option-selected").descendant(Selector.cls("mp-combo-label")), Style.color(brandDark))
      .rule("mp-combo-leading", Style.row.flexShrink(0.0))
      .rule("mp-combo-text", Style.column.gap(2.px).flexGrow(1.0).minWidth(0.px))
      .rule("mp-combo-label", Style.fontSize(14.px).fontWeight(FontWeight.w700).color(ink).lineHeight(1.3))
      .rule("mp-combo-meta", Style.fontSize(12.px).color(inkMuted).lineHeight(1.35))
      .rule("mp-combo-trailing", Style.row.flexShrink(0.0))
      .rule(
        "mp-combo-weight",
        Style.padding(3.px, 8.px)
          .rounded(999.px)
          .bg(surfaceAlt)
          .border(1.px, border)
          .fontSize(12.px)
          .fontWeight(FontWeight.w700)
          .color(inkSecondary)
          .whiteSpace(WhiteSpace.noWrap),
      )
      .rule("mp-combo-check", Style.row.width(16.px).flexShrink(0.0).color(brand))
      .rule("mp-combo-chevron", Style.row.flexShrink(0.0).color(inkMuted))
      .rule("mp-combo-mark", Style.bg(amberSoft).color(amberInk).rounded(3.px))
      .rule(
        "mp-combo-empty",
        Style.column.align(Alignment.center).gap(10.px).padding(22.px, 16.px).color(inkMuted).fontSize(13.px),
      )
      .rule(
        "mp-combo-footer",
        Style.row
          .justify(Justification.spaceBetween)
          .gap(10.px)
          .padding(7.px, 12.px)
          .borderTop(1.px, border)
          .bg(surfaceAlt)
          .fontSize(11.px)
          .color(inkMuted),
      )
      .rule(
        "mp-combo-swatch",
        Style.width(30.px).height(30.px).rounded(8.px).border(1.px, borderStrong).flexShrink(0.0),
      )
      .rule("mp-swatch-uncoated", Style.bgGradient(GradientDirection.toBottomRight, (Color.hex("#FAFAF7").getOrElse(Color.white), 0.pct), (Color.hex("#E7E5DE").getOrElse(Color.white), 100.pct)))
      .rule("mp-swatch-coatedgloss", Style.bgGradient(GradientDirection.toBottomRight, (Color.hex("#FFFFFF").getOrElse(Color.white), 0.pct), (Color.hex("#C7D2FE").getOrElse(Color.white), 100.pct)))
      .rule("mp-swatch-coatedsilk", Style.bgGradient(GradientDirection.toBottomRight, (Color.hex("#F8FAFC").getOrElse(Color.white), 0.pct), (Color.hex("#DDE3EC").getOrElse(Color.white), 100.pct)))
      .rule("mp-swatch-coatedmatte", Style.bgGradient(GradientDirection.toBottomRight, (Color.hex("#F1F2F4").getOrElse(Color.white), 0.pct), (Color.hex("#D4D7DD").getOrElse(Color.white), 100.pct)))
      .rule("mp-swatch-naturaltextured", Style.bgGradient(GradientDirection.toBottomRight, (Color.hex("#E9D8B8").getOrElse(Color.white), 0.pct), (Color.hex("#B9946A").getOrElse(Color.white), 100.pct)))
      .rule("mp-swatch-transparentfilm", Style.bgGradient(GradientDirection.toBottomRight, (Color.hex("#E0F2FE").getOrElse(Color.white), 0.pct), (Color.hex("#F8FAFC").getOrElse(Color.white), 100.pct)))
      .rule("mp-swatch-metallic", Style.bgGradient(GradientDirection.toBottomRight, (Color.hex("#E5E7EB").getOrElse(Color.white), 0.pct), (Color.hex("#9CA3AF").getOrElse(Color.white), 100.pct)))
      .rule("mp-swatch-glazed", Style.bgGradient(GradientDirection.toBottomRight, (Color.hex("#FFFFFF").getOrElse(Color.white), 0.pct), (Color.hex("#BFDBFE").getOrElse(Color.white), 100.pct)))
      .rule("mp-weight-variants", Style.row.flexWrap(FlexWrap.wrap).align(Alignment.center).gap(8.px))
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
          .gap(7.px)
          .padding(9.px, 16.px)
          .rounded(10.px)
          .fontSize(14.px)
          .fontWeight(FontWeight.w600)
          .lineHeight(1.25)
          .whiteSpace(WhiteSpace.noWrap)
          .cursor(Cursor.pointer)
          .textDecoration(TextDecoration.none)
          .border(1.px, Color.transparent)
          .transition(TransitionProperty.all, 140, Easing.easeOut)
          .active(Style.translate(0.px, 1.px)),
      )
      .rule(
        "mp-btn-primary",
        Style.bg(brand)
          .color(onBrand)
          .shadow(0.px, 1.px, 2.px, 0.px, shadowMedium)
          .hover(Style.bg(brandHover).shadow(0.px, 6.px, 16.px, (-6).px, Color.rgba(79, 70, 229, 0.55)))
          .disabled(Style.opacity(0.5).cursor(Cursor.notAllowed)),
      )
      .rule(
        "mp-btn-secondary",
        Style.bg(surface)
          .color(ink)
          .border(1.px, borderStrong)
          .shadow(0.px, 1.px, 2.px, 0.px, shadowSubtle)
          .hover(Style.bg(surfaceAlt).borderColor(brandBorder))
          .disabled(Style.opacity(0.5).cursor(Cursor.notAllowed)),
      )
      .rule(
        "mp-btn-ghost",
        Style.bg(Color.transparent)
          .color(inkSecondary)
          .hover(Style.bg(surfaceHover).color(ink))
          .disabled(Style.opacity(0.5).cursor(Cursor.notAllowed)),
      )
      .rule(
        "mp-btn-nav-active",
        Style.bg(brandSoft).color(brandDark).hover(Style.bg(brandSoft).color(brandDark)),
      )
      .rule(
        "mp-btn-danger",
        Style.bg(roseSoft)
          .color(roseInk)
          .border(1.px, roseBorder)
          .hover(Style.bg(rose).color(Color.white).borderColor(rose))
          .disabled(Style.opacity(0.5).cursor(Cursor.notAllowed)),
      )
      .rule(
        "mp-btn-sm",
        Style.padding(6.px, 12.px).fontSize(13.px).rounded(8.px),
      )
      .rule(
        "mp-btn-success",
        Style.bg(emerald)
          .color(onBrand)
          .shadow(0.px, 6.px, 16.px, (-6).px, Color.rgba(5, 150, 105, 0.55))
          .animation("mp-fade-in", 160, Easing.easeOut),
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
          .transition(TransitionProperty.all, 120, Easing.easeOut)
          .hover(Style.borderColor(inkMuted))
          .focus(Style.borderColor(brand).shadow(0.px, 0.px, 0.px, 3.px, focusRing)),
      )
      .rule(
        "mp-field-row",
        Style.row.flexWrap(FlexWrap.wrap).gap(12.px).width(100.pct),
      )
      .rule(
        "mp-field",
        Style.column.gap(6.px).flexGrow(1.0).minWidth(0.px),
      )
      // Configurator price dock (mobile / tablet only; shown by the <= 960px media block)
      .rule("mp-cfg-dock", Style.displayNone)
      .rule("mp-cfg-dock-backdrop", Style.displayNone)
      .rule(
        "mp-cfg-dock-bar",
        Style.row
          .align(Alignment.center)
          .gap(10.px)
          .padding(8.px, 8.px, 8.px, 6.px)
          .bg(surface),
      )
      .rule(
        "mp-cfg-dock-toggle",
        Style.row
          .align(Alignment.center)
          .gap(8.px)
          .flexGrow(1.0)
          .minWidth(0.px)
          .minHeight(48.px)
          .padding(4.px, 8.px)
          .rounded(12.px)
          .bg(Color.transparent)
          .border(0.px, Color.transparent)
          .color(ink)
          .textAlign(TextAlign.left)
          .cursor(Cursor.pointer)
          .hover(Style.bg(surfaceHover)),
      )
      .rule(
        "mp-cfg-dock-chevron",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .width(30.px)
          .height(30.px)
          .flexShrink(0.0)
          .rounded(999.px)
          .bg(surfaceAlt)
          .color(inkSecondary),
      )
      .rule("mp-cfg-dock-label", Style.fontSize(11.px).fontWeight(FontWeight.w600).color(inkMuted).letterSpacing(0.3.px).lineHeight(1.2))
      .rule("mp-cfg-dock-price", Style.fontSize(20.px).fontWeight(FontWeight.w800).color(ink).lineHeight(1.2).letterSpacing((-0.3).px))
      .rule("mp-cfg-dock-sub", Style.fontSize(12.px).fontWeight(FontWeight.w600).color(brand).lineHeight(1.2))
      .rule(
        "mp-cfg-dock-cta",
        Style.flexShrink(0.0).minHeight(48.px).padding(10.px, 18.px).rounded(12.px).fontSize(15.px),
      )
      .rule(
        "mp-cfg-dock-sheet",
        Style.column
          .gap(14.px)
          .padding(6.px, 16.px, 16.px, 16.px)
          .maxHeight(62.vh)
          .overflowY(Overflow.auto)
          .bg(surface)
          .borderBottom(1.px, border)
          .animation("mp-rise-in", 200, Easing.easeOut),
      )
      .rule(
        "mp-cfg-dock-grabber",
        Style.width(40.px).height(4.px).rounded(999.px).bg(borderStrong).margin(4.px, Length.Auto, 0.px, Length.Auto).flexShrink(0.0),
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
          .animation("mp-fade-in", 160, Easing.easeOut)
          .row
          .justify(Justification.end),
      )
      .rule(
        "mp-drawer",
        Style.column
          .width(460.px)
          .maxWidth(100.pct)
          .height(100.pct)
          .bg(surface)
          .borderLeft(1.px, border)
          .shadow((-12).px, 0.px, 40.px, 0.px, shadowStrong)
          .animation("mp-slide-in-right", 220, Easing.easeOut)
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
          .animation("mp-fade-in", 160, Easing.easeOut)
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
          .color(ink)
          .border(1.px, border)
          .rounded(20.px)
          .padding(24.px)
          .gap(16.px)
          .overflowY(Overflow.auto)
          .animation("mp-rise-in", 200, Easing.easeOut)
          .shadow(0.px, 24.px, 56.px, (-12).px, shadowStrong),
      )
      // Toast
      .rule(
        "mp-toast",
        Style.position(Position.fixed)
          .bottom(20.px)
          .right(20.px)
          .zIndex(100)
          .maxWidth(420.px)
          .padding(10.px, 10.px, 10.px, 14.px)
          .rounded(14.px)
          .bg(inverseBg)
          .color(inverseInk)
          .fontSize(14.px)
          .fontWeight(FontWeight.w600)
          .animation("mp-rise-in", 220, Easing.easeOut)
          .shadow(0.px, 12.px, 32.px, (-6).px, shadowStrong),
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
          .minWidth(200.px)
          .margin(6.px, 0.px, 0.px, 0.px)
          .bg(surface)
          .border(1.px, border)
          .rounded(12.px)
          .padding(5.px)
          .animation("mp-drop-in", 140, Easing.easeOut)
          .shadow(0.px, 14.px, 34.px, (-6).px, shadowMedium)
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
      // ── Icons ──────────────────────────────────────────────────────────────
      .rule(
        "mp-icon",
        Style.display(Display.inlineFlex)
          .align(Alignment.center)
          .justify(Justification.center)
          .flexShrink(0.0)
          .lineHeight(1.0),
      )
      // ── Segmented Control (single-select multi-button) ─────────────────────
      .rule(
        "mp-seg",
        Style.row
          .align(Alignment.center)
          .gap(2.px)
          .padding(3.px)
          .rounded(11.px)
          .bg(surfaceAlt)
          .border(1.px, border)
          .flexShrink(0.0),
      )
      .rule(
        "mp-seg-btn",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .gap(6.px)
          .padding(5.px, 10.px)
          .rounded(8.px)
          .bg(Color.transparent)
          .border(0.px, BorderStyle.none, Color.transparent)
          .color(inkMuted)
          .fontSize(12.px)
          .fontWeight(FontWeight.w700)
          .letterSpacing(0.2.px)
          .lineHeight(1.2)
          .whiteSpace(WhiteSpace.noWrap)
          .cursor(Cursor.pointer)
          .transition(TransitionProperty.all, 140, Easing.easeOut)
          .hover(Style.color(ink)),
      )
      .rule(
        "mp-seg-btn-active",
        Style.bg(surface)
          .color(ink)
          .shadow(0.px, 1.px, 3.px, 0.px, shadowMedium)
          .hover(Style.color(ink)),
      )
      .rule(
        "mp-seg-full",
        Style.width(100.pct),
      )
      .rule(
        Selector.cls("mp-seg-full").child(Selector.cls("mp-seg-btn")),
        Style.flexGrow(1.0).flexBasis(0.px),
      )
      // ── Joined Button Group ────────────────────────────────────────────────
      .rule(
        "mp-btn-group",
        Style.row
          .align(Alignment.stretch)
          .flexShrink(0.0),
      )
      .rule(
        Selector.cls("mp-btn-group").child(Selector.cls("mp-btn")),
        Style.rounded(0.px).margin(0.px, 0.px, 0.px, (-1).px),
      )
      .rule(
        Selector.cls("mp-btn-group").child(Selector.cls("mp-btn")).pseudo("first-child"),
        Style.rounded(10.px, 0.px, 0.px, 10.px).margin(0.px),
      )
      .rule(
        Selector.cls("mp-btn-group").child(Selector.cls("mp-btn")).pseudo("last-child"),
        Style.rounded(0.px, 10.px, 10.px, 0.px),
      )
      // ── Icon Buttons with Count Badge ──────────────────────────────────────
      .rule(
        "mp-icon-btn",
        Style.row
          .position(Position.relative)
          .align(Alignment.center)
          .justify(Justification.center)
          .width(38.px)
          .height(38.px)
          .flexShrink(0.0)
          .padding(0.px)
          .rounded(10.px)
          .bg(Color.transparent)
          .border(1.px, Color.transparent)
          .color(inkSecondary)
          .cursor(Cursor.pointer)
          .transition(TransitionProperty.all, 140, Easing.easeOut)
          .hover(Style.bg(surfaceHover).color(ink))
          .active(Style.translate(0.px, 1.px)),
      )
      .rule(
        "mp-icon-btn-outline",
        Style.bg(surface).border(1.px, border).hover(Style.bg(surfaceAlt).borderColor(borderStrong).color(ink)),
      )
      .rule(
        "mp-icon-btn-sm",
        Style.width(30.px).height(30.px).rounded(8.px),
      )
      .rule(
        "mp-count-badge",
        Style.row
          .position(Position.absolute)
          .top((-5).px)
          .right((-5).px)
          .align(Alignment.center)
          .justify(Justification.center)
          .minWidth(18.px)
          .height(18.px)
          .padding(0.px, 5.px)
          .rounded(999.px)
          .bg(rose)
          .color(Color.white)
          .border(2.px, surface)
          .fontSize(10.px)
          .fontWeight(FontWeight.w800)
          .lineHeight(1.0)
          .animation("mp-rise-in", 180, Easing.easeOut),
      )
      .rule(
        "mp-count-pill",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .minWidth(20.px)
          .height(20.px)
          .padding(0.px, 6.px)
          .rounded(999.px)
          .bg(Color.rgba(255, 255, 255, 0.24))
          .color(Color.white)
          .fontSize(11.px)
          .fontWeight(FontWeight.w800)
          .lineHeight(1.0),
      )
      // ── Avatar ─────────────────────────────────────────────────────────────
      .rule(
        "mp-avatar",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .width(32.px)
          .height(32.px)
          .flexShrink(0.0)
          .rounded(999.px)
          .bgGradient(GradientDirection.toBottomRight, (brandStatic, 0.pct), (violetStatic, 100.pct))
          .color(Color.white)
          .fontSize(12.px)
          .fontWeight(FontWeight.w800)
          .letterSpacing(0.3.px)
          .lineHeight(1.0),
      )
      .rule(
        "mp-avatar-sm",
        Style.width(24.px).height(24.px).fontSize(10.px),
      )
      .rule(
        "mp-avatar-lg",
        Style.width(40.px).height(40.px).fontSize(14.px),
      )
      // ── Brand Mark ─────────────────────────────────────────────────────────
      .rule(
        "mp-logo-mark",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .width(34.px)
          .height(34.px)
          .flexShrink(0.0)
          .rounded(10.px)
          .bgGradient(GradientDirection.toBottomRight, (brandStatic, 0.pct), (violetStatic, 100.pct))
          .color(Color.white)
          .shadow(0.px, 4.px, 12.px, (-2).px, Color.rgba(79, 70, 229, 0.45)),
      )
      // ── Search Field with Leading Icon ─────────────────────────────────────
      .rule(
        "mp-search",
        Style.position(Position.relative)
          .row
          .align(Alignment.center)
          .width(100.pct)
          .minWidth(0.px),
      )
      .rule(
        "mp-search-icon",
        Style.position(Position.absolute)
          .left(12.px)
          .top(0.px)
          .bottom(0.px)
          .row
          .align(Alignment.center)
          .color(inkMuted),
      )
      .rule(
        "mp-search-input",
        Style.padding(10.px, 12.px, 10.px, 38.px),
      )
      // ── Product Card Anatomy ───────────────────────────────────────────────
      .rule(
        "mp-product-visual",
        Style.row
          .position(Position.relative)
          .align(Alignment.center)
          .justify(Justification.spaceBetween)
          .gap(12.px)
          .height(104.px)
          .padding(18.px, 20.px)
          .overflow(Overflow.hidden)
          .borderBottom(1.px, border),
      )
      .rule(
        "mp-product-visual-img",
        Style.height(176.px)
          .padding(0.px)
          .bg(surfaceAlt),
      )
      .rule(
        "mp-product-glyph",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .width(56.px)
          .height(56.px)
          .rounded(16.px)
          .bg(surface)
          .border(1.px, border)
          .shadow(0.px, 6.px, 16.px, (-6).px, shadowMedium),
      )
      .rule(
        "mp-product-body",
        Style.column
          .gap(10.px)
          .padding(16.px, 20.px, 20.px, 20.px)
          .flexGrow(1.0),
      )
      .rule(
        "mp-chip",
        Style.display(Display.inlineFlex)
          .align(Alignment.center)
          .gap(5.px)
          .padding(3.px, 9.px)
          .rounded(999.px)
          .bg(surfaceAlt)
          .color(inkSecondary)
          .border(1.px, border)
          .fontSize(12.px)
          .fontWeight(FontWeight.w600)
          .lineHeight(1.2)
          .whiteSpace(WhiteSpace.noWrap),
      )
      // ── Product Detail (e-shop layout) ─────────────────────────────────────
      .rule(
        "mp-pd-breadcrumb",
        Style.row.flexWrap(FlexWrap.wrap).align(Alignment.center).gap(8.px).fontSize(13.px),
      )
      .rule(
        "mp-pd-crumb",
        Style.row
          .align(Alignment.center)
          .gap(6.px)
          .padding(4.px, 8.px)
          .rounded(8.px)
          .bg(Color.transparent)
          .border(1.px, Color.transparent)
          .color(inkSecondary)
          .fontSize(13.px)
          .fontWeight(FontWeight.w600)
          .cursor(Cursor.pointer)
          .hover(Style.bg(surfaceHover).color(ink)),
      )
      .rule(
        "mp-pd-top",
        Style.row.align(Alignment.start).gap(36.px).width(100.pct),
      )
      .rule(
        "mp-pd-gallery",
        Style.column
          .gap(12.px)
          .flexGrow(1.0)
          .flexShrink(1.0)
          .flexBasis(0.px)
          .minWidth(0.px)
          .position(Position.sticky)
          .top(82.px),
      )
      .rule(
        "mp-pd-media",
        Style.row
          .position(Position.relative)
          .align(Alignment.center)
          .justify(Justification.center)
          .width(100.pct)
          .height(460.px)
          .rounded(22.px)
          .overflow(Overflow.hidden)
          .bg(surfaceAlt)
          .border(1.px, border)
          .shadow(0.px, 18.px, 40.px, (-24).px, shadowMedium),
      )
      .rule(
        "mp-pd-buy",
        Style.column
          .gap(22.px)
          .flexGrow(1.0)
          .flexShrink(1.0)
          .flexBasis(0.px)
          .minWidth(0.px)
          .maxWidth(560.px),
      )
      .rule(
        "mp-pd-title",
        Style.margin(0.px).fontSize(34.px).fontWeight(FontWeight.w800).letterSpacing((-0.6).px).lineHeight(1.15).color(ink),
      )
      .rule("mp-pd-preset-list", Style.column.gap(8.px))
      .rule(
        "mp-pd-preset",
        Style.row
          .align(Alignment.center)
          .gap(12.px)
          .width(100.pct)
          .padding(12.px, 14.px)
          .rounded(14.px)
          .bg(surface)
          .border(1.px, border)
          .color(ink)
          .textAlign(TextAlign.left)
          .cursor(Cursor.pointer)
          .transition(TransitionProperty.all, 140, Easing.easeOut)
          .hover(Style.borderColor(brandBorder).bg(surfaceAlt)),
      )
      .rule(
        "mp-pd-preset-active",
        Style.bg(brandSoft).borderColor(brand).shadow(0.px, 0.px, 0.px, 3.px, focusRing).hover(Style.bg(brandSoft).borderColor(brand)),
      )
      .rule(
        "mp-pd-radio",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .width(18.px)
          .height(18.px)
          .flexShrink(0.0)
          .rounded(999.px)
          .border(2.px, borderStrong)
          .bg(surface),
      )
      .rule(Selector.cls("mp-pd-preset-active").descendant(Selector.cls("mp-pd-radio")), Style.borderColor(brand))
      .rule("mp-pd-radio-dot", Style.width(8.px).height(8.px).rounded(999.px).bg(brand))
      .rule(
        "mp-pd-price-box",
        Style.column
          .gap(10.px)
          .padding(18.px, 20.px)
          .rounded(16.px)
          .bg(surface)
          .border(1.px, border)
          .shadow(0.px, 1.px, 2.px, 0.px, shadowSubtle),
      )
      .rule(
        "mp-pd-price",
        Style.fontSize(26.px).fontWeight(FontWeight.w800).letterSpacing((-0.6).px).lineHeight(1.1).color(ink),
      )
      .rule("mp-pd-paths", Style.column.gap(10.px))
      .rule(
        "mp-pd-path",
        Style.row
          .align(Alignment.center)
          .gap(14.px)
          .width(100.pct)
          .padding(16.px, 18.px)
          .rounded(16.px)
          .border(1.px, Color.transparent)
          .color(Color.white)
          .textAlign(TextAlign.left)
          .textDecoration(TextDecoration.none)
          .cursor(Cursor.pointer)
          .transition(TransitionProperty.all, 160, Easing.easeOut)
          .active(Style.translate(0.px, 1.px)),
      )
      .rule(
        "mp-pd-path-primary",
        Style.bg(brand)
          .shadow(0.px, 10.px, 24.px, (-10).px, Color.rgba(79, 70, 229, 0.55))
          .hover(Style.bg(brandHover).shadow(0.px, 14.px, 30.px, (-10).px, Color.rgba(79, 70, 229, 0.7)).translate(0.px, (-1).px)),
      )
      .rule(
        "mp-pd-path-studio",
        Style.bgGradient(GradientDirection.toRight, (brandDeep, 0.pct), (violetStatic, 100.pct))
          .shadow(0.px, 10.px, 24.px, (-10).px, Color.rgba(124, 58, 237, 0.55))
          .hover(Style.shadow(0.px, 14.px, 30.px, (-10).px, Color.rgba(124, 58, 237, 0.75)).translate(0.px, (-1).px)),
      )
      .rule(
        "mp-pd-path-icon",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .width(44.px)
          .height(44.px)
          .flexShrink(0.0)
          .rounded(12.px)
          .bg(Color.rgba(255, 255, 255, 0.18)),
      )
      .rule("mp-pd-path-copy", Style.column.gap(3.px).flexGrow(1.0).minWidth(0.px))
      .rule("mp-pd-path-title", Style.fontSize(16.px).fontWeight(FontWeight.w700).lineHeight(1.25))
      .rule(
        "mp-pd-path-desc",
        Style.fontSize(13.px).fontWeight(FontWeight.w400).lineHeight(1.45).color(Color.rgba(255, 255, 255, 0.86)),
      )
      .rule("mp-pd-path-arrow", Style.row.align(Alignment.center).flexShrink(0.0).opacity(0.85))
      .rule(
        "mp-pd-link",
        Style.bg(Color.transparent)
          .border(0.px, BorderStyle.none, Color.transparent)
          .padding(0.px)
          .fontSize(13.px)
          .fontWeight(FontWeight.w600)
          .color(brand)
          .cursor(Cursor.pointer)
          .textDecoration(TextDecoration.underline)
          .hover(Style.color(brandHover)),
      )
      .rule("mp-pd-qty", Style.column.gap(8.px).padding(0.px, 0.px, 12.px, 0.px).borderBottom(1.px, border))
      .rule("mp-pd-qty-row", Style.row.flexWrap(FlexWrap.wrap).align(Alignment.center).gap(6.px))
      .rule("mp-pd-qty-input", Style.width(96.px).padding(6.px, 10.px).fontSize(13.px))
      .rule(
        "mp-pd-quick",
        Style.column
          .gap(14.px)
          .padding(18.px)
          .rounded(18.px)
          .bg(surfaceAlt)
          .border(1.px, border),
      )
      .rule(
        "mp-pd-trust",
        Style.row
          .flexWrap(FlexWrap.wrap)
          .gap(1.px)
          .rounded(16.px)
          .overflow(Overflow.hidden)
          .bg(border)
          .border(1.px, border),
      )
      .rule(
        "mp-pd-trust-item",
        Style.row
          .align(Alignment.center)
          .gap(12.px)
          .padding(16.px, 18.px)
          .bg(surface)
          .flexGrow(1.0)
          .flexBasis(220.px),
      )
      .rule(
        "mp-pd-trust-icon",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .width(38.px)
          .height(38.px)
          .flexShrink(0.0)
          .rounded(12.px)
          .bg(brandSoft)
          .color(brand),
      )
      .rule("mp-pd-details", Style.row.align(Alignment.start).gap(24.px).width(100.pct))
      .rule(
        "mp-pd-details-main",
        Style.column.gap(24.px).flexGrow(1.0).flexShrink(1.0).flexBasis(0.px).minWidth(0.px),
      )
      .rule("mp-pd-details-aside", Style.column.gap(24.px).width(360.px).flexShrink(0.0))
      .rule("mp-pd-compare", Style.row.flexWrap(FlexWrap.wrap).gap(14.px).align(Alignment.stretch))
      .rule(
        "mp-pd-compare-card",
        Style.column
          .gap(12.px)
          .padding(16.px, 18.px)
          .rounded(16.px)
          .bg(surface)
          .border(1.px, border)
          .flexGrow(1.0)
          .flexShrink(1.0)
          .flexBasis(240.px)
          .minWidth(0.px)
          .transition(TransitionProperty.all, 140, Easing.easeOut)
          .hover(Style.borderColor(brandBorder)),
      )
      .rule("mp-pd-compare-card-active", Style.borderColor(brand).shadow(0.px, 0.px, 0.px, 3.px, focusRing))
      .rule(
        "mp-pd-spec-row",
        Style.row.justify(Justification.spaceBetween).align(Alignment.start).gap(16.px).padding(10.px, 0.px).borderBottom(1.px, border),
      )
      .rule(
        "mp-pd-step-num",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .width(24.px)
          .height(24.px)
          .flexShrink(0.0)
          .rounded(999.px)
          .bg(brandSoft)
          .color(brand)
          .fontSize(12.px)
          .fontWeight(FontWeight.w800),
      )
      // ── Design Studio promo band ───────────────────────────────────────────
      .rule(
        "mp-studio-promo",
        Style.row
          .align(Alignment.center)
          .gap(32.px)
          .width(100.pct)
          .padding(36.px, 40.px)
          .rounded(24.px)
          .overflow(Overflow.hidden)
          .color(Color.white)
          .bgGradient(GradientDirection.toBottomRight, (violetStatic, 0.pct), (brandStatic, 60.pct), (brandDeep, 100.pct))
          .shadow(0.px, 18.px, 40.px, (-14).px, Color.rgba(124, 58, 237, 0.45)),
      )
      .rule("mp-studio-copy", Style.column.gap(14.px).flexGrow(1.0).flexShrink(1.0).flexBasis(0.px).minWidth(0.px))
      .rule(
        "mp-studio-eyebrow",
        Style.row
          .align(Alignment.center)
          .alignSelf(Alignment.start)
          .gap(6.px)
          .padding(5.px, 12.px)
          .rounded(999.px)
          .bg(Color.rgba(255, 255, 255, 0.16))
          .border(1.px, Color.rgba(255, 255, 255, 0.24))
          .fontSize(12.px)
          .fontWeight(FontWeight.w700)
          .letterSpacing(0.4.px),
      )
      .rule(
        "mp-studio-title",
        Style.margin(0.px).fontSize(28.px).fontWeight(FontWeight.w800).letterSpacing((-0.5).px).lineHeight(1.2).color(Color.white),
      )
      .rule(
        "mp-studio-feat-icon",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .width(26.px)
          .height(26.px)
          .flexShrink(0.0)
          .rounded(8.px)
          .bg(Color.rgba(255, 255, 255, 0.16)),
      )
      .rule(
        "mp-studio-cta",
        Style.bg(Color.white)
          .color(brandDeep)
          .padding(12.px, 20.px)
          .rounded(12.px)
          .fontWeight(FontWeight.w700)
          .shadow(0.px, 8.px, 20.px, (-6).px, Color.rgba(15, 23, 42, 0.35))
          .hover(Style.bg(Color.hex("#EEF2FF").getOrElse(Color.white))),
      )
      .rule("mp-studio-visual", Style.row.justify(Justification.center).flexShrink(0.0).width(380.px))
      .rule(
        "mp-studio-window",
        Style.column
          .width(100.pct)
          .rounded(16.px)
          .overflow(Overflow.hidden)
          .bg(Color.rgba(255, 255, 255, 0.10))
          .border(1.px, Color.rgba(255, 255, 255, 0.28))
          .shadow(0.px, 24.px, 48.px, (-16).px, Color.rgba(15, 23, 42, 0.55)),
      )
      .rule(
        "mp-studio-window-bar",
        Style.row.gap(6.px).padding(10.px, 12.px).bg(Color.rgba(255, 255, 255, 0.12)).borderBottom(1.px, Color.rgba(255, 255, 255, 0.18)),
      )
      .rule("mp-studio-window-body", Style.row.align(Alignment.stretch).height(210.px))
      .rule(
        "mp-studio-tools",
        Style.column.gap(10.px).padding(12.px, 10.px).borderRight(1.px, Color.rgba(255, 255, 255, 0.18)).color(Color.rgba(255, 255, 255, 0.9)),
      )
      .rule("mp-studio-canvas", Style.row.align(Alignment.center).justify(Justification.center).flexGrow(1.0).padding(18.px))
      .rule(
        "mp-studio-card",
        Style.row
          .gap(12.px)
          .align(Alignment.center)
          .width(230.px)
          .height(132.px)
          .padding(14.px)
          .rounded(8.px)
          .bg(Color.white)
          .shadow(0.px, 14.px, 28.px, (-10).px, Color.rgba(15, 23, 42, 0.5)),
      )
      .rule(
        "mp-studio-card-photo",
        Style.width(80.px)
          .height(100.px)
          .flexShrink(0.0)
          .rounded(6.px)
          .bgGradient(GradientDirection.toBottomRight, (Color.hex("#F472B6").getOrElse(Color.white), 0.pct), (Color.hex("#6366F1").getOrElse(Color.white), 100.pct)),
      )
      .rule("mp-studio-card-lines", Style.column.gap(8.px).flexGrow(1.0))
      // ── Section Header & Empty State ───────────────────────────────────────
      .rule(
        "mp-eyebrow",
        Style.fontSize(11.px)
          .fontWeight(FontWeight.w800)
          .letterSpacing(0.8.px)
          .textTransform(TextTransform.uppercase)
          .color(brand),
      )
      .rule(
        "mp-page-title",
        Style.margin(0.px)
          .fontSize(28.px)
          .fontWeight(FontWeight.w800)
          .letterSpacing((-0.5).px)
          .lineHeight(1.2)
          .color(ink),
      )
      .rule(
        "mp-empty",
        Style.column
          .align(Alignment.center)
          .justify(Justification.center)
          .textAlign(TextAlign.center)
          .gap(10.px)
          .padding(48.px, 24.px)
          .rounded(18.px)
          .bg(surface)
          .border(1.px, BorderStyle.dashed, borderStrong),
      )
      .rule(
        "mp-empty-icon",
        Style.row
          .align(Alignment.center)
          .justify(Justification.center)
          .width(52.px)
          .height(52.px)
          .rounded(16.px)
          .bg(brandSoft)
          .color(brand),
      )
      // ── Theme Toggle Menu ──────────────────────────────────────────────────
      .rule(
        "mp-theme-menu",
        Style.position(Position.absolute)
          .top(100.pct)
          .right(0.px)
          .zIndex(60)
          .minWidth(170.px)
          .margin(8.px, 0.px, 0.px, 0.px)
          .padding(5.px)
          .bg(surface)
          .border(1.px, border)
          .rounded(12.px)
          .animation("mp-drop-in", 140, Easing.easeOut)
          .shadow(0.px, 14.px, 34.px, (-6).px, shadowMedium)
          .column
          .gap(2.px),
      )
      // ── Responsive Visibility Helpers ──────────────────────────────────────
      .rule("mp-show-sm", Style.displayNone)
      // ── Keyboard Focus Rings (only for keyboard navigation) ────────────────
      .rule(Selector.cls("mp-btn").pseudo("focus-visible"), focusVisibleRing)
      .rule(Selector.cls("mp-icon-btn").pseudo("focus-visible"), focusVisibleRing)
      .rule(Selector.cls("mp-seg-btn").pseudo("focus-visible"), focusVisibleRing)
      .rule(Selector.cls("mp-pill").pseudo("focus-visible"), focusVisibleRing)
      .rule(Selector.cls("mp-pill-active").pseudo("focus-visible"), focusVisibleRing)
      .rule(Selector.cls("mp-tab").pseudo("focus-visible"), focusVisibleRing)
      .rule(Selector.cls("mp-option-card").pseudo("focus-visible"), focusVisibleRing)
      .rule(Selector.cls("mp-combo-trigger").pseudo("focus-visible"), focusVisibleRing)
      .rule(Selector.cls("mp-action-menu-item").pseudo("focus-visible"), focusVisibleRing)
      .rule(Selector.cls("mp-matrix-cell-btn").pseudo("focus-visible"), focusVisibleRing)
      // Quantity & Speed Price Matrix (Tier Table)
      .rule(
        "mp-matrix-modal",
        Style.width(680.px),
      )
      .rule(
        "mp-matrix-scroll",
        Style.width(100.pct).overflowX(Overflow.auto),
      )
      .rule(
        "mp-matrix-grid",
        Style.column.width(100.pct).minWidth(480.px).gap(6.px),
      )
      .rule(
        "mp-matrix-header",
        Style.row
          .align(Alignment.center)
          .gap(8.px)
          .padding(0.px, 4.px, 6.px, 4.px)
          .borderBottom(1.px, border),
      )
      .rule(
        "mp-matrix-row",
        Style.row
          .align(Alignment.stretch)
          .gap(8.px)
          .padding(4.px)
          .rounded(12.px)
          .transition(TransitionProperty.backgroundColor, 140, Easing.easeOut),
      )
      .rule(
        "mp-matrix-row-active",
        Style.bg(surfaceAlt),
      )
      .rule(
        "mp-matrix-qty-col",
        Style.column
          .justify(Justification.center)
          .align(Alignment.start)
          .gap(3.px)
          .width(136.px)
          .flexShrink(0.0)
          .padding(6.px, 8.px),
      )
      .rule(
        "mp-matrix-speed-col",
        Style.column
          .align(Alignment.center)
          .justify(Justification.center)
          .flexGrow(1.0)
          .flexBasis(110.px)
          .minWidth(100.px),
      )
      .rule(
        "mp-matrix-cell-btn",
        Style.column
          .align(Alignment.center)
          .justify(Justification.center)
          .gap(2.px)
          .width(100.pct)
          .padding(9.px, 10.px)
          .rounded(10.px)
          .bg(surface)
          .border(1.px, border)
          .color(ink)
          .cursor(Cursor.pointer)
          .textAlign(TextAlign.center)
          .transition(TransitionProperty.all, 140, Easing.easeOut)
          .hover(Style.borderColor(brandBorder).bg(surfaceAlt)),
      )
      .rule(
        "mp-matrix-cell-active",
        Style.bg(brandSoft)
          .border(1.px, brand)
          .shadow(0.px, 0.px, 0.px, 2.px, focusRing)
          .hover(Style.bg(brandSoft).borderColor(brand)),
      )
      .rule(
        "mp-matrix-cell-disabled",
        Style.bg(surfaceAlt)
          .border(1.px, border)
          .color(inkMuted)
          .opacity(0.6)
          .cursor(Cursor.notAllowed)
          .hover(Style.borderColor(border).bg(surfaceAlt)),
      )
      .rule(
        "mp-matrix-unit",
        Style.fontSize(13.px).fontWeight(FontWeight.w700).color(ink).lineHeight(1.2),
      )
      .rule(
        "mp-matrix-unit-active",
        Style.color(brandDark),
      )
      .rule(
        "mp-matrix-total",
        Style.fontSize(11.px).color(inkMuted).lineHeight(1.25),
      )
      // Field rows: fields share a row and wrap at 180px
      .rule(
        Selector.cls("mp-field-row").child(Selector.cls("mp-field")),
        Style.flexGrow(1.0).flexBasis(180.px),
      )
      // Compact desktop (<= 1200px): header nav collapses to icon-only buttons
      .media(Stylesheet.MediaQuery.maxWidth(1200.px))(
        Stylesheet.rule("mp-nav-label", visuallyHidden),
      )
      // Tablet breakpoint (<= 960px)
      .media(Stylesheet.MediaQuery.maxWidth(960.px))(
        Stylesheet
          .rule("mp-split-layout", Style.column.gap(20.px))
          // Stacked asides flow below the main column at full width (e.g. checkout's Order Summary)...
          .rule("mp-split-aside", Style.width(100.pct).position(Position.relative).top(0.px))
          // ...except the configurator's, whose role is taken over by the sticky price dock
          .rule("mp-cfg-aside", Style.displayNone)
          .rule(
            "mp-cfg-dock",
            Style.display(Display.flex).column
              .position(Position.sticky)
              .bottom(12.px)
              .zIndex(46)
              .width(100.pct)
              .bg(surface)
              .border(1.px, border)
              .rounded(18.px)
              .overflow(Overflow.hidden)
              .shadow(0.px, 18.px, 48.px, (-12).px, shadowStrong),
          )
          .rule("mp-cfg-dock-open", Style.borderColor(brandBorder))
          .rule(
            "mp-cfg-dock-backdrop",
            Style.display(Display.block)
              .position(Position.fixed)
              .top(0.px)
              .left(0.px)
              .right(0.px)
              .bottom(0.px)
              .zIndex(45)
              .bg(backdrop)
              .animation("mp-fade-in", 160, Easing.easeOut),
          )
          .rule("mp-desktop-nav", Style.displayNone)
          .rule("mp-hide-md", Style.displayNone)
          .rule("mp-mobile-only", Style.display(Display.flex).row.align(Alignment.center).gap(6.px))
          .rule("mp-hero-title", Style.fontSize(28.px))
          .rule("mp-pd-top", Style.column.gap(24.px))
          .rule("mp-pd-gallery", Style.width(100.pct).flexBasis(Length.Auto).position(Position.relative).top(0.px))
          .rule("mp-pd-buy", Style.width(100.pct).flexBasis(Length.Auto).maxWidth(100.pct))
          .rule("mp-pd-media", Style.height(360.px))
          .rule("mp-pd-details", Style.column)
          .rule("mp-pd-details-main", Style.width(100.pct).flexBasis(Length.Auto))
          .rule("mp-pd-details-aside", Style.width(100.pct))
          .rule("mp-studio-promo", Style.column.align(Alignment.stretch).padding(28.px, 24.px))
          .rule("mp-studio-visual", Style.width(100.pct))
          .rule("mp-dt-split", Style.column.minHeight(0.px))
          .rule("mp-dt-list-pane-split", Style.displayNone)
          .rule("mp-dt-splitter", Style.displayNone)
          .rule("mp-dt-detail-pane", Style.width(100.pct).flexBasis(Length.Auto).maxHeight(Length.Auto))
          .rule("mp-dt-mobile-back", Style.display(Display.flex).row.align(Alignment.center).gap(6.px)),
      )
      // Mobile breakpoint (<= 640px)
      .media(Stylesheet.MediaQuery.maxWidth(640.px))(
        Stylesheet
          .rule("mp-container", Style.padding(0.px, 14.px))
          .rule("mp-main", Style.padding(18.px, 14.px, 40.px, 14.px))
          .rule("mp-hide-sm", Style.displayNone)
          .rule("mp-show-sm", Style.display(Display.inlineFlex))
          .rule("mp-page-title", Style.fontSize(22.px))
          .rule("mp-toast", Style.left(12.px).right(12.px).bottom(12.px).maxWidth(Length.Auto))
          .rule("mp-product-visual", Style.height(88.px))
          .rule("mp-product-visual-img", Style.height(148.px))
          // Phones: the cart rises from the bottom edge as a sheet (continuing the configurator's price dock)
          .rule("mp-cart-backdrop", Style.column.justify(Justification.end))
          .rule(
            "mp-drawer",
            Style.width(100.pct)
              .height(Length.Auto)
              .maxHeight(92.pct)
              .borderLeft(1.px, Color.transparent)
              .borderTop(1.px, border)
              .rounded(20.px, 20.px, 0.px, 0.px)
              .shadow(0.px, (-12).px, 40.px, 0.px, shadowStrong)
              .animation("mp-slide-up", 240, Easing.easeOut),
          )
          .rule("mp-cfg-dock", Style.bottom(8.px).rounded(16.px))
          .rule("mp-cfg-dock-price", Style.fontSize(19.px))
          .rule("mp-cfg-dock-cta", Style.padding(10.px, 14.px))
          .rule("mp-header-inner", Style.padding(10.px, 14.px).gap(8.px))
          .rule("mp-hero", Style.padding(28.px, 20.px).rounded(18.px))
          .rule("mp-hero-title", Style.fontSize(24.px))
          .rule("mp-pd-media", Style.height(260.px).rounded(16.px))
          .rule("mp-pd-title", Style.fontSize(26.px))
          .rule("mp-pd-price", Style.fontSize(24.px))
          .rule("mp-pd-path", Style.padding(14.px).gap(12.px))
          .rule("mp-pd-quick", Style.padding(14.px))
          .rule("mp-studio-promo", Style.padding(24.px, 18.px).rounded(18.px))
          .rule("mp-studio-title", Style.fontSize(22.px))
          .rule("mp-studio-visual", Style.displayNone)
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
      Style.margin(0.px).padding(0.px).bg(canvas).color(ink).fontFamily(fontStack),
    ) ++ coreStylesheet

  val calculatorStylesheet: Stylesheet = coreStylesheet

  /** Storage key + `<html data-theme>` attribute name shared by every browser host. */
  val ThemeStorageKey: String = "mpbuilder.theme"

  /** Document-level CSS that typed `kyo.Style` cannot express (glass blur, native control color-scheme,
    * selection, scrollbars, tabular numerals, reduced motion, custom select chevron).
    * Injected verbatim by full-page browser hosts (`modules/ui`, `modules/ui-admin`); never by the embeddable calculator.
    */
  val documentCss: String =
    """html{-webkit-text-size-adjust:100%;color-scheme:light}
      |html[data-theme="dark"]{color-scheme:dark}
      |@media (prefers-color-scheme: dark){html[data-theme="system"]{color-scheme:dark}}
      |body{-webkit-font-smoothing:antialiased;-moz-osx-font-smoothing:grayscale;text-rendering:optimizeLegibility;font-feature-settings:"cv11","ss01","ss03"}
      |button,input,select,textarea{font:inherit;color:inherit}
      |::selection{background:var(--mp-brand-soft);color:var(--mp-ink)}
      |::placeholder{color:var(--mp-ink-muted);opacity:.85}
      |.mp-backdrop,.mp-modal-backdrop{-webkit-backdrop-filter:blur(3px);backdrop-filter:blur(3px)}
      |.mp-header,.mp-admin-topbar{-webkit-backdrop-filter:saturate(180%) blur(14px);backdrop-filter:saturate(180%) blur(14px)}
      |.mp-hero{position:relative;isolation:isolate}
      |.mp-hero::before{content:"";position:absolute;width:520px;height:520px;right:-140px;top:-220px;border-radius:50%;background:radial-gradient(closest-side,rgba(255,255,255,.22),rgba(255,255,255,0));z-index:-1;pointer-events:none}
      |.mp-hero::after{content:"";position:absolute;inset:0;background-image:radial-gradient(rgba(255,255,255,.14) 1px,transparent 1px);background-size:18px 18px;-webkit-mask-image:linear-gradient(to left,#000,transparent 70%);mask-image:linear-gradient(to left,#000,transparent 70%);z-index:-1;pointer-events:none}
      |.mp-product-visual:not(.mp-product-visual-img)::after{content:"";position:absolute;inset:0;background-image:radial-gradient(currentColor 1px,transparent 1px);background-size:14px 14px;opacity:.12;-webkit-mask-image:linear-gradient(to left,#000,transparent 75%);mask-image:linear-gradient(to left,#000,transparent 75%);pointer-events:none}
      |.mp-product-img{width:100%;height:100%;object-fit:cover;display:block}
      |.mp-pd-bullets,.mp-pd-facts,.mp-pd-steps,.mp-studio-feats{list-style:none;margin:0;padding:0;display:flex;flex-direction:column}
      |.mp-pd-bullets{gap:10px}.mp-pd-bullets li{display:flex;flex-direction:row;align-items:flex-start;gap:10px;font-size:14px;line-height:1.5;color:var(--mp-ink-secondary)}.mp-pd-bullets li>.mp-icon{color:var(--mp-emerald);margin-top:3px;flex-shrink:0}
      |.mp-pd-facts{gap:7px;flex-grow:1}.mp-pd-facts li{display:flex;flex-direction:row;align-items:flex-start;gap:8px;font-size:13px;line-height:1.4;color:var(--mp-ink-secondary)}
      |.mp-pd-steps{gap:12px}.mp-pd-steps li{display:flex;flex-direction:row;align-items:flex-start;gap:12px}
      |.mp-studio-feats{flex-direction:row;flex-wrap:wrap;gap:10px 18px}.mp-studio-feats li{display:flex;flex-direction:row;align-items:center;gap:8px;font-size:14px;font-weight:600;color:#fff}
      |.mp-pd-spec-row:last-child{border-bottom:0}
      |.mp-pd-media-glyph::after{content:"";position:absolute;inset:0;background-image:radial-gradient(currentColor 1px,transparent 1px);background-size:16px 16px;opacity:.14;pointer-events:none}
      |.mp-studio-promo{position:relative;isolation:isolate}
      |.mp-studio-promo::after{content:"";position:absolute;inset:0;background-image:radial-gradient(rgba(255,255,255,.14) 1px,transparent 1px);background-size:18px 18px;-webkit-mask-image:linear-gradient(to right,#000,transparent 65%);mask-image:linear-gradient(to right,#000,transparent 65%);z-index:-1;pointer-events:none}
      |.mp-studio-window-bar span{width:9px;height:9px;border-radius:50%;background:rgba(255,255,255,.45)}
      |.mp-studio-tools span{display:flex;align-items:center;justify-content:center;width:28px;height:28px;border-radius:8px;background:rgba(255,255,255,.12)}.mp-studio-tools span:first-child{background:#fff;color:#4F46E5}
      |.mp-studio-card{outline:1px dashed rgba(236,72,153,.7);outline-offset:6px}
      |.mp-studio-card-lines span{display:block;height:8px;border-radius:4px;background:#E2E8F0}.mp-studio-card-lines span:first-child{height:12px;width:85%;background:#312E81}.mp-studio-card-lines span:nth-child(2){width:65%}.mp-studio-card-lines span:nth-child(3){width:45%;background:#C4B5FD}
      |table,.mp-num,.mp-kpi-card,.mp-dt-table{font-variant-numeric:tabular-nums}
      |.mp-icon svg{display:block;overflow:visible}
      |*{scrollbar-width:thin;scrollbar-color:var(--mp-border-strong) transparent}
      |select.mp-input{-webkit-appearance:none;appearance:none;padding-right:36px;background-image:url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%2364748B' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");background-repeat:no-repeat;background-position:right 12px center}
      |input[type="checkbox"],input[type="radio"]{accent-color:var(--mp-brand)}
      |[data-tooltip]{position:relative}
      |[data-tooltip]:hover::after,[data-tooltip]:focus-visible::after{content:attr(data-tooltip);position:absolute;top:calc(100% + 8px);right:0;white-space:nowrap;padding:5px 9px;border-radius:8px;background:var(--mp-inverse-bg);color:var(--mp-inverse-ink);font-size:12px;font-weight:600;line-height:1.2;box-shadow:0 8px 20px -6px var(--mp-shadow-lg);z-index:70;pointer-events:none;animation:mp-drop-in 120ms ease-out}
      |:focus:not(:focus-visible){outline:none}
      |.mp-cfg-dock{margin-bottom:env(safe-area-inset-bottom)}
      |.mp-cfg-dock-toggle{-webkit-tap-highlight-color:transparent}
      |.mp-cfg-dock-chevron{transition:transform .2s ease}
      |.mp-cfg-dock-toggle:active .mp-cfg-dock-chevron{transform:scale(.9)}
      |.mp-cfg-dock-sheet{overscroll-behavior:contain}
      |@media (prefers-reduced-motion: reduce){*,*::before,*::after{animation-duration:1ms!important;animation-iteration-count:1!important;transition-duration:1ms!important;scroll-behavior:auto!important}}
      |""".stripMargin
