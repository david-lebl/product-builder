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
      "Premium glossy coated paper with a smooth, bright surface. Ideal for business cards and premium print products where vibrant colors and sharp details are important. The 300gsm weight provides a sturdy, professional feel.",
      "Prémiový lesklý křídový papír s hladkým, jasným povrchem. Ideální pro vizitky a prémiové tiskoviny, kde záleží na sytých barvách a ostrých detailech. Gramáž 300g zajišťuje pevný, profesionální dojem."
    )),
  )

  val uncoatedBond: Material = Material(
    id = uncoatedBondId,
    name = LocalizedString("Uncoated Bond Paper 120gsm", "Nenatíraný papír 120g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(120)),
    properties = Set(MaterialProperty.Matte, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Natural, uncoated paper with a matte finish. Easy to write on, making it suitable for letterheads, forms, and products where a natural, tactile feel is desired. Lighter weight suitable for multi-page documents.",
      "Přírodní nenatíraný papír s matným povrchem. Snadno se na něj píše, vhodný pro hlavičkové papíry, formuláře a produkty, kde je žádoucí přírodní hmatový vjem. Nižší gramáž vhodná pro vícestránkové dokumenty."
    )),
  )

  val kraft: Material = Material(
    id = kraftId,
    name = LocalizedString("Kraft Paper 250gsm", "Kraftový papír 250g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(250)),
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Strong, eco-friendly brown paper with a distinctive natural look. Popular for packaging, tags, and products with a rustic or organic aesthetic. Fully recyclable and biodegradable.",
      "Pevný, ekologický hnědý papír s výrazným přírodním vzhledem. Oblíbený pro obaly, visačky a produkty s rustikální nebo přírodní estetikou. Plně recyklovatelný a biologicky rozložitelný."
    )),
  )

  val vinyl: Material = Material(
    id = vinylId,
    name = LocalizedString("Adhesive Vinyl", "Samolepicí vinyl"),
    family = MaterialFamily.Vinyl,
    weight = None,
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.Glossy, MaterialProperty.SmoothSurface),
    description = Some(LocalizedString(
      "Durable, waterproof self-adhesive vinyl film. Perfect for outdoor banners, vehicle wraps, window graphics, and signage that needs to withstand weather conditions.",
      "Odolná, voděodolná samolepicí vinylová fólie. Ideální pro venkovní bannery, polepy vozidel, výlohy a signage, které musí odolávat povětrnostním podmínkám."
    )),
  )

  val corrugated: Material = Material(
    id = corrugatedId,
    name = LocalizedString("Corrugated Cardboard", "Vlnitá lepenka"),
    family = MaterialFamily.Cardboard,
    weight = None,
    properties = Set(MaterialProperty.Recyclable, MaterialProperty.Textured),
    description = Some(LocalizedString(
      "Lightweight yet sturdy cardboard with a fluted inner layer for added strength. Used for packaging boxes, displays, and protective shipping materials. Recyclable and cost-effective.",
      "Lehký, ale pevný karton s vlnitou vnitřní vrstvou pro zvýšenou odolnost. Používá se pro krabice, displeje a ochranné přepravní materiály. Recyklovatelný a cenově výhodný."
    )),
  )

  val coatedSilk250gsm: Material = Material(
    id = coatedSilk250gsmId,
    name = LocalizedString("Coated Silk 250gsm", "Křídový saténový papír 250g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(250)),
    properties = Set(MaterialProperty.Matte, MaterialProperty.SmoothSurface, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Silk-finish coated paper combining a smooth surface with reduced glare. Provides excellent color reproduction with a sophisticated, non-reflective appearance. Great for brochures, catalogs, and premium marketing materials.",
      "Křídový papír se saténovým povrchem kombinující hladký povrch se sníženým odleskem. Zajišťuje vynikající reprodukci barev se sofistikovaným, nereflexním vzhledem. Skvělý pro brožury, katalogy a prémiové marketingové materiály."
    )),
  )

  val yupo: Material = Material(
    id = yupoId,
    name = LocalizedString("Yupo Synthetic 200μm", "Syntetický papír Yupo 200μm"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(200)),
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.SmoothSurface),
    description = Some(LocalizedString(
      "Synthetic waterproof paper made from polypropylene. Tear-resistant and extremely durable, ideal for outdoor menus, maps, tags, and any application requiring resistance to water and rough handling.",
      "Syntetický voděodolný papír vyrobený z polypropylenu. Odolný proti roztržení a extrémně trvanlivý, ideální pro venkovní jídelní lístky, mapy, visačky a jakékoliv použití vyžadující odolnost vůči vodě a hrubému zacházení."
    )),
  )

  val adhesiveStock: Material = Material(
    id = adhesiveStockId,
    name = LocalizedString("Adhesive Stock 100gsm", "Samolepicí materiál 100g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(100)),
    properties = Set(MaterialProperty.Glossy, MaterialProperty.SmoothSurface),
    description = Some(LocalizedString(
      "Paper-based adhesive stock with a glossy surface and peel-off backing. Suitable for labels, stickers, and product markings. Provides vibrant print quality with permanent adhesive.",
      "Samolepicí materiál na papírové bázi s lesklým povrchem a odlepovací podložkou. Vhodný pro etikety, samolepky a značení produktů. Zajišťuje živou kvalitu tisku s permanentním lepidlem."
    )),
  )

  val clearVinyl: Material = Material(
    id = clearVinylId,
    name = LocalizedString("Clear Adhesive Vinyl", "Průhledný samolepicí vinyl"),
    family = MaterialFamily.Vinyl,
    weight = None,
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.SmoothSurface, MaterialProperty.Transparent),
    description = Some(LocalizedString(
      "Transparent self-adhesive vinyl allowing the background surface to show through. Ideal for window decals, glass door graphics, and transparent labels where a 'no label' look is desired.",
      "Průhledný samolepicí vinyl umožňující prosvítání podkladového povrchu. Ideální pro výlohové polepy, grafiku na skleněné dveře a průhledné etikety, kde je žádoucí vzhled 'bez etikety'."
    )),
  )

  val cotton: Material = Material(
    id = cottonId,
    name = LocalizedString("Cotton Paper 300gsm", "Bavlněný papír 300g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(300)),
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable),
    description = Some(LocalizedString(
      "Luxury paper made from cotton fibers with a distinctive textured feel. Offers an elegant, tactile experience perfect for premium business cards, wedding invitations, and fine stationery.",
      "Luxusní papír vyrobený z bavlněných vláken s výrazným texturovaným dojmem. Nabízí elegantní hmatový zážitek, ideální pro prémiové vizitky, svatební oznámení a kvalitní dopisní papíry."
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
      "Traditional high-quality printing method ideal for large print runs. Uses printing plates to transfer ink to paper, producing consistent, sharp results. Cost-effective for quantities over 500 pieces with excellent color fidelity.",
      "Tradiční vysoce kvalitní tisková metoda ideální pro velké náklady. Používá tiskové desky k přenosu barvy na papír, produkuje konzistentní, ostré výsledky. Cenově výhodný pro náklady nad 500 kusů s vynikající věrností barev."
    )),
  )

  val digitalMethod: PrintingMethod = PrintingMethod(
    id = digitalId,
    name = LocalizedString("Digital Printing", "Digitální tisk"),
    processType = PrintingProcessType.Digital,
    maxColorCount = None,
    description = Some(LocalizedString(
      "Modern printing directly from digital files without printing plates. Perfect for short runs, variable data printing, and quick turnaround. Supports full color (CMYK) with no minimum order quantity.",
      "Moderní tisk přímo z digitálních souborů bez tiskových desek. Ideální pro malé náklady, tisk s proměnnými daty a rychlou realizaci. Podporuje plnobarevný tisk (CMYK) bez minimálního objednaného množství."
    )),
  )

  val uvInkjetMethod: PrintingMethod = PrintingMethod(
    id = uvInkjetId,
    name = LocalizedString("UV Curable Inkjet", "UV inkoustový tisk"),
    processType = PrintingProcessType.UVCurableInkjet,
    maxColorCount = None,
    description = Some(LocalizedString(
      "Wide-format printing using UV-curable inks that are instantly dried with UV light. Ideal for banners, signage, and large-format prints on various substrates including vinyl, fabric, and rigid materials.",
      "Velkoformátový tisk pomocí UV vytvrditelných inkoustů, které jsou okamžitě sušeny UV světlem. Ideální pro bannery, signage a velkoformátové tisky na různé podklady včetně vinylu, textilu a rigidních materiálů."
    )),
  )

  val letterpressMethod: PrintingMethod = PrintingMethod(
    id = letterpressId,
    name = LocalizedString("Letterpress", "Knihtisk"),
    processType = PrintingProcessType.Letterpress,
    maxColorCount = Some(2),
    description = Some(LocalizedString(
      "Classic relief printing technique creating a distinctive debossed impression in the paper. Produces a tactile, luxurious feel highly valued for premium business cards, invitations, and stationery. Limited to 1-2 colors per pass.",
      "Klasická technika reliéfního tisku vytvářející výrazný vtlačený otisk do papíru. Produkuje hmatový, luxusní dojem vysoce ceněný u prémiových vizitek, pozvánek a dopisních papírů. Omezeno na 1-2 barvy na průchod."
    )),
  )

  // --- Finishes ---
  val matteLamination: Finish = Finish(
    id = matteLaminationId,
    name = LocalizedString("Matte Lamination", "Matná laminace"),
    finishType = FinishType.Lamination,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "A thin protective film applied to the printed surface creating a smooth, non-reflective matte finish. Reduces glare, resists fingerprints, and adds a sophisticated, elegant look. Enhances durability and protects against scratches.",
      "Tenká ochranná fólie aplikovaná na potištěný povrch vytvářející hladký, nereflexní matný povrch. Snižuje odlesky, odolává otiskům prstů a dodává sofistikovaný, elegantní vzhled. Zvyšuje trvanlivost a chrání proti poškrábání."
    )),
  )

  val glossLamination: Finish = Finish(
    id = glossLaminationId,
    name = LocalizedString("Gloss Lamination", "Lesklá laminace"),
    finishType = FinishType.Lamination,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "A shiny, reflective protective film that makes colors appear more vivid and vibrant. Enhances contrast and provides a premium, eye-catching finish. Ideal for marketing materials, photo prints, and products needing visual impact.",
      "Lesklá, reflexní ochranná fólie, díky které barvy vypadají živěji a sytěji. Zvyšuje kontrast a poskytuje prémiový, poutavý povrch. Ideální pro marketingové materiály, fototisk a produkty vyžadující vizuální dopad."
    )),
  )

  val uvCoating: Finish = Finish(
    id = uvCoatingId,
    name = LocalizedString("UV Coating", "UV lak"),
    finishType = FinishType.UVCoating,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "A liquid coating cured by ultraviolet light, creating a hard, glossy surface. Can be applied as a full flood coat or selectively (spot UV) to highlight specific design elements. Provides excellent scratch resistance.",
      "Tekutý lak vytvrzený ultrafialovým světlem vytvářející tvrdý, lesklý povrch. Může být aplikován jako celoplošný nátěr nebo selektivně (parciální UV) pro zvýraznění specifických designových prvků. Poskytuje vynikající odolnost proti poškrábání."
    )),
  )

  val embossing: Finish = Finish(
    id = embossingId,
    name = LocalizedString("Embossing", "Slepotisk"),
    finishType = FinishType.Embossing,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "A technique that creates a raised, three-dimensional design on paper by pressing it between custom dies. Adds a tactile, premium quality to business cards, logos, and decorative elements without using ink.",
      "Technika vytvářející vyvýšený, trojrozměrný design na papíru jeho lisováním mezi zakázkové raznice. Dodává hmatovou, prémiovou kvalitu vizitkám, logům a dekorativním prvkům bez použití barvy."
    )),
  )

  val foilStamping: Finish = Finish(
    id = foilStampingId,
    name = LocalizedString("Foil Stamping", "Ražba fólií"),
    finishType = FinishType.FoilStamping,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "A metallic or pigmented foil transferred to paper using heat and pressure. Available in gold, silver, copper, rose gold, and holographic finishes. Creates a luxurious, eye-catching metallic effect for logos, text, and design accents.",
      "Kovová nebo pigmentovaná fólie přenesená na papír pomocí tepla a tlaku. Dostupná ve zlaté, stříbrné, měděné, rose gold a holografické variantě. Vytváří luxusní, poutavý kovový efekt pro loga, text a designové akcenty."
    )),
  )

  val dieCut: Finish = Finish(
    id = dieCutId,
    name = LocalizedString("Die Cut", "Výsek"),
    finishType = FinishType.DieCut,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Custom cutting of paper or card into specific shapes using a sharp steel die. Used to create uniquely shaped business cards, packaging, folders, and promotional materials that stand out from standard rectangular formats.",
      "Zakázkové řezání papíru nebo kartonu do specifických tvarů pomocí ostrých ocelových nožů. Používá se k vytvoření unikátně tvarovaných vizitek, obalů, desek a propagačních materiálů, které vynikají oproti standardním obdélníkovým formátům."
    )),
  )

  val varnish: Finish = Finish(
    id = varnishId,
    name = LocalizedString("Spot Varnish", "Parciální lak"),
    finishType = FinishType.Varnish,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "A clear coating applied selectively to specific areas of a printed piece, creating contrast between glossy and matte surfaces. Used to highlight logos, images, or text, adding depth and visual interest to the design.",
      "Průhledný lak aplikovaný selektivně na specifické oblasti potištěného kusu, vytvářející kontrast mezi lesklým a matným povrchem. Používá se ke zvýraznění log, obrázků nebo textu, dodává hloubku a vizuální zajímavost designu."
    )),
  )

  val softTouchCoating: Finish = Finish(
    id = softTouchCoatingId,
    name = LocalizedString("Soft Touch Coating", "Soft touch laminace"),
    finishType = FinishType.SoftTouchCoating,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "A velvety, rubber-like coating that creates a luxurious tactile experience. Provides a unique 'peach skin' feel that is highly appealing for premium products. Excellent fingerprint resistance and a sophisticated matte appearance.",
      "Sametový, gumovitý povlak vytvářející luxusní hmatový zážitek. Poskytuje unikátní dojem 'broskvové kůže', který je vysoce atraktivní pro prémiové produkty. Vynikající odolnost proti otiskům prstů a sofistikovaný matný vzhled."
    )),
  )

  val aqueousCoating: Finish = Finish(
    id = aqueousCoatingId,
    name = LocalizedString("Aqueous Coating", "Disperzní lak"),
    finishType = FinishType.AqueousCoating,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "A water-based coating applied inline during printing. Provides basic protection against fingerprints and scuffing at a lower cost than lamination. Available in gloss, satin, or matte finishes. Eco-friendly and fast-drying.",
      "Lak na vodní bázi aplikovaný inline během tisku. Poskytuje základní ochranu proti otiskům prstů a odírání za nižší cenu než laminace. Dostupný v lesklém, saténovém nebo matném provedení. Ekologický a rychleschnoucí."
    )),
  )

  val debossing: Finish = Finish(
    id = debossingId,
    name = LocalizedString("Debossing", "Slepotisk do hloubky"),
    finishType = FinishType.Debossing,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "The opposite of embossing — creates an indented, recessed design pressed into the paper surface. Produces a subtle, elegant effect often used for logos and monograms on covers, business cards, and luxury packaging.",
      "Opak slepotisku — vytváří vtlačený, zapuštěný design vtlačený do povrchu papíru. Produkuje jemný, elegantní efekt často používaný pro loga a monogramy na obálky, vizitky a luxusní obaly."
    )),
  )

  val scoring: Finish = Finish(
    id = scoringId,
    name = LocalizedString("Scoring", "Bigování"),
    finishType = FinishType.Scoring,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "A crease line pressed into paper to create a clean, precise fold. Essential for heavier papers (200gsm+) to prevent cracking along the fold. Used in brochures, greeting cards, packaging, and any product that requires folding.",
      "Rýha vtlačená do papíru pro vytvoření čistého, přesného ohybu. Nezbytná pro těžší papíry (200g+), aby se zabránilo praskání v ohybu. Používá se u brožur, přání, obalů a jakéhokoli produktu vyžadujícího ohýbání."
    )),
  )

  val perforation: Finish = Finish(
    id = perforationId,
    name = LocalizedString("Perforation", "Perforace"),
    finishType = FinishType.Perforation,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "A series of small holes punched in a line to allow easy tearing along a straight path. Common in tickets, coupons, reply cards, and calendar pages. The pitch (distance between holes) can be adjusted for different tear strengths.",
      "Řada malých otvorů vyražených v řadě umožňující snadné odtržení podél přímé dráhy. Běžné u vstupenek, kupónů, odpovědních karet a listů kalendářů. Rozteč (vzdálenost mezi otvory) lze přizpůsobit pro různé síly odtržení."
    )),
  )

  val roundCorners: Finish = Finish(
    id = roundCornersId,
    name = LocalizedString("Round Corners", "Zaoblené rohy"),
    finishType = FinishType.RoundCorners,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Cutting the corners of a printed piece into smooth, rounded arcs. Available for 1 to 4 corners with adjustable radius. Gives a modern, polished look to business cards, postcards, and flyers while reducing corner damage.",
      "Oříznutí rohů tiskového kusu do hladkých, zaoblených oblouků. Dostupné pro 1 až 4 rohy s nastavitelným poloměrem. Dodává moderní, uhlazený vzhled vizitkám, pohlednicím a letákům a zároveň snižuje poškození rohů."
    )),
  )

  val grommets: Finish = Finish(
    id = grommetsId,
    name = LocalizedString("Grommets", "Průchodky"),
    finishType = FinishType.Grommets,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "Metal rings inserted along the edges of banners and large-format prints for hanging with hooks, ropes, or cable ties. Spacing between grommets can be adjusted. Essential for outdoor banners and exhibition displays.",
      "Kovové kroužky vložené podél okrajů bannerů a velkoformátových tisků pro zavěšení pomocí háčků, lan nebo stahovacích pásek. Rozteč mezi průchodkami lze upravit. Nezbytné pro venkovní bannery a výstavní displeje."
    )),
  )

  val kissCut: Finish = Finish(
    id = kissCutId,
    name = LocalizedString("Kiss Cut", "Výsek bez podkladu"),
    finishType = FinishType.KissCut,
    side = FinishSide.Both,
    description = Some(LocalizedString(
      "A cutting technique that cuts through the top layer of sticker material without cutting the backing sheet. Allows stickers to be easily peeled off while remaining on the sheet for distribution. Standard for sticker sheets and labels.",
      "Technika řezání, která prořízne horní vrstvu samolepicího materiálu bez proříznutí podkladového listu. Umožňuje snadné odlepení samolepek při zachování na listu pro distribuci. Standard pro samolepicí archy a etikety."
    )),
  )

  val overlamination: Finish = Finish(
    id = overlaminationId,
    name = LocalizedString("Overlamination", "Ochranná laminace"),
    finishType = FinishType.Overlamination,
    side = FinishSide.Front,
    description = Some(LocalizedString(
      "A clear protective film applied over printed graphics on vinyl, banners, or large-format prints. Provides UV protection, extends outdoor durability, and adds scratch resistance. Available in gloss, matte, or satin finishes.",
      "Průhledná ochranná fólie aplikovaná přes potištěnou grafiku na vinylu, bannerech nebo velkoformátových tiscích. Poskytuje UV ochranu, prodlužuje venkovní trvanlivost a přidává odolnost proti poškrábání. Dostupná v lesklém, matném nebo saténovém provedení."
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
      "Heavy-duty polyester film specifically designed for roll-up banner stands. Provides excellent color reproduction, is curl-resistant, and suitable for repeated rolling and unrolling without damage.",
      "Robustní polyesterová fólie speciálně navržená pro roll-up bannerové stojany. Poskytuje vynikající reprodukci barev, odolává zkroucení a je vhodná pro opakované rolování a rozrolování bez poškození."
    )),
  )

  val rollUpStandEconomy: Material = Material(
    id = rollUpStandEconomyId,
    name = LocalizedString("Roll-Up Stand Economy", "Roll-up stojánek Economy"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set.empty,
    description = Some(LocalizedString(
      "Budget-friendly retractable banner stand with an aluminum base. Suitable for indoor events, trade shows, and temporary displays. Lightweight and easy to transport with a carrying bag included.",
      "Cenově dostupný navíjecí bannerový stojan s hliníkovým podstavcem. Vhodný pro vnitřní akce, veletrhy a dočasné displeje. Lehký a snadno přenosný s přepravní taškou v ceně."
    )),
  )

  val rollUpStandPremium: Material = Material(
    id = rollUpStandPremiumId,
    name = LocalizedString("Roll-Up Stand Premium", "Roll-up stojánek Premium"),
    family = MaterialFamily.Hardware,
    weight = None,
    properties = Set.empty,
    description = Some(LocalizedString(
      "Professional-grade retractable banner stand with a wide, stable base and adjustable height. Features a smooth retraction mechanism for frequent use. Includes a padded carrying case and is built for long-term durability.",
      "Profesionální navíjecí bannerový stojan se širokou, stabilní základnou a nastavitelnou výškou. Disponuje hladkým navíjecím mechanismem pro časté používání. Zahrnuje polstrované přepravní pouzdro a je stavěn pro dlouhodobou trvanlivost."
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
      "Professional business cards printed on premium stock. Choose from various paper types, weights, and finishes to create a card that represents your brand. Standard sizes include 90×55mm (EU) and 89×51mm (US).",
      "Profesionální vizitky tištěné na prémiový materiál. Vyberte si z různých typů papíru, gramáží a povrchových úprav pro vizitku reprezentující vaši značku. Standardní velikosti zahrnují 90×55mm (EU) a 89×51mm (US)."
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
      "Single-sheet printed materials ideal for advertising, events, and promotions. Available in various sizes (A3 to DL) with single or double-sided printing. Choose paper weight based on intended use: lighter for handouts, heavier for premium feel.",
      "Jednolistové tiskové materiály ideální pro reklamu, akce a propagaci. Dostupné v různých velikostech (A3 až DL) s jednostranným nebo oboustranným tiskem. Gramáž papíru vybírejte podle zamýšleného použití: lehčí pro rozdávání, těžší pro prémiový dojem."
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
      "Folded print products with multiple panels. Choose a fold type to determine the number of panels and how the brochure opens. Scoring is recommended for heavier papers to ensure clean folds. Popular for menus, product info, and marketing materials.",
      "Skládané tiskoviny s více panely. Vyberte typ skladu pro určení počtu panelů a způsobu otevírání brožury. Pro těžší papíry se doporučuje bigování pro zajištění čistého ohybu. Oblíbené pro jídelní lístky, produktové informace a marketingové materiály."
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
      "Large-format vinyl banners for indoor and outdoor use. Printed with UV-curable inks for weather resistance. Add grommets for easy hanging. Custom sizes available — specify width and height in millimeters.",
      "Velkoformátové vinylové bannery pro vnitřní i venkovní použití. Tištěné UV vytvrditelné inkousty pro odolnost vůči počasí. Přidejte průchodky pro snadné zavěšení. Zakázkové velikosti — uveďte šířku a výšku v milimetrech."
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
      "Custom printed packaging including boxes, sleeves, and wraps. Available in kraft, corrugated, and synthetic materials. Die cutting creates custom box shapes, scoring enables clean folds, and various surface finishes add a premium look.",
      "Zakázkově potištěné obaly včetně krabic, návleků a zábalů. Dostupné v kraftovém, vlnitém a syntetickém materiálu. Výsek vytváří zakázkové tvary krabic, bigování umožňuje čisté ohyby a různé povrchové úpravy dodávají prémiový vzhled."
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
      "Multi-page bound publications with separate cover and body materials. Choose a binding method: saddle stitch (stapled, up to ~64 pages), perfect binding (glued spine, 48+ pages), spiral or wire-o (lay-flat). Page count must be a multiple of 4.",
      "Vícestránkové vázané publikace se samostatným materiálem obálky a vnitřku. Vyberte typ vazby: sešitová V1 (sešitá, do ~64 stran), lepená V2 (lepený hřbet, 48+ stran), kroužková nebo wire-o (ležící naplocho). Počet stran musí být násobek 4."
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
      "Wall and desk calendars with a separate cover and monthly pages. Choose binding method (typically wire-o or spiral for wall calendars). Perforation can be added to body pages for tear-off functionality.",
      "Nástěnné a stolní kalendáře se samostatnou obálkou a měsíčními stránkami. Vyberte typ vazby (obvykle wire-o nebo kroužková pro nástěnné kalendáře). K vnitřním stránkám lze přidat perforaci pro funkci odtrhávání."
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
      "Double-sided printed cards on heavy stock, ideal for direct mail, invitations, and promotional handouts. Typically printed on 250-350gsm paper for a sturdy feel. Multiple finish options available for a premium touch.",
      "Oboustranně potištěné karty na těžkém materiálu, ideální pro přímou poštu, pozvánky a propagační materiály. Obvykle tištěné na papíru 250-350g pro pevný dojem. K dispozici více možností povrchových úprav pro prémiový dotek."
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
      "Self-adhesive labels and stickers on paper or vinyl stock. Kiss cut leaves stickers on the backing sheet for easy peeling; die cut creates fully custom shapes. Choose clear vinyl for a transparent 'no label' look.",
      "Samolepicí etikety a samolepky na papírové nebo vinylové bázi. Výsek bez podkladu ponechává samolepky na podkladovém listu pro snadné odlepení; výsek vytváří plně zakázkové tvary. Zvolte průhledný vinyl pro transparentní vzhled 'bez etikety'."
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
      "Portable retractable banner displays with an aluminum stand and printed polyester graphic. Available in Economy (basic stand) and Premium (wide base, adjustable height) variants. Overlamination recommended for extended use and UV protection.",
      "Přenosné navíjecí bannerové displeje s hliníkovým stojanem a potištěnou polyesterovou grafikou. Dostupné v Economy (základní stojan) a Premium (široký podstavec, nastavitelná výška) variantách. Ochranná laminace doporučena pro delší životnost a UV ochranu."
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
    description = Some(LocalizedString(
      "Unrestricted product configuration allowing any combination of materials, finishes, and printing methods. Use this for custom or non-standard products that don't fit other categories.",
      "Neomezená konfigurace produktu umožňující jakoukoliv kombinaci materiálů, povrchových úprav a tiskových metod. Použijte pro zakázkové nebo nestandardní produkty, které nespadají do jiných kategorií."
    )),
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
