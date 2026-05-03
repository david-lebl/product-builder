package mpbuilder.domain.sample

import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*

/** Sample production cost rules for testing and demos.
  *
  * Cost values are set below the corresponding selling prices in `SamplePricelist`
  * to give realistic margins. For example, coated 300gsm sells at $0.12/unit and
  * costs $0.05/unit to produce (before overhead).
  */
object SampleProductionCosts:

  val costSheet: ProductionCostSheet = ProductionCostSheet(
    rules = List(
      // --- Material unit costs (per printed piece) ---
      ProductionCostRule.MaterialUnitCost(SampleCatalog.coated300gsmId, Money("0.05")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.uncoatedBondId, Money("0.02")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.kraftId, Money("0.04")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.corrugatedId, Money("0.12")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.coatedSilk250gsmId, Money("0.05")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.yupoId, Money("0.09")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.adhesiveStockId, Money("0.06")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.cottonId, Money("0.11")),

      // --- Material area costs (per sqm) ---
      ProductionCostRule.MaterialAreaCost(SampleCatalog.vinylId, Money("8.00")),
      ProductionCostRule.MaterialAreaCost(SampleCatalog.clearVinylId, Money("10.00")),

      // --- Process costs (per printed unit) ---
      ProductionCostRule.ProcessCost(PrintingProcessType.Offset, Money("0.01")),
      ProductionCostRule.ProcessCost(PrintingProcessType.Digital, Money("0.02")),
      ProductionCostRule.ProcessCost(PrintingProcessType.UVCurableInkjet, Money("0.03")),
      ProductionCostRule.ProcessCost(PrintingProcessType.Letterpress, Money("0.08")),
      ProductionCostRule.ProcessCost(PrintingProcessType.SolventInkjet, Money("0.04")),
      ProductionCostRule.ProcessCost(PrintingProcessType.LatexInkjet, Money("0.05")),

      // --- Finish costs (per unit) ---
      ProductionCostRule.FinishCost(SampleCatalog.matteLaminationId, Money("0.01")),
      ProductionCostRule.FinishCost(SampleCatalog.glossLaminationId, Money("0.01")),
      ProductionCostRule.FinishCost(SampleCatalog.softTouchCoatingId, Money("0.02")),
      ProductionCostRule.FinishCost(SampleCatalog.embossingId, Money("0.04")),
      ProductionCostRule.FinishCost(SampleCatalog.foilStampingId, Money("0.07")),
      ProductionCostRule.FinishCost(SampleCatalog.dieCutId, Money("0.05")),

      // --- Overhead: 15% on all direct costs ---
      ProductionCostRule.OverheadFactor(BigDecimal("1.15")),
    ),
    currency = Currency.USD,
  )

  /** CZK production cost sheet with proportionally scaled values. */
  val costSheetCzk: ProductionCostSheet = ProductionCostSheet(
    rules = List(
      ProductionCostRule.MaterialUnitCost(SampleCatalog.coated300gsmId, Money("1.20")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.uncoatedBondId, Money("0.50")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.kraftId, Money("1.00")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.corrugatedId, Money("2.80")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.coatedSilk250gsmId, Money("1.10")),

      ProductionCostRule.MaterialAreaCost(SampleCatalog.vinylId, Money("180.00")),
      ProductionCostRule.MaterialAreaCost(SampleCatalog.clearVinylId, Money("225.00")),

      ProductionCostRule.ProcessCost(PrintingProcessType.Offset, Money("0.25")),
      ProductionCostRule.ProcessCost(PrintingProcessType.Digital, Money("0.50")),
      ProductionCostRule.ProcessCost(PrintingProcessType.UVCurableInkjet, Money("0.70")),
      ProductionCostRule.ProcessCost(PrintingProcessType.SolventInkjet, Money("0.60")),
      ProductionCostRule.ProcessCost(PrintingProcessType.LatexInkjet, Money("0.80")),

      ProductionCostRule.FinishCost(SampleCatalog.matteLaminationId, Money("0.25")),
      ProductionCostRule.FinishCost(SampleCatalog.glossLaminationId, Money("0.25")),
      ProductionCostRule.FinishCost(SampleCatalog.embossingId, Money("1.00")),
      ProductionCostRule.FinishCost(SampleCatalog.foilStampingId, Money("1.60")),

      ProductionCostRule.OverheadFactor(BigDecimal("1.15")),
    ),
    currency = Currency.CZK,
  )
