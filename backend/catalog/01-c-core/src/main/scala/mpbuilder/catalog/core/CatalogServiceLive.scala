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

  // ── Catalog & Ruleset Administration ───────────────────────────────────────

  override def getRuleset: UIO[CompatibilityRuleset] =
    repo.loadRuleset

  override def upsertCategory(category: ProductCategory): IO[CatalogError, ProductCatalog] =
    for
      cat <- repo.loadCatalog
      next = cat.copy(
               version = bumpVersion(cat.version),
               categories = cat.categories + (category.id -> category),
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def deleteCategory(id: CategoryId): IO[CatalogError, ProductCatalog] =
    for
      cat <- repo.loadCatalog
      _   <- ZIO.fail(CatalogError.CategoryNotFound(id)).unless(cat.categories.contains(id))
      next = cat.copy(
               version = bumpVersion(cat.version),
               categories = cat.categories - id,
               showcases = cat.showcases - id,
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def upsertMaterial(material: Material): IO[CatalogError, ProductCatalog] =
    for
      cat <- repo.loadCatalog
      next = cat.copy(
               version = bumpVersion(cat.version),
               materials = cat.materials + (material.id -> material),
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def deleteMaterial(id: MaterialId): IO[CatalogError, ProductCatalog] =
    for
      cat <- repo.loadCatalog
      _   <- ZIO.fail(CatalogError.MaterialNotFound(id)).unless(cat.materials.contains(id))
      refs = cat.categories.values.toList
               .filter(_.components.exists(_.allowedMaterialIds.contains(id)))
               .map(_.id.value)
      _   <- ZIO.fail(CatalogError.EntityInUse("Material", id.value, refs)).when(refs.nonEmpty)
      next = cat.copy(
               version = bumpVersion(cat.version),
               materials = cat.materials - id,
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def upsertFinish(finish: Finish): IO[CatalogError, ProductCatalog] =
    for
      cat <- repo.loadCatalog
      next = cat.copy(
               version = bumpVersion(cat.version),
               finishes = cat.finishes + (finish.id -> finish),
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def deleteFinish(id: FinishId): IO[CatalogError, ProductCatalog] =
    for
      cat <- repo.loadCatalog
      _   <- ZIO.fail(CatalogError.FinishNotFound(id)).unless(cat.finishes.contains(id))
      refs = cat.categories.values.toList
               .filter(_.components.exists(_.allowedFinishIds.contains(id)))
               .map(_.id.value)
      _   <- ZIO.fail(CatalogError.EntityInUse("Finish", id.value, refs)).when(refs.nonEmpty)
      next = cat.copy(
               version = bumpVersion(cat.version),
               finishes = cat.finishes - id,
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def upsertPrintingMethod(method: PrintingMethod): IO[CatalogError, ProductCatalog] =
    for
      cat <- repo.loadCatalog
      next = cat.copy(
               version = bumpVersion(cat.version),
               printingMethods = cat.printingMethods + (method.id -> method),
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def deletePrintingMethod(id: PrintingMethodId): IO[CatalogError, ProductCatalog] =
    for
      cat <- repo.loadCatalog
      _   <- ZIO.fail(CatalogError.PrintingMethodNotFound(id)).unless(cat.printingMethods.contains(id))
      refs = cat.categories.values.toList
               .filter(_.components.exists(_.allowedPrintingMethodIds.contains(id)))
               .map(_.id.value)
      _   <- ZIO.fail(CatalogError.EntityInUse("PrintingMethod", id.value, refs)).when(refs.nonEmpty)
      next = cat.copy(
               version = bumpVersion(cat.version),
               printingMethods = cat.printingMethods - id,
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def upsertShowcaseListing(listing: ShowcaseListing): IO[CatalogError, ProductCatalog] =
    for
      cat <- repo.loadCatalog
      _   <- ZIO.fail(CatalogError.CategoryNotFound(listing.categoryId)).unless(cat.categories.contains(listing.categoryId))
      next = cat.copy(
               version = bumpVersion(cat.version),
               showcases = cat.showcases + (listing.categoryId -> listing),
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def deleteShowcaseListing(categoryId: CategoryId): IO[CatalogError, ProductCatalog] =
    for
      cat <- repo.loadCatalog
      next = cat.copy(
               version = bumpVersion(cat.version),
               showcases = cat.showcases - categoryId,
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def upsertRule(rule: CompatibilityRule): IO[CatalogError, CompatibilityRuleset] =
    for
      rs <- repo.loadRuleset
      updated = if rs.rules.exists(_.id == rule.id) then rs.rules.map(r => if r.id == rule.id then rule else r)
                else rs.rules :+ rule
      next = rs.copy(version = bumpVersion(rs.version), rules = updated)
      _   <- repo.saveRuleset(next)
    yield next

  override def deleteRule(id: RuleId): IO[CatalogError, CompatibilityRuleset] =
    for
      rs <- repo.loadRuleset
      _  <- ZIO.fail(CatalogError.RuleNotFound(id)).unless(rs.rules.exists(_.id == id))
      next = rs.copy(version = bumpVersion(rs.version), rules = rs.rules.filterNot(_.id == id))
      _   <- repo.saveRuleset(next)
    yield next

  private def bumpVersion(v: Version): Version =
    val raw = v.value
    val idx = raw.lastIndexOf('.')
    if idx >= 0 then
      raw.substring(idx + 1).toIntOption match
        case Some(n) => Version.unsafe(s"${raw.substring(0, idx + 1)}${n + 1}")
        case None    => Version.unsafe(s"$raw.1")
    else
      raw.toIntOption match
        case Some(n) => Version.unsafe(s"${n + 1}")
        case None    => Version.unsafe(s"$raw.1")

private[catalog] object CatalogServiceLive:
  val layer: ZLayer[CatalogRepository, Nothing, CatalogService] =
    ZLayer.fromFunction(CatalogServiceLive.apply)
