package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*
import org.scalajs.dom
import org.scalajs.dom.FileReader

/** Unified element list and editor for all canvas element types */
object ElementListEditor {
  def apply(): Element = {
    val currentPage = CalendarViewModel.currentPage
    val selectedElement = CalendarViewModel.selectedElement

    div(
      cls := "element-editor-section",

      h4("Page Elements"),

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
                CalendarViewModel.uploadPhoto(imageData)
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
          onClick --> { _ => CalendarViewModel.addTextField() }
        ),

        button(
          cls := "add-element-btn add-rect-btn",
          "▭ Rect",
          title := "Add rectangle",
          onClick --> { _ => CalendarViewModel.addShape(ShapeType.Rectangle) }
        ),

        button(
          cls := "add-element-btn add-line-btn",
          "— Line",
          title := "Add line",
          onClick --> { _ => CalendarViewModel.addShape(ShapeType.Line) }
        ),

        button(
          cls := "add-element-btn add-clipart-btn",
          "🎨 Clipart",
          title := "Add clipart (gallery coming soon)",
          onClick --> { _ =>
            // Placeholder — in future, open gallery. For now use a simple placeholder SVG.
            CalendarViewModel.addClipart("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='100'%3E%3Ccircle cx='50' cy='50' r='45' fill='%23667eea' stroke='%23764ba2' stroke-width='3'/%3E%3Ctext x='50' y='58' text-anchor='middle' fill='white' font-size='14'%3EClipart%3C/text%3E%3C/svg%3E")
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

      // ─── Selected element form editor ──────────────────────────
      child.maybe <-- selectedElement.combineWith(currentPage).map { (selected, page) =>
        selected.flatMap { selId =>
          page.elements.find(_.id == selId).map(renderElementForm)
        }
      }
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

        // Action buttons
        div(
          cls := "element-actions",

          button(
            cls := "element-action-btn",
            title := "Bring to front",
            "↑",
            onClick --> { ev => ev.stopPropagation(); CalendarViewModel.bringToFront(elem.id) }
          ),
          button(
            cls := "element-action-btn",
            title := "Send to back",
            "↓",
            onClick --> { ev => ev.stopPropagation(); CalendarViewModel.sendToBack(elem.id) }
          ),
          button(
            cls := "element-action-btn",
            title := "Duplicate",
            "⎘",
            onClick --> { ev => ev.stopPropagation(); CalendarViewModel.duplicateElement(elem.id) }
          ),
          button(
            cls := "element-action-btn element-delete-btn",
            title := "Delete",
            "×",
            onClick --> { ev => ev.stopPropagation(); CalendarViewModel.removeElement(elem.id) }
          ),
        )
      ),

      onClick --> { _ => CalendarViewModel.selectElement(elem.id) }
    )
  }

  // ─── Element form dispatcher ───────────────────────────────────

  private def renderElementForm(elem: CanvasElement): Element = elem match {
    case photo: PhotoElement   => renderPhotoForm(photo)
    case text: TextElement     => renderTextForm(text)
    case shape: ShapeElement   => renderShapeForm(shape)
    case clip: ClipartElement  => renderClipartForm(clip)
  }

  // ─── Photo form ────────────────────────────────────────────────

  private def renderPhotoForm(photo: PhotoElement): Element = {
    div(
      cls := "element-form",

      h5("Photo Controls"),

      renderPositionControls(photo.id, photo.position),
      renderSizeControls(photo.id, photo.size),
      renderRotationControl(photo.id, photo.rotation),

      button(
        cls := "done-editing-btn",
        "Done",
        onClick --> { _ => CalendarViewModel.deselectElement() }
      )
    )
  }

  // ─── Text form ─────────────────────────────────────────────────

