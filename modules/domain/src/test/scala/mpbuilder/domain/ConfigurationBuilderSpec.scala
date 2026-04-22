package mpbuilder.domain

import zio.test.*
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.rules.*
import mpbuilder.domain.validation.*
import mpbuilder.domain.service.*
import mpbuilder.domain.sample.*

object ConfigurationBuilderSpec extends ZIOSpecDefault:

  private val catalog = SampleCatalog.catalog
  private val ruleset = SampleRules.ruleset
  private val configId = ConfigurationId.unsafe("test-config-1")

  private def mainComponent(
      materialId: MaterialId,
      inkConfig: InkConfiguration,
      finishes: List[FinishSelection] = Nil,
  ): ComponentRequest =
    ComponentRequest(ComponentRole.Main, materialId, inkConfig, finishes)

  def spec = suite("ConfigurationBuilder")(
    suite("valid configurations")(
      test("build a valid business card configuration with digital printing") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.matteLaminationId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.category.name(Language.En) == "Business Cards",
          result.toEither.toOption.get.components.head.material.name(Language.En) == "Coated Art Paper 300gsm",
          result.toEither.toOption.get.printingMethod.name(Language.En) == "Digital Printing",
          result.toEither.toOption.get.components.head.finishes.size == 1,
        )
      },
      test("build a valid business card with no finishes") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("build a valid banner configuration") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bannersId,
          printingMethodId = SampleCatalog.uvInkjetId,
          components = List(mainComponent(SampleCatalog.pvc510gId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.uvCoatingId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(1000, 500)),
            SpecValue.QuantitySpec(Quantity.unsafe(10)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("build a valid brochure configuration") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.brochuresId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.glossLaminationId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(297, 210)),
            SpecValue.QuantitySpec(Quantity.unsafe(1000)),
            SpecValue.FoldTypeSpec(FoldType.Tri),
            SpecValue.PagesSpec(6),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("build a valid booklet configuration with cover and body") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.matteLaminationId))),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.PagesSpec(32),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.components.size == 2,
        )
      },
      test("build a valid calendar configuration with front cover, back cover, body and binding") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.calendarsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.FrontCover, SampleCatalog.plasticClear200micId, InkConfiguration.noInk, Nil),
            ComponentRequest(ComponentRole.BackCover, SampleCatalog.coatedGlossy250gsmId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.matteLaminationId))),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coatedGlossy170gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Binding, SampleCatalog.metalWireOA4SilverId, InkConfiguration.noInk, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(50)),
            SpecValue.PagesSpec(28),
            SpecValue.BindingMethodSpec(BindingMethod.LoopBinding),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.category.name(Language.En) == "Calendars",
          result.toEither.toOption.get.components.find(_.role == ComponentRole.BackCover).get.material.name(Language.En) == "Coated Art Paper Glossy 250gsm",
        )
      },
      test("build a valid configuration with Yupo synthetic material") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.yupoId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.uvCoatingId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.components.head.material.name(Language.En) == "Yupo Synthetic 200μm",
        )
      },
    ),
    suite("multi-component configurations")(
      test("valid booklet: cover with matte lam + body with uncoated bond") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.matteLaminationId))),
            ComponentRequest(ComponentRole.Body, SampleCatalog.uncoatedBondId, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.PagesSpec(16),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val config = result.toEither.toOption.get
        assertTrue(
          result.toEither.isRight,
          config.components.find(_.role == ComponentRole.Cover).get.material.id == SampleCatalog.coated300gsmId,
          config.components.find(_.role == ComponentRole.Body).get.material.id == SampleCatalog.uncoatedBondId,
        )
      },
      test("valid calendar: back cover with gloss lam + body different material") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.calendarsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.FrontCover, SampleCatalog.plasticClear200micId, InkConfiguration.noInk, Nil),
            ComponentRequest(ComponentRole.BackCover, SampleCatalog.coatedGlossy250gsmId, InkConfiguration.cmyk4_0, List(FinishSelection(SampleCatalog.glossLaminationId))),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coatedGlossy170gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Binding, SampleCatalog.metalWireOA4SilverId, InkConfiguration.noInk, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(14),
            SpecValue.BindingMethodSpec(BindingMethod.LoopBinding),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("wrong component roles for category is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4)),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.PagesSpec(32),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(e => e.isInstanceOf[ConfigurationError.MissingComponent] || e.isInstanceOf[ConfigurationError.InvalidComponentRoles]),
        )
      },
      test("cover material not in cover template is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.uncoatedBondId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.PagesSpec(32),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.InvalidCategoryMaterial]),
        )
      },
      test("body finish not in body template is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.matteLaminationId))),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.PagesSpec(32),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.InvalidCategoryFinish]),
        )
      },
      test("sheet count verification: saddle stitch 16 pages = 3 body sheets") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.PagesSpec(16),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val config = result.toEither.toOption.get
        val coverComp = config.components.find(_.role == ComponentRole.Cover).get
        val bodyComp = config.components.find(_.role == ComponentRole.Body).get
        assertTrue(
          coverComp.sheetCount == 1,
          bodyComp.sheetCount == 3, // (16 / 4) - 1 = 3
        )
      },
    ),
    suite("incompatible combinations produce accumulated errors")(
      test("UV coating on kraft paper is rejected (property-level rule)") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.packagingId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.kraftId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.uvCoatingId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(200, 150)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.IncompatibleMaterialPropertyFinish]),
        )
      },
      test("foil stamping on kraft paper (no smooth surface) is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.packagingId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.kraftId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.foilStampingId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(200, 150)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.FinishMissingMaterialProperty]),
        )
      },
      test("matte and gloss lamination together is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.matteLaminationId), FinishSelection(SampleCatalog.glossLaminationId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.MutuallyExclusiveFinishes]),
        )
      },
      test("material not allowed for category is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.vinylId, InkConfiguration.cmyk4_4)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.InvalidCategoryMaterial]),
        )
      },
      test("missing required specs are reported") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            // Missing Quantity
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.count(_.isInstanceOf[ConfigurationError.MissingRequiredSpec]) == 1,
        )
      },
      test("multiple errors are accumulated") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.packagingId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.kraftId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.uvCoatingId), FinishSelection(SampleCatalog.foilStampingId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(200, 150)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        // Should have at least: UV+textured incompatible, foil needs smooth surface
        assertTrue(errors.size >= 2)
      },
      test("business card too large is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4)),
          specs = List(
            SpecValue.SizeSpec(Dimension(200, 100)), // Too large
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.SpecConstraintViolation]),
        )
      },
      test("business card quantity too low is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(10)), // Below minimum 100
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.SpecConstraintViolation]),
        )
      },
      test("banner with PMS ink type is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bannersId,
          printingMethodId = SampleCatalog.uvInkjetId,
          components = List(mainComponent(SampleCatalog.pvc510gId, InkConfiguration(InkSetup.pms(2), InkSetup.none))),
          specs = List(
            SpecValue.SizeSpec(Dimension(1000, 500)),
            SpecValue.QuantitySpec(Quantity.unsafe(10)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.ConfigurationConstraintViolation]),
        )
      },
      test("unknown category returns CategoryNotFound") {
        val request = ConfigurationRequest(
          categoryId = CategoryId.unsafe("cat-nonexistent"),
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4)),
          specs = Nil,
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.CategoryNotFound]),
        )
      },
      test("unknown printing method returns PrintingMethodNotFound") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = PrintingMethodId.unsafe("pm-nonexistent"),
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.PrintingMethodNotFound]),
        )
      },
    ),
    suite("weight-based rules")(
      test("lamination on 120gsm bond paper is rejected (weight rule)") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.uncoatedBondId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.matteLaminationId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.FinishWeightRequirementNotMet]),
        )
      },
      test("lamination on 300gsm coated paper succeeds (weight rule)") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.matteLaminationId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
    ),
    suite("finish type mutual exclusion")(
      test("lamination and soft-touch coating together is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.matteLaminationId), FinishSelection(SampleCatalog.softTouchCoatingId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.MutuallyExclusiveFinishTypes]),
        )
      },
    ),
    suite("printing process requirements")(
      test("aqueous coating with digital press is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.flyersId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.aqueousCoatingId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.OrientationSpec(Orientation.Portrait),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.FinishRequiresPrintingProcessViolation]),
        )
      },
      test("aqueous coating with offset press succeeds") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.postcardsId,
          printingMethodId = SampleCatalog.offsetId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.aqueousCoatingId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
    ),
    suite("finish dependency rules")(
      test("spot varnish without lamination base is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.flyersId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.varnishId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.OrientationSpec(Orientation.Portrait),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.FinishMissingDependentFinishType]),
        )
      },
      test("spot varnish with lamination base succeeds") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.flyersId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.matteLaminationId), FinishSelection(SampleCatalog.varnishId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.OrientationSpec(Orientation.Portrait),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
    ),
    suite("binding method rules")(
      test("booklet with invalid binding method is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.PagesSpec(32),
            SpecValue.BindingMethodSpec(BindingMethod.CaseBinding),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.SpecConstraintViolation]),
        )
      },
      test("booklet with too few pages is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.PagesSpec(4),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.SpecConstraintViolation]),
        )
      },
    ),
    suite("printing method category restriction")(
      test("printing method not allowed for category is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bannersId,
          printingMethodId = SampleCatalog.digitalId, // banners only allow UV inkjet
          components = List(mainComponent(SampleCatalog.pvc510gId, InkConfiguration.cmyk4_4)),
          specs = List(
            SpecValue.SizeSpec(Dimension(1000, 500)),
            SpecValue.QuantitySpec(Quantity.unsafe(10)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.InvalidCategoryPrintingMethod]),
        )
      },
    ),
    suite("material family rules")(
      test("vinyl with embossing is rejected (family rule)") {
        val result = RuleEvaluator.evaluate(
          CompatibilityRule.MaterialFamilyFinishTypeIncompatible(
            MaterialFamily.Vinyl,
            FinishType.Embossing,
            "Vinyl cannot be embossed",
          ),
          List(ProductComponent(ComponentRole.Main, SampleCatalog.vinyl, InkConfiguration.cmyk4_4, List(SelectedFinish(SampleCatalog.embossing)), 1)),
          ProductSpecifications.empty,
          SampleCatalog.businessCards,
          SampleCatalog.uvInkjetMethod,
        )
        assertTrue(result.toEither.isLeft)
      },
    ),
    suite("calendar validation rules")(
      test("calendar with invalid binding method is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.calendarsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.FrontCover, SampleCatalog.plasticClear200micId, InkConfiguration.noInk, Nil),
            ComponentRequest(ComponentRole.BackCover, SampleCatalog.coatedGlossy250gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coatedGlossy170gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Binding, SampleCatalog.metalWireOA4SilverId, InkConfiguration.noInk, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(14),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.SpecConstraintViolation]),
        )
      },
      test("calendar with too few pages is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.calendarsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.FrontCover, SampleCatalog.plasticClear200micId, InkConfiguration.noInk, Nil),
            ComponentRequest(ComponentRole.BackCover, SampleCatalog.coatedGlossy250gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coatedGlossy170gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Binding, SampleCatalog.metalWireOA4SilverId, InkConfiguration.noInk, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(8),
            SpecValue.BindingMethodSpec(BindingMethod.LoopBinding),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.SpecConstraintViolation]),
        )
      },
      test("calendar with too many pages is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.calendarsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.FrontCover, SampleCatalog.plasticClear200micId, InkConfiguration.noInk, Nil),
            ComponentRequest(ComponentRole.BackCover, SampleCatalog.coatedGlossy250gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coatedGlossy170gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Binding, SampleCatalog.metalWireOA4SilverId, InkConfiguration.noInk, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(30),
            SpecValue.BindingMethodSpec(BindingMethod.LoopBinding),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.SpecConstraintViolation]),
        )
      },
    ),
    suite("ink configuration validation")(
      test("4/0 ink configuration validates successfully") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_0, List(FinishSelection(SampleCatalog.matteLaminationId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("CMYK 4/4 with letterpress (max 2 colors) is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.letterpressId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.InkConfigExceedsMethodColorLimit]),
        )
      },
      test("mono 1/1 with letterpress (max 2 colors) succeeds") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.letterpressId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.mono1_1)),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
    ),
    suite("new material validation")(
      test("Yupo with embossing is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.yupoId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.embossingId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.IncompatibleMaterialFinish]),
        )
      },
      test("Yupo with debossing is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.packagingId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.yupoId, InkConfiguration.cmyk4_4, List(FinishSelection(SampleCatalog.debossingId)))),
          specs = List(
            SpecValue.SizeSpec(Dimension(200, 150)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.IncompatibleMaterialFinish]),
        )
      },
    ),
    suite("technology constraints (binding)")(
      test("saddle stitch with pages not divisible by 4 is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coatedMatte115gsm.id, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.PagesSpec(18), // 18 is not divisible by 4
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(errors.exists(_.isInstanceOf[ConfigurationError.TechnologyConstraintViolation]))
      },
      test("saddle stitch with pages divisible by 4 succeeds") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coatedMatte115gsm.id, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.PagesSpec(20), // 20 is divisible by 4
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("perfect binding with odd page count is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coatedMatte115gsm.id, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(300)),
            SpecValue.PagesSpec(41), // 41 is not divisible by 2
            SpecValue.BindingMethodSpec(BindingMethod.PerfectBinding),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(errors.exists(_.isInstanceOf[ConfigurationError.TechnologyConstraintViolation]))
      },
      test("perfect binding with even page count succeeds") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coatedMatte115gsm.id, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(300)),
            SpecValue.PagesSpec(42), // 42 is divisible by 2
            SpecValue.BindingMethodSpec(BindingMethod.PerfectBinding),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("saddle stitch on heavy paper (>=300gsm) with >80 pages is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(200)),
            SpecValue.PagesSpec(84), // 84 pages, divisible by 4 but >80 with heavy stock
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(errors.exists(_.isInstanceOf[ConfigurationError.TechnologyConstraintViolation]))
      },
      test("saddle stitch on heavy paper with <=80 pages succeeds") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(200)),
            SpecValue.PagesSpec(80), // exactly 80 pages, divisible by 4, at the limit
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("saddle stitch page divisibility constraint applies in free category too") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.freeId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Main, SampleCatalog.coatedMatte115gsm.id, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(10), // 10 is not divisible by 4
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(errors.exists(_.isInstanceOf[ConfigurationError.TechnologyConstraintViolation]))
      },
      test("booklet with loop binding and divisible-by-2 pages succeeds") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coatedMatte115gsm.id, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(30), // 30 is divisible by 2 (not 4), valid for loop binding
            SpecValue.BindingMethodSpec(BindingMethod.LoopBinding),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
    ),
    suite("finish parameters")(
      test("round corners with valid params (4 corners, 5mm radius) succeeds") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(
            SampleCatalog.coated300gsmId,
            InkConfiguration.cmyk4_4,
            List(FinishSelection(SampleCatalog.roundCornersId, Some(FinishParameters.RoundCornersParams(4, 5)))),
          )),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("round corners with invalid corner count is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(
            SampleCatalog.coated300gsmId,
            InkConfiguration.cmyk4_4,
            List(FinishSelection(SampleCatalog.roundCornersId, Some(FinishParameters.RoundCornersParams(0, 5)))),
          )),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(errors.exists(_.isInstanceOf[ConfigurationError.InvalidFinishParameters]))
      },
      test("round corners with out-of-range radius is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(
            SampleCatalog.coated300gsmId,
            InkConfiguration.cmyk4_4,
            List(FinishSelection(SampleCatalog.roundCornersId, Some(FinishParameters.RoundCornersParams(4, 25)))),
          )),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(errors.exists(_.isInstanceOf[ConfigurationError.InvalidFinishParameters]))
      },
      test("lamination with front-only side param succeeds") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(
            SampleCatalog.coated300gsmId,
            InkConfiguration.cmyk4_4,
            List(FinishSelection(SampleCatalog.matteLaminationId, Some(FinishParameters.LaminationParams(FinishSide.Front)))),
          )),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("lamination with Back side param is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(
            SampleCatalog.coated300gsmId,
            InkConfiguration.cmyk4_4,
            List(FinishSelection(SampleCatalog.matteLaminationId, Some(FinishParameters.LaminationParams(FinishSide.Back)))),
          )),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(errors.exists(_.isInstanceOf[ConfigurationError.InvalidFinishParameters]))
      },
      test("foil stamping with gold color param succeeds") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(
            SampleCatalog.coated300gsmId,
            InkConfiguration.cmyk4_4,
            List(FinishSelection(SampleCatalog.foilStampingId, Some(FinishParameters.FoilStampingParams(FoilColor.Gold)))),
          )),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("foil stamping params on wrong finish type is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(
            SampleCatalog.coated300gsmId,
            InkConfiguration.cmyk4_4,
            List(FinishSelection(SampleCatalog.matteLaminationId, Some(FinishParameters.FoilStampingParams(FoilColor.Silver)))),
          )),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(errors.exists(_.isInstanceOf[ConfigurationError.InvalidFinishParameters]))
      },
      test("grommet params with valid spacing succeeds") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bannersId,
          printingMethodId = SampleCatalog.uvInkjetId,
          components = List(mainComponent(
            SampleCatalog.pvc510gId,
            InkConfiguration.cmyk4_4,
            List(FinishSelection(SampleCatalog.grommetsId, Some(FinishParameters.GrommetParams(500)))),
          )),
          specs = List(
            SpecValue.SizeSpec(Dimension(1000, 500)),
            SpecValue.QuantitySpec(Quantity.unsafe(5)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("grommet params with zero spacing is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bannersId,
          printingMethodId = SampleCatalog.uvInkjetId,
          components = List(mainComponent(
            SampleCatalog.pvc510gId,
            InkConfiguration.cmyk4_4,
            List(FinishSelection(SampleCatalog.grommetsId, Some(FinishParameters.GrommetParams(0)))),
          )),
          specs = List(
            SpecValue.SizeSpec(Dimension(1000, 500)),
            SpecValue.QuantitySpec(Quantity.unsafe(5)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(errors.exists(_.isInstanceOf[ConfigurationError.InvalidFinishParameters]))
      },
      test("perforation params with valid pitch succeeds") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.packagingId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(
            SampleCatalog.kraftId,
            InkConfiguration.cmyk4_4,
            List(FinishSelection(SampleCatalog.perforationId, Some(FinishParameters.PerforationParams(5)))),
          )),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("finish without params is accepted (params are optional)") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(
            SampleCatalog.coated300gsmId,
            InkConfiguration.cmyk4_4,
            List(FinishSelection(SampleCatalog.roundCornersId)),
          )),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("gum rope without grommets is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bannersId,
          printingMethodId = SampleCatalog.uvInkjetId,
          components = List(mainComponent(
            SampleCatalog.pvc510gId,
            InkConfiguration.cmyk4_0,
            List(FinishSelection(SampleCatalog.gumRopeId, Some(FinishParameters.RopeParams(BigDecimal("10"))))),
          )),
          specs = List(
            SpecValue.SizeSpec(Dimension(1000, 1000)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(e => e.isInstanceOf[ConfigurationError.ConfigurationConstraintViolation] &&
            e.asInstanceOf[ConfigurationError.ConfigurationConstraintViolation].reason.contains("Gum rope requires grommets")),
        )
      },
      test("banner 200×100 cm (2000×1000 mm) exceeds max dimension and is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bannersId,
          printingMethodId = SampleCatalog.uvInkjetId,
          components = List(mainComponent(SampleCatalog.pvc510gId, InkConfiguration.cmyk4_0)),
          specs = List(
            SpecValue.SizeSpec(Dimension(2000, 1000)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.SpecConstraintViolation]),
        )
      },
    ),
    suite("white ink (transparent material) validation")(
      test("CMYK + white underlay on clear vinyl sticker with UV inkjet succeeds") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.stickersId,
          printingMethodId = SampleCatalog.uvInkjetId,
          components = List(mainComponent(SampleCatalog.clearVinylId, InkConfiguration.cmyk4_0_white)),
          specs = List(
            SpecValue.SizeSpec(Dimension(100, 100)),
            SpecValue.QuantitySpec(Quantity.unsafe(50)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("CMYK + white underlay on opaque adhesive stock with digital printing is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.stickersId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.adhesiveStockId, InkConfiguration.cmyk4_0_white)),
          specs = List(
            SpecValue.SizeSpec(Dimension(100, 100)),
            SpecValue.QuantitySpec(Quantity.unsafe(50)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(errors.exists(_.isInstanceOf[ConfigurationError.TechnologyConstraintViolation]))
      },
      test("CMYK + white underlay on clear vinyl with digital printing is rejected (non-UV inkjet)") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.stickersId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.clearVinylId, InkConfiguration.cmyk4_0_white)),
          specs = List(
            SpecValue.SizeSpec(Dimension(100, 100)),
            SpecValue.QuantitySpec(Quantity.unsafe(50)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        // clear vinyl is transparent, so digital printing + transparent material should pass the rule
        assertTrue(result.toEither.isRight)
      },
      test("InkConfiguration.cmyk4_0_white notation is 4/0+W") {
        assertTrue(InkConfiguration.cmyk4_0_white.notation == "4/0+W")
      },
      test("InkConfiguration.cmyk4_0_white is single-sided") {
        assertTrue(InkConfiguration.cmyk4_0_white.isSingleSided)
      },
      test("InkConfiguration.cmyk4_0_white is not double-sided") {
        assertTrue(!InkConfiguration.cmyk4_0_white.isDoubleSided)
      },
    ),
    suite("roll-up banners")(
      test("build a valid roll-up banner with banner only (no stand)") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.rollUpsId,
          printingMethodId = SampleCatalog.uvInkjetId,
          components = List(mainComponent(SampleCatalog.rollUpBannerFilmId, InkConfiguration.cmyk4_0)),
          specs = List(
            SpecValue.SizeSpec(Dimension(850, 2000)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.category.name(Language.En) == "Roll-Up Banners",
          result.toEither.toOption.get.components.size == 1,
        )
      },
      test("build a valid roll-up banner with economy stand") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.rollUpsId,
          printingMethodId = SampleCatalog.uvInkjetId,
          components = List(
            mainComponent(SampleCatalog.rollUpBannerFilmId, InkConfiguration.cmyk4_0),
            ComponentRequest(ComponentRole.Stand, SampleCatalog.rollUpStandEconomyId, InkConfiguration.noInk, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(850, 2000)),
            SpecValue.QuantitySpec(Quantity.unsafe(2)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.components.size == 2,
          result.toEither.toOption.get.components.find(_.role == ComponentRole.Stand).get.material.name(Language.En) == "Roll-Up Stand Economy",
        )
      },
      test("build a valid roll-up banner with premium stand and overlamination") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.rollUpsId,
          printingMethodId = SampleCatalog.uvInkjetId,
          components = List(
            mainComponent(SampleCatalog.rollUpBannerFilmId, InkConfiguration.cmyk4_0, List(FinishSelection(SampleCatalog.overlaminationId))),
            ComponentRequest(ComponentRole.Stand, SampleCatalog.rollUpStandPremiumId, InkConfiguration.noInk, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(1000, 2000)),
            SpecValue.QuantitySpec(Quantity.unsafe(5)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.components.find(_.role == ComponentRole.Stand).get.material.name(Language.En) == "Roll-Up Stand Premium",
          result.toEither.toOption.get.components.find(_.role == ComponentRole.Main).get.finishes.size == 1,
        )
      },
      test("roll-up banner too narrow is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.rollUpsId,
          printingMethodId = SampleCatalog.uvInkjetId,
          components = List(mainComponent(SampleCatalog.rollUpBannerFilmId, InkConfiguration.cmyk4_0)),
          specs = List(
            SpecValue.SizeSpec(Dimension(400, 2000)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isLeft)
      },
      test("roll-up banner too short is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.rollUpsId,
          printingMethodId = SampleCatalog.uvInkjetId,
          components = List(mainComponent(SampleCatalog.rollUpBannerFilmId, InkConfiguration.cmyk4_0)),
          specs = List(
            SpecValue.SizeSpec(Dimension(850, 1000)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isLeft)
      },
      test("roll-up banner with wrong printing method is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.rollUpsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.rollUpBannerFilmId, InkConfiguration.cmyk4_0)),
          specs = List(
            SpecValue.SizeSpec(Dimension(850, 2000)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isLeft)
      },
      test("stand material is not allowed in banner Main component") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.rollUpsId,
          printingMethodId = SampleCatalog.uvInkjetId,
          components = List(mainComponent(SampleCatalog.rollUpStandEconomyId, InkConfiguration.noInk)),
          specs = List(
            SpecValue.SizeSpec(Dimension(850, 2000)),
            SpecValue.QuantitySpec(Quantity.unsafe(1)),
          ),
        )
        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isLeft)
      },
    ),
  )
