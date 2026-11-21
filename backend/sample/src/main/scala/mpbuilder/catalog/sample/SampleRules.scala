package mpbuilder.catalog.sample

import mpbuilder.catalog.*
import mpbuilder.shared.*

/** Complete compatibility ruleset replicated and enhanced from legacy `SampleRules`.
  *
  * Improvements over legacy rules:
  *   - Per-component rules use `perComponentRule` so Cover and Body never cross-leak
  *   - Saddle-stitch grammage/page limits are scoped to `ComponentKey.Body` (so 300g Cover + 130g Body passes!)
  *   - Non-blocking advisory rules (`RuleSeverity.Warning`) warn on heavy folded paper without scoring
  *   - Same canonical `RuleEngine` drives both full validation and `compatibleFinishes` filtering
  */
object SampleRules:

  import ComponentPredicate.*
  import ConfigPredicate.*
  import SampleCatalog.*

  /** Helper ensuring a component-level implication holds for EVERY component independently. */
  private def perComponent(
      id: String,
      scope: RuleScope,
      whenComp: ComponentPredicate,
      mustHoldComp: ComponentPredicate,
      reason: LocalizedString,
      severity: RuleSeverity = RuleSeverity.Error,
  ): CompatibilityRule =
    CompatibilityRule(
      id = RuleId.unsafe(id),
      scope = scope,
      severity = severity,
      when = ExistsComponent(whenComp),
      mustHold = ForAllComponents(!whenComp || mustHoldComp),
      reason = reason,
    )

  private def specRule(
      id: String,
      scope: RuleScope,
      when: ConfigPredicate,
      mustHold: ConfigPredicate,
      reason: LocalizedString,
      severity: RuleSeverity = RuleSeverity.Error,
  ): CompatibilityRule =
    CompatibilityRule(
      id = RuleId.unsafe(id),
      scope = scope,
      severity = severity,
      when = when,
      mustHold = mustHold,
      reason = reason,
    )

  private val largeFormatMethods: ComponentPredicate =
    HasPrintingProcess(PrintingProcessType.SolventInkjet) ||
      HasPrintingProcess(PrintingProcessType.UVCurableInkjet) ||
      HasPrintingProcess(PrintingProcessType.LatexInkjet)

  val rules: List[CompatibilityRule] = List(

    // ── 1. Global Material ↔ Finish Compatibility ────────────────────────────

    perComponent(
      id = "rule-textured-no-uv",
      scope = RuleScope.Global,
      whenComp = HasMaterialProperty(MaterialProperty.Textured),
      mustHoldComp = !HasFinish(uvCoatingId),
      reason = LocalizedString(
        "UV coating cannot be applied to textured substrates",
        "UV lak nelze aplikovat na strukturované materiály",
      ),
    ),

    perComponent(
      id = "rule-vinyl-no-embossing",
      scope = RuleScope.Global,
      whenComp = MaterialFamilyIs(MaterialFamily.Vinyl),
      mustHoldComp = !HasFinish(embossingId) && !HasFinish(debossingId),
      reason = LocalizedString(
        "Vinyl substrates cannot be embossed or debossed",
        "Vinylové materiály nelze opatřit slepotiskem ani hloubkovou ražbou",
      ),
    ),

    perComponent(
      id = "rule-lamination-min-weight",
      scope = RuleScope.Global,
      whenComp = HasFinishType(FinishType.Lamination) && MaterialFamilyIs(MaterialFamily.Paper),
      mustHoldComp = MinGrammage(200),
      reason = LocalizedString(
        "Paper lamination requires at least 200gsm paper weight",
        "Laminace papíru vyžaduje gramáž minimálně 200 g/m²",
      ),
    ),

    perComponent(
      id = "rule-soft-touch-min-weight",
      scope = RuleScope.Global,
      whenComp = HasFinish(softTouchCoatingId) && MaterialFamilyIs(MaterialFamily.Paper),
      mustHoldComp = MinGrammage(250),
      reason = LocalizedString(
        "Soft-touch lamination requires at least 250gsm paper weight",
        "Soft-touch laminace vyžaduje gramáž minimálně 250 g/m²",
      ),
    ),

    perComponent(
      id = "rule-embossing-min-weight",
      scope = RuleScope.Global,
      whenComp = (HasFinish(embossingId) || HasFinish(debossingId)) && MaterialFamilyIs(MaterialFamily.Paper),
      mustHoldComp = MinGrammage(250),
      reason = LocalizedString(
        "Embossing and debossing require at least 250gsm stock",
        "Slepotisk a hloubková ražba vyžadují karton minimálně 250 g/m²",
      ),
    ),

    perComponent(
      id = "rule-foil-requires-smooth",
      scope = RuleScope.Global,
      whenComp = HasFinish(foilStampingId),
      mustHoldComp = HasMaterialProperty(MaterialProperty.SmoothSurface),
      reason = LocalizedString(
        "Hot foil stamping requires a smooth-surface substrate",
        "Ražba fólií vyžaduje hladký povrch materiálu",
      ),
    ),

    perComponent(
      id = "rule-embossing-requires-smooth-or-cotton",
      scope = RuleScope.Global,
      whenComp = HasFinish(embossingId),
      mustHoldComp = HasMaterialProperty(MaterialProperty.SmoothSurface) || MaterialIs(cottonId),
      reason = LocalizedString(
        "Embossing requires a smooth-surface or cotton substrate",
        "Slepotisk vyžaduje hladký nebo bavlněný papír",
      ),
    ),

    // ── 2. Global Finish ↔ Finish & Finish ↔ Method Rules ────────────────────

    perComponent(
      id = "rule-matte-gloss-lam-exclusive",
      scope = RuleScope.Global,
      whenComp = HasFinish(matteLaminationId),
      mustHoldComp = !HasFinish(glossLaminationId),
      reason = LocalizedString(
        "Matte lamination and gloss lamination are mutually exclusive",
        "Matná a lesklá laminace se vzájemně vylučují",
      ),
    ),

    perComponent(
      id = "rule-soft-touch-lam-exclusive",
      scope = RuleScope.Global,
      whenComp = HasFinish(softTouchCoatingId),
      mustHoldComp = !HasFinish(matteLaminationId) && !HasFinish(glossLaminationId),
      reason = LocalizedString(
        "Soft-touch lamination cannot be combined with matte or gloss lamination",
        "Soft-touch laminaci nelze kombinovat s matnou nebo lesklou laminací",
      ),
    ),

    perComponent(
      id = "rule-spot-varnish-requires-lamination",
      scope = RuleScope.Global,
      whenComp = HasFinish(varnishId),
      mustHoldComp = HasFinishType(FinishType.Lamination) || HasFinish(softTouchCoatingId),
      reason = LocalizedString(
        "Spot varnish requires lamination or soft-touch base coating",
        "Parciální lak vyžaduje podkladovou laminaci nebo soft-touch povrch",
      ),
    ),

    perComponent(
      id = "rule-aqueous-requires-offset",
      scope = RuleScope.Global,
      whenComp = HasFinish(aqueousCoatingId),
      mustHoldComp = HasPrintingProcess(PrintingProcessType.Offset),
      reason = LocalizedString(
        "Aqueous dispersion coating is only available with offset printing",
        "Disperzní lak je dostupný pouze pro ofsetový tisk",
      ),
    ),

    perComponent(
      id = "rule-gum-rope-requires-grommets",
      scope = RuleScope.Global,
      whenComp = HasFinish(gumRopeId),
      mustHoldComp = HasFinish(grommetsId),
      reason = LocalizedString(
        "Elastic bungee rope requires metal grommets",
        "Gumolano vyžaduje kovová oka",
      ),
    ),

    perComponent(
      id = "rule-kisscut-diecut-exclusive",
      scope = RuleScope.Global,
      whenComp = HasFinish(kissCutId),
      mustHoldComp = !HasFinish(dieCutId),
      reason = LocalizedString(
        "Kiss-cut and full die-cut are mutually exclusive",
        "Násek (Kiss-Cut) a plný výsek se vzájemně vylučují",
      ),
    ),

    perComponent(
      id = "rule-large-format-single-sided",
      scope = RuleScope.Global,
      whenComp = largeFormatMethods,
      mustHoldComp = IsSingleSided,
      reason = LocalizedString(
        "Large-format inkjet printing supports single-sided printing only",
        "Velkoformátový inkoustový tisk podporuje pouze jednostranný tisk",
      ),
    ),

    perComponent(
      id = "rule-white-ink-requires-transparent-or-dark",
      scope = RuleScope.Global,
      whenComp = HasSpecialInk(SpecialInk.White),
      mustHoldComp =
        HasMaterialProperty(MaterialProperty.Transparent) ||
          MaterialFamilyIs(MaterialFamily.Fabric) ||
          MaterialFamilyIs(MaterialFamily.Metal) ||
          MaterialFamilyIs(MaterialFamily.Vinyl) ||
          MaterialIs(kraftId),
      reason = LocalizedString(
        "White ink underbase is intended for transparent, metallic, vinyl, fabric, or kraft substrates",
        "Bílý podtisk je určen pro čiré, kovové, vinylové, textilní nebo kraftové materiály",
      ),
      severity = RuleSeverity.Warning,
    ),

    // ── 3. Business Cards Rules ──────────────────────────────────────────────

    specRule(
      id = "rule-bc-dimensions",
      scope = RuleScope.Category(businessCardsId),
      when = Always,
      mustHold = DimensionCheck(DimensionPredicate.MinSize(PositiveMm.unsafe(50), PositiveMm.unsafe(40))) &&
        DimensionCheck(DimensionPredicate.MaxSize(PositiveMm.unsafe(100), PositiveMm.unsafe(100))),
      reason = LocalizedString(
        "Business card dimensions must be between 50×40mm and 100×100mm",
        "Rozměry vizitek musí být mezi 50×40 mm a 100×100 mm",
      ),
    ),

    specRule(
      id = "rule-bc-quantity",
      scope = RuleScope.Category(businessCardsId),
      when = Always,
      mustHold = QuantityCheck(QuantityPredicate.MinQuantity(50)) &&
        QuantityCheck(QuantityPredicate.MaxQuantity(50000)),
      reason = LocalizedString(
        "Business card quantity must be between 50 and 50,000 pieces",
        "Množství vizitek musí být mezi 50 a 50 000 kusy",
      ),
    ),

    // ── 4. Flyers Rules ──────────────────────────────────────────────────────

    specRule(
      id = "rule-flyers-dimensions",
      scope = RuleScope.Category(flyersId),
      when = Always,
      mustHold = DimensionCheck(DimensionPredicate.MinSize(PositiveMm.unsafe(70), PositiveMm.unsafe(70))) &&
        DimensionCheck(DimensionPredicate.MaxSize(PositiveMm.unsafe(320), PositiveMm.unsafe(450))),
      reason = LocalizedString(
        "Flyer dimensions must be between 70×70mm and 320×450mm",
        "Rozměry letáků musí být mezi 70×70 mm a 320×450 mm",
      ),
    ),

    perComponent(
      id = "rule-flyers-max-creases",
      scope = RuleScope.Category(flyersId),
      whenComp = HasFinish(scoringId),
      mustHoldComp = FinishParamMax(FinishType.Scoring, BigDecimal(4)),
      reason = LocalizedString(
        "Flyers support at most 4 scoring creases",
        "Letáky podporují maximálně 4 rylovací linky",
      ),
    ),

    // ── 5. Brochures Rules ───────────────────────────────────────────────────

    specRule(
      id = "rule-brochures-dimensions",
      scope = RuleScope.Category(brochuresId),
      when = Always,
      mustHold = DimensionCheck(DimensionPredicate.MinSize(PositiveMm.unsafe(70), PositiveMm.unsafe(100))) &&
        DimensionCheck(DimensionPredicate.MaxSize(PositiveMm.unsafe(210), PositiveMm.unsafe(297))),
      reason = LocalizedString(
        "Brochure finished folded size must be between 70×100mm and 210×297mm",
        "Finální složený rozměr brožury musí být mezi 70×100 mm a 210×297 mm",
      ),
    ),

    specRule(
      id = "rule-brochures-allowed-folds",
      scope = RuleScope.Category(brochuresId),
      when = Always,
      mustHold = FoldCheck(
        FoldPredicate.AllowedFolds(Set(FoldType.Half, FoldType.Tri, FoldType.Gate, FoldType.Accordion, FoldType.ZFold, FoldType.RollFold))
      ),
      reason = LocalizedString(
        "Brochures support Half, Tri, Gate, Accordion, Z-Fold, or Roll-Fold",
        "Brožury podporují lomy: půlený, trojitý, okénkový, harmonika, Z-lom nebo zavinovací",
      ),
    ),

    perComponent(
      id = "rule-brochures-max-creases",
      scope = RuleScope.Category(brochuresId),
      whenComp = HasFinish(scoringId),
      mustHoldComp = FinishParamMax(FinishType.Scoring, BigDecimal(4)),
      reason = LocalizedString(
        "Brochures support at most 4 scoring creases",
        "Brožury podporují maximálně 4 rylovací linky",
      ),
    ),

    perComponent(
      id = "rule-brochures-heavy-stock-scoring-advisory",
      scope = RuleScope.Category(brochuresId),
      whenComp = MinGrammage(170) && !HasFinish(scoringId),
      mustHoldComp = HasFinish(scoringId),
      reason = LocalizedString(
        "Folded paper 170gsm or heavier is recommended to include scoring to prevent cracking",
        "U skládaného papíru od 170 g/m² doporučujeme bigování proti praskání barvy v lomu",
      ),
      severity = RuleSeverity.Warning,
    ),

    // ── 6. Banners Rules ─────────────────────────────────────────────────────

    specRule(
      id = "rule-banners-dimensions",
      scope = RuleScope.Category(bannersId),
      when = Always,
      mustHold = DimensionCheck(DimensionPredicate.MinSize(PositiveMm.unsafe(300), PositiveMm.unsafe(300))) &&
        DimensionCheck(DimensionPredicate.MaxSize(PositiveMm.unsafe(5000), PositiveMm.unsafe(1500))),
      reason = LocalizedString(
        "Banner dimensions must be between 300×300mm and 5000×1500mm",
        "Rozměry banneru musí být mezi 300×300 mm a 5000×1500 mm",
      ),
    ),

    // ── 7. Booklets Rules (Component-Scoped Body Grammage!) ──────────────────

    specRule(
      id = "rule-booklets-allowed-bindings",
      scope = RuleScope.Category(bookletsId),
      when = Always,
      mustHold = BindingCheck(
        BindingPredicate.AllowedMethods(
          Set(BindingMethod.SaddleStitch, BindingMethod.PerfectBinding, BindingMethod.SpiralBinding, BindingMethod.WireOBinding)
        )
      ),
      reason = LocalizedString(
        "Booklets support Saddle Stitch, Perfect Binding, Spiral, or Wire-O binding",
        "Katalogy podporují vazbu V1 (šitá), V2 (lepená), spirálovou nebo Wire-O",
      ),
    ),

    specRule(
      id = "rule-booklets-saddle-stitch-pages",
      scope = RuleScope.Category(bookletsId),
      when = BindingCheck(BindingPredicate.MethodIn(Set(BindingMethod.SaddleStitch))),
      mustHold = BindingCheck(BindingPredicate.MinPages(8)) &&
        BindingCheck(BindingPredicate.MaxPages(96)) &&
        BindingCheck(BindingPredicate.PagesDivisibleBy(4)),
      reason = LocalizedString(
        "Saddle-stitched booklets must have 8–96 pages divisible by 4",
        "Šité katalogy V1 musí mít 8–96 stran dělitelných 4",
      ),
    ),

    specRule(
      id = "rule-booklets-saddle-heavy-body-170",
      scope = RuleScope.Category(bookletsId),
      when = BindingCheck(BindingPredicate.MethodIn(Set(BindingMethod.SaddleStitch))) &&
        ForComponent(ComponentKey.Body, MinGrammage(170)),
      mustHold = BindingCheck(BindingPredicate.MaxPages(64)),
      reason = LocalizedString(
        "Saddle-stitched booklets with ≥170gsm interior pages are limited to 64 pages",
        "Šité katalogy V1 s vnitřními listy ≥170 g/m² mohou mít maximálně 64 stran",
      ),
    ),

    specRule(
      id = "rule-booklets-perfect-binding-min-pages",
      scope = RuleScope.Category(bookletsId),
      when = BindingCheck(BindingPredicate.MethodIn(Set(BindingMethod.PerfectBinding))),
      mustHold = BindingCheck(BindingPredicate.MinPages(32)) &&
        BindingCheck(BindingPredicate.PagesDivisibleBy(2)),
      reason = LocalizedString(
        "Perfect-bound catalogs require at least 32 pages (divisible by 2)",
        "Lepená vazba V2 vyžaduje alespoň 32 stran (sudý počet)",
      ),
    ),

    // ── 8. Calendars Rules ───────────────────────────────────────────────────

    specRule(
      id = "rule-calendars-binding",
      scope = RuleScope.Category(calendarsId),
      when = Always,
      mustHold = BindingCheck(BindingPredicate.AllowedMethods(Set(BindingMethod.SpiralBinding, BindingMethod.WireOBinding))) &&
        BindingCheck(BindingPredicate.MinPages(12)) &&
        BindingCheck(BindingPredicate.MaxPages(56)),
      reason = LocalizedString(
        "Calendars require Spiral or Wire-O binding with 12–56 pages",
        "Kalendáře vyžadují spirálovou nebo Wire-O vazbu a 12–56 stran",
      ),
    ),

    // ── 9. Stickers Rules (Vinyl ↔ Large-Format Coupling) ────────────────────

    specRule(
      id = "rule-stickers-dimensions",
      scope = RuleScope.Category(stickersId),
      when = Always,
      mustHold = DimensionCheck(DimensionPredicate.MinSize(PositiveMm.unsafe(20), PositiveMm.unsafe(20))) &&
        DimensionCheck(DimensionPredicate.MaxSize(PositiveMm.unsafe(1500), PositiveMm.unsafe(1500))),
      reason = LocalizedString(
        "Sticker dimensions must be between 20×20mm and 1500×1500mm",
        "Rozměry samolepek musí být mezi 20×20 mm a 1500×1500 mm",
      ),
    ),

    perComponent(
      id = "rule-stickers-vinyl-requires-large-format",
      scope = RuleScope.Category(stickersId),
      whenComp = MaterialFamilyIs(MaterialFamily.Vinyl),
      mustHoldComp = largeFormatMethods,
      reason = LocalizedString(
        "Vinyl and clear vinyl stickers require large-format inkjet printing",
        "Vinylové a čiré samolepky vyžadují velkoformátový inkoustový tisk",
      ),
    ),

    perComponent(
      id = "rule-stickers-paper-requires-digital",
      scope = RuleScope.Category(stickersId),
      whenComp = MaterialIs(adhesiveStockId),
      mustHoldComp = HasPrintingMethod(digitalId),
      reason = LocalizedString(
        "Paper adhesive stock stickers use sheet-fed digital printing",
        "Papírové samolepky se tisknou digitálním archovým tiskem",
      ),
    ),

    // ── 10. Roll-Ups Rules ───────────────────────────────────────────────────

    specRule(
      id = "rule-rollups-exact-sizes",
      scope = RuleScope.Category(rollUpsId),
      when = Always,
      mustHold = DimensionCheck(
        DimensionPredicate.AllowedExactSizes(
          Set(
            Dimension2D.unsafeMm(850, 2000),
            Dimension2D.unsafeMm(1000, 2000),
            Dimension2D.unsafeMm(1200, 2000),
            Dimension2D.unsafeMm(1200, 2400),
          )
        )
      ),
      reason = LocalizedString(
        "Roll-Up banners are available in standard sizes: 85×200, 100×200, 120×200, or 120×240 cm",
        "Roll-Up bannery jsou dostupné ve standardních rozměrech: 85×200, 100×200, 120×200 nebo 120×240 cm",
      ),
    ),

    // ── 11. Promotional Categories Rules ─────────────────────────────────────

    perComponent(
      id = "rule-tshirts-sublimation-polyester-only",
      scope = RuleScope.Category(tshirtsId),
      whenComp = HasPrintingProcess(PrintingProcessType.Sublimation),
      mustHoldComp = MaterialIs(polyesterTshirtId) || MaterialIs(cottonPolyBlendId),
      reason = LocalizedString(
        "Sublimation printing on apparel requires polyester or cotton-poly blend fabric",
        "Sublimační potisk textilu vyžaduje polyesterový nebo směsový materiál",
      ),
    ),

    specRule(
      id = "rule-tshirts-min-qty-screenprint",
      scope = RuleScope.Category(tshirtsId),
      when = ExistsComponent(HasPrintingProcess(PrintingProcessType.ScreenPrint)),
      mustHold = QuantityCheck(QuantityPredicate.MinQuantity(20)),
      reason = LocalizedString(
        "Screen printing on T-shirts requires a minimum order of 20 pieces",
        "Sítotisk na trička vyžaduje minimálně 20 kusů",
      ),
    ),

    perComponent(
      id = "rule-ecobags-sublimation-synthetic-only",
      scope = RuleScope.Category(ecoBagsId),
      whenComp = HasPrintingProcess(PrintingProcessType.Sublimation),
      mustHoldComp = MaterialIs(recycledPetBagId) || MaterialIs(nonWovenPpBagId),
      reason = LocalizedString(
        "Sublimation on tote bags requires synthetic rPET or polypropylene material",
        "Sublimace na tašky vyžaduje syntetický rPET nebo polypropylenový materiál",
      ),
    ),

    specRule(
      id = "rule-pinbadges-allowed-sizes",
      scope = RuleScope.Category(pinBadgesId),
      when = Always,
      mustHold = DimensionCheck(
        DimensionPredicate.AllowedExactSizes(
          Set(
            Dimension2D.unsafeMm(25, 25),
            Dimension2D.unsafeMm(37, 37),
            Dimension2D.unsafeMm(44, 44),
            Dimension2D.unsafeMm(58, 58),
            Dimension2D.unsafeMm(75, 75),
          )
        )
      ),
      reason = LocalizedString(
        "Pin badges are available in standard diameters: 25, 37, 44, 58, or 75 mm",
        "Placky jsou dostupné v průměrech: 25, 37, 44, 58 nebo 75 mm",
      ),
    ),

    perComponent(
      id = "rule-pinbadges-single-back-mechanism",
      scope = RuleScope.Category(pinBadgesId),
      whenComp = HasFinish(safetyPinId) || HasFinish(magnetBackId) || HasFinish(bottleOpenerId),
      mustHoldComp = FinishCategoryCountAtMost(FinishCategory.Assembly, 1),
      reason = LocalizedString(
        "A pin badge can have at most one back attachment mechanism (safety pin, magnet, or bottle opener)",
        "Placka může mít pouze jeden typ zadního uchycení (špendlík, magnet nebo otvírák)",
      ),
    ),

    perComponent(
      id = "rule-cups-sublimation-glazed-only",
      scope = RuleScope.Category(cupsId),
      whenComp = HasPrintingProcess(PrintingProcessType.Sublimation),
      mustHoldComp = SurfaceCoatingIs(SurfaceCoating.Glazed),
      reason = LocalizedString(
        "Sublimation on mugs requires a polymer-glazed ceramic or enamel surface",
        "Sublimace na hrnky vyžaduje glazovaný keramický nebo smaltovaný povrch",
      ),
    ),

    specRule(
      id = "rule-cups-min-qty",
      scope = RuleScope.Category(cupsId),
      when = Always,
      mustHold = QuantityCheck(QuantityPredicate.MinQuantity(6)),
      reason = LocalizedString(
        "Custom mugs require a minimum order of 6 pieces",
        "Hrnky s potiskem mají minimální odběr 6 kusů",
      ),
    ),
  )

  val ruleset: CompatibilityRuleset = CompatibilityRuleset(
    version = Version.unsafe("2026.1"),
    rules = rules,
  )
