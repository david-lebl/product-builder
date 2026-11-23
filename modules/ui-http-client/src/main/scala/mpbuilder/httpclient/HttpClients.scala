package mpbuilder.httpclient

import mpbuilder.admin.port.BackofficeClient
import mpbuilder.ui.port.StorefrontClient

import scala.concurrent.ExecutionContext

/** Boot-time factory for the network-backed Kyo client ports (Phase 4, "Mode B").
  *
  * {{{
  *   val sf = HttpClients.storefront("https://api.printcraft.studio")
  * }}}
  */
object HttpClients:

  def storefrontFacade(baseUrl: String)(using ec: ExecutionContext = ExecutionContext.global): HttpStorefrontFacade =
    new HttpStorefrontFacade(HttpTransport(baseUrl))

  def backofficeFacade(baseUrl: String)(using ec: ExecutionContext = ExecutionContext.global): HttpBackofficeFacade =
    new HttpBackofficeFacade(HttpTransport(baseUrl))

  /** Storefront client over HTTP. `demoOtpCode` always yields `None` (codes are delivered out-of-band). */
  def storefront(baseUrl: String)(using ec: ExecutionContext = ExecutionContext.global): StorefrontClient =
    StorefrontClient.fromZio(storefrontFacade(baseUrl))

  def backoffice(baseUrl: String)(using ec: ExecutionContext = ExecutionContext.global): BackofficeClient =
    BackofficeClient.fromZio(backofficeFacade(baseUrl))
