package mpbuilder.domain.weight

import zio.prelude.*
import mpbuilder.domain.model.*

object WeightCalculator:

  def calculate(config: ProductConfiguration): Validation[WeightError, WeightBreakdown] =
    extractQuantity(config.specifications).flatMap { quantity =>
      val componentBreakdownsV: Validation[WeightError, List[ComponentWeightBreakdown]] =
        config.components
          .map { comp => calculateComponentBreakdown(comp, config.specifications, quantity) }
          .foldLeft(Validation.succeed(List.empty[ComponentWeightBreakdown]): Validation[WeightError, List[ComponentWeightBreakdown]]) {
            (accV, cbV) => accV.zipWith(cbV)(_ :+ _)
          }

      componentBreakdownsV.map { componentBreakdowns =>
        val weightPerItemG = componentBreakdowns.map(_.weightPerItemG).sum
        val totalWeightG   = weightPerItemG * quantity
        WeightBreakdown(
          componentBreakdowns = componentBreakdowns,
          weightPerItemG      = weightPerItemG,
          quantity            = quantity,
          totalWeightG        = totalWeightG,
          totalWeightKg       = totalWeightG / 1000.0,
        )
      }
    }

  private def calculateComponentBreakdown(
      comp: ProductComponent,
      specs: ProductSpecifications,
      quantity: Int,
  ): Validation[WeightError, ComponentWeightBreakdown] =
    specs.get(SpecKind.Size) match
      case Some(SpecValue.SizeSpec(dim)) =>
        comp.material.weight match
          case Some(paperWeight) =>
            val gsm = paperWeight.gsm
            val isSaddleStitchFolded =
              specs.get(SpecKind.BindingMethod)
                   .contains(SpecValue.BindingMethodSpec(BindingMethod.SaddleStitch)) &&
                (comp.role == ComponentRole.Cover || comp.role == ComponentRole.Body)
            val flatWidthMm = if isSaddleStitchFolded then dim.widthMm * 2 else dim.widthMm
            val sheetAreaM2 = (flatWidthMm / 1000.0) * (dim.heightMm / 1000.0)
            val weightPerItemG = comp.sheetCount * sheetAreaM2 * gsm
            Validation.succeed(ComponentWeightBreakdown(
              role           = comp.role,
              materialName   = comp.material.name.value,
              gsmWeight      = gsm,
              sheetsPerItem  = comp.sheetCount,
              sheetAreaM2    = sheetAreaM2,
              weightPerItemG = weightPerItemG,
              totalWeightG   = weightPerItemG * quantity,
            ))
          case None =>
            Validation.fail(WeightError.NoWeightForMaterial(comp.material.id, comp.role))
      case _ =>
        Validation.fail(WeightError.NoSizeInSpecifications)

  private def extractQuantity(specs: ProductSpecifications): Validation[WeightError, Int] =
    specs.get(SpecKind.Quantity) match
      case Some(SpecValue.QuantitySpec(q)) => Validation.succeed(q.value)
      case _                              => Validation.fail(WeightError.NoQuantityInSpecifications)
