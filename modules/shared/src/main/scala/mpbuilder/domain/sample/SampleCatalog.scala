package mpbuilder.domain.sample

import mpbuilder.domain.*
import mpbuilder.domain.catalog.*
import mpbuilder.domain.ids.*
import SampleIds.{category as cat, finish as fin, ink as inks, material as mat, method as met}

/** Full sample catalog — a faithful transcription of
  * docs/ideas/full-catalog-czk-price-list.md §1 (categories), §2 (materials),
  * §3 (finishes), §4 (printing methods & inks).
  */
object SampleCatalog:

  import MaterialProperty.*

  // ----- materials (§2) -------------------------------------------------------

  private val glossyPapers = mat.glossyWeights.map { gsm =>
    Material(
      mat.coatedGlossy(gsm),
      LocalizedText(s"Coated Art Paper Glossy ${gsm}gsm", s"Křídový papír lesklý $gsm g/m²"),
      Some(gsm),
      Set(Glossy, Smooth, Recyclable),
    )
  }

  private val mattePapers = mat.matteWeights.map { gsm =>
    Material(
      mat.coatedMatte(gsm),
      LocalizedText(s"Coated Art Paper Matte ${gsm}gsm", s"Křídový papír matný $gsm g/m²"),
      Some(gsm),
      Set(Matte, Smooth, Recyclable),
    )
  }

  val materials: List[Material] =
    glossyPapers ++ mattePapers ++ List(
      Material(mat.coatedArt300, LocalizedText("Coated Art Paper 300gsm", "Křídový papír 300 g/m²"), Some(300), Set(Glossy, Smooth, Recyclable)),
      Material(mat.coatedSilk250, LocalizedText("Coated Silk 250gsm", "Křídový papír silk 250 g/m²"), Some(250), Set(Matte, Smooth, Recyclable)),
      Material(mat.uncoatedBond120, LocalizedText("Uncoated Bond Paper 120gsm", "Ofsetový papír 120 g/m²"), Some(120), Set(Matte, Recyclable)),
      Material(mat.kraft250, LocalizedText("Kraft Paper 250gsm", "Kraftový papír 250 g/m²"), Some(250), Set(Textured, Recyclable)),
      Material(mat.cotton300, LocalizedText("Cotton Paper 300gsm", "Bavlněný papír 300 g/m²"), Some(300), Set(Textured, Recyclable)),
      Material(mat.yupoSynthetic, LocalizedText("Yupo Synthetic 200μm", "Syntetický papír Yupo 200 μm"), Some(200), Set(WaterResistant, Smooth)),
      Material(mat.adhesiveStock, LocalizedText("Adhesive Stock 100gsm", "Samolepicí papír 100 g/m²"), Some(100), Set(Glossy, Smooth)),
      // area-priced
      Material(mat.adhesiveVinyl, LocalizedText("Adhesive Vinyl", "Samolepicí vinyl"), None, Set(WaterResistant, Glossy, Smooth)),
      Material(mat.clearAdhesiveVinyl, LocalizedText("Clear Adhesive Vinyl", "Průhledný samolepicí vinyl"), None, Set(WaterResistant, Smooth, Transparent)),
      Material(mat.polyesterBannerFilm, LocalizedText("Polyester Banner Film 510gsm", "Polyesterová banerová fólie 510 g/m²"), Some(510), Set(WaterResistant, Smooth)),
      Material(mat.pvcBanner510, LocalizedText("PVC Banner 510g", "PVC baner 510 g/m²"), Some(510), Set(WaterResistant, Smooth)),
      // flat per-unit
      Material(mat.corrugatedCardboard, LocalizedText("Corrugated Cardboard", "Vlnitá lepenka"), None, Set(Recyclable, Textured)),
      Material(mat.rollUpStandEconomy, LocalizedText("Roll-Up Stand Economy", "Roll-up stojan Economy"), None, Set()),
      Material(mat.rollUpStandPremium, LocalizedText("Roll-Up Stand Premium", "Roll-up stojan Premium"), None, Set()),
      // promotional
      Material(mat.cottonTshirt150, LocalizedText("Cotton T-Shirt 150gsm", "Bavlněné tričko 150 g/m²"), Some(150), Set()),
      Material(mat.cottonTshirt180, LocalizedText("Cotton T-Shirt 180gsm", "Bavlněné tričko 180 g/m²"), Some(180), Set()),
      Material(mat.polyesterTshirt, LocalizedText("Polyester T-Shirt", "Polyesterové tričko"), Some(140), Set()),
      Material(mat.cottonPolyBlendTshirt, LocalizedText("Cotton-Polyester Blend T-Shirt", "Tričko z směsi bavlna-polyester"), Some(160), Set()),
      Material(mat.organicCottonTshirt, LocalizedText("Organic Cotton T-Shirt 180gsm", "Tričko z bio bavlny 180 g/m²"), Some(180), Set(Recyclable)),
      Material(mat.cottonCanvasBag, LocalizedText("Cotton Canvas Bag 220gsm", "Taška z bavlněného kanvasu 220 g/m²"), Some(220), Set(Recyclable)),
      Material(mat.organicCottonBag, LocalizedText("Organic Cotton Bag 180gsm", "Taška z bio bavlny 180 g/m²"), Some(180), Set(Recyclable)),
      Material(mat.recycledPetBag, LocalizedText("Recycled PET Bag", "Taška z recyklovaného PET"), Some(150), Set(Recyclable)),
      Material(mat.juteBag, LocalizedText("Jute/Burlap Bag", "Jutová taška"), Some(300), Set(Textured, Recyclable)),
      Material(mat.nonWovenPpBag, LocalizedText("Non-Woven Polypropylene Bag", "Taška z netkaného polypropylenu"), Some(80), Set()),
      Material(mat.tinplateBadge, LocalizedText("Tinplate Badge Blank", "Plechový odznak"), None, Set()),
      Material(mat.acrylicBadge, LocalizedText("Acrylic Badge Blank", "Akrylový odznak"), None, Set()),
      Material(mat.woodenBadge, LocalizedText("Wooden Badge Blank", "Dřevěný odznak"), None, Set(Textured)),
      Material(mat.ceramicMugWhite, LocalizedText("White Ceramic Mug 330ml", "Bílý keramický hrnek 330 ml"), None, Set()),
      Material(mat.ceramicMugColored, LocalizedText("Colored Ceramic Mug 330ml", "Barevný keramický hrnek 330 ml"), None, Set()),
      Material(mat.magicMug, LocalizedText("Magic Color-Changing Mug 330ml", "Magický hrnek měnící barvu 330 ml"), None, Set()),
      Material(mat.stainlessTravelMug, LocalizedText("Stainless Steel Travel Mug 450ml", "Nerezový cestovní hrnek 450 ml"), None, Set()),
      Material(mat.enamelMug, LocalizedText("Enamel Mug 350ml", "Smaltovaný hrnek 350 ml"), None, Set()),
      Material(mat.glassMug, LocalizedText("Glass Mug 300ml", "Skleněný hrnek 300 ml"), None, Set(Transparent)),
    )

  // ----- finishes (§3) --------------------------------------------------------

  import FinishType as FT

  val finishes: List[Finish] = List(
    Finish(fin.matteLamination, LocalizedText("Matte Lamination", "Matné lamino"), FT.Lamination),
    Finish(fin.glossLamination, LocalizedText("Gloss Lamination", "Lesklé lamino"), FT.Lamination),
    Finish(fin.softTouchCoating, LocalizedText("Soft Touch Coating", "Soft touch lamino"), FT.SoftTouch),
    Finish(fin.uvCoating, LocalizedText("UV Coating", "UV lak"), FT.UvCoating),
    Finish(fin.aqueousCoating, LocalizedText("Aqueous Coating", "Disperzní lak"), FT.AqueousCoating),
    Finish(fin.spotVarnish, LocalizedText("Spot Varnish", "Parciální lak"), FT.SpotVarnish),
    Finish(fin.embossing, LocalizedText("Embossing", "Slepotisk (ražba)"), FT.Embossing),
    Finish(fin.debossing, LocalizedText("Debossing", "Negativní ražba"), FT.Debossing),
    Finish(fin.foilStamping, LocalizedText("Foil Stamping", "Ražba fólií"), FT.FoilStamping),
    Finish(fin.dieCut, LocalizedText("Die Cut", "Výsek"), FT.DieCut),
    Finish(fin.kissCut, LocalizedText("Kiss Cut", "Kiss-cut výsek"), FT.KissCut),
    Finish(fin.scoring, LocalizedText("Scoring / Creasing", "Rylování"), FT.Scoring),
    Finish(fin.perforation, LocalizedText("Perforation", "Perforace"), FT.Perforation),
    Finish(fin.roundCorners, LocalizedText("Round Corners", "Kulaté rohy"), FT.RoundCorners),
    Finish(fin.grommets, LocalizedText("Grommets", "Očka (grometky)"), FT.Grommets),
    Finish(fin.overlamination, LocalizedText("Overlamination", "Ochranná laminace"), FT.Overlamination),
    Finish(fin.gumRope, LocalizedText("Gum Rope", "Napínací guma"), FT.GumRope),
    Finish(fin.heatPressTransfer, LocalizedText("Heat Press Transfer", "Termotransfer"), FT.HeatPressTransfer),
    Finish(fin.labelTagPrinting, LocalizedText("Label / Tag Printing", "Tisk etiket a visaček"), FT.LabelTagPrinting),
    Finish(fin.foldBagPackaging, LocalizedText("Fold & Bag Packaging", "Složení a balení do sáčku"), FT.FoldAndBag),
    Finish(fin.mylarOverlay, LocalizedText("Mylar Film Overlay", "Mylarová fólie"), FT.MylarOverlay),
    Finish(fin.safetyPinBack, LocalizedText("Safety Pin Back", "Zadní špendlík"), FT.SafetyPinBack),
    Finish(fin.magnetBack, LocalizedText("Magnet Back", "Zadní magnet"), FT.MagnetBack),
    Finish(fin.bottleOpenerBack, LocalizedText("Bottle Opener Back", "Otvírák na lahve"), FT.BottleOpenerBack),
    Finish(fin.dishwasherCoating, LocalizedText("Dishwasher-Safe Coating", "Povrch do myčky"), FT.DishwasherCoating),
    Finish(fin.giftBoxPackaging, LocalizedText("Gift Box Packaging", "Dárková krabička"), FT.GiftBoxPackaging),
    Finish(fin.ceramicGlaze, LocalizedText("Glossy Ceramic Glaze", "Lesklá keramická glazura"), FT.CeramicGlaze),
    Finish(fin.embroidery, LocalizedText("Embroidery", "Výšivka"), FT.Embroidery),
    Finish(fin.reinforcedHandles, LocalizedText("Reinforced Handles", "Zpevněná ucha"), FT.ReinforcedHandles),
  )

  // ----- printing methods & inks (§4) ------------------------------------------

  val printingMethods: List[PrintingMethod] = List(
    PrintingMethod(met.digital, LocalizedText("Digital Printing", "Digitální tisk"), None),
    PrintingMethod(met.offset, LocalizedText("Offset Printing", "Ofsetový tisk"), Some(6)),
    PrintingMethod(met.letterpress, LocalizedText("Letterpress", "Knihtisk"), Some(2)),
    PrintingMethod(met.uvInkjet, LocalizedText("UV Curable Inkjet", "UV inkjet"), None),
    PrintingMethod(met.screenPrinting, LocalizedText("Screen Printing", "Sítotisk"), Some(8)),
    PrintingMethod(met.dtg, LocalizedText("Direct-to-Garment (DTG)", "Přímý potisk textilu (DTG)"), None),
    PrintingMethod(met.sublimation, LocalizedText("Dye Sublimation", "Sublimační tisk"), None),
  )

  val inkConfigurations: List[InkConfiguration] = List(
    InkConfiguration(inks.fullColorBoth, LocalizedText("4/4 — full color both sides", "4/4 — plnobarevně oboustranně"), 4, 4),
    InkConfiguration(inks.fullColorFront, LocalizedText("4/0 — full color front only", "4/0 — plnobarevně jednostranně"), 4, 0),
    InkConfiguration(inks.fullFrontBlackBack, LocalizedText("4/1 — full color front, black back", "4/1 — plnobarevná přední, černá zadní"), 4, 1),
    InkConfiguration(inks.blackFront, LocalizedText("1/0 — black front only", "1/0 — černá jednostranně"), 1, 0),
    InkConfiguration(inks.blackBoth, LocalizedText("1/1 — black both sides", "1/1 — černá oboustranně"), 1, 1),
  )

  // ----- categories (§1) --------------------------------------------------------

  private val allGlossy = mat.glossyWeights.map(mat.coatedGlossy)
  private val allMatte  = mat.matteWeights.map(mat.coatedMatte)

  private def main(materials: Seq[MaterialId], finishes: Seq[FinishId]) =
    List(ComponentSpec(ComponentRole.Main, optional = false, AllowList.Only(materials.toSet), AllowList.Only(finishes.toSet)))

  import RequiredDetail.*

  val categories: List[Category] = List(
    Category(
      cat.businessCards,
      LocalizedText("Business Cards", "Vizitky"),
      main(
        List(mat.coatedArt300, mat.uncoatedBond120, mat.kraft250, mat.yupoSynthetic, mat.cotton300, mat.coatedSilk250,
          mat.coatedGlossy(250), mat.coatedGlossy(350), mat.coatedMatte(250), mat.coatedMatte(300), mat.coatedMatte(350)),
        List(fin.matteLamination, fin.glossLamination, fin.uvCoating, fin.embossing, fin.foilStamping, fin.softTouchCoating, fin.roundCorners),
      ),
      Set(Size, Quantity),
      AllowList.of(met.digital, met.letterpress),
    ),
    Category(
      cat.flyers,
      LocalizedText("Flyers", "Letáky"),
      main(
        List(mat.coatedArt300, mat.uncoatedBond120) ++ allGlossy ++ allMatte,
        List(fin.matteLamination, fin.glossLamination, fin.uvCoating, fin.spotVarnish, fin.aqueousCoating, fin.roundCorners, fin.scoring),
      ),
      Set(Size, Quantity, Orientation),
      AllowList.of(met.digital),
    ),
    Category(
      cat.brochures,
      LocalizedText("Brochures", "Brožury"),
      main(
        List(mat.coatedArt300, mat.uncoatedBond120, mat.coatedSilk250) ++ allGlossy ++ allMatte,
        List(fin.matteLamination, fin.glossLamination, fin.uvCoating, fin.scoring, fin.roundCorners),
      ),
      Set(Size, Quantity, Fold),
      AllowList.of(met.digital),
    ),
    Category(
      cat.banners,
      LocalizedText("Banners", "Banery"),
      main(List(mat.pvcBanner510), List(fin.uvCoating, fin.dieCut, fin.grommets, fin.gumRope)),
      Set(Size, Quantity),
      AllowList.of(met.uvInkjet),
    ),
    Category(
      cat.packaging,
      LocalizedText("Packaging", "Obaly"),
      main(
        List(mat.kraft250, mat.corrugatedCardboard, mat.yupoSynthetic),
        List(fin.matteLamination, fin.uvCoating, fin.embossing, fin.foilStamping, fin.dieCut, fin.scoring, fin.perforation, fin.debossing),
      ),
      Set(Size, Quantity),
      AllowList.of(met.digital),
    ),
    Category(
      cat.booklets,
      LocalizedText("Booklets", "Katalogy"),
      List(
        ComponentSpec(
          ComponentRole.Cover,
          optional = false,
          AllowList.Only((List(mat.coatedArt300, mat.coatedSilk250) ++ allGlossy ++ allMatte).toSet),
          AllowList.of(fin.matteLamination, fin.glossLamination, fin.uvCoating, fin.roundCorners),
        ),
        ComponentSpec(
          ComponentRole.Body,
          optional = false,
          AllowList.Only((List(mat.coatedArt300, mat.uncoatedBond120, mat.coatedSilk250) ++ allGlossy ++ allMatte).toSet),
          AllowList.of(fin.perforation),
        ),
      ),
      Set(Size, Quantity, Pages, Binding),
      AllowList.of(met.digital),
    ),
    Category(
      cat.calendars,
      LocalizedText("Calendars", "Kalendáře"),
      List(
        ComponentSpec(
          ComponentRole.Cover,
          optional = false,
          AllowList.Only(
            (List(mat.coatedArt300, mat.coatedSilk250)
              ++ List(170, 200, 250, 350).map(mat.coatedGlossy)
              ++ List(170, 200, 250, 300, 350).map(mat.coatedMatte)).toSet
          ),
          AllowList.of(fin.matteLamination, fin.glossLamination, fin.uvCoating),
        ),
        ComponentSpec(
          ComponentRole.Body,
          optional = false,
          AllowList.Only(
            (List(mat.coatedArt300, mat.coatedSilk250, mat.uncoatedBond120)
              ++ List(170, 200, 250, 350).map(mat.coatedGlossy)
              ++ List(170, 200, 250, 300, 350).map(mat.coatedMatte)).toSet
          ),
          AllowList.of(fin.perforation),
        ),
      ),
      Set(Size, Quantity, Pages, Binding),
      AllowList.of(met.digital),
    ),
    Category(
      cat.postcards,
      LocalizedText("Postcards", "Pohlednice"),
      main(
        List(mat.coatedArt300, mat.coatedSilk250, mat.cotton300,
          mat.coatedGlossy(250), mat.coatedGlossy(350), mat.coatedMatte(250), mat.coatedMatte(350)),
        List(fin.matteLamination, fin.glossLamination, fin.uvCoating, fin.softTouchCoating, fin.embossing,
          fin.foilStamping, fin.roundCorners, fin.aqueousCoating),
      ),
      Set(Size, Quantity),
      AllowList.of(met.offset, met.digital),
    ),
    Category(
      cat.stickers,
      LocalizedText("Stickers & Labels", "Samolepky a etikety"),
      main(
        List(mat.adhesiveStock, mat.yupoSynthetic, mat.clearAdhesiveVinyl),
        List(fin.kissCut, fin.dieCut, fin.roundCorners, fin.uvCoating),
      ),
      Set(Size, Quantity),
      AllowList.of(met.digital, met.uvInkjet),
    ),
    Category(
      cat.rollUps,
      LocalizedText("Roll-Up Banners", "Roll-up banery"),
      List(
        ComponentSpec(ComponentRole.Main, optional = false,
          AllowList.of(mat.polyesterBannerFilm), AllowList.of(fin.overlamination)),
        ComponentSpec(ComponentRole.Stand, optional = true,
          AllowList.of(mat.rollUpStandEconomy, mat.rollUpStandPremium), AllowList.of[FinishId]()),
      ),
      Set(Size, Quantity),
      AllowList.of(met.uvInkjet),
    ),
    Category(
      cat.freeConfig,
      LocalizedText("Free Configuration", "Volná konfigurace"),
      List(ComponentSpec(ComponentRole.Main, optional = false, AllowList.All(), AllowList.All())),
      Set(Size, Quantity),
      AllowList.All(),
    ),
    Category(
      cat.tShirts,
      LocalizedText("T-Shirts", "Trička"),
      main(
        List(mat.cottonTshirt150, mat.cottonTshirt180, mat.polyesterTshirt, mat.cottonPolyBlendTshirt, mat.organicCottonTshirt),
        List(fin.heatPressTransfer, fin.labelTagPrinting, fin.foldBagPackaging),
      ),
      Set(Size, Quantity),
      AllowList.of(met.screenPrinting, met.dtg, met.sublimation),
    ),
    Category(
      cat.ecoBags,
      LocalizedText("Eco Bags", "Eko tašky"),
      main(
        List(mat.cottonCanvasBag, mat.organicCottonBag, mat.recycledPetBag, mat.juteBag, mat.nonWovenPpBag),
        List(fin.heatPressTransfer, fin.embroidery, fin.reinforcedHandles, fin.foldBagPackaging),
      ),
      Set(Size, Quantity),
      AllowList.of(met.screenPrinting, met.dtg),
    ),
    Category(
      cat.pinBadges,
      LocalizedText("Pin Badges", "Odznaky"),
      main(
        List(mat.tinplateBadge, mat.acrylicBadge, mat.woodenBadge),
        List(fin.mylarOverlay, fin.safetyPinBack, fin.magnetBack, fin.bottleOpenerBack),
      ),
      Set(Size, Quantity),
      AllowList.of(met.digital, met.offset),
    ),
    Category(
      cat.cupsMugs,
      LocalizedText("Cups & Mugs", "Hrnky"),
      main(
        List(mat.ceramicMugWhite, mat.ceramicMugColored, mat.magicMug, mat.stainlessTravelMug, mat.enamelMug, mat.glassMug),
        List(fin.dishwasherCoating, fin.giftBoxPackaging, fin.ceramicGlaze),
      ),
      Set(Size, Quantity),
      AllowList.of(met.sublimation, met.screenPrinting, met.uvInkjet),
    ),
  )

  val catalog: Catalog = Catalog(categories, materials, finishes, printingMethods, inkConfigurations)
