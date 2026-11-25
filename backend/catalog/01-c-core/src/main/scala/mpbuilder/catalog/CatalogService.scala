package mpbuilder.catalog

import mpbuilder.shared.*
import kyo.*
import java.time.Instant

// ── Domain Events ────────────────────────────────────────────────────────────

final case class CatalogPublished(
    catalogVersion: Version,
    rulesetVersion: Version,
    occurredAt: Instant,
) extends DomainEvent

// ── Public Inbound Ports (Module API) ────────────────────────────────────────

trait CatalogService:
  def getCatalog: ProductCatalog < Async
  def getCategory(id: CategoryId): ProductCategory < (Async & Abort[CatalogError.CategoryNotFound])
  def getMaterial(id: MaterialId): Material < (Async & Abort[CatalogError.MaterialNotFound])
  def getFinish(id: FinishId): Finish < (Async & Abort[CatalogError.FinishNotFound])
  def getPrintingMethod(id: PrintingMethodId): PrintingMethod < (Async & Abort[CatalogError.PrintingMethodNotFound])

  /** Returns finishes compatible with the given partial component selection,
    * evaluated through the SAME canonical RuleEngine as full validation (zero drift).
    */
  def compatibleFinishes(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
      alreadySelectedFinishIds: Set[FinishId] = Set.empty,
  ): List[Finish] < (Async & Abort[CatalogError])

  /** Returns effective numeric parameter upper bounds for parameterized finishes (e.g. scoring max creases),
    * derived from the canonical RuleEngine so the UI never duplicates constraint rules.
    */
  def finishParameterLimits(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
  ): Map[FinishType, BigDecimal] < (Async & Abort[CatalogError])

  def configure(cmd: ConfigureProductCmd): ConfigurationResult < (Async & Abort[CatalogError])

  // ── Catalog & Ruleset Administration ───────────────────────────────────────
  def getRuleset: CompatibilityRuleset < Async
  def upsertCategory(category: ProductCategory): ProductCatalog < (Async & Abort[CatalogError])
  def deleteCategory(id: CategoryId): ProductCatalog < (Async & Abort[CatalogError])
  def upsertMaterial(material: Material): ProductCatalog < (Async & Abort[CatalogError])
  def deleteMaterial(id: MaterialId): ProductCatalog < (Async & Abort[CatalogError])
  def upsertFinish(finish: Finish): ProductCatalog < (Async & Abort[CatalogError])
  def deleteFinish(id: FinishId): ProductCatalog < (Async & Abort[CatalogError])
  def upsertPrintingMethod(method: PrintingMethod): ProductCatalog < (Async & Abort[CatalogError])
  def deletePrintingMethod(id: PrintingMethodId): ProductCatalog < (Async & Abort[CatalogError])
  def upsertShowcaseListing(listing: ShowcaseListing): ProductCatalog < (Async & Abort[CatalogError])
  def deleteShowcaseListing(categoryId: CategoryId): ProductCatalog < (Async & Abort[CatalogError])
  def upsertRule(rule: CompatibilityRule): CompatibilityRuleset < (Async & Abort[CatalogError])
  def deleteRule(id: RuleId): CompatibilityRuleset < (Async & Abort[CatalogError])
