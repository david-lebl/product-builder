package mpbuilder.catalog

import mpbuilder.commons.*
import mpbuilder.catalog.impl.legacy.LegacyCatalogService
import mpbuilder.domain.sample.SampleCatalog
import zio.*
import zio.test.*

/** Contract tests for [[CatalogService]].
  *
  * Deliberately written against the *public* surface only — no domain types appear below. That is
  * the point of the façade: when the catalog model is extracted in Phase 7 and
  * `LegacyCatalogService` is deleted, this file should pass unchanged against its replacement.
  */
object CatalogServiceSpec extends ZIOSpecDefault:

  private val service: CatalogService = LegacyCatalogService.of(SampleCatalog.catalog, mpbuilder.domain.sample.SampleRules.ruleset)

  private val businessCards = ConfigurationRequestDto(
    categoryId = SampleCatalog.businessCardsId.value,
    printingMethodId = SampleCatalog.digitalId.value,
    components = List(
      ComponentRequestDto(
        role = "Main",
        materialId = SampleCatalog.coated300gsmId.value,
        ink = InkConfigurationDto(InkSetupDto("CMYK", 4), InkSetupDto("CMYK", 4)),
        finishes = List(FinishSelectionDto(SampleCatalog.matteLaminationId.value)),
      )
    ),
    specifications = SpecificationsDto(
      size = Some(SizeDto(90, 55)),
      quantity = Some(500),
    ),
  )

  def spec = suite("CatalogService")(
    suite("configure")(
      test("accepts a valid configuration and returns a storable snapshot") {
        for view <- service.configure(businessCards)
        yield assertTrue(
          view.snapshot.payload.nonEmpty,
          view.snapshot.catalogVersion.value.nonEmpty,
          view.description(Language.En).contains("Business Cards"),
        )
      },
      test("describes the configuration in both languages") {
        for view <- service.configure(businessCards)
        yield assertTrue(
          view.description(Language.En).contains("500×"),
          view.description(Language.En).contains("90×55 mm"),
          view.description(Language.En) != view.description(Language.Cs),
        )
      },
      test("reports an unknown category rather than guessing") {
        for error <- service.configure(businessCards.copy(categoryId = "no-such-category")).flip
        yield assertTrue(
          error.isInstanceOf[CatalogError.Rejected],
          error.message(Language.En).nonEmpty,
          error.message(Language.Cs) != error.message(Language.En),
        )
      },
      test("accumulates every problem instead of stopping at the first") {
        val doublyBroken = businessCards.copy(
          categoryId = "no-such-category",
          printingMethodId = "no-such-method",
        )
        for error <- service.configure(doublyBroken).flip
        yield assertTrue(
          error match
            case CatalogError.Rejected(problems) => problems.size >= 2
            case _                               => false
        )
      },
      test("rejects an out-of-range enum value with the offending field") {
        for error <- service.configure(
            businessCards.copy(components =
              businessCards.components.map(_.copy(role = "sideways"))
            )
          ).flip
        yield assertTrue(error == CatalogError.UnknownValue("role", "sideways"))
      },
      test("enum parsing is case-insensitive") {
        for view <- service.configure(
            businessCards.copy(components = businessCards.components.map(_.copy(role = "main")))
          )
        yield assertTrue(view.snapshot.payload.nonEmpty)
      },
    ),
    suite("revalidate")(
      test("a freshly built snapshot is still buildable") {
        for
          view <- service.configure(businessCards)
          result <- service.revalidate(view.snapshot).exit
        yield assertTrue(result.isSuccess)
      },
      test("a snapshot built against a catalog that no longer has the material is refused") {
        // Simulates a material being withdrawn between add-to-basket and checkout.
        val withdrawn = SampleCatalog.catalog.copy(
          materials = SampleCatalog.catalog.materials - SampleCatalog.coated300gsmId
        )
        val afterWithdrawal =
          LegacyCatalogService.of(withdrawn, mpbuilder.domain.sample.SampleRules.ruleset)
        for
          view <- service.configure(businessCards)
          result <- afterWithdrawal.revalidate(view.snapshot).exit
        yield assertTrue(result.isFailure)
      },
      test("corrupt stored data is reported as malformed, not as a user error") {
        val corrupt = ConfigurationSnapshot("{not json}", CatalogVersion("v1"))
        for error <- service.revalidate(corrupt).flip
        yield assertTrue(error.isInstanceOf[CatalogError.MalformedSnapshot])
      },
    ),
    suite("describe")(
      test("re-describes a stored snapshot without consulting the live catalog") {
        // The snapshot is self-contained, so a catalog that no longer contains the material
        // must still be able to render what was bought.
        val emptied = SampleCatalog.catalog.copy(materials = Map.empty, categories = Map.empty)
        val afterPurge =
          LegacyCatalogService.of(emptied, mpbuilder.domain.sample.SampleRules.ruleset)
        for
          view <- service.configure(businessCards)
          text <- afterPurge.describe(view.snapshot, Language.En)
        yield assertTrue(text.contains("Business Cards"), text.contains("Coated Art Paper 300gsm"))
      }
    ),
    suite("snapshot identity")(
      test("the same request fingerprints identically, for basket deduplication") {
        for
          a <- service.configure(businessCards)
          b <- service.configure(businessCards)
        yield assertTrue(a.snapshot.fingerprint == b.snapshot.fingerprint)
      },
      test("a different quantity fingerprints differently") {
        for
          a <- service.configure(businessCards)
          b <- service.configure(
            businessCards.copy(specifications = businessCards.specifications.copy(quantity = Some(1000)))
          )
        yield assertTrue(a.snapshot.fingerprint != b.snapshot.fingerprint)
      },
    ),
    suite("currentVersion")(
      test("is stable for an unchanged catalog and changes when the catalog does") {
        val changed = SampleCatalog.catalog.copy(
          materials = SampleCatalog.catalog.materials - SampleCatalog.coated300gsmId
        )
        val other = LegacyCatalogService.of(changed, mpbuilder.domain.sample.SampleRules.ruleset)
        for
          a <- service.currentVersion
          b <- service.currentVersion
          c <- other.currentVersion
        yield assertTrue(a == b, a != c)
      }
    ),
  )
