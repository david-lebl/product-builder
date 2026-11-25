package mpbuilder.editor.browser

import kyo.*
import mpbuilder.design.*
import mpbuilder.editor.*
import mpbuilder.editor.theme.EditorTheme
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}

/** DOM glue of the Visual Editor (Scala.js only). Installed once per page; it talks to whichever
  * [[EditorController]] is attached to [[EditorBridge]].
  *
  * Responsibilities: pointer gestures on the canvas (with direct SVG transforms for 60 fps feedback and a
  * single commit on release), keyboard shortcuts, ctrl+wheel zoom, viewport measurement for "fit", photo
  * picking / drag-and-drop (hashing, measuring and storing originals in IndexedDB), dragging photos from the
  * panel onto frames, focusing the text box on double-click, and proof downloads.
  */
object BrowserEditor:

  import kyo.AllowUnsafe.embrace.danger

  private var installed = false

  private val U = DesignSvg.U

  // ── Styles & fonts ───────────────────────────────────────────────────────

  def installStyles(): Unit =
    if dom.document.getElementById("ve-styles") == null then
      val style = dom.document.createElement("style")
      style.id = "ve-styles"
      style.textContent = EditorTheme.css
      val _ = dom.document.head.appendChild(style)
      val link = dom.document.createElement("link").asInstanceOf[dom.HTMLLinkElement]
      link.rel = "stylesheet"
      link.href = FontCatalog.googleFontsCssUrl
      val _ = dom.document.head.appendChild(link)

  /** Stable anonymous device id (owner of designs created before signing in). */
  def deviceId: String =
    val key = "mpbuilder.deviceId"
    try
      Option(dom.window.localStorage.getItem(key)).filter(_.nonEmpty).getOrElse {
        val id = "dev-" + java.lang.Long.toString(java.lang.System.currentTimeMillis(), 36) + "-" + scala.util.Random.alphanumeric.take(8).mkString.toLowerCase
        dom.window.localStorage.setItem(key, id)
        id
      }
    catch case _: Throwable => "dev-session"

  // ── Helpers ──────────────────────────────────────────────────────────────

  private def controller: Option[EditorController] = EditorBridge.current

  private def evalSync[A](eff: A < Sync): A = Sync.Unsafe.evalOrThrow(eff)

  private def closest(target: dom.EventTarget, selector: String): Option[dom.Element] =
    target match
      case el: dom.Element => Option(el.closest(selector))
      case _               => None

  private def pageElement: Option[dom.Element] =
    Option(dom.document.querySelector("[data-ve-page]"))

  /** Pointer position in page millimetres (trim origin). */
  private def toMm(page: dom.Element, clientX: Double, clientY: Double): Point =
    val rect  = page.getBoundingClientRect()
    val parts = page.getAttribute("data-ve-page").split(' ').map(_.toDouble)
    val (bx, by, bw, bh) = (parts(0), parts(1), parts(2), parts(3))
    Point(bx + (clientX - rect.left) / math.max(1.0, rect.width) * bw, by + (clientY - rect.top) / math.max(1.0, rect.height) * bh)

  private def isEditable(target: dom.EventTarget): Boolean =
    target match
      case el: dom.HTMLElement =>
        val tag = el.tagName.toLowerCase
        tag == "input" || tag == "textarea" || tag == "select" || el.isContentEditable
      case _ => false

  private def editing(ctrl: EditorController): Boolean =
    dom.document.querySelector(".ve-workspace") != null && evalSync(ctrl.screen.get) == EditorScreen.Editing

  // ── Gesture state ────────────────────────────────────────────────────────

  private final case class Drag(pointerId: Double, saved: Map[String, Option[String]])
  private var drag: Option[Drag]                   = None
  private var pendingMove: Option[dom.PointerEvent] = None
  private var frameRequested                       = false

  /** Photo dragged from the Photos panel towards a frame on the canvas. */
  private final case class PhotoDrag(assetId: String, startX: Double, startY: Double, var moved: Boolean)
  private var photoDrag: Option[PhotoDrag] = None

  private def transformFor(p: GesturePreview): String =
    val s  = p.start
    val c  = p.current
    val sx = if s.width > 0 then c.width / s.width else 1.0
    val sy = if s.height > 0 then c.height / s.height else 1.0
    s"rotate(${c.rotation} ${c.centerX * U} ${c.centerY * U}) translate(${c.x * U} ${c.y * U}) scale($sx $sy) translate(${-s.x * U} ${-s.y * U})"

  private def elementNode(id: ElementId): Option[dom.Element] =
    pageElement.flatMap(p => Option(p.querySelector(s"""[data-ve-el="${id.value}"]""")))

  private def applyPreviews(previews: List[GesturePreview]): Unit =
    previews.foreach { p =>
      elementNode(p.id).foreach { node =>
        drag.foreach { d =>
          if !d.saved.contains(p.id.value) then drag = Some(d.copy(saved = d.saved + (p.id.value -> Option(node.getAttribute("transform")))))
        }
        node.setAttribute("transform", transformFor(p))
      }
    }

  /** Restores nodes the commit did not re-render (no-op gestures, locked elements). */
  private def scheduleRestore(saved: Map[String, Option[String]]): Unit =
    val nodes = saved.flatMap((id, orig) => elementNode(ElementId.unsafe(id)).map(_ -> orig))
    val _ = dom.window.setTimeout(
      () =>
        nodes.foreach { (node, orig) =>
          if node.isConnected then
            orig match
              case Some(t) => node.setAttribute("transform", t)
              case None    => node.removeAttribute("transform")
        },
      400,
    )

  private def flushMove(): Unit =
    frameRequested = false
    for
      ev   <- pendingMove
      ctrl <- controller
      page <- pageElement
    do
      pendingMove = None
      val previews = evalSync(ctrl.pointerMove(toMm(page, ev.clientX, ev.clientY), ev.shiftKey, ev.altKey))
      applyPreviews(previews)

  // ── Photo import ─────────────────────────────────────────────────────────

  private val AcceptedTypes = Set("image/jpeg", "image/png", "image/webp")

  private def hex(buf: ArrayBuffer): String =
    val bytes = new Uint8Array(buf)
    val sb    = new StringBuilder(bytes.length * 2)
    var i     = 0
    while i < bytes.length do
      val b = bytes(i).toInt & 0xff
      sb.append("0123456789abcdef".charAt(b >>> 4)).append("0123456789abcdef".charAt(b & 0x0f))
      i += 1
    sb.toString

  private def sha256(file: dom.File): js.Promise[Option[String]] =
    val subtle = dom.window.asInstanceOf[js.Dynamic].crypto.selectDynamic("subtle")
    if js.isUndefined(subtle) || subtle == null then js.Promise.resolve[Option[String]](None)
    else
      file.asInstanceOf[js.Dynamic].arrayBuffer().asInstanceOf[js.Promise[ArrayBuffer]].`then`[Option[String]]((buf: ArrayBuffer) =>
        subtle.digest("SHA-256", buf).asInstanceOf[js.Promise[ArrayBuffer]].`then`[Option[String]]((digest: ArrayBuffer) => Some(hex(digest)))
      )

  private def measure(url: String): js.Promise[(Int, Int)] =
    new js.Promise[(Int, Int)]((resolve, reject) => {
      val img = dom.document.createElement("img").asInstanceOf[dom.HTMLImageElement]
      img.onload = (_: dom.Event) => resolve((img.naturalWidth, img.naturalHeight))
      img.addEventListener("error", (_: dom.Event) => reject("decode failed"))
      img.src = url
    })

  /** Hashes, measures and stores each photo, then hands it to the controller (placing the first at `drop`). */
  def importFiles(files: Seq[dom.File], drop: Option[Point]): Unit =
    controller.foreach { ctrl =>
      files.filter(f => AcceptedTypes.contains(f.`type`)).zipWithIndex.foreach { (file, idx) =>
        val fut =
          for
            digest <- sha256(file).toFuture
            id      = AssetId.unsafe("img-" + digest.map(_.take(24)).getOrElse(s"${file.size.toLong}-${scala.util.Random.alphanumeric.take(10).mkString.toLowerCase}"))
            url     = IndexedDbStores.objectUrl(id, file)
            dims   <- measure(url).toFuture
            asset   = ImageAsset(id, file.name, file.`type`, dims._1, dims._2, file.size.toLong, digest, AssetStorage.DeviceOnly)
            _      <- (if IndexedDbStores.available then IndexedDbStores.putAsset(asset, file).toFuture.map(_ => ()) else scala.concurrent.Future.unit)
          yield (asset, url)
        fut.foreach((asset, url) => EditorBridge.runAsync(ctrl.addPhoto(asset, url, if idx == 0 then drop else None)))
        fut.failed.foreach { err =>
          dom.console.error(s"Photo import failed: $err")
          EditorBridge.runAsync(ctrl.toastMessage("ve-toast-upload-failed"))
        }
      }
    }

  private def pickFiles(): Unit =
    val input = dom.document.createElement("input").asInstanceOf[dom.HTMLInputElement]
    input.`type` = "file"
    input.accept = "image/jpeg,image/png,image/webp"
    input.multiple = true
    input.onchange = (_: dom.Event) =>
      if input.files != null then importFiles((0 until input.files.length).map(input.files(_)), None)
    input.click()

  // ── Listener installation ────────────────────────────────────────────────

  def install(): Unit =
    if !installed then
      installed = true

      dom.document.addEventListener(
        "pointerdown",
        (ev: dom.PointerEvent) =>
          for ctrl <- controller if ev.button == 0 do
            closest(ev.target, "[data-ve-photo]") match
              case Some(photo) if closest(ev.target, "[data-ve-canvas]").isEmpty =>
                photoDrag = Some(PhotoDrag(photo.getAttribute("data-ve-photo"), ev.clientX, ev.clientY, moved = false))
              case _ =>
                if closest(ev.target, "[data-ve-canvas]").isDefined then
                  pageElement.foreach { page =>
                    val point = toMm(page, ev.clientX, ev.clientY)
                    val target = closest(ev.target, "[data-ve-handle]") match
                      case Some(h) =>
                        val code = h.getAttribute("data-ve-handle")
                        if code == "rotate" then Some(CanvasTarget.RotateHandle)
                        else Handle.fromCode(code).map(CanvasTarget.ResizeHandle(_))
                      case None =>
                        closest(ev.target, "[data-ve-el]") match
                          case Some(el) => Some(CanvasTarget.Element(ElementId.unsafe(el.getAttribute("data-ve-el"))))
                          case None     => Some(CanvasTarget.Background)
                    target.foreach { t =>
                      evalSync(ctrl.pointerDown(t, point, ev.shiftKey))
                      if evalSync(ctrl.gestureActive) then
                        drag = Some(Drag(ev.pointerId, Map.empty))
                        ev.preventDefault()
                    }
                  }
      )

      dom.window.addEventListener(
        "pointermove",
        (ev: dom.PointerEvent) =>
          if drag.exists(_.pointerId == ev.pointerId) then
            pendingMove = Some(ev)
            if !frameRequested then
              frameRequested = true
              val _ = dom.window.requestAnimationFrame((_: Double) => flushMove())
          photoDrag.foreach { pd =>
            if !pd.moved && math.abs(ev.clientX - pd.startX) + math.abs(ev.clientY - pd.startY) > 6 then
              pd.moved = true
              dom.document.body.style.cursor = "copy"
          }
      )

      val finish: js.Function1[dom.PointerEvent, Unit] = (ev: dom.PointerEvent) =>
        drag.filter(_.pointerId == ev.pointerId).foreach { d =>
          drag = None
          pendingMove = None
          for
            ctrl <- controller
            page <- pageElement
          do
            if ev.`type` == "pointercancel" then evalSync(ctrl.pointerCancel)
            else evalSync(ctrl.pointerUp(toMm(page, ev.clientX, ev.clientY), ev.shiftKey, ev.altKey))
          scheduleRestore(d.saved)
        }
        photoDrag.foreach { pd =>
          photoDrag = None
          dom.document.body.style.cursor = ""
          if pd.moved && ev.`type` == "pointerup" then
            val over = dom.document.elementFromPoint(ev.clientX, ev.clientY)
            for
              ctrl <- controller
              page <- pageElement
              if over != null && over.closest("[data-ve-canvas]") != null
            do EditorBridge.runAsync(ctrl.placePhoto(AssetId.unsafe(pd.assetId), Some(toMm(page, ev.clientX, ev.clientY))))
        }
      dom.window.addEventListener("pointerup", finish)
      dom.window.addEventListener("pointercancel", finish)

      dom.document.addEventListener(
        "keydown",
        (ev: dom.KeyboardEvent) =>
          for ctrl <- controller if editing(ctrl) && !isEditable(ev.target) do
            EditorShortcuts.resolve(ev.key, ev.ctrlKey || ev.metaKey, ev.shiftKey, ev.altKey).foreach { cmd =>
              ev.preventDefault()
              EditorBridge.runAsync(ctrl.run(cmd))
            }
      )

      dom.document.addEventListener(
        "wheel",
        ((ev: dom.WheelEvent) =>
          if (ev.ctrlKey || ev.metaKey) && closest(ev.target, "[data-ve-canvas]").isDefined then
            controller.foreach { ctrl =>
              ev.preventDefault()
              evalSync(ctrl.zoomBy(if ev.deltaY < 0 then 1.1 else 1 / 1.1))
            }
        ): js.Function1[dom.WheelEvent, Unit],
        js.Dynamic.literal(passive = false).asInstanceOf[dom.EventListenerOptions],
      )

      dom.document.addEventListener(
        "dblclick",
        (ev: dom.MouseEvent) =>
          if closest(ev.target, "[data-ve-canvas]").isDefined && closest(ev.target, "[data-ve-el]").isDefined then
            val _ = dom.window.setTimeout(
              () =>
                Option(dom.document.querySelector("[data-ve-text-input]")).foreach { el =>
                  val ta = el.asInstanceOf[dom.HTMLTextAreaElement]
                  ta.focus()
                  ta.select()
                },
              80,
            )
      )

      dom.document.addEventListener(
        "click",
        (ev: dom.MouseEvent) =>
          if controller.isDefined then
            if closest(ev.target, "[data-ve-upload]").isDefined then pickFiles()
            closest(ev.target, "[data-ve-download]").foreach { btn =>
              val a = dom.document.createElement("a").asInstanceOf[dom.HTMLAnchorElement]
              a.href = btn.getAttribute("data-ve-download")
              a.setAttribute("download", Option(btn.getAttribute("data-ve-download-name")).getOrElse("design.svg"))
              dom.document.body.appendChild(a)
              a.click()
              dom.document.body.removeChild(a)
            }
      )

      val allowDrop: js.Function1[dom.DragEvent, Unit] = (ev: dom.DragEvent) =>
        if controller.isDefined && closest(ev.target, ".ve-root").isDefined then ev.preventDefault()
      dom.document.addEventListener("dragover", allowDrop)
      dom.document.addEventListener("dragenter", allowDrop)
      dom.document.addEventListener(
        "drop",
        (ev: dom.DragEvent) =>
          if controller.isDefined && closest(ev.target, ".ve-root").isDefined then
            ev.preventDefault()
            val files = ev.dataTransfer.files
            if files != null && files.length > 0 then
              val point =
                if closest(ev.target, "[data-ve-canvas]").isDefined then pageElement.map(p => toMm(p, ev.clientX, ev.clientY))
                else None
              importFiles((0 until files.length).map(files(_)), point)
      )

      // Viewport size drives "zoom to fit" (the canvas element is re-created when screens switch).
      var lastSize = (0.0, 0.0)
      val _ = dom.window.setInterval(
        () =>
          for
            ctrl   <- controller
            canvas <- Option(dom.document.querySelector("[data-ve-canvas]"))
          do
            val size = (canvas.clientWidth.toDouble, canvas.clientHeight.toDouble)
            if size != lastSize && size._1 > 0 && size._2 > 0 then
              lastSize = size
              evalSync(ctrl.setViewport(size._1, size._2))
        ,
        300,
      )
