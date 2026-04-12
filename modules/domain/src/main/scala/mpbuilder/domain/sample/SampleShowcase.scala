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
  ).sortBy(_.sortOrder)

  /** Products grouped by catalog group, in display order. */
  val byGroup: Map[CatalogGroup, List[ShowcaseProduct]] =
    allProducts.groupBy(_.group)

  /** Look up a showcase product by its category ID. */
  def forCategory(categoryId: CategoryId): Option[ShowcaseProduct] =
    allProducts.find(_.categoryId == categoryId)
