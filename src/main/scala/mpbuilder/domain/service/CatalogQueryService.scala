package mpbuilder.domain.service

import mpbuilder.domain.model.*
import mpbuilder.domain.rules.*

object CatalogQueryService:

  def availableMaterials(
      categoryId: CategoryId,
      catalog: ProductCatalog,
  ): List[Material] =
    catalog.categories.get(categoryId) match
      case None => Nil
      case Some(category) =>
        category.allowedMaterialIds.toList.flatMap(catalog.materials.get)

  def compatibleFinishes(
      categoryId: CategoryId,
      materialId: MaterialId,
      catalog: ProductCatalog,
      ruleset: CompatibilityRuleset,
  ): List[Finish] =
    val categoryFinishes = catalog.categories.get(categoryId) match
      case None           => Nil
      case Some(category) => category.allowedFinishIds.toList.flatMap(catalog.finishes.get)

    val material = catalog.materials.get(materialId)

    material match
      case None => categoryFinishes
      case Some(mat) =>
        categoryFinishes.filter { finish =>
          ruleset.rules.forall {
            case CompatibilityRule.MaterialFinishIncompatible(matId, finId, _) =>
              !(mat.id == matId && finish.id == finId)
            case CompatibilityRule.FinishRequiresMaterialProperty(finId, reqProp, _) =>
              !(finish.id == finId && !mat.properties.contains(reqProp))
            case _ => true
          }
        }

  def requiredSpecifications(
      categoryId: CategoryId,
      catalog: ProductCatalog,
  ): Set[SpecKind] =
    catalog.categories.get(categoryId) match
      case None           => Set.empty
      case Some(category) => category.requiredSpecKinds
