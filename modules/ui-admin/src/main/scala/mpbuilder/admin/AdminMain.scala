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

  run {
    val initialLang  = detectInitialLanguage
    val initialRoute = AdminRoute.fromHash(dom.window.location.hash, requireAdminPrefix = false).getOrElse(AdminRoute.Dashboard)

    for
      client  <- bootBackofficeClient
      appI18n <- AdminTranslations.init(initialLang)
      _       <- UI.runStylesheet(AdminTheme.adminStylesheet)
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
