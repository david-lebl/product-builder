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
    finishes: List[SelectedFinish],
    sheetCount: Int,
)

final case class FinishSelection(
    finishId: FinishId,
    params: Option[FinishParameters] = None,
)

final case class ComponentRequest(
    role: ComponentRole,
    materialId: MaterialId,
    inkConfiguration: InkConfiguration,
    finishes: List[FinishSelection],
)
