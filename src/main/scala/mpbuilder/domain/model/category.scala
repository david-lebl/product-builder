package mpbuilder.domain.model

enum SpecKind:
  case Size, Quantity, ColorMode, Orientation, Bleed, Pages, FoldType, BindingMethod

final case class ProductCategory(
    id: CategoryId,
    name: LocalizedString,
    allowedMaterialIds: Set[MaterialId],
    allowedFinishIds: Set[FinishId],
    requiredSpecKinds: Set[SpecKind],
    allowedPrintingMethodIds: Set[PrintingMethodId],
)
