package mpbuilder.admin

import kyo.*
import mpbuilder.admin.i18n.AdminTranslations
import mpbuilder.admin.port.BackofficeClient
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.admin.theme.AdminTheme
import mpbuilder.backoffice.BackofficeFacade
import mpbuilder.sample.SampleShowcaseRuntime
import mpbuilder.shared.Language
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

  private def bootBackofficeClient: BackofficeClient < Async =
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

  run {
    val initialLang  = detectInitialLanguage
    val initialRoute = AdminRoute.fromHash(dom.window.location.hash, requireAdminPrefix = false).getOrElse(AdminRoute.Dashboard)

    for
      client  <- bootBackofficeClient
      appI18n <- AdminTranslations.init(initialLang)
      _       <- UI.runStylesheet(AdminTheme.adminStylesheet)
      _       <- Sync.defer(installMasterDetailSplitter())
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
