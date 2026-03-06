package mpbuilder.ui.catalog

import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.L
import org.scalajs.dom
import mpbuilder.domain.model.*

/** Main catalog editor application component.
  * Contains navigation sidebar and section content area.
  */
object CatalogEditorApp:

  def apply(): Element =
    val vm = CatalogEditorViewModel

    div(
      cls := "catalog-editor",

      // Sidebar navigation
      div(
        cls := "catalog-sidebar",
        h3("Catalog Editor"),
        CatalogSection.values.map { section =>
          button(
            cls <-- vm.state.map(s =>
              if s.activeSection == section then "catalog-nav-item active"
              else "catalog-nav-item"
            ),
            sectionLabel(section),
            onClick --> { _ => vm.setActiveSection(section) },
          )
        }.toSeq,
      ),

      // Main content area
      div(
        cls := "catalog-main",

        // Message banner
        child <-- vm.state.map { s =>
          s.message match
            case Some(msg) =>
              div(
                cls := "catalog-message",
                span(msg),
                button(
                  cls := "catalog-message-close",
                  "×",
                  onClick --> { _ => vm.clearMessage() },
                ),
              )
            case None => emptyNode
        },

        // Section content
        child <-- vm.state.map { s =>
          s.activeSection match
            case CatalogSection.Materials       => MaterialsEditor()
            case CatalogSection.Finishes        => FinishesEditor()
            case CatalogSection.PrintingMethods => PrintingMethodsEditor()
            case CatalogSection.Categories      => CategoriesEditor()
            case CatalogSection.Pricelist       => PricelistEditor()
            case CatalogSection.ImportExport    => ImportExportEditor()
        },
      ),
    )

  private def sectionLabel(section: CatalogSection): String = section match
    case CatalogSection.Materials       => "📦 Materials"
    case CatalogSection.Finishes        => "✨ Finishes"
    case CatalogSection.PrintingMethods => "🖨 Methods"
    case CatalogSection.Categories      => "📁 Categories"
    case CatalogSection.Pricelist       => "💰 Pricelist"
    case CatalogSection.ImportExport    => "📋 Import / Export"


/** Materials list & editor. */
object MaterialsEditor:
  import FormComponents.*

  def apply(): Element =
    val vm = CatalogEditorViewModel
    div(
      cls := "catalog-section",
      sectionHeader("Materials",
        actionLabel = Some("+ Add Material"),
        onAction = () => vm.addMaterial()),

      // List of materials
      div(
        cls := "catalog-item-list",
        children <-- vm.state.map { s =>
          s.materials.zipWithIndex.map { case (mat, idx) =>
            div(
              cls <-- vm.state.map(st =>
                if st.editingItemIndex.contains(idx) then "catalog-item-row selected"
                else "catalog-item-row"
              ),
              div(
                cls := "catalog-item-summary",
                strong(if mat.id.nonEmpty then mat.id else "(new)"),
                span(s" — ${mat.nameEn}"),
                span(cls := "catalog-item-meta", s" [${mat.family}${mat.weight.map(w => s" ${w}gsm").getOrElse("")}]"),
              ),
              div(
                cls := "catalog-item-actions",
                button(cls := "catalog-btn catalog-btn-small", "Edit",
                  onClick --> { _ => vm.setEditingIndex(Some(idx)) }),
                button(cls := "catalog-btn catalog-btn-small catalog-btn-danger", "×",
                  onClick --> { _ => vm.removeMaterial(idx) }),
              ),
            )
          }
        },
      ),

      // Edit form for selected material
      child <-- vm.state.map { s =>
        s.editingItemIndex.flatMap(idx => s.materials.lift(idx).map(idx -> _)) match
          case Some((idx, mat)) => materialForm(idx, mat)
          case None => emptyNode
      },
    )

  private def materialForm(idx: Int, initial: MaterialEditState): Element =
    val vm = CatalogEditorViewModel
    val matVar = Var(initial)
    def update(fn: MaterialEditState => MaterialEditState): Unit =
      matVar.update(fn)
      vm.updateMaterial(idx, matVar.now())

    div(
      cls := "catalog-edit-form card",
      h3("Edit Material"),
      textField("ID:", matVar.signal.map(_.id), v => update(_.copy(id = v)), "e.g. mat-coated-300gsm"),
      localizedStringEditor("Name:",
        matVar.signal.map(_.nameEn), matVar.signal.map(_.nameCs),
        v => update(_.copy(nameEn = v)), v => update(_.copy(nameCs = v))),
      enumSelectRequired("Family:", MaterialFamily.values, matVar.signal.map(_.family),
        v => update(_.copy(family = v))),
      optionalNumberField("Weight (gsm):", matVar.signal.map(_.weight),
        v => update(_.copy(weight = v)), "None"),
      enumCheckboxSet("Properties:", MaterialProperty.values, matVar.signal.map(_.properties),
        v => update(_.copy(properties = v))),
    )


