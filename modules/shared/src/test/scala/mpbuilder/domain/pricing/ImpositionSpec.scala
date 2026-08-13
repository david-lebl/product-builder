package mpbuilder.domain.pricing

import mpbuilder.domain.DimensionsMm
import zio.test.*

object ImpositionSpec extends ZIOSpecDefault:

  def spec = suite("Imposition")(
    test("85×55 business card nests 21 per SRA3 sheet (golden, catalog doc §11)") {
      // effective 91×61 → upright 3 cols × 7 rows = 21 beats rotated 5 × 4 = 20
      val l = Imposition.layout(DimensionsMm(85, 55))
      assertTrue(
        l.map(_.copiesPerSheet).contains(21),
        l.map(_.cols).contains(3),
        l.map(_.rows).contains(7),
        l.exists(!_.rotated),
      )
    },
    test("200 cards at 21/sheet need 10 sheets") {
      assertTrue(Imposition.sheetsNeeded(200, 21) == 10)
    },
    test("exact multiples don't round up") {
      assertTrue(Imposition.sheetsNeeded(42, 21) == 2)
    },
    test("rotated orientation wins when it fits more copies") {
      // 200×90 → effective 206×96: upright 1×4=4, rotated (96×206) 3×2=6
      val l = Imposition.layout(DimensionsMm(200, 90))
      assertTrue(l.map(_.copiesPerSheet).contains(6), l.exists(_.rotated))
    },
    test("item larger than the sheet in both orientations doesn't fit") {
      assertTrue(Imposition.layout(DimensionsMm(500, 460)).isEmpty)
    },
    test("A4 fits 2 per sheet") {
      // 210×297 → effective 216×303: upright 1×1, rotated (303×216) 1×2
      val l = Imposition.layout(DimensionsMm(210, 297))
      assertTrue(l.map(_.copiesPerSheet).contains(2))
    },
    test("cuts per sheet counts all column and row boundaries incl. edge trims") {
      val l = Imposition.layout(DimensionsMm(85, 55)).get
      assertTrue(l.cutsPerSheet == (3 + 1) + (7 + 1))
    },
    test("body sheets: 100 booklets × 8 pages at 2 A4-slots/sheet = 200 sheets") {
      // 8 pages = 4 leaves per booklet; 100 × 4 = 400 leaves / 2 per sheet
      assertTrue(Imposition.bodySheetsNeeded(100, 8, 2) == 200)
    },
    test("body sheets round odd page counts up to whole leaves") {
      // 5 pages = 3 leaves; 10 copies × 3 = 30 leaves / 4 = 7.5 → 8
      assertTrue(Imposition.bodySheetsNeeded(10, 5, 4) == 8)
    },
  )
