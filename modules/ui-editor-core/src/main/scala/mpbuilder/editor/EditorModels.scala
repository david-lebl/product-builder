package mpbuilder.editor

import kyo.*
import mpbuilder.design.*
import mpbuilder.shared.*

/** What the host asks the editor to open.
  *
  *   - Opened from the storefront configurator: `product`, `format` and `pageCount` describe the configured item.
  *   - `designId` re-opens an existing design ("Edit design" on a cart line).
  *   - Standalone (`/editor/`): everything empty → the "My designs" home screen.
  */
final case class EditorLaunch(
    product: Option[ProductBinding] = None,
    format: Option[DesignFormat] = None,
    pageCount: Option[Int] = None,
    designId: Option[DesignId] = None,
    title: Option[String] = None,
    year: Option[Int] = None,
) derives CanEqual:
  def kind: Option[DesignProductKind] = product.map(_.kind)

/** Everything the host gets back when the customer confirms the design for ordering. */
final case class EditorResult(
    project: DesignProject,
    proofSvg: String,
    proofDataUrl: String,
    issues: List[DesignIssue],
    minimumDpi: Option[Int],
)

/** A design as stored on this device, with its server sync bookkeeping. */
final case class LocalDesign(
    project: DesignProject,
    /** Server revision this local copy was last synced with (`None` = never uploaded). */
    syncedRevision: Option[Long] = None,
    /** Local edits not yet pushed to the server. */
    pendingPush: Boolean = true,
)

// ── Outbound ports (implemented per platform) ───────────────────────────────

/** Local design persistence (IndexedDB in the browser, in-memory on the JVM / in tests). */
trait DesignStore:
  def list: List[LocalDesign] < Async
  def load(id: DesignId): Option[LocalDesign] < Async
  def save(design: LocalDesign): Unit < Async
  def delete(id: DesignId): Unit < Async

/** Resolves an image asset to a displayable URL (object URL of the local blob, built-in or server URL). */
trait AssetResolver:
  def resolve(asset: ImageAsset): Option[String] < Async

/** Optional server sync for signed-in customers (see docs/analysis/visual-editor-architecture.md §Sync). */
trait DesignSync:
  /** Pushes a local design; `Left(remote)` on a revision conflict with the server copy. */
  def push(design: LocalDesign): Either[DesignProject, DesignProject] < (Async & Abort[DesignError])
  def list: List[DesignSummary] < (Async & Abort[DesignError])
  def pull(id: DesignId): DesignProject < (Async & Abort[DesignError])

object InMemoryDesignStore:
  def make: DesignStore < Sync =
    AtomicRef.init(Map.empty[DesignId, LocalDesign]).map { ref =>
      new DesignStore:
        def list: List[LocalDesign] < Async             = ref.get.map(_.values.toList.sortBy(_.project.updatedAt).reverse)
        def load(id: DesignId): Option[LocalDesign] < Async = ref.get.map(_.get(id))
        def save(design: LocalDesign): Unit < Async     = ref.updateAndGet(_ + (design.project.id -> design)).unit
        def delete(id: DesignId): Unit < Async          = ref.updateAndGet(_ - id).unit
    }

object AssetResolver:
  /** Built-in assets resolve to their URL; device/uploaded assets need a platform resolver. */
  val builtinOnly: AssetResolver = new AssetResolver:
    def resolve(asset: ImageAsset): Option[String] < Async = asset.storage match
      case AssetStorage.Builtin(url) => Some(url)
      case _                         => None

/** Host integration: identity, callbacks and optional sync. */
final case class EditorHost(
    deviceId: String,
    store: DesignStore,
    assets: AssetResolver = AssetResolver.builtinOnly,
    sync: Option[DesignSync] = None,
    onClose: Option[() => Unit < Async] = None,
    onComplete: Option[EditorResult => Unit < Async] = None,
    /** i18n key of the confirm button ("Use this design", "Add to order" …). */
    completeLabelKey: String = "ve-complete-default",
    /** Signed-in customer (designs are then owned by the account and synced). */
    customerId: Option[CustomerId] = None,
):
  def owner: DesignOwner = customerId match
    case Some(c) => DesignOwner.Customer(c)
    case None    => DesignOwner.Device(NonEmptyString.unsafe(if deviceId.trim.isEmpty then "device" else deviceId.trim))

// ── Editor UI state values ──────────────────────────────────────────────────

enum EditorScreen derives CanEqual:
  case Home, Editing

enum LeftPanel derives CanEqual:
  case Templates, Text, Photos, Elements, Background, Layouts

enum SaveStatus derives CanEqual:
  case Idle
  case Dirty
  case Saving
  case Saved
  case Failed(message: String)

enum ZoomMode derives CanEqual:
  case Fit, Manual

/** Live (un-committed) frames while a pointer gesture is in progress. */
final case class GesturePreview(id: ElementId, start: Placement, current: Placement) derives CanEqual

/** What the pointer went down on (decoded by the browser bridge from `data-ve-*` attributes). */
enum CanvasTarget derives CanEqual:
  case Element(id: ElementId)
  case ResizeHandle(handle: Handle)
  case RotateHandle
  case Background

/** Keyboard / menu commands. */
enum EditorCommand derives CanEqual:
  case Undo, Redo, Delete, Duplicate, Copy, Paste, Deselect, SelectAll, NextPage, PrevPage, ZoomIn, ZoomOut, ZoomFit, Save
  case Nudge(dx: Double, dy: Double)

object EditorShortcuts:
  /** Maps a key press to an editor command (`key` is `KeyboardEvent.key`). */
  def resolve(key: String, ctrlOrMeta: Boolean, shift: Boolean, alt: Boolean): Option[EditorCommand] =
    val step = if shift then 10.0 else 1.0
    key match
      case "z" | "Z" if ctrlOrMeta && shift => Some(EditorCommand.Redo)
      case "z" | "Z" if ctrlOrMeta          => Some(EditorCommand.Undo)
      case "y" | "Y" if ctrlOrMeta          => Some(EditorCommand.Redo)
      case "d" | "D" if ctrlOrMeta          => Some(EditorCommand.Duplicate)
      case "c" | "C" if ctrlOrMeta          => Some(EditorCommand.Copy)
      case "v" | "V" if ctrlOrMeta          => Some(EditorCommand.Paste)
      case "a" | "A" if ctrlOrMeta          => Some(EditorCommand.SelectAll)
      case "s" | "S" if ctrlOrMeta          => Some(EditorCommand.Save)
      case "Delete" | "Backspace"           => Some(EditorCommand.Delete)
      case "Escape"                         => Some(EditorCommand.Deselect)
      case "ArrowLeft" if !ctrlOrMeta       => Some(EditorCommand.Nudge(-step, 0))
      case "ArrowRight" if !ctrlOrMeta      => Some(EditorCommand.Nudge(step, 0))
      case "ArrowUp" if !ctrlOrMeta         => Some(EditorCommand.Nudge(0, -step))
      case "ArrowDown" if !ctrlOrMeta       => Some(EditorCommand.Nudge(0, step))
      case "PageDown"                       => Some(EditorCommand.NextPage)
      case "PageUp"                         => Some(EditorCommand.PrevPage)
      case "+" | "=" if ctrlOrMeta          => Some(EditorCommand.ZoomIn)
      case "-" if ctrlOrMeta                => Some(EditorCommand.ZoomOut)
      case "0" if ctrlOrMeta                => Some(EditorCommand.ZoomFit)
      case _                                => None
