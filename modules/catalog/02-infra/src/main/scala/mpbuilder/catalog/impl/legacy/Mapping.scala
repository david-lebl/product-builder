package mpbuilder.catalog
package impl
package legacy

import mpbuilder.commons.*
import mpbuilder.domain.model as dm
import mpbuilder.domain.service.ConfigurationRequest
import mpbuilder.domain.validation.ConfigurationError

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
      categoryId = dm.CategoryId.unsafe(dto.categoryId),
      printingMethodId = dm.PrintingMethodId.unsafe(dto.printingMethodId),
      components = components,
      specs = specs,
    )

  private def toComponent(dto: ComponentRequestDto): Either[CatalogError, dm.ComponentRequest] =
    for
      role <- parse("role", dto.role, dm.ComponentRole.values)
      front <- toInkSetup(dto.ink.front)
      back <- toInkSetup(dto.ink.back)
      finishes <- traverse(dto.finishes)(toFinishSelection)
    yield dm.ComponentRequest(
      role = role,
      materialId = dm.MaterialId.unsafe(dto.materialId),
      inkConfiguration = dm.InkConfiguration(front, back),
      finishes = finishes,
    )

  private def toInkSetup(dto: InkSetupDto): Either[CatalogError, dm.InkSetup] =
    parse("ink.inkType", dto.inkType, dm.InkType.values).map(dm.InkSetup(_, dto.colorCount))

  private def toFinishSelection(dto: FinishSelectionDto): Either[CatalogError, dm.FinishSelection] =
    dto.params.fold(Right(None): Either[CatalogError, Option[dm.FinishParameters]])(
      toFinishParams(_).map(Some(_))
    ).map(params => dm.FinishSelection(dm.FinishId.unsafe(dto.finishId), params))

  private def toFinishParams(dto: FinishParamsDto): Either[CatalogError, dm.FinishParameters] =
    dto match
      case FinishParamsDto.RoundCorners(count, radius) =>
        Right(dm.FinishParameters.RoundCornersParams(count, radius))
      case FinishParamsDto.Lamination(side) =>
        parse("finish.side", side, dm.FinishSide.values).map(dm.FinishParameters.LaminationParams(_))
      case FinishParamsDto.FoilStamping(color) =>
        parse("finish.color", color, dm.FoilColor.values).map(dm.FinishParameters.FoilStampingParams(_))
      case FinishParamsDto.Grommet(spacing)   => Right(dm.FinishParameters.GrommetParams(spacing))
      case FinishParamsDto.Perforation(pitch) => Right(dm.FinishParameters.PerforationParams(pitch))
      case FinishParamsDto.Rope(length)       => Right(dm.FinishParameters.RopeParams(length))
      case FinishParamsDto.Scoring(creases)   => Right(dm.FinishParameters.ScoringParams(creases))

  private def toSpecs(dto: SpecificationsDto): Either[CatalogError, List[dm.SpecValue]] =
    for
      orientation <- optional(dto.orientation)(parse("orientation", _, dm.Orientation.values))
      foldType <- optional(dto.foldType)(parse("foldType", _, dm.FoldType.values))
      binding <- optional(dto.bindingMethod)(parse("bindingMethod", _, dm.BindingMethod.values))
      speed <- optional(dto.speed)(parse("speed", _, dm.ManufacturingSpeed.values))
    yield List(
      dto.size.map(s => dm.SpecValue.SizeSpec(Dimension(s.widthMm, s.heightMm))),
      dto.quantity.map(q => dm.SpecValue.QuantitySpec(Quantity.unsafe(q))),
      orientation.map(dm.SpecValue.OrientationSpec(_)),
      dto.bleedMm.map(dm.SpecValue.BleedSpec(_)),
      dto.pages.map(dm.SpecValue.PagesSpec(_)),
      foldType.map(dm.SpecValue.FoldTypeSpec(_)),
      binding.map(dm.SpecValue.BindingMethodSpec(_)),
      speed.map(dm.SpecValue.ManufacturingSpeedSpec(_)),
    ).flatten

  // ── domain → domain (for revalidation) ───────────────────────────────────

  /** Recover the request a stored configuration was built from, so it can be rebuilt against the
    * current catalog.
    */
  def toRequest(config: dm.ProductConfiguration): ConfigurationRequest =
    ConfigurationRequest(
      categoryId = config.category.id,
      printingMethodId = config.printingMethod.id,
      components = config.components.map { c =>
        dm.ComponentRequest(
          role = c.role,
          materialId = c.material.id,
          inkConfiguration = c.inkConfiguration,
          finishes = c.finishes.map(sf => dm.FinishSelection(sf.finish.id, sf.params)),
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
