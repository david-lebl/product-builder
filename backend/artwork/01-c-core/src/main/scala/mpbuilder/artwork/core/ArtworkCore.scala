package mpbuilder.artwork
package core

import mpbuilder.shared.*
import kyo.*
import java.time.Instant

/** Pure automated prepress preflight evaluator. */
private[artwork] object PreflightEvaluator:

  def evaluate(
      specs: ExtractedFileSpecs,
      req: PreflightRequirement,
      now: Instant,
  ): PreflightReport =
    val resolutionCheck =
      if specs.resolutionDpi >= req.minResolutionDpi then
        PreflightCheckResult(PreflightCheckKind.Resolution, CheckOutcome.Passed)
      else if specs.resolutionDpi >= (req.minResolutionDpi * 2) / 3 then
        PreflightCheckResult(
          PreflightCheckKind.Resolution,
          CheckOutcome.Warning(s"Resolution ${specs.resolutionDpi} DPI is below recommended ${req.minResolutionDpi} DPI"),
        )
      else
        PreflightCheckResult(
          PreflightCheckKind.Resolution,
          CheckOutcome.Failed(s"Resolution ${specs.resolutionDpi} DPI is below minimum ${req.minResolutionDpi} DPI"),
        )

    val bleedCheck =
      if specs.bleed.value >= req.requiredBleed.value then
        PreflightCheckResult(PreflightCheckKind.Bleed, CheckOutcome.Passed)
      else
        PreflightCheckResult(
          PreflightCheckKind.Bleed,
          CheckOutcome.Failed(s"Bleed ${specs.bleed.value}mm is less than required ${req.requiredBleed.value}mm"),
        )

    val tol = req.dimensionToleranceMm.value
    val matchesNormal =
      (specs.trimSize.width.value - req.expectedTrimSize.width.value).abs <= tol &&
        (specs.trimSize.height.value - req.expectedTrimSize.height.value).abs <= tol
    val matchesRotated =
      (specs.trimSize.width.value - req.expectedTrimSize.height.value).abs <= tol &&
        (specs.trimSize.height.value - req.expectedTrimSize.width.value).abs <= tol

    val dimCheck =
      if matchesNormal || matchesRotated then
        PreflightCheckResult(PreflightCheckKind.TrimDimensions, CheckOutcome.Passed)
      else
        PreflightCheckResult(
          PreflightCheckKind.TrimDimensions,
          CheckOutcome.Failed(
            s"Trim size ${specs.trimSize.width.value}×${specs.trimSize.height.value}mm does not match expected " +
              s"${req.expectedTrimSize.width.value}×${req.expectedTrimSize.height.value}mm (±${tol}mm)"
          ),
        )

    val colorCheck =
      if req.allowedColorSpaces.contains(specs.colorSpace) then
        PreflightCheckResult(PreflightCheckKind.ColorSpace, CheckOutcome.Passed)
      else if specs.colorSpace == ColorSpace.RGB then
        PreflightCheckResult(
          PreflightCheckKind.ColorSpace,
          CheckOutcome.Warning("RGB artwork will be auto-converted to CMYK; minor color shift possible"),
        )
      else
        PreflightCheckResult(
          PreflightCheckKind.ColorSpace,
          CheckOutcome.Failed(s"Color space ${specs.colorSpace} not allowed (expected one of ${req.allowedColorSpaces.mkString(", ")})"),
        )

    val pageCheckOpt = req.expectedPageCount.map { expected =>
      if specs.pageCount == expected then
        PreflightCheckResult(PreflightCheckKind.PageCount, CheckOutcome.Passed)
      else
        PreflightCheckResult(
          PreflightCheckKind.PageCount,
          CheckOutcome.Failed(s"Expected $expected page(s), found ${specs.pageCount}"),
        )
    }

    val fontCheck =
      if !req.requireEmbeddedFonts || specs.fontsEmbedded then
        PreflightCheckResult(PreflightCheckKind.FontEmbedding, CheckOutcome.Passed)
      else
        PreflightCheckResult(
          PreflightCheckKind.FontEmbedding,
          CheckOutcome.Failed("Fonts are not embedded or converted to outlines"),
        )

    val transparencyCheck =
      if !specs.hasUnflattenedTransparency then
        PreflightCheckResult(PreflightCheckKind.Transparency, CheckOutcome.Passed)
      else
        PreflightCheckResult(
          PreflightCheckKind.Transparency,
          CheckOutcome.Warning("Unflattened transparency detected — verify overprint preview"),
        )

    PreflightReport(
      checks = List(resolutionCheck, bleedCheck, dimCheck, colorCheck, fontCheck, transparencyCheck) ++ pageCheckOpt.toList,
      evaluatedAt = now,
    )

