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
    guideSections = List(
      GuideSection(
        title = LocalizedString("Understanding Paper Weights & Types", "Porozumění gramážím a typům papíru"),
        body = LocalizedString(
          "Paper weight is measured in GSM (grams per square metre) — the higher the number, the thicker and sturdier the card. " +
            "Coated papers have a smooth, sealed surface ideal for vibrant colour reproduction, while uncoated stocks offer a natural, " +
            "writable texture. Cotton paper feels luxuriously soft with visible fibres, kraft gives an eco-friendly rustic look, " +
            "and synthetic Yupo is tear-proof and waterproof. For most business cards, 300–350 gsm strikes the best balance between " +
            "premium feel and practical durability.",
          "Gramáž papíru se měří v g/m² — čím vyšší číslo, tím silnější a odolnější karta. " +
            "Křídové papíry mají hladký uzavřený povrch ideální pro živou reprodukci barev, zatímco nenatírané papíry nabízejí " +
            "přirozený popisovatelný povrch. Bavlněný papír je luxusně měkký s viditelnými vlákny, kraft působí ekologicky a rustikálně " +
            "a syntetický Yupo je odolný proti roztržení i vodě. Pro většinu vizitek představuje 300–350 g/m² " +
            "nejlepší rovnováhu mezi prémiovým dojmem a praktickou odolností.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Finishing Options Explained", "Vysvětlení povrchových úprav"),
        body = LocalizedString(
          "Lamination adds a thin plastic film over the print — gloss lamination makes colours pop and adds shine, while matte " +
            "lamination gives a sophisticated, muted look. Soft-touch coating creates a velvety texture that people love to hold. " +
            "Embossing raises parts of the design so they stand out physically, adding a tactile dimension. Foil stamping applies " +
            "metallic or holographic foil to specific areas for eye-catching accents. You can combine multiple finishes — for example, " +
            "matte lamination with gold foil on the logo — to create a truly premium card.",
          "Laminace přidává na potisk tenkou plastovou fólii — lesklá laminace zvýrazní barvy a dodá lesk, zatímco matná " +
            "laminace vytváří sofistikovaný tlumený vzhled. Soft-touch lak vytváří sametový povrch, který je příjemný na dotek. " +
            "Slepotisk zvyšuje části designu, aby fyzicky vystupovaly, a přidává hmatový rozměr. Ražba fólií nanáší " +
            "metalickou nebo holografickou fólii na vybrané oblasti pro výrazné akcenty. Úpravy můžete kombinovat — například " +
            "matnou laminaci se zlatou fólií na logu — pro opravdu prémiovou vizitku.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Digital vs Letterpress Printing", "Digitální tisk vs knihtisk"),
        body = LocalizedString(
          "Digital printing is the modern standard — it handles full-colour photos, gradients, and complex designs with ease and is " +
            "cost-effective at any quantity. Letterpress is a traditional technique where inked metal or polymer plates press into the " +
            "paper, leaving a beautiful debossed impression you can feel with your fingertips. Letterpress works best with simple, " +
            "bold designs using one or two spot colours. It costs more and takes longer, but the tactile result is unmatched. " +
            "Choose digital for photo-rich designs, and letterpress when you want a handcrafted, artisan feel.",
          "Digitální tisk je moderní standard — snadno si poradí s plnobarevnými fotografiemi, přechody a složitými designy a je " +
            "cenově výhodný při jakémkoli nákladu. Knihtisk je tradiční technika, při které natisknuté kovové nebo polymerové štočky " +
            "vtlačují inkoust do papíru a zanechávají krásný hmatatelný otisk. Knihtisk funguje nejlépe s jednoduchými, " +
            "výraznými designy s jednou nebo dvěma přímými barvami. Stojí více a trvá déle, ale hmatový výsledek je nesrovnatelný. " +
            "Zvolte digitální tisk pro designy bohaté na fotografie a knihtisk, když chcete řemeslný, autorský dojem.",
        ),
      ),
    ),
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
    guideSections = List(
      GuideSection(
        title = LocalizedString("Choosing the Right Paper Weight", "Jak vybrat správnou gramáž papíru"),
        body = LocalizedString(
          "Paper weight dramatically affects how your flyer feels and performs. Lightweight 90 gsm paper is thin and economical — " +
            "perfect for mass distribution at events or door-to-door drops where you need thousands of copies. Standard 130 gsm " +
            "is the go-to choice for most promotional flyers: sturdy enough to feel professional, yet affordable in quantity. " +
            "For premium flyers like restaurant menus or product sheets that need to survive repeated handling, go with 250 gsm or " +
            "heavier — these feel more like cards than paper and convey quality instantly.",
          "Gramáž papíru zásadně ovlivňuje, jak váš leták vypadá a jak funguje. Lehký 90g papír je tenký a ekonomický — " +
            "ideální pro hromadnou distribuci na akcích nebo roznášku do schránek, kde potřebujete tisíce kopií. Standardních 130 g/m² " +
            "je nejčastější volba pro většinu propagačních letáků: dostatečně pevný pro profesionální dojem, a přesto cenově dostupný. " +
            "Pro prémiové letáky jako jídelní lístky nebo produktové listy, které musí vydržet opakované používání, zvolte 250 g/m² " +
            "nebo více — ty působí spíše jako karty než papír a okamžitě vyjadřují kvalitu.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Coating Options Guide", "Průvodce ochrannými povrchovými úpravami"),
        body = LocalizedString(
          "UV coating is a liquid finish cured with ultraviolet light that adds a high-gloss protective layer — it's the most " +
            "affordable option and great for flyers that need basic scuff resistance. Lamination bonds a thin plastic film to the " +
            "paper surface, offering superior durability and a choice between gloss (vivid, shiny) or matte (elegant, non-reflective) " +
            "looks. Aqueous coating is a water-based finish that provides light protection and a subtle sheen while being the most " +
            "eco-friendly option. For everyday flyers UV coating is usually sufficient, while lamination is worth the upgrade " +
            "for pieces that will be handled frequently or need to last longer.",
          "UV lak je tekutá úprava vytvrzená ultrafialovým světlem, která dodává vysoce lesklou ochrannou vrstvu — je to " +
            "nejdostupnější varianta skvělá pro letáky, které potřebují základní odolnost proti otěru. Laminace spojuje tenkou " +
            "plastovou fólii s povrchem papíru a nabízí vynikající odolnost s volbou mezi lesklým (živý, zářivý) nebo matným " +
            "(elegantní, neodrazivý) vzhledem. Disperzní lak je vodou ředěný, poskytuje lehkou ochranu a jemný lesk a je " +
            "nejekologičtější variantou. Pro běžné letáky většinou stačí UV lak, zatímco laminace se vyplatí " +
            "pro materiály, které se budou často používat nebo mají vydržet déle.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Design Tips for Effective Flyers", "Designové tipy pro efektivní letáky"),
        body = LocalizedString(
          "Always design your flyer with 3 mm bleed on all sides — this is the extra area beyond the trim line that ensures colours " +
            "and images extend to the very edge without white borders after cutting. Keep all important text and logos within the " +
            "safe zone, at least 5 mm from the trim edge, to avoid accidental cropping. Use images with a resolution of at least " +
            "300 DPI (dots per inch) for sharp, professional results; web images at 72 DPI will look blurry in print. " +
            "A clear visual hierarchy — bold headline, supporting image, concise body text, and a prominent call to action — " +
            "makes your flyer effective in the few seconds it has to capture attention.",
          "Vždy navrhujte leták se spadávkou 3 mm na všech stranách — to je oblast za ořezovou linkou, která zajistí, že barvy " +
            "a obrázky dosahují až k samému okraji bez bílých okrajů po ořezu. Veškerý důležitý text a loga udržujte v bezpečné zóně, " +
            "minimálně 5 mm od ořezové hrany, abyste předešli nechtěnému oříznutí. Používejte obrázky s rozlišením alespoň " +
            "300 DPI (bodů na palec) pro ostrý profesionální výsledek; webové obrázky se 72 DPI budou v tisku rozmazané. " +
            "Jasná vizuální hierarchie — výrazný titulek, podpůrný obrázek, stručný text a výrazná výzva k akci — " +
            "zajistí, že váš leták zaujme v těch pár sekundách, které má k dispozici.",
        ),
      ),
    ),
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
    guideSections = List(
      GuideSection(
        title = LocalizedString("Understanding Fold Types", "Porozumění typům skladu"),
        body = LocalizedString(
          "A bi-fold is the simplest option — one fold down the middle creates four panels, like a greeting card. " +
            "A tri-fold uses two parallel folds to create six panels that tuck neatly into a DL envelope, making it the most " +
            "popular choice for marketing brochures. A Z-fold (accordion fold) also has six panels but zigzags open, which is " +
            "great for step-by-step content or maps. A gate fold has two outer panels that swing open like doors to reveal a large " +
            "centre panel — perfect for dramatic product reveals or panoramic images.",
          "Sklad na půl je nejjednodušší varianta — jeden přehyb uprostřed vytvoří čtyři panely, podobně jako pohlednice. " +
            "Sklad na třetiny používá dva rovnoběžné přehyby a vytváří šest panelů, které se úhledně vejdou do obálky DL, " +
            "což z něj dělá nejoblíbenější volbu pro marketingové brožury. Z-sklad (harmonika) má také šest panelů, ale " +
            "otevírá se cikcak, což je skvělé pro postupný obsah nebo mapy. Dvoudveřový sklad má dva vnější panely, které se " +
            "otevírají jako dveře a odhalují velký střední panel — ideální pro dramatické prezentace produktů nebo panoramatické obrázky.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Paper Weight for Folded Products", "Gramáž papíru pro skládané produkty"),
        body = LocalizedString(
          "The weight of your paper directly affects fold quality. Lighter stocks (115–150 gsm) fold easily and are ideal for " +
            "tri-folds and Z-folds where multiple creases are needed. Medium weights (170–200 gsm) offer a more substantial feel " +
            "and work well for bi-folds. Once you go above 200 gsm, the paper becomes stiff enough that folding without scoring " +
            "(pre-creasing the fold line) can cause cracking or rough edges. We automatically include scoring on stocks above 170 gsm " +
            "to ensure every fold is clean and professional.",
          "Gramáž papíru přímo ovlivňuje kvalitu skladu. Lehčí papíry (115–150 g/m²) se snadno skládají a jsou ideální pro " +
            "sklad na třetiny a Z-sklad, kde je potřeba více přehybů. Střední gramáže (170–200 g/m²) nabízejí solidnější dojem " +
            "a dobře fungují pro sklad na půl. Jakmile překročíte 200 g/m², papír je natolik tuhý, že skládání bez bigování " +
            "(předrýhování linie přehybu) může způsobit praskání nebo nerovné hrany. Automaticky zahrnujeme bigování na papírech " +
            "nad 170 g/m², aby každý přehyb byl čistý a profesionální.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Planning Your Panel Layout", "Plánování rozložení panelů"),
        body = LocalizedString(
          "Each fold type creates a specific number of panels, and understanding the reading order is crucial for effective design. " +
            "In a tri-fold, the inner flap panel is slightly narrower than the other two so it tucks in without bulging — your " +
            "designer or our templates account for this automatically. Readers typically see the front cover first, then open to " +
            "the inner flap, then unfold to the full interior spread. Place your most important message or call to action on the " +
            "front panel, use the inner flap for a teaser, and reserve the full interior for detailed content.",
          "Každý typ skladu vytváří určitý počet panelů a pochopení pořadí čtení je klíčové pro efektivní design. " +
            "U skladu na třetiny je vnitřní chlopňový panel o něco užší než ostatní dva, aby se zahnul bez vyboulení — váš " +
            "designér nebo naše šablony to zohledňují automaticky. Čtenáři obvykle vidí nejprve přední obálku, pak otevřou " +
            "vnitřní chlopeň a poté rozloží na celý vnitřní rozkládací panel. Umístěte nejdůležitější sdělení nebo výzvu k akci " +
            "na přední panel, vnitřní chlopeň využijte jako upoutávku a celý vnitřek vyhraďte podrobnému obsahu.",
        ),
      ),
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
    guideSections = List(
      GuideSection(
        title = LocalizedString("Paper Stock Selection Guide", "Průvodce výběrem papíru"),
        body = LocalizedString(
          "Postcards need heavier paper to feel substantial in the hand and survive postal handling. At 250 gsm, you get a solid " +
            "card that's cost-effective for large mailings, though it can feel slightly flexible. The sweet spot is 300 gsm — thick " +
            "enough to feel premium and rigid, which is the industry standard for direct mail. For luxury invitations or high-end " +
            "brand pieces, 350 gsm provides a reassuringly stiff card. Coated paper delivers sharp, vivid photos; silk paper offers " +
            "a sophisticated sheen without glare; and cotton stock has a textured, artisanal feel perfect for boutique brands.",
          "Pohlednice potřebují silnější papír, aby působily solidně a přežily poštovní zpracování. Při 250 g/m² získáte pevnou " +
            "kartu, která je cenově výhodná pro velké zásilky, i když může být mírně ohebná. Optimální volba je 300 g/m² — " +
            "dostatečně silný pro prémiový a pevný dojem, což je průmyslový standard pro direct mail. Pro luxusní pozvánky nebo " +
            "špičkové značkové materiály nabízí 350 g/m² uklidňující tuhost. Křídový papír zajistí ostré, živé fotky; hedvábný " +
            "papír nabídne sofistikovaný lesk bez odlesků; a bavlněný papír má texturovaný řemeslný dojem ideální pro butikové značky.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Direct Mail Specifications", "Specifikace pro poštovní zásilky"),
        body = LocalizedString(
          "If you plan to send postcards by mail, size matters for postal rates. Standard A6 (105×148 mm) qualifies for the " +
            "cheapest letter rate in most countries and fits easily into mailboxes. DL size (99×210 mm) is a popular alternative " +
            "that stands out with its elongated format. Leave a clear addressing area on the back — typically the right half — " +
            "free from dark backgrounds so addresses and barcodes are easily readable by postal sorting machines. " +
            "We provide templates with pre-marked mailing zones to make this simple.",
          "Pokud plánujete pohlednice posílat poštou, velikost ovlivňuje poštovní sazby. Standardní A6 (105×148 mm) spadá do " +
            "nejlevnější sazby za dopis ve většině zemí a snadno se vejde do schránek. Formát DL (99×210 mm) je oblíbenou " +
            "alternativou, která zaujme podlouhlým tvarem. Ponechte na zadní straně čistou oblast pro adresu — obvykle pravou " +
            "polovinu — bez tmavých pozadí, aby adresy a čárové kódy byly snadno čitelné poštovními třídicími stroji. " +
            "Poskytujeme šablony s předem vyznačenými poštovními zónami pro jednoduchost.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Finishing for Maximum Impact", "Povrchové úpravy pro maximální efekt"),
        body = LocalizedString(
          "The right finish transforms a simple postcard into something people keep. Gloss lamination makes colours vibrant and " +
            "photos sharp, ideal for property listings or travel promotions. Matte lamination gives a refined, modern aesthetic " +
            "that reduces glare and feels smooth to the touch. Soft-touch coating takes matte a step further with a velvety, " +
            "almost rubber-like surface that feels luxurious. For the ultimate wow factor, add spot UV — a glossy raised coating " +
            "applied only to specific areas like your logo or key images, creating an eye-catching contrast against a matte background.",
          "Správná povrchová úprava promění obyčejnou pohlednici v něco, co si lidé ponechají. Lesklá laminace dodá barvám " +
            "živost a fotkám ostrost — ideální pro nemovitostní inzeráty nebo cestovní propagaci. Matná laminace poskytne " +
            "rafinovanou moderní estetiku, sníží odlesky a je příjemná na dotek. Soft-touch lak posouvá matný povrch ještě dál " +
            "se sametovým, téměř gumovým povrchem, který působí luxusně. Pro maximální wow efekt přidejte spot UV — lesklý " +
            "vyvýšený lak aplikovaný pouze na určité oblasti jako logo nebo klíčové obrázky, který vytváří poutavý kontrast vůči matné ploše.",
        ),
      ),
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
    guideSections = List(
      GuideSection(
        title = LocalizedString("Saddle Stitch vs Perfect Binding", "V-vazba vs lepená vazba"),
        body = LocalizedString(
          "Saddle stitch is the most common booklet binding — sheets are folded together and stapled through the spine with wire " +
            "staples. It's fast, affordable, and works beautifully for booklets from 8 to about 64 pages. Perfect binding uses " +
            "a glued spine to hold individual pages together inside a wrap-around cover, similar to a paperback novel. It requires " +
            "a minimum of about 48 pages to create enough spine width for the glue to grip. Choose saddle stitch for slim, " +
            "budget-friendly publications; choose perfect binding when your booklet is thicker, needs a professional spine, " +
            "or when you want that polished book-like appearance.",
          "V-vazba je nejběžnější vazba brožur — archy se složí dohromady a sešijí drátěnými svorkami přes hřbet. Je rychlá, " +
            "cenově dostupná a skvěle funguje pro brožury od 8 do přibližně 64 stran. Lepená vazba používá lepený hřbet " +
            "k držení jednotlivých stran uvnitř obalového archu, podobně jako u paperbackové knihy. Vyžaduje minimum asi 48 stran, " +
            "aby vznikla dostatečná šířka hřbetu pro lepidlo. Zvolte V-vazbu pro tenké, cenově výhodné publikace; " +
            "zvolte lepenou vazbu, když je vaše brožura tlustší, potřebuje profesionální hřbet " +
            "nebo když chcete vyleštěný knižní vzhled.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Choosing Cover vs Interior Paper", "Výběr papíru pro obálku a vnitřek"),
        body = LocalizedString(
          "Using different paper stocks for the cover and interior pages is standard practice in booklet production. A thicker, " +
            "sturdier cover (typically 250–350 gsm) protects the booklet and feels premium in the hand, while lighter interior " +
            "pages (100–170 gsm) keep the booklet flexible and cost-effective. A popular combination is a 300 gsm matte-laminated " +
            "cover with 130 gsm gloss interior pages. For perfect-bound booklets, remember that the cover needs to wrap around the " +
            "spine, so consult our spine width calculator to ensure your cover artwork is the right size.",
          "Použití různých papírů pro obálku a vnitřní stránky je standardní praxe při výrobě brožur. Silnější a odolnější " +
            "obálka (typicky 250–350 g/m²) chrání brožuru a působí prémiově, zatímco lehčí vnitřní stránky (100–170 g/m²) " +
            "zajistí flexibilitu a cenovou efektivitu. Oblíbenou kombinací je 300g matně laminovaná obálka se 130g lesklými " +
            "vnitřními stránkami. U lepených brožur pamatujte, že obálka musí obepnout hřbet, proto využijte naši kalkulačku " +
            "šířky hřbetu, abyste zajistili správnou velikost grafiky obálky.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Page Count Planning", "Plánování počtu stran"),
        body = LocalizedString(
          "For saddle-stitched booklets, the page count must be a multiple of 4 (8, 12, 16, 20, etc.) because each printed " +
            "sheet folds into four pages. If your content doesn't fill an exact multiple, you'll need to add or remove pages. " +
            "'Self-cover' means the cover is printed on the same paper as the interior — it's simpler and cheaper but less " +
            "durable. 'Plus-cover' uses a separate, heavier stock for the cover, giving a more polished, professional result. " +
            "When planning content, remember that the first and last pages are the front and back covers, so your actual content " +
            "area is the total page count minus four.",
          "U brožur s V-vazbou musí být počet stran násobkem 4 (8, 12, 16, 20 atd.), protože každý tištěný arch se skládá " +
            "na čtyři stránky. Pokud váš obsah přesně nezaplní násobek, budete muset stránky přidat nebo odebrat. " +
            "'Samoobálka' znamená, že obálka je tištěna na stejném papíru jako vnitřek — je jednodušší a levnější, ale méně " +
            "odolná. 'Přidaná obálka' používá pro obálku samostatný silnější papír pro uhlazenější profesionální výsledek. " +
            "Při plánování obsahu pamatujte, že první a poslední stránky jsou přední a zadní obálka, takže váš skutečný " +
            "prostor pro obsah je celkový počet stran minus čtyři.",
        ),
      ),
    ),
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
    guideSections = List(
      GuideSection(
        title = LocalizedString("Wall vs Desk Calendar Formats", "Nástěnné vs stolní kalendáře"),
        body = LocalizedString(
          "Wall calendars are typically A4 or A3 format, designed to hang from a hook or nail. They offer large monthly spreads " +
            "with plenty of room for striking photos or artwork on top and a date grid below. Desk calendars are smaller — usually " +
            "A5 or DL size — and stand upright on a triangular base or Wire-O binding. They sit on your workspace and provide " +
            "quick date reference at a glance. Wall calendars typically have 13–14 pages (cover plus 12 months), while desk " +
            "versions may include additional note pages. Consider your audience: wall calendars make great gifts and branding " +
            "tools, while desk calendars are practical everyday items.",
          "Nástěnné kalendáře jsou obvykle ve formátu A4 nebo A3, navržené k zavěšení na háček nebo hřebík. Nabízejí velké " +
            "měsíční plochy s dostatkem místa pro efektní fotografie nebo grafiku nahoře a datovou mřížku dole. Stolní kalendáře " +
            "jsou menší — obvykle A5 nebo DL — a stojí vzpřímeně na trojúhelníkové základně nebo Wire-O vazbě. Sedí na vašem " +
            "pracovním stole a poskytují rychlý přehled dat na první pohled. Nástěnné kalendáře mají typicky 13–14 stran (obálka " +
            "plus 12 měsíců), stolní verze mohou obsahovat další stránky na poznámky. Zvažte své publikum: nástěnné kalendáře " +
            "jsou skvělé dárky a brandingové nástroje, stolní kalendáře jsou praktické každodenní předměty.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Paper Selection for Calendars", "Výběr papíru pro kalendáře"),
        body = LocalizedString(
          "Calendar paper choice depends on whether it's hanging on a wall or standing on a desk. Wall calendars need paper " +
            "heavy enough to hang flat without curling — we recommend 200–250 gsm for interior pages and 300 gsm for the cover. " +
            "Gloss or silk finish shows off photographs beautifully. Desk calendars can use slightly lighter interior paper " +
            "(170–200 gsm) since the binding supports the weight. The cover or backing card should be at least 300 gsm for " +
            "structural stability. For both types, coated papers reproduce photos with sharper detail and richer colours than " +
            "uncoated stocks.",
          "Výběr papíru pro kalendáře závisí na tom, zda bude viset na zdi nebo stát na stole. Nástěnné kalendáře potřebují " +
            "papír dostatečně těžký, aby visel rovně bez kroucení — doporučujeme 200–250 g/m² pro vnitřní stránky a 300 g/m² " +
            "pro obálku. Lesklý nebo hedvábný povrch nádherně vynikne fotografiím. Stolní kalendáře mohou používat mírně lehčí " +
            "vnitřní papír (170–200 g/m²), protože váhu drží vazba. Obálka nebo zadní karta by měla mít alespoň 300 g/m² " +
            "pro strukturální stabilitu. Pro oba typy platí, že křídové papíry reprodukují fotky s ostřejšími detaily " +
            "a sytějšími barvami než nenatírané papíry.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Wire-O Binding Explained", "Vysvětlení Wire-O vazby"),
        body = LocalizedString(
          "Wire-O binding (also called twin-loop or double-loop wire) uses a continuous C-shaped metal wire that threads through " +
            "small punched holes along the spine edge. It allows pages to flip 360 degrees and lie perfectly flat — essential for " +
            "wall calendars that need to hang flush against the surface and desk calendars that fold back on themselves. The wire " +
            "comes in a range of colours (black, white, silver, and gold are popular) so you can match or complement your design. " +
            "Wire-O is the standard choice for calendars because it's durable, attractive, and functional.",
          "Wire-O vazba (také nazývaná dvojitá drátěná smyčka) používá průběžný kovový drát ve tvaru C, který prochází " +
            "malými proděravěnými otvory podél hřbetní hrany. Umožňuje stránkám překlápět se o 360 stupňů a ležet dokonale " +
            "naplocho — nezbytné pro nástěnné kalendáře, které musí viset těsně u povrchu, a stolní kalendáře, které se " +
            "překládají nazpět. Drát je k dispozici v řadě barev (oblíbené jsou černá, bílá, stříbrná a zlatá), " +
            "takže můžete ladit nebo doplnit svůj design. Wire-O je standardní volba pro kalendáře, protože je odolná, " +
            "atraktivní a funkční.",
        ),
      ),
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
    guideSections = List(
      GuideSection(
        title = LocalizedString("Indoor vs Outdoor Banner Materials", "Interiérové vs exteriérové bannerové materiály"),
        body = LocalizedString(
          "Indoor banners are typically printed on smooth, lightweight vinyl or fabric that hangs flat and looks great under " +
            "artificial lighting. They don't need weatherproofing, so the material can be thinner and more affordable. Outdoor " +
            "banners require heavier-duty vinyl (usually 440–550 gsm) designed to withstand rain, sun, and wind. For windy " +
            "locations, mesh vinyl is a smart choice — it has tiny perforations that let wind pass through, reducing strain on " +
            "the mounting points and preventing the banner from acting like a sail. Always specify indoor or outdoor use so we " +
            "can recommend the right material for your environment.",
          "Interiérové bannery se obvykle tisknou na hladký, lehký vinyl nebo textilii, která visí rovně a vypadá skvěle " +
            "pod umělým osvětlením. Nepotřebují ochranu proti povětrnosti, takže materiál může být tenčí a cenově dostupnější. " +
            "Exteriérové bannery vyžadují odolnější vinyl (obvykle 440–550 g/m²) navržený tak, aby odolával dešti, slunci a větru. " +
            "Pro větrné lokality je chytrá volba mesh vinyl — má drobné perforace, které propouštějí vítr, snižují namáhání " +
            "upevňovacích bodů a zabraňují banneru chovat se jako plachta. Vždy uveďte, zda jde o interiérové nebo exteriérové " +
            "použití, abychom mohli doporučit správný materiál pro vaše prostředí.",
        ),
      ),
      GuideSection(
        title = LocalizedString("UV Printing Technology", "Technologie UV tisku"),
        body = LocalizedString(
          "UV printing uses special inks that are instantly cured (hardened) by ultraviolet light as they're applied to the " +
            "material. Unlike traditional solvent inks that dry by evaporation, UV-cured inks form a tough, flexible layer that " +
            "bonds directly to the vinyl surface. This means the colours are vibrant from day one and stay that way — UV-printed " +
            "banners resist fading from sunlight for up to 2–3 years outdoors. The process also produces less odour and fewer " +
            "volatile compounds than solvent printing, making it a more environmentally responsible choice.",
          "UV tisk používá speciální inkousty, které jsou okamžitě vytvrzeny (ztvrdnou) ultrafialovým světlem při nanášení " +
            "na materiál. Na rozdíl od tradičních solventních inkoustů, které schnou odpařováním, UV inkousty vytvářejí pevnou " +
            "flexibilní vrstvu, která se přímo váže na vinylový povrch. To znamená, že barvy jsou živé od prvního dne a takové " +
            "zůstanou — UV tištěné bannery odolávají vyblednutí od slunce až 2–3 roky v exteriéru. Proces také produkuje méně " +
            "zápachu a méně těkavých látek než solventní tisk, což z něj dělá ekologicky odpovědnější volbu.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Installation & Finishing Options", "Instalace a dokončovací úpravy"),
        body = LocalizedString(
          "Grommets (metal eyelets) are the most popular finishing option — they're punched into the corners and edges of the " +
            "banner, providing sturdy attachment points for ropes, zip ties, or bungee cords. Pole pockets are sleeves sewn along " +
            "the top and/or bottom edges that slide over a pole or dowel for a clean, smooth display. Hemming reinforces the " +
            "banner edges by folding and heat-welding the material, preventing fraying and extending the banner's life. For " +
            "indoor displays, simple top-only grommets or a hanging rail sleeve may be all you need. For outdoor use, we " +
            "recommend full hemming plus grommets every 50–60 cm along the edges.",
          "Průchodky (kovové očka) jsou nejoblíbenější dokončovací úpravou — jsou proraženy v rozích a hranách banneru " +
            "a poskytují pevné upevňovací body pro lana, stahovací pásky nebo gumové popruhy. Tunýlky pro tyč jsou kapsy " +
            "sešité podél horní a/nebo dolní hrany, do kterých se zasune tyč nebo kolík pro čistou hladkou prezentaci. " +
            "Olemování zpevňuje okraje banneru přeložením a svařením materiálu, zabraňuje třepení a prodlužuje životnost. " +
            "Pro interiérové displeje mohou stačit jednoduché průchodky jen nahoře nebo lišta na zavěšení. Pro exteriérové " +
            "použití doporučujeme plné olemování plus průchodky každých 50–60 cm podél okrajů.",
        ),
      ),
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
    guideSections = List(
      GuideSection(
        title = LocalizedString("Choosing Economy vs Premium Stands", "Výběr ekonomického vs prémiového stojanu"),
        body = LocalizedString(
          "Economy roll-up stands are made from lightweight aluminium and typically weigh around 2–3 kg. They're perfect for " +
            "occasional use — a few trade shows or events per year — and come at a fraction of the cost of premium models. " +
            "Premium stands feature a wider, heavier base (4–5 kg), a more robust retraction mechanism, and often include a " +
            "lifetime warranty on the hardware. If you exhibit frequently or transport your display often, the premium stand pays " +
            "for itself through durability. Both types accept the same standard banner film, so you can always upgrade the stand " +
            "later without reprinting your graphic.",
          "Ekonomické rolovací stojany jsou vyrobeny z lehkého hliníku a obvykle váží kolem 2–3 kg. Jsou ideální pro " +
            "příležitostné použití — několik veletrhů nebo akcí ročně — a stojí zlomek ceny prémiových modelů. " +
            "Prémiové stojany mají širší, těžší základnu (4–5 kg), robustnější navíjecí mechanismus a často zahrnují " +
            "doživotní záruku na hardware. Pokud vystavujete často nebo svůj displej často přepravujete, prémiový stojan se " +
            "zaplatí svou odolností. Oba typy přijímají stejnou standardní bannerovou fólii, takže můžete stojan kdykoli " +
            "upgradovat bez přetisku grafiky.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Banner Film Materials", "Materiály bannerové fólie"),
        body = LocalizedString(
          "Roll-up banner film is a specialized material designed to retract smoothly into the cassette without curling or " +
            "creasing. Standard PVC film (400–500 micron) is the most common — it's durable, prints beautifully, and rolls " +
            "tightly without damage. Anti-curl film has a special backing layer that prevents the edges from curling outward " +
            "after extended use, keeping your banner looking crisp. Polyester-based films are a greener alternative, free of " +
            "PVC and fully recyclable. For the sharpest results, our banner films support printing at up to 1440 DPI, ensuring " +
            "your images and text look razor-sharp even from close viewing distances.",
          "Bannerová fólie pro roll-upy je specializovaný materiál navržený tak, aby se hladce navíjel do kazety bez " +
            "kroucení nebo lámání. Standardní PVC fólie (400–500 mikronů) je nejběžnější — je odolná, krásně se na ní tiskne " +
            "a těsně se navíjí bez poškození. Anti-curl fólie má speciální podkladovou vrstvu, která zabraňuje kroucení okrajů " +
            "směrem ven po delším používání, aby váš banner vypadal stále ostře. Fólie na bázi polyesteru jsou zelenější " +
            "alternativou, bez PVC a plně recyklovatelné. Pro nejostřejší výsledky naše bannerové fólie podporují tisk " +
            "až do 1440 DPI, což zajistí, že vaše obrázky a text vypadají ostře i při pohledu zblízka.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Transport & Care Tips", "Tipy pro přepravu a údržbu"),
        body = LocalizedString(
          "Every roll-up stand comes with a padded carrying case — always use it during transport to protect the mechanism and " +
            "banner from bumps and scratches. When not in use, store the roll-up retracted in its case in a cool, dry place away " +
            "from direct sunlight to prevent the print from fading or the film from warping. To clean the banner surface, use a " +
            "soft damp cloth and avoid abrasive cleaners that could scratch the print. When your graphic needs updating, you can " +
            "order a replacement banner film without buying a new stand — simply slide the old banner out and attach the new one.",
          "Každý rolovací stojan je dodáván s polstrovaným přepravním pouzdrem — vždy ho používejte při přepravě k ochraně " +
            "mechanismu a banneru před nárazy a poškrábáním. Když roll-up nepoužíváte, skladujte ho zavinutý v pouzdře na " +
            "chladném suchém místě mimo přímé sluneční světlo, abyste předešli vyblednutí potisku nebo deformaci fólie. " +
            "K čištění povrchu banneru použijte měkký vlhký hadřík a vyhněte se abrazivním čistidlům, která by mohla " +
            "poškrábat potisk. Když je potřeba grafiku aktualizovat, můžete objednat náhradní bannerovou fólii bez nákupu " +
            "nového stojanu — jednoduše starou fólii vysuňte a připevněte novou.",
        ),
      ),
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
    guideSections = List(
      GuideSection(
        title = LocalizedString("Material Guide for Packaging", "Průvodce materiály pro obaly"),
        body = LocalizedString(
          "Kraft paper is the natural, eco-friendly choice — its signature brown colour signals sustainability and works " +
            "beautifully for artisanal and organic brands. It's sturdy, recyclable, and can be printed with excellent results. " +
            "Corrugated cardboard has a fluted inner layer sandwiched between flat sheets, providing outstanding crush resistance " +
            "and cushioning for heavier or fragile products. Rigid board (greyboard wrapped in printed paper) is the premium " +
            "option used for luxury gift boxes, electronics packaging, and cosmetics — it won't flex or collapse. Consider your " +
            "product weight and brand positioning when choosing: kraft for eco appeal, corrugated for protection, rigid for luxury.",
          "Kraftový papír je přirozená ekologická volba — jeho typická hnědá barva signalizuje udržitelnost a krásně funguje " +
            "pro řemeslné a bio značky. Je pevný, recyklovatelný a tiskne se na něj s vynikajícími výsledky. " +
            "Vlnitá lepenka má zvlněnou vnitřní vrstvu mezi plochými archy, což poskytuje vynikající odolnost proti zmačkání " +
            "a tlumení pro těžší nebo křehké produkty. Tuhá lepenka (šedá lepenka obalená potištěným papírem) je prémiová " +
            "varianta používaná pro luxusní dárkové krabice, elektroniku a kosmetiku — neohne se ani nesloží. Zvažte hmotnost " +
            "produktu a pozici značky: kraft pro ekologický apel, vlnitou lepenku pro ochranu, tuhou lepenku pro luxus.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Die-Cutting Explained", "Vysvětlení výseku"),
        body = LocalizedString(
          "Die-cutting is a manufacturing process that uses a custom-shaped metal blade (called a die) to cut packaging material " +
            "into precise shapes — think of it like a giant cookie cutter for cardboard. The die is created from your packaging " +
            "design template, including all fold lines (scored, not cut through), cut lines, and window openings. This means your " +
            "packaging can be any shape you imagine: hexagonal boxes, boxes with windows, pillow packs, or complex multi-chamber " +
            "inserts. When designing for die-cut packaging, work from a dieline template that shows exactly where cuts and folds " +
            "will occur, and keep important artwork at least 3 mm from any fold or cut line.",
          "Výsek je výrobní proces, který používá kovové ostří na míru (zvané výseková forma) k řezání obalového materiálu " +
            "do přesných tvarů — představte si to jako obří vykrajovátko na lepenku. Forma se vytváří podle vaší obalové " +
            "designové šablony, včetně všech přehybových linií (bigovaných, ne prořezaných), řezných linií a okenních otvorů. " +
            "To znamená, že váš obal může mít jakýkoli tvar: šestiúhelníkové krabice, krabice s okénky, polštářkové obaly " +
            "nebo složité vícekomorové vložky. Při navrhování pro výsekové obaly pracujte ze šablony výsekové linie, která " +
            "přesně ukazuje, kde budou řezy a přehyby, a důležitou grafiku udržujte alespoň 3 mm od jakékoli linie.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Premium Finishing for Packaging", "Prémiové úpravy pro obaly"),
        body = LocalizedString(
          "Embossing raises selected design elements above the surface of the packaging, creating a three-dimensional effect you " +
            "can see and feel. Debossing does the opposite — it presses elements into the surface for a subtle, elegant impression. " +
            "Foil stamping applies thin metallic or holographic foil to specific areas using heat and pressure, adding eye-catching " +
            "shine to logos, text, or patterns. Spot UV applies a high-gloss coating only to chosen design elements, creating a " +
            "striking contrast against a matte background. These finishes dramatically enhance the unboxing experience and are " +
            "especially effective for premium brands, gift packaging, and limited-edition product runs.",
          "Slepotisk (embossing) zdvihá vybrané designové prvky nad povrch obalu a vytváří trojrozměrný efekt, který můžete " +
            "vidět i cítit. Debossing dělá opak — vtlačuje prvky do povrchu pro jemný elegantní dojem. Ražba fólií nanáší " +
            "tenkou metalickou nebo holografickou fólii na vybrané oblasti pomocí tepla a tlaku, čímž dodává poutavý lesk " +
            "logům, textu nebo vzorům. Spot UV nanáší vysoce lesklý lak pouze na vybrané designové prvky a vytváří výrazný " +
            "kontrast vůči matnému pozadí. Tyto úpravy dramaticky zlepšují zážitek z rozbalování a jsou obzvláště účinné " +
            "pro prémiové značky, dárkové balení a limitované edice produktů.",
        ),
      ),
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
    guideSections = List(
      GuideSection(
        title = LocalizedString("Sticker Material Comparison", "Srovnání materiálů pro samolepky"),
        body = LocalizedString(
          "Paper adhesive stickers are the most affordable option — great for indoor labels, product packaging seals, and " +
            "promotional giveaways. They print beautifully but aren't waterproof. Vinyl stickers are thicker, waterproof, and " +
            "UV-resistant, making them ideal for laptop stickers, water bottles, and car bumpers. Clear vinyl has a transparent " +
            "background that lets the surface show through — perfect for window decals or when you want the sticker to look " +
            "painted on. Yupo is a synthetic paper that combines the printability of paper with waterproof durability; it's " +
            "tear-resistant and excellent for outdoor labels and harsh environments.",
          "Papírové samolepky jsou nejdostupnější variantou — skvělé pro interiérové etikety, pečeti na balení a propagační " +
            "dárky. Krásně se na ně tiskne, ale nejsou voděodolné. Vinylové samolepky jsou silnější, voděodolné a odolné UV, " +
            "což je činí ideálními pro nálepky na notebooky, lahve na vodu a nárazníky aut. Průhledný vinyl má transparentní " +
            "pozadí, které prosvítá povrch — ideální pro okenní polepy nebo když chcete, aby samolepka vypadala jako " +
            "namalovaná. Yupo je syntetický papír kombinující tiskové vlastnosti papíru s voděodolnou odolností; je odolný " +
            "proti roztržení a vynikající pro venkovní etikety a náročná prostředí.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Cutting Methods Explained", "Vysvětlení metod řezu"),
        body = LocalizedString(
          "Kiss-cut means the blade cuts through the sticker material but stops at the backing paper, leaving the sticker on " +
            "its sheet — this is how sticker sheets work, making it easy to peel off individual stickers. Die-cut goes all the " +
            "way through both the sticker and the backing, creating individually separated stickers in custom shapes. Contour " +
            "cut (also called cut-to-shape) follows the outline of your design precisely, producing stickers with no excess " +
            "border — the shape of the sticker matches the shape of your artwork. Choose kiss-cut for sticker sheets, die-cut " +
            "for individually handed-out stickers, and contour cut for a polished, professional look.",
          "Kiss-cut znamená, že nůž prořízne materiál samolepky, ale zastaví se na podkladovém papíru a ponechá samolepku " +
            "na archu — takto fungují samolepkové archy, které umožňují snadno odlepovat jednotlivé kusy. Die-cut prořízne " +
            "celým materiálem samolepky i podkladem a vytváří individuálně oddělené samolepky ve vlastních tvarech. Obrysový " +
            "řez (contour cut) přesně sleduje obrys vašeho designu a vytváří samolepky bez přebytečného okraje — tvar " +
            "samolepky odpovídá tvaru vaší grafiky. Zvolte kiss-cut pro samolepkové archy, die-cut pro individuálně " +
            "rozdávané samolepky a obrysový řez pro vyleštěný profesionální vzhled.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Indoor vs Outdoor Durability", "Odolnost v interiéru vs exteriéru"),
        body = LocalizedString(
          "Indoor stickers face minimal environmental stress, so standard paper or thin vinyl works perfectly — they'll last " +
            "for years on notebooks, packaging, or store displays. Outdoor stickers must withstand rain, sunlight, temperature " +
            "swings, and physical abrasion. For outdoor use, choose vinyl or Yupo material printed with UV-resistant inks. " +
            "Adding a clear laminate overlay provides an extra layer of UV and scratch protection, extending the sticker's outdoor " +
            "life to 3–5 years. The adhesive type matters too: permanent adhesive bonds strongly to surfaces, while removable " +
            "adhesive lets you peel the sticker off cleanly without residue.",
          "Interiérové samolepky čelí minimálnímu vlivu prostředí, takže standardní papír nebo tenký vinyl funguje perfektně — " +
            "vydrží roky na noteboocích, baleních nebo v obchodních výlohách. Exteriérové samolepky musí odolávat dešti, " +
            "slunečnímu záření, teplotním výkyvům a fyzickému oděru. Pro venkovní použití zvolte vinylový nebo Yupo materiál " +
            "potištěný UV odolnými inkousty. Přidání průhledné laminace poskytne další vrstvu UV a ochranné ochrany proti " +
            "poškrábání a prodlouží venkovní životnost samolepky na 3–5 let. Záleží i na typu lepidla: permanentní lepidlo " +
            "se silně váže k povrchu, zatímco odnímatelné lepidlo umožňuje samolepku čistě odlepit bez zbytků.",
        ),
      ),
    ),
    popularFinishes = List("Kiss Cut", "Die Cut", "Round Corners"),
    turnaroundDays = Some("3-5"),
    sortOrder = 31,
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
    guideSections = List(
      GuideSection(
        title = LocalizedString("Screen Printing vs DTG vs Sublimation", "Sítotisk vs DTG vs sublimace"),
        body = LocalizedString(
          "Screen printing pushes ink through a mesh stencil onto fabric — each colour requires a separate screen, so it's most " +
            "cost-effective for simple designs (1–4 colours) in large quantities (50+ pieces). The ink sits on top of the fabric, " +
            "producing bold, vibrant colours that last hundreds of washes. DTG (Direct-to-Garment) works like an inkjet printer " +
            "for fabric, spraying detailed full-colour designs directly into the fibres. It's perfect for photo-realistic prints " +
            "and small batches, but costs more per unit. Sublimation uses heat to transfer dye into polyester fibres, creating " +
            "permanent all-over prints that won't crack or peel — ideal for sports jerseys and fashion pieces, but only works " +
            "on white or light polyester fabrics.",
          "Sítotisk protlačuje barvu přes síťovou šablonu na textilii — každá barva vyžaduje samostatnou šablonu, proto je " +
            "nejefektivnější pro jednoduché designy (1–4 barvy) ve velkých nákladech (50+ kusů). Barva sedí na povrchu " +
            "textilie a vytváří výrazné živé barvy, které vydrží stovky praní. DTG (přímý tisk na textil) funguje jako " +
            "inkoustová tiskárna pro textil a vstřikuje detailní plnobarevné designy přímo do vláken. Je ideální pro " +
            "fotorealistické potisky a malé série, ale stojí více za kus. Sublimace využívá teplo k přenosu barviva do " +
            "polyesterových vláken a vytváří trvalé celoplošné potisky, které nepraskají ani se neloupou — ideální pro " +
            "sportovní dresy a módní kousky, ale funguje pouze na bílých nebo světlých polyesterových textiliích.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Fabric Guide", "Průvodce textiliemi"),
        body = LocalizedString(
          "100% cotton is the classic choice — soft, breathable, and comfortable for everyday wear. It's compatible with screen " +
            "printing and DTG but not sublimation. Organic cotton offers the same comfort with certified sustainable farming " +
            "practices. Polyester is lightweight, moisture-wicking, and perfect for sportswear and sublimation printing. " +
            "Cotton-polyester blends (like 65/35 or 50/50) combine the softness of cotton with polyester's durability and wrinkle " +
            "resistance. Heavier fabrics (180 gsm) feel more premium and drape better, while lighter options (140–150 gsm) are " +
            "great for promotional giveaways and warm-weather events.",
          "100% bavlna je klasická volba — měkká, prodyšná a pohodlná pro každodenní nošení. Je kompatibilní se sítotiskem " +
            "a DTG, ale ne se sublimací. Bio bavlna nabízí stejný komfort s certifikovanými udržitelnými zemědělskými postupy. " +
            "Polyester je lehký, odvádí vlhkost a je ideální pro sportovní oblečení a sublimační tisk. Bavlněno-polyesterové " +
            "směsi (jako 65/35 nebo 50/50) kombinují měkkost bavlny s odolností polyesteru a odolností vůči pomačkání. " +
            "Těžší textilie (180 g/m²) působí prémiověji a lépe padnou, zatímco lehčí varianty (140–150 g/m²) jsou skvělé " +
            "pro propagační dárky a letní akce.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Preparing Artwork for Textile Printing", "Příprava grafiky pro tisk na textil"),
        body = LocalizedString(
          "For screen printing, design in spot colours (Pantone) rather than CMYK for the most accurate colour matching — each " +
            "colour in your design maps to one screen. For DTG and sublimation, use CMYK or RGB colour mode with images at 300 DPI " +
            "at the actual print size. Keep your design at least 150 mm wide for chest prints — anything smaller can look lost on " +
            "the garment. Common placement options include left chest (logo), full front, full back, and sleeve prints. Always " +
            "provide your artwork on a transparent background (PNG format) so only your design prints on the fabric, not a " +
            "white rectangle around it.",
          "Pro sítotisk navrhujte v přímých barvách (Pantone) místo CMYK pro nejpřesnější shodu barev — každá barva " +
            "v designu odpovídá jedné šabloně. Pro DTG a sublimaci použijte barevný režim CMYK nebo RGB s obrázky v 300 DPI " +
            "ve skutečné tiskové velikosti. Udržujte design alespoň 150 mm široký pro potisk na hrudi — cokoli menšího může " +
            "na oblečení působit ztraceně. Běžné možnosti umístění zahrnují levou stranu hrudi (logo), celou přední stranu, " +
            "celou zadní stranu a potisk rukávu. Grafiku vždy dodejte na průhledném pozadí (formát PNG), aby se na textilii " +
            "tiskl pouze váš design, ne bílý obdélník kolem něj.",
        ),
      ),
    ),
    popularFinishes = List("Screen Printing", "DTG Full Color", "Sublimation"),
    turnaroundDays = Some("5-7"),
    sortOrder = 40,
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
    guideSections = List(
      GuideSection(
        title = LocalizedString("Material Sustainability Guide", "Průvodce udržitelností materiálů"),
        body = LocalizedString(
          "Organic cotton is grown without synthetic pesticides or fertilisers, certified by standards like GOTS (Global Organic " +
            "Textile Standard). It feels just like regular cotton but has a significantly lower environmental impact. Recycled PET " +
            "fabric is made from post-consumer plastic bottles — each standard tote uses roughly 5–8 recycled bottles, giving " +
            "plastic waste a second life. Jute is a natural plant fibre that's 100% biodegradable, grows quickly without " +
            "irrigation, and has a distinctive rustic texture. When choosing a material, consider your brand message: organic " +
            "cotton for premium eco-conscious brands, recycled PET for circular economy messaging, and jute for natural, " +
            "earth-friendly aesthetics.",
          "Bio bavlna se pěstuje bez syntetických pesticidů nebo hnojiv a je certifikována standardy jako GOTS (Global Organic " +
            "Textile Standard). Působí stejně jako běžná bavlna, ale má výrazně nižší dopad na životní prostředí. Textilie " +
            "z recyklovaného PET se vyrábí z použitých plastových lahví — každá standardní taška využívá přibližně 5–8 " +
            "recyklovaných lahví, čímž dává plastovému odpadu druhý život. Juta je přírodní rostlinné vlákno, které je 100% " +
            "biologicky odbouratelné, rychle roste bez zavlažování a má výraznou rustikální texturu. Při výběru materiálu " +
            "zvažte svou značkovou zprávu: bio bavlnu pro prémiové ekologicky uvědomělé značky, recyklovaný PET pro cirkulární " +
            "ekonomiku a jutu pro přírodní ekologickou estetiku.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Print & Decoration Methods", "Tiskové a dekorační metody"),
        body = LocalizedString(
          "Screen printing is the most popular method for eco bags — it's durable, vibrant, and cost-effective for medium to " +
            "large runs. It works exceptionally well on cotton canvas and jute. DTG (Direct-to-Garment) printing is ideal when " +
            "you need full-colour photographic designs or small batches, though it works best on smooth cotton surfaces. Heat " +
            "transfer vinyl is great for small, detailed logos and metallic effects. Embroidery stitches your design directly " +
            "into the fabric with coloured thread — it's the most premium and durable decoration method, adding texture and " +
            "a luxury feel that printing cannot match. Best suited for simple logos with 1–6 thread colours.",
          "Sítotisk je nejoblíbenější metoda pro eko tašky — je odolný, živý a cenově efektivní pro střední a velké náklady. " +
            "Výborně funguje na bavlněném plátně a jutě. DTG (přímý tisk) je ideální, když potřebujete plnobarevné fotografické " +
            "designy nebo malé série, i když nejlépe funguje na hladkých bavlněných površích. Přenos termofólií je skvělý " +
            "pro malá detailní loga a metalické efekty. Výšivka vyšívá váš design přímo do textilie barevnou nití — je to " +
            "nejprémiovější a nejodolnější dekorační metoda, která přidává texturu a luxusní dojem, se kterým se tisk nemůže " +
            "srovnávat. Nejvhodnější pro jednoduchá loga s 1–6 barvami nití.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Handle Types & Construction", "Typy uch a konstrukce"),
        body = LocalizedString(
          "Short handles (about 30 cm) are designed for carrying the bag by hand at your side — they're the classic tote bag " +
            "style and work well for shopping bags and gift bags. Long shoulder straps (55–65 cm) allow the bag to hang " +
            "comfortably over the shoulder, making them more practical for everyday use and commuting. Some designs offer both " +
            "short and long handles for maximum versatility. Handle reinforcement matters for durability: cross-stitched handles " +
            "that continue down the sides of the bag distribute weight more evenly and can carry heavier loads. For heavy-duty " +
            "bags, look for double-stitched handles with box-tack reinforcement at the stress points.",
          "Krátká ucha (asi 30 cm) jsou navržena pro nošení tašky rukou po boku — jsou to klasické nákupní tašky, " +
            "které dobře fungují jako nákupní a dárkové tašky. Dlouhé ramenní popruhy (55–65 cm) umožňují tašku pohodlně " +
            "nosit přes rameno, což je praktičtější pro každodenní použití a dojíždění. Některé designy nabízejí jak krátká, " +
            "tak dlouhá ucha pro maximální univerzálnost. Zpevnění uch je důležité pro odolnost: křížově prošitá ucha, " +
            "která pokračují po stranách tašky, lépe rozkládají váhu a unesou těžší náklady. Pro tašky na vysokou zátěž " +
            "hledejte dvojitě prošitá ucha s box-tack zpevněním v místech namáhání.",
        ),
      ),
    ),
    popularFinishes = List("Screen Printing", "Heat Transfer", "Embroidery"),
    turnaroundDays = Some("7-10"),
    sortOrder = 41,
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
    guideSections = List(
      GuideSection(
        title = LocalizedString("Badge Material Options", "Možnosti materiálů pro odznaky"),
        body = LocalizedString(
          "Tinplate (steel) badges are the classic choice — lightweight, affordable, and perfect for events, campaigns, and " +
            "promotional giveaways. They produce sharp, vibrant prints protected under a clear mylar film. Acrylic badges have " +
            "a modern, glossy appearance with more depth and a slight transparency at the edges, making them feel more premium. " +
            "Wooden badges offer a unique natural aesthetic — each piece has subtle grain variations that make it one-of-a-kind. " +
            "They're perfect for eco-conscious brands, craft events, and artisanal markets. Consider your occasion: tinplate " +
            "for large events, acrylic for retail merchandise, and wood for boutique and sustainable branding.",
          "Plechové (ocelové) odznaky jsou klasická volba — lehké, cenově dostupné a ideální pro akce, kampaně a propagační " +
            "dárky. Vytvářejí ostrý, živý potisk chráněný průhlednou mylarovou fólií. Akrylátové odznaky mají moderní lesklý " +
            "vzhled s větší hloubkou a mírnou průhledností na okrajích, díky čemuž působí prémiověji. Dřevěné odznaky nabízejí " +
            "unikátní přírodní estetiku — každý kus má jemné variace ve struktuře dřeva, což ho činí jedinečným. Jsou ideální " +
            "pro ekologické značky, řemeslné akce a trhy. Zvažte příležitost: plech pro velké akce, akrylát pro maloobchodní " +
            "merchandising a dřevo pro butikový a udržitelný branding.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Attachment Back Types", "Typy zadního uchycení"),
        body = LocalizedString(
          "The standard safety pin back is the most versatile — it pins securely through fabric and is easy to attach and remove. " +
            "It's the best choice for clothing, lanyards, and tote bags. Magnet backs use two strong neodymium magnets that grip " +
            "through fabric without piercing it — ideal for delicate garments, suits, and situations where you don't want pin " +
            "holes. Bottle opener backs turn your badge into a functional keepsake that people actually use and keep. Butterfly " +
            "clutch backs (also called military clutch) use a spring-loaded clasp for a secure, professional attachment — popular " +
            "for corporate name badges and staff identification.",
          "Standardní zavírací špendlík je nejuniverzálnější — bezpečně se připne přes textilii a snadno se nasazuje a sundává. " +
            "Je nejlepší volbou pro oblečení, šňůrky na krk a tašky. Magnetové uchycení používá dva silné neodymové magnety, " +
            "které drží přes textilii bez propíchnutí — ideální pro delikátní oděvy, obleky a situace, kdy nechcete dírky " +
            "od špendlíku. Uchycení s otvírákem na lahve promění váš odznak ve funkční upomínkový předmět, který lidé skutečně " +
            "používají a uchovávají. Motýlkový uzávěr (butterfly clutch) používá pružinovou svorku pro bezpečné profesionální " +
            "uchycení — oblíbené pro firemní jmenovky a identifikaci zaměstnanců.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Design Tips for Small Formats", "Designové tipy pro malé formáty"),
        body = LocalizedString(
          "Badges are small, so simplicity is key — avoid fine details and tiny text that won't be legible at 32 or 58 mm. " +
            "As a rule, keep text at least 2 mm tall (about 6pt) for readability. Use bold, high-contrast colours; a bright " +
            "design on a dark background (or vice versa) reads much better than subtle tones at badge size. For round badges, " +
            "remember that the outer 2–3 mm wraps around the edge during assembly, so keep all important elements within the " +
            "central safe area. Simple icons, bold logos, and short slogans work far better than complex illustrations on " +
            "something this size.",
          "Odznaky jsou malé, proto je klíčem jednoduchost — vyhněte se jemným detailům a drobnému textu, který nebude " +
            "čitelný při 32 nebo 58 mm. Obecné pravidlo: udržujte text alespoň 2 mm vysoký (asi 6 bodů) pro čitelnost. " +
            "Používejte výrazné kontrastní barvy; jasný design na tmavém pozadí (nebo naopak) je mnohem lépe čitelný " +
            "než jemné tóny ve velikosti odznaku. U kulatých odznaků pamatujte, že vnější 2–3 mm se při montáži ohýbají " +
            "přes okraj, takže všechny důležité prvky udržujte v centrální bezpečné oblasti. Jednoduché ikony, výrazná loga " +
            "a krátké slogany fungují mnohem lépe než složité ilustrace v tak malém formátu.",
        ),
      ),
    ),
    popularFinishes = List("Mylar Overlay", "Safety Pin", "Magnet Back"),
    turnaroundDays = Some("3-5"),
    sortOrder = 42,
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
    guideSections = List(
      GuideSection(
        title = LocalizedString("Material Comparison", "Srovnání materiálů"),
        body = LocalizedString(
          "White ceramic is the classic mug material — affordable, excellent for sublimation printing with vivid wrap-around " +
            "graphics, and comfortable to drink from. It retains heat well but is fragile if dropped. Stainless steel travel mugs " +
            "are virtually indestructible, keep drinks hot or cold for hours with double-wall insulation, and work great for " +
            "on-the-go branding. Enamel mugs have a charming vintage look with a steel core coated in vitreous enamel — they're " +
            "durable and oven-safe but can chip on the rim over time. Glass mugs look elegant and are perfect for showcasing " +
            "layered beverages, though they're the most delicate option for everyday use.",
          "Bílá keramika je klasický hrnkový materiál — cenově dostupný, vynikající pro sublimační tisk s živými celoplošnými " +
            "grafikami a příjemný na pití. Dobře drží teplo, ale je křehký při pádu. Nerezové cestovní hrnky jsou prakticky " +
            "nezničitelné, udrží nápoje horké nebo studené po hodiny díky dvouplášťové izolaci a skvěle fungují pro branding " +
            "na cestách. Smaltované hrnky mají okouzlující vintage vzhled s ocelovým jádrem pokrytým sklovitým smaltem — jsou " +
            "odolné a vhodné do trouby, ale okraj může časem odštípnout. Skleněné hrnky vypadají elegantně a jsou ideální " +
            "pro prezentaci vrstvených nápojů, i když jsou nejkřehčí variantou pro každodenní použití.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Printing Methods for Drinkware", "Tiskové metody pro nápojové nádobí"),
        body = LocalizedString(
          "Sublimation is the gold standard for ceramic mugs — heat and pressure transfer dye directly into the ceramic coating, " +
            "producing photo-quality wrap-around prints that won't peel, crack, or fade. It only works on white or light-coloured " +
            "ceramics. Screen printing applies ink to the mug surface through a stencil — it's durable and cost-effective for " +
            "simple 1–3 colour logos on large runs. UV direct printing uses ultraviolet-cured inks sprayed directly onto any " +
            "surface, including coloured mugs, glass, and stainless steel. It supports full-colour graphics in a spot position " +
            "rather than wrap-around. For dishwasher safety, sublimation prints are the most durable, followed by properly cured " +
            "screen prints — always hand-wash UV-printed items for best longevity.",
          "Sublimace je zlatý standard pro keramické hrnky — teplo a tlak přenášejí barvivo přímo do keramického nátěru, " +
            "čímž vznikají fotokvalitní celoplošné potisky, které se neodlupují, nepraskají ani nevyblednou. Funguje pouze na " +
            "bílé nebo světlé keramice. Sítotisk nanáší barvu na povrch hrnku přes šablonu — je odolný a cenově efektivní " +
            "pro jednoduchá 1–3 barevná loga ve velkých nákladech. UV přímý tisk používá ultrafialově vytvrzené inkousty " +
            "stříkané přímo na jakýkoli povrch, včetně barevných hrnků, skla a nerezu. Podporuje plnobarevnou grafiku na " +
            "určitém místě, nikoliv celoplošně. Pro odolnost v myčce jsou sublimační potisky nejodolnější, následované " +
            "správně vypálenými sítotisky — UV tištěné předměty vždy myjte ručně pro nejdelší životnost.",
        ),
      ),
      GuideSection(
        title = LocalizedString("Gift Packaging Options", "Možnosti dárkového balení"),
        body = LocalizedString(
          "A beautifully packaged mug transforms a simple promotional item into a memorable gift. Individual white or kraft " +
            "cardboard boxes protect the mug during shipping and provide a clean canvas for branding. Custom-printed boxes with " +
            "your logo and design create a complete branded unboxing experience — ideal for corporate gift sets and e-commerce. " +
            "Foam or corrugated inserts hold the mug securely inside the box, preventing movement and breakage during transit. " +
            "For premium presentations, consider window boxes that let the recipient peek at the mug inside, or add tissue paper " +
            "and a branded sticker seal for an extra-special touch.",
          "Krásně zabalený hrnek promění jednoduchý propagační předmět v nezapomenutelný dárek. Individuální bílé nebo " +
            "kraftové kartonové krabičky chrání hrnek při přepravě a poskytují čistý podklad pro branding. Krabičky s vlastním " +
            "potiskem s vaším logem a designem vytvářejí kompletní značkový zážitek z rozbalování — ideální pro firemní " +
            "dárkové sety a e-commerce. Pěnové nebo vlnité vložky drží hrnek bezpečně uvnitř krabičky a zabraňují pohybu " +
            "a rozbití při přepravě. Pro prémiové prezentace zvažte krabičky s okénkem, které umožňují příjemci nahlédnout " +
            "na hrnek uvnitř, nebo přidejte hedvábný papír a značkovou samolepku pro extra speciální dojem.",
        ),
      ),
    ),
    popularFinishes = List("Sublimation Wrap-Around", "UV Direct Print", "Gift Box"),
    turnaroundDays = Some("5-7"),
    sortOrder = 43,
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
