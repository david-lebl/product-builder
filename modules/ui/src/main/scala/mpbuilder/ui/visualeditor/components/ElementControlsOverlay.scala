package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.*
import org.scalajs.dom
import scala.scalajs.js

/** Selection overlay with resize handles, rotation handle, drag handle, and element toolbar */
object ElementControlsOverlay {

  def render(elem: CanvasElement): Element = {
    div(
      cls := "selection-overlay",
      styleAttr := s"position: absolute; left: ${elem.position.x}px; top: ${elem.position.y}px; width: ${elem.size.width}px; height: ${elem.size.height}px; transform: rotate(${elem.rotation}deg); transform-origin: center; z-index: 10000; pointer-events: none; border: 2px solid var(--color-primary);",

      // Resize handles
      renderResizeHandles(elem.id, elem),

      // Rotation handle
      renderRotationHandle(elem.id, elem),

      // Drag handle
      renderDragHandle(elem.id, elem.position),

      // Element toolbar
      renderElementToolbar(elem),
    )
  }

  // ─── Resize handles ──────────────────────────────────────────────

  private def renderResizeHandles(elementId: String, elem: CanvasElement): Element = {
    div(
      cls := "resize-handles",

      List("nw", "ne", "sw", "se").map { corner =>
        div(
          cls := s"resize-handle resize-handle-$corner",
          onMouseDown --> { ev =>
            ev.preventDefault()
            ev.stopPropagation()

            val startX = ev.clientX
            val startY = ev.clientY
            val startWidth = elem.size.width
            val startHeight = elem.size.height
            val startPosX = elem.position.x
            val startPosY = elem.position.y
            val aspectRatio = if startHeight != 0 then startWidth / startHeight else 1.0

            var mouseUpHandlerOpt: Option[js.Function1[dom.MouseEvent, Unit]] = None

            val mouseMoveHandler: js.Function1[dom.MouseEvent, Unit] = { moveEv =>
              val deltaX = moveEv.clientX - startX
              val deltaY = moveEv.clientY - startY

              corner match {
                case "se" =>
                  val newWidth = math.max(30, startWidth + deltaX)
                  val newHeight = newWidth / aspectRatio
                  VisualEditorViewModel.updateElementSize(elementId, Size(newWidth, newHeight))

                case "sw" =>
                  val newWidth = math.max(30, startWidth - deltaX)
                  val newHeight = newWidth / aspectRatio
                  VisualEditorViewModel.updateElementSize(elementId, Size(newWidth, newHeight))
                  VisualEditorViewModel.updateElementPosition(elementId, Position(startPosX + (startWidth - newWidth), startPosY))

                case "ne" =>
                  val newWidth = math.max(30, startWidth + deltaX)
                  val newHeight = newWidth / aspectRatio
                  VisualEditorViewModel.updateElementSize(elementId, Size(newWidth, newHeight))
                  VisualEditorViewModel.updateElementPosition(elementId, Position(startPosX, startPosY + (startHeight - newHeight)))

                case "nw" =>
                  val newWidth = math.max(30, startWidth - deltaX)
                  val newHeight = newWidth / aspectRatio
                  VisualEditorViewModel.updateElementSize(elementId, Size(newWidth, newHeight))
                  VisualEditorViewModel.updateElementPosition(elementId, Position(startPosX + (startWidth - newWidth), startPosY + (startHeight - newHeight)))

                case _ => ()
              }
            }

            val mouseUpHandler: js.Function1[dom.MouseEvent, Unit] = { _ =>
              dom.window.removeEventListener("mousemove", mouseMoveHandler)
              mouseUpHandlerOpt.foreach(h => dom.window.removeEventListener("mouseup", h))
              VisualEditorViewModel.commitElementChange()
            }

            mouseUpHandlerOpt = Some(mouseUpHandler)
            dom.window.addEventListener("mousemove", mouseMoveHandler)
            dom.window.addEventListener("mouseup", mouseUpHandler)
          }
        )
      },

      List("n", "s", "e", "w").map { side =>
        div(
          cls := s"resize-handle resize-handle-$side",
          onMouseDown --> { ev =>
            ev.preventDefault()
            ev.stopPropagation()

            val startX = ev.clientX
            val startY = ev.clientY
            val startWidth = elem.size.width
            val startHeight = elem.size.height
            val startPosX = elem.position.x
            val startPosY = elem.position.y

            var mouseUpHandlerOpt: Option[js.Function1[dom.MouseEvent, Unit]] = None

            val mouseMoveHandler: js.Function1[dom.MouseEvent, Unit] = { moveEv =>
              val deltaX = moveEv.clientX - startX
              val deltaY = moveEv.clientY - startY

              side match {
                case "e" =>
                  VisualEditorViewModel.updateElementSize(elementId, Size(math.max(30, startWidth + deltaX), startHeight))
                case "w" =>
                  val newWidth = math.max(30, startWidth - deltaX)
                  VisualEditorViewModel.updateElementSize(elementId, Size(newWidth, startHeight))
                  VisualEditorViewModel.updateElementPosition(elementId, Position(startPosX + (startWidth - newWidth), startPosY))
                case "s" =>
                  VisualEditorViewModel.updateElementSize(elementId, Size(startWidth, math.max(30, startHeight + deltaY)))
                case "n" =>
                  val newHeight = math.max(30, startHeight - deltaY)
                  VisualEditorViewModel.updateElementSize(elementId, Size(startWidth, newHeight))
                  VisualEditorViewModel.updateElementPosition(elementId, Position(startPosX, startPosY + (startHeight - newHeight)))
                case _ => ()
              }
            }

            val mouseUpHandler: js.Function1[dom.MouseEvent, Unit] = { _ =>
              dom.window.removeEventListener("mousemove", mouseMoveHandler)
              mouseUpHandlerOpt.foreach(h => dom.window.removeEventListener("mouseup", h))
              VisualEditorViewModel.commitElementChange()
            }

            mouseUpHandlerOpt = Some(mouseUpHandler)
            dom.window.addEventListener("mousemove", mouseMoveHandler)
            dom.window.addEventListener("mouseup", mouseUpHandler)
          }
        )
      }
    )
  }

