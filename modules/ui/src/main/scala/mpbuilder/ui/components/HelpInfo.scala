package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*

/** A small help button (ⓘ) that shows a popup with a description when clicked.
  * Used next to select fields and labels to explain configuration options
  * like materials, finishes, printing methods, etc.
  */
object HelpInfo:

  /** Renders a help info trigger button. When clicked, shows a popup with the description.
    * Returns an empty node if the description is None.
    */
  def apply(description: Option[LocalizedString], lang: Signal[Language]): HtmlElement =
    description match
      case None => span(display := "none")
      case Some(desc) =>
        val isOpen = Var(false)
        span(
          cls := "help-info-wrapper",
          button(
            cls := "help-info-trigger",
            typ := "button",
            "?",
            onClick.stopPropagation --> { _ => isOpen.update(!_) },
          ),
          child.maybe <-- isOpen.signal.combineWith(lang).map { case (open, l) =>
            if open then
              Some(div(
                cls := "help-info-popup",
                p(desc(l)),
                // Close when clicking outside
                onMountCallback { ctx =>
                  val closeHandler: org.scalajs.dom.Event => Unit = { (e: org.scalajs.dom.Event) =>
                    val target = e.target.asInstanceOf[org.scalajs.dom.Element]
                    val wrapper = ctx.thisNode.ref.parentElement
                    if wrapper != null && !wrapper.contains(target) then
                      isOpen.set(false)
                  }
                  org.scalajs.dom.document.addEventListener("click", closeHandler)
                  ctx.thisNode.amend(
                    onUnmountCallback { _ =>
                      org.scalajs.dom.document.removeEventListener("click", closeHandler)
                    }
                  )
                },
              ))
            else None
          },
        )

  /** Convenience overload that takes a Signal of Option[LocalizedString] for reactive descriptions. */
  def fromSignal(description: Signal[Option[LocalizedString]], lang: Signal[Language]): HtmlElement =
    val isOpen = Var(false)
    span(
      cls := "help-info-wrapper",
      child.maybe <-- description.map { desc =>
        desc.map { _ =>
          button(
            cls := "help-info-trigger",
            typ := "button",
            "?",
            onClick.stopPropagation --> { _ => isOpen.update(!_) },
          )
        }
      },
      child.maybe <-- isOpen.signal.combineWith(description, lang).map { case (open, descOpt, l) =>
        if open then
          descOpt.map { desc =>
            div(
              cls := "help-info-popup",
              p(desc(l)),
              onMountCallback { ctx =>
                val closeHandler: org.scalajs.dom.Event => Unit = { (e: org.scalajs.dom.Event) =>
                  val target = e.target.asInstanceOf[org.scalajs.dom.Element]
                  val wrapper = ctx.thisNode.ref.parentElement
                  if wrapper != null && !wrapper.contains(target) then
                    isOpen.set(false)
                }
                org.scalajs.dom.document.addEventListener("click", closeHandler)
                ctx.thisNode.amend(
                  onUnmountCallback { _ =>
                    org.scalajs.dom.document.removeEventListener("click", closeHandler)
                  }
                )
              },
            )
          }
        else None
      },
    )
