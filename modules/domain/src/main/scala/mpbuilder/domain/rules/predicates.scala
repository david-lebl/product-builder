package mpbuilder.domain.rules

import mpbuilder.domain.model.*

enum SpecPredicate:
  case MinDimension(minWidthMm: Double, minHeightMm: Double)
  case MaxDimension(maxWidthMm: Double, maxHeightMm: Double)
  case MinQuantity(min: Int)
  case MaxQuantity(max: Int)
  case AllowedBindingMethods(methods: Set[BindingMethod])
  case AllowedFoldTypes(foldTypes: Set[FoldType])
  case MinPages(min: Int)
  case MaxPages(max: Int)
  case PagesDivisibleBy(n: Int)
  case SquareDimension()
  case AllowedDimensions(sizes: Set[(Double, Double)])

enum ConfigurationPredicate:
  case Spec(predicate: SpecPredicate)
  case HasMaterialProperty(property: MaterialProperty)
  case HasMaterialFamily(family: MaterialFamily)
  case HasPrintingProcess(processType: PrintingProcessType)
  case HasMinWeight(minGsm: Int)
  case AllowedInkTypes(inkTypes: Set[InkType])
  case MaxColorCountPerSide(max: Int)
  case BindingMethodIs(methods: Set[BindingMethod])
  case HasInkType(inkType: InkType)
  case HasFinishId(finishId: FinishId)
  /** True when every component's ink configuration is single-sided (back side is None or White). */
  case IsSingleSided
  /** True when the product size spec fits within every component material's sheet dimension.
    * Materials that do not define a sheet dimension are treated as unrestricted and always pass.
    * Both the width and the height of the size spec must be ≤ the corresponding sheet dimension values.
    */
  case SizeWithinMaterialSheet
  case And(left: ConfigurationPredicate, right: ConfigurationPredicate)
  case Or(left: ConfigurationPredicate, right: ConfigurationPredicate)
  case Not(inner: ConfigurationPredicate)
