package mpbuilder.orderintake

import mpbuilder.commons.*
import mpbuilder.orderintake.impl.CheckoutPolicy
import zio.test.*

/** The checkout rules, tested directly.
  *
  * They are pure functions with no ports and no clock, so they can be enumerated rather than
  * sampled — which is the whole reason for keeping them out of the service.
  */
object CheckoutPolicySpec extends ZIOSpecDefault:

  private def corporate(canPayOnAccount: Boolean) = Buyer.Known(
    customerId = "cust-1",
    displayName = "Approved Corp",
    email = "buyer@approved.example",
    customerType = "RegisteredCorporate",
    canPayOnAccount = canPayOnAccount,
  )

  private val approved = corporate(canPayOnAccount = true)
  private val unapproved = corporate(canPayOnAccount = false)

  private def offer(buyer: Buyer, method: PaymentMethod): PaymentOffer =
    CheckoutPolicy
      .paymentOffers(buyer)
      .find(_.method == method)
      .getOrElse(throw new AssertionError(s"$method was not offered at all"))

  private def isAvailable(offer: PaymentOffer): Boolean = offer match
    case PaymentOffer.Available(_, _)      => true
    case PaymentOffer.Unavailable(_, _, _) => false

  def spec = suite("CheckoutPolicy")(
    suite("paymentOffers")(
      test("every method is listed for every buyer, offered or not") {
        // A method that simply vanished from the list would leave a customer expecting to be
        // invoiced with nothing to read.
        val everyone = List(Buyer.Guest, approved, unapproved)
        assertTrue(
          everyone.forall(b => CheckoutPolicy.paymentOffers(b).map(_.method).toSet == PaymentMethod.values.toSet)
        )
      },
      test("bank transfer is available to everyone, including guests") {
        assertTrue(
          isAvailable(offer(Buyer.Guest, PaymentMethod.BankTransferQR)),
          isAvailable(offer(approved, PaymentMethod.BankTransferQR)),
        )
      },
      test("card payment is refused, because it is not built") {
        // Offering it would take an order that could never be paid.
        assertTrue(!isAvailable(offer(Buyer.Guest, PaymentMethod.Card)))
      },
      test("invoicing is available only to an approved account") {
        assertTrue(
          isAvailable(offer(approved, PaymentMethod.InvoiceOnAccount)),
          !isAvailable(offer(unapproved, PaymentMethod.InvoiceOnAccount)),
          !isAvailable(offer(Buyer.Guest, PaymentMethod.InvoiceOnAccount)),
        )
      },
      test("a guest and an unapproved account are told different things") {
        // "Sign in to use it" and "this account is not approved" are different problems with
        // different fixes; one message for both would send half the customers the wrong way.
        val guestReason = offer(Buyer.Guest, PaymentMethod.InvoiceOnAccount) match
          case PaymentOffer.Unavailable(_, _, reason) => reason(Language.En)
          case _                                      => ""
        val accountReason = offer(unapproved, PaymentMethod.InvoiceOnAccount) match
          case PaymentOffer.Unavailable(_, _, reason) => reason(Language.En)
          case _                                      => ""
        assertTrue(guestReason.nonEmpty, accountReason.nonEmpty, guestReason != accountReason)
      },
      test("every reason is localized to both languages") {
        val reasons = List(Buyer.Guest, approved, unapproved).flatMap(CheckoutPolicy.paymentOffers).collect {
          case PaymentOffer.Unavailable(_, _, reason) => reason
        }
        assertTrue(
          reasons.nonEmpty,
          reasons.forall(r => r(Language.En).nonEmpty && r(Language.Cs).nonEmpty),
          reasons.forall(r => r(Language.En) != r(Language.Cs)),
        )
      },
    ),
    suite("checkPayment")(
      test("accepts a method that is on offer") {
        assertTrue(CheckoutPolicy.checkPayment(approved, PaymentMethod.InvoiceOnAccount).isRight)
      },
      test("refuses with the same reason the customer was already shown") {
        val shown = offer(Buyer.Guest, PaymentMethod.InvoiceOnAccount) match
          case PaymentOffer.Unavailable(_, _, reason) => Some(reason)
          case _                                      => None
        val refusal = CheckoutPolicy.checkPayment(Buyer.Guest, PaymentMethod.InvoiceOnAccount) match
          case Left(CheckoutError.PaymentNotOffered(_, reason)) => Some(reason)
          case _                                                => None
        assertTrue(refusal.isDefined, refusal == shown)
      },
    ),
    suite("totals")(
      test("a free-delivery code takes nothing off the goods") {
        val decision = Some(DiscountDecision.Applied("FREESHIP", DiscountBenefit.FreeDelivery))
        assertTrue(
          CheckoutPolicy.discountOff(decision) == Money.zero,
          CheckoutPolicy.waivesDelivery(decision),
        )
      },
      test("an amount code takes its amount off, and does not waive delivery") {
        val decision = Some(DiscountDecision.Applied("TENOFF", DiscountBenefit.Amount(Money(150))))
        assertTrue(
          CheckoutPolicy.discountOff(decision) == Money(150),
          !CheckoutPolicy.waivesDelivery(decision),
        )
      },
      test("a refused code changes nothing") {
        val decision =
          Some(DiscountDecision.Refused("NOPE", LocalizedString("No such code", "Kód neexistuje")))
        assertTrue(
          CheckoutPolicy.discountOff(decision) == Money.zero,
          !CheckoutPolicy.waivesDelivery(decision),
        )
      },
      test("delivery is added to the discounted goods") {
        val charge = DeliveryCharge("courier-standard", LocalizedString("Standard", "Standard"), Money(99), waived = false)
        assertTrue(
          CheckoutPolicy.grandTotal(Money(1000), Money(100), Some(charge)) == Money(999)
        )
      },
      test("a waived surcharge is not charged") {
        val charge = DeliveryCharge("courier-standard", LocalizedString("Standard", "Standard"), Money(99), waived = true)
        assertTrue(
          charge.payable == Money.zero,
          CheckoutPolicy.grandTotal(Money(1000), Money.zero, Some(charge)) == Money(1000),
        )
      },
      test("a discount larger than the basket floors the goods at zero") {
        // Without the floor a fixed-amount code could produce a negative goods total that a
        // delivery surcharge would partly hide, and the customer would be charged the difference.
        val charge = DeliveryCharge("courier-standard", LocalizedString("Standard", "Standard"), Money(99), waived = false)
        assertTrue(
          CheckoutPolicy.grandTotal(Money(50), Money(500), None) == Money.zero,
          CheckoutPolicy.grandTotal(Money(50), Money(500), Some(charge)) == Money(99),
        )
      },
    ),
  )
