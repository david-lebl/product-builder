package mpbuilder.domain

import zio.test.*
import mpbuilder.domain.model.*
import mpbuilder.domain.model.ArtworkCheck.*

object ArtworkCheckSpec extends ZIOSpecDefault:

  def spec = suite("ArtworkCheck")(
    suite("unchecked")(
      test("all flags are NotChecked") {
        val ac = ArtworkCheck.unchecked
        assertTrue(
          ac.resolution == CheckStatus.NotChecked,
          ac.bleed == CheckStatus.NotChecked,
          ac.colorProfile == CheckStatus.NotChecked,
          ac.notes.isEmpty,
        )
      },
    ),
    suite("isFullyPassed")(
      test("true when all flags are Passed") {
        val ac = ArtworkCheck(CheckStatus.Passed, CheckStatus.Passed, CheckStatus.Passed, "")
        assertTrue(ac.isFullyPassed)
      },
      test("false when any flag is not Passed") {
        val ac = ArtworkCheck(CheckStatus.Passed, CheckStatus.Warning, CheckStatus.Passed, "")
        assertTrue(!ac.isFullyPassed)
      },
      test("false when unchecked") {
        assertTrue(!ArtworkCheck.unchecked.isFullyPassed)
      },
    ),
    suite("hasIssues")(
      test("true when resolution is Failed") {
        val ac = ArtworkCheck(CheckStatus.Failed, CheckStatus.Passed, CheckStatus.Passed, "Low resolution (72 DPI)")
        assertTrue(ac.hasIssues)
      },
      test("true when bleed is Failed") {
        val ac = ArtworkCheck(CheckStatus.Passed, CheckStatus.Failed, CheckStatus.Passed, "No bleed area")
        assertTrue(ac.hasIssues)
      },
      test("true when colorProfile is Failed") {
        val ac = ArtworkCheck(CheckStatus.Passed, CheckStatus.Passed, CheckStatus.Failed, "RGB only")
        assertTrue(ac.hasIssues)
      },
      test("false when no flags are Failed") {
        val ac = ArtworkCheck(CheckStatus.Passed, CheckStatus.Warning, CheckStatus.Passed, "")
        assertTrue(!ac.hasIssues)
      },
    ),
    suite("hasWarnings")(
      test("true when there are warnings but no failures") {
        val ac = ArtworkCheck(CheckStatus.Passed, CheckStatus.Warning, CheckStatus.Passed, "")
        assertTrue(ac.hasWarnings)
      },
      test("false when there is a failure (even with warnings)") {
        val ac = ArtworkCheck(CheckStatus.Failed, CheckStatus.Warning, CheckStatus.Passed, "")
        assertTrue(!ac.hasWarnings)
      },
      test("false when all passed") {
        val ac = ArtworkCheck(CheckStatus.Passed, CheckStatus.Passed, CheckStatus.Passed, "")
        assertTrue(!ac.hasWarnings)
      },
    ),
    suite("CheckStatus extensions")(
      test("display names are defined for all statuses") {
        assertTrue(
          CheckStatus.NotChecked.displayName == "Not Checked",
          CheckStatus.Passed.displayName == "Passed",
          CheckStatus.Warning.displayName == "Warning",
          CheckStatus.Failed.displayName == "Failed",
        )
      },
      test("icons are defined for all statuses") {
        assertTrue(
          CheckStatus.NotChecked.icon.nonEmpty,
          CheckStatus.Passed.icon.nonEmpty,
          CheckStatus.Warning.icon.nonEmpty,
          CheckStatus.Failed.icon.nonEmpty,
        )
      },
    ),
    suite("PaymentStatus extensions")(
      test("display names are defined for all statuses") {
        assertTrue(
          PaymentStatus.Pending.displayName == "Pending",
          PaymentStatus.Confirmed.displayName == "Confirmed",
          PaymentStatus.Failed.displayName == "Failed",
        )
      },
      test("icons are defined for all statuses") {
        assertTrue(
          PaymentStatus.Pending.icon.nonEmpty,
          PaymentStatus.Confirmed.icon.nonEmpty,
          PaymentStatus.Failed.icon.nonEmpty,
        )
      },
    ),
  )
