package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.*
import org.scalajs.dom
import org.scalajs.dom.FileReader

/** Floating horizontal control strip rendered just under the currently selected
  * element on the canvas. Replaces the old sidebar element form. Each element
  * type renders its own set of compact controls; layer actions are common to all.
  *
  * The popup is mounted as a child of `.calendar-page` so its `position: absolute`
  * coordinates align with the canvas's internal coordinate space.
  */
object ElementControlsPopup {

  // Per-photo file inputs are mounted lazily by id; we just trigger them by id.
  private def replaceInputId(photoId: String): String = s"popup-photo-replace-$photoId"

  def apply(): Element = {
    val selectedSignal = VisualEditorViewModel.selectedElement
      .combineWith(VisualEditorViewModel.currentPage)
      .map { (selOpt, page) =>
        selOpt.flatMap(id => page.elements.find(_.id == id))
      }

    div(
      // The popup wrapper exists only when an element is selected
      child.maybe <-- selectedSignal.map(_.map(renderPopupFor))
    )
  }

  // ─── Position computation ────────────────────────────────────────

  private def positionStyle(elem: CanvasElement): String = {
    // Place under the element, centered horizontally on its bbox
    val popupY = elem.position.y + elem.size.height + 12
    val popupX = elem.position.x + elem.size.width / 2
    // The popup uses translateX(-50%) so its center matches popupX. The popup
    // doesn't rotate with the element — keeping it screen-aligned.
    s"position: absolute; left: ${popupX}px; top: ${popupY}px; transform: translateX(-50%); z-index: 200;"
  }

  // ─── Top-level dispatch ──────────────────────────────────────────

  private def renderPopupFor(elem: CanvasElement): Element = {
    val controls: Element = elem match {
      case p: PhotoElement   => renderPhotoControls(p)
      case t: TextElement    => renderTextControls(t)
      case s: ShapeElement   => renderShapeControls(s)
      case c: ClipartElement => renderClipartControls(c)
    }

    div(
      cls := "element-controls-popup",
      styleAttr := positionStyle(elem),
      // Stop click propagation so deselect-on-canvas-click doesn't fire
      onClick --> { ev => ev.stopPropagation() },
      onMouseDown --> { ev => ev.stopPropagation() },
      controls,
      popupSeparator,
      renderLayerActions(elem.id),
    )
  }

  // ─── Photo controls ──────────────────────────────────────────────

  private def renderPhotoControls(photo: PhotoElement): Element = {
    div(
      cls := "popup-controls-row",

      // Hidden file input for replacing the photo
      input(
        typ := "file",
        accept := "image/*",
        idAttr := replaceInputId(photo.id),
        display := "none",
        onChange --> { ev =>
          val inp = ev.target.asInstanceOf[dom.html.Input]
          if inp.files.length > 0 then
            val file = inp.files(0)
            val reader = new FileReader()
            reader.onload = { _ =>
              val data = reader.result.asInstanceOf[String]
              VisualEditorViewModel.replacePhotoImage(photo.id, data)
            }
            reader.readAsDataURL(file)
          inp.value = ""
        }
      ),

      iconButton("📷", "Replace image", () => {
        dom.document.getElementById(replaceInputId(photo.id)).asInstanceOf[dom.html.Input].click()
      }),
      iconButton("✕", "Clear image", () => VisualEditorViewModel.clearPhotoImage(photo.id), variant = "danger"),

      popupSeparator,

      // Zoom slider
      sliderControl(
        "Zoom",
        photo.imageScale,
        min = 1.0, max = 3.0, step = 0.05,
        onChange = v => VisualEditorViewModel.updatePhotoImageScale(photo.id, v),
      ),

      popupSeparator,

      iconButton("⟳", "Rotate image 90°", () => VisualEditorViewModel.rotatePhotoImage90(photo.id)),
      iconButton("⇆", "Flip horizontal", () => VisualEditorViewModel.togglePhotoFlipH(photo.id)),
      iconButton("⇅", "Flip vertical", () => VisualEditorViewModel.togglePhotoFlipV(photo.id)),

      popupSeparator,

      // Opacity slider
      sliderControl(
        "Opacity",
        photo.opacity,
        min = 0.0, max = 1.0, step = 0.05,
        onChange = v => VisualEditorViewModel.updatePhotoOpacity(photo.id, v),
      ),
    )
  }

  // ─── Text controls ───────────────────────────────────────────────

