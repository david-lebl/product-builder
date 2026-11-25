package mpbuilder.catalog

import mpbuilder.shared.*

// ── Context-local IDs ────────────────────────────────────────────────────────

opaque type RuleId = String
object RuleId:
  def apply(raw: String): Validation[String, RuleId] =
    if raw.trim.nonEmpty then Validation.succeed(raw.trim)
    else Validation.fail("RuleId must not be empty")
  def unsafe(raw: String): RuleId = raw
  extension (id: RuleId) def value: String = id

opaque type PresetId = String
object PresetId:
  def apply(raw: String): Validation[String, PresetId] =
    if raw.trim.nonEmpty then Validation.succeed(raw.trim)
    else Validation.fail("PresetId must not be empty")
  def unsafe(raw: String): PresetId = raw
  extension (id: PresetId) def value: String = id

opaque type ConfigurationId = String
object ConfigurationId:
  def apply(raw: String): Validation[String, ConfigurationId] =
    if raw.trim.nonEmpty then Validation.succeed(raw.trim)
    else Validation.fail("ConfigurationId must not be empty")
  def unsafe(raw: String): ConfigurationId = raw
  extension (id: ConfigurationId) def value: String = id

// ── Materials & Stock Formats ────────────────────────────────────────────────

enum MaterialFamily:
  case Paper, Synthetic, Adhesive, Vinyl, Cardboard, RigidBoard, Fabric, Ceramic, Metal, Wood, Hardware

/** Intrinsic substrate surface coating (distinct from post-press applied finishes). */
enum SurfaceCoating:
  case Uncoated, CoatedGloss, CoatedSilk, CoatedMatte, NaturalTextured, TransparentFilm, Metallic, Glazed

enum MaterialProperty:
  case Recyclable, WaterResistant, TearResistant, SelfAdhesive, Transparent, SmoothSurface, Textured, Writable, OutdoorRated, FoodSafe, Carbonless

/** Physical weight or thickness representation appropriate to the substrate family. */
enum MaterialWeight:
  /** Paper, fabric, or vinyl banner grammage (g/m²). */
  case Grammage(weight: PaperWeight)
  /** Rigid board or plastic sheet thickness (mm) with optional area density for weight estimates. */
  case Thickness(thicknessMm: PositiveMm, densityGsm: Option[PaperWeight] = None)
  /** Fixed weight per discrete blank item (e.g. ceramic mug 320 g, aluminum roll-up stand 2000 g). */
  case UnitWeight(grams: Grams)

  def grammageGsm: Option[Int] = this match
    case Grammage(w)                => Some(w.gsm)
    case Thickness(_, Some(w))      => Some(w.gsm)
    case Thickness(_, None)         => None
    case UnitWeight(_)              => None

/** Physical stock/media format owned by the catalog (NOT duplicated across currency pricelists). */
enum MediaFormat:
  case SheetStock(
      formatId: StockFormatId,
      sheetSize: Dimension2D,
      defaultBleed: NonNegativeMm,
      defaultGutter: NonNegativeMm,
  )
  case RollStock(
      formatId: StockFormatId,
      rollWidth: PositiveMm,
      printableWidth: PositiveMm,
      maxLength: Option[PositiveMm] = None,
  )
  case DiscreteBlank(
      blankSize: Option[Dimension2D] = None,
  )

final case class Material(
    id: MaterialId,
    name: LocalizedString,
    family: MaterialFamily,
    surfaceCoating: SurfaceCoating,
    weight: MaterialWeight,
    stockFormats: NonEmptyChunk[MediaFormat],
    properties: Set[MaterialProperty] = Set.empty,
    description: Option[LocalizedString] = None,
    enabled: Boolean = true,
)

// ── Printing Methods & Proper Ink Configuration ──────────────────────────────

enum PrintingProcessType:
  case Offset, Digital, Letterpress, ScreenPrint
  case UVCurableInkjet, LatexInkjet, SolventInkjet
  case DTG, Sublimation, HeatTransfer

  def isLargeFormatInkjet: Boolean = this match
    case UVCurableInkjet | LatexInkjet | SolventInkjet => true
    case _                                             => false