  private def renderTextForm(text: TextElement): Element = {
    div(
      cls := "element-form",

      h5("Text Controls"),

      // Text content
      div(
        cls := "control-group",
        label("Text:"),
        input(
          typ := "text",
          value := text.text,
          cls := "text-input",
          onInput.mapToValue --> { v =>
            CalendarViewModel.updateTextFieldText(text.id, v)
          }
        )
      ),

      // Font family
      div(
        cls := "control-group",
        label("Font:"),
        select(
          cls := "font-select",
          value := text.fontFamily,
          List("Arial", "Helvetica", "Times New Roman", "Georgia", "Courier New", "Verdana", "Impact", "Comic Sans MS").map { f =>
            option(value := f, f, selected := (f == text.fontFamily))
          },
          onChange.mapToValue --> { v =>
            CalendarViewModel.updateTextFieldFontFamily(text.id, v)
          }
        )
      ),

      // Font size
      div(
        cls := "control-group",
        label("Font Size:"),
        input(
          typ := "number",
          value := text.fontSize.toString,
          minAttr := "8",
          maxAttr := "72",
          stepAttr := "1",
          cls := "font-size-input",
          onInput.mapToValue --> { v =>
            v.toIntOption.foreach(sz => CalendarViewModel.updateTextFieldFontSize(text.id, sz))
          }
        )
      ),

      // Bold / Italic / Alignment row
      div(
        cls := "control-group text-format-row",
        button(
          cls := "format-btn",
          cls := "active" -> text.bold,
          "B",
          title := "Bold",
          onClick --> { _ => CalendarViewModel.updateTextFieldBold(text.id, !text.bold) }
        ),
        button(
          cls := "format-btn format-italic",
          cls := "active" -> text.italic,
          "I",
          title := "Italic",
          onClick --> { _ => CalendarViewModel.updateTextFieldItalic(text.id, !text.italic) }
        ),
        span(cls := "format-separator"),
        button(
          cls := "format-btn",
          cls := "active" -> (text.textAlign == TextAlignment.Left),
          "≡←",
          title := "Align left",
          onClick --> { _ => CalendarViewModel.updateTextFieldAlign(text.id, TextAlignment.Left) }
        ),
        button(
          cls := "format-btn",
          cls := "active" -> (text.textAlign == TextAlignment.Center),
          "≡",
          title := "Align center",
          onClick --> { _ => CalendarViewModel.updateTextFieldAlign(text.id, TextAlignment.Center) }
        ),
        button(
          cls := "format-btn",
          cls := "active" -> (text.textAlign == TextAlignment.Right),
          "≡→",
          title := "Align right",
          onClick --> { _ => CalendarViewModel.updateTextFieldAlign(text.id, TextAlignment.Right) }
        ),
      ),

      // Color
      div(
        cls := "control-group",
        label("Color:"),
        input(
          typ := "color",
          value := text.color,
          cls := "color-input",
          onInput.mapToValue --> { v =>
            CalendarViewModel.updateTextFieldColor(text.id, v)
          }
        )
      ),

      renderPositionControls(text.id, text.position),
      renderSizeControls(text.id, text.size),
      renderRotationControl(text.id, text.rotation),

      button(
        cls := "done-editing-btn",
        "Done",
        onClick --> { _ => CalendarViewModel.deselectElement() }
      )
    )
  }

  // ─── Shape form ────────────────────────────────────────────────

