package mpbuilder.domain.validation

import mpbuilder.domain.model.*
import mpbuilder.domain.rules.SpecPredicate

enum ConfigurationError:
  case CategoryNotFound(categoryId: CategoryId)
  case MaterialNotFound(materialId: MaterialId)
  case FinishNotFound(finishId: FinishId)
  case InvalidCategoryMaterial(categoryId: CategoryId, materialId: MaterialId)
  case InvalidCategoryFinish(categoryId: CategoryId, finishId: FinishId)
  case MissingRequiredSpec(categoryId: CategoryId, specKind: SpecKind)
  case IncompatibleMaterialFinish(materialId: MaterialId, finishId: FinishId, reason: String)
  case MissingRequiredFinish(materialId: MaterialId, requiredFinishIds: Set[FinishId], reason: String)
  case FinishMissingMaterialProperty(finishId: FinishId, requiredProperty: MaterialProperty, reason: String)
  case MutuallyExclusiveFinishes(finishIdA: FinishId, finishIdB: FinishId, reason: String)
  case SpecConstraintViolation(categoryId: CategoryId, predicate: SpecPredicate, reason: String)

  def message: String = this match
    case CategoryNotFound(id) =>
      s"Category '${id.value}' not found in catalog"
    case MaterialNotFound(id) =>
      s"Material '${id.value}' not found in catalog"
    case FinishNotFound(id) =>
      s"Finish '${id.value}' not found in catalog"
    case InvalidCategoryMaterial(catId, matId) =>
      s"Material '${matId.value}' is not allowed for category '${catId.value}'"
    case InvalidCategoryFinish(catId, finId) =>
      s"Finish '${finId.value}' is not allowed for category '${catId.value}'"
    case MissingRequiredSpec(catId, kind) =>
      s"Category '${catId.value}' requires specification '$kind'"
    case IncompatibleMaterialFinish(matId, finId, reason) =>
      s"Material '${matId.value}' is incompatible with finish '${finId.value}': $reason"
    case MissingRequiredFinish(matId, finIds, reason) =>
      s"Material '${matId.value}' requires one of finishes [${finIds.map(_.value).mkString(", ")}]: $reason"
    case FinishMissingMaterialProperty(finId, prop, reason) =>
      s"Finish '${finId.value}' requires material property '$prop': $reason"
    case MutuallyExclusiveFinishes(a, b, reason) =>
      s"Finishes '${a.value}' and '${b.value}' are mutually exclusive: $reason"
    case SpecConstraintViolation(catId, pred, reason) =>
      s"Specification constraint violated for category '${catId.value}': $reason"
