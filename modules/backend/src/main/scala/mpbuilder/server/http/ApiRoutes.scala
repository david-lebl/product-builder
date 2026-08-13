package mpbuilder.server.http

import java.util.UUID

import mpbuilder.api.*
import mpbuilder.server.service.OrderService
import zio.*
import zio.http.*
import zio.json.*

object ApiRoutes:

  val routes: Routes[OrderService, Nothing] = Routes(
    Method.GET / "api" / "health" -> handler(Response.text("ok")),

    Method.GET / "api" / "catalog" -> handler {
      ZIO.serviceWithZIO[OrderService](_.catalogResponse).map(jsonOk(_))
    },

    Method.POST / "api" / "orders" -> handler { (request: Request) =>
      (for
        body <- request.body.asString.orDie
        parsed <- ZIO
          .fromEither(body.fromJson[CreateOrderRequest])
          .mapError(msg => Response.json(ApiError(List(badJson(msg))).toJson).status(Status.BadRequest))
        response <- ZIO
          .serviceWithZIO[OrderService](_.create(parsed))
          .mapError {
            case OrderService.Rejection(errors) =>
              Response.json(ApiError(errors).toJson).status(Status.UnprocessableEntity)
            case t: Throwable => internalError(t)
          }
      yield jsonOk(response).status(Status.Created)).merge
    },

    Method.GET / "api" / "orders" -> handler { (request: Request) =>
      val limit = request.url.queryParams.queryParam("limit").flatMap(_.toIntOption).getOrElse(50)
      ZIO
        .serviceWithZIO[OrderService](_.list(limit.max(1).min(500)))
        .mapBoth(internalError, jsonOk(_))
        .merge
    },

    Method.GET / "api" / "orders" / string("id") -> handler { (id: String, _: Request) =>
      ZIO.attempt(UUID.fromString(id)).option.flatMap {
        case None => ZIO.succeed(notFound)
        case Some(uuid) =>
          ZIO
            .serviceWithZIO[OrderService](_.byId(uuid))
            .fold(internalError, _.fold(notFound)(jsonOk(_)))
      }
    },
  )

  private def jsonOk[A: JsonEncoder](value: A): Response = Response.json(value.toJson)

  private val notFound: Response =
    Response.json(ApiError(List(ErrorDto("NotFound", mpbuilder.domain.LocalizedText("Order not found", "Objednávka nenalezena")))).toJson)
      .status(Status.NotFound)

  private def badJson(message: String): ErrorDto =
    ErrorDto("MalformedRequest", mpbuilder.domain.LocalizedText(s"Malformed request: $message", s"Neplatný požadavek: $message"))

  private def internalError(t: Throwable): Response =
    Response.json(ApiError(List(ErrorDto("Internal", mpbuilder.domain.LocalizedText("Internal server error", "Interní chyba serveru")))).toJson)
      .status(Status.InternalServerError)
