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

/** Server logic for the basket endpoints.
  *
  * Resolving the caller is the only interesting part: a bearer token is verified through identity,
  * an `X-Basket-Session` header identifies an anonymous shopper, and a request carrying neither is
  * refused rather than silently given a shared basket.
  */
private[orderintake] final class BasketRoutes(baskets: BasketService, auth: id.AuthService):

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
    resolveActor(bearer, session).flatMap(actor => f(actor).mapBoth(toResponse, toBasketResponse))

  private def resolveActor(
      authorization: Option[String],
      sessionHeader: Option[String],
  ): ZIO[Any, (StatusCode, ErrorResponse), Actor] =
    authorization.map(_.stripPrefix("Bearer ").trim).filter(_.nonEmpty) match
      case Some(token) =>
        auth
          .verify(token)
          .mapBoth(
            error =>
              (StatusCode.Unauthorized, ErrorResponse(List(ErrorItem("Unauthorized", error.message)))),
            principal =>
              Actor.Authenticated(principal.userId, principal.customerId, principal.isStaff),
          )
      case None =>
        sessionHeader.filter(_.trim.nonEmpty) match
          case Some(session) => ZIO.succeed(Actor.Anonymous(session.trim))
          // Without either header there is no way to tell one anonymous shopper from another, and
          // inventing a basket would mean handing everyone the same one.
          case None =>
            ZIO.fail(
              (
                StatusCode.BadRequest,
                ErrorResponse(
                  List(
                    ErrorItem(
                      "MissingSession",
                      "Provide an Authorization bearer token or an X-Basket-Session header",
                    )
                  )
                ),
              )
            )

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
