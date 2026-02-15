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
  case IncompatibleMaterialPropertyFinish(property: MaterialProperty, finishType: FinishType, reason: String)
  case IncompatibleMaterialFamilyFinish(family: MaterialFamily, finishType: FinishType, reason: String)
  case FinishWeightRequirementNotMet(finishType: FinishType, minWeightGsm: Int, actualWeightGsm: Option[Int], reason: String)
  case MutuallyExclusiveFinishTypes(finishTypeA: FinishType, finishTypeB: FinishType, reason: String)
  case FinishCategoryLimitExceeded(category: FinishCategory, reason: String)
  case FinishRequiresPrintingProcessViolation(finishType: FinishType, requiredProcessTypes: Set[PrintingProcessType], reason: String)
  case PrintingMethodFinishIncompatibleError(printingMethodId: PrintingMethodId, finishType: FinishType, reason: String)
  case FinishMissingDependentFinishType(finishId: FinishId, requiredFinishType: FinishType, reason: String)
  case InvalidCategoryPrintingMethod(categoryId: CategoryId, printingMethodId: PrintingMethodId)
  case PrintingMethodNotFound(printingMethodId: PrintingMethodId)
  case ConfigurationConstraintViolation(categoryId: CategoryId, reason: String)

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
    case IncompatibleMaterialPropertyFinish(prop, ft, reason) =>
      s"Material property '$prop' is incompatible with finish type '$ft': $reason"
    case IncompatibleMaterialFamilyFinish(family, ft, reason) =>
      s"Material family '$family' is incompatible with finish type '$ft': $reason"
    case FinishWeightRequirementNotMet(ft, minGsm, actualGsm, reason) =>
      s"Finish type '$ft' requires minimum ${minGsm}gsm (actual: ${actualGsm.map(g => s"${g}gsm").getOrElse("unknown")}): $reason"
    case MutuallyExclusiveFinishTypes(ftA, ftB, reason) =>
      s"Finish types '$ftA' and '$ftB' are mutually exclusive: $reason"
    case FinishCategoryLimitExceeded(cat, reason) =>
      s"Too many finishes in category '$cat': $reason"
    case FinishRequiresPrintingProcessViolation(ft, procs, reason) =>
      s"Finish type '$ft' requires printing process ${procs.mkString(" or ")}: $reason"
    case PrintingMethodFinishIncompatibleError(pmId, ft, reason) =>
      s"Printing method '${pmId.value}' is incompatible with finish type '$ft': $reason"
    case FinishMissingDependentFinishType(finId, reqFt, reason) =>
      s"Finish '${finId.value}' requires a finish of type '$reqFt': $reason"
    case InvalidCategoryPrintingMethod(catId, pmId) =>
      s"Printing method '${pmId.value}' is not allowed for category '${catId.value}'"
    case PrintingMethodNotFound(pmId) =>
      s"Printing method '${pmId.value}' not found in catalog"
    case ConfigurationConstraintViolation(catId, reason) =>
      s"Configuration constraint violated for category '${catId.value}': $reason"
