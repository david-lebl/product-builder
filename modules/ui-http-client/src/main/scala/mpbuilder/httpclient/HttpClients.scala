package mpbuilder.httpclient

import mpbuilder.admin.port.BackofficeClient
import mpbuilder.ui.port.StorefrontClient

/** Boot-time factory for the network-backed Kyo client ports ("Mode B").
  *
  * {{{
  *   val sf = HttpClients.storefront("https://api.printcraft.studio")
  * }}}
  */
object HttpClients:

  def storefrontFacade(baseUrl: String, transport: HttpTransport = HttpTransport.default): HttpStorefrontFacade =
    new HttpStorefrontFacade(transport, baseUrl)

  def backofficeFacade(baseUrl: String, transport: HttpTransport = HttpTransport.default): HttpBackofficeFacade =
    new HttpBackofficeFacade(transport, baseUrl)

  /** Storefront client over HTTP. `demoOtpCode` always yields `None` (codes are delivered out-of-band). */
  def storefront(baseUrl: String): StorefrontClient =
    StorefrontClient.fromFacade(storefrontFacade(baseUrl))

  def backoffice(baseUrl: String): BackofficeClient =
    BackofficeClient.fromFacade(backofficeFacade(baseUrl))
