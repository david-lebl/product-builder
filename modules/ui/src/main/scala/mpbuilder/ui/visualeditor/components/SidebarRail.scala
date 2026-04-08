package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*

/** A single panel definition in the sidebar rail. */
case class RailPanel(
  id: String,
  icon: String,
  label: Signal[String],
  content: () => HtmlElement,
)

/** Vertical icon rail with collapsible drawer.
  *
  * Layout:
  *   - Desktop: 56px-wide icon column on the left. Clicking an icon opens a
  *     ~300px drawer to the right of the rail with that panel's content.
  *     Clicking the same icon (or the drawer's × button) collapses the drawer.
  *   - Mobile (≤1200px): the rail switches to a fixed bottom bar; the drawer
  *     becomes a bottom sheet that slides up over the canvas (handled by CSS).
  *
  * `activePanel` is `None` when no drawer is open, so the canvas can use the
  * full available width by default.
  */
object SidebarRail {
  def apply(panels: List[RailPanel], activePanel: Var[Option[String]]): HtmlElement =
    div(
      cls := "sidebar-rail-container",

      // Icon column
      div(
        cls := "sidebar-rail",
        panels.map { panel =>
          button(
            tpe := "button",
            cls <-- activePanel.signal.map { active =>
              if active.contains(panel.id) then "sidebar-rail-icon sidebar-rail-icon--active"
              else "sidebar-rail-icon"
            },
            title <-- panel.label,
            span(cls := "sidebar-rail-icon-glyph", panel.icon),
            onClick --> { _ =>
              activePanel.update {
                case Some(id) if id == panel.id => None
                case _                          => Some(panel.id)
              }
            },
          )
        },
      ),

      // Collapsible drawer (only mounted when a panel is active)
      child.maybe <-- activePanel.signal.map { active =>
        active.flatMap(id => panels.find(_.id == id)).map { panel =>
          div(
            cls := "sidebar-rail-drawer",
            div(
              cls := "sidebar-rail-drawer-header",
              span(cls := "sidebar-rail-drawer-title", child.text <-- panel.label),
              button(
                tpe := "button",
                cls := "sidebar-rail-drawer-close",
                title := "Close",
                "×",
                onClick --> { _ => activePanel.set(None) },
              ),
            ),
            div(
              cls := "sidebar-rail-drawer-body",
              panel.content(),
            ),
          )
        }
      },
    )
}
