package mpbuilder.ui.productbuilder.basket

import mpbuilder.commons.*
import mpbuilder.product.model.ProductConfiguration
import mpbuilder.product.model.*

import scala.concurrent.Future

/** One line of the basket, as the configurator renders it.
  *
  * Carries the resolved [[ProductConfiguration]] because that is what the drawer, the checkout
  * summary and the order e-mail actually display — category, materials, finishes, specs. The
  * server hands it back verbatim, so a page reload rebuilds exactly what was there.
  *
  * `id` is the line's identity, and it comes from whoever owns the basket. Lines used to be keyed
  * by `ConfigurationId`, which stopped working the moment the server became the owner: a stored
  * configuration is a *value*, and two identical products deliberately share one.
  */
final case class BasketLine(
    id: String,
    configuration: ProductConfiguration,
    quantity: Int,
    /** What one production run of this configuration costs. */
    unitPrice: Money,
    /** `unitPrice` × `quantity`, as the owner of the basket computed it. */
    lineTotal: Money,
    currency: Currency,
)

/** The basket as last seen. A value, not a handle: every operation returns a whole new one. */
final case class BasketSnapshot(
    lines: List[BasketLine],
    total: Money,
    currency: Currency,
):
  def isEmpty: Boolean = lines.isEmpty
  def nonEmpty: Boolean = lines.nonEmpty
  def size: Int = lines.size
  def find(lineId: String): Option[BasketLine] = lines.find(_.id == lineId)

object BasketSnapshot:
  /** CZK because that is the shop's currency and the only pricelist the configurator loads; an
    * empty basket has no items to derive one from.
    */
  val empty: BasketSnapshot = BasketSnapshot(Nil, Money.zero, Currency.CZK)

/** What came back from a basket operation.
  *
  * Three outcomes rather than two, because "the shop refused this" and "the shop could not be
  * reached" call for different words and different advice, and collapsing them would tell a
  * customer with a dropped connection that their product is invalid.
  */
enum BasketOutcome:
  case Updated(basket: BasketSnapshot)

  /** The basket owner refused, with every reason it found, already in the caller's language. */
  case Refused(problems: List[String])

  /** The basket could not be reached or answered unintelligibly. Never the customer's fault. */
  case Unavailable(detail: String)

/** Where the basket lives.
  *
  * The seam that lets the same configurator run against a server in the full SPA and entirely in
  * memory in the embeddable calculator. Everything is asynchronous because one of the two
  * implementations genuinely is; making the offline one pretend to be async costs nothing, whereas
  * making the online one pretend to be synchronous is impossible.
  *
  * `language` is passed per call rather than held, so a customer who switches language sees the
  * next problem in the language they just chose.
  */
trait BasketBackend:
  def load(language: Language): Future[BasketOutcome]
  def add(configuration: ProductConfiguration, quantity: Int, language: Language): Future[BasketOutcome]
  def updateQuantity(lineId: String, quantity: Int, language: Language): Future[BasketOutcome]
  def remove(lineId: String, language: Language): Future[BasketOutcome]
  def clear(language: Language): Future[BasketOutcome]
