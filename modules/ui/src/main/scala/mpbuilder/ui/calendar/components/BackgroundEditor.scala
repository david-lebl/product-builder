package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*
import org.scalajs.dom
import org.scalajs.dom.FileReader

/** Editor for page background (color or image) and template selection */
object BackgroundEditor {
  def apply(): Element = {
    val currentPage = CalendarViewModel.currentPage

    div(
      cls := "background-editor-section",

      h4("Background & Template"),

      // Template type selection
      div(
        cls := "control-group",
        label("Template:"),
        select(
          cls := "template-select",
          child <-- currentPage.map { page =>
            val current = page.template.templateType
            option(
              value := "grid",
              "Grid Calendar",
              selected := (current == CalendarTemplateType.GridTemplate)
            )
          },
          // More template options will be added here in the future
          onChange.mapToValue --> { _ =>
            CalendarViewModel.setTemplateType(CalendarTemplateType.GridTemplate)
          }
        )
      ),

      // Background color picker
      div(
        cls := "control-group",
        label("Background Color:"),
        input(
          typ := "color",
          cls := "color-input",
          value <-- currentPage.map { page =>
            page.template.background match {
              case PageBackground.SolidColor(c) => c
              case _ => "#ffffff"
            }
          },
          onInput.mapToValue --> { v =>
            CalendarViewModel.setBackgroundColor(v)
          }
        )
      ),

      // Background image upload
      div(
        cls := "control-group",
        label("Background Image:"),
        div(
          cls := "bg-image-controls",

          input(
            typ := "file",
            accept := "image/*",
            idAttr := "bg-upload-input",
            display := "none",
            onChange --> { ev =>
              val inp = ev.target.asInstanceOf[dom.html.Input]
              val files = inp.files
              if files.length > 0 then
                val file = files(0)
                val reader = new FileReader()
                reader.onload = { _ =>
                  val imageData = reader.result.asInstanceOf[String]
                  CalendarViewModel.setBackgroundImage(imageData)
                }
                reader.readAsDataURL(file)
            }
          ),

          button(
            cls := "bg-upload-btn",
            "Upload Image",
            onClick --> { _ =>
              dom.document.getElementById("bg-upload-input").asInstanceOf[dom.html.Input].click()
            }
          ),

          // Predefined texture placeholders
          div(
            cls := "predefined-textures",
            p(cls := "info-hint", "Predefined textures coming soon"),
          ),

          // Reset to solid color
          button(
            cls := "bg-reset-btn",
            "Reset to White",
            onClick --> { _ =>
              CalendarViewModel.setBackgroundColor("#ffffff")
            }
          ),

          // Apply to all pages
          button(
            cls := "bg-upload-btn",
            "Apply Background to All Pages",
            onClick --> { _ =>
              CalendarViewModel.applyBackgroundToAllPages()
            }
          ),

          // Apply template to all pages
          button(
            cls := "bg-upload-btn",
            "Apply Template to All Pages",
            onClick --> { _ =>
              CalendarViewModel.applyTemplateToAllPages()
            }
          )
        )
      )
    )
  }
}
