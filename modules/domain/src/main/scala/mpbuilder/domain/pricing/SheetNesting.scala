package mpbuilder.domain.pricing

object SheetNesting:
  def piecesPerSheet(
      sheetW: Double,
      sheetH: Double,
      itemW: Double,
      itemH: Double,
      bleedMm: Double,
      gutterMm: Double,
  ): Int =
    val effectiveW = itemW + 2 * bleedMm
    val effectiveH = itemH + 2 * bleedMm

    def countOrientation(ew: Double, eh: Double): Int =
      val cols = math.floor((sheetW + gutterMm) / (ew + gutterMm)).toInt
      val rows = math.floor((sheetH + gutterMm) / (eh + gutterMm)).toInt
      cols * rows

    val normal  = countOrientation(effectiveW, effectiveH)
    val rotated = countOrientation(effectiveH, effectiveW)
    math.max(math.max(normal, rotated), 1)
