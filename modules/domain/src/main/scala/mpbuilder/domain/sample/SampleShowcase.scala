package mpbuilder.domain.sample

import mpbuilder.domain.model.*

/** Sample showcase products for the customer-facing product catalog.
  *
  * Each entry enriches an existing [[ProductCategory]] from [[SampleCatalog]]
  * with marketing content: tagline, description, images, variations, features,
  * and ordering instructions.
  */
object SampleShowcase:

  // ─── Sheet Products ───────────────────────────────────────────────────

  val businessCards: ShowcaseProduct = ShowcaseProduct(
    categoryId = SampleCatalog.businessCardsId,
    group = CatalogGroup.Sheet,
    tagline = LocalizedString(
      "Make a lasting first impression",
      "Udělejte nezapomenutelný první dojem",
    ),
    detailedDescription = LocalizedString(
      "Our business cards are crafted from premium paper stocks ranging from 250 to 350 gsm. " +
        "Choose from coated, uncoated, cotton, kraft, or synthetic Yupo for a unique tactile experience. " +
        "Elevate your design with luxurious finishes like soft-touch coating, embossing, foil stamping, " +
        "and matte or gloss lamination. Available in digital and letterpress printing.",
      "Naše vizitky jsou vyrobeny z prémiových papírů o gramáži 250 až 350 g/m². " +
        "Vybírejte z křídového, nenatíraného, bavlněného, kraftového nebo syntetického papíru Yupo " +
        "pro jedinečný haptický zážitek. Povýšte svůj design luxusními úpravami jako slepotisk, " +
        "ražba fólií, soft-touch lak nebo laminace. K dispozici v digitálním a knihtisku.",
    ),
    imageUrl = "https://images.unsplash.com/photo-1628891439478-c613e85af7d6?w=600&h=400&fit=crop",
    galleryImageUrls = List(
      "https://images.unsplash.com/photo-1628891439478-c613e85af7d6?w=800&h=600&fit=crop",
      "https://images.unsplash.com/photo-1704030459012-bfbe0d55fec6?w=800&h=600&fit=crop",
    ),
    variations = List(
      ProductVariation(
        LocalizedString("Basic", "Základní"),
        LocalizedString("Coated 300gsm, single-sided CMYK, 85×55 mm", "Křídový 300g, jednostranný CMYK, 85×55 mm"),
        presetId = Some(PresetId.unsafe("preset-bc-basic")),
      ),
      ProductVariation(
        LocalizedString("Premium", "Prémiové"),
        LocalizedString("Matte 350gsm, double-sided CMYK, lamination + round corners", "Matný 350g, oboustranný CMYK, laminace + zaoblené rohy"),
        presetId = Some(PresetId.unsafe("preset-bc-premium")),
      ),
    ),
    features = List(
      ProductFeature("📐", LocalizedString("Custom Sizes", "Vlastní rozměry"), LocalizedString("Standard 90×50mm or fully custom dimensions", "Standardní 90×50mm nebo zcela vlastní rozměry")),
      ProductFeature("✨", LocalizedString("Premium Finishes", "Prémiové úpravy"), LocalizedString("Lamination, embossing, foil stamping, soft-touch", "Laminace, slepotisk, ražba fólií, soft-touch")),
      ProductFeature("🖨️", LocalizedString("Two Print Methods", "Dvě tiskové metody"), LocalizedString("Digital printing or artisan letterpress", "Digitální tisk nebo řemeslný knihtisk")),
      ProductFeature("📦", LocalizedString("Bulk Pricing", "Množstevní slevy"), LocalizedString("Volume discounts from 250+ pieces", "Množstevní slevy od 250 kusů")),
    ),
    instructions = Some(LocalizedString(
      "1. Choose your paper stock and weight  2. Select finishing options (lamination, foil, embossing)  " +
        "3. Set your preferred size and quantity  4. Upload your artwork or design in our visual editor  " +
        "5. Review the proof and approve for production",
      "1. Vyberte typ a gramáž papíru  2. Zvolte povrchové úpravy (laminace, fólie, slepotisk)  " +
        "3. Nastavte velikost a počet kusů  4. Nahrajte svůj návrh nebo ho vytvořte v editoru  " +
        "5. Zkontrolujte náhled a schvalte do výroby",
    )),
    popularFinishes = List("Matte Lamination", "Soft-Touch Coating", "Gold Foil Stamping"),
    turnaroundDays = Some("3-5"),
    sortOrder = 1,
    icon = "🪪",
  )

  val flyers: ShowcaseProduct = ShowcaseProduct(
    categoryId = SampleCatalog.flyersId,
    group = CatalogGroup.Sheet,
    tagline = LocalizedString(
      "Eye-catching promotional prints",
      "Poutavé propagační materiály",
    ),
    detailedDescription = LocalizedString(
      "Single-sheet promotional flyers available in paper weights from lightweight 90gsm to sturdy 350gsm. " +
        "Perfect for event handouts, menus, product sheets, and marketing collateral. " +
        "Choose landscape or portrait orientation with a range of protective coatings.",
      "Jednostránkové propagační letáky s gramáží od lehkých 90g po pevné 350g. " +
        "Ideální pro eventové materiály, jídelní lístky, produktové listy a marketingové podklady. " +
        "Na výšku nebo na šířku s řadou ochranných povrchových úprav.",
    ),
    imageUrl = "https://images.unsplash.com/photo-1563097013-a1df1d1fd1c7?w=600&h=400&fit=crop",
    galleryImageUrls = List(
      "https://images.unsplash.com/photo-1563097013-a1df1d1fd1c7?w=600&h=400&fit=crop",
    ),
    variations = List(
      ProductVariation(
        LocalizedString("Standard", "Standardní"),
        LocalizedString("Glossy 130gsm, single-sided CMYK, A5", "Lesklý 130g, jednostranný CMYK, A5"),
        presetId = Some(PresetId.unsafe("preset-flyers-standard")),
      ),
      ProductVariation(
        LocalizedString("Premium", "Prémiové"),
        LocalizedString("Matte 250gsm, double-sided CMYK with lamination", "Matný 250g, oboustranný CMYK s laminací"),
        presetId = Some(PresetId.unsafe("preset-flyers-premium")),
      ),
      ProductVariation(
        LocalizedString("Lightweight", "Lehké"),
        LocalizedString("Glossy 90gsm — ideal for mass distribution", "Lesklý 90g — ideální pro hromadnou distribuci"),
        presetId = Some(PresetId.unsafe("preset-flyers-lightweight")),
      ),
    ),
    features = List(
      ProductFeature("📄", LocalizedString("Wide Paper Range", "Široká nabídka papírů"), LocalizedString("From 90gsm leaflets to 350gsm card flyers", "Od 90g letáčků po 350g kartónové letáky")),
      ProductFeature("🔄", LocalizedString("Both Orientations", "Obě orientace"), LocalizedString("Portrait or landscape layout", "Na výšku nebo na šířku")),
      ProductFeature("🛡️", LocalizedString("Protective Coatings", "Ochranné úpravy"), LocalizedString("UV coating, lamination, aqueous coating", "UV lak, laminace, disperzní lak")),
    ),
    instructions = Some(LocalizedString(
      "1. Pick a paper weight appropriate for your use  2. Choose orientation (portrait or landscape)  " +
        "3. Add optional coatings for durability  4. Set quantity and upload your artwork",
      "1. Zvolte gramáž vhodnou pro vaše použití  2. Vyberte orientaci (na výšku nebo na šířku)  " +
        "3. Přidejte volitelné povrchové úpravy  4. Nastavte množství a nahrajte návrh",
    )),
    popularFinishes = List("Gloss Lamination", "UV Coating"),
    turnaroundDays = Some("2-4"),
    sortOrder = 2,
    icon = "📄",
  )

  val brochures: ShowcaseProduct = ShowcaseProduct(
    categoryId = SampleCatalog.brochuresId,
    group = CatalogGroup.Sheet,
    tagline = LocalizedString(
      "Professionally folded marketing pieces",
      "Profesionálně skládané marketingové materiály",
    ),
    detailedDescription = LocalizedString(
      "Folded brochures and leaflets with multiple fold types: bi-fold, tri-fold, Z-fold, and gate fold. " +
        "Scoring is automatically included for clean folds on heavier paper stocks. " +
        "Perfect for product catalogs, restaurant menus, travel guides, and corporate presentations.",
      "Skládané brožury a letáky s více typy skladu: na půl, na třetiny, Z-sklad a dvoudveřový sklad. " +
        "Bigování je automaticky zahrnuto pro čisté přehyby na silnějších papírech. " +
        "Ideální pro produktové katalogy, jídelní lístky, průvodce a firemní prezentace.",
    ),
    imageUrl = "https://images.unsplash.com/photo-1695634621121-691d54259d37?w=600&h=400&fit=crop",
    variations = List(
      ProductVariation(
        LocalizedString("Tri-Fold", "Na třetiny"),
        LocalizedString("Glossy 150gsm, two folds creating 6 panels", "Lesklý 150g, dva sklady vytvářející 6 panelů"),
        presetId = Some(PresetId.unsafe("preset-brochures-standard")),
      ),
      ProductVariation(
        LocalizedString("Bi-Fold", "Na půl"),
        LocalizedString("Matte 200gsm, single fold creating 4 panels", "Matný 200g, jeden sklad vytvářející 4 panely"),
        presetId = Some(PresetId.unsafe("preset-brochures-bifold")),
      ),
      ProductVariation(
        LocalizedString("Z-Fold", "Z-sklad"),
        LocalizedString("Glossy 170gsm, accordion-style zigzag fold", "Lesklý 170g, harmonikový cikcak sklad"),
        presetId = Some(PresetId.unsafe("preset-brochures-zfold")),
      ),
    ),
    features = List(
      ProductFeature("📖", LocalizedString("Multiple Fold Types", "Více typů skladu"), LocalizedString("Bi-fold, tri-fold, Z-fold, gate fold", "Na půl, na třetiny, Z-sklad, dvoudveřový")),
      ProductFeature("✂️", LocalizedString("Auto Scoring", "Automatické bigování"), LocalizedString("Clean folds included on heavy stocks", "Čisté přehyby na silných papírech")),
    ),
    popularFinishes = List("Matte Lamination", "UV Coating", "Scoring"),
    turnaroundDays = Some("3-5"),
    sortOrder = 3,
    icon = "📋",
  )

  val postcards: ShowcaseProduct = ShowcaseProduct(
    categoryId = SampleCatalog.postcardsId,
    group = CatalogGroup.Sheet,
    tagline = LocalizedString(
      "Versatile cards for every occasion",
      "Univerzální karty pro každou příležitost",
    ),
    detailedDescription = LocalizedString(
      "Premium postcards printed on heavy stocks from 250 to 350gsm. " +
        "Ideal for direct mail campaigns, invitations, thank-you cards, and promotional mailers. " +
        "Available in coated, silk, and cotton papers with a variety of finishing options.",
      "Prémiové pohlednice tištěné na silných papírech od 250 do 350g. " +
        "Ideální pro direct mail kampaně, pozvánky, děkovné karty a propagační zásilky. " +
        "K dispozici v křídovém, hedvábném a bavlněném papíru s řadou povrchových úprav.",
    ),
    imageUrl = "https://images.unsplash.com/photo-1557717398-f6a5f68cd158?w=600&h=400&fit=crop",
    variations = List(
      ProductVariation(
        LocalizedString("Standard", "Standardní"),
        LocalizedString("Coated 300gsm, double-sided CMYK, A6", "Křídový 300g, oboustranný CMYK, A6"),
        presetId = Some(PresetId.unsafe("preset-postcards-standard")),
      ),
      ProductVariation(
        LocalizedString("Premium Cotton", "Prémiové bavlněné"),
        LocalizedString("Cotton 300gsm with soft-touch coating", "Bavlněný 300g se soft-touch lakem"),
        presetId = Some(PresetId.unsafe("preset-postcards-premium")),
      ),
    ),
    features = List(
      ProductFeature("💌", LocalizedString("Direct Mail Ready", "Připraveno pro poštu"), LocalizedString("Standard postcard sizes for mailing", "Standardní velikosti pro poštovní zásilky")),
      ProductFeature("🎨", LocalizedString("Full Color", "Plnobarevný tisk"), LocalizedString("Vibrant CMYK printing on both sides", "Živý CMYK tisk na obou stranách")),
    ),
    popularFinishes = List("Gloss Lamination", "Soft-Touch Coating"),
    turnaroundDays = Some("2-4"),
    sortOrder = 4,
    icon = "📮",
  )

  // ─── Bound Products ───────────────────────────────────────────────────

  val booklets: ShowcaseProduct = ShowcaseProduct(
    categoryId = SampleCatalog.bookletsId,
    group = CatalogGroup.Bound,
    tagline = LocalizedString(
      "Multi-page bound publications",
      "Vícestránkové vázané publikace",
    ),
    detailedDescription = LocalizedString(
      "Professional booklets with separate cover and body components. " +
        "Choose different paper stocks for cover and interior pages, with binding options " +
        "including saddle stitch, perfect binding, and wire-o. Ideal for catalogs, programs, " +
        "annual reports, and product lookbooks.",
      "Profesionální brožury se samostatným obalem a vnitřním blokem. " +
        "Vyberte různé papíry pro obálku a vnitřní stránky s vazbami " +
        "včetně V-vazby, lepené vazby a drátěné spirály. Ideální pro katalogy, programy, " +
        "výroční zprávy a produktové lookbooky.",
    ),
    imageUrl = "https://images.unsplash.com/photo-1593723117578-c3f29d0b62c3?w=600&h=400&fit=crop",
    variations = List(
      ProductVariation(
        LocalizedString("Saddle Stitch", "V-vazba"),
        LocalizedString("Glossy cover + body, wire-stapled, ideal for 8-64 pages", "Lesklá obálka + tělo, sešité, ideální pro 8-64 stran"),
        presetId = Some(PresetId.unsafe("preset-booklets-standard")),
      ),
      ProductVariation(
        LocalizedString("Perfect Binding", "Lepená vazba"),
        LocalizedString("Matte laminated cover, glued spine, 48+ pages", "Matná laminovaná obálka, lepený hřbet, 48+ stran"),
        presetId = Some(PresetId.unsafe("preset-booklets-perfect")),
      ),
    ),
    features = List(
      ProductFeature("📚", LocalizedString("Dual Paper Stocks", "Dva typy papírů"), LocalizedString("Different papers for cover and interior", "Různé papíry pro obálku a vnitřek")),
      ProductFeature("🔗", LocalizedString("Multiple Bindings", "Více typů vazby"), LocalizedString("Saddle stitch, perfect, wire-o binding", "V-vazba, lepená, drátěná spirála")),
      ProductFeature("📏", LocalizedString("Custom Page Count", "Vlastní počet stran"), LocalizedString("From 8 to 200+ pages", "Od 8 do 200+ stran")),
    ),
    instructions = Some(LocalizedString(
      "1. Select cover paper stock  2. Choose interior paper  3. Pick binding method  " +
        "4. Set page count and size  5. Upload or design your layout",
      "1. Vyberte papír na obálku  2. Zvolte papír na vnitřek  3. Vyberte typ vazby  " +
        "4. Nastavte počet stran a velikost  5. Nahrajte nebo navrhněte layout",
    )),
    popularFinishes = List("Matte Lamination", "UV Coating"),
    turnaroundDays = Some("5-7"),
    sortOrder = 10,
    icon = "📚",
  )

  val calendars: ShowcaseProduct = ShowcaseProduct(
    categoryId = SampleCatalog.calendarsId,
    group = CatalogGroup.Bound,
    tagline = LocalizedString(
      "Custom printed calendars",
      "Vlastní tištěné kalendáře",
    ),
    detailedDescription = LocalizedString(
      "Wall and desk calendars with premium cover and interior pages. " +
        "Multi-component construction lets you choose a heavier stock for the cover " +
        "and lighter paper for the monthly pages. Wire-o binding standard.",
      "Nástěnné a stolní kalendáře s prémiovým obalem a vnitřními stránkami. " +
        "Vícekomponentová konstrukce umožňuje zvolit silnější papír na obálku " +
        "a lehčí papír na měsíční stránky. Standardní drátěná vazba.",
    ),
    imageUrl = "https://images.unsplash.com/photo-1598972692343-a17711455a22?w=600&h=400&fit=crop",
    variations = List(
      ProductVariation(
        LocalizedString("Wall Calendar", "Nástěnný kalendář"),
        LocalizedString("A4 glossy, wire-o binding, 28 pages", "A4 lesklý, kroužková vazba, 28 stran"),
        presetId = Some(PresetId.unsafe("preset-calendars-wall")),
      ),
      ProductVariation(
        LocalizedString("Desk Calendar", "Stolní kalendář"),
        LocalizedString("A5 matte, wire-o binding, 28 pages", "A5 matný, kroužková vazba, 28 stran"),
        presetId = Some(PresetId.unsafe("preset-calendars-desk")),
      ),
    ),
    features = List(
      ProductFeature("📅", LocalizedString("Wall & Desk", "Nástěnné a stolní"), LocalizedString("Multiple formats for every space", "Více formátů pro každý prostor")),
      ProductFeature("🎨", LocalizedString("Full Customization", "Plná přizpůsobitelnost"), LocalizedString("Custom photos and layouts per month", "Vlastní fotky a rozložení pro každý měsíc")),
    ),
    popularFinishes = List("Gloss Lamination"),
    turnaroundDays = Some("5-7"),
    sortOrder = 11,
    icon = "📅",
  )

  // ─── Large Format Products ────────────────────────────────────────────

  val banners: ShowcaseProduct = ShowcaseProduct(
    categoryId = SampleCatalog.bannersId,
    group = CatalogGroup.LargeFormat,
    tagline = LocalizedString(
      "Large-scale impactful displays",
      "Velkoformátové efektní prezentace",
    ),
    detailedDescription = LocalizedString(
      "Durable vinyl banners printed with UV-curable inkjet technology for vivid, " +
        "weather-resistant graphics. Ideal for outdoor events, trade shows, storefronts, " +
        "and construction site signage. Available with grommets for easy hanging.",
      "Odolné vinylové bannery tištěné UV inkoustem pro živé, " +
        "povětrnostně odolné grafiky. Ideální pro venkovní akce, veletrhy, výlohy " +
        "a stavební reklamu. K dispozici s průchodkami pro snadné zavěšení.",
    ),
    imageUrl = "https://images.unsplash.com/photo-1558452919-08ae4aea8e29?w=600&h=400&fit=crop",
    galleryImageUrls = List(
      "https://images.unsplash.com/photo-1558618666-fcd25c85f82e?w=800&h=600&fit=crop",
    ),
    variations = List(
      ProductVariation(
        LocalizedString("Indoor", "Interiérový"),
        LocalizedString("Vinyl, 1000×2000 mm, smooth surface for indoor displays", "Vinyl, 1000×2000 mm, hladký povrch pro interiérové prezentace"),
        presetId = Some(PresetId.unsafe("preset-banners-standard")),
      ),
      ProductVariation(
        LocalizedString("Outdoor with Grommets", "Exteriérový s průchodkami"),
        LocalizedString("Vinyl with UV coating + grommets, 1500×3000 mm", "Vinyl s UV lakem + průchodky, 1500×3000 mm"),
        presetId = Some(PresetId.unsafe("preset-banners-outdoor")),
      ),
    ),
    features = List(
      ProductFeature("☀️", LocalizedString("Weather Resistant", "Odolné počasí"), LocalizedString("UV-curable inks resist fading", "UV inkousty odolné vyblednutí")),
      ProductFeature("📏", LocalizedString("Custom Sizes", "Vlastní rozměry"), LocalizedString("Any size up to 5m wide", "Jakákoliv velikost do šířky 5m")),
      ProductFeature("🔩", LocalizedString("Grommets Available", "Průchodky k dispozici"), LocalizedString("Metal eyelets for easy installation", "Kovové očka pro snadnou instalaci")),
    ),
    popularFinishes = List("Grommets", "UV Coating"),
    turnaroundDays = Some("3-5"),
    sortOrder = 20,
    icon = "🏳️",
  )

  val rollUps: ShowcaseProduct = ShowcaseProduct(
    categoryId = SampleCatalog.rollUpsId,
    group = CatalogGroup.LargeFormat,
    tagline = LocalizedString(
      "Portable retractable displays",
      "Přenosné rolovací displeje",
    ),
    detailedDescription = LocalizedString(
      "Roll-up banner systems combine a printed banner with an aluminum retractable stand. " +
        "Set up in seconds — perfect for trade shows, conferences, retail spaces, and office lobbies. " +
        "Choose economy or premium stands paired with high-resolution printed banner film.",
      "Rolovací bannery kombinují potištěný banner s hliníkovým samonavíjecím stojanem. " +
        "Instalace za pár sekund — ideální pro veletrhy, konference, obchody a firemní recepce. " +
        "Vyberte ekonomický nebo prémiový stojan s vysoce kvalitním potištěným bannerovým filmem.",
    ),
    imageUrl = "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=600&h=400&fit=crop",
    variations = List(
      ProductVariation(
        LocalizedString("Economy Stand", "Ekonomický stojan"),
        LocalizedString("Lightweight aluminum stand + banner film, 850×2000 mm", "Lehký hliníkový stojan + bannerová fólie, 850×2000 mm"),
        presetId = Some(PresetId.unsafe("preset-rollup-economy")),
      ),
      ProductVariation(
        LocalizedString("Premium Stand", "Prémiový stojan"),
        LocalizedString("Heavy-duty stand for frequent transport, 850×2000 mm", "Robustní stojan pro častý transport, 850×2000 mm"),
        presetId = Some(PresetId.unsafe("preset-rollup-premium")),
      ),
    ),
    features = List(
      ProductFeature("⚡", LocalizedString("Quick Setup", "Rychlá instalace"), LocalizedString("Set up in under 30 seconds", "Instalace pod 30 sekund")),
      ProductFeature("🧳", LocalizedString("Portable", "Přenosný"), LocalizedString("Includes carrying case", "Včetně přepravního pouzdra")),
      ProductFeature("🔄", LocalizedString("Replaceable Print", "Vyměnitelný potisk"), LocalizedString("Change graphics without new stand", "Výměna grafiky bez nového stojanu")),
    ),
    popularFinishes = List("Overlamination"),
    turnaroundDays = Some("3-5"),
    sortOrder = 21,
    icon = "🪧",
  )

  // ─── Specialty Products ───────────────────────────────────────────────

  val packaging: ShowcaseProduct = ShowcaseProduct(
    categoryId = SampleCatalog.packagingId,
    group = CatalogGroup.Specialty,
    tagline = LocalizedString(
      "Custom branded packaging solutions",
      "Vlastní značkové obalové řešení",
    ),
    detailedDescription = LocalizedString(
      "Custom packaging boxes and containers made from kraft paper, corrugated cardboard, " +
        "or synthetic Yupo. Add premium touches with embossing, debossing, foil stamping, " +
        "and die-cutting for unique unboxing experiences. Perfect for retail, e-commerce, and gifts.",
      "Vlastní obalové krabice a kontejnery z kraftového papíru, vlnité lepenky " +
        "nebo syntetického papíru Yupo. Přidejte prémiové prvky jako slepotisk, " +
        "ražbu fólií a výsek pro jedinečný unboxing zážitek. Ideální pro retail, e-commerce a dárky.",
    ),
    imageUrl = "https://images.unsplash.com/photo-1617825295690-28ae56c56135?w=600&h=400&fit=crop",
    variations = List(
      ProductVariation(
        LocalizedString("Standard Kraft", "Standardní kraft"),
        LocalizedString("Natural brown kraft, 300×200 mm, 100 pcs", "Přírodní hnědý kraft, 300×200 mm, 100 ks"),
        presetId = Some(PresetId.unsafe("preset-packaging-standard")),
      ),
      ProductVariation(
        LocalizedString("Premium Die-Cut", "Prémiové s výsekem"),
        LocalizedString("Kraft with die-cut + scoring for custom shapes, 300×200 mm", "Kraft s výsekem + bigování pro vlastní tvary, 300×200 mm"),
        presetId = Some(PresetId.unsafe("preset-packaging-premium")),
      ),
    ),
    features = List(
      ProductFeature("📦", LocalizedString("Custom Die-Cut", "Vlastní výsek"), LocalizedString("Unique shapes and fold patterns", "Unikátní tvary a vzory skladu")),
      ProductFeature("♻️", LocalizedString("Eco Options", "Eko varianty"), LocalizedString("Recyclable kraft and corrugated stocks", "Recyklovatelný kraft a vlnitá lepenka")),
      ProductFeature("✨", LocalizedString("Premium Touches", "Prémiové prvky"), LocalizedString("Foil stamping, embossing, debossing", "Ražba fólií, slepotisk")),
    ),
    popularFinishes = List("Embossing", "Foil Stamping", "Die Cut"),
    turnaroundDays = Some("5-10"),
    sortOrder = 30,
    icon = "📦",
  )

  val stickers: ShowcaseProduct = ShowcaseProduct(
    categoryId = SampleCatalog.stickersId,
    group = CatalogGroup.Specialty,
    tagline = LocalizedString(
      "Custom stickers and labels",
      "Vlastní samolepky a etikety",
    ),
    detailedDescription = LocalizedString(
      "Self-adhesive stickers and labels on premium adhesive stock, clear vinyl, " +
        "or weather-resistant Yupo. Available with kiss-cut, die-cut, or round corners. " +
        "Perfect for product labels, packaging seals, laptop stickers, and brand merchandise.",
      "Samolepicí nálepky a etikety na prémiovém samolepicím materiálu, průhledném vinylu " +
        "nebo povětrnostně odolném papíru Yupo. K dispozici s kiss-cut, die-cut nebo zaoblenými rohy. " +
        "Ideální pro produktové etikety, pečeti na balení, nálepky na notebook a značkový merchandising.",
    ),
    imageUrl = "https://images.unsplash.com/photo-1589384267710-7a170981ca78?w=600&h=400&fit=crop",
    variations = List(
      ProductVariation(
        LocalizedString("Standard", "Standardní"),
        LocalizedString("Adhesive stock, 50×50 mm, peel-off sheet", "Samolepicí materiál, 50×50 mm, odlepovací arch"),
        presetId = Some(PresetId.unsafe("preset-stickers-standard")),
      ),
      ProductVariation(
        LocalizedString("Die-Cut", "Výsekový"),
        LocalizedString("Adhesive stock with die-cut for individual shapes", "Samolepicí materiál s výsekem pro jednotlivé tvary"),
        presetId = Some(PresetId.unsafe("preset-stickers-diecut")),
      ),
      ProductVariation(
        LocalizedString("Clear Vinyl", "Průhledný vinyl"),
        LocalizedString("Transparent background, UV inkjet print", "Průhledné pozadí, UV inkjet tisk"),
        presetId = Some(PresetId.unsafe("preset-stickers-clear")),
      ),
    ),
    features = List(
      ProductFeature("🏷️", LocalizedString("Multiple Materials", "Více materiálů"), LocalizedString("Paper, vinyl, clear, and synthetic", "Papír, vinyl, průhledný a syntetický")),
      ProductFeature("✂️", LocalizedString("Precision Cutting", "Přesný řez"), LocalizedString("Kiss-cut, die-cut, contour cut", "Kiss-cut, die-cut, obrysový řez")),
      ProductFeature("💧", LocalizedString("Waterproof Options", "Voděodolné varianty"), LocalizedString("Clear vinyl and Yupo resist water", "Průhledný vinyl a Yupo odolávají vodě")),
    ),
    popularFinishes = List("Kiss Cut", "Die Cut", "Round Corners"),
    turnaroundDays = Some("3-5"),
    sortOrder = 31,
    icon = "🏷️",
  )

  // ─── Promotional Products ────────────────────────────────────────────

  val tshirts: ShowcaseProduct = ShowcaseProduct(
    categoryId = SampleCatalog.tshirtsId,
    group = CatalogGroup.Promotional,
    tagline = LocalizedString(
      "Custom branded T-shirts for every occasion",
      "Trička s vlastním potiskem na každou příležitost",
    ),
    detailedDescription = LocalizedString(
      "High-quality custom T-shirts in cotton, polyester, and blended fabrics ranging from 140 to 180 gsm. " +
        "Choose from screen printing for bold solid colors, DTG for full-color photo prints, " +
        "or sublimation for all-over designs on polyester. Available with heat press transfer, " +
        "custom label printing, and individual fold & bag packaging.",
      "Vysoce kvalitní trička s vlastním potiskem z bavlny, polyesteru a směsových materiálů o gramáži 140 až 180 g/m². " +
        "Vyberte si sítotisk pro výrazné plné barvy, DTG pro plnobarevný fototisk " +
        "nebo sublimaci pro celoplošné designy na polyesteru. K dispozici s přenosem tepelným lisem, " +
        "tiskem vlastních štítků a individuálním balením do sáčku.",
    ),
    imageUrl = "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=600&h=400&fit=crop",
    galleryImageUrls = List(
      "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800&h=600&fit=crop",
      "https://images.unsplash.com/photo-1562157873-818bc0726f68?w=800&h=600&fit=crop",
    ),
    variations = List(
      ProductVariation(
        LocalizedString("Standard Cotton", "Standardní bavlna"),
        LocalizedString("Cotton 180gsm, screen print, 50 pcs", "Bavlna 180g, sítotisk, 50 ks"),
        presetId = Some(PresetId.unsafe("preset-tshirt-standard")),
      ),
      ProductVariation(
        LocalizedString("Premium DTG", "Prémiový DTG"),
        LocalizedString("Organic cotton 180gsm, DTG full-color, 25 pcs", "Bio bavlna 180g, DTG plnobarevný, 25 ks"),
        presetId = Some(PresetId.unsafe("preset-tshirt-premium-dtg")),
      ),
      ProductVariation(
        LocalizedString("Sublimation All-Over", "Sublimace celoplošná"),
        LocalizedString("Polyester, sublimation all-over print, 100 pcs", "Polyester, celoplošný sublimační potisk, 100 ks"),
        presetId = Some(PresetId.unsafe("preset-tshirt-sublimation")),
      ),
    ),
    features = List(
      ProductFeature("👕", LocalizedString("Multiple Fabrics", "Více materiálů"), LocalizedString("Cotton, polyester, organic, and blends from 140-180gsm", "Bavlna, polyester, bio a směsi od 140 do 180 g/m²")),
      ProductFeature("🎨", LocalizedString("3 Print Methods", "3 tiskové metody"), LocalizedString("Screen print, DTG, and sublimation", "Sítotisk, DTG a sublimace")),
      ProductFeature("📦", LocalizedString("Packaging Options", "Možnosti balení"), LocalizedString("Heat press, label printing, fold & bag", "Tepelný lis, tisk štítků, složení a balení")),
      ProductFeature("💰", LocalizedString("Bulk Pricing", "Množstevní slevy"), LocalizedString("Volume discounts for large orders", "Množstevní slevy pro velké objednávky")),
    ),
    instructions = Some(LocalizedString(
      "1. Choose your fabric type and weight  2. Select your print method (screen, DTG, sublimation)  " +
        "3. Set size and quantity  4. Upload your artwork  5. Select finishing options and approve",
      "1. Vyberte typ a gramáž materiálu  2. Zvolte tiskovou metodu (sítotisk, DTG, sublimace)  " +
        "3. Nastavte velikost a počet kusů  4. Nahrajte svůj návrh  5. Zvolte úpravy a schvalte",
    )),
    popularFinishes = List("Screen Printing", "DTG Full Color", "Sublimation"),
    turnaroundDays = Some("5-7"),
    sortOrder = 40,
    icon = "👕",
  )

  val ecoBags: ShowcaseProduct = ShowcaseProduct(
    categoryId = SampleCatalog.ecoBagsId,
    group = CatalogGroup.Promotional,
    tagline = LocalizedString(
      "Sustainable branded bags that carry your message",
      "Ekologické tašky s vlastním potiskem",
    ),
    detailedDescription = LocalizedString(
      "Eco-friendly tote bags crafted from cotton canvas, organic cotton, recycled PET, jute, " +
        "and non-woven polypropylene. Perfect for events, retail, and corporate gifts. " +
        "Available with screen printing for bold logos or DTG for full-color designs. " +
        "Add embroidery for a premium touch or reinforced handles for extra durability.",
      "Ekologické tašky z bavlněného plátna, bio bavlny, recyklovaného PET, juty " +
        "a netkané polypropylénové textilie. Ideální pro akce, retail a firemní dárky. " +
        "K dispozici se sítotiskem pro výrazná loga nebo DTG pro plnobarevné designy. " +
        "Přidejte výšivku pro prémiový dojem nebo zpevněná ucha pro extra odolnost.",
    ),
    imageUrl = "https://images.unsplash.com/photo-1772890753145-089cd7618a8e?w=600&h=400&fit=crop",
    galleryImageUrls = List(
      "https://images.unsplash.com/photo-1772890753145-089cd7618a8e?w=600&h=400&fit=crop",
      "https://images.unsplash.com/photo-1772890753143-bf12df009a51?w=800&h=600&fit=crop",
    ),
    variations = List(
      ProductVariation(
        LocalizedString("Standard Canvas", "Standardní plátno"),
        LocalizedString("Cotton canvas 220gsm, 1-color screen print, 100 pcs", "Bavlněné plátno 220g, jednobarevný sítotisk, 100 ks"),
        presetId = Some(PresetId.unsafe("preset-bag-canvas")),
      ),
      ProductVariation(
        LocalizedString("Organic Eco", "Bio eko"),
        LocalizedString("Organic cotton, full-color DTG, 50 pcs", "Bio bavlna, plnobarevný DTG, 50 ks"),
        presetId = Some(PresetId.unsafe("preset-bag-organic")),
      ),
    ),
    features = List(
      ProductFeature("♻️", LocalizedString("Eco Materials", "Eko materiály"), LocalizedString("Organic cotton, recycled PET, jute", "Bio bavlna, recyklovaný PET, juta")),
      ProductFeature("🧵", LocalizedString("Embroidery Available", "Výšivka k dispozici"), LocalizedString("Thread-based logos for premium feel", "Vyšívané logo pro prémiový dojem")),
      ProductFeature("💪", LocalizedString("Durable Construction", "Odolná konstrukce"), LocalizedString("Reinforced handles and quality stitching", "Zpevněná ucha a kvalitní prošití")),
    ),
    instructions = Some(LocalizedString(
      "1. Choose your bag material  2. Select print method (screen print or DTG)  " +
        "3. Set size and quantity  4. Upload your design  5. Add finishing options and confirm",
      "1. Vyberte materiál tašky  2. Zvolte tiskovou metodu (sítotisk nebo DTG)  " +
        "3. Nastavte velikost a počet kusů  4. Nahrajte svůj design  5. Přidejte úpravy a potvrďte",
    )),
    popularFinishes = List("Screen Printing", "Heat Transfer", "Embroidery"),
    turnaroundDays = Some("7-10"),
    sortOrder = 41,
    icon = "🛍️",
  )

  val pinBadges: ShowcaseProduct = ShowcaseProduct(
    categoryId = SampleCatalog.pinBadgesId,
    group = CatalogGroup.Promotional,
    tagline = LocalizedString(
      "Custom pin badges for events, brands, and campaigns",
      "Vlastní odznaky pro akce, značky a kampaně",
    ),
    detailedDescription = LocalizedString(
      "Custom pin badges available in tinplate, acrylic, and wood. Full-color digital or offset printing " +
        "with protective mylar overlay. Choose from safety pin, magnet, or bottle opener backs. " +
        "Available in 32mm and 58mm standard sizes. Perfect for events, promotions, and brand merchandise.",
      "Vlastní odznaky z plechu, akrylátu a dřeva. Plnobarevný digitální nebo ofsetový tisk " +
        "s ochrannou mylarovou fólií. Vyberte si ze zavíracího špendlíku, magnetu nebo otvíráku na lahve. " +
        "K dispozici ve standardních velikostech 32mm a 58mm. Ideální pro akce, propagaci a merchandising.",
    ),
    imageUrl = "https://images.unsplash.com/photo-1521249692263-e0659c60326e?w=600&h=400&fit=crop",
    galleryImageUrls = List(
      "https://images.unsplash.com/photo-1521249692263-e0659c60326e?w=600&h=400&fit=crop",
    ),
    variations = List(
      ProductVariation(
        LocalizedString("Standard Round 58mm", "Standardní kulatý 58mm"),
        LocalizedString("Tinplate, digital print, safety pin, 100 pcs", "Plech, digitální tisk, špendlík, 100 ks"),
        presetId = Some(PresetId.unsafe("preset-badge-standard")),
      ),
      ProductVariation(
        LocalizedString("Small Round 32mm", "Malý kulatý 32mm"),
        LocalizedString("Tinplate, digital print, safety pin, 200 pcs", "Plech, digitální tisk, špendlík, 200 ks"),
        presetId = Some(PresetId.unsafe("preset-badge-small")),
      ),
      ProductVariation(
        LocalizedString("Magnet Badge", "Magnetický odznak"),
        LocalizedString("Tinplate, digital print, magnet back, 50 pcs", "Plech, digitální tisk, magnet, 50 ks"),
        presetId = Some(PresetId.unsafe("preset-badge-magnet")),
      ),
    ),
    features = List(
      ProductFeature("🏅", LocalizedString("Multiple Materials", "Více materiálů"), LocalizedString("Tinplate, acrylic, and wooden blanks", "Plech, akrylát a dřevo")),
      ProductFeature("🔒", LocalizedString("Back Options", "Volba uchycení"), LocalizedString("Safety pin, magnet, or bottle opener", "Špendlík, magnet nebo otvírák")),
      ProductFeature("⚡", LocalizedString("Quick Turnaround", "Rychlá výroba"), LocalizedString("3-5 day production for standard orders", "3-5 dní výroba pro standardní objednávky")),
    ),
    popularFinishes = List("Mylar Overlay", "Safety Pin", "Magnet Back"),
    turnaroundDays = Some("3-5"),
    sortOrder = 42,
    icon = "📌",
  )

  val cups: ShowcaseProduct = ShowcaseProduct(
    categoryId = SampleCatalog.cupsId,
    group = CatalogGroup.Promotional,
    tagline = LocalizedString(
      "Personalized mugs and cups for gifts and branding",
      "Personalizované hrnky a šálky pro dárky a branding",
    ),
    detailedDescription = LocalizedString(
      "Personalized mugs and cups in ceramic, stainless steel, enamel, and glass. " +
        "White ceramic mugs with sublimation for vivid wrap-around prints, colored mugs with UV direct print, " +
        "and stainless steel travel mugs for on-the-go branding. Add dishwasher-safe coating, " +
        "glossy ceramic glaze, or individual gift box packaging for a premium presentation.",
      "Personalizované hrnky a šálky z keramiky, nerezové oceli, smaltu a skla. " +
        "Bílé keramické hrnky se sublimací pro živé celoplošné potisky, barevné hrnky s UV přímým tiskem " +
        "a nerezové cestovní hrnky pro branding na cestách. Přidejte nátěr odolný myčce, " +
        "lesklou keramickou glazuru nebo individuální dárkovou krabičku pro prémiovou prezentaci.",
    ),
    imageUrl = "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=600&h=400&fit=crop",
    galleryImageUrls = List(
      "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=800&h=600&fit=crop",
      "https://images.unsplash.com/photo-1577937927133-66ef06acdf18?w=800&h=600&fit=crop",
    ),
    variations = List(
      ProductVariation(
        LocalizedString("Standard White Mug", "Standardní bílý hrnek"),
        LocalizedString("White ceramic 330ml, sublimation, 50 pcs", "Bílý keramický 330ml, sublimace, 50 ks"),
        presetId = Some(PresetId.unsafe("preset-mug-standard")),
      ),
      ProductVariation(
        LocalizedString("Corporate Gift Set", "Firemní dárkový set"),
        LocalizedString("White ceramic 330ml, sublimation, gift box, 25 pcs", "Bílý keramický 330ml, sublimace, dárková krabička, 25 ks"),
        presetId = Some(PresetId.unsafe("preset-mug-gift")),
      ),
      ProductVariation(
        LocalizedString("Travel Mug", "Cestovní hrnek"),
        LocalizedString("Stainless 450ml, UV print, 20 pcs", "Nerezový 450ml, UV tisk, 20 ks"),
        presetId = Some(PresetId.unsafe("preset-mug-travel")),
      ),
    ),
    features = List(
      ProductFeature("☕", LocalizedString("6 Material Options", "6 materiálů"), LocalizedString("Ceramic, stainless steel, enamel, glass, magic mug", "Keramika, nerez, smalt, sklo, magický hrnek")),
      ProductFeature("🎨", LocalizedString("Full-Color Printing", "Plnobarevný tisk"), LocalizedString("Sublimation, screen print, UV direct print", "Sublimace, sítotisk, UV přímý tisk")),
      ProductFeature("🎁", LocalizedString("Gift Packaging", "Dárkové balení"), LocalizedString("Individual gift boxes available", "Individuální dárkové krabičky k dispozici")),
    ),
    instructions = Some(LocalizedString(
      "1. Choose your mug type and material  2. Select print method (sublimation, screen, UV)  " +
        "3. Set quantity  4. Upload your artwork  5. Add finishing options (coating, gift box) and confirm",
      "1. Vyberte typ hrnku a materiál  2. Zvolte tiskovou metodu (sublimace, sítotisk, UV)  " +
        "3. Nastavte počet kusů  4. Nahrajte svůj návrh  5. Přidejte úpravy (nátěr, dárková krabička) a potvrďte",
    )),
    popularFinishes = List("Sublimation Wrap-Around", "UV Direct Print", "Gift Box"),
    turnaroundDays = Some("5-7"),
    sortOrder = 43,
    icon = "☕",
  )

  // ─── All products ─────────────────────────────────────────────────────

  val allProducts: List[ShowcaseProduct] = List(
    businessCards,
    flyers,
    brochures,
    postcards,
    booklets,
    calendars,
    banners,
    rollUps,
    packaging,
    stickers,
    tshirts,
    ecoBags,
    pinBadges,
    cups,
  ).sortBy(_.sortOrder)

  /** Products grouped by catalog group, in display order. */
  val byGroup: Map[CatalogGroup, List[ShowcaseProduct]] =
    allProducts.groupBy(_.group)

  /** Look up a showcase product by its category ID. */
  def forCategory(categoryId: CategoryId): Option[ShowcaseProduct] =
    allProducts.find(_.categoryId == categoryId)
