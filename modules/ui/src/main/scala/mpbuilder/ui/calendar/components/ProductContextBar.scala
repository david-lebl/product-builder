package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*
import mpbuilder.domain.model.Language
import mpbuilder.ui.productbuilder.ProductBuilderViewModel

/** Read-only info bar shown when the editor session is linked to a product configuration */
object ProductContextBar {

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "product-context-bar",

      child.maybe <-- CalendarViewModel.linkedProductDescription.combineWith(
        CalendarViewModel.productFormat
      ).combineWith(CalendarViewModel.state).combineWith(lang).map {
        (descOpt: Option[String], format: ProductFormat, state: CalendarState, l: Language) =>
          descOpt.map { desc =>
            val formatStr = l match
              case Language.En => s"${format.nameEn} (${format.widthMm}×${format.heightMm}mm)"
              case Language.Cs => s"${format.nameCs} (${format.widthMm}×${format.heightMm}mm)"
            val pageCountStr = l match
              case Language.En => s"${state.pages.size} pages"
              case Language.Cs => s"${state.pages.size} stránek"

            div(
              cls := "product-context-content",
              span(cls := "product-context-icon", "🔗"),
              span(cls := "product-context-text", s"$desc · $formatStr · $pageCountStr"),
              span(
                cls := "product-context-badge",
                l match
                  case Language.En => "Linked to Product Builder"
                  case Language.Cs => "Propojeno s konfigurátorem"
              ),
            )
          }
      },
    )
  }
}
