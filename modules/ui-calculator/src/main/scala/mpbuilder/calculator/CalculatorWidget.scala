package mpbuilder.calculator

import kyo.*
import mpbuilder.identity.sample.InMemoryOtpInbox
import mpbuilder.sample.SampleShowcaseRuntime
import mpbuilder.shared.{CategoryId, Currency, Language}
import mpbuilder.storefront.StorefrontFacade
import mpbuilder.ui.calculator.{CalculatorConfig, CalculatorView}
import mpbuilder.ui.i18n.Translations
import mpbuilder.ui.port.StorefrontClient
import mpbuilder.ui.theme.AppTheme
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.URIUtils
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/** Embeddable Standalone Print Price Calculator Widget.
  *
  * Preserves the public JS API:
  * {{{
  *   MPCalculator.mount("#calculator-root", {
  *     lang: "cs",          // "en" | "cs" (optional)
  *     currency: "CZK",     // "CZK" | "USD" (optional)
  *     category: "cat-business-cards", // optional CategoryId
  *     orderEmail: "orders@printshop.example", // optional
  *     apiBaseUrl: "https://api.printshop.example" // optional — live pricing over HTTP
  *   })
  * }}}
  *
  * Without `apiBaseUrl` (and without a global `window.__API_BASE_URL__`) the widget prices
  * fully client-side with the bundled in-memory sample runtime.
  * {{{
  * }}}
  */
@JSExportTopLevel("MPCalculator")
object CalculatorWidget:

  private var stylesheetInjected: Boolean = false

  private def parseConfig(raw: js.UndefOr[js.Dynamic]): CalculatorConfig =
    raw.toOption match
      case None => CalculatorConfig()
      case Some(cfg) =>
        def strField(name: String): Option[String] =
          val v = cfg.selectDynamic(name)
          if js.isUndefined(v) || v == null then None
          else Option(v.toString.trim).filter(_.nonEmpty)

        val lang = strField("lang").map(_.toLowerCase) match
          case Some("cs") => Language.Cs
          case Some("en") => Language.En
          case _          => Language.En

        val currency = strField("currency").map(_.toUpperCase) match
          case Some("USD") => Currency.USD
          case Some("EUR") => Currency.EUR
          case Some("GBP") => Currency.GBP
          case _           => Currency.CZK

        val category = strField("category").map(CategoryId.unsafe)
        val email    = strField("orderEmail")

        CalculatorConfig(
          initialLanguage = lang,
          initialCurrency = currency,
          initialCategoryId = category,
          orderEmail = email,
        )

  /** `apiBaseUrl` mount option, falling back to a page-wide `window.__API_BASE_URL__`. */
  private def apiBaseUrl(raw: js.UndefOr[js.Dynamic]): Option[String] =
    def clean(v: js.Dynamic): Option[String] =
      if js.isUndefined(v) || v == null then None else Option(v.toString.trim).filter(_.nonEmpty)
    raw.toOption.flatMap(cfg => clean(cfg.selectDynamic("apiBaseUrl")))
      .orElse(clean(dom.window.asInstanceOf[js.Dynamic].selectDynamic("__API_BASE_URL__")))

  private def bootClient(raw: js.UndefOr[js.Dynamic]): StorefrontClient < Async =
    apiBaseUrl(raw) match
      case Some(base) => Sync.defer(mpbuilder.httpclient.HttpClients.storefront(base))
      case None       => bootSampleClient

  private def bootSampleClient: StorefrontClient < Async =
    SampleShowcaseRuntime.make().map(rt => StorefrontClient.fromFacade(rt.storefront, rt.otpInbox.codeFor))

  private def copyToClipboard(text: String): Unit < Sync =
    Sync.defer {
      try
        val nav = dom.window.navigator.asInstanceOf[js.Dynamic]
        if !js.isUndefined(nav.clipboard) then
          val _ = nav.clipboard.writeText(text)
      catch case _: Throwable => ()
    }

  private def openMailto(to: String, subject: String, body: String): Unit < Sync =
    Sync.defer {
      val encSubj = URIUtils.encodeURIComponent(subject)
      val encBody = URIUtils.encodeURIComponent(body)
      dom.window.location.href = s"mailto:$to?subject=$encSubj&body=$encBody"
    }

  @JSExport
  def mount(selector: String, config: js.UndefOr[js.Dynamic] = js.undefined): Unit =
    val parsed = parseConfig(config)

    val program: Unit < Async =
      Scope.run {
        for
          client  <- bootClient(config)
          appI18n <- Translations.init(parsed.initialLanguage)
          _ <- {
                 if !stylesheetInjected then
                   Sync.defer { stylesheetInjected = true }.andThen(UI.runStylesheet(AppTheme.calculatorStylesheet))
                 else Kyo.unit
               }
          _ <- I18n.let(appI18n.handle) {
                 for
                   ui <- CalculatorView.render(
                           client = client,
                           i18n = appI18n,
                           config = parsed,
                           onCopyText = copyToClipboard,
                           onOpenMailto = openMailto,
                         )
                   _ <- UI.runMount(ui, selector)
                   _ <- Async.never
                 yield ()
               }
        yield ()
      }

    import kyo.AllowUnsafe.embrace.danger
    val _ = Sync.Unsafe.evalOrThrow(Fiber.initUnscoped(program))
