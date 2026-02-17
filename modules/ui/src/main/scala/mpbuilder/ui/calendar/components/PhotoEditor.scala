package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*
import org.scalajs.dom
import org.scalajs.dom.FileReader
import scala.scalajs.js

object PhotoEditor {
  def apply(): Element = {
    val currentPage = CalendarViewModel.currentPage
    val photoEditorOpen = CalendarViewModel.photoEditorOpen
    
    div(
      cls := "photo-editor-section",
      
      h4("Photo"),
      
      // Photo upload section
      div(
        cls := "photo-upload",
        
        // File input (hidden)
        input(
          typ := "file",
          accept := "image/*",
          idAttr := "photo-upload-input",
          display := "none",
          onChange --> { ev =>
            val input = ev.target.asInstanceOf[dom.html.Input]
            val files = input.files
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
        
        // Upload button
        button(
          cls := "photo-upload-btn",
          "Upload Photo",
          onClick --> { _ =>
            dom.document.getElementById("photo-upload-input").asInstanceOf[dom.html.Input].click()
          }
        ),
        
        // Show remove button if photo exists
        child.maybe <-- currentPage.map(_.photo.map { photo =>
          button(
            cls := "photo-remove-btn",
            "Remove Photo",
            onClick --> { _ => CalendarViewModel.removePhoto() }
          )
        }),
      ),
      
      // Photo controls (only show if photo exists)
      child.maybe <-- currentPage.map(_.photo.map { photo =>
        div(
          cls := "photo-controls",
          
          h5("Photo Controls"),
          
          // Size controls
          div(
            cls := "control-group",
            label("Width:"),
            input(
              typ := "number",
              value := photo.size.width.toString,
              minAttr := "50",
              maxAttr := "800",
              stepAttr := "10",
              cls := "size-input",
              onInput.mapToValue --> { value =>
                value.toDoubleOption.foreach { w =>
                  CalendarViewModel.updatePhotoSize(Size(w, photo.size.height))
                }
              }
            ),
          ),
          
          div(
            cls := "control-group",
            label("Height:"),
            input(
              typ := "number",
              value := photo.size.height.toString,
              minAttr := "50",
              maxAttr := "800",
              stepAttr := "10",
              cls := "size-input",
              onInput.mapToValue --> { value =>
                value.toDoubleOption.foreach { h =>
                  CalendarViewModel.updatePhotoSize(Size(photo.size.width, h))
                }
              }
            ),
          ),
          
          // Rotation control
          div(
            cls := "control-group",
            label("Rotation:"),
            input(
              typ := "range",
              value := photo.rotation.toString,
              minAttr := "0",
              maxAttr := "360",
              stepAttr := "1",
              cls := "rotation-slider",
              onInput.mapToValue --> { value =>
                value.toDoubleOption.foreach { r =>
                  CalendarViewModel.updatePhotoRotation(r)
                }
              }
            ),
            span(s"${photo.rotation.toInt}°"),
          ),
          
          // Position controls
          div(
            cls := "control-group",
            label("X Position:"),
            input(
              typ := "number",
              value := photo.position.x.toString,
              minAttr := "0",
              maxAttr := "1000",
              stepAttr := "5",
              cls := "position-input",
              onInput.mapToValue --> { value =>
                value.toDoubleOption.foreach { x =>
                  CalendarViewModel.updatePhotoPosition(Position(x, photo.position.y))
                }
              }
            ),
          ),
          
          div(
            cls := "control-group",
            label("Y Position:"),
            input(
              typ := "number",
              value := photo.position.y.toString,
              minAttr := "0",
              maxAttr := "1000",
              stepAttr := "5",
              cls := "position-input",
              onInput.mapToValue --> { value =>
                value.toDoubleOption.foreach { y =>
                  CalendarViewModel.updatePhotoPosition(Position(photo.position.x, y))
                }
              }
            ),
          ),
        )
      }),
    )
  }
}
