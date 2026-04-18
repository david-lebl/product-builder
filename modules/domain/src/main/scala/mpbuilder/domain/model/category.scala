package mpbuilder.domain.model

enum SpecKind:
  case Size, Quantity, Orientation, Bleed, Pages, FoldType, BindingMethod, BindingColor, ManufacturingSpeed

final case class ProductCategory(
    id: CategoryId,
    name: LocalizedString,
    components: List[ComponentTemplate],
    requiredSpecKinds: Set[SpecKind],
    allowedPrintingMethodIds: Set[PrintingMethodId],
    description: Option[LocalizedString] = None,
    presets: List[CategoryPreset] = List.empty,
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

    /** The first preset (if any) — used as the auto-applied default. */
    def defaultPreset: Option[CategoryPreset] =
      cat.presets.headOption

    /** Look up a preset by id. */
    def presetById(id: PresetId): Option[CategoryPreset] =
      cat.presets.find(_.id == id)
