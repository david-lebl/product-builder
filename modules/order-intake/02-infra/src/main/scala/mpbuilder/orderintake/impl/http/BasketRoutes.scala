package mpbuilder.orderintake
package impl
package http

import mpbuilder.commons.*
import mpbuilder.identity as id
import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import sttp.tapir.server.ServerEndpoint
import zio.*

import BasketEndpoints.*

/** Server logic for the basket endpoints. */
private[orderintake] final class BasketRoutes(baskets: BasketService, auth: id.AuthService):

  private val actors = Actors(auth)

  def endpoints: List[ServerEndpoint[Any, Task]] = List(
    // tapir flattens tuple inputs, so the auth headers arrive as separate arguments rather than
    // as the pair they were declared as.
    BasketEndpoints.current.zServerLogic { case (bearer, session) =>
      withActor(bearer, session)(baskets.current)
    },
    BasketEndpoints.addItem.zServerLogic { case (bearer, session, req) =>
      withActor(bearer, session)(actor =>
        baskets.addItem(actor, AddItem(req.configuration, req.quantity, req.speed, req.artworkId))
      )
    },
    BasketEndpoints.updateQuantity.zServerLogic { case (bearer, session, itemId, req) =>
      withActor(bearer, session)(baskets.updateQuantity(_, itemId, UpdateQuantity(req.quantity)))
    },
    BasketEndpoints.changeSpeed.zServerLogic { case (bearer, session, itemId, req) =>
      withActor(bearer, session)(baskets.changeSpeed(_, itemId, UpdateSpeed(req.speed)))
    },
    BasketEndpoints.removeItem.zServerLogic { case (bearer, session, itemId) =>
      withActor(bearer, session)(baskets.removeItem(_, itemId))
    },
    BasketEndpoints.clear.zServerLogic { case (bearer, session) =>
      withActor(bearer, session)(baskets.clear)
    },
    BasketEndpoints.requote.zServerLogic { case (bearer, session) =>
      withActor(bearer, session)(baskets.requote)
    },
    BasketEndpoints.merge.zServerLogic { case (bearer, session, req) =>
      withActor(bearer, session)(baskets.merge(_, req.fromSession))
    },
  )

  private def withActor(bearer: Option[String], session: Option[String])(
      f: Actor => IO[BasketError, BasketView]
  ): ZIO[Any, (StatusCode, ErrorResponse), BasketResponse] =
    actors.resolve(bearer, session).flatMap(actor => f(actor).mapBoth(toResponse, toBasketResponse))

  private def toBasketResponse(view: BasketView): BasketResponse =
    BasketResponse(
      id = view.id,
      items = view.items.map { item =>
        ItemView(
          id = item.id,
          description = Language.values.map(l => l.toCode -> item.description(l)).toMap,
          quantity = item.quantity,
          speed = item.speed,
          artworkId = item.artworkId,
          price = PriceView(
            item.price.total,
            item.price.currency,
            item.price.quotedAt,
            item.price.pricelistVersion,
          ),
        )
      },
      total = view.total,
      currency = view.currency,
      itemCount = view.itemCount,
      expiresAt = view.expiresAt,
    )

  private def toResponse(error: BasketError): (StatusCode, ErrorResponse) =
    val status = error match
      case BasketError.NotFound          => StatusCode.NotFound
      case BasketError.ItemNotFound(_)   => StatusCode.NotFound
      case BasketError.Expired           => StatusCode.Gone
      case BasketError.NotAuthorised     => StatusCode.Forbidden
      case BasketError.TooManyItems(_)   => StatusCode.UnprocessableEntity
      case BasketError.MixedCurrency(_, _) => StatusCode.UnprocessableEntity
      case BasketError.Rejected(_)       => StatusCode.UnprocessableEntity
      case BasketError.InvalidQuantity(_, _, _) => StatusCode.BadRequest
      case BasketError.UnknownValue(_, _)       => StatusCode.BadRequest

    // Accumulated problems stay accumulated on the wire, so a configurator can show everything
    // wrong at once instead of one problem per attempt.
    val items = error match
      case BasketError.Rejected(problems) =>
        problems.toList.map(p => ErrorItem(p.code, p.message(Language.En)))
      case other =>
        List(ErrorItem(other.toString.takeWhile(_ != '('), other.message(Language.En)))

    (status, ErrorResponse(items))
