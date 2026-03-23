package mpbuilder.domain

import zio.test.*
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.service.*
import mpbuilder.domain.sample.*

object VisualProductDomainMappingSpec extends ZIOSpecDefault:

  def spec = suite("VisualProductDomainMapping")(
    suite("toCategoryId")(
      test("MonthlyCalendar maps to cat-calendars") {
        val result = VisualProductDomainMapping.toCategoryId(VisualProductType.MonthlyCalendar)
        assertTrue(result.value == "cat-calendars")
      },
      test("WeeklyCalendar maps to cat-calendars") {
        val result = VisualProductDomainMapping.toCategoryId(VisualProductType.WeeklyCalendar)
        assertTrue(result.value == "cat-calendars")
      },
      test("BiweeklyCalendar maps to cat-calendars") {
        val result = VisualProductDomainMapping.toCategoryId(VisualProductType.BiweeklyCalendar)
        assertTrue(result.value == "cat-calendars")
      },
      test("PhotoBook maps to cat-booklets") {
        val result = VisualProductDomainMapping.toCategoryId(VisualProductType.PhotoBook)
        assertTrue(result.value == "cat-booklets")
      },
      test("WallPicture maps to cat-banners") {
        val result = VisualProductDomainMapping.toCategoryId(VisualProductType.WallPicture)
        assertTrue(result.value == "cat-banners")
      },
      test("all calendar types map to the same category") {
        val monthly = VisualProductDomainMapping.toCategoryId(VisualProductType.MonthlyCalendar)
        val weekly = VisualProductDomainMapping.toCategoryId(VisualProductType.WeeklyCalendar)
        val biweekly = VisualProductDomainMapping.toCategoryId(VisualProductType.BiweeklyCalendar)
        assertTrue(monthly == weekly && weekly == biweekly)
      },
      test("category IDs match SampleCatalog IDs") {
        val calId = VisualProductDomainMapping.toCategoryId(VisualProductType.MonthlyCalendar)
        val bookId = VisualProductDomainMapping.toCategoryId(VisualProductType.PhotoBook)
        val banId = VisualProductDomainMapping.toCategoryId(VisualProductType.WallPicture)
        assertTrue(
          calId == SampleCatalog.calendarsId,
          bookId == SampleCatalog.bookletsId,
          banId == SampleCatalog.bannersId,
        )
      },
    ),
    suite("toSpecifications")(
      test("MonthlyCalendar with WallCalendar produces correct specs") {
        val specs = VisualProductDomainMapping.toSpecifications(
          VisualProductType.MonthlyCalendar, ProductFormat.WallCalendar
        )
        val sizeSpec = specs.get(SpecKind.Size)
        val pagesSpec = specs.get(SpecKind.Pages)
        val orientSpec = specs.get(SpecKind.Orientation)
        assertTrue(
          sizeSpec == Some(SpecValue.SizeSpec(Dimension(210.0, 297.0))),
          pagesSpec == Some(SpecValue.PagesSpec(12)),
          orientSpec == Some(SpecValue.OrientationSpec(Orientation.Portrait)),
        )
      },
      test("WeeklyCalendar produces 52 pages") {
        val specs = VisualProductDomainMapping.toSpecifications(
          VisualProductType.WeeklyCalendar, ProductFormat.WallCalendar
        )
        assertTrue(specs.get(SpecKind.Pages) == Some(SpecValue.PagesSpec(52)))
      },
      test("BiweeklyCalendar produces 26 pages") {
        val specs = VisualProductDomainMapping.toSpecifications(
          VisualProductType.BiweeklyCalendar, ProductFormat.DeskCalendar
        )
        assertTrue(specs.get(SpecKind.Pages) == Some(SpecValue.PagesSpec(26)))
      },
      test("PhotoBook with square format produces correct dimensions") {
        val specs = VisualProductDomainMapping.toSpecifications(
          VisualProductType.PhotoBook, ProductFormat.PhotoBookSquare
        )
        assertTrue(
          specs.get(SpecKind.Size) == Some(SpecValue.SizeSpec(Dimension(210.0, 210.0))),
          specs.get(SpecKind.Pages) == Some(SpecValue.PagesSpec(12)),
        )
      },
      test("WallPicture produces 1 page") {
        val specs = VisualProductDomainMapping.toSpecifications(
          VisualProductType.WallPicture, ProductFormat.WallPictureSmall
        )
        assertTrue(specs.get(SpecKind.Pages) == Some(SpecValue.PagesSpec(1)))
      },
      test("landscape format produces Landscape orientation") {
        val specs = VisualProductDomainMapping.toSpecifications(
          VisualProductType.MonthlyCalendar, ProductFormat.DeskCalendar
        )
        assertTrue(
          specs.get(SpecKind.Orientation) == Some(SpecValue.OrientationSpec(Orientation.Landscape)),
        )
      },
    ),
    suite("isFormatApplicable")(
      test("calendar formats are applicable to calendar types") {
        assertTrue(
          VisualProductDomainMapping.isFormatApplicable(VisualProductType.MonthlyCalendar, ProductFormat.WallCalendar),
          VisualProductDomainMapping.isFormatApplicable(VisualProductType.WeeklyCalendar, ProductFormat.DeskCalendar),
          VisualProductDomainMapping.isFormatApplicable(VisualProductType.BiweeklyCalendar, ProductFormat.WallCalendarLarge),
        )
      },
      test("photo book formats are not applicable to calendar types") {
        assertTrue(
          !VisualProductDomainMapping.isFormatApplicable(VisualProductType.MonthlyCalendar, ProductFormat.PhotoBookSquare),
        )
      },
      test("calendar formats are not applicable to photo book") {
        assertTrue(
          !VisualProductDomainMapping.isFormatApplicable(VisualProductType.PhotoBook, ProductFormat.WallCalendar),
        )
      },
      test("wall picture formats are only applicable to wall picture") {
        assertTrue(
          VisualProductDomainMapping.isFormatApplicable(VisualProductType.WallPicture, ProductFormat.WallPictureLarge),
          !VisualProductDomainMapping.isFormatApplicable(VisualProductType.MonthlyCalendar, ProductFormat.WallPictureLarge),
          !VisualProductDomainMapping.isFormatApplicable(VisualProductType.PhotoBook, ProductFormat.WallPictureLarge),
        )
      },
    ),
    suite("ProductFormat")(
      test("formatsFor returns 4 calendar formats for calendar types") {
        assertTrue(
          ProductFormat.formatsFor(VisualProductType.MonthlyCalendar).size == 4,
          ProductFormat.formatsFor(VisualProductType.WeeklyCalendar).size == 4,
          ProductFormat.formatsFor(VisualProductType.BiweeklyCalendar).size == 4,
        )
      },
      test("formatsFor returns 3 photo book formats") {
        assertTrue(ProductFormat.formatsFor(VisualProductType.PhotoBook).size == 3)
      },
      test("formatsFor returns 3 wall picture formats") {
        assertTrue(ProductFormat.formatsFor(VisualProductType.WallPicture).size == 3)
      },
      test("defaultFor returns the first format for each type") {
        assertTrue(
          ProductFormat.defaultFor(VisualProductType.MonthlyCalendar) == ProductFormat.WallCalendar,
          ProductFormat.defaultFor(VisualProductType.PhotoBook) == ProductFormat.PhotoBookSquare,
          ProductFormat.defaultFor(VisualProductType.WallPicture) == ProductFormat.WallPictureSmall,
        )
      },
      test("findById returns correct format") {
        assertTrue(
          ProductFormat.findById("wall-calendar") == Some(ProductFormat.WallCalendar),
          ProductFormat.findById("photobook-square") == Some(ProductFormat.PhotoBookSquare),
          ProductFormat.findById("nonexistent") == None,
        )
      },
      test("all contains all 10 formats") {
        assertTrue(ProductFormat.all.size == 10)
      },
      test("orientation methods are correct") {
        assertTrue(
          ProductFormat.WallCalendar.isPortrait,
          ProductFormat.DeskCalendar.isLandscape,
          ProductFormat.PhotoBookSquare.isSquare,
          !ProductFormat.WallCalendar.isLandscape,
          !ProductFormat.DeskCalendar.isPortrait,
        )
      },
      test("toDimension converts to domain Dimension") {
        val dim = ProductFormat.WallCalendar.toDimension
        assertTrue(dim.widthMm == 210.0 && dim.heightMm == 297.0)
      },
      test("toSizeSpec converts to domain SizeSpec") {
        val spec = ProductFormat.WallCalendar.toSizeSpec
        assertTrue(spec == SpecValue.SizeSpec(Dimension(210.0, 297.0)))
      },
    ),
    suite("VisualProductType")(
      test("defaultPageCount returns correct values") {
        assertTrue(
          VisualProductType.MonthlyCalendar.defaultPageCount == 12,
          VisualProductType.WeeklyCalendar.defaultPageCount == 52,
          VisualProductType.BiweeklyCalendar.defaultPageCount == 26,
          VisualProductType.PhotoBook.defaultPageCount == 12,
          VisualProductType.WallPicture.defaultPageCount == 1,
        )
      },
      test("displayName returns English names") {
        assertTrue(
          VisualProductType.MonthlyCalendar.displayName(Language.En) == "Monthly Calendar",
          VisualProductType.PhotoBook.displayName(Language.En) == "Photo Book",
          VisualProductType.WallPicture.displayName(Language.En) == "Wall Picture",
        )
      },
      test("displayName returns Czech names") {
        assertTrue(
          VisualProductType.MonthlyCalendar.displayName(Language.Cs) == "Měsíční kalendář",
          VisualProductType.PhotoBook.displayName(Language.Cs) == "Fotokniha",
          VisualProductType.WallPicture.displayName(Language.Cs) == "Obraz na zeď",
        )
      },
    ),
  )
