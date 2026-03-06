package mpbuilder.ui.catalog

import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.rules.*

/** Represents which section of the catalog editor is currently active. */
enum CatalogSection:
  case Materials, Finishes, PrintingMethods, Categories, Pricelist, ImportExport

/** Editing state for a single material. */
final case class MaterialEditState(
  id: String,
  nameEn: String,
  nameCs: String,
  family: MaterialFamily,
  weight: Option[Int],
  properties: Set[MaterialProperty],
)

object MaterialEditState:
  def fromMaterial(m: Material): MaterialEditState =
    MaterialEditState(
      id = m.id.value,
      nameEn = m.name(Language.En),
      nameCs = m.name(Language.Cs),
      family = m.family,
      weight = m.weight.map(_.gsm),
      properties = m.properties,
    )

  def toMaterial(s: MaterialEditState): Material =
    Material(
      id = MaterialId.unsafe(s.id),
      name = LocalizedString(s.nameEn, s.nameCs),
      family = s.family,
      weight = s.weight.map(PaperWeight.unsafe),
      properties = s.properties,
    )

  def empty: MaterialEditState =
    MaterialEditState("", "", "", MaterialFamily.Paper, None, Set.empty)

/** Editing state for a single finish. */
final case class FinishEditState(
  id: String,
  nameEn: String,
  nameCs: String,
  finishType: FinishType,
  side: FinishSide,
)

object FinishEditState:
  def fromFinish(f: Finish): FinishEditState =
    FinishEditState(
      id = f.id.value,
      nameEn = f.name(Language.En),
      nameCs = f.name(Language.Cs),
      finishType = f.finishType,
      side = f.side,
    )

  def toFinish(s: FinishEditState): Finish =
    Finish(
      id = FinishId.unsafe(s.id),
      name = LocalizedString(s.nameEn, s.nameCs),
      finishType = s.finishType,
      side = s.side,
    )

  def empty: FinishEditState =
    FinishEditState("", "", "", FinishType.Lamination, FinishSide.Both)

/** Editing state for a printing method. */
final case class PrintingMethodEditState(
  id: String,
  nameEn: String,
  nameCs: String,
  processType: PrintingProcessType,
  maxColorCount: Option[Int],
)

object PrintingMethodEditState:
  def fromPrintingMethod(pm: PrintingMethod): PrintingMethodEditState =
    PrintingMethodEditState(
      id = pm.id.value,
      nameEn = pm.name(Language.En),
      nameCs = pm.name(Language.Cs),
      processType = pm.processType,
      maxColorCount = pm.maxColorCount,
    )

  def toPrintingMethod(s: PrintingMethodEditState): PrintingMethod =
    PrintingMethod(
      id = PrintingMethodId.unsafe(s.id),
      name = LocalizedString(s.nameEn, s.nameCs),
      processType = s.processType,
      maxColorCount = s.maxColorCount,
    )

  def empty: PrintingMethodEditState =
    PrintingMethodEditState("", "", "", PrintingProcessType.Digital, None)

/** Editing state for a component template within a category. */
final case class ComponentTemplateEditState(
  role: ComponentRole,
  allowedMaterialIds: Set[String],
  allowedFinishIds: Set[String],
)

object ComponentTemplateEditState:
  def fromTemplate(ct: ComponentTemplate): ComponentTemplateEditState =
    ComponentTemplateEditState(
      role = ct.role,
      allowedMaterialIds = ct.allowedMaterialIds.map(_.value),
      allowedFinishIds = ct.allowedFinishIds.map(_.value),
    )

  def toTemplate(s: ComponentTemplateEditState): ComponentTemplate =
    ComponentTemplate(
      role = s.role,
      allowedMaterialIds = s.allowedMaterialIds.map(MaterialId.unsafe),
      allowedFinishIds = s.allowedFinishIds.map(FinishId.unsafe),
    )

/** Editing state for a product category. */
final case class CategoryEditState(
  id: String,
  nameEn: String,
  nameCs: String,
  components: List[ComponentTemplateEditState],
  requiredSpecKinds: Set[SpecKind],
  allowedPrintingMethodIds: Set[String],
)

object CategoryEditState:
  def fromCategory(c: ProductCategory): CategoryEditState =
    CategoryEditState(
      id = c.id.value,
      nameEn = c.name(Language.En),
      nameCs = c.name(Language.Cs),
      components = c.components.map(ComponentTemplateEditState.fromTemplate),
      requiredSpecKinds = c.requiredSpecKinds,
      allowedPrintingMethodIds = c.allowedPrintingMethodIds.map(_.value),
    )

  def toCategory(s: CategoryEditState): ProductCategory =
    ProductCategory(
      id = CategoryId.unsafe(s.id),
      name = LocalizedString(s.nameEn, s.nameCs),
      components = s.components.map(ComponentTemplateEditState.toTemplate),
      requiredSpecKinds = s.requiredSpecKinds,
      allowedPrintingMethodIds = s.allowedPrintingMethodIds.map(PrintingMethodId.unsafe),
    )

  def empty: CategoryEditState =
    CategoryEditState("", "", "", List(ComponentTemplateEditState(ComponentRole.Main, Set.empty, Set.empty)), Set.empty, Set.empty)

