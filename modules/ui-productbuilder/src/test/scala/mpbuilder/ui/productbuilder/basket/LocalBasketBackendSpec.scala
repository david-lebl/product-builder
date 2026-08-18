package mpbuilder.ui.productbuilder.basket

import mpbuilder.commons.*
import mpbuilder.domain.model as dm
import mpbuilder.domain.sample.{SampleCatalog, SamplePricelist, SampleRules}
import mpbuilder.domain.service.{ConfigurationBuilder, ConfigurationRequest}
import zio.*
import zio.test.*

/** The basket with no server behind it.
  *
  * This is what the embeddable calculator ships with, and its promise is that the widget keeps
  * working with the network switched off. These tests are the promise written down: the same
  * operations the drawer performs, answered entirely in the browser.
  */
object LocalBasketBackendSpec extends ZIOSpecDefault:

  private def card(quantity: Int): dm.ProductConfiguration =
    ConfigurationBuilder
      .build(
        ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            dm.ComponentRequest(
              dm.ComponentRole.Main,
              SampleCatalog.coated300gsmId,
              dm.InkConfiguration.cmyk4_4,
              Nil,
            )
          ),
          specs = List(
            dm.SpecValue.SizeSpec(Dimension(90, 55)),
            dm.SpecValue.QuantitySpec(Quantity.unsafe(quantity)),
          ),
        ),
        SampleCatalog.catalog,
        SampleRules.ruleset,
        dm.ConfigurationId.unsafe(s"card-$quantity"),
      )
      .toEither
      .fold(errors => throw new AssertionError(s"fixture did not build: $errors"), identity)

  private def backend = LocalBasketBackend(() => SamplePricelist.pricelistCzkSheet)

  private def basketOf(outcome: BasketOutcome): BasketSnapshot = outcome match
    case BasketOutcome.Updated(basket) => basket
    case other                         => throw new AssertionError(s"expected a basket, got $other")

  def spec = suite("LocalBasketBackend")(
    test("starts empty") {
      for outcome <- ZIO.fromFuture(_ => backend.load(Language.En))
      yield assertTrue(basketOf(outcome).isEmpty, basketOf(outcome).total == Money.zero)
    },
    test("prices what it adds") {
      val local = backend
      for
        outcome <- ZIO.fromFuture(_ => local.add(card(500), 1, Language.En))
        basket = basketOf(outcome)
      yield assertTrue(
        basket.size == 1,
        basket.lines.head.unitPrice.value > BigDecimal(0),
        basket.lines.head.lineTotal == basket.lines.head.unitPrice,
        basket.total == basket.lines.head.lineTotal,
      )
    },
    test("the line total follows the quantity") {
      val local = backend
      for
        added <- ZIO.fromFuture(_ => local.add(card(500), 1, Language.En))
        lineId = basketOf(added).lines.head.id
        unit = basketOf(added).lines.head.unitPrice
        updated <- ZIO.fromFuture(_ => local.updateQuantity(lineId, 4, Language.En))
        basket = basketOf(updated)
      yield assertTrue(
        basket.lines.head.quantity == 4,
        basket.lines.head.lineTotal == (unit * 4).rounded,
        basket.total == (unit * 4).rounded,
      )
    },
    test("two products are two lines, and the total is their sum") {
      val local = backend
      for
        _ <- ZIO.fromFuture(_ => local.add(card(500), 1, Language.En))
        outcome <- ZIO.fromFuture(_ => local.add(card(250), 2, Language.En))
        basket = basketOf(outcome)
      yield assertTrue(
        basket.size == 2,
        basket.total == basket.lines.map(_.lineTotal).foldLeft(Money.zero)(_ + _).rounded,
      )
    },
    test("every line has its own id") {
      // Two identical products are two lines here, and the drawer has to be able to remove one of
      // them without removing the other.
      val local = backend
      for
        _ <- ZIO.fromFuture(_ => local.add(card(500), 1, Language.En))
        outcome <- ZIO.fromFuture(_ => local.add(card(500), 1, Language.En))
        basket = basketOf(outcome)
      yield assertTrue(basket.lines.map(_.id).distinct.size == 2)
    },
    test("removing one line leaves the rest") {
      val local = backend
      for
        first <- ZIO.fromFuture(_ => local.add(card(500), 1, Language.En))
        _ <- ZIO.fromFuture(_ => local.add(card(250), 1, Language.En))
        outcome <- ZIO.fromFuture(_ => local.remove(basketOf(first).lines.head.id, Language.En))
        basket = basketOf(outcome)
      yield assertTrue(basket.size == 1, basket.total == basket.lines.head.lineTotal)
    },
    test("clearing empties it") {
      val local = backend
      for
        _ <- ZIO.fromFuture(_ => local.add(card(500), 1, Language.En))
        outcome <- ZIO.fromFuture(_ => local.clear(Language.En))
      yield assertTrue(basketOf(outcome).isEmpty, basketOf(outcome).total == Money.zero)
    },
    test("a quantity of zero is refused, in the caller's language") {
      val local = backend
      for
        english <- ZIO.fromFuture(_ => local.add(card(500), 0, Language.En))
        czech <- ZIO.fromFuture(_ => local.add(card(500), 0, Language.Cs))
      yield assertTrue(
        english match
          case BasketOutcome.Refused(problems) => problems.nonEmpty
          case _                               => false,
        (english, czech) match
          case (BasketOutcome.Refused(en), BasketOutcome.Refused(cs)) => en != cs
          case _                                                      => false,
      )
    },
    test("changing an unknown line is refused rather than silently ignored") {
      for outcome <- ZIO.fromFuture(_ => backend.updateQuantity("no-such-line", 3, Language.En))
      yield assertTrue(outcome.isInstanceOf[BasketOutcome.Refused])
    },
  )
