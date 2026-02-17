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
        backgroundColor := page.template.backgroundImage match {
          case "white" => "#ffffff"
          case color => color
        }
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
      position := "absolute",
      left := s"${field.position.x}px",
      top := s"${field.position.y}px",
      fontSize := s"${field.fontSize}px",
      fontFamily := field.fontFamily,
      color := field.color,
      cursor := (if locked then "default" else "move"),
      userSelect := "none",
      
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
            dom.window.removeEventListener("mouseup", mouseUpHandler)
          }
          
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
      position := "absolute",
      left := s"${photo.position.x}px",
      top := s"${photo.position.y}px",
      width := s"${photo.size.width}px",
      height := s"${photo.size.height}px",
      transform := s"rotate(${photo.rotation}deg)",
      cursor := "move",
      
      img(
        src := photo.imageData,
        width := "100%",
        height := "100%",
        objectFit := "cover",
        draggable := false
      ),
      
      // Make draggable
      onMouseDown --> { ev =>
        ev.preventDefault()
        
        val startX = ev.clientX
        val startY = ev.clientY
        val startPosX = photo.position.x
        val startPosY = photo.position.y
        
        val mouseMoveHandler: js.Function1[dom.MouseEvent, Unit] = { moveEv =>
          val deltaX = moveEv.clientX - startX
          val deltaY = moveEv.clientY - startY
          CalendarViewModel.updatePhotoPosition(
            Position(startPosX + deltaX, startPosY + deltaY)
          )
        }
        
        val mouseUpHandler: js.Function1[dom.MouseEvent, Unit] = { _ =>
          dom.window.removeEventListener("mousemove", mouseMoveHandler)
          dom.window.removeEventListener("mouseup", mouseUpHandler)
        }
        
        dom.window.addEventListener("mousemove", mouseMoveHandler)
        dom.window.addEventListener("mouseup", mouseUpHandler)
      }
    )
  }
}
