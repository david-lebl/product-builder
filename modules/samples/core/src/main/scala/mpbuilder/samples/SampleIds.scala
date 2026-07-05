package mpbuilder.samples

import mpbuilder.catalog.*
import mpbuilder.kernel.*


object SampleIds:

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
  val clearVinylId: MaterialId      = MaterialId.unsafe("mat-clear-vinyl")
  val pvc510gId: MaterialId         = MaterialId.unsafe("mat-pvc-510g")

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
  val kissCutId: FinishId           = FinishId.unsafe("fin-kiss-cut")
  val overlaminationId: FinishId    = FinishId.unsafe("fin-overlamination")
  val gumRopeId: FinishId           = FinishId.unsafe("fin-gum-rope")

  // --- Promotional Finish IDs ---
  val heatPressId: FinishId          = FinishId.unsafe("fin-heat-press")
  val labelPrintId: FinishId         = FinishId.unsafe("fin-label-print")
  val foldBagId: FinishId            = FinishId.unsafe("fin-fold-bag")
  val mylarOverlayId: FinishId       = FinishId.unsafe("fin-mylar-overlay")
  val safetyPinId: FinishId          = FinishId.unsafe("fin-safety-pin")
  val magnetBackId: FinishId         = FinishId.unsafe("fin-magnet-back")
  val bottleOpenerId: FinishId       = FinishId.unsafe("fin-bottle-opener")
  val dishwasherCoatId: FinishId     = FinishId.unsafe("fin-dishwasher-coat")
  val giftBoxId: FinishId            = FinishId.unsafe("fin-gift-box")
  val glossyGlazeId: FinishId        = FinishId.unsafe("fin-glossy-glaze")
  val embroideryId: FinishId         = FinishId.unsafe("fin-embroidery")
  val reinforcedHandlesId: FinishId  = FinishId.unsafe("fin-reinforced-handles")

  // --- Category IDs ---
  val businessCardsId: CategoryId   = CategoryId.unsafe("cat-business-cards")
  val flyersId: CategoryId          = CategoryId.unsafe("cat-flyers")
  val brochuresId: CategoryId       = CategoryId.unsafe("cat-brochures")
  val bannersId: CategoryId         = CategoryId.unsafe("cat-banners")
  val packagingId: CategoryId       = CategoryId.unsafe("cat-packaging")
  val bookletsId: CategoryId        = CategoryId.unsafe("cat-booklets")
  val calendarsId: CategoryId       = CategoryId.unsafe("cat-calendars")
  val postcardsId: CategoryId       = CategoryId.unsafe("cat-postcards")
  val stickersId: CategoryId        = CategoryId.unsafe("cat-stickers")
  val rollUpsId: CategoryId         = CategoryId.unsafe("cat-roll-ups")
  val freeId: CategoryId            = CategoryId.unsafe("cat-free")

  // --- Promotional Category IDs ---
  val tshirtsId: CategoryId          = CategoryId.unsafe("cat-tshirts")
  val ecoBagsId: CategoryId          = CategoryId.unsafe("cat-eco-bags")
  val pinBadgesId: CategoryId        = CategoryId.unsafe("cat-pin-badges")
  val cupsId: CategoryId             = CategoryId.unsafe("cat-cups")

  // --- Roll-Up Material IDs ---
  val rollUpBannerFilmId: MaterialId    = MaterialId.unsafe("mat-rollup-banner-film")
  val rollUpStandEconomyId: MaterialId  = MaterialId.unsafe("mat-rollup-stand-economy")
  val rollUpStandPremiumId: MaterialId  = MaterialId.unsafe("mat-rollup-stand-premium")

  // --- Promotional Material IDs ---
  // T-Shirts
  val cottonTshirt150Id: MaterialId       = MaterialId.unsafe("mat-cotton-tshirt-150")
  val cottonTshirt180Id: MaterialId       = MaterialId.unsafe("mat-cotton-tshirt-180")
  val polyesterTshirtId: MaterialId       = MaterialId.unsafe("mat-polyester-tshirt")
  val cottonPolyBlendId: MaterialId       = MaterialId.unsafe("mat-cotton-poly-blend")
  val organicCottonTshirtId: MaterialId   = MaterialId.unsafe("mat-organic-cotton-tshirt")
  // Eco Bags
  val cottonCanvasBagId: MaterialId       = MaterialId.unsafe("mat-cotton-canvas-bag")
  val organicCottonBagId: MaterialId      = MaterialId.unsafe("mat-organic-cotton-bag")
  val recycledPetBagId: MaterialId        = MaterialId.unsafe("mat-recycled-pet-bag")
  val juteBagId: MaterialId               = MaterialId.unsafe("mat-jute-bag")
  val nonWovenPpBagId: MaterialId         = MaterialId.unsafe("mat-non-woven-pp-bag")
  // Pin Badges
  val tinplateBadgeId: MaterialId         = MaterialId.unsafe("mat-tinplate-badge")
  val acrylicBadgeId: MaterialId          = MaterialId.unsafe("mat-acrylic-badge")
  val woodenBadgeId: MaterialId           = MaterialId.unsafe("mat-wooden-badge")
  // Cups & Mugs
  val ceramicMugWhiteId: MaterialId       = MaterialId.unsafe("mat-ceramic-mug-white")
  val ceramicMugColoredId: MaterialId     = MaterialId.unsafe("mat-ceramic-mug-colored")
  val magicMugId: MaterialId              = MaterialId.unsafe("mat-magic-mug")
  val stainlessTravelMugId: MaterialId    = MaterialId.unsafe("mat-stainless-travel-mug")
  val enamelMugId: MaterialId             = MaterialId.unsafe("mat-enamel-mug")
  val glassMugId: MaterialId              = MaterialId.unsafe("mat-glass-mug")

  // --- Printing Method IDs ---
  val offsetId: PrintingMethodId       = PrintingMethodId.unsafe("pm-offset")
  val digitalId: PrintingMethodId      = PrintingMethodId.unsafe("pm-digital")
  val uvInkjetId: PrintingMethodId     = PrintingMethodId.unsafe("pm-uv-inkjet")
  val letterpressId: PrintingMethodId  = PrintingMethodId.unsafe("pm-letterpress")

  // --- Large-Format Printing Method IDs ---
  val solventInkjetId: PrintingMethodId  = PrintingMethodId.unsafe("pm-solvent-inkjet")
  val epson8ColorId: PrintingMethodId    = PrintingMethodId.unsafe("pm-epson-8color")

  // --- Promotional Printing Method IDs ---
  val screenPrintId: PrintingMethodId    = PrintingMethodId.unsafe("pm-screen-print")
  val dtgId: PrintingMethodId            = PrintingMethodId.unsafe("pm-dtg")
  val sublimationId: PrintingMethodId    = PrintingMethodId.unsafe("pm-sublimation")

