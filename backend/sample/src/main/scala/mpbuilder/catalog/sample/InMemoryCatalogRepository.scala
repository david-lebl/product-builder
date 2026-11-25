package mpbuilder.catalog.sample

import mpbuilder.catalog.*
import mpbuilder.catalog.core.{CatalogRepository, CatalogServiceLive}
import kyo.*
import mpbuilder.shared.*

/** Mutable in-memory `CatalogRepository` backed by Kyo `AtomicRef`, suitable for
  * unit tests AND backend-less UI showcases (supports live admin edits).
  */
final class InMemoryCatalogRepository(
    catalogRef: AtomicRef[ProductCatalog],
    rulesetRef: AtomicRef[CompatibilityRuleset],
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
  ): InMemoryCatalogRepository < Sync =
    for
      catRef  <- AtomicRef.init(initialCatalog)
      ruleRef <- AtomicRef.init(initialRuleset)
    yield new InMemoryCatalogRepository(catRef, ruleRef)

  def service(
      initialCatalog: ProductCatalog = SampleCatalog.catalog,
      initialRuleset: CompatibilityRuleset = SampleRules.ruleset,
  ): CatalogService < Sync =
    repository(initialCatalog, initialRuleset).map(CatalogServiceLive.make)
