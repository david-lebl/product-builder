package mpbuilder.domain.catalog

import mpbuilder.domain.*
import mpbuilder.domain.ids.*
import zio.json.*

enum ComponentRole derives JsonCodec:
  case Main, Cover, Body, Stand

enum MaterialProperty derives JsonCodec:
  case Glossy, Matte, Textured, Smooth, WaterResistant, Recyclable, Transparent

final case class Material(
  id: MaterialId,
  name: LocalizedText,
  weightGsm: Option[Int],
  properties: Set[MaterialProperty],
  description: Option[LocalizedText] = None,
) derives JsonCodec

enum FinishType derives JsonCodec:
  case Lamination, UvCoating, SoftTouch, AqueousCoating, SpotVarnish, Embossing, Debossing,
    FoilStamping, DieCut, KissCut, Scoring, Perforation, RoundCorners, Grommets, Overlamination,
    GumRope, HeatPressTransfer, Embroidery, LabelTagPrinting, FoldAndBag, MylarOverlay,
    SafetyPinBack, MagnetBack, BottleOpenerBack, DishwasherCoating, GiftBoxPackaging,
    CeramicGlaze, ReinforcedHandles

enum FinishSide derives JsonCodec:
  case Front, Back, Both

final case class Finish(
  id: FinishId,
  name: LocalizedText,
  finishType: FinishType,
  side: FinishSide = FinishSide.Both,
  description: Option[LocalizedText] = None,
) derives JsonCodec

final case class PrintingMethod(
  id: PrintingMethodId,
  name: LocalizedText,
  maxColors: Option[Int], // None = unlimited
  description: Option[LocalizedText] = None,
) derives JsonCodec

/** Colors printed front/back — e.g. 4/4 full color both sides, 1/0 black front only. */
final case class InkConfiguration(
  id: InkConfigId,
  name: LocalizedText,
  frontColors: Int,
  backColors: Int,
) derives JsonCodec:
  def maxColorsUsed: Int = math.max(frontColors, backColors)

enum FoldType derives JsonCodec:
  case HalfFold, TriFold, GateFold, AccordionFold, ZFold, RollFold, FrenchFold, CrossFold

enum BindingMethod derives JsonCodec:
  case SaddleStitch, PerfectBinding, SpiralBinding, WireOBinding, CaseBinding

enum SpeedTier derives JsonCodec:
  case Express, Standard, Economy

enum RequiredDetail derives JsonCodec:
  case Size, Quantity, Orientation, Pages, Fold, Binding

/** One physical part of a product (a booklet's cover, a roll-up's stand, ...). */
final case class ComponentSpec(
  role: ComponentRole,
  optional: Boolean,
  allowedMaterials: AllowList[MaterialId],
  allowedFinishes: AllowList[FinishId],
) derives JsonCodec

final case class Category(
  id: CategoryId,
  name: LocalizedText,
  components: List[ComponentSpec],
  requiredDetails: Set[RequiredDetail],
  allowedPrintingMethods: AllowList[PrintingMethodId],
  description: Option[LocalizedText] = None,
) derives JsonCodec:
  def componentSpec(role: ComponentRole): Option[ComponentSpec] = components.find(_.role == role)

final case class Catalog(
  categories: List[Category],
  materials: List[Material],
  finishes: List[Finish],
  printingMethods: List[PrintingMethod],
  inkConfigurations: List[InkConfiguration],
):
  lazy val categoriesById: Map[CategoryId, Category]              = categories.map(c => c.id -> c).toMap
  lazy val materialsById: Map[MaterialId, Material]               = materials.map(m => m.id -> m).toMap
  lazy val finishesById: Map[FinishId, Finish]                    = finishes.map(f => f.id -> f).toMap
  lazy val printingMethodsById: Map[PrintingMethodId, PrintingMethod] =
    printingMethods.map(p => p.id -> p).toMap
  lazy val inkConfigurationsById: Map[InkConfigId, InkConfiguration] =
    inkConfigurations.map(i => i.id -> i).toMap

object Catalog:
  given JsonCodec[Catalog] = DeriveJsonCodec.gen[Catalog]
