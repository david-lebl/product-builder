package mpbuilder.domain.sample

import mpbuilder.domain.model.*
import mpbuilder.domain.rules.*

object SampleRules:

  private val cat = SampleCatalog

  val rules: List[CompatibilityRule] = List(
    // --- Property-level rules (replace material-specific UV coating rules) ---
    // Textured materials can't have UV coating (uneven application)
    CompatibilityRule.MaterialPropertyFinishTypeIncompatible(
      MaterialProperty.Textured,
      FinishType.UVCoating,
      "UV coating cannot be applied evenly to textured surfaces",
    ),
    // --- Family-level rules ---
    // Vinyl can't be embossed
    CompatibilityRule.MaterialFamilyFinishTypeIncompatible(
      MaterialFamily.Vinyl,
      FinishType.Embossing,
      "Vinyl material cannot be embossed",
    ),
    // Cardboard can't have soft-touch coating
    CompatibilityRule.MaterialFamilyFinishTypeIncompatible(
      MaterialFamily.Cardboard,
      FinishType.SoftTouchCoating,
      "Corrugated cardboard is not suitable for soft-touch coating",
    ),
    // --- Weight rules ---
    // Lamination requires at least 200gsm
    CompatibilityRule.MaterialWeightFinishType(
      FinishType.Lamination,
      200,
      "Lamination requires paper weight of at least 200gsm to prevent warping",
    ),
    // Embossing requires at least 250gsm
    CompatibilityRule.MaterialWeightFinishType(
      FinishType.Embossing,
      250,
      "Embossing requires paper weight of at least 250gsm for proper impression",
    ),
    // --- Finish type mutual exclusion ---
    // Lamination and soft-touch coating are mutually exclusive
    CompatibilityRule.FinishTypeMutuallyExclusive(
      FinishType.Lamination,
      FinishType.SoftTouchCoating,
      "Cannot apply both lamination and soft-touch coating",
    ),
    // UV coating and aqueous coating are mutually exclusive
    CompatibilityRule.FinishTypeMutuallyExclusive(
      FinishType.UVCoating,
      FinishType.AqueousCoating,
      "Cannot apply both UV coating and aqueous coating",
    ),
    // --- Surface category exclusive (only one surface coating) ---
    CompatibilityRule.FinishCategoryExclusive(
      FinishCategory.Surface,
      "Only one surface coating finish is allowed per product",
    ),
    // --- ID-level mutual exclusion (matte vs gloss lamination) ---
    CompatibilityRule.FinishMutuallyExclusive(
      cat.matteLaminationId,
      cat.glossLaminationId,
      "Cannot apply both matte and gloss lamination",
    ),
    // --- Finish dependency rules ---
    // Spot varnish requires lamination base
    CompatibilityRule.FinishRequiresFinishType(
      cat.varnishId,
      FinishType.Lamination,
      "Spot varnish requires a lamination base coat",
    ),
    // --- Process requirements ---
    // Aqueous coating only works with offset printing
    CompatibilityRule.FinishRequiresPrintingProcess(
      FinishType.AqueousCoating,
      Set(PrintingProcessType.Offset),
      "Aqueous coating is only compatible with offset printing",
    ),
    // --- Existing ID-level rules that are still unique ---
    // Foil stamping requires smooth surface
    CompatibilityRule.FinishRequiresMaterialProperty(
      cat.foilStampingId,
      MaterialProperty.SmoothSurface,
      "Foil stamping requires a smooth surface for proper adhesion",
    ),
    // Embossing requires smooth surface
    CompatibilityRule.FinishRequiresMaterialProperty(
      cat.embossingId,
      MaterialProperty.SmoothSurface,
      "Embossing works best on smooth surface materials",
    ),
    // --- Spec constraints ---
    // Business cards: min size 50x25mm
    CompatibilityRule.SpecConstraint(
      cat.businessCardsId,
      SpecPredicate.MinDimension(50, 25),
      "Business cards must be at least 50x25mm",
    ),
    // Business cards: max size 100x60mm
    CompatibilityRule.SpecConstraint(
      cat.businessCardsId,
      SpecPredicate.MaxDimension(100, 60),
      "Business cards must not exceed 100x60mm",
    ),
    // Business cards: min quantity 100
    CompatibilityRule.SpecConstraint(
      cat.businessCardsId,
      SpecPredicate.MinQuantity(100),
      "Business cards minimum order is 100 units",
    ),
    // Banners: min size 300x200mm
    CompatibilityRule.SpecConstraint(
      cat.bannersId,
      SpecPredicate.MinDimension(300, 200),
      "Banners must be at least 300x200mm",
    ),
    // Banners: only CMYK ink type (now a ConfigurationConstraint)
    CompatibilityRule.ConfigurationConstraint(
      cat.bannersId,
      ConfigurationPredicate.AllowedInkTypes(Set(InkType.CMYK)),
      "Banners only support CMYK ink type",
    ),
    // Booklets: allowed binding methods
    CompatibilityRule.SpecConstraint(
      cat.bookletsId,
      SpecPredicate.AllowedBindingMethods(Set(BindingMethod.SaddleStitch, BindingMethod.PerfectBinding)),
      "Booklets only support saddle stitch or perfect binding",
    ),
    // Booklets: min pages 8
    CompatibilityRule.SpecConstraint(
      cat.bookletsId,
      SpecPredicate.MinPages(8),
      "Booklets must have at least 8 pages",
    ),
    // Booklets: max pages 96
    CompatibilityRule.SpecConstraint(
      cat.bookletsId,
      SpecPredicate.MaxPages(96),
      "Booklets must not exceed 96 pages",
    ),
    // --- Calendar rules ---
    // Calendars: allowed binding methods
    CompatibilityRule.SpecConstraint(
      cat.calendarsId,
      SpecPredicate.AllowedBindingMethods(Set(BindingMethod.SpiralBinding, BindingMethod.WireOBinding)),
      "Calendars only support spiral or wire-o binding",
    ),
    // Calendars: min pages 12
    CompatibilityRule.SpecConstraint(
      cat.calendarsId,
      SpecPredicate.MinPages(12),
      "Calendars must have at least 12 pages (12 months)",
    ),
    // Calendars: max pages 28 (12 months + cover + extras)
    CompatibilityRule.SpecConstraint(
      cat.calendarsId,
      SpecPredicate.MaxPages(28),
      "Calendars must not exceed 28 pages",
    ),
    // --- Yupo (synthetic) rules ---
    // Yupo cannot be embossed or debossed (rigid plastic)
    CompatibilityRule.MaterialFinishIncompatible(
      cat.yupoId,
      cat.embossingId,
      "Yupo synthetic material cannot be embossed",
    ),
    CompatibilityRule.MaterialFinishIncompatible(
      cat.yupoId,
      cat.debossingId,
      "Yupo synthetic material cannot be debossed",
    ),
  )

  val ruleset: CompatibilityRuleset = CompatibilityRuleset(
    rules = rules,
    version = "3.0.0",
  )
