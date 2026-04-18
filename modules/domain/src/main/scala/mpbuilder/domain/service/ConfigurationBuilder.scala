package mpbuilder.domain.service

import zio.prelude.*
import mpbuilder.domain.model.*
import mpbuilder.domain.rules.*
import mpbuilder.domain.validation.*

final case class ConfigurationRequest(
    categoryId: CategoryId,
    printingMethodId: PrintingMethodId,
    components: List[ComponentRequest],
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

    val printingMethodV: Validation[ConfigurationError, PrintingMethod] =
      catalog.printingMethods.get(request.printingMethodId) match
        case Some(pm) => Validation.succeed(pm)
        case None     => Validation.fail(ConfigurationError.PrintingMethodNotFound(request.printingMethodId))

    val resolvedComponentsV: Validation[ConfigurationError, List[(ComponentRequest, Material, List[SelectedFinish])]] =
      request.components
        .map { compReq =>
          val materialV: Validation[ConfigurationError, Material] =
            catalog.materials.get(compReq.materialId) match
              case Some(m) => Validation.succeed(m)
              case None    => Validation.fail(ConfigurationError.MaterialNotFound(compReq.materialId))

          val finishesV: Validation[ConfigurationError, List[SelectedFinish]] =
            compReq.finishes
              .map { sel =>
                catalog.finishes.get(sel.finishId) match
                  case Some(f) => Validation.succeed(SelectedFinish(f, sel.params))
                  case None    => Validation.fail(ConfigurationError.FinishNotFound(sel.finishId))
              }
              .foldLeft(Validation.succeed(List.empty[SelectedFinish]): Validation[ConfigurationError, List[SelectedFinish]]) {
                (accV, finV) => accV.zipWith(finV)(_ :+ _)
              }

          materialV.zipWith(finishesV)((mat, fins) => (compReq, mat, fins))
        }
        .foldLeft(Validation.succeed(List.empty[(ComponentRequest, Material, List[SelectedFinish])]): Validation[ConfigurationError, List[(ComponentRequest, Material, List[SelectedFinish])]]) {
          (accV, tripV) => accV.zipWith(tripV)(_ :+ _)
        }

    Validation
      .validateWith(categoryV, printingMethodV, resolvedComponentsV)((cat, pm, resolved) => (cat, pm, resolved))
      .flatMap { case (category, printingMethod, resolvedComponents) =>
        val specifications = ProductSpecifications.fromSpecs(request.specs)

        val productComponents = resolvedComponents.map { case (compReq, material, finishes) =>
          ProductComponent(
            role = compReq.role,
            material = material,
            inkConfiguration = compReq.inkConfiguration,
            finishes = finishes,
            sheetCount = deriveSheetCount(compReq.role, specifications),
          )
        }

        ConfigurationValidator
          .validate(category, productComponents, specifications, ruleset, printingMethod)
          .map(_ =>
            ProductConfiguration(
              id = configId,
              category = category,
              printingMethod = printingMethod,
              components = productComponents,
              specifications = specifications,
            ),
          )
      }

  private def deriveSheetCount(
      role: ComponentRole,
      specs: ProductSpecifications,
  ): Int =
    role match
      case ComponentRole.Main  => 1
      case ComponentRole.Cover => 1
      case ComponentRole.Stand => 1
      case ComponentRole.Body =>
        val totalPages = specs.get(SpecKind.Pages) match
          case Some(SpecValue.PagesSpec(count)) => count
          case _                                => 0

        val bindingMethod = specs.get(SpecKind.BindingMethod) match
          case Some(SpecValue.BindingMethodSpec(method)) => Some(method)
          case _                                         => None

        bindingMethod match
          case Some(BindingMethod.SaddleStitch) =>
            (totalPages / 4) - 1
          case Some(BindingMethod.PerfectBinding) | Some(BindingMethod.CaseBinding) =>
            (totalPages - 4) / 2
          case Some(BindingMethod.PlasticOBinding) | Some(BindingMethod.MetalWireBinding) =>
            (totalPages - 2) / 2
          case None => 1
