package mpbuilder.domain

import zio.test.*
import zio.json.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.rules.*
import mpbuilder.domain.json.DomainCodecs.given
import mpbuilder.domain.sample.{SampleCatalog, SamplePricelist, SampleRules}

object JsonCodecsSpec extends ZIOSpecDefault:
  def spec = suite("JsonCodecsSpec")(

    test("Material roundtrips through JSON") {
      val mat = SampleCatalog.coated300gsm
      val json = mat.toJson
      val decoded = json.fromJson[Material]
      assertTrue(decoded == Right(mat))
    },

    test("Finish roundtrips through JSON") {
      val fin = SampleCatalog.matteLamination
      val json = fin.toJson
      val decoded = json.fromJson[Finish]
      assertTrue(decoded == Right(fin))
    },

    test("PrintingMethod roundtrips through JSON") {
      val pm = SampleCatalog.offsetMethod
      val json = pm.toJson
      val decoded = json.fromJson[PrintingMethod]
      assertTrue(decoded == Right(pm))
    },

    test("ProductCategory roundtrips through JSON") {
      val cat = SampleCatalog.businessCards
      val json = cat.toJson
      val decoded = json.fromJson[ProductCategory]
      assertTrue(decoded == Right(cat))
    },

    test("ProductCatalog roundtrips through JSON") {
      val catalog = SampleCatalog.catalog
      val json = catalog.toJson
      val decoded = json.fromJson[ProductCatalog]
      assertTrue(decoded == Right(catalog))
    },

    test("Pricelist roundtrips through JSON") {
      val pl = SamplePricelist.pricelist
      val json = pl.toJson
      val decoded = json.fromJson[Pricelist]
      assertTrue(decoded == Right(pl))
    },

    test("CompatibilityRuleset roundtrips through JSON") {
      val rs = SampleRules.ruleset
      val json = rs.toJson
      val decoded = json.fromJson[CompatibilityRuleset]
      assertTrue(decoded == Right(rs))
    },

    test("LocalizedString roundtrips through JSON") {
      val ls = LocalizedString("Hello", "Ahoj")
      val json = ls.toJson
      val decoded = json.fromJson[LocalizedString]
      assertTrue(
        decoded == Right(ls),
        json.contains("en"),
        json.contains("Hello"),
      )
    },

    test("Money roundtrips through JSON") {
      val m = Money("12.50")
      val json = m.toJson
      val decoded = json.fromJson[Money]
      assertTrue(decoded == Right(m))
    },

    test("PricingRule.MaterialSheetPrice roundtrips through JSON") {
      val rule = PricingRule.MaterialSheetPrice(
        MaterialId.unsafe("test-mat"), Money("1.50"), 640.0, 900.0, 3.0, 5.0
      )
      val json = rule.toJson
      val decoded = json.fromJson[PricingRule]
      assertTrue(decoded == Right(rule))
    },

    test("PricingRule.QuantityTier with None max roundtrips") {
      val rule = PricingRule.QuantityTier(1000, None, BigDecimal("0.85"))
      val json = rule.toJson
      val decoded = json.fromJson[PricingRule]
      assertTrue(decoded == Right(rule))
    },

    test("CompatibilityRule.ConfigurationConstraint roundtrips through JSON") {
      val rule = CompatibilityRule.ConfigurationConstraint(
        CategoryId.unsafe("cat-test"),
        ConfigurationPredicate.And(
          ConfigurationPredicate.HasMaterialFamily(MaterialFamily.Paper),
          ConfigurationPredicate.HasMinWeight(200),
        ),
        "Test reason",
      )
      val json = rule.toJson
      val decoded = json.fromJson[CompatibilityRule]
      assertTrue(decoded == Right(rule))
    },
  )
