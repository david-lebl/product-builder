package mpbuilder.catalog.sample

import mpbuilder.catalog.*
import mpbuilder.shared.*

/** Full sample product catalog replicated and modernized from legacy `SampleCatalog` + `SampleShowcase`.
  *
  * Includes:
  *   - 50 materials across Paper, Synthetic, Adhesive, Vinyl, Cardboard, Fabric, Ceramic, Metal, Wood & Hardware
  *     with physical `MediaFormat` (SRA3 sheets, B2 carton sheets, 1600 mm wide-format rolls, discrete blanks)
  *   - 9 printing methods with explicit duplex & special-ink capabilities (`DTG`, `Sublimation`, `White` ink)
  *   - 29 finishes with declarative `FinishSidePolicy` and `FinishParamSchema`
  *   - 15 product categories with multi-component templates (including non-printable Roll-Up Stand), presets & showcases
  */
object SampleCatalog:

  // ── Stock Format IDs ───────────────────────────────────────────────────────
  val sra3FormatId: StockFormatId    = StockFormatId.unsafe("fmt-sra3")
  val b2FormatId: StockFormatId      = StockFormatId.unsafe("fmt-b2")
  val roll1600FormatId: StockFormatId = StockFormatId.unsafe("fmt-roll-1600")

  private val sra3Sheet: MediaFormat = MediaFormat.SheetStock(
    formatId = sra3FormatId,
    sheetSize = Dimension2D.SRA3,
    defaultBleed = NonNegativeMm.unsafe(3),
    defaultGutter = NonNegativeMm.unsafe(2),
  )

  private val b2Sheet: MediaFormat = MediaFormat.SheetStock(
    formatId = b2FormatId,
    sheetSize = Dimension2D.B2,
    defaultBleed = NonNegativeMm.unsafe(3),
    defaultGutter = NonNegativeMm.unsafe(4),
  )

  private val roll1600: MediaFormat = MediaFormat.RollStock(
    formatId = roll1600FormatId,
    rollWidth = PositiveMm.unsafe(1600),
    printableWidth = PositiveMm.unsafe(1550),
  )

  private def discrete(w: Int, h: Int): NonEmptyChunk[MediaFormat] =
    NonEmptyChunk.single(MediaFormat.DiscreteBlank(Some(Dimension2D.unsafeMm(w, h))))

  private val discreteUnsized: NonEmptyChunk[MediaFormat] =
    NonEmptyChunk.single(MediaFormat.DiscreteBlank(None))

  // ── Material IDs (50) ──────────────────────────────────────────────────────
  val coated300gsmId: MaterialId     = MaterialId.unsafe("mat-coated-300gsm")
  val uncoatedBondId: MaterialId     = MaterialId.unsafe("mat-uncoated-bond")
  val kraftId: MaterialId            = MaterialId.unsafe("mat-kraft")
  val vinylId: MaterialId            = MaterialId.unsafe("mat-vinyl")
  val pvc510gId: MaterialId          = MaterialId.unsafe("mat-pvc-510g")
  val corrugatedId: MaterialId       = MaterialId.unsafe("mat-corrugated")
  val coatedSilk250gsmId: MaterialId = MaterialId.unsafe("mat-coated-silk-250gsm")
  val yupoId: MaterialId             = MaterialId.unsafe("mat-yupo")
  val adhesiveStockId: MaterialId    = MaterialId.unsafe("mat-adhesive-stock")
  val clearVinylId: MaterialId       = MaterialId.unsafe("mat-clear-vinyl")
  val cottonId: MaterialId           = MaterialId.unsafe("mat-cotton")

  // Coated Art Paper — Glossy
  val coatedGlossy90gsmId: MaterialId  = MaterialId.unsafe("mat-coated-glossy-90gsm")
  val coatedGlossy115gsmId: MaterialId = MaterialId.unsafe("mat-coated-glossy-115gsm")
  val coatedGlossy130gsmId: MaterialId = MaterialId.unsafe("mat-coated-glossy-130gsm")
  val coatedGlossy150gsmId: MaterialId = MaterialId.unsafe("mat-coated-glossy-150gsm")
  val coatedGlossy170gsmId: MaterialId = MaterialId.unsafe("mat-coated-glossy-170gsm")
  val coatedGlossy200gsmId: MaterialId = MaterialId.unsafe("mat-coated-glossy-200gsm")
  val coatedGlossy250gsmId: MaterialId = MaterialId.unsafe("mat-coated-glossy-250gsm")
  val coatedGlossy350gsmId: MaterialId = MaterialId.unsafe("mat-coated-glossy-350gsm")

  // Coated Art Paper — Matte
  val coatedMatte90gsmId: MaterialId  = MaterialId.unsafe("mat-coated-matte-90gsm")
  val coatedMatte115gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-115gsm")
  val coatedMatte130gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-130gsm")
  val coatedMatte150gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-150gsm")
  val coatedMatte170gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-170gsm")
  val coatedMatte200gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-200gsm")
  val coatedMatte250gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-250gsm")
  val coatedMatte300gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-300gsm")
  val coatedMatte350gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-350gsm")

  // Roll-Up
  val rollUpBannerFilmId: MaterialId   = MaterialId.unsafe("mat-rollup-banner-film")
  val rollUpStandEconomyId: MaterialId = MaterialId.unsafe("mat-rollup-stand-economy")
  val rollUpStandPremiumId: MaterialId = MaterialId.unsafe("mat-rollup-stand-premium")

  // Promotional — T-Shirts
  val cottonTshirt150Id: MaterialId     = MaterialId.unsafe("mat-cotton-tshirt-150")
  val cottonTshirt180Id: MaterialId     = MaterialId.unsafe("mat-cotton-tshirt-180")
  val polyesterTshirtId: MaterialId     = MaterialId.unsafe("mat-polyester-tshirt")
  val cottonPolyBlendId: MaterialId     = MaterialId.unsafe("mat-cotton-poly-blend")
  val organicCottonTshirtId: MaterialId = MaterialId.unsafe("mat-organic-cotton-tshirt")

  // Promotional — Eco Bags
  val cottonCanvasBagId: MaterialId  = MaterialId.unsafe("mat-cotton-canvas-bag")
  val organicCottonBagId: MaterialId = MaterialId.unsafe("mat-organic-cotton-bag")
  val recycledPetBagId: MaterialId   = MaterialId.unsafe("mat-recycled-pet-bag")
  val juteBagId: MaterialId          = MaterialId.unsafe("mat-jute-bag")
  val nonWovenPpBagId: MaterialId    = MaterialId.unsafe("mat-non-woven-pp-bag")

  // Promotional — Pin Badges
  val tinplateBadgeId: MaterialId = MaterialId.unsafe("mat-tinplate-badge")
  val acrylicBadgeId: MaterialId  = MaterialId.unsafe("mat-acrylic-badge")
  val woodenBadgeId: MaterialId   = MaterialId.unsafe("mat-wooden-badge")

  // Promotional — Cups & Mugs
  val ceramicMugWhiteId: MaterialId    = MaterialId.unsafe("mat-ceramic-mug-white")
  val ceramicMugColoredId: MaterialId  = MaterialId.unsafe("mat-ceramic-mug-colored")
  val magicMugId: MaterialId           = MaterialId.unsafe("mat-magic-mug")
  val stainlessTravelMugId: MaterialId = MaterialId.unsafe("mat-stainless-travel-mug")
  val enamelMugId: MaterialId          = MaterialId.unsafe("mat-enamel-mug")
  val glassMugId: MaterialId           = MaterialId.unsafe("mat-glass-mug")

  // ── Printing Method IDs (9) ────────────────────────────────────────────────
  val offsetId: PrintingMethodId        = PrintingMethodId.unsafe("pm-offset")
  val digitalId: PrintingMethodId       = PrintingMethodId.unsafe("pm-digital")
  val uvInkjetId: PrintingMethodId      = PrintingMethodId.unsafe("pm-uv-inkjet")
  val letterpressId: PrintingMethodId   = PrintingMethodId.unsafe("pm-letterpress")
  val solventInkjetId: PrintingMethodId = PrintingMethodId.unsafe("pm-solvent-inkjet")
  val epson8ColorId: PrintingMethodId   = PrintingMethodId.unsafe("pm-epson-8color")
  val screenPrintId: PrintingMethodId   = PrintingMethodId.unsafe("pm-screen-print")
  val dtgId: PrintingMethodId           = PrintingMethodId.unsafe("pm-dtg")
  val sublimationId: PrintingMethodId   = PrintingMethodId.unsafe("pm-sublimation")

  // ── Finish IDs (29) ────────────────────────────────────────────────────────
  val matteLaminationId: FinishId  = FinishId.unsafe("fin-matte-lamination")
  val glossLaminationId: FinishId  = FinishId.unsafe("fin-gloss-lamination")
  val uvCoatingId: FinishId        = FinishId.unsafe("fin-uv-coating")
  val embossingId: FinishId        = FinishId.unsafe("fin-embossing")
  val foilStampingId: FinishId     = FinishId.unsafe("fin-foil-stamping")
  val dieCutId: FinishId           = FinishId.unsafe("fin-die-cut")
  val varnishId: FinishId          = FinishId.unsafe("fin-varnish")
  val softTouchCoatingId: FinishId = FinishId.unsafe("fin-soft-touch")
  val aqueousCoatingId: FinishId   = FinishId.unsafe("fin-aqueous-coating")
  val debossingId: FinishId        = FinishId.unsafe("fin-debossing")
  val scoringId: FinishId          = FinishId.unsafe("fin-scoring")
  val perforationId: FinishId      = FinishId.unsafe("fin-perforation")
  val roundCornersId: FinishId     = FinishId.unsafe("fin-round-corners")
  val grommetsId: FinishId         = FinishId.unsafe("fin-grommets")
  val kissCutId: FinishId          = FinishId.unsafe("fin-kiss-cut")
  val overlaminationId: FinishId   = FinishId.unsafe("fin-overlamination")
  val gumRopeId: FinishId          = FinishId.unsafe("fin-gum-rope")

  // Promotional Finish IDs
  val heatPressId: FinishId         = FinishId.unsafe("fin-heat-press")
  val labelPrintId: FinishId        = FinishId.unsafe("fin-label-print")
  val foldBagId: FinishId           = FinishId.unsafe("fin-fold-bag")
  val mylarOverlayId: FinishId      = FinishId.unsafe("fin-mylar-overlay")
  val safetyPinId: FinishId         = FinishId.unsafe("fin-safety-pin")
  val magnetBackId: FinishId        = FinishId.unsafe("fin-magnet-back")
  val bottleOpenerId: FinishId      = FinishId.unsafe("fin-bottle-opener")
  val dishwasherCoatId: FinishId    = FinishId.unsafe("fin-dishwasher-coat")
  val giftBoxId: FinishId           = FinishId.unsafe("fin-gift-box")
  val glossyGlazeId: FinishId       = FinishId.unsafe("fin-glossy-glaze")
  val embroideryId: FinishId        = FinishId.unsafe("fin-embroidery")
  val reinforcedHandlesId: FinishId = FinishId.unsafe("fin-reinforced-handles")

  // ── Category IDs (15) ──────────────────────────────────────────────────────
  val businessCardsId: CategoryId = CategoryId.unsafe("cat-business-cards")
  val flyersId: CategoryId        = CategoryId.unsafe("cat-flyers")
  val brochuresId: CategoryId     = CategoryId.unsafe("cat-brochures")
  val bannersId: CategoryId       = CategoryId.unsafe("cat-banners")
  val packagingId: CategoryId     = CategoryId.unsafe("cat-packaging")
  val bookletsId: CategoryId      = CategoryId.unsafe("cat-booklets")
  val calendarsId: CategoryId     = CategoryId.unsafe("cat-calendars")
  val postcardsId: CategoryId     = CategoryId.unsafe("cat-postcards")
  val stickersId: CategoryId      = CategoryId.unsafe("cat-stickers")
  val rollUpsId: CategoryId       = CategoryId.unsafe("cat-roll-ups")
  val tshirtsId: CategoryId       = CategoryId.unsafe("cat-tshirts")
  val ecoBagsId: CategoryId       = CategoryId.unsafe("cat-eco-bags")
  val pinBadgesId: CategoryId     = CategoryId.unsafe("cat-pin-badges")
  val cupsId: CategoryId          = CategoryId.unsafe("cat-cups")
  val freeId: CategoryId          = CategoryId.unsafe("cat-free")

  // ── Materials (50) ─────────────────────────────────────────────────────────

  private def glossyPaper(id: MaterialId, gsm: Int): Material = Material(
    id = id,
    name = LocalizedString(s"Coated Art Paper Glossy ${gsm}gsm", s"Křídový papír lesklý ${gsm}g"),
    family = MaterialFamily.Paper,
    surfaceCoating = SurfaceCoating.CoatedGloss,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(gsm)),
    stockFormats = NonEmptyChunk.single(sra3Sheet),
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.Recyclable),
  )

  private def mattePaper(id: MaterialId, gsm: Int): Material = Material(
    id = id,
    name = LocalizedString(s"Coated Art Paper Matte ${gsm}gsm", s"Křídový papír matný ${gsm}g"),
    family = MaterialFamily.Paper,
    surfaceCoating = SurfaceCoating.CoatedMatte,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(gsm)),
    stockFormats = NonEmptyChunk.single(sra3Sheet),
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.Recyclable),
  )

  val coated300gsm: Material = Material(
    id = coated300gsmId,
    name = LocalizedString("Coated Art Paper Glossy 300gsm", "Křídový papír lesklý 300g"),
    family = MaterialFamily.Paper,
    surfaceCoating = SurfaceCoating.CoatedGloss,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(300)),
    stockFormats = NonEmptyChunk.single(sra3Sheet),
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.Recyclable),
  )

  val uncoatedBond: Material = Material(
    id = uncoatedBondId,
    name = LocalizedString("Uncoated Bond 120gsm", "Nenatíraný ofsetový papír 120g"),
    family = MaterialFamily.Paper,
    surfaceCoating = SurfaceCoating.Uncoated,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(120)),
    stockFormats = NonEmptyChunk.single(sra3Sheet),
    properties = Set(MaterialProperty.Recyclable, MaterialProperty.Writable),
  )

  val kraft: Material = Material(
    id = kraftId,
    name = LocalizedString("Kraft Paper 250gsm", "Kraftový papír 250g"),
    family = MaterialFamily.Paper,
    surfaceCoating = SurfaceCoating.NaturalTextured,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(250)),
    stockFormats = NonEmptyChunk(sra3Sheet, b2Sheet),
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable, MaterialProperty.Writable),
  )

  val vinyl: Material = Material(
    id = vinylId,
    name = LocalizedString("Self-Adhesive Vinyl", "Samolepicí vinyl"),
    family = MaterialFamily.Vinyl,
    surfaceCoating = SurfaceCoating.CoatedGloss,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(140)),
    stockFormats = NonEmptyChunk.single(roll1600),
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.SelfAdhesive, MaterialProperty.OutdoorRated),
  )

  val pvc510g: Material = Material(
    id = pvc510gId,
    name = LocalizedString("PVC Frontlit Banner 510g", "PVC banner 510g"),
    family = MaterialFamily.Vinyl,
    surfaceCoating = SurfaceCoating.CoatedMatte,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(510)),
    stockFormats = NonEmptyChunk.single(roll1600),
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.TearResistant, MaterialProperty.OutdoorRated),
  )

  val corrugated: Material = Material(
    id = corrugatedId,
    name = LocalizedString("Corrugated Cardboard E-Flute", "Vlnitá lepenka E-vlna"),
    family = MaterialFamily.Cardboard,
    surfaceCoating = SurfaceCoating.Uncoated,
    weight = MaterialWeight.Thickness(PositiveMm.unsafe(BigDecimal("1.5")), Some(PaperWeight.unsafe(350))),
    stockFormats = NonEmptyChunk(sra3Sheet, b2Sheet),
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable),
  )

  val coatedSilk250gsm: Material = Material(
    id = coatedSilk250gsmId,
    name = LocalizedString("Coated Silk 250gsm", "Křídový hedvábný papír 250g"),
    family = MaterialFamily.Paper,
    surfaceCoating = SurfaceCoating.CoatedSilk,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(250)),
    stockFormats = NonEmptyChunk.single(sra3Sheet),
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.Recyclable),
  )

  val yupo: Material = Material(
    id = yupoId,
    name = LocalizedString("Synthetic Paper (Yupo) 200gsm", "Syntetický papír (Yupo) 200g"),
    family = MaterialFamily.Synthetic,
    surfaceCoating = SurfaceCoating.CoatedMatte,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(200)),
    stockFormats = NonEmptyChunk.single(sra3Sheet),
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.TearResistant, MaterialProperty.SmoothSurface),
  )

  val adhesiveStock: Material = Material(
    id = adhesiveStockId,
    name = LocalizedString("Adhesive Paper Stock 100gsm", "Samolepicí papír 100g"),
    family = MaterialFamily.Adhesive,
    surfaceCoating = SurfaceCoating.CoatedGloss,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(100)),
    stockFormats = NonEmptyChunk.single(sra3Sheet),
    properties = Set(MaterialProperty.SelfAdhesive, MaterialProperty.SmoothSurface),
  )

  val clearVinyl: Material = Material(
    id = clearVinylId,
    name = LocalizedString("Clear Vinyl", "Čirý vinyl"),
    family = MaterialFamily.Vinyl,
    surfaceCoating = SurfaceCoating.TransparentFilm,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(140)),
    stockFormats = NonEmptyChunk.single(roll1600),
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.SelfAdhesive, MaterialProperty.Transparent, MaterialProperty.OutdoorRated),
  )

  val cotton: Material = Material(
    id = cottonId,
    name = LocalizedString("Cotton Paper 300gsm", "Bavlněný papír 300g"),
    family = MaterialFamily.Paper,
    surfaceCoating = SurfaceCoating.NaturalTextured,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(300)),
    stockFormats = NonEmptyChunk.single(sra3Sheet),
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable, MaterialProperty.Writable),
  )

  val coatedGlossy90gsm: Material  = glossyPaper(coatedGlossy90gsmId, 90)
  val coatedGlossy115gsm: Material = glossyPaper(coatedGlossy115gsmId, 115)
  val coatedGlossy130gsm: Material = glossyPaper(coatedGlossy130gsmId, 130)
  val coatedGlossy150gsm: Material = glossyPaper(coatedGlossy150gsmId, 150)
  val coatedGlossy170gsm: Material = glossyPaper(coatedGlossy170gsmId, 170)
  val coatedGlossy200gsm: Material = glossyPaper(coatedGlossy200gsmId, 200)
  val coatedGlossy250gsm: Material = glossyPaper(coatedGlossy250gsmId, 250)
  val coatedGlossy350gsm: Material = glossyPaper(coatedGlossy350gsmId, 350)

  val coatedMatte90gsm: Material  = mattePaper(coatedMatte90gsmId, 90)
  val coatedMatte115gsm: Material = mattePaper(coatedMatte115gsmId, 115)
  val coatedMatte130gsm: Material = mattePaper(coatedMatte130gsmId, 130)
  val coatedMatte150gsm: Material = mattePaper(coatedMatte150gsmId, 150)
  val coatedMatte170gsm: Material = mattePaper(coatedMatte170gsmId, 170)
  val coatedMatte200gsm: Material = mattePaper(coatedMatte200gsmId, 200)
  val coatedMatte250gsm: Material = mattePaper(coatedMatte250gsmId, 250)
  val coatedMatte300gsm: Material = mattePaper(coatedMatte300gsmId, 300)
  val coatedMatte350gsm: Material = mattePaper(coatedMatte350gsmId, 350)

  val rollUpBannerFilm: Material = Material(
    id = rollUpBannerFilmId,
    name = LocalizedString("Roll-Up PVC Banner Film 510g", "Roll-Up PVC bannerová fólie 510g"),
    family = MaterialFamily.Vinyl,
    surfaceCoating = SurfaceCoating.CoatedMatte,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(510)),
    stockFormats = NonEmptyChunk.single(roll1600),
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.TearResistant, MaterialProperty.SmoothSurface),
  )

  val rollUpStandEconomy: Material = Material(
    id = rollUpStandEconomyId,
    name = LocalizedString("Economy Aluminum Roll-Up Stand", "Ekonomický hliníkový Roll-Up stojan"),
    family = MaterialFamily.Hardware,
    surfaceCoating = SurfaceCoating.Metallic,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(2000)),
    stockFormats = discreteUnsized,
    properties = Set(MaterialProperty.Recyclable),
  )

  val rollUpStandPremium: Material = Material(
    id = rollUpStandPremiumId,
    name = LocalizedString("Premium Weighted Aluminum Roll-Up Stand", "Prémiový zátěžový hliníkový Roll-Up stojan"),
    family = MaterialFamily.Hardware,
    surfaceCoating = SurfaceCoating.Metallic,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(3800)),
    stockFormats = discreteUnsized,
    properties = Set(MaterialProperty.Recyclable),
  )

  // Promotional — T-Shirts
  val cottonTshirt150: Material = Material(
    id = cottonTshirt150Id,
    name = LocalizedString("Cotton T-Shirt 150gsm", "Bavlněné tričko 150g"),
    family = MaterialFamily.Fabric,
    surfaceCoating = SurfaceCoating.Uncoated,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(155)),
    stockFormats = discrete(300, 400),
    properties = Set(MaterialProperty.Recyclable),
  )

  val cottonTshirt180: Material = Material(
    id = cottonTshirt180Id,
    name = LocalizedString("Heavy Cotton T-Shirt 180gsm", "Těžké bavlněné tričko 180g"),
    family = MaterialFamily.Fabric,
    surfaceCoating = SurfaceCoating.Uncoated,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(190)),
    stockFormats = discrete(300, 400),
    properties = Set(MaterialProperty.Recyclable),
  )

  val polyesterTshirt: Material = Material(
    id = polyesterTshirtId,
    name = LocalizedString("Performance Polyester T-Shirt", "Funkční polyesterové tričko"),
    family = MaterialFamily.Fabric,
    surfaceCoating = SurfaceCoating.Uncoated,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(140)),
    stockFormats = discrete(300, 400),
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.TearResistant),
  )

  val cottonPolyBlend: Material = Material(
    id = cottonPolyBlendId,
    name = LocalizedString("Cotton-Poly Blend T-Shirt 165gsm", "Směsové tričko bavlna/poly 165g"),
    family = MaterialFamily.Fabric,
    surfaceCoating = SurfaceCoating.Uncoated,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(170)),
    stockFormats = discrete(300, 400),
    properties = Set(MaterialProperty.TearResistant),
  )

  val organicCottonTshirt: Material = Material(
    id = organicCottonTshirtId,
    name = LocalizedString("Organic Cotton T-Shirt 180gsm", "Bio bavlněné tričko 180g"),
    family = MaterialFamily.Fabric,
    surfaceCoating = SurfaceCoating.Uncoated,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(190)),
    stockFormats = discrete(300, 400),
    properties = Set(MaterialProperty.Recyclable),
  )

  // Promotional — Eco Bags
  val cottonCanvasBag: Material = Material(
    id = cottonCanvasBagId,
    name = LocalizedString("Cotton Canvas Tote Bag 280gsm", "Bavlněná plátěná taška 280g"),
    family = MaterialFamily.Fabric,
    surfaceCoating = SurfaceCoating.NaturalTextured,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(140)),
    stockFormats = discrete(380, 420),
    properties = Set(MaterialProperty.Recyclable, MaterialProperty.TearResistant),
  )

  val organicCottonBag: Material = Material(
    id = organicCottonBagId,
    name = LocalizedString("Organic Cotton Tote Bag 220gsm", "Bio bavlněná taška 220g"),
    family = MaterialFamily.Fabric,
    surfaceCoating = SurfaceCoating.NaturalTextured,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(115)),
    stockFormats = discrete(380, 420),
    properties = Set(MaterialProperty.Recyclable),
  )

  val recycledPetBag: Material = Material(
    id = recycledPetBagId,
    name = LocalizedString("Recycled rPET Tote Bag", "Taška z recyklovaného rPET"),
    family = MaterialFamily.Synthetic,
    surfaceCoating = SurfaceCoating.CoatedMatte,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(95)),
    stockFormats = discrete(380, 420),
    properties = Set(MaterialProperty.Recyclable, MaterialProperty.WaterResistant),
  )

  val juteBag: Material = Material(
    id = juteBagId,
    name = LocalizedString("Natural Jute Tote Bag", "Přírodní jutová taška"),
    family = MaterialFamily.Fabric,
    surfaceCoating = SurfaceCoating.NaturalTextured,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(210)),
    stockFormats = discrete(380, 420),
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable, MaterialProperty.TearResistant),
  )

  val nonWovenPpBag: Material = Material(
    id = nonWovenPpBagId,
    name = LocalizedString("Non-Woven Polypropylene Bag", "Netkaná polypropylenová taška"),
    family = MaterialFamily.Synthetic,
    surfaceCoating = SurfaceCoating.Uncoated,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(65)),
    stockFormats = discrete(380, 420),
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.Recyclable),
  )

  // Promotional — Pin Badges
  val tinplateBadge: Material = Material(
    id = tinplateBadgeId,
    name = LocalizedString("Tinplate Metal Badge", "Plechová placka"),
    family = MaterialFamily.Metal,
    surfaceCoating = SurfaceCoating.Metallic,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(12)),
    stockFormats = discrete(75, 75),
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.Recyclable),
  )

  val acrylicBadge: Material = Material(
    id = acrylicBadgeId,
    name = LocalizedString("Clear Acrylic Badge", "Akrylová placka"),
    family = MaterialFamily.Synthetic,
    surfaceCoating = SurfaceCoating.TransparentFilm,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(9)),
    stockFormats = discrete(75, 75),
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.WaterResistant),
  )

  val woodenBadge: Material = Material(
    id = woodenBadgeId,
    name = LocalizedString("Natural Wood Badge", "Dřevěná placka"),
    family = MaterialFamily.Wood,
    surfaceCoating = SurfaceCoating.NaturalTextured,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(10)),
    stockFormats = discrete(75, 75),
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable),
  )

  // Promotional — Cups & Mugs
  val ceramicMugWhite: Material = Material(
    id = ceramicMugWhiteId,
    name = LocalizedString("White Ceramic Mug 330ml", "Bílý keramický hrnek 330 ml"),
    family = MaterialFamily.Ceramic,
    surfaceCoating = SurfaceCoating.Glazed,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(320)),
    stockFormats = discrete(200, 90),
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.FoodSafe),
  )

  val ceramicMugColored: Material = Material(
    id = ceramicMugColoredId,
    name = LocalizedString("Colored Rim Ceramic Mug 330ml", "Barevný keramický hrnek 330 ml"),
    family = MaterialFamily.Ceramic,
    surfaceCoating = SurfaceCoating.Glazed,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(330)),
    stockFormats = discrete(200, 90),
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.FoodSafe),
  )

  val magicMug: Material = Material(
    id = magicMugId,
    name = LocalizedString("Color-Changing Magic Mug 330ml", "Magický termohrnek 330 ml"),
    family = MaterialFamily.Ceramic,
    surfaceCoating = SurfaceCoating.Glazed,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(340)),
    stockFormats = discrete(200, 90),
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.FoodSafe),
  )

  val stainlessTravelMug: Material = Material(
    id = stainlessTravelMugId,
    name = LocalizedString("Stainless Steel Travel Mug 450ml", "Nerezový cestovní hrnek 450 ml"),
    family = MaterialFamily.Metal,
    surfaceCoating = SurfaceCoating.Metallic,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(290)),
    stockFormats = discrete(200, 120),
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.FoodSafe, MaterialProperty.Recyclable),
  )

  val enamelMug: Material = Material(
    id = enamelMugId,
    name = LocalizedString("Enamel Camp Mug 350ml", "Smaltovaný plecháček 350 ml"),
    family = MaterialFamily.Metal,
    surfaceCoating = SurfaceCoating.Glazed,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(170)),
    stockFormats = discrete(200, 80),
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.FoodSafe, MaterialProperty.OutdoorRated),
  )

  val glassMug: Material = Material(
    id = glassMugId,
    name = LocalizedString("Frosted Glass Mug 330ml", "Matný skleněný hrnek 330 ml"),
    family = MaterialFamily.Ceramic,
    surfaceCoating = SurfaceCoating.TransparentFilm,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(350)),
    stockFormats = discrete(200, 90),
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.Transparent, MaterialProperty.FoodSafe),
  )

  val allMaterials: List[Material] = List(
    coated300gsm, uncoatedBond, kraft, vinyl, pvc510g, corrugated, coatedSilk250gsm,
    yupo, adhesiveStock, clearVinyl, cotton,
    coatedGlossy90gsm, coatedGlossy115gsm, coatedGlossy130gsm, coatedGlossy150gsm,
    coatedGlossy170gsm, coatedGlossy200gsm, coatedGlossy250gsm, coatedGlossy350gsm,
    coatedMatte90gsm, coatedMatte115gsm, coatedMatte130gsm, coatedMatte150gsm,
    coatedMatte170gsm, coatedMatte200gsm, coatedMatte250gsm, coatedMatte300gsm, coatedMatte350gsm,
    rollUpBannerFilm, rollUpStandEconomy, rollUpStandPremium,
    cottonTshirt150, cottonTshirt180, polyesterTshirt, cottonPolyBlend, organicCottonTshirt,
    cottonCanvasBag, organicCottonBag, recycledPetBag, juteBag, nonWovenPpBag,
    tinplateBadge, acrylicBadge, woodenBadge,
    ceramicMugWhite, ceramicMugColored, magicMug, stainlessTravelMug, enamelMug, glassMug,
  )

  // Convenience material groups
  val allCoatedPapers: Set[MaterialId] = Set(
    coatedGlossy90gsmId, coatedGlossy115gsmId, coatedGlossy130gsmId, coatedGlossy150gsmId,
    coatedGlossy170gsmId, coatedGlossy200gsmId, coatedGlossy250gsmId, coated300gsmId, coatedGlossy350gsmId,
    coatedMatte90gsmId, coatedMatte115gsmId, coatedMatte130gsmId, coatedMatte150gsmId,
    coatedMatte170gsmId, coatedMatte200gsmId, coatedMatte250gsmId, coatedMatte300gsmId, coatedMatte350gsmId,
    coatedSilk250gsmId,
  )

  val heavyCardStocks: Set[MaterialId] = Set(
    coatedGlossy250gsmId, coated300gsmId, coatedGlossy350gsmId,
    coatedMatte250gsmId, coatedMatte300gsmId, coatedMatte350gsmId,
    coatedSilk250gsmId, kraftId, cottonId, yupoId,
  )

  val bookletCoverStocks: Set[MaterialId] = Set(
    coatedGlossy170gsmId, coatedGlossy200gsmId, coatedGlossy250gsmId, coated300gsmId, coatedGlossy350gsmId,
    coatedMatte170gsmId, coatedMatte200gsmId, coatedMatte250gsmId, coatedMatte300gsmId, coatedMatte350gsmId,
    coatedSilk250gsmId, kraftId,
  )

  val bookletBodyStocks: Set[MaterialId] = Set(
    coatedGlossy90gsmId, coatedGlossy115gsmId, coatedGlossy130gsmId, coatedGlossy150gsmId, coatedGlossy170gsmId,
    coatedMatte90gsmId, coatedMatte115gsmId, coatedMatte130gsmId, coatedMatte150gsmId, coatedMatte170gsmId,
    uncoatedBondId,
  )

  val tshirtMaterials: Set[MaterialId] =
    Set(cottonTshirt150Id, cottonTshirt180Id, polyesterTshirtId, cottonPolyBlendId, organicCottonTshirtId)

  val ecoBagMaterials: Set[MaterialId] =
    Set(cottonCanvasBagId, organicCottonBagId, recycledPetBagId, juteBagId, nonWovenPpBagId)

  val pinBadgeMaterials: Set[MaterialId] =
    Set(tinplateBadgeId, acrylicBadgeId, woodenBadgeId)

  val cupMaterials: Set[MaterialId] =
    Set(ceramicMugWhiteId, ceramicMugColoredId, magicMugId, stainlessTravelMugId, enamelMugId, glassMugId)

  // ── Printing Methods (9) ───────────────────────────────────────────────────

  val offset: PrintingMethod = PrintingMethod(
    id = offsetId,
    name = LocalizedString("Offset Lithography", "Ofsetový tisk"),
    processType = PrintingProcessType.Offset,
    maxColorsPerSide = Some(6),
    supportsDoubleSided = true,
    supportedSpecialInks = Set(SpecialInk.MetallicGold, SpecialInk.MetallicSilver),
  )

  val digital: PrintingMethod = PrintingMethod(
    id = digitalId,
    name = LocalizedString("Digital Press (CMYK)", "Digitální tisk (CMYK)"),
    processType = PrintingProcessType.Digital,
    maxColorsPerSide = Some(5),
    supportsDoubleSided = true,
    supportedSpecialInks = Set(SpecialInk.White, SpecialInk.ClearSpotVarnish),
  )

  val uvInkjet: PrintingMethod = PrintingMethod(
    id = uvInkjetId,
    name = LocalizedString("UV Curable Inkjet", "UV inkoustový tisk"),
    processType = PrintingProcessType.UVCurableInkjet,
    maxColorsPerSide = Some(6),
    supportsDoubleSided = false,
    supportedSpecialInks = Set(SpecialInk.White, SpecialInk.ClearSpotVarnish),
  )

  val letterpress: PrintingMethod = PrintingMethod(
    id = letterpressId,
    name = LocalizedString("Artisan Letterpress", "Knihtisk"),
    processType = PrintingProcessType.Letterpress,
    maxColorsPerSide = Some(2),
    supportsDoubleSided = true,
    supportedSpecialInks = Set(SpecialInk.MetallicGold, SpecialInk.MetallicSilver),
  )

  val solventInkjet: PrintingMethod = PrintingMethod(
    id = solventInkjetId,
    name = LocalizedString("Eco-Solvent Large Format", "Solventní velkoformátový tisk"),
    processType = PrintingProcessType.SolventInkjet,
    maxColorsPerSide = Some(4),
    supportsDoubleSided = false,
  )

  val epson8Color: PrintingMethod = PrintingMethod(
    id = epson8ColorId,
    name = LocalizedString("Epson 8-Color Photo Latex", "Epson 8-barevný foto tisk"),
    processType = PrintingProcessType.LatexInkjet,
    maxColorsPerSide = Some(8),
    supportsDoubleSided = false,
    supportedSpecialInks = Set(SpecialInk.White),
  )

  val screenPrint: PrintingMethod = PrintingMethod(
    id = screenPrintId,
    name = LocalizedString("Screen Printing", "Sítotisk"),
    processType = PrintingProcessType.ScreenPrint,
    maxColorsPerSide = Some(8),
    supportsDoubleSided = true,
    supportedSpecialInks = Set(SpecialInk.White, SpecialInk.MetallicGold, SpecialInk.MetallicSilver),
  )

  val dtg: PrintingMethod = PrintingMethod(
    id = dtgId,
    name = LocalizedString("Direct-to-Garment (DTG)", "Přímý tisk na textil (DTG)"),
    processType = PrintingProcessType.DTG,
    maxColorsPerSide = Some(5),
    supportsDoubleSided = true,
    supportedSpecialInks = Set(SpecialInk.White),
  )

  val sublimation: PrintingMethod = PrintingMethod(
    id = sublimationId,
    name = LocalizedString("Dye Sublimation", "Sublimační tisk"),
    processType = PrintingProcessType.Sublimation,
    maxColorsPerSide = Some(4),
    supportsDoubleSided = false,
  )

  val allPrintingMethods: List[PrintingMethod] = List(
    offset, digital, uvInkjet, letterpress, solventInkjet, epson8Color, screenPrint, dtg, sublimation,
  )

  // ── Finishes (29) ──────────────────────────────────────────────────────────

  private val frontOrBoth = FinishSidePolicy.Selectable(Set(FinishSide.Front, FinishSide.Both), FinishSide.Front)
  private val frontOnly   = FinishSidePolicy.Fixed(FinishSide.Front)

  val matteLamination: Finish = Finish(
    id = matteLaminationId,
    name = LocalizedString("Matte Lamination", "Matná laminace"),
    finishType = FinishType.Lamination,
    sidePolicy = frontOrBoth,
  )

  val glossLamination: Finish = Finish(
    id = glossLaminationId,
    name = LocalizedString("Gloss Lamination", "Lesklá laminace"),
    finishType = FinishType.Lamination,
    sidePolicy = frontOrBoth,
  )

  val uvCoating: Finish = Finish(
    id = uvCoatingId,
    name = LocalizedString("UV Coating", "UV lak"),
    finishType = FinishType.UVCoating,
    sidePolicy = frontOrBoth,
  )

  val embossing: Finish = Finish(
    id = embossingId,
    name = LocalizedString("Blind Embossing", "Slepotisk (reliéfní ražba)"),
    finishType = FinishType.Embossing,
    sidePolicy = frontOnly,
  )

  val foilStamping: Finish = Finish(
    id = foilStampingId,
    name = LocalizedString("Hot Foil Stamping", "Ražba fólií"),
    finishType = FinishType.FoilStamping,
    sidePolicy = frontOnly,
    paramSchema = FinishParamSchema.FoilColorSchema(FoilColor.values.toSet),
  )

  val dieCut: Finish = Finish(
    id = dieCutId,
    name = LocalizedString("Custom Die Cut", "Tvarový výsek"),
    finishType = FinishType.DieCut,
    sidePolicy = FinishSidePolicy.NotApplicable,
  )

  val varnish: Finish = Finish(
    id = varnishId,
    name = LocalizedString("Spot Varnish", "Parciální lak"),
    finishType = FinishType.Varnish,
    sidePolicy = frontOrBoth,
  )

  val softTouchCoating: Finish = Finish(
    id = softTouchCoatingId,
    name = LocalizedString("Soft-Touch Lamination", "Soft-Touch laminace"),
    finishType = FinishType.SoftTouchCoating,
    sidePolicy = frontOrBoth,
  )

  val aqueousCoating: Finish = Finish(
    id = aqueousCoatingId,
    name = LocalizedString("Aqueous Dispersion Coating", "Disperzní lak"),
    finishType = FinishType.AqueousCoating,
    sidePolicy = frontOrBoth,
  )

  val debossing: Finish = Finish(
    id = debossingId,
    name = LocalizedString("Debossing", "Hloubková ražba"),
    finishType = FinishType.Debossing,
    sidePolicy = frontOnly,
  )

  val scoring: Finish = Finish(
    id = scoringId,
    name = LocalizedString("Scoring / Creasing", "Bigování (rylování)"),
    finishType = FinishType.Scoring,
    sidePolicy = FinishSidePolicy.NotApplicable,
    paramSchema = FinishParamSchema.CreaseCountSchema(1, 8),
  )

  val perforation: Finish = Finish(
    id = perforationId,
    name = LocalizedString("Perforation", "Perforace"),
    finishType = FinishType.Perforation,
    sidePolicy = FinishSidePolicy.NotApplicable,
    paramSchema = FinishParamSchema.PerforationSchema(1, 100),
  )

  val roundCorners: Finish = Finish(
    id = roundCornersId,
    name = LocalizedString("Rounded Corners", "Zaoblené rohy"),
    finishType = FinishType.RoundCorners,
    sidePolicy = FinishSidePolicy.NotApplicable,
    paramSchema = FinishParamSchema.RoundCornersSchema(Set(1, 2, 3, 4), 1, 20),
  )

  val grommets: Finish = Finish(
    id = grommetsId,
    name = LocalizedString("Metal Grommets / Eyelets", "Kovová oka"),
    finishType = FinishType.Grommets,
    sidePolicy = FinishSidePolicy.NotApplicable,
    paramSchema = FinishParamSchema.GrommetSpacingSchema(Set(250, 300, 500, 1000)),
  )

  val kissCut: Finish = Finish(
    id = kissCutId,
    name = LocalizedString("Kiss-Cut Contour", "Násek (Kiss-Cut)"),
    finishType = FinishType.KissCut,
    sidePolicy = FinishSidePolicy.NotApplicable,
  )

  val overlamination: Finish = Finish(
    id = overlaminationId,
    name = LocalizedString("Protective UV Overlamination", "Ochranná UV laminace"),
    finishType = FinishType.Overlamination,
    sidePolicy = frontOnly,
  )

  val gumRope: Finish = Finish(
    id = gumRopeId,
    name = LocalizedString("Elastic Bungee Rope", "Gumolano"),
    finishType = FinishType.RopeAccessory,
    sidePolicy = FinishSidePolicy.NotApplicable,
    paramSchema = FinishParamSchema.LinearLengthSchema(BigDecimal("0.5"), BigDecimal("50")),
  )

  // Promotional finishes
  val heatPress: Finish = Finish(
    id = heatPressId,
    name = LocalizedString("Heat-Press Curing", "Tepelná fixace"),
    finishType = FinishType.HeatTransfer,
    sidePolicy = FinishSidePolicy.NotApplicable,
  )

  val labelPrint: Finish = Finish(
    id = labelPrintId,
    name = LocalizedString("Custom Neck Label Print", "Potisk vnitřního štítku"),
    finishType = FinishType.HeatTransfer,
    sidePolicy = FinishSidePolicy.NotApplicable,
  )

  val foldBag: Finish = Finish(
    id = foldBagId,
    name = LocalizedString("Individual Polybag Folding", "Jednotlivé skládání a balení"),
    finishType = FinishType.ProtectivePackaging,
    sidePolicy = FinishSidePolicy.NotApplicable,
  )

  val mylarOverlay: Finish = Finish(
    id = mylarOverlayId,
    name = LocalizedString("Protective Mylar Film", "Ochranná mylarová fólie"),
    finishType = FinishType.Overlamination,
    sidePolicy = frontOnly,
  )

  val safetyPin: Finish = Finish(
    id = safetyPinId,
    name = LocalizedString("Safety Pin Back", "Zavírací špendlík"),
    finishType = FinishType.HardwareAttachment,
    sidePolicy = FinishSidePolicy.NotApplicable,
  )

  val magnetBack: Finish = Finish(
    id = magnetBackId,
    name = LocalizedString("Magnetic Back", "Magnetická zadní strana"),
    finishType = FinishType.HardwareAttachment,
    sidePolicy = FinishSidePolicy.NotApplicable,
  )

  val bottleOpener: Finish = Finish(
    id = bottleOpenerId,
    name = LocalizedString("Bottle Opener Back", "Otvírák na zadní straně"),
    finishType = FinishType.HardwareAttachment,
    sidePolicy = FinishSidePolicy.NotApplicable,
  )

  val dishwasherCoat: Finish = Finish(
    id = dishwasherCoatId,
    name = LocalizedString("Dishwasher-Safe Polymer Coat", "Ochranná vrstva do myčky"),
    finishType = FinishType.UVCoating,
    sidePolicy = FinishSidePolicy.NotApplicable,
  )

  val giftBox: Finish = Finish(
    id = giftBoxId,
    name = LocalizedString("Individual Gift Box", "Dárková krabička"),
    finishType = FinishType.ProtectivePackaging,
    sidePolicy = FinishSidePolicy.NotApplicable,
  )

  val glossyGlaze: Finish = Finish(
    id = glossyGlazeId,
    name = LocalizedString("High-Gloss Glaze Finish", "Vysoce lesklá glazura"),
    finishType = FinishType.Varnish,
    sidePolicy = FinishSidePolicy.NotApplicable,
  )

  val embroidery: Finish = Finish(
    id = embroideryId,
    name = LocalizedString("Custom Embroidery", "Strojová výšivka"),
    finishType = FinishType.Embroidery,
    sidePolicy = frontOnly,
  )

  val reinforcedHandles: Finish = Finish(
    id = reinforcedHandlesId,
    name = LocalizedString("Cross-Stitched Reinforced Handles", "Zesílená křížově šitá ucha"),
    finishType = FinishType.HardwareAttachment,
    sidePolicy = FinishSidePolicy.NotApplicable,
  )

  val allFinishes: List[Finish] = List(
    matteLamination, glossLamination, uvCoating, embossing, foilStamping, dieCut, varnish,
    softTouchCoating, aqueousCoating, debossing, scoring, perforation, roundCorners, grommets,
    kissCut, overlamination, gumRope,
    heatPress, labelPrint, foldBag, mylarOverlay, safetyPin, magnetBack, bottleOpener,
    dishwasherCoat, giftBox, glossyGlaze, embroidery, reinforcedHandles,
  )

  // ── Product Categories (15) with Presets ───────────────────────────────────

  val businessCards: ProductCategory = ProductCategory(
    id = businessCardsId,
    name = LocalizedString("Business Cards", "Vizitky"),
    components = NonEmptyChunk.single(
      ComponentTemplate(
        key = ComponentKey.Main,
        label = LocalizedString("Card Stock", "Karton"),
        allowedMaterialIds = heavyCardStocks,
        allowedFinishIds = Set(
          matteLaminationId, glossLaminationId, softTouchCoatingId, uvCoatingId,
          varnishId, embossingId, debossingId, foilStampingId, roundCornersId, dieCutId,
        ),
      )
    ),
    allowedPrintingMethodIds = Set(digitalId, offsetId, letterpressId),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-bc-basic"),
        name = LocalizedString("Basic Business Cards", "Základní vizitky"),
        components = NonEmptyChunk.single(
          ComponentPreset(ComponentKey.Main, coated300gsmId, Some(digitalId), Some(InkConfiguration.cmyk4_0))
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.unsafeMm(85, 55)),
          quantity = Quantity.unsafe(100),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-bc-premium"),
        name = LocalizedString("Premium Laminated Cards", "Prémiové laminované vizitky"),
        components = NonEmptyChunk.single(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = coatedMatte350gsmId,
            printingMethodId = Some(digitalId),
            inkConfiguration = Some(InkConfiguration.cmyk4_4),
            finishes = List(
              FinishSelection(matteLaminationId, Some(FinishSide.Both)),
              FinishSelection(roundCornersId, params = Some(FinishParams.RoundCorners(4, 5))),
            ),
          )
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.unsafeMm(85, 55)),
          quantity = Quantity.unsafe(250),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-bc-letterpress"),
        name = LocalizedString("Artisan Cotton Letterpress", "Řemeslný knihtisk na bavlně"),
        components = NonEmptyChunk.single(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = cottonId,
            printingMethodId = Some(letterpressId),
            inkConfiguration = Some(InkConfiguration(SideInk.pms(2), SideInk.blank)),
          )
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.unsafeMm(90, 50)),
          quantity = Quantity.unsafe(100),
        ),
      ),
    ),
  )

  val flyers: ProductCategory = ProductCategory(
    id = flyersId,
    name = LocalizedString("Flyers", "Letáky"),
    components = NonEmptyChunk.single(
      ComponentTemplate(
        key = ComponentKey.Main,
        label = LocalizedString("Sheet", "List"),
        allowedMaterialIds = allCoatedPapers + uncoatedBondId + kraftId,
        allowedFinishIds = Set(
          matteLaminationId, glossLaminationId, uvCoatingId, aqueousCoatingId,
          varnishId, scoringId, perforationId, roundCornersId,
        ),
      )
    ),
    specRequirements = SpecRequirements(requiresOrientation = true),
    allowedPrintingMethodIds = Set(digitalId, offsetId),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-flyers-standard"),
        name = LocalizedString("Standard A5 Flyers", "Standardní letáky A5"),
        components = NonEmptyChunk.single(
          ComponentPreset(ComponentKey.Main, coatedGlossy130gsmId, Some(digitalId), Some(InkConfiguration.cmyk4_0))
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.A5),
          quantity = Quantity.unsafe(500),
          orientation = Some(Orientation.Portrait),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-flyers-premium"),
        name = LocalizedString("Premium A4 Flyers", "Prémiové letáky A4"),
        components = NonEmptyChunk.single(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = coatedMatte250gsmId,
            printingMethodId = Some(offsetId),
            inkConfiguration = Some(InkConfiguration.cmyk4_4),
            finishes = List(FinishSelection(matteLaminationId, Some(FinishSide.Both))),
          )
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.A4),
          quantity = Quantity.unsafe(500),
          orientation = Some(Orientation.Portrait),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-flyers-lightweight"),
        name = LocalizedString("Mass Distribution 90gsm", "Hromadná distribuce 90g"),
        components = NonEmptyChunk.single(
          ComponentPreset(ComponentKey.Main, coatedGlossy90gsmId, Some(offsetId), Some(InkConfiguration.cmyk4_4))
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.A5),
          quantity = Quantity.unsafe(1000),
          orientation = Some(Orientation.Portrait),
        ),
      ),
    ),
  )

  val brochures: ProductCategory = ProductCategory(
    id = brochuresId,
    name = LocalizedString("Folded Brochures", "Skládané brožury"),
    components = NonEmptyChunk.single(
      ComponentTemplate(
        key = ComponentKey.Main,
        label = LocalizedString("Folded Sheet", "Skládaný arch"),
        allowedMaterialIds = allCoatedPapers + uncoatedBondId + kraftId,
        allowedFinishIds = Set(
          matteLaminationId, glossLaminationId, uvCoatingId, aqueousCoatingId,
          varnishId, scoringId, perforationId,
        ),
      )
    ),
    specRequirements = SpecRequirements(requiresFoldedDimensions = true),
    allowedPrintingMethodIds = Set(digitalId, offsetId),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-brochure-trifold-dl"),
        name = LocalizedString("Tri-Fold DL Brochure (A4 flat)", "Trojskládaná DL brožura (A4 rozložená)"),
        components = NonEmptyChunk.single(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = coatedGlossy150gsmId,
            printingMethodId = Some(digitalId),
            inkConfiguration = Some(InkConfiguration.cmyk4_4),
          )
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Folded2D(Dimension2D.DL, FoldType.Tri),
          quantity = Quantity.unsafe(250),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-brochure-halffold-a5"),
        name = LocalizedString("Half-Fold A5 Heavy Scored", "Půlená A5 s bigováním"),
        components = NonEmptyChunk.single(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = coatedMatte200gsmId,
            printingMethodId = Some(digitalId),
            inkConfiguration = Some(InkConfiguration.cmyk4_4),
            finishes = List(
              FinishSelection(matteLaminationId, Some(FinishSide.Both)),
              FinishSelection(scoringId, params = Some(FinishParams.Scoring(1))),
            ),
          )
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Folded2D(Dimension2D.A5, FoldType.Half),
          quantity = Quantity.unsafe(250),
        ),
      ),
    ),
  )

  val banners: ProductCategory = ProductCategory(
    id = bannersId,
    name = LocalizedString("Banners & Wide Format", "Bannery a velkoformát"),
    components = NonEmptyChunk.single(
      ComponentTemplate(
        key = ComponentKey.Main,
        label = LocalizedString("Banner Media", "Bannerový materiál"),
        allowedMaterialIds = Set(pvc510gId, vinylId),
        allowedFinishIds = Set(grommetsId, overlaminationId, gumRopeId),
      )
    ),
    allowedPrintingMethodIds = Set(solventInkjetId, uvInkjetId, epson8ColorId),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-banner-pvc-2x1"),
        name = LocalizedString("Outdoor PVC Banner 2×1m with Grommets", "Venkovní PVC banner 2×1m s oky"),
        components = NonEmptyChunk.single(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = pvc510gId,
            printingMethodId = Some(solventInkjetId),
            inkConfiguration = Some(InkConfiguration.cmyk4_0),
            finishes = List(
              FinishSelection(grommetsId, params = Some(FinishParams.Grommets(500))),
              FinishSelection(gumRopeId, params = Some(FinishParams.LinearLength(BigDecimal(6)))),
            ),
          )
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.unsafeMm(2000, 1000)),
          quantity = Quantity.unsafe(1),
        ),
      ),
    ),
  )

  val packaging: ProductCategory = ProductCategory(
    id = packagingId,
    name = LocalizedString("Custom Packaging", "Obaly a krabičky"),
    components = NonEmptyChunk.single(
      ComponentTemplate(
        key = ComponentKey.Main,
        label = LocalizedString("Box Carton", "Karton"),
        allowedMaterialIds = Set(corrugatedId, kraftId, coated300gsmId, coatedMatte350gsmId, coatedGlossy350gsmId),
        allowedFinishIds = Set(
          dieCutId, matteLaminationId, glossLaminationId, uvCoatingId,
          embossingId, foilStampingId, scoringId,
        ),
      )
    ),
    allowedPrintingMethodIds = Set(offsetId, digitalId, uvInkjetId),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-packaging-kraft-box"),
        name = LocalizedString("Die-Cut Kraft Box 120×80×60mm", "Výseková kraftová krabička 120×80×60mm"),
        components = NonEmptyChunk.single(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = kraftId,
            printingMethodId = Some(digitalId),
            inkConfiguration = Some(InkConfiguration.cmyk4_0),
            finishes = List(FinishSelection(dieCutId)),
          )
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Box3D(
            Dimension3D(PositiveMm.unsafe(120), PositiveMm.unsafe(80), PositiveMm.unsafe(60))
          ),
          quantity = Quantity.unsafe(100),
        ),
      ),
    ),
  )

  val booklets: ProductCategory = ProductCategory(
    id = bookletsId,
    name = LocalizedString("Booklets & Catalogs", "Katalogy a brožury"),
    components = NonEmptyChunk(
      ComponentTemplate(
        key = ComponentKey.Cover,
        label = LocalizedString("Cover", "Obálka"),
        required = true,
        printable = true,
        allowedMaterialIds = bookletCoverStocks,
        allowedFinishIds = Set(
          matteLaminationId, glossLaminationId, softTouchCoatingId,
          uvCoatingId, varnishId, foilStampingId, embossingId,
        ),
      ),
      ComponentTemplate(
        key = ComponentKey.Body,
        label = LocalizedString("Interior Pages", "Vnitřní listy"),
        required = true,
        printable = true,
        allowedMaterialIds = bookletBodyStocks,
        allowedFinishIds = Set(aqueousCoatingId),
      ),
    ),
    specRequirements = SpecRequirements(requiresBinding = true),
    allowedPrintingMethodIds = Set(digitalId, offsetId),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-booklet-a4-saddle"),
        name = LocalizedString("A4 Saddle-Stitched Catalog 16pp", "Katalog A4 šitý V1, 16 stran"),
        components = NonEmptyChunk(
          ComponentPreset(
            key = ComponentKey.Cover,
            materialId = coated300gsmId,
            printingMethodId = Some(digitalId),
            inkConfiguration = Some(InkConfiguration.cmyk4_4),
            finishes = List(FinishSelection(matteLaminationId, Some(FinishSide.Front))),
          ),
          ComponentPreset(
            key = ComponentKey.Body,
            materialId = coatedGlossy130gsmId,
            printingMethodId = Some(digitalId),
            inkConfiguration = Some(InkConfiguration.cmyk4_4),
          ),
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.A4),
          quantity = Quantity.unsafe(100),
          binding = Some(BindingSpec(BindingMethod.SaddleStitch, totalPages = 16)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-booklet-a5-perfect"),
        name = LocalizedString("A5 Perfect-Bound Catalog 64pp", "Katalog A5 lepený V2, 64 stran"),
        components = NonEmptyChunk(
          ComponentPreset(
            key = ComponentKey.Cover,
            materialId = coatedMatte300gsmId,
            printingMethodId = Some(offsetId),
            inkConfiguration = Some(InkConfiguration.cmyk4_0),
            finishes = List(FinishSelection(matteLaminationId, Some(FinishSide.Front))),
          ),
          ComponentPreset(
            key = ComponentKey.Body,
            materialId = coatedMatte115gsmId,
            printingMethodId = Some(offsetId),
            inkConfiguration = Some(InkConfiguration.cmyk4_4),
          ),
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.A5),
          quantity = Quantity.unsafe(250),
          binding = Some(BindingSpec(BindingMethod.PerfectBinding, totalPages = 64)),
        ),
      ),
    ),
  )

  val calendars: ProductCategory = ProductCategory(
    id = calendarsId,
    name = LocalizedString("Wall & Desk Calendars", "Nástěnné a stolní kalendáře"),
    components = NonEmptyChunk(
      ComponentTemplate(
        key = ComponentKey.Cover,
        label = LocalizedString("Cover Sheet", "Titulní list"),
        required = true,
        printable = true,
        allowedMaterialIds = bookletCoverStocks,
        allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId),
      ),
      ComponentTemplate(
        key = ComponentKey.Body,
        label = LocalizedString("Monthly Pages", "Měsíční listy"),
        required = true,
        printable = true,
        allowedMaterialIds = bookletBodyStocks,
        allowedFinishIds = Set.empty,
      ),
    ),
    specRequirements = SpecRequirements(requiresBinding = true),
    allowedPrintingMethodIds = Set(digitalId, offsetId),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-calendar-a3-wall"),
        name = LocalizedString("A3 Wall Calendar (14pp Spiral)", "Nástěnný kalendář A3 (14 stran spirála)"),
        components = NonEmptyChunk(
          ComponentPreset(ComponentKey.Cover, coatedGlossy250gsmId, Some(digitalId), Some(InkConfiguration.cmyk4_0)),
          ComponentPreset(ComponentKey.Body, coatedGlossy150gsmId, Some(digitalId), Some(InkConfiguration.cmyk4_0)),
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.A3),
          quantity = Quantity.unsafe(50),
          binding = Some(BindingSpec(BindingMethod.WireOBinding, totalPages = 28)),
        ),
      ),
    ),
  )

  val postcards: ProductCategory = ProductCategory(
    id = postcardsId,
    name = LocalizedString("Postcards & Invitations", "Pohlednice a pozvánky"),
    components = NonEmptyChunk.single(
      ComponentTemplate(
        key = ComponentKey.Main,
        label = LocalizedString("Card Stock", "Karton"),
        allowedMaterialIds = heavyCardStocks + uncoatedBondId,
        allowedFinishIds = Set(
          matteLaminationId, glossLaminationId, uvCoatingId, softTouchCoatingId,
          embossingId, foilStampingId, roundCornersId,
        ),
      )
    ),
    allowedPrintingMethodIds = Set(digitalId, offsetId, letterpressId),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-postcard-a6"),
        name = LocalizedString("Standard A6 Postcards", "Standardní pohlednice A6"),
        components = NonEmptyChunk.single(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = coated300gsmId,
            printingMethodId = Some(digitalId),
            inkConfiguration = Some(InkConfiguration.cmyk4_1),
            finishes = List(FinishSelection(glossLaminationId, Some(FinishSide.Front))),
          )
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.A6),
          quantity = Quantity.unsafe(250),
        ),
      ),
    ),
  )

  val stickers: ProductCategory = ProductCategory(
    id = stickersId,
    name = LocalizedString("Stickers & Labels", "Samolepky a etikety"),
    components = NonEmptyChunk.single(
      ComponentTemplate(
        key = ComponentKey.Main,
        label = LocalizedString("Adhesive Stock", "Samolepicí materiál"),
        allowedMaterialIds = Set(adhesiveStockId, vinylId, clearVinylId),
        allowedFinishIds = Set(kissCutId, dieCutId, overlaminationId, matteLaminationId, glossLaminationId),
      )
    ),
    allowedPrintingMethodIds = Set(digitalId, uvInkjetId, solventInkjetId, epson8ColorId),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-sticker-paper-round"),
        name = LocalizedString("Paper Kiss-Cut Stickers 70mm", "Papírové násekové samolepky 70mm"),
        components = NonEmptyChunk.single(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = adhesiveStockId,
            printingMethodId = Some(digitalId),
            inkConfiguration = Some(InkConfiguration.cmyk4_0),
            finishes = List(FinishSelection(kissCutId)),
          )
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Circular(PositiveMm.unsafe(70)),
          quantity = Quantity.unsafe(250),
          deliveryFormat = DeliveryFormat.KissCutSheets,
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-sticker-clear-vinyl-white"),
        name = LocalizedString("Clear Vinyl with White Underbase 100×100mm", "Čirý vinyl s bílou podtiskovou 100×100mm"),
        components = NonEmptyChunk.single(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = clearVinylId,
            printingMethodId = Some(uvInkjetId),
            inkConfiguration = Some(InkConfiguration.cmyk4W_0),
            finishes = List(FinishSelection(kissCutId), FinishSelection(overlaminationId)),
          )
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.unsafeMm(100, 100)),
          quantity = Quantity.unsafe(100),
          deliveryFormat = DeliveryFormat.KissCutSheets,
        ),
      ),
    ),
  )

  val rollUps: ProductCategory = ProductCategory(
    id = rollUpsId,
    name = LocalizedString("Roll-Up Banners", "Roll-Up bannery"),
    components = NonEmptyChunk(
      ComponentTemplate(
        key = ComponentKey.Main,
        label = LocalizedString("Printed Banner Graphic", "Tištěný grafický pás"),
        required = true,
        printable = true,
        allowedMaterialIds = Set(rollUpBannerFilmId),
        allowedFinishIds = Set(overlaminationId),
      ),
      ComponentTemplate(
        key = ComponentKey.Stand,
        label = LocalizedString("Aluminum Retractable Stand", "Hliníkový stojan"),
        required = false,
        printable = false,
        allowedMaterialIds = Set(rollUpStandEconomyId, rollUpStandPremiumId),
        allowedFinishIds = Set.empty,
      ),
    ),
    allowedPrintingMethodIds = Set(solventInkjetId, uvInkjetId, epson8ColorId),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-rollup-economy-85x200"),
        name = LocalizedString("Economy Roll-Up 85×200 cm", "Ekonomický Roll-Up 85×200 cm"),
        components = NonEmptyChunk(
          ComponentPreset(ComponentKey.Main, rollUpBannerFilmId, Some(solventInkjetId), Some(InkConfiguration.cmyk4_0)),
          ComponentPreset(ComponentKey.Stand, rollUpStandEconomyId, None, None),
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.unsafeMm(850, 2000)),
          quantity = Quantity.unsafe(1),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-rollup-premium-100x200"),
        name = LocalizedString("Premium Roll-Up 100×200 cm", "Prémiový Roll-Up 100×200 cm"),
        components = NonEmptyChunk(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = rollUpBannerFilmId,
            printingMethodId = Some(epson8ColorId),
            inkConfiguration = Some(InkConfiguration.cmyk4_0),
            finishes = List(FinishSelection(overlaminationId)),
          ),
          ComponentPreset(ComponentKey.Stand, rollUpStandPremiumId, None, None),
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.unsafeMm(1000, 2000)),
          quantity = Quantity.unsafe(2),
        ),
      ),
    ),
  )

  val tshirts: ProductCategory = ProductCategory(
    id = tshirtsId,
    name = LocalizedString("Custom T-Shirts", "Trička s potiskem"),
    components = NonEmptyChunk.single(
      ComponentTemplate(
        key = ComponentKey.Main,
        label = LocalizedString("Garment", "Textil"),
        allowedMaterialIds = tshirtMaterials,
        allowedFinishIds = Set(heatPressId, labelPrintId, foldBagId, embroideryId),
      )
    ),
    allowedPrintingMethodIds = Set(screenPrintId, dtgId, sublimationId),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-tshirt-dtg-cotton"),
        name = LocalizedString("Heavy Cotton DTG T-Shirt", "Těžké bavlněné tričko DTG"),
        components = NonEmptyChunk.single(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = cottonTshirt180Id,
            printingMethodId = Some(dtgId),
            inkConfiguration = Some(InkConfiguration.cmyk4W_0),
            finishes = List(FinishSelection(heatPressId), FinishSelection(foldBagId)),
          )
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.PrintArea(Dimension2D.unsafeMm(280, 350), Some("L")),
          quantity = Quantity.unsafe(25),
          deliveryFormat = DeliveryFormat.PackedUnits,
        ),
      ),
    ),
  )

  val ecoBags: ProductCategory = ProductCategory(
    id = ecoBagsId,
    name = LocalizedString("Eco Tote Bags", "Ekologické plátěné tašky"),
    components = NonEmptyChunk.single(
      ComponentTemplate(
        key = ComponentKey.Main,
        label = LocalizedString("Tote Bag", "Taška"),
        allowedMaterialIds = ecoBagMaterials,
        allowedFinishIds = Set(heatPressId, foldBagId, embroideryId, reinforcedHandlesId),
      )
    ),
    allowedPrintingMethodIds = Set(screenPrintId, dtgId, sublimationId),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-ecobag-canvas-screen"),
        name = LocalizedString("Cotton Canvas Screen-Printed Bag", "Plátěná taška se sítotiskem"),
        components = NonEmptyChunk.single(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = cottonCanvasBagId,
            printingMethodId = Some(screenPrintId),
            inkConfiguration = Some(InkConfiguration.cmyk4_0),
            finishes = List(FinishSelection(reinforcedHandlesId)),
          )
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.PrintArea(Dimension2D.unsafeMm(260, 280), Some("38×42cm")),
          quantity = Quantity.unsafe(50),
          deliveryFormat = DeliveryFormat.PackedUnits,
        ),
      ),
    ),
  )

  val pinBadges: ProductCategory = ProductCategory(
    id = pinBadgesId,
    name = LocalizedString("Pin Badges & Buttons", "Placky a odznaky"),
    components = NonEmptyChunk.single(
      ComponentTemplate(
        key = ComponentKey.Main,
        label = LocalizedString("Badge Shell", "Tělo placky"),
        allowedMaterialIds = pinBadgeMaterials,
        allowedFinishIds = Set(mylarOverlayId, safetyPinId, magnetBackId, bottleOpenerId),
      )
    ),
    allowedPrintingMethodIds = Set(digitalId, uvInkjetId),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-badge-58mm-pin"),
        name = LocalizedString("Classic 58mm Safety-Pin Badge", "Klasická placka 58mm se špendlíkem"),
        components = NonEmptyChunk.single(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = tinplateBadgeId,
            printingMethodId = Some(digitalId),
            inkConfiguration = Some(InkConfiguration.cmyk4_0),
            finishes = List(FinishSelection(mylarOverlayId), FinishSelection(safetyPinId)),
          )
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Circular(PositiveMm.unsafe(58)),
          quantity = Quantity.unsafe(100),
          deliveryFormat = DeliveryFormat.PackedUnits,
        ),
      ),
    ),
  )

  val cups: ProductCategory = ProductCategory(
    id = cupsId,
    name = LocalizedString("Custom Cups & Mugs", "Hrnky s potiskem"),
    components = NonEmptyChunk.single(
      ComponentTemplate(
        key = ComponentKey.Main,
        label = LocalizedString("Mug Blank", "Hrnek"),
        allowedMaterialIds = cupMaterials,
        allowedFinishIds = Set(dishwasherCoatId, giftBoxId, glossyGlazeId),
      )
    ),
    allowedPrintingMethodIds = Set(sublimationId, uvInkjetId, screenPrintId),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-cup-ceramic-sublimation"),
        name = LocalizedString("White Ceramic Mug 330ml Sublimation", "Bílý keramický hrnek 330ml sublimace"),
        components = NonEmptyChunk.single(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = ceramicMugWhiteId,
            printingMethodId = Some(sublimationId),
            inkConfiguration = Some(InkConfiguration.cmyk4_0),
            finishes = List(FinishSelection(dishwasherCoatId), FinishSelection(giftBoxId)),
          )
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.PrintArea(Dimension2D.unsafeMm(200, 85), Some("330ml")),
          quantity = Quantity.unsafe(24),
          deliveryFormat = DeliveryFormat.PackedUnits,
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-cup-travel-uv"),
        name = LocalizedString("Stainless Travel Mug UV Print", "Nerezový termohrnek s UV potiskem"),
        components = NonEmptyChunk.single(
          ComponentPreset(
            key = ComponentKey.Main,
            materialId = stainlessTravelMugId,
            printingMethodId = Some(uvInkjetId),
            inkConfiguration = Some(InkConfiguration.cmyk4W_0),
            finishes = List(FinishSelection(giftBoxId)),
          )
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.PrintArea(Dimension2D.unsafeMm(180, 100), Some("450ml")),
          quantity = Quantity.unsafe(20),
          deliveryFormat = DeliveryFormat.PackedUnits,
        ),
      ),
    ),
  )

  val free: ProductCategory = ProductCategory(
    id = freeId,
    name = LocalizedString("Custom Print (Free Configuration)", "Vlastní tisk (volná konfigurace)"),
    components = NonEmptyChunk.single(
      ComponentTemplate(
        key = ComponentKey.Main,
        label = LocalizedString("Custom Media", "Materiál"),
        allowedMaterialIds = allMaterials.map(_.id).toSet - rollUpStandEconomyId - rollUpStandPremiumId,
        allowedFinishIds = allFinishes.map(_.id).toSet,
      )
    ),
    allowedPrintingMethodIds = Set.empty, // Open: allows any printing method
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-free-a4"),
        name = LocalizedString("Custom A4 Sheet Print", "Vlastní tisk A4"),
        components = NonEmptyChunk.single(
          ComponentPreset(ComponentKey.Main, coatedMatte200gsmId, Some(digitalId), Some(InkConfiguration.cmyk4_4))
        ),
        defaultSpecs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.A4),
          quantity = Quantity.unsafe(100),
        ),
      ),
    ),
  )

  val allCategories: List[ProductCategory] = List(
    businessCards, flyers, brochures, banners, packaging, booklets, calendars,
    postcards, stickers, rollUps, tshirts, ecoBags, pinBadges, cups, free,
  )

  // ── Showcase Listings (15) ─────────────────────────────────────────────────

  val allShowcases: List[ShowcaseListing] = List(
    ShowcaseListing(businessCardsId, CatalogGroup.Sheet,
      LocalizedString("Make a lasting first impression", "Udělejte nezapomenutelný první dojem"),
      LocalizedString("Premium 250–350gsm card stocks with lamination, embossing, foil stamping, and letterpress.", "Prémiové kartony 250–350g s laminací, slepotiskem, ražbou fólií a knihtiskem."), 1,
      imageUrl = Some("https://images.unsplash.com/photo-1628891439478-c613e85af7d6?w=600&h=400&fit=crop")),
    ShowcaseListing(flyersId, CatalogGroup.Sheet,
      LocalizedString("Eye-catching promotional prints", "Poutavé propagační materiály"),
      LocalizedString("Single-sheet flyers from 90gsm to 350gsm in portrait or landscape with optional coatings.", "Jednostránkové letáky od 90g po 350g na výšku či šířku s ochrannými úpravami."), 2,
      imageUrl = Some("https://images.unsplash.com/photo-1563097013-a1df1d1fd1c7?w=600&h=400&fit=crop")),
    ShowcaseListing(brochuresId, CatalogGroup.Sheet,
      LocalizedString("Structured storytelling in every fold", "Přehledná prezentace v každém skladu"),
      LocalizedString("Half-fold, tri-fold, Z-fold, and gate-fold brochures with automatic flat-sheet imposition.", "Půlené, trojskládané a Z-skládané brožury s automatickým vyřazením na tiskový arch."), 3,
      imageUrl = Some("https://images.unsplash.com/photo-1695634621121-691d54259d37?w=600&h=400&fit=crop")),
    ShowcaseListing(postcardsId, CatalogGroup.Sheet,
      LocalizedString("Tactile cards that stand out", "Pohlednice a pozvánky, které zaujmou"),
      LocalizedString("Heavy coated, cotton, or kraft cards with front gloss lamination or luxury foil.", "Silné křídové, bavlněné či kraftové karty s přední laminací nebo zlatou ražbou."), 4,
      imageUrl = Some("https://images.unsplash.com/photo-1557717398-f6a5f68cd158?w=600&h=400&fit=crop")),
    ShowcaseListing(bannersId, CatalogGroup.LargeFormat,
      LocalizedString("Bold outdoor visibility at scale", "Výrazná venkovní viditelnost"),
      LocalizedString("Weather-resistant 510g PVC banners and vinyls with metal grommets and elastic bungee ropes.", "Odolné PVC bannery 510g s kovovými oky a gumolanem."), 5,
      imageUrl = Some("https://images.unsplash.com/photo-1558452919-08ae4aea8e29?w=600&h=400&fit=crop")),
    ShowcaseListing(rollUpsId, CatalogGroup.LargeFormat,
      LocalizedString("Portable trade-show displays", "Přenosné veletržní stojany"),
      LocalizedString("High-resolution printed banner film paired with economy or weighted aluminum stands.", "Kvalitní tiskový pás v kombinaci s ekonomickým nebo zátěžovým hliníkovým stojanem."), 6,
      imageUrl = Some("https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=600&h=400&fit=crop")),
    ShowcaseListing(stickersId, CatalogGroup.Specialty,
      LocalizedString("Stick your brand anywhere", "Samolepky pro každou příležitost"),
      LocalizedString("Paper kiss-cut sheets or outdoor-rated vinyl and clear vinyl with white ink underbase.", "Papírové násekové archy nebo venkovní a čirý vinyl s bílým podtiskem."), 7,
      imageUrl = Some("https://images.unsplash.com/photo-1589384267710-7a170981ca78?w=600&h=400&fit=crop")),
    ShowcaseListing(bookletsId, CatalogGroup.Bound,
      LocalizedString("Multi-page catalogs built to last", "Vícestránkové katalogy a brožury"),
      LocalizedString("Saddle-stitched or perfect-bound catalogs with separate Cover and Body paper selections.", "Šité V1 nebo lepené V2 katalogy s nezávislým výběrem papíru pro obálku a vnitřek."), 8,
      imageUrl = Some("https://images.unsplash.com/photo-1593723117578-c3f29d0b62c3?w=600&h=400&fit=crop")),
    ShowcaseListing(calendarsId, CatalogGroup.Bound,
      LocalizedString("Year-round brand presence", "Vaše značka na očích celý rok"),
      LocalizedString("Wall and desk calendars with spiral or Wire-O binding and custom cover finishes.", "Nástěnné a stolní kalendáře se spirálovou vazbou a laminovanou obálkou."), 9,
      imageUrl = Some("https://images.unsplash.com/photo-1598972692343-a17711455a22?w=600&h=400&fit=crop")),
    ShowcaseListing(packagingId, CatalogGroup.Specialty,
      LocalizedString("Unforgettable unboxing experiences", "Nezapomenutelný zážitek z rozbalení"),
      LocalizedString("Custom die-cut kraft, corrugated, and folding carton boxes sized from 3D dimensions.", "Výsekové kraftové, vlnité a skládané krabičky s výpočtem rozvinu z 3D rozměrů."), 10,
      imageUrl = Some("https://images.unsplash.com/photo-1617825295690-28ae56c56135?w=600&h=400&fit=crop")),
    ShowcaseListing(tshirtsId, CatalogGroup.Promotional,
      LocalizedString("Wearable brand merch", "Kvalitní reklamní textil"),
      LocalizedString("Cotton, organic, and performance tees printed via DTG, screen printing, or sublimation.", "Bavlněná, bio i funkční trička potištěná DTG, sítotiskem nebo sublimací."), 11,
      imageUrl = Some("https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=600&h=400&fit=crop")),
    ShowcaseListing(ecoBagsId, CatalogGroup.Promotional,
      LocalizedString("Sustainable everyday visibility", "Ekologická reklama na každý den"),
      LocalizedString("Canvas, organic cotton, rPET, and jute totes with reinforced handles or embroidery.", "Plátěné, bio, rPET a jutové tašky se zesílenými uchy nebo výšivkou."), 12,
      imageUrl = Some("https://images.unsplash.com/photo-1772890753145-089cd7618a8e?w=600&h=400&fit=crop")),
    ShowcaseListing(pinBadgesId, CatalogGroup.Promotional,
      LocalizedString("Compact collectibles & event swag", "Placky pro akce a kampaně"),
      LocalizedString("Tinplate, acrylic, and wooden pin badges with safety pin, magnet, or bottle opener backs.", "Plechové, akrylové a dřevěné placky se špendlíkem, magnetem nebo otvírákem."), 13,
      imageUrl = Some("https://images.unsplash.com/photo-1521249692263-e0659c60326e?w=600&h=400&fit=crop")),
    ShowcaseListing(cupsId, CatalogGroup.Promotional,
      LocalizedString("Daily desk companions", "Hrnky pro každodenní kávu"),
      LocalizedString("Ceramic, magic color-changing, enamel, and stainless travel mugs with optional gift boxes.", "Keramické, magické, smaltované i nerezové hrnky včetně dárkové krabičky."), 14,
      imageUrl = Some("https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=600&h=400&fit=crop")),
    ShowcaseListing(freeId, CatalogGroup.Specialty,
      LocalizedString("Open custom configuration", "Otevřená vlastní konfigurace"),
      LocalizedString("Full unrestricted access to all substrates, print processes, and finishing operations.", "Plný přístup ke všem materiálům, tiskovým technologiím a dokončujícím úpravám."), 15),
  )

  val catalog: ProductCatalog = ProductCatalog(
    version = Version.unsafe("2026.1"),
    categories = allCategories.map(c => c.id -> c).toMap,
    materials = allMaterials.map(m => m.id -> m).toMap,
    finishes = allFinishes.map(f => f.id -> f).toMap,
    printingMethods = allPrintingMethods.map(p => p.id -> p).toMap,
    showcases = allShowcases.map(s => s.categoryId -> s).toMap,
  )
