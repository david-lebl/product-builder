package mpbuilder.design
package core

import mpbuilder.shared.*
import kyo.*

/** Outbound persistence port (in-memory in `backend/sample`, PostgreSQL JSONB in `design/02-o-db` later). */
private[design] trait DesignRepository:
  def findById(id: DesignId): Option[DesignProject] < Async
  def findByOwner(owner: DesignOwner): List[DesignProject] < Async
  /** Inserts or replaces the project (revision checks happen in the service; adapters may add a guard). */
  def save(project: DesignProject): Unit < Async
  def delete(id: DesignId): Unit < Async

final class DesignServiceLive private[design] (repo: DesignRepository, events: Option[EventBus] = None) extends DesignService:

  private def validName(name: String): String < Abort[DesignError] =
    val t = name.trim
    if t.isEmpty || t.length > 120 then Abort.fail(DesignError.InvalidName(name)) else (t: String < Abort[DesignError])

  private def validDocument(doc: DesignDocument): DesignDocument < Abort[DesignError] =
    DesignValidation.validate(DesignOps.pruneAssets(doc)).fold(
      errs => Abort.fail(DesignError.InvalidDocument(errs.toList)),
      d => (d: DesignDocument < Abort[DesignError]),
    )

  private def load(id: DesignId): DesignProject < (Async & Abort[DesignError]) =
    repo.findById(id).map {
      case Some(p) => (p: DesignProject < Abort[DesignError])
      case None    => Abort.fail(DesignError.NotFound(id))
    }

  private def owned(id: DesignId, requester: DesignOwner): DesignProject < (Async & Abort[DesignError]) =
    load(id).map(p => if p.owner == requester then (p: DesignProject < Abort[DesignError]) else Abort.fail(DesignError.AccessDenied(id)))

  override def create(cmd: CreateDesignCmd): DesignProject < (Async & Abort[DesignError]) =
    for
      existing <- repo.findById(cmd.id)
      _        <- if existing.isDefined then Abort.fail(DesignError.AlreadyExists(cmd.id)) else Kyo.unit
      name     <- validName(cmd.name)
      doc      <- validDocument(cmd.document)
      now      <- Effects.now
      project   = DesignProject(cmd.id, name, cmd.owner, doc, 1L, DesignStatus.Draft, now, now)
      _        <- repo.save(project)
    yield project

  override def save(cmd: SaveDesignCmd): DesignProject < (Async & Abort[DesignError]) =
    for
      current <- owned(cmd.id, cmd.requester)
      _       <- if !current.isEditable then Abort.fail(DesignError.NotEditable(cmd.id)) else Kyo.unit
      _ <-
        if current.revision != cmd.expectedRevision then
          Abort.fail(DesignError.RevisionConflict(cmd.id, cmd.expectedRevision, current.revision))
        else Kyo.unit
      name <- validName(cmd.name)
      doc  <- validDocument(cmd.document)
      now  <- Effects.now
      next  = current.copy(name = name, document = doc, revision = current.revision + 1, updatedAt = now)
      _    <- repo.save(next)
    yield next

  override def get(id: DesignId, requester: DesignOwner): DesignProject < (Async & Abort[DesignError]) =
    owned(id, requester)

  override def listByOwner(owner: DesignOwner): List[DesignSummary] < Async =
    repo.findByOwner(owner).map(_.filterNot(_.status == DesignStatus.Archived).sortBy(_.updatedAt).reverse.map(_.summary))

  override def rename(id: DesignId, requester: DesignOwner, name: String): DesignProject < (Async & Abort[DesignError]) =
    for
      current <- owned(id, requester)
      n       <- validName(name)
      now     <- Effects.now
      next     = current.copy(name = n, revision = current.revision + 1, updatedAt = now)
      _       <- repo.save(next)
    yield next

  override def duplicate(id: DesignId, newId: DesignId, requester: DesignOwner, name: String): DesignProject < (Async & Abort[DesignError]) =
    for
      source <- owned(id, requester)
      copy   <- create(CreateDesignCmd(newId, requester, name, source.document))
    yield copy

  override def delete(id: DesignId, requester: DesignOwner): Unit < (Async & Abort[DesignError]) =
    for
      current <- owned(id, requester)
      _ <- (current.status match
             case _: DesignStatus.Submitted =>
               // Keep submitted designs for production; hide them from the owner instead.
               Effects.now.map(now => repo.save(current.copy(status = DesignStatus.Archived, updatedAt = now)))
             case _ => repo.delete(id)
           ): Unit < Async
    yield ()

  override def markSubmitted(id: DesignId, requester: DesignOwner, artworkId: ArtworkId): DesignProject < (Async & Abort[DesignError]) =
    for
      current <- owned(id, requester)
      now     <- Effects.now
      next     = current.copy(status = DesignStatus.Submitted(artworkId, now), updatedAt = now)
      _       <- repo.save(next)
      _       <- events match
                   case Some(bus) => bus.publish(DesignSubmitted(id, artworkId, next.revision, now))
                   case None      => (Kyo.unit: Unit < Sync)
    yield next

  override def claimDeviceDesigns(deviceId: NonEmptyString, customerId: CustomerId): Int < Async =
    for
      designs <- repo.findByOwner(DesignOwner.Device(deviceId))
      now     <- Effects.now
      _ <- Kyo.foreachDiscard(designs)(p =>
             repo.save(p.copy(owner = DesignOwner.Customer(customerId), revision = p.revision + 1, updatedAt = now))
           )
    yield designs.size

  override def getForProduction(id: DesignId): DesignProject < (Async & Abort[DesignError.NotFound]) =
    repo.findById(id).map {
      case Some(p) => (p: DesignProject < Abort[DesignError.NotFound])
      case None    => Abort.fail(DesignError.NotFound(id))
    }

object DesignServiceLive:
  def make(repo: DesignRepository, events: Option[EventBus] = None): DesignService =
    new DesignServiceLive(repo, events)
