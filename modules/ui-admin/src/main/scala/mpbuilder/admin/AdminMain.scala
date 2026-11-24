package mpbuilder.admin

import mpbuilder.ui.theme.{AppTheme, ThemeController, ThemeMode}
import kyo.*
import mpbuilder.admin.i18n.AdminTranslations
import mpbuilder.admin.port.BackofficeClient
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.admin.theme.AdminTheme
import mpbuilder.backoffice.BackofficeFacade
import mpbuilder.sample.SampleShowcaseRuntime
import mpbuilder.shared.Language
import mpbuilder.ui.components.{BrowserPickedFile, FileDropRegistry}
import org.scalajs.dom
import scala.scalajs.js

/** Standalone client-side Staff Backoffice SPA powered by Kyo UI + modular Scala 3 backend sample runtime. */
object AdminMain extends KyoApp:

  private val LangStorageKey = "mpbuilder.lang"

  private def detectInitialLanguage: Language =
    val saved =
      try Option(dom.window.localStorage.getItem(LangStorageKey))
      catch case _: Throwable => None
    val raw = saved.orElse(Option(dom.window.navigator.language)).getOrElse("en").toLowerCase
    if raw.startsWith("cs") then Language.Cs else Language.En

  /** Mode B (network API) when the host page sets `window.__API_BASE_URL__`; otherwise Mode A (static in-memory). */
  private def apiBaseUrl: Option[String] =
    try
      val v = dom.window.asInstanceOf[scala.scalajs.js.Dynamic].selectDynamic("__API_BASE_URL__")
      if scala.scalajs.js.isUndefined(v) || v == null then None else Option(v.toString.trim).filter(_.nonEmpty)
    catch case _: Throwable => None

  private def bootBackofficeClient: BackofficeClient < Async =
    apiBaseUrl match
      case Some(base) => Sync.defer(mpbuilder.httpclient.HttpClients.backoffice(base))
      case None       => bootInMemoryBackofficeClient

  private def bootInMemoryBackofficeClient: BackofficeClient < Async =
    val bootZio: zio.UIO[BackofficeFacade] =
      zio.ZIO.scoped {
        SampleShowcaseRuntime.layer().build.map(_.get[BackofficeFacade])
      }
    Abort.run[Nothing](ZIOs.get(bootZio)).map { res =>
      BackofficeClient.fromZio(res.getOrThrow)
    }

  /** Installs delegated pointer-drag resizing (plus double-click 50/50 reset) on any
    * Vaadin-style `[data-mp-dt-splitter]` divider inside a `[data-mp-dt-split-root]` container.
    */
  private def detectThemeMode: ThemeMode =
    try Option(dom.window.localStorage.getItem(AppTheme.ThemeStorageKey)).map(ThemeMode.fromCode).getOrElse(ThemeMode.System)
    catch case _: Throwable => ThemeMode.System

  /** Applies the theme to `<html data-theme>` (matching `AppTheme.themeStylesheet`) and persists the choice. */
  private def applyTheme(mode: ThemeMode): Unit < Sync =
    Sync.defer {
      dom.document.documentElement.setAttribute("data-theme", mode.code)
      try dom.window.localStorage.setItem(AppTheme.ThemeStorageKey, mode.code)
      catch case _: Throwable => ()
    }

  private def installDocumentCss(): Unit =
    val styleEl = dom.document.createElement("style")
    styleEl.textContent = AppTheme.documentCss
    dom.document.head.appendChild(styleEl)

  private def installMasterDetailSplitter(): Unit =
    val styleEl = dom.document.createElement("style")
    styleEl.textContent = "[data-mp-dt-splitter]{cursor:col-resize;user-select:none;touch-action:none;}"
    dom.document.head.appendChild(styleEl)

    var activeRoot: dom.HTMLElement   = null
    var activeMaster: dom.HTMLElement = null

    val onPointerMove: js.Function1[dom.PointerEvent, Unit] = ev =>
      if activeRoot != null && activeMaster != null then
        val rect = activeRoot.getBoundingClientRect()
        if rect.width > 0 then
          val rawPct = ((ev.clientX - rect.left) / rect.width) * 100.0
          val clamped = math.max(28.0, math.min(72.0, rawPct))
          activeMaster.style.setProperty("flex-basis", f"$clamped%.2f%%")

    val onPointerUp: js.Function1[dom.PointerEvent, Unit] = _ =>
      if activeRoot != null then
        activeRoot = null
        activeMaster = null
        dom.document.body.style.cursor = ""
        dom.document.body.style.asInstanceOf[js.Dynamic].userSelect = ""

    val onPointerDown: js.Function1[dom.PointerEvent, Unit] = ev =>
      ev.target match
        case el: dom.Element =>
          val splitter = el.closest("[data-mp-dt-splitter]")
          if splitter != null then
            val root = splitter.closest("[data-mp-dt-split-root]")
            if root != null then
              val master = root.querySelector("[data-mp-dt-master]")
              if master != null then
                ev.preventDefault()
                activeRoot = root.asInstanceOf[dom.HTMLElement]
                activeMaster = master.asInstanceOf[dom.HTMLElement]
                dom.document.body.style.cursor = "col-resize"
                dom.document.body.style.asInstanceOf[js.Dynamic].userSelect = "none"
        case _ => ()

    val onDblClick: js.Function1[dom.MouseEvent, Unit] = ev =>
      ev.target match
        case el: dom.Element =>
          val splitter = el.closest("[data-mp-dt-splitter]")
          if splitter != null then
            val root = splitter.closest("[data-mp-dt-split-root]")
            if root != null then
              val master = root.querySelector("[data-mp-dt-master]")
              if master != null then
                master.asInstanceOf[dom.HTMLElement].style.setProperty("flex-basis", "50%")
        case _ => ()

    dom.document.addEventListener("pointerdown", onPointerDown)
    dom.window.addEventListener("pointermove", onPointerMove)
    dom.window.addEventListener("pointerup", onPointerUp)
    dom.window.addEventListener("pointercancel", onPointerUp)
    dom.document.addEventListener("dblclick", onDblClick)

  private def installArtworkFileBridge(appI18n: mpbuilder.ui.i18n.AppI18n): Unit =
    import kyo.AllowUnsafe.embrace.danger

    def inferMime(file: dom.File): String =
      val raw = Option(file.`type`).map(_.trim).filter(_.nonEmpty)
      raw.getOrElse {
        val lower = file.name.toLowerCase
        if lower.endsWith(".pdf") then "application/pdf"
        else if lower.endsWith(".png") then "image/png"
        else if lower.endsWith(".jpg") || lower.endsWith(".jpeg") then "image/jpeg"
        else if lower.endsWith(".svg") then "image/svg+xml"
        else if lower.endsWith(".tif") || lower.endsWith(".tiff") then "image/tiff"
        else "application/octet-stream"
      }

    def dispatchPicked(zoneId: String, picked: BrowserPickedFile): Unit =
      val eff = I18n.let(appI18n.handle)(FileDropRegistry.dispatch(zoneId, picked))
      val _   = Sync.Unsafe.evalOrThrow(Fiber.initUnscoped(eff))

    def processFile(zoneId: String, file: dom.File): Unit =
      val mime    = inferMime(file)
      val blobUrl = dom.URL.createObjectURL(file)
      val size    = file.size.toLong.max(1L)
      if mime.startsWith("image/") then
        val reader = new dom.FileReader()
        reader.onload = _ =>
          val dataUrl = Option(reader.result).map(_.toString).filter(_.nonEmpty).getOrElse(blobUrl)
          val probe   = new dom.Image()
          probe.onload = _ =>
            dispatchPicked(
              zoneId,
              BrowserPickedFile(
                fileName = file.name,
                mimeType = mime,
                sizeBytes = size,
                downloadUrl = blobUrl,
                previewUrl = Some(dataUrl),
                pixelWidth = Option.when(probe.naturalWidth > 0)(probe.naturalWidth),
                pixelHeight = Option.when(probe.naturalHeight > 0)(probe.naturalHeight),
              ),
            )
          probe.addEventListener(
            "error",
            (_: dom.Event) =>
              dispatchPicked(zoneId, BrowserPickedFile(file.name, mime, size, blobUrl, Some(dataUrl))),
          )
          probe.src = dataUrl
        reader.onerror = _ =>
          dispatchPicked(zoneId, BrowserPickedFile(file.name, mime, size, blobUrl, Some(blobUrl)))
        reader.readAsDataURL(file)
      else
        dispatchPicked(zoneId, BrowserPickedFile(file.name, mime, size, blobUrl, Some(blobUrl)))

    def activeDropZone(target: dom.EventTarget): Option[(dom.Element, String)] =
      target match
        case el: dom.Element =>
          val zone = el.closest("[data-mp-dropzone]")
          if zone != null && zone.getAttribute("data-mp-dropzone-disabled") != "true" then
            Option(zone.getAttribute("data-mp-dropzone")).filter(_.nonEmpty).map(id => (zone, id))
          else None
        case _ => None

    val onDragOver: js.Function1[dom.DragEvent, Unit] = ev =>
      activeDropZone(ev.target).foreach { case (zone, _) =>
        ev.preventDefault()
        zone.classList.add("mp-dropzone-active")
      }

    val onDragLeave: js.Function1[dom.DragEvent, Unit] = ev =>
      activeDropZone(ev.target).foreach { case (zone, _) =>
        zone.classList.remove("mp-dropzone-active")
      }

    val onDrop: js.Function1[dom.DragEvent, Unit] = ev =>
      activeDropZone(ev.target).foreach { case (zone, zoneId) =>
        ev.preventDefault()
        zone.classList.remove("mp-dropzone-active")
        val files = ev.dataTransfer.files
        if files != null && files.length > 0 then
          processFile(zoneId, files(0))
      }

    val onClick: js.Function1[dom.MouseEvent, Unit] = ev =>
      ev.target match
        case el: dom.Element =>
          val dlBtn = el.closest("[data-mp-download-url]")
          if dlBtn != null then
            val url  = dlBtn.getAttribute("data-mp-download-url")
            val name = Option(dlBtn.getAttribute("data-mp-download-name")).filter(_.nonEmpty).getOrElse("artwork.pdf")
            if url != null && url.nonEmpty then
              val a = dom.document.createElement("a").asInstanceOf[dom.HTMLAnchorElement]
              a.href = url
              a.setAttribute("download", name)
              a.style.display = "none"
              dom.document.body.appendChild(a)
              a.click()
              dom.document.body.removeChild(a)

          val copyBtn = el.closest("[data-mp-copy-url]")
          if copyBtn != null then
            val url = copyBtn.getAttribute("data-mp-copy-url")
            if url != null && url.nonEmpty then
              try
                val clip = dom.window.navigator.asInstanceOf[js.Dynamic].clipboard
                if !js.isUndefined(clip) && clip != null then
                  val _ = clip.writeText(url)
                else
                  val ta = dom.document.createElement("textarea").asInstanceOf[dom.HTMLTextAreaElement]
                  ta.value = url
                  dom.document.body.appendChild(ta)
                  ta.select()
                  dom.document.execCommand("copy")
                  dom.document.body.removeChild(ta)
              catch case _: Throwable => ()

          activeDropZone(el).foreach { case (_, zoneId) =>
            val input = dom.document.createElement("input").asInstanceOf[dom.HTMLInputElement]
            input.`type` = "file"
            input.accept = ".pdf,.png,.jpg,.jpeg,.svg,.tif,.tiff,.ai,.eps"
            input.onchange = _ =>
              if input.files != null && input.files.length > 0 then
                processFile(zoneId, input.files(0))
            input.click()
          }
        case _ => ()

    dom.document.addEventListener("dragenter", onDragOver)
    dom.document.addEventListener("dragover", onDragOver)
    dom.document.addEventListener("dragleave", onDragLeave)
    dom.document.addEventListener("drop", onDrop)
    dom.document.addEventListener("click", onClick)

  run {
    val initialLang  = detectInitialLanguage
    val initialRoute = AdminRoute.fromHash(dom.window.location.hash, requireAdminPrefix = false).getOrElse(AdminRoute.Dashboard)

    for
      client  <- bootBackofficeClient
      appI18n <- AdminTranslations.init(initialLang)
      _       <- UI.runStylesheet(AdminTheme.adminStylesheet)
      _       <- Sync.defer(installDocumentCss())
      themeCtl <- ThemeController.init(detectThemeMode, applyTheme)
      _       <- Sync.defer(installMasterDetailSplitter())
      _       <- Sync.defer(installArtworkFileBridge(appI18n))
      _ <- I18n.let(appI18n.handle) {
             for
               state <- BackofficeState.init(
                          client = client,
                          i18n = appI18n,
                          initialRoute = initialRoute,
                          onRouteChange = r =>
                            Sync.defer {
                              val target = AdminRoute.toHash(r, prefix = "#")
                              if dom.window.location.hash != target then
                                dom.window.location.hash = target
                            },
                          onLanguagePersist = lang =>
                            Sync.defer {
                              try dom.window.localStorage.setItem(LangStorageKey, lang.code)
                              catch case _: Throwable => ()
                            },
                          theme = Some(themeCtl),
                        )
               _ <- Sync.defer {
                      import kyo.AllowUnsafe.embrace.danger
                      val listener: js.Function1[dom.HashChangeEvent, Unit] = _ =>
                        AdminRoute.fromHash(dom.window.location.hash, requireAdminPrefix = false).foreach { nextRoute =>
                          val eff = I18n.let(appI18n.handle)(state.navigate(nextRoute, syncHash = false))
                          val _   = Sync.Unsafe.evalOrThrow(Fiber.initUnscoped(eff))
                        }
                      dom.window.addEventListener("hashchange", listener)
                    }
               ui <- BackofficeApp.render(state, onBackToStorefront = None)
               _  <- UI.runMount(ui, "#app-root")
               _  <- Async.never
             yield ()
           }
    yield ()
  }
