package mpbuilder.domain

import mpbuilder.commons.*

import zio.test.*
import zio.json.*
import mpbuilder.domain.model.*
import mpbuilder.domain.rules.*
import mpbuilder.domain.sample.*
import mpbuilder.domain.service.{ConfigurationBuilder, ConfigurationRequest}
import mpbuilder.domain.codec.ConfigurationCodecs.given

/** Round-trip tests for the configuration snapshot format.
  *
  * This format is what an order line stores, so a break here is a break in stored orders — not just
  * a serialization bug. Each test builds a *real* configuration through `ConfigurationBuilder`
  * rather than hand-assembling one, so the fixtures stay honest as the model evolves.
  */
object ConfigurationCodecSpec extends ZIOSpecDefault:

  private val catalog = SampleCatalog.catalog
  private val ruleset = SampleRules.ruleset
  private val configId = ConfigurationId.unsafe("snapshot-test")

  private def build(request: ConfigurationRequest): ProductConfiguration =
    ConfigurationBuilder.build(request, catalog, ruleset, configId).toEither match
      case Right(config) => config
      case Left(errors) =>
        throw new AssertionError(s"fixture did not build: ${errors.toList.map(_.message)}")

  private val businessCards = build(
    ConfigurationRequest(
      categoryId = SampleCatalog.businessCardsId,
      printingMethodId = SampleCatalog.digitalId,
      components = List(
        ComponentRequest(
          ComponentRole.Main,
          SampleCatalog.coated300gsmId,
          InkConfiguration.cmyk4_4,
          List(FinishSelection(SampleCatalog.matteLaminationId)),
        )
      ),
      specs = List(
        SpecValue.SizeSpec(Dimension(90, 55)),
        SpecValue.QuantitySpec(Quantity.unsafe(500)),
      ),
    )
  )

  private def roundTrip(config: ProductConfiguration): Either[String, ProductConfiguration] =
    config.toJson.fromJson[ProductConfiguration]

  def spec = suite("ConfigurationCodecSpec")(
    suite("ProductConfiguration round-trip")(
      test("a complete configuration survives encode/decode unchanged") {
        assertTrue(roundTrip(businessCards) == Right(businessCards))
      },
      test("the resolved catalog entities are embedded, not referenced by id") {
        // The snapshot must stand alone: a catalog edit after the order was placed
        // must not be able to change what the customer bought.
        val json = businessCards.toJson
        assertTrue(
          json.contains("Business Cards"),
          json.contains("Coated Art Paper 300gsm"),
          json.contains("Digital Printing"),
        )
      },
      test("decoding does not consult the live catalog") {
        val decoded = roundTrip(businessCards).toOption.get
        assertTrue(
          decoded.category.name(Language.En) == "Business Cards",
          decoded.components.head.material.name(Language.En) == "Coated Art Paper 300gsm",
          decoded.printingMethod.name(Language.En) == "Digital Printing",
        )
      },
    ),
    suite("component detail")(
      test("selected finishes and their parameters survive") {
        val decoded = roundTrip(businessCards).toOption.get
        assertTrue(
          decoded.components.head.finishes.size == 1,
          decoded.components.head.finishes == businessCards.components.head.finishes,
        )
      },
      test("ink configuration survives") {
        val decoded = roundTrip(businessCards).toOption.get
        assertTrue(decoded.components.head.inkConfiguration == InkConfiguration.cmyk4_4)
      },
      test("every FinishParameters variant round-trips") {
        val params: List[FinishParameters] = List(
          FinishParameters.RoundCornersParams(4, 3),
          FinishParameters.LaminationParams(FinishSide.Both),
          FinishParameters.FoilStampingParams(FoilColor.Gold),
          FinishParameters.GrommetParams(500),
          FinishParameters.PerforationParams(3),
          FinishParameters.RopeParams(BigDecimal("1.5")),
          FinishParameters.ScoringParams(2),
        )
        assertTrue(params.forall(p => p.toJson.fromJson[FinishParameters] == Right(p)))
      },
    ),
    suite("specifications")(
      test("the SpecKind-keyed map round-trips") {
        val decoded = roundTrip(businessCards).toOption.get
        assertTrue(decoded.specifications == businessCards.specifications)
      },
      test("every SpecValue variant round-trips") {
        val values: List[SpecValue] = List(
          SpecValue.SizeSpec(Dimension(210, 297)),
          SpecValue.QuantitySpec(Quantity.unsafe(1000)),
          SpecValue.OrientationSpec(Orientation.Portrait),
          SpecValue.BleedSpec(3.0),
          SpecValue.PagesSpec(24),
          SpecValue.FoldTypeSpec(FoldType.Half),
          SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
          SpecValue.ManufacturingSpeedSpec(ManufacturingSpeed.Express),
        )
        assertTrue(values.forall(v => v.toJson.fromJson[SpecValue] == Right(v)))
      },
      test("every SpecKind survives as a JSON field name") {
        val specs = ProductSpecifications(
          SpecKind.values.map(k => k -> SpecValue.PagesSpec(1)).toMap
        )
        assertTrue(specs.toJson.fromJson[ProductSpecifications] == Right(specs))
      },
    ),
    suite("malformed input")(
      test("garbage is rejected rather than silently defaulted") {
        assertTrue(
          """{"nope":1}""".fromJson[ProductConfiguration].isLeft,
          "not json at all".fromJson[ProductConfiguration].isLeft,
        )
      }
    ),
  )
