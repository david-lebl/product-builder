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
    
    // Signal for required spec kinds based on selected category
    val requiredSpecs = ProductBuilderViewModel.requiredSpecKinds
    
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

      // Orientation (required for Flyers)
      div(
        cls := "form-group",
        display <-- requiredSpecs.map(kinds =>
          if kinds.contains(SpecKind.Orientation) then "block" else "none"
        ),
        label("Orientation:"),
        select(
          option("-- Select orientation --", value := ""),
          option("Portrait", value := "portrait"),
          option("Landscape", value := "landscape"),
          onChange.mapToValue --> { value =>
            value match
              case "portrait" =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.OrientationSpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.OrientationSpec(Orientation.Portrait)
                )
              case "landscape" =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.OrientationSpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.OrientationSpec(Orientation.Landscape)
                )
              case _ => ()
          },
        ),
      ),

      // Fold Type (required for Brochures)
      div(
        cls := "form-group",
        display <-- requiredSpecs.map(kinds =>
          if kinds.contains(SpecKind.FoldType) then "block" else "none"
        ),
        label("Fold Type:"),
        select(
          option("-- Select fold type --", value := ""),
          FoldType.values.map { ft =>
            option(ft.toString, value := ft.toString)
          }.toSeq,
          onChange.mapToValue --> { value =>
            if value.nonEmpty then
              FoldType.values.find(_.toString == value).foreach { ft =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.FoldTypeSpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.FoldTypeSpec(ft)
                )
              }
          },
        ),
      ),

      // Binding Method (required for Booklets)
      div(
        cls := "form-group",
        display <-- requiredSpecs.map(kinds =>
          if kinds.contains(SpecKind.BindingMethod) then "block" else "none"
        ),
        label("Binding Method:"),
        select(
          option("-- Select binding method --", value := ""),
          BindingMethod.values.map { bm =>
            option(bm.toString, value := bm.toString)
          }.toSeq,
          onChange.mapToValue --> { value =>
            if value.nonEmpty then
              BindingMethod.values.find(_.toString == value).foreach { bm =>
                ProductBuilderViewModel.removeSpecification(classOf[SpecValue.BindingMethodSpec])
                ProductBuilderViewModel.addSpecification(
                  SpecValue.BindingMethodSpec(bm)
                )
              }
          },
        ),
      ),
      
      div(
        cls := "info-box",
        p("Note: Additional specifications like binding type or lamination can be added based on your product category."),
      ),
    )
