package mpbuilder.catalog

import mpbuilder.kernel.*


enum WeightError:
  case NoSizeInSpecifications
  case NoQuantityInSpecifications
  case NoWeightForMaterial(materialId: MaterialId, role: ComponentRole)

  def message: String = this match
    case NoSizeInSpecifications            => "Size specification is required for weight calculation"
    case NoQuantityInSpecifications        => "Quantity specification is required for weight calculation"
    case NoWeightForMaterial(id, role)     => s"Material ${id.value} (${role}) has no weight defined"
