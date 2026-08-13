package mpbuilder.domain.sample

import mpbuilder.domain.*
import mpbuilder.domain.catalog.*
import mpbuilder.domain.config.*
import mpbuilder.domain.ids.*
import SampleIds.{category as cat, finish as fin, ink as inks, material as mat, method as met}

/** Quick-order preset recipes — transcription of
  * docs/ideas/full-catalog-czk-price-list.md §10.
  *
  * Where the doc leaves a preset's printing method or ink implicit, the
  * category's default method and a sensible ink setup are used. Promotional
  * "sizes" are nominal print areas (the categories require a Size detail).
  */
object SamplePresets:

  private val a4 = DimensionsMm(210, 297)
  private val a5 = DimensionsMm(148, 210)
  private val a6 = DimensionsMm(105, 148)
  private val businessCard = DimensionsMm(85, 55)

  private def preset(
    id: String,
    categoryId: CategoryId,
    en: String,
    cs: String,
    method: PrintingMethodId,
    ink: InkConfigId,
    components: List[ComponentConfiguration],
    size: DimensionsMm,
    quantity: Int,
    orientation: Option[Orientation] = None,
    pages: Option[Int] = None,
    fold: Option[FoldType] = None,
    binding: Option[BindingMethod] = None,
  ): Preset =
    Preset(
      PresetId(id),
      categoryId,
      LocalizedText(en, cs),
      ProductConfiguration(
        categoryId = categoryId,
        printingMethodId = method,
        inkConfigurationId = ink,
        components = components,
        details = ProductDetails(Some(size), Some(quantity), orientation, pages, fold, binding),
        speedTier = SpeedTier.Standard,
      ),
    )

  private def mainComp(materialId: MaterialId, finishes: SelectedFinish*) =
    List(ComponentConfiguration(ComponentRole.Main, materialId, finishes.toList))

  private def f(id: FinishId): SelectedFinish = SelectedFinish(id)

  val presets: List[Preset] = List(
    // Business Cards
    preset("bc-basic", cat.businessCards, "Basic", "Základní", met.digital, inks.fullColorFront,
      mainComp(mat.coatedArt300), businessCard, 100),
    preset("bc-premium", cat.businessCards, "Premium", "Prémiové", met.digital, inks.fullColorBoth,
      mainComp(mat.coatedMatte(350), f(fin.matteLamination),
        SelectedFinish(fin.roundCorners, Some(FinishParams.RoundCorners(4, 3)))),
      businessCard, 100),
    // Flyers
    preset("flyers-standard", cat.flyers, "Standard", "Standardní", met.digital, inks.fullColorFront,
      mainComp(mat.coatedGlossy(130)), a5, 500, orientation = Some(Orientation.Portrait)),
    preset("flyers-premium", cat.flyers, "Premium", "Prémiové", met.digital, inks.fullColorBoth,
      mainComp(mat.coatedMatte(250), f(fin.matteLamination)), a5, 500, orientation = Some(Orientation.Portrait)),
    preset("flyers-lightweight", cat.flyers, "Lightweight", "Lehké", met.digital, inks.fullColorFront,
      mainComp(mat.coatedGlossy(90)), a5, 1000, orientation = Some(Orientation.Portrait)),
    // Brochures
    preset("brochures-standard", cat.brochures, "Standard", "Standardní", met.digital, inks.fullColorBoth,
      mainComp(mat.coatedGlossy(150)), a4, 250, fold = Some(FoldType.TriFold)),
    preset("brochures-bifold", cat.brochures, "Bi-Fold", "Půlený lom", met.digital, inks.fullColorBoth,
      mainComp(mat.coatedMatte(200)), a4, 250, fold = Some(FoldType.HalfFold)),
    preset("brochures-zfold", cat.brochures, "Z-Fold", "Z lom", met.digital, inks.fullColorBoth,
      mainComp(mat.coatedGlossy(170)), a4, 250, fold = Some(FoldType.ZFold)),
    // Banners
    preset("banners-standard", cat.banners, "Standard", "Standardní", met.uvInkjet, inks.fullColorFront,
      mainComp(mat.pvcBanner510), DimensionsMm(1000, 1000), 1),
    preset("banners-outdoor-grommets", cat.banners, "Outdoor with Grommets", "Venkovní s očky", met.uvInkjet, inks.fullColorFront,
      mainComp(mat.pvcBanner510, f(fin.uvCoating),
        SelectedFinish(fin.grommets, Some(FinishParams.GrommetSpacing(500)))),
      DimensionsMm(1000, 1500), 1),
    // Packaging
    preset("packaging-standard-kraft", cat.packaging, "Standard Kraft", "Standardní kraft", met.digital, inks.fullColorFront,
      mainComp(mat.kraft250), DimensionsMm(300, 200), 100),
    preset("packaging-premium-diecut", cat.packaging, "Premium Die-Cut", "Prémiové s výsekem", met.digital, inks.fullColorFront,
      mainComp(mat.kraft250, f(fin.dieCut), SelectedFinish(fin.scoring, Some(FinishParams.Creases(2)))),
      DimensionsMm(300, 200), 50),
    // Booklets
    preset("booklets-saddle", cat.booklets, "Saddle Stitch", "Šitá vazba", met.digital, inks.fullColorBoth,
      List(
        ComponentConfiguration(ComponentRole.Cover, mat.coatedGlossy(250)),
        ComponentConfiguration(ComponentRole.Body, mat.coatedGlossy(130)),
      ),
      a4, 100, pages = Some(8), binding = Some(BindingMethod.SaddleStitch)),
    preset("booklets-perfect", cat.booklets, "Perfect Binding", "Lepená vazba", met.digital, inks.fullColorBoth,
      List(
        ComponentConfiguration(ComponentRole.Cover, mat.coatedMatte(300), List(f(fin.matteLamination))),
        ComponentConfiguration(ComponentRole.Body, mat.coatedMatte(130)),
      ),
      a4, 50, pages = Some(48), binding = Some(BindingMethod.PerfectBinding)),
    // Calendars
    preset("calendars-wall", cat.calendars, "Wall Calendar", "Nástěnný kalendář", met.digital, inks.fullColorBoth,
      List(
        ComponentConfiguration(ComponentRole.Cover, mat.coatedGlossy(250)),
        ComponentConfiguration(ComponentRole.Body, mat.coatedGlossy(170)),
      ),
      a4, 50, pages = Some(28), binding = Some(BindingMethod.WireOBinding)),
    preset("calendars-desk", cat.calendars, "Desk Calendar", "Stolní kalendář", met.digital, inks.fullColorBoth,
      List(
        ComponentConfiguration(ComponentRole.Cover, mat.coatedMatte(300)),
        ComponentConfiguration(ComponentRole.Body, mat.coatedMatte(200)),
      ),
      a5, 50, pages = Some(28), binding = Some(BindingMethod.WireOBinding)),
    // Postcards
    preset("postcards-standard", cat.postcards, "Standard", "Standardní", met.digital, inks.fullColorBoth,
      mainComp(mat.coatedArt300), a6, 200),
    preset("postcards-premium", cat.postcards, "Premium", "Prémiové", met.digital, inks.fullColorBoth,
      mainComp(mat.cotton300, f(fin.softTouchCoating)), a6, 100),
    // Stickers & Labels
    preset("stickers-standard", cat.stickers, "Standard", "Standardní", met.digital, inks.fullColorFront,
      mainComp(mat.adhesiveStock), DimensionsMm(50, 50), 500),
    preset("stickers-diecut", cat.stickers, "Die-Cut", "S výsekem", met.digital, inks.fullColorFront,
      mainComp(mat.adhesiveStock, f(fin.dieCut)), DimensionsMm(50, 50), 500),
    preset("stickers-clear-vinyl", cat.stickers, "Clear Vinyl", "Průhledný vinyl", met.uvInkjet, inks.fullColorFront,
      mainComp(mat.clearAdhesiveVinyl), DimensionsMm(50, 50), 250),
    // Roll-Up Banners
    preset("rollup-economy", cat.rollUps, "Economy", "Economy", met.uvInkjet, inks.fullColorFront,
      List(
        ComponentConfiguration(ComponentRole.Main, mat.polyesterBannerFilm),
        ComponentConfiguration(ComponentRole.Stand, mat.rollUpStandEconomy),
      ),
      DimensionsMm(850, 2000), 1),
    preset("rollup-premium", cat.rollUps, "Premium", "Premium", met.uvInkjet, inks.fullColorFront,
      List(
        ComponentConfiguration(ComponentRole.Main, mat.polyesterBannerFilm),
        ComponentConfiguration(ComponentRole.Stand, mat.rollUpStandPremium),
      ),
      DimensionsMm(850, 2000), 1),
    // T-Shirts (nominal 300×400 print area)
    preset("tshirts-standard", cat.tShirts, "Standard Cotton", "Standardní bavlna", met.screenPrinting, inks.fullColorFront,
      mainComp(mat.cottonTshirt180), DimensionsMm(300, 400), 50),
    preset("tshirts-eco", cat.tShirts, "Eco-Friendly", "Ekologické", met.screenPrinting, inks.fullColorFront,
      mainComp(mat.organicCottonTshirt, f(fin.foldBagPackaging)), DimensionsMm(300, 400), 50),
    preset("tshirts-dtg", cat.tShirts, "Premium DTG", "Prémiový DTG", met.dtg, inks.fullColorFront,
      mainComp(mat.organicCottonTshirt, f(fin.foldBagPackaging)), DimensionsMm(300, 400), 25),
    preset("tshirts-sublimation", cat.tShirts, "Sublimation All-Over", "Sublimace přes celou plochu", met.sublimation, inks.fullColorFront,
      mainComp(mat.polyesterTshirt, f(fin.foldBagPackaging)), DimensionsMm(300, 400), 100),
    // Eco Bags (nominal 380×420)
    preset("bags-canvas", cat.ecoBags, "Standard Canvas", "Standardní kanvas", met.screenPrinting, inks.blackFront,
      mainComp(mat.cottonCanvasBag, f(fin.reinforcedHandles)), DimensionsMm(380, 420), 100),
    preset("bags-organic", cat.ecoBags, "Organic Eco", "Bio eko", met.dtg, inks.fullColorFront,
      mainComp(mat.organicCottonBag), DimensionsMm(380, 420), 50),
    // Pin Badges
    preset("badges-58", cat.pinBadges, "Standard Round 58mm", "Kulatý 58 mm", met.digital, inks.fullColorFront,
      mainComp(mat.tinplateBadge, f(fin.safetyPinBack)), DimensionsMm(58, 58), 100),
    preset("badges-32", cat.pinBadges, "Small Round 32mm", "Malý kulatý 32 mm", met.digital, inks.fullColorFront,
      mainComp(mat.tinplateBadge, f(fin.safetyPinBack)), DimensionsMm(32, 32), 200),
    preset("badges-magnet", cat.pinBadges, "Magnet Badge", "Odznak s magnetem", met.digital, inks.fullColorFront,
      mainComp(mat.tinplateBadge, f(fin.magnetBack)), DimensionsMm(58, 58), 50),
    // Cups & Mugs (nominal 200×90 print area)
    preset("mugs-standard", cat.cupsMugs, "Standard White Mug", "Standardní bílý hrnek", met.sublimation, inks.fullColorFront,
      mainComp(mat.ceramicMugWhite), DimensionsMm(200, 90), 50),
    preset("mugs-giftset", cat.cupsMugs, "Corporate Gift Set", "Firemní dárková sada", met.sublimation, inks.fullColorFront,
      mainComp(mat.ceramicMugWhite, f(fin.giftBoxPackaging)), DimensionsMm(200, 90), 25),
    preset("mugs-travel", cat.cupsMugs, "Travel Mug", "Cestovní hrnek", met.uvInkjet, inks.fullColorFront,
      mainComp(mat.stainlessTravelMug), DimensionsMm(200, 90), 20),
  )