enum ProcessInk:
  case None
  case Grayscale
  case CMYK
  case ExtendedGamut(channels: Int)

  def channelCount: Int = this match
    case None             => 0
    case Grayscale        => 1
    case CMYK             => 4
    case ExtendedGamut(n) => n

enum SpecialInk:
  case White, MetallicGold, MetallicSilver, ClearSpotVarnish, Fluorescent

/** Ink channels printed on a single side of a substrate.
  *
  * Eliminates the legacy hack of encoding white underbase into the `back` slot,
  * so `4+W/0`, `4+W/4+W`, and `4+2PMS/4+0` are all unambiguous and exact.
  */
final case class SideInk(
    process: ProcessInk,
    spotColors: Int = 0,
    specialInks: Set[SpecialInk] = Set.empty,
):
  def totalChannels: Int = process.channelCount + math.max(0, spotColors) + specialInks.size
  def isBlank: Boolean   = totalChannels == 0

  def notation: String =
    val base = process.channelCount + math.max(0, spotColors)
    val specials = specialInks.toList.sortBy(_.ordinal).map {
      case SpecialInk.White            => "W"
      case SpecialInk.MetallicGold     => "Au"
      case SpecialInk.MetallicSilver   => "Ag"
      case SpecialInk.ClearSpotVarnish => "V"
      case SpecialInk.Fluorescent      => "Fl"
    }
    if specials.isEmpty then base.toString
    else s"$base+${specials.mkString("+")}"

object SideInk:
  val blank: SideInk     = SideInk(ProcessInk.None)
  val grayscale: SideInk = SideInk(ProcessInk.Grayscale)
  val cmyk: SideInk      = SideInk(ProcessInk.CMYK)
  val cmykWhite: SideInk = SideInk(ProcessInk.CMYK, specialInks = Set(SpecialInk.White))
  def pms(spots: Int): SideInk = SideInk(ProcessInk.None, spotColors = spots)

final case class InkConfiguration(front: SideInk, back: SideInk):
  def notation: String        = s"${front.notation}/${back.notation}"
  def isSingleSided: Boolean  = back.isBlank
  def isDoubleSided: Boolean  = !back.isBlank
  def maxChannelsPerSide: Int = math.max(front.totalChannels, back.totalChannels)
  def allSpecialInks: Set[SpecialInk] = front.specialInks ++ back.specialInks
  def usesProcess(p: ProcessInk): Boolean = front.process == p || back.process == p

object InkConfiguration:
  val unprinted: InkConfiguration   = InkConfiguration(SideInk.blank, SideInk.blank)
  val cmyk4_0: InkConfiguration     = InkConfiguration(SideInk.cmyk, SideInk.blank)
  val cmyk4_4: InkConfiguration     = InkConfiguration(SideInk.cmyk, SideInk.cmyk)
  val cmyk4_1: InkConfiguration     = InkConfiguration(SideInk.cmyk, SideInk.grayscale)
  val cmyk4W_0: InkConfiguration    = InkConfiguration(SideInk.cmykWhite, SideInk.blank)
  val cmyk4W_4W: InkConfiguration   = InkConfiguration(SideInk.cmykWhite, SideInk.cmykWhite)
  val mono1_0: InkConfiguration     = InkConfiguration(SideInk.grayscale, SideInk.blank)
  val mono1_1: InkConfiguration     = InkConfiguration(SideInk.grayscale, SideInk.grayscale)

final case class PrintingMethod(
    id: PrintingMethodId,
    name: LocalizedString,
    processType: PrintingProcessType,
    maxColorsPerSide: Option[Int],
    supportsDoubleSided: Boolean,
    supportedSpecialInks: Set[SpecialInk] = Set.empty,
    description: Option[LocalizedString] = None,
    enabled: Boolean = true,
)

// ── Finishes & Schema-Validated Parameters ───────────────────────────────────

