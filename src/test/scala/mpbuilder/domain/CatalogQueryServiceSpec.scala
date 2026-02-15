package mpbuilder.domain

import zio.test.*
import mpbuilder.domain.model.*
import mpbuilder.domain.service.*
import mpbuilder.domain.sample.*

object CatalogQueryServiceSpec extends ZIOSpecDefault:

  private val catalog = SampleCatalog.catalog
  private val ruleset = SampleRules.ruleset

  def spec = suite("CatalogQueryService")(
    suite("availableMaterials")(
      test("returns allowed materials for business cards") {
        val materials = CatalogQueryService.availableMaterials(SampleCatalog.businessCardsId, catalog)
        val materialIds = materials.map(_.id).toSet
        assertTrue(
          materialIds.contains(SampleCatalog.coated300gsmId),
          materialIds.contains(SampleCatalog.uncoatedBondId),
          materialIds.contains(SampleCatalog.kraftId),
          !materialIds.contains(SampleCatalog.vinylId),
          !materialIds.contains(SampleCatalog.corrugatedId),
        )
      },
      test("returns only vinyl for banners") {
        val materials = CatalogQueryService.availableMaterials(SampleCatalog.bannersId, catalog)
        assertTrue(
          materials.size == 1,
          materials.head.id == SampleCatalog.vinylId,
        )
      },
      test("returns empty for unknown category") {
        val materials = CatalogQueryService.availableMaterials(CategoryId.unsafe("unknown"), catalog)
        assertTrue(materials.isEmpty)
      },
    ),
    suite("compatibleFinishes")(
      test("UV coating is filtered out for kraft paper in packaging") {
        val finishes = CatalogQueryService.compatibleFinishes(
          SampleCatalog.packagingId,
          SampleCatalog.kraftId,
          catalog,
          ruleset,
        )
        val finishIds = finishes.map(_.id).toSet
        assertTrue(
          !finishIds.contains(SampleCatalog.uvCoatingId),
          finishIds.contains(SampleCatalog.matteLaminationId),
        )
      },
      test("foil stamping is filtered out for kraft paper (no smooth surface)") {
        val finishes = CatalogQueryService.compatibleFinishes(
          SampleCatalog.packagingId,
          SampleCatalog.kraftId,
          catalog,
          ruleset,
        )
        val finishIds = finishes.map(_.id).toSet
        assertTrue(!finishIds.contains(SampleCatalog.foilStampingId))
      },
      test("coated paper allows all business card finishes") {
        val finishes = CatalogQueryService.compatibleFinishes(
          SampleCatalog.businessCardsId,
          SampleCatalog.coated300gsmId,
          catalog,
          ruleset,
        )
        val finishIds = finishes.map(_.id).toSet
        assertTrue(
          finishIds.contains(SampleCatalog.matteLaminationId),
          finishIds.contains(SampleCatalog.glossLaminationId),
          finishIds.contains(SampleCatalog.uvCoatingId),
          finishIds.contains(SampleCatalog.embossingId),
          finishIds.contains(SampleCatalog.foilStampingId),
        )
      },
    ),
    suite("requiredSpecifications")(
      test("business cards require Size, Quantity, ColorMode") {
        val specs = CatalogQueryService.requiredSpecifications(SampleCatalog.businessCardsId, catalog)
        assertTrue(
          specs == Set(SpecKind.Size, SpecKind.Quantity, SpecKind.ColorMode),
        )
      },
      test("brochures require Size, Quantity, ColorMode, FoldType, Pages") {
        val specs = CatalogQueryService.requiredSpecifications(SampleCatalog.brochuresId, catalog)
        assertTrue(
          specs == Set(SpecKind.Size, SpecKind.Quantity, SpecKind.ColorMode, SpecKind.FoldType, SpecKind.Pages),
        )
      },
      test("unknown category returns empty") {
        val specs = CatalogQueryService.requiredSpecifications(CategoryId.unsafe("unknown"), catalog)
        assertTrue(specs.isEmpty)
      },
    ),
  )