  private def renderShapeForm(shape: ShapeElement): Element = {
    div(
      cls := "element-form",

      h5(s"${shape.shapeType} Controls"),

      // Stroke color
      div(
        cls := "control-group",
        label("Stroke Color:"),
        input(
          typ := "color",
          value := shape.strokeColor,
          cls := "color-input",
          onInput.mapToValue --> { v =>
            CalendarViewModel.updateShape(shape.id, _.copy(strokeColor = v))
          }
        )
      ),

      // Fill color (for rectangle)
      if shape.shapeType == ShapeType.Rectangle then
        div(
          cls := "control-group",
          label("Fill Color:"),
          input(
            typ := "color",
            value := (if shape.fillColor == "transparent" then "#ffffff" else shape.fillColor),
            cls := "color-input",
            onInput.mapToValue --> { v =>
              CalendarViewModel.updateShape(shape.id, _.copy(fillColor = v))
            }
          )
        )
      else emptyMod,

      // Stroke width
      div(
        cls := "control-group",
        label("Stroke Width:"),
        input(
          typ := "number",
          value := shape.strokeWidth.toString,
          minAttr := "1",
          maxAttr := "20",
          stepAttr := "1",
          cls := "stroke-input",
          onInput.mapToValue --> { v =>
            v.toDoubleOption.foreach(sw => CalendarViewModel.updateShape(shape.id, _.copy(strokeWidth = sw)))
          }
        )
      ),

      renderPositionControls(shape.id, shape.position),
      renderSizeControls(shape.id, shape.size),
      renderRotationControl(shape.id, shape.rotation),

      button(
        cls := "done-editing-btn",
        "Done",
        onClick --> { _ => CalendarViewModel.deselectElement() }
      )
    )
  }

  // ─── Clipart form ──────────────────────────────────────────────

  private def renderClipartForm(clip: ClipartElement): Element = {
    div(
      cls := "element-form",

      h5("Clipart Controls"),
      p(cls := "info-hint", "Clipart gallery coming soon. Replace image via upload."),

      renderPositionControls(clip.id, clip.position),
      renderSizeControls(clip.id, clip.size),
      renderRotationControl(clip.id, clip.rotation),

      button(
        cls := "done-editing-btn",
        "Done",
        onClick --> { _ => CalendarViewModel.deselectElement() }
      )
    )
  }

  // ─── Shared form helpers ───────────────────────────────────────

  private def renderPositionControls(elementId: String, pos: Position): Element = {
    div(
      cls := "control-row",
      div(
        cls := "control-group half",
        label("X:"),
        input(
          typ := "number",
          value := pos.x.toInt.toString,
          stepAttr := "5",
          cls := "position-input",
          onInput.mapToValue --> { v =>
            v.toDoubleOption.foreach(x => CalendarViewModel.updateElementPosition(elementId, Position(x, pos.y)))
          }
        )
      ),
      div(
        cls := "control-group half",
        label("Y:"),
        input(
          typ := "number",
          value := pos.y.toInt.toString,
          stepAttr := "5",
          cls := "position-input",
          onInput.mapToValue --> { v =>
            v.toDoubleOption.foreach(y => CalendarViewModel.updateElementPosition(elementId, Position(pos.x, y)))
          }
        )
      )
    )
  }

  private def renderSizeControls(elementId: String, sz: Size): Element = {
    div(
      cls := "control-row",
      div(
        cls := "control-group half",
        label("W:"),
        input(
          typ := "number",
          value := sz.width.toInt.toString,
          minAttr := "10",
          stepAttr := "10",
          cls := "size-input",
          onInput.mapToValue --> { v =>
            v.toDoubleOption.foreach(w => CalendarViewModel.updateElementSize(elementId, Size(w, sz.height)))
          }
        )
      ),
      div(
        cls := "control-group half",
        label("H:"),
        input(
          typ := "number",
          value := sz.height.toInt.toString,
          minAttr := "10",
          stepAttr := "10",
          cls := "size-input",
          onInput.mapToValue --> { v =>
            v.toDoubleOption.foreach(h => CalendarViewModel.updateElementSize(elementId, Size(sz.width, h)))
          }
        )
      )
    )
  }

  private def renderRotationControl(elementId: String, rotation: Double): Element = {
    div(
      cls := "control-group",
      label("Rotation:"),
      div(
        cls := "rotation-row",
        input(
          typ := "range",
          value := rotation.toString,
          minAttr := "0",
          maxAttr := "360",
          stepAttr := "1",
          cls := "rotation-slider",
          onInput.mapToValue --> { v =>
            v.toDoubleOption.foreach(r => CalendarViewModel.updateElementRotation(elementId, r))
          }
        ),
        span(cls := "rotation-value", s"${rotation.toInt}°"),
      )
    )
  }
}