/** Overall catalog editor state. */
final case class CatalogEditorState(
  materials: List[MaterialEditState],
  finishes: List[FinishEditState],
  printingMethods: List[PrintingMethodEditState],
  categories: List[CategoryEditState],
  pricelist: PricelistEditState,
  activeSection: CatalogSection,
  editingItemIndex: Option[Int],
  message: Option[String],
)

/** Editing state for a pricelist. */
final case class PricelistEditState(
  rules: List[PricingRuleEditState],
  currency: Currency,
  version: String,
)

object PricelistEditState:
  def fromPricelist(pl: Pricelist): PricelistEditState =
    PricelistEditState(
      rules = pl.rules.map(PricingRuleEditState.fromRule),
      currency = pl.currency,
      version = pl.version,
    )

  def toPricelist(s: PricelistEditState): Pricelist =
    Pricelist(
      rules = s.rules.flatMap(r => PricingRuleEditState.toRule(r)),
      currency = s.currency,
      version = s.version,
    )

  def empty: PricelistEditState =
    PricelistEditState(Nil, Currency.USD, "1.0")

/** Simplified editing state for a pricing rule using string fields. */
final case class PricingRuleEditState(
  ruleType: String,
  fields: Map[String, String],
)

object PricingRuleEditState:
  val ruleTypes: List[String] = List(
    "MaterialBasePrice", "MaterialAreaPrice", "MaterialSheetPrice",
    "FinishSurcharge", "FinishTypeSurcharge",
    "PrintingProcessSurcharge", "CategorySurcharge",
    "QuantityTier", "SheetQuantityTier",
    "InkConfigurationFactor", "CuttingSurcharge",
    "FinishTypeSetupFee", "FinishSetupFee",
    "FoldTypeSurcharge", "BindingMethodSurcharge",
    "FoldTypeSetupFee", "BindingMethodSetupFee",
    "MinimumOrderPrice",
  )

  def fieldsForType(ruleType: String): List[(String, String)] = ruleType match
    case "MaterialBasePrice" => List("materialId" -> "Material ID", "unitPrice" -> "Unit Price")
    case "MaterialAreaPrice" => List("materialId" -> "Material ID", "pricePerSqMeter" -> "Price / m²")
    case "MaterialSheetPrice" => List("materialId" -> "Material ID", "pricePerSheet" -> "Price / Sheet",
      "sheetWidthMm" -> "Sheet Width (mm)", "sheetHeightMm" -> "Sheet Height (mm)",
      "bleedMm" -> "Bleed (mm)", "gutterMm" -> "Gutter (mm)")
    case "FinishSurcharge" => List("finishId" -> "Finish ID", "surchargePerUnit" -> "Surcharge / Unit")
    case "FinishTypeSurcharge" => List("finishType" -> "Finish Type", "surchargePerUnit" -> "Surcharge / Unit")
    case "PrintingProcessSurcharge" => List("processType" -> "Process Type", "surchargePerUnit" -> "Surcharge / Unit")
    case "CategorySurcharge" => List("categoryId" -> "Category ID", "surchargePerUnit" -> "Surcharge / Unit")
    case "QuantityTier" => List("minQuantity" -> "Min Quantity", "maxQuantity" -> "Max Quantity (empty=none)", "multiplier" -> "Multiplier")
    case "SheetQuantityTier" => List("minSheets" -> "Min Sheets", "maxSheets" -> "Max Sheets (empty=none)", "multiplier" -> "Multiplier")
    case "InkConfigurationFactor" => List("frontColorCount" -> "Front Colors", "backColorCount" -> "Back Colors", "materialMultiplier" -> "Multiplier")
    case "CuttingSurcharge" => List("costPerCut" -> "Cost / Cut")
    case "FinishTypeSetupFee" => List("finishType" -> "Finish Type", "setupCost" -> "Setup Cost")
    case "FinishSetupFee" => List("finishId" -> "Finish ID", "setupCost" -> "Setup Cost")
    case "FoldTypeSurcharge" => List("foldType" -> "Fold Type", "surchargePerUnit" -> "Surcharge / Unit")
    case "BindingMethodSurcharge" => List("bindingMethod" -> "Binding Method", "surchargePerUnit" -> "Surcharge / Unit")
    case "FoldTypeSetupFee" => List("foldType" -> "Fold Type", "setupCost" -> "Setup Cost")
    case "BindingMethodSetupFee" => List("bindingMethod" -> "Binding Method", "setupCost" -> "Setup Cost")
    case "MinimumOrderPrice" => List("minTotal" -> "Minimum Total")
    case _ => Nil

  def fromRule(rule: PricingRule): PricingRuleEditState = rule match
    case PricingRule.MaterialBasePrice(mid, up) =>
      PricingRuleEditState("MaterialBasePrice", Map("materialId" -> mid.value, "unitPrice" -> up.value.toString))
    case PricingRule.MaterialAreaPrice(mid, p) =>
      PricingRuleEditState("MaterialAreaPrice", Map("materialId" -> mid.value, "pricePerSqMeter" -> p.value.toString))
    case PricingRule.MaterialSheetPrice(mid, pps, sw, sh, b, g) =>
      PricingRuleEditState("MaterialSheetPrice", Map(
        "materialId" -> mid.value, "pricePerSheet" -> pps.value.toString,
        "sheetWidthMm" -> sw.toString, "sheetHeightMm" -> sh.toString,
        "bleedMm" -> b.toString, "gutterMm" -> g.toString))
    case PricingRule.FinishSurcharge(fid, s) =>
      PricingRuleEditState("FinishSurcharge", Map("finishId" -> fid.value, "surchargePerUnit" -> s.value.toString))
    case PricingRule.FinishTypeSurcharge(ft, s) =>
      PricingRuleEditState("FinishTypeSurcharge", Map("finishType" -> ft.toString, "surchargePerUnit" -> s.value.toString))
    case PricingRule.PrintingProcessSurcharge(pt, s) =>
      PricingRuleEditState("PrintingProcessSurcharge", Map("processType" -> pt.toString, "surchargePerUnit" -> s.value.toString))
    case PricingRule.CategorySurcharge(cid, s) =>
      PricingRuleEditState("CategorySurcharge", Map("categoryId" -> cid.value, "surchargePerUnit" -> s.value.toString))
    case PricingRule.QuantityTier(min, max, mult) =>
      PricingRuleEditState("QuantityTier", Map("minQuantity" -> min.toString, "maxQuantity" -> max.map(_.toString).getOrElse(""), "multiplier" -> mult.toString))
    case PricingRule.SheetQuantityTier(min, max, mult) =>
      PricingRuleEditState("SheetQuantityTier", Map("minSheets" -> min.toString, "maxSheets" -> max.map(_.toString).getOrElse(""), "multiplier" -> mult.toString))
    case PricingRule.InkConfigurationFactor(fc, bc, mult) =>
      PricingRuleEditState("InkConfigurationFactor", Map("frontColorCount" -> fc.toString, "backColorCount" -> bc.toString, "materialMultiplier" -> mult.toString))
    case PricingRule.CuttingSurcharge(c) =>
      PricingRuleEditState("CuttingSurcharge", Map("costPerCut" -> c.value.toString))
    case PricingRule.FinishTypeSetupFee(ft, c) =>
      PricingRuleEditState("FinishTypeSetupFee", Map("finishType" -> ft.toString, "setupCost" -> c.value.toString))
    case PricingRule.FinishSetupFee(fid, c) =>
      PricingRuleEditState("FinishSetupFee", Map("finishId" -> fid.value, "setupCost" -> c.value.toString))
    case PricingRule.FoldTypeSurcharge(ft, s) =>
      PricingRuleEditState("FoldTypeSurcharge", Map("foldType" -> ft.toString, "surchargePerUnit" -> s.value.toString))
    case PricingRule.BindingMethodSurcharge(bm, s) =>
      PricingRuleEditState("BindingMethodSurcharge", Map("bindingMethod" -> bm.toString, "surchargePerUnit" -> s.value.toString))
    case PricingRule.FoldTypeSetupFee(ft, c) =>
      PricingRuleEditState("FoldTypeSetupFee", Map("foldType" -> ft.toString, "setupCost" -> c.value.toString))
    case PricingRule.BindingMethodSetupFee(bm, c) =>
      PricingRuleEditState("BindingMethodSetupFee", Map("bindingMethod" -> bm.toString, "setupCost" -> c.value.toString))
    case PricingRule.MinimumOrderPrice(m) =>
      PricingRuleEditState("MinimumOrderPrice", Map("minTotal" -> m.value.toString))

  def toRule(s: PricingRuleEditState): Option[PricingRule] =
    def mid(k: String) = s.fields.get(k).filter(_.nonEmpty).map(MaterialId.unsafe)
    def fid(k: String) = s.fields.get(k).filter(_.nonEmpty).map(FinishId.unsafe)
    def cid(k: String) = s.fields.get(k).filter(_.nonEmpty).map(CategoryId.unsafe)
    def money(k: String) = s.fields.get(k).filter(_.nonEmpty).map(v => Money(BigDecimal(v)))
    def dbl(k: String) = s.fields.get(k).flatMap(_.toDoubleOption)
    def int(k: String) = s.fields.get(k).flatMap(_.toIntOption)
    def optInt(k: String) = Some(s.fields.get(k).flatMap(v => if v.isEmpty then None else v.toIntOption))
    def bd(k: String) = s.fields.get(k).filter(_.nonEmpty).map(BigDecimal(_))
    def ft(k: String) = s.fields.get(k).filter(_.nonEmpty).flatMap(v => scala.util.Try(FinishType.valueOf(v)).toOption)
    def pt(k: String) = s.fields.get(k).filter(_.nonEmpty).flatMap(v => scala.util.Try(PrintingProcessType.valueOf(v)).toOption)
    def fdt(k: String) = s.fields.get(k).filter(_.nonEmpty).flatMap(v => scala.util.Try(FoldType.valueOf(v)).toOption)
    def bm(k: String) = s.fields.get(k).filter(_.nonEmpty).flatMap(v => scala.util.Try(BindingMethod.valueOf(v)).toOption)

    try s.ruleType match
      case "MaterialBasePrice" =>
        for m <- mid("materialId"); u <- money("unitPrice") yield PricingRule.MaterialBasePrice(m, u)
      case "MaterialAreaPrice" =>
        for m <- mid("materialId"); p <- money("pricePerSqMeter") yield PricingRule.MaterialAreaPrice(m, p)
      case "MaterialSheetPrice" =>
        for
          m <- mid("materialId"); p <- money("pricePerSheet")
          sw <- dbl("sheetWidthMm"); sh <- dbl("sheetHeightMm")
          b <- dbl("bleedMm"); g <- dbl("gutterMm")
        yield PricingRule.MaterialSheetPrice(m, p, sw, sh, b, g)
      case "FinishSurcharge" =>
        for f <- fid("finishId"); s <- money("surchargePerUnit") yield PricingRule.FinishSurcharge(f, s)
      case "FinishTypeSurcharge" =>
        for f <- ft("finishType"); s <- money("surchargePerUnit") yield PricingRule.FinishTypeSurcharge(f, s)
      case "PrintingProcessSurcharge" =>
        for p <- pt("processType"); s <- money("surchargePerUnit") yield PricingRule.PrintingProcessSurcharge(p, s)
      case "CategorySurcharge" =>
        for c <- cid("categoryId"); s <- money("surchargePerUnit") yield PricingRule.CategorySurcharge(c, s)
      case "QuantityTier" =>
        for min <- int("minQuantity"); max <- optInt("maxQuantity"); m <- bd("multiplier") yield PricingRule.QuantityTier(min, max, m)
      case "SheetQuantityTier" =>
        for min <- int("minSheets"); max <- optInt("maxSheets"); m <- bd("multiplier") yield PricingRule.SheetQuantityTier(min, max, m)
      case "InkConfigurationFactor" =>
        for fc <- int("frontColorCount"); bc <- int("backColorCount"); m <- bd("materialMultiplier") yield PricingRule.InkConfigurationFactor(fc, bc, m)
      case "CuttingSurcharge" =>
        money("costPerCut").map(PricingRule.CuttingSurcharge(_))
      case "FinishTypeSetupFee" =>
        for f <- ft("finishType"); c <- money("setupCost") yield PricingRule.FinishTypeSetupFee(f, c)
      case "FinishSetupFee" =>
        for f <- fid("finishId"); c <- money("setupCost") yield PricingRule.FinishSetupFee(f, c)
      case "FoldTypeSurcharge" =>
        for f <- fdt("foldType"); s <- money("surchargePerUnit") yield PricingRule.FoldTypeSurcharge(f, s)
      case "BindingMethodSurcharge" =>
        for b <- bm("bindingMethod"); s <- money("surchargePerUnit") yield PricingRule.BindingMethodSurcharge(b, s)
      case "FoldTypeSetupFee" =>
        for f <- fdt("foldType"); c <- money("setupCost") yield PricingRule.FoldTypeSetupFee(f, c)
      case "BindingMethodSetupFee" =>
        for b <- bm("bindingMethod"); c <- money("setupCost") yield PricingRule.BindingMethodSetupFee(b, c)
      case "MinimumOrderPrice" =>
        money("minTotal").map(PricingRule.MinimumOrderPrice(_))
      case _ => None
    catch case _: Exception => None

  def empty(ruleType: String): PricingRuleEditState =
    PricingRuleEditState(ruleType, fieldsForType(ruleType).map(_._1 -> "").toMap)
