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

  // --- Category IDs ---
  val businessCardsId: CategoryId   = CategoryId.unsafe("cat-business-cards")
  val flyersId: CategoryId          = CategoryId.unsafe("cat-flyers")
  val brochuresId: CategoryId       = CategoryId.unsafe("cat-brochures")
  val bannersId: CategoryId         = CategoryId.unsafe("cat-banners")
  val packagingId: CategoryId       = CategoryId.unsafe("cat-packaging")
  val bookletsId: CategoryId        = CategoryId.unsafe("cat-booklets")
  val calendarsId: CategoryId       = CategoryId.unsafe("cat-calendars")

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

  val coatedSilk250gsm: Material = Material(
    id = coatedSilk250gsmId,
    name = LocalizedString("Coated Silk 250gsm", "Křídový saténový papír 250g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(250)),
    properties = Set(MaterialProperty.Matte, MaterialProperty.SmoothSurface, MaterialProperty.Recyclable),
  )

  val yupo: Material = Material(
    id = yupoId,
    name = LocalizedString("Yupo Synthetic 200μm", "Syntetický papír Yupo 200μm"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(200)),
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.SmoothSurface),
  )

  val adhesiveStock: Material = Material(
    id = adhesiveStockId,
    name = LocalizedString("Adhesive Stock 100gsm", "Samolepicí materiál 100g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(100)),
    properties = Set(MaterialProperty.Glossy, MaterialProperty.SmoothSurface),
  )

  val cotton: Material = Material(
    id = cottonId,
    name = LocalizedString("Cotton Paper 300gsm", "Bavlněný papír 300g"),
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(300)),
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable),
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
    allowedMaterialIds = Set(coated300gsmId, uncoatedBondId, kraftId, yupoId, cottonId),
    allowedFinishIds = Set(
      matteLaminationId, glossLaminationId, uvCoatingId, embossingId,
      foilStampingId, softTouchCoatingId, roundCornersId,
    ),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.InkConfig),
    allowedPrintingMethodIds = Set(offsetId, digitalId, letterpressId),
  )

  val flyers: ProductCategory = ProductCategory(
    id = flyersId,
    name = LocalizedString("Flyers", "Letáky"),
    allowedMaterialIds = Set(
      coated300gsmId, uncoatedBondId,
      coatedGlossy90gsmId, coatedGlossy115gsmId, coatedGlossy130gsmId,
      coatedGlossy150gsmId, coatedGlossy170gsmId, coatedGlossy200gsmId,
      coatedGlossy250gsmId, coatedGlossy350gsmId,
      coatedMatte90gsmId, coatedMatte115gsmId, coatedMatte130gsmId,
      coatedMatte150gsmId, coatedMatte170gsmId, coatedMatte200gsmId,
      coatedMatte250gsmId, coatedMatte300gsmId, coatedMatte350gsmId,
    ),
    allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId, varnishId, aqueousCoatingId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.InkConfig, SpecKind.Orientation),
    allowedPrintingMethodIds = Set(offsetId, digitalId),
  )

  val brochures: ProductCategory = ProductCategory(
    id = brochuresId,
    name = LocalizedString("Brochures", "Brožury"),
    allowedMaterialIds = Set(coated300gsmId, uncoatedBondId),
    allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId, scoringId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.InkConfig, SpecKind.FoldType, SpecKind.Pages),
    allowedPrintingMethodIds = Set(offsetId, digitalId),
  )

  val banners: ProductCategory = ProductCategory(
    id = bannersId,
    name = LocalizedString("Banners", "Bannery"),
    allowedMaterialIds = Set(vinylId),
    allowedFinishIds = Set(uvCoatingId, dieCutId, grommetsId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.InkConfig),
    allowedPrintingMethodIds = Set(uvInkjetId),
  )

  val packaging: ProductCategory = ProductCategory(
    id = packagingId,
    name = LocalizedString("Packaging", "Obaly"),
    allowedMaterialIds = Set(kraftId, corrugatedId, yupoId),
    allowedFinishIds = Set(matteLaminationId, uvCoatingId, embossingId, foilStampingId, dieCutId, scoringId, perforationId, debossingId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.InkConfig),
    allowedPrintingMethodIds = Set(offsetId, digitalId),
  )

  val booklets: ProductCategory = ProductCategory(
    id = bookletsId,
    name = LocalizedString("Booklets", "Brožurky"),
    allowedMaterialIds = Set(coated300gsmId, uncoatedBondId),
    allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId, perforationId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.InkConfig, SpecKind.Pages, SpecKind.BindingMethod),
    allowedPrintingMethodIds = Set(offsetId, digitalId),
  )

  val calendars: ProductCategory = ProductCategory(
    id = calendarsId,
    name = LocalizedString("Calendars", "Kalendáře"),
    allowedMaterialIds = Set(coated300gsmId, coatedSilk250gsmId, uncoatedBondId),
    allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId, perforationId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.InkConfig, SpecKind.Pages, SpecKind.BindingMethod),
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
      calendarsId     -> calendars,
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