enum FinishCategory:
  case Surface, Decorative, Structural, LargeFormat, Assembly, Packaging

enum FinishType:
  case Lamination, Overlamination, UVCoating, AqueousCoating, SoftTouchCoating, Varnish
  case Embossing, Debossing, FoilStamping, Thermography, EdgePainting, Embroidery
  case DieCut, ContourCut, KissCut, Scoring, Perforation, RoundCorners, Drilling, Numbering, Mounting
  case Grommets, Hem, PolePocket, RopeAccessory
  case HeatTransfer, HardwareAttachment, ProtectivePackaging

  def category: FinishCategory = this match
    case Lamination | Overlamination | UVCoating | AqueousCoating | SoftTouchCoating =>
      FinishCategory.Surface
    case Embossing | Debossing | FoilStamping | Thermography | EdgePainting | Varnish | Embroidery =>
      FinishCategory.Decorative
    case DieCut | ContourCut | KissCut | Scoring | Perforation | RoundCorners | Drilling | Numbering | Mounting =>
      FinishCategory.Structural
    case Grommets | Hem | PolePocket | RopeAccessory =>
      FinishCategory.LargeFormat
    case HeatTransfer | HardwareAttachment =>
      FinishCategory.Assembly
    case ProtectivePackaging =>
      FinishCategory.Packaging

enum FinishSide:
  case Front, Back, Both

/** Single source of truth for how sides work on a finish (replaces duplicate Finish.side / LaminationParams.side). */
enum FinishSidePolicy:
  case NotApplicable
  case Fixed(side: FinishSide)
  case Selectable(allowed: Set[FinishSide], default: FinishSide)

enum FoilColor:
  case Gold, Silver, Copper, RoseGold, Holographic

/** Parameter schema owned by the `Finish` definition so validation is declarative and exhaustive. */
enum FinishParamSchema:
  case NoParams
  case RoundCornersSchema(allowedCornerCounts: Set[Int], minRadiusMm: Int, maxRadiusMm: Int)
  case FoilColorSchema(allowedColors: Set[FoilColor])
  case GrommetSpacingSchema(allowedSpacingsMm: Set[Int])
  case PerforationSchema(minPitchMm: Int, maxPitchMm: Int)
  case LinearLengthSchema(minMeters: BigDecimal, maxMeters: BigDecimal)
  case CreaseCountSchema(minCreases: Int, maxCreases: Int)

enum FinishParams:
  case RoundCorners(cornerCount: Int, radiusMm: Int)
  case FoilStamping(color: FoilColor)
  case Grommets(spacingMm: Int)
  case Perforation(pitchMm: Int)
  case LinearLength(lengthMeters: BigDecimal)
  case Scoring(creaseCount: Int)

  def numericMetric: Option[BigDecimal] = this match
    case Scoring(n)          => Some(BigDecimal(n))
    case Grommets(s)         => Some(BigDecimal(s))
    case Perforation(p)      => Some(BigDecimal(p))
    case LinearLength(m)     => Some(m)
    case RoundCorners(c, _)  => Some(BigDecimal(c))
    case FoilStamping(_)     => None

final case class Finish(
    id: FinishId,
    name: LocalizedString,
    finishType: FinishType,
    sidePolicy: FinishSidePolicy,
    paramSchema: FinishParamSchema = FinishParamSchema.NoParams,
    description: Option[LocalizedString] = None,
    enabled: Boolean = true,
)

final case class FinishSelection(
    finishId: FinishId,
    requestedSide: Option[FinishSide] = None,
    params: Option[FinishParams] = None,
)

final case class SelectedFinish(
    finish: Finish,
    appliedSide: Option[FinishSide],
    params: Option[FinishParams],
):
  def id: FinishId             = finish.id
  def finishType: FinishType   = finish.finishType
  def sideMultiplier: Int      = appliedSide match
    case Some(FinishSide.Both) => 2
    case _                     => 1

// ── Physical Product Specifications ──────────────────────────────────────────

enum Orientation:
  case Portrait, Landscape

