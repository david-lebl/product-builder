package mpbuilder.domain

import zio.test.*
import zio.json.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.rules.*
import mpbuilder.domain.sample.*
import mpbuilder.domain.codec.DomainCodecs.given

object CatalogCodecSpec extends ZIOSpecDefault:

  private val catalog = SampleCatalog.catalog
  private val ruleset = SampleRules.ruleset
  private val pricelist = SamplePricelist.pricelist

  def spec = suite("CatalogCodecSpec")(

    suite("ID round-trips")(
      test("CategoryId encodes and decodes") {
        val id = CategoryId.unsafe("test-cat")
        val json = id.toJson
        val decoded = json.fromJson[CategoryId]
        assertTrue(decoded == Right(id), json == "\"test-cat\"")
      },
      test("MaterialId encodes and decodes") {
        val id = MaterialId.unsafe("test-mat")
        val json = id.toJson
        val decoded = json.fromJson[MaterialId]
        assertTrue(decoded == Right(id))
      },
      test("FinishId encodes and decodes") {
        val id = FinishId.unsafe("test-fin")
        val json = id.toJson
        val decoded = json.fromJson[FinishId]
        assertTrue(decoded == Right(id))
      },
      test("PrintingMethodId encodes and decodes") {
        val id = PrintingMethodId.unsafe("test-pm")
        val json = id.toJson
        val decoded = json.fromJson[PrintingMethodId]
        assertTrue(decoded == Right(id))
      },
    ),

    suite("enum round-trips")(
      test("MaterialFamily round-trips") {
        val values = MaterialFamily.values.toList
        assertTrue(values.forall { v =>
          v.toJson.fromJson[MaterialFamily] == Right(v)
        })
      },
      test("FinishType round-trips") {
        val values = FinishType.values.toList
        assertTrue(values.forall { v =>
          v.toJson.fromJson[FinishType] == Right(v)
        })
      },
      test("Currency round-trips") {
        val values = Currency.values.toList
        assertTrue(values.forall { v =>
          v.toJson.fromJson[Currency] == Right(v)
        })
      },
      test("ComponentRole round-trips") {
        val values = ComponentRole.values.toList
        assertTrue(values.forall { v =>
          v.toJson.fromJson[ComponentRole] == Right(v)
        })
      },
      test("PrintingProcessType round-trips") {
        val values = PrintingProcessType.values.toList
        assertTrue(values.forall { v =>
          v.toJson.fromJson[PrintingProcessType] == Right(v)
        })
      },
    ),

    suite("value object round-trips")(
      test("Money round-trips") {
        val m = Money(BigDecimal("12.50"))
        val json = m.toJson
        val decoded = json.fromJson[Money]
        assertTrue(decoded == Right(m))
      },
      test("LocalizedString round-trips") {
        val ls = LocalizedString("Hello", "Ahoj")
        val json = ls.toJson
        val decoded = json.fromJson[LocalizedString]
        assertTrue(decoded.map(_(Language.En)) == Right("Hello"))
        assertTrue(decoded.map(_(Language.Cs)) == Right("Ahoj"))
      },
      test("Dimension round-trips") {
        val d = Dimension(210.0, 297.0)
        val json = d.toJson
        val decoded = json.fromJson[Dimension]
        assertTrue(decoded == Right(d))
      },
      test("InkConfiguration round-trips") {
        val ic = InkConfiguration.cmyk4_4
        val json = ic.toJson
        val decoded = json.fromJson[InkConfiguration]
        assertTrue(decoded == Right(ic))
      },
    ),

    suite("entity round-trips")(
      test("Material round-trips") {
        val mat = Material(
          id = MaterialId.unsafe("mat-1"),
          name = LocalizedString("Coated 300gsm", "Křídový 300g"),
          family = MaterialFamily.Paper,
          weight = Some(PaperWeight.unsafe(300)),
          properties = Set(MaterialProperty.Glossy, MaterialProperty.SmoothSurface),
        )
        val json = mat.toJson
        val decoded = json.fromJson[Material]
        assertTrue(decoded == Right(mat))
      },
      test("Finish round-trips") {
        val fin = Finish(
          id = FinishId.unsafe("fin-1"),
          name = LocalizedString("Matte Lamination", "Matná laminace"),
          finishType = FinishType.Lamination,
          side = FinishSide.Both,
        )
        val json = fin.toJson
        val decoded = json.fromJson[Finish]
        assertTrue(decoded == Right(fin))
      },
      test("PrintingMethod round-trips") {
        val pm = PrintingMethod(
          id = PrintingMethodId.unsafe("pm-1"),
          name = LocalizedString("Digital", "Digitální"),
          processType = PrintingProcessType.Digital,
          maxColorCount = Some(4),
        )
        val json = pm.toJson
        val decoded = json.fromJson[PrintingMethod]
        assertTrue(decoded == Right(pm))
      },
      test("ProductCategory round-trips") {
        val cat = ProductCategory(
          id = CategoryId.unsafe("cat-1"),
          name = LocalizedString("Business Cards", "Vizitky"),
          components = List(
            ComponentTemplate(
              role = ComponentRole.Main,
              allowedMaterialIds = Set(MaterialId.unsafe("mat-1")),
              allowedFinishIds = Set(FinishId.unsafe("fin-1")),
            )
          ),
          requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
          allowedPrintingMethodIds = Set(PrintingMethodId.unsafe("pm-1")),
        )
        val json = cat.toJson
        val decoded = json.fromJson[ProductCategory]
        assertTrue(decoded == Right(cat))
      },
    ),

    suite("rule round-trips")(
      test("CompatibilityRule.MaterialFinishIncompatible round-trips") {
        val rule = CompatibilityRule.MaterialFinishIncompatible(
          materialId = MaterialId.unsafe("mat-1"),
          finishId = FinishId.unsafe("fin-1"),
          reason = "Not compatible",
        )
        val json = rule.toJson
        val decoded = json.fromJson[CompatibilityRule]
        assertTrue(decoded == Right(rule))
      },
      test("CompatibilityRule.SpecConstraint with predicate round-trips") {
        val rule = CompatibilityRule.SpecConstraint(
          categoryId = CategoryId.unsafe("cat-1"),
          predicate = SpecPredicate.MinDimension(50.0, 50.0),
          reason = "Too small",
        )
        val json = rule.toJson
        val decoded = json.fromJson[CompatibilityRule]
        assertTrue(decoded == Right(rule))
      },
      test("ConfigurationPredicate with And/Or/Not round-trips") {
        val pred = ConfigurationPredicate.And(
          ConfigurationPredicate.HasMaterialFamily(MaterialFamily.Paper),
          ConfigurationPredicate.Or(
            ConfigurationPredicate.HasMinWeight(200),
            ConfigurationPredicate.Not(
              ConfigurationPredicate.HasMaterialProperty(MaterialProperty.Transparent)
            ),
          ),
        )
        val json = pred.toJson
        val decoded = json.fromJson[ConfigurationPredicate]
        assertTrue(decoded == Right(pred))
      },
      test("CompatibilityRuleset round-trips") {
        val json = ruleset.toJson
        val decoded = json.fromJson[CompatibilityRuleset]
        assertTrue(decoded.isRight)
        assertTrue(decoded.map(_.rules.size) == Right(ruleset.rules.size))
        assertTrue(decoded.map(_.version) == Right(ruleset.version))
      },
    ),

    suite("pricing round-trips")(
      test("PricingRule.MaterialBasePrice round-trips") {
        val rule = PricingRule.MaterialBasePrice(
          materialId = MaterialId.unsafe("mat-1"),
          unitPrice = Money(BigDecimal("2.50")),
        )
        val json = rule.toJson
        val decoded = json.fromJson[PricingRule]
        assertTrue(decoded == Right(rule))
      },
      test("PricingRule.QuantityTier round-trips") {
        val rule = PricingRule.QuantityTier(
          minQuantity = 100,
          maxQuantity = Some(499),
          multiplier = BigDecimal("0.9"),
        )
        val json = rule.toJson
        val decoded = json.fromJson[PricingRule]
        assertTrue(decoded == Right(rule))
      },
      test("Pricelist round-trips") {
        val json = pricelist.toJson
        val decoded = json.fromJson[Pricelist]
        assertTrue(decoded.isRight)
        assertTrue(decoded.map(_.rules.size) == Right(pricelist.rules.size))
        assertTrue(decoded.map(_.currency) == Right(pricelist.currency))
      },
    ),

    suite("full catalog round-trips")(
      test("ProductCatalog round-trips") {
        val json = catalog.toJson
        val decoded = json.fromJson[ProductCatalog]
        assertTrue(decoded.isRight)
        assertTrue(decoded.map(_.categories.size) == Right(catalog.categories.size))
        assertTrue(decoded.map(_.materials.size) == Right(catalog.materials.size))
        assertTrue(decoded.map(_.finishes.size) == Right(catalog.finishes.size))
        assertTrue(decoded.map(_.printingMethods.size) == Right(catalog.printingMethods.size))
      },
      test("CatalogExport round-trips the full sample data") {
        import mpbuilder.domain.codec.DomainCodecs.CatalogExport
        val export_ = CatalogExport(
          catalog = catalog,
          ruleset = ruleset,
          pricelists = List(pricelist, SamplePricelist.pricelistCzk),
        )
        val json = export_.toJson
        val decoded = json.fromJson[CatalogExport]
        assertTrue(decoded.isRight)
        assertTrue(decoded.map(_.catalog.categories.size) == Right(catalog.categories.size))
        assertTrue(decoded.map(_.ruleset.rules.size) == Right(ruleset.rules.size))
        assertTrue(decoded.map(_.pricelists.size) == Right(2))
      },
    ),
  )
