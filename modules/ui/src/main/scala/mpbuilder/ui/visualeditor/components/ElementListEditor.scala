package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.*
import org.scalajs.dom
import org.scalajs.dom.FileReader

/** Unified element list and editor for all canvas element types */
object ElementListEditor {
  def apply(): Element = {
    val currentPage = VisualEditorViewModel.currentPage
    val selectedElement = VisualEditorViewModel.selectedElement

    div(
      cls := "element-editor-section",

      // ─── Add element buttons ───────────────────────────────────
      div(
        cls := "add-element-buttons",

        // Photo upload
        input(
          typ := "file",
          accept := "image/*",
          idAttr := "photo-upload-input",
          display := "none",
          onChange --> { ev =>
            val inp = ev.target.asInstanceOf[dom.html.Input]
            val files = inp.files
            if files.length > 0 then
              val file = files(0)
              val reader = new FileReader()
              reader.onload = { _ =>
                val imageData = reader.result.asInstanceOf[String]
                VisualEditorViewModel.uploadPhoto(imageData)
              }
              reader.readAsDataURL(file)
          }
        ),

        button(
          cls := "add-element-btn add-photo-btn",
          "📷 Photo",
          title := "Upload photo",
          onClick --> { _ =>
            dom.document.getElementById("photo-upload-input").asInstanceOf[dom.html.Input].click()
          }
        ),

        button(
          cls := "add-element-btn add-text-btn",
          "T Text",
          title := "Add text field",
          onClick --> { _ => VisualEditorViewModel.addTextField() }
        ),

        button(
          cls := "add-element-btn add-rect-btn",
          "▭ Rect",
          title := "Add rectangle",
          onClick --> { _ => VisualEditorViewModel.addShape(ShapeType.Rectangle) }
        ),

        button(
          cls := "add-element-btn add-line-btn",
          "— Line",
          title := "Add line",
          onClick --> { _ => VisualEditorViewModel.addShape(ShapeType.Line) }
        ),

        button(
          cls := "add-element-btn add-clipart-btn",
          "🎨 Clipart",
          title := "Add clipart (gallery coming soon)",
          onClick --> { _ =>
            VisualEditorViewModel.addClipart("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='100'%3E%3Ccircle cx='50' cy='50' r='45' fill='%23667eea' stroke='%23764ba2' stroke-width='3'/%3E%3Ctext x='50' y='58' text-anchor='middle' fill='white' font-size='14'%3EClipart%3C/text%3E%3C/svg%3E")
          }
        ),
      ),

      // ─── Element list ──────────────────────────────────────────
      div(
        cls := "elements-list",
        children <-- currentPage.combineWith(selectedElement).map { (page, selected) =>
          if page.elements.isEmpty then
            List(div(cls := "empty-elements", "No elements on this page"))
          else
            page.elements.sortBy(_.zIndex).reverse.zipWithIndex.map { case (elem, _) =>
              renderElementItem(elem, selected)
            }
        }
      ),

    )
  }

  // ─── Element item in list ──────────────────────────────────────

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
            title := "Move up one layer",
            "↑",
            onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.bringToFront(elem.id) }
          ),
          button(
            cls := "element-action-btn",
            title := "Move down one layer",
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
