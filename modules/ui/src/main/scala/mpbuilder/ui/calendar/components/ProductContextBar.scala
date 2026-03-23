package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language

/** Info bar shown when the editor session is linked to a product configuration */
object ProductContextBar {

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      child.maybe <-- CalendarViewModel.currentSession.combineWith(
        CalendarViewModel.state, lang
      ).map { (meta: Option[SessionMeta], state: CalendarState, language: Language) =>
        meta.flatMap(_.linkedConfigurationId).map { configId =>
          val typeName = productTypeName(state.productType, language)
          val dims = s"${state.productFormat.widthMm}x${state.productFormat.heightMm} mm"
          val pages = state.pages.length

          div(
            cls := "product-context-bar",
            span(cls := "context-badge", language match {
              case Language.En => "Linked Product"
              case Language.Cs => "Propojeny produkt"
            }),
            span(cls := "context-info",
              s"$typeName · $dims · $pages ",
              language match {
                case Language.En => "pages"
                case Language.Cs => "stranek"
              },
            ),
            span(cls := "context-config-id", s"ID: ${configId.take(12)}..."),
          )
        }
      }
    )
  }

  private def productTypeName(pt: VisualProductType, lang: Language): String = pt match
    case VisualProductType.MonthlyCalendar  => if lang == Language.En then "Monthly Calendar" else "Mesicni kalendar"
    case VisualProductType.WeeklyCalendar   => if lang == Language.En then "Weekly Calendar" else "Tydenni kalendar"
    case VisualProductType.BiweeklyCalendar => if lang == Language.En then "Bi-weekly Calendar" else "Dvoutydenni kalendar"
    case VisualProductType.PhotoBook        => if lang == Language.En then "Photo Book" else "Fotokniha"
    case VisualProductType.WallPicture      => if lang == Language.En then "Wall Picture" else "Obraz na zed"
}
