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

      // ─── Selected element form editor ──────────────────────────
      child.maybe <-- selectedElement.map { selected =>
        selected.flatMap { selId =>
          VisualEditorViewModel.currentPageSnapshot().elements.find(_.id == selId).map { elem =>
            elem match {
              case _: PhotoElement   => renderPhotoFormReactive(selId)
              case _: TextElement    => renderTextFormReactive(selId)
              case _: ShapeElement   => renderShapeFormReactive(selId)
              case _: ClipartElement => renderClipartFormReactive(selId)
            }
          }
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

  // ─── Photo form (reactive) ─────────────────────────────────────

  private def renderPhotoFormReactive(photoId: String): Element = {
    val photoSignal: Signal[Option[PhotoElement]] = VisualEditorViewModel.currentPage.map(
      _.elements.collectFirst { case p: PhotoElement if p.id == photoId => p }
    )

    div(
      cls := "element-form",

      h5("Photo Controls"),

      div(
        cls := "control-group",
        input(
          typ := "file",
          accept := "image/*",
          idAttr := s"photo-replace-$photoId",
          display := "none",
          onChange --> { ev =>
            val inp = ev.target.asInstanceOf[dom.html.Input]
            val files = inp.files
            if files.length > 0 then
              val file = files(0)
              val reader = new FileReader()
              reader.onload = { _ =>
                val imageData = reader.result.asInstanceOf[String]
                VisualEditorViewModel.replacePhotoImage(photoId, imageData)
              }
              reader.readAsDataURL(file)
          }
        ),
        div(
          cls := "photo-action-buttons",
          button(
            cls := "add-element-btn",
            "📷 Replace Image",
            onClick --> { _ =>
              dom.document.getElementById(s"photo-replace-$photoId").asInstanceOf[dom.html.Input].click()
            }
          ),
          button(
            cls := "add-element-btn element-delete-btn",
            "✕ Clear Image",
            onClick --> { _ => VisualEditorViewModel.clearPhotoImage(photoId) }
          ),
        ),
      ),

      // Zoom control
      div(
        cls := "control-group",
        label("Zoom:"),
        div(
          cls := "zoom-row",
          button(
            cls := "zoom-btn",
            "-",
            title := "Zoom out",
            onClick --> { _ =>
              VisualEditorViewModel.currentPageSnapshot().elements.collectFirst {
                case p: PhotoElement if p.id == photoId => p
              }.foreach { p =>
                VisualEditorViewModel.updatePhotoImageScale(photoId, p.imageScale - 0.1)
              }
            }
          ),
          input(
            typ := "range",
            value <-- photoSignal.map(_.map(_.imageScale.toString).getOrElse("1.0")),
            minAttr := "1",
            maxAttr := "3",
            stepAttr := "0.1",
            cls := "zoom-slider",
            onInput.mapToValue --> { v =>
              v.toDoubleOption.foreach(s => VisualEditorViewModel.updatePhotoImageScale(photoId, s))
            }
          ),
          button(
            cls := "zoom-btn",
            "+",
            title := "Zoom in",
            onClick --> { _ =>
              VisualEditorViewModel.currentPageSnapshot().elements.collectFirst {
                case p: PhotoElement if p.id == photoId => p
              }.foreach { p =>
                VisualEditorViewModel.updatePhotoImageScale(photoId, p.imageScale + 0.1)
              }
            }
          ),
          span(cls := "zoom-value", child.text <-- photoSignal.map(p =>
            s"${(p.map(_.imageScale).getOrElse(1.0) * 100).toInt}%"
          )),
        ),
      ),

      renderPositionControlsReactive(photoId),
      renderSizeControlsReactive(photoId),
      renderRotationControlReactive(photoId),

      button(
        cls := "done-editing-btn",
        "Done",
        onClick --> { _ => VisualEditorViewModel.deselectElement() }
      )
    )
  }

  // ─── Text form (reactive) ──────────────────────────────────────

  private def renderTextFormReactive(textId: String): Element = {
    val textSignal: Signal[Option[TextElement]] = VisualEditorViewModel.currentPage.map(
      _.elements.collectFirst { case t: TextElement if t.id == textId => t }
    )

    div(
      cls := "element-form",

      h5("Text Controls"),

      div(
        cls := "control-group",
        label("Text:"),
        input(
          typ := "text",
          controlled(
            value <-- textSignal.map(_.map(_.text).getOrElse("")),
            onInput.mapToValue --> { v =>
              VisualEditorViewModel.updateTextFieldText(textId, v)
            }
          ),
          cls := "text-input",
        )
      ),

      div(
        cls := "control-group",
        label("Font:"),
        select(
          cls := "font-select",
          value <-- textSignal.map(_.map(_.fontFamily).getOrElse("Arial")),
          List("Arial", "Helvetica", "Times New Roman", "Georgia", "Courier New", "Verdana", "Impact", "Comic Sans MS").map { f =>
            option(value := f, f)
          },
          onChange.mapToValue --> { v =>
            VisualEditorViewModel.updateTextFieldFontFamily(textId, v)
          }
        )
      ),

      div(
        cls := "control-group",
        label("Font Size:"),
        input(
          typ := "number",
          controlled(
            value <-- textSignal.map(_.map(_.fontSize.toString).getOrElse("14")),
            onInput.mapToValue --> { v =>
              v.toIntOption.foreach(sz => VisualEditorViewModel.updateTextFieldFontSize(textId, sz))
            }
          ),
          minAttr := "8",
          maxAttr := "72",
          stepAttr := "1",
          cls := "font-size-input",
        )
      ),

      div(
        cls := "control-group text-format-row",
        button(
          cls := "format-btn",
          cls <-- textSignal.map(_.exists(_.bold)).map(b => if b then "active" else ""),
          "B",
          title := "Bold",
          onClick --> { _ =>
            VisualEditorViewModel.currentPageSnapshot().elements.collectFirst {
              case t: TextElement if t.id == textId => t
            }.foreach(t => VisualEditorViewModel.updateTextFieldBold(textId, !t.bold))
          }
        ),
        button(
          cls := "format-btn format-italic",
          cls <-- textSignal.map(_.exists(_.italic)).map(i => if i then "active" else ""),
          "I",
          title := "Italic",
          onClick --> { _ =>
            VisualEditorViewModel.currentPageSnapshot().elements.collectFirst {
              case t: TextElement if t.id == textId => t
            }.foreach(t => VisualEditorViewModel.updateTextFieldItalic(textId, !t.italic))
          }
        ),
        span(cls := "format-separator"),
        button(
          cls := "format-btn",
          cls <-- textSignal.map(_.exists(_.textAlign == TextAlignment.Left)).map(a => if a then "active" else ""),
          "L",
          title := "Align left",
          onClick --> { _ => VisualEditorViewModel.updateTextFieldAlign(textId, TextAlignment.Left) }
        ),
        button(
          cls := "format-btn",
          cls <-- textSignal.map(_.exists(_.textAlign == TextAlignment.Center)).map(a => if a then "active" else ""),
          "C",
          title := "Align center",
          onClick --> { _ => VisualEditorViewModel.updateTextFieldAlign(textId, TextAlignment.Center) }
        ),
        button(
          cls := "format-btn",
          cls <-- textSignal.map(_.exists(_.textAlign == TextAlignment.Right)).map(a => if a then "active" else ""),
          "R",
          title := "Align right",
          onClick --> { _ => VisualEditorViewModel.updateTextFieldAlign(textId, TextAlignment.Right) }
        ),
      ),

      div(
        cls := "control-group",
        label("Color:"),
        input(
          typ := "color",
          value <-- textSignal.map(_.map(_.color).getOrElse("#000000")),
          cls := "color-input",
          onInput.mapToValue --> { v =>
            VisualEditorViewModel.updateTextFieldColor(textId, v)
          }
        )
      ),

      renderPositionControlsReactive(textId),
      renderSizeControlsReactive(textId),
      renderRotationControlReactive(textId),

      button(
        cls := "done-editing-btn",
        "Done",
        onClick --> { _ => VisualEditorViewModel.deselectElement() }
      )
    )
  }

  // ─── Shape form (reactive) ─────────────────────────────────────

  private def renderShapeFormReactive(shapeId: String): Element = {
    val shapeSignal: Signal[Option[ShapeElement]] = VisualEditorViewModel.currentPage.map(
      _.elements.collectFirst { case s: ShapeElement if s.id == shapeId => s }
    )

    val shapeType = VisualEditorViewModel.currentPageSnapshot().elements.collectFirst {
      case s: ShapeElement if s.id == shapeId => s.shapeType
    }.getOrElse(ShapeType.Rectangle)

    div(
      cls := "element-form",

      h5(s"$shapeType Controls"),

      div(
        cls := "control-group",
        label("Stroke Color:"),
        input(
          typ := "color",
          value <-- shapeSignal.map(_.map(_.strokeColor).getOrElse("#000000")),
          cls := "color-input",
          onInput.mapToValue --> { v =>
            VisualEditorViewModel.updateShape(shapeId, _.copy(strokeColor = v))
          }
        )
      ),

      if shapeType == ShapeType.Rectangle then
        div(
          cls := "control-group",
          label("Fill Color:"),
          input(
            typ := "color",
            value <-- shapeSignal.map(_.map(s => if s.fillColor == "transparent" then "#ffffff" else s.fillColor).getOrElse("#ffffff")),
            cls := "color-input",
            onInput.mapToValue --> { v =>
              VisualEditorViewModel.updateShape(shapeId, _.copy(fillColor = v))
            }
          )
        )
      else emptyMod,

      div(
        cls := "control-group",
        label("Stroke Width:"),
        input(
          typ := "number",
          controlled(
            value <-- shapeSignal.map(_.map(_.strokeWidth.toString).getOrElse("2")),
            onInput.mapToValue --> { v =>
              v.toDoubleOption.foreach(sw => VisualEditorViewModel.updateShape(shapeId, _.copy(strokeWidth = sw)))
            }
          ),
          minAttr := "1",
          maxAttr := "20",
          stepAttr := "1",
          cls := "stroke-input",
        )
      ),

      renderPositionControlsReactive(shapeId),
      renderSizeControlsReactive(shapeId),
      renderRotationControlReactive(shapeId),

      button(
        cls := "done-editing-btn",
        "Done",
        onClick --> { _ => VisualEditorViewModel.deselectElement() }
      )
    )
  }

  // ─── Clipart form (reactive) ───────────────────────────────────

  private def renderClipartFormReactive(clipId: String): Element = {
    div(
      cls := "element-form",

      h5("Clipart Controls"),
      p(cls := "info-hint", "Clipart gallery coming soon. Replace image via upload."),

      renderPositionControlsReactive(clipId),
      renderSizeControlsReactive(clipId),
      renderRotationControlReactive(clipId),

      button(
        cls := "done-editing-btn",
        "Done",
        onClick --> { _ => VisualEditorViewModel.deselectElement() }
      )
    )
  }

  // ─── Shared reactive form helpers ──────────────────────────────

  private def renderPositionControlsReactive(elementId: String): Element = {
    val elemSignal = VisualEditorViewModel.currentPage.map(_.elements.find(_.id == elementId))
    div(
      cls := "control-row",
      div(
        cls := "control-group half",
        label("X:"),
        input(
          typ := "number",
          controlled(
            value <-- elemSignal.map(_.map(_.position.x.toInt.toString).getOrElse("0")),
            onInput.mapToValue --> { v =>
              v.toDoubleOption.foreach(x => VisualEditorViewModel.updateElementPositionX(elementId, x))
            }
          ),
          stepAttr := "5",
          cls := "position-input",
        )
      ),
      div(
        cls := "control-group half",
        label("Y:"),
        input(
          typ := "number",
          controlled(
            value <-- elemSignal.map(_.map(_.position.y.toInt.toString).getOrElse("0")),
            onInput.mapToValue --> { v =>
              v.toDoubleOption.foreach(y => VisualEditorViewModel.updateElementPositionY(elementId, y))
            }
          ),
          stepAttr := "5",
          cls := "position-input",
        )
      )
    )
  }

  private def renderSizeControlsReactive(elementId: String): Element = {
    val elemSignal = VisualEditorViewModel.currentPage.map(_.elements.find(_.id == elementId))
    div(
      cls := "control-row",
      div(
        cls := "control-group half",
        label("W:"),
        input(
          typ := "number",
          controlled(
            value <-- elemSignal.map(_.map(_.size.width.toInt.toString).getOrElse("0")),
            onInput.mapToValue --> { v =>
              v.toDoubleOption.foreach(w => VisualEditorViewModel.updateElementSizeWidth(elementId, w))
            }
          ),
          minAttr := "10",
          stepAttr := "10",
          cls := "size-input",
        )
      ),
      div(
        cls := "control-group half",
        label("H:"),
        input(
          typ := "number",
          controlled(
            value <-- elemSignal.map(_.map(_.size.height.toInt.toString).getOrElse("0")),
            onInput.mapToValue --> { v =>
              v.toDoubleOption.foreach(h => VisualEditorViewModel.updateElementSizeHeight(elementId, h))
            }
          ),
          minAttr := "10",
          stepAttr := "10",
          cls := "size-input",
        )
      )
    )
  }

  private def renderRotationControlReactive(elementId: String): Element = {
    val elemSignal = VisualEditorViewModel.currentPage.map(_.elements.find(_.id == elementId))
    div(
      cls := "control-group",
      label("Rotation:"),
      div(
        cls := "rotation-row",
        input(
          typ := "range",
          value <-- elemSignal.map(_.map(_.rotation.toString).getOrElse("0")),
          minAttr := "0",
          maxAttr := "360",
          stepAttr := "1",
          cls := "rotation-slider",
          onInput.mapToValue --> { v =>
            v.toDoubleOption.foreach(r => VisualEditorViewModel.updateElementRotation(elementId, r))
          }
        ),
        span(cls := "rotation-value", child.text <-- elemSignal.map(e =>
          s"${e.map(_.rotation.toInt).getOrElse(0)}deg"
        )),
      )
    )
  }
}
