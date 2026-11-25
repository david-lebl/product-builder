package mpbuilder.design

enum IssueSeverity derives CanEqual:
  case Error, Warning, Info

enum IssueKind derives CanEqual:
  /** Photo would print below the minimum acceptable resolution. */
  case VeryLowResolution(dpi: Int)
  /** Photo prints below the recommended resolution (visible softness). */
  case LowResolution(dpi: Int)
  /** A photo slot was left empty (prints as blank paper). */
  case EmptyPhotoSlot
  /** Text or another important element crosses the safe margin and may be trimmed off. */
  case OutsideSafeArea
  /** Content touches the trim edge without extending into the bleed (white hairline after cutting). */
  case ExtendToBleed
  /** Content overlaps the binding holes strip (wire-o / spiral). */
  case InBindingZone
  case TextTooSmall(sizePt: Double)
  case EmptyText
  /** The element references an image that is missing from the document. */
  case MissingImage
  /** The image is only stored on this device; it must be uploaded before ordering or syncing. */
  case NotUploaded

final case class DesignIssue(severity: IssueSeverity, kind: IssueKind, pageId: PageId, elementId: Option[ElementId]) derives CanEqual

/** Print-readiness checks the editor shows live and the order flow evaluates before the design is submitted. */
object DesignPreflight:

  val MinDpi: Int         = 150
  val RecommendedDpi: Int = 250
  val MinTextPt: Double   = 6.0
  private val Epsilon     = 0.05

  def check(doc: DesignDocument, includeUploadState: Boolean = false): List[DesignIssue] =
    val f     = doc.format
    val trim  = f.trimBox
    val safe  = f.safeBox
    val bleed = f.bleedBox
    doc.pages.toList.flatMap { page =>
      page.elements.toList.flatMap { el =>
        val b      = el.frame.bounds
        val issue  = (sev: IssueSeverity, k: IssueKind) => DesignIssue(sev, k, page.id, Some(el.id))
        val onPage = b.intersects(trim)

        val contentIssues: List[DesignIssue] = el.content match
          case ElementContent.Image(None, _, _, _, _) =>
            List(issue(IssueSeverity.Warning, IssueKind.EmptyPhotoSlot))
          case ElementContent.Image(Some(assetId), crop, _, _, _) =>
            doc.asset(assetId) match
              case None => List(issue(IssueSeverity.Error, IssueKind.MissingImage))
              case Some(asset) =>
                val dpi = DesignGeometry.effectiveDpi(el.frame, crop, asset.pixelWidth, asset.pixelHeight)
                val res =
                  if dpi < MinDpi then List(issue(IssueSeverity.Error, IssueKind.VeryLowResolution(dpi)))
                  else if dpi < RecommendedDpi then List(issue(IssueSeverity.Warning, IssueKind.LowResolution(dpi)))
                  else Nil
                val upload =
                  if includeUploadState && asset.storage == AssetStorage.DeviceOnly then List(issue(IssueSeverity.Info, IssueKind.NotUploaded))
                  else Nil
                res ++ upload
          case ElementContent.Text(text, style) =>
            val empty = if text.trim.isEmpty then List(issue(IssueSeverity.Info, IssueKind.EmptyText)) else Nil
            val small = if style.sizePt < MinTextPt then List(issue(IssueSeverity.Warning, IssueKind.TextTooSmall(style.sizePt))) else Nil
            empty ++ small
          case _ => Nil

        // Text & calendars must stay inside the safe area; decorative graphics may bleed.
        val needsSafety = el.content match
          case _: ElementContent.Text | _: ElementContent.Calendar => true
          case _                                                  => false
        val safety =
          if needsSafety && onPage && !(b.x >= safe.x - Epsilon && b.y >= safe.y - Epsilon && b.right <= safe.right + Epsilon && b.bottom <= safe.bottom + Epsilon)
          then List(issue(IssueSeverity.Warning, IssueKind.OutsideSafeArea))
          else Nil

        // Images, shapes and clipart touching a trim edge should run into the bleed.
        val bleedCheck = el.content match
          case _: ElementContent.Image | _: ElementContent.Shape if onPage && f.bleedMm > 0 && el.frame.rotation == 0.0 =>
            val near = 1.5
            val touches = List(
              math.abs(b.x - trim.x) <= near && b.x > bleed.x + Epsilon,
              math.abs(b.y - trim.y) <= near && b.y > bleed.y + Epsilon,
              math.abs(b.right - trim.right) <= near && b.right < bleed.right - Epsilon,
              math.abs(b.bottom - trim.bottom) <= near && b.bottom < bleed.bottom - Epsilon,
            )
            if touches.exists(identity) then List(issue(IssueSeverity.Warning, IssueKind.ExtendToBleed)) else Nil
          case _ => Nil

        val binding = f.bindingZone match
          case Some(zone) if needsSafety && b.intersects(zone) => List(issue(IssueSeverity.Warning, IssueKind.InBindingZone))
          case _                                               => Nil

        contentIssues ++ safety ++ bleedCheck ++ binding
      }
    }

  def errors(issues: List[DesignIssue]): List[DesignIssue]   = issues.filter(_.severity == IssueSeverity.Error)
  def warnings(issues: List[DesignIssue]): List[DesignIssue] = issues.filter(_.severity == IssueSeverity.Warning)

  /** Lowest effective resolution of any placed photo (`None` for purely vector designs). */
  def minimumDpi(doc: DesignDocument): Option[Int] =
    doc.pages.flatMap(_.elements).flatMap { el =>
      el.content match
        case ElementContent.Image(Some(a), crop, _, _, _) =>
          doc.asset(a).map(asset => DesignGeometry.effectiveDpi(el.frame, crop, asset.pixelWidth, asset.pixelHeight))
        case _ => None
    }.minOption