/** Internal ArtworkSubmission aggregate with immutable revision history. */
private[artwork] final case class ArtworkSubmission(
    id: ArtworkId,
    owner: ArtworkOwner,
    componentKey: ComponentKey,
    versionIndex: Int,
    status: ArtworkStatus,
    revisions: NonEmptyChunk[ArtworkRevision],
):
  def latest: ArtworkRevision = revisions.last

  def addRevision(
      file: AssetFileMetadata,
      specs: ExtractedFileSpecs,
      reqOpt: Option[PreflightRequirement],
      now: Instant,
  ): Either[ArtworkError, ArtworkSubmission] =
    status match
      case _: ArtworkStatus.OperatorApproved =>
        Left(ArtworkError.AlreadyFinalized(id, status))
      case _ if file.sizeBytes <= 0 =>
        Left(ArtworkError.InvalidFileSize(file.sizeBytes))
      case _ =>
        val reportOpt = reqOpt.map(r => PreflightEvaluator.evaluate(specs, r, now))
        val newStatus = reportOpt match
          case None                        => ArtworkStatus.Uploaded
          case Some(r) if r.hasFailures    => ArtworkStatus.PreflightFailed
          case Some(_)                     => ArtworkStatus.PreflightPassed
        val rev = ArtworkRevision(revisions.size + 1, file, specs, reportOpt, now)
        Right(copy(status = newStatus, revisions = revisions.appended(rev)))

  def withPreflight(req: PreflightRequirement, now: Instant): ArtworkSubmission =
    val report    = PreflightEvaluator.evaluate(latest.extractedSpecs, req, now)
    val updatedRev = latest.copy(preflight = Some(report))
    val updatedRevs = NonEmptyChunk.fromIterableOption(revisions.init :+ updatedRev).getOrElse(revisions)
    val newStatus =
      if report.hasFailures then ArtworkStatus.PreflightFailed
      else ArtworkStatus.PreflightPassed
    copy(status = newStatus, revisions = updatedRevs)

  def approve(operatorId: EmployeeId, overrideFailures: Boolean, now: Instant): Either[ArtworkError, ArtworkSubmission] =
    status match
      case ArtworkStatus.PreflightFailed if !overrideFailures =>
        Left(ArtworkError.CannotApproveFailedPreflight(id))
      case _: ArtworkStatus.Rejected =>
        Left(ArtworkError.AlreadyFinalized(id, status))
      case _ =>
        Right(copy(status = ArtworkStatus.OperatorApproved(operatorId, now)))

  def requestChanges(notes: NonEmptyString): Either[ArtworkError, ArtworkSubmission] =
    status match
      case _: ArtworkStatus.OperatorApproved =>
        Left(ArtworkError.AlreadyFinalized(id, status))
      case _ =>
        Right(copy(status = ArtworkStatus.ChangesRequested(notes)))

  def reject(reason: NonEmptyString): Either[ArtworkError, ArtworkSubmission] =
    status match
      case _: ArtworkStatus.OperatorApproved =>
        Left(ArtworkError.AlreadyFinalized(id, status))
      case _ =>
        Right(copy(status = ArtworkStatus.Rejected(reason)))

  def withOwner(newOwner: ArtworkOwner): ArtworkSubmission =
    copy(owner = newOwner)

  def toView: ArtworkSubmissionView =
    ArtworkSubmissionView(
      id = id,
      owner = owner,
      componentKey = componentKey,
      versionIndex = versionIndex,
      status = status,
      latestRevision = latest,
      revisionCount = revisions.size,
      revisions = revisions.toList,
      createdAt = revisions.head.uploadedAt,
    )

private[artwork] object ArtworkSubmission:
  def create(cmd: UploadArtworkCmd, now: Instant): Either[ArtworkError, ArtworkSubmission] =
    if cmd.file.sizeBytes <= 0 then Left(ArtworkError.InvalidFileSize(cmd.file.sizeBytes))
    else if cmd.versionIndex < 1 then Left(ArtworkError.InvalidVersionIndex(cmd.versionIndex))
    else
      val reportOpt = cmd.requirement.map(r => PreflightEvaluator.evaluate(cmd.extractedSpecs, r, now))
      val status = reportOpt match
        case None                     => ArtworkStatus.Uploaded
        case Some(r) if r.hasFailures => ArtworkStatus.PreflightFailed
        case Some(_)                  => ArtworkStatus.PreflightPassed
      val rev = ArtworkRevision(1, cmd.file, cmd.extractedSpecs, reportOpt, now)
      Right(ArtworkSubmission(cmd.artworkId, cmd.owner, cmd.componentKey, cmd.versionIndex, status, NonEmptyChunk.single(rev)))

private[artwork] trait ArtworkRepository:
  def findById(id: ArtworkId): Option[ArtworkSubmission] < Async
  def findByOwner(owner: ArtworkOwner): List[ArtworkSubmission] < Async = Kyo.lift(Nil)
  def save(submission: ArtworkSubmission): Unit < Async
  def delete(id: ArtworkId): Unit < Async = Kyo.unit

