package mpbuilder.ui

import kyo.*
import mpbuilder.identity.sample.InMemoryOtpInbox
import mpbuilder.sample.SampleShowcaseRuntime
import mpbuilder.shared.{Currency, Language}
import mpbuilder.storefront.StorefrontFacade
import mpbuilder.ui.i18n.Translations
import mpbuilder.ui.port.StorefrontClient
import mpbuilder.ui.storefront.{AppRoute, StorefrontApp, StorefrontState}
import mpbuilder.ui.theme.AppTheme
import org.scalajs.dom
import scala.scalajs.js

/** Static client-side Storefront SPA entrypoint powered by Kyo UI + modular Scala 3 backend sample runtime. */
object Main extends KyoApp:

  private val LangStorageKey = "mpbuilder.lang"

  private def detectInitialLanguage: Language =
    val saved =
      try Option(dom.window.localStorage.getItem(LangStorageKey))
      catch case _: Throwable => None
    val raw = saved.orElse(Option(dom.window.navigator.language)).getOrElse("en").toLowerCase
    if raw.startsWith("cs") then Language.Cs else Language.En

  private def bootSampleClient: StorefrontClient < Async =
    val bootZio: zio.UIO[(StorefrontFacade, InMemoryOtpInbox)] =
      zio.ZIO.scoped {
        SampleShowcaseRuntime.layer().build.map { env =>
          (env.get[StorefrontFacade], env.get[InMemoryOtpInbox])
        }
      }
    Abort.run[Nothing](ZIOs.get(bootZio)).map { res =>
      val (facade, otpInbox) = res.getOrThrow
      StorefrontClient.fromZio(facade, otpInbox.codeFor)
    }

  run {
    val initialLang  = detectInitialLanguage
    val initialRoute = AppRoute.fromHash(dom.window.location.hash).getOrElse(AppRoute.Catalog)

    for
      client  <- bootSampleClient
      appI18n <- Translations.init(initialLang)
      _       <- UI.runStylesheet(AppTheme.storefrontStylesheet)
      _ <- I18n.let(appI18n.handle) {
             for
               state <- StorefrontState.init(
                          client = client,
                          i18n = appI18n,
                          initialCurrency = Currency.CZK,
                          initialRoute = initialRoute,
                          onRouteChange = r =>
                            Sync.defer {
                              val target = AppRoute.toHash(r)
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
                        AppRoute.fromHash(dom.window.location.hash).foreach { nextRoute =>
                          val eff = I18n.let(appI18n.handle)(state.navigate(nextRoute, syncHash = false))
                          val _   = Sync.Unsafe.evalOrThrow(Fiber.initUnscoped(eff))
                        }
                      dom.window.addEventListener("hashchange", listener)
                    }
               ui <- StorefrontApp.render(state)
               _  <- UI.runMount(ui, "#app-root")
               _  <- Async.never
             yield ()
           }
    yield ()
  }
