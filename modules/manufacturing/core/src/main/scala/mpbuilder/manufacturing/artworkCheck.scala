package mpbuilder.manufacturing

/** Artwork check flag status */
enum CheckStatus:
  case NotChecked, Passed, Warning, Failed

object CheckStatus:
  extension (cs: CheckStatus) def displayName: String = cs match
    case NotChecked => "Not Checked"
    case Passed     => "Passed"
    case Warning    => "Warning"
    case Failed     => "Failed"

  extension (cs: CheckStatus) def icon: String = cs match
    case NotChecked => "⬜"
    case Passed     => "✅"
    case Warning    => "⚠️"
    case Failed     => "❌"

/** Artwork review check for prepress file validation */
final case class ArtworkCheck(
    resolution: CheckStatus,
    bleed: CheckStatus,
    colorProfile: CheckStatus,
    notes: String,
)

object ArtworkCheck:
  val unchecked: ArtworkCheck = ArtworkCheck(CheckStatus.NotChecked, CheckStatus.NotChecked, CheckStatus.NotChecked, "")

  extension (ac: ArtworkCheck)
    def isFullyPassed: Boolean =
      ac.resolution == CheckStatus.Passed && ac.bleed == CheckStatus.Passed && ac.colorProfile == CheckStatus.Passed

    def hasIssues: Boolean =
      ac.resolution == CheckStatus.Failed || ac.bleed == CheckStatus.Failed || ac.colorProfile == CheckStatus.Failed

    def hasWarnings: Boolean =
      !ac.hasIssues && (ac.resolution == CheckStatus.Warning || ac.bleed == CheckStatus.Warning || ac.colorProfile == CheckStatus.Warning)
