package mpbuilder.uikit.containers

import com.raquo.laminar.api.L.*

case class TabDef(id: String, label: Signal[String], content: () => HtmlElement)

object Tabs:
  def apply(tabs: List[TabDef], activeTab: Var[String], mods: Modifier[HtmlElement]*): HtmlElement =
    div(
      cls := "tabs",
      // Tab bar
      div(
        cls := "tabs-bar",
        tabs.map { tab =>
          button(
            cls <-- activeTab.signal.map(active =>
              if active == tab.id then "tab-button tab-button--active" else "tab-button"
            ),
            child.text <-- tab.label,
            onClick --> { _ => activeTab.set(tab.id) },
          )
        },
      ),
      // Tab content
      div(
        cls := "tabs-content",
        children <-- activeTab.signal.map { active =>
          tabs.find(_.id == active).map(_.content()).toList
        },
      ),
      mods,
    )
