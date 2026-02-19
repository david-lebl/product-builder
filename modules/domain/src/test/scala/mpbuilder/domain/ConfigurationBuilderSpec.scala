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
      finishIds: List[FinishId] = Nil,
  ): ComponentRequest =
    ComponentRequest(ComponentRole.Main, materialId, inkConfig, finishIds)

  def spec = suite("ConfigurationBuilder")(
    suite("valid configurations")(
      test("build a valid business card configuration with offset printing") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.offsetId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(SampleCatalog.matteLaminationId))),
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
          result.toEither.toOption.get.printingMethod.name(Language.En) == "Offset Printing",
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
          components = List(mainComponent(SampleCatalog.vinylId, InkConfiguration.cmyk4_4, List(SampleCatalog.uvCoatingId))),
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
          printingMethodId = SampleCatalog.offsetId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(SampleCatalog.glossLaminationId))),
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
          printingMethodId = SampleCatalog.offsetId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(SampleCatalog.matteLaminationId)),
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
      test("build a valid calendar configuration with cover and body") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.calendarsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coatedSilk250gsmId, InkConfiguration.cmyk4_4, List(SampleCatalog.matteLaminationId)),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coatedSilk250gsmId, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(297, 210)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(14),
            SpecValue.BindingMethodSpec(BindingMethod.SpiralBinding),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.category.name(Language.En) == "Calendars",
          result.toEither.toOption.get.components.find(_.role == ComponentRole.Cover).get.material.name(Language.En) == "Coated Silk 250gsm",
        )
      },
      test("build a valid configuration with Yupo synthetic material") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(mainComponent(SampleCatalog.yupoId, InkConfiguration.cmyk4_4, List(SampleCatalog.uvCoatingId))),
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
          printingMethodId = SampleCatalog.offsetId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(SampleCatalog.matteLaminationId)),
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
      test("valid calendar: cover with gloss lam + body different material") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.calendarsId,
          printingMethodId = SampleCatalog.digitalId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coatedSilk250gsmId, InkConfiguration.cmyk4_0, List(SampleCatalog.glossLaminationId)),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(297, 210)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(14),
            SpecValue.BindingMethodSpec(BindingMethod.SpiralBinding),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("wrong component roles for category is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          printingMethodId = SampleCatalog.offsetId,
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
          printingMethodId = SampleCatalog.offsetId,
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
          printingMethodId = SampleCatalog.offsetId,
          components = List(
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(SampleCatalog.matteLaminationId)),
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
          printingMethodId = SampleCatalog.offsetId,
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
          printingMethodId = SampleCatalog.offsetId,
          components = List(mainComponent(SampleCatalog.kraftId, InkConfiguration.cmyk4_4, List(SampleCatalog.uvCoatingId))),
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
          printingMethodId = SampleCatalog.offsetId,
          components = List(mainComponent(SampleCatalog.kraftId, InkConfiguration.cmyk4_4, List(SampleCatalog.foilStampingId))),
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
          printingMethodId = SampleCatalog.offsetId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(SampleCatalog.matteLaminationId, SampleCatalog.glossLaminationId))),
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
          printingMethodId = SampleCatalog.offsetId,
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
          printingMethodId = SampleCatalog.offsetId,
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
          printingMethodId = SampleCatalog.offsetId,
          components = List(mainComponent(SampleCatalog.kraftId, InkConfiguration.cmyk4_4, List(SampleCatalog.uvCoatingId, SampleCatalog.foilStampingId))),
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
          printingMethodId = SampleCatalog.offsetId,
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
          printingMethodId = SampleCatalog.offsetId,
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
          components = List(mainComponent(SampleCatalog.vinylId, InkConfiguration(InkSetup.pms(2), InkSetup.none))),
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
          printingMethodId = SampleCatalog.offsetId,
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
          printingMethodId = SampleCatalog.offsetId,
          components = List(mainComponent(SampleCatalog.uncoatedBondId, InkConfiguration.cmyk4_4, List(SampleCatalog.matteLaminationId))),
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
          printingMethodId = SampleCatalog.offsetId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(SampleCatalog.matteLaminationId))),
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
          printingMethodId = SampleCatalog.offsetId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(SampleCatalog.matteLaminationId, SampleCatalog.softTouchCoatingId))),
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
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(SampleCatalog.aqueousCoatingId))),
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
          categoryId = SampleCatalog.flyersId,
          printingMethodId = SampleCatalog.offsetId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(SampleCatalog.aqueousCoatingId))),
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
    suite("finish dependency rules")(
      test("spot varnish without lamination base is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.flyersId,
          printingMethodId = SampleCatalog.offsetId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(SampleCatalog.varnishId))),
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
          printingMethodId = SampleCatalog.offsetId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_4, List(SampleCatalog.matteLaminationId, SampleCatalog.varnishId))),
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
          printingMethodId = SampleCatalog.offsetId,
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
          printingMethodId = SampleCatalog.offsetId,
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
          printingMethodId = SampleCatalog.offsetId, // banners only allow UV inkjet
          components = List(mainComponent(SampleCatalog.vinylId, InkConfiguration.cmyk4_4)),
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
          List(ProductComponent(ComponentRole.Main, SampleCatalog.vinyl, InkConfiguration.cmyk4_4, List(SampleCatalog.embossing), 1)),
          ProductSpecifications.empty,
          SampleCatalog.bannersId,
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
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coatedSilk250gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coatedSilk250gsmId, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(297, 210)),
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
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coatedSilk250gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coatedSilk250gsmId, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(297, 210)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(8),
            SpecValue.BindingMethodSpec(BindingMethod.SpiralBinding),
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
            ComponentRequest(ComponentRole.Cover, SampleCatalog.coatedSilk250gsmId, InkConfiguration.cmyk4_4, Nil),
            ComponentRequest(ComponentRole.Body, SampleCatalog.coatedSilk250gsmId, InkConfiguration.cmyk4_4, Nil),
          ),
          specs = List(
            SpecValue.SizeSpec(Dimension(297, 210)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.PagesSpec(30),
            SpecValue.BindingMethodSpec(BindingMethod.SpiralBinding),
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
          printingMethodId = SampleCatalog.offsetId,
          components = List(mainComponent(SampleCatalog.coated300gsmId, InkConfiguration.cmyk4_0, List(SampleCatalog.matteLaminationId))),
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
          components = List(mainComponent(SampleCatalog.yupoId, InkConfiguration.cmyk4_4, List(SampleCatalog.embossingId))),
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
          components = List(mainComponent(SampleCatalog.yupoId, InkConfiguration.cmyk4_4, List(SampleCatalog.debossingId))),
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
  )
