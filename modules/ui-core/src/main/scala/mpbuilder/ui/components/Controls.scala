package mpbuilder.ui.components

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.shared.Language
import mpbuilder.ui.components.Icons.Icon
import mpbuilder.ui.i18n.AppI18n
import mpbuilder.ui.theme.{ThemeController, ThemeMode}

/** Small, reusable interactive controls shared by Storefront, Backoffice and the Calculator:
  * segmented multi-buttons, language / theme switches, icon buttons with count badges, avatars,
  * toasts and empty states. All styling lives in `AppTheme` (`mp-seg*`, `mp-icon-btn*`, `mp-avatar*`, ...).
  */
object Controls:

  // ── Segmented Control ──────────────────────────────────────────────────────

  /** Single-select segmented control ("multi-button"): one pill per option, the active one raised. */
  def segmented[A](
      options: Seq[(A, UI)],
      current: A,
      onSelect: A => Any < Async,
      fullWidth: Boolean = false,
      ariaLabel: String = "",
  )(using CanEqual[A, A]): UI =
    div
      .cssClass(if fullWidth then "mp-seg mp-seg-full" else "mp-seg")
      .role("group")
      .aria("label", ariaLabel)(
        options.map { case (value, content) =>
          val active = value == current
          button
            .cssClass(if active then "mp-seg-btn mp-seg-btn-active" else "mp-seg-btn")
            .aria("pressed", active.toString)
            .onClick(onSelect(value))(content)
        }*
      )

  /** EN / CS language switch bound to the shared i18n language signal. */
  def languageSwitch(i18n: AppI18n, onSelect: Language => Any < Async): UI =
    i18n.language.render { lang =>
      segmented[Language](
        Seq(Language.En -> span("EN"), Language.Cs -> span("CS")),
        lang,
        onSelect,
        ariaLabel = if lang == Language.Cs then "Jazyk" else "Language",
      )
    }

  // ── Theme Toggle ───────────────────────────────────────────────────────────

  private def themeIcon(mode: ThemeMode): Icon = mode match
    case ThemeMode.System => Icon.Monitor
    case ThemeMode.Light  => Icon.Sun
    case ThemeMode.Dark   => Icon.Moon

  private def themeLabel(mode: ThemeMode, lang: Language): String = (mode, lang) match
    case (ThemeMode.System, Language.Cs) => "Motiv: podle systému"
    case (ThemeMode.System, _)           => "Theme: match system"
    case (ThemeMode.Light, Language.Cs)  => "Motiv: světlý"
    case (ThemeMode.Light, _)            => "Theme: light"
    case (ThemeMode.Dark, Language.Cs)   => "Motiv: tmavý"
    case (ThemeMode.Dark, _)             => "Theme: dark"

  private def nextMode(mode: ThemeMode): ThemeMode = mode match
    case ThemeMode.System => ThemeMode.Light
    case ThemeMode.Light  => ThemeMode.Dark
    case ThemeMode.Dark   => ThemeMode.System

  /** Icon button cycling System → Light → Dark; the glyph shows the active preference. */
  def themeToggle(theme: ThemeController, i18n: AppI18n, outlined: Boolean = false): UI =
    theme.mode.combineLatest(i18n.language).render { case (mode, lang) =>
      val label = themeLabel(mode, lang)
      button
        .cssClass(if outlined then "mp-icon-btn mp-icon-btn-outline" else "mp-icon-btn")
        .aria("label", label)
        .data("tooltip", label)
        .onClick(theme.set(nextMode(mode)))(Icons(themeIcon(mode), 18))
    }

  /** Full-width three-way theme selector (used inside mobile menus). */
  def themeSegmented(theme: ThemeController, i18n: AppI18n): UI =
    theme.mode.combineLatest(i18n.language).render { case (mode, lang) =>
      val isCs = lang == Language.Cs
      segmented[ThemeMode](
        Seq(
          ThemeMode.System -> UI.fragment(Icons(Icon.Monitor, 14), span(if isCs then "Systém" else "System")),
          ThemeMode.Light  -> UI.fragment(Icons(Icon.Sun, 14), span(if isCs then "Světlý" else "Light")),
          ThemeMode.Dark   -> UI.fragment(Icons(Icon.Moon, 14), span(if isCs then "Tmavý" else "Dark")),
        ),
        mode,
        m => theme.set(m),
        fullWidth = true,
        ariaLabel = if isCs then "Motiv" else "Theme",
      )
    }

  // ── Icon Buttons, Avatars & Count Badges ───────────────────────────────────

  /** Square icon-only button with an accessible label and an optional red count bubble. */
  def iconButton(
      icon: Icon,
      ariaLabel: String,
      onClick: => Any < Async,
      count: Int = 0,
      outlined: Boolean = false,
      small: Boolean = false,
  ): UI =
    val cls =
      List("mp-icon-btn") ++ Option.when(outlined)("mp-icon-btn-outline") ++ Option.when(small)("mp-icon-btn-sm")
    button
      .cssClass(cls.mkString(" "))
      .aria("label", ariaLabel)
      .onClick(onClick)(
        Icons(icon, if small then 15 else 18),
        if count > 0 then span(if count > 99 then "99+" else count.toString).cssClass("mp-count-badge") else UI.empty,
      )

  /** Up-to-two-letter initials from a display name ("Jana Nováková" → "JN"). */
  def initials(name: String): String =
    val parts = name.trim.split("\\s+").toList.filter(_.nonEmpty)
    parts match
      case Nil           => "?"
      case single :: Nil => single.take(2).toUpperCase
      case first :: rest => s"${first.head}${rest.last.head}".toUpperCase

  def avatar(name: String, size: String = "md"): UI =
    val sizeCls = size match
      case "sm" => " mp-avatar-sm"
      case "lg" => " mp-avatar-lg"
      case _    => ""
    span(initials(name)).cssClass(s"mp-avatar$sizeCls").aria("hidden", "true")

  /** Count pill rendered inside a primary button (e.g. "Cart 3"). */
  def countPill(count: Int): UI =
    if count > 0 then span(count.toString).cssClass("mp-count-pill") else UI.empty

  // ── Toast & Empty State ────────────────────────────────────────────────────

  /** Bottom-right toast with a success glyph and dismiss button. */
  def toast(message: String, onDismiss: => Any < Async, dismissLabel: String = "Dismiss"): UI =
    div
      .cssClass("mp-toast")
      .role("status")
      .aria("live", "polite")
      .style(Style.row.align(Alignment.center).gap(10.px))(
        span.style(Style.color(Color.hex("#34D399").getOrElse(Color.green)).row.align(Alignment.center))(Icons(Icon.CheckCircle, 18)),
        span(message).style(Style.flexGrow(1.0).minWidth(0.px)),
        button
          .cssClass("mp-icon-btn mp-icon-btn-sm")
          .style(Style.color(Color.rgba(248, 250, 252, 0.72)).hover(Style.bg(Color.rgba(255, 255, 255, 0.12)).color(Color.white)))
          .aria("label", dismissLabel)
          .onClick(onDismiss)(Icons(Icon.Close, 15)),
      )

  /** Centered dashed empty state with a soft brand glyph. */
  def emptyState(icon: Icon, title: UI, description: UI, action: UI = UI.empty): UI =
    div.cssClass("mp-empty")(
      div.cssClass("mp-empty-icon")(Icons(icon, 24)),
      h3(title).style(Style.margin(4.px, 0.px, 0.px, 0.px).fontSize(17.px).fontWeight(FontWeight.w700)),
      p(description).style(Style.margin(0.px).fontSize(14.px).maxWidth(420.px).lineHeight(1.55).color(mpbuilder.ui.theme.AppTheme.Palette.inkMuted)),
      action,
    )
