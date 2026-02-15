package mpbuilder.domain.service

import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.rules.*
import mpbuilder.domain.validation.*

final case class ConfigurationRequest(
    categoryId: CategoryId,
    materialId: MaterialId,
    finishIds: List[FinishId],
    specs: List[SpecValue],
)

object ConfigurationBuilder:

  def build(
      request: ConfigurationRequest,
      catalog: ProductCatalog,
      ruleset: CompatibilityRuleset,
      configId: ConfigurationId,
  ): Validation[ConfigurationError, ProductConfiguration] =
    val categoryV: Validation[ConfigurationError, ProductCategory] =
      catalog.categories.get(request.categoryId) match
        case Some(c) => Validation.succeed(c)
        case None    => Validation.fail(ConfigurationError.CategoryNotFound(request.categoryId))

    val materialV: Validation[ConfigurationError, Material] =
      catalog.materials.get(request.materialId) match
        case Some(m) => Validation.succeed(m)
        case None    => Validation.fail(ConfigurationError.MaterialNotFound(request.materialId))

    val finishesV: Validation[ConfigurationError, List[Finish]] =
      request.finishIds
        .map { fid =>
          catalog.finishes.get(fid) match
            case Some(f) => Validation.succeed(f)
            case None    => Validation.fail(ConfigurationError.FinishNotFound(fid))
        }
        .foldLeft(Validation.succeed(List.empty[Finish]): Validation[ConfigurationError, List[Finish]]) {
          (accV, finV) =>
            accV.zipWith(finV)(_ :+ _)
        }

    Validation
      .validateWith(categoryV, materialV, finishesV)((cat, mat, fins) => (cat, mat, fins))
      .flatMap { case (category, material, finishes) =>
        val specifications = ProductSpecifications.fromSpecs(request.specs)
        ConfigurationValidator
          .validate(category, material, finishes, specifications, ruleset)
          .map(_ =>
            ProductConfiguration(
              id = configId,
              category = category,
              material = material,
              finishes = finishes,
              specifications = specifications,
            ),
          )
      }