private[artwork] final class ArtworkServiceLive(
    repo: ArtworkRepository,
    eventBus: EventBus,
) extends ArtworkService:

  override def upload(cmd: UploadArtworkCmd): ArtworkSubmissionView < (Async & Abort[ArtworkError]) =
    for
      now <- Effects.now
      sub <- (ArtworkSubmission.create(cmd, now)).orAbort
      _   <- repo.save(sub)
      _   <- eventBus.publish(ArtworkUploaded(sub.id, sub.owner, sub.componentKey, sub.latest.revisionNumber, now))
    yield sub.toView

  override def uploadRevision(cmd: UploadRevisionCmd): ArtworkSubmissionView < (Async & Abort[ArtworkError]) =
    for
      now <- Effects.now
      sub <- repo.findById(cmd.artworkId).someOrFail(ArtworkError.NotFound(cmd.artworkId))
      upd <- (sub.addRevision(cmd.file, cmd.extractedSpecs, cmd.requirement, now)).orAbort
      _   <- repo.save(upd)
      _   <- eventBus.publish(ArtworkUploaded(upd.id, upd.owner, upd.componentKey, upd.latest.revisionNumber, now))
    yield upd.toView

  override def runPreflight(artworkId: ArtworkId, requirement: PreflightRequirement): ArtworkSubmissionView < (Async & Abort[ArtworkError]) =
    for
      now <- Effects.now
      sub <- repo.findById(artworkId).someOrFail(ArtworkError.NotFound(artworkId))
      upd  = sub.withPreflight(requirement, now)
      _   <- repo.save(upd)
    yield upd.toView

  override def approve(
      artworkId: ArtworkId,
      operatorId: EmployeeId,
      overrideWarningsOrFailures: Boolean,
  ): ArtworkSubmissionView < (Async & Abort[ArtworkError]) =
    for
      now <- Effects.now
      sub <- repo.findById(artworkId).someOrFail(ArtworkError.NotFound(artworkId))
      upd <- (sub.approve(operatorId, overrideWarningsOrFailures, now)).orAbort
      _   <- repo.save(upd)
      _   <- eventBus.publish(ArtworkApproved(upd.id, upd.owner, operatorId, now))
    yield upd.toView

  override def requestChanges(artworkId: ArtworkId, notes: NonEmptyString): ArtworkSubmissionView < (Async & Abort[ArtworkError]) =
    for
      now <- Effects.now
      sub <- repo.findById(artworkId).someOrFail(ArtworkError.NotFound(artworkId))
      upd <- sub.requestChanges(notes).orAbort
      _   <- repo.save(upd)
      _   <- eventBus.publish(ArtworkChangesRequested(upd.id, upd.owner, notes, now))
    yield upd.toView

  override def reject(artworkId: ArtworkId, operatorId: EmployeeId, reason: NonEmptyString): ArtworkSubmissionView < (Async & Abort[ArtworkError]) =
    for
      now <- Effects.now
      sub <- repo.findById(artworkId).someOrFail(ArtworkError.NotFound(artworkId))
      upd <- sub.reject(reason).orAbort
      _   <- repo.save(upd)
      _   <- eventBus.publish(ArtworkChangesRequested(upd.id, upd.owner, reason, now))
    yield upd.toView

  override def reassignOwner(artworkId: ArtworkId, newOwner: ArtworkOwner): ArtworkSubmissionView < (Async & Abort[ArtworkError]) =
    for
      sub <- repo.findById(artworkId).someOrFail(ArtworkError.NotFound(artworkId))
      upd  = sub.withOwner(newOwner)
      _   <- repo.save(upd)
    yield upd.toView

  override def deleteDraft(artworkId: ArtworkId): Unit < (Async & Abort[ArtworkError]) =
    for
      sub <- repo.findById(artworkId).someOrFail(ArtworkError.NotFound(artworkId))
      _ <- sub.owner match
             case _: ArtworkOwner.DraftSession => repo.delete(artworkId)
             case other                        => Abort.fail(ArtworkError.CannotDeleteNonDraft(artworkId, other))
    yield ()

  override def getById(artworkId: ArtworkId): ArtworkSubmissionView < (Async & Abort[ArtworkError.NotFound]) =
    repo.findById(artworkId).someOrFail(new ArtworkError.NotFound(artworkId)).map(_.toView)

  override def listByOwner(owner: ArtworkOwner): List[ArtworkSubmissionView] < Async =
    repo.findByOwner(owner).map(_.map(_.toView))

private[artwork] object ArtworkServiceLive:
  def make(repo: ArtworkRepository, eventBus: EventBus): ArtworkService = ArtworkServiceLive(repo, eventBus)
