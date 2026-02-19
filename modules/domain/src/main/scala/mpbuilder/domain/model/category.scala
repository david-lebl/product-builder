package mpbuilder.domain.model

enum SpecKind:
  case Size, Quantity, InkConfig, Orientation, Bleed, Pages, FoldType, BindingMethod

final case class ProductCategory(
    id: CategoryId,
    name: LocalizedString,
    allowedMaterialIds: Set[MaterialId],
    allowedFinishIds: Set[FinishId],
    requiredSpecKinds: Set[SpecKind],
    allowedPrintingMethodIds: Set[PrintingMethodId],
    componentRoles: Set[ComponentRole] = Set.empty,
    allowedMaterialIdsByRole: Map[ComponentRole, Set[MaterialId]] = Map.empty,
)

object ProductCategory:
  def isMultiComponent(category: ProductCategory): Boolean =
    category.componentRoles.nonEmpty

