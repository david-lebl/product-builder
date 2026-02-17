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
        
        // Render the calendar page
        child <-- currentPage.map(renderPage)
      )
    )
  }
  
  private def renderPage(page: CalendarPage): Element = {
    div(
      cls := "calendar-page",
      
      // Background
      div(
        cls := "calendar-background",
        styleAttr := s"background-color: ${page.template.background match {
          case PageBackground.SolidColor(c) => c
          case PageBackground.BackgroundImage(_) => "#ffffff"
        }}"
      ),
      
      // Month title (locked template field)
      renderTemplateTextField(page.template.monthField),
      
      // Days grid (locked template fields)
      page.template.daysGrid.map(dayField => renderTemplateTextField(dayField)),
      
      // All canvas elements (sorted by z-index)
      page.elements.sortBy(_.zIndex).map(renderCanvasElement)
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
    case photo: PhotoElement  => renderPhoto(photo)
    case text: TextElement    => renderTextElement(text)
    case shape: ShapeElement  => renderShapeElement(shape)
    case clip: ClipartElement => renderClipartElement(clip)
  }

  private def renderTextElement(field: TextElement): Element = {
    div(
      cls := "calendar-text-field",
      styleAttr := s"position: absolute; left: ${field.position.x}px; top: ${field.position.y}px; font-size: ${field.fontSize}px; font-family: ${field.fontFamily}; color: ${field.color}; cursor: move; user-select: none; font-weight: ${if field.bold then "bold" else "normal"}; font-style: ${if field.italic then "italic" else "normal"}; text-align: ${field.textAlign match { case TextAlignment.Left => "left"; case TextAlignment.Center => "center"; case TextAlignment.Right => "right" }}; transform: rotate(${field.rotation}deg);",
      field.text,
      onMouseDown --> { ev =>
        ev.preventDefault()
        CalendarViewModel.selectElement(field.id)
        
        val startX = ev.clientX
        val startY = ev.clientY
        val startPosX = field.position.x
        val startPosY = field.position.y
        
        var mouseUpHandlerOpt: Option[js.Function1[dom.MouseEvent, Unit]] = None
        
        val mouseMoveHandler: js.Function1[dom.MouseEvent, Unit] = { moveEv =>
          val deltaX = moveEv.clientX - startX
          val deltaY = moveEv.clientY - startY
          CalendarViewModel.updateTextFieldPosition(
            field.id,
            Position(startPosX + deltaX, startPosY + deltaY)
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
    )
  }

  private def renderShapeElement(shape: ShapeElement): Element = {
    div(
      cls := "calendar-shape",
      styleAttr := s"position: absolute; left: ${shape.position.x}px; top: ${shape.position.y}px; width: ${shape.size.width}px; height: ${shape.size.height}px; border: ${shape.strokeWidth}px solid ${shape.strokeColor}; background-color: ${shape.fillColor}; transform: rotate(${shape.rotation}deg);",
    )
  }

  private def renderClipartElement(clip: ClipartElement): Element = {
    div(
      cls := "calendar-clipart",
      styleAttr := s"position: absolute; left: ${clip.position.x}px; top: ${clip.position.y}px; width: ${clip.size.width}px; height: ${clip.size.height}px; transform: rotate(${clip.rotation}deg); transform-origin: center;",
      img(
        src := clip.imageData,
        styleAttr := "width: 100%; height: 100%; object-fit: contain; pointer-events: none;",
        draggable := false,
      )
    )
  }
  
  private def renderPhoto(photo: PhotoElement): Element = {
    val selectedPhotoId = CalendarViewModel.selectedPhoto
    
    div(
      cls := "calendar-photo",
      cls <-- selectedPhotoId.map(selected => if selected.contains(photo.id) then "selected" else ""),
      styleAttr := s"position: absolute; left: ${photo.position.x}px; top: ${photo.position.y}px; width: ${photo.size.width}px; height: ${photo.size.height}px; transform: rotate(${photo.rotation}deg); transform-origin: center; overflow: hidden;",
      
      // Main image
      img(
        src := photo.imageData,
        styleAttr := "width: 100%; height: 100%; object-fit: contain; pointer-events: none;",
        draggable := false
      ),
      
      // Resize handles (visible when selected)
      child.maybe <-- selectedPhotoId.map { selected =>
        if selected.contains(photo.id) then Some(renderResizeHandles(photo)) else None
      },
      
      // Rotation button (visible when selected)
      child.maybe <-- selectedPhotoId.map { selected =>
        if selected.contains(photo.id) then Some(renderRotationButton(photo)) else None
      },
      
      // Make draggable
      onMouseDown --> { ev =>
        // Don't interfere with handle dragging
        if !ev.target.asInstanceOf[dom.Element].classList.contains("resize-handle") &&
           !ev.target.asInstanceOf[dom.Element].classList.contains("rotate-btn") then
          ev.preventDefault()
          CalendarViewModel.selectPhoto(photo.id)
          
          val startX = ev.clientX
          val startY = ev.clientY
          val startPosX = photo.position.x
          val startPosY = photo.position.y
          
          var mouseUpHandlerOpt: Option[js.Function1[dom.MouseEvent, Unit]] = None
          
          val mouseMoveHandler: js.Function1[dom.MouseEvent, Unit] = { moveEv =>
            val deltaX = moveEv.clientX - startX
            val deltaY = moveEv.clientY - startY
            CalendarViewModel.updatePhotoPosition(
              photo.id,
              Position(startPosX + deltaX, startPosY + deltaY)
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
    )
  }
  
  private def renderResizeHandles(photo: PhotoElement): Element = {
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
            val startWidth = photo.size.width
            val startHeight = photo.size.height
            val startPosX = photo.position.x
            val startPosY = photo.position.y
            val aspectRatio = startWidth / startHeight
            
            var mouseUpHandlerOpt: Option[js.Function1[dom.MouseEvent, Unit]] = None
            
            val mouseMoveHandler: js.Function1[dom.MouseEvent, Unit] = { moveEv =>
              val deltaX = moveEv.clientX - startX
              val deltaY = moveEv.clientY - startY
              
              corner match {
                case "se" => // Bottom-right: resize from top-left
                  val newWidth = math.max(50, startWidth + deltaX)
                  val newHeight = newWidth / aspectRatio
                  CalendarViewModel.updatePhotoSize(photo.id, Size(newWidth, newHeight))
                  
                case "sw" => // Bottom-left: resize and move
                  val newWidth = math.max(50, startWidth - deltaX)
                  val newHeight = newWidth / aspectRatio
                  CalendarViewModel.updatePhotoSize(photo.id, Size(newWidth, newHeight))
                  CalendarViewModel.updatePhotoPosition(photo.id, Position(startPosX + (startWidth - newWidth), startPosY))
                  
                case "ne" => // Top-right: resize and move
                  val newWidth = math.max(50, startWidth + deltaX)
                  val newHeight = newWidth / aspectRatio
                  CalendarViewModel.updatePhotoSize(photo.id, Size(newWidth, newHeight))
                  CalendarViewModel.updatePhotoPosition(photo.id, Position(startPosX, startPosY + (startHeight - newHeight)))
                  
                case "nw" => // Top-left: resize and move both
                  val newWidth = math.max(50, startWidth - deltaX)
                  val newHeight = newWidth / aspectRatio
                  CalendarViewModel.updatePhotoSize(photo.id, Size(newWidth, newHeight))
                  CalendarViewModel.updatePhotoPosition(photo.id, Position(startPosX + (startWidth - newWidth), startPosY + (startHeight - newHeight)))
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
            val startWidth = photo.size.width
            val startHeight = photo.size.height
            val startPosX = photo.position.x
            val startPosY = photo.position.y
            val aspectRatio = startWidth / startHeight
            
            var mouseUpHandlerOpt: Option[js.Function1[dom.MouseEvent, Unit]] = None
            
            val mouseMoveHandler: js.Function1[dom.MouseEvent, Unit] = { moveEv =>
              val deltaX = moveEv.clientX - startX
              val deltaY = moveEv.clientY - startY
              
              side match {
                case "e" => // Right
                  val newWidth = math.max(50, startWidth + deltaX)
                  val newHeight = newWidth / aspectRatio
                  CalendarViewModel.updatePhotoSize(photo.id, Size(newWidth, newHeight))
                  
                case "w" => // Left
                  val newWidth = math.max(50, startWidth - deltaX)
                  val newHeight = newWidth / aspectRatio
                  CalendarViewModel.updatePhotoSize(photo.id, Size(newWidth, newHeight))
                  CalendarViewModel.updatePhotoPosition(photo.id, Position(startPosX + (startWidth - newWidth), startPosY))
                  
                case "s" => // Bottom
                  val newHeight = math.max(50, startHeight + deltaY)
                  val newWidth = newHeight * aspectRatio
                  CalendarViewModel.updatePhotoSize(photo.id, Size(newWidth, newHeight))
                  
                case "n" => // Top
                  val newHeight = math.max(50, startHeight - deltaY)
                  val newWidth = newHeight * aspectRatio
                  CalendarViewModel.updatePhotoSize(photo.id, Size(newWidth, newHeight))
                  CalendarViewModel.updatePhotoPosition(photo.id, Position(startPosX, startPosY + (startHeight - newHeight)))
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
  
  private def renderRotationButton(photo: PhotoElement): Element = {
    div(
      cls := "rotate-btn",
      "↻",
      title := "Rotate",
      onClick --> { ev =>
        ev.preventDefault()
        ev.stopPropagation()
        val newRotation = (photo.rotation + 15) % 360
        CalendarViewModel.updatePhotoRotation(photo.id, newRotation)
      }
    )
  }
}
