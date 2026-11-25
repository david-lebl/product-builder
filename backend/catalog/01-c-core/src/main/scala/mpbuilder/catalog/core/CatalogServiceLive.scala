package mpbuilder.catalog
package core

import mpbuilder.shared.*
import kyo.*

/** Live Kyo implementation of `CatalogService` (compile-time isolated inside `catalog`). */
private[catalog] final class CatalogServiceLive(
    repo: CatalogRepository,
) extends CatalogService:

  override def getCatalog: ProductCatalog < Async =
    repo.loadCatalog

  override def getCategory(id: CategoryId): ProductCategory < (Async & Abort[CatalogError.CategoryNotFound]) =
    repo.loadCatalog.flatMap(c => c.categories.get(id).orFail(CatalogError.CategoryNotFound(id)))

  override def getMaterial(id: MaterialId): Material < (Async & Abort[CatalogError.MaterialNotFound]) =
    repo.loadCatalog.flatMap(c => c.materials.get(id).orFail(CatalogError.MaterialNotFound(id)))

  override def getFinish(id: FinishId): Finish < (Async & Abort[CatalogError.FinishNotFound]) =
    repo.loadCatalog.flatMap(c => c.finishes.get(id).orFail(CatalogError.FinishNotFound(id)))

  override def getPrintingMethod(id: PrintingMethodId): PrintingMethod < (Async & Abort[CatalogError.PrintingMethodNotFound]) =
    repo.loadCatalog.flatMap(c =>
      c.printingMethods.get(id).orFail(CatalogError.PrintingMethodNotFound(id))
    )

  override def compatibleFinishes(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
      alreadySelectedFinishIds: Set[FinishId],
  ): List[Finish] < (Async & Abort[CatalogError]) =
    for
      catalog  <- repo.loadCatalog
      ruleset  <- repo.loadRuleset
      category <- catalog.categories.get(categoryId).orFail(CatalogError.CategoryNotFound(categoryId))
      template <- category.templateFor(componentKey).orFail(CatalogError.UnexpectedComponent(categoryId, componentKey))
      material <- catalog.materials.get(materialId).orFail(CatalogError.MaterialNotFound(materialId))
      pmOpt    <- Effects.traverse(printingMethodId)(id =>
                    catalog.printingMethods.get(id).orFail(CatalogError.PrintingMethodNotFound(id))
                  )
      existing  = alreadySelectedFinishIds.toList.flatMap(catalog.finishes.get).map { f =>
                    val side = f.sidePolicy match
                      case FinishSidePolicy.Fixed(s)         => Some(s)
                      case FinishSidePolicy.Selectable(_, d) => Some(d)
                      case FinishSidePolicy.NotApplicable    => None
                    SelectedFinish(f, side, None)
                  }
      candidates = template.allowedFinishIds.toList.flatMap(catalog.finishes.get).filter(_.enabled)
    yield candidates.filter { fin =>
      alreadySelectedFinishIds.contains(fin.id) ||
      RuleEngine.isCandidateFinishCompatible(ruleset, categoryId, componentKey, material, pmOpt, existing, fin)
    }

  override def finishParameterLimits(
      categoryId: CategoryId,
      componentKey: ComponentKey,
      materialId: MaterialId,
      printingMethodId: Option[PrintingMethodId],
  ): Map[FinishType, BigDecimal] < (Async & Abort[CatalogError]) =
    for
      catalog  <- repo.loadCatalog
      ruleset  <- repo.loadRuleset
      category <- catalog.categories.get(categoryId).orFail(CatalogError.CategoryNotFound(categoryId))
      _        <- category.templateFor(componentKey).orFail(CatalogError.UnexpectedComponent(categoryId, componentKey))
      material <- catalog.materials.get(materialId).orFail(CatalogError.MaterialNotFound(materialId))
      pmOpt    <- Effects.traverse(printingMethodId)(id =>
                    catalog.printingMethods.get(id).orFail(CatalogError.PrintingMethodNotFound(id))
                  )
    yield RuleEngine.finishParameterLimits(ruleset, categoryId, componentKey, material, pmOpt)

  override def configure(cmd: ConfigureProductCmd): ConfigurationResult < (Async & Abort[CatalogError]) =
    for
      catalog <- repo.loadCatalog
      ruleset <- repo.loadRuleset
      result  <- ConfigurationEngine.configure(cmd, catalog, ruleset).fold(
                   errors => Abort.fail(
                     if errors.size == 1 then errors.head
                     else CatalogError.ConfigurationValidationFailed(errors)
                   ),
                   Kyo.lift(_),
                 )
    yield result

  // ── Catalog & Ruleset Administration ───────────────────────────────────────

  override def getRuleset: CompatibilityRuleset < Async =
    repo.loadRuleset

  override def upsertCategory(category: ProductCategory): ProductCatalog < (Async & Abort[CatalogError]) =
    for
      cat <- repo.loadCatalog
      next = cat.copy(
               version = bumpVersion(cat.version),
               categories = cat.categories + (category.id -> category),
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def deleteCategory(id: CategoryId): ProductCatalog < (Async & Abort[CatalogError]) =
    for
      cat <- repo.loadCatalog
      _   <- Effects.failUnless(cat.categories.contains(id))(CatalogError.CategoryNotFound(id))
      next = cat.copy(
               version = bumpVersion(cat.version),
               categories = cat.categories - id,
               showcases = cat.showcases - id,
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def upsertMaterial(material: Material): ProductCatalog < (Async & Abort[CatalogError]) =
    for
      cat <- repo.loadCatalog
      next = cat.copy(
               version = bumpVersion(cat.version),
               materials = cat.materials + (material.id -> material),
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def deleteMaterial(id: MaterialId): ProductCatalog < (Async & Abort[CatalogError]) =
    for
      cat <- repo.loadCatalog
      _   <- Effects.failUnless(cat.materials.contains(id))(CatalogError.MaterialNotFound(id))
      refs = cat.categories.values.toList
               .filter(_.components.exists(_.allowedMaterialIds.contains(id)))
               .map(_.id.value)
      _   <- Effects.failWhen(refs.nonEmpty)(CatalogError.EntityInUse("Material", id.value, refs))
      next = cat.copy(
               version = bumpVersion(cat.version),
               materials = cat.materials - id,
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def upsertFinish(finish: Finish): ProductCatalog < (Async & Abort[CatalogError]) =
    for
      cat <- repo.loadCatalog
      next = cat.copy(
               version = bumpVersion(cat.version),
               finishes = cat.finishes + (finish.id -> finish),
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def deleteFinish(id: FinishId): ProductCatalog < (Async & Abort[CatalogError]) =
    for
      cat <- repo.loadCatalog
      _   <- Effects.failUnless(cat.finishes.contains(id))(CatalogError.FinishNotFound(id))
      refs = cat.categories.values.toList
               .filter(_.components.exists(_.allowedFinishIds.contains(id)))
               .map(_.id.value)
      _   <- Effects.failWhen(refs.nonEmpty)(CatalogError.EntityInUse("Finish", id.value, refs))
      next = cat.copy(
               version = bumpVersion(cat.version),
               finishes = cat.finishes - id,
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def upsertPrintingMethod(method: PrintingMethod): ProductCatalog < (Async & Abort[CatalogError]) =
    for
      cat <- repo.loadCatalog
      next = cat.copy(
               version = bumpVersion(cat.version),
               printingMethods = cat.printingMethods + (method.id -> method),
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def deletePrintingMethod(id: PrintingMethodId): ProductCatalog < (Async & Abort[CatalogError]) =
    for
      cat <- repo.loadCatalog
      _   <- Effects.failUnless(cat.printingMethods.contains(id))(CatalogError.PrintingMethodNotFound(id))
      refs = cat.categories.values.toList
               .filter(_.components.exists(_.allowedPrintingMethodIds.contains(id)))
               .map(_.id.value)
      _   <- Effects.failWhen(refs.nonEmpty)(CatalogError.EntityInUse("PrintingMethod", id.value, refs))
      next = cat.copy(
               version = bumpVersion(cat.version),
               printingMethods = cat.printingMethods - id,
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def upsertShowcaseListing(listing: ShowcaseListing): ProductCatalog < (Async & Abort[CatalogError]) =
    for
      cat <- repo.loadCatalog
      _   <- Effects.failUnless(cat.categories.contains(listing.categoryId))(CatalogError.CategoryNotFound(listing.categoryId))
      next = cat.copy(
               version = bumpVersion(cat.version),
               showcases = cat.showcases + (listing.categoryId -> listing),
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def deleteShowcaseListing(categoryId: CategoryId): ProductCatalog < (Async & Abort[CatalogError]) =
    for
      cat <- repo.loadCatalog
      next = cat.copy(
               version = bumpVersion(cat.version),
               showcases = cat.showcases - categoryId,
             )
      _   <- repo.saveCatalog(next)
    yield next

  override def upsertRule(rule: CompatibilityRule): CompatibilityRuleset < (Async & Abort[CatalogError]) =
    for
      rs <- repo.loadRuleset
      updated = if rs.rules.exists(_.id == rule.id) then rs.rules.map(r => if r.id == rule.id then rule else r)
                else rs.rules :+ rule
      next = rs.copy(version = bumpVersion(rs.version), rules = updated)
      _   <- repo.saveRuleset(next)
    yield next

  override def deleteRule(id: RuleId): CompatibilityRuleset < (Async & Abort[CatalogError]) =
    for
      rs <- repo.loadRuleset
      _  <- Effects.failUnless(rs.rules.exists(_.id == id))(CatalogError.RuleNotFound(id))
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
  def make(repo: CatalogRepository): CatalogService = new CatalogServiceLive(repo)
