package mpbuilder.domain.model

enum ComponentRole:
  case Cover, Body

final case class ProductComponent(
    role: ComponentRole,
    material: Material,
    finishes: List[Finish],
    inkConfiguration: Option[InkConfiguration],
)

final case class ComponentRequest(
    role: ComponentRole,
    materialId: MaterialId,
    finishIds: List[FinishId],
    inkConfiguration: Option[InkConfiguration],
)
