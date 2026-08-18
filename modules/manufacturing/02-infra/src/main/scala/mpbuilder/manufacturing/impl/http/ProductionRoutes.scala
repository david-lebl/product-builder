package mpbuilder.manufacturing
package impl
package http

import mpbuilder.commons.*
import mpbuilder.identity as id
import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import sttp.tapir.server.ServerEndpoint
import zio.*

import ProductionEndpoints.*

/** Server logic for the shop floor endpoints.
  *
  * Staff only, every one of them: approving work and declaring goods shipped are the shop's own
  * decisions, and nothing a customer sends should be able to make them.
  */
private[manufacturing] final class ProductionRoutes(
    production: ProductionService,
    auth: id.AuthService,
):

  def endpoints: List[ServerEndpoint[Any, Task]] = List(
    ProductionEndpoints.queue.zServerLogic { case (bearer, lang) =>
      asStaff(bearer, lang)(_ => production.queue)
        .map(orders => QueueResponse(orders.map(toResponse)))
    },
    ProductionEndpoints.find.zServerLogic { case (bearer, lang, productionId) =>
      asStaff(bearer, lang)(_ => production.find(productionId)).map(toResponse)
    },
    ProductionEndpoints.approve.zServerLogic { case (bearer, lang, productionId) =>
      asStaff(bearer, lang)(who => production.approve(productionId, who)).map(toResponse)
    },
    ProductionEndpoints.dispatch.zServerLogic { case (bearer, lang, productionId, req) =>
      asStaff(bearer, lang)(_ => production.dispatch(productionId, req.tracking)).map(toResponse)
    },
  )

  /** Resolve the caller and refuse anyone who is not staff, handing the operation their id so the
    * approval records who gave it.
    */
  private def asStaff[A](bearer: Option[String], acceptLanguage: Option[String])(
      f: String => IO[ProductionError, A]
  ): ZIO[Any, (StatusCode, ErrorResponse), A] =
    val lang = Languages.parse(acceptLanguage)
    bearer.map(_.stripPrefix("Bearer ").trim).filter(_.nonEmpty) match
      case None =>
        ZIO.fail(
          (
            StatusCode.Unauthorized,
            ErrorResponse(List(ErrorItem("Unauthorized", "Provide an Authorization bearer token"))),
          )
        )
      case Some(token) =>
        auth
          .verify(token)
          .mapError(error =>
            (StatusCode.Unauthorized, ErrorResponse(List(ErrorItem("Unauthorized", error.message))))
          )
          .flatMap { principal =>
            if principal.isStaff then f(principal.userId).mapError(toResponse(lang))
            else
              ZIO.fail(
                (
                  StatusCode.Forbidden,
                  ErrorResponse(
                    List(
                      ErrorItem(
                        "StaffOnly",
                        lang match
                          case Language.En => "Only staff may do that"
                          case Language.Cs => "K této akci je oprávněn pouze personál",
                      )
                    )
                  ),
                )
              )
          }

  private def text(value: LocalizedString): Map[String, String] =
    Language.values.map(l => l.toCode -> value(l)).toMap

  private def toStatusView(status: ProductionStatus): StatusView = status match
    case ProductionStatus.AwaitingApproval    => StatusView("AwaitingApproval", None, None)
    case ProductionStatus.Approved(at, by)    => StatusView("Approved", Some(at), Some(by))
    case ProductionStatus.Dispatched(at, trk) => StatusView("Dispatched", Some(at), trk)

  private def toResponse(order: ProductionOrderView): ProductionOrderResponse =
    ProductionOrderResponse(
      id = order.id,
      orderId = order.orderId,
      orderNumber = order.orderNumber,
      customerName = order.customerName,
      status = toStatusView(order.status),
      speed = order.speed,
      lines = order.lines.map(line =>
        LineView(
          lineId = line.lineId,
          lineNo = line.lineNo,
          description = text(line.description),
          quantity = line.quantity,
          steps = line.steps.map(s =>
            StepView(s.id, s.station, s.componentRole, s.dependsOn.toList.sorted)
          ),
        )
      ),
      stepCount = order.stepCount,
      receivedAt = order.receivedAt,
    )

  private def toResponse(lang: Language)(error: ProductionError): (StatusCode, ErrorResponse) =
    val status = error match
      case ProductionError.NotFound(_)          => StatusCode.NotFound
      case ProductionError.CannotPlan(_)        => StatusCode.UnprocessableEntity
      case ProductionError.AlreadyApproved(_)   => StatusCode.Conflict
      case ProductionError.AlreadyDispatched(_) => StatusCode.Conflict
      case ProductionError.NotApproved(_)       => StatusCode.Conflict

    val items = error match
      case ProductionError.CannotPlan(problems) =>
        problems.toList.map(p => ErrorItem(p.code, p.message(lang)))
      case other =>
        List(ErrorItem(other.toString.takeWhile(_ != '('), other.message(lang)))

    (status, ErrorResponse(items))