/** Finishes list & editor. */
object FinishesEditor:
  import FormComponents.*

  def apply(): Element =
    val vm = CatalogEditorViewModel
    div(
      cls := "catalog-section",
      sectionHeader("Finishes",
        actionLabel = Some("+ Add Finish"),
        onAction = () => vm.addFinish()),

      div(
        cls := "catalog-item-list",
        children <-- vm.state.map { s =>
          s.finishes.zipWithIndex.map { case (fin, idx) =>
            div(
              cls <-- vm.state.map(st =>
                if st.editingItemIndex.contains(idx) then "catalog-item-row selected"
                else "catalog-item-row"
              ),
              div(
                cls := "catalog-item-summary",
                strong(if fin.id.nonEmpty then fin.id else "(new)"),
                span(s" — ${fin.nameEn}"),
                span(cls := "catalog-item-meta", s" [${fin.finishType}, ${fin.side}]"),
              ),
              div(
                cls := "catalog-item-actions",
                button(cls := "catalog-btn catalog-btn-small", "Edit",
                  onClick --> { _ => vm.setEditingIndex(Some(idx)) }),
                button(cls := "catalog-btn catalog-btn-small catalog-btn-danger", "×",
                  onClick --> { _ => vm.removeFinish(idx) }),
              ),
            )
          }
        },
      ),

      child <-- vm.state.map { s =>
        s.editingItemIndex.flatMap(idx => s.finishes.lift(idx).map(idx -> _)) match
          case Some((idx, fin)) => finishForm(idx, fin)
          case None => emptyNode
      },
    )

  private def finishForm(idx: Int, initial: FinishEditState): Element =
    val vm = CatalogEditorViewModel
    val finVar = Var(initial)
    def update(fn: FinishEditState => FinishEditState): Unit =
      finVar.update(fn)
      vm.updateFinish(idx, finVar.now())

    div(
      cls := "catalog-edit-form card",
      h3("Edit Finish"),
      textField("ID:", finVar.signal.map(_.id), v => update(_.copy(id = v)), "e.g. fin-matte-lam"),
      localizedStringEditor("Name:",
        finVar.signal.map(_.nameEn), finVar.signal.map(_.nameCs),
        v => update(_.copy(nameEn = v)), v => update(_.copy(nameCs = v))),
      enumSelectRequired("Finish Type:", FinishType.values, finVar.signal.map(_.finishType),
        v => update(_.copy(finishType = v))),
      enumSelectRequired("Side:", FinishSide.values, finVar.signal.map(_.side),
        v => update(_.copy(side = v))),
    )


