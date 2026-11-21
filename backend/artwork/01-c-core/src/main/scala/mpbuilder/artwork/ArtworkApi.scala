package mpbuilder.artwork

import mpbuilder.shared.*
import zio.*
import java.time.Instant

// ── Public Models, Commands, Errors & Events ─────────────────────────────────

enum ArtworkOwner:
  case DraftSession(sessionId: NonEmptyString)
  case OrderLineRef(orderId: OrderId, lineIndex: Int)

enum ColorSpace:
  case CMYK, RGB, Grayscale, SpotPantone, CMYKWithSpot

final case class AssetFileMetadata(
    fileName: NonEmptyString,
    mimeType: NonEmptyString,
    sizeBytes: Long,
    storageKey: NonEmptyString,
    checksumSha256: Option[NonEmptyString] = None,
)

final case class ExtractedFileSpecs(
    resolutionDpi: Int,
    trimSize: Dimension2D,
    bleed: NonNegativeMm,
    colorSpace: ColorSpace,
    pageCount: Int,
    fontsEmbedded: Boolean,
    hasUnflattenedTransparency: Boolean = false,
)

/** Print-readiness contract owned by `artwork` (decoupled from `catalog` via ACL translation). */
final case class PreflightRequirement(
    expectedTrimSize: Dimension2D,
    dimensionToleranceMm: NonNegativeMm = NonNegativeMm.unsafe(1),
    requiredBleed: NonNegativeMm,
    minResolutionDpi: Int = 300,
    expectedPageCount: Option[Int] = None,
    allowedColorSpaces: Set[ColorSpace] = Set(ColorSpace.CMYK, ColorSpace.Grayscale, ColorSpace.CMYKWithSpot),
    requireEmbeddedFonts: Boolean = true,
)

enum PreflightCheckKind:
  case Resolution, Bleed, TrimDimensions, ColorSpace, PageCount, FontEmbedding, Transparency

enum CheckOutcome:
  case Passed
  case Warning(detail: String)
  case Failed(detail: String)

final case class PreflightCheckResult(
    kind: PreflightCheckKind,
    outcome: CheckOutcome,
)

final case class PreflightReport(
    checks: List[PreflightCheckResult],
    evaluatedAt: Instant,
):
  def hasFailures: Boolean = checks.exists(_.outcome.isInstanceOf[CheckOutcome.Failed])
  def hasWarnings: Boolean = !hasFailures && checks.exists(_.outcome.isInstanceOf[CheckOutcome.Warning])
  def isCleanPass: Boolean = checks.forall(_.outcome == CheckOutcome.Passed)

enum ArtworkStatus:
  case Uploaded
  case PreflightPassed
  case PreflightFailed
  case ChangesRequested(notes: NonEmptyString)
  case OperatorApproved(approvedBy: EmployeeId, approvedAt: Instant)
  case Rejected(reason: NonEmptyString)

final case class ArtworkRevision(
    revisionNumber: Int,
    file: AssetFileMetadata,
    extractedSpecs: ExtractedFileSpecs,
    preflight: Option[PreflightReport],
    uploadedAt: Instant,
)

final case class ArtworkSubmissionView(
    id: ArtworkId,
    owner: ArtworkOwner,
    componentKey: ComponentKey,
    versionIndex: Int,
    status: ArtworkStatus,
    latestRevision: ArtworkRevision,
    revisionCount: Int,
)

final case class UploadArtworkCmd(
    artworkId: ArtworkId,
    owner: ArtworkOwner,
    componentKey: ComponentKey,
    versionIndex: Int = 1,
    file: AssetFileMetadata,
    extractedSpecs: ExtractedFileSpecs,
    requirement: Option[PreflightRequirement] = None,
)

final case class UploadRevisionCmd(
    artworkId: ArtworkId,
    file: AssetFileMetadata,
    extractedSpecs: ExtractedFileSpecs,
    requirement: Option[PreflightRequirement] = None,
)

enum ArtworkError(val message: String):
  case NotFound(id: ArtworkId)
      extends ArtworkError(s"Artwork '${id.value}' not found")
  case InvalidFileSize(sizeBytes: Long)
      extends ArtworkError(s"Artwork file size must be positive, got $sizeBytes bytes")
  case InvalidVersionIndex(versionIndex: Int)
      extends ArtworkError(s"Artwork version index must be >= 1, got $versionIndex")
  case CannotApproveFailedPreflight(id: ArtworkId)
      extends ArtworkError(s"Artwork '${id.value}' has failed preflight checks and cannot be approved without override")
  case AlreadyFinalized(id: ArtworkId, status: ArtworkStatus)
      extends ArtworkError(s"Artwork '${id.value}' is already finalized in status $status")

// ── Domain Events ────────────────────────────────────────────────────────────

final case class ArtworkUploaded(
    artworkId: ArtworkId,
    owner: ArtworkOwner,
    componentKey: ComponentKey,
    revisionNumber: Int,
    occurredAt: Instant,
) extends DomainEvent

final case class ArtworkApproved(
    artworkId: ArtworkId,
    owner: ArtworkOwner,
    approvedBy: EmployeeId,
    occurredAt: Instant,
) extends DomainEvent

final case class ArtworkChangesRequested(
    artworkId: ArtworkId,
    owner: ArtworkOwner,
    notes: NonEmptyString,
    occurredAt: Instant,
) extends DomainEvent

// ── Public Inbound Port (Module API) ─────────────────────────────────────────

trait ArtworkService:
  def upload(cmd: UploadArtworkCmd): IO[ArtworkError, ArtworkSubmissionView]
  def uploadRevision(cmd: UploadRevisionCmd): IO[ArtworkError, ArtworkSubmissionView]
  def runPreflight(artworkId: ArtworkId, requirement: PreflightRequirement): IO[ArtworkError, ArtworkSubmissionView]
  def approve(artworkId: ArtworkId, operatorId: EmployeeId, overrideWarningsOrFailures: Boolean = false): IO[ArtworkError, ArtworkSubmissionView]
  def requestChanges(artworkId: ArtworkId, notes: NonEmptyString): IO[ArtworkError, ArtworkSubmissionView]
  def getById(artworkId: ArtworkId): IO[ArtworkError.NotFound, ArtworkSubmissionView]

object ArtworkService:
  def upload(cmd: UploadArtworkCmd): ZIO[ArtworkService, ArtworkError, ArtworkSubmissionView] =
    ZIO.serviceWithZIO[ArtworkService](_.upload(cmd))
