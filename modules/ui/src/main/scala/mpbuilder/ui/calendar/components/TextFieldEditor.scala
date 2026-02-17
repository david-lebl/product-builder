package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*

object TextFieldEditor {
  def apply(): Element = {
    val currentPage = CalendarViewModel.currentPage
    val selectedElement = CalendarViewModel.selectedElement
    
    div(
      cls := "text-editor-section",
      
      h4("Custom Text Fields"),
      
      // Add text field button
      button(
        cls := "add-text-btn",
        "Add Text Field",
        onClick --> { _ => CalendarViewModel.addTextField() }
      ),
      
      // List of custom text fields
      div(
        cls := "text-fields-list",
        children <-- currentPage.map { page =>
          page.customTextFields.map { textField =>
            renderTextField(textField, selectedElement.now())
          }
        }
      ),
      
      // Selected text field editor
      child.maybe <-- selectedElement.signal.combineWith(currentPage).map {
        case (Some(selectedId), page) =>
          page.customTextFields.find(_.id == selectedId).map { textField =>
            renderTextFieldEditor(textField)
          }
        case _ => None
      }
    )
  }
  
  private def renderTextField(textField: TextField, selectedId: Option[String]): Element = {
    val isSelected = selectedId.contains(textField.id)
    
    div(
      cls := "text-field-item",
      cls := "selected" -> isSelected,
      
      div(
        cls := "text-field-preview",
        span(
          cls := "text-preview",
          textField.text
        ),
        button(
          cls := "edit-text-btn",
          "Edit",
          onClick --> { _ => CalendarViewModel.selectElement(textField.id) }
        ),
        button(
          cls := "remove-text-btn",
          "×",
          onClick --> { _ => CalendarViewModel.removeTextField(textField.id) }
        )
      )
    )
  }
  
  private def renderTextFieldEditor(textField: TextField): Element = {
    div(
      cls := "text-field-editor",
      
      h5("Edit Text Field"),
      
      // Text content
      div(
        cls := "control-group",
        label("Text:"),
        input(
          typ := "text",
          value := textField.text,
          cls := "text-input",
          onInput.mapToValue --> { value =>
            CalendarViewModel.updateTextFieldText(textField.id, value)
          }
        )
      ),
      
      // Font size
      div(
        cls := "control-group",
        label("Font Size:"),
        input(
          typ := "number",
          value := textField.fontSize.toString,
          minAttr := "8",
          maxAttr := "72",
          step := "1",
          cls := "font-size-input",
          onInput.mapToValue --> { value =>
            value.toIntOption.foreach { size =>
              CalendarViewModel.updateTextFieldFontSize(textField.id, size)
            }
          }
        )
      ),
      
      // Color
      div(
        cls := "control-group",
        label("Color:"),
        input(
          typ := "color",
          value := textField.color,
          cls := "color-input",
          onInput.mapToValue --> { value =>
            CalendarViewModel.updateTextFieldColor(textField.id, value)
          }
        )
      ),
      
      // Position
      div(
        cls := "control-group",
        label("X Position:"),
        input(
          typ := "number",
          value := textField.position.x.toString,
          minAttr := "0",
          maxAttr := "1000",
          step := "5",
          cls := "position-input",
          onInput.mapToValue --> { value =>
            value.toDoubleOption.foreach { x =>
              CalendarViewModel.updateTextFieldPosition(textField.id, Position(x, textField.position.y))
            }
          }
        )
      ),
      
      div(
        cls := "control-group",
        label("Y Position:"),
        input(
          typ := "number",
          value := textField.position.y.toString,
          minAttr := "0",
          maxAttr := "1000",
          step := "5",
          cls := "position-input",
          onInput.mapToValue --> { value =>
            value.toDoubleOption.foreach { y =>
              CalendarViewModel.updateTextFieldPosition(textField.id, Position(textField.position.x, y))
            }
          }
        )
      ),
      
      // Done button
      button(
        cls := "done-editing-btn",
        "Done",
        onClick --> { _ => CalendarViewModel.deselectElement() }
      )
    )
  }
}
