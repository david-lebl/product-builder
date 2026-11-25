package mpbuilder.editor

import kyo.*

/** Hand-off point between the pure editor and the thin browser glue (`browser.BrowserEditor`, Scala.js only).
  *
  * Kyo UI has no pointer-move events, so the canvas gestures (drag / resize / rotate), keyboard shortcuts,
  * wheel zoom, photo picking and drag-and-drop are captured by plain DOM listeners which call back into the
  * active [[EditorController]] — the same pattern `modules/ui` uses for its file drop zones.
  *
  * `runAsync` executes an effect on the host's runtime (wrapped in the host's `I18n.let`).
  */
object EditorBridge:

  @volatile private var active: Option[EditorController] = None
  @volatile private var runner: (Unit < Async) => Unit   = _ => ()

  def attach(controller: EditorController, run: (Unit < Async) => Unit): Unit =
    active = Some(controller)
    runner = run

  def detach(controller: EditorController): Unit =
    if active.contains(controller) then active = None

  def current: Option[EditorController] = active

  def runAsync(effect: Unit < Async): Unit = runner(effect)

  // DOM hooks shared by the views and the browser glue.
  val CanvasAttr: String      = "ve-canvas"
  val PageAttr: String        = "ve-page"
  val ElementAttr: String     = "ve-el"
  val HandleAttr: String      = "ve-handle"
  val UploadAttr: String      = "ve-upload"
  val TextInputAttr: String   = "ve-text-input"
  val PhotoAttr: String       = "ve-photo"
