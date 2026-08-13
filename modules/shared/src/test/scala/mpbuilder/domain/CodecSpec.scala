package mpbuilder.domain

import mpbuilder.domain.catalog.*
import mpbuilder.domain.config.*
import mpbuilder.domain.ids.*
import zio.json.*
import zio.test.*

object CodecSpec extends ZIOSpecDefault:

  private val sampleConfig = ProductConfiguration(
    categoryId = CategoryId("business-cards"),
    printingMethodId = PrintingMethodId("digital"),
    inkConfigurationId = InkConfigId("4-4"),
    components = List(
      ComponentConfiguration(
        role = ComponentRole.Main,
        materialId = MaterialId("coated-matte-350"),
        finishes = List(
          SelectedFinish(FinishId("matte-lamination")),
          SelectedFinish(FinishId("round-corners"), Some(FinishParams.RoundCorners(4, 3))),
          SelectedFinish(FinishId("scoring"), Some(FinishParams.Creases(2))),
        ),
      )
    ),
    details = ProductDetails(
      size = Some(DimensionsMm(85, 55)),
      quantity = Some(200),
      foldType = Some(FoldType.TriFold),
      bindingMethod = Some(BindingMethod.SaddleStitch),
    ),
    speedTier = SpeedTier.Express,
  )

  private val sampleCategory = Category(
    id = CategoryId("free-configuration"),
    name = LocalizedText("Free Configuration", "Volná konfigurace"),
    components = List(
      ComponentSpec(ComponentRole.Main, optional = false, AllowList.All(), AllowList.All()),
      ComponentSpec(
        ComponentRole.Stand,
        optional = true,
        AllowList.of(MaterialId("stand-economy"), MaterialId("stand-premium")),
        AllowList.of[FinishId](),
      ),
    ),
    requiredDetails = Set(RequiredDetail.Size, RequiredDetail.Quantity),
    allowedPrintingMethods = AllowList.All(),
  )

  def spec = suite("JSON codecs")(
    test("ProductConfiguration round-trips") {
      val json = sampleConfig.toJson
      assertTrue(json.fromJson[ProductConfiguration] == Right(sampleConfig))
    },
    test("Category with AllowList round-trips") {
      val json = sampleCategory.toJson
      assertTrue(json.fromJson[Category] == Right(sampleCategory))
    },
    test("Money round-trips exactly") {
      val money = Money(BigDecimal("6860.00"), Currency.CZK)
      assertTrue(money.toJson.fromJson[Money] == Right(money))
    },
    test("Money rounds HALF_UP to two decimals") {
      assertTrue(
        Money(BigDecimal("2.345"), Currency.CZK).round2.amount == BigDecimal("2.35"),
        Money(BigDecimal("2.344"), Currency.CZK).round2.amount == BigDecimal("2.34"),
      )
    },
  )
