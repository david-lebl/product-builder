package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.*
import org.scalajs.dom
import org.scalajs.dom.FileReader

/** Editor for page background (color or image) and template selection */
object BackgroundEditor {
  def apply(): Element = {
    val currentPage = VisualEditorViewModel.currentPage

    div(
      cls := "background-editor-section",

      h4("Background & Template"),

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
              selected := (current == PageTemplateType.GridTemplate)
            )
          },
          onChange.mapToValue --> { _ =>
            VisualEditorViewModel.setTemplateType(PageTemplateType.GridTemplate)
          }
        )
      ),

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
            VisualEditorViewModel.setBackgroundColor(v)
          }
        )
      ),

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
                  VisualEditorViewModel.setBackgroundImage(imageData)
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

          div(
            cls := "predefined-textures",
            p(cls := "info-hint", "Predefined textures coming soon"),
          ),

          button(
            cls := "bg-reset-btn",
            "Reset to White",
            onClick --> { _ =>
              VisualEditorViewModel.setBackgroundColor("#ffffff")
            }
          ),

          button(
            cls := "bg-upload-btn",
            "Apply Background to All Pages",
            onClick --> { _ =>
              VisualEditorViewModel.applyBackgroundToAllPages()
            }
          ),

          button(
            cls := "bg-upload-btn",
            "Apply Template to All Pages",
            onClick --> { _ =>
              VisualEditorViewModel.applyTemplateToAllPages()
            }
          )
        )
      )
    )
  }
}
