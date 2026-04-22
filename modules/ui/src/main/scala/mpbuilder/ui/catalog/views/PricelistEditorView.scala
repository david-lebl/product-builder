package mpbuilder.ui.catalog.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.ui.catalog.*
import mpbuilder.uikit.containers.*

/** Editor view for managing Pricelists and their Pricing Rules using SplitTableView. */
object PricelistEditorView:

  private case class IndexedPricingRule(rule: PricingRule, index: Int)

  def apply(): HtmlElement =
    val searchVar = Var("")
    val selectedKey: Var[Option[String]] = Var(None)

    val indexedRules: Signal[List[IndexedPricingRule]] =
      CatalogEditorViewModel.pricingRules.combineWith(searchVar.signal).map { case (rules, query) =>
        val q = query.trim.toLowerCase
        val indexed = rules.zipWithIndex.map { case (r, i) => IndexedPricingRule(r, i) }
        if q.isEmpty then indexed
        else indexed.filter(ir => pricingRuleSummary(ir.rule).toLowerCase.contains(q) || pricingRuleTypeName(ir.rule).toLowerCase.contains(q))
      }

    val tableConfig = SplitTableConfig[IndexedPricingRule](
      columns = List(
        ColumnDef("#", ir => span(cls := "entity-id", s"${ir.index + 1}"), width = Some("50px")),
        ColumnDef("Type", ir => span(cls := "entity-name", pricingRuleTypeName(ir.rule)), Some(ir => pricingRuleTypeName(ir.rule)), Some("200px")),
        ColumnDef("Summary", ir => span(pricingRuleSummary(ir.rule))),
        ColumnDef("", ir => div(
          cls := "entity-actions",
          button(cls := "btn btn-sm btn-danger", "✕", onClick.stopPropagation --> { _ =>
            CatalogEditorViewModel.removePricingRule(ir.index)
          }),
        ), width = Some("50px")),
      ),
      rowKey = ir => ir.index.toString,
      searchPlaceholder = "Search pricing rules…",
      onRowSelect = Some(ir => {
        selectedKey.set(Some(ir.index.toString))
        CatalogEditorViewModel.setEditState(EditState.EditingPricingRule(ir.index))
      }),
      emptyMessage = "No pricing rules defined for this pricelist.",
    )

    val sidePanel: Signal[Option[HtmlElement]] =
      CatalogEditorViewModel.editState.combineWith(CatalogEditorViewModel.pricingRules).map {
        case (EditState.CreatingPricingRule, _) =>
          Some(pricingRuleForm(None, -1))
        case (EditState.EditingPricingRule(idx), rules) =>
          rules.lift(idx).map(r => pricingRuleForm(Some(r), idx))
        case _ => None
      }

    div(
      cls := "catalog-section",
      h2(cls := "manufacturing-view-title", "Pricelists"),

      // Pricelist selector tabs
      div(
        cls := "pricelist-tabs",
        children <-- CatalogEditorViewModel.state.map { s =>
          s.pricelists.zipWithIndex.map { case (pl, idx) =>
            button(
              cls := (if idx == s.selectedPricelistIndex then "btn btn-sm btn-active" else "btn btn-sm"),
              s"${pl.currency} v${pl.version}",
              onClick --> { _ => CatalogEditorViewModel.selectPricelist(idx) },
            )
          } :+ button(
            cls := "btn btn-sm",
            "+ Add Pricelist",
            onClick --> { _ => CatalogEditorViewModel.addPricelist(Currency.USD) },
          )
        },
      ),

      SplitTableView(
        config = tableConfig,
        items = indexedRules,
        selectedKey = selectedKey.signal,
        searchQuery = searchVar,
        sidePanel = sidePanel,
        headerActions = Some(
          FormComponents.actionButton("+ Add Pricing Rule", () => {
            selectedKey.set(None)
            CatalogEditorViewModel.setEditState(EditState.CreatingPricingRule)
          })
        ),
      ),
    )

  private def pricingRuleSummary(rule: PricingRule): String = rule match
    case r: PricingRule.MaterialBasePrice => s"MaterialBasePrice: ${r.materialId.value} = ${r.unitPrice.value}"
    case r: PricingRule.MaterialAreaPrice => s"MaterialAreaPrice: ${r.materialId.value} = ${r.pricePerSqMeter.value}/m²"
    case r: PricingRule.MaterialSheetPrice => s"MaterialSheetPrice: ${r.materialId.value} = ${r.pricePerSheet.value}/sheet"
    case r: PricingRule.FinishSurcharge => s"FinishSurcharge: ${r.finishId.value} = ${r.surchargePerUnit.value}/unit"
    case r: PricingRule.FinishTypeSurcharge => s"FinishTypeSurcharge: ${r.finishType} = ${r.surchargePerUnit.value}/unit"
    case r: PricingRule.PrintingProcessSurcharge => s"ProcessSurcharge: ${r.processType} = ${r.surchargePerUnit.value}/unit"
    case r: PricingRule.CategorySurcharge => s"CategorySurcharge: ${r.categoryId.value} = ${r.surchargePerUnit.value}/unit"
    case r: PricingRule.QuantityTier => s"QuantityTier: ${r.minQuantity}-${r.maxQuantity.getOrElse("∞")} × ${r.multiplier}"
    case r: PricingRule.SheetQuantityTier => s"SheetQuantityTier: ${r.minSheets}-${r.maxSheets.getOrElse("∞")} × ${r.multiplier}"
    case r: PricingRule.InkConfigurationSheetPrice => s"InkConfigSheetPrice: ${r.frontColorCount}/${r.backColorCount} (${r.printingMethodId.value}) = ${r.pricePerSheet.value}/unit"
    case r: PricingRule.InkConfigurationAreaPrice => s"InkConfigAreaPrice: ${r.frontColorCount}/${r.backColorCount} (${r.printingMethodId.value}) = ${r.pricePerSqM.value}/m²"
    case r: PricingRule.CuttingSurcharge => s"CuttingSurcharge: ${r.costPerCut.value}/cut"
    case r: PricingRule.FinishTypeSetupFee => s"FinishTypeSetupFee: ${r.finishType} = ${r.setupCost.value}"
    case r: PricingRule.FinishSetupFee => s"FinishSetupFee: ${r.finishId.value} = ${r.setupCost.value}"
    case r: PricingRule.FoldTypeSurcharge => s"FoldTypeSurcharge: ${r.foldType} = ${r.surchargePerUnit.value}/unit"
    case r: PricingRule.BindingMethodSurcharge => s"BindingSurcharge: ${r.bindingMethod} = ${r.surchargePerUnit.value}/unit"
    case r: PricingRule.FoldTypeSetupFee => s"FoldTypeSetupFee: ${r.foldType} = ${r.setupCost.value}"
    case r: PricingRule.BindingMethodSetupFee => s"BindingSetupFee: ${r.bindingMethod} = ${r.setupCost.value}"
    case r: PricingRule.MinimumOrderPrice => s"MinimumOrderPrice: ${r.minTotal.value}"
    case r: PricingRule.ManufacturingSpeedSurcharge => s"SpeedSurcharge: ${r.tier} × ${r.multiplier}"
    case r: PricingRule.MaterialAreaTier => s"MaterialAreaTier: ${r.materialId.value} (${r.tiers.size} tiers)"
    case r: PricingRule.GrommetSpacingAreaPrice => s"GrommetSpacingAreaPrice: ${r.finishId.value} (${r.tiers.size} tiers)"
    case r: PricingRule.FinishLinearMeterPrice => s"FinishLinearMeterPrice: ${r.finishId.value} = ${r.pricePerMeter.value}/m"
    case r: PricingRule.ScoringCountSurcharge => s"ScoringCountSurcharge: ${r.creaseCount} crease(s) = ${r.surchargePerUnit.value}/unit"
    case _: PricingRule.ScoringSetupFee => s"ScoringSetupFee: (flat)"

  private def pricingRuleForm(existing: Option[PricingRule], index: Int): HtmlElement =
    val ruleTypeVar = Var(existing.map(pricingRuleTypeName).getOrElse("MaterialBasePrice"))
    val materialIdVar = Var(extractPricingMaterialId(existing).getOrElse(""))
    val finishIdVar = Var(extractPricingFinishId(existing).getOrElse(""))
    val amountVar = Var(extractPricingAmount(existing).map(_.toString).getOrElse("0"))
    val finishTypeVar = Var(extractPricingFinishType(existing).getOrElse(FinishType.Lamination))
    val processTypeVar = Var(extractPricingProcessType(existing).getOrElse(PrintingProcessType.Digital))
    val categoryIdVar = Var(extractPricingCategoryId(existing).getOrElse(""))
    val minQtyVar = Var(extractMinQty(existing).map(_.toString).getOrElse("1"))
    val maxQtyVar = Var(extractMaxQty(existing).map(_.toString).getOrElse(""))
    val multiplierVar = Var(extractMultiplier(existing).map(_.toString).getOrElse("1.0"))
    val frontColorVar = Var(extractFrontColor(existing).map(_.toString).getOrElse("4"))
    val backColorVar = Var(extractBackColor(existing).map(_.toString).getOrElse("4"))
    val foldTypeVar = Var(extractFoldType(existing).getOrElse(FoldType.Half))
    val printingMethodIdVar = Var(extractPrintingMethodId(existing).getOrElse("pm-offset"))
    val bindingMethodVar = Var(extractBindingMethod(existing).getOrElse(BindingMethod.SaddleStitch))
    val sheetHeightVar = Var(extractSheetHeight(existing).map(_.toString).getOrElse("1000"))
    val sheetWidthVar = Var(extractSheetWidth(existing).map(_.toString).getOrElse("700"))
    val bleedVar = Var(extractBleed(existing).map(_.toString).getOrElse("3"))

    val gutterVar = Var(extractGutter(existing).map(_.toString).getOrElse("2"))

    val ruleTypes = List(
      "MaterialBasePrice", "MaterialAreaPrice", "MaterialSheetPrice",
      "FinishSurcharge", "FinishTypeSurcharge",
      "PrintingProcessSurcharge", "CategorySurcharge",
      "QuantityTier", "SheetQuantityTier", "InkConfigurationSheetPrice", "InkConfigurationAreaPrice",
      "CuttingSurcharge", "FinishTypeSetupFee", "FinishSetupFee", "FoldTypeSurcharge",
      "BindingMethodSurcharge", "FoldTypeSetupFee", "BindingMethodSetupFee", "MinimumOrderPrice",
    )

    div(
      cls := "catalog-detail-panel",
      button(cls := "detail-panel-close", "×", onClick --> { _ =>
        CatalogEditorViewModel.setEditState(EditState.None)
      }),
      div(cls := "detail-panel-header",
        h3(if existing.isDefined then "Edit Pricing Rule" else "New Pricing Rule"),
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

      // Contextual fields based on rule type
      child <-- ruleTypeVar.signal.map { rt =>
        div(
          if Set("MaterialBasePrice", "MaterialAreaPrice", "MaterialSheetPrice").contains(rt) then
            FormComponents.textField("Material ID", materialIdVar.signal, materialIdVar.writer)
          else emptyNode,

          if rt == "MaterialSheetPrice" then
            div(
              FormComponents.numberField("Sheet Width (mm)", sheetWidthVar.signal, sheetWidthVar.writer),
              FormComponents.numberField("Sheet Height (mm)", sheetHeightVar.signal, sheetHeightVar.writer),
              FormComponents.numberField("Bleed (mm)", bleedVar.signal, bleedVar.writer),
              FormComponents.numberField("Gutter (mm)", gutterVar.signal, gutterVar.writer),
            )
          else emptyNode,

          if Set("FinishSurcharge", "FinishSetupFee").contains(rt) then
            FormComponents.textField("Finish ID", finishIdVar.signal, finishIdVar.writer)
          else emptyNode,

          if Set("FinishTypeSurcharge", "FinishTypeSetupFee").contains(rt) then
            FormComponents.enumSelectRequired[FinishType]("Finish Type", FinishType.values, finishTypeVar.signal, finishTypeVar.writer)
          else emptyNode,

          if rt == "PrintingProcessSurcharge" then
            FormComponents.enumSelectRequired[PrintingProcessType]("Process Type", PrintingProcessType.values, processTypeVar.signal, processTypeVar.writer)
          else emptyNode,

          if rt == "CategorySurcharge" then
            FormComponents.textField("Category ID", categoryIdVar.signal, categoryIdVar.writer)
          else emptyNode,

          if Set("QuantityTier", "SheetQuantityTier").contains(rt) then
            div(
              FormComponents.numberField(if rt == "SheetQuantityTier" then "Min Sheets" else "Min Quantity", minQtyVar.signal, minQtyVar.writer),
              FormComponents.numberField(if rt == "SheetQuantityTier" then "Max Sheets (empty = ∞)" else "Max Quantity (empty = ∞)", maxQtyVar.signal, maxQtyVar.writer),
              FormComponents.numberField("Multiplier", multiplierVar.signal, multiplierVar.writer),
            )
          else emptyNode,

          if Set("InkConfigurationSheetPrice", "InkConfigurationAreaPrice").contains(rt) then
            div(
              FormComponents.textField("Printing Method ID", printingMethodIdVar.signal, printingMethodIdVar.writer),
              FormComponents.numberField("Front Color Count", frontColorVar.signal, frontColorVar.writer),
              FormComponents.numberField("Back Color Count", backColorVar.signal, backColorVar.writer),
            )
          else emptyNode,

          if Set("FoldTypeSurcharge", "FoldTypeSetupFee").contains(rt) then
            FormComponents.enumSelectRequired[FoldType]("Fold Type", FoldType.values, foldTypeVar.signal, foldTypeVar.writer)
          else emptyNode,

          if Set("BindingMethodSurcharge", "BindingMethodSetupFee").contains(rt) then
            FormComponents.enumSelectRequired[BindingMethod]("Binding Method", BindingMethod.values, bindingMethodVar.signal, bindingMethodVar.writer)
          else emptyNode,

          if !Set("QuantityTier", "SheetQuantityTier").contains(rt) then
            FormComponents.numberField("Amount", amountVar.signal, amountVar.writer)
          else emptyNode,
        )
      },

      ), // close detail-panel-section

      div(
        cls := "detail-panel-actions",
        FormComponents.actionButton("Save", () => {
          buildPricingRule(
            ruleTypeVar.now(), materialIdVar.now(), finishIdVar.now(), amountVar.now(),
            finishTypeVar.now(), processTypeVar.now(), categoryIdVar.now(),
            minQtyVar.now(), maxQtyVar.now(), multiplierVar.now(),
            frontColorVar.now(), backColorVar.now(), printingMethodIdVar.now(),
            foldTypeVar.now(), bindingMethodVar.now(),
            sheetWidthVar.now(), sheetHeightVar.now(), bleedVar.now(), gutterVar.now(),
          ).foreach { rule =>
            if existing.isDefined then CatalogEditorViewModel.updatePricingRule(index, rule)
            else CatalogEditorViewModel.addPricingRule(rule)
          }
        }),
        FormComponents.dangerButton("Cancel", () =>
          CatalogEditorViewModel.setEditState(EditState.None)
        ),
      ),
    )

  private def buildPricingRule(
    ruleType: String, materialId: String, finishId: String, amount: String,
    finishType: FinishType, processType: PrintingProcessType, categoryId: String,
    minQty: String, maxQty: String, multiplier: String,
    frontColor: String, backColor: String, printingMethodId: String,
    foldType: FoldType, bindingMethod: BindingMethod,
    sheetWidth: String, sheetHeight: String, bleed: String, gutter: String,
  ): Option[PricingRule] =
    val money = scala.util.Try(Money(BigDecimal(amount))).toOption
    val mult = scala.util.Try(BigDecimal(multiplier)).toOption
    ruleType match
      case "MaterialBasePrice" => money.filter(_ => materialId.nonEmpty).map(m => PricingRule.MaterialBasePrice(MaterialId.unsafe(materialId), m))
      case "MaterialAreaPrice" => money.filter(_ => materialId.nonEmpty).map(m => PricingRule.MaterialAreaPrice(MaterialId.unsafe(materialId), m))
      case "MaterialSheetPrice" =>
        for
          m <- money
          if materialId.nonEmpty
          sw <- sheetWidth.toDoubleOption
          sh <- sheetHeight.toDoubleOption
          bl <- bleed.toDoubleOption
          gt <- gutter.toDoubleOption
        yield PricingRule.MaterialSheetPrice(MaterialId.unsafe(materialId), m, sw, sh, bl, gt)
      case "FinishSurcharge" => money.filter(_ => finishId.nonEmpty).map(m => PricingRule.FinishSurcharge(FinishId.unsafe(finishId), m))
      case "FinishTypeSurcharge" => money.map(m => PricingRule.FinishTypeSurcharge(finishType, m))
      case "PrintingProcessSurcharge" => money.map(m => PricingRule.PrintingProcessSurcharge(processType, m))
      case "CategorySurcharge" => money.filter(_ => categoryId.nonEmpty).map(m => PricingRule.CategorySurcharge(CategoryId.unsafe(categoryId), m))
      case "QuantityTier" =>
        for
          m <- mult
          min <- minQty.toIntOption
        yield PricingRule.QuantityTier(min, maxQty.toIntOption, m)
      case "SheetQuantityTier" =>
        for
          m <- mult
          min <- minQty.toIntOption
        yield PricingRule.SheetQuantityTier(min, maxQty.toIntOption, m)
      case "InkConfigurationSheetPrice" =>
        for
          m <- money
          fc <- frontColor.toIntOption
          bc <- backColor.toIntOption
          if printingMethodId.nonEmpty
        yield PricingRule.InkConfigurationSheetPrice(PrintingMethodId.unsafe(printingMethodId), fc, bc, m)
      case "InkConfigurationAreaPrice" =>
        for
          m <- money
          fc <- frontColor.toIntOption
          bc <- backColor.toIntOption
          if printingMethodId.nonEmpty
        yield PricingRule.InkConfigurationAreaPrice(PrintingMethodId.unsafe(printingMethodId), fc, bc, m)
      case "CuttingSurcharge" => money.map(m => PricingRule.CuttingSurcharge(m))
      case "FinishTypeSetupFee" => money.map(m => PricingRule.FinishTypeSetupFee(finishType, m))
      case "FinishSetupFee" => money.filter(_ => finishId.nonEmpty).map(m => PricingRule.FinishSetupFee(FinishId.unsafe(finishId), m))
      case "FoldTypeSurcharge" => money.map(m => PricingRule.FoldTypeSurcharge(foldType, m))
      case "BindingMethodSurcharge" => money.map(m => PricingRule.BindingMethodSurcharge(bindingMethod, m))
      case "FoldTypeSetupFee" => money.map(m => PricingRule.FoldTypeSetupFee(foldType, m))
      case "BindingMethodSetupFee" => money.map(m => PricingRule.BindingMethodSetupFee(bindingMethod, m))
      case "MinimumOrderPrice" => money.map(m => PricingRule.MinimumOrderPrice(m))
      case _ => None

  // Extractors for populating form from existing rules
  private def pricingRuleTypeName(rule: PricingRule): String = rule match
    case _: PricingRule.MaterialBasePrice => "MaterialBasePrice"
    case _: PricingRule.MaterialAreaPrice => "MaterialAreaPrice"
    case _: PricingRule.MaterialSheetPrice => "MaterialSheetPrice"
    case _: PricingRule.FinishSurcharge => "FinishSurcharge"
    case _: PricingRule.FinishTypeSurcharge => "FinishTypeSurcharge"
    case _: PricingRule.PrintingProcessSurcharge => "PrintingProcessSurcharge"
    case _: PricingRule.CategorySurcharge => "CategorySurcharge"
    case _: PricingRule.QuantityTier => "QuantityTier"
    case _: PricingRule.SheetQuantityTier => "SheetQuantityTier"
    case _: PricingRule.InkConfigurationSheetPrice => "InkConfigurationSheetPrice"
    case _: PricingRule.InkConfigurationAreaPrice => "InkConfigurationAreaPrice"
    case _: PricingRule.CuttingSurcharge => "CuttingSurcharge"
    case _: PricingRule.FinishTypeSetupFee => "FinishTypeSetupFee"
    case _: PricingRule.FinishSetupFee => "FinishSetupFee"
    case _: PricingRule.FoldTypeSurcharge => "FoldTypeSurcharge"
    case _: PricingRule.BindingMethodSurcharge => "BindingMethodSurcharge"
    case _: PricingRule.FoldTypeSetupFee => "FoldTypeSetupFee"
    case _: PricingRule.BindingMethodSetupFee => "BindingMethodSetupFee"
    case _: PricingRule.MinimumOrderPrice => "MinimumOrderPrice"
    case _: PricingRule.ManufacturingSpeedSurcharge => "ManufacturingSpeedSurcharge"
    case _: PricingRule.MaterialAreaTier => "MaterialAreaTier"
    case _: PricingRule.GrommetSpacingAreaPrice => "GrommetSpacingAreaPrice"
    case _: PricingRule.FinishLinearMeterPrice => "FinishLinearMeterPrice"
    case _: PricingRule.ScoringCountSurcharge => "ScoringCountSurcharge"
    case _: PricingRule.ScoringSetupFee => "ScoringSetupFee"

  private def extractPricingMaterialId(rule: Option[PricingRule]): Option[String] = rule.collect {
    case r: PricingRule.MaterialBasePrice => r.materialId.value
    case r: PricingRule.MaterialAreaPrice => r.materialId.value
    case r: PricingRule.MaterialSheetPrice => r.materialId.value
  }

  private def extractPricingFinishId(rule: Option[PricingRule]): Option[String] = rule.collect {
    case r: PricingRule.FinishSurcharge => r.finishId.value
    case r: PricingRule.FinishSetupFee => r.finishId.value
  }

  private def extractPricingAmount(rule: Option[PricingRule]): Option[BigDecimal] = rule.collect {
    case r: PricingRule.MaterialBasePrice => r.unitPrice.value
    case r: PricingRule.MaterialAreaPrice => r.pricePerSqMeter.value
    case r: PricingRule.MaterialSheetPrice => r.pricePerSheet.value
    case r: PricingRule.FinishSurcharge => r.surchargePerUnit.value
    case r: PricingRule.FinishTypeSurcharge => r.surchargePerUnit.value
    case r: PricingRule.PrintingProcessSurcharge => r.surchargePerUnit.value
    case r: PricingRule.CategorySurcharge => r.surchargePerUnit.value
    case r: PricingRule.CuttingSurcharge => r.costPerCut.value
    case r: PricingRule.FinishTypeSetupFee => r.setupCost.value
    case r: PricingRule.FinishSetupFee => r.setupCost.value
    case r: PricingRule.FoldTypeSurcharge => r.surchargePerUnit.value
    case r: PricingRule.BindingMethodSurcharge => r.surchargePerUnit.value
    case r: PricingRule.FoldTypeSetupFee => r.setupCost.value
    case r: PricingRule.BindingMethodSetupFee => r.setupCost.value
    case r: PricingRule.MinimumOrderPrice => r.minTotal.value
  }

  private def extractPrintingMethodId(rule: Option[PricingRule]): Option[String] = rule.collect {
    case r: PricingRule.InkConfigurationSheetPrice => r.printingMethodId.value
    case r: PricingRule.InkConfigurationAreaPrice => r.printingMethodId.value
  }

  private def extractPricingFinishType(rule: Option[PricingRule]): Option[FinishType] = rule.collect {
    case r: PricingRule.FinishTypeSurcharge => r.finishType
    case r: PricingRule.FinishTypeSetupFee => r.finishType
  }

  private def extractPricingProcessType(rule: Option[PricingRule]): Option[PrintingProcessType] = rule.collect {
    case r: PricingRule.PrintingProcessSurcharge => r.processType
  }

  private def extractPricingCategoryId(rule: Option[PricingRule]): Option[String] = rule.collect {
    case r: PricingRule.CategorySurcharge => r.categoryId.value
  }

  private def extractMinQty(rule: Option[PricingRule]): Option[Int] = rule.collect {
    case r: PricingRule.QuantityTier => r.minQuantity
    case r: PricingRule.SheetQuantityTier => r.minSheets
  }

  private def extractMaxQty(rule: Option[PricingRule]): Option[Int] = rule.collect {
    case r: PricingRule.QuantityTier => r.maxQuantity
    case r: PricingRule.SheetQuantityTier => r.maxSheets
  }.flatten

  private def extractMultiplier(rule: Option[PricingRule]): Option[BigDecimal] = rule.collect {
    case r: PricingRule.QuantityTier => r.multiplier
    case r: PricingRule.SheetQuantityTier => r.multiplier
  }

  private def extractFrontColor(rule: Option[PricingRule]): Option[Int] = rule.collect {
    case r: PricingRule.InkConfigurationSheetPrice => r.frontColorCount
    case r: PricingRule.InkConfigurationAreaPrice => r.frontColorCount
  }

  private def extractBackColor(rule: Option[PricingRule]): Option[Int] = rule.collect {
    case r: PricingRule.InkConfigurationSheetPrice => r.backColorCount
    case r: PricingRule.InkConfigurationAreaPrice => r.backColorCount
  }

  private def extractFoldType(rule: Option[PricingRule]): Option[FoldType] = rule.collect {
    case r: PricingRule.FoldTypeSurcharge => r.foldType
    case r: PricingRule.FoldTypeSetupFee => r.foldType
  }

  private def extractBindingMethod(rule: Option[PricingRule]): Option[BindingMethod] = rule.collect {
    case r: PricingRule.BindingMethodSurcharge => r.bindingMethod
    case r: PricingRule.BindingMethodSetupFee => r.bindingMethod
  }

  private def extractSheetWidth(rule: Option[PricingRule]): Option[Double] = rule.collect {
    case r: PricingRule.MaterialSheetPrice => r.sheetWidthMm
  }

  private def extractSheetHeight(rule: Option[PricingRule]): Option[Double] = rule.collect {
    case r: PricingRule.MaterialSheetPrice => r.sheetHeightMm
  }

  private def extractBleed(rule: Option[PricingRule]): Option[Double] = rule.collect {
    case r: PricingRule.MaterialSheetPrice => r.bleedMm
  }

  private def extractGutter(rule: Option[PricingRule]): Option[Double] = rule.collect {
    case r: PricingRule.MaterialSheetPrice => r.gutterMm
  }
