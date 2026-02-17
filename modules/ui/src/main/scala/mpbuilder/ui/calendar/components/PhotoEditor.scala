package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*
import org.scalajs.dom
import org.scalajs.dom.FileReader
import scala.scalajs.js

object PhotoEditor {
  def apply(): Element = {
    val currentPage = CalendarViewModel.currentPage
    val selectedPhoto = CalendarViewModel.selectedPhoto
    
    div(
      cls := "photo-editor-section",
      
      h4("Photos"),
      
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
      ),
      
      // List of photos
      div(
        cls := "photos-list",
        children <-- currentPage.combineWith(selectedPhoto).map { (page, selected) =>
          page.elements.collect { case p: PhotoElement => p }.zipWithIndex.map { case (photo, index) =>
            renderPhotoItem(photo, index + 1, selected)
          }
        }
      ),
      
      // Selected photo controls
      child.maybe <-- selectedPhoto.signal.combineWith(currentPage).map {
        case (Some(selectedId), page) =>
          page.elements.collectFirst { case p: PhotoElement if p.id == selectedId => p }.map { photo =>
            renderPhotoControls(photo)
          }
        case _ => None
      }
    )
  }
  
  private def renderPhotoItem(photo: PhotoElement, number: Int, selectedId: Option[String]): Element = {
    val isSelected = selectedId.contains(photo.id)
    
    div(
      cls := "photo-item",
      cls := "selected" -> isSelected,
      
      div(
        cls := "photo-preview",
        span(
          cls := "photo-number",
          s"Photo $number"
        ),
        button(
          cls := "select-photo-btn",
          "Select",
          onClick --> { _ => CalendarViewModel.selectPhoto(photo.id) }
        ),
        button(
          cls := "remove-photo-btn",
          "×",
          onClick --> { _ => CalendarViewModel.removePhoto(photo.id) }
        )
      )
    )
  }
  
  private def renderPhotoControls(photo: PhotoElement): Element = {
    div(
      cls := "photo-controls",
      
      h5("Photo Controls"),
      p(cls := "photo-hint", "Tip: Click and drag corners to resize. Click rotate button (↻) to rotate."),
          
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
                  CalendarViewModel.updatePhotoSize(photo.id, Size(w, photo.size.height))
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
                  CalendarViewModel.updatePhotoSize(photo.id, Size(photo.size.width, h))
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
                  CalendarViewModel.updatePhotoRotation(photo.id, r)
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
                  CalendarViewModel.updatePhotoPosition(photo.id, Position(x, photo.position.y))
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
                  CalendarViewModel.updatePhotoPosition(photo.id, Position(photo.position.x, y))
                }
              }
            ),
          ),
        )
  }
}
