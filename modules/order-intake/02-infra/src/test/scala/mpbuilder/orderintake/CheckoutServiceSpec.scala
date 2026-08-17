package mpbuilder.orderintake

import mpbuilder.catalog.json.given
import mpbuilder.catalog as cat
import mpbuilder.commons.*
import mpbuilder.customers as cus
import mpbuilder.pricing as pri
import zio.*
import zio.json.*
import zio.test.*

/** Contract tests for [[CheckoutService]].
  *
  * Wired to [[Stubs]] for the same reason [[BasketServiceSpec]] is: this module cannot see any
  * other context's implementation, and specifying against the contracts is what keeps these tests
  * valid when those implementations are replaced.
  *
  * Every test uses its own session, so the shared basket store cannot leak between them.
  */
object CheckoutServiceSpec extends ZIOSpecDefault:

  private val services =
    ZLayer.succeed[cat.CatalogService](Stubs.StubCatalog()) ++
      ZLayer.succeed[pri.PricingService](Stubs.StubPricing()) ++
      ZLayer.succeed[cus.CustomerService](Stubs.StubCustomers()) >>>
      OrderIntakeModule.inMemory()

  /** A shop whose material was withdrawn *after* the basket was filled — configure still accepts
    * it, revalidate no longer does.
    */
  private val afterWithdrawal =
    ZLayer.succeed[cat.CatalogService](Stubs.StubCatalog(withdrawnLater = Set("mat-coated-300"))) ++
      ZLayer.succeed[pri.PricingService](Stubs.StubPricing()) ++
      ZLayer.succeed[cus.CustomerService](Stubs.StubCustomers()) >>>
      OrderIntakeModule.inMemory()

  private def configuration(quantity: Int = 500): String =
    cat
      .ConfigurationRequestDto(
        categoryId = "cat-business-cards",
        printingMethodId = "pm-digital",
        components = List(
          cat.ComponentRequestDto(
            role = "Main",
            materialId = "mat-coated-300",
            ink = cat.InkConfigurationDto(cat.InkSetupDto("CMYK", 4), cat.InkSetupDto("CMYK", 4)),
          )
        ),
        specifications = cat.SpecificationsDto(
          size = Some(cat.SizeDto(90, 55)),
          quantity = Some(quantity),
        ),
      )
      .toJson

  private def guest(name: String): Actor = Actor.Anonymous(s"checkout-$name")

  private def signedIn(name: String, customerId: Option[String]): Actor =
    Actor.Authenticated(s"user-$name", customerId, isStaff = false)

  private def fill(actor: Actor, copies: Int = 1, pieces: Int = 500) =
    BasketService.addItem(actor, AddItem(configuration(pieces), copies))

  /** The stub prices a 500-piece run at 10 each with a 20 % volume discount above 100 pieces:
    * 500 × 10 × 0.8. Stated as a number rather than derived, so a change in the stub shows up as a
    * failing expectation instead of quietly redefining what the tests are asserting.
    */
  private val onePack = Money(4000)

  private def paymentFor(options: CheckoutOptions, method: PaymentMethod): Option[PaymentOffer] =
    options.payment.find(_.method == method)

  private def available(offer: Option[PaymentOffer]): Boolean = offer match
    case Some(PaymentOffer.Available(_, _)) => true
    case _                                  => false

  def spec = suite("CheckoutService")(
    suite("options")(
      test("a guest may pay by bank transfer but not by invoice") {
        for
          actor <- ZIO.succeed(guest("options-guest"))
          options <- CheckoutService.options(actor)
        yield assertTrue(
          options.buyer == Buyer.Guest,
          available(paymentFor(options, PaymentMethod.BankTransferQR)),
          !available(paymentFor(options, PaymentMethod.InvoiceOnAccount)),
        )
      },
      test("an approved corporate customer may be invoiced") {
        for
          actor <- ZIO.succeed(signedIn("approved", Some(Stubs.StubCustomers.approvedId)))
          options <- CheckoutService.options(actor)
        yield assertTrue(
          available(paymentFor(options, PaymentMethod.InvoiceOnAccount)),
          options.buyer match
            case Buyer.Known(_, _, _, _, canPayOnAccount) => canPayOnAccount
            case Buyer.Guest                              => false,
        )
      },
      test("an account not approved for invoicing is told why") {
        for
          actor <- ZIO.succeed(signedIn("pending", Some(Stubs.StubCustomers.unapprovedId)))
          options <- CheckoutService.options(actor)
        yield assertTrue(paymentFor(options, PaymentMethod.InvoiceOnAccount) match
          case Some(PaymentOffer.Unavailable(_, _, reason)) =>
            reason(Language.En).nonEmpty && reason(Language.Cs) != reason(Language.En)
          case _ => false
        )
      },
      test("a signed-in user with no customer record buys as a guest") {
        // An identity without a business profile is not a half-populated customer.
        for
          actor <- ZIO.succeed(signedIn("no-profile", None))
          options <- CheckoutService.options(actor)
        yield assertTrue(options.buyer == Buyer.Guest)
      },
      test("a stale customer id does not break the checkout") {
        for
          actor <- ZIO.succeed(signedIn("stale", Some("cust-deleted")))
          options <- CheckoutService.options(actor)
        yield assertTrue(options.buyer == Buyer.Guest)
      },
      test("collecting the goods is free; couriers are not") {
        for options <- CheckoutService.options(guest("options-delivery"))
        yield
          val pickups = options.delivery.filter(_.kind == DeliveryKind.Pickup)
          val couriers = options.delivery.filter(_.kind == DeliveryKind.Courier)
          assertTrue(
            pickups.nonEmpty,
            couriers.nonEmpty,
            pickups.forall(_.surcharge == Money.zero),
            couriers.forall(_.surcharge.value > BigDecimal(0)),
          )
      },
    ),
    suite("quote")(
      test("an empty basket cannot be checked out") {
        for result <- CheckoutService.quote(guest("quote-empty"), CheckoutDraft()).flip
        yield assertTrue(result == CheckoutError.EmptyBasket)
      },
      test("prices the basket with no choices made yet") {
        // The running total has to be answerable before delivery or payment is chosen, or it would
        // appear only on the last step of the wizard.
        val actor = guest("quote-bare")
        for
          _ <- fill(actor)
          quote <- CheckoutService.quote(actor, CheckoutDraft())
        yield assertTrue(
          quote.itemsTotal == onePack,
          quote.grandTotal == onePack,
          quote.discount.isEmpty,
          quote.delivery.isEmpty,
          quote.payment.isEmpty,
          quote.lines.size == 1,
        )
      },
      test("a courier surcharge is added to the total") {
        val actor = guest("quote-courier")
        for
          _ <- fill(actor)
          quote <- CheckoutService.quote(actor, CheckoutDraft(deliveryOptionId = Some("courier-standard")))
        yield assertTrue(
          quote.delivery.map(_.optionId).contains("courier-standard"),
          quote.grandTotal == (onePack + Money(99)).rounded,
        )
      },
      test("collecting the goods adds nothing") {
        val actor = guest("quote-pickup")
        for
          _ <- fill(actor)
          quote <- CheckoutService.quote(actor, CheckoutDraft(deliveryOptionId = Some("shop-brno")))
        yield assertTrue(
          quote.delivery.map(_.surcharge).contains(Money.zero),
          quote.grandTotal == onePack,
        )
      },
      test("an unknown delivery option is refused") {
        val actor = guest("quote-bad-delivery")
        for
          _ <- fill(actor)
          result <- CheckoutService.quote(actor, CheckoutDraft(deliveryOptionId = Some("teleport"))).flip
        yield assertTrue(result == CheckoutError.UnknownDelivery("teleport"))
      },
      test("an amount discount comes off the goods") {
        val actor = guest("quote-discount")
        for
          _ <- fill(actor)
          quote <- CheckoutService.quote(actor, CheckoutDraft(discountCode = Some("TENOFF")))
        yield assertTrue(
          quote.discountOff == Money(400),
          quote.grandTotal == Money(3600),
          quote.discount.exists(_.isInstanceOf[DiscountDecision.Applied]),
        )
      },
      test("a free-delivery code waives the surcharge instead of the goods") {
        // The bug this shape exists to prevent: reported as an amount of zero, FREESHIP would
        // apply, change nothing, and look to the customer like a code that was accepted and ignored.
        val actor = guest("quote-freeship")
        for
          _ <- fill(actor)
          quote <- CheckoutService.quote(
            actor,
            CheckoutDraft(deliveryOptionId = Some("courier-express"), discountCode = Some("FREESHIP")),
          )
        yield assertTrue(
          quote.discountOff == Money.zero,
          quote.delivery.exists(_.waived),
          quote.delivery.map(_.surcharge).contains(Money(249)),
          quote.delivery.map(_.payable).contains(Money.zero),
          quote.grandTotal == onePack,
        )
      },
      test("a refused code is part of a successful answer") {
        val actor = guest("quote-bad-code")
        for
          _ <- fill(actor)
          quote <- CheckoutService.quote(actor, CheckoutDraft(discountCode = Some("NOTACODE")))
        yield assertTrue(
          quote.grandTotal == onePack,
          quote.discountOff == Money.zero,
          quote.discount match
            case Some(DiscountDecision.Refused(_, reason)) => reason(Language.En).nonEmpty
            case _                                         => false,
        )
      },
      test("a blank code is not offered at all") {
        val actor = guest("quote-blank-code")
        for
          _ <- fill(actor)
          quote <- CheckoutService.quote(actor, CheckoutDraft(discountCode = Some("   ")))
        yield assertTrue(quote.discount.isEmpty)
      },
      test("a payment method the buyer may not use is refused") {
        val actor = guest("quote-invoice")
        for
          _ <- fill(actor)
          result <- CheckoutService
            .quote(actor, CheckoutDraft(paymentMethod = Some("InvoiceOnAccount")))
            .flip
        yield assertTrue(result match
          case CheckoutError.PaymentNotOffered(method, _) => method == PaymentMethod.InvoiceOnAccount
          case _                                          => false
        )
      },
      test("a payment method the buyer may use is echoed back") {
        val actor = guest("quote-transfer")
        for
          _ <- fill(actor)
          quote <- CheckoutService.quote(actor, CheckoutDraft(paymentMethod = Some("BankTransferQR")))
        yield assertTrue(quote.payment.contains(PaymentMethod.BankTransferQR))
      },
      test("an unrecognised payment method is a bad request, not a refusal") {
        val actor = guest("quote-nonsense-payment")
        for
          _ <- fill(actor)
          result <- CheckoutService.quote(actor, CheckoutDraft(paymentMethod = Some("Cheque"))).flip
        yield assertTrue(result == CheckoutError.UnknownValue("paymentMethod", "Cheque"))
      },
      test("every line is priced, so quantity is reflected in the total") {
        val actor = guest("quote-two-lines")
        for
          _ <- fill(actor, copies = 2)
          quote <- CheckoutService.quote(actor, CheckoutDraft())
        yield assertTrue(quote.itemsTotal == Money(8000))
      },
      test("a line that can no longer be produced stops the checkout") {
        // The point of re-validating: a basket may have sat for weeks, and selling something the
        // shop cannot make is worse than losing the sale.
        val actor = guest("quote-withdrawn")
        val scenario = for
          _ <- fill(actor)
          result <- CheckoutService.quote(actor, CheckoutDraft()).flip
        yield assertTrue(result match
          case CheckoutError.LineNoLongerAvailable(_, problems) =>
            problems.exists(_.code == "MaterialNotFound")
          case _ => false
        )
        scenario.provide(afterWithdrawal)
      },
    ),
    suite("applyDiscount")(
      test("an empty basket has nothing to discount") {
        for result <- CheckoutService.applyDiscount(guest("discount-empty"), "TENOFF").flip
        yield assertTrue(result == CheckoutError.EmptyBasket)
      },
      test("answers about the code without repricing the basket") {
        val actor = guest("discount-only")
        for
          _ <- fill(actor)
          decision <- CheckoutService.applyDiscount(actor, "TENOFF")
        yield assertTrue(decision match
          case DiscountDecision.Applied(_, DiscountBenefit.Amount(off)) => off == Money(400)
          case _                                                        => false
        )
      },
      test("who is asking reaches the discount engine") {
        // VIPONLY is accepted only for a buyer with a customer id. If the buyer were not resolved
        // and passed through, a customer-restricted code could never be honoured — which is
        // precisely the bug the pricing contract change fixed.
        val anonymous = guest("discount-anon")
        val known = signedIn("discount-known", Some(Stubs.StubCustomers.approvedId))
        for
          _ <- fill(anonymous)
          _ <- fill(known)
          refused <- CheckoutService.applyDiscount(anonymous, "VIPONLY")
          applied <- CheckoutService.applyDiscount(known, "VIPONLY")
        yield assertTrue(
          refused.isInstanceOf[DiscountDecision.Refused],
          applied.isInstanceOf[DiscountDecision.Applied],
        )
      },
    ),
    suite("speedOffers")(
      test("reports the reason a tier cannot be sold, not merely its absence") {
        val actor = guest("speeds")
        for
          _ <- fill(actor)
          basket <- BasketService.current(actor)
          offers <- CheckoutService.speedOffers(actor, basket.items.head.id)
        yield assertTrue(
          offers.map(_.speed).toSet == ProductionSpeed.values.toSet,
          offers.exists {
            case SpeedOfferView.Unavailable(ProductionSpeed.Express, reason) =>
              reason(Language.En).nonEmpty && reason(Language.Cs) != reason(Language.En)
            case _ => false
          },
        )
      },
      test("the surcharge is for the whole line, not for one production run") {
        // The stub prices Economy at 5 less per run. Four runs are 20 less, and reporting the
        // per-run figure against the line would understate the difference by three quarters.
        val actor = guest("speeds-quantity")
        for
          added <- BasketService.addItem(actor, AddItem(configuration(), 4))
          offers <- CheckoutService.speedOffers(actor, added.items.head.id)
        yield assertTrue(
          offers.contains(SpeedOfferView.Available(ProductionSpeed.Economy, Money(-20))),
          offers.contains(SpeedOfferView.Available(ProductionSpeed.Standard, Money.zero)),
        )
      },
      test("an unknown line has no speeds") {
        for result <- CheckoutService.speedOffers(guest("speeds-missing"), "nope").flip
        yield assertTrue(result == CheckoutError.ItemNotFound("nope"))
      },
    ),
  ).provideShared(services)
