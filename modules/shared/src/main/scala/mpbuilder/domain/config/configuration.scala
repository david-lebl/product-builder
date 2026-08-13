package mpbuilder.domain.config

import mpbuilder.domain.*
import mpbuilder.domain.catalog.*
import mpbuilder.domain.ids.*
import zio.json.*

enum Orientation derives JsonCodec:
  case Portrait, Landscape

/** Customer-supplied parameter for parameterized finishes (spec §2.3). */
enum FinishParams derives JsonCodec:
  case Creases(count: Int)
  case GrommetSpacing(spacingMm: Int)
  case RopeLength(metres: BigDecimal)
  case RoundCorners(corners: Int, radiusMm: Int)

final case class SelectedFinish(
  finishId: FinishId,
  params: Option[FinishParams] = None,
) derives JsonCodec

final case class ComponentConfiguration(
  role: ComponentRole,
  materialId: MaterialId,
  finishes: List[SelectedFinish] = Nil,
) derives JsonCodec

final case class ProductDetails(
  size: Option[DimensionsMm] = None,
  quantity: Option[Int] = None,
  orientation: Option[Orientation] = None,
  pages: Option[Int] = None,
  foldType: Option[FoldType] = None,
  bindingMethod: Option[BindingMethod] = None,
) derives JsonCodec

/** The customer's complete product configuration — the exact value the UI edits,
  * prices locally, and POSTs to the backend.
  */
final case class ProductConfiguration(
  categoryId: CategoryId,
  printingMethodId: PrintingMethodId,
  inkConfigurationId: InkConfigId,
  components: List[ComponentConfiguration],
  details: ProductDetails = ProductDetails(),
  speedTier: SpeedTier = SpeedTier.Standard,
) derives JsonCodec:
  def component(role: ComponentRole): Option[ComponentConfiguration] = components.find(_.role == role)

final case class CustomerContact(
  name: String,
  email: String,
  phone: String,
  company: Option[String] = None,
) derives JsonCodec

/** Ready-made quick-order starting point for a category (spec §2.1). */
final case class Preset(
  id: PresetId,
  categoryId: CategoryId,
  name: LocalizedText,
  configuration: ProductConfiguration,
  description: Option[LocalizedText] = None,
) derives JsonCodec