  private def renderTextControls(text: TextElement): Element = {
    div(
      cls := "popup-controls-row",

      // Font family
      select(
        cls := "popup-select",
        value := text.fontFamily,
        title := "Font family",
        List("Arial", "Helvetica", "Times New Roman", "Georgia", "Courier New", "Verdana", "Impact", "Comic Sans MS").map { f =>
          option(value := f, selected := (f == text.fontFamily), f)
        },
        onChange.mapToValue --> { v => VisualEditorViewModel.updateTextFieldFontFamily(text.id, v) },
      ),

      // Font size
      input(
        cls := "popup-number",
        typ := "number",
        title := "Font size",
        minAttr := "8",
        maxAttr := "120",
        stepAttr := "1",
        value := text.fontSize.toString,
        onInput.mapToValue --> { v =>
          v.toIntOption.foreach(sz => VisualEditorViewModel.updateTextFieldFontSize(text.id, sz))
        },
      ),

      popupSeparator,

      // Bold / Italic
      toggleButton("B", "Bold", text.bold, () =>
        VisualEditorViewModel.updateTextFieldBold(text.id, !text.bold),
        extraCls = "popup-bold"),
      toggleButton("I", "Italic", text.italic, () =>
        VisualEditorViewModel.updateTextFieldItalic(text.id, !text.italic),
        extraCls = "popup-italic"),

      popupSeparator,

      // Alignment
      toggleButton("L", "Align left", text.textAlign == TextAlignment.Left, () =>
        VisualEditorViewModel.updateTextFieldAlign(text.id, TextAlignment.Left)),
      toggleButton("C", "Align center", text.textAlign == TextAlignment.Center, () =>
        VisualEditorViewModel.updateTextFieldAlign(text.id, TextAlignment.Center)),
      toggleButton("R", "Align right", text.textAlign == TextAlignment.Right, () =>
        VisualEditorViewModel.updateTextFieldAlign(text.id, TextAlignment.Right)),

      popupSeparator,

      // Color picker
      input(
        cls := "popup-color",
        typ := "color",
        title := "Text color",
        value := text.color,
        onInput.mapToValue --> { v => VisualEditorViewModel.updateTextFieldColor(text.id, v) },
      ),
    )
  }

  // ─── Shape controls ──────────────────────────────────────────────

  private def renderShapeControls(shape: ShapeElement): Element = {
    div(
      cls := "popup-controls-row",

      // Stroke color
      input(
        cls := "popup-color",
        typ := "color",
        title := "Stroke color",
        value := shape.strokeColor,
        onInput.mapToValue --> { v =>
          VisualEditorViewModel.updateShape(shape.id, _.copy(strokeColor = v))
        },
      ),

      // Fill color (rectangles only)
      if shape.shapeType == ShapeType.Rectangle then
        input(
          cls := "popup-color",
          typ := "color",
          title := "Fill color",
          value := (if shape.fillColor == "transparent" then "#ffffff" else shape.fillColor),
          onInput.mapToValue --> { v =>
            VisualEditorViewModel.updateShape(shape.id, _.copy(fillColor = v))
          },
        )
      else emptyMod,

      // Stroke width
      input(
        cls := "popup-number",
        typ := "number",
        title := "Stroke width",
        minAttr := "1",
        maxAttr := "20",
        stepAttr := "1",
        value := shape.strokeWidth.toInt.toString,
        onInput.mapToValue --> { v =>
          v.toDoubleOption.foreach(sw =>
            VisualEditorViewModel.updateShape(shape.id, _.copy(strokeWidth = sw)))
        },
      ),
    )
  }

  // ─── Clipart controls ────────────────────────────────────────────

  private def renderClipartControls(clip: ClipartElement): Element = {
    // Cliparts only get layer actions — no per-element styling.
    div(
      cls := "popup-controls-row popup-controls-empty",
      span(cls := "popup-hint", "Clipart"),
    )
  }

  // ─── Common: layer actions ───────────────────────────────────────

  private def renderLayerActions(elementId: String): Element =
    div(
      cls := "popup-controls-row",
      iconButton("↑", "Bring to front", () => VisualEditorViewModel.bringToFront(elementId)),
      iconButton("↓", "Send to back", () => VisualEditorViewModel.sendToBack(elementId)),
      iconButton("⎘", "Duplicate", () => VisualEditorViewModel.duplicateElement(elementId)),
      iconButton("×", "Delete", () => VisualEditorViewModel.removeElement(elementId), variant = "danger"),
    )

  // ─── Small UI helpers ────────────────────────────────────────────

  private def popupSeparator: Element = span(cls := "popup-separator")

  private def iconButton(label: String, tip: String, action: () => Unit, variant: String = "default"): Element =
    button(
      tpe := "button",
      cls := s"popup-btn popup-btn--$variant",
      title := tip,
      label,
      onClick --> { ev =>
        ev.stopPropagation()
        action()
      },
    )

  private def toggleButton(
    label: String,
    tip: String,
    active: Boolean,
    action: () => Unit,
    extraCls: String = "",
  ): Element =
    button(
      tpe := "button",
      cls := s"popup-btn popup-btn--toggle ${if active then "popup-btn--active" else ""} $extraCls",
      title := tip,
      label,
      onClick --> { ev =>
        ev.stopPropagation()
        action()
      },
    )

  private def sliderControl(
    tip: String,
    current: Double,
    min: Double,
    max: Double,
    step: Double,
    onChange: Double => Unit,
  ): Element =
    div(
      cls := "popup-slider-group",
      title := tip,
      input(
        cls := "popup-slider",
        typ := "range",
        minAttr := min.toString,
        maxAttr := max.toString,
        stepAttr := step.toString,
        value := current.toString,
        onInput.mapToValue --> { v => v.toDoubleOption.foreach(onChange) },
      ),
    )
}
