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
  case InkConfigExceedsMethodColorLimit(printingMethodId: PrintingMethodId, inkConfig: InkConfiguration, maxAllowed: Int)
  case MissingRequiredComponent(categoryId: CategoryId, role: ComponentRole)
  case InvalidComponentMaterial(categoryId: CategoryId, role: ComponentRole, materialId: MaterialId)
  case MissingComponentInkConfig(role: ComponentRole)
  case ComponentInkConfigExceedsMethodColorLimit(role: ComponentRole, printingMethodId: PrintingMethodId, inkConfig: InkConfiguration, maxAllowed: Int)

  def message: String = message(Language.En)

  def message(lang: Language): String = this match
    case CategoryNotFound(id) => lang match
      case Language.En => s"Category '${id.value}' not found in catalog"
      case Language.Cs => s"Kategorie '${id.value}' nebyla nalezena v katalogu"
    case MaterialNotFound(id) => lang match
      case Language.En => s"Material '${id.value}' not found in catalog"
      case Language.Cs => s"Materiál '${id.value}' nebyl nalezen v katalogu"
    case FinishNotFound(id) => lang match
      case Language.En => s"Finish '${id.value}' not found in catalog"
      case Language.Cs => s"Povrchová úprava '${id.value}' nebyla nalezena v katalogu"
    case InvalidCategoryMaterial(catId, matId) => lang match
      case Language.En => s"Material '${matId.value}' is not allowed for category '${catId.value}'"
      case Language.Cs => s"Materiál '${matId.value}' není povolen pro kategorii '${catId.value}'"
    case InvalidCategoryFinish(catId, finId) => lang match
      case Language.En => s"Finish '${finId.value}' is not allowed for category '${catId.value}'"
      case Language.Cs => s"Povrchová úprava '${finId.value}' není povolena pro kategorii '${catId.value}'"
    case MissingRequiredSpec(catId, kind) => lang match
      case Language.En => s"Category '${catId.value}' requires specification '$kind'"
      case Language.Cs => s"Kategorie '${catId.value}' vyžaduje specifikaci '$kind'"
    case IncompatibleMaterialFinish(matId, finId, reason) => lang match
      case Language.En => s"Material '${matId.value}' is incompatible with finish '${finId.value}': $reason"
      case Language.Cs => s"Materiál '${matId.value}' je nekompatibilní s úpravou '${finId.value}': $reason"
    case MissingRequiredFinish(matId, finIds, reason) => lang match
      case Language.En => s"Material '${matId.value}' requires one of finishes [${finIds.map(_.value).mkString(", ")}]: $reason"
      case Language.Cs => s"Materiál '${matId.value}' vyžaduje jednu z úprav [${finIds.map(_.value).mkString(", ")}]: $reason"
    case FinishMissingMaterialProperty(finId, prop, reason) => lang match
      case Language.En => s"Finish '${finId.value}' requires material property '$prop': $reason"
      case Language.Cs => s"Úprava '${finId.value}' vyžaduje vlastnost materiálu '$prop': $reason"
    case MutuallyExclusiveFinishes(a, b, reason) => lang match
      case Language.En => s"Finishes '${a.value}' and '${b.value}' are mutually exclusive: $reason"
      case Language.Cs => s"Úpravy '${a.value}' a '${b.value}' se vzájemně vylučují: $reason"
    case SpecConstraintViolation(catId, pred, reason) => lang match
      case Language.En => s"Specification constraint violated for category '${catId.value}': $reason"
      case Language.Cs => s"Porušeno omezení specifikace pro kategorii '${catId.value}': $reason"
    case IncompatibleMaterialPropertyFinish(prop, ft, reason) => lang match
      case Language.En => s"Material property '$prop' is incompatible with finish type '$ft': $reason"
      case Language.Cs => s"Vlastnost materiálu '$prop' je nekompatibilní s typem úpravy '$ft': $reason"
    case IncompatibleMaterialFamilyFinish(family, ft, reason) => lang match
      case Language.En => s"Material family '$family' is incompatible with finish type '$ft': $reason"
      case Language.Cs => s"Rodina materiálů '$family' je nekompatibilní s typem úpravy '$ft': $reason"
    case FinishWeightRequirementNotMet(ft, minGsm, actualGsm, reason) => lang match
      case Language.En => s"Finish type '$ft' requires minimum ${minGsm}gsm (actual: ${actualGsm.map(g => s"${g}gsm").getOrElse("unknown")}): $reason"
      case Language.Cs => s"Typ úpravy '$ft' vyžaduje minimálně ${minGsm}gsm (skutečnost: ${actualGsm.map(g => s"${g}gsm").getOrElse("neznámá")}): $reason"
    case MutuallyExclusiveFinishTypes(ftA, ftB, reason) => lang match
      case Language.En => s"Finish types '$ftA' and '$ftB' are mutually exclusive: $reason"
      case Language.Cs => s"Typy úprav '$ftA' a '$ftB' se vzájemně vylučují: $reason"
    case FinishCategoryLimitExceeded(cat, reason) => lang match
      case Language.En => s"Too many finishes in category '$cat': $reason"
      case Language.Cs => s"Příliš mnoho úprav v kategorii '$cat': $reason"
    case FinishRequiresPrintingProcessViolation(ft, procs, reason) => lang match
      case Language.En => s"Finish type '$ft' requires printing process ${procs.mkString(" or ")}: $reason"
      case Language.Cs => s"Typ úpravy '$ft' vyžaduje tiskový proces ${procs.mkString(" nebo ")}: $reason"
    case PrintingMethodFinishIncompatibleError(pmId, ft, reason) => lang match
      case Language.En => s"Printing method '${pmId.value}' is incompatible with finish type '$ft': $reason"
      case Language.Cs => s"Tisková metoda '${pmId.value}' je nekompatibilní s typem úpravy '$ft': $reason"
    case FinishMissingDependentFinishType(finId, reqFt, reason) => lang match
      case Language.En => s"Finish '${finId.value}' requires a finish of type '$reqFt': $reason"
      case Language.Cs => s"Úprava '${finId.value}' vyžaduje úpravu typu '$reqFt': $reason"
    case InvalidCategoryPrintingMethod(catId, pmId) => lang match
      case Language.En => s"Printing method '${pmId.value}' is not allowed for category '${catId.value}'"
      case Language.Cs => s"Tisková metoda '${pmId.value}' není povolena pro kategorii '${catId.value}'"
    case PrintingMethodNotFound(pmId) => lang match
      case Language.En => s"Printing method '${pmId.value}' not found in catalog"
      case Language.Cs => s"Tisková metoda '${pmId.value}' nebyla nalezena v katalogu"
    case ConfigurationConstraintViolation(catId, reason) => lang match
      case Language.En => s"Configuration constraint violated for category '${catId.value}': $reason"
      case Language.Cs => s"Porušeno omezení konfigurace pro kategorii '${catId.value}': $reason"
    case InkConfigExceedsMethodColorLimit(pmId, inkConfig, maxAllowed) => lang match
      case Language.En => s"Ink configuration '${inkConfig.notation}' exceeds printing method '${pmId.value}' maximum of $maxAllowed colors per side"
      case Language.Cs => s"Konfigurace inkoustu '${inkConfig.notation}' překračuje maximum $maxAllowed barev na stranu pro tiskovou metodu '${pmId.value}'"
    case MissingRequiredComponent(catId, role) => lang match
      case Language.En => s"Category '${catId.value}' requires a '${role}' component"
      case Language.Cs => s"Kategorie '${catId.value}' vyžaduje komponentu '${role}'"
    case InvalidComponentMaterial(catId, role, matId) => lang match
      case Language.En => s"Material '${matId.value}' is not allowed for '${role}' component in category '${catId.value}'"
      case Language.Cs => s"Materiál '${matId.value}' není povolen pro komponentu '${role}' v kategorii '${catId.value}'"
    case MissingComponentInkConfig(role) => lang match
      case Language.En => s"Ink configuration is required for '${role}' component"
      case Language.Cs => s"Konfigurace inkoustu je vyžadována pro komponentu '${role}'"
    case ComponentInkConfigExceedsMethodColorLimit(role, pmId, inkConfig, maxAllowed) => lang match
      case Language.En => s"Ink configuration '${inkConfig.notation}' for '${role}' component exceeds printing method '${pmId.value}' maximum of $maxAllowed colors per side"
      case Language.Cs => s"Konfigurace inkoustu '${inkConfig.notation}' pro komponentu '${role}' překračuje maximum $maxAllowed barev na stranu pro tiskovou metodu '${pmId.value}'"
