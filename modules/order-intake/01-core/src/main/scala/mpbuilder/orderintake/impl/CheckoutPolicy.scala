package mpbuilder.orderintake
package impl

import mpbuilder.commons.*

/** Every checkout rule, as pure functions.
  *
  * Deliberately free of effects, for the same reason [[BasketPolicy]] is: which payment methods a
  * buyer may use, and how a discount and a delivery surcharge combine into a total, are the two
  * things most worth being able to test exhaustively and cheaply.
  */
private[orderintake] object CheckoutPolicy:

  // ── Payment ─────────────────────────────────────────────────────────────

  private val bankTransferName =
    LocalizedString("Bank transfer (QR code)", "Bankovní převod (QR kód)")
  private val cardName = LocalizedString("Card payment", "Platba kartou")
  private val invoiceName = LocalizedString("Invoice on account", "Faktura")

  private val cardNotBuilt = LocalizedString(
    "Card payment is not available yet",
    "Platba kartou zatím není k dispozici",
  )
  private val guestCannotInvoice = LocalizedString(
    "Invoicing is available to approved business accounts; sign in to use it",
    "Fakturace je dostupná schváleným firemním účtům; přihlaste se prosím",
  )
  private val accountNotApproved = LocalizedString(
    "This account is not approved for invoicing",
    "Tento účet nemá schválenou fakturaci",
  )

  /** What this buyer may pay with, and why not, for the rest.
    *
    * Every method appears in the answer whether it is on offer or not. Silently omitting one leaves
    * a customer who expected to be invoiced with nothing to read.
    */
  def paymentOffers(buyer: Buyer): List[PaymentOffer] =
    val invoice = buyer match
      case Buyer.Known(_, _, _, _, true) =>
        PaymentOffer.Available(PaymentMethod.InvoiceOnAccount, invoiceName)
      case Buyer.Known(_, _, _, _, false) =>
        PaymentOffer.Unavailable(PaymentMethod.InvoiceOnAccount, invoiceName, accountNotApproved)
      case Buyer.Guest =>
        PaymentOffer.Unavailable(PaymentMethod.InvoiceOnAccount, invoiceName, guestCannotInvoice)

    List(
      // Needs no integration and no trust, so it is the one method always on the table.
      PaymentOffer.Available(PaymentMethod.BankTransferQR, bankTransferName),
      // Described in the product spec, not yet built. Offering it would take an order that could
      // never be paid.
      PaymentOffer.Unavailable(PaymentMethod.Card, cardName, cardNotBuilt),
      invoice,
    )

  /** Check a chosen method against what is on offer, refusing it with the reason the customer was
    * already shown rather than a second, differently-worded one.
    */
  def checkPayment(buyer: Buyer, method: PaymentMethod): Either[CheckoutError, PaymentMethod] =
    paymentOffers(buyer)
      .collectFirst { case PaymentOffer.Unavailable(m, _, reason) if m == method => reason }
      .toLeft(method)
      .left
      .map(CheckoutError.PaymentNotOffered(method, _))

  // ── Totals ──────────────────────────────────────────────────────────────

  /** What comes off the goods total. A free-delivery code takes nothing off it — it waives the
    * surcharge instead — so it must not be counted twice.
    */
  def discountOff(decision: Option[DiscountDecision]): Money = decision match
    case Some(DiscountDecision.Applied(_, DiscountBenefit.Amount(off))) => off
    case _                                                              => Money.zero

  def waivesDelivery(decision: Option[DiscountDecision]): Boolean = decision match
    case Some(DiscountDecision.Applied(_, DiscountBenefit.FreeDelivery)) => true
    case _                                                               => false

  def charge(option: DeliveryOption, waived: Boolean): DeliveryCharge =
    DeliveryCharge(option.id, option.name, option.surcharge, waived)

  /** goods − discount, floored at zero, then + delivery.
    *
    * The floor matters: a fixed-amount code larger than a small basket would otherwise produce a
    * negative goods total that a delivery surcharge could partly hide.
    */
  def grandTotal(itemsTotal: Money, discountOff: Money, delivery: Option[DeliveryCharge]): Money =
    val goods = Money(itemsTotal.value - discountOff.value).atLeast(Money.zero)
    delivery.map(d => (goods + d.payable).rounded).getOrElse(goods.rounded)
