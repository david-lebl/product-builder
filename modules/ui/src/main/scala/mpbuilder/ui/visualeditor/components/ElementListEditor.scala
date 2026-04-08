package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.*

/** Layered element list for the (deprecated) Page Elements panel.
  *
  * Add controls and per-element forms have moved to:
  *   - Canvas top-right add buttons (photo + text)
  *   - Cliparts gallery panel (cliparts)
  *   - The element-controls popup that floats under the selected element
  *
  * This panel now exists only as a quick layered overview with reorder /
  * duplicate / delete actions.
  */
object ElementListEditor {
  def apply(): Element = {
    val currentPage = VisualEditorViewModel.currentPage
    val selectedElement = VisualEditorViewModel.selectedElement

    div(
      cls := "element-editor-section",

      h4("Page Elements"),

      div(
        cls := "deprecated-panel-hint",
        "Element controls have moved to the canvas — select an element to see its options.",
      ),

      div(
        cls := "elements-list",
        children <-- currentPage.combineWith(selectedElement).map { (page, selected) =>
          if page.elements.isEmpty then
            List(div(cls := "empty-elements", "No elements on this page"))
          else
            page.elements.sortBy(_.zIndex).reverse.map { elem =>
              renderElementItem(elem, selected)
            }
        }
      ),
    )
  }

  private def renderElementItem(elem: CanvasElement, selectedId: Option[String]): Element = {
    val isSelected = selectedId.contains(elem.id)
    val (icon, label) = elem match {
      case _: PhotoElement   => ("📷", "Photo")
      case t: TextElement    => ("T", s"\"${t.text.take(15)}${if t.text.length > 15 then "…" else ""}\"")
      case s: ShapeElement   => (if s.shapeType == ShapeType.Line then "—" else "▭", s.shapeType.toString)
      case _: ClipartElement => ("🎨", "Clipart")
    }

    div(
      cls := "element-item",
      cls := "selected" -> isSelected,

      div(
        cls := "element-item-row",

        span(cls := "element-icon", icon),
        span(cls := "element-label", label),

        div(
          cls := "element-actions",

          button(
            cls := "element-action-btn",
            title := "Bring to front",
            "↑",
            onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.bringToFront(elem.id) }
          ),
          button(
            cls := "element-action-btn",
            title := "Send to back",
            "↓",
            onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.sendToBack(elem.id) }
          ),
          button(
            cls := "element-action-btn",
            title := "Duplicate",
            "⎘",
            onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.duplicateElement(elem.id) }
          ),
          button(
            cls := "element-action-btn element-delete-btn",
            title := "Delete",
            "×",
            onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.removeElement(elem.id) }
          ),
        )
      ),

      onClick --> { _ => VisualEditorViewModel.selectElement(elem.id) }
    )
  }
}
