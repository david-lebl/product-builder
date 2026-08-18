package mpbuilder.ui.productbuilder.basket

import mpbuilder.commons.*
import mpbuilder.catalog.model.ProductConfiguration
import mpbuilder.catalog.model.*
import mpbuilder.domain.pricing.{PriceCalculator, Pricelist}

import scala.concurrent.Future

/** The basket, entirely in the browser.
  *
  * What the embeddable calculator uses, and the default for any host that installs nothing else.
  * It prices with the same `PriceCalculator` the price preview already runs, so the widget keeps
  * working with the network switched off — the guarantee the whole `BuilderEnvironment` seam
  * exists to protect.
  *
  * Line ids are generated here. They are opaque to every caller, which is the point: the full SPA
  * gets its ids from the server and nothing downstream can tell the difference.
  */
final class LocalBasketBackend(pricelist: () => Pricelist) extends BasketBackend:

  private var lines: List[BasketLine] = Nil
  private var nextId: Int = 0

  def load(language: Language): Future[BasketOutcome] = Future.successful(current)

  def add(
      configuration: ProductConfiguration,
      quantity: Int,
      language: Language,
  ): Future[BasketOutcome] =
    if quantity <= 0 then Future.successful(invalidQuantity(quantity, language))
    else
      PriceCalculator.calculate(configuration, pricelist(), language).toEither match
        case Left(errors) =>
          Future.successful(BasketOutcome.Refused(errors.toList.map(_.message(language))))
        case Right(breakdown) =>
          nextId += 1
          lines = lines :+ BasketLine(
            id = s"local-$nextId",
            configuration = configuration,
            quantity = quantity,
            unitPrice = breakdown.total,
            lineTotal = (breakdown.total * quantity).rounded,
            currency = breakdown.currency,
          )
          Future.successful(current)

  def updateQuantity(lineId: String, quantity: Int, language: Language): Future[BasketOutcome] =
    if quantity <= 0 then Future.successful(invalidQuantity(quantity, language))
    else
      lines.find(_.id == lineId) match
        case None => Future.successful(lineNotFound(lineId, language))
        case Some(_) =>
          lines = lines.map { line =>
            if line.id == lineId then
              line.copy(quantity = quantity, lineTotal = (line.unitPrice * quantity).rounded)
            else line
          }
          Future.successful(current)

  def remove(lineId: String, language: Language): Future[BasketOutcome] =
    lines = lines.filterNot(_.id == lineId)
    Future.successful(current)

  def clear(language: Language): Future[BasketOutcome] =
    lines = Nil
    Future.successful(current)

  private def current: BasketOutcome =
    val currency = lines.headOption.map(_.currency).getOrElse(BasketSnapshot.empty.currency)
    BasketOutcome.Updated(
      BasketSnapshot(
        lines = lines,
        total = lines.map(_.lineTotal).foldLeft(Money.zero)(_ + _).rounded,
        currency = currency,
      )
    )

  private def invalidQuantity(quantity: Int, language: Language): BasketOutcome =
    BasketOutcome.Refused(
      List(language match
        case Language.En => s"Quantity $quantity is not valid — at least 1 is required"
        case Language.Cs => s"Množství $quantity není platné — je potřeba alespoň 1"
      )
    )

  private def lineNotFound(lineId: String, language: Language): BasketOutcome =
    BasketOutcome.Refused(
      List(language match
        case Language.En => s"No item '$lineId' in this basket"
        case Language.Cs => s"Položka '$lineId' v košíku není"
      )
    )
