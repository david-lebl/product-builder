package mpbuilder.ui.components

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Reusable underline tab bar for switching between distinct subviews
  * (visually distinct from rounded `.mp-pill` filter chips).
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
                    .bg(if isActive then brand else surfaceAlt)
                    .color(if isActive then Color.white else inkSecondary),
                )
              case None => UI.empty,
          )
      }*
    )
