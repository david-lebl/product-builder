package mpbuilder.catalog
package core

import mpbuilder.shared.*
import zio.*

/** Live ZIO implementation of `CatalogService` (compile-time isolated inside `catalog`). */
private[catalog] final class CatalogServiceLive(
    repo: CatalogRepository,
) extends CatalogService:

  override def getCatalog: UIO[ProductCatalog] =
    repo.loadCatalog

  override def getCategory(id: CategoryId): IO[CatalogError.CategoryNotFound, ProductCategory] =
    repo.loadCatalog.flatMap(c => ZIO.fromOption(c.categories.get(id)).orElseFail(CatalogError.CategoryNotFound(id)))

  override def getMaterial(id: MaterialId): IO[CatalogError.MaterialNotFound, Material] =
    repo.loadCatalog.flatMap(c => ZIO.fromOption(c.materials.get(id)).orElseFail(CatalogError.MaterialNotFound(id)))

  override def getFinish(id: FinishId): IO[CatalogError.FinishNotFound, Finish] =
    repo.loadCatalog.flatMap(c => ZIO.fromOption(c.finishes.get(id)).orElseFail(CatalogError.FinishNotFound(id)))

  override def getPrintingMethod(id: PrintingMethodId): IO[CatalogError.PrintingMethodNotFound, PrintingMethod] =
    repo.loadCatalog.flatMap(c =>
      ZIO.fromOption(c.printingMethods.get(id)).orElseFail(CatalogError.PrintingMethodNotFound(id))
    )

  override def compatibleFinishes(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
      alreadySelectedFinishIds: Set[FinishId],
  ): IO[CatalogError, List[Finish]] =
    for
      catalog  <- repo.loadCatalog
      ruleset  <- repo.loadRuleset
      category <- ZIO.fromOption(catalog.categories.get(categoryId)).orElseFail(CatalogError.CategoryNotFound(categoryId))
      template <- ZIO.fromOption(category.templateFor(componentKey))
                    .orElseFail(CatalogError.UnexpectedComponent(categoryId, componentKey))
      material <- ZIO.fromOption(catalog.materials.get(materialId)).orElseFail(CatalogError.MaterialNotFound(materialId))
      pmOpt    <- ZIO.foreach(printingMethodId)(id =>
                    ZIO.fromOption(catalog.printingMethods.get(id)).orElseFail(CatalogError.PrintingMethodNotFound(id))
                  )
      existing  = alreadySelectedFinishIds.toList.flatMap(catalog.finishes.get).map { f =>
                    val side = f.sidePolicy match
                      case FinishSidePolicy.Fixed(s)         => Some(s)
                      case FinishSidePolicy.Selectable(_, d) => Some(d)
                      case FinishSidePolicy.NotApplicable    => None
                    SelectedFinish(f, side, None)
                  }
      candidates = template.allowedFinishIds.toList.flatMap(catalog.finishes.get)
    yield candidates.filter { fin =>
      alreadySelectedFinishIds.contains(fin.id) ||
      RuleEngine.isCandidateFinishCompatible(ruleset, categoryId, componentKey, material, pmOpt, existing, fin)
    }

  override def finishParameterLimits(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
  ): IO[CatalogError, Map[FinishType, BigDecimal]] =
    for
      catalog  <- repo.loadCatalog
      ruleset  <- repo.loadRuleset
      category <- ZIO.fromOption(catalog.categories.get(categoryId)).orElseFail(CatalogError.CategoryNotFound(categoryId))
      _        <- ZIO.fromOption(category.templateFor(componentKey))
                    .orElseFail(CatalogError.UnexpectedComponent(categoryId, componentKey))
      material <- ZIO.fromOption(catalog.materials.get(materialId)).orElseFail(CatalogError.MaterialNotFound(materialId))
      pmOpt    <- ZIO.foreach(printingMethodId)(id =>
                    ZIO.fromOption(catalog.printingMethods.get(id)).orElseFail(CatalogError.PrintingMethodNotFound(id))
                  )
    yield RuleEngine.finishParameterLimits(ruleset, categoryId, componentKey, material, pmOpt)

  override def configure(cmd: ConfigureProductCmd): IO[CatalogError, ConfigurationResult] =
    for
      catalog <- repo.loadCatalog
      ruleset <- repo.loadRuleset
      result  <- ConfigurationEngine.configure(cmd, catalog, ruleset).fold(
                   errors => ZIO.fail(
                     if errors.size == 1 then errors.head
                     else CatalogError.ConfigurationValidationFailed(errors)
                   ),
                   ZIO.succeed(_),
                 )
    yield result

private[catalog] object CatalogServiceLive:
  val layer: ZLayer[CatalogRepository, Nothing, CatalogService] =
    ZLayer.fromFunction(CatalogServiceLive.apply)
