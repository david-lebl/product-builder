package mpbuilder.ui.catalog

import com.raquo.laminar.api.L.*
import zio.json.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.rules.*
import mpbuilder.domain.sample.*
import mpbuilder.domain.codec.DomainCodecs
import mpbuilder.domain.codec.DomainCodecs.given

/** Reactive state management for the catalog editor.
  *
  * All mutations go through this object. Views subscribe to signals derived from `state`.
  */
object CatalogEditorViewModel:

  private val stateVar: Var[CatalogEditorState] = Var(CatalogEditorState.initial)
  val state: Signal[CatalogEditorState] = stateVar.signal

  // ── Derived signals ────────────────────────────────────────────────────

  val catalog: Signal[ProductCatalog] = state.map(_.catalog)
  val ruleset: Signal[CompatibilityRuleset] = state.map(_.ruleset)
  val activeSection: Signal[CatalogSection] = state.map(_.activeSection)
  val editState: Signal[EditState] = state.map(_.editState)
  val statusMessage: Signal[Option[String]] = state.map(_.statusMessage)

  val categories: Signal[List[ProductCategory]] = catalog.map(_.categories.values.toList.sortBy(_.id.value))
  val materials: Signal[List[Material]] = catalog.map(_.materials.values.toList.sortBy(_.id.value))
  val finishes: Signal[List[Finish]] = catalog.map(_.finishes.values.toList.sortBy(_.id.value))
  val printingMethods: Signal[List[PrintingMethod]] = catalog.map(_.printingMethods.values.toList.sortBy(_.id.value))
  val rules: Signal[List[CompatibilityRule]] = ruleset.map(_.rules)

  val currentPricelist: Signal[Option[Pricelist]] = state.map { s =>
    s.pricelists.lift(s.selectedPricelistIndex)
  }

  val pricingRules: Signal[List[PricingRule]] = currentPricelist.map(_.map(_.rules).getOrElse(Nil))

  // ── Navigation ─────────────────────────────────────────────────────────

  def setSection(section: CatalogSection): Unit =
    stateVar.update(_.copy(activeSection = section, editState = EditState.None))

  def setEditState(es: EditState): Unit =
    stateVar.update(_.copy(editState = es))

  def clearStatus(): Unit =
    stateVar.update(_.copy(statusMessage = None))

  // ── Load sample data ──────────────────────────────────────────────────

  def loadSampleData(): Unit =
    stateVar.update(_.copy(
      catalog = SampleCatalog.catalog,
      ruleset = SampleRules.ruleset,
      pricelists = List(SamplePricelist.pricelist, SamplePricelist.pricelistCzk),
      statusMessage = Some("Sample data loaded successfully"),
    ))

  // ── Category CRUD ──────────────────────────────────────────────────────

  def addCategory(cat: ProductCategory): Unit =
    stateVar.update { s =>
      s.copy(
        catalog = s.catalog.copy(categories = s.catalog.categories + (cat.id -> cat)),
        editState = EditState.None,
        statusMessage = Some(s"Category '${cat.name.value}' added"),
      )
    }

  def updateCategory(cat: ProductCategory): Unit =
    stateVar.update { s =>
      s.copy(
        catalog = s.catalog.copy(categories = s.catalog.categories.updated(cat.id, cat)),
        editState = EditState.None,
        statusMessage = Some(s"Category '${cat.name.value}' updated"),
      )
    }

  def removeCategory(id: CategoryId): Unit =
    stateVar.update { s =>
      s.copy(
        catalog = s.catalog.copy(categories = s.catalog.categories - id),
        editState = EditState.None,
        statusMessage = Some("Category removed"),
      )
    }

  // ── Material CRUD ─────────────────────────────────────────────────────

  def addMaterial(mat: Material): Unit =
    stateVar.update { s =>
      s.copy(
        catalog = s.catalog.copy(materials = s.catalog.materials + (mat.id -> mat)),
        editState = EditState.None,
        statusMessage = Some(s"Material '${mat.name.value}' added"),
      )
    }

  def updateMaterial(mat: Material): Unit =
    stateVar.update { s =>
      s.copy(
        catalog = s.catalog.copy(materials = s.catalog.materials.updated(mat.id, mat)),
        editState = EditState.None,
        statusMessage = Some(s"Material '${mat.name.value}' updated"),
      )
    }

  def removeMaterial(id: MaterialId): Unit =
    stateVar.update { s =>
      s.copy(
        catalog = s.catalog.copy(materials = s.catalog.materials - id),
        editState = EditState.None,
        statusMessage = Some("Material removed"),
      )
    }

  // ── Finish CRUD ───────────────────────────────────────────────────────

  def addFinish(fin: Finish): Unit =
    stateVar.update { s =>
      s.copy(
        catalog = s.catalog.copy(finishes = s.catalog.finishes + (fin.id -> fin)),
        editState = EditState.None,
        statusMessage = Some(s"Finish '${fin.name.value}' added"),
      )
    }

  def updateFinish(fin: Finish): Unit =
    stateVar.update { s =>
      s.copy(
        catalog = s.catalog.copy(finishes = s.catalog.finishes.updated(fin.id, fin)),
        editState = EditState.None,
        statusMessage = Some(s"Finish '${fin.name.value}' updated"),
      )
    }

  def removeFinish(id: FinishId): Unit =
    stateVar.update { s =>
      s.copy(
        catalog = s.catalog.copy(finishes = s.catalog.finishes - id),
        editState = EditState.None,
        statusMessage = Some("Finish removed"),
      )
    }

  // ── PrintingMethod CRUD ───────────────────────────────────────────────

  def addPrintingMethod(pm: PrintingMethod): Unit =
    stateVar.update { s =>
      s.copy(
        catalog = s.catalog.copy(printingMethods = s.catalog.printingMethods + (pm.id -> pm)),
        editState = EditState.None,
        statusMessage = Some(s"Printing method '${pm.name.value}' added"),
      )
    }

  def updatePrintingMethod(pm: PrintingMethod): Unit =
    stateVar.update { s =>
      s.copy(
        catalog = s.catalog.copy(printingMethods = s.catalog.printingMethods.updated(pm.id, pm)),
        editState = EditState.None,
        statusMessage = Some(s"Printing method '${pm.name.value}' updated"),
      )
    }

  def removePrintingMethod(id: PrintingMethodId): Unit =
    stateVar.update { s =>
      s.copy(
        catalog = s.catalog.copy(printingMethods = s.catalog.printingMethods - id),
        editState = EditState.None,
        statusMessage = Some("Printing method removed"),
      )
    }

  // ── Rules CRUD ────────────────────────────────────────────────────────

  def addRule(rule: CompatibilityRule): Unit =
    stateVar.update { s =>
      s.copy(
        ruleset = s.ruleset.copy(rules = s.ruleset.rules :+ rule),
        editState = EditState.None,
        statusMessage = Some("Rule added"),
      )
    }

  def updateRule(index: Int, rule: CompatibilityRule): Unit =
    stateVar.update { s =>
      s.copy(
        ruleset = s.ruleset.copy(rules = s.ruleset.rules.updated(index, rule)),
        editState = EditState.None,
        statusMessage = Some("Rule updated"),
      )
    }

  def removeRule(index: Int): Unit =
    stateVar.update { s =>
      val updated = s.ruleset.rules.zipWithIndex.filterNot(_._2 == index).map(_._1)
      s.copy(
        ruleset = s.ruleset.copy(rules = updated),
        editState = EditState.None,
        statusMessage = Some("Rule removed"),
      )
    }

  // ── Pricelist management ──────────────────────────────────────────────

  def selectPricelist(index: Int): Unit =
    stateVar.update(_.copy(selectedPricelistIndex = index, editState = EditState.None))

  def addPricingRule(rule: PricingRule): Unit =
    stateVar.update { s =>
      val pl = s.pricelists(s.selectedPricelistIndex)
      val updated = pl.copy(rules = pl.rules :+ rule)
      s.copy(
        pricelists = s.pricelists.updated(s.selectedPricelistIndex, updated),
        editState = EditState.None,
        statusMessage = Some("Pricing rule added"),
      )
    }

  def updatePricingRule(index: Int, rule: PricingRule): Unit =
    stateVar.update { s =>
      val pl = s.pricelists(s.selectedPricelistIndex)
      val updated = pl.copy(rules = pl.rules.updated(index, rule))
      s.copy(
        pricelists = s.pricelists.updated(s.selectedPricelistIndex, updated),
        editState = EditState.None,
        statusMessage = Some("Pricing rule updated"),
      )
    }

  def removePricingRule(index: Int): Unit =
    stateVar.update { s =>
      val pl = s.pricelists(s.selectedPricelistIndex)
      val updated = pl.copy(rules = pl.rules.zipWithIndex.filterNot(_._2 == index).map(_._1))
      s.copy(
        pricelists = s.pricelists.updated(s.selectedPricelistIndex, updated),
        editState = EditState.None,
        statusMessage = Some("Pricing rule removed"),
      )
    }

  def addPricelist(currency: Currency): Unit =
    stateVar.update { s =>
      val newPl = Pricelist(Nil, currency, "1.0")
      s.copy(
        pricelists = s.pricelists :+ newPl,
        selectedPricelistIndex = s.pricelists.size,
        statusMessage = Some(s"New ${currency} pricelist added"),
      )
    }

  // ── JSON Import/Export ────────────────────────────────────────────────

  def exportJson(): String =
    val s = stateVar.now()
    val export_ = DomainCodecs.CatalogExport(s.catalog, s.ruleset, s.pricelists)
    export_.toJsonPretty

  def importJson(json: String): Unit =
    json.fromJson[DomainCodecs.CatalogExport] match
      case Right(export_) =>
        stateVar.update(_.copy(
          catalog = export_.catalog,
          ruleset = export_.ruleset,
          pricelists = export_.pricelists,
          selectedPricelistIndex = 0,
          editState = EditState.None,
          statusMessage = Some(s"Imported: ${export_.catalog.categories.size} categories, ${export_.catalog.materials.size} materials, ${export_.catalog.finishes.size} finishes, ${export_.pricelists.size} pricelists"),
        ))
      case Left(error) =>
        stateVar.update(_.copy(
          statusMessage = Some(s"Import failed: $error"),
        ))
