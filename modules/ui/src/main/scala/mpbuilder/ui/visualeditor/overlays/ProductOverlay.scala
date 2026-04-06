package mpbuilder.ui.visualeditor.overlays

import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.L.{Modifier, HtmlElement}
import mpbuilder.ui.visualeditor.{ProductContext, ProductFormat}

/** Non-interactive visual overlays that help the customer visualize the final product.
  * Rendered on top of the canvas page background, behind user elements.
  */
object ProductOverlay:

  def render(ctx: Option[ProductContext], format: ProductFormat): Modifier[HtmlElement] =
    ctx match
      case Some(c) =>
        val binding = c.bindingMethod.getOrElse("")
        val catId = c.categoryId.getOrElse("")

        if catId.contains("calendar") && (binding.contains("Spiral") || binding.contains("WireO")) then
          renderWireBinding(format)
        else if catId.contains("calendar") then
          renderCalendarHanger(format)
        else if catId.contains("roll-up") then
          renderRollUpStand(format)
        else if catId.contains("poster") then
          renderFrame(format)
        else
          emptyMod
      case None => emptyMod

  /** Wire/spiral binding rings along the top edge */
  private def renderWireBinding(format: ProductFormat): Element =
    val ringCount = format.widthMm / 20 // one ring per ~20mm
    val canvasWidth = if ProductFormat.isLandscape(format) then 760 else 560
    val spacing = canvasWidth.toDouble / (ringCount + 1)

    div(
      cls := "product-overlay wire-binding-overlay",
      styleAttr := "position: absolute; top: -8px; left: 0; right: 0; height: 16px; z-index: 9000; pointer-events: none; display: flex; justify-content: space-evenly; align-items: center;",
      (1 to ringCount).map { _ =>
        div(
          styleAttr := "width: 12px; height: 12px; border: 2px solid #888; border-radius: 50%; background: transparent;",
        )
      }.toList,
    )

  /** Simple hanger hole for calendars without wire binding */
  private def renderCalendarHanger(format: ProductFormat): Element =
    div(
      cls := "product-overlay calendar-hanger-overlay",
      styleAttr := "position: absolute; top: -4px; left: 50%; transform: translateX(-50%); z-index: 9000; pointer-events: none;",
      div(
        styleAttr := "width: 20px; height: 20px; border: 2px solid #aaa; border-radius: 50%; background: white;",
      ),
    )

  /** Roll-up stand platform at the bottom */
  private def renderRollUpStand(format: ProductFormat): Element =
    div(
      cls := "product-overlay rollup-stand-overlay",
      styleAttr := "position: absolute; bottom: -30px; left: 10%; right: 10%; z-index: 9000; pointer-events: none; text-align: center;",
      // Stand base
      div(
        styleAttr := "height: 8px; background: linear-gradient(to bottom, #999, #666); border-radius: 0 0 4px 4px;",
      ),
      // Support pole
      div(
        styleAttr := "width: 4px; height: 20px; background: #888; margin: 0 auto;",
      ),
      // Base plate
      div(
        styleAttr := "width: 60%; height: 6px; background: linear-gradient(to bottom, #777, #555); border-radius: 3px; margin: 0 auto;",
      ),
    )

  /** Decorative frame border for posters/pictures */
  private def renderFrame(format: ProductFormat): Element =
    div(
      cls := "product-overlay frame-overlay",
      styleAttr := "position: absolute; top: -6px; left: -6px; right: -6px; bottom: -6px; border: 6px solid #8B7355; z-index: 9000; pointer-events: none; box-shadow: inset 0 0 4px rgba(0,0,0,0.3);",
    )
