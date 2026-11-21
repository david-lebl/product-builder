package mpbuilder.catalog
package core

import zio.*

/** Outbound port for loading the active catalog and compatibility ruleset. */
private[catalog] trait CatalogRepository:
  def loadCatalog: UIO[ProductCatalog]
  def loadRuleset: UIO[CompatibilityRuleset]
