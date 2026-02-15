package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object SpecificationForm:
  def apply(): Element =
    // Vars to track width and height inputs
    val widthVar = Var[Option[Double]](None)
    val heightVar = Var[Option[Double]](None)
    
    // Combined signal that updates size spec when both are present
    val sizeSignal = widthVar.signal.combineWith(heightVar.signal)
    
    div(
      cls := "form-group",
      
      // Quantity
      div(
        cls := "form-group",
        label("Quantity:"),
        input(
          typ := "number",
          placeholder := "e.g., 1000",
          onInput.mapToValue.map(_.toIntOption) --> { qtyOpt =>
            qtyOpt.foreach { qty =>
              if qty > 0 then
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.QuantitySpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.QuantitySpec(Quantity.unsafe(qty))
                )
            }
          },
        ),
      ),
      
      // Size (Width x Height in mm)
      div(
        cls := "form-group",
        label("Size (Width x Height in mm):"),
        div(
          styleAttr := "display: flex; gap: 10px;",
          input(
            typ := "number",
            placeholder := "Width (mm)",
            styleAttr := "flex: 1;",
            onInput.mapToValue.map(_.toDoubleOption) --> widthVar.writer,
          ),
          span("×", styleAttr := "line-height: 40px;"),
          input(
            typ := "number",
            placeholder := "Height (mm)",
            styleAttr := "flex: 1;",
            onInput.mapToValue.map(_.toDoubleOption) --> heightVar.writer,
          ),
        ),
        // Observer that updates the spec when both width and height are available
        sizeSignal --> { case (widthOpt, heightOpt) =>
          (widthOpt, heightOpt) match
            case (Some(w), Some(h)) if w > 0 && h > 0 =>
              ProductBuilderViewModel.removeSpecification(classOf[SpecValue.SizeSpec])
              ProductBuilderViewModel.addSpecification(
                SpecValue.SizeSpec(Dimension(w, h))
              )
            case _ => ()
        },
      ),
      
      // Pages (for multi-page products)
      div(
        cls := "form-group",
        label("Number of Pages (optional, for booklets/brochures):"),
        input(
          typ := "number",
          placeholder := "e.g., 8",
          onInput.mapToValue.map(_.toIntOption) --> { pagesOpt =>
            pagesOpt.foreach { pages =>
              if pages > 0 then
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.PagesSpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.PagesSpec(pages)
                )
            }
          },
        ),
      ),
      
      // Color Mode
      div(
        cls := "form-group",
        label("Color Mode:"),
        select(
          option("-- Select color mode --", value := ""),
          option("CMYK (Full Color)", value := "cmyk"),
          option("Grayscale", value := "grayscale"),
          onChange.mapToValue --> { value =>
            value match
              case "cmyk" =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.ColorModeSpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.ColorModeSpec(ColorMode.CMYK)
                )
              case "grayscale" =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.ColorModeSpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.ColorModeSpec(ColorMode.Grayscale)
                )
              case _ => ()
          },
        ),
      ),
      
      div(
        cls := "info-box",
        p("Note: Additional specifications like binding type or lamination can be added based on your product category."),
      ),
    )
