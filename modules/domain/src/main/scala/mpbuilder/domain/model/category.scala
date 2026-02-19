package mpbuilder.domain.model

enum SpecKind:
  case Size, Quantity, Orientation, Bleed, Pages, FoldType, BindingMethod

final case class ProductCategory(
    id: CategoryId,
    name: LocalizedString,
    components: List[ComponentTemplate],
    requiredSpecKinds: Set[SpecKind],
    allowedPrintingMethodIds: Set[PrintingMethodId],
)

object ProductCategory:
  extension (cat: ProductCategory)
    def componentFor(role: ComponentRole): Option[ComponentTemplate] =
      cat.components.find(_.role == role)

    def isMultiComponent: Boolean =
      cat.components.exists(_.role != ComponentRole.Main)

    def allAllowedMaterialIds: Set[MaterialId] =
      cat.components.flatMap(_.allowedMaterialIds).toSet

    def allAllowedFinishIds: Set[FinishId] =
      cat.components.flatMap(_.allowedFinishIds).toSet