/** Printing Methods list & editor. */
object PrintingMethodsEditor:
  import FormComponents.*

  def apply(): Element =
    val vm = CatalogEditorViewModel
    div(
      cls := "catalog-section",
      sectionHeader("Printing Methods",
        actionLabel = Some("+ Add Method"),
        onAction = () => vm.addPrintingMethod()),

      div(
        cls := "catalog-item-list",
        children <-- vm.state.map { s =>
          s.printingMethods.zipWithIndex.map { case (pm, idx) =>
            div(
              cls <-- vm.state.map(st =>
                if st.editingItemIndex.contains(idx) then "catalog-item-row selected"
                else "catalog-item-row"
              ),
              div(
                cls := "catalog-item-summary",
                strong(if pm.id.nonEmpty then pm.id else "(new)"),
                span(s" — ${pm.nameEn}"),
                span(cls := "catalog-item-meta", s" [${pm.processType}${pm.maxColorCount.map(c => s", max $c colors").getOrElse("")}]"),
              ),
              div(
                cls := "catalog-item-actions",
                button(cls := "catalog-btn catalog-btn-small", "Edit",
                  onClick --> { _ => vm.setEditingIndex(Some(idx)) }),
                button(cls := "catalog-btn catalog-btn-small catalog-btn-danger", "×",
                  onClick --> { _ => vm.removePrintingMethod(idx) }),
              ),
            )
          }
        },
      ),

      child <-- vm.state.map { s =>
        s.editingItemIndex.flatMap(idx => s.printingMethods.lift(idx).map(idx -> _)) match
          case Some((idx, pm)) => printingMethodForm(idx, pm)
          case None => emptyNode
      },
    )

  private def printingMethodForm(idx: Int, initial: PrintingMethodEditState): Element =
    val vm = CatalogEditorViewModel
    val pmVar = Var(initial)
    def update(fn: PrintingMethodEditState => PrintingMethodEditState): Unit =
      pmVar.update(fn)
      vm.updatePrintingMethod(idx, pmVar.now())

    div(
      cls := "catalog-edit-form card",
      h3("Edit Printing Method"),
      textField("ID:", pmVar.signal.map(_.id), v => update(_.copy(id = v)), "e.g. pm-digital"),
      localizedStringEditor("Name:",
        pmVar.signal.map(_.nameEn), pmVar.signal.map(_.nameCs),
        v => update(_.copy(nameEn = v)), v => update(_.copy(nameCs = v))),
      enumSelectRequired("Process Type:", PrintingProcessType.values, pmVar.signal.map(_.processType),
        v => update(_.copy(processType = v))),
      optionalNumberField("Max Color Count:", pmVar.signal.map(_.maxColorCount),
        v => update(_.copy(maxColorCount = v)), "Unlimited"),
    )


/** Categories list & editor. */
object CategoriesEditor:
  import FormComponents.*

  def apply(): Element =
    val vm = CatalogEditorViewModel
    div(
      cls := "catalog-section",
      sectionHeader("Product Categories",
        actionLabel = Some("+ Add Category"),
        onAction = () => vm.addCategory()),

      div(
        cls := "catalog-item-list",
        children <-- vm.state.map { s =>
          s.categories.zipWithIndex.map { case (cat, idx) =>
            div(
              cls <-- vm.state.map(st =>
                if st.editingItemIndex.contains(idx) then "catalog-item-row selected"
                else "catalog-item-row"
              ),
              div(
                cls := "catalog-item-summary",
                strong(if cat.id.nonEmpty then cat.id else "(new)"),
                span(s" — ${cat.nameEn}"),
                span(cls := "catalog-item-meta", s" [${cat.components.size} components]"),
              ),
              div(
                cls := "catalog-item-actions",
                button(cls := "catalog-btn catalog-btn-small", "Edit",
                  onClick --> { _ => vm.setEditingIndex(Some(idx)) }),
                button(cls := "catalog-btn catalog-btn-small catalog-btn-danger", "×",
                  onClick --> { _ => vm.removeCategory(idx) }),
              ),
            )
          }
        },
      ),

      child <-- vm.state.map { s =>
        s.editingItemIndex.flatMap(idx => s.categories.lift(idx).map(idx -> _)) match
          case Some((idx, cat)) => categoryForm(idx, cat, s)
          case None => emptyNode
      },
    )

  private def categoryForm(idx: Int, initial: CategoryEditState, editorState: CatalogEditorState): Element =
    val vm = CatalogEditorViewModel
    val catVar = Var(initial)
    def update(fn: CategoryEditState => CategoryEditState): Unit =
      catVar.update(fn)
      vm.updateCategory(idx, catVar.now())

    val availableMaterials = Val(editorState.materials.map(m => (m.id, s"${m.id} (${m.nameEn})")).filter(_._1.nonEmpty))
    val availableFinishes = Val(editorState.finishes.map(f => (f.id, s"${f.id} (${f.nameEn})")).filter(_._1.nonEmpty))
    val availableMethods = Val(editorState.printingMethods.map(pm => (pm.id, s"${pm.id} (${pm.nameEn})")).filter(_._1.nonEmpty))

    div(
      cls := "catalog-edit-form card",
      h3("Edit Category"),
      textField("ID:", catVar.signal.map(_.id), v => update(_.copy(id = v)), "e.g. cat-business-cards"),
      localizedStringEditor("Name:",
        catVar.signal.map(_.nameEn), catVar.signal.map(_.nameCs),
        v => update(_.copy(nameEn = v)), v => update(_.copy(nameCs = v))),

      enumCheckboxSet("Required Specifications:", SpecKind.values, catVar.signal.map(_.requiredSpecKinds),
        v => update(_.copy(requiredSpecKinds = v))),

      idCheckboxSet("Allowed Printing Methods (empty = all):", availableMethods.signal,
        catVar.signal.map(_.allowedPrintingMethodIds),
        v => update(_.copy(allowedPrintingMethodIds = v))),

      // Component templates
      div(
        cls := "form-section",
        div(
          cls := "catalog-section-header",
          h3("Component Templates"),
          actionButton("+ Add Component", () => {
            val newComp = ComponentTemplateEditState(ComponentRole.Main, Set.empty, Set.empty)
            update(s => s.copy(components = s.components :+ newComp))
          }, "catalog-btn-small"),
        ),

        children <-- catVar.signal.map { cat =>
          cat.components.zipWithIndex.map { case (comp, compIdx) =>
            div(
              cls := "catalog-component-template",
              div(
                cls := "catalog-section-header",
                h4(s"Component ${compIdx + 1}"),
                dangerButton("Remove", () => {
                  update(s => s.copy(components = s.components.patch(compIdx, Nil, 1)))
                }),
              ),
              enumSelectRequired("Role:", ComponentRole.values,
                catVar.signal.map(_.components.lift(compIdx).map(_.role).getOrElse(ComponentRole.Main)),
                v => update { s =>
                  val updated = s.components.updated(compIdx, s.components(compIdx).copy(role = v))
                  s.copy(components = updated)
                }),
              idCheckboxSet("Allowed Materials:", availableMaterials.signal,
                catVar.signal.map(_.components.lift(compIdx).map(_.allowedMaterialIds).getOrElse(Set.empty)),
                v => update { s =>
                  val updated = s.components.updated(compIdx, s.components(compIdx).copy(allowedMaterialIds = v))
                  s.copy(components = updated)
                }),
              idCheckboxSet("Allowed Finishes:", availableFinishes.signal,
                catVar.signal.map(_.components.lift(compIdx).map(_.allowedFinishIds).getOrElse(Set.empty)),
                v => update { s =>
                  val updated = s.components.updated(compIdx, s.components(compIdx).copy(allowedFinishIds = v))
                  s.copy(components = updated)
                }),
            )
          }
        },
      ),
    )