enum FoldType:
  case Half, Tri, Gate, Accordion, ZFold, RollFold, FrenchFold, CrossFold

  /** Number of panels along width and height when unfolded into a flat sheet. */
  def panelLayout: (Int, Int) = this match
    case Half       => (2, 1)
    case Tri        => (3, 1)
    case Gate       => (3, 1)
    case Accordion  => (3, 1)
    case ZFold      => (3, 1)
    case RollFold   => (3, 1)
    case FrenchFold => (2, 2)
    case CrossFold  => (2, 2)

enum BindingMethod:
  case SaddleStitch, PerfectBinding, SpiralBinding, WireOBinding, CaseBinding

/** Explicit physical dimension semantics per product archetype. */
enum ProductDimensions:
  /** Flat sheet or wide-format print (business cards, flyers, banners, roll-up film). */
  case Flat2D(size: Dimension2D)
  /** Folded piece: `finishedSize` is the folded size; flat size is derived via `foldType.panelLayout`. */
  case Folded2D(finishedSize: Dimension2D, foldType: FoldType)
  /** 3D packaging box with optional exact custom dieline flat size. */
  case Box3D(box: Dimension3D, customBlankSize: Option[Dimension2D] = None)
  /** Circular item (round stickers, pin badges). */
  case Circular(diameter: PositiveMm)
  /** Discrete promotional item with a defined print area (T-shirt, mug, tote bag). */
  case PrintArea(area: Dimension2D, variantLabel: Option[String] = None)

  def finishedBoundingBox: Dimension2D = this match
    case Flat2D(s)           => s
    case Folded2D(s, _)      => s
    case Box3D(b, Some(fl))  => fl
    case Box3D(b, None)      => b.approximateBlank2D
    case Circular(d)         => Dimension2D(d, d)
    case PrintArea(area, _)  => area

  def foldTypeOpt: Option[FoldType] = this match
    case Folded2D(_, ft) => Some(ft)
    case _               => None

final case class BindingSpec(
    method: BindingMethod,
    totalPages: Int,
    selfCover: Boolean = false,
)

enum DeliveryFormat:
  case CutSingles
  case KissCutSheets
  case RollLabels(coreDiameterMm: Int, unwindDirection: Int)
  case PackedUnits

/** Typed physical specifications of a product (zero fulfillment/speed fields here). */
final case class ProductSpecs(
    dimensions: ProductDimensions,
    quantity: Quantity,
    versionCount: VersionCount = VersionCount.single,
    orientation: Option[Orientation] = None,
    bleed: Option[NonNegativeMm] = None,
    binding: Option[BindingSpec] = None,
    deliveryFormat: DeliveryFormat = DeliveryFormat.CutSingles,
)

// ── Category, Components & Presets ───────────────────────────────────────────

final case class ComponentTemplate(
    key: ComponentKey,
    label: LocalizedString,
    required: Boolean = true,
    printable: Boolean = true,
    allowedMaterialIds: Set[MaterialId],
    allowedFinishIds: Set[FinishId] = Set.empty,
    allowedPrintingMethodIds: Option[Set[PrintingMethodId]] = None,
)

final case class SpecRequirements(
    requiresFoldedDimensions: Boolean = false,
    requiresBinding: Boolean = false,
    requiresOrientation: Boolean = false,
)

final case class ComponentPreset(
    key: ComponentKey,
    materialId: MaterialId,
    printingMethodId: Option[PrintingMethodId],
    inkConfiguration: Option[InkConfiguration],
    finishes: List[FinishSelection] = List.empty,
)

final case class CategoryPreset(
    id: PresetId,
    name: LocalizedString,
    description: Option[LocalizedString] = None,
    components: NonEmptyChunk[ComponentPreset],
    defaultSpecs: ProductSpecs,
)

