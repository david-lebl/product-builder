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

  def spec = suite("ConfigurationBuilder")(
    suite("valid configurations")(
      test("build a valid business card configuration with offset printing") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = List(SampleCatalog.matteLaminationId),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.category.name == "Business Cards",
          result.toEither.toOption.get.material.name == "Coated Art Paper 300gsm",
          result.toEither.toOption.get.printingMethod.name == "Offset Printing",
          result.toEither.toOption.get.finishes.size == 1,
        )
      },
      test("build a valid business card with no finishes") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.digitalId,
          finishIds = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("build a valid banner configuration") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bannersId,
          materialId = SampleCatalog.vinylId,
          printingMethodId = SampleCatalog.uvInkjetId,
          finishIds = List(SampleCatalog.uvCoatingId),
          specs = List(
            SpecValue.SizeSpec(Dimension(1000, 500)),
            SpecValue.QuantitySpec(Quantity.unsafe(10)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("build a valid brochure configuration") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.brochuresId,
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = List(SampleCatalog.glossLaminationId),
          specs = List(
            SpecValue.SizeSpec(Dimension(297, 210)),
            SpecValue.QuantitySpec(Quantity.unsafe(1000)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
            SpecValue.FoldTypeSpec(FoldType.Tri),
            SpecValue.PagesSpec(6),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
      test("build a valid booklet configuration with binding method") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bookletsId,
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = List(SampleCatalog.matteLaminationId),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
            SpecValue.PagesSpec(32),
            SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        assertTrue(result.toEither.isRight)
      },
    ),
    suite("incompatible combinations produce accumulated errors")(
      test("UV coating on kraft paper is rejected (property-level rule)") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.packagingId,
          materialId = SampleCatalog.kraftId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = List(SampleCatalog.uvCoatingId),
          specs = List(
            SpecValue.SizeSpec(Dimension(200, 150)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.kraftId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = List(SampleCatalog.foilStampingId),
          specs = List(
            SpecValue.SizeSpec(Dimension(200, 150)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = List(SampleCatalog.matteLaminationId, SampleCatalog.glossLaminationId),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.vinylId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            // Missing Quantity and ColorMode
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.count(_.isInstanceOf[ConfigurationError.MissingRequiredSpec]) == 2,
        )
      },
      test("multiple errors are accumulated") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.packagingId,
          materialId = SampleCatalog.kraftId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = List(SampleCatalog.uvCoatingId, SampleCatalog.foilStampingId),
          specs = List(
            SpecValue.SizeSpec(Dimension(200, 150)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(200, 100)), // Too large
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(10)), // Below minimum 100
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.SpecConstraintViolation]),
        )
      },
      test("banner with PMS color mode is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.bannersId,
          materialId = SampleCatalog.vinylId,
          printingMethodId = SampleCatalog.uvInkjetId,
          finishIds = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(1000, 500)),
            SpecValue.QuantitySpec(Quantity.unsafe(10)),
            SpecValue.ColorModeSpec(ColorMode.PMS),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.SpecConstraintViolation]),
        )
      },
      test("unknown category returns CategoryNotFound") {
        val request = ConfigurationRequest(
          categoryId = CategoryId.unsafe("cat-nonexistent"),
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = Nil,
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
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = PrintingMethodId.unsafe("pm-nonexistent"),
          finishIds = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.uncoatedBondId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = List(SampleCatalog.matteLaminationId),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = List(SampleCatalog.matteLaminationId),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = List(SampleCatalog.matteLaminationId, SampleCatalog.softTouchCoatingId),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.digitalId,
          finishIds = List(SampleCatalog.aqueousCoatingId),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = List(SampleCatalog.aqueousCoatingId),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = List(SampleCatalog.varnishId),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = List(SampleCatalog.matteLaminationId, SampleCatalog.varnishId),
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.coated300gsmId,
          printingMethodId = SampleCatalog.offsetId,
          finishIds = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 148)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
          materialId = SampleCatalog.vinylId,
          printingMethodId = SampleCatalog.offsetId, // banners only allow UV inkjet
          finishIds = Nil,
          specs = List(
            SpecValue.SizeSpec(Dimension(1000, 500)),
            SpecValue.QuantitySpec(Quantity.unsafe(10)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
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
        // Vinyl is only allowed for banners, but embossing is not in banner's allowed finishes
        // Test the family rule through the evaluator directly
        val result = RuleEvaluator.evaluate(
          CompatibilityRule.MaterialFamilyFinishTypeIncompatible(
            MaterialFamily.Vinyl,
            FinishType.Embossing,
            "Vinyl cannot be embossed",
          ),
          SampleCatalog.vinyl,
          List(SampleCatalog.embossing),
          ProductSpecifications.empty,
          SampleCatalog.bannersId,
          SampleCatalog.uvInkjetMethod,
        )
        assertTrue(result.toEither.isLeft)
      },
    ),
  )
