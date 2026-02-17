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
      
      // Photo (if exists)
      page.photo.map(renderPhoto).toSeq,
      
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
    div(
      cls := "calendar-photo",
      styleAttr := s"position: absolute; left: ${photo.position.x}px; top: ${photo.position.y}px; width: ${photo.size.width}px; height: ${photo.size.height}px; transform: rotate(${photo.rotation}deg); cursor: move; overflow: hidden;",
      
      img(
        src := photo.imageData,
        styleAttr := "width: 100%; height: 100%; object-fit: contain; pointer-events: none;",
        draggable := false
      ),
      
      // Make draggable
      onMouseDown --> { ev =>
        ev.preventDefault()
        
        val startX = ev.clientX
        val startY = ev.clientY
        val startPosX = photo.position.x
        val startPosY = photo.position.y
        
        var mouseUpHandlerOpt: Option[js.Function1[dom.MouseEvent, Unit]] = None
        
        val mouseMoveHandler: js.Function1[dom.MouseEvent, Unit] = { moveEv =>
          val deltaX = moveEv.clientX - startX
          val deltaY = moveEv.clientY - startY
          CalendarViewModel.updatePhotoPosition(
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
}
