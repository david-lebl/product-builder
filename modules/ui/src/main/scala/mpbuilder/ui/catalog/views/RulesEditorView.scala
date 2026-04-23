package mpbuilder.ui.catalog.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.rules.*
import mpbuilder.ui.catalog.*
import mpbuilder.uikit.fields.SelectOption
import mpbuilder.uikit.containers.*

/** Editor view for managing Compatibility Rules using SplitTableView.
  *
  * Rules are displayed as a summary table; each rule type has its own form editor.
  */
object RulesEditorView:

  private case class IndexedRule(rule: CompatibilityRule, index: Int)

  def apply(): HtmlElement =
    val searchVar = Var("")
    val selectedKey: Var[Option[String]] = Var(None)

    val indexedRules: Signal[List[IndexedRule]] =
      CatalogEditorViewModel.rules.combineWith(searchVar.signal).map { case (rules, query) =>
        val q = query.trim.toLowerCase
        val indexed = rules.zipWithIndex.map { case (r, i) => IndexedRule(r, i) }
        if q.isEmpty then indexed
        else indexed.filter(ir => ruleSummary(ir.rule).toLowerCase.contains(q) || ruleTypeName(ir.rule).toLowerCase.contains(q))
      }

    val tableConfig = SplitTableConfig[IndexedRule](
      columns = List(
        ColumnDef("#", ir => span(cls := "entity-id", s"${ir.index + 1}"), width = Some("50px")),
        ColumnDef("Type", ir => span(cls := "entity-name", ruleTypeName(ir.rule)), Some(ir => ruleTypeName(ir.rule)), Some("220px")),
        ColumnDef("Summary", ir => span(ruleSummary(ir.rule))),
        ColumnDef("", ir => div(
          cls := "entity-actions",
          button(cls := "btn btn-sm btn-danger", "✕", onClick.stopPropagation --> { _ =>
            CatalogEditorViewModel.removeRule(ir.index)
          }),
        ), width = Some("50px")),
      ),
      rowKey = ir => ir.index.toString,
      searchPlaceholder = "Search rules…",
      onRowSelect = Some(ir => {
        selectedKey.set(Some(ir.index.toString))
        CatalogEditorViewModel.setEditState(EditState.EditingRule(ir.index))
      }),
      emptyMessage = "No compatibility rules defined.",
    )

    val sidePanel: Signal[Option[HtmlElement]] =
      CatalogEditorViewModel.editState.combineWith(CatalogEditorViewModel.rules).map {
        case (EditState.CreatingRule, _) =>
          Some(ruleForm(None, -1))
        case (EditState.EditingRule(idx), rules) =>
          rules.lift(idx).map(r => ruleForm(Some(r), idx))
        case _ => None
      }

    div(
      cls := "catalog-section",
      h2(cls := "manufacturing-view-title", "Compatibility Rules"),
      SplitTableView(
        config = tableConfig,
        items = indexedRules,
        selectedKey = selectedKey.signal,
        searchQuery = searchVar,
        sidePanel = sidePanel,
        headerActions = Some(
          FormComponents.actionButton("+ Add Rule", () => {
            selectedKey.set(None)
            CatalogEditorViewModel.setEditState(EditState.CreatingRule)
          })
        ),
      ),
    )

  private def ruleSummary(rule: CompatibilityRule): String = rule match
    case r: CompatibilityRule.MaterialFinishIncompatible => s"MaterialFinishIncompatible: ${r.materialId.value} ↔ ${r.finishId.value}"
    case r: CompatibilityRule.MaterialRequiresFinish => s"MaterialRequiresFinish: ${r.materialId.value} → ${r.requiredFinishIds.map(_.value).mkString(", ")}"
    case r: CompatibilityRule.FinishRequiresMaterialProperty => s"FinishRequiresMaterialProperty: ${r.finishId.value} needs ${r.requiredProperty}"
    case r: CompatibilityRule.FinishMutuallyExclusive => s"FinishMutuallyExclusive: ${r.finishIdA.value} ↔ ${r.finishIdB.value}"
    case r: CompatibilityRule.SpecConstraint => s"SpecConstraint: ${r.categoryId.value}"
    case r: CompatibilityRule.MaterialPropertyFinishTypeIncompatible => s"MaterialPropertyFinishTypeIncompatible: ${r.property} ↔ ${r.finishType}"
    case r: CompatibilityRule.MaterialFamilyFinishTypeIncompatible => s"MaterialFamilyFinishTypeIncompatible: ${r.family} ↔ ${r.finishType}"
    case r: CompatibilityRule.MaterialWeightFinishType => s"MaterialWeightFinishType: ${r.finishType} min ${r.minWeightGsm}gsm"
    case r: CompatibilityRule.FinishTypeMutuallyExclusive => s"FinishTypeMutuallyExclusive: ${r.finishTypeA} ↔ ${r.finishTypeB}"
    case r: CompatibilityRule.FinishCategoryExclusive => s"FinishCategoryExclusive: ${r.category}"
    case r: CompatibilityRule.FinishRequiresFinishType => s"FinishRequiresFinishType: ${r.finishId.value} needs ${r.requiredFinishType}"
    case r: CompatibilityRule.FinishRequiresPrintingProcess => s"FinishRequiresPrintingProcess: ${r.finishType} needs ${r.requiredProcessTypes.mkString(",")}"
    case r: CompatibilityRule.ConfigurationConstraint => s"ConfigurationConstraint: ${r.categoryId.value}"
    case r: CompatibilityRule.TechnologyConstraint => s"TechnologyConstraint"
    case r: CompatibilityRule.ScoringMaxCreasesForCategory => s"ScoringMaxCreasesForCategory: ${r.categoryId.value}"
    case r: CompatibilityRule.ScoringMaxCreasesForMaterial => s"ScoringMaxCreasesForMaterial: ${r.materialId.value}"
    case r: CompatibilityRule.ScoringMaxCreasesForPrintingProcess => s"ScoringMaxCreasesForPrintingProcess: ${r.processType}"
    case r: CompatibilityRule.RequiresExternalPartner => s"RequiresExternalPartner: ${r.categoryId.value} → ${r.candidatePartners.map(_.value).mkString(", ")}"

  /** Rule creation/editing form with a rule type selector. */
  private def ruleForm(existing: Option[CompatibilityRule], index: Int): HtmlElement =
    val ruleTypeVar = Var(existing.map(ruleTypeName).getOrElse("MaterialFinishIncompatible"))

    // Common field Vars for the most common rule types
    val materialIdVar = Var(extractMaterialId(existing).getOrElse(""))
    val finishIdVar = Var(extractFinishId(existing).getOrElse(""))
    val reasonVar = Var(extractReason(existing).getOrElse(""))
    val finishTypeVar = Var(extractFinishType(existing).getOrElse(FinishType.Lamination))
    val finishTypeBVar = Var(extractFinishTypeB(existing).getOrElse(FinishType.UVCoating))
    val materialPropertyVar = Var(extractMaterialProperty(existing).getOrElse(MaterialProperty.Glossy))
    val materialFamilyVar = Var(extractMaterialFamily(existing).getOrElse(MaterialFamily.Paper))
    val minWeightVar = Var(extractMinWeight(existing).map(_.toString).getOrElse("200"))
    val finishIdBVar = Var(extractFinishIdB(existing).getOrElse(""))
    val categoryIdVar = Var(extractCategoryId(existing).getOrElse(""))
    val finishCategoryVar = Var(extractFinishCategory(existing).getOrElse(FinishCategory.Surface))
    val requiredFinishIdsVar = Var(extractRequiredFinishIds(existing).getOrElse(""))
    val processTypesVar = Var(extractProcessTypes(existing).getOrElse(""))

    val ruleTypes = List(
      "MaterialFinishIncompatible",
      "MaterialRequiresFinish",
      "FinishRequiresMaterialProperty",
      "FinishMutuallyExclusive",
      "SpecConstraint",
      "MaterialPropertyFinishTypeIncompatible",
      "MaterialFamilyFinishTypeIncompatible",
      "MaterialWeightFinishType",
      "FinishTypeMutuallyExclusive",
      "FinishCategoryExclusive",
      "FinishRequiresFinishType",
      "FinishRequiresPrintingProcess",
      "ConfigurationConstraint",
      "TechnologyConstraint",
    )

    div(
      cls := "catalog-detail-panel",
      button(cls := "detail-panel-close", "×", onClick --> { _ =>
        CatalogEditorViewModel.setEditState(EditState.None)
      }),
      div(cls := "detail-panel-header",
        h3(if existing.isDefined then "Edit Rule" else "New Rule"),
      ),

      div(cls := "detail-panel-section",
      div(
        cls := "form-group",
        com.raquo.laminar.api.L.label("Rule Type"),
        select(
          children <-- Val(ruleTypes.map { rt =>
            option(rt, value := rt, selected := (rt == ruleTypeVar.now()))
          }),
          onChange.mapToValue --> ruleTypeVar.writer,
        ),
      ),

      // Material ID field (for rules that need it)
      child <-- ruleTypeVar.signal.map { rt =>
        if Set("MaterialFinishIncompatible", "MaterialRequiresFinish").contains(rt) then
          FormComponents.textField("Material ID", materialIdVar.signal, materialIdVar.writer, "")
        else emptyNode
      },

      // Finish ID field
      child <-- ruleTypeVar.signal.map { rt =>
        if Set("MaterialFinishIncompatible", "FinishRequiresMaterialProperty", "FinishMutuallyExclusive", "FinishRequiresFinishType").contains(rt) then
          FormComponents.textField("Finish ID", finishIdVar.signal, finishIdVar.writer, "")
        else emptyNode
      },

      // Second Finish ID field (for mutual exclusion)
      child <-- ruleTypeVar.signal.map { rt =>
        if Set("FinishMutuallyExclusive").contains(rt) then
          FormComponents.textField("Finish ID B", finishIdBVar.signal, finishIdBVar.writer, "")
        else emptyNode
      },

      // Required finish IDs (comma-separated)
      child <-- ruleTypeVar.signal.map { rt =>
        if rt == "MaterialRequiresFinish" then
          FormComponents.textField("Required Finish IDs (comma-separated)", requiredFinishIdsVar.signal, requiredFinishIdsVar.writer, "")
        else emptyNode
      },

      // Category ID field
      child <-- ruleTypeVar.signal.map { rt =>
        if Set("SpecConstraint", "ConfigurationConstraint").contains(rt) then
          FormComponents.textField("Category ID", categoryIdVar.signal, categoryIdVar.writer, "")
        else emptyNode
      },

      // Finish type field
      child <-- ruleTypeVar.signal.map { rt =>
        if Set("MaterialPropertyFinishTypeIncompatible", "MaterialFamilyFinishTypeIncompatible", "MaterialWeightFinishType", "FinishTypeMutuallyExclusive", "FinishRequiresFinishType", "FinishRequiresPrintingProcess").contains(rt) then
          FormComponents.enumSelectRequired[FinishType]("Finish Type", FinishType.values, finishTypeVar.signal, finishTypeVar.writer, _.toString)
        else emptyNode
      },

      // Second finish type (for FinishTypeMutuallyExclusive)
      child <-- ruleTypeVar.signal.map { rt =>
        if rt == "FinishTypeMutuallyExclusive" then
          FormComponents.enumSelectRequired[FinishType]("Finish Type B", FinishType.values, finishTypeBVar.signal, finishTypeBVar.writer, _.toString)
        else emptyNode
      },

      // Material property field
      child <-- ruleTypeVar.signal.map { rt =>
        if Set("FinishRequiresMaterialProperty", "MaterialPropertyFinishTypeIncompatible").contains(rt) then
          FormComponents.enumSelectRequired[MaterialProperty]("Material Property", MaterialProperty.values, materialPropertyVar.signal, materialPropertyVar.writer, _.toString)
        else emptyNode
      },

      // Material family field
      child <-- ruleTypeVar.signal.map { rt =>
        if rt == "MaterialFamilyFinishTypeIncompatible" then
          FormComponents.enumSelectRequired[MaterialFamily]("Material Family", MaterialFamily.values, materialFamilyVar.signal, materialFamilyVar.writer, _.toString)
        else emptyNode
      },

      // Min weight field
      child <-- ruleTypeVar.signal.map { rt =>
        if rt == "MaterialWeightFinishType" then
          FormComponents.numberField("Min Weight (gsm)", minWeightVar.signal, minWeightVar.writer)
        else emptyNode
      },

      // Finish category (for FinishCategoryExclusive)
      child <-- ruleTypeVar.signal.map { rt =>
        if rt == "FinishCategoryExclusive" then
          FormComponents.enumSelectRequired[FinishCategory]("Finish Category", FinishCategory.values, finishCategoryVar.signal, finishCategoryVar.writer, _.toString)
        else emptyNode
      },

      // Required process types (comma-separated)
      child <-- ruleTypeVar.signal.map { rt =>
        if rt == "FinishRequiresPrintingProcess" then
          FormComponents.textField("Required Process Types (comma-separated)", processTypesVar.signal, processTypesVar.writer, "e.g. Digital,Offset")
        else emptyNode
      },

      FormComponents.textField("Reason", reasonVar.signal, reasonVar.writer, "Explanation for this rule"),
      ), // close detail-panel-section

      div(
        cls := "detail-panel-actions",
        FormComponents.actionButton("Save", () => {
          buildRule(
            ruleTypeVar.now(), materialIdVar.now(), finishIdVar.now(), finishIdBVar.now(),
            reasonVar.now(), finishTypeVar.now(), finishTypeBVar.now(),
            materialPropertyVar.now(), materialFamilyVar.now(),
            minWeightVar.now().toIntOption.getOrElse(200),
            categoryIdVar.now(), finishCategoryVar.now(),
            requiredFinishIdsVar.now(), processTypesVar.now(),
          ).foreach { rule =>
            if existing.isDefined then CatalogEditorViewModel.updateRule(index, rule)
            else CatalogEditorViewModel.addRule(rule)
          }
        }),
        FormComponents.dangerButton("Cancel", () =>
          CatalogEditorViewModel.setEditState(EditState.None)
        ),
      ),
    )

  private def buildRule(
    ruleType: String,
    materialId: String,
    finishId: String,
    finishIdB: String,
    reason: String,
    finishType: FinishType,
    finishTypeB: FinishType,
    materialProperty: MaterialProperty,
    materialFamily: MaterialFamily,
    minWeight: Int,
    categoryId: String,
    finishCategory: FinishCategory,
    requiredFinishIds: String,
    processTypes: String,
  ): Option[CompatibilityRule] =
    ruleType match
      case "MaterialFinishIncompatible" if materialId.nonEmpty && finishId.nonEmpty =>
        Some(CompatibilityRule.MaterialFinishIncompatible(MaterialId.unsafe(materialId), FinishId.unsafe(finishId), reason))
      case "MaterialRequiresFinish" if materialId.nonEmpty && requiredFinishIds.nonEmpty =>
        val ids = requiredFinishIds.split(",").map(_.trim).filter(_.nonEmpty).map(FinishId.unsafe).toSet
        Some(CompatibilityRule.MaterialRequiresFinish(MaterialId.unsafe(materialId), ids, reason))
      case "FinishRequiresMaterialProperty" if finishId.nonEmpty =>
        Some(CompatibilityRule.FinishRequiresMaterialProperty(FinishId.unsafe(finishId), materialProperty, reason))
      case "FinishMutuallyExclusive" if finishId.nonEmpty && finishIdB.nonEmpty =>
        Some(CompatibilityRule.FinishMutuallyExclusive(FinishId.unsafe(finishId), FinishId.unsafe(finishIdB), reason))
      case "SpecConstraint" if categoryId.nonEmpty =>
        // Default to a MinQuantity(1) predicate — users can edit JSON for complex predicates
        Some(CompatibilityRule.SpecConstraint(CategoryId.unsafe(categoryId), SpecPredicate.MinQuantity(1), reason))
      case "MaterialPropertyFinishTypeIncompatible" =>
        Some(CompatibilityRule.MaterialPropertyFinishTypeIncompatible(materialProperty, finishType, reason))
      case "MaterialFamilyFinishTypeIncompatible" =>
        Some(CompatibilityRule.MaterialFamilyFinishTypeIncompatible(materialFamily, finishType, reason))
      case "MaterialWeightFinishType" =>
        Some(CompatibilityRule.MaterialWeightFinishType(finishType, minWeight, reason))
      case "FinishTypeMutuallyExclusive" =>
        Some(CompatibilityRule.FinishTypeMutuallyExclusive(finishType, finishTypeB, reason))
      case "FinishCategoryExclusive" =>
        Some(CompatibilityRule.FinishCategoryExclusive(finishCategory, reason))
      case "FinishRequiresFinishType" if finishId.nonEmpty =>
        Some(CompatibilityRule.FinishRequiresFinishType(FinishId.unsafe(finishId), finishType, reason))
      case "FinishRequiresPrintingProcess" if processTypes.nonEmpty =>
        val pts = processTypes.split(",").map(_.trim).filter(_.nonEmpty).flatMap(s => scala.util.Try(PrintingProcessType.valueOf(s)).toOption).toSet
        Some(CompatibilityRule.FinishRequiresPrintingProcess(finishType, pts, reason))
      case "ConfigurationConstraint" if categoryId.nonEmpty =>
        // Default to HasMinWeight(200) — users can edit JSON for complex predicates
        Some(CompatibilityRule.ConfigurationConstraint(CategoryId.unsafe(categoryId), ConfigurationPredicate.HasMinWeight(200), reason))
      case "TechnologyConstraint" =>
        // Default to HasMinWeight(200) — users can edit JSON for complex predicates
        Some(CompatibilityRule.TechnologyConstraint(ConfigurationPredicate.HasMinWeight(200), reason))
      case _ => None

  // Helper extractors for populating forms from existing rules
  private def ruleTypeName(rule: CompatibilityRule): String = rule match
    case _: CompatibilityRule.MaterialFinishIncompatible => "MaterialFinishIncompatible"
    case _: CompatibilityRule.MaterialRequiresFinish => "MaterialRequiresFinish"
    case _: CompatibilityRule.FinishRequiresMaterialProperty => "FinishRequiresMaterialProperty"
    case _: CompatibilityRule.FinishMutuallyExclusive => "FinishMutuallyExclusive"
    case _: CompatibilityRule.SpecConstraint => "SpecConstraint"
    case _: CompatibilityRule.MaterialPropertyFinishTypeIncompatible => "MaterialPropertyFinishTypeIncompatible"
    case _: CompatibilityRule.MaterialFamilyFinishTypeIncompatible => "MaterialFamilyFinishTypeIncompatible"
    case _: CompatibilityRule.MaterialWeightFinishType => "MaterialWeightFinishType"
    case _: CompatibilityRule.FinishTypeMutuallyExclusive => "FinishTypeMutuallyExclusive"
    case _: CompatibilityRule.FinishCategoryExclusive => "FinishCategoryExclusive"
    case _: CompatibilityRule.FinishRequiresFinishType => "FinishRequiresFinishType"
    case _: CompatibilityRule.FinishRequiresPrintingProcess => "FinishRequiresPrintingProcess"
    case _: CompatibilityRule.ConfigurationConstraint => "ConfigurationConstraint"
    case _: CompatibilityRule.TechnologyConstraint => "TechnologyConstraint"
    case _: CompatibilityRule.ScoringMaxCreasesForCategory => "ScoringMaxCreasesForCategory"
    case _: CompatibilityRule.ScoringMaxCreasesForMaterial => "ScoringMaxCreasesForMaterial"
    case _: CompatibilityRule.ScoringMaxCreasesForPrintingProcess => "ScoringMaxCreasesForPrintingProcess"
    case _: CompatibilityRule.RequiresExternalPartner => "RequiresExternalPartner"

  private def extractMaterialId(rule: Option[CompatibilityRule]): Option[String] = rule.collect {
    case r: CompatibilityRule.MaterialFinishIncompatible => r.materialId.value
    case r: CompatibilityRule.MaterialRequiresFinish => r.materialId.value
  }

  private def extractFinishId(rule: Option[CompatibilityRule]): Option[String] = rule.collect {
    case r: CompatibilityRule.MaterialFinishIncompatible => r.finishId.value
    case r: CompatibilityRule.FinishRequiresMaterialProperty => r.finishId.value
    case r: CompatibilityRule.FinishRequiresFinishType => r.finishId.value
  }

  private def extractReason(rule: Option[CompatibilityRule]): Option[String] = rule.map {
    case r: CompatibilityRule.MaterialFinishIncompatible => r.reason
    case r: CompatibilityRule.MaterialRequiresFinish => r.reason
    case r: CompatibilityRule.FinishRequiresMaterialProperty => r.reason
    case r: CompatibilityRule.FinishMutuallyExclusive => r.reason
    case r: CompatibilityRule.SpecConstraint => r.reason
    case r: CompatibilityRule.MaterialPropertyFinishTypeIncompatible => r.reason
    case r: CompatibilityRule.MaterialFamilyFinishTypeIncompatible => r.reason
    case r: CompatibilityRule.MaterialWeightFinishType => r.reason
    case r: CompatibilityRule.FinishTypeMutuallyExclusive => r.reason
    case r: CompatibilityRule.FinishCategoryExclusive => r.reason
    case r: CompatibilityRule.FinishRequiresFinishType => r.reason
    case r: CompatibilityRule.FinishRequiresPrintingProcess => r.reason
    case r: CompatibilityRule.ConfigurationConstraint => r.reason
    case r: CompatibilityRule.TechnologyConstraint => r.reason
    case _: CompatibilityRule.ScoringMaxCreasesForCategory => ""
    case _: CompatibilityRule.ScoringMaxCreasesForMaterial => ""
    case _: CompatibilityRule.ScoringMaxCreasesForPrintingProcess => ""
    case r: CompatibilityRule.RequiresExternalPartner => r.reason(Language.En)
  }

  private def extractFinishType(rule: Option[CompatibilityRule]): Option[FinishType] = rule.collect {
    case r: CompatibilityRule.MaterialPropertyFinishTypeIncompatible => r.finishType
    case r: CompatibilityRule.MaterialFamilyFinishTypeIncompatible => r.finishType
    case r: CompatibilityRule.MaterialWeightFinishType => r.finishType
    case r: CompatibilityRule.FinishRequiresFinishType => r.requiredFinishType
  }

  private def extractMaterialProperty(rule: Option[CompatibilityRule]): Option[MaterialProperty] = rule.collect {
    case r: CompatibilityRule.FinishRequiresMaterialProperty => r.requiredProperty
    case r: CompatibilityRule.MaterialPropertyFinishTypeIncompatible => r.property
  }

  private def extractMaterialFamily(rule: Option[CompatibilityRule]): Option[MaterialFamily] = rule.collect {
    case r: CompatibilityRule.MaterialFamilyFinishTypeIncompatible => r.family
  }

  private def extractMinWeight(rule: Option[CompatibilityRule]): Option[Int] = rule.collect {
    case r: CompatibilityRule.MaterialWeightFinishType => r.minWeightGsm
  }

  private def extractFinishIdB(rule: Option[CompatibilityRule]): Option[String] = rule.collect {
    case r: CompatibilityRule.FinishMutuallyExclusive => r.finishIdB.value
  }

  private def extractFinishTypeB(rule: Option[CompatibilityRule]): Option[FinishType] = rule.collect {
    case r: CompatibilityRule.FinishTypeMutuallyExclusive => r.finishTypeB
  }

  private def extractCategoryId(rule: Option[CompatibilityRule]): Option[String] = rule.collect {
    case r: CompatibilityRule.SpecConstraint => r.categoryId.value
    case r: CompatibilityRule.ConfigurationConstraint => r.categoryId.value
  }

  private def extractFinishCategory(rule: Option[CompatibilityRule]): Option[FinishCategory] = rule.collect {
    case r: CompatibilityRule.FinishCategoryExclusive => r.category
  }

  private def extractRequiredFinishIds(rule: Option[CompatibilityRule]): Option[String] = rule.collect {
    case r: CompatibilityRule.MaterialRequiresFinish => r.requiredFinishIds.map(_.value).mkString(", ")
  }

  private def extractProcessTypes(rule: Option[CompatibilityRule]): Option[String] = rule.collect {
    case r: CompatibilityRule.FinishRequiresPrintingProcess => r.requiredProcessTypes.map(_.toString).mkString(", ")
  }