/** Pricelist editor. */
object PricelistEditor:
  import FormComponents.*

  def apply(): Element =
    val vm = CatalogEditorViewModel
    val addRuleType = Var("MaterialBasePrice")

    div(
      cls := "catalog-section",
      sectionHeader("Pricelist"),

      // Pricelist metadata
      div(
        cls := "catalog-edit-form card",
        h3("Pricelist Settings"),
        enumSelectRequired("Currency:", mpbuilder.domain.pricing.Currency.values,
          vm.state.map(_.pricelist.currency),
          v => vm.updatePricelist(vm.stateVar.now().pricelist.copy(currency = v))),
        textField("Version:", vm.state.map(_.pricelist.version),
          v => vm.updatePricelist(vm.stateVar.now().pricelist.copy(version = v)), "e.g. 1.0"),
      ),

      // Add new rule
      div(
        cls := "catalog-add-rule",
        label("Add pricing rule:"),
        div(
          cls := "catalog-add-rule-row",
          select(
            PricingRuleEditState.ruleTypes.map { rt =>
              option(rt, value := rt)
            },
            onChange.mapToValue --> { v => addRuleType.set(v) },
          ),
          actionButton("+ Add Rule", () => vm.addPricingRule(addRuleType.now())),
        ),
      ),

      // Rules list
      div(
        cls := "catalog-item-list",
        children <-- vm.state.map { s =>
          s.pricelist.rules.zipWithIndex.map { case (rule, idx) =>
            pricingRuleRow(idx, rule)
          }
        },
      ),
    )

  private def pricingRuleRow(idx: Int, initial: PricingRuleEditState): Element =
    val vm = CatalogEditorViewModel
    val expanded = Var(false)

    div(
      cls := "catalog-pricing-rule",
      div(
        cls := "catalog-item-row",
        div(
          cls := "catalog-item-summary",
          strong(initial.ruleType),
          span(cls := "catalog-item-meta", {
            val fieldDefs = PricingRuleEditState.fieldsForType(initial.ruleType)
            fieldDefs.take(3).flatMap { case (k, _) =>
              initial.fields.get(k).map(v => s"$k=$v")
            }.mkString(" ")
          }),
        ),
        div(
          cls := "catalog-item-actions",
          button(cls := "catalog-btn catalog-btn-small",
            child.text <-- expanded.signal.map(e => if e then "▲" else "▼"),
            onClick --> { _ => expanded.update(!_) }),
          button(cls := "catalog-btn catalog-btn-small catalog-btn-danger", "×",
            onClick --> { _ => vm.removePricingRule(idx) }),
        ),
      ),
      child <-- expanded.signal.map { isExpanded =>
        if !isExpanded then emptyNode
        else pricingRuleForm(idx, initial)
      },
    )

  private def pricingRuleForm(idx: Int, initial: PricingRuleEditState): Element =
    val vm = CatalogEditorViewModel
    val ruleVar = Var(initial)
    val fieldDefs = PricingRuleEditState.fieldsForType(initial.ruleType)

    div(
      cls := "catalog-pricing-rule-form",
      fieldDefs.map { case (fieldKey, fieldLabel) =>
        FormComponents.textField(
          fieldLabel + ":",
          ruleVar.signal.map(_.fields.getOrElse(fieldKey, "")),
          v => {
            ruleVar.update(r => r.copy(fields = r.fields.updated(fieldKey, v)))
            vm.updatePricingRule(idx, ruleVar.now())
          },
          s"Enter $fieldLabel",
        )
      },
    )


