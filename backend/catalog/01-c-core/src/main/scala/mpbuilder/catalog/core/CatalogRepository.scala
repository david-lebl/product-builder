package mpbuilder.catalog
package core

import zio.*

/** Outbound port for loading and persisting the active catalog and compatibility ruleset. */
private[catalog] trait CatalogRepository:
  def loadCatalog: UIO[ProductCatalog]
  def loadRuleset: UIO[CompatibilityRuleset]
  def saveCatalog(catalog: ProductCatalog): UIO[Unit]        = ZIO.unit
  def saveRuleset(ruleset: CompatibilityRuleset): UIO[Unit]  = ZIO.unit
