package mpbuilder.artwork

import mpbuilder.artwork.core.*
import mpbuilder.shared.*
import kyo.*
import kyo.test.*
import java.time.Instant

class ArtworkDomainSpec extends Test[Any]:

  private val now = Instant.parse("2026-03-10T10:00:00Z")

  private val a5Req = PreflightRequirement(
    expectedTrimSize = Dimension2D.A5,
    dimensionToleranceMm = NonNegativeMm.unsafe(1),
    requiredBleed = NonNegativeMm.unsafe(3),
    minResolutionDpi = 300,
    expectedPageCount = Some(2),
    requireEmbeddedFonts = true,
  )

  private val goodFile = AssetFileMetadata(
    fileName = NonEmptyString.unsafe("flyer-v1.pdf"),
    mimeType = NonEmptyString.unsafe("application/pdf"),
    sizeBytes = 2_400_000L,
    storageKey = NonEmptyString.unsafe("s3://artworks/flyer-v1.pdf"),
  )

  private val badSpecs = ExtractedFileSpecs(
    resolutionDpi = 120,                   // FAIL (< 200)
    trimSize = Dimension2D.A4,             // FAIL (expected A5)
    bleed = NonNegativeMm.zero,            // FAIL (< 3mm)
    colorSpace = ColorSpace.RGB,           // WARNING
    pageCount = 1,                         // FAIL (expected 2)
    fontsEmbedded = false,                 // FAIL
  )

  private val goodSpecs = ExtractedFileSpecs(
    resolutionDpi = 300,
    trimSize = Dimension2D.A5.rotated,     // rotated A5 still matches!
    bleed = NonNegativeMm.unsafe(3),
    colorSpace = ColorSpace.CMYK,
    pageCount = 2,
    fontsEmbedded = true,
  )

  private def inMemoryRepo: ArtworkRepository < Sync =
    AtomicRef.init(Map.empty[ArtworkId, ArtworkSubmission]).map { ref =>
      new ArtworkRepository:
        override def findById(id: ArtworkId): Option[ArtworkSubmission] < Async =
          ref.get.map(_.get(id))
        override def save(submission: ArtworkSubmission): Unit < Async =
          ref.update(_ + (submission.id -> submission))
        override def delete(id: ArtworkId): Unit < Async =
          ref.update(_ - id)
    }

  private def services: (ArtworkService, EventBus) < Sync =
    for
      bus  <- EventBus.init
      repo <- inMemoryRepo
    yield (ArtworkServiceLive.make(repo, bus), bus)

  "ArtworkDomainSpec" - {
    "PreflightEvaluator catches low DPI, missing bleed, wrong trim size, wrong page count, unembedded fonts, and passes rotated match" in {
      val badReport  = PreflightEvaluator.evaluate(badSpecs, a5Req, now)
      val goodReport = PreflightEvaluator.evaluate(goodSpecs, a5Req, now)
      assert(badReport.hasFailures)
      assert(badReport.checks.count(_.outcome.isInstanceOf[CheckOutcome.Failed]) == 5)
      assert(goodReport.isCleanPass)
      assert(!goodReport.hasFailures)
    }

    "approval is blocked on failed preflight unless overridden, and revision upload preserves immutable history" in {
      val artId = ArtworkId.unsafe("art-1")
      val opId  = EmployeeId.unsafe("emp-prepress")
      val owner = ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-1"), lineIndex = 0)

      locally {
        for
          (svc, bus) <- services
          queue     <- bus.subscribe
          v1        <- svc.upload(UploadArtworkCmd(artId, owner, ComponentKey.Main, 1, goodFile, badSpecs, Some(a5Req)))
          blockAppr <- svc.approve(artId, opId, overrideWarningsOrFailures = false).either
          chg       <- svc.requestChanges(artId, NonEmptyString.unsafe("Please add 3mm bleed and embed fonts"))
          v2        <- svc.uploadRevision(
                         UploadRevisionCmd(
                           artId,
                           goodFile.copy(fileName = NonEmptyString.unsafe("flyer-v2.pdf")),
                           goodSpecs,
                           Some(a5Req),
                         )
                       )
          approved  <- svc.approve(artId, opId)
          noMoreRev <- svc.uploadRevision(UploadRevisionCmd(artId, goodFile, goodSpecs, Some(a5Req))).either
          events    <- queue.drain
          ev1        = events(0)
          ev2        = events(1)
          ev3        = events(2)
          ev4        = events(3)
        yield {
          assert(v1.status == ArtworkStatus.PreflightFailed)
          assert(blockAppr == Left(ArtworkError.CannotApproveFailedPreflight(artId)))
          assert(chg.status.isInstanceOf[ArtworkStatus.ChangesRequested])
          assert(v2.status == ArtworkStatus.PreflightPassed)
          assert(v2.revisionCount == 2)
          assert(v2.allRevisions.size == 2)
          assert(v2.allRevisions.map(_.revisionNumber) == List(1, 2))
          assert(v2.createdAt == v1.latestRevision.uploadedAt)
          assert(v2.latestRevision.revisionNumber == 2)
          assert(approved.status.isInstanceOf[ArtworkStatus.OperatorApproved])
          assert(noMoreRev.isLeft)
          assert(ev1.isInstanceOf[ArtworkUploaded])
          assert(ev2.isInstanceOf[ArtworkChangesRequested])
          assert(ev3.isInstanceOf[ArtworkUploaded])
          assert(ev4.isInstanceOf[ArtworkApproved])
        }
      }
    }

    "URL metadata, reassignOwner (Draft -> OrderLine), and deleteDraft guard work cleanly" in {
      val draftId1  = ArtworkId.unsafe("art-draft-1")
      val draftId2  = ArtworkId.unsafe("art-draft-2")
      val draftOwn  = ArtworkOwner.DraftSession(NonEmptyString.unsafe("sess-42"))
      val lineOwner = ArtworkOwner.OrderLineRef(OrderId.unsafe("ord-42"), lineIndex = 0)
      val fileWithUrls = goodFile.copy(
        downloadUrl = Some(NonEmptyString.unsafe("https://cdn.printcraft.demo/flyer-v1.pdf")),
        previewUrl = Some(NonEmptyString.unsafe("data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=")),
      )

      for
        (svc, _)   <- services
        u1         <- svc.upload(UploadArtworkCmd(draftId1, draftOwn, ComponentKey.Main, 1, fileWithUrls, goodSpecs, Some(a5Req)))
        u2         <- svc.upload(UploadArtworkCmd(draftId2, draftOwn, ComponentKey.Main, 1, goodFile, goodSpecs, Some(a5Req)))
        // Delete draft2 succeeds
        _          <- svc.deleteDraft(draftId2)
        afterDel   <- svc.getById(draftId2).either
        // Reassign draft1 to OrderLineRef succeeds
        reassigned <- svc.reassignOwner(draftId1, lineOwner)
        // Deleting an order-owned artwork fails with CannotDeleteNonDraft
        delBlocked <- svc.deleteDraft(draftId1).either
      yield {
        assert(u1.latestRevision.file.shareableUrl == "https://cdn.printcraft.demo/flyer-v1.pdf")
        assert(u2.latestRevision.file.shareableUrl == "https://assets.printcraft.demo/s3://artworks/flyer-v1.pdf")
        assert(afterDel == Left(ArtworkError.NotFound(draftId2)))
        assert(reassigned.owner == lineOwner)
        assert(delBlocked == Left(ArtworkError.CannotDeleteNonDraft(draftId1, lineOwner)))
      }
    }
  }
