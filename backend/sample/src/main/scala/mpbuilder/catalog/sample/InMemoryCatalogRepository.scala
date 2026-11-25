package mpbuilder.catalog.sample

import mpbuilder.catalog.*
import mpbuilder.catalog.core.{CatalogRepository, CatalogServiceLive}
import kyo.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.sample.SampleStorage
import mpbuilder.shared.*

/** Mutable in-memory `CatalogRepository` backed by [[StateRef]]s, suitable for
  * unit tests AND backend-less UI showcases (supports live admin edits, optionally persisted).
  */
final class InMemoryCatalogRepository(
    catalogRef: StateRef[ProductCatalog],
    rulesetRef: StateRef[CompatibilityRuleset],
) extends CatalogRepository:

  override def loadCatalog: ProductCatalog < Async              = catalogRef.get
  override def loadRuleset: CompatibilityRuleset < Async        = rulesetRef.get
  override def saveCatalog(catalog: ProductCatalog): Unit < Async       = catalogRef.set(catalog)
  override def saveRuleset(ruleset: CompatibilityRuleset): Unit < Async = rulesetRef.set(ruleset)

  def updateCatalog(f: ProductCatalog => ProductCatalog): Unit < Async =
    catalogRef.update(f)

  def updateRuleset(f: CompatibilityRuleset => CompatibilityRuleset): Unit < Async =
    rulesetRef.update(f)

object CatalogSampleWiring:

  def repository(
      initialCatalog: ProductCatalog = SampleCatalog.catalog,
      initialRuleset: CompatibilityRuleset = SampleRules.ruleset,
      storage: SampleStorage = SampleStorage.memory,
  ): InMemoryCatalogRepository < Sync =
    for
      catRef  <- storage.value("catalog.catalog", initialCatalog)
      ruleRef <- storage.value("catalog.ruleset", initialRuleset)
    yield new InMemoryCatalogRepository(catRef, ruleRef)

  def service(
      initialCatalog: ProductCatalog = SampleCatalog.catalog,
      initialRuleset: CompatibilityRuleset = SampleRules.ruleset,
      storage: SampleStorage = SampleStorage.memory,
  ): CatalogService < Sync =
    repository(initialCatalog, initialRuleset, storage).map(CatalogServiceLive.make)
