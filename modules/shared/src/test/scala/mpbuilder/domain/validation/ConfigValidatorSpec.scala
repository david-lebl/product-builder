package mpbuilder.domain.validation

import mpbuilder.domain.*
import mpbuilder.domain.catalog.*
import mpbuilder.domain.config.*
import mpbuilder.domain.ids.*
import mpbuilder.domain.pricing.PricingEngine
import mpbuilder.domain.rules.OptionFilter
import mpbuilder.domain.sample.*
import mpbuilder.domain.sample.SampleIds.{category as cat, finish as fin, ink as inks, material as mat, method as met}
import zio.test.*

object ConfigValidatorSpec extends ZIOSpecDefault:

  private val catalog = SampleCatalog.catalog
  private val rules   = SampleRules.rules

  private def validate(config: ProductConfiguration) =
    ConfigValidator.validate(catalog, rules, config).toEither

  def spec = suite("ConfigValidator")(

    test("a configuration with three independent problems reports all three at once") {
      // kraft paper is not allowed for flyers + quantity missing + letterpress (2 colors) can't print 4/4
      val config = ProductConfiguration(
        categoryId = cat.flyers,
        printingMethodId = met.letterpress,
        inkConfigurationId = inks.fullColorBoth,
        components = List(ComponentConfiguration(ComponentRole.Main, mat.kraft250)),
        details = ProductDetails(size = Some(DimensionsMm(148, 210)), orientation = Some(Orientation.Portrait)),
      )
      val errors = validate(config).swap.toOption.map(_.toList).getOrElse(Nil)
      assertTrue(
        errors.contains(ValidationError.MaterialNotAllowedInCategory(ComponentRole.Main, mat.kraft250)),
        errors.contains(ValidationError.MissingDetail(RequiredDetail.Quantity)),
        errors.contains(ValidationError.TooManyInkColors(met.letterpress, 2, 4)),
        errors.contains(ValidationError.PrintingMethodNotAllowed(met.letterpress)), // flyers are digital-only
        errors.size == 4,
      )
    },

    test("matte and gloss lamination together violate one-lamination rule") {
      val config = ProductConfiguration(
        categoryId = cat.businessCards,
        printingMethodId = met.digital,
        inkConfigurationId = inks.fullColorBoth,
        components = List(
          ComponentConfiguration(
            ComponentRole.Main,
            mat.coatedMatte(350),
            List(SelectedFinish(fin.matteLamination), SelectedFinish(fin.glossLamination)),
          )
        ),
        details = ProductDetails(size = Some(DimensionsMm(85, 55)), quantity = Some(100)),
      )
      val errors = validate(config).swap.toOption.map(_.toList).getOrElse(Nil)
      assertTrue(
        errors.contains(ValidationError.OnePerFinishTypeExceeded(ComponentRole.Main, FinishType.Lamination))
      )
    },

    test("embossing on thin stock violates the minimum-weight rule") {
      val config = ProductConfiguration(
        categoryId = cat.businessCards,
        printingMethodId = met.digital,
        inkConfigurationId = inks.fullColorFront,
        components = List(
          ComponentConfiguration(ComponentRole.Main, mat.uncoatedBond120, List(SelectedFinish(fin.embossing)))
        ),
        details = ProductDetails(size = Some(DimensionsMm(85, 55)), quantity = Some(100)),
      )
      val errors = validate(config).swap.toOption.map(_.toList).getOrElse(Nil)
      assertTrue(
        errors.contains(ValidationError.FinishRequiresMinWeight(ComponentRole.Main, FinishType.Embossing, 250, 120))
      )
    },

    test("scoring without a crease count and 9 creases both fail") {
      def config(params: Option[FinishParams]) = ProductConfiguration(
        categoryId = cat.brochures,
        printingMethodId = met.digital,
        inkConfigurationId = inks.fullColorBoth,
        components = List(
          ComponentConfiguration(ComponentRole.Main, mat.coatedGlossy(150), List(SelectedFinish(fin.scoring, params)))
        ),
        details = ProductDetails(size = Some(DimensionsMm(210, 297)), quantity = Some(250), foldType = Some(FoldType.TriFold)),
      )
      val missing  = validate(config(None)).swap.toOption.map(_.toList).getOrElse(Nil)
      val tooMany  = validate(config(Some(FinishParams.Creases(9)))).swap.toOption.map(_.toList).getOrElse(Nil)
      assertTrue(
        missing.contains(ValidationError.MissingFinishParams(ComponentRole.Main, fin.scoring)),
        tooMany.contains(ValidationError.TooManyCreases(8, 9)),
      )
    },

    test("booklet without its Body component reports the missing component") {
      val config = ProductConfiguration(
        categoryId = cat.booklets,
        printingMethodId = met.digital,
        inkConfigurationId = inks.fullColorBoth,
        components = List(ComponentConfiguration(ComponentRole.Cover, mat.coatedGlossy(250))),
        details = ProductDetails(
          size = Some(DimensionsMm(210, 297)), quantity = Some(100),
          pages = Some(8), bindingMethod = Some(BindingMethod.SaddleStitch),
        ),
      )
      val errors = validate(config).swap.toOption.map(_.toList).getOrElse(Nil)
      assertTrue(errors.contains(ValidationError.MissingRequiredComponent(ComponentRole.Body)))
    },

    test("roll-up without the optional Stand is valid") {
      val config = ProductConfiguration(
        categoryId = cat.rollUps,
        printingMethodId = met.uvInkjet,
        inkConfigurationId = inks.fullColorFront,
        components = List(ComponentConfiguration(ComponentRole.Main, mat.polyesterBannerFilm)),
        details = ProductDetails(size = Some(DimensionsMm(850, 2000)), quantity = Some(1)),
      )
      assertTrue(validate(config).isRight)
    },

    test("contact validation accumulates all field errors") {
      val bad = CustomerContact(name = " ", email = "not-an-email", phone = "")
      val errors = ConfigValidator.validateContact(bad).toEither.swap.toOption.map(_.toList).getOrElse(Nil)
      assertTrue(
        errors.exists { case ValidationError.ContactInvalid("name", _) => true; case _ => false },
        errors.exists { case ValidationError.ContactInvalid("email", _) => true; case _ => false },
        errors.exists { case ValidationError.ContactInvalid("phone", _) => true; case _ => false },
        ConfigValidator.validateContact(CustomerContact("Jan Novák", "jan@example.com", "+420123456789")).toEither.isRight,
      )
    },

    test("all sample presets validate AND price successfully (catalog-data smoke test)") {
      val validationFailures = SamplePresets.presets.flatMap { p =>
        validate(p.configuration).swap.toOption.map(errs => p.id.raw -> errs.toList)
      }
      val pricingFailures = SamplePresets.presets.flatMap { p =>
        PricingEngine.price(catalog, SamplePricelistCzk.pricelist, p.configuration)
          .swap.toOption.map(errs => p.id.raw -> errs.toList)
      }
      assertTrue(validationFailures.isEmpty, pricingFailures.isEmpty)
    },
  )

