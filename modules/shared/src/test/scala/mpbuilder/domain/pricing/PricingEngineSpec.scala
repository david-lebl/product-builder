package mpbuilder.domain.pricing

import mpbuilder.domain.*
import mpbuilder.domain.catalog.*
import mpbuilder.domain.config.*
import mpbuilder.domain.ids.*
import zio.test.*

/** Golden tests against the worked examples in
  * docs/ideas/business-specification.md §5.13, plus engine edge cases.
  * Each example uses a purpose-built mini pricelist matching its figures.
  */
object PricingEngineSpec extends ZIOSpecDefault:

  // ---- shared mini-catalog fixtures -----------------------------------------

  private def txt(s: String) = LocalizedText.plain(s)

  private val cards    = CategoryId("cards")
  private val booklets = CategoryId("booklets")

  private val paper     = MaterialId("paper")
  private val coverPap  = MaterialId("cover-paper")
  private val vinyl     = MaterialId("vinyl")

  private val matteLam  = FinishId("matte-lamination")
  private val glossLam  = FinishId("gloss-lamination")
  private val uvCoat    = FinishId("uv-coating")
  private val scoring   = FinishId("scoring")
  private val grommets  = FinishId("grommets")
  private val gumRope   = FinishId("gum-rope")

  private val digital  = PrintingMethodId("digital")
  private val offset   = PrintingMethodId("offset")
  private val uvInkjet = PrintingMethodId("uv-inkjet")

  private val ink44 = InkConfigId("4-4")

  private val catalog = Catalog(
    categories = List(
      Category(cards, txt("Cards"), List(ComponentSpec(ComponentRole.Main, false, AllowList.All(), AllowList.All())),
        Set(RequiredDetail.Size, RequiredDetail.Quantity), AllowList.All()),
      Category(booklets, txt("Booklets"),
        List(
          ComponentSpec(ComponentRole.Cover, false, AllowList.All(), AllowList.All()),
          ComponentSpec(ComponentRole.Body, false, AllowList.All(), AllowList.All()),
        ),
        Set(RequiredDetail.Size, RequiredDetail.Quantity, RequiredDetail.Pages, RequiredDetail.Binding),
        AllowList.All()),
    ),
    materials = List(
      Material(paper, txt("Paper"), Some(300), Set()),
      Material(coverPap, txt("Cover paper"), Some(250), Set()),
      Material(vinyl, txt("Adhesive vinyl"), None, Set(MaterialProperty.WaterResistant)),
    ),
    finishes = List(
      Finish(matteLam, txt("Matte lamination"), FinishType.Lamination),
      Finish(glossLam, txt("Gloss lamination"), FinishType.Lamination),
      Finish(uvCoat, txt("UV coating"), FinishType.UvCoating),
      Finish(scoring, txt("Scoring"), FinishType.Scoring),
      Finish(grommets, txt("Grommets"), FinishType.Grommets),
      Finish(gumRope, txt("Gum rope"), FinishType.GumRope),
    ),
    printingMethods = List(
      PrintingMethod(digital, txt("Digital"), None),
      PrintingMethod(offset, txt("Offset"), Some(6)),
      PrintingMethod(uvInkjet, txt("UV Inkjet"), None),
    ),
    inkConfigurations = List(InkConfiguration(ink44, txt("4/4 full color both sides"), 4, 4)),
  )

  private def config(
    categoryId: CategoryId = cards,
    method: PrintingMethodId = digital,
    components: List[ComponentConfiguration] = List(ComponentConfiguration(ComponentRole.Main, paper)),
    size: Option[DimensionsMm] = None,
    quantity: Option[Int] = Some(500),
    pages: Option[Int] = None,
    fold: Option[FoldType] = None,
    binding: Option[BindingMethod] = None,
    speed: SpeedTier = SpeedTier.Standard,
  ) = ProductConfiguration(
    categoryId = categoryId,
    printingMethodId = method,
    inkConfigurationId = ink44,
    components = components,
    details = ProductDetails(size, quantity, None, pages, fold, binding),
    speedTier = speed,
  )

  private def pricelist(currency: Currency)(rules: PricingRule*) =
    Pricelist("test", currency, rules.toList)

  private def total(result: Either[?, PriceBreakdown]): Option[BigDecimal] =
    result.toOption.map(_.total.amount)

  import PricingRule.*

  // ---------------------------------------------------------------------------

  def spec = suite("PricingEngine")(

    test("§5.13 ex.1 — 500 business cards, unit-priced, offset 4/4, matte lamination → $67.50") {
      val pl = pricelist(Currency.USD)(
        MaterialUnitPrice(paper, BigDecimal("0.08")),
        InkPricePerUnit(offset, ink44, BigDecimal("0.04")),
        FinishSurcharge(FinishTarget.Specific(matteLam), BigDecimal("0.03")),
        QuantityVolumeDiscount(List(DiscountTier(1, 1), DiscountTier(250, BigDecimal("0.90")), DiscountTier(1000, BigDecimal("0.80")))),
      )
      val cfg = config(
        method = offset,
        components = List(ComponentConfiguration(ComponentRole.Main, paper, List(SelectedFinish(matteLam)))),
      )
      val result = PricingEngine.price(catalog, pl, cfg)
      assertTrue(
        result.map(_.subtotal.amount) == Right(BigDecimal("75.00")),
        result.toOption.flatMap(_.volumeDiscount).map(_.multiplier).contains(BigDecimal("0.90")),
        total(result).contains(BigDecimal("67.50")),
      )
    },

    test("§5.13 ex.2 — 100 tri-fold brochures, sheet-priced → 2,245 CZK with setup fees after discount") {
      val pl = pricelist(Currency.CZK)(
        MaterialSheetPrice(paper, BigDecimal(40)),
        FinishSurcharge(FinishTarget.Specific(matteLam), BigDecimal(4)),
        FoldSurcharge(FoldType.TriFold, BigDecimal("1.50")),
        SheetVolumeDiscount(List(DiscountTier(50, BigDecimal("0.90")))),
        FinishSetupFee(FinishTarget.Specific(matteLam), BigDecimal(50)),
        FoldSetupFee(FoldType.TriFold, BigDecimal(80)),
      )
      // A4 (210×297) nests 2/sheet → 100 brochures = 50 sheets → material 2000, lamination 200
      val cfg = config(
        components = List(ComponentConfiguration(ComponentRole.Main, paper, List(SelectedFinish(matteLam)))),
        size = Some(DimensionsMm(210, 297)),
        quantity = Some(100),
        fold = Some(FoldType.TriFold),
      )
      val result = PricingEngine.price(catalog, pl, cfg)
      assertTrue(
        result.map(_.subtotal.amount) == Right(BigDecimal("2350.00")),
        result.map(_.discountedSubtotal.amount) == Right(BigDecimal("2115.00")),
        result.map(_.setupFees.map(_.fee.amount).sum) == Right(BigDecimal("130.00")),
        total(result).contains(BigDecimal("2245.00")),
      )
    },

    test("§5.13 ex.3 — 500 creased brochures, 2 creases → 6,860 CZK") {
      val pl = pricelist(Currency.CZK)(
        MaterialUnitPrice(paper, BigDecimal(12)),
        InkPricePerUnit(digital, ink44, BigDecimal(3)),
        CreaseCountPrice(2, BigDecimal("1.00")),
        CreaseCountPrice(3, BigDecimal("1.30")),
        QuantityVolumeDiscount(List(DiscountTier(500, BigDecimal("0.85")))),
        CreasingSetupFee(BigDecimal(60)),
        FinishSetupFee(FinishTarget.OfType(FinishType.Scoring), BigDecimal(999)), // must lose to CreasingSetupFee
      )
      def cfg(creases: Int) = config(
        components = List(
          ComponentConfiguration(ComponentRole.Main, paper, List(SelectedFinish(scoring, Some(FinishParams.Creases(creases)))))
        )
      )
      val two   = PricingEngine.price(catalog, pl, cfg(2))
      val three = PricingEngine.price(catalog, pl, cfg(3))
      val nine  = PricingEngine.price(catalog, pl, cfg(9))
      assertTrue(
        two.map(_.subtotal.amount) == Right(BigDecimal("8000.00")),
        two.map(_.discountedSubtotal.amount) == Right(BigDecimal("6800.00")),
        two.map(_.setupFees.map(_.fee.amount).sum) == Right(BigDecimal("60.00")),
        total(two).contains(BigDecimal("6860.00")),
        // 3 creases uses its own configured price (1.30 × 500), not 1.5 × the 2-crease price
        three.map(_.subtotal.amount) == Right(BigDecimal("8150.00")),
        // unpriced crease count is a hard error, never a silent zero charge
        nine.swap.toOption.exists(_.contains(PricingError.NoCreaseCountPrice(9))),
      )
    },

    test("§5.13 ex.4 — 10 banners 1m×0.5m, area-priced → $90.20, no qualifying tier = no discount") {
      val pl = pricelist(Currency.USD)(
        MaterialAreaPrice(vinyl, BigDecimal("16.20")),
        InkPricePerM2(uvInkjet, ink44, BigDecimal("1.80")),
        FinishSurcharge(FinishTarget.Specific(uvCoat), BigDecimal("0.04")),
        QuantityVolumeDiscount(List(DiscountTier(1, 1), DiscountTier(250, BigDecimal("0.90")))),
      )
      val cfg = config(
        method = uvInkjet,
        components = List(ComponentConfiguration(ComponentRole.Main, vinyl, List(SelectedFinish(uvCoat)))),
        size = Some(DimensionsMm(1000, 500)),
        quantity = Some(10),
      )
      val result = PricingEngine.price(catalog, pl, cfg)
      assertTrue(
        result.map(_.subtotal.amount) == Right(BigDecimal("90.20")),
        result.toOption.exists(_.volumeDiscount.isEmpty),
        total(result).contains(BigDecimal("90.20")),
      )
    },

    test("speed multipliers apply after discount, before setup fees; setup fees never multiplied") {
      val pl = pricelist(Currency.CZK)(
        MaterialUnitPrice(paper, BigDecimal(12)),
        QuantityVolumeDiscount(List(DiscountTier(500, BigDecimal("0.85")))),
        CreasingSetupFee(BigDecimal(60)),
        CreaseCountPrice(2, BigDecimal("1.00")),
        SpeedMultiplier(SpeedTier.Express, BigDecimal("1.35")),
        SpeedMultiplier(SpeedTier.Standard, BigDecimal(1)),
        SpeedMultiplier(SpeedTier.Economy, BigDecimal("0.85")),
      )
      def cfg(speed: SpeedTier) = config(
        components = List(
          ComponentConfiguration(ComponentRole.Main, paper, List(SelectedFinish(scoring, Some(FinishParams.Creases(2)))))
        ),
        speed = speed,
      )
      // subtotal 6500 → ×0.85 = 5525
      val express  = PricingEngine.price(catalog, pl, cfg(SpeedTier.Express))
      val standard = PricingEngine.price(catalog, pl, cfg(SpeedTier.Standard))
      val economy  = PricingEngine.price(catalog, pl, cfg(SpeedTier.Economy))
      assertTrue(
        total(express).contains(BigDecimal("5525.00") * BigDecimal("1.35") + 60),  // 7458.75 + 60
        total(standard).contains(BigDecimal("5585.00")),
        standard.toOption.exists(_.speedAdjustment.isEmpty),
        total(economy).contains(BigDecimal("5525.00") * BigDecimal("0.85") + 60),  // 4696.25 + 60
      )
    },

    test("minimum order price floors the total and is reported") {
      val pl = pricelist(Currency.CZK)(
        MaterialUnitPrice(paper, BigDecimal("0.50")),
        MinimumOrderPrice(BigDecimal(200)),
      )
      val cfg    = config(quantity = Some(10))
      val result = PricingEngine.price(catalog, pl, cfg)
      assertTrue(
        total(result).contains(BigDecimal("200")),
        result.toOption.flatMap(_.minimumApplied).map(_.amount).contains(BigDecimal(200)),
      )
    },

    test("setup fee charged once per distinct finish across components (booklet cover+body lamination)") {
      val pl = pricelist(Currency.CZK)(
        MaterialSheetPrice(paper, BigDecimal(6)),
        MaterialSheetPrice(coverPap, BigDecimal(12)),
        FinishSurcharge(FinishTarget.OfType(FinishType.Lamination), BigDecimal(6)),
        FinishSetupFee(FinishTarget.OfType(FinishType.Lamination), BigDecimal(300)),
        BindingSurcharge(BindingMethod.SaddleStitch, BigDecimal(1)),
        BindingSetupFee(BindingMethod.SaddleStitch, BigDecimal(50)),
      )
      val cfg = config(
        categoryId = booklets,
        components = List(
          ComponentConfiguration(ComponentRole.Cover, coverPap, List(SelectedFinish(matteLam))),
          ComponentConfiguration(ComponentRole.Body, paper, List(SelectedFinish(matteLam))),
        ),
        size = Some(DimensionsMm(210, 297)),
        quantity = Some(100),
        pages = Some(8),
        binding = Some(BindingMethod.SaddleStitch),
      )
      val result = PricingEngine.price(catalog, pl, cfg)
      assertTrue(
        result.map(_.setupFees.map(_.key)) == Right(List("finish:matte-lamination", "binding:SaddleStitch")),
        result.map(_.setupFees.map(_.fee.amount).sum) == Right(BigDecimal(350)),
      )
    },

    test("paged Body component uses the 2-pages-per-copy-slot sheet convention") {
      val pl  = pricelist(Currency.CZK)(MaterialSheetPrice(paper, BigDecimal(10)))
      val cfg = config(
        categoryId = booklets,
        components = List(ComponentConfiguration(ComponentRole.Body, paper)),
        size = Some(DimensionsMm(210, 297)), // 2 slots/sheet
        quantity = Some(100),
        pages = Some(8), // 4 leaves each → 400 leaves → 200 sheets
      )
      val result = PricingEngine.price(catalog, pl, cfg)
      assertTrue(result.map(_.components.head.sheetsUsed) == Right(Some(200)))
    },

    test("specific finish surcharge beats finish-type surcharge") {
      val pl = pricelist(Currency.CZK)(
        MaterialUnitPrice(paper, BigDecimal(1)),
        FinishSurcharge(FinishTarget.OfType(FinishType.Lamination), BigDecimal(9)),
        FinishSurcharge(FinishTarget.Specific(matteLam), BigDecimal(6)),
      )
      def cfg(finish: FinishId) = config(
        components = List(ComponentConfiguration(ComponentRole.Main, paper, List(SelectedFinish(finish)))),
        quantity = Some(10),
      )
      val specific = PricingEngine.price(catalog, pl, cfg(matteLam))
      val typed    = PricingEngine.price(catalog, pl, cfg(glossLam))
      def finishTotal(r: Either[?, PriceBreakdown]) =
        r.toOption.flatMap(_.components.head.lines.find(_.kind == LineKind.Finish)).map(_.total.amount)
      assertTrue(
        finishTotal(specific).contains(BigDecimal("60.00")), // 6 × 10, specific wins
        finishTotal(typed).contains(BigDecimal("90.00")),    // 9 × 10, type fallback
      )
    },

    test("grommets priced by spacing and area; unpriced spacing is an error") {
      val pl = pricelist(Currency.CZK)(
        MaterialAreaPrice(vinyl, BigDecimal(100)),
        GrommetSpacingPrice(500, BigDecimal(40)),
      )
      def cfg(spacing: Int) = config(
        components = List(
          ComponentConfiguration(ComponentRole.Main, vinyl, List(SelectedFinish(grommets, Some(FinishParams.GrommetSpacing(spacing)))))
        ),
        size = Some(DimensionsMm(1000, 1500)), // 1.5 m²
        quantity = Some(1),
      )
      val priced  = PricingEngine.price(catalog, pl, cfg(500))
      val missing = PricingEngine.price(catalog, pl, cfg(300))
      assertTrue(
        priced.map(_.components.head.lines.filter(_.kind == LineKind.Finish).map(_.total.amount).sum) ==
          Right(BigDecimal("60.00")), // 40 × 1.5
        missing.swap.toOption.exists(_.contains(PricingError.NoGrommetSpacingPrice(300))),
      )
    },

    test("gum rope priced per linear metre") {
      val pl = pricelist(Currency.CZK)(
        MaterialAreaPrice(vinyl, BigDecimal(100)),
        LinearLengthPrice(FinishTarget.OfType(FinishType.GumRope), BigDecimal(18)),
      )
      val cfg = config(
        components = List(
          ComponentConfiguration(ComponentRole.Main, vinyl, List(SelectedFinish(gumRope, Some(FinishParams.RopeLength(BigDecimal(3))))))
        ),
        size = Some(DimensionsMm(1000, 1000)),
        quantity = Some(2),
      )
      val result = PricingEngine.price(catalog, pl, cfg)
      assertTrue(
        result.map(_.components.head.lines.filter(_.kind == LineKind.Finish).map(_.total.amount).sum) ==
          Right(BigDecimal("108.00")) // 18 × 3 m × 2 pcs
      )
    },

    test("tiered area pricing picks the tier by per-item area, including the boundary") {
      val tiers = List(
        AreaTier(BigDecimal(0), BigDecimal(555)),
        AreaTier(BigDecimal(2), BigDecimal(455)),
        AreaTier(BigDecimal(5), BigDecimal(405)),
        AreaTier(BigDecimal(10), BigDecimal(355)),
      )
      val pl = pricelist(Currency.CZK)(MaterialTieredAreaPrice(vinyl, tiers))
      def cfg(w: Int, h: Int) = config(
        components = List(ComponentConfiguration(ComponentRole.Main, vinyl)),
        size = Some(DimensionsMm(w, h)),
        quantity = Some(1),
      )
      def materialRate(r: Either[?, PriceBreakdown]) =
        r.toOption.map(_.components.head.lines.head.unitPrice.amount)
      assertTrue(
        materialRate(PricingEngine.price(catalog, pl, cfg(1000, 1500))).contains(BigDecimal(555)), // 1.5 m²
        materialRate(PricingEngine.price(catalog, pl, cfg(1000, 2000))).contains(BigDecimal(455)), // exactly 2 m²
        materialRate(PricingEngine.price(catalog, pl, cfg(2000, 3000))).contains(BigDecimal(405)), // 6 m²
        materialRate(PricingEngine.price(catalog, pl, cfg(4000, 3000))).contains(BigDecimal(355)), // 12 m²
      )
    },

    test("cutting surcharge bills per cut on sheet-mode components") {
      val pl = pricelist(Currency.CZK)(
        MaterialSheetPrice(paper, BigDecimal(16)),
        CuttingSurcharge(BigDecimal("0.10")),
      )
      val cfg = config(
        size = Some(DimensionsMm(85, 55)), // 3×7 layout → 12 cuts/sheet
        quantity = Some(200),              // 10 sheets → 120 cuts
      )
      val result = PricingEngine.price(catalog, pl, cfg)
      val cutting = result.toOption.flatMap(_.components.head.lines.find(_.kind == LineKind.Cutting))
      assertTrue(
        cutting.map(_.billedQuantity).contains(BigDecimal(120)),
        cutting.map(_.total.amount).contains(BigDecimal("12.00")),
      )
    },

    test("errors accumulate: no quantity + two unpriced materials reported together") {
      val pl  = pricelist(Currency.CZK)()
      val cfg = config(
        categoryId = booklets,
        components = List(
          ComponentConfiguration(ComponentRole.Cover, coverPap),
          ComponentConfiguration(ComponentRole.Body, paper),
        ),
        quantity = None,
      )
      val errors = PricingEngine.price(catalog, pl, cfg).swap.toOption.map(_.toList)
      assertTrue(
        errors.exists(_.contains(PricingError.NoQuantity)),
        errors.exists(_.contains(PricingError.NoMaterialPrice(coverPap))),
        errors.exists(_.contains(PricingError.NoMaterialPrice(paper))),
        errors.exists(_.size == 3),
      )
    },

    test("sheet-priced material without a size is an error") {
      val pl  = pricelist(Currency.CZK)(MaterialSheetPrice(paper, BigDecimal(16)))
      val cfg = config(size = None)
      assertTrue(
        PricingEngine.price(catalog, pl, cfg).swap.toOption
          .exists(_.contains(PricingError.SizeRequiredForPricing(paper)))
      )
    },

    test("intermediate results round HALF_UP to 2 decimals at every step") {
      val pl = pricelist(Currency.CZK)(
        MaterialUnitPrice(paper, BigDecimal("0.333")),
        QuantityVolumeDiscount(List(DiscountTier(1, BigDecimal("0.85")))),
      )
      val cfg    = config(quantity = Some(101))
      val result = PricingEngine.price(catalog, pl, cfg)
      // line: 0.333 × 101 = 33.633 → 33.63; discount: 33.63 × 0.85 = 28.5855 → 28.59
      assertTrue(
        result.map(_.subtotal.amount) == Right(BigDecimal("33.63")),
        result.map(_.discountedSubtotal.amount) == Right(BigDecimal("28.59")),
      )
    },
  )
