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
        styleAttr := s"background-color: ${page.template.backgroundImage match {
          case "white" => "#ffffff"
          case color => color
        }}"
      ),
      
      // Month title (locked)
      renderTextField(page.template.monthField, locked = true),
      
      // Days grid (locked)
      page.template.daysGrid.map(dayField => renderTextField(dayField, locked = true)),
      
      // All photos
      page.photos.map(renderPhoto),
      
      // Custom text fields
      page.customTextFields.map(field => renderTextField(field, locked = false))
    )
  }
  
  private def renderTextField(field: TextField, locked: Boolean): Element = {
    div(
      cls := "calendar-text-field",
      cls := "locked" -> locked,
      styleAttr := s"position: absolute; left: ${field.position.x}px; top: ${field.position.y}px; font-size: ${field.fontSize}px; font-family: ${field.fontFamily}; color: ${field.color}; cursor: ${if locked then "default" else "move"}; user-select: none;",
      
      field.text,
      
      // Make draggable if not locked
      if !locked then
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
      else
        emptyMod
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
