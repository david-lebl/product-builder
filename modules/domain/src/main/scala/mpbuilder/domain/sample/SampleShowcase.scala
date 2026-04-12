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
        "and matte or gloss lamination. Available in digital and letterpress printing." +
        "\n\n" +
        "Paper stock matters more than you might think. Coated papers (glossy or silk) produce the sharpest " +
        "colors and are the most popular choice for corporate cards. Uncoated stocks offer a natural, " +
        "writable surface — ideal if recipients need to jot down notes. Cotton paper feels soft and premium, " +
        "often chosen for luxury brands. Kraft paper gives an eco-friendly, artisanal look. Yupo is a " +
        "synthetic, tear-proof and waterproof material that stands out from traditional paper entirely." +
        "\n\n" +
        "When selecting a finish, think about what impression you want to make. Matte lamination provides " +
        "a smooth, elegant feel and protects against fingerprints. Gloss lamination makes colors pop and " +
        "gives a vibrant, eye-catching surface. Soft-touch (or velvet) coating creates a luxurious, suede-like " +
        "texture that people love to hold. Foil stamping (gold, silver, or holographic) adds metallic shine to " +
        "logos or text, while embossing creates a raised, tactile pattern you can feel with your fingertips." +
        "\n\n" +
        "For printing, digital printing is fast, cost-effective, and ideal for short runs with full-color " +
        "designs. Letterpress is a traditional technique that physically presses the design into the paper, " +
        "creating a beautiful debossed effect — it works best with simpler designs and heavier stocks.",
      "Naše vizitky jsou vyrobeny z prémiových papírů o gramáži 250 až 350 g/m². " +
        "Vybírejte z křídového, nenatíraného, bavlněného, kraftového nebo syntetického papíru Yupo " +
        "pro jedinečný haptický zážitek. Povýšte svůj design luxusními úpravami jako slepotisk, " +
        "ražba fólií, soft-touch lak nebo laminace. K dispozici v digitálním a knihtisku." +
        "\n\n" +
        "Volba papíru je důležitější, než si možná myslíte. Křídové papíry (lesklé nebo hedvábné) " +
        "produkují nejostřejší barvy a jsou nejoblíbenější volbou pro firemní vizitky. Nenatírané papíry " +
        "nabízejí přirozený, popisovatelný povrch — ideální, pokud si příjemci potřebují něco poznamenat. " +
        "Bavlněný papír je měkký a prémiový, často volený luxusními značkami. Kraftový papír dodává " +
        "ekologický, řemeslný vzhled. Yupo je syntetický, neroztrhnitelný a voděodolný materiál, " +
        "který se zcela odlišuje od tradičního papíru." +
        "\n\n" +
        "Při výběru povrchové úpravy přemýšlejte o dojmu, který chcete vyvolat. Matná laminace " +
        "poskytuje hladký, elegantní pocit a chrání před otisky prstů. Lesklá laminace nechá barvy " +
        "vyniknout a dodá vizitkám výrazný, poutavý povrch. Soft-touch (sametový) lak vytváří luxusní " +
        "texturu připomínající semiš. Ražba fólií (zlatá, stříbrná nebo holografická) přidává kovový " +
        "lesk logům nebo textu, zatímco slepotisk vytváří vystouplý, hmatatelný vzor." +
        "\n\n" +
        "Pro tisk je digitální tisk rychlý, cenově dostupný a ideální pro malé náklady s plnobarevnými " +
        "návrhy. Knihtisk je tradiční technika, která fyzicky vtlačí design do papíru a vytvoří krásný " +
        "prohloubený efekt — nejlépe funguje s jednoduššími návrhy a silnějšími papíry.",
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
        "Choose landscape or portrait orientation with a range of protective coatings." +
        "\n\n" +
        "Paper weight is the single most important choice for flyers. Lightweight 90–115 gsm is ideal " +
        "for mass distribution — think event handouts, newspaper inserts, or door-to-door leaflets where " +
        "cost per piece matters. Mid-range 130–170 gsm is the sweet spot for most marketing flyers — " +
        "sturdy enough to feel professional, yet affordable in quantity. Heavy 200–350 gsm creates card-like " +
        "flyers that double as mini-posters or premium product sheets. The heavier the stock, the more " +
        "durable and high-quality the piece feels in hand." +
        "\n\n" +
        "Coated vs. uncoated paper makes a big difference in appearance. Coated (glossy or silk) paper " +
        "enhances color vibrancy and image sharpness — the best choice for photo-heavy designs like " +
        "real estate listings or product showcases. Uncoated paper offers a softer, more natural look " +
        "that is easier to write on — great for forms, coupons, or reply cards." +
        "\n\n" +
        "Protective coatings extend the life of your flyers. UV coating adds a thin, high-gloss layer " +
        "that resists scuffing and moisture. Lamination (matte or gloss) provides the highest level of " +
        "protection and is recommended for pieces that will be handled frequently, such as menus or " +
        "pocket-sized reference guides. Aqueous (water-based) coating is a cost-effective middle ground " +
        "that provides moderate protection without adding much to the price.",
      "Jednostránkové propagační letáky s gramáží od lehkých 90g po pevné 350g. " +
        "Ideální pro eventové materiály, jídelní lístky, produktové listy a marketingové podklady. " +
        "Na výšku nebo na šířku s řadou ochranných povrchových úprav." +
        "\n\n" +
        "Gramáž papíru je nejdůležitější volba u letáků. Lehký papír 90–115 g/m² je ideální pro " +
        "hromadnou distribuci — eventové materiály, novinové přílohy nebo door-to-door letáčky, kde " +
        "záleží na ceně za kus. Střední gramáž 130–170 g/m² je optimální kompromis pro většinu " +
        "marketingových letáků — dostatečně pevné pro profesionální dojem, přitom cenově dostupné. " +
        "Těžký papír 200–350 g/m² vytváří kartonové letáky, které mohou sloužit jako minipostery " +
        "nebo prémiové produktové listy." +
        "\n\n" +
        "Křídový vs. nenatíraný papír zásadně ovlivní vzhled. Křídový (lesklý nebo hedvábný) papír " +
        "zvýrazní sytost barev a ostrost obrázků — nejlepší volba pro fotografie a produktové prezentace. " +
        "Nenatíraný papír nabízí měkčí, přirozenější vzhled a lze na něj snadno psát — skvělý " +
        "pro formuláře, kupóny nebo odpovědní karty." +
        "\n\n" +
        "Ochranné úpravy prodlouží životnost letáků. UV lak přidá tenkou lesklou vrstvu odolnou " +
        "proti otěru a vlhkosti. Laminace (matná nebo lesklá) poskytuje nejvyšší úroveň ochrany " +
        "a je doporučena pro materiály, které budou často v rukou, například jídelní lístky. " +
        "Disperzní lak je cenově výhodný kompromis s dostatečnou ochranou bez výrazného navýšení ceny.",
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
        "Perfect for product catalogs, restaurant menus, travel guides, and corporate presentations." +
        "\n\n" +
        "Each fold type serves a different purpose. A bi-fold (single fold) creates 4 panels and is the " +
        "simplest option — ideal for price lists, event programs, or product overviews where you need a " +
        "clear front/back structure. A tri-fold (letter fold) creates 6 panels and is the most popular " +
        "choice for marketing brochures — it fits in a standard #10 envelope and unfolds sequentially, " +
        "guiding the reader through your content. A Z-fold (accordion fold) also creates 6 panels but " +
        "unfolds like a zigzag, making it great for maps, timelines, or step-by-step instructions. " +
        "A gate fold has two flaps that open from the center like doors — it creates a dramatic reveal " +
        "effect, perfect for luxury product launches or event invitations." +
        "\n\n" +
        "Paper choice affects both the feel and foldability. Lighter stocks (130–150 gsm) fold easily and " +
        "are economical for mass distribution. Medium stocks (170–200 gsm) offer a more professional feel " +
        "while still folding cleanly. For heavier stocks (250+ gsm), scoring (a pre-creased line) is " +
        "essential to prevent cracking along the fold — we include this automatically. Coated papers " +
        "give sharp, vivid images, while uncoated papers are easier to write on and feel more organic." +
        "\n\n" +
        "For finishing, consider lamination on the outer panels to protect against wear if the brochure " +
        "will be displayed in a rack or handled frequently. UV spot coating can highlight specific design " +
        "elements, such as logos or headings, adding a subtle gloss contrast on a matte background.",
      "Skládané brožury a letáky s více typy skladu: na půl, na třetiny, Z-sklad a dvoudveřový sklad. " +
        "Bigování je automaticky zahrnuto pro čisté přehyby na silnějších papírech. " +
        "Ideální pro produktové katalogy, jídelní lístky, průvodce a firemní prezentace." +
        "\n\n" +
        "Každý typ skladu slouží jinému účelu. Sklad na půl (bi-fold) vytváří 4 panely a je " +
        "nejjednodušší možností — ideální pro ceníky, programy akcí nebo přehledy produktů. " +
        "Sklad na třetiny (tri-fold) vytváří 6 panelů a je nejoblíbenější volbou pro marketingové " +
        "brožury — vejde se do standardní obálky a postupně se rozkládá, čímž provádí čtenáře obsahem. " +
        "Z-sklad (harmonika) také vytváří 6 panelů, ale rozkládá se jako cikcak — skvělý pro mapy, " +
        "časové osy nebo postupy krok za krokem. Dvoudveřový sklad má dva panely, které se otvírají " +
        "ze středu jako dveře — vytváří dramatický efekt odhalení, ideální pro luxusní produkty " +
        "nebo pozvánky na akce." +
        "\n\n" +
        "Volba papíru ovlivní jak pocit, tak skládatelnost. Lehčí papíry (130–150 g/m²) se snadno " +
        "skládají a jsou ekonomické pro hromadnou distribuci. Střední gramáže (170–200 g/m²) nabízejí " +
        "profesionálnější dojem a stále se čistě skládají. U silnějších papírů (250+ g/m²) je bigování " +
        "(předem nalinkovaný přehyb) nezbytné pro zabránění praskání — zahrnujeme ho automaticky." +
        "\n\n" +
        "Jako povrchovou úpravu zvažte laminaci vnějších panelů pro ochranu proti opotřebení, pokud " +
        "bude brožura vystavena ve stojanu nebo často v rukou. UV bodový lak může zvýraznit konkrétní " +
        "prvky designu, jako loga nebo nadpisy, a přidat jemný lesklý kontrast na matném pozadí.",
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
        "Available in coated, silk, and cotton papers with a variety of finishing options." +
        "\n\n" +
        "Postcards are one of the most versatile print products. For direct mail campaigns, the key is " +
        "choosing a stock heavy enough to survive the postal system — 300 gsm coated is the industry " +
        "standard. If your design is photo-heavy (real estate, travel, food), go with glossy coated paper " +
        "for maximum color impact. For a more refined, upscale feel — think wedding invitations or boutique " +
        "brand communications — silk or cotton paper creates an immediate sense of quality." +
        "\n\n" +
        "Size matters for mailing. Standard postcard sizes (A6 at 105×148 mm or DL at 99×210 mm) qualify " +
        "for lower postal rates in most countries. Oversized postcards grab more attention in the mailbox " +
        "but may cost more to send. We offer both standard and custom dimensions to match your campaign needs." +
        "\n\n" +
        "Finishing options can make your postcard stand out. Soft-touch coating gives a velvety feel that " +
        "recipients are reluctant to throw away. Spot UV adds a localized high-gloss accent — for instance, " +
        "on a logo or product image — that catches the light. For a truly premium impression, consider " +
        "combining a matte laminated base with gold or silver foil stamping on your brand name or key " +
        "design elements. Round corners are a subtle but effective touch that gives postcards a more " +
        "modern, friendly appearance.",
      "Prémiové pohlednice tištěné na silných papírech od 250 do 350g. " +
        "Ideální pro direct mail kampaně, pozvánky, děkovné karty a propagační zásilky. " +
        "K dispozici v křídovém, hedvábném a bavlněném papíru s řadou povrchových úprav." +
        "\n\n" +
        "Pohlednice patří mezi nejuniverzálnější tiskové produkty. Pro direct mail kampaně je klíčové " +
        "zvolit dostatečně silný papír, který přežije poštovní systém — 300 g/m² křídový je průmyslový " +
        "standard. Pokud je váš design plný fotografií (reality, cestování, jídlo), zvolte lesklý " +
        "křídový papír pro maximální barevný dojem. Pro rafinovanější, luxusnější pocit — třeba " +
        "svatební oznámení nebo komunikaci prémiových značek — hedvábný nebo bavlněný papír " +
        "vytvoří okamžitý dojem kvality." +
        "\n\n" +
        "Velikost je důležitá pro poštovní zásilky. Standardní formáty (A6 105×148 mm nebo DL " +
        "99×210 mm) splňují podmínky pro nižší poštovné ve většině zemí. Nadrozměrné pohlednice " +
        "upoutají více pozornosti, ale mohou být dražší na odeslání." +
        "\n\n" +
        "Povrchové úpravy mohou vaši pohlednici odlišit. Soft-touch lak dodává sametový pocit, " +
        "který příjemce odrazuje od vyhození. Bodový UV lak přidává lokalizovaný lesklý akcent — " +
        "například na logo — který zachycuje světlo. Pro skutečně prémiový dojem zvažte kombinaci " +
        "matné laminace se zlatou nebo stříbrnou ražbou fólií. Zaoblené rohy jsou jemný, ale " +
        "účinný detail, který dodává pohlednicím modernější, přátelštější vzhled.",
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
        "annual reports, and product lookbooks." +
        "\n\n" +
        "Understanding binding types is key to choosing the right booklet. Saddle stitch is the most " +
        "common and affordable method — sheets are folded, nested together, and stapled through the spine " +
        "with two or three wire staples. It works best for booklets with 8 to 64 pages (page count must " +
        "be a multiple of 4). Saddle-stitched booklets lay fairly flat when open and are perfect for " +
        "newsletters, event programs, product catalogs, and instruction manuals." +
        "\n\n" +
        "Perfect binding uses a glued spine, much like a paperback book. The interior pages are gathered, " +
        "their spine edge is roughened, and hot adhesive is applied to bond them to a wraparound cover. " +
        "This method is best for booklets with 48 or more pages — below that, the spine is too thin to " +
        "hold properly. Perfect binding gives a clean, professional look with a printable flat spine, " +
        "making it ideal for annual reports, product lookbooks, course materials, and thick catalogs. " +
        "The trade-off is that perfect-bound booklets do not lay completely flat when open." +
        "\n\n" +
        "Wire-o (twin-loop wire) binding uses a metal wire comb inserted through punched holes along the " +
        "spine. It allows the booklet to lay completely flat or fold back 360°, which is excellent for " +
        "reference manuals, cookbooks, planners, and presentation materials. Wire-o works with any page " +
        "count and supports mixing different paper stocks within the same booklet." +
        "\n\n" +
        "Using different papers for cover and interior is common practice. A heavier, laminated cover " +
        "(250–350 gsm) protects the booklet and creates a strong first impression, while lighter interior " +
        "pages (100–170 gsm) keep the booklet easy to flip through and control costs. For photo-heavy " +
        "interiors, use coated paper for sharp images; for text-heavy content, uncoated paper reduces " +
        "glare and is easier to read.",
      "Profesionální brožury se samostatným obalem a vnitřním blokem. " +
        "Vyberte různé papíry pro obálku a vnitřní stránky s vazbami " +
        "včetně V-vazby, lepené vazby a drátěné spirály. Ideální pro katalogy, programy, " +
        "výroční zprávy a produktové lookbooky." +
        "\n\n" +
        "Porozumění typům vazby je klíčové pro správný výběr. V-vazba (sešitová) je nejběžnější " +
        "a nejdostupnější metoda — archy se přeloží, vloží do sebe a sešijí drátěnými sponkami " +
        "přes hřbet. Nejlépe funguje pro brožury s 8 až 64 stranami (počet stran musí být " +
        "násobkem 4). Sešitové brožury leží poměrně naplocho a jsou ideální pro newslettery, " +
        "programy akcí, produktové katalogy a návody." +
        "\n\n" +
        "Lepená vazba používá lepený hřbet, podobně jako kniha v měkké vazbě. Vnitřní stránky " +
        "se seřadí, hřbetní hrana se zdrsní a horké lepidlo je přilepí k obalové obálce. " +
        "Tato metoda je nejlepší pro brožury s 48 a více stranami — pod tímto počtem je hřbet " +
        "příliš tenký. Lepená vazba dodává čistý, profesionální vzhled s potisknutelným plochým " +
        "hřbetem, ideální pro výroční zprávy, lookbooky a objemné katalogy. Nevýhodou je, " +
        "že brožury s lepenou vazbou neleží zcela naplocho." +
        "\n\n" +
        "Drátěná spirála (wire-o) používá kovový hřeben vložený přes děrované otvory podél " +
        "hřbetu. Umožňuje brožuře ležet zcela naplocho nebo se otočit o 360°, což je vynikající " +
        "pro referenční příručky, kuchařky, plánovače a prezentační materiály." +
        "\n\n" +
        "Použití různých papírů pro obálku a vnitřek je běžná praxe. Silnější, laminovaná obálka " +
        "(250–350 g/m²) chrání brožuru a vytváří silný první dojem, zatímco lehčí vnitřní stránky " +
        "(100–170 g/m²) usnadňují listování a kontrolují náklady. Pro fotografie použijte křídový " +
        "papír pro ostré obrázky; pro text je nenatíraný papír pohodlnější na čtení.",
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
        "and lighter paper for the monthly pages. Wire-o binding standard." +
        "\n\n" +
        "Wall calendars are typically A3 or A4 format with wire-o binding at the top edge. " +
        "Each spread features a large photo or illustration on the upper half and the calendar grid " +
        "below. A standard 14-page wall calendar includes a cover, 12 monthly pages, and a back cover. " +
        "For the cover, choose a heavier stock (250–300 gsm) with lamination for durability. Monthly " +
        "pages work well at 170–200 gsm coated paper for vivid photo reproduction." +
        "\n\n" +
        "Desk calendars (also called tent calendars) are a compact A5 or A6 format that sits on a " +
        "desk or table using wire-o binding with a built-in stand. They are popular corporate gifts — " +
        "your brand stays visible in the recipient's workspace all year long. A thicker back cover " +
        "(300+ gsm) acts as the base, keeping the calendar stable." +
        "\n\n" +
        "Wire-o binding is the standard for calendars because it allows pages to flip cleanly over the " +
        "top (wall) or fold back (desk) without springing forward. It is available in white, black, or " +
        "silver wire to complement your design. For photo-heavy calendars, coated (glossy or silk) " +
        "paper is recommended, as it brings out color depth and detail. For a more artistic, tactile " +
        "feel, uncoated or textured paper can give your calendar a unique character.",
      "Nástěnné a stolní kalendáře s prémiovým obalem a vnitřními stránkami. " +
        "Vícekomponentová konstrukce umožňuje zvolit silnější papír na obálku " +
        "a lehčí papír na měsíční stránky. Standardní drátěná vazba." +
        "\n\n" +
        "Nástěnné kalendáře jsou typicky ve formátu A3 nebo A4 s wire-o vazbou na horním okraji. " +
        "Každá dvoustrana obsahuje velkou fotografii v horní části a kalendářní mřížku pod ní. " +
        "Standardní 14stránkový nástěnný kalendář zahrnuje obálku, 12 měsíčních stran a zadní obálku. " +
        "Pro obálku zvolte silnější papír (250–300 g/m²) s laminací pro odolnost. Měsíční stránky " +
        "fungují dobře na 170–200 g/m² křídovém papíru pro živou reprodukci fotografií." +
        "\n\n" +
        "Stolní kalendáře (také nazývané stanové) jsou v kompaktním formátu A5 nebo A6 a stojí na " +
        "stole pomocí wire-o vazby s vestavěným stojánkem. Jsou oblíbeným firemním dárkem — vaše " +
        "značka zůstává viditelná na pracovním stole příjemce celý rok. Silnější zadní obálka " +
        "(300+ g/m²) slouží jako základna a udržuje kalendář stabilní." +
        "\n\n" +
        "Wire-o vazba je standard pro kalendáře, protože umožňuje stránkám čistě přelistovat přes " +
        "horní okraj (nástěnné) nebo se přeložit zpět (stolní). K dispozici v bílém, černém nebo " +
        "stříbrném drátu. Pro fotografické kalendáře je doporučen křídový papír, protože zdůrazňuje " +
        "hloubku barev. Pro umělečtější, hmatatelný pocit může nenatíraný nebo texturovaný papír " +
        "dodat kalendáři jedinečný charakter.",
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
        "and construction site signage. Available with grommets for easy hanging." +
        "\n\n" +
        "Vinyl (PVC) is the most common banner material. It is flexible, waterproof, and durable — " +
        "a well-made vinyl banner can last 2–5 years outdoors. Standard vinyl comes in 440–510 g/m² " +
        "weights. Lighter weights are more portable; heavier weights are more rigid and resist wind better. " +
        "For indoor use, a thinner, smoother vinyl is sufficient and produces sharper images at close viewing " +
        "distances." +
        "\n\n" +
        "Mesh vinyl is a great option for large outdoor banners in windy areas. It has tiny perforations " +
        "that let wind pass through, dramatically reducing the force on the banner and its mounting points. " +
        "The perforations are virtually invisible from a normal viewing distance." +
        "\n\n" +
        "Finishing options determine how you hang or install the banner. Grommets (metal eyelets) are " +
        "punched along the edges, typically every 50 cm, and allow the banner to be attached with ropes, " +
        "zip ties, or hooks. Hemmed edges (where material is folded over and welded) reinforce the banner " +
        "and prevent tearing. Pole pockets are sleeves sewn along the top and/or bottom edges that slide " +
        "onto a pole or rod — common for street-pole banners." +
        "\n\n" +
        "UV-curable inks are the industry standard for banners. They are cured instantly by UV light " +
        "during printing, resulting in vibrant, scratch-resistant colors that resist fading from sunlight. " +
        "For banners that will be displayed outdoors for extended periods, UV inks combined with a protective " +
        "overlamination can extend the lifespan significantly.",
      "Odolné vinylové bannery tištěné UV inkoustem pro živé, " +
        "povětrnostně odolné grafiky. Ideální pro venkovní akce, veletrhy, výlohy " +
        "a stavební reklamu. K dispozici s průchodkami pro snadné zavěšení." +
        "\n\n" +
        "Vinyl (PVC) je nejběžnější materiál na bannery. Je flexibilní, voděodolný a trvanlivý — " +
        "kvalitní vinylový banner vydrží venku 2–5 let. Standardní vinyl má gramáž 440–510 g/m². " +
        "Lehčí gramáže jsou přenosnější; těžší gramáže jsou tužší a lépe odolávají větru. " +
        "Pro interiérové použití stačí tenčí, hladší vinyl, který navíc produkuje ostřejší obraz " +
        "při pohledu zblízka." +
        "\n\n" +
        "Síťový (mesh) vinyl je skvělá volba pro velké venkovní bannery ve větrných oblastech. " +
        "Má drobné perforace, které propouštějí vítr, což dramaticky snižuje sílu působící na " +
        "banner. Perforace jsou z normální pozorovací vzdálenosti prakticky neviditelné." +
        "\n\n" +
        "Povrchové úpravy určují způsob zavěšení banneru. Průchodky (kovová očka) se vyrazí " +
        "podél okrajů, typicky každých 50 cm, a umožňují uchycení provazy, stahovacími pásky " +
        "nebo háčky. Lemované okraje (přeložený a svařený materiál) zpevňují banner a zabraňují " +
        "trhání. Tunýlky jsou rukávy podél horního nebo spodního okraje, do kterých se navlékne tyč." +
        "\n\n" +
        "UV vytvrzovací inkousty jsou průmyslovým standardem. Vytvrzují se okamžitě UV světlem " +
        "během tisku, což vede k živým, odolným barvám, které neblednou sluncem. Pro bannery " +
        "vystavené dlouhodobě venku může ochranná přelaminace výrazně prodloužit životnost.",
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
        "Choose economy or premium stands paired with high-resolution printed banner film." +
        "\n\n" +
        "A roll-up (or retractable) banner consists of two parts: the printed graphic and the stand. " +
        "The graphic retracts into the base like a window blind. To set up, you simply pull the banner " +
        "upward, attach the support pole, and it is ready — typically under 30 seconds. This makes " +
        "roll-ups the go-to choice for anyone who needs portable, professional signage that can be " +
        "set up and taken down repeatedly." +
        "\n\n" +
        "Economy stands are lightweight aluminum, ideal for occasional use at events or in-store " +
        "promotions. They are affordable and perfectly functional, but the retraction mechanism " +
        "may wear after heavy repeated use. Premium stands feature a heavier, wider base for greater " +
        "stability, a smoother retraction mechanism rated for thousands of uses, and often include " +
        "a padded carrying case. For businesses that attend trade shows regularly, a premium stand " +
        "pays for itself through durability." +
        "\n\n" +
        "The banner graphic itself is printed on a semi-rigid film (typically anti-curl polyester or " +
        "polypropylene) using high-resolution inkjet printing. Overlamination — a thin protective film " +
        "applied over the print — is strongly recommended for roll-ups, as it prevents scratches from " +
        "the repeated rolling and unrolling action. It also makes the graphic wipeable and extends its " +
        "vibrant appearance." +
        "\n\n" +
        "Standard roll-up width is 850 mm (about 33 inches) with a visible height of approximately " +
        "2000 mm. Wider options (1000 mm, 1200 mm) are available for greater visual impact. When " +
        "designing, keep in mind that the bottom 100–150 mm may be hidden by the base cassette, " +
        "so place important content above that zone.",
      "Rolovací bannery kombinují potištěný banner s hliníkovým samonavíjecím stojanem. " +
        "Instalace za pár sekund — ideální pro veletrhy, konference, obchody a firemní recepce. " +
        "Vyberte ekonomický nebo prémiový stojan s vysoce kvalitním potištěným bannerovým filmem." +
        "\n\n" +
        "Roll-up (samonavíjecí) banner se skládá ze dvou částí: potištěné grafiky a stojanu. " +
        "Grafika se zasouvá do základny jako roleta. Pro instalaci jednoduše vytáhněte banner " +
        "nahoru, připojte podpěrnou tyč a je hotovo — typicky pod 30 sekund. Díky tomu jsou " +
        "roll-upy první volbou pro každého, kdo potřebuje přenosné, profesionální značení." +
        "\n\n" +
        "Ekonomické stojany jsou z lehkého hliníku, ideální pro příležitostné použití na akcích " +
        "nebo v prodejnách. Jsou cenově dostupné a plně funkční, ale mechanismus navíjení se může " +
        "opotřebit po intenzivním opakovaném používání. Prémiové stojany mají těžší, širší " +
        "základnu pro větší stabilitu a hladší mechanismus dimenzovaný na tisíce použití. " +
        "Pro firmy, které pravidelně vystavují na veletrzích, se prémiový stojan vyplatí." +
        "\n\n" +
        "Grafika banneru je tištěna na polotuhou fólii (typicky antikurlový polyester nebo " +
        "polypropylen) vysokorozlišovacím inkoustovým tiskem. Přelaminace — tenká ochranná fólie " +
        "přes potisk — je u roll-upů důrazně doporučena, protože chrání před poškrábáním " +
        "při opakovaném rolování a odrolování." +
        "\n\n" +
        "Standardní šířka roll-upu je 850 mm s viditelnou výškou přibližně 2000 mm. Širší " +
        "varianty (1000 mm, 1200 mm) jsou k dispozici pro větší vizuální dopad. Při návrhu " +
        "pamatujte, že spodních 100–150 mm může být skryto pod kazetou základny.",
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
        "and die-cutting for unique unboxing experiences. Perfect for retail, e-commerce, and gifts." +
        "\n\n" +
        "Material choice depends on what you are packaging. Kraft paper (solid board, 300–600 gsm) " +
        "is the most popular option for lightweight to mid-weight products — cosmetics, candles, " +
        "small electronics, and confectionery. It offers a natural, eco-friendly look that can be " +
        "printed on or left raw with a simple sticker or stamp. White-coated kraft provides a brighter " +
        "canvas for full-color designs." +
        "\n\n" +
        "Corrugated cardboard features a wavy fluted layer sandwiched between flat liners, giving it " +
        "excellent cushioning and structural strength. It is the standard choice for shipping boxes, " +
        "subscription boxes, and any product that needs protection during transit. Single-wall " +
        "corrugated is sufficient for most products; double-wall provides extra strength for heavy items." +
        "\n\n" +
        "Die-cutting allows the creation of custom box shapes, window cutouts, and complex fold patterns. " +
        "A die (a custom-shaped metal blade) is pressed into the material to cut it precisely. This is " +
        "how you get tuck-end boxes, pillow boxes, drawer-style boxes, and boxes with built-in inserts " +
        "or compartments. Scoring (a partial-depth cut) creates clean fold lines." +
        "\n\n" +
        "Premium finishing elevates the unboxing experience. Embossing raises a design above the surface " +
        "for a tactile effect; debossing presses it in. Foil stamping applies a thin layer of metallic " +
        "foil (gold, silver, copper, or holographic) to highlight logos, brand names, or decorative " +
        "patterns. Spot UV adds a glossy accent to specific areas, creating contrast on a matte surface. " +
        "These finishing techniques transform basic packaging into a memorable brand touchpoint.",
      "Vlastní obalové krabice a kontejnery z kraftového papíru, vlnité lepenky " +
        "nebo syntetického papíru Yupo. Přidejte prémiové prvky jako slepotisk, " +
        "ražbu fólií a výsek pro jedinečný unboxing zážitek. Ideální pro retail, e-commerce a dárky." +
        "\n\n" +
        "Volba materiálu závisí na tom, co balíte. Kraftový papír (celistvá lepenka, 300–600 g/m²) " +
        "je nejoblíbenější volbou pro lehké až středně těžké produkty — kosmetiku, svíčky, drobnou " +
        "elektroniku a cukrovinky. Nabízí přirozený, ekologický vzhled, na který lze tisknout, " +
        "nebo ponechat v surové podobě s jednoduchou nálepkou. Bíle natíraný kraft poskytuje " +
        "jasnější podklad pro plnobarevné designy." +
        "\n\n" +
        "Vlnitá lepenka má vlnitou vrstvu mezi dvěma plochými krycími vrstvami, což jí dává " +
        "vynikající tlumicí schopnost a strukturální pevnost. Je standardní volbou pro přepravní " +
        "krabice, předplatné boxy a cokoli, co potřebuje ochranu při přepravě." +
        "\n\n" +
        "Výsek (die-cut) umožňuje vytváření vlastních tvarů krabic, okenních výřezů a složitých " +
        "skládacích vzorů. Výsekový nástroj (kovová čepel na míru) se vtlačí do materiálu a přesně " +
        "ho prořízne. Takto vznikají krabice se zasouvacím víkem, polštářové krabice, zásuvkové " +
        "krabice a krabice s vestavěnými vložkami." +
        "\n\n" +
        "Prémiové úpravy povyšují zážitek z rozbalování. Slepotisk vystouplý zvedne design nad " +
        "povrch pro hmatový efekt; prohloubený slepotisk ho vtlačí dovnitř. Ražba fólií nanese " +
        "tenkou vrstvu kovové fólie (zlaté, stříbrné, měděné nebo holografické) pro zvýraznění " +
        "loga nebo dekorativních vzorů. Tyto techniky promění základní obal v nezapomenutelný " +
        "kontaktní bod se značkou.",
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
        "Perfect for product labels, packaging seals, laptop stickers, and brand merchandise." +
        "\n\n" +
        "Understanding cutting types helps you pick the right option. Kiss-cut means the blade cuts " +
        "through the sticker material but not the backing sheet — your stickers remain on a larger " +
        "peel-off sheet for easy handling, which is ideal for sticker packs, retail sheets, or giveaway " +
        "sets. Die-cut goes all the way through both the sticker and the backing, producing individual " +
        "stickers in your exact custom shape — this is the classic option for laptop stickers, " +
        "water bottles, and individual handouts. Contour cut follows the exact outline of your design " +
        "with only a small border, giving each sticker a clean, professional look." +
        "\n\n" +
        "Material choice depends on where the sticker will live. Standard adhesive paper is lightweight " +
        "and affordable — perfect for indoor product labels, packaging seals, and short-term " +
        "promotions. White or clear vinyl is waterproof and UV-resistant, making it ideal for outdoor " +
        "use, water bottles, car bumpers, and any surface exposed to moisture or sunlight. Yupo " +
        "(synthetic paper) is tear-proof and water-resistant with a smooth, paper-like feel — great " +
        "for premium labels where durability and tactile quality both matter." +
        "\n\n" +
        "Adhesive types vary as well. Permanent adhesive bonds strongly to most surfaces and is the " +
        "default choice for product labels and branding stickers. Removable adhesive peels off cleanly " +
        "without residue — useful for temporary labels, wall decals, or packaging seals that " +
        "consumers need to open. For curved surfaces like bottles, choose an adhesive formulated for " +
        "flexible bonding." +
        "\n\n" +
        "For printing, digital printing gives you vibrant full-color designs with no minimum order " +
        "quantity. White ink printing is available on clear vinyl to make colors pop on transparent " +
        "backgrounds. Matte or gloss overlamination adds scratch resistance and a professional finish.",
      "Samolepicí nálepky a etikety na prémiovém samolepicím materiálu, průhledném vinylu " +
        "nebo povětrnostně odolném papíru Yupo. K dispozici s kiss-cut, die-cut nebo zaoblenými rohy. " +
        "Ideální pro produktové etikety, pečeti na balení, nálepky na notebook a značkový merchandising." +
        "\n\n" +
        "Pochopení typů řezu vám pomůže vybrat správnou variantu. Kiss-cut znamená, že čepel prořízne " +
        "materiál nálepky, ale ne podkladový arch — vaše nálepky zůstanou na větším odlepovacím archu " +
        "pro snadnou manipulaci, což je ideální pro sady nálepek nebo dárkové sety. Die-cut prořízne " +
        "celou nálepku i podklad a vytvoří jednotlivé nálepky ve vašem přesném tvaru — klasická volba " +
        "pro nálepky na notebook nebo lahve. Obrysový řez sleduje přesný obrys vašeho designu " +
        "s malým okrajem, což dodává každé nálepce čistý, profesionální vzhled." +
        "\n\n" +
        "Volba materiálu závisí na tom, kde bude nálepka žít. Standardní samolepicí papír je lehký " +
        "a cenově dostupný — ideální pro interiérové etikety a krátkodobé propagace. Bílý nebo " +
        "průhledný vinyl je voděodolný a UV-odolný, ideální pro venkovní použití, lahve s vodou " +
        "a jakýkoli povrch vystavený vlhkosti nebo slunci. Yupo (syntetický papír) je neroztrhnitelný " +
        "a voděodolný s hladkým, papírovým pocitem." +
        "\n\n" +
        "Typy lepidla se také liší. Permanentní lepidlo se silně váže na většinu povrchů a je " +
        "standardní volbou. Snímatelné lepidlo se odlepuje čistě bez zbytků — užitečné pro dočasné " +
        "etikety nebo pečeti na balení. Pro zakřivené povrchy jako láhve zvolte lepidlo " +
        "formulované pro flexibilní přilnavost." +
        "\n\n" +
        "Pro tisk nabízí digitální tisk živé plnobarevné designy bez minimálního množství. " +
        "Tisk bílým inkoustem je k dispozici na průhledném vinylu. Matná nebo lesklá přelaminace " +
        "přidává odolnost proti poškrábání a profesionální povrch.",
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
        "custom label printing, and individual fold & bag packaging." +
        "\n\n" +
        "Choosing the right print method is the most important decision. Screen printing is the " +
        "traditional workhorse — ink is pushed through a stenciled mesh screen onto the fabric. " +
        "It produces the most vibrant, opaque colors and is extremely durable (designs survive " +
        "hundreds of washes). However, each color in your design requires a separate screen, so " +
        "screen printing is most cost-effective for simple designs (1–4 colors) in larger quantities " +
        "(50+ pieces). For small runs or complex designs, setup costs can be prohibitive." +
        "\n\n" +
        "DTG (Direct to Garment) printing works like an inkjet printer for fabric. It sprays water-based " +
        "inks directly onto the shirt, allowing unlimited colors, gradients, and photographic detail " +
        "in a single pass. DTG shines for small orders (even 1 piece), complex artwork, and designs " +
        "with many colors. The print is softer to the touch than screen printing. It works best on " +
        "100% cotton or high-cotton blend fabrics — the ink bonds to natural fibers more effectively." +
        "\n\n" +
        "Sublimation printing uses heat to transfer dye into polyester fabric at a molecular level. " +
        "The result is an all-over print with no texture — the design becomes part of the fabric " +
        "itself. Sublimation allows edge-to-edge designs that are impossible with other methods. " +
        "The catch: it only works on white or very light polyester garments. On dark or cotton fabrics, " +
        "sublimation will not work." +
        "\n\n" +
        "Fabric choice matters for both comfort and print quality. 100% cotton (160–180 gsm) is " +
        "the most comfortable and breathable, ideal for everyday wear and DTG printing. Polyester " +
        "is moisture-wicking and wrinkle-resistant, required for sublimation. Cotton–polyester blends " +
        "(e.g. 65/35) combine comfort with durability. Organic cotton offers the same quality with " +
        "an eco-conscious appeal. Heavier gsm means a thicker, more premium feel; lighter gsm is " +
        "cooler for warm weather or athletic use.",
      "Vysoce kvalitní trička s vlastním potiskem z bavlny, polyesteru a směsových materiálů o gramáži 140 až 180 g/m². " +
        "Vyberte si sítotisk pro výrazné plné barvy, DTG pro plnobarevný fototisk " +
        "nebo sublimaci pro celoplošné designy na polyesteru. K dispozici s přenosem tepelným lisem, " +
        "tiskem vlastních štítků a individuálním balením do sáčku." +
        "\n\n" +
        "Výběr správné tiskové metody je nejdůležitější rozhodnutí. Sítotisk je tradiční metoda — " +
        "inkoust se protlačuje přes šablonovou síťku na látku. Produkuje nejživější, neprůhledné " +
        "barvy a je extrémně odolný (designy přežijí stovky praní). Každá barva vyžaduje samostatnou " +
        "síťku, takže sítotisk je nejefektivnější pro jednoduché designy (1–4 barvy) ve větších " +
        "množstvích (50+ kusů). Pro malé náklady mohou být náklady na přípravu prohibitivní." +
        "\n\n" +
        "DTG (Direct to Garment) tisk funguje jako inkoustová tiskárna na textil. Stříká inkousty " +
        "na vodní bázi přímo na tričko, což umožňuje neomezené barvy, přechody a fotografické " +
        "detaily v jednom průchodu. DTG vyniká u malých objednávek (i 1 kus) a složitých návrhů. " +
        "Potisk je měkčí na dotek než sítotisk. Nejlépe funguje na 100% bavlně nebo směsích " +
        "s vysokým podílem bavlny." +
        "\n\n" +
        "Sublimační tisk používá teplo k přenosu barviva do polyesterové tkaniny na molekulární " +
        "úrovni. Výsledkem je celoplošný potisk bez textury — design se stane součástí samotné " +
        "tkaniny. Sublimace umožňuje designy od okraje k okraji, které jsou jinými metodami " +
        "nemožné. Háček: funguje pouze na bílém nebo velmi světlém polyesteru." +
        "\n\n" +
        "Volba materiálu ovlivňuje komfort i kvalitu tisku. 100% bavlna (160–180 g/m²) je " +
        "nejpohodlnější a nejprodyšnější, ideální pro denní nošení a DTG tisk. Polyester odvádí " +
        "vlhkost a nekrčí se, je vyžadován pro sublimaci. Směsi bavlna-polyester kombinují " +
        "komfort s odolností. Organická bavlna nabízí stejnou kvalitu s ekologickým rozměrem.",
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
        "Add embroidery for a premium touch or reinforced handles for extra durability." +
        "\n\n" +
        "Material choice defines the character of your bag. Cotton canvas (180–280 gsm) is the most " +
        "popular choice — sturdy, washable, and naturally soft. It works with all print methods and " +
        "folds compactly into a pocket or purse. Organic cotton is identical in quality but grown " +
        "without synthetic pesticides or fertilizers, making it the preferred option for eco-conscious " +
        "brands. Look for GOTS (Global Organic Textile Standard) certification for verified organic " +
        "sourcing." +
        "\n\n" +
        "Recycled PET bags are made from recycled plastic bottles — each bag typically uses 3–5 bottles. " +
        "They are lightweight, water-resistant, and have a slightly different texture from cotton. " +
        "Jute (burlap) is a natural fiber with a distinctive rustic appearance — ideal for artisan " +
        "brands, farmers' markets, and gift bags. Non-woven polypropylene (PP) is the most affordable " +
        "option, commonly used for trade show giveaways and grocery bags. It is tear-resistant and " +
        "recyclable, though it does not have the same premium feel as natural fibers." +
        "\n\n" +
        "Screen printing is the go-to for bold, simple logo designs (1–3 colors) on large runs. " +
        "The ink sits on the surface of the fabric and produces vibrant, opaque results. DTG is best " +
        "for full-color, photographic, or complex multi-color designs, especially on smaller orders. " +
        "Embroidery uses threaded stitching to create your logo — it is the most premium and durable " +
        "decoration method, but works best with simple designs at smaller scales (chest logos, bag corners)." +
        "\n\n" +
        "Construction details matter for longevity. Reinforced handles (double-stitched or cross-stitched " +
        "to the body) prevent tearing under heavy loads. Gusseted bottoms let the bag stand upright and " +
        "hold more items. Inner pockets or zippered closures add practical value that keeps the bag in " +
        "active use long after the event.",
      "Ekologické tašky z bavlněného plátna, bio bavlny, recyklovaného PET, juty " +
        "a netkané polypropylénové textilie. Ideální pro akce, retail a firemní dárky. " +
        "K dispozici se sítotiskem pro výrazná loga nebo DTG pro plnobarevné designy. " +
        "Přidejte výšivku pro prémiový dojem nebo zpevněná ucha pro extra odolnost." +
        "\n\n" +
        "Volba materiálu definuje charakter vaší tašky. Bavlněné plátno (180–280 g/m²) je " +
        "nejoblíbenější volba — pevné, pratelné a přirozeně měkké. Funguje se všemi tiskovými " +
        "metodami a kompaktně se složí do kapsy. Organická bavlna je kvalitativně identická, ale " +
        "pěstovaná bez syntetických pesticidů, což z ní dělá preferovanou volbu pro eko-značky. " +
        "Hledejte certifikaci GOTS pro ověřený organický původ." +
        "\n\n" +
        "Tašky z recyklovaného PET jsou vyrobeny z recyklovaných plastových lahví — každá taška " +
        "typicky využívá 3–5 lahví. Jsou lehké a voděodolné. Juta je přírodní vlákno " +
        "s výrazným rustikálním vzhledem — ideální pro řemeslné značky a farmářské trhy. " +
        "Netkaný polypropylen (PP) je nejdostupnější varianta, běžně používaná pro veletržní " +
        "dárky a nákupní tašky." +
        "\n\n" +
        "Sítotisk je ideální pro výrazné, jednoduché logotypy (1–3 barvy) na velké náklady. " +
        "DTG je nejlepší pro plnobarevné, fotografické nebo složité designy, zejména na menší " +
        "objednávky. Výšivka používá nitěné stehy k vytvoření loga — je to nejprémiovější " +
        "a nejodolnější dekorační metoda, ale nejlépe funguje u jednoduchých designů." +
        "\n\n" +
        "Konstrukční detaily jsou důležité pro životnost. Zpevněná ucha (dvojitě prošitá " +
        "nebo křížově přišitá) zabraňují trhání pod těžkým nákladem. Klínové dno umožňuje " +
        "tašce stát zpříma. Vnitřní kapsy nebo zipové zapínání přidávají praktickou hodnotu.",
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
        "Available in 32mm and 58mm standard sizes. Perfect for events, promotions, and brand merchandise." +
        "\n\n" +
        "Pin badges are one of the most affordable and effective promotional items. They are small enough " +
        "to slip into a conference bag or hand out at the door, yet they stay visible on bags, jackets, and " +
        "lanyards long after an event. For campaigns, they create a sense of belonging — people wear them " +
        "to show support for a cause, brand, or community." +
        "\n\n" +
        "Material choice affects both look and feel. Tinplate (metal) badges are the classic option — " +
        "lightweight, rigid, and affordable in bulk. They produce a satisfying click when pinned. " +
        "Acrylic badges are transparent or translucent, giving a modern, design-forward appearance that " +
        "works especially well with colorful or layered designs. Wooden badges have a natural, eco-friendly " +
        "aesthetic — they are laser-cut from thin plywood and printed with UV inkjet, ideal for brands " +
        "that emphasize sustainability." +
        "\n\n" +
        "Size matters for impact and wearability. The 32 mm (1.25 inch) size is subtle and discreet — " +
        "perfect for corporate lapel badges, event attendance markers, or wearable design accents. " +
        "The 58 mm (2.25 inch) size is much more visible and offers more design space — " +
        "great for campaign badges, statement pins, and promotional giveaways." +
        "\n\n" +
        "Back options determine how the badge is worn or used. A standard safety pin clasp is the most " +
        "common and works on any fabric. Magnet backs avoid piercing fabric — preferred for suits, " +
        "delicate garments, and name badges at conferences. Bottle opener backs add a practical function " +
        "that keeps your brand in use beyond wearing. A mylar (clear plastic) overlay is applied over " +
        "the printed face to protect the design from scratches and moisture.",
      "Vlastní odznaky z plechu, akrylátu a dřeva. Plnobarevný digitální nebo ofsetový tisk " +
        "s ochrannou mylarovou fólií. Vyberte si ze zavíracího špendlíku, magnetu nebo otvíráku na lahve. " +
        "K dispozici ve standardních velikostech 32mm a 58mm. Ideální pro akce, propagaci a merchandising." +
        "\n\n" +
        "Odznaky jsou jedním z nejdostupnějších a nejúčinnějších propagačních předmětů. Jsou " +
        "dostatečně malé, aby se vešly do konferenční tašky, ale zůstávají viditelné na taškách, " +
        "bundách a páskách ještě dlouho po akci. U kampaní vytvářejí pocit sounáležitosti — " +
        "lidé je nosí, aby vyjádřili podporu značce nebo komunitě." +
        "\n\n" +
        "Volba materiálu ovlivní vzhled i pocit. Plechové odznaky jsou klasická varianta — lehké, " +
        "tuhé a cenově výhodné ve velkém. Akrylátové odznaky jsou průhledné nebo průsvitné, " +
        "což dodává moderní vzhled, který skvěle funguje s barevnými nebo vrstvenými designy. " +
        "Dřevěné odznaky mají přirozený, ekologický charakter — jsou laserově řezány z tenké " +
        "překližky a potištěny UV inkjetem, ideální pro značky zdůrazňující udržitelnost." +
        "\n\n" +
        "Velikost ovlivňuje dopad a nositelnost. Velikost 32 mm je jemná a diskrétní — ideální " +
        "pro firemní klopové odznaky a eventové identifikátory. Velikost 58 mm je mnohem " +
        "viditelnější a nabízí více designového prostoru — skvělá pro kampaňové odznaky " +
        "a propagační dárky." +
        "\n\n" +
        "Typ uchycení určuje způsob nošení. Standardní zavírací špendlík je nejběžnější a funguje " +
        "na jakékoli tkanině. Magnetické uchycení se vyhne propíchnutí látky — preferováno " +
        "pro obleky a delikátní oděvy. Uchycení s otvírákem přidává praktickou funkci. " +
        "Mylarová (průhledná plastová) fólie chrání potisk před poškrábáním a vlhkostí.",
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
  )

  val cups: ShowcaseProduct = ShowcaseProduct(
    categoryId = SampleCatalog.cupsId,
    group = CatalogGroup.Promotional,
    tagline = LocalizedString(
      "Personalized mugs and cups for gifts and branding",
      "Personalizované hrnky a šálky pro dárky a branding",
    ),
    detailedDescription = LocalizedString(
      "Personalized mugs and cups in ceramic, stainless steel, enamel, glass, and magic (heat-reveal) " +
        "materials. White ceramic mugs with sublimation for vivid wrap-around prints, colored mugs with " +
        "UV direct print, and stainless steel travel mugs for on-the-go branding. Add dishwasher-safe " +
        "coating, glossy ceramic glaze, or individual gift box packaging for a premium presentation." +
        "\n\n" +
        "Ceramic mugs are the classic choice — they are affordable, universally recognized, and available " +
        "in a wide range of shapes (standard 330 ml, latte tall, espresso, and oversized 450 ml). " +
        "White ceramic is best for sublimation printing, which produces the most vibrant, photographic-quality " +
        "wrap-around prints. The dye infuses into the ceramic coating, resulting in a smooth surface with " +
        "no raised texture. Colored ceramic mugs (black, red, blue interior, etc.) use UV direct print — " +
        "the ink sits on the surface and is then cured, so the finish is slightly different but " +
        "still durable." +
        "\n\n" +
        "Magic (heat-reveal) mugs have a thermochromic coating that is black at room temperature but " +
        "turns transparent when hot liquid is poured in, revealing the printed design underneath. They " +
        "make fun gifts and attention-grabbing promotional items." +
        "\n\n" +
        "Stainless steel travel mugs are double-walled and vacuum-insulated, keeping drinks hot for " +
        "hours. They are printed using UV direct print technology, which adheres well to metal surfaces. " +
        "These are the most premium option and are ideal for corporate gifts or employee welcome kits. " +
        "Enamel mugs have a vintage, outdoor aesthetic — they are lightweight, durable, and printed " +
        "with high-temperature ceramic decals that survive campfires and dishwashers alike." +
        "\n\n" +
        "For care and durability, sublimation prints on ceramic are generally dishwasher-safe " +
        "but last longest with hand washing. UV prints are more sensitive to abrasion, so hand washing " +
        "is recommended. An optional dishwasher-safe clear coating can be applied to both types for " +
        "added protection. Gift box packaging — a white or kraft box with a foam insert — elevates " +
        "the presentation for corporate gifts, making the mug feel like a curated product.",
      "Personalizované hrnky a šálky z keramiky, nerezové oceli, smaltu, skla a magického " +
        "(termochromního) materiálu. Bílé keramické hrnky se sublimací pro živé celoplošné " +
        "potisky, barevné hrnky s UV přímým tiskem a nerezové cestovní hrnky pro branding na cestách. " +
        "Přidejte nátěr odolný myčce, lesklou keramickou glazuru nebo individuální dárkovou " +
        "krabičku pro prémiovou prezentaci." +
        "\n\n" +
        "Keramické hrnky jsou klasická volba — jsou cenově dostupné, univerzálně uznávané a k dispozici " +
        "v široké škále tvarů (standardní 330 ml, latte, espresso a nadrozměrné 450 ml). Bílá " +
        "keramika je nejlepší pro sublimační tisk, který produkuje nejživější, fotografické celoplošné " +
        "potisky. Barvivo se vsakuje do keramického nátěru, výsledkem je hladký povrch bez " +
        "vyvýšené textury. Barevné keramické hrnky (černé, červené, modré uvnitř atd.) používají " +
        "UV přímý tisk — inkoust sedí na povrchu a je následně vytvrzen." +
        "\n\n" +
        "Magické (termochromní) hrnky mají nátěr, který je při pokojové teplotě černý, ale při " +
        "nalití horkého nápoje se stane průhledným a odhalí potištěný design. Jsou zábavným " +
        "dárkem a poutavým propagačním předmětem." +
        "\n\n" +
        "Nerezové cestovní hrnky jsou dvouplášťové a vakuově izolované, udrží nápoje teplé " +
        "po hodiny. Jsou potištěny UV přímým tiskem, který dobře drží na kovovém povrchu. " +
        "Jsou to nejprémiovější varianta, ideální pro firemní dárky. Smaltované hrnky mají " +
        "vintage, outdoorový charakter — jsou lehké, odolné a potištěny vysokoteplotními " +
        "keramickými obtisky, které přežijí jak táborák, tak myčku." +
        "\n\n" +
        "Pro péči a odolnost jsou sublimační potisky na keramice obecně odolné myčce, ale " +
        "nejdéle vydrží při ručním mytí. UV potisky jsou citlivější na otěr, proto je doporučeno " +
        "ruční mytí. Volitelný průhledný nátěr odolný myčce lze aplikovat pro zvýšenou ochranu. " +
        "Dárkové balení — bílá nebo kraftová krabička s pěnovou vložkou — povyšuje prezentaci " +
        "pro firemní dárky.",
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
