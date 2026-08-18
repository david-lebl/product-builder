package mpbuilder.ui.basket

import mpbuilder.commons.*
import mpbuilder.commons.json.given
import mpbuilder.product.model.ProductConfiguration
import mpbuilder.product.model.*
import mpbuilder.ui.productbuilder.basket.*
import org.scalajs.dom
import zio.json.*

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Thenable.Implicits.*

/** The basket, on the server.
  *
  * What the full SPA uses. Every operation is a request, and the answer is the whole basket — so
  * the browser never computes a total, and the number on screen is the number the shop quoted.
  * That is the difference this class exists to make: a basket that survives a reload, follows the
  * customer to another device, and is priced by the only party entitled to price it.
  */
final class HttpBasketBackend(baseUrl: String) extends BasketBackend:

  import HttpBasketBackend.*

  def load(language: Language): Future[BasketOutcome] =
    request(dom.HttpMethod.GET, "/current", None, language)

  def add(
      configuration: ProductConfiguration,
      quantity: Int,
      language: Language,
  ): Future[BasketOutcome] =
    val body = AddItemRequest(
      // Reduced back to a request rather than sent as the browser resolved it, so the catalog gets
      // to say whether this can still be built.
      configuration = ConfigurationRequests.json(configuration),
      quantity = quantity,
      speed = speedOf(configuration),
    )
    request(dom.HttpMethod.POST, "/current/items", Some(body.toJson), language)

  def updateQuantity(lineId: String, quantity: Int, language: Language): Future[BasketOutcome] =
    request(
      dom.HttpMethod.PATCH,
      s"/current/items/${encode(lineId)}",
      Some(UpdateQuantityRequest(quantity).toJson),
      language,
    )

  def remove(lineId: String, language: Language): Future[BasketOutcome] =
    request(dom.HttpMethod.DELETE, s"/current/items/${encode(lineId)}", None, language)

  def clear(language: Language): Future[BasketOutcome] =
    request(dom.HttpMethod.DELETE, "/current", None, language)

  // ── internals ───────────────────────────────────────────────────────────

  /** The production speed the customer chose, as the basket's vocabulary spells it.
    *
    * Carried inside the configuration's specifications rather than passed separately, because that
    * is where the configurator keeps it. Absent means Standard, which is also the server's default.
    */
  private def speedOf(configuration: ProductConfiguration): String =
    configuration.specifications.specs.values
      .collectFirst { case mpbuilder.product.model.SpecValue.ManufacturingSpeedSpec(s) => s.toString }
      .getOrElse("Standard")

  private def request(
      method: dom.HttpMethod,
      path: String,
      body: Option[String],
      language: Language,
  ): Future[BasketOutcome] =
    val headers = js.Dictionary[String](
      "Accept" -> "application/json",
      // The domain carries both languages; this is what stops the server picking one for us.
      "Accept-Language" -> language.toCode,
      // The SPA shops anonymously: its sign-in is still the mock OTP flow, not identity's real
      // tokens. The server already merges an anonymous basket into a customer's on login, so this
      // token is what that merge will name when `LoginWidget` is switched over.
      "X-Basket-Session" -> BasketSession.token,
    )
    body.foreach(_ => headers("Content-Type") = "application/json")

    val init = new dom.RequestInit {}
    init.method = method
    init.headers = headers
    body.foreach(init.body = _)

    dom
      .fetch(s"$baseUrl$path", init)
      .toFuture
      .flatMap(response => response.text().toFuture.map(text => (response.status, text)))
      .map { case (status, text) => interpret(status, text, language) }
      .recover { case error => BasketOutcome.Unavailable(describe(error, language)) }

  private def interpret(status: Int, text: String, language: Language): BasketOutcome =
    if status >= 200 && status < 300 then
      text.fromJson[BasketResponse].flatMap(toSnapshot) match
        case Right(snapshot) => BasketOutcome.Updated(snapshot)
        // A basket we cannot read is not a basket we may show. Reporting it as unavailable rather
        // than as an empty basket keeps a parsing bug from looking like a customer losing their
        // items.
        case Left(detail) => BasketOutcome.Unavailable(unreadable(detail, language))
    else
      text.fromJson[ErrorResponse] match
        case Right(errors) if errors.errors.nonEmpty =>
          BasketOutcome.Refused(errors.errors.map(_.message))
        case _ => BasketOutcome.Unavailable(s"HTTP $status")

  private def toSnapshot(response: BasketResponse): Either[String, BasketSnapshot] =
    response.items
      .foldRight(Right(Nil): Either[String, List[BasketLine]]) { (item, acc) =>
        for
          rest <- acc
          configuration <- ConfigurationPayloads.decode(item.configuration)
        yield BasketLine(
          id = item.id,
          configuration = configuration,
          quantity = item.quantity,
          // The server quotes the whole line; one run's worth is what the drawer labels "unit".
          unitPrice =
            if item.quantity > 0 then (item.price.total / BigDecimal(item.quantity)).rounded
            else item.price.total,
          lineTotal = item.price.total,
          currency = item.price.currency,
        ) :: rest
      }
      .map(lines => BasketSnapshot(lines, response.total, response.currency))

  private def encode(segment: String): String = js.URIUtils.encodeURIComponent(segment)

  private def unreadable(detail: String, language: Language): String = language match
    case Language.En => s"the basket could not be read ($detail)"
    case Language.Cs => s"košík nelze přečíst ($detail)"

  private def describe(error: Throwable, language: Language): String =
    Option(error.getMessage).filter(_.nonEmpty).getOrElse(language match
      case Language.En => "no connection"
      case Language.Cs => "bez připojení"
    )

object HttpBasketBackend:

  /** The basket API's wire shapes.
    *
    * Hand-written, and deliberately only the parts the configurator reads. They mirror
    * `orderintake.impl.http.BasketEndpoints`, which is JVM-only because it is built out of tapir —
    * the *configuration* payload, which is the large and volatile half of this format, is not
    * duplicated here at all: it comes from `catalog/01-core`, which is cross-compiled precisely so
    * that both sides can share it.
    */
  final case class PriceView(
      total: Money,
      currency: Currency,
      quotedAt: Timestamp,
      pricelistVersion: String,
  )

  final case class ItemView(
      id: String,
      description: Map[String, String],
      configuration: String,
      quantity: Int,
      speed: String,
      artworkId: Option[String],
      price: PriceView,
  )

  final case class BasketResponse(
      id: String,
      items: List[ItemView],
      total: Money,
      currency: Currency,
      itemCount: Int,
      expiresAt: Timestamp,
  )

  final case class ErrorItem(code: String, message: String)
  final case class ErrorResponse(errors: List[ErrorItem])

  final case class AddItemRequest(configuration: String, quantity: Int, speed: String)
  final case class UpdateQuantityRequest(quantity: Int)

  given JsonCodec[PriceView] = DeriveJsonCodec.gen
  given JsonCodec[ItemView] = DeriveJsonCodec.gen
  given JsonCodec[BasketResponse] = DeriveJsonCodec.gen
  given JsonCodec[ErrorItem] = DeriveJsonCodec.gen
  given JsonCodec[ErrorResponse] = DeriveJsonCodec.gen
  given JsonCodec[AddItemRequest] = DeriveJsonCodec.gen
  given JsonCodec[UpdateQuantityRequest] = DeriveJsonCodec.gen
