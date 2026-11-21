package mpbuilder.artwork.sample

import mpbuilder.artwork.*
import mpbuilder.artwork.core.{ArtworkRepository, ArtworkServiceLive, ArtworkSubmission}
import mpbuilder.shared.*
import zio.*

final class InMemoryArtworkRepository(
    ref: Ref[Map[ArtworkId, ArtworkSubmission]],
) extends ArtworkRepository:

  override def findById(id: ArtworkId): UIO[Option[ArtworkSubmission]] =
    ref.get.map(_.get(id))

  override def save(submission: ArtworkSubmission): UIO[Unit] =
    ref.update(_ + (submission.id -> submission))

  def listAll: UIO[List[ArtworkSubmissionView]] =
    ref.get.map(_.values.map(_.toView).toList)

object ArtworkSampleLayer:

  val repositoryLayer: ULayer[InMemoryArtworkRepository] =
    ZLayer.fromZIO(Ref.make(Map.empty[ArtworkId, ArtworkSubmission]).map(new InMemoryArtworkRepository(_)))

  val live: ZLayer[EventBus, Nothing, ArtworkService] =
    (repositoryLayer ++ ZLayer.service[EventBus]) >>> ArtworkServiceLive.layer
