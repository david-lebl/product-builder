package mpbuilder.catalog

import mpbuilder.shared.*
import zio.*
import java.time.Instant

// ── Domain Events ────────────────────────────────────────────────────────────

final case class CatalogPublished(
    catalogVersion: Version,
    rulesetVersion: Version,
    occurredAt: Instant,
) extends DomainEvent

// ── Public Inbound Ports (Module API) ────────────────────────────────────────

trait CatalogService:
  def getCatalog: UIO[ProductCatalog]
  def getCategory(id: CategoryId): IO[CatalogError.CategoryNotFound, ProductCategory]
  def getMaterial(id: MaterialId): IO[CatalogError.MaterialNotFound, Material]
  def getFinish(id: FinishId): IO[CatalogError.FinishNotFound, Finish]
  def getPrintingMethod(id: PrintingMethodId): IO[CatalogError.PrintingMethodNotFound, PrintingMethod]

  /** Returns finishes compatible with the given partial component selection,
    * evaluated through the SAME canonical RuleEngine as full validation (zero drift).
    */
  def compatibleFinishes(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
      alreadySelectedFinishIds: Set[FinishId] = Set.empty,
  ): IO[CatalogError, List[Finish]]

  def configure(cmd: ConfigureProductCmd): IO[CatalogError, ConfigurationResult]

object CatalogService:
  def configure(cmd: ConfigureProductCmd): ZIO[CatalogService, CatalogError, ConfigurationResult] =
    ZIO.serviceWithZIO[CatalogService](_.configure(cmd))

  def compatibleFinishes(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
      alreadySelectedFinishIds: Set[FinishId] = Set.empty,
  ): ZIO[CatalogService, CatalogError, List[Finish]] =
    ZIO.serviceWithZIO[CatalogService](
      _.compatibleFinishes(categoryId, componentKey, materialId, printingMethodId, alreadySelectedFinishIds)
    )
