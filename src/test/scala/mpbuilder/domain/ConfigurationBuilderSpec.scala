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
      test("build a valid business card configuration") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          materialId = SampleCatalog.coated300gsmId,
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
          result.toEither.toOption.get.finishes.size == 1,
        )
      },
      test("build a valid business card with no finishes") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          materialId = SampleCatalog.coated300gsmId,
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
    ),
    suite("incompatible combinations produce accumulated errors")(
      test("UV coating on kraft paper is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.packagingId,
          materialId = SampleCatalog.kraftId,
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
          errors.exists(_.isInstanceOf[ConfigurationError.IncompatibleMaterialFinish]),
        )
      },
      test("foil stamping on kraft paper (no smooth surface) is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.packagingId,
          materialId = SampleCatalog.kraftId,
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
          finishIds = List(SampleCatalog.uvCoatingId, SampleCatalog.foilStampingId),
          specs = List(
            SpecValue.SizeSpec(Dimension(200, 150)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        // Should have at least: UV+kraft incompatible, foil needs smooth surface
        assertTrue(errors.size >= 2)
      },
      test("business card too large is rejected") {
        val request = ConfigurationRequest(
          categoryId = SampleCatalog.businessCardsId,
          materialId = SampleCatalog.coated300gsmId,
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
          finishIds = Nil,
          specs = Nil,
        )

        val result = ConfigurationBuilder.build(request, catalog, ruleset, configId)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          errors.exists(_.isInstanceOf[ConfigurationError.CategoryNotFound]),
        )
      },
    ),
  )
