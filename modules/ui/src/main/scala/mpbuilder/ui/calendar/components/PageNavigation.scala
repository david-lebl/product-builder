package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.Language

object PageNavigation {
  def apply(): Element = {
    val currentPageIndex = CalendarViewModel.currentPageIndex
    val state = CalendarViewModel.state
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "page-navigation-strip",

      // Previous button
      button(
        cls := "nav-strip-btn prev-btn",
        "←",
        disabled <-- currentPageIndex.map(_ == 0),
        onClick --> { _ => CalendarViewModel.goToPreviousPage() }
      ),

      // Page indicator
      div(
        cls := "page-indicator",
        child.text <-- state.combineWith(lang).map { case (s, language) =>
          val index = s.currentPageIndex
          val total = s.pages.length
          language match {
            case Language.En => s"${index + 1} / $total"
            case Language.Cs => s"${index + 1} / $total"
          }
        }
      ),

      // Next button
      button(
        cls := "nav-strip-btn next-btn",
        "→",
        disabled <-- state.map(s => s.currentPageIndex >= s.pages.length - 1),
        onClick --> { _ => CalendarViewModel.goToNextPage() }
      ),

      // Horizontally scrollable page thumbnails
      div(
        cls := "page-thumbnails-strip",
        children <-- state.map { s =>
          s.pages.zipWithIndex.map { case (page, index) =>
            renderPageThumbnail(page, index, s.currentPageIndex)
          }
        }
      )
    )
  }

  private def renderPageThumbnail(page: CalendarPage, index: Int, currentIndex: Int): Element = {
    val isActive = index == currentIndex

    div(
      cls := "page-thumbnail-item",
      cls := "active" -> isActive,

      div(
        cls := "thumbnail-number",
        (index + 1).toString
      ),

      div(
        cls := "thumbnail-label",
        page.template.monthField.text.take(5)
      ),

      onClick --> { _ => CalendarViewModel.goToPage(index) }
    )
  }
}
