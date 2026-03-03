package mpbuilder.domain

import zio.test.*
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.weight.*
import mpbuilder.domain.sample.*

object WeightCalculatorSpec extends ZIOSpecDefault:

  private val configId = ConfigurationId.unsafe("test-weight-1")

  private def makeConfig(
      category: ProductCategory,
      material: Material,
      printingMethod: PrintingMethod,
      specs: List[SpecValue],
      sheetCount: Int = 1,
  ): ProductConfiguration =
    ProductConfiguration(
      id = configId,
      category = category,
      printingMethod = printingMethod,
      components = List(ProductComponent(
        role = ComponentRole.Main,
        material = material,
        inkConfiguration = InkConfiguration.cmyk4_4,
        finishes = List.empty,
        sheetCount = sheetCount,
      )),
      specifications = ProductSpecifications.fromSpecs(specs),
    )

  private def makeBookletConfig(
      coverMaterial: Material,
      coverSheetCount: Int,
      bodyMaterial: Material,
      bodySheetCount: Int,
      bindingMethod: BindingMethod,
      widthMm: Double,
      heightMm: Double,
      quantity: Int,
  ): ProductConfiguration =
    ProductConfiguration(
      id = configId,
      category = SampleCatalog.booklets,
      printingMethod = SampleCatalog.digitalMethod,
      components = List(
        ProductComponent(
          role = ComponentRole.Cover,
          material = coverMaterial,
          inkConfiguration = InkConfiguration.cmyk4_4,
          finishes = List.empty,
          sheetCount = coverSheetCount,
        ),
        ProductComponent(
          role = ComponentRole.Body,
          material = bodyMaterial,
          inkConfiguration = InkConfiguration.cmyk4_4,
          finishes = List.empty,
          sheetCount = bodySheetCount,
        ),
      ),
      specifications = ProductSpecifications.fromSpecs(List(
        SpecValue.SizeSpec(Dimension(widthMm, heightMm)),
        SpecValue.QuantitySpec(Quantity.unsafe(quantity)),
        SpecValue.PagesSpec(16),
        SpecValue.BindingMethodSpec(bindingMethod),
      )),
    )

  def spec = suite("WeightCalculator")(
    suite("single component products")(
      test("business card 90×55mm 300gsm qty 500") {
        // 90mm × 55mm = 0.09m × 0.055m = 0.00495 m²
        // 300gsm × 0.00495 m² = 1.485 g per card
        // 500 cards × 1.485 g = 742.5 g = 0.7425 kg
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.digitalMethod,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
          ),
        )
        val result = WeightCalculator.calculate(config)
        val bd = result.toEither.toOption.get
        val cb = bd.componentBreakdowns.head
        assertTrue(
          result.toEither.isRight,
          cb.gsmWeight == 300,
          cb.sheetsPerItem == 1,
          math.abs(cb.sheetAreaM2 - 0.00495) < 1e-9,
          math.abs(cb.weightPerItemG - 1.485) < 1e-9,
          math.abs(bd.weightPerItemG - 1.485) < 1e-9,
          bd.quantity == 500,
          math.abs(bd.totalWeightG - 742.5) < 1e-9,
          math.abs(bd.totalWeightKg - 0.7425) < 1e-9,
        )
      },
      test("A4 flyer 210×297mm 120gsm qty 100") {
        // 0.21m × 0.297m = 0.06237 m²
        // 120 × 0.06237 = 7.4844 g per flyer
        // 100 × 7.4844 = 748.44 g
        val config = makeConfig(
          category = SampleCatalog.flyers,
          material = SampleCatalog.uncoatedBond,
          printingMethod = SampleCatalog.digitalMethod,
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.OrientationSpec(Orientation.Portrait),
          ),
        )
        val result = WeightCalculator.calculate(config)
        val bd = result.toEither.toOption.get
        val cb = bd.componentBreakdowns.head
        val expectedArea = 0.210 * 0.297
        val expectedWeightPerItem = 120.0 * expectedArea
        assertTrue(
          result.toEither.isRight,
          cb.gsmWeight == 120,
          math.abs(cb.sheetAreaM2 - expectedArea) < 1e-9,
          math.abs(cb.weightPerItemG - expectedWeightPerItem) < 1e-9,
          bd.quantity == 100,
          math.abs(bd.totalWeightG - expectedWeightPerItem * 100) < 1e-9,
        )
      },
    ),
    suite("multi-component booklets")(
      test("saddle-stitch booklet: cover and body use 2× width") {
        // Saddle-stitch: flat sheet width is doubled (printed unfolded, then folded)
        // Cover: 300gsm, 1 sheet, flat width = 148mm*2 = 296mm, height = 210mm
        //   area = 0.296 × 0.210 = 0.06216 m²
        //   weight per item = 1 × 0.06216 × 300 = 18.648 g
        // Body: 120gsm, 3 sheets, flat width = 296mm, height = 210mm
        //   area = 0.296 × 0.210 = 0.06216 m²
        //   weight per item = 3 × 0.06216 × 120 = 22.3776 g
        // Total per item = 18.648 + 22.3776 = 41.0256 g
        // 50 items = 2051.28 g
        val config = makeBookletConfig(
          coverMaterial  = SampleCatalog.coated300gsm,
          coverSheetCount = 1,
          bodyMaterial   = SampleCatalog.uncoatedBond,
          bodySheetCount  = 3,
          bindingMethod  = BindingMethod.SaddleStitch,
          widthMm        = 148,
          heightMm       = 210,
          quantity       = 50,
        )
        val result = WeightCalculator.calculate(config)
        val bd = result.toEither.toOption.get
        val cover = bd.componentBreakdowns.find(_.role == ComponentRole.Cover).get
        val body  = bd.componentBreakdowns.find(_.role == ComponentRole.Body).get
        val flatW = 0.296
        val h     = 0.210
        val coverExpectedArea   = flatW * h
        val coverExpectedWeight = 1 * coverExpectedArea * 300
        val bodyExpectedArea    = flatW * h
        val bodyExpectedWeight  = 3 * bodyExpectedArea * 120
        val totalPerItem        = coverExpectedWeight + bodyExpectedWeight
        assertTrue(
          result.toEither.isRight,
          math.abs(cover.sheetAreaM2 - coverExpectedArea) < 1e-9,
          math.abs(cover.weightPerItemG - coverExpectedWeight) < 1e-9,
          math.abs(body.sheetAreaM2 - bodyExpectedArea) < 1e-9,
          math.abs(body.weightPerItemG - bodyExpectedWeight) < 1e-9,
          math.abs(bd.weightPerItemG - totalPerItem) < 1e-9,
          math.abs(bd.totalWeightG - totalPerItem * 50) < 1e-9,
        )
      },
      test("perfect-binding booklet: no 2× width multiplier") {
        // Perfect binding: sheets are not folded, use actual width
        // Cover: 300gsm, 1 sheet, width = 148mm, height = 210mm
        //   area = 0.148 × 0.210 = 0.03108 m²
        //   weight per item = 1 × 0.03108 × 300 = 9.324 g
        // Body: 120gsm, 3 sheets, width = 148mm, height = 210mm
        //   area = 0.148 × 0.210 = 0.03108 m²
        //   weight per item = 3 × 0.03108 × 120 = 11.1888 g
        val config = makeBookletConfig(
          coverMaterial  = SampleCatalog.coated300gsm,
          coverSheetCount = 1,
          bodyMaterial   = SampleCatalog.uncoatedBond,
          bodySheetCount  = 3,
          bindingMethod  = BindingMethod.PerfectBinding,
          widthMm        = 148,
          heightMm       = 210,
          quantity       = 50,
        )
        val result = WeightCalculator.calculate(config)
        val bd = result.toEither.toOption.get
        val cover = bd.componentBreakdowns.find(_.role == ComponentRole.Cover).get
        val body  = bd.componentBreakdowns.find(_.role == ComponentRole.Body).get
        val normalW = 0.148
        val h       = 0.210
        val coverExpectedArea   = normalW * h
        val coverExpectedWeight = 1 * coverExpectedArea * 300
        val bodyExpectedArea    = normalW * h
        val bodyExpectedWeight  = 3 * bodyExpectedArea * 120
        assertTrue(
          result.toEither.isRight,
          math.abs(cover.sheetAreaM2 - coverExpectedArea) < 1e-9,
          math.abs(cover.weightPerItemG - coverExpectedWeight) < 1e-9,
          math.abs(body.sheetAreaM2 - bodyExpectedArea) < 1e-9,
          math.abs(body.weightPerItemG - bodyExpectedWeight) < 1e-9,
        )
      },
    ),
    suite("error cases")(
      test("missing SizeSpec returns NoSizeInSpecifications") {
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.digitalMethod,
          specs = List(
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            // no SizeSpec
          ),
        )
        val result = WeightCalculator.calculate(config)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          result.toEither.isLeft,
          errors.exists(_.isInstanceOf[WeightError.NoSizeInSpecifications.type]),
        )
      },
      test("missing QuantitySpec returns NoQuantityInSpecifications") {
        val config = makeConfig(
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.digitalMethod,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            // no QuantitySpec
          ),
        )
        val result = WeightCalculator.calculate(config)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          result.toEither.isLeft,
          errors.exists(_.isInstanceOf[WeightError.NoQuantityInSpecifications.type]),
        )
      },
      test("vinyl material (no GSM) returns NoWeightForMaterial") {
        val config = makeConfig(
          category = SampleCatalog.banners,
          material = SampleCatalog.vinyl,
          printingMethod = SampleCatalog.uvInkjetMethod,
          specs = List(
            SpecValue.SizeSpec(Dimension(1000, 500)),
            SpecValue.QuantitySpec(Quantity.unsafe(10)),
          ),
        )
        val result = WeightCalculator.calculate(config)
        val errors = result.toEither.left.toOption.get.toList
        assertTrue(
          result.toEither.isLeft,
          errors.exists {
            case WeightError.NoWeightForMaterial(id, _) => id == SampleCatalog.vinylId
            case _ => false
          },
        )
      },
    ),
  )
