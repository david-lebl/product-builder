package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*

/** Reusable help information popup component.
  *
  * Renders a small "?" trigger button that, when clicked, toggles a popup
  * overlay with descriptive help text. Used next to section labels (overview help)
  * and next to selected items (detail help) in the product builder UI.
  */
object HelpInfo:

  /** Static help popup — shows a fixed description string.
    *
    * @param description the localized help text to display
    * @param lang        current language
    */
  def apply(description: LocalizedString, lang: Language): HtmlElement =
    val isOpen = Var(false)
    div(
      cls := "help-info-wrapper",
      button(
        cls := "help-info-trigger",
        "?",
        onClick.preventDefault --> { _ => isOpen.update(!_) },
      ),
      div(
        cls <-- isOpen.signal.map(open => if open then "help-info-popup help-info-popup--visible" else "help-info-popup"),
        div(
          cls := "help-info-popup-content",
          p(description(lang)),
        ),
        button(
          cls := "help-info-popup-close",
          "×",
          onClick.preventDefault --> { _ => isOpen.set(false) },
        ),
      ),
      // Backdrop to close popup when clicking outside
      div(
        cls <-- isOpen.signal.map(open => if open then "help-info-backdrop help-info-backdrop--visible" else "help-info-backdrop"),
        onClick --> { _ => isOpen.set(false) },
      ),
    )

  /** Reactive help popup — description comes from a Signal.
    *
    * Use this when the description changes based on the currently selected item,
    * e.g. showing help for the selected material in a dropdown.
    *
    * @param descriptionSignal signal of optional localized description
    * @param lang              current language
    */
  def fromSignal(descriptionSignal: Signal[Option[LocalizedString]], lang: Signal[Language]): HtmlElement =
    val isOpen = Var(false)
    div(
      cls := "help-info-wrapper help-info-wrapper--inline",
      child.maybe <-- descriptionSignal.map { descOpt =>
        descOpt.map { _ =>
          button(
            cls := "help-info-trigger help-info-trigger--small",
            "ℹ",
            onClick.preventDefault --> { _ => isOpen.update(!_) },
          )
        }
      },
      div(
        cls <-- isOpen.signal.map(open => if open then "help-info-popup help-info-popup--visible" else "help-info-popup"),
        div(
          cls := "help-info-popup-content",
          child.text <-- descriptionSignal.combineWith(lang).map { case (descOpt, l) =>
            descOpt.map(_(l)).getOrElse("")
          },
        ),
        button(
          cls := "help-info-popup-close",
          "×",
          onClick.preventDefault --> { _ => isOpen.set(false) },
        ),
      ),
      div(
        cls <-- isOpen.signal.map(open => if open then "help-info-backdrop help-info-backdrop--visible" else "help-info-backdrop"),
        onClick --> { _ => isOpen.set(false) },
      ),
    )