final case class ProductCategory(
    id: CategoryId,
    name: LocalizedString,
    components: NonEmptyChunk[ComponentTemplate],
    specRequirements: SpecRequirements = SpecRequirements(),
    allowedPrintingMethodIds: Set[PrintingMethodId] = Set.empty,
    presets: List[CategoryPreset] = List.empty,
    description: Option[LocalizedString] = None,
    enabled: Boolean = true,
):
  def templateFor(key: ComponentKey): Option[ComponentTemplate] =
    components.find(_.key == key)

  def isMultiComponent: Boolean = components.size > 1

enum CatalogGroup:
  case Sheet, LargeFormat, Bound, Specialty, Promotional

final case class ShowcaseListing(
    categoryId: CategoryId,
    group: CatalogGroup,
    tagline: LocalizedString,
    detailedDescription: LocalizedString,
    sortOrder: Int = 0,
    imageUrl: Option[String] = None,
    enabled: Boolean = true,
)

final case class ProductCatalog(
    version: Version,
    categories: Map[CategoryId, ProductCategory],
    materials: Map[MaterialId, Material],
    finishes: Map[FinishId, Finish],
    printingMethods: Map[PrintingMethodId, PrintingMethod],
    showcases: Map[CategoryId, ShowcaseListing] = Map.empty,
)

// ── Unified Component-Scoped Compatibility Rules ─────────────────────────────

enum RuleSeverity:
  /** Blocks configuration and checkout. */
  case Error
  /** Non-blocking advisory shown to customer / prepress (e.g. "Folded ≥170gsm paper should be scored"). */
  case Warning

enum RuleScope:
  case Global
  case Category(categoryId: CategoryId)

enum DimensionPredicate:
  case MinSize(minWidth: PositiveMm, minHeight: PositiveMm)
  case MaxSize(maxWidth: PositiveMm, maxHeight: PositiveMm)
  case IsSquare
  case AllowedExactSizes(sizes: Set[Dimension2D])

enum QuantityPredicate:
  case MinQuantity(min: Int)
  case MaxQuantity(max: Int)

enum BindingPredicate:
  case AllowedMethods(methods: Set[BindingMethod])
  case MethodIn(methods: Set[BindingMethod])
  case MinPages(min: Int)
  case MaxPages(max: Int)
  case PagesDivisibleBy(divisor: Int)

enum FoldPredicate:
  case AllowedFolds(folds: Set[FoldType])
  case FoldIs(fold: FoldType)

/** Predicates evaluated against a single component — eliminates cross-component leakage (e.g. Cover vs Body weight). */
enum ComponentPredicate:
  case AnyComponent
  case KeyIs(key: ComponentKey)
  case MaterialIs(materialId: MaterialId)
  case MaterialFamilyIs(family: MaterialFamily)
  case SurfaceCoatingIs(coating: SurfaceCoating)
  case HasMaterialProperty(property: MaterialProperty)
  case MinGrammage(minGsm: Int)
  case MaxGrammage(maxGsm: Int)
  case HasFinish(finishId: FinishId)
  case HasFinishType(finishType: FinishType)
  case FinishCategoryCountAtMost(category: FinishCategory, maxCount: Int)
  case FinishParamMax(finishType: FinishType, maxValue: BigDecimal)
  case HasPrintingProcess(processType: PrintingProcessType)
  case HasPrintingMethod(methodId: PrintingMethodId)
  case AllowedProcessInks(inks: Set[ProcessInk])
  case HasSpecialInk(ink: SpecialInk)
  case MaxColorsPerSide(max: Int)
  case IsSingleSided
  case IsPrintable
  case And(left: ComponentPredicate, right: ComponentPredicate)
  case Or(left: ComponentPredicate, right: ComponentPredicate)
  case Not(inner: ComponentPredicate)

  def &&(other: ComponentPredicate): ComponentPredicate = ComponentPredicate.And(this, other)
  def ||(other: ComponentPredicate): ComponentPredicate = ComponentPredicate.Or(this, other)
  def unary_! : ComponentPredicate                      = ComponentPredicate.Not(this)

