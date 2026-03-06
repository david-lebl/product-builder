package mpbuilder.ui.catalog

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.sample.{SampleCatalog, SamplePricelist}
import mpbuilder.domain.json.DomainCodecs.given
import zio.json.*

/** Reactive state management for the catalog editor.
  * Uses Laminar Var/Signal for reactive UI updates.
  */
object CatalogEditorViewModel:

  val stateVar: Var[CatalogEditorState] = Var(initialState)

  val state: Signal[CatalogEditorState] = stateVar.signal

  private def initialState: CatalogEditorState =
    val catalog = SampleCatalog.catalog
    val pricelist = SamplePricelist.pricelist
    CatalogEditorState(
      materials = catalog.materials.values.toList.map(MaterialEditState.fromMaterial),
      finishes = catalog.finishes.values.toList.map(FinishEditState.fromFinish),
      printingMethods = catalog.printingMethods.values.toList.map(PrintingMethodEditState.fromPrintingMethod),
      categories = catalog.categories.values.toList.map(CategoryEditState.fromCategory),
      pricelist = PricelistEditState.fromPricelist(pricelist),
      activeSection = CatalogSection.Materials,
      editingItemIndex = None,
      message = None,
    )

  // ── Navigation ─────────────────────────────────────────────────

  def setActiveSection(section: CatalogSection): Unit =
    stateVar.update(_.copy(activeSection = section, editingItemIndex = None, message = None))

  def setEditingIndex(idx: Option[Int]): Unit =
    stateVar.update(_.copy(editingItemIndex = idx))

  def clearMessage(): Unit =
    stateVar.update(_.copy(message = None))

  // ── Materials ──────────────────────────────────────────────────

  def addMaterial(): Unit =
    stateVar.update { s =>
      val updated = s.materials :+ MaterialEditState.empty
      s.copy(materials = updated, editingItemIndex = Some(updated.length - 1))
    }

  def updateMaterial(idx: Int, mat: MaterialEditState): Unit =
    stateVar.update { s =>
      s.copy(materials = s.materials.updated(idx, mat))
    }

  def removeMaterial(idx: Int): Unit =
    stateVar.update { s =>
      val updated = s.materials.patch(idx, Nil, 1)
      s.copy(materials = updated, editingItemIndex = None)
    }

  // ── Finishes ───────────────────────────────────────────────────

  def addFinish(): Unit =
    stateVar.update { s =>
      val updated = s.finishes :+ FinishEditState.empty
      s.copy(finishes = updated, editingItemIndex = Some(updated.length - 1))
    }

  def updateFinish(idx: Int, fin: FinishEditState): Unit =
    stateVar.update { s =>
      s.copy(finishes = s.finishes.updated(idx, fin))
    }

  def removeFinish(idx: Int): Unit =
    stateVar.update { s =>
      s.copy(finishes = s.finishes.patch(idx, Nil, 1), editingItemIndex = None)
    }

  // ── Printing Methods ───────────────────────────────────────────

  def addPrintingMethod(): Unit =
    stateVar.update { s =>
      val updated = s.printingMethods :+ PrintingMethodEditState.empty
      s.copy(printingMethods = updated, editingItemIndex = Some(updated.length - 1))
    }

  def updatePrintingMethod(idx: Int, pm: PrintingMethodEditState): Unit =
    stateVar.update { s =>
      s.copy(printingMethods = s.printingMethods.updated(idx, pm))
    }

  def removePrintingMethod(idx: Int): Unit =
    stateVar.update { s =>
      s.copy(printingMethods = s.printingMethods.patch(idx, Nil, 1), editingItemIndex = None)
    }

  // ── Categories ─────────────────────────────────────────────────

  def addCategory(): Unit =
    stateVar.update { s =>
      val updated = s.categories :+ CategoryEditState.empty
      s.copy(categories = updated, editingItemIndex = Some(updated.length - 1))
    }

  def updateCategory(idx: Int, cat: CategoryEditState): Unit =
    stateVar.update { s =>
      s.copy(categories = s.categories.updated(idx, cat))
    }

  def removeCategory(idx: Int): Unit =
    stateVar.update { s =>
      s.copy(categories = s.categories.patch(idx, Nil, 1), editingItemIndex = None)
    }

  // ── Pricelist ──────────────────────────────────────────────────

  def updatePricelist(pl: PricelistEditState): Unit =
    stateVar.update(_.copy(pricelist = pl))

  def addPricingRule(ruleType: String): Unit =
    stateVar.update { s =>
      val newRule = PricingRuleEditState.empty(ruleType)
      val updated = s.pricelist.copy(rules = s.pricelist.rules :+ newRule)
      s.copy(pricelist = updated)
    }

  def updatePricingRule(idx: Int, rule: PricingRuleEditState): Unit =
    stateVar.update { s =>
      val updated = s.pricelist.copy(rules = s.pricelist.rules.updated(idx, rule))
      s.copy(pricelist = updated)
    }

  def removePricingRule(idx: Int): Unit =
    stateVar.update { s =>
      val updated = s.pricelist.copy(rules = s.pricelist.rules.patch(idx, Nil, 1))
      s.copy(pricelist = updated)
    }

  // ── Build domain objects from state ────────────────────────────

  def buildCatalog(): ProductCatalog =
    val s = stateVar.now()
    val materials = s.materials.filter(_.id.nonEmpty).map(MaterialEditState.toMaterial)
    val finishes = s.finishes.filter(_.id.nonEmpty).map(FinishEditState.toFinish)
    val methods = s.printingMethods.filter(_.id.nonEmpty).map(PrintingMethodEditState.toPrintingMethod)
    val categories = s.categories.filter(_.id.nonEmpty).map(CategoryEditState.toCategory)
    ProductCatalog(
      categories = categories.map(c => c.id -> c).toMap,
      materials = materials.map(m => m.id -> m).toMap,
      finishes = finishes.map(f => f.id -> f).toMap,
      printingMethods = methods.map(pm => pm.id -> pm).toMap,
    )

  def buildPricelist(): Pricelist =
    PricelistEditState.toPricelist(stateVar.now().pricelist)

  // ── JSON Export / Import ───────────────────────────────────────

  def exportCatalogJson(): String =
    buildCatalog().toJson

  def exportPricelistJson(): String =
    buildPricelist().toJson

  def importCatalogJson(json: String): Unit =
    json.fromJson[ProductCatalog] match
      case Right(catalog) =>
        stateVar.update { s =>
          s.copy(
            materials = catalog.materials.values.toList.map(MaterialEditState.fromMaterial),
            finishes = catalog.finishes.values.toList.map(FinishEditState.fromFinish),
            printingMethods = catalog.printingMethods.values.toList.map(PrintingMethodEditState.fromPrintingMethod),
            categories = catalog.categories.values.toList.map(CategoryEditState.fromCategory),
            message = Some("✓ Catalog imported successfully"),
            editingItemIndex = None,
          )
        }
      case Left(err) =>
        stateVar.update(_.copy(message = Some(s"✗ Import error: $err")))

  def importPricelistJson(json: String): Unit =
    json.fromJson[Pricelist] match
      case Right(pl) =>
        stateVar.update { s =>
          s.copy(
            pricelist = PricelistEditState.fromPricelist(pl),
            message = Some("✓ Pricelist imported successfully"),
          )
        }
      case Left(err) =>
        stateVar.update(_.copy(message = Some(s"✗ Import error: $err")))

  /** Load the sample catalog from SampleCatalog. */
  def loadSampleCatalog(): Unit =
    val catalog = SampleCatalog.catalog
    stateVar.update { s =>
      s.copy(
        materials = catalog.materials.values.toList.map(MaterialEditState.fromMaterial),
        finishes = catalog.finishes.values.toList.map(FinishEditState.fromFinish),
        printingMethods = catalog.printingMethods.values.toList.map(PrintingMethodEditState.fromPrintingMethod),
        categories = catalog.categories.values.toList.map(CategoryEditState.fromCategory),
        message = Some("✓ Sample catalog loaded"),
        editingItemIndex = None,
      )
    }

  /** Load the sample pricelist. */
  def loadSamplePricelist(): Unit =
    val pl = SamplePricelist.pricelist
    stateVar.update { s =>
      s.copy(
        pricelist = PricelistEditState.fromPricelist(pl),
        message = Some("✓ Sample pricelist loaded"),
      )
    }

  /** Clear everything to start from scratch. */
  def clearAll(): Unit =
    stateVar.update { s =>
      s.copy(
        materials = Nil,
        finishes = Nil,
        printingMethods = Nil,
        categories = Nil,
        pricelist = PricelistEditState.empty,
        editingItemIndex = None,
        message = Some("Catalog cleared"),
      )
    }
