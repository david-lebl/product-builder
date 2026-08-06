package mpbuilder.catalog

import mpbuilder.catalog.ComponentRole

final case class ComponentWeightBreakdown(
    role: ComponentRole,
    materialName: String,
    gsmWeight: Int,
    sheetsPerItem: Int,
    sheetAreaM2: Double,
    weightPerItemG: Double,
    totalWeightG: Double,
)

final case class WeightBreakdown(
    componentBreakdowns: List[ComponentWeightBreakdown],
    weightPerItemG: Double,
    quantity: Int,
    totalWeightG: Double,
    totalWeightKg: Double,
)
