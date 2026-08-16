package mpbuilder.catalog

/** Wire shapes for configuring a product.
  *
  * These are deliberately *not* the domain model. Ids are plain strings and enums are plain strings,
  * so the public API does not move every time an internal type is renamed, and so callers in other
  * contexts need no catalog types to build a request. Conversion happens once, at the boundary, in
  * `02-infra`.
  */

/** A finish's extra parameters. Modelled as a sum rather than a bag of optional fields, so it is
  * impossible to send, say, a corner radius for a grommet.
  */
enum FinishParamsDto:
  case RoundCorners(cornerCount: Int, radiusMm: Int)
  case Lamination(side: String)
  case FoilStamping(color: String)
  case Grommet(spacingMm: Int)
  case Perforation(pitchMm: Int)
  case Rope(lengthMeters: BigDecimal)
  case Scoring(creaseCount: Int)

final case class FinishSelectionDto(
    finishId: String,
    params: Option[FinishParamsDto] = None,
)

final case class InkSetupDto(inkType: String, colorCount: Int)

final case class InkConfigurationDto(front: InkSetupDto, back: InkSetupDto)

final case class ComponentRequestDto(
    role: String,
    materialId: String,
    ink: InkConfigurationDto,
    finishes: List[FinishSelectionDto] = Nil,
)

final case class SizeDto(widthMm: Double, heightMm: Double)

/** The specifications a category may require.
  *
  * Unlike [[FinishParamsDto]], a record of independent `Option`s is the honest shape here: each
  * specification is genuinely present or absent on its own, mirroring the `Map[SpecKind, SpecValue]`
  * the domain keeps. Which ones are *required* is a per-category rule, enforced during conversion.
  */
final case class SpecificationsDto(
    size: Option[SizeDto] = None,
    quantity: Option[Int] = None,
    orientation: Option[String] = None,
    bleedMm: Option[Double] = None,
    pages: Option[Int] = None,
    foldType: Option[String] = None,
    bindingMethod: Option[String] = None,
    speed: Option[String] = None,
)

final case class ConfigurationRequestDto(
    categoryId: String,
    printingMethodId: String,
    components: List[ComponentRequestDto],
    specifications: SpecificationsDto = SpecificationsDto(),
)
