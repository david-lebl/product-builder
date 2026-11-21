package mpbuilder.ordering.sample

import mpbuilder.ordering.*
import mpbuilder.ordering.core.{Cart, CartRepository, Order, OrderRepository, OrderingCoreLayers}
import mpbuilder.shared.*
import zio.*

/** In-memory `CartRepository` with convenience `initCart` for UI showcase sessions. */
final class InMemoryCartRepository(
    ref: Ref[Map[CartId, Cart]],
) extends CartRepository:

  override def findById(id: CartId): UIO[Option[Cart]] =
    ref.get.map(_.get(id))

  override def save(cart: Cart): UIO[Unit] =
    ref.update(_ + (cart.id -> cart))

  def createCart(cartId: CartId, currency: Currency, customerId: Option[CustomerId] = None): UIO[CartView] =
    val cart = Cart(cartId, customerId, currency, Nil, None)
    save(cart).as(cart.toView)

final class InMemoryOrderRepository(
    ref: Ref[Map[OrderId, Order]],
) extends OrderRepository:

  override def findById(id: OrderId): UIO[Option[Order]] =
    ref.get.map(_.get(id))

  override def save(order: Order): UIO[Unit] =
    ref.update(_ + (order.id -> order))

  def listAll: UIO[List[OrderView]] =
    ref.get.map(_.values.map(_.toView).toList.sortBy(_.placedAt))

object OrderingSampleLayer:

  def cartRepositoryLayer(
      seedCarts: List[(CartId, Currency, Option[CustomerId])] = List(
        (CartId.unsafe("cart-showcase-czk"), Currency.CZK, None),
        (CartId.unsafe("cart-showcase-usd"), Currency.USD, None),
      ),
  ): ULayer[InMemoryCartRepository] =
    ZLayer.fromZIO {
      val initial = seedCarts.map { case (id, cur, cid) => id -> Cart(id, cid, cur, Nil, None) }.toMap
      Ref.make(initial).map(new InMemoryCartRepository(_))
    }

  val orderRepositoryLayer: ULayer[InMemoryOrderRepository] =
    ZLayer.fromZIO(Ref.make(Map.empty[OrderId, Order]).map(new InMemoryOrderRepository(_)))

  val live: ZLayer[EventBus, Nothing, CartService & OrderService] =
    (cartRepositoryLayer() >>> OrderingCoreLayers.cartLayer) ++
      ((orderRepositoryLayer ++ ZLayer.service[EventBus]) >>> OrderingCoreLayers.orderLayer)
