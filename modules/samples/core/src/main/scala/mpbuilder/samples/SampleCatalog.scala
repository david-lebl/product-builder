package mpbuilder.samples

import mpbuilder.catalog.*

object SampleCatalog:

  export SampleIds.*
  export SampleMaterials.*
  export SamplePrintingMethods.*
  export SampleFinishes.*
  export SampleCategories.*

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
      postcardsId     -> postcards,
      stickersId      -> stickers,
      rollUpsId       -> rollUps,
      freeId          -> free,
      // Promotional
      tshirtsId    -> tshirts,
      ecoBagsId    -> ecoBags,
      pinBadgesId  -> pinBadges,
      cupsId       -> cups,
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
      clearVinylId        -> clearVinyl,
      pvc510gId           -> pvc510g,
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
      // Roll-Up Materials
      rollUpBannerFilmId    -> rollUpBannerFilm,
      rollUpStandEconomyId  -> rollUpStandEconomy,
      rollUpStandPremiumId  -> rollUpStandPremium,
      // Promotional Materials
      cottonTshirt150Id      -> cottonTshirt150,
      cottonTshirt180Id      -> cottonTshirt180,
      polyesterTshirtId      -> polyesterTshirt,
      cottonPolyBlendId      -> cottonPolyBlend,
      organicCottonTshirtId  -> organicCottonTshirt,
      cottonCanvasBagId      -> cottonCanvasBag,
      organicCottonBagId     -> organicCottonBag,
      recycledPetBagId       -> recycledPetBag,
      juteBagId              -> juteBag,
      nonWovenPpBagId        -> nonWovenPpBag,
      tinplateBadgeId        -> tinplateBadge,
      acrylicBadgeId         -> acrylicBadge,
      woodenBadgeId          -> woodenBadge,
      ceramicMugWhiteId      -> ceramicMugWhite,
      ceramicMugColoredId    -> ceramicMugColored,
      magicMugId             -> magicMug,
      stainlessTravelMugId   -> stainlessTravelMug,
      enamelMugId            -> enamelMug,
      glassMugId             -> glassMug,
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
      kissCutId          -> kissCut,
      overlaminationId   -> overlamination,
      gumRopeId          -> gumRope,
      // Promotional Finishes
      heatPressId         -> heatPress,
      labelPrintId        -> labelPrint,
      foldBagId           -> foldBag,
      mylarOverlayId      -> mylarOverlay,
      safetyPinId         -> safetyPin,
      magnetBackId        -> magnetBack,
      bottleOpenerId      -> bottleOpener,
      dishwasherCoatId    -> dishwasherCoat,
      giftBoxId           -> giftBox,
      glossyGlazeId       -> glossyGlaze,
      embroideryId        -> embroideryFinish,
      reinforcedHandlesId -> reinforcedHandles,
    ),
    printingMethods = Map(
      offsetId      -> offsetMethod,
      digitalId     -> digitalMethod,
      uvInkjetId    -> uvInkjetMethod,
      letterpressId -> letterpressMethod,
      // Large-format / sticker printing
      solventInkjetId -> solventInkjetMethod,
      epson8ColorId   -> epson8ColorMethod,
      // Promotional
      screenPrintId  -> screenPrintMethod,
      dtgId          -> dtgMethod,
      sublimationId  -> sublimationMethod,
    ),
  )
