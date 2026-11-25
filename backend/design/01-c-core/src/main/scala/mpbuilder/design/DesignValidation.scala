package mpbuilder.design

import mpbuilder.shared.{NonEmptyChunk, Validation}

/** Structural invariants of a [[DesignDocument]], enforced when a design is persisted (locally or on the server). */
object DesignValidation:

  val MaxPages: Int            = 200
  val MaxElementsPerPage: Int  = 300
  val MaxTextLength: Int       = 5000
  val MaxAssets: Int           = 1000
  val MaxTrimMm: Double        = 3000.0

  private def finite(v: Double): Boolean = !v.isNaN && !v.isInfinite

  def validate(doc: DesignDocument): Validation[String, DesignDocument] =
    val errors = List.newBuilder[String]
    def check(cond: Boolean, msg: => String): Unit = if !cond then errors += msg

    check(doc.formatVersion >= 1 && doc.formatVersion <= DesignDocument.CurrentFormatVersion, s"Unsupported format version ${doc.formatVersion}")

    val f = doc.format
    check(finite(f.trimWidthMm) && f.trimWidthMm > 0 && f.trimWidthMm <= MaxTrimMm, "Trim width out of range")
    check(finite(f.trimHeightMm) && f.trimHeightMm > 0 && f.trimHeightMm <= MaxTrimMm, "Trim height out of range")
    check(f.bleedMm >= 0 && f.bleedMm <= 25, "Bleed out of range")
    check(f.safeMarginMm >= 0 && f.safeMarginMm <= 50, "Safe margin out of range")

    val policy = doc.pagePolicy
    check(policy.minPages >= 1 && policy.maxPages >= policy.minPages && policy.maxPages <= MaxPages && policy.step >= 1, "Invalid page policy")
    check(doc.pages.nonEmpty, "A design needs at least one page")
    check(doc.pages.size <= MaxPages, s"At most $MaxPages pages")
    check(doc.pages.map(_.id).distinct.size == doc.pages.size, "Duplicate page ids")
    check(doc.assets.size <= MaxAssets, s"At most $MaxAssets images")
    check(doc.assets.map(_.id).distinct.size == doc.assets.size, "Duplicate asset ids")

    val elements = doc.allElements
    check(elements.map(_.id).distinct.size == elements.size, "Duplicate element ids")
    val assetIds = doc.assets.map(_.id).toSet

    doc.pages.foreach { page =>
      check(page.elements.size <= MaxElementsPerPage, s"Page ${page.id.value} has more than $MaxElementsPerPage elements")
      page.elements.foreach { el =>
        val fr = el.frame
        val ok = List(fr.x, fr.y, fr.width, fr.height, fr.rotation).forall(finite)
        check(ok && fr.width > 0 && fr.height > 0, s"Element ${el.id.value} has an invalid frame")
        check(el.opacity >= 0 && el.opacity <= 1, s"Element ${el.id.value} opacity out of range")
        el.content match
          case ElementContent.Text(text, style) =>
            check(text.length <= MaxTextLength, s"Element ${el.id.value} text too long")
            check(style.sizePt > 0 && style.sizePt <= 1000, s"Element ${el.id.value} font size out of range")
            check(FontCatalog.contains(style.fontKey), s"Element ${el.id.value} uses unknown font '${style.fontKey}'")
          case ElementContent.Image(asset, crop, _, _, _) =>
            asset.foreach(a => check(assetIds.contains(a), s"Element ${el.id.value} references missing image ${a.value}"))
            check(crop.zoom >= 0.1 && crop.zoom <= 10, s"Element ${el.id.value} zoom out of range")
          case ElementContent.Calendar(spec) =>
            check(spec.month >= 1 && spec.month <= 12 && spec.year >= 1900 && spec.year <= 2200, s"Element ${el.id.value} has an invalid calendar month")
            check(FontCatalog.contains(spec.fontKey), s"Element ${el.id.value} uses unknown font '${spec.fontKey}'")
          case _ => ()
      }
    }

    errors.result() match
      case Nil          => Validation.succeed(doc)
      case head :: tail => Validation.Failure(NonEmptyChunk(head, tail*))
