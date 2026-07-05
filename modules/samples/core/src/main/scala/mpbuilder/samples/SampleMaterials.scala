package mpbuilder.samples

import mpbuilder.catalog.*
import mpbuilder.kernel.*
import SampleIds.*

object SampleMaterials:

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

  val pvc510g: Material = Material(
    id = pvc510gId,
    name = LocalizedString("PVC Banner 510g", "PVC banner 510g"),
    family = MaterialFamily.Vinyl,
    weight = None,
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.SmoothSurface),
    description = Some(LocalizedString(
      "Heavy-duty 510 g/m² PVC frontlit banner material. Excellent UV and weather resistance for outdoor large-format banners. Suitable for grommets and hem finishing.",
      "Odolný PVC frontlit bannerový materiál 510 g/m². Vynikající odolnost proti UV záření a povětrnostním vlivům pro venkovní velkoformátové bannery. Vhodný pro průchodky a lemování.",
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
  private[samples] val allCoatedGlossyIds: Set[MaterialId] = Set(
    coatedGlossy90gsmId, coatedGlossy115gsmId, coatedGlossy130gsmId,
    coatedGlossy150gsmId, coatedGlossy170gsmId, coatedGlossy200gsmId,
    coatedGlossy250gsmId, coatedGlossy350gsmId,
  )

  private[samples] val allCoatedMatteIds: Set[MaterialId] = Set(
    coatedMatte90gsmId, coatedMatte115gsmId, coatedMatte130gsmId,
    coatedMatte150gsmId, coatedMatte170gsmId, coatedMatte200gsmId,
    coatedMatte250gsmId, coatedMatte300gsmId, coatedMatte350gsmId,
  )

  private[samples] val heavyCoatedGlossyIds: Set[MaterialId] = Set(
    coatedGlossy250gsmId, coatedGlossy350gsmId,
  )

  private[samples] val heavyCoatedMatteIds: Set[MaterialId] = Set(
    coatedMatte250gsmId, coatedMatte300gsmId, coatedMatte350gsmId,
  )

  private[samples] val mediumHeavyCoatedGlossyIds: Set[MaterialId] = Set(
    coatedGlossy170gsmId, coatedGlossy200gsmId,
    coatedGlossy250gsmId, coatedGlossy350gsmId,
  )

  private[samples] val mediumHeavyCoatedMatteIds: Set[MaterialId] = Set(
    coatedMatte170gsmId, coatedMatte200gsmId,
    coatedMatte250gsmId, coatedMatte300gsmId, coatedMatte350gsmId,
  )

