package mpbuilder.domain.pricing

import mpbuilder.domain.DimensionsMm

/** Sheet nesting (imposition) math for sheet-priced materials.
  *
  * All sheet-related conventions live in this one object so a change of press
  * sheet, margins, cut-counting, or the booklet page model is a one-line edit:
  *
  *   - Press sheet: SRA3, 320 × 450 mm.
  *   - Each copy needs 3 mm bleed on every side, and copies are separated by a
  *     2 mm gutter (spec doc: full-catalog-czk-price-list.md, header note).
  *   - Cuts per sheet: guillotine cuts along every column and row boundary
  *     including the four edge trims — (cols + 1) + (rows + 1). Stage-1
  *     convention; the docs price "per cut" without defining the count.
  *   - Booklet/calendar body: each nested copy-slot is one leaf printed on both
  *     sides, i.e. 2 pages. Stage-1 convention.
  */
object Imposition:
  val SheetWidthMm: Int  = 320
  val SheetHeightMm: Int = 450
  val BleedMm: Int       = 3
  val GutterMm: Int      = 2

  final case class Layout(copiesPerSheet: Int, cols: Int, rows: Int, rotated: Boolean):
    /** Guillotine cuts needed to separate one sheet into copies, incl. edge trims. */
    def cutsPerSheet: Int = (cols + 1) + (rows + 1)

  /** How many copies of `item` (trim size) nest onto one press sheet, trying
    * both orientations. None if the item doesn't fit at all.
    */
  def layout(item: DimensionsMm): Option[Layout] =
    def fit(spaceMm: Int, itemMm: Int): Int =
      if itemMm <= 0 || itemMm > spaceMm then 0
      else (spaceMm - itemMm) / (itemMm + GutterMm) + 1

    val w = item.widthMm + 2 * BleedMm
    val h = item.heightMm + 2 * BleedMm

    def candidate(itemW: Int, itemH: Int, rotated: Boolean): Option[Layout] =
      val cols = fit(SheetWidthMm, itemW)
      val rows = fit(SheetHeightMm, itemH)
      val copies = cols * rows
      Option.when(copies > 0)(Layout(copies, cols, rows, rotated))

    (candidate(w, h, rotated = false) ++ candidate(h, w, rotated = true))
      .maxByOption(_.copiesPerSheet)

  /** Physical press sheets needed to produce `quantity` finished single-leaf items. */
  def sheetsNeeded(quantity: Int, copiesPerSheet: Int): Int =
    if copiesPerSheet <= 0 then 0
    else (quantity + copiesPerSheet - 1) / copiesPerSheet

  /** Press sheets for a paged body component (booklets/calendars): every
    * copy-slot on the sheet is one leaf carrying 2 pages.
    */
  def bodySheetsNeeded(quantity: Int, pages: Int, copiesPerSheet: Int): Int =
    if copiesPerSheet <= 0 then 0
    else
      val leaves = quantity.toLong * ((pages + 1) / 2)
      ((leaves + copiesPerSheet - 1) / copiesPerSheet).toInt
