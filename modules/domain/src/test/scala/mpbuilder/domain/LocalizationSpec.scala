package mpbuilder.domain

import zio.test.*
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.validation.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.service.*
import mpbuilder.domain.sample.*

object LocalizationSpec extends ZIOSpecDefault:

  private val catalog = SampleCatalog.catalog
  private val ruleset = SampleRules.ruleset
  private val pricelist = SamplePricelist.pricelist
  private val configId = ConfigurationId.unsafe("test-i18n-1")

  def spec = suite("Localization")(
    suite("LocalizedString")(
      test("returns English text for Language.En") {
        val ls = LocalizedString("Hello", "Ahoj")
        assertTrue(ls(Language.En) == "Hello")
      },
      test("returns Czech text for Language.Cs") {
        val ls = LocalizedString("Hello", "Ahoj")
        assertTrue(ls(Language.Cs) == "Ahoj")
      },
      test("value returns English by default") {
        val ls = LocalizedString("Hello", "Ahoj")
        assertTrue(ls.value == "Hello")
      },
      test("falls back to English when language not available") {
        val ls = LocalizedString("Hello")
        assertTrue(ls(Language.Cs) == "Hello")
      },
    ),
    suite("SampleCatalog Czech translations")(
      test("materials have Czech names") {
        assertTrue(
          SampleCatalog.coated300gsm.name(Language.Cs) == "Křídový papír 300g",
          SampleCatalog.uncoatedBond.name(Language.Cs) == "Nenatíraný papír 120g",
          SampleCatalog.kraft.name(Language.Cs) == "Kraftový papír 250g",
          SampleCatalog.vinyl.name(Language.Cs) == "Samolepicí vinyl",
          SampleCatalog.corrugated.name(Language.Cs) == "Vlnitá lepenka",
        )
      },
      test("materials have English names") {
        assertTrue(
          SampleCatalog.coated300gsm.name(Language.En) == "Coated Art Paper 300gsm",
          SampleCatalog.vinyl.name(Language.En) == "Adhesive Vinyl",
        )
      },
      test("categories have Czech names") {
        assertTrue(
          SampleCatalog.businessCards.name(Language.Cs) == "Vizitky",
          SampleCatalog.flyers.name(Language.Cs) == "Letáky",
          SampleCatalog.banners.name(Language.Cs) == "Bannery",
          SampleCatalog.packaging.name(Language.Cs) == "Obaly",
        )
      },
      test("finishes have Czech names") {
        assertTrue(
          SampleCatalog.matteLamination.name(Language.Cs) == "Matná laminace",
          SampleCatalog.glossLamination.name(Language.Cs) == "Lesklá laminace",
          SampleCatalog.uvCoating.name(Language.Cs) == "UV lak",
          SampleCatalog.embossing.name(Language.Cs) == "Slepotisk",
          SampleCatalog.dieCut.name(Language.Cs) == "Výsek",
        )
      },
      test("printing methods have Czech names") {
        assertTrue(
          SampleCatalog.offsetMethod.name(Language.Cs) == "Ofsetový tisk",
          SampleCatalog.digitalMethod.name(Language.Cs) == "Digitální tisk",
          SampleCatalog.letterpressMethod.name(Language.Cs) == "Knihtisk",
        )
      },
    ),
    suite("ConfigurationError Czech messages")(
      test("CategoryNotFound message in Czech") {
        val err = ConfigurationError.CategoryNotFound(CategoryId.unsafe("cat-test"))
        assertTrue(
          err.message(Language.Cs).contains("nebyla nalezena"),
          err.message(Language.En).contains("not found in catalog"),
        )
      },
      test("InvalidCategoryMaterial message in Czech") {
        val err = ConfigurationError.InvalidCategoryMaterial(
          CategoryId.unsafe("cat-test"),
          MaterialId.unsafe("mat-test"),
        )
        assertTrue(
          err.message(Language.Cs).contains("není povolen"),
          err.message(Language.En).contains("is not allowed"),
        )
      },
      test("backward-compatible parameterless message returns English") {
        val err = ConfigurationError.MaterialNotFound(MaterialId.unsafe("mat-test"))
        assertTrue(err.message == err.message(Language.En))
      },
    ),
    suite("PricingError Czech messages")(
      test("NoBasePriceForMaterial in Czech") {
        val err = PricingError.NoBasePriceForMaterial(MaterialId.unsafe("mat-test"))
        assertTrue(
          err.message(Language.Cs).contains("Nebyla nalezena základní cena"),
          err.message(Language.En).contains("No base price found"),
        )
      },
      test("NoQuantityInSpecifications in Czech") {
        val err = PricingError.NoQuantityInSpecifications
        assertTrue(
          err.message(Language.Cs).contains("vyžadována specifikace množství"),
          err.message(Language.En).contains("Quantity specification is required"),
        )
      },
      test("backward-compatible parameterless message returns English") {
        val err = PricingError.NoQuantityInSpecifications
        assertTrue(err.message == err.message(Language.En))
      },
    ),
    suite("PriceCalculator with language")(
      test("price breakdown labels use Czech when specified") {
        val config = ProductConfiguration(
          id = configId,
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List(SampleCatalog.matteLamination),
          specifications = ProductSpecifications.fromSpecs(List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          )),
        )

        val result = PriceCalculator.calculate(config, pricelist, Language.Cs)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          breakdown.materialLine.label.contains("Křídový papír 300g"),
          breakdown.finishLines.head.label.contains("Matná laminace"),
        )
      },
      test("price breakdown labels use English by default") {
        val config = ProductConfiguration(
          id = configId,
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List(SampleCatalog.matteLamination),
          specifications = ProductSpecifications.fromSpecs(List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          )),
        )

        val result = PriceCalculator.calculate(config, pricelist)
        val breakdown = result.toEither.toOption.get
        assertTrue(
          breakdown.materialLine.label.contains("Coated Art Paper 300gsm"),
          breakdown.finishLines.head.label.contains("Matte Lamination"),
        )
      },
    ),
  )
