package mpbuilder.domain.sample

import mpbuilder.domain.model.*
import mpbuilder.domain.rules.*

object SampleRules:

  private val cat = SampleCatalog

  val rules: List[CompatibilityRule] = List(
    // UV coating incompatible with kraft paper (textured surface absorbs coating unevenly)
    CompatibilityRule.MaterialFinishIncompatible(
      cat.kraftId,
      cat.uvCoatingId,
      "UV coating cannot be applied to textured kraft paper",
    ),
    // UV coating incompatible with corrugated cardboard
    CompatibilityRule.MaterialFinishIncompatible(
      cat.corrugatedId,
      cat.uvCoatingId,
      "UV coating cannot be applied to corrugated cardboard",
    ),
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
    // Matte and gloss lamination are mutually exclusive
    CompatibilityRule.FinishMutuallyExclusive(
      cat.matteLaminationId,
      cat.glossLaminationId,
      "Cannot apply both matte and gloss lamination",
    ),
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
    // Banners: only CMYK color mode
    CompatibilityRule.SpecConstraint(
      cat.bannersId,
      SpecPredicate.AllowedColorModes(Set(ColorMode.CMYK)),
      "Banners only support CMYK color mode",
    ),
  )

  val ruleset: CompatibilityRuleset = CompatibilityRuleset(
    rules = rules,
    version = "1.0.0",
  )
