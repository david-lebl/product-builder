package mpbuilder.domain.sample

import mpbuilder.domain.model.*

object SampleCatalog:

  // --- Material IDs ---
  val coated300gsmId: MaterialId    = MaterialId.unsafe("mat-coated-300gsm")
  val uncoatedBondId: MaterialId    = MaterialId.unsafe("mat-uncoated-bond")
  val kraftId: MaterialId           = MaterialId.unsafe("mat-kraft")
  val vinylId: MaterialId           = MaterialId.unsafe("mat-vinyl")
  val corrugatedId: MaterialId      = MaterialId.unsafe("mat-corrugated")

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

  // --- Category IDs ---
  val businessCardsId: CategoryId   = CategoryId.unsafe("cat-business-cards")
  val flyersId: CategoryId          = CategoryId.unsafe("cat-flyers")
  val brochuresId: CategoryId       = CategoryId.unsafe("cat-brochures")
  val bannersId: CategoryId         = CategoryId.unsafe("cat-banners")
  val packagingId: CategoryId       = CategoryId.unsafe("cat-packaging")
  val bookletsId: CategoryId        = CategoryId.unsafe("cat-booklets")

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
  )

  val uncoatedBond: Material = Material(
    id = uncoatedBondId,
    name = LocalizedString("Uncoated Bond Paper 120gsm", "Nenatíraný papír 120g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(120)),
    properties = Set(MaterialProperty.Matte, MaterialProperty.Recyclable),
  )

  val kraft: Material = Material(
    id = kraftId,
    name = LocalizedString("Kraft Paper 250gsm", "Kraftový papír 250g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(250)),
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable),
  )

  val vinyl: Material = Material(
    id = vinylId,
    name = LocalizedString("Adhesive Vinyl", "Samolepicí vinyl"),
    family = MaterialFamily.Vinyl,
    weight = None,
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.Glossy, MaterialProperty.SmoothSurface),
  )

  val corrugated: Material = Material(
    id = corrugatedId,
    name = LocalizedString("Corrugated Cardboard", "Vlnitá lepenka"),
    family = MaterialFamily.Cardboard,
    weight = None,
    properties = Set(MaterialProperty.Recyclable, MaterialProperty.Textured),
  )

  // --- Printing Methods ---
  val offsetMethod: PrintingMethod = PrintingMethod(
    id = offsetId,
    name = LocalizedString("Offset Printing", "Ofsetový tisk"),
    processType = PrintingProcessType.Offset,
    maxColorCount = Some(6),
  )

  val digitalMethod: PrintingMethod = PrintingMethod(
    id = digitalId,
    name = LocalizedString("Digital Printing", "Digitální tisk"),
    processType = PrintingProcessType.Digital,
    maxColorCount = None,
  )

  val uvInkjetMethod: PrintingMethod = PrintingMethod(
    id = uvInkjetId,
    name = LocalizedString("UV Curable Inkjet", "UV inkoustový tisk"),
    processType = PrintingProcessType.UVCurableInkjet,
    maxColorCount = None,
  )

  val letterpressMethod: PrintingMethod = PrintingMethod(
    id = letterpressId,
    name = LocalizedString("Letterpress", "Knihtisk"),
    processType = PrintingProcessType.Letterpress,
    maxColorCount = Some(2),
  )

  // --- Finishes ---
  val matteLamination: Finish = Finish(
    id = matteLaminationId,
    name = LocalizedString("Matte Lamination", "Matná laminace"),
    finishType = FinishType.Lamination,
    side = FinishSide.Both,
  )

  val glossLamination: Finish = Finish(
    id = glossLaminationId,
    name = LocalizedString("Gloss Lamination", "Lesklá laminace"),
    finishType = FinishType.Lamination,
    side = FinishSide.Both,
  )

  val uvCoating: Finish = Finish(
    id = uvCoatingId,
    name = LocalizedString("UV Coating", "UV lak"),
    finishType = FinishType.UVCoating,
    side = FinishSide.Front,
  )

  val embossing: Finish = Finish(
    id = embossingId,
    name = LocalizedString("Embossing", "Slepotisk"),
    finishType = FinishType.Embossing,
    side = FinishSide.Front,
  )

  val foilStamping: Finish = Finish(
    id = foilStampingId,
    name = LocalizedString("Foil Stamping", "Ražba fólií"),
    finishType = FinishType.FoilStamping,
    side = FinishSide.Front,
  )

  val dieCut: Finish = Finish(
    id = dieCutId,
    name = LocalizedString("Die Cut", "Výsek"),
    finishType = FinishType.DieCut,
    side = FinishSide.Both,
  )

  val varnish: Finish = Finish(
    id = varnishId,
    name = LocalizedString("Spot Varnish", "Parciální lak"),
    finishType = FinishType.Varnish,
    side = FinishSide.Front,
  )

  val softTouchCoating: Finish = Finish(
    id = softTouchCoatingId,
    name = LocalizedString("Soft Touch Coating", "Soft touch laminace"),
    finishType = FinishType.SoftTouchCoating,
    side = FinishSide.Both,
  )

  val aqueousCoating: Finish = Finish(
    id = aqueousCoatingId,
    name = LocalizedString("Aqueous Coating", "Disperzní lak"),
    finishType = FinishType.AqueousCoating,
    side = FinishSide.Both,
  )

  val debossing: Finish = Finish(
    id = debossingId,
    name = LocalizedString("Debossing", "Slepotisk do hloubky"),
    finishType = FinishType.Debossing,
    side = FinishSide.Front,
  )

  val scoring: Finish = Finish(
    id = scoringId,
    name = LocalizedString("Scoring", "Bigování"),
    finishType = FinishType.Scoring,
    side = FinishSide.Both,
  )

  val perforation: Finish = Finish(
    id = perforationId,
    name = LocalizedString("Perforation", "Perforace"),
    finishType = FinishType.Perforation,
    side = FinishSide.Both,
  )

  val roundCorners: Finish = Finish(
    id = roundCornersId,
    name = LocalizedString("Round Corners", "Zaoblené rohy"),
    finishType = FinishType.RoundCorners,
    side = FinishSide.Both,
  )

  val grommets: Finish = Finish(
    id = grommetsId,
    name = LocalizedString("Grommets", "Průchodky"),
    finishType = FinishType.Grommets,
    side = FinishSide.Both,
  )

  // --- Categories ---
  val businessCards: ProductCategory = ProductCategory(
    id = businessCardsId,
    name = LocalizedString("Business Cards", "Vizitky"),
    allowedMaterialIds = Set(coated300gsmId, uncoatedBondId, kraftId),
    allowedFinishIds = Set(
      matteLaminationId, glossLaminationId, uvCoatingId, embossingId,
      foilStampingId, softTouchCoatingId, roundCornersId,
    ),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.ColorMode),
    allowedPrintingMethodIds = Set(offsetId, digitalId, letterpressId),
  )

  val flyers: ProductCategory = ProductCategory(
    id = flyersId,
    name = LocalizedString("Flyers", "Letáky"),
    allowedMaterialIds = Set(coated300gsmId, uncoatedBondId),
    allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId, varnishId, aqueousCoatingId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.ColorMode, SpecKind.Orientation),
    allowedPrintingMethodIds = Set(offsetId, digitalId),
  )

  val brochures: ProductCategory = ProductCategory(
    id = brochuresId,
    name = LocalizedString("Brochures", "Brožury"),
    allowedMaterialIds = Set(coated300gsmId, uncoatedBondId),
    allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId, scoringId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.ColorMode, SpecKind.FoldType, SpecKind.Pages),
    allowedPrintingMethodIds = Set(offsetId, digitalId),
  )

  val banners: ProductCategory = ProductCategory(
    id = bannersId,
    name = LocalizedString("Banners", "Bannery"),
    allowedMaterialIds = Set(vinylId),
    allowedFinishIds = Set(uvCoatingId, dieCutId, grommetsId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.ColorMode),
    allowedPrintingMethodIds = Set(uvInkjetId),
  )

  val packaging: ProductCategory = ProductCategory(
    id = packagingId,
    name = LocalizedString("Packaging", "Obaly"),
    allowedMaterialIds = Set(kraftId, corrugatedId),
    allowedFinishIds = Set(matteLaminationId, uvCoatingId, embossingId, foilStampingId, dieCutId, scoringId, perforationId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.ColorMode),
    allowedPrintingMethodIds = Set(offsetId, digitalId),
  )

  val booklets: ProductCategory = ProductCategory(
    id = bookletsId,
    name = LocalizedString("Booklets", "Brožurky"),
    allowedMaterialIds = Set(coated300gsmId, uncoatedBondId),
    allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId, perforationId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.ColorMode, SpecKind.Pages, SpecKind.BindingMethod),
    allowedPrintingMethodIds = Set(offsetId, digitalId),
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
    ),
    materials = Map(
      coated300gsmId -> coated300gsm,
      uncoatedBondId -> uncoatedBond,
      kraftId        -> kraft,
      vinylId        -> vinyl,
      corrugatedId   -> corrugated,
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
    ),
    printingMethods = Map(
      offsetId      -> offsetMethod,
      digitalId     -> digitalMethod,
      uvInkjetId    -> uvInkjetMethod,
      letterpressId -> letterpressMethod,
    ),
  )
