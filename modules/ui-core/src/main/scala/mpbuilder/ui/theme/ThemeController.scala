package mpbuilder.ui.theme

import kyo.*

/** Color-scheme preference. `System` follows the OS `prefers-color-scheme` setting. */
enum ThemeMode(val code: String):
  case System extends ThemeMode("system")
  case Light  extends ThemeMode("light")
  case Dark   extends ThemeMode("dark")

object ThemeMode:
  def fromCode(raw: String): ThemeMode =
    values.find(_.code == raw.trim.toLowerCase).getOrElse(System)

/** Reactive, platform-neutral theme preference shared by Storefront and Backoffice.
  *
  * The UI never touches the DOM: browser hosts pass `onApply`, which sets `data-theme` on `<html>`
  * (matching the `[data-theme=…]` variable blocks in [[AppTheme.themeStylesheet]]) and persists the choice.
  */
final class ThemeController(
    val mode: SignalRef[ThemeMode],
    onApply: ThemeMode => Unit < Sync,
):
  def set(next: ThemeMode): Unit < Sync =
    mode.set(next).andThen(onApply(next))

object ThemeController:
  def init(
      initial: ThemeMode = ThemeMode.System,
      onApply: ThemeMode => Unit < Sync = _ => Kyo.unit,
  ): ThemeController < Sync =
    for
      ref <- Signal.initRef(initial)
      _   <- onApply(initial)
    yield ThemeController(ref, onApply)
