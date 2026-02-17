package mpbuilder.ui.calendar

import com.raquo.laminar.api.L.*
import mpbuilder.ui.calendar.components.*

object CalendarBuilderApp {
  def apply(): Element = {
    div(
      cls := "calendar-builder-app",
      
      // Header
      div(
        cls := "calendar-header",
        h1("Photo Calendar Builder"),
        p("Create your custom photo calendar - upload photos, add text, and customize each month")
      ),
      
      // Main content area
      div(
        cls := "calendar-main-content",
        
        // Left sidebar: Controls
        div(
          cls := "calendar-sidebar",
          
          div(
            cls := "calendar-controls-card",
            
            // Photo editor section
            PhotoEditor(),
            
            hr(),
            
            // Text editor section
            TextFieldEditor(),
          )
        ),
        
        // Center: Calendar page canvas
        div(
          cls := "calendar-canvas-area",
          CalendarPageCanvas()
        ),
        
        // Right sidebar: Page navigation
        div(
          cls := "calendar-navigation-sidebar",
          
          div(
            cls := "calendar-nav-card",
            PageNavigation()
          )
        )
      ),
      
      // Footer with actions
      div(
        cls := "calendar-footer",
        button(
          cls := "reset-btn",
          "Reset Calendar",
          onClick --> { _ =>
            if org.scalajs.dom.window.confirm("Are you sure you want to reset the calendar? All changes will be lost.") then
              CalendarViewModel.reset()
          }
        ),
        button(
          cls := "preview-btn",
          "Preview All Pages",
          onClick --> { _ =>
            org.scalajs.dom.window.alert("Preview feature coming soon!")
          }
        )
      )
    )
  }
}
