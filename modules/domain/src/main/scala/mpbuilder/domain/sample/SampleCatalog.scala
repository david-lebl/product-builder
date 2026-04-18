package mpbuilder.domain.sample

import mpbuilder.domain.model.*

object SampleCatalog:

  // --- Material IDs ---
  val coated300gsmId: MaterialId    = MaterialId.unsafe("mat-coated-300gsm")
  val uncoatedBondId: MaterialId    = MaterialId.unsafe("mat-uncoated-bond")
  val kraftId: MaterialId           = MaterialId.unsafe("mat-kraft")
  val vinylId: MaterialId           = MaterialId.unsafe("mat-vinyl")
  val corrugatedId: MaterialId      = MaterialId.unsafe("mat-corrugated")
  val coatedSilk250gsmId: MaterialId = MaterialId.unsafe("mat-coated-silk-250gsm")
  val yupoId: MaterialId            = MaterialId.unsafe("mat-yupo")
  val adhesiveStockId: MaterialId   = MaterialId.unsafe("mat-adhesive-stock")
  val cottonId: MaterialId          = MaterialId.unsafe("mat-cotton-300gsm")
  val clearVinylId: MaterialId      = MaterialId.unsafe("mat-clear-vinyl")

  // --- Coated Art Paper Glossy IDs ---
  val coatedGlossy90gsmId: MaterialId  = MaterialId.unsafe("mat-coated-glossy-90gsm")
  val coatedGlossy115gsmId: MaterialId = MaterialId.unsafe("mat-coated-glossy-115gsm")
  val coatedGlossy130gsmId: MaterialId = MaterialId.unsafe("mat-coated-glossy-130gsm")
  val coatedGlossy150gsmId: MaterialId = MaterialId.unsafe("mat-coated-glossy-150gsm")
  val coatedGlossy170gsmId: MaterialId = MaterialId.unsafe("mat-coated-glossy-170gsm")
  val coatedGlossy200gsmId: MaterialId = MaterialId.unsafe("mat-coated-glossy-200gsm")
  val coatedGlossy250gsmId: MaterialId = MaterialId.unsafe("mat-coated-glossy-250gsm")
  val coatedGlossy350gsmId: MaterialId = MaterialId.unsafe("mat-coated-glossy-350gsm")

  // --- Coated Art Paper Matte IDs ---
  val coatedMatte90gsmId: MaterialId  = MaterialId.unsafe("mat-coated-matte-90gsm")
  val coatedMatte115gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-115gsm")
  val coatedMatte130gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-130gsm")
  val coatedMatte150gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-150gsm")
  val coatedMatte170gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-170gsm")
  val coatedMatte200gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-200gsm")
  val coatedMatte250gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-250gsm")
  val coatedMatte300gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-300gsm")
  val coatedMatte350gsmId: MaterialId = MaterialId.unsafe("mat-coated-matte-350gsm")

  // --- Finish IDs ---
  val matteLaminationId: FinishId   = FinishId.unsafe("fin-matte-lam")
  val glossLaminationId: FinishId   = FinishId.unsafe("fin-gloss-lam")
  val uvCoatingId: FinishId         = FinishId.unsafe("fin-uv-coating")
  val embossingId: FinishId         = FinishId.unsafe("fin-embossing")
  val foilStampingId: FinishId      = FinishId.unsafe("fin-foil-stamping")
  val dieCutId: FinishId            = FinishId.unsafe("fin-die-cut")
  val varnishId: FinishId           = FinishId.unsafe("fin-varnish")
  val softTouchCoatingId: FinishId  = FinishId.unsafe("fin-soft-touch")
  val aqueousCoatingId: FinishId    = FinishId.unsafe("fin-aqueous-coating")
  val debossingId: FinishId         = FinishId.unsafe("fin-debossing")
  val scoringId: FinishId           = FinishId.unsafe("fin-scoring")
  val perforationId: FinishId       = FinishId.unsafe("fin-perforation")
  val roundCornersId: FinishId      = FinishId.unsafe("fin-round-corners")
  val grommetsId: FinishId          = FinishId.unsafe("fin-grommets")
  val kissCutId: FinishId           = FinishId.unsafe("fin-kiss-cut")
  val overlaminationId: FinishId    = FinishId.unsafe("fin-overlamination")

  // --- Promotional Finish IDs ---
  val heatPressId: FinishId          = FinishId.unsafe("fin-heat-press")
  val labelPrintId: FinishId         = FinishId.unsafe("fin-label-print")
  val foldBagId: FinishId            = FinishId.unsafe("fin-fold-bag")
  val mylarOverlayId: FinishId       = FinishId.unsafe("fin-mylar-overlay")
  val safetyPinId: FinishId          = FinishId.unsafe("fin-safety-pin")
  val magnetBackId: FinishId         = FinishId.unsafe("fin-magnet-back")
  val bottleOpenerId: FinishId       = FinishId.unsafe("fin-bottle-opener")
  val dishwasherCoatId: FinishId     = FinishId.unsafe("fin-dishwasher-coat")
  val giftBoxId: FinishId            = FinishId.unsafe("fin-gift-box")
  val glossyGlazeId: FinishId        = FinishId.unsafe("fin-glossy-glaze")
  val embroideryId: FinishId         = FinishId.unsafe("fin-embroidery")
  val reinforcedHandlesId: FinishId  = FinishId.unsafe("fin-reinforced-handles")

  // --- Category IDs ---
  val businessCardsId: CategoryId   = CategoryId.unsafe("cat-business-cards")
  val flyersId: CategoryId          = CategoryId.unsafe("cat-flyers")
  val brochuresId: CategoryId       = CategoryId.unsafe("cat-brochures")
  val bannersId: CategoryId         = CategoryId.unsafe("cat-banners")
  val packagingId: CategoryId       = CategoryId.unsafe("cat-packaging")
  val bookletsId: CategoryId        = CategoryId.unsafe("cat-booklets")
  val calendarsId: CategoryId       = CategoryId.unsafe("cat-calendars")
  val postcardsId: CategoryId       = CategoryId.unsafe("cat-postcards")
  val stickersId: CategoryId        = CategoryId.unsafe("cat-stickers")
  val rollUpsId: CategoryId         = CategoryId.unsafe("cat-roll-ups")
  val freeId: CategoryId            = CategoryId.unsafe("cat-free")

  // --- Promotional Category IDs ---
  val tshirtsId: CategoryId          = CategoryId.unsafe("cat-tshirts")
  val ecoBagsId: CategoryId          = CategoryId.unsafe("cat-eco-bags")
  val pinBadgesId: CategoryId        = CategoryId.unsafe("cat-pin-badges")
  val cupsId: CategoryId             = CategoryId.unsafe("cat-cups")

  // --- Roll-Up Material IDs ---
  val rollUpBannerFilmId: MaterialId    = MaterialId.unsafe("mat-rollup-banner-film")
  val rollUpStandEconomyId: MaterialId  = MaterialId.unsafe("mat-rollup-stand-economy")
  val rollUpStandPremiumId: MaterialId  = MaterialId.unsafe("mat-rollup-stand-premium")

  // --- Calendar Protective Cover Material IDs ---
  val transparentPlasticCoverId: MaterialId  = MaterialId.unsafe("mat-transparent-plastic-cover")
  val cardboard350gWhiteId: MaterialId       = MaterialId.unsafe("mat-cardboard-350g-white")
  val cardboard350gBlackId: MaterialId       = MaterialId.unsafe("mat-cardboard-350g-black")
  val cardboard350gGreyId: MaterialId        = MaterialId.unsafe("mat-cardboard-350g-grey")
  val cardboard350gBrownId: MaterialId       = MaterialId.unsafe("mat-cardboard-350g-brown")

  // --- Binding Color Material IDs ---
  // Plastic O-Ring binding (all available colors)
  val bindingPlasticBlackId: MaterialId  = MaterialId.unsafe("mat-binding-plastic-black")
  val bindingPlasticWhiteId: MaterialId  = MaterialId.unsafe("mat-binding-plastic-white")
  val bindingPlasticSilverId: MaterialId = MaterialId.unsafe("mat-binding-plastic-silver")
  val bindingPlasticBlueId: MaterialId   = MaterialId.unsafe("mat-binding-plastic-blue")
  val bindingPlasticRedId: MaterialId    = MaterialId.unsafe("mat-binding-plastic-red")
  val bindingPlasticClearId: MaterialId  = MaterialId.unsafe("mat-binding-plastic-clear")
  // Metal wire binding colors
  val bindingMetalBlackId: MaterialId    = MaterialId.unsafe("mat-binding-metal-black")
  val bindingMetalSilverId: MaterialId   = MaterialId.unsafe("mat-binding-metal-silver")
  val bindingMetalWhiteId: MaterialId    = MaterialId.unsafe("mat-binding-metal-white")

  /** All plastic o-ring binding material IDs */
  val allBindingPlasticIds: Set[MaterialId] = Set(
    bindingPlasticBlackId, bindingPlasticWhiteId, bindingPlasticSilverId,
    bindingPlasticBlueId, bindingPlasticRedId, bindingPlasticClearId,
  )

  /** All metal wire binding material IDs */
  val allBindingMetalIds: Set[MaterialId] = Set(
    bindingMetalBlackId, bindingMetalSilverId, bindingMetalWhiteId,
  )

  /** All binding color material IDs combined */
  val allBindingMaterialIds: Set[MaterialId] = allBindingPlasticIds ++ allBindingMetalIds

  // --- Promotional Material IDs ---
  // T-Shirts
  val cottonTshirt150Id: MaterialId       = MaterialId.unsafe("mat-cotton-tshirt-150")
  val cottonTshirt180Id: MaterialId       = MaterialId.unsafe("mat-cotton-tshirt-180")
  val polyesterTshirtId: MaterialId       = MaterialId.unsafe("mat-polyester-tshirt")
  val cottonPolyBlendId: MaterialId       = MaterialId.unsafe("mat-cotton-poly-blend")
  val organicCottonTshirtId: MaterialId   = MaterialId.unsafe("mat-organic-cotton-tshirt")
  // Eco Bags
  val cottonCanvasBagId: MaterialId       = MaterialId.unsafe("mat-cotton-canvas-bag")
  val organicCottonBagId: MaterialId      = MaterialId.unsafe("mat-organic-cotton-bag")
  val recycledPetBagId: MaterialId        = MaterialId.unsafe("mat-recycled-pet-bag")
  val juteBagId: MaterialId               = MaterialId.unsafe("mat-jute-bag")
  val nonWovenPpBagId: MaterialId         = MaterialId.unsafe("mat-non-woven-pp-bag")
  // Pin Badges
  val tinplateBadgeId: MaterialId         = MaterialId.unsafe("mat-tinplate-badge")
  val acrylicBadgeId: MaterialId          = MaterialId.unsafe("mat-acrylic-badge")
  val woodenBadgeId: MaterialId           = MaterialId.unsafe("mat-wooden-badge")
  // Cups & Mugs
  val ceramicMugWhiteId: MaterialId       = MaterialId.unsafe("mat-ceramic-mug-white")
  val ceramicMugColoredId: MaterialId     = MaterialId.unsafe("mat-ceramic-mug-colored")
  val magicMugId: MaterialId              = MaterialId.unsafe("mat-magic-mug")
  val stainlessTravelMugId: MaterialId    = MaterialId.unsafe("mat-stainless-travel-mug")
  val enamelMugId: MaterialId             = MaterialId.unsafe("mat-enamel-mug")
  val glassMugId: MaterialId              = MaterialId.unsafe("mat-glass-mug")

  // --- Printing Method IDs ---
  val offsetId: PrintingMethodId       = PrintingMethodId.unsafe("pm-offset")
  val digitalId: PrintingMethodId      = PrintingMethodId.unsafe("pm-digital")
  val uvInkjetId: PrintingMethodId     = PrintingMethodId.unsafe("pm-uv-inkjet")
  val letterpressId: PrintingMethodId  = PrintingMethodId.unsafe("pm-letterpress")

  // --- Promotional Printing Method IDs ---
  val screenPrintId: PrintingMethodId    = PrintingMethodId.unsafe("pm-screen-print")
  val dtgId: PrintingMethodId            = PrintingMethodId.unsafe("pm-dtg")
  val sublimationId: PrintingMethodId    = PrintingMethodId.unsafe("pm-sublimation")

  // --- Materials ---
  val coated300gsm: Material = Material(
    id = coated300gsmId,
    name = LocalizedString("Coated Art Paper 300gsm", "Křídový papír 300g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(300)),
    properties = Set(MaterialProperty.Glossy, MaterialProperty.SmoothSurface, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Premium coated art paper with a smooth, glossy surface. Ideal for business cards and high-quality print materials where vibrant colors and sharp detail are important.",
      "Prémiový křídový papír s hladkým lesklým povrchem. Ideální pro vizitky a kvalitní tiskové materiály, kde záleží na sytých barvách a ostrých detailech.",
    )),
  )

  val uncoatedBond: Material = Material(
    id = uncoatedBondId,
    name = LocalizedString("Uncoated Bond Paper 120gsm", "Nenatíraný papír 120g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(120)),
    properties = Set(MaterialProperty.Matte, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Natural, uncoated paper with a soft matte feel. Easy to write on, making it perfect for letterheads, forms, and materials that require a handwritten note.",
      "Přírodní nenatíraný papír s jemným matným povrchem. Snadno se na něj píše, ideální pro hlavičkové papíry, formuláře a materiály vyžadující ruční poznámky.",
    )),
  )

  val kraft: Material = Material(
    id = kraftId,
    name = LocalizedString("Kraft Paper 250gsm", "Kraftový papír 250g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(250)),
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Eco-friendly brown kraft paper with a distinctive natural texture. Popular for organic brands, eco packaging, and rustic design aesthetics.",
      "Ekologický hnědý kraftový papír s výraznou přírodní texturou. Oblíbený pro bio značky, eko balení a rustikální designový styl.",
    )),
  )

  val vinyl: Material = Material(
    id = vinylId,
    name = LocalizedString("Adhesive Vinyl", "Samolepicí vinyl"),
    family = MaterialFamily.Vinyl,
    weight = None,
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.Glossy, MaterialProperty.SmoothSurface),
    description = Some(LocalizedString(
      "Durable, weather-resistant adhesive vinyl for outdoor banners, vehicle wraps, and signage. Resistant to UV, rain, and temperature changes.",
      "Odolný, povětrnostně stálý samolepicí vinyl pro venkovní bannery, polepy vozidel a značení. Odolný vůči UV, dešti a změnám teploty.",
    )),
  )

  val corrugated: Material = Material(
    id = corrugatedId,
    name = LocalizedString("Corrugated Cardboard", "Vlnitá lepenka"),
    family = MaterialFamily.Cardboard,
    weight = None,
    properties = Set(MaterialProperty.Recyclable, MaterialProperty.Textured),
    description = Some(LocalizedString(
      "Sturdy corrugated cardboard for packaging and displays. Lightweight yet strong, with excellent structural rigidity for boxes and protective packaging.",
      "Pevná vlnitá lepenka pro balení a displeje. Lehká, ale pevná, s vynikající strukturální tuhostí pro krabice a ochranné obaly.",
    )),
  )

  val coatedSilk250gsm: Material = Material(
    id = coatedSilk250gsmId,
    name = LocalizedString("Coated Silk 250gsm", "Křídový saténový papír 250g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(250)),
    properties = Set(MaterialProperty.Matte, MaterialProperty.SmoothSurface, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Semi-matte coated paper with a silky smooth finish. Reduces glare while maintaining excellent color reproduction. Great for brochures and catalogs.",
      "Polopololesklý křídový papír s hedvábně hladkým povrchem. Snižuje odlesky při zachování vynikající reprodukce barev. Skvělý pro brožury a katalogy.",
    )),
  )

  val yupo: Material = Material(
    id = yupoId,
    name = LocalizedString("Yupo Synthetic 200μm", "Syntetický papír Yupo 200μm"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(200)),
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.SmoothSurface),
    description = Some(LocalizedString(
      "Waterproof synthetic paper that won't tear, wrinkle, or yellow. Perfect for outdoor menus, maps, tags, and applications requiring extreme durability.",
      "Voděodolný syntetický papír, který se netrhá, nekrčí ani nežloutne. Ideální pro venkovní jídelníčky, mapy, štítky a aplikace vyžadující extrémní odolnost.",
    )),
  )

  val adhesiveStock: Material = Material(
    id = adhesiveStockId,
    name = LocalizedString("Adhesive Stock 100gsm", "Samolepicí materiál 100g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(100)),
    properties = Set(MaterialProperty.Glossy, MaterialProperty.SmoothSurface),
    description = Some(LocalizedString(
      "Self-adhesive label stock with a glossy face. Suitable for product labels, stickers, and decals with a peel-and-stick backing.",
      "Samolepicí materiál s lesklým povrchem. Vhodný pro produktové štítky, samolepky a obtisky s odlepovacím podkladem.",
    )),
  )

  val clearVinyl: Material = Material(
    id = clearVinylId,
    name = LocalizedString("Clear Adhesive Vinyl", "Průhledný samolepicí vinyl"),
    family = MaterialFamily.Vinyl,
    weight = None,
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.SmoothSurface, MaterialProperty.Transparent),
    description = Some(LocalizedString(
      "Transparent self-adhesive vinyl for window graphics, clear labels, and overlay applications where see-through effect is desired.",
      "Průhledný samolepicí vinyl pro okenní grafiku, průhledné štítky a překryvné aplikace, kde je žádoucí průhledný efekt.",
    )),
  )

  val cotton: Material = Material(
    id = cottonId,
    name = LocalizedString("Cotton Paper 300gsm", "Bavlněný papír 300g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(300)),
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Luxurious cotton paper with a distinctive textured feel. Made from cotton fibers for a premium tactile experience. Perfect for high-end business cards and invitations.",
      "Luxusní bavlněný papír s výrazným hmatovým dojmem. Vyrobený z bavlněných vláken pro prémiový hmatový zážitek. Ideální pro luxusní vizitky a pozvánky.",
    )),
  )

  // --- Coated Art Paper Glossy ---
  private val glossyCoatedProps = Set(MaterialProperty.Glossy, MaterialProperty.SmoothSurface, MaterialProperty.Recyclable)

  val coatedGlossy90gsm: Material = Material(
    id = coatedGlossy90gsmId,
    name = LocalizedString("Coated Art Paper Glossy 90gsm", "Křídový papír lesklý 90g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(90)),
    properties = glossyCoatedProps,
  )

  val coatedGlossy115gsm: Material = Material(
    id = coatedGlossy115gsmId,
    name = LocalizedString("Coated Art Paper Glossy 115gsm", "Křídový papír lesklý 115g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(115)),
    properties = glossyCoatedProps,
  )

  val coatedGlossy130gsm: Material = Material(
    id = coatedGlossy130gsmId,
    name = LocalizedString("Coated Art Paper Glossy 130gsm", "Křídový papír lesklý 130g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(130)),
    properties = glossyCoatedProps,
  )

  val coatedGlossy150gsm: Material = Material(
    id = coatedGlossy150gsmId,
    name = LocalizedString("Coated Art Paper Glossy 150gsm", "Křídový papír lesklý 150g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(150)),
    properties = glossyCoatedProps,
  )

  val coatedGlossy170gsm: Material = Material(
    id = coatedGlossy170gsmId,
    name = LocalizedString("Coated Art Paper Glossy 170gsm", "Křídový papír lesklý 170g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(170)),
    properties = glossyCoatedProps,
  )

  val coatedGlossy200gsm: Material = Material(
    id = coatedGlossy200gsmId,
    name = LocalizedString("Coated Art Paper Glossy 200gsm", "Křídový papír lesklý 200g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(200)),
    properties = glossyCoatedProps,
  )

  val coatedGlossy250gsm: Material = Material(
    id = coatedGlossy250gsmId,
    name = LocalizedString("Coated Art Paper Glossy 250gsm", "Křídový papír lesklý 250g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(250)),
    properties = glossyCoatedProps,
  )

  val coatedGlossy350gsm: Material = Material(
    id = coatedGlossy350gsmId,
    name = LocalizedString("Coated Art Paper Glossy 350gsm", "Křídový papír lesklý 350g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(350)),
    properties = glossyCoatedProps,
  )

  // --- Coated Art Paper Matte ---
  private val matteCoatedProps = Set(MaterialProperty.Matte, MaterialProperty.SmoothSurface, MaterialProperty.Recyclable)

  val coatedMatte90gsm: Material = Material(
    id = coatedMatte90gsmId,
    name = LocalizedString("Coated Art Paper Matte 90gsm", "Křídový papír matný 90g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(90)),
    properties = matteCoatedProps,
  )

  val coatedMatte115gsm: Material = Material(
    id = coatedMatte115gsmId,
    name = LocalizedString("Coated Art Paper Matte 115gsm", "Křídový papír matný 115g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(115)),
    properties = matteCoatedProps,
  )

  val coatedMatte130gsm: Material = Material(
    id = coatedMatte130gsmId,
    name = LocalizedString("Coated Art Paper Matte 130gsm", "Křídový papír matný 130g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(130)),
    properties = matteCoatedProps,
  )

  val coatedMatte150gsm: Material = Material(
    id = coatedMatte150gsmId,
    name = LocalizedString("Coated Art Paper Matte 150gsm", "Křídový papír matný 150g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(150)),
    properties = matteCoatedProps,
  )

  val coatedMatte170gsm: Material = Material(
    id = coatedMatte170gsmId,
    name = LocalizedString("Coated Art Paper Matte 170gsm", "Křídový papír matný 170g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(170)),
    properties = matteCoatedProps,
  )

  val coatedMatte200gsm: Material = Material(
    id = coatedMatte200gsmId,
    name = LocalizedString("Coated Art Paper Matte 200gsm", "Křídový papír matný 200g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(200)),
    properties = matteCoatedProps,
  )

  val coatedMatte250gsm: Material = Material(
    id = coatedMatte250gsmId,
    name = LocalizedString("Coated Art Paper Matte 250gsm", "Křídový papír matný 250g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(250)),
    properties = matteCoatedProps,
  )

  val coatedMatte300gsm: Material = Material(
    id = coatedMatte300gsmId,
    name = LocalizedString("Coated Art Paper Matte 300gsm", "Křídový papír matný 300g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(300)),
    properties = matteCoatedProps,
  )

  val coatedMatte350gsm: Material = Material(
    id = coatedMatte350gsmId,
    name = LocalizedString("Coated Art Paper Matte 350gsm", "Křídový papír matný 350g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(350)),
    properties = matteCoatedProps,
  )

  // --- Printing Methods ---
  val offsetMethod: PrintingMethod = PrintingMethod(
    id = offsetId,
    name = LocalizedString("Offset Printing", "Ofsetový tisk"),
    processType = PrintingProcessType.Offset,
    maxColorCount = Some(6),
    description = Some(LocalizedString(
      "Traditional high-quality printing using plates. Best for large runs (500+) with consistent color accuracy. Supports Pantone spot colors for precise brand matching.",
      "Tradiční vysoce kvalitní tisk pomocí tiskových desek. Nejlepší pro velké náklady (500+) s konzistentní přesností barev. Podporuje přímé barvy Pantone pro přesné shody značek.",
    )),
  )

  val digitalMethod: PrintingMethod = PrintingMethod(
    id = digitalId,
    name = LocalizedString("Digital Printing", "Digitální tisk"),
    processType = PrintingProcessType.Digital,
    maxColorCount = None,
    description = Some(LocalizedString(
      "Modern toner or inkjet-based printing. Cost-effective for short runs and variable data. No plate setup required — ideal for quick turnaround and personalized prints.",
      "Moderní tisk na bázi toneru nebo inkoustu. Ekonomický pro malé náklady a variabilní data. Nevyžaduje přípravu tiskových desek — ideální pro rychlou realizaci a personalizované tisky.",
    )),
  )

  val uvInkjetMethod: PrintingMethod = PrintingMethod(
    id = uvInkjetId,
    name = LocalizedString("UV Curable Inkjet", "UV inkoustový tisk"),
    processType = PrintingProcessType.UVCurableInkjet,
    maxColorCount = None,
    description = Some(LocalizedString(
      "Wide-format inkjet printing with UV-cured inks. Produces durable, scratch-resistant output for banners, signage, and outdoor graphics. Excellent on vinyl and rigid substrates.",
      "Velkoformátový inkoustový tisk s UV vytvrzovanými inkousty. Produkuje odolný, proti poškrábání odolný výstup pro bannery, značení a venkovní grafiku. Vynikající na vinylu a tuhých materiálech.",
    )),
  )

  val letterpressMethod: PrintingMethod = PrintingMethod(
    id = letterpressId,
    name = LocalizedString("Letterpress", "Knihtisk"),
    processType = PrintingProcessType.Letterpress,
    maxColorCount = Some(2),
    description = Some(LocalizedString(
      "Artisan relief printing that presses ink into thick paper creating a debossed tactile impression. Limited to 1–2 colors. Premium choice for luxury business cards and invitations.",
      "Řemeslný reliéfní tisk, který vtlačuje inkoust do silného papíru a vytváří hmatatelný dojem. Omezeno na 1–2 barvy. Prémiová volba pro luxusní vizitky a pozvánky.",
    )),
  )

  // --- Promotional Printing Methods ---
  val screenPrintMethod: PrintingMethod = PrintingMethod(
    id = screenPrintId,
    name = LocalizedString("Screen Printing", "Sítotisk"),
    processType = PrintingProcessType.ScreenPrint,
    maxColorCount = Some(8),
    description = Some(LocalizedString("Best for bulk orders with vibrant solid colors", "Nejlepší pro velké náklady se sytými plnými barvami")),
  )

  val dtgMethod: PrintingMethod = PrintingMethod(
    id = dtgId,
    name = LocalizedString("Direct-to-Garment (DTG)", "Přímý tisk na textil (DTG)"),
    processType = PrintingProcessType.Digital,
    maxColorCount = None,
    description = Some(LocalizedString("Full-color photo prints, best for small runs", "Plnobarevný fototisk, nejlepší pro malé náklady")),
  )

  val sublimationMethod: PrintingMethod = PrintingMethod(
    id = sublimationId,
    name = LocalizedString("Dye Sublimation", "Sublimační tisk"),
    processType = PrintingProcessType.Digital,
    maxColorCount = None,
    description = Some(LocalizedString("All-over prints on polyester and coated surfaces", "Celoplošný tisk na polyester a povrchově upravené materiály")),
  )

  // --- Finishes ---
  val matteLamination: Finish = Finish(
    id = matteLaminationId,
    name = LocalizedString("Matte Lamination", "Matná laminace"),
    finishType = FinishType.Lamination,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Protective matte film applied to the surface. Provides a smooth, non-reflective finish with a velvety feel. Reduces glare and fingerprints.",
      "Ochranná matná fólie aplikovaná na povrch. Poskytuje hladký, nereflexní povrch s hedvábným dojmem. Snižuje odlesky a otisky prstů.",
    )),
  )

  val glossLamination: Finish = Finish(
    id = glossLaminationId,
    name = LocalizedString("Gloss Lamination", "Lesklá laminace"),
    finishType = FinishType.Lamination,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Shiny protective film that enhances color vibrancy and contrast. Makes images pop with a high-gloss mirror-like finish. Adds durability and water resistance.",
      "Lesklá ochranná fólie, která zvyšuje sytost barev a kontrast. Dodává obrázkům výraznost s vysoce lesklým zrcadlovým povrchem. Přidává odolnost a voděodolnost.",
    )),
  )

  val uvCoating: Finish = Finish(
    id = uvCoatingId,
    name = LocalizedString("UV Coating", "UV lak"),
    finishType = FinishType.UVCoating,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "Liquid coating cured with ultraviolet light. Provides a high-gloss, scratch-resistant surface. Can be applied as full coverage or spot UV for selective highlighting.",
      "Tekutý lak vytvrzený ultrafialovým světlem. Poskytuje vysoce lesklý povrch odolný proti poškrábání. Lze aplikovat plošně nebo jako parciální UV pro selektivní zvýraznění.",
    )),
  )

  val embossing: Finish = Finish(
    id = embossingId,
    name = LocalizedString("Embossing", "Slepotisk"),
    finishType = FinishType.Embossing,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "Raised relief pressing that creates a 3D tactile effect on the paper surface. Adds a premium, luxurious feel to logos, text, or patterns.",
      "Reliéfní lisování, které vytváří 3D hmatový efekt na povrchu papíru. Dodává prémiový, luxusní dojem logům, textu nebo vzorům.",
    )),
  )

  val foilStamping: Finish = Finish(
    id = foilStampingId,
    name = LocalizedString("Foil Stamping", "Ražba fólií"),
    finishType = FinishType.FoilStamping,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "Metallic or colored foil pressed onto the surface using heat and pressure. Available in gold, silver, copper, rose gold, and holographic. Creates an eye-catching luxurious accent.",
      "Kovová nebo barevná fólie vtlačená na povrch pomocí tepla a tlaku. K dispozici ve zlaté, stříbrné, měděné, růžovozlaté a holografické. Vytváří poutavý luxusní akcent.",
    )),
  )

  val dieCut: Finish = Finish(
    id = dieCutId,
    name = LocalizedString("Die Cut", "Výsek"),
    finishType = FinishType.DieCut,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Custom shape cutting using a metal die. Allows creating unique product shapes beyond standard rectangles — rounded edges, windows, or custom contours.",
      "Řezání do vlastního tvaru pomocí kovové formy. Umožňuje vytvořit jedinečné tvary produktů mimo standardní obdélníky — zaoblené hrany, okénka nebo vlastní kontury.",
    )),
  )

  val varnish: Finish = Finish(
    id = varnishId,
    name = LocalizedString("Spot Varnish", "Parciální lak"),
    finishType = FinishType.Varnish,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "Selective glossy coating applied to specific areas (logos, images, text) to create contrast against a matte background. Adds depth and visual interest.",
      "Selektivní lesklý lak aplikovaný na konkrétní oblasti (loga, obrázky, text) pro vytvoření kontrastu oproti matnému pozadí. Přidává hloubku a vizuální zajímavost.",
    )),
  )

  val softTouchCoating: Finish = Finish(
    id = softTouchCoatingId,
    name = LocalizedString("Soft Touch Coating", "Soft touch laminace"),
    finishType = FinishType.SoftTouchCoating,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Luxurious velvet-like coating that feels like suede. Creates an ultra-premium tactile experience. Subdues colors slightly for an elegant, sophisticated look.",
      "Luxusní sametový povlak, který na dotek připomíná semiš. Vytváří ultra-prémiový hmatový zážitek. Mírně tlumí barvy pro elegantní, sofistikovaný vzhled.",
    )),
  )

  val aqueousCoating: Finish = Finish(
    id = aqueousCoatingId,
    name = LocalizedString("Aqueous Coating", "Disperzní lak"),
    finishType = FinishType.AqueousCoating,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Water-based protective coating applied during printing. Eco-friendly option that provides basic scuff protection and enhances print quality. Fast-drying and recyclable.",
      "Ochranný lak na vodní bázi aplikovaný během tisku. Ekologická volba, která poskytuje základní ochranu proti oděru a zlepšuje kvalitu tisku. Rychleschnoucí a recyklovatelný.",
    )),
  )

  val debossing: Finish = Finish(
    id = debossingId,
    name = LocalizedString("Debossing", "Slepotisk do hloubky"),
    finishType = FinishType.Debossing,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "Pressed indentation creating a sunken relief in the paper. The opposite of embossing — pushes the design into the material for a subtle, elegant tactile effect.",
      "Vtlačený otisk vytvářející zapuštěný reliéf v papíru. Opak slepotisku — vtlačuje design do materiálu pro jemný, elegantní hmatový efekt.",
    )),
  )

  val scoring: Finish = Finish(
    id = scoringId,
    name = LocalizedString("Scoring", "Bigování"),
    finishType = FinishType.Scoring,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Creased line pressed into the paper to enable clean, precise folding without cracking. Essential for thick paper stocks and folded products like brochures.",
      "Rýha vtlačená do papíru pro čisté, přesné skládání bez praskání. Nezbytné pro silné papíry a skládané produkty jako brožury.",
    )),
  )

  val perforation: Finish = Finish(
    id = perforationId,
    name = LocalizedString("Perforation", "Perforace"),
    finishType = FinishType.Perforation,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Line of tiny holes punched into the paper allowing easy tearing along a straight line. Used for tear-off coupons, tickets, and response cards.",
      "Řada drobných otvorů vyražených do papíru umožňující snadné odtržení podél přímky. Používá se pro odtrhávací kupóny, vstupenky a odpovědní karty.",
    )),
  )

  val roundCorners: Finish = Finish(
    id = roundCornersId,
    name = LocalizedString("Round Corners", "Zaoblené rohy"),
    finishType = FinishType.RoundCorners,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Rounded corner cutting for a modern, friendly look. Prevents dog-earing and gives a polished appearance. Configurable corner count (1–4) and radius.",
      "Zaoblení rohů pro moderní, přátelský vzhled. Zabraňuje ohýbání a dodává uhlazenou podobu. Konfigurovatelný počet rohů (1–4) a poloměr.",
    )),
  )

  val grommets: Finish = Finish(
    id = grommetsId,
    name = LocalizedString("Grommets", "Průchodky"),
    finishType = FinishType.Grommets,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Metal eyelets punched along the edges for hanging banners with hooks, ropes, or zip ties. Spacing is configurable. Essential for large-format outdoor displays.",
      "Kovové průchodky vyražené podél okrajů pro zavěšení bannerů pomocí háčků, lan nebo stahovacích pásků. Rozestupy jsou konfigurovatelné. Nezbytné pro velkoformátové venkovní displeje.",
    )),
  )

  val kissCut: Finish = Finish(
    id = kissCutId,
    name = LocalizedString("Kiss Cut", "Výsek bez podkladu"),
    finishType = FinishType.KissCut,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Precision cutting through the top layer only, leaving the backing sheet intact. Ideal for sticker sheets where individual stickers peel off easily.",
      "Přesné řezání pouze skrze horní vrstvu, přičemž podkladový arch zůstává neporušený. Ideální pro samolepicí archy, kde se jednotlivé samolepky snadno odlepují.",
    )),
  )

  val overlamination: Finish = Finish(
    id = overlaminationId,
    name = LocalizedString("Overlamination", "Ochranná laminace"),
    finishType = FinishType.Overlamination,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "Additional protective laminate layer for large-format prints. Shields against UV fading, scratches, and moisture. Extends the life of outdoor graphics significantly.",
      "Dodatečná ochranná laminátová vrstva pro velkoformátové tisky. Chrání před UV vyblednutím, poškrábáním a vlhkostí. Výrazně prodlužuje životnost venkovní grafiky.",
    )),
  )

  // --- Promotional Finishes ---
  val heatPress: Finish = Finish(
    id = heatPressId,
    name = LocalizedString("Heat Press Transfer", "Přenos tepelným lisem"),
    finishType = FinishType.Mounting,
    side = FinishSide.Front,
    description = Some(LocalizedString("Design transferred via heat press onto garment", "Design přenesený tepelným lisem na textil")),
  )

  val labelPrint: Finish = Finish(
    id = labelPrintId,
    name = LocalizedString("Label / Tag Printing", "Tisk štítků / visaček"),
    finishType = FinishType.Numbering,
    side = FinishSide.Back,
    description = Some(LocalizedString("Custom labels sewn or printed on collar/hem", "Vlastní štítky všité nebo potištěné na límci/lemu")),
  )

  val foldBag: Finish = Finish(
    id = foldBagId,
    name = LocalizedString("Fold & Bag Packaging", "Složení a balení do sáčku"),
    finishType = FinishType.Binding,
    side = FinishSide.Both,
    description = Some(LocalizedString("Individual folding and polybag packaging", "Individuální složení a balení do polyethylenového sáčku")),
  )

  val mylarOverlay: Finish = Finish(
    id = mylarOverlayId,
    name = LocalizedString("Mylar Film Overlay", "Mylarová fólie"),
    finishType = FinishType.Overlamination,
    side = FinishSide.Front,
    description = Some(LocalizedString("Protective clear film over printed design", "Ochranná průhledná fólie přes potisk")),
  )

  val safetyPin: Finish = Finish(
    id = safetyPinId,
    name = LocalizedString("Safety Pin Back", "Zadní špendlík"),
    finishType = FinishType.Mounting,
    side = FinishSide.Back,
    description = Some(LocalizedString("Standard safety pin mechanism", "Standardní zavírací špendlík")),
  )

  val magnetBack: Finish = Finish(
    id = magnetBackId,
    name = LocalizedString("Magnet Back", "Magnetické uchycení"),
    finishType = FinishType.Mounting,
    side = FinishSide.Back,
    description = Some(LocalizedString("Magnetic backing instead of pin", "Magnetické uchycení místo špendlíku")),
  )

  val bottleOpener: Finish = Finish(
    id = bottleOpenerId,
    name = LocalizedString("Bottle Opener Back", "Otvírák na lahve"),
    finishType = FinishType.Mounting,
    side = FinishSide.Back,
    description = Some(LocalizedString("Dual-purpose badge with bottle opener", "Dvojúčelový odznak s otvírákem na lahve")),
  )

  val dishwasherCoat: Finish = Finish(
    id = dishwasherCoatId,
    name = LocalizedString("Dishwasher-Safe Coating", "Nátěr odolný myčce"),
    finishType = FinishType.Overlamination,
    side = FinishSide.Both,
    description = Some(LocalizedString("Protective coating for durability in dishwashers", "Ochranný nátěr pro odolnost v myčce")),
  )

  val giftBox: Finish = Finish(
    id = giftBoxId,
    name = LocalizedString("Gift Box Packaging", "Dárková krabička"),
    finishType = FinishType.Binding,
    side = FinishSide.Both,
    description = Some(LocalizedString("Individual gift box for each item", "Individuální dárková krabička pro každý kus")),
  )

  val glossyGlaze: Finish = Finish(
    id = glossyGlazeId,
    name = LocalizedString("Glossy Ceramic Glaze", "Lesklá keramická glazura"),
    finishType = FinishType.UVCoating,
    side = FinishSide.Both,
    description = Some(LocalizedString("High-gloss ceramic glaze finish", "Vysokolesklá keramická glazura")),
  )

  val embroideryFinish: Finish = Finish(
    id = embroideryId,
    name = LocalizedString("Embroidery", "Výšivka"),
    finishType = FinishType.Embroidery,
    side = FinishSide.Front,
    description = Some(LocalizedString("Thread-based logo/design application", "Aplikace loga/designu výšivkou")),
  )

  val reinforcedHandles: Finish = Finish(
    id = reinforcedHandlesId,
    name = LocalizedString("Reinforced Handles", "Zpevněná ucha"),
    finishType = FinishType.Binding,
    side = FinishSide.Both,
    description = Some(LocalizedString("Double-stitched handles for durability", "Dvojitě prošitá ucha pro odolnost")),
  )

  // --- Roll-Up Materials ---
  val rollUpBannerFilm: Material = Material(
    id = rollUpBannerFilmId,
    name = LocalizedString("Polyester Banner Film 510gsm", "Polyesterová fólie pro roll-up 510g"),
    family = MaterialFamily.Fabric,
    weight = None,
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.SmoothSurface),
    description = Some(LocalizedString(
      "Heavy-duty polyester film designed specifically for roll-up banner displays. Smooth, non-curl surface with excellent ink adhesion for vibrant graphics.",
      "Odolná polyesterová fólie navržená speciálně pro roll-up bannerové displeje. Hladký, nekroutivý povrch s vynikající přilnavostí inkoustu pro sytou grafiku.",
    )),
  )

  val rollUpStandEconomy: Material = Material(
    id = rollUpStandEconomyId,
    name = LocalizedString("Roll-Up Stand Economy", "Roll-up stojánek Economy"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set.empty,
    description = Some(LocalizedString(
      "Budget-friendly retractable banner stand. Lightweight aluminium construction (~2 kg), basic snap-rail top bar. Suitable for indoor use, short-term events, and single-use promotions. Typically lasts 10–20 setups.",
      "Ekonomický zatažitelný bannerový stojan. Lehká hliníková konstrukce (~2 kg), základní zacvakávací horní lišta. Vhodný pro vnitřní použití, krátkodobé akce a jednorázové propagace. Typicky vydrží 10–20 rozložení.",
    )),
  )

  val rollUpStandPremium: Material = Material(
    id = rollUpStandPremiumId,
    name = LocalizedString("Roll-Up Stand Premium", "Roll-up stojánek Premium"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set.empty,
    description = Some(LocalizedString(
      "Professional-grade retractable banner stand. Wide base with adjustable feet for stability, tensioned top rail for a flat banner surface. Built for frequent use at trade shows and permanent displays. Lasts 100+ setups with interchangeable cassettes.",
      "Profesionální zatažitelný bannerový stojan. Široká základna s nastavitelnými nožkami pro stabilitu, napínací horní lišta pro rovný povrch banneru. Vyroben pro časté použití na veletrzích a permanentní displeje. Vydrží 100+ rozložení s vyměnitelnými kazetami.",
    )),
  )

  // --- Calendar Protective Cover Materials ---
  val transparentPlasticCover: Material = Material(
    id = transparentPlasticCoverId,
    name = LocalizedString("Transparent Plastic Cover", "Průhledný plastový obal"),
    family = MaterialFamily.Plastic,
    weight = None,
    properties = Set(MaterialProperty.Transparent, MaterialProperty.SmoothSurface),
    description = Some(LocalizedString(
      "Clear transparent PVC or PP plastic cover for calendar front protection. Protects the printed cover while keeping it visible.",
      "Čirý průhledný PVC nebo PP plastový obal pro ochranu přední strany kalendáře. Chrání potištěnou obálku a zároveň ji ponechává viditelnou.",
    )),
  )

  val cardboard350gWhite: Material = Material(
    id = cardboard350gWhiteId,
    name = LocalizedString("Cardboard 350gsm White", "Karton 350g bílý"),
    family = MaterialFamily.Cardboard,
    weight = Some(PaperWeight.unsafe(350)),
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Sturdy 350gsm white cardboard for calendar back cover. Provides rigid support and a clean appearance.",
      "Pevný 350g bílý karton pro zadní desku kalendáře. Poskytuje tuhý podklad a čistý vzhled.",
    )),
  )

  val cardboard350gBlack: Material = Material(
    id = cardboard350gBlackId,
    name = LocalizedString("Cardboard 350gsm Black", "Karton 350g černý"),
    family = MaterialFamily.Cardboard,
    weight = Some(PaperWeight.unsafe(350)),
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Sturdy 350gsm black cardboard for calendar back cover. Premium look with rigid support.",
      "Pevný 350g černý karton pro zadní desku kalendáře. Prémiový vzhled s tuhým podkladem.",
    )),
  )

  val cardboard350gGrey: Material = Material(
    id = cardboard350gGreyId,
    name = LocalizedString("Cardboard 350gsm Grey", "Karton 350g šedý"),
    family = MaterialFamily.Cardboard,
    weight = Some(PaperWeight.unsafe(350)),
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Sturdy 350gsm grey cardboard for calendar back cover. Neutral appearance with rigid support.",
      "Pevný 350g šedý karton pro zadní desku kalendáře. Neutrální vzhled s tuhým podkladem.",
    )),
  )

  val cardboard350gBrown: Material = Material(
    id = cardboard350gBrownId,
    name = LocalizedString("Cardboard 350gsm Brown", "Karton 350g hnědý"),
    family = MaterialFamily.Cardboard,
    weight = Some(PaperWeight.unsafe(350)),
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Sturdy 350gsm brown cardboard (kraft-like) for calendar back cover. Eco-friendly appearance with rigid support.",
      "Pevný 350g hnědý karton (kraftového typu) pro zadní desku kalendáře. Ekologický vzhled s tuhým podkladem.",
    )),
  )

  // --- Binding Color Materials ---
  // Plastic O-Ring binding colors
  val bindingPlasticBlack: Material = Material(
    id = bindingPlasticBlackId,
    name = LocalizedString("Plastic O-Ring Black", "Plastová kroužková vazba - černá"),
    family = MaterialFamily.Plastic,
    weight = None,
    properties = Set.empty,
  )

  val bindingPlasticWhite: Material = Material(
    id = bindingPlasticWhiteId,
    name = LocalizedString("Plastic O-Ring White", "Plastová kroužková vazba - bílá"),
    family = MaterialFamily.Plastic,
    weight = None,
    properties = Set.empty,
  )

  val bindingPlasticSilver: Material = Material(
    id = bindingPlasticSilverId,
    name = LocalizedString("Plastic O-Ring Silver", "Plastová kroužková vazba - stříbrná"),
    family = MaterialFamily.Plastic,
    weight = None,
    properties = Set.empty,
  )

  val bindingPlasticBlue: Material = Material(
    id = bindingPlasticBlueId,
    name = LocalizedString("Plastic O-Ring Blue", "Plastová kroužková vazba - modrá"),
    family = MaterialFamily.Plastic,
    weight = None,
    properties = Set.empty,
  )

  val bindingPlasticRed: Material = Material(
    id = bindingPlasticRedId,
    name = LocalizedString("Plastic O-Ring Red", "Plastová kroužková vazba - červená"),
    family = MaterialFamily.Plastic,
    weight = None,
    properties = Set.empty,
  )

  val bindingPlasticClear: Material = Material(
    id = bindingPlasticClearId,
    name = LocalizedString("Plastic O-Ring Clear", "Plastová kroužková vazba - průhledná"),
    family = MaterialFamily.Plastic,
    weight = None,
    properties = Set(MaterialProperty.Transparent),
  )

  // Metal wire binding colors
  val bindingMetalBlack: Material = Material(
    id = bindingMetalBlackId,
    name = LocalizedString("Metal Wire Black", "Drátěná vazba - černá"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set.empty,
  )

  val bindingMetalSilver: Material = Material(
    id = bindingMetalSilverId,
    name = LocalizedString("Metal Wire Silver", "Drátěná vazba - stříbrná"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set.empty,
  )

  val bindingMetalWhite: Material = Material(
    id = bindingMetalWhiteId,
    name = LocalizedString("Metal Wire White", "Drátěná vazba - bílá"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set.empty,
  )

  // --- Promotional Materials: T-Shirts ---
  val cottonTshirt150: Material = Material(
    id = cottonTshirt150Id,
    name = LocalizedString("Cotton T-Shirt 150gsm", "Bavlněné tričko 150g"),
    family = MaterialFamily.Fabric,
    weight = Some(PaperWeight.unsafe(150)),
    properties = Set(MaterialProperty.Recyclable),
  )

  val cottonTshirt180: Material = Material(
    id = cottonTshirt180Id,
    name = LocalizedString("Cotton T-Shirt 180gsm", "Bavlněné tričko 180g"),
    family = MaterialFamily.Fabric,
    weight = Some(PaperWeight.unsafe(180)),
    properties = Set(MaterialProperty.Recyclable),
  )

  val polyesterTshirt: Material = Material(
    id = polyesterTshirtId,
    name = LocalizedString("Polyester T-Shirt", "Polyesterové tričko"),
    family = MaterialFamily.Fabric,
    weight = Some(PaperWeight.unsafe(140)),
    properties = Set(MaterialProperty.WaterResistant),
  )

  val cottonPolyBlend: Material = Material(
    id = cottonPolyBlendId,
    name = LocalizedString("Cotton-Polyester Blend T-Shirt", "Směsové tričko bavlna-polyester"),
    family = MaterialFamily.Fabric,
    weight = Some(PaperWeight.unsafe(160)),
    properties = Set(MaterialProperty.Recyclable),
  )

  val organicCottonTshirt: Material = Material(
    id = organicCottonTshirtId,
    name = LocalizedString("Organic Cotton T-Shirt 180gsm", "Bio bavlněné tričko 180g"),
    family = MaterialFamily.Fabric,
    weight = Some(PaperWeight.unsafe(180)),
    properties = Set(MaterialProperty.Recyclable),
  )

  // --- Promotional Materials: Eco Bags ---
  val cottonCanvasBag: Material = Material(
    id = cottonCanvasBagId,
    name = LocalizedString("Cotton Canvas 220gsm", "Bavlněné plátno 220g"),
    family = MaterialFamily.Fabric,
    weight = Some(PaperWeight.unsafe(220)),
    properties = Set(MaterialProperty.Recyclable),
  )

  val organicCottonBag: Material = Material(
    id = organicCottonBagId,
    name = LocalizedString("Organic Cotton Bag 180gsm", "Bio bavlněná taška 180g"),
    family = MaterialFamily.Fabric,
    weight = Some(PaperWeight.unsafe(180)),
    properties = Set(MaterialProperty.Recyclable),
  )

  val recycledPetBag: Material = Material(
    id = recycledPetBagId,
    name = LocalizedString("Recycled PET Bag", "Recyklovaná PET taška"),
    family = MaterialFamily.Fabric,
    weight = Some(PaperWeight.unsafe(150)),
    properties = Set(MaterialProperty.Recyclable, MaterialProperty.WaterResistant),
  )

  val juteBag: Material = Material(
    id = juteBagId,
    name = LocalizedString("Jute / Burlap Bag", "Jutová taška"),
    family = MaterialFamily.Fabric,
    weight = Some(PaperWeight.unsafe(300)),
    properties = Set(MaterialProperty.Recyclable, MaterialProperty.Textured),
  )

  val nonWovenPpBag: Material = Material(
    id = nonWovenPpBagId,
    name = LocalizedString("Non-Woven Polypropylene Bag", "Netkaná PP taška"),
    family = MaterialFamily.Fabric,
    weight = Some(PaperWeight.unsafe(80)),
    properties = Set(MaterialProperty.WaterResistant),
  )

  // --- Promotional Materials: Pin Badges ---
  val tinplateBadge: Material = Material(
    id = tinplateBadgeId,
    name = LocalizedString("Tinplate Badge Blank", "Plechový polotovar na odznak"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set(MaterialProperty.SmoothSurface),
  )

  val acrylicBadge: Material = Material(
    id = acrylicBadgeId,
    name = LocalizedString("Acrylic Badge Blank", "Akrylátový polotovar na odznak"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.Transparent),
  )

  val woodenBadge: Material = Material(
    id = woodenBadgeId,
    name = LocalizedString("Wooden Badge Blank", "Dřevěný polotovar na odznak"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable),
  )

  // --- Promotional Materials: Cups & Mugs ---
  val ceramicMugWhite: Material = Material(
    id = ceramicMugWhiteId,
    name = LocalizedString("White Ceramic Mug 330ml", "Bílý keramický hrnek 330ml"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set(MaterialProperty.SmoothSurface),
  )

  val ceramicMugColored: Material = Material(
    id = ceramicMugColoredId,
    name = LocalizedString("Colored Ceramic Mug 330ml", "Barevný keramický hrnek 330ml"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set(MaterialProperty.SmoothSurface),
  )

  val magicMug: Material = Material(
    id = magicMugId,
    name = LocalizedString("Magic Color-Changing Mug 330ml", "Magický měnící hrnek 330ml"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set(MaterialProperty.SmoothSurface),
  )

  val stainlessTravelMug: Material = Material(
    id = stainlessTravelMugId,
    name = LocalizedString("Stainless Steel Travel Mug 450ml", "Nerezový cestovní hrnek 450ml"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.WaterResistant),
  )

  val enamelMug: Material = Material(
    id = enamelMugId,
    name = LocalizedString("Enamel Mug 350ml", "Smaltovaný hrnek 350ml"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set(MaterialProperty.SmoothSurface),
  )

  val glassMug: Material = Material(
    id = glassMugId,
    name = LocalizedString("Glass Mug 300ml", "Skleněný hrnek 300ml"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set(MaterialProperty.SmoothSurface, MaterialProperty.Transparent),
  )

  // --- Reusable Material ID Sets ---
  private val allCoatedGlossyIds: Set[MaterialId] = Set(
    coatedGlossy90gsmId, coatedGlossy115gsmId, coatedGlossy130gsmId,
    coatedGlossy150gsmId, coatedGlossy170gsmId, coatedGlossy200gsmId,
    coatedGlossy250gsmId, coatedGlossy350gsmId,
  )

  private val allCoatedMatteIds: Set[MaterialId] = Set(
    coatedMatte90gsmId, coatedMatte115gsmId, coatedMatte130gsmId,
    coatedMatte150gsmId, coatedMatte170gsmId, coatedMatte200gsmId,
    coatedMatte250gsmId, coatedMatte300gsmId, coatedMatte350gsmId,
  )

  private val heavyCoatedGlossyIds: Set[MaterialId] = Set(
    coatedGlossy250gsmId, coatedGlossy350gsmId,
  )

  private val heavyCoatedMatteIds: Set[MaterialId] = Set(
    coatedMatte250gsmId, coatedMatte300gsmId, coatedMatte350gsmId,
  )

  private val mediumHeavyCoatedGlossyIds: Set[MaterialId] = Set(
    coatedGlossy170gsmId, coatedGlossy200gsmId,
    coatedGlossy250gsmId, coatedGlossy350gsmId,
  )

  private val mediumHeavyCoatedMatteIds: Set[MaterialId] = Set(
    coatedMatte170gsmId, coatedMatte200gsmId,
    coatedMatte250gsmId, coatedMatte300gsmId, coatedMatte350gsmId,
  )

  // --- Categories ---
  val businessCards: ProductCategory = ProductCategory(
    id = businessCardsId,
    name = LocalizedString("Business Cards", "Vizitky"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(coated300gsmId, uncoatedBondId, kraftId, yupoId, cottonId, coatedSilk250gsmId) ++
      heavyCoatedGlossyIds ++ heavyCoatedMatteIds,
      allowedFinishIds = Set(
        matteLaminationId, glossLaminationId, uvCoatingId, embossingId,
        foilStampingId, softTouchCoatingId, roundCornersId,
      ),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(digitalId, letterpressId),
    description = Some(LocalizedString(
      "Standard and premium business cards. Choose from a wide range of papers (coated, uncoated, cotton, kraft) and finishing options including lamination, embossing, and foil stamping.",
      "Standardní a prémiové vizitky. Vyberte si z široké nabídky papírů (křídový, nenatíraný, bavlněný, kraftový) a dokončovacích úprav včetně laminace, slepotisku a ražby fólií.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-bc-basic"),
        name = LocalizedString("Basic", "Základní"),
        description = Some(LocalizedString(
          "Coated 300gsm, 4+0 CMYK, 85×55 mm, 100 pcs",
          "Křídový 300g, 4+0 CMYK, 85×55 mm, 100 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coated300gsmId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(85, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-bc-premium"),
        name = LocalizedString("Premium", "Prémiové"),
        description = Some(LocalizedString(
          "Coated 350gsm, 4+4 CMYK, matte lamination + round corners, 85×55 mm, 100 pcs",
          "Křídový 350g, 4+4 CMYK, matná laminace + zaoblené rohy, 85×55 mm, 100 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coatedMatte350gsmId,
          inkConfiguration = InkConfiguration.cmyk4_4,
          finishSelections = List(
            FinishSelection(matteLaminationId),
            FinishSelection(roundCornersId, Some(FinishParameters.RoundCornersParams(4, 3))),
          ),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(85, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
        ),
      ),
    ),
  )

  val flyers: ProductCategory = ProductCategory(
    id = flyersId,
    name = LocalizedString("Flyers", "Letáky"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(coated300gsmId, uncoatedBondId) ++
      allCoatedGlossyIds ++ allCoatedMatteIds,
      allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId, varnishId, aqueousCoatingId, roundCornersId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.Orientation),
    allowedPrintingMethodIds = Set(digitalId),
    description = Some(LocalizedString(
      "Single-sheet promotional flyers in various sizes. Available in a wide range of paper weights from lightweight 90gsm to sturdy 350gsm. Landscape or portrait orientation.",
      "Jednostránkové propagační letáky v různých velikostech. K dispozici v široké škále gramáží od lehkých 90g po pevné 350g. Na výšku nebo na šířku.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-flyers-standard"),
        name = LocalizedString("Standard", "Standardní"),
        description = Some(LocalizedString(
          "Glossy 130gsm, 4+0 CMYK, A5, 500 pcs",
          "Lesklý 130g, 4+0 CMYK, A5, 500 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coatedGlossy130gsmId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(148, 210)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.OrientationSpec(Orientation.Portrait),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-flyers-premium"),
        name = LocalizedString("Premium", "Prémiové"),
        description = Some(LocalizedString(
          "Matte 250gsm, 4+4 CMYK, matte lamination, A5, 500 pcs",
          "Matný 250g, 4+4 CMYK, matná laminace, A5, 500 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coatedMatte250gsmId,
          inkConfiguration = InkConfiguration.cmyk4_4,
          finishSelections = List(FinishSelection(matteLaminationId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(148, 210)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.OrientationSpec(Orientation.Portrait),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-flyers-lightweight"),
        name = LocalizedString("Lightweight", "Lehké"),
        description = Some(LocalizedString(
          "Glossy 90gsm, 4+0 CMYK, A5, 1000 pcs",
          "Lesklý 90g, 4+0 CMYK, A5, 1000 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coatedGlossy90gsmId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(148, 210)),
          SpecValue.QuantitySpec(Quantity.unsafe(1000)),
          SpecValue.OrientationSpec(Orientation.Portrait),
        ),
      ),
    ),
  )

  val brochures: ProductCategory = ProductCategory(
    id = brochuresId,
    name = LocalizedString("Brochures", "Skládaný letáky"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(coated300gsmId, uncoatedBondId, coatedSilk250gsmId) ++
      allCoatedGlossyIds ++ allCoatedMatteIds,
      allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId, scoringId, roundCornersId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.FoldType),
    allowedPrintingMethodIds = Set(digitalId),
    description = Some(LocalizedString(
      "Folded brochures and leaflets. Choose from multiple fold types (bi-fold, tri-fold, Z-fold, gate fold). Scoring is included for clean folds on heavier stocks.",
      "Skládané brožury a letáky. Vyberte si z více typů skládání (na půl, na třetiny, Z-sklad, dvoudveřový sklad). Bigování je zahrnuto pro čisté sklady na silnějších papírech.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-brochures-standard"),
        name = LocalizedString("Standard", "Standardní"),
        description = Some(LocalizedString(
          "Glossy 150gsm, 4+4 CMYK, A4 tri-fold, 250 pcs",
          "Lesklý 150g, 4+4 CMYK, A4 na třetiny, 250 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coatedGlossy150gsmId,
          inkConfiguration = InkConfiguration.cmyk4_4,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(210, 297)),
          SpecValue.QuantitySpec(Quantity.unsafe(250)),
          SpecValue.FoldTypeSpec(FoldType.Tri),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-brochures-bifold"),
        name = LocalizedString("Bi-Fold", "Na půl"),
        description = Some(LocalizedString(
          "Matte 200gsm, 4+4 CMYK, A4 bi-fold, 250 pcs",
          "Matný 200g, 4+4 CMYK, A4 na půl, 250 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coatedMatte200gsmId,
          inkConfiguration = InkConfiguration.cmyk4_4,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(210, 297)),
          SpecValue.QuantitySpec(Quantity.unsafe(250)),
          SpecValue.FoldTypeSpec(FoldType.Half),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-brochures-zfold"),
        name = LocalizedString("Z-Fold", "Z-sklad"),
        description = Some(LocalizedString(
          "Glossy 170gsm, 4+4 CMYK, A4 Z-fold, 250 pcs",
          "Lesklý 170g, 4+4 CMYK, A4 Z-sklad, 250 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coatedGlossy170gsmId,
          inkConfiguration = InkConfiguration.cmyk4_4,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(210, 297)),
          SpecValue.QuantitySpec(Quantity.unsafe(250)),
          SpecValue.FoldTypeSpec(FoldType.ZFold),
        ),
      ),
    ),
  )

  val banners: ProductCategory = ProductCategory(
    id = bannersId,
    name = LocalizedString("Banners", "Bannery"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(vinylId),
      allowedFinishIds = Set(uvCoatingId, dieCutId, grommetsId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(uvInkjetId),
    description = Some(LocalizedString(
      "Large-format vinyl banners for outdoor and indoor use. Printed with UV-curable inks for weather resistance. Optional grommets for hanging and die-cutting for custom shapes.",
      "Velkoformátové vinylové bannery pro venkovní i vnitřní použití. Tištěné UV vytvrzovanými inkousty pro odolnost proti povětrnostním vlivům. Volitelné průchodky pro zavěšení a výsek pro vlastní tvary.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-banners-standard"),
        name = LocalizedString("Standard", "Standardní"),
        description = Some(LocalizedString(
          "Vinyl, 4+0 CMYK, 1000×2000 mm, 1 pc",
          "Vinyl, 4+0 CMYK, 1000×2000 mm, 1 ks",
        )),
        printingMethodId = uvInkjetId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = vinylId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(1000, 2000)),
          SpecValue.QuantitySpec(Quantity.unsafe(1)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-banners-outdoor"),
        name = LocalizedString("Outdoor with Grommets", "Exteriérový s průchodkami"),
        description = Some(LocalizedString(
          "Vinyl, 4+0 CMYK, UV coating + grommets, 1500×3000 mm, 1 pc",
          "Vinyl, 4+0 CMYK, UV lak + průchodky, 1500×3000 mm, 1 ks",
        )),
        printingMethodId = uvInkjetId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = vinylId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(
            FinishSelection(uvCoatingId),
            FinishSelection(grommetsId),
          ),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(1500, 3000)),
          SpecValue.QuantitySpec(Quantity.unsafe(1)),
        ),
      ),
    ),
  )

  val packaging: ProductCategory = ProductCategory(
    id = packagingId,
    name = LocalizedString("Packaging", "Krabice a obaly"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(kraftId, corrugatedId, yupoId),
      allowedFinishIds = Set(matteLaminationId, uvCoatingId, embossingId, foilStampingId, dieCutId, scoringId, perforationId, debossingId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(digitalId),
    description = Some(LocalizedString(
      "Custom packaging boxes and wraps. Available in kraft, corrugated, and synthetic materials. Supports die-cutting for custom box shapes, scoring for fold lines, and premium finishes.",
      "Zakázkové balicí krabice a obaly. K dispozici v kraftovém, vlnitém a syntetickém materiálu. Podporuje výsek pro vlastní tvary krabic, bigování pro linie ohybu a prémiové dokončení.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-packaging-standard"),
        name = LocalizedString("Standard Kraft", "Standardní kraft"),
        description = Some(LocalizedString(
          "Kraft, 4+0 CMYK, 300×200 mm, 100 pcs",
          "Kraft, 4+0 CMYK, 300×200 mm, 100 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = kraftId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(300, 200)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-packaging-premium"),
        name = LocalizedString("Premium Die-Cut", "Prémiové s výsekem"),
        description = Some(LocalizedString(
          "Kraft, 4+0 CMYK, die-cut + scoring, 300×200 mm, 50 pcs",
          "Kraft, 4+0 CMYK, výsek + bigování, 300×200 mm, 50 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = kraftId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(
            FinishSelection(dieCutId),
            FinishSelection(scoringId),
          ),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(300, 200)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
        ),
      ),
    ),
  )

  val booklets: ProductCategory = ProductCategory(
    id = bookletsId,
    name = LocalizedString("Booklets", "Brožury - Katalogy"),
    components = List(
      ComponentTemplate(
        ComponentRole.Cover,
        allowedMaterialIds = Set(coated300gsmId, coatedSilk250gsmId) ++
          allCoatedGlossyIds ++ allCoatedMatteIds,
        allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId, roundCornersId),
      ),
      ComponentTemplate(
        ComponentRole.Body,
        allowedMaterialIds = Set(coated300gsmId, uncoatedBondId, uncoatedBondId, coatedSilk250gsmId) ++
          allCoatedGlossyIds ++ allCoatedMatteIds,
        allowedFinishIds = Set(perforationId),
      ),
      ComponentTemplate(
        role = ComponentRole.Binding,
        allowedMaterialIds = allBindingMaterialIds,
        allowedFinishIds = Set.empty,
        optional = true,
      ),
    ),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.Pages, SpecKind.BindingMethod),
    allowedPrintingMethodIds = Set(digitalId),
    description = Some(LocalizedString(
      "Multi-page booklets and catalogs with separate cover and body components. Choose different materials for the cover and inner pages. Binding options include saddle stitch, perfect binding, and wire-o.",
      "Vícestránkové brožury a katalogy se samostatnou obálkou a vnitřními stranami. Vyberte různé materiály pro obálku a vnitřní strany. Možnosti vazby zahrnují V-vazbu, lepenou vazbu a kroužkovou vazbu.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-booklets-standard"),
        name = LocalizedString("Saddle Stitch", "V-vazba"),
        description = Some(LocalizedString(
          "Glossy 250gsm cover + 130gsm body, 4+4 CMYK, A4, saddle stitch, 8 pages, 100 pcs",
          "Lesklý 250g obálka + 130g tělo, 4+4 CMYK, A4, V-vazba, 8 stran, 100 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(
          ComponentPreset(
            role = ComponentRole.Cover,
            materialId = coatedGlossy250gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
          ),
          ComponentPreset(
            role = ComponentRole.Body,
            materialId = coatedGlossy130gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
          ),
        ),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(210, 297)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
          SpecValue.PagesSpec(8),
          SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-booklets-perfect"),
        name = LocalizedString("Perfect Binding", "Lepená vazba"),
        description = Some(LocalizedString(
          "Matte 300gsm cover + 130gsm body, 4+4 CMYK, A4, perfect binding, 48 pages, 50 pcs",
          "Matný 300g obálka + 130g tělo, 4+4 CMYK, A4, lepená vazba, 48 stran, 50 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(
          ComponentPreset(
            role = ComponentRole.Cover,
            materialId = coatedMatte300gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
            finishSelections = List(FinishSelection(matteLaminationId)),
          ),
          ComponentPreset(
            role = ComponentRole.Body,
            materialId = coatedMatte130gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
          ),
        ),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(210, 297)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
          SpecValue.PagesSpec(48),
          SpecValue.BindingMethodSpec(BindingMethod.PerfectBinding),
        ),
      ),
    ),
  )

  val calendars: ProductCategory = ProductCategory(
    id = calendarsId,
    name = LocalizedString("Calendars", "Kalendáře"),
    components = List(
      ComponentTemplate(
        ComponentRole.Cover,
        allowedMaterialIds = Set(coated300gsmId, coatedSilk250gsmId) ++
        mediumHeavyCoatedGlossyIds ++ mediumHeavyCoatedMatteIds,
    allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId),
      ),
      ComponentTemplate(
        ComponentRole.Body,
        allowedMaterialIds = Set(coated300gsmId, coatedSilk250gsmId, uncoatedBondId) ++
          mediumHeavyCoatedGlossyIds ++ mediumHeavyCoatedMatteIds,
        allowedFinishIds = Set(perforationId),
      ),
      ComponentTemplate(
        role = ComponentRole.FrontCover,
        allowedMaterialIds = Set(transparentPlasticCoverId),
        allowedFinishIds = Set.empty,
        optional = true,
      ),
      ComponentTemplate(
        role = ComponentRole.BackCover,
        allowedMaterialIds = Set(cardboard350gWhiteId, cardboard350gBlackId, cardboard350gGreyId, cardboard350gBrownId),
        allowedFinishIds = Set.empty,
        optional = true,
      ),
      ComponentTemplate(
        role = ComponentRole.Binding,
        allowedMaterialIds = allBindingMaterialIds,
        allowedFinishIds = Set.empty,
        optional = true,
      ),
    ),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.Pages, SpecKind.BindingMethod),
    allowedPrintingMethodIds = Set(digitalId),
    description = Some(LocalizedString(
      "Wall and desk calendars with cover and monthly pages. Separate cover and body components allow different paper choices. Metal wire binding is most common.",
      "Nástěnné a stolní kalendáře s obálkou a měsíčními stránkami. Samostatná obálka a vnitřní strany umožňují různé volby papíru. Drátěná vazba je nejběžnější.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-calendars-wall"),
        name = LocalizedString("Wall Calendar", "Nástěnný kalendář"),
        description = Some(LocalizedString(
          "Glossy 250gsm cover + 170gsm body, 4+4 CMYK, A4, metal wire, 28 pages, 50 pcs, with transparent front cover and white cardboard back",
          "Lesklý 250g obálka + 170g tělo, 4+4 CMYK, A4, drátěná vazba, 28 stran, 50 ks, s průhlednou přední deskou a bílým kartonovým zadním dílem",
        )),
        printingMethodId = digitalId,
        componentPresets = List(
          ComponentPreset(
            role = ComponentRole.Cover,
            materialId = coatedGlossy250gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
          ),
          ComponentPreset(
            role = ComponentRole.Body,
            materialId = coatedGlossy170gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
          ),
          ComponentPreset(
            role = ComponentRole.FrontCover,
            materialId = transparentPlasticCoverId,
            inkConfiguration = InkConfiguration.noInk,
          ),
          ComponentPreset(
            role = ComponentRole.BackCover,
            materialId = cardboard350gWhiteId,
            inkConfiguration = InkConfiguration.noInk,
          ),
          ComponentPreset(
            role = ComponentRole.Binding,
            materialId = bindingMetalSilverId,
            inkConfiguration = InkConfiguration.noInk,
          ),
        ),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(210, 297)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
          SpecValue.PagesSpec(28),
          SpecValue.BindingMethodSpec(BindingMethod.MetalWireBinding),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-calendars-desk"),
        name = LocalizedString("Desk Calendar", "Stolní kalendář"),
        description = Some(LocalizedString(
          "Matte 300gsm cover + 200gsm body, 4+4 CMYK, A5, metal wire, 28 pages, 50 pcs, with transparent front cover and grey cardboard back",
          "Matný 300g obálka + 200g tělo, 4+4 CMYK, A5, drátěná vazba, 28 stran, 50 ks, s průhlednou přední deskou a šedým kartonovým zadním dílem",
        )),
        printingMethodId = digitalId,
        componentPresets = List(
          ComponentPreset(
            role = ComponentRole.Cover,
            materialId = coatedMatte300gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
          ),
          ComponentPreset(
            role = ComponentRole.Body,
            materialId = coatedMatte200gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
          ),
          ComponentPreset(
            role = ComponentRole.FrontCover,
            materialId = transparentPlasticCoverId,
            inkConfiguration = InkConfiguration.noInk,
          ),
          ComponentPreset(
            role = ComponentRole.BackCover,
            materialId = cardboard350gGreyId,
            inkConfiguration = InkConfiguration.noInk,
          ),
          ComponentPreset(
            role = ComponentRole.Binding,
            materialId = bindingMetalBlackId,
            inkConfiguration = InkConfiguration.noInk,
          ),
        ),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(148, 210)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
          SpecValue.PagesSpec(28),
          SpecValue.BindingMethodSpec(BindingMethod.MetalWireBinding),
        ),
      ),
    ),
  )

  private val allMaterialIds: Set[MaterialId] = Set(
    coated300gsmId, uncoatedBondId, kraftId, vinylId, corrugatedId,
    coatedSilk250gsmId, yupoId, adhesiveStockId, cottonId, clearVinylId,
    // Calendar protective covers
    transparentPlasticCoverId, cardboard350gWhiteId, cardboard350gBlackId, cardboard350gGreyId, cardboard350gBrownId,
    // Promotional materials
    // Promotional materials
    cottonTshirt150Id, cottonTshirt180Id, polyesterTshirtId, cottonPolyBlendId, organicCottonTshirtId,
    cottonCanvasBagId, organicCottonBagId, recycledPetBagId, juteBagId, nonWovenPpBagId,
    tinplateBadgeId, acrylicBadgeId, woodenBadgeId,
    ceramicMugWhiteId, ceramicMugColoredId, magicMugId, stainlessTravelMugId, enamelMugId, glassMugId,
  ) ++ allCoatedGlossyIds ++ allCoatedMatteIds ++ allBindingMaterialIds

  private val allFinishIds: Set[FinishId] = Set(
    matteLaminationId, glossLaminationId, uvCoatingId, embossingId,
    foilStampingId, dieCutId, varnishId, softTouchCoatingId, aqueousCoatingId,
    debossingId, scoringId, perforationId, roundCornersId, grommetsId, kissCutId,
    overlaminationId,
    // Promotional finishes
    heatPressId, labelPrintId, foldBagId, mylarOverlayId, safetyPinId, magnetBackId,
    bottleOpenerId, dishwasherCoatId, giftBoxId, glossyGlazeId, embroideryId, reinforcedHandlesId,
  )

  val postcards: ProductCategory = ProductCategory(
    id = postcardsId,
    name = LocalizedString("Postcards", "Pohlednice"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(coated300gsmId, coatedSilk250gsmId, cottonId) ++
        heavyCoatedGlossyIds ++ heavyCoatedMatteIds,
      allowedFinishIds = Set(
        matteLaminationId, glossLaminationId, uvCoatingId, softTouchCoatingId,
        embossingId, foilStampingId, roundCornersId, aqueousCoatingId,
      ),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(offsetId, digitalId),
    description = Some(LocalizedString(
      "Postcards and mailers on thick card stock. Available with both offset and digital printing. Supports premium finishes for a high-end direct mail piece.",
      "Pohlednice a reklamní zásilky na silném kartonu. K dispozici s ofsetovým i digitálním tiskem. Podporuje prémiové dokončení pro vysoce kvalitní poštovní zásilky.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-postcards-standard"),
        name = LocalizedString("Standard", "Standardní"),
        description = Some(LocalizedString(
          "Coated 300gsm, 4+4 CMYK, A6, 200 pcs",
          "Křídový 300g, 4+4 CMYK, A6, 200 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coated300gsmId,
          inkConfiguration = InkConfiguration.cmyk4_4,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(105, 148)),
          SpecValue.QuantitySpec(Quantity.unsafe(200)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-postcards-premium"),
        name = LocalizedString("Premium", "Prémiové"),
        description = Some(LocalizedString(
          "Cotton 300gsm, 4+4 CMYK, soft-touch coating, A6, 100 pcs",
          "Bavlněný 300g, 4+4 CMYK, soft-touch lak, A6, 100 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = cottonId,
          inkConfiguration = InkConfiguration.cmyk4_4,
          finishSelections = List(FinishSelection(softTouchCoatingId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(105, 148)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
        ),
      ),
    ),
  )

  val stickers: ProductCategory = ProductCategory(
    id = stickersId,
    name = LocalizedString("Stickers & Labels", "Samolepky a štítky"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(adhesiveStockId, yupoId, clearVinylId),
      allowedFinishIds = Set(kissCutId, dieCutId, roundCornersId, uvCoatingId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(digitalId, uvInkjetId),
    description = Some(LocalizedString(
      "Custom stickers and product labels on adhesive, synthetic, or clear vinyl stock. Kiss-cut for peel-off sheets or die-cut for individual shapes.",
      "Zakázkové samolepky a produktové štítky na samolepicím, syntetickém nebo průhledném vinylovém materiálu. Výsek bez podkladu pro odlepovací archy nebo výsek pro jednotlivé tvary.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-stickers-standard"),
        name = LocalizedString("Standard", "Standardní"),
        description = Some(LocalizedString(
          "Adhesive stock, 4+0 CMYK, 50×50 mm, 500 pcs",
          "Samolepicí materiál, 4+0 CMYK, 50×50 mm, 500 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = adhesiveStockId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(50, 50)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-stickers-diecut"),
        name = LocalizedString("Die-Cut", "Výsekové"),
        description = Some(LocalizedString(
          "Adhesive stock, 4+0 CMYK, die-cut, 50×50 mm, 500 pcs",
          "Samolepicí materiál, 4+0 CMYK, výsek, 50×50 mm, 500 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = adhesiveStockId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(dieCutId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(50, 50)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-stickers-clear"),
        name = LocalizedString("Clear Vinyl", "Průhledný vinyl"),
        description = Some(LocalizedString(
          "Clear vinyl, 4+0 CMYK, UV inkjet, 50×50 mm, 250 pcs",
          "Průhledný vinyl, 4+0 CMYK, UV inkjet, 50×50 mm, 250 ks",
        )),
        printingMethodId = uvInkjetId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = clearVinylId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(50, 50)),
          SpecValue.QuantitySpec(Quantity.unsafe(250)),
        ),
      ),
    ),
  )

  val rollUps: ProductCategory = ProductCategory(
    id = rollUpsId,
    name = LocalizedString("Roll-Up Banners", "Roll-up bannery"),
    components = List(
      ComponentTemplate(
        ComponentRole.Main,
        allowedMaterialIds = Set(rollUpBannerFilmId),
        allowedFinishIds = Set(overlaminationId),
      ),
      ComponentTemplate(
        ComponentRole.Stand,
        allowedMaterialIds = Set(rollUpStandEconomyId, rollUpStandPremiumId),
        allowedFinishIds = Set.empty,
        optional = true,
      ),
    ),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(uvInkjetId),
    description = Some(LocalizedString(
      "Portable retractable banner displays. Includes a printed banner and optional stand (Economy or Premium). Economy stands are for single-use events; Premium stands are built for repeated trade show use.",
      "Přenosné zatažitelné bannerové displeje. Zahrnuje potištěný banner a volitelný stojan (Economy nebo Premium). Economy stojany jsou pro jednorázové akce; Premium stojany jsou postaveny pro opakované použití na veletrzích.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-rollup-economy"),
        name = LocalizedString("Economy", "Economy"),
        description = Some(LocalizedString(
          "Banner film + economy stand, 4+0 CMYK, 850×2000 mm, 1 pc",
          "Bannerová fólie + economy stojan, 4+0 CMYK, 850×2000 mm, 1 ks",
        )),
        printingMethodId = uvInkjetId,
        componentPresets = List(
          ComponentPreset(
            role = ComponentRole.Main,
            materialId = rollUpBannerFilmId,
            inkConfiguration = InkConfiguration.cmyk4_0,
          ),
          ComponentPreset(
            role = ComponentRole.Stand,
            materialId = rollUpStandEconomyId,
            inkConfiguration = InkConfiguration.noInk,
          ),
        ),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(850, 2000)),
          SpecValue.QuantitySpec(Quantity.unsafe(1)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-rollup-premium"),
        name = LocalizedString("Premium", "Premium"),
        description = Some(LocalizedString(
          "Banner film + premium stand, 4+0 CMYK, 850×2000 mm, 1 pc",
          "Bannerová fólie + premium stojan, 4+0 CMYK, 850×2000 mm, 1 ks",
        )),
        printingMethodId = uvInkjetId,
        componentPresets = List(
          ComponentPreset(
            role = ComponentRole.Main,
            materialId = rollUpBannerFilmId,
            inkConfiguration = InkConfiguration.cmyk4_0,
          ),
          ComponentPreset(
            role = ComponentRole.Stand,
            materialId = rollUpStandPremiumId,
            inkConfiguration = InkConfiguration.noInk,
          ),
        ),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(850, 2000)),
          SpecValue.QuantitySpec(Quantity.unsafe(1)),
        ),
      ),
    ),
  )

  // --- Promotional Product Categories ---

  val tshirts: ProductCategory = ProductCategory(
    id = tshirtsId,
    name = LocalizedString("T-Shirts", "Trička"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(cottonTshirt150Id, cottonTshirt180Id, polyesterTshirtId, cottonPolyBlendId, organicCottonTshirtId),
      allowedFinishIds = Set(heatPressId, labelPrintId, foldBagId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(screenPrintId, dtgId, sublimationId),
    description = Some(LocalizedString(
      "Custom printed T-shirts in cotton, polyester, and blended fabrics. Available with screen printing, DTG, and sublimation. Dimensions specify the print area size.",
      "Trička s vlastním potiskem z bavlny, polyesteru a směsových materiálů. K dispozici v sítotisku, DTG a sublimaci. Rozměry určují velikost tiskové plochy.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-tshirt-standard"),
        name = LocalizedString("Standard Cotton", "Standardní bavlna"),
        description = Some(LocalizedString(
          "White cotton tee 180gsm, screen print, 50 pcs",
          "Bílé bavlněné tričko 180g, sítotisk, 50 ks",
        )),
        printingMethodId = screenPrintId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = cottonTshirt180Id,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(foldBagId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(300, 350)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-tshirt-premium-dtg"),
        name = LocalizedString("Premium DTG", "Prémiový DTG"),
        description = Some(LocalizedString(
          "Full-color photo print on organic cotton, 25 pcs",
          "Plnobarevný fototisk na bio bavlnu, 25 ks",
        )),
        printingMethodId = dtgId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = organicCottonTshirtId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(foldBagId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(300, 350)),
          SpecValue.QuantitySpec(Quantity.unsafe(25)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-tshirt-sublimation"),
        name = LocalizedString("Sublimation All-Over", "Sublimace celoplošná"),
        description = Some(LocalizedString(
          "All-over print on polyester, 100 pcs",
          "Celoplošný potisk na polyester, 100 ks",
        )),
        printingMethodId = sublimationId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = polyesterTshirtId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(foldBagId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(350, 400)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
        ),
      ),
    ),
  )

  val ecoBags: ProductCategory = ProductCategory(
    id = ecoBagsId,
    name = LocalizedString("Eco Bags", "Eko tašky"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(cottonCanvasBagId, organicCottonBagId, recycledPetBagId, juteBagId, nonWovenPpBagId),
      allowedFinishIds = Set(heatPressId, embroideryId, reinforcedHandlesId, foldBagId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(screenPrintId, dtgId),
    description = Some(LocalizedString(
      "Sustainable branded tote bags in cotton canvas, organic cotton, recycled PET, jute, and non-woven polypropylene. Dimensions specify the print area size.",
      "Ekologické reklamní tašky z bavlněného plátna, bio bavlny, recyklovaného PET, juty a netkané polypropylénové textilie. Rozměry určují velikost tiskové plochy.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-bag-canvas"),
        name = LocalizedString("Standard Canvas", "Standardní plátno"),
        description = Some(LocalizedString(
          "Natural cotton canvas, 1-color screen print, 100 pcs",
          "Přírodní bavlněné plátno, jednobarevný sítotisk, 100 ks",
        )),
        printingMethodId = screenPrintId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = cottonCanvasBagId,
          inkConfiguration = InkConfiguration.mono1_0,
          finishSelections = List(FinishSelection(reinforcedHandlesId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(250, 250)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-bag-organic"),
        name = LocalizedString("Organic Eco", "Bio eko"),
        description = Some(LocalizedString(
          "Organic cotton, full-color DTG print, 50 pcs",
          "Bio bavlna, plnobarevný DTG tisk, 50 ks",
        )),
        printingMethodId = dtgId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = organicCottonBagId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(250, 250)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
        ),
      ),
    ),
  )

  val pinBadges: ProductCategory = ProductCategory(
    id = pinBadgesId,
    name = LocalizedString("Pin Badges", "Odznaky"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(tinplateBadgeId, acrylicBadgeId, woodenBadgeId),
      allowedFinishIds = Set(mylarOverlayId, safetyPinId, magnetBackId, bottleOpenerId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(digitalId, offsetId),
    description = Some(LocalizedString(
      "Custom pin badges in tinplate, acrylic, or wood. Available with safety pin, magnet, or bottle opener backs. Dimensions specify badge diameter — only stock sizes (32mm, 58mm) are available.",
      "Vlastní odznaky z plechu, akrylátu nebo dřeva. K dispozici se špendlíkem, magnetem nebo otvírákem. Rozměry udávají průměr odznaku — pouze skladové velikosti (32mm, 58mm).",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-badge-standard"),
        name = LocalizedString("Standard Round 58mm", "Standardní kulatý 58mm"),
        description = Some(LocalizedString(
          "58mm tinplate, digital print, safety pin, 100 pcs",
          "58mm plech, digitální tisk, zavírací špendlík, 100 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = tinplateBadgeId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(safetyPinId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(58, 58)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-badge-small"),
        name = LocalizedString("Small Round 32mm", "Malý kulatý 32mm"),
        description = Some(LocalizedString(
          "32mm tinplate, digital print, safety pin, 200 pcs",
          "32mm plech, digitální tisk, zavírací špendlík, 200 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = tinplateBadgeId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(safetyPinId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(32, 32)),
          SpecValue.QuantitySpec(Quantity.unsafe(200)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-badge-magnet"),
        name = LocalizedString("Magnet Badge", "Magnetický odznak"),
        description = Some(LocalizedString(
          "58mm tinplate, digital print, magnet back, 50 pcs",
          "58mm plech, digitální tisk, magnetické uchycení, 50 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = tinplateBadgeId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(magnetBackId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(58, 58)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
        ),
      ),
    ),
  )

  val cups: ProductCategory = ProductCategory(
    id = cupsId,
    name = LocalizedString("Cups & Mugs", "Hrnky a šálky"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(ceramicMugWhiteId, ceramicMugColoredId, magicMugId, stainlessTravelMugId, enamelMugId, glassMugId),
      allowedFinishIds = Set(dishwasherCoatId, giftBoxId, glossyGlazeId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(sublimationId, screenPrintId, uvInkjetId),
    description = Some(LocalizedString(
      "Personalized mugs and cups in ceramic, stainless steel, enamel, and glass. Available with sublimation, screen print, and UV direct print. Dimensions specify the print area size.",
      "Personalizované hrnky a šálky z keramiky, nerezu, smaltu a skla. K dispozici se sublimací, sítotiskem a UV přímým tiskem. Rozměry určují velikost tiskové plochy.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-mug-standard"),
        name = LocalizedString("Standard White Mug", "Standardní bílý hrnek"),
        description = Some(LocalizedString(
          "White ceramic 330ml, sublimation, 50 pcs",
          "Bílý keramický 330ml, sublimace, 50 ks",
        )),
        printingMethodId = sublimationId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = ceramicMugWhiteId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(190, 80)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-mug-gift"),
        name = LocalizedString("Corporate Gift Set", "Firemní dárkový set"),
        description = Some(LocalizedString(
          "White ceramic 330ml, sublimation, gift box, 25 pcs",
          "Bílý keramický 330ml, sublimace, dárková krabička, 25 ks",
        )),
        printingMethodId = sublimationId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = ceramicMugWhiteId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(giftBoxId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(190, 80)),
          SpecValue.QuantitySpec(Quantity.unsafe(25)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-mug-travel"),
        name = LocalizedString("Travel Mug", "Cestovní hrnek"),
        description = Some(LocalizedString(
          "Stainless 450ml, UV print, 20 pcs",
          "Nerezový 450ml, UV tisk, 20 ks",
        )),
        printingMethodId = uvInkjetId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = stainlessTravelMugId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(180, 60)),
          SpecValue.QuantitySpec(Quantity.unsafe(20)),
        ),
      ),
    ),
  )

  val free: ProductCategory = ProductCategory(
    id = freeId,
    name = LocalizedString("Free Configuration", "Volná konfigurace"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = allMaterialIds,
      allowedFinishIds = allFinishIds,
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set.empty, // empty = all methods allowed
  )

  // --- Product Catalog ---
  val catalog: ProductCatalog = ProductCatalog(
    categories = Map(
      businessCardsId -> businessCards,
      flyersId        -> flyers,
      brochuresId     -> brochures,
      bannersId       -> banners,
      packagingId     -> packaging,
      bookletsId      -> booklets,
      calendarsId     -> calendars,
      postcardsId     -> postcards,
      stickersId      -> stickers,
      rollUpsId       -> rollUps,
      freeId          -> free,
      // Promotional
      tshirtsId    -> tshirts,
      ecoBagsId    -> ecoBags,
      pinBadgesId  -> pinBadges,
      cupsId       -> cups,
    ),
    materials = Map(
      coated300gsmId      -> coated300gsm,
      uncoatedBondId      -> uncoatedBond,
      kraftId             -> kraft,
      vinylId             -> vinyl,
      corrugatedId        -> corrugated,
      coatedSilk250gsmId  -> coatedSilk250gsm,
      yupoId              -> yupo,
      adhesiveStockId     -> adhesiveStock,
      cottonId            -> cotton,
      clearVinylId        -> clearVinyl,
      // Coated Art Paper Glossy
      coatedGlossy90gsmId  -> coatedGlossy90gsm,
      coatedGlossy115gsmId -> coatedGlossy115gsm,
      coatedGlossy130gsmId -> coatedGlossy130gsm,
      coatedGlossy150gsmId -> coatedGlossy150gsm,
      coatedGlossy170gsmId -> coatedGlossy170gsm,
      coatedGlossy200gsmId -> coatedGlossy200gsm,
      coatedGlossy250gsmId -> coatedGlossy250gsm,
      coatedGlossy350gsmId -> coatedGlossy350gsm,
      // Coated Art Paper Matte
      coatedMatte90gsmId  -> coatedMatte90gsm,
      coatedMatte115gsmId -> coatedMatte115gsm,
      coatedMatte130gsmId -> coatedMatte130gsm,
      coatedMatte150gsmId -> coatedMatte150gsm,
      coatedMatte170gsmId -> coatedMatte170gsm,
      coatedMatte200gsmId -> coatedMatte200gsm,
      coatedMatte250gsmId -> coatedMatte250gsm,
      coatedMatte300gsmId -> coatedMatte300gsm,
      coatedMatte350gsmId -> coatedMatte350gsm,
      // Roll-Up Materials
      rollUpBannerFilmId    -> rollUpBannerFilm,
      rollUpStandEconomyId  -> rollUpStandEconomy,
      rollUpStandPremiumId  -> rollUpStandPremium,
      // Calendar Protective Covers
      transparentPlasticCoverId -> transparentPlasticCover,
      cardboard350gWhiteId      -> cardboard350gWhite,
      cardboard350gBlackId      -> cardboard350gBlack,
      cardboard350gGreyId       -> cardboard350gGrey,
      cardboard350gBrownId      -> cardboard350gBrown,
      // Binding Color Materials
      bindingPlasticBlackId  -> bindingPlasticBlack,
      bindingPlasticWhiteId  -> bindingPlasticWhite,
      bindingPlasticSilverId -> bindingPlasticSilver,
      bindingPlasticBlueId   -> bindingPlasticBlue,
      bindingPlasticRedId    -> bindingPlasticRed,
      bindingPlasticClearId  -> bindingPlasticClear,
      bindingMetalBlackId    -> bindingMetalBlack,
      bindingMetalSilverId   -> bindingMetalSilver,
      bindingMetalWhiteId    -> bindingMetalWhite,
      // Promotional Materials
      cottonTshirt150Id      -> cottonTshirt150,
      cottonTshirt180Id      -> cottonTshirt180,
      polyesterTshirtId      -> polyesterTshirt,
      cottonPolyBlendId      -> cottonPolyBlend,
      organicCottonTshirtId  -> organicCottonTshirt,
      cottonCanvasBagId      -> cottonCanvasBag,
      organicCottonBagId     -> organicCottonBag,
      recycledPetBagId       -> recycledPetBag,
      juteBagId              -> juteBag,
      nonWovenPpBagId        -> nonWovenPpBag,
      tinplateBadgeId        -> tinplateBadge,
      acrylicBadgeId         -> acrylicBadge,
      woodenBadgeId          -> woodenBadge,
      ceramicMugWhiteId      -> ceramicMugWhite,
      ceramicMugColoredId    -> ceramicMugColored,
      magicMugId             -> magicMug,
      stainlessTravelMugId   -> stainlessTravelMug,
      enamelMugId            -> enamelMug,
      glassMugId             -> glassMug,
    ),
    finishes = Map(
      matteLaminationId  -> matteLamination,
      glossLaminationId  -> glossLamination,
      uvCoatingId        -> uvCoating,
      embossingId        -> embossing,
      foilStampingId     -> foilStamping,
      dieCutId           -> dieCut,
      varnishId          -> varnish,
      softTouchCoatingId -> softTouchCoating,
      aqueousCoatingId   -> aqueousCoating,
      debossingId        -> debossing,
      scoringId          -> scoring,
      perforationId      -> perforation,
      roundCornersId     -> roundCorners,
      grommetsId         -> grommets,
      kissCutId          -> kissCut,
      overlaminationId   -> overlamination,
      // Promotional Finishes
      heatPressId         -> heatPress,
      labelPrintId        -> labelPrint,
      foldBagId           -> foldBag,
      mylarOverlayId      -> mylarOverlay,
      safetyPinId         -> safetyPin,
      magnetBackId        -> magnetBack,
      bottleOpenerId      -> bottleOpener,
      dishwasherCoatId    -> dishwasherCoat,
      giftBoxId           -> giftBox,
      glossyGlazeId       -> glossyGlaze,
      embroideryId        -> embroideryFinish,
      reinforcedHandlesId -> reinforcedHandles,
    ),
    printingMethods = Map(
      offsetId      -> offsetMethod,
      digitalId     -> digitalMethod,
      uvInkjetId    -> uvInkjetMethod,
      letterpressId -> letterpressMethod,
      // Promotional
      screenPrintId  -> screenPrintMethod,
      dtgId          -> dtgMethod,
      sublimationId  -> sublimationMethod,
    ),
  )
