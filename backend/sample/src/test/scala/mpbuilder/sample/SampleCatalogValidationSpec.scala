package mpbuilder.sample

import mpbuilder.catalog.*
import mpbuilder.catalog.sample.{CatalogSampleLayer, SampleCatalog, SampleRules}
import mpbuilder.shared.*
import zio.*
import zio.test.*

object SampleCatalogValidationSpec extends ZIOSpecDefault:

  import SampleCatalog.*

  private def cfg(
      catId: CategoryId,
      comps: NonEmptyChunk[ComponentSelection],
      specs: ProductSpecs,
  ): ZIO[CatalogService, CatalogError, ConfigurationResult] =
    CatalogService.configure(ConfigureProductCmd(ConfigurationId.unsafe("cfg-test"), catId, comps, specs))

  private def singleComp(
      matId: MaterialId,
      pmId: PrintingMethodId,
      ink: InkConfiguration = InkConfiguration.cmyk4_0,
      finishes: List[FinishSelection] = Nil,
  ): NonEmptyChunk[ComponentSelection] =
    NonEmptyChunk.single(ComponentSelection(ComponentKey.Main, matId, Some(pmId), Some(ink), finishes))

  private def hasRuleViolation(err: CatalogError, ruleId: String): Boolean =
    err match
      case CatalogError.RuleViolation(id, _) => id.value == ruleId
      case CatalogError.ConfigurationValidationFailed(errs) =>
        errs.exists(hasRuleViolation(_, ruleId))
      case _ => false

  override def spec = suite("SampleCatalogValidationSpec")(
    test("all presets across all 15 categories configure cleanly with valid imposition and weight") {
      for
        catalog <- ZIO.serviceWithZIO[CatalogService](_.getCatalog)
        results <- ZIO.foreach(catalog.categories.values.toList.sortBy(_.id.value)) { cat =>
                     ZIO.foreach(cat.presets) { preset =>
                       val cmd = DomainBridges.presetToConfigureCmd(
                         ConfigurationId.unsafe(s"cfg-${preset.id.value}"),
                         cat.id,
                         preset,
                       )
                       CatalogService.configure(cmd).map(res => (cat.id, preset.id, res))
                     }
                   }
        flat = results.flatten
      yield assertTrue(
        catalog.materials.size == 50,
        catalog.printingMethods.size == 9,
        catalog.finishes.size == 29,
        catalog.categories.size == 15,
        catalog.showcases.size == 15,
        flat.size >= 20,
        flat.forall { case (_, _, r) => r.product.weight.exists(_.totalWeight.value > BigDecimal(0)) },
      )
    },

    test("material and finish compatibility rules block invalid combinations") {
      val bcSpecs = ProductSpecs(ProductDimensions.Flat2D(Dimension2D.unsafeMm(85, 55)), Quantity.unsafe(100))
      val flyerSpecs = ProductSpecs(
        ProductDimensions.Flat2D(Dimension2D.A5),
        Quantity.unsafe(250),
        orientation = Some(Orientation.Portrait),
      )
      for
        texturedUv <- cfg(businessCardsId, singleComp(kraftId, digitalId, finishes = List(FinishSelection(uvCoatingId))), bcSpecs).exit
        lightLam   <- cfg(flyersId, singleComp(coatedGlossy130gsmId, digitalId, finishes = List(FinishSelection(matteLaminationId))), flyerSpecs).exit
        heavyLam   <- cfg(flyersId, singleComp(coatedMatte250gsmId, digitalId, finishes = List(FinishSelection(matteLaminationId))), flyerSpecs)
        matteGloss <- cfg(
                        businessCardsId,
                        singleComp(
                          coated300gsmId,
                          digitalId,
                          finishes = List(FinishSelection(matteLaminationId), FinishSelection(glossLaminationId)),
                        ),
                        bcSpecs,
                      ).exit
        varnishNoLam <- cfg(businessCardsId, singleComp(coated300gsmId, digitalId, finishes = List(FinishSelection(varnishId))), bcSpecs).exit
        varnishWithLam <- cfg(
                            businessCardsId,
                            singleComp(
                              coated300gsmId,
                              digitalId,
                              finishes = List(FinishSelection(matteLaminationId), FinishSelection(varnishId)),
                            ),
                            bcSpecs,
                          )
        aqDigital <- cfg(flyersId, singleComp(coatedGlossy130gsmId, digitalId, finishes = List(FinishSelection(aqueousCoatingId))), flyerSpecs).exit
        aqOffset  <- cfg(flyersId, singleComp(coatedGlossy130gsmId, offsetId, finishes = List(FinishSelection(aqueousCoatingId))), flyerSpecs)
        foilKraft <- cfg(
                       businessCardsId,
                       singleComp(kraftId, digitalId, finishes = List(FinishSelection(foilStampingId, params = Some(FinishParams.FoilStamping(FoilColor.Gold))))),
                       bcSpecs,
                     ).exit
      yield assertTrue(
        texturedUv.isFailure && texturedUv.foldExit(c => c.failureOption.exists(hasRuleViolation(_, "rule-textured-no-uv")), _ => false),
        lightLam.isFailure && lightLam.foldExit(c => c.failureOption.exists(hasRuleViolation(_, "rule-lamination-min-weight")), _ => false),
        heavyLam.product.components.head.finishes.size == 1,
        matteGloss.isFailure && matteGloss.foldExit(c => c.failureOption.exists(hasRuleViolation(_, "rule-matte-gloss-lam-exclusive")), _ => false),
        varnishNoLam.isFailure && varnishNoLam.foldExit(c => c.failureOption.exists(hasRuleViolation(_, "rule-spot-varnish-requires-lamination")), _ => false),
        varnishWithLam.product.components.head.finishes.size == 2,
        aqDigital.isFailure && aqDigital.foldExit(c => c.failureOption.exists(hasRuleViolation(_, "rule-aqueous-requires-offset")), _ => false),
        aqOffset.product.components.head.finishes.size == 1,
        foilKraft.isFailure && foilKraft.foldExit(c => c.failureOption.exists(hasRuleViolation(_, "rule-foil-requires-smooth")), _ => false),
      )
    },

    test("brochure Folded2D unfolds to A4, nests 2-up on SRA3, and emits non-blocking scoring warning for >=170gsm") {
      val specs = ProductSpecs(
        dimensions = ProductDimensions.Folded2D(Dimension2D.DL, FoldType.Tri),
        quantity = Quantity.unsafe(200),
      )
      for
        resNoScore <- cfg(brochuresId, singleComp(coatedMatte200gsmId, digitalId, InkConfiguration.cmyk4_4), specs)
        resScored  <- cfg(
                        brochuresId,
                        singleComp(
                          coatedMatte200gsmId,
                          digitalId,
                          InkConfiguration.cmyk4_4,
                          finishes = List(FinishSelection(scoringId, params = Some(FinishParams.Scoring(2)))),
                        ),
                        specs,
                      )
        tooManyCreases <- cfg(
                            brochuresId,
                            singleComp(
                              coatedMatte200gsmId,
                              digitalId,
                              InkConfiguration.cmyk4_4,
                              finishes = List(FinishSelection(scoringId, params = Some(FinishParams.Scoring(6)))),
                            ),
                            specs,
                          ).exit
      yield
        val geom = resNoScore.product.components.head.geometry
        val imp  = geom.imposition.asInstanceOf[ImpositionResult.SheetImposition]
        assertTrue(
          geom.flatSize == Dimension2D.unsafeMm(297, 210),
          imp.piecesPerSheet == 2,
          imp.sheetsNeeded == 100,
          resNoScore.warnings.exists(_.ruleId.value == "rule-brochures-heavy-stock-scoring-advisory"),
          resScored.warnings.isEmpty,
          tooManyCreases.isFailure && tooManyCreases.foldExit(
            c => c.failureOption.exists(hasRuleViolation(_, "rule-brochures-max-creases")),
            _ => false,
          ),
        )
    },

    test("booklet saddle-stitch weight rule is scoped to Body component (300g Cover + 130g Body passes; 170g Body 80pp fails)") {
      val specs80 = ProductSpecs(
        dimensions = ProductDimensions.Flat2D(Dimension2D.A4),
        quantity = Quantity.unsafe(100),
        binding = Some(BindingSpec(BindingMethod.SaddleStitch, totalPages = 80)),
      )
      val okComps = NonEmptyChunk(
        ComponentSelection(ComponentKey.Cover, coated300gsmId, Some(digitalId), Some(InkConfiguration.cmyk4_4)),
        ComponentSelection(ComponentKey.Body, coatedGlossy130gsmId, Some(digitalId), Some(InkConfiguration.cmyk4_4)),
      )
      val heavyBodyComps = NonEmptyChunk(
        ComponentSelection(ComponentKey.Cover, coated300gsmId, Some(digitalId), Some(InkConfiguration.cmyk4_4)),
        ComponentSelection(ComponentKey.Body, coatedGlossy170gsmId, Some(digitalId), Some(InkConfiguration.cmyk4_4)),
      )
      for
        okRes   <- cfg(bookletsId, okComps, specs80)
        failRes <- cfg(bookletsId, heavyBodyComps, specs80).exit
      yield
        val bodyGeom = okRes.product.components.find(_.key == ComponentKey.Body).get.geometry
        assertTrue(
          // 80pp total - 4pp cover = 76 body pages / 4 per spread = 19 spreads per booklet
          bodyGeom.unitsPerProduct == 19,
          bodyGeom.totalFlatUnits == 1900,
          failRes.isFailure && failRes.foldExit(
            c => c.failureOption.exists(hasRuleViolation(_, "rule-booklets-saddle-heavy-body-170")),
            _ => false,
          ),
        )
    },

    test("large-format banners, roll-ups, and stickers enforce roll constraints, hardware rules, and white ink") {
      val bannerSpecs = ProductSpecs(ProductDimensions.Flat2D(Dimension2D.unsafeMm(2000, 1000)), Quantity.unsafe(2))
      val rollupSpecs = ProductSpecs(ProductDimensions.Flat2D(Dimension2D.unsafeMm(850, 2000)), Quantity.unsafe(1))
      val badRollupSpecs = ProductSpecs(ProductDimensions.Flat2D(Dimension2D.unsafeMm(900, 2000)), Quantity.unsafe(1))
      val stickerSpecs = ProductSpecs(ProductDimensions.Flat2D(Dimension2D.unsafeMm(100, 100)), Quantity.unsafe(100))

      for
        ropeNoGrommet <- cfg(
                           bannersId,
                           singleComp(pvc510gId, solventInkjetId, finishes = List(FinishSelection(gumRopeId, params = Some(FinishParams.LinearLength(BigDecimal(4)))))),
                           bannerSpecs,
                         ).exit
        bannerDuplex <- cfg(bannersId, singleComp(pvc510gId, solventInkjetId, InkConfiguration.cmyk4_4), bannerSpecs).exit
        rollupNoStand <- cfg(rollUpsId, singleComp(rollUpBannerFilmId, solventInkjetId), rollupSpecs)
        rollupStandWithPrint <- cfg(
                                  rollUpsId,
                                  NonEmptyChunk(
                                    ComponentSelection(ComponentKey.Main, rollUpBannerFilmId, Some(solventInkjetId), Some(InkConfiguration.cmyk4_0)),
                                    ComponentSelection(ComponentKey.Stand, rollUpStandEconomyId, Some(solventInkjetId), Some(InkConfiguration.cmyk4_0)),
                                  ),
                                  rollupSpecs,
                                ).exit
        rollupBadSize <- cfg(rollUpsId, singleComp(rollUpBannerFilmId, solventInkjetId), badRollupSpecs).exit
        vinylDigital  <- cfg(stickersId, singleComp(vinylId, digitalId), stickerSpecs).exit
        clearWhiteUv  <- cfg(stickersId, singleComp(clearVinylId, uvInkjetId, InkConfiguration.cmyk4W_0, List(FinishSelection(kissCutId))), stickerSpecs)
        kissAndDie    <- cfg(
                           stickersId,
                           singleComp(adhesiveStockId, digitalId, finishes = List(FinishSelection(kissCutId), FinishSelection(dieCutId))),
                           stickerSpecs,
                         ).exit
      yield assertTrue(
        ropeNoGrommet.isFailure && ropeNoGrommet.foldExit(c => c.failureOption.exists(hasRuleViolation(_, "rule-gum-rope-requires-grommets")), _ => false),
        bannerDuplex.isFailure,
        rollupNoStand.product.components.size == 1,
        rollupStandWithPrint.isFailure,
        rollupBadSize.isFailure && rollupBadSize.foldExit(c => c.failureOption.exists(hasRuleViolation(_, "rule-rollups-exact-sizes")), _ => false),
        vinylDigital.isFailure && vinylDigital.foldExit(c => c.failureOption.exists(hasRuleViolation(_, "rule-stickers-vinyl-requires-large-format")), _ => false),
        clearWhiteUv.product.components.head.print.exists(_.ink.notation == "4+W/0"),
        kissAndDie.isFailure && kissAndDie.foldExit(c => c.failureOption.exists(hasRuleViolation(_, "rule-kisscut-diecut-exclusive")), _ => false),
      )
    },

    test("promotional categories enforce fabric/sublimation, pin-badge back mutual exclusion, and min quantities") {
      val shirtSpecs = ProductSpecs(ProductDimensions.PrintArea(Dimension2D.unsafeMm(280, 350)), Quantity.unsafe(10))
      val badgeSpecs = ProductSpecs(ProductDimensions.Circular(PositiveMm.unsafe(58)), Quantity.unsafe(50))
      val cupSpecsLow = ProductSpecs(ProductDimensions.PrintArea(Dimension2D.unsafeMm(200, 85)), Quantity.unsafe(3))
      val cupSpecsOk  = ProductSpecs(ProductDimensions.PrintArea(Dimension2D.unsafeMm(200, 85)), Quantity.unsafe(12))

      for
        subOnCotton   <- cfg(tshirtsId, singleComp(cottonTshirt180Id, sublimationId), shirtSpecs).exit
        subOnPoly     <- cfg(tshirtsId, singleComp(polyesterTshirtId, sublimationId), shirtSpecs)
        screenLowQty  <- cfg(tshirtsId, singleComp(cottonTshirt180Id, screenPrintId), shirtSpecs).exit
        doubleBack    <- cfg(
                           pinBadgesId,
                           singleComp(tinplateBadgeId, digitalId, finishes = List(FinishSelection(safetyPinId), FinishSelection(magnetBackId))),
                           badgeSpecs,
                         ).exit
        cupsTooFew    <- cfg(cupsId, singleComp(ceramicMugWhiteId, sublimationId), cupSpecsLow).exit
        subStainless  <- cfg(cupsId, singleComp(stainlessTravelMugId, sublimationId), cupSpecsOk).exit
        uvStainless   <- cfg(cupsId, singleComp(stainlessTravelMugId, uvInkjetId, InkConfiguration.cmyk4W_0), cupSpecsOk)
      yield assertTrue(
        subOnCotton.isFailure && subOnCotton.foldExit(c => c.failureOption.exists(hasRuleViolation(_, "rule-tshirts-sublimation-polyester-only")), _ => false),
        subOnPoly.product.categoryId == tshirtsId,
        screenLowQty.isFailure && screenLowQty.foldExit(c => c.failureOption.exists(hasRuleViolation(_, "rule-tshirts-min-qty-screenprint")), _ => false),
        doubleBack.isFailure && doubleBack.foldExit(c => c.failureOption.exists(hasRuleViolation(_, "rule-pinbadges-single-back-mechanism")), _ => false),
        cupsTooFew.isFailure && cupsTooFew.foldExit(c => c.failureOption.exists(hasRuleViolation(_, "rule-cups-min-qty")), _ => false),
        subStainless.isFailure && subStainless.foldExit(c => c.failureOption.exists(hasRuleViolation(_, "rule-cups-sublimation-glazed-only")), _ => false),
        uvStainless.product.categoryId == cupsId,
      )
    },

    test("compatibleFinishes uses the exact same RuleEngine as full validation (zero drift)") {
      for
        kraftFins      <- CatalogService.compatibleFinishes(businessCardsId, ComponentKey.Main, kraftId, Some(digitalId))
        lightPaperFins <- CatalogService.compatibleFinishes(flyersId, ComponentKey.Main, coatedGlossy130gsmId, Some(digitalId))
        beforeLam      <- CatalogService.compatibleFinishes(businessCardsId, ComponentKey.Main, coated300gsmId, Some(digitalId))
        afterLam       <- CatalogService.compatibleFinishes(businessCardsId, ComponentKey.Main, coated300gsmId, Some(digitalId), Set(matteLaminationId))
      yield
        val kraftIds      = kraftFins.map(_.id).toSet
        val lightIds      = lightPaperFins.map(_.id).toSet
        val beforeLamIds  = beforeLam.map(_.id).toSet
        val afterLamIds   = afterLam.map(_.id).toSet
        assertTrue(
          !kraftIds.contains(uvCoatingId),
          !kraftIds.contains(foilStampingId),
          !lightIds.contains(matteLaminationId),
          !lightIds.contains(aqueousCoatingId),
          !beforeLamIds.contains(varnishId),
          afterLamIds.contains(varnishId),
          !afterLamIds.contains(glossLaminationId),
        )
    },
  ).provideLayer(CatalogSampleLayer.live())
