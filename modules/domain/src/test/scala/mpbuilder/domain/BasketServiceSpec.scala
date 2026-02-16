package mpbuilder.domain

import zio.test.*
import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.service.*
import mpbuilder.domain.sample.*

object BasketServiceSpec extends ZIOSpecDefault:

  private val pricelist = SamplePricelist.pricelist
  private val basketId = BasketId.unsafe("test-basket-1")

  private def makeConfig(
      id: String,
      category: ProductCategory,
      material: Material,
      printingMethod: PrintingMethod,
      finishes: List[Finish],
      specs: List[SpecValue],
  ): ProductConfiguration =
    ProductConfiguration(
      id = ConfigurationId.unsafe(id),
      category = category,
      material = material,
      printingMethod = printingMethod,
      finishes = finishes,
      specifications = ProductSpecifications.fromSpecs(specs),
    )

  def spec = suite("BasketService")(
    suite("empty basket")(
      test("creates an empty basket") {
        val basket = BasketService.empty(basketId)
        assertTrue(
          basket.id == basketId,
          basket.items.isEmpty,
        )
      },
      test("calculates zero total for empty basket") {
        val basket = BasketService.empty(basketId)
        val calculation = BasketService.calculateTotal(basket)
        assertTrue(
          calculation.items.isEmpty,
          calculation.subtotal == Money.zero,
          calculation.total == Money.zero,
          calculation.currency == Currency.USD,
        )
      },
    ),
    suite("adding items")(
      test("adds a single item to basket") {
        val config = makeConfig(
          id = "config-1",
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List(SampleCatalog.matteLamination),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val basket = BasketService.empty(basketId)
        val result = BasketService.addItem(basket, config, 1, pricelist)

        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.items.size == 1,
          result.toEither.toOption.get.items.head.configuration.id == ConfigurationId.unsafe("config-1"),
          result.toEither.toOption.get.items.head.quantity == 1,
        )
      },
      test("adds multiple items to basket") {
        val config1 = makeConfig(
          id = "config-1",
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List.empty,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val config2 = makeConfig(
          id = "config-2",
          category = SampleCatalog.flyers,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List.empty,
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(200)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val basket = BasketService.empty(basketId)
        val result1 = BasketService.addItem(basket, config1, 2, pricelist)
        val result2 = result1.flatMap(b => BasketService.addItem(b, config2, 3, pricelist))

        assertTrue(
          result2.toEither.isRight,
          result2.toEither.toOption.get.items.size == 2,
          result2.toEither.toOption.get.items.head.quantity == 2,
          result2.toEither.toOption.get.items.last.quantity == 3,
        )
      },
      test("rejects zero quantity") {
        val config = makeConfig(
          id = "config-1",
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List.empty,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val basket = BasketService.empty(basketId)
        val result = BasketService.addItem(basket, config, 0, pricelist)
        val errors = result.toEither.left.toOption.get.toList

        assertTrue(
          result.toEither.isLeft,
          errors.exists(_.isInstanceOf[BasketError.InvalidQuantity]),
        )
      },
      test("rejects negative quantity") {
        val config = makeConfig(
          id = "config-1",
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List.empty,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val basket = BasketService.empty(basketId)
        val result = BasketService.addItem(basket, config, -5, pricelist)
        val errors = result.toEither.left.toOption.get.toList

        assertTrue(
          result.toEither.isLeft,
          errors.exists(_.isInstanceOf[BasketError.InvalidQuantity]),
        )
      },
    ),
    suite("removing items")(
      test("removes an item from basket") {
        val config = makeConfig(
          id = "config-1",
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List.empty,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val basket = BasketService.empty(basketId)
        val withItem = BasketService.addItem(basket, config, 1, pricelist).toEither.toOption.get
        val removed = BasketService.removeItem(withItem, ConfigurationId.unsafe("config-1"))

        assertTrue(
          withItem.items.size == 1,
          removed.items.isEmpty,
        )
      },
      test("does nothing when removing non-existent item") {
        val basket = BasketService.empty(basketId)
        val removed = BasketService.removeItem(basket, ConfigurationId.unsafe("non-existent"))

        assertTrue(
          removed.items.isEmpty,
        )
      },
      test("removes only specified item from basket with multiple items") {
        val config1 = makeConfig(
          id = "config-1",
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List.empty,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val config2 = makeConfig(
          id = "config-2",
          category = SampleCatalog.flyers,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List.empty,
          specs = List(
            SpecValue.SizeSpec(Dimension(210, 297)),
            SpecValue.QuantitySpec(Quantity.unsafe(200)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val basket = BasketService.empty(basketId)
        val withItems = BasketService
          .addItem(basket, config1, 1, pricelist)
          .flatMap(b => BasketService.addItem(b, config2, 1, pricelist))
          .toEither
          .toOption
          .get

        val removed = BasketService.removeItem(withItems, ConfigurationId.unsafe("config-1"))

        assertTrue(
          withItems.items.size == 2,
          removed.items.size == 1,
          removed.items.head.configuration.id == ConfigurationId.unsafe("config-2"),
        )
      },
    ),
    suite("updating quantities")(
      test("updates quantity of existing item") {
        val config = makeConfig(
          id = "config-1",
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List.empty,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val basket = BasketService.empty(basketId)
        val withItem = BasketService.addItem(basket, config, 2, pricelist).toEither.toOption.get
        val updated = BasketService.updateQuantity(withItem, ConfigurationId.unsafe("config-1"), 5)

        assertTrue(
          updated.toEither.isRight,
          updated.toEither.toOption.get.items.head.quantity == 5,
        )
      },
      test("fails when updating non-existent item") {
        val basket = BasketService.empty(basketId)
        val result = BasketService.updateQuantity(basket, ConfigurationId.unsafe("non-existent"), 5)

        assertTrue(
          result.toEither.isLeft,
        )
      },
      test("fails when updating to zero quantity") {
        val config = makeConfig(
          id = "config-1",
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List.empty,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val basket = BasketService.empty(basketId)
        val withItem = BasketService.addItem(basket, config, 2, pricelist).toEither.toOption.get
        val result = BasketService.updateQuantity(withItem, ConfigurationId.unsafe("config-1"), 0)

        assertTrue(
          result.toEither.isLeft,
        )
      },
      test("fails when updating to negative quantity") {
        val config = makeConfig(
          id = "config-1",
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List.empty,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val basket = BasketService.empty(basketId)
        val withItem = BasketService.addItem(basket, config, 2, pricelist).toEither.toOption.get
        val result = BasketService.updateQuantity(withItem, ConfigurationId.unsafe("config-1"), -3)

        assertTrue(
          result.toEither.isLeft,
        )
      },
    ),
    suite("calculating total")(
      test("calculates total for single item") {
        val config = makeConfig(
          id = "config-1",
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List(SampleCatalog.matteLamination),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val basket = BasketService.empty(basketId)
        val withItem = BasketService.addItem(basket, config, 1, pricelist).toEither.toOption.get
        val calculation = BasketService.calculateTotal(withItem)

        // Price for 500× business cards with matte lamination = 67.50
        // Total for 1× this configuration = 67.50
        assertTrue(
          calculation.items.size == 1,
          calculation.subtotal == Money("67.50"),
          calculation.total == Money("67.50"),
          calculation.currency == Currency.USD,
        )
      },
      test("calculates total for single item with multiple quantity") {
        val config = makeConfig(
          id = "config-1",
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List(SampleCatalog.matteLamination),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val basket = BasketService.empty(basketId)
        val withItem = BasketService.addItem(basket, config, 3, pricelist).toEither.toOption.get
        val calculation = BasketService.calculateTotal(withItem)

        // Price for 500× business cards with matte lamination = 67.50
        // Total for 3× this configuration = 67.50 × 3 = 202.50
        assertTrue(
          calculation.items.size == 1,
          calculation.subtotal == Money("202.50"),
          calculation.total == Money("202.50"),
        )
      },
      test("calculates total for multiple items") {
        val config1 = makeConfig(
          id = "config-1",
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List.empty,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val config2 = makeConfig(
          id = "config-2",
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List(SampleCatalog.matteLamination),
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(500)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val basket = BasketService.empty(basketId)
        val withItems = BasketService
          .addItem(basket, config1, 2, pricelist)
          .flatMap(b => BasketService.addItem(b, config2, 1, pricelist))
          .toEither
          .toOption
          .get

        val calculation = BasketService.calculateTotal(withItems)

        // Config 1: 100× business cards = 12.00 (material only, no tier discount)
        // Config 1 total: 12.00 × 2 = 24.00
        // Config 2: 500× business cards with matte lamination = 67.50
        // Config 2 total: 67.50 × 1 = 67.50
        // Grand total: 24.00 + 67.50 = 91.50
        assertTrue(
          calculation.items.size == 2,
          calculation.subtotal == Money("91.50"),
          calculation.total == Money("91.50"),
        )
      },
    ),
    suite("clearing basket")(
      test("clears all items from basket") {
        val config = makeConfig(
          id = "config-1",
          category = SampleCatalog.businessCards,
          material = SampleCatalog.coated300gsm,
          printingMethod = SampleCatalog.offsetMethod,
          finishes = List.empty,
          specs = List(
            SpecValue.SizeSpec(Dimension(90, 55)),
            SpecValue.QuantitySpec(Quantity.unsafe(100)),
            SpecValue.ColorModeSpec(ColorMode.CMYK),
          ),
        )

        val basket = BasketService.empty(basketId)
        val withItem = BasketService.addItem(basket, config, 1, pricelist).toEither.toOption.get
        val cleared = BasketService.clear(withItem)

        assertTrue(
          withItem.items.size == 1,
          cleared.items.isEmpty,
          cleared.id == basketId,
        )
      },
    ),
  )
