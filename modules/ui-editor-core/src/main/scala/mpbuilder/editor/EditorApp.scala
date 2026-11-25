package mpbuilder.editor

import kyo.*
import kyo.UI.*
import mpbuilder.editor.views.{EditorView, HomeView}


/** Root of the Visual Editor: the home screen ("My designs" / templates) or the editing workspace. */
object EditorApp:

  def render(ctrl: EditorController, embedded: Boolean = false): UI =
    div.cssClass(if embedded then "ve-root ve-embedded" else "ve-root")(
      ctrl.screen.render {
        case EditorScreen.Home    => HomeView.render(ctrl)
        case EditorScreen.Editing => EditorView.render(ctrl)
      },
      ctrl.toast.render {
        case None      => (UI.empty: UI)
        case Some(key) =>
          div.cssClass("mp-toast ve-toast").role("status").aria("live", "polite")(
            span(ctrl.i18n.t(key)),
            button.cssClass("mp-icon-btn mp-icon-btn-sm").aria("label", "Dismiss").onClick(ctrl.toast.set(None))("×"),
          )
      },
    )
