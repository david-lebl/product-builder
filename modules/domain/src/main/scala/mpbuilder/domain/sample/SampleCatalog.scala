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

  // --- Roll-Up Material IDs ---
  val rollUpBannerFilmId: MaterialId    = MaterialId.unsafe("mat-rollup-banner-film")
  val rollUpStandEconomyId: MaterialId  = MaterialId.unsafe("mat-rollup-stand-economy")
  val rollUpStandPremiumId: MaterialId  = MaterialId.unsafe("mat-rollup-stand-premium")

  // --- Printing Method IDs ---
  val offsetId: PrintingMethodId       = PrintingMethodId.unsafe("pm-offset")
  val digitalId: PrintingMethodId      = PrintingMethodId.unsafe("pm-digital")
  val uvInkjetId: PrintingMethodId     = PrintingMethodId.unsafe("pm-uv-inkjet")
  val letterpressId: PrintingMethodId  = PrintingMethodId.unsafe("pm-letterpress")

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
    ),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.Pages, SpecKind.BindingMethod),
    allowedPrintingMethodIds = Set(digitalId),
    description = Some(LocalizedString(
      "Multi-page booklets and catalogs with separate cover and body components. Choose different materials for the cover and inner pages. Binding options include saddle stitch, perfect binding, and wire-o.",
      "Vícestránkové brožury a katalogy se samostatnou obálkou a vnitřními stranami. Vyberte různé materiály pro obálku a vnitřní strany. Možnosti vazby zahrnují V-vazbu, lepenou vazbu a kroužkovou vazbu.",
    )),
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
    ),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.Pages, SpecKind.BindingMethod),
    allowedPrintingMethodIds = Set(digitalId),
    description = Some(LocalizedString(
      "Wall and desk calendars with cover and monthly pages. Separate cover and body components allow different paper choices. Wire-o binding is most common.",
      "Nástěnné a stolní kalendáře s obálkou a měsíčními stránkami. Samostatná obálka a vnitřní strany umožňují různé volby papíru. Kroužková vazba je nejběžnější.",
    )),
  )

  private val allMaterialIds: Set[MaterialId] = Set(
    coated300gsmId, uncoatedBondId, kraftId, vinylId, corrugatedId,
    coatedSilk250gsmId, yupoId, adhesiveStockId, cottonId, clearVinylId,
  ) ++ allCoatedGlossyIds ++ allCoatedMatteIds

  private val allFinishIds: Set[FinishId] = Set(
    matteLaminationId, glossLaminationId, uvCoatingId, embossingId,
    foilStampingId, dieCutId, varnishId, softTouchCoatingId, aqueousCoatingId,
    debossingId, scoringId, perforationId, roundCornersId, grommetsId, kissCutId,
    overlaminationId,
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
    ),
    printingMethods = Map(
      offsetId      -> offsetMethod,
      digitalId     -> digitalMethod,
      uvInkjetId    -> uvInkjetMethod,
      letterpressId -> letterpressMethod,
    ),
  )
