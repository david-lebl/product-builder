package mpbuilder.design

import mpbuilder.shared.*
import kyo.*
import java.time.Instant

// ── Design projects (persistence & sync envelope) ───────────────────────────

/** Who may read and write a design.
  *
  *   - `Device` — an anonymous browser (random id kept in `localStorage`); designs live on that device and,
  *     optionally, as server backups keyed by the device id.
  *   - `Customer` — a signed-in customer; designs sync across all their devices.
  *
  * On sign-in, device designs are re-owned by the customer ([[DesignService.claimDeviceDesigns]]).
  */
enum DesignOwner derives CanEqual:
  case Device(deviceId: NonEmptyString)
  case Customer(customerId: CustomerId)

enum DesignStatus derives CanEqual:
  /** Being edited. */
  case Draft
  /** Frozen: attached to a cart/order line as artwork. Further edits happen on a copy. */
  case Submitted(artworkId: ArtworkId, submittedAt: Instant)
  case Archived

final case class DesignProject(
    id: DesignId,
    name: String,
    owner: DesignOwner,
    document: DesignDocument,
    revision: Long,
    status: DesignStatus,
    createdAt: Instant,
    updatedAt: Instant,
):
  def isEditable: Boolean = status == DesignStatus.Draft
  def summary: DesignSummary =
    DesignSummary(
      id = id,
      name = name,
      productKind = document.product.kind,
      categoryId = document.product.categoryId,
      trimWidthMm = document.format.trimWidthMm,
      trimHeightMm = document.format.trimHeightMm,
      pageCount = document.pages.size,
      revision = revision,
      status = status,
      createdAt = createdAt,
      updatedAt = updatedAt,
    )

/** Lightweight listing row ("My designs"). */
final case class DesignSummary(
    id: DesignId,
    name: String,
    productKind: DesignProductKind,
    categoryId: Option[CategoryId],
    trimWidthMm: Double,
    trimHeightMm: Double,
    pageCount: Int,
    revision: Long,
    status: DesignStatus,
    createdAt: Instant,
    updatedAt: Instant,
)

final case class CreateDesignCmd(id: DesignId, owner: DesignOwner, name: String, document: DesignDocument)

/** Optimistic-concurrency save: succeeds only when `expectedRevision` equals the stored revision. */
final case class SaveDesignCmd(
    id: DesignId,
    requester: DesignOwner,
    expectedRevision: Long,
    name: String,
    document: DesignDocument,
)

enum DesignError(val message: String) derives CanEqual:
  case NotFound(id: DesignId) extends DesignError(s"Design '${id.value}' not found")
  case AlreadyExists(id: DesignId) extends DesignError(s"Design '${id.value}' already exists")
  case AccessDenied(id: DesignId) extends DesignError(s"Design '${id.value}' belongs to someone else")
  case RevisionConflict(id: DesignId, expected: Long, actual: Long)
      extends DesignError(s"Design '${id.value}' was changed elsewhere (expected revision $expected, found $actual)")
  case NotEditable(id: DesignId) extends DesignError(s"Design '${id.value}' was already submitted and can no longer be edited")
  case InvalidDocument(errors: List[String]) extends DesignError(s"Invalid design: ${errors.mkString("; ")}")
  case InvalidName(name: String) extends DesignError("Design name must be 1–120 characters")

// ── Domain events ───────────────────────────────────────────────────────────

final case class DesignSubmitted(designId: DesignId, artworkId: ArtworkId, revision: Long, occurredAt: Instant) extends DomainEvent

// ── Inbound port ────────────────────────────────────────────────────────────

trait DesignService:
  def create(cmd: CreateDesignCmd): DesignProject < (Async & Abort[DesignError])
  def save(cmd: SaveDesignCmd): DesignProject < (Async & Abort[DesignError])
  def get(id: DesignId, requester: DesignOwner): DesignProject < (Async & Abort[DesignError])
  def listByOwner(owner: DesignOwner): List[DesignSummary] < Async
  def rename(id: DesignId, requester: DesignOwner, name: String): DesignProject < (Async & Abort[DesignError])
  def duplicate(id: DesignId, newId: DesignId, requester: DesignOwner, name: String): DesignProject < (Async & Abort[DesignError])
  def delete(id: DesignId, requester: DesignOwner): Unit < (Async & Abort[DesignError])

  /** Freezes the design as the artwork of a cart/order line (the PDF renderer reads this exact revision). */
  def markSubmitted(id: DesignId, requester: DesignOwner, artworkId: ArtworkId): DesignProject < (Async & Abort[DesignError])

  /** Moves every design of an anonymous device to the customer who just signed in on it. Returns how many moved. */
  def claimDeviceDesigns(deviceId: NonEmptyString, customerId: CustomerId): Int < Async

  /** Staff / production read access (no owner check). */
  def getForProduction(id: DesignId): DesignProject < (Async & Abort[DesignError.NotFound])
