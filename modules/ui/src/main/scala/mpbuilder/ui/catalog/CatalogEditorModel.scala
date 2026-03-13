package mpbuilder.ui.catalog

import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.rules.*
import mpbuilder.domain.sample.*

/** Which section of the catalog editor is currently active. */
enum CatalogSection:
  case Categories, Materials, Finishes, PrintingMethods, Rules, Pricelist, Export

/** Edit state for the catalog editor — tracks which entity is being edited. */
enum EditState:
  case None
  case EditingCategory(id: CategoryId)
  case EditingMaterial(id: MaterialId)
  case EditingFinish(id: FinishId)
  case EditingPrintingMethod(id: PrintingMethodId)
  case EditingRule(index: Int)
  case EditingPricingRule(index: Int)
  case CreatingCategory
  case CreatingMaterial
  case CreatingFinish
  case CreatingPrintingMethod
  case CreatingRule
  case CreatingPricingRule

/** Full state of the catalog editor. */
final case class CatalogEditorState(
  catalog: ProductCatalog,
  ruleset: CompatibilityRuleset,
  pricelists: List[Pricelist],
  activeSection: CatalogSection,
  editState: EditState,
  selectedPricelistIndex: Int,
  importExportJson: String,
  statusMessage: Option[String],
)

object CatalogEditorState:
  def initial: CatalogEditorState = CatalogEditorState(
    catalog = SampleCatalog.catalog,
    ruleset = SampleRules.ruleset,
    pricelists = List(SamplePricelist.pricelistCzk, SamplePricelist.pricelist),
    activeSection = CatalogSection.Categories,
    editState = EditState.None,
    selectedPricelistIndex = 0,
    importExportJson = "",
    statusMessage = None,
  )