  // ─── Rotation handle (drag-to-rotate) ────────────────────────────

  private def renderRotationHandle(elementId: String, elem: CanvasElement): Element = {
    div(
      cls := "rotate-handle-container",
      // Connecting line from element to handle
      div(cls := "rotate-handle-line"),
      // Draggable rotation handle
      div(
        cls := "rotate-handle",
        "⟳",
        title := "Drag to rotate",
        onMouseDown --> { ev =>
          ev.preventDefault()
          ev.stopPropagation()

          // Get the element's center position for angle calculation
          val target = ev.target.asInstanceOf[dom.Element]
          val parent = target.closest(".selection-overlay")
          if parent != null then
            val rect = parent.getBoundingClientRect()
            val centerX = rect.left + rect.width / 2
            val centerY = rect.top + rect.height / 2
            val startAngle = math.atan2(ev.clientY - centerY, ev.clientX - centerX) * 180 / math.Pi
            val startRotation = elem.rotation

            var mouseUpHandlerOpt: Option[js.Function1[dom.MouseEvent, Unit]] = None

            val mouseMoveHandler: js.Function1[dom.MouseEvent, Unit] = { moveEv =>
              val currentAngle = math.atan2(moveEv.clientY - centerY, moveEv.clientX - centerX) * 180 / math.Pi
              val delta = currentAngle - startAngle
              val newRotation = ((startRotation + delta) % 360 + 360) % 360
              VisualEditorViewModel.updateElementRotation(elementId, newRotation)
            }

            val mouseUpHandler: js.Function1[dom.MouseEvent, Unit] = { _ =>
              dom.window.removeEventListener("mousemove", mouseMoveHandler)
              mouseUpHandlerOpt.foreach(h => dom.window.removeEventListener("mouseup", h))
              VisualEditorViewModel.commitElementChange()
            }

            mouseUpHandlerOpt = Some(mouseUpHandler)
            dom.window.addEventListener("mousemove", mouseMoveHandler)
            dom.window.addEventListener("mouseup", mouseUpHandler)
        }
      )
    )
  }

  // ─── Drag handle ─────────────────────────────────────────────────

  private def renderDragHandle(elementId: String, startPos: Position): Element = {
    div(
      cls := "drag-handle",
      "✥",
      title := "Drag to move",
      onMouseDown --> { ev =>
        ev.preventDefault()
        ev.stopPropagation()
        EditorPageCanvas.startDrag(elementId, startPos, ev)
      }
    )
  }

  // ─── Element toolbar ─────────────────────────────────────────────

  private def renderElementToolbar(elem: CanvasElement): Element = {
    val toolbarContent = elem match {
      case text: TextElement     => renderTextToolbar(text)
      case photo: PhotoElement   => renderPhotoToolbar(photo)
      case shape: ShapeElement   => renderShapeToolbar(shape)
      case clip: ClipartElement  => renderClipartToolbar(clip)
    }

    // Counter-rotate the toolbar so it stays horizontal regardless of element rotation
    div(
      cls := "element-toolbar",
      styleAttr := s"transform: translateX(-50%) rotate(${-elem.rotation}deg);",
      toolbarContent,
      renderLayerActions(elem.id),
    )
  }

