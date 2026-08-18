package mpbuilder.domain.model

import zio.prelude.Validation

/** The ids still owned by the legacy module.
  *
  * Catalog's five moved to `mpbuilder.product.model` when that context was extracted; each of
  * these follows its own context — order and basket to order-intake, customer to customers,
  * discount to pricing, artwork to artwork.
  */

opaque type BasketId = String
object BasketId:
  def apply(value: String): Validation[String, BasketId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("BasketId must not be empty")

  def unsafe(value: String): BasketId = value

  extension (id: BasketId) def value: String = id

opaque type OrderId = String
object OrderId:
  def apply(value: String): Validation[String, OrderId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("OrderId must not be empty")

  def unsafe(value: String): OrderId = value

  extension (id: OrderId) def value: String = id

opaque type CustomerId = String
object CustomerId:
  def apply(value: String): Validation[String, CustomerId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("CustomerId must not be empty")

  def unsafe(value: String): CustomerId = value

  extension (id: CustomerId) def value: String = id


opaque type ArtworkId = String
object ArtworkId:
  def apply(value: String): Validation[String, ArtworkId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("ArtworkId must not be empty")

  def unsafe(value: String): ArtworkId = value

  def generate(): ArtworkId = s"artwork-${System.currentTimeMillis()}-${scala.util.Random.nextInt(10000)}"

  extension (id: ArtworkId) def value: String = id
