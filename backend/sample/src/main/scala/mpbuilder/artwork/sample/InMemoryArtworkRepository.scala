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

  override def findByOwner(owner: ArtworkOwner): UIO[List[ArtworkSubmission]] =
    ref.get.map(_.values.filter(_.owner == owner).toList)

  override def save(submission: ArtworkSubmission): UIO[Unit] =
    ref.update(_ + (submission.id -> submission))

  def listAll: UIO[List[ArtworkSubmissionView]] =
    ref.get.map(_.values.map(_.toView).toList)

object ArtworkSampleLayer:

  private val baseInstant = java.time.Instant.ofEpochMilli(1740000000000L)

  private def seedArtworks: Map[ArtworkId, ArtworkSubmission] =
    val art1 = ArtworkSubmission
      .create(
        UploadArtworkCmd(
          artworkId = ArtworkId.unsafe("art-seed-0901"),
          owner = ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-seed-0901"), 0),
          componentKey = ComponentKey.Main,
          file = AssetFileMetadata(
            fileName = NonEmptyString.unsafe("dvorak-business-cards-v1.pdf"),
            mimeType = NonEmptyString.unsafe("application/pdf"),
            sizeBytes = 4_820_000L,
            storageKey = NonEmptyString.unsafe("demo/ord-seed-0901/dvorak-bc-v1.pdf"),
          ),
          extractedSpecs = ExtractedFileSpecs(
            resolutionDpi = 240,
            trimSize = Dimension2D(PositiveMm.unsafe(90), PositiveMm.unsafe(50)),
            bleed = NonNegativeMm.unsafe(3),
            colorSpace = ColorSpace.RGB,
            pageCount = 2,
            fontsEmbedded = true,
          ),
          requirement = Some(
            PreflightRequirement(
              expectedTrimSize = Dimension2D(PositiveMm.unsafe(90), PositiveMm.unsafe(50)),
              requiredBleed = NonNegativeMm.unsafe(3),
              minResolutionDpi = 300,
              expectedPageCount = Some(2),
            )
          ),
        ),
        baseInstant,
      )
      .toOption
      .get

    val art2 = ArtworkSubmission
      .create(
        UploadArtworkCmd(
          artworkId = ArtworkId.unsafe("art-seed-0902"),
          owner = ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-seed-0902"), 0),
          componentKey = ComponentKey.Main,
          file = AssetFileMetadata(
            fileName = NonEmptyString.unsafe("printshoppro-a5-flyers-spring.pdf"),
            mimeType = NonEmptyString.unsafe("application/pdf"),
            sizeBytes = 12_400_000L,
            storageKey = NonEmptyString.unsafe("demo/ord-seed-0902/flyers-spring.pdf"),
          ),
          extractedSpecs = ExtractedFileSpecs(
            resolutionDpi = 180,
            trimSize = Dimension2D(PositiveMm.unsafe(148), PositiveMm.unsafe(210)),
            bleed = NonNegativeMm.unsafe(1),
            colorSpace = ColorSpace.RGB,
            pageCount = 2,
            fontsEmbedded = true,
            hasUnflattenedTransparency = true,
          ),
          requirement = Some(
            PreflightRequirement(
              expectedTrimSize = Dimension2D(PositiveMm.unsafe(148), PositiveMm.unsafe(210)),
              requiredBleed = NonNegativeMm.unsafe(3),
              minResolutionDpi = 300,
              expectedPageCount = Some(2),
            )
          ),
        ),
        baseInstant.plusSeconds(3600),
      )
      .toOption
      .get
      .requestChanges(NonEmptyString.unsafe("Please re-export at 300 DPI with 3 mm bleed and CMYK color profile."))
      .toOption
      .get

    val art3 = ArtworkSubmission
      .create(
        UploadArtworkCmd(
          artworkId = ArtworkId.unsafe("art-seed-0903"),
          owner = ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-seed-0903"), 0),
          componentKey = ComponentKey.Main,
          file = AssetFileMetadata(
            fileName = NonEmptyString.unsafe("megacorp-brochure-final-cmyk.pdf"),
            mimeType = NonEmptyString.unsafe("application/pdf"),
            sizeBytes = 28_900_000L,
            storageKey = NonEmptyString.unsafe("demo/ord-seed-0903/megacorp-brochure.pdf"),
          ),
          extractedSpecs = ExtractedFileSpecs(
            resolutionDpi = 300,
            trimSize = Dimension2D(PositiveMm.unsafe(210), PositiveMm.unsafe(297)),
            bleed = NonNegativeMm.unsafe(3),
            colorSpace = ColorSpace.CMYK,
            pageCount = 4,
            fontsEmbedded = true,
          ),
          requirement = Some(
            PreflightRequirement(
              expectedTrimSize = Dimension2D(PositiveMm.unsafe(210), PositiveMm.unsafe(297)),
              requiredBleed = NonNegativeMm.unsafe(3),
              minResolutionDpi = 300,
              expectedPageCount = Some(4),
            )
          ),
        ),
        baseInstant.plusSeconds(7200),
      )
      .toOption
      .get
      .approve(EmployeeId.unsafe("emp-prepress"), overrideFailures = false, baseInstant.plusSeconds(9000))
      .toOption
      .get

    val art4 = ArtworkSubmission
      .create(
        UploadArtworkCmd(
          artworkId = ArtworkId.unsafe("art-seed-0904"),
          owner = ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-seed-0904"), 0),
          componentKey = ComponentKey.Main,
          file = AssetFileMetadata(
            fileName = NonEmptyString.unsafe("graphicdesign-rollups-cmyk.pdf"),
            mimeType = NonEmptyString.unsafe("application/pdf"),
            sizeBytes = 19_500_000L,
            storageKey = NonEmptyString.unsafe("demo/ord-seed-0904/rollups.pdf"),
          ),
          extractedSpecs = ExtractedFileSpecs(
            resolutionDpi = 300,
            trimSize = Dimension2D(PositiveMm.unsafe(850), PositiveMm.unsafe(2000)),
            bleed = NonNegativeMm.unsafe(5),
            colorSpace = ColorSpace.CMYK,
            pageCount = 1,
            fontsEmbedded = true,
          ),
          requirement = Some(
            PreflightRequirement(
              expectedTrimSize = Dimension2D(PositiveMm.unsafe(850), PositiveMm.unsafe(2000)),
              requiredBleed = NonNegativeMm.unsafe(3),
              minResolutionDpi = 150,
              expectedPageCount = Some(1),
            )
          ),
        ),
        baseInstant.plusSeconds(10800),
      )
      .toOption
      .get
      .approve(EmployeeId.unsafe("emp-prepress"), overrideFailures = false, baseInstant.plusSeconds(12000))
      .toOption
      .get

    List(art1, art2, art3, art4).map(a => a.id -> a).toMap

  def repositoryLayer(seedSampleArtworks: Boolean = true): ULayer[InMemoryArtworkRepository] =
    val init = if seedSampleArtworks then seedArtworks else Map.empty[ArtworkId, ArtworkSubmission]
    ZLayer.fromZIO(Ref.make(init).map(new InMemoryArtworkRepository(_)))

  val live: ZLayer[EventBus, Nothing, ArtworkService] =
    live(seedSampleArtworks = true)

  def live(seedSampleArtworks: Boolean): ZLayer[EventBus, Nothing, ArtworkService] =
    (repositoryLayer(seedSampleArtworks) ++ ZLayer.service[EventBus]) >>> ArtworkServiceLive.layer