/** Predicates evaluated against an entire product configuration with explicit component quantifiers. */
enum ConfigPredicate:
  case Always
  case ForAllComponents(predicate: ComponentPredicate)
  case ExistsComponent(predicate: ComponentPredicate)
  case ForComponent(key: ComponentKey, predicate: ComponentPredicate)
  case DimensionCheck(predicate: DimensionPredicate)
  case QuantityCheck(predicate: QuantityPredicate)
  case BindingCheck(predicate: BindingPredicate)
  case FoldCheck(predicate: FoldPredicate)
  case And(left: ConfigPredicate, right: ConfigPredicate)
  case Or(left: ConfigPredicate, right: ConfigPredicate)
  case Not(inner: ConfigPredicate)

  def &&(other: ConfigPredicate): ConfigPredicate = ConfigPredicate.And(this, other)
  def ||(other: ConfigPredicate): ConfigPredicate = ConfigPredicate.Or(this, other)
  def unary_! : ConfigPredicate                   = ConfigPredicate.Not(this)

/** Implication rule: `if (when) then (mustHold)` with localized reason and severity. */
final case class CompatibilityRule(
    id: RuleId,
    scope: RuleScope,
    severity: RuleSeverity,
    when: ConfigPredicate,
    mustHold: ConfigPredicate,
    reason: LocalizedString,
    enabled: Boolean = true,
)

final case class CompatibilityRuleset(
    version: Version,
    rules: List[CompatibilityRule],
)

// ── Configuration Input & Resolved Output ────────────────────────────────────

/** Print specification attached ONLY to printable components (`Option[PrintSpec]` on component). */
final case class PrintSpec(
    printingMethod: PrintingMethod,
    ink: InkConfiguration,
)

final case class ComponentSelection(
    key: ComponentKey,
    materialId: MaterialId,
    printingMethodId: Option[PrintingMethodId] = None,
    inkConfiguration: Option[InkConfiguration] = None,
    finishes: List[FinishSelection] = List.empty,
)

final case class ConfigureProductCmd(
    configurationId: ConfigurationId,
    categoryId: CategoryId,
    components: NonEmptyChunk[ComponentSelection],
    specs: ProductSpecs,
)

/** Press-sheet or roll imposition computed once and shared across pricing, weight, and manufacturing. */
enum ImpositionResult:
  case SheetImposition(
      stockFormatId: StockFormatId,
      sheetSize: Dimension2D,
      piecesPerSheet: Int,
      sheetsNeeded: Int,
      guillotineCutsPerSheet: Int,
      wasteRatio: BigDecimal,
  )
  case RollImposition(
      stockFormatId: StockFormatId,
      rollWidth: PositiveMm,
      piecesAcrossRoll: Int,
      linearMetersUsed: BigDecimal,
      totalAreaSqM: SquareMeters,
  )
  case DiscreteUnit(
      totalUnits: Int,
  )

final case class ComponentGeometry(
    finishedSize: Dimension2D,
    flatSize: Dimension2D,
    unitsPerProduct: Int,
    totalFlatUnits: Int,
    areaPerUnitSqM: SquareMeters,
    perimeterPerUnitM: BigDecimal,
    imposition: ImpositionResult,
)

final case class ComponentWeight(
    key: ComponentKey,
    weightPerProduct: Grams,
    totalWeight: Grams,
)

final case class WeightSummary(
    components: NonEmptyChunk[ComponentWeight],
    weightPerProduct: Grams,
    totalWeight: Grams,
)

final case class ConfiguredComponent(
    key: ComponentKey,
    material: Material,
    print: Option[PrintSpec],
    finishes: List[SelectedFinish],
    geometry: ComponentGeometry,
)

final case class ConfiguredProduct(
    id: ConfigurationId,
    catalogVersion: Version,
    rulesetVersion: Version,
    categoryId: CategoryId,
    categoryName: LocalizedString,
    components: NonEmptyChunk[ConfiguredComponent],
    specs: ProductSpecs,
    weight: Option[WeightSummary],
)

final case class ConfigurationWarning(
    ruleId: RuleId,
    reason: LocalizedString,
)

final case class ConfigurationResult(
    product: ConfiguredProduct,
    warnings: List[ConfigurationWarning],
)
