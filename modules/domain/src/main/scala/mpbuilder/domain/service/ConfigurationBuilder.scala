package mpbuilder.domain.service

import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.rules.*
import mpbuilder.domain.validation.*

final case class ConfigurationRequest(
    categoryId: CategoryId,
    materialId: MaterialId,
    printingMethodId: PrintingMethodId,
    finishIds: List[FinishId],
    specs: List[SpecValue],
    components: List[ComponentRequest] = Nil,
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

    val printingMethodV: Validation[ConfigurationError, PrintingMethod] =
      catalog.printingMethods.get(request.printingMethodId) match
        case Some(pm) => Validation.succeed(pm)
        case None     => Validation.fail(ConfigurationError.PrintingMethodNotFound(request.printingMethodId))

    categoryV.flatMap { category =>
      printingMethodV.flatMap { printingMethod =>
        if ProductCategory.isMultiComponent(category) then
          buildMultiComponent(request, category, printingMethod, catalog, ruleset, configId)
        else
          buildSingleComponent(request, category, printingMethod, catalog, ruleset, configId)
      }
    }

  private def buildSingleComponent(
      request: ConfigurationRequest,
      category: ProductCategory,
      printingMethod: PrintingMethod,
      catalog: ProductCatalog,
      ruleset: CompatibilityRuleset,
      configId: ConfigurationId,
  ): Validation[ConfigurationError, ProductConfiguration] =
    val materialV: Validation[ConfigurationError, Material] =
      catalog.materials.get(request.materialId) match
        case Some(m) => Validation.succeed(m)
        case None    => Validation.fail(ConfigurationError.MaterialNotFound(request.materialId))

    val finishesV: Validation[ConfigurationError, List[Finish]] =
      resolveFinishes(request.finishIds, catalog)

    Validation
      .validateWith(materialV, finishesV)((mat, fins) => (mat, fins))
      .flatMap { case (material, finishes) =>
        val specifications = ProductSpecifications.fromSpecs(request.specs)
        ConfigurationValidator
          .validate(category, material, finishes, specifications, ruleset, printingMethod)
          .map(_ =>
            ProductConfiguration(
              id = configId,
              category = category,
              material = material,
              printingMethod = printingMethod,
              finishes = finishes,
              specifications = specifications,
            ),
          )
      }

  private def buildMultiComponent(
      request: ConfigurationRequest,
      category: ProductCategory,
      printingMethod: PrintingMethod,
      catalog: ProductCatalog,
      ruleset: CompatibilityRuleset,
      configId: ConfigurationId,
  ): Validation[ConfigurationError, ProductConfiguration] =
    val specifications = ProductSpecifications.fromSpecs(request.specs)

    // Resolve all components
    val componentsV: Validation[ConfigurationError, List[ProductComponent]] =
      resolveComponents(request.components, catalog)

    componentsV.flatMap { components =>
      // Validate components and shared configuration
      ConfigurationValidator
        .validateMultiComponent(category, components, specifications, ruleset, printingMethod)
        .map { _ =>
          // Use Cover component's material/finishes as primary (backward compat)
          val coverComponent = components.find(_.role == ComponentRole.Cover)
          val primaryMaterial = coverComponent.map(_.material).getOrElse(components.head.material)
          val primaryFinishes = coverComponent.map(_.finishes).getOrElse(components.head.finishes)

          ProductConfiguration(
            id = configId,
            category = category,
            material = primaryMaterial,
            printingMethod = printingMethod,
            finishes = primaryFinishes,
            specifications = specifications,
            components = components,
          )
        }
    }

  private def resolveFinishes(
      finishIds: List[FinishId],
      catalog: ProductCatalog,
  ): Validation[ConfigurationError, List[Finish]] =
    finishIds
      .map { fid =>
        catalog.finishes.get(fid) match
          case Some(f) => Validation.succeed(f)
          case None    => Validation.fail(ConfigurationError.FinishNotFound(fid))
      }
      .foldLeft(Validation.succeed(List.empty[Finish]): Validation[ConfigurationError, List[Finish]]) {
        (accV, finV) =>
          accV.zipWith(finV)(_ :+ _)
      }

  private def resolveComponents(
      componentRequests: List[ComponentRequest],
      catalog: ProductCatalog,
  ): Validation[ConfigurationError, List[ProductComponent]] =
    componentRequests
      .map { cr =>
        val materialV: Validation[ConfigurationError, Material] =
          catalog.materials.get(cr.materialId) match
            case Some(m) => Validation.succeed(m)
            case None    => Validation.fail(ConfigurationError.MaterialNotFound(cr.materialId))

        val finishesV: Validation[ConfigurationError, List[Finish]] =
          resolveFinishes(cr.finishIds, catalog)

        Validation.validateWith(materialV, finishesV) { (mat, fins) =>
          ProductComponent(
            role = cr.role,
            material = mat,
            finishes = fins,
            inkConfiguration = cr.inkConfiguration,
          )
        }
      }
      .foldLeft(Validation.succeed(List.empty[ProductComponent]): Validation[ConfigurationError, List[ProductComponent]]) {
        (accV, compV) =>
          accV.zipWith(compV)(_ :+ _)
      }
