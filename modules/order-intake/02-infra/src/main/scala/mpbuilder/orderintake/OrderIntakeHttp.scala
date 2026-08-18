package mpbuilder.orderintake

import mpbuilder.identity as id
import mpbuilder.orderintake.impl.http.{
  BasketEndpoints,
  BasketRoutes,
  CheckoutEndpoints,
  CheckoutRoutes,
  OrderEndpoints,
  OrderRoutes,
}
import sttp.tapir.AnyEndpoint
import sttp.tapir.server.ServerEndpoint
import zio.Task

/** The order-intake context's HTTP surface, as the composition root sees it. */
object OrderIntakeHttp:

  def routes(
      baskets: BasketService,
      checkout: CheckoutService,
      orders: OrderService,
      auth: id.AuthService,
  ): List[ServerEndpoint[Any, Task]] =
    BasketRoutes(baskets, auth).endpoints ++
      CheckoutRoutes(checkout, auth).endpoints ++
      OrderRoutes(orders, auth).endpoints

  val endpoints: List[AnyEndpoint] =
    BasketEndpoints.all ++ CheckoutEndpoints.all ++ OrderEndpoints.all
