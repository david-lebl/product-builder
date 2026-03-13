package mpbuilder.ui.catalog.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.rules.*
import mpbuilder.ui.catalog.*
import mpbuilder.uikit.fields.SelectOption

/** Editor view for managing Compatibility Rules.
  *
  * Rules are displayed as a summary list; each rule type has its own form editor.
  */
object RulesEditorView:

  def apply(): HtmlElement =
    div(
      cls := "catalog-section",
      div(
        cls := "catalog-section-header",
        h3("Compatibility Rules"),
        FormComponents.actionButton("+ Add Rule", () =>
          CatalogEditorViewModel.setEditState(EditState.CreatingRule)
        ),
      ),

      div(
        cls := "catalog-entity-list",
        children <-- CatalogEditorViewModel.rules.map { rules =>
          if rules.isEmpty then List(p(cls := "empty-message", "No compatibility rules defined."))
          else rules.zipWithIndex.map { case (rule, idx) => ruleRow(rule, idx) }
        },
      ),

      child <-- CatalogEditorViewModel.editState.combineWith(CatalogEditorViewModel.rules).map {
        case (EditState.CreatingRule, _) => ruleForm(None, -1)
        case (EditState.EditingRule(idx), rules) =>
          rules.lift(idx).map(r => ruleForm(Some(r), idx)).getOrElse(emptyNode)
        case _ => emptyNode
      },
    )

  private def ruleRow(rule: CompatibilityRule, index: Int): HtmlElement =
    div(
      cls := "catalog-entity-row",
      div(
        cls := "entity-info",
        span(cls := "entity-id", s"#${index + 1}"),
        span(cls := "entity-name", ruleSummary(rule)),
      ),
      div(
        cls := "entity-actions",
        button(cls := "btn btn-sm", "Edit", onClick --> { _ =>
          CatalogEditorViewModel.setEditState(EditState.EditingRule(index))
        }),
        button(cls := "btn btn-sm btn-danger", "Remove", onClick --> { _ =>
          CatalogEditorViewModel.removeRule(index)
        }),
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

  /** Rule creation/editing form with a rule type selector. */
  private def ruleForm(existing: Option[CompatibilityRule], index: Int): HtmlElement =
    val ruleTypeVar = Var(existing.map(ruleTypeName).getOrElse("MaterialFinishIncompatible"))

    // Common field Vars for the most common rule types
    val materialIdVar = Var(extractMaterialId(existing).getOrElse(""))
    val finishIdVar = Var(extractFinishId(existing).getOrElse(""))
    val reasonVar = Var(extractReason(existing).getOrElse(""))
    val finishTypeVar = Var(extractFinishType(existing).getOrElse(FinishType.Lamination))
    val materialPropertyVar = Var(extractMaterialProperty(existing).getOrElse(MaterialProperty.Glossy))
    val materialFamilyVar = Var(extractMaterialFamily(existing).getOrElse(MaterialFamily.Paper))
    val minWeightVar = Var(extractMinWeight(existing).map(_.toString).getOrElse("200"))

    val ruleTypes = List(
      "MaterialFinishIncompatible",
      "MaterialRequiresFinish",
      "FinishRequiresMaterialProperty",
      "FinishMutuallyExclusive",
      "MaterialPropertyFinishTypeIncompatible",
      "MaterialFamilyFinishTypeIncompatible",
      "MaterialWeightFinishType",
      "FinishTypeMutuallyExclusive",
      "FinishCategoryExclusive",
      "FinishRequiresFinishType",
      "FinishRequiresPrintingProcess",
    )

    div(
      cls := "catalog-edit-form",
      h4(if existing.isDefined then "Edit Rule" else "New Rule"),

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
          FormComponents.textField("Material ID", materialIdVar.signal, materialIdVar.writer)
        else emptyNode
      },

      // Finish ID field
      child <-- ruleTypeVar.signal.map { rt =>
        if Set("MaterialFinishIncompatible", "FinishRequiresMaterialProperty", "FinishMutuallyExclusive", "FinishRequiresFinishType").contains(rt) then
          FormComponents.textField("Finish ID", finishIdVar.signal, finishIdVar.writer)
        else emptyNode
      },

      // Finish type field
      child <-- ruleTypeVar.signal.map { rt =>
        if Set("MaterialPropertyFinishTypeIncompatible", "MaterialFamilyFinishTypeIncompatible", "MaterialWeightFinishType", "FinishTypeMutuallyExclusive", "FinishRequiresFinishType", "FinishRequiresPrintingProcess").contains(rt) then
          FormComponents.enumSelectRequired[FinishType]("Finish Type", FinishType.values, finishTypeVar.signal, finishTypeVar.writer)
        else emptyNode
      },

      // Material property field
      child <-- ruleTypeVar.signal.map { rt =>
        if Set("FinishRequiresMaterialProperty", "MaterialPropertyFinishTypeIncompatible").contains(rt) then
          FormComponents.enumSelectRequired[MaterialProperty]("Material Property", MaterialProperty.values, materialPropertyVar.signal, materialPropertyVar.writer)
        else emptyNode
      },

      // Material family field
      child <-- ruleTypeVar.signal.map { rt =>
        if rt == "MaterialFamilyFinishTypeIncompatible" then
          FormComponents.enumSelectRequired[MaterialFamily]("Material Family", MaterialFamily.values, materialFamilyVar.signal, materialFamilyVar.writer)
        else emptyNode
      },

      // Min weight field
      child <-- ruleTypeVar.signal.map { rt =>
        if rt == "MaterialWeightFinishType" then
          FormComponents.numberField("Min Weight (gsm)", minWeightVar.signal, minWeightVar.writer)
        else emptyNode
      },

      FormComponents.textField("Reason", reasonVar.signal, reasonVar.writer, "Explanation for this rule"),

      div(
        cls := "form-actions",
        FormComponents.actionButton("Save", () => {
          buildRule(ruleTypeVar.now(), materialIdVar.now(), finishIdVar.now(), reasonVar.now(), finishTypeVar.now(), materialPropertyVar.now(), materialFamilyVar.now(), minWeightVar.now().toIntOption.getOrElse(200)).foreach { rule =>
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
    reason: String,
    finishType: FinishType,
    materialProperty: MaterialProperty,
    materialFamily: MaterialFamily,
    minWeight: Int,
  ): Option[CompatibilityRule] =
    ruleType match
      case "MaterialFinishIncompatible" if materialId.nonEmpty && finishId.nonEmpty =>
        Some(CompatibilityRule.MaterialFinishIncompatible(MaterialId.unsafe(materialId), FinishId.unsafe(finishId), reason))
      case "FinishRequiresMaterialProperty" if finishId.nonEmpty =>
        Some(CompatibilityRule.FinishRequiresMaterialProperty(FinishId.unsafe(finishId), materialProperty, reason))
      case "MaterialPropertyFinishTypeIncompatible" =>
        Some(CompatibilityRule.MaterialPropertyFinishTypeIncompatible(materialProperty, finishType, reason))
      case "MaterialFamilyFinishTypeIncompatible" =>
        Some(CompatibilityRule.MaterialFamilyFinishTypeIncompatible(materialFamily, finishType, reason))
      case "MaterialWeightFinishType" =>
        Some(CompatibilityRule.MaterialWeightFinishType(finishType, minWeight, reason))
      case "FinishRequiresFinishType" if finishId.nonEmpty =>
        Some(CompatibilityRule.FinishRequiresFinishType(FinishId.unsafe(finishId), finishType, reason))
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
