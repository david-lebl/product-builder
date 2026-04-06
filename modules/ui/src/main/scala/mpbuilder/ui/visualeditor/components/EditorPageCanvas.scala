package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.*
import mpbuilder.ui.visualeditor.overlays.ProductOverlay
import org.scalajs.dom
import scala.scalajs.js

object EditorPageCanvas {
  def apply(): Element = {
    val currentPage = VisualEditorViewModel.currentPage
    val format = VisualEditorViewModel.productFormat

    div(
      cls := "calendar-canvas-container",
      styleAttr <-- format.map { fmt =>
        val maxW = if ProductFormat.isLandscape(fmt) then 800 else 600
        s"max-width: ${maxW}px;"
      },

      div(
        cls := "calendar-canvas",

        // Click on empty area to deselect
        onClick --> { ev =>
          if ev.target.asInstanceOf[dom.Element].classList.contains("calendar-page") ||
             ev.target.asInstanceOf[dom.Element].classList.contains("calendar-background") then
            VisualEditorViewModel.deselectElement()
        },

        // Render the page
        child <-- currentPage.combineWith(format, VisualEditorViewModel.productContext).map { (page, fmt, ctxOpt) =>
          renderPage(page, fmt, ctxOpt)
        }
      )
    )
  }

  private def renderPage(page: EditorPage, format: ProductFormat, productContext: Option[ProductContext]): Element = {
    // Scale to fit canvas: use aspect ratio from physical dimensions
    val aspectRatio = format.widthMm.toDouble / format.heightMm
    val canvasWidth = if ProductFormat.isLandscape(format) then 760 else 560
    val pageHeight = (canvasWidth / aspectRatio).toInt

    div(
      cls := "calendar-page",
      styleAttr := s"height: ${pageHeight}px;",

      // Background
      renderBackground(page.template.background),

      // Product overlay (wire binding, frame, stand, etc.)
      ProductOverlay.render(productContext, format),

      // Month title (locked template field) — only render if text is non-empty
      if page.template.monthField.text.nonEmpty then renderTemplateTextField(page.template.monthField) else emptyMod,

      // Days grid (locked template fields)
      page.template.daysGrid.map(dayField => renderTemplateTextField(dayField)),

      // All canvas elements (sorted by z-index)
      page.elements.sortBy(_.zIndex).map(renderCanvasElement)
    )
  }

  private def renderBackground(bg: PageBackground): Element = bg match {
    case PageBackground.SolidColor(color) =>
      div(
        cls := "calendar-background",
        styleAttr := s"background-color: $color;"
      )
    case PageBackground.BackgroundImage(imageData) =>
      div(
        cls := "calendar-background",
        styleAttr := s"background-image: url($imageData); background-size: cover; background-position: center;"
      )
  }

  private def renderTemplateTextField(field: TemplateTextField): Element = {
    div(
      cls := "calendar-text-field locked",
      styleAttr := s"position: absolute; left: ${field.position.x}px; top: ${field.position.y}px; font-size: ${field.fontSize}px; font-family: ${field.fontFamily}; color: ${field.color}; cursor: default; user-select: none;",
      field.text
    )
  }

  private def renderCanvasElement(elem: CanvasElement): Element = elem match {
    case photo: PhotoElement   => renderPhoto(photo)
    case text: TextElement     => renderTextElement(text)
    case shape: ShapeElement   => renderShapeElement(shape)
    case clip: ClipartElement  => renderClipartElement(clip)
  }

  // ─── Text Element ────────────────────────────────────────────────

  private def renderTextElement(field: TextElement): Element = {
    val selected = VisualEditorViewModel.selectedElement

    div(
      cls := "calendar-element calendar-text-element",
      cls <-- selected.map(sel => if sel.contains(field.id) then "selected" else ""),
      styleAttr := s"position: absolute; left: ${field.position.x}px; top: ${field.position.y}px; width: ${field.size.width}px; height: ${field.size.height}px; transform: rotate(${field.rotation}deg); transform-origin: center; z-index: ${field.zIndex};",

      // Inner text with styling
      div(
        cls := "text-element-content",
        styleAttr := s"font-size: ${field.fontSize}px; font-family: ${field.fontFamily}; color: ${field.color}; font-weight: ${if field.bold then "bold" else "normal"}; font-style: ${if field.italic then "italic" else "normal"}; text-align: ${alignToCss(field.textAlign)}; width: 100%; height: 100%; overflow: hidden; word-wrap: break-word; overflow-wrap: break-word;",
        field.text
      ),

      // Resize handles (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(field.id) then Some(renderResizeHandles(field.id, field)) else None
      },

      // Rotation buttons (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(field.id) then Some(renderRotationButtons(field.id, field.rotation)) else None
      },

