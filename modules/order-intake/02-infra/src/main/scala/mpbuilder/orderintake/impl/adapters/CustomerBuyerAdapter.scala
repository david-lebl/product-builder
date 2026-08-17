package mpbuilder.orderintake
package impl
package adapters

import mpbuilder.customers as cus
import zio.*

/** [[BuyerPort]] satisfied by calling the customers context's public service.
  *
  * Note what does not cross: negotiated pricing. Checkout never learns a customer's rates — it
  * passes the customer id to pricing and is told a number. That is what keeps a price list and a
  * customer record independently changeable.
  */
private[orderintake] final class CustomerBuyerAdapter(customers: cus.CustomerService)
    extends BuyerPort:

  def resolve(actor: Actor): IO[CheckoutError, Buyer] = actor match
    case Actor.Anonymous(_)                  => ZIO.succeed(Buyer.Guest)
    case Actor.Authenticated(_, None, _)     =>
      // Signed in, but no customer record linked — an identity without a business profile buys as
      // a guest rather than as a half-populated customer.
      ZIO.succeed(Buyer.Guest)
    case Actor.Authenticated(_, Some(id), _) =>
      customers
        .find(id)
        .mapError(e => CheckoutError.BuyerLookupFailed(e.message))
        .map {
          case Some(summary) => toBuyer(summary)
          // A customer id that resolves to nothing is a stale token, not a fault worth failing
          // the checkout over: they can still buy as a guest.
          case None => Buyer.Guest
        }

  private def toBuyer(summary: cus.CustomerSummary): Buyer =
    Buyer.Known(
      customerId = summary.id,
      displayName = summary.displayName,
      email = summary.email,
      customerType = summary.customerType,
      // Derived once, by customers, from the corporate-and-active rule. Re-deriving it here is how
      // the two would eventually disagree.
      canPayOnAccount = summary.canPayOnAccount,
    )

private[orderintake] object CustomerBuyerAdapter:
  val layer: URLayer[cus.CustomerService, BuyerPort] =
    ZLayer.fromFunction(new CustomerBuyerAdapter(_))
