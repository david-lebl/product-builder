package mpbuilder.orderintake
package impl
package http

import mpbuilder.identity as id
import sttp.model.StatusCode
import zio.*

import BasketEndpoints.{ErrorItem, ErrorResponse}

/** Turning the two auth headers into an [[Actor]].
  *
  * Shared by the basket and checkout routes: the two must agree about who is calling, or a customer
  * would check out a basket other than the one they were shown.
  */
private[orderintake] final class Actors(auth: id.AuthService):

  def resolve(
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
