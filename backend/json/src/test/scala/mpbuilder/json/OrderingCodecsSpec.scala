package mpbuilder.json

import kyo.*
import kyo.test.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.json.OrderingCodecs.given
import mpbuilder.json.SharedCodecs.given
import mpbuilder.ordering.*
import mpbuilder.shared.*

class OrderingCodecsSpec extends Test[Any]:

  private val line = OrderLine(
    lineId = OrderLineId.unsafe("line-1"),
    product = ProductSnapshot(
      categoryId = CategoryId.unsafe("cat-flyers"),
      categoryName = LocalizedString("Flyers", "Letáky"),
      catalogVersion = Version.unsafe("1.0"),
      components = NonEmptyChunk.single(
        ComponentSnapshot(
          key = ComponentKey.Main,
          materialId = MaterialId.unsafe("mat-1"),
          materialName = LocalizedString("Silk 170g", "Křída 170g"),
          printingMethodId = Some(PrintingMethodId.unsafe("pm-digital")),
          inkNotation = Some("4/4"),
          finishes = List(
            FinishSnapshot(
              finishId = FinishId.unsafe("fin-foil"),
              name = LocalizedString("Foil", "Fólie"),
              finishType = "FoilStamping",
              side = Some(FinishSnapshot.Side.Front),
              params = Some(FinishSnapshot.Params.FoilStamping(FinishSnapshot.FoilColor.RoseGold)),
            ),
            FinishSnapshot(
              finishId = FinishId.unsafe("fin-corners"),
              name = LocalizedString("Rounded Corners", "Zaoblené rohy"),
              finishType = "RoundCorners",
              side = None,
              params = Some(FinishSnapshot.Params.RoundCorners(4, 3)),
            ),
          ),
          flatSize = Dimension2D.A5,
          finishedSize = Dimension2D.A5,
          sheetsUsed = 10,
        )
      ),
      specs = ProductSpecSnapshot(
        dimensions = ProductSpecSnapshot.Dimensions.Folded2D(Dimension2D.A5, ProductSpecSnapshot.FoldType.Tri),
        orientation = Some(ProductSpecSnapshot.Orientation.Landscape),
        bleed = NonNegativeMm.unsafe(3),
        binding = Some(ProductSpecSnapshot.Binding(ProductSpecSnapshot.BindingMethod.PerfectBinding, 64, selfCover = true)),
        deliveryFormat = ProductSpecSnapshot.DeliveryFormat.RollLabels(coreDiameterMm = 76, unwindDirection = 3),
      ),
    ),
    runQuantity = Quantity.unsafe(100),
    versionCount = VersionCount.single,
    speed = ManufacturingSpeed.Standard,
    price = LinePriceSnapshot(
      quoteId = QuoteId.unsafe("q-1"),
      pricelistId = PricelistId.unsafe("pl-1"),
      pricelistVersion = Version.unsafe("1.0"),
      currency = Currency.CZK,
      netAmount = Money(BigDecimal("100.00")),
      taxRate = TaxRate.czStandard,
      taxAmount = Money(BigDecimal("21.00")),
      grossAmount = Money(BigDecimal("121.00")),
    ),
    artworkIds = Map.empty,
    productionNote = Some("Logo side is the front"),
  )

  "OrderingCodecs" - {
    "order lines round-trip with the full product configuration" in {
      assert(fromJsonUnsafe[OrderLine](toJson(line)) == line)
    }

    "order lines round-trip with production instructions" in {
      assert(fromJsonUnsafe[OrderLine](toJson(line)) == line)
    }

    "order lines persisted before production instructions existed still decode" in {
      val legacy = toJson(line.copy(productionNote = None)).replace(""","productionNote":null""", "")
      assert(!legacy.contains("productionNote"))
      assert(fromJson[OrderLine](legacy) == Right(line.copy(productionNote = None)))
    }
  }
