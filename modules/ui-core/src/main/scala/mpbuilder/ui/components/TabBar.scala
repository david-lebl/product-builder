package mpbuilder.ui.components

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** High-contrast folder-style tab bar for switching between distinct subviews.
  *
  * Active tab renders as a crisp white rounded folder tab seamlessly bridging
  * across the strong horizontal divider line beneath the tab strip.
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
  ): UI =
    div.cssClass("mp-tabs")(
      items.map { item =>
        val isActive = item.value == active
        button
          .cssClass(if isActive then "mp-tab mp-tab-active" else "mp-tab")
          .onClick(onSelect(item.value))(
            span(item.label),
            item.count match
              case Some(c) =>
                span(c.toString).style(
                  Style
                    .padding(1.px, 7.px)
                    .rounded(999.px)
                    .fontSize(11.px)
                    .fontWeight(FontWeight.w700)
                    .bg(if isActive then brandSoft else mpbuilder.ui.theme.AppTheme.Palette.border)
                    .color(if isActive then brand else inkSecondary),
                )
              case None => UI.empty,
          )
      }*
    )
