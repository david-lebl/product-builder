package mpbuilder.domain.model

enum ComponentRole:
  case Main
  case Cover
  case Body

final case class ComponentTemplate(
    role: ComponentRole,
    allowedMaterialIds: Set[MaterialId],
    allowedFinishIds: Set[FinishId],
)

final case class ProductComponent(
    role: ComponentRole,
    material: Material,
    inkConfiguration: InkConfiguration,
    finishes: List[Finish],
    sheetCount: Int,
)

final case class ComponentRequest(
    role: ComponentRole,
    materialId: MaterialId,
    inkConfiguration: InkConfiguration,
    finishIds: List[FinishId],
)
