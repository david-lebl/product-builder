package mpbuilder.domain.rules

import mpbuilder.domain.model.*

enum SpecPredicate:
  case MinDimension(minWidthMm: Double, minHeightMm: Double)
  case MaxDimension(maxWidthMm: Double, maxHeightMm: Double)
  case MinQuantity(min: Int)
  case MaxQuantity(max: Int)
  case AllowedColorModes(modes: Set[ColorMode])
  case AllowedBindingMethods(methods: Set[BindingMethod])
  case AllowedFoldTypes(foldTypes: Set[FoldType])
  case MinPages(min: Int)
  case MaxPages(max: Int)

enum ConfigurationPredicate:
  case Spec(predicate: SpecPredicate)
  case HasMaterialProperty(property: MaterialProperty)
  case HasMaterialFamily(family: MaterialFamily)
  case HasPrintingProcess(processType: PrintingProcessType)
  case HasMinWeight(minGsm: Int)
  case And(left: ConfigurationPredicate, right: ConfigurationPredicate)
  case Or(left: ConfigurationPredicate, right: ConfigurationPredicate)
  case Not(inner: ConfigurationPredicate)
