package mpbuilder.domain

import zio.test.*
import mpbuilder.domain.model.*
import mpbuilder.domain.sample.{SampleCatalog, SampleShowcase}

object ShowcaseSpec extends ZIOSpecDefault:

  def spec = suite("ShowcaseProduct")(
    suite("SampleShowcase")(
      test("all products reference valid category IDs") {
        val catalog = SampleCatalog.catalog
        val invalid = SampleShowcase.allProducts.filterNot(p =>
          catalog.categories.contains(p.categoryId)
        )
        assertTrue(invalid.isEmpty)
      },
      test("all products have non-empty tagline and description") {
        val missing = SampleShowcase.allProducts.filter { p =>
          p.tagline(Language.En).isEmpty ||
          p.tagline(Language.Cs).isEmpty ||
          p.detailedDescription(Language.En).isEmpty ||
          p.detailedDescription(Language.Cs).isEmpty
        }
        assertTrue(missing.isEmpty)
      },
      test("all products have a valid image URL") {
        val missing = SampleShowcase.allProducts.filter(_.imageUrl.isEmpty)
        assertTrue(missing.isEmpty)
      },
      test("all products have a catalog group assigned") {
        // Simply verify each product has a group and the groups are diverse
        val groups = SampleShowcase.allProducts.map(_.group).toSet
        assertTrue(
          groups.contains(CatalogGroup.Sheet),
          groups.contains(CatalogGroup.LargeFormat),
          groups.contains(CatalogGroup.Bound),
          groups.contains(CatalogGroup.Specialty),
        )
      },
      test("byGroup covers all products") {
        val totalInGroups = SampleShowcase.byGroup.values.flatten.toList.size
        assertTrue(totalInGroups == SampleShowcase.allProducts.size)
      },
      test("forCategory finds existing products") {
        val found = SampleShowcase.forCategory(SampleCatalog.businessCardsId)
        assertTrue(found.isDefined)
      },
      test("forCategory returns None for unknown category") {
        val notFound = SampleShowcase.forCategory(CategoryId.unsafe("nonexistent"))
        assertTrue(notFound.isEmpty)
      },
      test("products are sorted by sortOrder") {
        val orders = SampleShowcase.allProducts.map(_.sortOrder)
        assertTrue(orders == orders.sorted)
      },
      test("no duplicate category IDs in showcase") {
        val ids = SampleShowcase.allProducts.map(_.categoryId)
        assertTrue(ids.distinct.size == ids.size)
      },
      test("sheet group contains expected products") {
        val sheetIds = SampleShowcase.byGroup
          .getOrElse(CatalogGroup.Sheet, Nil)
          .map(_.categoryId)
          .toSet
        assertTrue(
          sheetIds.contains(SampleCatalog.businessCardsId),
          sheetIds.contains(SampleCatalog.flyersId),
          sheetIds.contains(SampleCatalog.brochuresId),
          sheetIds.contains(SampleCatalog.postcardsId),
        )
      },
    ),
  )
