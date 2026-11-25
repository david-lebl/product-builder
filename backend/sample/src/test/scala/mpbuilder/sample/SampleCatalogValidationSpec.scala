package mpbuilder.sample

import mpbuilder.catalog.*
import mpbuilder.catalog.sample.{SampleCatalog, SampleRules}
import mpbuilder.shared.*
import kyo.*
import kyo.test.*
import ShowcaseTestEnv.*

class SampleCatalogValidationSpec extends Test[Any]:

  import SampleCatalog.*

  private def cfg(
      catId: CategoryId,
      comps: NonEmptyChunk[ComponentSelection],
      specs: ProductSpecs,
  ): ConfigurationResult < (Async & Abort[CatalogError] & Env[CatalogService]) =
    Env.use[CatalogService](_.configure(ConfigureProductCmd(ConfigurationId.unsafe("cfg-test"), catId, comps, specs)))

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

  "SampleCatalogValidationSpec" - {
    "all presets across all 15 categories configure cleanly with valid imposition and weight" in {
      for
        catalog <- Env.use[CatalogService](_.getCatalog)
        results <- Effects.traverse(catalog.categories.values.toList.sortBy(_.id.value)) { cat =>
                     Effects.traverse(cat.presets) { preset =>
                       val cmd = DomainBridges.presetToConfigureCmd(
                         ConfigurationId.unsafe(s"cfg-${preset.id.value}"),
                         cat.id,
                         preset,
                       )
                       Env.use[CatalogService](_.configure(cmd)).map(res => (cat.id, preset.id, res))
                     }
                   }
        flat = results.flatten
      yield {
        assert(catalog.materials.size == 50)
        assert(catalog.printingMethods.size == 9)
        assert(catalog.finishes.size == 29)
        assert(catalog.categories.size == 15)
        assert(catalog.showcases.size == 15)
        assert(flat.size >= 20)
        assert(flat.forall { case (_, _, r) => r.product.weight.exists(_.totalWeight.value > BigDecimal(0)) })
      }
    }.provideCatalog

    "material and finish compatibility rules block invalid combinations" in {
      val bcSpecs = ProductSpecs(ProductDimensions.Flat2D(Dimension2D.unsafeMm(85, 55)), Quantity.unsafe(100))
      val flyerSpecs = ProductSpecs(
        ProductDimensions.Flat2D(Dimension2D.A5),
        Quantity.unsafe(250),
        orientation = Some(Orientation.Portrait),
      )
      for
        texturedUv <- cfg(businessCardsId, singleComp(kraftId, digitalId, finishes = List(FinishSelection(uvCoatingId))), bcSpecs).result
        lightLam   <- cfg(flyersId, singleComp(coatedGlossy130gsmId, digitalId, finishes = List(FinishSelection(matteLaminationId))), flyerSpecs).result
        heavyLam   <- cfg(flyersId, singleComp(coatedMatte250gsmId, digitalId, finishes = List(FinishSelection(matteLaminationId))), flyerSpecs)
        matteGloss <- cfg(
                        businessCardsId,
                        singleComp(
                          coated300gsmId,
                          digitalId,
                          finishes = List(FinishSelection(matteLaminationId), FinishSelection(glossLaminationId)),
                        ),
                        bcSpecs,
                      ).result
        varnishNoLam <- cfg(businessCardsId, singleComp(coated300gsmId, digitalId, finishes = List(FinishSelection(varnishId))), bcSpecs).result
        varnishWithLam <- cfg(
                            businessCardsId,
                            singleComp(
                              coated300gsmId,
                              digitalId,
                              finishes = List(FinishSelection(matteLaminationId), FinishSelection(varnishId)),
                            ),
                            bcSpecs,
                          )
        aqDigital <- cfg(flyersId, singleComp(coatedGlossy130gsmId, digitalId, finishes = List(FinishSelection(aqueousCoatingId))), flyerSpecs).result
        aqOffset  <- cfg(flyersId, singleComp(coatedGlossy130gsmId, offsetId, finishes = List(FinishSelection(aqueousCoatingId))), flyerSpecs)
        foilKraft <- cfg(
                       businessCardsId,
                       singleComp(kraftId, digitalId, finishes = List(FinishSelection(foilStampingId, params = Some(FinishParams.FoilStamping(FoilColor.Gold))))),
                       bcSpecs,
                     ).result
      yield {
        assert(texturedUv.isFailure && texturedUv.failure.exists(hasRuleViolation(_, "rule-textured-no-uv")))
        assert(lightLam.isFailure && lightLam.failure.exists(hasRuleViolation(_, "rule-lamination-min-weight")))
        assert(heavyLam.product.components.head.finishes.size == 1)
        assert(matteGloss.isFailure && matteGloss.failure.exists(hasRuleViolation(_, "rule-matte-gloss-lam-exclusive")))
        assert(varnishNoLam.isFailure && varnishNoLam.failure.exists(hasRuleViolation(_, "rule-spot-varnish-requires-lamination")))
        assert(varnishWithLam.product.components.head.finishes.size == 2)
        assert(aqDigital.isFailure && aqDigital.failure.exists(hasRuleViolation(_, "rule-aqueous-requires-offset")))
        assert(aqOffset.product.components.head.finishes.size == 1)
        assert(foilKraft.isFailure && foilKraft.failure.exists(hasRuleViolation(_, "rule-foil-requires-smooth")))
      }
    }.provideCatalog

    "brochure Folded2D unfolds to A4, nests 2-up on SRA3, and emits non-blocking scoring warning for >=170gsm" in {
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
                          ).result
      yield
        val geom = resNoScore.product.components.head.geometry
        val imp  = geom.imposition.asInstanceOf[ImpositionResult.SheetImposition]
        assert(geom.flatSize == Dimension2D.unsafeMm(297, 210))
        assert(imp.piecesPerSheet == 2)
        assert(imp.sheetsNeeded == 100)
        assert(resNoScore.warnings.exists(_.ruleId.value == "rule-brochures-heavy-stock-scoring-advisory"))
        assert(resScored.warnings.isEmpty)
        assert(tooManyCreases.isFailure && tooManyCreases.failure.exists(hasRuleViolation(_, "rule-brochures-max-creases")))
    }.provideCatalog

    "booklet saddle-stitch weight rule is scoped to Body component (300g Cover + 130g Body passes; 170g Body 80pp fails)" in {
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
        failRes <- cfg(bookletsId, heavyBodyComps, specs80).result
      yield
        val bodyGeom = okRes.product.components.find(_.key == ComponentKey.Body).get.geometry
        // 80pp total - 4pp cover = 76 body pages / 4 per spread = 19 spreads per booklet
        assert(bodyGeom.unitsPerProduct == 19)
        assert(bodyGeom.totalFlatUnits == 1900)
        assert(failRes.isFailure && failRes.failure.exists(hasRuleViolation(_, "rule-booklets-saddle-heavy-body-170")))
    }.provideCatalog

    "large-format banners, roll-ups, and stickers enforce roll constraints, hardware rules, and white ink" in {
      val bannerSpecs = ProductSpecs(ProductDimensions.Flat2D(Dimension2D.unsafeMm(2000, 1000)), Quantity.unsafe(2))
      val rollupSpecs = ProductSpecs(ProductDimensions.Flat2D(Dimension2D.unsafeMm(850, 2000)), Quantity.unsafe(1))
      val badRollupSpecs = ProductSpecs(ProductDimensions.Flat2D(Dimension2D.unsafeMm(900, 2000)), Quantity.unsafe(1))
      val stickerSpecs = ProductSpecs(ProductDimensions.Flat2D(Dimension2D.unsafeMm(100, 100)), Quantity.unsafe(100))

      for
        ropeNoGrommet <- cfg(
                           bannersId,
                           singleComp(pvc510gId, solventInkjetId, finishes = List(FinishSelection(gumRopeId, params = Some(FinishParams.LinearLength(BigDecimal(4)))))),
                           bannerSpecs,
                         ).result
        bannerDuplex <- cfg(bannersId, singleComp(pvc510gId, solventInkjetId, InkConfiguration.cmyk4_4), bannerSpecs).result
        rollupNoStand <- cfg(rollUpsId, singleComp(rollUpBannerFilmId, solventInkjetId), rollupSpecs)
        rollupStandWithPrint <- cfg(
                                  rollUpsId,
                                  NonEmptyChunk(
                                    ComponentSelection(ComponentKey.Main, rollUpBannerFilmId, Some(solventInkjetId), Some(InkConfiguration.cmyk4_0)),
                                    ComponentSelection(ComponentKey.Stand, rollUpStandEconomyId, Some(solventInkjetId), Some(InkConfiguration.cmyk4_0)),
                                  ),
                                  rollupSpecs,
                                ).result
        rollupBadSize <- cfg(rollUpsId, singleComp(rollUpBannerFilmId, solventInkjetId), badRollupSpecs).result
        vinylDigital  <- cfg(stickersId, singleComp(vinylId, digitalId), stickerSpecs).result
        clearWhiteUv  <- cfg(stickersId, singleComp(clearVinylId, uvInkjetId, InkConfiguration.cmyk4W_0, List(FinishSelection(kissCutId))), stickerSpecs)
        kissAndDie    <- cfg(
                           stickersId,
                           singleComp(adhesiveStockId, digitalId, finishes = List(FinishSelection(kissCutId), FinishSelection(dieCutId))),
                           stickerSpecs,
                         ).result
      yield {
        assert(ropeNoGrommet.isFailure && ropeNoGrommet.failure.exists(hasRuleViolation(_, "rule-gum-rope-requires-grommets")))
        assert(bannerDuplex.isFailure)
        assert(rollupNoStand.product.components.size == 1)
        assert(rollupStandWithPrint.isFailure)
        assert(rollupBadSize.isFailure && rollupBadSize.failure.exists(hasRuleViolation(_, "rule-rollups-exact-sizes")))
        assert(vinylDigital.isFailure && vinylDigital.failure.exists(hasRuleViolation(_, "rule-stickers-vinyl-requires-large-format")))
        assert(clearWhiteUv.product.components.head.print.exists(_.ink.notation == "4+W/0"))
        assert(kissAndDie.isFailure && kissAndDie.failure.exists(hasRuleViolation(_, "rule-kisscut-diecut-exclusive")))
      }
    }.provideCatalog

    "promotional categories enforce fabric/sublimation, pin-badge back mutual exclusion, and min quantities" in {
      val shirtSpecs = ProductSpecs(ProductDimensions.PrintArea(Dimension2D.unsafeMm(280, 350)), Quantity.unsafe(10))
      val badgeSpecs = ProductSpecs(ProductDimensions.Circular(PositiveMm.unsafe(58)), Quantity.unsafe(50))
      val cupSpecsLow = ProductSpecs(ProductDimensions.PrintArea(Dimension2D.unsafeMm(200, 85)), Quantity.unsafe(3))
      val cupSpecsOk  = ProductSpecs(ProductDimensions.PrintArea(Dimension2D.unsafeMm(200, 85)), Quantity.unsafe(12))

      for
        subOnCotton   <- cfg(tshirtsId, singleComp(cottonTshirt180Id, sublimationId), shirtSpecs).result
        subOnPoly     <- cfg(tshirtsId, singleComp(polyesterTshirtId, sublimationId), shirtSpecs)
        screenLowQty  <- cfg(tshirtsId, singleComp(cottonTshirt180Id, screenPrintId), shirtSpecs).result
        doubleBack    <- cfg(
                           pinBadgesId,
                           singleComp(tinplateBadgeId, digitalId, finishes = List(FinishSelection(safetyPinId), FinishSelection(magnetBackId))),
                           badgeSpecs,
                         ).result
        cupsTooFew    <- cfg(cupsId, singleComp(ceramicMugWhiteId, sublimationId), cupSpecsLow).result
        subStainless  <- cfg(cupsId, singleComp(stainlessTravelMugId, sublimationId), cupSpecsOk).result
        uvStainless   <- cfg(cupsId, singleComp(stainlessTravelMugId, uvInkjetId, InkConfiguration.cmyk4W_0), cupSpecsOk)
      yield {
        assert(subOnCotton.isFailure && subOnCotton.failure.exists(hasRuleViolation(_, "rule-tshirts-sublimation-polyester-only")))
        assert(subOnPoly.product.categoryId == tshirtsId)
        assert(screenLowQty.isFailure && screenLowQty.failure.exists(hasRuleViolation(_, "rule-tshirts-min-qty-screenprint")))
        assert(doubleBack.isFailure && doubleBack.failure.exists(hasRuleViolation(_, "rule-pinbadges-single-back-mechanism")))
        assert(cupsTooFew.isFailure && cupsTooFew.failure.exists(hasRuleViolation(_, "rule-cups-min-qty")))
        assert(subStainless.isFailure && subStainless.failure.exists(hasRuleViolation(_, "rule-cups-sublimation-glazed-only")))
        assert(uvStainless.product.categoryId == cupsId)
      }
    }.provideCatalog

    "compatibleFinishes uses the exact same RuleEngine as full validation (zero drift)" in {
      for
        kraftFins      <- Env.use[CatalogService](_.compatibleFinishes(businessCardsId, ComponentKey.Main, kraftId, Some(digitalId)))
        lightPaperFins <- Env.use[CatalogService](_.compatibleFinishes(flyersId, ComponentKey.Main, coatedGlossy130gsmId, Some(digitalId)))
        beforeLam      <- Env.use[CatalogService](_.compatibleFinishes(businessCardsId, ComponentKey.Main, coated300gsmId, Some(digitalId)))
        afterLam       <- Env.use[CatalogService](_.compatibleFinishes(businessCardsId, ComponentKey.Main, coated300gsmId, Some(digitalId), Set(matteLaminationId)))
      yield
        val kraftIds      = kraftFins.map(_.id).toSet
        val lightIds      = lightPaperFins.map(_.id).toSet
        val beforeLamIds  = beforeLam.map(_.id).toSet
        val afterLamIds   = afterLam.map(_.id).toSet
        assert(!kraftIds.contains(uvCoatingId))
        assert(!kraftIds.contains(foilStampingId))
        assert(!lightIds.contains(matteLaminationId))
        assert(!lightIds.contains(aqueousCoatingId))
        assert(!beforeLamIds.contains(varnishId))
        assert(afterLamIds.contains(varnishId))
        assert(!afterLamIds.contains(glossLaminationId))
    }.provideCatalog
  }
