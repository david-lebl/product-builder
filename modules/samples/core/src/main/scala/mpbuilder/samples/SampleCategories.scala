package mpbuilder.samples

import mpbuilder.catalog.*
import mpbuilder.kernel.*
import SampleIds.*
import SampleMaterials.*

object SampleCategories:

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
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-bc-basic"),
        name = LocalizedString("Basic", "Základní"),
        description = Some(LocalizedString(
          "Coated 300gsm, 4+0 CMYK, 85×55 mm, 100 pcs",
          "Křídový 300g, 4+0 CMYK, 85×55 mm, 100 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coated300gsmId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(85, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-bc-premium"),
        name = LocalizedString("Premium", "Prémiové"),
        description = Some(LocalizedString(
          "Coated 350gsm, 4+4 CMYK, matte lamination + round corners, 85×55 mm, 100 pcs",
          "Křídový 350g, 4+4 CMYK, matná laminace + zaoblené rohy, 85×55 mm, 100 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coatedMatte350gsmId,
          inkConfiguration = InkConfiguration.cmyk4_4,
          finishSelections = List(
            FinishSelection(matteLaminationId),
            FinishSelection(roundCornersId, Some(FinishParameters.RoundCornersParams(4, 3))),
          ),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(85, 55)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
        ),
      ),
    ),
  )

  val flyers: ProductCategory = ProductCategory(
    id = flyersId,
    name = LocalizedString("Flyers", "Letáky"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(coated300gsmId, uncoatedBondId) ++
      allCoatedGlossyIds ++ allCoatedMatteIds,
      allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId, varnishId, aqueousCoatingId, roundCornersId, scoringId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.Orientation),
    allowedPrintingMethodIds = Set(digitalId),
    description = Some(LocalizedString(
      "Single-sheet promotional flyers in various sizes. Available in a wide range of paper weights from lightweight 90gsm to sturdy 350gsm. Landscape or portrait orientation.",
      "Jednostránkové propagační letáky v různých velikostech. K dispozici v široké škále gramáží od lehkých 90g po pevné 350g. Na výšku nebo na šířku.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-flyers-standard"),
        name = LocalizedString("Standard", "Standardní"),
        description = Some(LocalizedString(
          "Glossy 130gsm, 4+0 CMYK, A5, 500 pcs",
          "Lesklý 130g, 4+0 CMYK, A5, 500 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coatedGlossy130gsmId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(148, 210)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.OrientationSpec(Orientation.Portrait),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-flyers-premium"),
        name = LocalizedString("Premium", "Prémiové"),
        description = Some(LocalizedString(
          "Matte 250gsm, 4+4 CMYK, matte lamination, A5, 500 pcs",
          "Matný 250g, 4+4 CMYK, matná laminace, A5, 500 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coatedMatte250gsmId,
          inkConfiguration = InkConfiguration.cmyk4_4,
          finishSelections = List(FinishSelection(matteLaminationId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(148, 210)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
          SpecValue.OrientationSpec(Orientation.Portrait),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-flyers-lightweight"),
        name = LocalizedString("Lightweight", "Lehké"),
        description = Some(LocalizedString(
          "Glossy 90gsm, 4+0 CMYK, A5, 1000 pcs",
          "Lesklý 90g, 4+0 CMYK, A5, 1000 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coatedGlossy90gsmId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(148, 210)),
          SpecValue.QuantitySpec(Quantity.unsafe(1000)),
          SpecValue.OrientationSpec(Orientation.Portrait),
        ),
      ),
    ),
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
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-brochures-standard"),
        name = LocalizedString("Standard", "Standardní"),
        description = Some(LocalizedString(
          "Glossy 150gsm, 4+4 CMYK, A4 tri-fold, 250 pcs",
          "Lesklý 150g, 4+4 CMYK, A4 na třetiny, 250 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coatedGlossy150gsmId,
          inkConfiguration = InkConfiguration.cmyk4_4,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(210, 297)),
          SpecValue.QuantitySpec(Quantity.unsafe(250)),
          SpecValue.FoldTypeSpec(FoldType.Tri),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-brochures-bifold"),
        name = LocalizedString("Bi-Fold", "Na půl"),
        description = Some(LocalizedString(
          "Matte 200gsm, 4+4 CMYK, A4 bi-fold, 250 pcs",
          "Matný 200g, 4+4 CMYK, A4 na půl, 250 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coatedMatte200gsmId,
          inkConfiguration = InkConfiguration.cmyk4_4,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(210, 297)),
          SpecValue.QuantitySpec(Quantity.unsafe(250)),
          SpecValue.FoldTypeSpec(FoldType.Half),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-brochures-zfold"),
        name = LocalizedString("Z-Fold", "Z-sklad"),
        description = Some(LocalizedString(
          "Glossy 170gsm, 4+4 CMYK, A4 Z-fold, 250 pcs",
          "Lesklý 170g, 4+4 CMYK, A4 Z-sklad, 250 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coatedGlossy170gsmId,
          inkConfiguration = InkConfiguration.cmyk4_4,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(210, 297)),
          SpecValue.QuantitySpec(Quantity.unsafe(250)),
          SpecValue.FoldTypeSpec(FoldType.ZFold),
        ),
      ),
    ),
  )

  val banners: ProductCategory = ProductCategory(
    id = bannersId,
    name = LocalizedString("Banners", "Bannery"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(pvc510gId),
      allowedFinishIds = Set(uvCoatingId, dieCutId, grommetsId, gumRopeId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(uvInkjetId),
    description = Some(LocalizedString(
      "Large-format PVC banners for outdoor and indoor use. Printed with UV-curable inks for weather resistance. Optional grommets for hanging, gum rope for tensioning, and die-cutting for custom shapes.",
      "Velkoformátové PVC bannery pro venkovní i vnitřní použití. Tištěné UV vytvrzovanými inkousty pro odolnost proti povětrnostním vlivům. Volitelné průchodky pro zavěšení, gumový provaz pro napnutí a výsek pro vlastní tvary.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-banners-standard"),
        name = LocalizedString("Standard", "Standardní"),
        description = Some(LocalizedString(
          "PVC 510g, 4+0 CMYK, 1000×1000 mm, 1 pc",
          "PVC 510g, 4+0 CMYK, 1000×1000 mm, 1 ks",
        )),
        printingMethodId = uvInkjetId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = pvc510gId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(1000, 1000)),
          SpecValue.QuantitySpec(Quantity.unsafe(1)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-banners-outdoor"),
        name = LocalizedString("Outdoor with Grommets", "Exteriérový s průchodkami"),
        description = Some(LocalizedString(
          "PVC 510g, 4+0 CMYK, UV coating + grommets (500 mm), 1000×1500 mm, 1 pc",
          "PVC 510g, 4+0 CMYK, UV lak + průchodky (500 mm), 1000×1500 mm, 1 ks",
        )),
        printingMethodId = uvInkjetId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = pvc510gId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(
            FinishSelection(uvCoatingId),
            FinishSelection(grommetsId, Some(FinishParameters.GrommetParams(500))),
          ),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(1000, 1500)),
          SpecValue.QuantitySpec(Quantity.unsafe(1)),
        ),
      ),
    ),
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
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-packaging-standard"),
        name = LocalizedString("Standard Kraft", "Standardní kraft"),
        description = Some(LocalizedString(
          "Kraft, 4+0 CMYK, 300×200 mm, 100 pcs",
          "Kraft, 4+0 CMYK, 300×200 mm, 100 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = kraftId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(300, 200)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-packaging-premium"),
        name = LocalizedString("Premium Die-Cut", "Prémiové s výsekem"),
        description = Some(LocalizedString(
          "Kraft, 4+0 CMYK, die-cut + scoring, 300×200 mm, 50 pcs",
          "Kraft, 4+0 CMYK, výsek + bigování, 300×200 mm, 50 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = kraftId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(
            FinishSelection(dieCutId),
            FinishSelection(scoringId),
          ),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(300, 200)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
        ),
      ),
    ),
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
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-booklets-standard"),
        name = LocalizedString("Saddle Stitch", "V-vazba"),
        description = Some(LocalizedString(
          "Glossy 250gsm cover + 130gsm body, 4+4 CMYK, A4, saddle stitch, 8 pages, 100 pcs",
          "Lesklý 250g obálka + 130g tělo, 4+4 CMYK, A4, V-vazba, 8 stran, 100 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(
          ComponentPreset(
            role = ComponentRole.Cover,
            materialId = coatedGlossy250gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
          ),
          ComponentPreset(
            role = ComponentRole.Body,
            materialId = coatedGlossy130gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
          ),
        ),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(210, 297)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
          SpecValue.PagesSpec(8),
          SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-booklets-perfect"),
        name = LocalizedString("Perfect Binding", "Lepená vazba"),
        description = Some(LocalizedString(
          "Matte 300gsm cover + 130gsm body, 4+4 CMYK, A4, perfect binding, 48 pages, 50 pcs",
          "Matný 300g obálka + 130g tělo, 4+4 CMYK, A4, lepená vazba, 48 stran, 50 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(
          ComponentPreset(
            role = ComponentRole.Cover,
            materialId = coatedMatte300gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
            finishSelections = List(FinishSelection(matteLaminationId)),
          ),
          ComponentPreset(
            role = ComponentRole.Body,
            materialId = coatedMatte130gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
          ),
        ),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(210, 297)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
          SpecValue.PagesSpec(48),
          SpecValue.BindingMethodSpec(BindingMethod.PerfectBinding),
        ),
      ),
    ),
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
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-calendars-wall"),
        name = LocalizedString("Wall Calendar", "Nástěnný kalendář"),
        description = Some(LocalizedString(
          "Glossy 250gsm cover + 170gsm body, 4+4 CMYK, A4, wire-o, 28 pages, 50 pcs",
          "Lesklý 250g obálka + 170g tělo, 4+4 CMYK, A4, kroužková vazba, 28 stran, 50 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(
          ComponentPreset(
            role = ComponentRole.Cover,
            materialId = coatedGlossy250gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
          ),
          ComponentPreset(
            role = ComponentRole.Body,
            materialId = coatedGlossy170gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
          ),
        ),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(210, 297)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
          SpecValue.PagesSpec(28),
          SpecValue.BindingMethodSpec(BindingMethod.WireOBinding),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-calendars-desk"),
        name = LocalizedString("Desk Calendar", "Stolní kalendář"),
        description = Some(LocalizedString(
          "Matte 300gsm cover + 200gsm body, 4+4 CMYK, A5, wire-o, 28 pages, 50 pcs",
          "Matný 300g obálka + 200g tělo, 4+4 CMYK, A5, kroužková vazba, 28 stran, 50 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(
          ComponentPreset(
            role = ComponentRole.Cover,
            materialId = coatedMatte300gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
          ),
          ComponentPreset(
            role = ComponentRole.Body,
            materialId = coatedMatte200gsmId,
            inkConfiguration = InkConfiguration.cmyk4_4,
          ),
        ),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(148, 210)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
          SpecValue.PagesSpec(28),
          SpecValue.BindingMethodSpec(BindingMethod.WireOBinding),
        ),
      ),
    ),
  )

  private val allMaterialIds: Set[MaterialId] = Set(
    coated300gsmId, uncoatedBondId, kraftId, vinylId, corrugatedId,
    coatedSilk250gsmId, yupoId, adhesiveStockId, cottonId, clearVinylId,
    pvc510gId,
    // Promotional materials
    cottonTshirt150Id, cottonTshirt180Id, polyesterTshirtId, cottonPolyBlendId, organicCottonTshirtId,
    cottonCanvasBagId, organicCottonBagId, recycledPetBagId, juteBagId, nonWovenPpBagId,
    tinplateBadgeId, acrylicBadgeId, woodenBadgeId,
    ceramicMugWhiteId, ceramicMugColoredId, magicMugId, stainlessTravelMugId, enamelMugId, glassMugId,
  ) ++ allCoatedGlossyIds ++ allCoatedMatteIds

  private val allFinishIds: Set[FinishId] = Set(
    matteLaminationId, glossLaminationId, uvCoatingId, embossingId,
    foilStampingId, dieCutId, varnishId, softTouchCoatingId, aqueousCoatingId,
    debossingId, scoringId, perforationId, roundCornersId, grommetsId, kissCutId,
    overlaminationId, gumRopeId,
    // Promotional finishes
    heatPressId, labelPrintId, foldBagId, mylarOverlayId, safetyPinId, magnetBackId,
    bottleOpenerId, dishwasherCoatId, giftBoxId, glossyGlazeId, embroideryId, reinforcedHandlesId,
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
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-postcards-standard"),
        name = LocalizedString("Standard", "Standardní"),
        description = Some(LocalizedString(
          "Coated 300gsm, 4+4 CMYK, A6, 200 pcs",
          "Křídový 300g, 4+4 CMYK, A6, 200 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = coated300gsmId,
          inkConfiguration = InkConfiguration.cmyk4_4,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(105, 148)),
          SpecValue.QuantitySpec(Quantity.unsafe(200)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-postcards-premium"),
        name = LocalizedString("Premium", "Prémiové"),
        description = Some(LocalizedString(
          "Cotton 300gsm, 4+4 CMYK, soft-touch coating, A6, 100 pcs",
          "Bavlněný 300g, 4+4 CMYK, soft-touch lak, A6, 100 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = cottonId,
          inkConfiguration = InkConfiguration.cmyk4_4,
          finishSelections = List(FinishSelection(softTouchCoatingId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(105, 148)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
        ),
      ),
    ),
  )

  val stickers: ProductCategory = ProductCategory(
    id = stickersId,
    name = LocalizedString("Stickers & Labels", "Samolepky a štítky"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(adhesiveStockId, yupoId, vinylId, clearVinylId),
      allowedFinishIds = Set(kissCutId, dieCutId, roundCornersId, uvCoatingId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(digitalId, uvInkjetId, solventInkjetId, epson8ColorId),
    description = Some(LocalizedString(
      "Custom stickers and product labels on adhesive, synthetic, or clear vinyl stock. Kiss-cut for peel-off sheets or die-cut for individual shapes.",
      "Zakázkové samolepky a produktové štítky na samolepicím, syntetickém nebo průhledném vinylovém materiálu. Výsek bez podkladu pro odlepovací archy nebo výsek pro jednotlivé tvary.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-stickers-standard"),
        name = LocalizedString("Standard", "Standardní"),
        description = Some(LocalizedString(
          "Adhesive stock, 4+0 CMYK, 50×50 mm, 500 pcs",
          "Samolepicí materiál, 4+0 CMYK, 50×50 mm, 500 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = adhesiveStockId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(50, 50)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-stickers-diecut"),
        name = LocalizedString("Die-Cut", "Výsekové"),
        description = Some(LocalizedString(
          "Adhesive stock, 4+0 CMYK, die-cut, 50×50 mm, 500 pcs",
          "Samolepicí materiál, 4+0 CMYK, výsek, 50×50 mm, 500 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = adhesiveStockId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(dieCutId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(50, 50)),
          SpecValue.QuantitySpec(Quantity.unsafe(500)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-stickers-clear"),
        name = LocalizedString("Clear Vinyl", "Průhledný vinyl"),
        description = Some(LocalizedString(
          "Clear vinyl, 4+0 CMYK, UV inkjet, 50×50 mm, 250 pcs",
          "Průhledný vinyl, 4+0 CMYK, UV inkjet, 50×50 mm, 250 ks",
        )),
        printingMethodId = uvInkjetId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = clearVinylId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(50, 50)),
          SpecValue.QuantitySpec(Quantity.unsafe(250)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-stickers-solvent"),
        name = LocalizedString("Outdoor Vinyl (Solvent)", "Venkovní vinyl (solvent)"),
        description = Some(LocalizedString(
          "Adhesive vinyl, 4+0 CMYK, solvent inkjet, 100×100 mm, 250 pcs",
          "Samolepicí vinyl, 4+0 CMYK, solventový tisk, 100×100 mm, 250 ks",
        )),
        printingMethodId = solventInkjetId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = vinylId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(100, 100)),
          SpecValue.QuantitySpec(Quantity.unsafe(250)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-stickers-epson8color"),
        name = LocalizedString("Premium 8-Color (Epson)", "Prémiový 8-barevný (Epson)"),
        description = Some(LocalizedString(
          "Clear vinyl, 4+0 CMYK, Epson 8-color, 50×50 mm, 250 pcs",
          "Průhledný vinyl, 4+0 CMYK, Epson 8 barev, 50×50 mm, 250 ks",
        )),
        printingMethodId = epson8ColorId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = clearVinylId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(50, 50)),
          SpecValue.QuantitySpec(Quantity.unsafe(250)),
        ),
      ),
    ),
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
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-rollup-economy"),
        name = LocalizedString("Economy", "Economy"),
        description = Some(LocalizedString(
          "Banner film + economy stand, 4+0 CMYK, 850×2000 mm, 1 pc",
          "Bannerová fólie + economy stojan, 4+0 CMYK, 850×2000 mm, 1 ks",
        )),
        printingMethodId = uvInkjetId,
        componentPresets = List(
          ComponentPreset(
            role = ComponentRole.Main,
            materialId = rollUpBannerFilmId,
            inkConfiguration = InkConfiguration.cmyk4_0,
          ),
          ComponentPreset(
            role = ComponentRole.Stand,
            materialId = rollUpStandEconomyId,
            inkConfiguration = InkConfiguration.noInk,
          ),
        ),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(850, 2000)),
          SpecValue.QuantitySpec(Quantity.unsafe(1)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-rollup-premium"),
        name = LocalizedString("Premium", "Premium"),
        description = Some(LocalizedString(
          "Banner film + premium stand, 4+0 CMYK, 850×2000 mm, 1 pc",
          "Bannerová fólie + premium stojan, 4+0 CMYK, 850×2000 mm, 1 ks",
        )),
        printingMethodId = uvInkjetId,
        componentPresets = List(
          ComponentPreset(
            role = ComponentRole.Main,
            materialId = rollUpBannerFilmId,
            inkConfiguration = InkConfiguration.cmyk4_0,
          ),
          ComponentPreset(
            role = ComponentRole.Stand,
            materialId = rollUpStandPremiumId,
            inkConfiguration = InkConfiguration.noInk,
          ),
        ),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(850, 2000)),
          SpecValue.QuantitySpec(Quantity.unsafe(1)),
        ),
      ),
    ),
  )

  // --- Promotional Product Categories ---

  val tshirts: ProductCategory = ProductCategory(
    id = tshirtsId,
    name = LocalizedString("T-Shirts", "Trička"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(cottonTshirt150Id, cottonTshirt180Id, polyesterTshirtId, cottonPolyBlendId, organicCottonTshirtId),
      allowedFinishIds = Set(heatPressId, labelPrintId, foldBagId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(screenPrintId, dtgId, sublimationId),
    description = Some(LocalizedString(
      "Custom printed T-shirts in cotton, polyester, and blended fabrics. Available with screen printing, DTG, and sublimation. Dimensions specify the print area size.",
      "Trička s vlastním potiskem z bavlny, polyesteru a směsových materiálů. K dispozici v sítotisku, DTG a sublimaci. Rozměry určují velikost tiskové plochy.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-tshirt-standard"),
        name = LocalizedString("Standard Cotton", "Standardní bavlna"),
        description = Some(LocalizedString(
          "White cotton tee 180gsm, screen print, 50 pcs",
          "Bílé bavlněné tričko 180g, sítotisk, 50 ks",
        )),
        printingMethodId = screenPrintId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = cottonTshirt180Id,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(foldBagId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(300, 350)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-tshirt-premium-dtg"),
        name = LocalizedString("Premium DTG", "Prémiový DTG"),
        description = Some(LocalizedString(
          "Full-color photo print on organic cotton, 25 pcs",
          "Plnobarevný fototisk na bio bavlnu, 25 ks",
        )),
        printingMethodId = dtgId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = organicCottonTshirtId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(foldBagId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(300, 350)),
          SpecValue.QuantitySpec(Quantity.unsafe(25)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-tshirt-sublimation"),
        name = LocalizedString("Sublimation All-Over", "Sublimace celoplošná"),
        description = Some(LocalizedString(
          "All-over print on polyester, 100 pcs",
          "Celoplošný potisk na polyester, 100 ks",
        )),
        printingMethodId = sublimationId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = polyesterTshirtId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(foldBagId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(350, 400)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
        ),
      ),
    ),
  )

  val ecoBags: ProductCategory = ProductCategory(
    id = ecoBagsId,
    name = LocalizedString("Eco Bags", "Eko tašky"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(cottonCanvasBagId, organicCottonBagId, recycledPetBagId, juteBagId, nonWovenPpBagId),
      allowedFinishIds = Set(heatPressId, embroideryId, reinforcedHandlesId, foldBagId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(screenPrintId, dtgId),
    description = Some(LocalizedString(
      "Sustainable branded tote bags in cotton canvas, organic cotton, recycled PET, jute, and non-woven polypropylene. Dimensions specify the print area size.",
      "Ekologické reklamní tašky z bavlněného plátna, bio bavlny, recyklovaného PET, juty a netkané polypropylénové textilie. Rozměry určují velikost tiskové plochy.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-bag-canvas"),
        name = LocalizedString("Standard Canvas", "Standardní plátno"),
        description = Some(LocalizedString(
          "Natural cotton canvas, 1-color screen print, 100 pcs",
          "Přírodní bavlněné plátno, jednobarevný sítotisk, 100 ks",
        )),
        printingMethodId = screenPrintId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = cottonCanvasBagId,
          inkConfiguration = InkConfiguration.mono1_0,
          finishSelections = List(FinishSelection(reinforcedHandlesId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(250, 250)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-bag-organic"),
        name = LocalizedString("Organic Eco", "Bio eko"),
        description = Some(LocalizedString(
          "Organic cotton, full-color DTG print, 50 pcs",
          "Bio bavlna, plnobarevný DTG tisk, 50 ks",
        )),
        printingMethodId = dtgId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = organicCottonBagId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(250, 250)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
        ),
      ),
    ),
  )

  val pinBadges: ProductCategory = ProductCategory(
    id = pinBadgesId,
    name = LocalizedString("Pin Badges", "Odznaky"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(tinplateBadgeId, acrylicBadgeId, woodenBadgeId),
      allowedFinishIds = Set(mylarOverlayId, safetyPinId, magnetBackId, bottleOpenerId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(digitalId, offsetId),
    description = Some(LocalizedString(
      "Custom pin badges in tinplate, acrylic, or wood. Available with safety pin, magnet, or bottle opener backs. Dimensions specify badge diameter — only stock sizes (32mm, 58mm) are available.",
      "Vlastní odznaky z plechu, akrylátu nebo dřeva. K dispozici se špendlíkem, magnetem nebo otvírákem. Rozměry udávají průměr odznaku — pouze skladové velikosti (32mm, 58mm).",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-badge-standard"),
        name = LocalizedString("Standard Round 58mm", "Standardní kulatý 58mm"),
        description = Some(LocalizedString(
          "58mm tinplate, digital print, safety pin, 100 pcs",
          "58mm plech, digitální tisk, zavírací špendlík, 100 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = tinplateBadgeId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(safetyPinId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(58, 58)),
          SpecValue.QuantitySpec(Quantity.unsafe(100)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-badge-small"),
        name = LocalizedString("Small Round 32mm", "Malý kulatý 32mm"),
        description = Some(LocalizedString(
          "32mm tinplate, digital print, safety pin, 200 pcs",
          "32mm plech, digitální tisk, zavírací špendlík, 200 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = tinplateBadgeId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(safetyPinId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(32, 32)),
          SpecValue.QuantitySpec(Quantity.unsafe(200)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-badge-magnet"),
        name = LocalizedString("Magnet Badge", "Magnetický odznak"),
        description = Some(LocalizedString(
          "58mm tinplate, digital print, magnet back, 50 pcs",
          "58mm plech, digitální tisk, magnetické uchycení, 50 ks",
        )),
        printingMethodId = digitalId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = tinplateBadgeId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(magnetBackId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(58, 58)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
        ),
      ),
    ),
  )

  val cups: ProductCategory = ProductCategory(
    id = cupsId,
    name = LocalizedString("Cups & Mugs", "Hrnky a šálky"),
    components = List(ComponentTemplate(
      ComponentRole.Main,
      allowedMaterialIds = Set(ceramicMugWhiteId, ceramicMugColoredId, magicMugId, stainlessTravelMugId, enamelMugId, glassMugId),
      allowedFinishIds = Set(dishwasherCoatId, giftBoxId, glossyGlazeId),
    )),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity),
    allowedPrintingMethodIds = Set(sublimationId, screenPrintId, uvInkjetId),
    description = Some(LocalizedString(
      "Personalized mugs and cups in ceramic, stainless steel, enamel, and glass. Available with sublimation, screen print, and UV direct print. Dimensions specify the print area size.",
      "Personalizované hrnky a šálky z keramiky, nerezu, smaltu a skla. K dispozici se sublimací, sítotiskem a UV přímým tiskem. Rozměry určují velikost tiskové plochy.",
    )),
    presets = List(
      CategoryPreset(
        id = PresetId.unsafe("preset-mug-standard"),
        name = LocalizedString("Standard White Mug", "Standardní bílý hrnek"),
        description = Some(LocalizedString(
          "White ceramic 330ml, sublimation, 50 pcs",
          "Bílý keramický 330ml, sublimace, 50 ks",
        )),
        printingMethodId = sublimationId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = ceramicMugWhiteId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(190, 80)),
          SpecValue.QuantitySpec(Quantity.unsafe(50)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-mug-gift"),
        name = LocalizedString("Corporate Gift Set", "Firemní dárkový set"),
        description = Some(LocalizedString(
          "White ceramic 330ml, sublimation, gift box, 25 pcs",
          "Bílý keramický 330ml, sublimace, dárková krabička, 25 ks",
        )),
        printingMethodId = sublimationId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = ceramicMugWhiteId,
          inkConfiguration = InkConfiguration.cmyk4_0,
          finishSelections = List(FinishSelection(giftBoxId)),
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(190, 80)),
          SpecValue.QuantitySpec(Quantity.unsafe(25)),
        ),
      ),
      CategoryPreset(
        id = PresetId.unsafe("preset-mug-travel"),
        name = LocalizedString("Travel Mug", "Cestovní hrnek"),
        description = Some(LocalizedString(
          "Stainless 450ml, UV print, 20 pcs",
          "Nerezový 450ml, UV tisk, 20 ks",
        )),
        printingMethodId = uvInkjetId,
        componentPresets = List(ComponentPreset(
          role = ComponentRole.Main,
          materialId = stainlessTravelMugId,
          inkConfiguration = InkConfiguration.cmyk4_0,
        )),
        specOverrides = List(
          SpecValue.SizeSpec(Dimension(180, 60)),
          SpecValue.QuantitySpec(Quantity.unsafe(20)),
        ),
      ),
    ),
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

