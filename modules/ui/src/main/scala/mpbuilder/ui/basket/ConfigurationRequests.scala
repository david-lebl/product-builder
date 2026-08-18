package mpbuilder.ui.basket

import mpbuilder.catalog as cat
import mpbuilder.domain.model as dm

/** Turning a configured product back into the request that describes it.
  *
  * The configurator resolves a request into a full [[dm.ProductConfiguration]] locally so it can
  * price and render it as the customer builds. The server has to be told what was chosen, not what
  * the browser concluded — so this reduces the resolved configuration back to ids and plain values,
  * and the catalog resolves it again on its own terms.
  *
  * That round trip is the point. If the browser sent its resolved configuration, a material that
  * has since been withdrawn, or a rule that has since tightened, would sail straight through.
  */
private[ui] object ConfigurationRequests:

  def of(config: dm.ProductConfiguration): cat.ConfigurationRequestDto =
    cat.ConfigurationRequestDto(
      categoryId = config.category.id.value,
      printingMethodId = config.printingMethod.id.value,
      components = config.components.map(component),
      specifications = specifications(config.specifications),
    )

  private def component(c: dm.ProductComponent): cat.ComponentRequestDto =
    cat.ComponentRequestDto(
      role = c.role.toString,
      materialId = c.material.id.value,
      ink = cat.InkConfigurationDto(ink(c.inkConfiguration.front), ink(c.inkConfiguration.back)),
      finishes = c.finishes.map(finish),
    )

  private def ink(setup: dm.InkSetup): cat.InkSetupDto =
    cat.InkSetupDto(setup.inkType.toString, setup.colorCount)

  private def finish(selected: dm.SelectedFinish): cat.FinishSelectionDto =
    cat.FinishSelectionDto(selected.id.value, selected.params.map(params))

  private def params(p: dm.FinishParameters): cat.FinishParamsDto = p match
    case dm.FinishParameters.RoundCornersParams(count, radius) =>
      cat.FinishParamsDto.RoundCorners(count, radius)
    case dm.FinishParameters.LaminationParams(side) =>
      cat.FinishParamsDto.Lamination(side.toString)
    case dm.FinishParameters.FoilStampingParams(color) =>
      cat.FinishParamsDto.FoilStamping(color.toString)
    case dm.FinishParameters.GrommetParams(spacing)   => cat.FinishParamsDto.Grommet(spacing)
    case dm.FinishParameters.PerforationParams(pitch) => cat.FinishParamsDto.Perforation(pitch)
    case dm.FinishParameters.RopeParams(length)       => cat.FinishParamsDto.Rope(length)
    case dm.FinishParameters.ScoringParams(creases)   => cat.FinishParamsDto.Scoring(creases)

  private def specifications(specs: dm.ProductSpecifications): cat.SpecificationsDto =
    specs.specs.values.foldLeft(cat.SpecificationsDto()) { (dto, value) =>
      value match
        case dm.SpecValue.SizeSpec(d) =>
          dto.copy(size = Some(cat.SizeDto(d.widthMm, d.heightMm)))
        case dm.SpecValue.QuantitySpec(q)         => dto.copy(quantity = Some(q.value))
        case dm.SpecValue.OrientationSpec(o)      => dto.copy(orientation = Some(o.toString))
        case dm.SpecValue.BleedSpec(mm)           => dto.copy(bleedMm = Some(mm))
        case dm.SpecValue.PagesSpec(count)        => dto.copy(pages = Some(count))
        case dm.SpecValue.FoldTypeSpec(f)         => dto.copy(foldType = Some(f.toString))
        case dm.SpecValue.BindingMethodSpec(m)    => dto.copy(bindingMethod = Some(m.toString))
        case dm.SpecValue.ManufacturingSpeedSpec(s) => dto.copy(speed = Some(s.toString))
    }

  /** The request, as JSON, encoded with catalog's own codecs.
    *
    * Encoding lives here rather than at the call site so this file is the only one that needs
    * `catalog.json` in scope: those codecs and the legacy domain's both define an encoding for
    * `Money`, and importing the two together is ambiguous.
    */
  def json(config: dm.ProductConfiguration): String =
    import mpbuilder.catalog.json.given
    import zio.json.*
    of(config).toJson
