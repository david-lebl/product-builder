package mpbuilder.uicommon

import com.raquo.laminar.api.L.*

/** Reusable tab panel with button-based tab switching.
  *
  * Renders a row of tab buttons and a content area that changes based on the
  * selected tab. The active tab state is managed by a caller-provided `Var[String]`.
  */
object TabPanel:

  /** Describes a single tab. */
  case class Tab(
    id: String,
    labelMod: Modifier[HtmlElement],
    content: () => Element,
  )

  /** Renders a tab panel.
    *
    * @param tabs          The list of available tabs
    * @param activeTabVar  A `Var[String]` holding the currently selected tab ID
    * @param tabsCls       CSS class for the tab button row (default: `"sidebar-tabs"`)
    * @param tabBtnCls     CSS class for each tab button (default: `"sidebar-tab-btn"`)
    * @param contentCls    CSS class for the content container (default: `""`)
    */
  def apply(
    tabs: List[Tab],
    activeTabVar: Var[String],
    tabsCls: String = "sidebar-tabs",
    tabBtnCls: String = "sidebar-tab-btn",
    contentCls: String = "",
  ): HtmlElement =
    div(
      // Tab buttons
      div(
        cls := tabsCls,
        tabs.map { tab =>
          button(
            cls := tabBtnCls,
            cls <-- activeTabVar.signal.map(active => if active == tab.id then "active" else ""),
            tab.labelMod,
            onClick --> { _ => activeTabVar.set(tab.id) },
          )
        },
      ),
      // Tab content
      div(
        if contentCls.nonEmpty then cls := contentCls else emptyMod,
        child <-- activeTabVar.signal.map { activeId =>
          tabs.find(_.id == activeId)
            .map(_.content())
            .getOrElse(tabs.headOption.map(_.content()).getOrElse(div()))
        },
      ),
    )