/** Import / Export section. */
object ImportExportEditor:
  import FormComponents.*

  def apply(): Element =
    val vm = CatalogEditorViewModel
    val importCatalogText = Var("")
    val importPricelistText = Var("")
    val exportCatalogText = Var("")
    val exportPricelistText = Var("")

    div(
      cls := "catalog-section",
      sectionHeader("Import / Export"),

      // Quick actions
      div(
        cls := "catalog-edit-form card",
        h3("Quick Actions"),
        div(
          cls := "catalog-quick-actions",
          actionButton("Load Sample Catalog", () => vm.loadSampleCatalog()),
          actionButton("Load Sample Pricelist", () => vm.loadSamplePricelist()),
          FormComponents.dangerButton("Clear All", () => vm.clearAll()),
        ),
      ),

      // Export
      div(
        cls := "catalog-edit-form card",
        h3("Export as JSON"),
        div(
          cls := "catalog-export-actions",
          actionButton("Export Catalog", () => exportCatalogText.set(vm.exportCatalogJson())),
          actionButton("Export Pricelist", () => exportPricelistText.set(vm.exportPricelistJson())),
        ),
        child <-- exportCatalogText.signal.map { text =>
          if text.isEmpty then emptyNode
          else div(
            cls := "form-group",
            L.label("Catalog JSON:"),
            L.textArea(
              cls := "catalog-json-area",
              L.readOnly := true,
              L.value := text,
            ),
            actionButton("Copy to Clipboard", () => copyToClipboard(text)),
          )
        },
        child <-- exportPricelistText.signal.map { text =>
          if text.isEmpty then emptyNode
          else div(
            cls := "form-group",
            L.label("Pricelist JSON:"),
            L.textArea(
              cls := "catalog-json-area",
              L.readOnly := true,
              L.value := text,
            ),
            actionButton("Copy to Clipboard", () => copyToClipboard(text)),
          )
        },
      ),

      // Import
      div(
        cls := "catalog-edit-form card",
        h3("Import from JSON"),
        div(
          cls := "form-group",
          L.label("Catalog JSON:"),
          L.textArea(
            cls := "catalog-json-area",
            placeholder := "Paste catalog JSON here...",
            controlled(
              value <-- importCatalogText.signal,
              onInput.mapToValue --> { v => importCatalogText.set(v) },
            ),
          ),
          actionButton("Import Catalog", () => {
            vm.importCatalogJson(importCatalogText.now())
            importCatalogText.set("")
          }),
        ),
        div(
          cls := "form-group",
          L.label("Pricelist JSON:"),
          L.textArea(
            cls := "catalog-json-area",
            placeholder := "Paste pricelist JSON here...",
            controlled(
              value <-- importPricelistText.signal,
              onInput.mapToValue --> { v => importPricelistText.set(v) },
            ),
          ),
          actionButton("Import Pricelist", () => {
            vm.importPricelistJson(importPricelistText.now())
            importPricelistText.set("")
          }),
        ),
      ),
    )

  private def copyToClipboard(text: String): Unit =
    dom.window.navigator.clipboard.writeText(text)
