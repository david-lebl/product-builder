package mpbuilder.manufacturing
package impl
package http

import mpbuilder.commons.*
import mpbuilder.commons.json.given
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import zio.json.*

/** Wire shapes and endpoint descriptions for the shop floor. */
private[manufacturing] object ProductionEndpoints:

  final case class StepView(
      id: String,
      station: String,
      componentRole: Option[String],
      dependsOn: List[String],
  )

  final case class LineView(
      lineId: String,
      lineNo: Int,
      description: Map[String, String],
      quantity: Int,
      steps: List[StepView],
  )

  final case class StatusView(kind: String, at: Option[Timestamp], detail: Option[String])

  final case class ProductionOrderResponse(
      id: String,
      orderId: String,
      orderNumber: String,
      customerName: String,
      status: StatusView,
      speed: String,
      lines: List[LineView],
      stepCount: Int,
      receivedAt: Timestamp,
  )

  final case class QueueResponse(orders: List[ProductionOrderResponse])
  final case class DispatchRequest(tracking: Option[String] = None)

  final case class ErrorItem(code: String, message: String)
  final case class ErrorResponse(errors: List[ErrorItem])

  given Schema[Timestamp] = Schema.schemaForLong.map(v => Some(Timestamp(v)))(_.epochMillis)

  given JsonCodec[StepView] = DeriveJsonCodec.gen
  given JsonCodec[LineView] = DeriveJsonCodec.gen
  given JsonCodec[StatusView] = DeriveJsonCodec.gen
  given JsonCodec[ProductionOrderResponse] = DeriveJsonCodec.gen
  given JsonCodec[QueueResponse] = DeriveJsonCodec.gen
  given JsonCodec[DispatchRequest] = DeriveJsonCodec.gen
  given JsonCodec[ErrorItem] = DeriveJsonCodec.gen
  given JsonCodec[ErrorResponse] = DeriveJsonCodec.gen

  private val base = endpoint
    .in("api" / "v1" / "production")
    .in(header[Option[String]]("Authorization"))
    .in(header[Option[String]]("Accept-Language"))
    .errorOut(
      oneOf[(StatusCode, ErrorResponse)](
        oneOfVariantValueMatcher(statusCode.and(jsonBody[ErrorResponse]))({ case _ => true })
      )
    )
    .tag("production")

  val queue =
    base.get
      .in("queue")
      .out(jsonBody[QueueResponse])
      .summary("Orders waiting to be approved for production")
      .description("Oldest first — it is a queue, and the shop works through it in arrival order.")

  val find =
    base.get.in(path[String]("productionId")).out(jsonBody[ProductionOrderResponse])
      .summary("One production order, with its planned steps")

  val approve =
    base.post
      .in(path[String]("productionId") / "approve")
      .out(jsonBody[ProductionOrderResponse])
      .summary("Authorise production")
      .description("This is what moves the commercial order into production, via an event.")

  val dispatch =
    base.post
      .in(path[String]("productionId") / "dispatch")
      .in(jsonBody[DispatchRequest])
      .out(jsonBody[ProductionOrderResponse])
      .summary("Record that the goods have left")

  val all = List(queue, find, approve, dispatch)
