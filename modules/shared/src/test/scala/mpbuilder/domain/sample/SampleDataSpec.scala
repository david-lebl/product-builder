package mpbuilder.domain.sample

import mpbuilder.domain.*
import mpbuilder.domain.catalog.*
import mpbuilder.domain.config.*
import mpbuilder.domain.ids.*
import mpbuilder.domain.pricing.*
import SampleIds.{category as cat, finish as fin, ink as inks, material as mat, method as met}
import zio.test.*

/** Golden tests running the real CZK sample pricelist through the engine —
  * locking the numbers of docs/ideas/full-catalog-czk-price-list.md.
  */
object SampleDataSpec extends ZIOSpecDefault:

  private val catalog   = SampleCatalog.catalog
  private val pricelist = SamplePricelistCzk.pricelist

  private def price(config: ProductConfiguration) = PricingEngine.price(catalog, pricelist, config)

  def spec = suite("CZK sample data")(

    test("§11 worked example — 200 premium business cards = 617 CZK") {
      // Coated Matte 350 (16/sheet) + digital 4/4 (4/sheet) + matte lam (6/sheet)
      // + round corners (0.50/sheet) at 21 copies/sheet → 10 sheets, ×26.50 = 265
      // + cutting 12 cuts/sheet × 10 × 0.10 = 12 → subtotal 277
      // 10 sheets → no discount tier; setup 300 (lamination) + 40 (round corners)
      val config = ProductConfiguration(
        categoryId = cat.businessCards,
        printingMethodId = met.digital,
        inkConfigurationId = inks.fullColorBoth,
        components = List(
          ComponentConfiguration(
            ComponentRole.Main,
            mat.coatedMatte(350),
            List(
              SelectedFinish(fin.matteLamination),
              SelectedFinish(fin.roundCorners, Some(FinishParams.RoundCorners(4, 3))),
            ),
          )
        ),
        details = ProductDetails(size = Some(DimensionsMm(85, 55)), quantity = Some(200)),
      )
      val result = price(config)
      val comp   = result.toOption.map(_.components.head)
      assertTrue(
        comp.flatMap(_.copiesPerSheet).contains(21),
        comp.flatMap(_.sheetsUsed).contains(10),
        result.map(_.subtotal.amount) == Right(BigDecimal("277.00")),
        result.toOption.exists(_.volumeDiscount.isEmpty),
        result.map(_.setupFees.map(_.fee.amount).sum) == Right(BigDecimal("340.00")),
        result.map(_.total.amount) == Right(BigDecimal("617.00")),
        result.toOption.exists(_.minimumApplied.isEmpty),
      )
    },

    test("outdoor banner preset — tiered PVC rate, per-m² ink/finishes, UV setup fee = 1,227 CZK") {
      // 1×1.5 m banner: 1.5 m² → 555/m² tier → 832.50; ink 4/0 22×1.5 = 33;
      // UV coating 1×1.5 = 1.50; grommets @500mm 40×1.5 = 60 → subtotal 927
      // + UV coating setup 300 → 1227
      val config = SamplePresets.presets.find(_.id == PresetId("banners-outdoor-grommets")).get.configuration
      val result = price(config)
      assertTrue(
        result.map(_.subtotal.amount) == Right(BigDecimal("927.00")),
        result.map(_.setupFees.map(_.fee.amount).sum) == Right(BigDecimal("300.00")),
        result.map(_.total.amount) == Right(BigDecimal("1227.00")),
      )
    },

    test("tiny order hits the 200 CZK minimum and reports it") {
      val config = ProductConfiguration(
        categoryId = cat.businessCards,
        printingMethodId = met.digital,
        inkConfigurationId = inks.fullColorFront,
        components = List(ComponentConfiguration(ComponentRole.Main, mat.coatedArt300)),
        details = ProductDetails(size = Some(DimensionsMm(85, 55)), quantity = Some(10)),
      )
      // 1 sheet: material 14 + ink 2 + cutting 1.20 = 17.20 → floored to 200
      val result = price(config)
      assertTrue(
        result.map(_.subtotal.amount) == Right(BigDecimal("17.20")),
        result.toOption.flatMap(_.minimumApplied).map(_.amount).contains(BigDecimal("200")),
        result.map(_.total.amount) == Right(BigDecimal("200")),
      )
    },

    test("saddle-stitch booklet preset — sheet discount across cover+body, one binding setup") {
      // cover 50 sheets (glossy-250) + body 200 sheets (glossy-130, 8 pages)
      // → 250 sheets total → ×0.80 tier
      val config = SamplePresets.presets.find(_.id == PresetId("booklets-saddle")).get.configuration
      val result = price(config)
      val sheets = result.toOption.map(_.components.flatMap(_.sheetsUsed))
      assertTrue(
        sheets.contains(List(50, 200)),
        result.toOption.flatMap(_.volumeDiscount).map(_.multiplier).contains(BigDecimal("0.80")),
        result.toOption.flatMap(_.volumeDiscount).map(_.basisCount).contains(250),
        result.map(_.setupFees.map(_.key)) == Right(List("binding:SaddleStitch")),
      )
    },

    test("lamination on cover and body of one product charges one setup fee") {
      val config = ProductConfiguration(
        categoryId = cat.booklets,
        printingMethodId = met.digital,
        inkConfigurationId = inks.fullColorBoth,
        components = List(
          ComponentConfiguration(ComponentRole.Cover, mat.coatedGlossy(250), List(SelectedFinish(fin.matteLamination))),
          ComponentConfiguration(ComponentRole.Body, mat.coatedGlossy(130), List(SelectedFinish(fin.matteLamination))),
        ),
        details = ProductDetails(
          size = Some(DimensionsMm(210, 297)),
          quantity = Some(100),
          pages = Some(8),
          bindingMethod = Some(BindingMethod.SaddleStitch),
        ),
      )
      val result = price(config)
      assertTrue(
        result.map(_.setupFees.count(_.key == s"finish:${fin.matteLamination.raw}")) == Right(1),
        result.map(_.setupFees.map(_.fee.amount).sum) == Right(BigDecimal("350.00")), // 300 lam + 50 binding
      )
    },

    test("UV coating: type-level surcharge (1 CZK) but specific setup fee (300 CZK)") {
      val config = ProductConfiguration(
        categoryId = cat.flyers,
        printingMethodId = met.digital,
        inkConfigurationId = inks.fullColorFront,
        components = List(ComponentConfiguration(ComponentRole.Main, mat.coatedGlossy(130), List(SelectedFinish(fin.uvCoating)))),
        details = ProductDetails(size = Some(DimensionsMm(148, 210)), quantity = Some(500), orientation = Some(Orientation.Portrait)),
      )
      val result = price(config)
      val uvLine = result.toOption.flatMap(_.components.head.lines.find(_.kind == LineKind.Finish))
      assertTrue(
        uvLine.map(_.unitPrice.amount).contains(BigDecimal("1")),
        result.map(_.setupFees.map(_.fee.amount)) == Right(List(BigDecimal("300"))),
      )
    },

    test("T-shirt preset — unit pricing, category + process surcharges, no ink charge, no discount") {
      val config = SamplePresets.presets.find(_.id == PresetId("tshirts-standard")).get.configuration
      val result = price(config)
      // 50 × (85 material + 15 category + 4 screen-printing) = 5200; no ink rule for screen printing
      assertTrue(
        result.map(_.subtotal.amount) == Right(BigDecimal("5200.00")),
        result.toOption.exists(_.volumeDiscount.isEmpty),
        result.toOption.exists(_.components.head.lines.forall(_.kind != LineKind.Ink)),
        result.map(_.total.amount) == Right(BigDecimal("5200.00")),
      )
    },

    test("all sample presets price successfully against the CZK pricelist") {
      val failures = SamplePresets.presets.flatMap { p =>
        price(p.configuration).swap.toOption.map(errs => p.id.raw -> errs.toList)
      }
      assertTrue(failures.isEmpty)
    },

    test("every catalog material has a price rule in the CZK pricelist") {
      import PricingRule.*
      val priced = pricelist.rules.collect {
        case MaterialTieredAreaPrice(id, _) => id
        case MaterialAreaPrice(id, _)       => id
        case MaterialSheetPrice(id, _)      => id
        case MaterialUnitPrice(id, _)       => id
      }.toSet
      val unpriced = catalog.materials.map(_.id).filterNot(priced)
      assertTrue(unpriced.isEmpty)
    },
  )