object OptionFilterSpec extends ZIOSpecDefault:

  private val catalog = SampleCatalog.catalog
  private val rules   = SampleRules.rules

  def spec = suite("OptionFilter")(

    test("choosing matte lamination removes gloss lamination from further options") {
      val remaining = OptionFilter.finishesFor(
        catalog, rules, SampleIds.category.businessCards, ComponentRole.Main,
        Some(SampleIds.material.coatedMatte(350)),
        List(SelectedFinish(SampleIds.finish.matteLamination)),
      )
      val ids = remaining.map(_.id)
      assertTrue(
        !ids.contains(SampleIds.finish.glossLamination),
        !ids.contains(SampleIds.finish.matteLamination), // already chosen
        ids.contains(SampleIds.finish.roundCorners),
      )
    },

    test("thin material hides weight-restricted finishes") {
      val remaining = OptionFilter.finishesFor(
        catalog, rules, SampleIds.category.businessCards, ComponentRole.Main,
        Some(SampleIds.material.uncoatedBond120), Nil,
      )
      val thick = OptionFilter.finishesFor(
        catalog, rules, SampleIds.category.businessCards, ComponentRole.Main,
        Some(SampleIds.material.coatedMatte(350)), Nil,
      )
      assertTrue(
        !remaining.map(_.id).contains(SampleIds.finish.embossing),
        thick.map(_.id).contains(SampleIds.finish.embossing),
      )
    },

    test("letterpress limits ink configurations to 2 colors") {
      val inks = OptionFilter.inkConfigsFor(catalog, SampleIds.method.letterpress)
      assertTrue(inks.map(_.id.raw).sorted == List("1-0", "1-1"))
    },

    test("printing methods are restricted per category; Free Configuration allows all") {
      val flyers = OptionFilter.printingMethodsFor(catalog, SampleIds.category.flyers)
      val free   = OptionFilter.printingMethodsFor(catalog, SampleIds.category.freeConfig)
      assertTrue(
        flyers.map(_.id) == List(SampleIds.method.digital),
        free.size == catalog.printingMethods.size,
      )
    },

    test("materials for a Stand component are exactly the two stands") {
      val stands = OptionFilter.materialsFor(catalog, SampleIds.category.rollUps, ComponentRole.Stand)
      assertTrue(
        stands.map(_.id).toSet ==
          Set(SampleIds.material.rollUpStandEconomy, SampleIds.material.rollUpStandPremium)
      )
    },
  )
