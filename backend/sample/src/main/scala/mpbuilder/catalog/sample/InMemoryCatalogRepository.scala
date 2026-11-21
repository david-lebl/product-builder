package mpbuilder.catalog.sample

import mpbuilder.catalog.*
import mpbuilder.catalog.core.{CatalogRepository, CatalogServiceLive}
import zio.*

/** Mutable in-memory `CatalogRepository` backed by ZIO `Ref`, suitable for
  * unit tests AND backend-less UI showcases (supports live admin edits).
  */
final class InMemoryCatalogRepository(
    catalogRef: Ref[ProductCatalog],
    rulesetRef: Ref[CompatibilityRuleset],
) extends CatalogRepository:

  override def loadCatalog: UIO[ProductCatalog]       = catalogRef.get
  override def loadRuleset: UIO[CompatibilityRuleset] = rulesetRef.get

  def updateCatalog(f: ProductCatalog => ProductCatalog): UIO[Unit] =
    catalogRef.update(f)

  def updateRuleset(f: CompatibilityRuleset => CompatibilityRuleset): UIO[Unit] =
    rulesetRef.update(f)

object CatalogSampleLayer:

  def repositoryLayer(
      initialCatalog: ProductCatalog = SampleCatalog.catalog,
      initialRuleset: CompatibilityRuleset = SampleRules.ruleset,
  ): ULayer[InMemoryCatalogRepository] =
    ZLayer.fromZIO {
      for
        catRef  <- Ref.make(initialCatalog)
        ruleRef <- Ref.make(initialRuleset)
      yield new InMemoryCatalogRepository(catRef, ruleRef)
    }

  def live(
      initialCatalog: ProductCatalog = SampleCatalog.catalog,
      initialRuleset: CompatibilityRuleset = SampleRules.ruleset,
  ): ULayer[CatalogService] =
    repositoryLayer(initialCatalog, initialRuleset) >>> CatalogServiceLive.layer
