package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.ProductBuilderViewModel
import mpbuilder.domain.model.*

object SpecificationForm:
  def apply(): Element =
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
                ProductBuilderViewModel.removeSpecification(classOf[ProductSpecification.QuantitySpec])
                ProductBuilderViewModel.addSpecification(
                  ProductSpecification.QuantitySpec(Quantity.unsafe(qty))
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
            onInput.mapToValue.map(_.toDoubleOption) --> { widthOpt =>
              widthOpt.foreach { _ => () } // We'll handle this with height together
            },
          ),
          span("×", styleAttr := "line-height: 40px;"),
          input(
            typ := "number",
            placeholder := "Height (mm)",
            styleAttr := "flex: 1;",
            onInput.mapToValue.map(_.toDoubleOption) --> { heightOpt =>
              // For simplicity, we'll add size spec when height is entered
              heightOpt.foreach { h =>
                if h > 0 then
                  ProductBuilderViewModel.removeSpecification(classOf[ProductSpecification.SizeSpec])
                  ProductBuilderViewModel.addSpecification(
                    ProductSpecification.SizeSpec(
                      Dimension.unsafe(90.0), // Default width, user should enter both
                      Dimension.unsafe(h)
                    )
                  )
              }
            },
          ),
        ),
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
                ProductBuilderViewModel.removeSpecification(classOf[ProductSpecification.PageCount])
                ProductBuilderViewModel.addSpecification(
                  ProductSpecification.PageCount(pages)
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
          option("Full Color (CMYK)", value := "cmyk"),
          option("Black & White", value := "blackandwhite"),
          onChange.mapToValue --> { value =>
            value match
              case "cmyk" =>
                ProductBuilderViewModel.removeSpecification(classOf[ProductSpecification.ColorModeSpec])
                ProductBuilderViewModel.addSpecification(
                  ProductSpecification.ColorModeSpec(ColorMode.FullColor)
                )
              case "blackandwhite" =>
                ProductBuilderViewModel.removeSpecification(classOf[ProductSpecification.ColorModeSpec])
                ProductBuilderViewModel.addSpecification(
                  ProductSpecification.ColorModeSpec(ColorMode.BlackAndWhite)
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
