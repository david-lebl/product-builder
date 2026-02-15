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

  // --- Category IDs ---
  val businessCardsId: CategoryId   = CategoryId.unsafe("cat-business-cards")
  val flyersId: CategoryId          = CategoryId.unsafe("cat-flyers")
  val brochuresId: CategoryId       = CategoryId.unsafe("cat-brochures")
  val bannersId: CategoryId         = CategoryId.unsafe("cat-banners")
  val packagingId: CategoryId       = CategoryId.unsafe("cat-packaging")

  // --- Materials ---
  val coated300gsm: Material = Material(
    id = coated300gsmId,
    name = "Coated Art Paper 300gsm",
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(300)),
    properties = Set(MaterialProperty.Glossy, MaterialProperty.SmoothSurface, MaterialProperty.Recyclable),
  )

  val uncoatedBond: Material = Material(
    id = uncoatedBondId,
    name = "Uncoated Bond Paper 120gsm",
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(120)),
    properties = Set(MaterialProperty.Matte, MaterialProperty.Recyclable),
  )

  val kraft: Material = Material(
    id = kraftId,
    name = "Kraft Paper 250gsm",
    family = MaterialFamily.Paper,
    weight = Some(PaperWeight.unsafe(250)),
    properties = Set(MaterialProperty.Textured, MaterialProperty.Recyclable),
  )

  val vinyl: Material = Material(
    id = vinylId,
    name = "Adhesive Vinyl",
    family = MaterialFamily.Vinyl,
    weight = None,
    properties = Set(MaterialProperty.WaterResistant, MaterialProperty.Glossy, MaterialProperty.SmoothSurface),
  )

  val corrugated: Material = Material(
    id = corrugatedId,
    name = "Corrugated Cardboard",
    family = MaterialFamily.Cardboard,
    weight = None,
    properties = Set(MaterialProperty.Recyclable, MaterialProperty.Textured),
  )

  // --- Finishes ---
  val matteLamination: Finish = Finish(
    id = matteLaminationId,
    name = "Matte Lamination",
    finishType = FinishType.Lamination,
    side = FinishSide.Both,
  )

  val glossLamination: Finish = Finish(
    id = glossLaminationId,
    name = "Gloss Lamination",
    finishType = FinishType.Lamination,
    side = FinishSide.Both,
  )

  val uvCoating: Finish = Finish(
    id = uvCoatingId,
    name = "UV Coating",
    finishType = FinishType.UVCoating,
    side = FinishSide.Front,
  )

  val embossing: Finish = Finish(
    id = embossingId,
    name = "Embossing",
    finishType = FinishType.Embossing,
    side = FinishSide.Front,
  )

  val foilStamping: Finish = Finish(
    id = foilStampingId,
    name = "Foil Stamping",
    finishType = FinishType.FoilStamping,
    side = FinishSide.Front,
  )

  val dieCut: Finish = Finish(
    id = dieCutId,
    name = "Die Cut",
    finishType = FinishType.DieCut,
    side = FinishSide.Both,
  )

  val varnish: Finish = Finish(
    id = varnishId,
    name = "Spot Varnish",
    finishType = FinishType.Varnish,
    side = FinishSide.Front,
  )

  // --- Categories ---
  val businessCards: ProductCategory = ProductCategory(
    id = businessCardsId,
    name = "Business Cards",
    allowedMaterialIds = Set(coated300gsmId, uncoatedBondId, kraftId),
    allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId, embossingId, foilStampingId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.ColorMode),
  )

  val flyers: ProductCategory = ProductCategory(
    id = flyersId,
    name = "Flyers",
    allowedMaterialIds = Set(coated300gsmId, uncoatedBondId),
    allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId, varnishId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.ColorMode, SpecKind.Orientation),
  )

  val brochures: ProductCategory = ProductCategory(
    id = brochuresId,
    name = "Brochures",
    allowedMaterialIds = Set(coated300gsmId, uncoatedBondId),
    allowedFinishIds = Set(matteLaminationId, glossLaminationId, uvCoatingId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.ColorMode, SpecKind.FoldType, SpecKind.Pages),
  )

  val banners: ProductCategory = ProductCategory(
    id = bannersId,
    name = "Banners",
    allowedMaterialIds = Set(vinylId),
    allowedFinishIds = Set(uvCoatingId, dieCutId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.ColorMode),
  )

  val packaging: ProductCategory = ProductCategory(
    id = packagingId,
    name = "Packaging",
    allowedMaterialIds = Set(kraftId, corrugatedId),
    allowedFinishIds = Set(matteLaminationId, uvCoatingId, embossingId, foilStampingId, dieCutId),
    requiredSpecKinds = Set(SpecKind.Size, SpecKind.Quantity, SpecKind.ColorMode),
  )

  // --- Product Catalog ---
  val catalog: ProductCatalog = ProductCatalog(
    categories = Map(
      businessCardsId -> businessCards,
      flyersId        -> flyers,
      brochuresId     -> brochures,
      bannersId       -> banners,
      packagingId     -> packaging,
    ),
    materials = Map(
      coated300gsmId -> coated300gsm,
      uncoatedBondId -> uncoatedBond,
      kraftId        -> kraft,
      vinylId        -> vinyl,
      corrugatedId   -> corrugated,
    ),
    finishes = Map(
      matteLaminationId -> matteLamination,
      glossLaminationId -> glossLamination,
      uvCoatingId       -> uvCoating,
      embossingId       -> embossing,
      foilStampingId    -> foilStamping,
      dieCutId          -> dieCut,
      varnishId         -> varnish,
    ),
  )
