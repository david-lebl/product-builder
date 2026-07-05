package mpbuilder.catalog

import mpbuilder.kernel.*

enum ComponentRole:
  case Main
  case Cover
  case Body
  case Stand

final case class ComponentTemplate(
    role: ComponentRole,
    allowedMaterialIds: Set[MaterialId],
    allowedFinishIds: Set[FinishId],
    optional: Boolean = false,
)

final case class ProductComponent(
    role: ComponentRole,
    material: Material,
    inkConfiguration: InkConfiguration,
    finishes: List[SelectedFinish],
    sheetCount: Int,
)

final case class ComponentRequest(
    role: ComponentRole,
    materialId: MaterialId,
    inkConfiguration: InkConfiguration,
    finishes: List[FinishSelection],
)
