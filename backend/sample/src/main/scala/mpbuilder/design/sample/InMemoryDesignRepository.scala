package mpbuilder.design.sample

import mpbuilder.design.*
import mpbuilder.design.core.{DesignRepository, DesignServiceLive}
import mpbuilder.shared.*
import kyo.*

final class InMemoryDesignRepository(ref: AtomicRef[Map[DesignId, DesignProject]]) extends DesignRepository:

  override def findById(id: DesignId): Option[DesignProject] < Async =
    ref.get.map(_.get(id))

  override def findByOwner(owner: DesignOwner): List[DesignProject] < Async =
    ref.get.map(_.values.filter(_.owner == owner).toList)

  override def save(project: DesignProject): Unit < Async =
    ref.update(_ + (project.id -> project))

  override def delete(id: DesignId): Unit < Async =
    ref.update(_ - id)

object DesignSampleWiring:

  def makeRepository: InMemoryDesignRepository < Sync =
    AtomicRef.init(Map.empty[DesignId, DesignProject]).map(InMemoryDesignRepository(_))

  def makeService(events: Option[EventBus] = None): DesignService < Sync =
    makeRepository.map(repo => DesignServiceLive.make(repo, events))
