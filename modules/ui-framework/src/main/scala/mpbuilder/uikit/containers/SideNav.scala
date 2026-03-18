package mpbuilder.uikit.containers

import com.raquo.laminar.api.L.*

/** A single item in the sidebar navigation. */
case class SideNavItem(
    icon: String,
    label: String,
    isActive: Signal[Boolean],
    onClick: () => Unit,
)

/** Dark sidebar navigation panel with icon, title, and a list of nav items.
  *
  * Used by manufacturing, catalog editor, and customer management apps to provide
  * a consistent left-side navigation experience. CSS classes are defined in
  * uikit.css (app-sidebar-* prefix).
  *
  * Pair with the `app-sidebar-layout` CSS class on the parent grid container and
  * `app-sidebar-content` on the content area.
  */
object SideNav:

  def apply(
      icon: String,
      title: String,
      items: List[SideNavItem],
  ): HtmlElement =
    htmlTag("nav")(
      cls := "app-sidebar",
      div(
        cls := "app-sidebar-header",
        span(cls := "app-sidebar-header-icon", icon),
        span(cls := "app-sidebar-header-title", title),
      ),
      div(
        cls := "app-sidebar-nav",
        items.map { item =>
          button(
            cls <-- item.isActive.map(active =>
              if active then "app-sidebar-item app-sidebar-item--active"
              else "app-sidebar-item"
            ),
            span(cls := "app-sidebar-item-icon", item.icon),
            span(cls := "app-sidebar-item-label", item.label),
            onClick --> { _ => item.onClick() },
          )
        },
      ),
    )
