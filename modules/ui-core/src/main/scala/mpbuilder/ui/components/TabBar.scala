package mpbuilder.ui.components

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Clean tab bar with a full-width bottom divider line clearly scoping all content below.
  *
  * Supports a primary page-level style (`compact = false`) and a sleek compact variant
  * (`compact = true`) for secondary tabs inside Master-Detail panes.
  */
object TabBar:

  final case class TabItem[T](
      value: T,
      label: String,
      count: Option[Int] = None,
  )

  def render[T](
      items: List[TabItem[T]],
      active: T,
      onSelect: T => Unit < Async,
      compact: Boolean = false,
  ): UI =
    val stripCls = if compact then "mp-tabs mp-tabs-sm" else "mp-tabs"
    val baseCls  = if compact then "mp-tab mp-tab-sm" else "mp-tab"
    val actCls   = if compact then "mp-tab mp-tab-sm mp-tab-active" else "mp-tab mp-tab-active"

    div.cssClass(stripCls)(
      items.map { item =>
        val isActive = item.value == active
        button
          .cssClass(if isActive then actCls else baseCls)
          .onClick(onSelect(item.value))(
            span(item.label),
            item.count match
              case Some(c) =>
                span(c.toString).style(
                  Style
                    .padding(1.px, 6.px)
                    .rounded(999.px)
                    .fontSize(11.px)
                    .fontWeight(FontWeight.w700)
                    .bg(if isActive then brandSoft else surfaceAlt)
                    .color(if isActive then brand else inkMuted),
                )
              case None => UI.empty,
          )
      }*
    )
