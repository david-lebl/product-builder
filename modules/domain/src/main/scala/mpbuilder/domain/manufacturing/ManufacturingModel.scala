package mpbuilder.domain.manufacturing

import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.{PriceBreakdown, Money, Currency}

opaque type StationId = String
object StationId:
  def unsafe(value: String): StationId = value
  extension (id: StationId) def value: String = id

opaque type ManufacturingOrderId = String
object ManufacturingOrderId:
  def unsafe(value: String): ManufacturingOrderId = value
  extension (id: ManufacturingOrderId) def value: String = id

enum OrderStatus:
  case Queued, InProgress, Completed, OnHold

enum OrderPriority:
  case Low, Normal, High, Urgent

enum StationStatus:
  case Available, Busy, Disabled

enum StationType:
  case Prepress
  case DigitalPrinter
  case OffsetPress
  case LargeFormatPrinter
  case Letterpress
  case Cutter
  case Laminator
  case UVCoater
  case EmbossingFoil
  case Folder
  case Binder
  case LargeFormatFinishing
  case QualityControl
  case Packaging

enum WorkflowStatus:
  case Pending, InProgress, Completed, OnHold, Cancelled

final case class Station(
    id: StationId,
    name: LocalizedString,
    stationType: StationType,
    sortOrder: Int,
)

final case class ProductionStep(
    stationId: StationId,
    status: OrderStatus,
    queuedAt: Long,
    startedAt: Option[Long] = None,
    completedAt: Option[Long] = None,
)

final case class Attachment(
    name: String,
    fileType: String,
    sizeBytes: Long,
    uploadedAt: Long,
)

final case class ManufacturingOrder(
    id: ManufacturingOrderId,
    orderId: OrderId,
    customerName: String,
    configuration: ProductConfiguration,
    quantity: Int,
    priceBreakdown: PriceBreakdown,
    steps: List[ProductionStep],
    currentStationId: Option[StationId],
    priority: OrderPriority,
    deadline: Option[Long],
    notes: String,
    attachments: List[Attachment],
    createdAt: Long,
)

object ManufacturingOrder:
  extension (order: ManufacturingOrder)
    def currentStep: Option[ProductionStep] =
      order.currentStationId.flatMap(sid =>
        order.steps.find(_.stationId == sid)
      )

    def currentStatus: Option[OrderStatus] =
      currentStep.map(_.status)

    def isAtStation(stationId: StationId): Boolean =
      order.currentStationId.contains(stationId)

    def stepsCompleted: Int =
      order.steps.count(_.status == OrderStatus.Completed)

    def totalSteps: Int =
      order.steps.size

    def isFullyCompleted: Boolean =
      order.steps.nonEmpty && order.steps.forall(_.status == OrderStatus.Completed)