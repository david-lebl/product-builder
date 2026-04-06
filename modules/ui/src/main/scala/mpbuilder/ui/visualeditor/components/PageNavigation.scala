package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language

object PageNavigation {
  def apply(): Element = {
    val currentPageIndex = VisualEditorViewModel.currentPageIndex
    val state = VisualEditorViewModel.state
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "page-navigation-strip",

      button(
        cls := "nav-strip-btn prev-btn",
        "<-",
        disabled <-- currentPageIndex.map(_ == 0),
        onClick --> { _ => VisualEditorViewModel.goToPreviousPage() }
      ),

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

      button(
        cls := "nav-strip-btn next-btn",
        "->",
        disabled <-- state.map(s => s.currentPageIndex >= s.pages.length - 1),
        onClick --> { _ => VisualEditorViewModel.goToNextPage() }
      ),

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

  private def renderPageThumbnail(page: EditorPage, index: Int, currentIndex: Int): Element = {
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

      onClick --> { _ => VisualEditorViewModel.goToPage(index) }
    )
  }
}
