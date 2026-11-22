package mpbuilder.ui

import kyo.*
import kyo.UI.*
import mpbuilder.admin.BackofficeApp
import mpbuilder.admin.i18n.AdminTranslations
import mpbuilder.admin.port.BackofficeClient
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.admin.theme.AdminTheme
import mpbuilder.backoffice.BackofficeFacade
import mpbuilder.identity.sample.InMemoryOtpInbox
import mpbuilder.sample.SampleShowcaseRuntime
import mpbuilder.shared.{Currency, Language}
import mpbuilder.storefront.StorefrontFacade
import mpbuilder.ui.components.{BrowserPickedFile, FileDropRegistry}
import mpbuilder.ui.port.StorefrontClient
import mpbuilder.ui.storefront.{AppRoute, StorefrontApp, StorefrontState}
import org.scalajs.dom
import scala.scalajs.js

/** Unified static client-side Demo SPA hosting BOTH Storefront (`#/catalog`, `#/builder`, ...)
  * AND Staff Backoffice (`#/admin/...`) over ONE shared in-memory `SampleShowcaseRuntime`.
  *
  * Orders placed in the Storefront land immediately in Staff Order Intake, and changes
  * made in Backoffice (order status, promos, B2B approvals, delivery settings) reflect live.
  */
object Main extends KyoApp:

  private val LangStorageKey = "mpbuilder.lang"

  private enum AppMode:
    case Storefront, Admin

  private def detectInitialLanguage: Language =
    val saved =
      try Option(dom.window.localStorage.getItem(LangStorageKey))
      catch case _: Throwable => None
    val raw = saved.orElse(Option(dom.window.navigator.language)).getOrElse("en").toLowerCase
    if raw.startsWith("cs") then Language.Cs else Language.En

  private def bootSharedClients: (StorefrontClient, BackofficeClient) < Async =
    val bootZio: zio.UIO[(StorefrontFacade, BackofficeFacade, InMemoryOtpInbox)] =
      zio.ZIO.scoped {
        SampleShowcaseRuntime.layer().build.map { env =>
          (env.get[StorefrontFacade], env.get[BackofficeFacade], env.get[InMemoryOtpInbox])
        }
      }
    Abort.run[Nothing](ZIOs.get(bootZio)).map { res =>
      val (sfFacade, boFacade, otpInbox) = res.getOrThrow
      (
        StorefrontClient.fromZio(sfFacade, otpInbox.codeFor),
        BackofficeClient.fromZio(boFacade),
      )
    }

  private def persistLanguage(lang: Language): Unit < Sync =
    Sync.defer {
      try dom.window.localStorage.setItem(LangStorageKey, lang.code)
      catch case _: Throwable => ()
    }

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
              dispatchPicked(
                zoneId,
                BrowserPickedFile(file.name, mime, size, blobUrl, Some(dataUrl)),
              ),
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
          // 1. Trigger file download
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

          // 2. Copy shareable artwork URL to clipboard
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

          // 3. Open native file picker when clicking a dropzone
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
    val initialLang       = detectInitialLanguage
    val initialHash       = dom.window.location.hash
    val initialAdminRoute = AdminRoute.fromHash(initialHash, requireAdminPrefix = true)
    val initialMode       = if initialAdminRoute.isDefined then AppMode.Admin else AppMode.Storefront
    val initialSfRoute    = AppRoute.fromHash(initialHash).getOrElse(AppRoute.Catalog)

    for
      clients <- bootSharedClients
      (sfClient, boClient) = clients
      appI18n <- AdminTranslations.init(initialLang)
      _       <- UI.runStylesheet(AdminTheme.adminStylesheet)
      _       <- Sync.defer(installMasterDetailSplitter())
      _       <- Sync.defer(installArtworkFileBridge(appI18n))
      modeRef <- Signal.initRef[AppMode](initialMode)
      _ <- I18n.let(appI18n.handle) {
             for
               sfState <- StorefrontState.init(
                            client = sfClient,
                            i18n = appI18n,
                            initialCurrency = Currency.CZK,
                            initialRoute = initialSfRoute,
                            onRouteChange = r =>
                              Sync.defer {
                                val target = AppRoute.toHash(r)
                                if dom.window.location.hash != target then
                                  dom.window.location.hash = target
                              },
                            onLanguagePersist = persistLanguage,
                          )
               boState <- BackofficeState.init(
                            client = boClient,
                            i18n = appI18n,
                            initialRoute = initialAdminRoute.getOrElse(AdminRoute.Dashboard),
                            onRouteChange = r =>
                              Sync.defer {
                                val target = AdminRoute.toHash(r, prefix = "#/admin")
                                if dom.window.location.hash != target then
                                  dom.window.location.hash = target
                              },
                            onLanguagePersist = persistLanguage,
                          )
               openAdmin: (() => Unit < Async) = () =>
                 for
                   _ <- modeRef.set(AppMode.Admin)
                   r <- boState.route.get
                   _ <- boState.navigate(r, syncHash = true)
                 yield ()
               backToStorefront: (() => Unit < Async) = () =>
                 for
                   _ <- modeRef.set(AppMode.Storefront)
                   r <- sfState.route.get
                   _ <- sfState.navigate(r, syncHash = true)
                   _ <- sfState.refreshOrders
                   _ <- sfState.configurator.recompute
                 yield ()
               _ <- Sync.defer {
                      import kyo.AllowUnsafe.embrace.danger
                      val listener: js.Function1[dom.HashChangeEvent, Unit] = _ =>
                        val hash = dom.window.location.hash
                        AdminRoute.fromHash(hash, requireAdminPrefix = true) match
                          case Some(adminRoute) =>
                            val eff = I18n.let(appI18n.handle) {
                              modeRef.set(AppMode.Admin).andThen(boState.navigate(adminRoute, syncHash = false))
                            }
                            val _ = Sync.Unsafe.evalOrThrow(Fiber.initUnscoped(eff))
                          case None =>
                            AppRoute.fromHash(hash).foreach { sfRoute =>
                              val eff = I18n.let(appI18n.handle) {
                                modeRef.set(AppMode.Storefront)
                                  .andThen(sfState.navigate(sfRoute, syncHash = false))
                                  .andThen(sfState.refreshOrders)
                              }
                              val _ = Sync.Unsafe.evalOrThrow(Fiber.initUnscoped(eff))
                            }
                      dom.window.addEventListener("hashchange", listener)
                    }
               sfUi <- StorefrontApp.render(sfState, onOpenAdmin = Some(openAdmin))
               boUi <- BackofficeApp.render(boState, onBackToStorefront = Some(backToStorefront))
               rootUi = modeRef.render {
                          case AppMode.Storefront => sfUi
                          case AppMode.Admin      => boUi
                        }
               _ <- UI.runMount(rootUi, "#app-root")
               _ <- Async.never
             yield ()
           }
    yield ()
  }
