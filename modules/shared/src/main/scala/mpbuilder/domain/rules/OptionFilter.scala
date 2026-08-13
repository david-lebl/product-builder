package mpbuilder.domain.rules

import mpbuilder.domain.catalog.*
import mpbuilder.domain.config.*
import mpbuilder.domain.ids.*

/** Derives the options that are still valid given what's already chosen, from
  * the same data the validator uses — so the UI only ever offers valid choices
  * (spec §4) and can never drift from validation.
  */
object OptionFilter:

  def materialsFor(catalog: Catalog, categoryId: CategoryId, role: ComponentRole): List[Material] =
    (for
      category <- catalog.categoriesById.get(categoryId)
      spec     <- category.componentSpec(role)
    yield catalog.materials.filter(m => spec.allowedMaterials.allows(m.id)))
      .getOrElse(Nil)

  /** Finishes still selectable for a component: allowed by the category, and
    * adding them to the current selection would violate no finish rule.
    */
  def finishesFor(
    catalog: Catalog,
    rules: List[CompatibilityRule],
    categoryId: CategoryId,
    role: ComponentRole,
    chosenMaterial: Option[MaterialId],
    chosenFinishes: List[SelectedFinish],
  ): List[Finish] =
    (for
      category <- catalog.categoriesById.get(categoryId)
      spec     <- category.componentSpec(role)
    yield
      val chosenTypes =
        chosenFinishes.flatMap(sf => catalog.finishesById.get(sf.finishId)).map(_.finishType)
      val chosenIds = chosenFinishes.map(_.finishId).toSet
      val materialWeight = chosenMaterial.flatMap(catalog.materialsById.get).flatMap(_.weightGsm)

      catalog.finishes.filter { finish =>
        spec.allowedFinishes.allows(finish.id)
          && !chosenIds.contains(finish.id)
          && rules.forall {
            case CompatibilityRule.OnePerFinishType(t) =>
              t != finish.finishType || !chosenTypes.contains(t)
            case CompatibilityRule.MutuallyExclusiveFinishTypes(a, b) =>
              !(finish.finishType == a && chosenTypes.contains(b))
                && !(finish.finishType == b && chosenTypes.contains(a))
            case CompatibilityRule.MutuallyExclusiveFinishes(a, b) =>
              !(finish.id == a && chosenIds.contains(b)) && !(finish.id == b && chosenIds.contains(a))
            case CompatibilityRule.FinishTypeMinWeight(t, minGsm) =>
              t != finish.finishType || materialWeight.forall(_ >= minGsm)
            case _ => true
          }
      }
    ).getOrElse(Nil)

  def printingMethodsFor(catalog: Catalog, categoryId: CategoryId): List[PrintingMethod] =
    catalog.categoriesById
      .get(categoryId)
      .map(category => catalog.printingMethods.filter(m => category.allowedPrintingMethods.allows(m.id)))
      .getOrElse(Nil)

  def inkConfigsFor(catalog: Catalog, methodId: PrintingMethodId): List[InkConfiguration] =
    catalog.printingMethodsById
      .get(methodId)
      .map { method =>
        method.maxColors match
          case None      => catalog.inkConfigurations
          case Some(max) => catalog.inkConfigurations.filter(_.maxColorsUsed <= max)
      }
      .getOrElse(Nil)
