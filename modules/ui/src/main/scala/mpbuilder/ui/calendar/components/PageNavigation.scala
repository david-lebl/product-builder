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
      cls := "page-navigation",
      
      // Previous button
      button(
        cls := "nav-btn prev-btn",
        child.text <-- lang.map {
          case Language.En => "← Previous"
          case Language.Cs => "← Předchozí"
        },
        disabled <-- currentPageIndex.map(_ == 0),
        onClick --> { _ => CalendarViewModel.goToPreviousPage() }
      ),
      
      // Page indicator
      div(
        cls := "page-indicator",
        child.text <-- currentPageIndex.combineWith(lang).map { (index, language) =>
          language match {
            case Language.En => s"Page ${index + 1} of 12"
            case Language.Cs => s"Stránka ${index + 1} z 12"
          }
        }
      ),
      
      // Next button
      button(
        cls := "nav-btn next-btn",
        child.text <-- lang.map {
          case Language.En => "Next →"
          case Language.Cs => "Další →"
        },
        disabled <-- currentPageIndex.map(_ == 11),
        onClick --> { _ => CalendarViewModel.goToNextPage() }
      ),
      
      // Page thumbnails/quick navigation
      div(
        cls := "page-thumbnails",
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
      cls := "page-thumbnail",
      cls := "active" -> isActive,
      
      div(
        cls := "thumbnail-number",
        (index + 1).toString
      ),
      
      div(
        cls := "thumbnail-month",
        page.template.monthField.text.take(3) // Show abbreviated month name
      ),
      
      onClick --> { _ => CalendarViewModel.goToPage(index) }
    )
  }
}