  private def renderTextToolbar(text: TextElement): Element = {
    div(
      cls := "toolbar-group",
      // Font selector
      select(
        cls := "toolbar-font-select",
        List("Arial", "Helvetica", "Times New Roman", "Georgia", "Courier New", "Verdana", "Impact", "Comic Sans MS").map { f =>
          option(value := f, selected := (f == text.fontFamily), f)
        },
        onChange.mapToValue --> { v =>
          VisualEditorViewModel.updateTextFieldFontFamily(text.id, v)
        },
        onMouseDown --> { ev => ev.stopPropagation() },
      ),
      span(cls := "toolbar-separator"),
      button(cls := "toolbar-btn" + (if text.bold then " active" else ""), "B", title := "Bold",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updateTextFieldBold(text.id, !text.bold) }),
      button(cls := "toolbar-btn toolbar-italic" + (if text.italic then " active" else ""), "I", title := "Italic",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updateTextFieldItalic(text.id, !text.italic) }),
      span(cls := "toolbar-separator"),
      button(cls := "toolbar-btn" + (if text.textAlign == TextAlignment.Left then " active" else ""), "⫷", title := "Align left",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updateTextFieldAlign(text.id, TextAlignment.Left) }),
      button(cls := "toolbar-btn" + (if text.textAlign == TextAlignment.Center then " active" else ""), "☰", title := "Align center",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updateTextFieldAlign(text.id, TextAlignment.Center) }),
      button(cls := "toolbar-btn" + (if text.textAlign == TextAlignment.Right then " active" else ""), "⫸", title := "Align right",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updateTextFieldAlign(text.id, TextAlignment.Right) }),
      span(cls := "toolbar-separator"),
      input(typ := "color", cls := "toolbar-color", value := text.color,
        onInput.mapToValue --> { v => VisualEditorViewModel.updateTextFieldColor(text.id, v) }),
      div(
        cls := "toolbar-font-stepper",
        button(cls := "toolbar-btn", "−", title := "Decrease font size",
          onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updateTextFieldFontSize(text.id, math.max(8, text.fontSize - 1)) }),
        span(cls := "toolbar-font-value", s"${text.fontSize}"),
        button(cls := "toolbar-btn", "+", title := "Increase font size",
          onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updateTextFieldFontSize(text.id, math.min(72, text.fontSize + 1)) }),
      ),
    )
  }

  private def renderPhotoToolbar(photo: PhotoElement): Element = {
    div(
      cls := "toolbar-group",

      // Hidden file input for replace
      input(
        typ := "file",
        accept := "image/*",
        idAttr := s"toolbar-replace-${photo.id}",
        display := "none",
        onChange --> { ev =>
          val inp = ev.target.asInstanceOf[dom.html.Input]
          val files = inp.files
          if files.length > 0 then
            val file = files(0)
            val reader = new dom.FileReader()
            reader.onload = { _ =>
              val imageData = reader.result.asInstanceOf[String]
              VisualEditorViewModel.replacePhotoImage(photo.id, imageData)
            }
            reader.readAsDataURL(file)
        }
      ),

      button(cls := "toolbar-btn", "📷", title := "Replace image",
        onClick --> { ev => ev.stopPropagation(); dom.document.getElementById(s"toolbar-replace-${photo.id}").asInstanceOf[dom.html.Input].click() }),
      button(cls := "toolbar-btn", "✕", title := "Clear image",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.clearPhotoImage(photo.id) }),
      span(cls := "toolbar-separator"),
      button(cls := "toolbar-btn", "🔍-", title := "Zoom out",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updatePhotoImageScale(photo.id, math.max(0.1, photo.imageScale - 0.1)) }),
      button(cls := "toolbar-btn", "🔍+", title := "Zoom in",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updatePhotoImageScale(photo.id, math.min(3.0, photo.imageScale + 0.1)) }),
      span(cls := "toolbar-separator"),
      button(cls := "toolbar-btn", "↔", title := "Mirror offset",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.updatePhotoImageOffset(photo.id, -photo.imageOffsetX, photo.imageOffsetY) }),
    )
  }

  private def renderShapeToolbar(shape: ShapeElement): Element = {
    div(
      cls := "toolbar-group",
      input(typ := "color", cls := "toolbar-color", value := shape.strokeColor,
        onInput.mapToValue --> { v => VisualEditorViewModel.updateShape(shape.id, _.copy(strokeColor = v)) }),
    )
  }

  private def renderClipartToolbar(clip: ClipartElement): Element = {
    div(cls := "toolbar-group") // Layer actions only
  }

  private def renderLayerActions(elementId: String): Element = {
    div(
      cls := "toolbar-group toolbar-layer-actions",
      button(cls := "toolbar-btn", "↑", title := "Move up one layer",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.bringToFront(elementId) }),
      button(cls := "toolbar-btn", "↓", title := "Move down one layer",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.sendToBack(elementId) }),
      button(cls := "toolbar-btn", "⎘", title := "Duplicate",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.duplicateElement(elementId) }),
      button(cls := "toolbar-btn toolbar-delete-btn", "🗑", title := "Delete",
        onClick --> { ev => ev.stopPropagation(); VisualEditorViewModel.removeElement(elementId) }),
    )
  }
}
