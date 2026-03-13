package mpbuilder.domain.sample

import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*

/** Sample production cost data for testing and demonstration.
  *
  * Costs are set below the selling prices in `SamplePricelist` to produce
  * realistic margin analysis. The overhead factor represents general business
  * overhead (utilities, rent, equipment depreciation, etc.).
  */
object SampleProductionCosts:

  val costSheet: ProductionCostSheet = ProductionCostSheet(
    rules = List(
      // --- Material unit costs (per unit, USD) ---
      // Typically 40-60% of selling price for paper/cardboard
      ProductionCostRule.MaterialUnitCost(SampleCatalog.coated300gsmId, Money("0.05")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.uncoatedBondId, Money("0.02")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.kraftId, Money("0.04")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.corrugatedId, Money("0.10")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.coatedSilk250gsmId, Money("0.05")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.yupoId, Money("0.09")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.adhesiveStockId, Money("0.06")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.cottonId, Money("0.12")),

      // --- Material area costs (per sqm, USD) ---
      ProductionCostRule.MaterialAreaCost(SampleCatalog.vinylId, Money("8.00")),
      ProductionCostRule.MaterialAreaCost(SampleCatalog.clearVinylId, Money("10.00")),

      // --- Roll-Up material costs ---
      ProductionCostRule.MaterialAreaCost(SampleCatalog.rollUpBannerFilmId, Money("5.00")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.rollUpStandEconomyId, Money("12.00")),
      ProductionCostRule.MaterialUnitCost(SampleCatalog.rollUpStandPremiumId, Money("28.00")),

      // --- Process costs (per unit, USD) ---
      ProductionCostRule.ProcessCost(PrintingProcessType.Offset, Money("0.01")),
      ProductionCostRule.ProcessCost(PrintingProcessType.Digital, Money("0.02")),
      ProductionCostRule.ProcessCost(PrintingProcessType.Letterpress, Money("0.08")),
      ProductionCostRule.ProcessCost(PrintingProcessType.UVCurableInkjet, Money("0.03")),

      // --- Finish costs (per unit, USD) ---
      ProductionCostRule.FinishCost(SampleCatalog.matteLaminationId, Money("0.01")),
      ProductionCostRule.FinishCost(SampleCatalog.glossLaminationId, Money("0.01")),
      ProductionCostRule.FinishCost(SampleCatalog.softTouchCoatingId, Money("0.02")),
      ProductionCostRule.FinishCost(SampleCatalog.embossingId, Money("0.03")),
      ProductionCostRule.FinishCost(SampleCatalog.debossingId, Money("0.03")),
      ProductionCostRule.FinishCost(SampleCatalog.foilStampingId, Money("0.07")),
      ProductionCostRule.FinishCost(SampleCatalog.dieCutId, Money("0.04")),
      ProductionCostRule.FinishCost(SampleCatalog.uvCoatingId, Money("0.02")),

      // --- Overhead factor: 15% on top of direct costs ---
      ProductionCostRule.OverheadFactor(BigDecimal("1.15")),
    ),
    currency = Currency.USD,
  )

  /** Cost sheet without overhead — useful for testing overhead impact. */
  val costSheetNoOverhead: ProductionCostSheet = ProductionCostSheet(
    rules = costSheet.rules.filterNot(_.isInstanceOf[ProductionCostRule.OverheadFactor]),
    currency = Currency.USD,
  )