      // Drag behavior
      onMouseDown --> { ev =>
        if !ev.target.asInstanceOf[dom.Element].classList.contains("resize-handle") &&
           !ev.target.asInstanceOf[dom.Element].classList.contains("rotate-btn") then
          ev.preventDefault()
          ev.stopPropagation()
          VisualEditorViewModel.selectElement(field.id)
          startDrag(field.id, field.position, ev)
      }
    )
  }

  // ─── Shape Element ───────────────────────────────────────────────

  private def renderShapeElement(shape: ShapeElement): Element = {
    val selected = VisualEditorViewModel.selectedElement

    val shapeContent = shape.shapeType match {
      case ShapeType.Line =>
        div(
          cls := "shape-line-inner",
          styleAttr := s"width: 100%; height: 0; border-top: ${shape.strokeWidth}px solid ${shape.strokeColor}; position: absolute; top: 50%; transform: translateY(-50%);"
        )
      case ShapeType.Rectangle =>
        div(
          cls := "shape-rect-inner",
          styleAttr := s"width: 100%; height: 100%; border: ${shape.strokeWidth}px solid ${shape.strokeColor}; background-color: ${shape.fillColor}; box-sizing: border-box;"
        )
    }

    div(
      cls := "calendar-element calendar-shape-element",
      cls <-- selected.map(sel => if sel.contains(shape.id) then "selected" else ""),
      styleAttr := s"position: absolute; left: ${shape.position.x}px; top: ${shape.position.y}px; width: ${shape.size.width}px; height: ${shape.size.height}px; transform: rotate(${shape.rotation}deg); transform-origin: center; z-index: ${shape.zIndex};",

      shapeContent,

      child.maybe <-- selected.map { sel =>
        if sel.contains(shape.id) then Some(renderResizeHandles(shape.id, shape)) else None
      },

      child.maybe <-- selected.map { sel =>
        if sel.contains(shape.id) then Some(renderRotationButtons(shape.id, shape.rotation)) else None
      },

      onMouseDown --> { ev =>
        if !ev.target.asInstanceOf[dom.Element].classList.contains("resize-handle") &&
           !ev.target.asInstanceOf[dom.Element].classList.contains("rotate-btn") then
          ev.preventDefault()
          ev.stopPropagation()
          VisualEditorViewModel.selectElement(shape.id)
          startDrag(shape.id, shape.position, ev)
      }
    )
  }

  // ─── Clipart Element ─────────────────────────────────────────────

  private def renderClipartElement(clip: ClipartElement): Element = {
    val selected = VisualEditorViewModel.selectedElement

    div(
      cls := "calendar-element calendar-clipart-element",
      cls <-- selected.map(sel => if sel.contains(clip.id) then "selected" else ""),
      styleAttr := s"position: absolute; left: ${clip.position.x}px; top: ${clip.position.y}px; width: ${clip.size.width}px; height: ${clip.size.height}px; transform: rotate(${clip.rotation}deg); transform-origin: center; z-index: ${clip.zIndex};",

      img(
        src := clip.imageData,
        styleAttr := "width: 100%; height: 100%; object-fit: contain; pointer-events: none;",
        draggable := false,
      ),

      child.maybe <-- selected.map { sel =>
        if sel.contains(clip.id) then Some(renderResizeHandles(clip.id, clip)) else None
      },

      child.maybe <-- selected.map { sel =>
        if sel.contains(clip.id) then Some(renderRotationButtons(clip.id, clip.rotation)) else None
      },

      onMouseDown --> { ev =>
        if !ev.target.asInstanceOf[dom.Element].classList.contains("resize-handle") &&
           !ev.target.asInstanceOf[dom.Element].classList.contains("rotate-btn") then
          ev.preventDefault()
          ev.stopPropagation()
          VisualEditorViewModel.selectElement(clip.id)
          startDrag(clip.id, clip.position, ev)
      }
    )
  }

  // ─── Photo Element ───────────────────────────────────────────────

  private def renderPhoto(photo: PhotoElement): Element = {
    val selected = VisualEditorViewModel.selectedElement

    div(
      cls := "calendar-element calendar-photo",
      cls <-- selected.map(sel => if sel.contains(photo.id) then "selected" else ""),
      styleAttr := s"position: absolute; left: ${photo.position.x}px; top: ${photo.position.y}px; width: ${photo.size.width}px; height: ${photo.size.height}px; transform: rotate(${photo.rotation}deg); transform-origin: center; z-index: ${photo.zIndex};",

      div(
        cls := "photo-image-container",
        styleAttr := "width: 100%; height: 100%; overflow: hidden; position: relative;",

        if photo.imageData.nonEmpty then
          img(
            src := photo.imageData,
            styleAttr := s"width: 100%; height: 100%; object-fit: cover; pointer-events: none; transform: scale(${photo.imageScale}) translate(${photo.imageOffsetX}px, ${photo.imageOffsetY}px); transform-origin: center;",
            draggable := false
          )
        else
          div(
            cls := "photo-placeholder",
            styleAttr := "width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f0f0f0; border: 2px dashed #ccc; box-sizing: border-box; color: #999; font-size: 14px; text-align: center; pointer-events: none;",
            "Click to add photo"
          )
      ),

      child.maybe <-- selected.map { sel =>
        if sel.contains(photo.id) then Some(renderResizeHandles(photo.id, photo)) else None
      },

      child.maybe <-- selected.map { sel =>
        if sel.contains(photo.id) then Some(renderRotationButtons(photo.id, photo.rotation)) else None
      },

      onMouseDown --> { ev =>
        val target = ev.target.asInstanceOf[dom.Element]
        if !target.classList.contains("resize-handle") &&
           !target.classList.contains("rotate-btn") then
          ev.preventDefault()
          ev.stopPropagation()
          VisualEditorViewModel.selectElement(photo.id)
          startDrag(photo.id, photo.position, ev)
      }
    )
  }

  // ─── Generic drag helper ─────────────────────────────────────────

  private def startDrag(elementId: String, startPos: Position, ev: dom.MouseEvent): Unit = {
    val startX = ev.clientX
    val startY = ev.clientY

    var mouseUpHandlerOpt: Option[js.Function1[dom.MouseEvent, Unit]] = None

    val mouseMoveHandler: js.Function1[dom.MouseEvent, Unit] = { moveEv =>
      val deltaX = moveEv.clientX - startX
      val deltaY = moveEv.clientY - startY
      VisualEditorViewModel.updateElementPosition(
        elementId,
        Position(startPos.x + deltaX, startPos.y + deltaY)
      )
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

  // ─── Generic resize handles ──────────────────────────────────────

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

  // ─── Rotation buttons ─────────────────────────────────────────

  private def renderRotationButtons(elementId: String, currentRotation: Double): Element = {
    div(
      cls := "rotate-buttons-container",
      div(
        cls := "rotate-btn rotate-btn-left",
        "↺",
        title := "Rotate left 15°",
        onClick --> { ev =>
          ev.preventDefault()
          ev.stopPropagation()
          val newRotation = (currentRotation - 15 + 360) % 360
          VisualEditorViewModel.updateElementRotation(elementId, newRotation)
        }
      ),
      div(
        cls := "rotate-btn rotate-btn-right",
        "↻",
        title := "Rotate right 15°",
        onClick --> { ev =>
          ev.preventDefault()
          ev.stopPropagation()
          val newRotation = (currentRotation + 15) % 360
          VisualEditorViewModel.updateElementRotation(elementId, newRotation)
        }
      )
    )
  }

  private def alignToCss(align: TextAlignment): String = align match {
    case TextAlignment.Left   => "left"
    case TextAlignment.Center => "center"
    case TextAlignment.Right  => "right"
  }
}
