package mpbuilder.domain.service

import mpbuilder.domain.model.*
import mpbuilder.domain.model.FinishType.finishCategory
import mpbuilder.domain.model.ProductCategory.*
import mpbuilder.domain.rules.*

object CatalogQueryService:

  def availableMaterials(
      categoryId: CategoryId,
      catalog: ProductCatalog,
      role: ComponentRole,
  ): List[Material] =
    catalog.categories.get(categoryId) match
      case None => Nil
      case Some(category) =>
        category.componentFor(role) match
          case None if role == ComponentRole.Main =>
            category.allAllowedMaterialIds.toList.flatMap(catalog.materials.get)
          case None           => Nil
          case Some(template) => template.allowedMaterialIds.toList.flatMap(catalog.materials.get)

  def compatibleFinishes(
      categoryId: CategoryId,
      materialId: MaterialId,
      catalog: ProductCatalog,
      ruleset: CompatibilityRuleset,
      printingMethodId: Option[PrintingMethodId],
      role: ComponentRole,
  ): List[Finish] =
    val categoryFinishes = catalog.categories.get(categoryId) match
      case None => Nil
      case Some(category) =>
        category.componentFor(role) match
          case None           => Nil
          case Some(template) => template.allowedFinishIds.toList.flatMap(catalog.finishes.get)

    val material = catalog.materials.get(materialId)
    val printingMethod = printingMethodId.flatMap(catalog.printingMethods.get)

    material match
      case None => categoryFinishes
      case Some(mat) =>
        categoryFinishes.filter { finish =>
          ruleset.rules.forall {
            case CompatibilityRule.MaterialFinishIncompatible(matId, finId, _) =>
              !(mat.id == matId && finish.id == finId)
            case CompatibilityRule.FinishRequiresMaterialProperty(finId, reqProp, _) =>
              !(finish.id == finId && !mat.properties.contains(reqProp))
            case CompatibilityRule.MaterialPropertyFinishTypeIncompatible(property, finishType, _) =>
              !(mat.properties.contains(property) && finish.finishType == finishType)
            case CompatibilityRule.MaterialFamilyFinishTypeIncompatible(family, finishType, _) =>
              !(mat.family == family && finish.finishType == finishType)
            case CompatibilityRule.MaterialWeightFinishType(finishType, minWeightGsm, _) =>
              if finish.finishType == finishType then
                mat.weight.exists(_.gsm >= minWeightGsm)
              else true
            case CompatibilityRule.FinishRequiresPrintingProcess(finishType, requiredProcessTypes, _) =>
              if finish.finishType == finishType then
                printingMethod match
                  case None     => true // not selected yet, don't filter
                  case Some(pm) => requiredProcessTypes.contains(pm.processType)
              else true
            case CompatibilityRule.MaterialRequiresFinish(_, _, _)      => true
            case CompatibilityRule.FinishMutuallyExclusive(_, _, _)     => true
            case CompatibilityRule.SpecConstraint(_, _, _)              => true
            case CompatibilityRule.FinishTypeMutuallyExclusive(_, _, _) => true
            case CompatibilityRule.FinishCategoryExclusive(_, _)        => true
            case CompatibilityRule.FinishRequiresFinishType(_, _, _)    => true
            case CompatibilityRule.ConfigurationConstraint(_, _, _)     => true
          }
        }

  def requiredSpecifications(
      categoryId: CategoryId,
      catalog: ProductCatalog,
  ): Set[SpecKind] =
    catalog.categories.get(categoryId) match
      case None           => Set.empty
      case Some(category) => category.requiredSpecKinds

  def availablePrintingMethods(
      categoryId: CategoryId,
      catalog: ProductCatalog,
  ): List[PrintingMethod] =
    catalog.categories.get(categoryId) match
      case None => Nil
      case Some(category) =>
        if category.allowedPrintingMethodIds.isEmpty then
          catalog.printingMethods.values.toList
        else
          category.allowedPrintingMethodIds.toList.flatMap(catalog.printingMethods.get)
