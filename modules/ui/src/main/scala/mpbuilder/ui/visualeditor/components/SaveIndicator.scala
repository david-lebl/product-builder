package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.VisualEditorViewModel

/** Small save status indicator shown in the editor header */
object SaveIndicator {
  def apply(): Element = {
    div(
      cls := "save-indicator",
      child.text <-- VisualEditorViewModel.isSaving.combineWith(VisualEditorViewModel.lastSaved, VisualEditorViewModel.currentSessionId).map {
        case (_, _, None) => "" // No active session
        case (true, _, _) => "Saving..."
        case (false, Some(ts), _) =>
          val seconds = ((System.currentTimeMillis().toDouble - ts) / 1000).toInt
          if seconds < 5 then "Saved"
          else if seconds < 60 then s"Saved ${seconds}s ago"
          else if seconds < 3600 then s"Saved ${seconds / 60}m ago"
          else "Saved"
        case (false, None, Some(_)) => "Not yet saved"
      },
    )
  }
}
