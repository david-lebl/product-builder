package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*
import org.scalajs.dom
import scala.scalajs.js

object CalendarPageCanvas {
  def apply(): Element = {
    val currentPage = CalendarViewModel.currentPage

    div(
      cls := "calendar-canvas-container",

      div(
        cls := "calendar-canvas",

        // Click on empty area to deselect
        onClick --> { ev =>
          if ev.target.asInstanceOf[dom.Element].classList.contains("calendar-page") ||
             ev.target.asInstanceOf[dom.Element].classList.contains("calendar-background") then
            CalendarViewModel.deselectElement()
        },

        // Render the calendar page
        child <-- currentPage.map(renderPage)
      )
    )
  }

  private def renderPage(page: CalendarPage): Element = {
    div(
      cls := "calendar-page",

      // Background
      renderBackground(page.template.background),

      // Month title (locked template field)
      renderTemplateTextField(page.template.monthField),

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
    val selected = CalendarViewModel.selectedElement

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

      // Rotation button (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(field.id) then Some(renderRotationButton(field.id, field.rotation)) else None
      },

      // Drag behavior
      onMouseDown --> { ev =>
        if !ev.target.asInstanceOf[dom.Element].classList.contains("resize-handle") &&
           !ev.target.asInstanceOf[dom.Element].classList.contains("rotate-btn") then
          ev.preventDefault()
          ev.stopPropagation()
          CalendarViewModel.selectElement(field.id)
          startDrag(field.id, field.position, ev)
      }
    )
  }

  // ─── Shape Element ───────────────────────────────────────────────

  private def renderShapeElement(shape: ShapeElement): Element = {
    val selected = CalendarViewModel.selectedElement

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

      // Resize handles (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(shape.id) then Some(renderResizeHandles(shape.id, shape)) else None
      },

      // Rotation button (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(shape.id) then Some(renderRotationButton(shape.id, shape.rotation)) else None
      },

      // Drag behavior
      onMouseDown --> { ev =>
        if !ev.target.asInstanceOf[dom.Element].classList.contains("resize-handle") &&
           !ev.target.asInstanceOf[dom.Element].classList.contains("rotate-btn") then
          ev.preventDefault()
          ev.stopPropagation()
          CalendarViewModel.selectElement(shape.id)
          startDrag(shape.id, shape.position, ev)
      }
    )
  }

  // ─── Clipart Element ─────────────────────────────────────────────

  private def renderClipartElement(clip: ClipartElement): Element = {
    val selected = CalendarViewModel.selectedElement

    div(
      cls := "calendar-element calendar-clipart-element",
      cls <-- selected.map(sel => if sel.contains(clip.id) then "selected" else ""),
      styleAttr := s"position: absolute; left: ${clip.position.x}px; top: ${clip.position.y}px; width: ${clip.size.width}px; height: ${clip.size.height}px; transform: rotate(${clip.rotation}deg); transform-origin: center; z-index: ${clip.zIndex};",

      img(
        src := clip.imageData,
        styleAttr := "width: 100%; height: 100%; object-fit: contain; pointer-events: none;",
        draggable := false,
      ),

      // Resize handles (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(clip.id) then Some(renderResizeHandles(clip.id, clip)) else None
      },

      // Rotation button (when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(clip.id) then Some(renderRotationButton(clip.id, clip.rotation)) else None
      },

      // Drag behavior
      onMouseDown --> { ev =>
        if !ev.target.asInstanceOf[dom.Element].classList.contains("resize-handle") &&
           !ev.target.asInstanceOf[dom.Element].classList.contains("rotate-btn") then
          ev.preventDefault()
          ev.stopPropagation()
          CalendarViewModel.selectElement(clip.id)
          startDrag(clip.id, clip.position, ev)
      }
    )
  }

  // ─── Photo Element ───────────────────────────────────────────────

  private def renderPhoto(photo: PhotoElement): Element = {
    val selected = CalendarViewModel.selectedElement

    div(
      cls := "calendar-element calendar-photo",
      cls <-- selected.map(sel => if sel.contains(photo.id) then "selected" else ""),
      styleAttr := s"position: absolute; left: ${photo.position.x}px; top: ${photo.position.y}px; width: ${photo.size.width}px; height: ${photo.size.height}px; transform: rotate(${photo.rotation}deg); transform-origin: center; overflow: hidden; z-index: ${photo.zIndex};",

      // Main image
      img(
        src := photo.imageData,
        styleAttr := "width: 100%; height: 100%; object-fit: contain; pointer-events: none;",
        draggable := false
      ),

      // Resize handles (visible when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(photo.id) then Some(renderResizeHandles(photo.id, photo)) else None
      },

      // Rotation button (visible when selected)
      child.maybe <-- selected.map { sel =>
        if sel.contains(photo.id) then Some(renderRotationButton(photo.id, photo.rotation)) else None
      },

      // Make draggable
      onMouseDown --> { ev =>
        if !ev.target.asInstanceOf[dom.Element].classList.contains("resize-handle") &&
           !ev.target.asInstanceOf[dom.Element].classList.contains("rotate-btn") then
          ev.preventDefault()
          ev.stopPropagation()
          CalendarViewModel.selectElement(photo.id)
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
      CalendarViewModel.updateElementPosition(
        elementId,
        Position(startPos.x + deltaX, startPos.y + deltaY)
      )
    }

    val mouseUpHandler: js.Function1[dom.MouseEvent, Unit] = { _ =>
      dom.window.removeEventListener("mousemove", mouseMoveHandler)
      mouseUpHandlerOpt.foreach(h => dom.window.removeEventListener("mouseup", h))
    }

    mouseUpHandlerOpt = Some(mouseUpHandler)

    dom.window.addEventListener("mousemove", mouseMoveHandler)
    dom.window.addEventListener("mouseup", mouseUpHandler)
  }

  // ─── Generic resize handles ──────────────────────────────────────

  private def renderResizeHandles(elementId: String, elem: CanvasElement): Element = {
    div(
      cls := "resize-handles",

      // Corner handles
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
                  CalendarViewModel.updateElementSize(elementId, Size(newWidth, newHeight))

                case "sw" =>
                  val newWidth = math.max(30, startWidth - deltaX)
                  val newHeight = newWidth / aspectRatio
                  CalendarViewModel.updateElementSize(elementId, Size(newWidth, newHeight))
                  CalendarViewModel.updateElementPosition(elementId, Position(startPosX + (startWidth - newWidth), startPosY))

                case "ne" =>
                  val newWidth = math.max(30, startWidth + deltaX)
                  val newHeight = newWidth / aspectRatio
                  CalendarViewModel.updateElementSize(elementId, Size(newWidth, newHeight))
                  CalendarViewModel.updateElementPosition(elementId, Position(startPosX, startPosY + (startHeight - newHeight)))

                case "nw" =>
                  val newWidth = math.max(30, startWidth - deltaX)
                  val newHeight = newWidth / aspectRatio
                  CalendarViewModel.updateElementSize(elementId, Size(newWidth, newHeight))
                  CalendarViewModel.updateElementPosition(elementId, Position(startPosX + (startWidth - newWidth), startPosY + (startHeight - newHeight)))

                case _ => ()
              }
            }

            val mouseUpHandler: js.Function1[dom.MouseEvent, Unit] = { _ =>
              dom.window.removeEventListener("mousemove", mouseMoveHandler)
              mouseUpHandlerOpt.foreach(h => dom.window.removeEventListener("mouseup", h))
            }

            mouseUpHandlerOpt = Some(mouseUpHandler)
            dom.window.addEventListener("mousemove", mouseMoveHandler)
            dom.window.addEventListener("mouseup", mouseUpHandler)
          }
        )
      },

      // Side handles
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
                  CalendarViewModel.updateElementSize(elementId, Size(math.max(30, startWidth + deltaX), startHeight))
                case "w" =>
                  val newWidth = math.max(30, startWidth - deltaX)
                  CalendarViewModel.updateElementSize(elementId, Size(newWidth, startHeight))
                  CalendarViewModel.updateElementPosition(elementId, Position(startPosX + (startWidth - newWidth), startPosY))
                case "s" =>
                  CalendarViewModel.updateElementSize(elementId, Size(startWidth, math.max(30, startHeight + deltaY)))
                case "n" =>
                  val newHeight = math.max(30, startHeight - deltaY)
                  CalendarViewModel.updateElementSize(elementId, Size(startWidth, newHeight))
                  CalendarViewModel.updateElementPosition(elementId, Position(startPosX, startPosY + (startHeight - newHeight)))
                case _ => ()
              }
            }

            val mouseUpHandler: js.Function1[dom.MouseEvent, Unit] = { _ =>
              dom.window.removeEventListener("mousemove", mouseMoveHandler)
              mouseUpHandlerOpt.foreach(h => dom.window.removeEventListener("mouseup", h))
            }

            mouseUpHandlerOpt = Some(mouseUpHandler)
            dom.window.addEventListener("mousemove", mouseMoveHandler)
            dom.window.addEventListener("mouseup", mouseUpHandler)
          }
        )
      }
    )
  }

  // ─── Rotation button ─────────────────────────────────────────────

  private def renderRotationButton(elementId: String, currentRotation: Double): Element = {
    div(
      cls := "rotate-btn",
      "↻",
      title := "Rotate 15°",
      onClick --> { ev =>
        ev.preventDefault()
        ev.stopPropagation()
        val newRotation = (currentRotation + 15) % 360
        CalendarViewModel.updateElementRotation(elementId, newRotation)
      }
    )
  }

  private def alignToCss(align: TextAlignment): String = align match {
    case TextAlignment.Left   => "left"
    case TextAlignment.Center => "center"
    case TextAlignment.Right  => "right"
  }
}
