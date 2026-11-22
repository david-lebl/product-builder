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
