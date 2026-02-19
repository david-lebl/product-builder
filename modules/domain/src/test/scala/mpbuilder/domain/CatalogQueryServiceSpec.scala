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
      test("UV coating is filtered out for kraft paper in packaging (property rule)") {
        val finishes = CatalogQueryService.compatibleFinishes(
          SampleCatalog.packagingId,
          SampleCatalog.kraftId,
          catalog,
          ruleset,
          None,
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
          None,
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
          None,
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
      test("weight rule filters lamination for lightweight paper") {
        val finishes = CatalogQueryService.compatibleFinishes(
          SampleCatalog.businessCardsId,
          SampleCatalog.uncoatedBondId,
          catalog,
          ruleset,
          None,
        )
        val finishIds = finishes.map(_.id).toSet
        assertTrue(
          !finishIds.contains(SampleCatalog.matteLaminationId),
          !finishIds.contains(SampleCatalog.glossLaminationId),
        )
      },
      test("aqueous coating filtered when digital printing selected") {
        val finishes = CatalogQueryService.compatibleFinishes(
          SampleCatalog.flyersId,
          SampleCatalog.coated300gsmId,
          catalog,
          ruleset,
          Some(SampleCatalog.digitalId),
        )
        val finishIds = finishes.map(_.id).toSet
        assertTrue(!finishIds.contains(SampleCatalog.aqueousCoatingId))
      },
      test("aqueous coating available when offset printing selected") {
        val finishes = CatalogQueryService.compatibleFinishes(
          SampleCatalog.flyersId,
          SampleCatalog.coated300gsmId,
          catalog,
          ruleset,
          Some(SampleCatalog.offsetId),
        )
        val finishIds = finishes.map(_.id).toSet
        assertTrue(finishIds.contains(SampleCatalog.aqueousCoatingId))
      },
      test("aqueous coating available when no printing method selected") {
        val finishes = CatalogQueryService.compatibleFinishes(
          SampleCatalog.flyersId,
          SampleCatalog.coated300gsmId,
          catalog,
          ruleset,
          None,
        )
        val finishIds = finishes.map(_.id).toSet
        assertTrue(finishIds.contains(SampleCatalog.aqueousCoatingId))
      },
    ),
    suite("requiredSpecifications")(
      test("business cards require Size, Quantity, InkConfig") {
        val specs = CatalogQueryService.requiredSpecifications(SampleCatalog.businessCardsId, catalog)
        assertTrue(
          specs == Set(SpecKind.Size, SpecKind.Quantity, SpecKind.InkConfig),
        )
      },
      test("brochures require Size, Quantity, InkConfig, FoldType, Pages") {
        val specs = CatalogQueryService.requiredSpecifications(SampleCatalog.brochuresId, catalog)
        assertTrue(
          specs == Set(SpecKind.Size, SpecKind.Quantity, SpecKind.InkConfig, SpecKind.FoldType, SpecKind.Pages),
        )
      },
      test("booklets require Size, Quantity, InkConfig, Pages, BindingMethod") {
        val specs = CatalogQueryService.requiredSpecifications(SampleCatalog.bookletsId, catalog)
        assertTrue(
          specs == Set(SpecKind.Size, SpecKind.Quantity, SpecKind.InkConfig, SpecKind.Pages, SpecKind.BindingMethod),
        )
      },
      test("unknown category returns empty") {
        val specs = CatalogQueryService.requiredSpecifications(CategoryId.unsafe("unknown"), catalog)
        assertTrue(specs.isEmpty)
      },
    ),
    suite("availablePrintingMethods")(
      test("returns allowed printing methods for business cards") {
        val methods = CatalogQueryService.availablePrintingMethods(SampleCatalog.businessCardsId, catalog)
        val methodIds = methods.map(_.id).toSet
        assertTrue(
          methodIds.contains(SampleCatalog.offsetId),
          methodIds.contains(SampleCatalog.digitalId),
          methodIds.contains(SampleCatalog.letterpressId),
          !methodIds.contains(SampleCatalog.uvInkjetId),
        )
      },
      test("returns only UV inkjet for banners") {
        val methods = CatalogQueryService.availablePrintingMethods(SampleCatalog.bannersId, catalog)
        assertTrue(
          methods.size == 1,
          methods.head.id == SampleCatalog.uvInkjetId,
        )
      },
      test("returns empty for unknown category") {
        val methods = CatalogQueryService.availablePrintingMethods(CategoryId.unsafe("unknown"), catalog)
        assertTrue(methods.isEmpty)
      },
    ),
  )
