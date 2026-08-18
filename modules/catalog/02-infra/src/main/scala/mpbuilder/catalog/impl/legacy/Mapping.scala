package mpbuilder.catalog
package impl
package legacy

import mpbuilder.commons.*
import mpbuilder.catalog.model as cm
import mpbuilder.domain.model as dm
import mpbuilder.catalog.model.*
import mpbuilder.catalog.service.ConfigurationRequest
import mpbuilder.catalog.validation.ConfigurationError

/** Translation between the public DTOs and the legacy domain model.
  *
  * This is the whole anti-corruption layer for catalog: the only place that knows both vocabularies.
  * Keeping it in one object means the blast radius of a domain rename is one file.
  */
private[catalog] object Mapping:

  // ── DTO → domain ─────────────────────────────────────────────────────────

  def toDomain(dto: ConfigurationRequestDto): Either[CatalogError, ConfigurationRequest] =
    for
      components <- traverse(dto.components)(toComponent)
      specs <- toSpecs(dto.specifications)
    yield ConfigurationRequest(
      categoryId = cm.CategoryId.unsafe(dto.categoryId),
      printingMethodId = cm.PrintingMethodId.unsafe(dto.printingMethodId),
      components = components,
      specs = specs,
    )

  private def toComponent(dto: ComponentRequestDto): Either[CatalogError, cm.ComponentRequest] =
    for
      role <- parse("role", dto.role, cm.ComponentRole.values)
      front <- toInkSetup(dto.ink.front)
      back <- toInkSetup(dto.ink.back)
      finishes <- traverse(dto.finishes)(toFinishSelection)
    yield cm.ComponentRequest(
      role = role,
      materialId = cm.MaterialId.unsafe(dto.materialId),
      inkConfiguration = cm.InkConfiguration(front, back),
      finishes = finishes,
    )

  private def toInkSetup(dto: InkSetupDto): Either[CatalogError, cm.InkSetup] =
    parse("ink.inkType", dto.inkType, cm.InkType.values).map(cm.InkSetup(_, dto.colorCount))

  private def toFinishSelection(dto: FinishSelectionDto): Either[CatalogError, cm.FinishSelection] =
    dto.params.fold(Right(None): Either[CatalogError, Option[cm.FinishParameters]])(
      toFinishParams(_).map(Some(_))
    ).map(params => cm.FinishSelection(cm.FinishId.unsafe(dto.finishId), params))

  private def toFinishParams(dto: FinishParamsDto): Either[CatalogError, cm.FinishParameters] =
    dto match
      case FinishParamsDto.RoundCorners(count, radius) =>
        Right(cm.FinishParameters.RoundCornersParams(count, radius))
      case FinishParamsDto.Lamination(side) =>
        parse("finish.side", side, cm.FinishSide.values).map(cm.FinishParameters.LaminationParams(_))
      case FinishParamsDto.FoilStamping(color) =>
        parse("finish.color", color, cm.FoilColor.values).map(cm.FinishParameters.FoilStampingParams(_))
      case FinishParamsDto.Grommet(spacing)   => Right(cm.FinishParameters.GrommetParams(spacing))
      case FinishParamsDto.Perforation(pitch) => Right(cm.FinishParameters.PerforationParams(pitch))
      case FinishParamsDto.Rope(length)       => Right(cm.FinishParameters.RopeParams(length))
      case FinishParamsDto.Scoring(creases)   => Right(cm.FinishParameters.ScoringParams(creases))

  private def toSpecs(dto: SpecificationsDto): Either[CatalogError, List[cm.SpecValue]] =
    for
      orientation <- optional(dto.orientation)(parse("orientation", _, cm.Orientation.values))
      foldType <- optional(dto.foldType)(parse("foldType", _, cm.FoldType.values))
      binding <- optional(dto.bindingMethod)(parse("bindingMethod", _, cm.BindingMethod.values))
      speed <- optional(dto.speed)(parse("speed", _, cm.ManufacturingSpeed.values))
    yield List(
      dto.size.map(s => cm.SpecValue.SizeSpec(Dimension(s.widthMm, s.heightMm))),
      dto.quantity.map(q => cm.SpecValue.QuantitySpec(Quantity.unsafe(q))),
      orientation.map(cm.SpecValue.OrientationSpec(_)),
      dto.bleedMm.map(cm.SpecValue.BleedSpec(_)),
      dto.pages.map(cm.SpecValue.PagesSpec(_)),
      foldType.map(cm.SpecValue.FoldTypeSpec(_)),
      binding.map(cm.SpecValue.BindingMethodSpec(_)),
      speed.map(cm.SpecValue.ManufacturingSpeedSpec(_)),
    ).flatten

  // ── domain → domain (for revalidation) ───────────────────────────────────

  /** Recover the request a stored configuration was built from, so it can be rebuilt against the
    * current catalog.
    */
  def toRequest(config: cm.ProductConfiguration): ConfigurationRequest =
    ConfigurationRequest(
      categoryId = config.category.id,
      printingMethodId = config.printingMethod.id,
      components = config.components.map { c =>
        cm.ComponentRequest(
          role = c.role,
          materialId = c.material.id,
          inkConfiguration = c.inkConfiguration,
          finishes = c.finishes.map(sf => cm.FinishSelection(sf.finish.id, sf.params)),
        )
      },
      specs = config.specifications.specs.values.toList,
    )

  // ── domain → public ──────────────────────────────────────────────────────

  def toProblem(error: ConfigurationError): Problem =
    Problem(
      code = error.toString.takeWhile(_ != '('),
      message = LocalizedString(error.message(Language.En), error.message(Language.Cs)),
    )

  // ── helpers ──────────────────────────────────────────────────────────────

  private def parse[A](field: String, raw: String, values: Array[A]): Either[CatalogError, A] =
    values
      .find(_.toString.equalsIgnoreCase(raw))
      .toRight(CatalogError.UnknownValue(field, raw))

  private def optional[A, B](o: Option[A])(f: A => Either[CatalogError, B]): Either[CatalogError, Option[B]] =
    o.fold(Right(None))(f(_).map(Some(_)))

  private def traverse[A, B](as: List[A])(f: A => Either[CatalogError, B]): Either[CatalogError, List[B]] =
    as.foldRight(Right(Nil): Either[CatalogError, List[B]]) { (a, acc) =>
      for
        rest <- acc
        b <- f(a)
      yield b :: rest
    }
