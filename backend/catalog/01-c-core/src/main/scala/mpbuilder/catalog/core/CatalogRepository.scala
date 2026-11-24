package mpbuilder.catalog
package core

import kyo.*

/** Outbound port for loading and persisting the active catalog and compatibility ruleset. */
private[catalog] trait CatalogRepository:
  def loadCatalog: ProductCatalog < Async
  def loadRuleset: CompatibilityRuleset < Async
  def saveCatalog(catalog: ProductCatalog): Unit < Async        = Kyo.unit
  def saveRuleset(ruleset: CompatibilityRuleset): Unit < Async  = Kyo.unit
