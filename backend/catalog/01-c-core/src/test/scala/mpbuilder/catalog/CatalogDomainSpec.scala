package mpbuilder.catalog

import mpbuilder.catalog.core.*
import mpbuilder.shared.*
import zio.*
import zio.test.*

object CatalogDomainSpec extends ZIOSpecDefault:

  // ── Shared Catalog Fixtures ──────────────────────────────────────────────

  private val sra3Stock = MediaFormat.SheetStock(
    formatId = StockFormatId.unsafe("sra3"),
    sheetSize = Dimension2D.SRA3,
    defaultBleed = NonNegativeMm.unsafe(3),
    defaultGutter = NonNegativeMm.unsafe(2),
  )

  private val vinylRollStock = MediaFormat.RollStock(
    formatId = StockFormatId.unsafe("roll-1370"),
    rollWidth = PositiveMm.unsafe(1370),
    printableWidth = PositiveMm.unsafe(1350),
  )

  private val art300 = Material(
    id = MaterialId.unsafe("art-300"),
    name = LocalizedString("Coated Art Paper 300g", "Křídový papír 300g"),
    family = MaterialFamily.Paper,
    surfaceCoating = SurfaceCoating.CoatedSilk,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(300)),
    stockFormats = NonEmptyChunk(sra3Stock),
  )

  private val art115 = Material(
    id = MaterialId.unsafe("art-115"),
    name = LocalizedString("Coated Art Paper 115g"),
    family = MaterialFamily.Paper,
    surfaceCoating = SurfaceCoating.CoatedSilk,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(115)),
    stockFormats = NonEmptyChunk(sra3Stock),
  )

  private val clearVinyl = Material(
    id = MaterialId.unsafe("clear-vinyl"),
    name = LocalizedString("Transparent Vinyl Film"),
    family = MaterialFamily.Adhesive,
    surfaceCoating = SurfaceCoating.TransparentFilm,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(140)),
    stockFormats = NonEmptyChunk(vinylRollStock),
    properties = Set(MaterialProperty.Transparent, MaterialProperty.SelfAdhesive),
  )

  private val rollupFilm = Material(
    id = MaterialId.unsafe("rollup-film"),
    name = LocalizedString("Roll-Up PP Film 220g"),
    family = MaterialFamily.Synthetic,
    surfaceCoating = SurfaceCoating.CoatedMatte,
    weight = MaterialWeight.Grammage(PaperWeight.unsafe(220)),
    stockFormats = NonEmptyChunk(vinylRollStock),
  )

  private val aluStand = Material(
    id = MaterialId.unsafe("alu-stand-85"),
    name = LocalizedString("Aluminum Roll-Up Stand 85cm"),
    family = MaterialFamily.Hardware,
    surfaceCoating = SurfaceCoating.Metallic,
    weight = MaterialWeight.UnitWeight(Grams.unsafe(BigDecimal(2000))),
    stockFormats = NonEmptyChunk(MediaFormat.DiscreteBlank()),
  )

  private val digitalMethod = PrintingMethod(
    id = PrintingMethodId.unsafe("digital"),
    name = LocalizedString("Digital Press"),
    processType = PrintingProcessType.Digital,
    maxColorsPerSide = Some(4),
    supportsDoubleSided = true,
    supportedSpecialInks = Set.empty,
  )

  private val uvInkjetMethod = PrintingMethod(
    id = PrintingMethodId.unsafe("uv-inkjet"),
    name = LocalizedString("UV Flatbed/Roll Inkjet"),
    processType = PrintingProcessType.UVCurableInkjet,
    maxColorsPerSide = Some(5),
    supportsDoubleSided = false,
    supportedSpecialInks = Set(SpecialInk.White, SpecialInk.ClearSpotVarnish),
  )

  private val matteLam = Finish(
    id = FinishId.unsafe("matte-lam"),
    name = LocalizedString("Matte Lamination"),
    finishType = FinishType.Lamination,
    sidePolicy = FinishSidePolicy.Selectable(Set(FinishSide.Front, FinishSide.Both), FinishSide.Front),
  )

  private val glossLam = Finish(
    id = FinishId.unsafe("gloss-lam"),
    name = LocalizedString("Gloss Lamination"),
    finishType = FinishType.Lamination,
    sidePolicy = FinishSidePolicy.Selectable(Set(FinishSide.Front, FinishSide.Both), FinishSide.Front),
  )

  private val scoring = Finish(
    id = FinishId.unsafe("scoring"),
    name = LocalizedString("Scoring / Creasing"),
    finishType = FinishType.Scoring,
    sidePolicy = FinishSidePolicy.NotApplicable,
    paramSchema = FinishParamSchema.CreaseCountSchema(minCreases = 1, maxCreases = 6),
  )

  private val roundCorners = Finish(
    id = FinishId.unsafe("round-corners"),
    name = LocalizedString("Round Corners"),
    finishType = FinishType.RoundCorners,
    sidePolicy = FinishSidePolicy.NotApplicable,
    paramSchema = FinishParamSchema.RoundCornersSchema(Set(2, 4), minRadiusMm = 3, maxRadiusMm = 10),
  )

  private val bookletCatId  = CategoryId.unsafe("booklets")
  private val brochureCatId = CategoryId.unsafe("brochures")
  private val stickerCatId  = CategoryId.unsafe("stickers")
  private val rollupCatId   = CategoryId.unsafe("rollups")

  private val catalog = ProductCatalog(
    version = Version.unsafe("2026.1"),
    categories = Map(
      bookletCatId -> ProductCategory(
        id = bookletCatId,
        name = LocalizedString("Booklets"),
        components = NonEmptyChunk(
          ComponentTemplate(
            key = ComponentKey.Cover,
            label = LocalizedString("Cover"),
            allowedMaterialIds = Set(art300.id, art115.id),
            allowedFinishIds = Set(matteLam.id, glossLam.id),
          ),
          ComponentTemplate(
            key = ComponentKey.Body,
            label = LocalizedString("Body"),
            allowedMaterialIds = Set(art115.id, art300.id),
            allowedFinishIds = Set.empty,
          ),
        ),
        specRequirements = SpecRequirements(requiresBinding = true),
        allowedPrintingMethodIds = Set(digitalMethod.id),
      ),
      brochureCatId -> ProductCategory(
        id = brochureCatId,
        name = LocalizedString("Folded Brochures"),
        components = NonEmptyChunk(
          ComponentTemplate(
            key = ComponentKey.Main,
            label = LocalizedString("Sheet"),
            allowedMaterialIds = Set(art300.id, art115.id),
            allowedFinishIds = Set(matteLam.id, glossLam.id, scoring.id, roundCorners.id),
          ),
        ),
        specRequirements = SpecRequirements(requiresFoldedDimensions = true),
        allowedPrintingMethodIds = Set(digitalMethod.id),
      ),
      stickerCatId -> ProductCategory(
        id = stickerCatId,
        name = LocalizedString("Stickers"),
        components = NonEmptyChunk(
          ComponentTemplate(
            key = ComponentKey.Main,
            label = LocalizedString("Sticker Film"),
            allowedMaterialIds = Set(clearVinyl.id),
            allowedFinishIds = Set(matteLam.id),
          ),
        ),
        allowedPrintingMethodIds = Set(digitalMethod.id, uvInkjetMethod.id),
      ),
      rollupCatId -> ProductCategory(
        id = rollupCatId,
        name = LocalizedString("Roll-Up Banner"),
        components = NonEmptyChunk(
          ComponentTemplate(
            key = ComponentKey.Main,
            label = LocalizedString("Printed Banner"),
            required = true,
            printable = true,
            allowedMaterialIds = Set(rollupFilm.id),
            allowedFinishIds = Set(matteLam.id),
            allowedPrintingMethodIds = Some(Set(uvInkjetMethod.id)),
          ),
          ComponentTemplate(
            key = ComponentKey.Stand,
            label = LocalizedString("Aluminum Stand"),
            required = true,
            printable = false,
            allowedMaterialIds = Set(aluStand.id),
            allowedFinishIds = Set.empty,
          ),
        ),
        allowedPrintingMethodIds = Set(uvInkjetMethod.id),
      ),
    ),
    materials = Map(
      art300.id     -> art300,
      art115.id     -> art115,
      clearVinyl.id -> clearVinyl,
      rollupFilm.id -> rollupFilm,
      aluStand.id   -> aluStand,
    ),
    finishes = Map(
      matteLam.id     -> matteLam,
      glossLam.id     -> glossLam,
      scoring.id      -> scoring,
      roundCorners.id -> roundCorners,
    ),
    printingMethods = Map(
      digitalMethod.id  -> digitalMethod,
      uvInkjetMethod.id -> uvInkjetMethod,
    ),
  )

  private val ruleset = CompatibilityRuleset(
    version = Version.unsafe("2026.1"),
    rules = List(
      // Component-scoped rule: Saddle stitch Body pages must be <= 170g (must NOT falsely trigger on 300g Cover!)
      CompatibilityRule(
        id = RuleId.unsafe("saddle-body-max-gsm"),
        scope = RuleScope.Category(bookletCatId),
        severity = RuleSeverity.Error,
        when = ConfigPredicate.BindingCheck(BindingPredicate.MethodIn(Set(BindingMethod.SaddleStitch))),
        mustHold = ConfigPredicate.ForComponent(ComponentKey.Body, ComponentPredicate.MaxGrammage(170)),
        reason = LocalizedString("Saddle-stitched booklet body paper must be at most 170 gsm"),
      ),
      // Saddle stitch page count divisible by 4 and max 64pp
      CompatibilityRule(
        id = RuleId.unsafe("saddle-pages"),
        scope = RuleScope.Category(bookletCatId),
        severity = RuleSeverity.Error,
        when = ConfigPredicate.BindingCheck(BindingPredicate.MethodIn(Set(BindingMethod.SaddleStitch))),
        mustHold = ConfigPredicate.BindingCheck(BindingPredicate.PagesDivisibleBy(4)) &&
          ConfigPredicate.BindingCheck(BindingPredicate.MaxPages(64)),
        reason = LocalizedString("Saddle stitch requires page count divisible by 4, up to 64 pages"),
      ),
      // Advisory warning (non-blocking!): folding paper >= 170g without scoring
      CompatibilityRule(
        id = RuleId.unsafe("heavy-fold-recommend-scoring"),
        scope = RuleScope.Category(brochureCatId),
        severity = RuleSeverity.Warning,
        when = ConfigPredicate.ForComponent(ComponentKey.Main, ComponentPredicate.MinGrammage(170)),
        mustHold = ConfigPredicate.ForComponent(ComponentKey.Main, ComponentPredicate.HasFinishType(FinishType.Scoring)),
        reason = LocalizedString("Heavy paper (>=170g) should be scored before folding to prevent cracking"),
      ),
      // Mutual exclusion: at most 1 Surface finish (e.g. Matte Lam OR Gloss Lam, not both)
      CompatibilityRule(
        id = RuleId.unsafe("max-one-surface-finish"),
        scope = RuleScope.Global,
        severity = RuleSeverity.Error,
        when = ConfigPredicate.Always,
        mustHold = ConfigPredicate.ForAllComponents(
          ComponentPredicate.FinishCategoryCountAtMost(FinishCategory.Surface, 1)
        ),
        reason = LocalizedString("At most one surface finish (lamination/varnish) can be applied per component"),
      ),
    ),
  )

  private val testRepoLayer: ULayer[CatalogRepository] =
    ZLayer.succeed(new CatalogRepository:
      override def loadCatalog: UIO[ProductCatalog]          = ZIO.succeed(catalog)
      override def loadRuleset: UIO[CompatibilityRuleset]    = ZIO.succeed(ruleset)
    )

  private val serviceLayer: ULayer[CatalogService] =
    testRepoLayer >>> CatalogServiceLive.layer

  def spec = suite("CatalogDomainSpec")(
    test("InkConfiguration cleanly distinguishes 4+W/0 from 4/1 and validates special inks & duplex") {
      assertTrue(
        InkConfiguration.cmyk4W_0.notation == "4+W/0",
        InkConfiguration.cmyk4W_0.isSingleSided,
        InkConfiguration.cmyk4W_0.allSpecialInks == Set(SpecialInk.White),
        InkConfiguration.cmyk4_1.notation == "4/1",
        InkConfiguration.cmyk4_1.isDoubleSided,
        InkConfiguration.cmyk4_1.allSpecialInks.isEmpty,
      ) && {
        val uvCmd = ConfigureProductCmd(
          configurationId = ConfigurationId.unsafe("cfg-uv-white"),
          categoryId = stickerCatId,
          components = NonEmptyChunk(
            ComponentSelection(
              key = ComponentKey.Main,
              materialId = clearVinyl.id,
              printingMethodId = Some(uvInkjetMethod.id),
              inkConfiguration = Some(InkConfiguration.cmyk4W_0),
            )
          ),
          specs = ProductSpecs(
            dimensions = ProductDimensions.Flat2D(Dimension2D.unsafeMm(100, 100)),
            quantity = Quantity.unsafe(50),
          ),
        )
        val digitalWhiteCmd = uvCmd.copy(
          configurationId = ConfigurationId.unsafe("cfg-dig-white"),
          components = NonEmptyChunk(
            ComponentSelection(
              key = ComponentKey.Main,
              materialId = clearVinyl.id,
              printingMethodId = Some(digitalMethod.id),
              inkConfiguration = Some(InkConfiguration.cmyk4W_0),
            )
          ),
        )
        val uvDuplexCmd = uvCmd.copy(
          configurationId = ConfigurationId.unsafe("cfg-uv-duplex"),
          components = NonEmptyChunk(
            ComponentSelection(
              key = ComponentKey.Main,
              materialId = clearVinyl.id,
              printingMethodId = Some(uvInkjetMethod.id),
              inkConfiguration = Some(InkConfiguration.cmyk4_4),
            )
          ),
        )
        val okRes     = ConfigurationEngine.configure(uvCmd, catalog, ruleset)
        val badWhite  = ConfigurationEngine.configure(digitalWhiteCmd, catalog, ruleset)
        val badDuplex = ConfigurationEngine.configure(uvDuplexCmd, catalog, ruleset)
        assertTrue(
          okRes.isSuccess,
          badWhite.isFailure,
          badDuplex.isFailure,
        )
      }
    },
    test("multi-component booklet (300g Cover + 115g Body, 64pp saddle stitch) does NOT falsely reject Cover weight") {
      val cmd = ConfigureProductCmd(
        configurationId = ConfigurationId.unsafe("cfg-booklet"),
        categoryId = bookletCatId,
        components = NonEmptyChunk(
          ComponentSelection(
            key = ComponentKey.Cover,
            materialId = art300.id,
            printingMethodId = Some(digitalMethod.id),
            inkConfiguration = Some(InkConfiguration.cmyk4_4),
            finishes = List(FinishSelection(matteLam.id, requestedSide = Some(FinishSide.Front))),
          ),
          ComponentSelection(
            key = ComponentKey.Body,
            materialId = art115.id,
            printingMethodId = Some(digitalMethod.id),
            inkConfiguration = Some(InkConfiguration.cmyk4_4),
          ),
        ),
        specs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.A5),
          quantity = Quantity.unsafe(100),
          binding = Some(BindingSpec(BindingMethod.SaddleStitch, totalPages = 64, selfCover = false)),
        ),
      )
      for res <- CatalogService.configure(cmd)
      yield
        val cover = res.product.components.find(_.key == ComponentKey.Cover).get
        val body  = res.product.components.find(_.key == ComponentKey.Body).get
        assertTrue(
          res.warnings.isEmpty,
          // A5 saddle-stitch spread unfolds to 296x210 mm
          cover.geometry.flatSize == Dimension2D.unsafeMm(296, 210),
          // Separate cover = 1 spread per booklet, body = (64 - 4)/4 = 15 spreads per booklet
          cover.geometry.unitsPerProduct == 1,
          body.geometry.unitsPerProduct == 15,
          res.product.weight.isDefined,
          res.product.weight.get.weightPerProduct.value > BigDecimal(0),
        )
    },
    test("tri-fold DL brochure unfolds to 297x210 for sheet imposition and emits Warning if 300g is unscored") {
      val unscoredCmd = ConfigureProductCmd(
        configurationId = ConfigurationId.unsafe("cfg-trifold-warn"),
        categoryId = brochureCatId,
        components = NonEmptyChunk(
          ComponentSelection(
            key = ComponentKey.Main,
            materialId = art300.id,
            printingMethodId = Some(digitalMethod.id),
            inkConfiguration = Some(InkConfiguration.cmyk4_4),
          )
        ),
        specs = ProductSpecs(
          dimensions = ProductDimensions.Folded2D(Dimension2D.DL, FoldType.Tri),
          quantity = Quantity.unsafe(200),
        ),
      )
      val scoredCmd = unscoredCmd.copy(
        configurationId = ConfigurationId.unsafe("cfg-trifold-ok"),
        components = NonEmptyChunk(
          ComponentSelection(
            key = ComponentKey.Main,
            materialId = art300.id,
            printingMethodId = Some(digitalMethod.id),
            inkConfiguration = Some(InkConfiguration.cmyk4_4),
            finishes = List(FinishSelection(scoring.id, params = Some(FinishParams.Scoring(2)))),
          )
        ),
      )
      for
        unscoredRes <- CatalogService.configure(unscoredCmd)
        scoredRes   <- CatalogService.configure(scoredCmd)
      yield
        val flat = unscoredRes.product.components.head.geometry.flatSize
        val imp  = unscoredRes.product.components.head.geometry.imposition
        assertTrue(
          flat == Dimension2D.unsafeMm(297, 210), // 3 × 99mm × 210mm
          imp.isInstanceOf[ImpositionResult.SheetImposition],
          unscoredRes.warnings.map(_.ruleId) == List(RuleId.unsafe("heavy-fold-recommend-scoring")),
          scoredRes.warnings.isEmpty,
        )
    },
    test("roll-up banner combines RollStock printed Main + DiscreteBlank unprinted Stand with accurate weight") {
      val validCmd = ConfigureProductCmd(
        configurationId = ConfigurationId.unsafe("cfg-rollup"),
        categoryId = rollupCatId,
        components = NonEmptyChunk(
          ComponentSelection(
            key = ComponentKey.Main,
            materialId = rollupFilm.id,
            printingMethodId = Some(uvInkjetMethod.id),
            inkConfiguration = Some(InkConfiguration.cmyk4_0),
          ),
          ComponentSelection(
            key = ComponentKey.Stand,
            materialId = aluStand.id,
            printingMethodId = None,
            inkConfiguration = None,
          ),
        ),
        specs = ProductSpecs(
          dimensions = ProductDimensions.Flat2D(Dimension2D.unsafeMm(850, 2000)),
          quantity = Quantity.unsafe(2),
        ),
      )
      val invalidPrintOnStand = validCmd.copy(
        components = NonEmptyChunk(
          validCmd.components.head,
          ComponentSelection(
            key = ComponentKey.Stand,
            materialId = aluStand.id,
            printingMethodId = Some(uvInkjetMethod.id),
            inkConfiguration = Some(InkConfiguration.cmyk4_0),
          ),
        )
      )
      for
        ok   <- CatalogService.configure(validCmd)
        fail <- CatalogService.configure(invalidPrintOnStand).either
      yield
        val mainComp  = ok.product.components.find(_.key == ComponentKey.Main).get
        val standComp = ok.product.components.find(_.key == ComponentKey.Stand).get
        // Banner: 0.85 * 2.0 * 220 = 374g; Stand: 2000g -> total per product = 2374g
        val perUnitG = ok.product.weight.get.weightPerProduct.value
        assertTrue(
          mainComp.print.isDefined,
          standComp.print.isEmpty,
          mainComp.geometry.imposition.isInstanceOf[ImpositionResult.RollImposition],
          standComp.geometry.imposition == ImpositionResult.DiscreteUnit(2),
          perUnitG == BigDecimal("2374.00"),
          fail == Left(CatalogError.UnexpectedPrintSpec(ComponentKey.Stand)),
        )
    },
    test("canonical RuleEngine guarantees parity between compatibleFinishes and full configure") {
      for
        withNone  <- CatalogService.compatibleFinishes(brochureCatId, ComponentKey.Main, art300.id, Some(digitalMethod.id))
        withMatte <- CatalogService.compatibleFinishes(
                       brochureCatId,
                       ComponentKey.Main,
                       art300.id,
                       Some(digitalMethod.id),
                       alreadySelectedFinishIds = Set(matteLam.id),
                     )
        bothLamRes <- CatalogService.configure(
                        ConfigureProductCmd(
                          configurationId = ConfigurationId.unsafe("cfg-double-lam"),
                          categoryId = brochureCatId,
                          components = NonEmptyChunk(
                            ComponentSelection(
                              key = ComponentKey.Main,
                              materialId = art300.id,
                              printingMethodId = Some(digitalMethod.id),
                              inkConfiguration = Some(InkConfiguration.cmyk4_4),
                              finishes = List(
                                FinishSelection(matteLam.id),
                                FinishSelection(glossLam.id),
                              ),
                            )
                          ),
                          specs = ProductSpecs(
                            dimensions = ProductDimensions.Folded2D(Dimension2D.DL, FoldType.Tri),
                            quantity = Quantity.unsafe(100),
                          ),
                        )
                      ).either
      yield assertTrue(
        withNone.map(_.id).toSet.contains(glossLam.id),
        !withMatte.map(_.id).toSet.contains(glossLam.id),
        withMatte.map(_.id).toSet.contains(scoring.id),
        bothLamRes.isLeft,
      )
    },
    test("validates FinishSidePolicy and FinishParamSchema with error accumulation") {
      val badCmd = ConfigureProductCmd(
        configurationId = ConfigurationId.unsafe("cfg-bad-params"),
        categoryId = brochureCatId,
        components = NonEmptyChunk(
          ComponentSelection(
            key = ComponentKey.Main,
            materialId = art300.id,
            printingMethodId = Some(digitalMethod.id),
            inkConfiguration = Some(InkConfiguration.cmyk4_4),
            finishes = List(
              // Invalid side (Back not allowed on matteLam) AND invalid corner count (3 not in {2,4}) + radius out of bounds
              FinishSelection(matteLam.id, requestedSide = Some(FinishSide.Back)),
              FinishSelection(roundCorners.id, params = Some(FinishParams.RoundCorners(cornerCount = 3, radiusMm = 20))),
            ),
          )
        ),
        specs = ProductSpecs(
          dimensions = ProductDimensions.Folded2D(Dimension2D.DL, FoldType.Tri),
          quantity = Quantity.unsafe(100),
        ),
      )
      for res <- CatalogService.configure(badCmd).either
      yield assertTrue(
        res match
          case Left(CatalogError.ConfigurationValidationFailed(errs)) => errs.size >= 3
          case _                                                      => false
      )
    },
  ).provideLayerShared(serviceLayer)
