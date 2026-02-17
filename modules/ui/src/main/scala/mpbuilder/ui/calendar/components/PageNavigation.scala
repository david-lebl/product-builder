package mpbuilder.ui.calendar.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.*

object PageNavigation {
  def apply(): Element = {
    val currentPageIndex = CalendarViewModel.currentPageIndex
    val state = CalendarViewModel.state
    
    div(
      cls := "page-navigation",
      
      // Previous button
      button(
        cls := "nav-btn prev-btn",
        "← Previous",
        disabled <-- currentPageIndex.map(_ == 0),
        onClick --> { _ => CalendarViewModel.goToPreviousPage() }
      ),
      
      // Page indicator
      div(
        cls := "page-indicator",
        child.text <-- currentPageIndex.map { index =>
          s"Page ${index + 1} of 12"
        }
      ),
      
      // Next button
      button(
        cls := "nav-btn next-btn",
        "Next →",
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
