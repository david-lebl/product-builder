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
      "Premium glossy coated paper ideal for business cards and high-end print materials. The 300gsm weight provides a sturdy, professional feel with excellent color reproduction.",
      "Prémiový lesklý křídový papír ideální pro vizitky a luxusní tiskové materiály. Gramáž 300g poskytuje pevný, profesionální pocit s vynikající reprodukcí barev.",
    )),
  )

  val uncoatedBond: Material = Material(
    id = uncoatedBondId,
    name = LocalizedString("Uncoated Bond Paper 120gsm", "Nenatíraný papír 120g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(120)),
    properties = Set(MaterialProperty.Matte, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Natural matte paper with a soft, tactile surface. Great for writing on and gives a classic, understated look. Best for letterheads, forms, and eco-friendly prints.",
      "Přírodní matný papír s měkkým, hmatovým povrchem. Skvělý pro psaní a dodává klasický, decentní vzhled. Nejlepší pro hlavičkové papíry, formuláře a ekologické tisky.",
    )),
  )

  val kraft: Material = Material(
    id = kraftId,
    name = LocalizedString("Kraft Paper 250gsm", "Kraftový papír 250g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(250)),
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Rustic, eco-friendly brown paper with a natural textured finish. Popular for packaging, tags, and products with an organic or artisanal brand identity.",
      "Rustikální, ekologický hnědý papír s přírodní texturou. Oblíbený pro obaly, visačky a produkty s organickou nebo řemeslnou identitou značky.",
    )),
  )

  val vinyl: Material = Material(
    id = vinylId,
    name = LocalizedString("Adhesive Vinyl", "Samolepicí vinyl"),
    family = MaterialFamily.Vinyl,
    weight = None,
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.Glossy, MaterialProperty.SmoothSurface),
    description = Some(LocalizedString(
      "Durable, waterproof adhesive vinyl suitable for outdoor banners, signage, and vehicle wraps. Excellent weather resistance and vivid color output.",
      "Odolný, voděodolný samolepicí vinyl vhodný pro venkovní bannery, cedule a polepy vozidel. Vynikající odolnost vůči povětrnostním vlivům a živé barvy.",
    )),
  )

  val corrugated: Material = Material(
    id = corrugatedId,
    name = LocalizedString("Corrugated Cardboard", "Vlnitá lepenka"),
    family = MaterialFamily.Cardboard,
    weight = None,
    properties = Set(MaterialProperty.Recyclable, MaterialProperty.Textured),
    description = Some(LocalizedString(
      "Lightweight yet sturdy corrugated cardboard for packaging and displays. The fluted structure provides excellent cushioning and structural support.",
      "Lehká, ale pevná vlnitá lepenka pro obaly a displeje. Vlnitá struktura poskytuje vynikající tlumení a strukturální oporu.",
    )),
  )

  val coatedSilk250gsm: Material = Material(
    id = coatedSilk250gsmId,
    name = LocalizedString("Coated Silk 250gsm", "Křídový saténový papír 250g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(250)),
    properties = Set(MaterialProperty.Matte, MaterialProperty.SmoothSurface, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Semi-matte silk finish paper that combines the readability of matte with subtle sheen. Ideal for brochures and catalogs where text and images need to coexist beautifully.",
      "Papír s polo-matným saténovým povrchem, který kombinuje čitelnost matného papíru s jemným leskem. Ideální pro brožury a katalogy, kde text a obrázky potřebují krásně koexistovat.",
    )),
  )

  val yupo: Material = Material(
    id = yupoId,
    name = LocalizedString("Yupo Synthetic 200μm", "Syntetický papír Yupo 200μm"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(200)),
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.SmoothSurface),
    description = Some(LocalizedString(
      "Tear-resistant, waterproof synthetic paper made from polypropylene. Perfect for outdoor menus, maps, tags, and any application requiring extreme durability.",
      "Odolný, voděodolný syntetický papír vyrobený z polypropylenu. Ideální pro venkovní jídelní lístky, mapy, visačky a jakékoli použití vyžadující extrémní odolnost.",
    )),
  )

  val adhesiveStock: Material = Material(
    id = adhesiveStockId,
    name = LocalizedString("Adhesive Stock 100gsm", "Samolepicí materiál 100g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(100)),
    properties = Set(MaterialProperty.Glossy, MaterialProperty.SmoothSurface),
  )

  val clearVinyl: Material = Material(
    id = clearVinylId,
    name = LocalizedString("Clear Adhesive Vinyl", "Průhledný samolepicí vinyl"),
    family = MaterialFamily.Vinyl,
    weight = None,
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.SmoothSurface, MaterialProperty.Transparent),
  )

  val cotton: Material = Material(
    id = cottonId,
    name = LocalizedString("Cotton Paper 300gsm", "Bavlněný papír 300g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(300)),
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Luxurious cotton-fiber paper with a distinctive soft texture. A premium choice for business cards, invitations, and stationery that demand a tactile, artisan quality.",
      "Luxusní bavlněný papír s výraznou měkkou texturou. Prémiová volba pro vizitky, pozvánky a papírnické potřeby vyžadující hmatovou, řemeslnou kvalitu.",
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
      "Traditional high-volume printing method that transfers ink from a plate to a rubber blanket, then to paper. Delivers exceptional color accuracy and consistency for large print runs (500+ copies).",
      "Tradiční velkokapacitní tisková metoda přenášející inkoust z desky na gumový válec a poté na papír. Poskytuje výjimečnou přesnost a konzistenci barev pro velké náklady (500+ kusů).",
    )),
  )

  val digitalMethod: PrintingMethod = PrintingMethod(
    id = digitalId,
    name = LocalizedString("Digital Printing", "Digitální tisk"),
    processType = PrintingProcessType.Digital,
    maxColorCount = None,
    description = Some(LocalizedString(
      "Modern print technology that applies toner or inkjet directly to the substrate. Cost-effective for short runs, supports variable data printing, and requires no plates or setup fees.",
      "Moderní tisková technologie aplikující toner nebo inkjet přímo na podklad. Nákladově efektivní pro malé náklady, podporuje proměnná data a nevyžaduje tiskové desky ani přípravné poplatky.",
    )),
  )

  val uvInkjetMethod: PrintingMethod = PrintingMethod(
    id = uvInkjetId,
    name = LocalizedString("UV Curable Inkjet", "UV inkoustový tisk"),
    processType = PrintingProcessType.UVCurableInkjet,
    maxColorCount = None,
    description = Some(LocalizedString(
      "Wide-format printing that uses UV light to cure ink instantly on contact. Ideal for banners, signage, and rigid substrates. Produces vibrant, durable prints resistant to fading and scratching.",
      "Velkoformátový tisk využívající UV světlo k okamžitému vytvrzení inkoustu. Ideální pro bannery, cedule a tuhé podklady. Vytváří živé, odolné tisky odolné vůči vyblednutí a poškrábání.",
    )),
  )

  val letterpressMethod: PrintingMethod = PrintingMethod(
    id = letterpressId,
    name = LocalizedString("Letterpress", "Knihtisk"),
    processType = PrintingProcessType.Letterpress,
    maxColorCount = Some(2),
    description = Some(LocalizedString(
      "Classic relief printing that physically presses inked type into paper, creating a distinctive tactile impression. Perfect for luxury business cards, invitations, and artisan stationery. Limited to 1–2 colors per pass.",
      "Klasický reliéfní tisk, který fyzicky vtlačuje namočený typ do papíru a vytváří charakteristický hmatový otisk. Ideální pro luxusní vizitky, pozvánky a řemeslné papírnické potřeby. Omezeno na 1–2 barvy na průchod.",
    )),
  )

  // --- Finishes ---
  val matteLamination: Finish = Finish(
    id = matteLaminationId,
    name = LocalizedString("Matte Lamination", "Matná laminace"),
    finishType = FinishType.Lamination,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "A thin matte film applied over the printed surface. Reduces glare, provides a soft velvety feel, and protects against scratches and fingerprints. Gives a sophisticated, premium look.",
      "Tenká matná fólie aplikovaná na tiskový povrch. Snižuje odlesky, poskytuje měkký sametový pocit a chrání proti poškrábání a otiskům prstů. Dodává sofistikovaný, prémiový vzhled.",
    )),
  )

  val glossLamination: Finish = Finish(
    id = glossLaminationId,
    name = LocalizedString("Gloss Lamination", "Lesklá laminace"),
    finishType = FinishType.Lamination,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "A shiny transparent film that enhances color vibrancy and adds a glossy, eye-catching finish. Excellent protection against wear, moisture, and fading. Makes colors pop.",
      "Lesklá průhledná fólie, která zvyšuje živost barev a dodává lesklý, poutavý povrch. Vynikající ochrana proti opotřebení, vlhkosti a vyblednutí. Barvy vyniknou.",
    )),
  )

  val uvCoating: Finish = Finish(
    id = uvCoatingId,
    name = LocalizedString("UV Coating", "UV lak"),
    finishType = FinishType.UVCoating,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "A liquid coating cured by ultraviolet light, creating an ultra-glossy protective layer. Can be applied as a full flood coat or as spot application to highlight specific design elements.",
      "Tekutý lak vytvrzený ultrafialovým světlem, vytvářející ultra-lesklou ochrannou vrstvu. Může být aplikován celoplošně nebo bodově pro zvýraznění specifických designových prvků.",
    )),
  )

  val embossing: Finish = Finish(
    id = embossingId,
    name = LocalizedString("Embossing", "Slepotisk"),
    finishType = FinishType.Embossing,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "Creates a raised 3D effect by pressing the paper from behind using a metal die. Adds tactile dimension and luxury feel to logos, text, or decorative patterns.",
      "Vytváří vyvýšený 3D efekt vtlačením papíru zezadu pomocí kovové raznice. Přidává hmatový rozměr a luxusní pocit logům, textu nebo dekorativním vzorům.",
    )),
  )

  val foilStamping: Finish = Finish(
    id = foilStampingId,
    name = LocalizedString("Foil Stamping", "Ražba fólií"),
    finishType = FinishType.FoilStamping,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "Metallic or colored foil pressed onto paper using heat and a die. Available in gold, silver, copper, rose gold, and holographic. Creates a striking, reflective accent for logos and text.",
      "Metalická nebo barevná fólie vtlačená na papír pomocí tepla a raznice. K dispozici ve zlaté, stříbrné, měděné, růžově zlaté a holografické. Vytváří výrazný, reflexní akcent pro loga a text.",
    )),
  )

  val dieCut: Finish = Finish(
    id = dieCutId,
    name = LocalizedString("Die Cut", "Výsek"),
    finishType = FinishType.DieCut,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Custom shape cutting using a sharp steel die. Allows products to have unique non-rectangular shapes, windows, or intricate cut-out patterns for creative packaging and cards.",
      "Řezání vlastních tvarů pomocí ostré ocelové raznice. Umožňuje produktům mít unikátní neobdélníkové tvary, okénka nebo složité výřezové vzory pro kreativní obaly a karty.",
    )),
  )

  val varnish: Finish = Finish(
    id = varnishId,
    name = LocalizedString("Spot Varnish", "Parciální lak"),
    finishType = FinishType.Varnish,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "A glossy or matte varnish applied to specific areas of the print to create contrast and draw attention to key design elements like logos or images.",
      "Lesklý nebo matný lak aplikovaný na specifické oblasti tisku pro vytvoření kontrastu a přitažení pozornosti ke klíčovým designovým prvkům jako loga nebo obrázky.",
    )),
  )

  val softTouchCoating: Finish = Finish(
    id = softTouchCoatingId,
    name = LocalizedString("Soft Touch Coating", "Soft touch laminace"),
    finishType = FinishType.SoftTouchCoating,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "An ultra-matte lamination with a rubber-like velvety texture. Provides an exceptional tactile experience often associated with luxury brands. Highly resistant to fingerprints and scuffs.",
      "Ultra-matná laminace s gumovitou sametovou texturou. Poskytuje výjimečný hmatový zážitek často spojovaný s luxusními značkami. Vysoce odolná vůči otiskům prstů a odřeninám.",
    )),
  )

  val aqueousCoating: Finish = Finish(
    id = aqueousCoatingId,
    name = LocalizedString("Aqueous Coating", "Disperzní lak"),
    finishType = FinishType.AqueousCoating,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "A water-based protective coating that dries quickly and provides a light satin or gloss finish. Eco-friendly alternative to UV coating with good scuff resistance.",
      "Ochranný lak na vodní bázi, který rychle schne a poskytuje lehký saténový nebo lesklý povrch. Ekologická alternativa k UV laku s dobrou odolností proti odření.",
    )),
  )

  val debossing: Finish = Finish(
    id = debossingId,
    name = LocalizedString("Debossing", "Slepotisk do hloubky"),
    finishType = FinishType.Debossing,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "The reverse of embossing — presses the design into the paper, creating an indented impression. Provides a subtle, elegant tactile effect often used for logos and monograms.",
      "Opak slepotisku — vtlačuje design do papíru a vytváří vtisknutý otisk. Poskytuje jemný, elegantní hmatový efekt často používaný pro loga a monogramy.",
    )),
  )

  val scoring: Finish = Finish(
    id = scoringId,
    name = LocalizedString("Scoring", "Bigování"),
    finishType = FinishType.Scoring,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "A crease line pressed into paper to allow clean, precise folding without cracking. Essential for thick stocks (200gsm+) and products with folds like brochures or greeting cards.",
      "Ryska vtlačená do papíru pro čisté, přesné ohýbání bez praskání. Nezbytné pro silné papíry (200g+) a produkty s ohyby jako brožury nebo přání.",
    )),
  )

  val perforation: Finish = Finish(
    id = perforationId,
    name = LocalizedString("Perforation", "Perforace"),
    finishType = FinishType.Perforation,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "A line of small holes punched through paper to allow easy tearing along a defined path. Used for tear-off coupons, tickets, response cards, and calendar pages.",
      "Řada malých otvorů proražených do papíru pro snadné odtržení podél definované linie. Používáno pro trhací kupóny, lístky, odpovědní karty a stránky kalendáře.",
    )),
  )

  val roundCorners: Finish = Finish(
    id = roundCornersId,
    name = LocalizedString("Round Corners", "Zaoblené rohy"),
    finishType = FinishType.RoundCorners,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Rounds one or more corners of the product for a softer, modern look. Choose the number of corners (1–4) and radius. Adds a distinctive touch to business cards and postcards.",
      "Zaoblí jeden nebo více rohů produktu pro měkčí, moderní vzhled. Zvolte počet rohů (1–4) a poloměr. Dodává výrazný dotek vizitkám a pohlednicím.",
    )),
  )

  val grommets: Finish = Finish(
    id = grommetsId,
    name = LocalizedString("Grommets", "Průchodky"),
    finishType = FinishType.Grommets,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Metal or plastic eyelets inserted along banner edges for hanging with hooks, ropes, or cable ties. Spacing is configurable. Standard for outdoor vinyl banners.",
      "Kovové nebo plastové očka vložená podél okrajů banneru pro zavěšení háčky, provazy nebo stahovacími páskami. Rozteč je konfigurovatelná. Standard pro venkovní vinylové bannery.",
    )),
  )

  val kissCut: Finish = Finish(
    id = kissCutId,
    name = LocalizedString("Kiss Cut", "Výsek bez podkladu"),
    finishType = FinishType.KissCut,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "A precision cut through the top layer of sticker material without cutting the backing sheet. Allows individual stickers to be easily peeled off while remaining on a shared sheet.",
      "Přesný řez horní vrstvou samolepicího materiálu bez proříznutí podkladového listu. Umožňuje snadné odlepení jednotlivých samolepek, které zůstávají na sdíleném listu.",
    )),
  )

  val overlamination: Finish = Finish(
    id = overlaminationId,
    name = LocalizedString("Overlamination", "Ochranná laminace"),
    finishType = FinishType.Overlamination,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "A protective transparent film applied over wide-format prints to shield against UV rays, moisture, and abrasion. Extends the outdoor lifespan of banners and signage significantly.",
      "Ochranná průhledná fólie aplikovaná na velkoformátové tisky pro ochranu proti UV záření, vlhkosti a odření. Významně prodlužuje venkovní životnost bannerů a cedulí.",
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
      "Heavy-duty polyester film specifically designed for roll-up banner systems. Provides a smooth, non-curling surface with excellent ink adhesion and color vibrancy.",
      "Těžká polyesterová fólie speciálně navržená pro roll-up bannerové systémy. Poskytuje hladký, nekroutící se povrch s vynikající přilnavostí inkoustu a živostí barev.",
    )),
  )

  val rollUpStandEconomy: Material = Material(
    id = rollUpStandEconomyId,
    name = LocalizedString("Roll-Up Stand Economy", "Roll-up stojánek Economy"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set.empty,
    description = Some(LocalizedString(
      "Budget-friendly aluminum roll-up stand for single-use events and short-term displays. Lightweight and easy to assemble. Suitable for indoor use only.",
      "Ekonomický hliníkový roll-up stojánek pro jednorázové akce a krátkodobé displeje. Lehký a snadno sestavitelný. Vhodný pouze pro vnitřní použití.",
    )),
  )

  val rollUpStandPremium: Material = Material(
    id = rollUpStandPremiumId,
    name = LocalizedString("Roll-Up Stand Premium", "Roll-up stojánek Premium"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set.empty,
    description = Some(LocalizedString(
      "Professional-grade retractable roll-up stand with wide base and adjustable height. Durable construction for repeated use at trade shows and events. Includes a carry bag.",
      "Profesionální zatahovací roll-up stojánek se širokým podstavcem a nastavitelnou výškou. Odolná konstrukce pro opakované použití na veletrzích a akcích. Včetně přepravní tašky.",
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
      "Standard business cards printed on premium stock. Choose from various paper types (coated, uncoated, cotton, kraft) and enhance with lamination, embossing, foil, or round corners.",
      "Standardní vizitky tištěné na prémiový materiál. Vyberte z různých typů papíru (křídový, nenatíraný, bavlněný, kraft) a vylepšete laminací, slepotiskem, fólií nebo zaoblenými rohy.",
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
      "Single-sheet printed flyers in various sizes (A3–A6, DL). Available in portrait or landscape orientation with optional lamination or coating for a polished finish.",
      "Jednolistové tištěné letáky v různých velikostech (A3–A6, DL). K dispozici na výšku nebo na šířku s volitelnou laminací nebo lakem pro perfektní povrch.",
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
      "Folded leaflets with various fold types (half, tri-fold, gate, Z-fold, etc.). Scoring is included for clean folding. Ideal for menus, product info, and marketing materials.",
      "Skládané letáky s různými typy skladů (půlený, trojsklad, bránový, Z-sklad atd.). Bigování je zahrnuto pro čisté ohýbání. Ideální pro jídelní lístky, produktové informace a marketingové materiály.",
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
      "Large-format vinyl banners for outdoor and indoor use. Printed with UV-curable inkjet for maximum durability. Add grommets for hanging or die-cut for custom shapes.",
      "Velkoformátové vinylové bannery pro venkovní i vnitřní použití. Tištěné UV inkjetovým tiskem pro maximální odolnost. Přidejte průchodky pro zavěšení nebo výsek pro vlastní tvary.",
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
      "Custom printed boxes and packaging on kraft, corrugated, or synthetic stock. Enhance with embossing, foil stamping, or die-cut windows for premium unboxing experience.",
      "Vlastní potištěné krabice a obaly na kraft, vlnitou lepenku nebo syntetický materiál. Vylepšete slepotiskem, ražbou fólií nebo výsekovými okénky pro prémiový zážitek z rozbalení.",
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
      "Multi-page booklets and catalogs with separate cover and body stocks. Choose from saddle stitch, perfect, spiral, wire-O, or case binding. Cover can have different material and finishing than body.",
      "Vícestránkové brožury a katalogy s oddělenou obálkou a vnitřkem. Vyberte ze sešitové, lepené, kroužkové, wire-O nebo tuhé vazby. Obálka může mít jiný materiál a povrchovou úpravu než vnitřek.",
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
