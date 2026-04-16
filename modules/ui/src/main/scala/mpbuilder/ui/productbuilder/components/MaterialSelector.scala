package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.uikit.feedback.HelpInfo
import mpbuilder.domain.model.*

object MaterialSelector:
  def apply(role: ComponentRole): Element =
    val availableMaterials = ProductBuilderViewModel.availableMaterials(role)
    val selectedMaterialId = ProductBuilderViewModel.selectedMaterialId(role)
    val lang = ProductBuilderViewModel.currentLanguage
    val catalog = ProductBuilderViewModel.catalog

    div(
      cls := "form-group form-group--horizontal",
      div(
        cls := "label-with-help",
        label(child.text <-- lang.map {
          case Language.En => "Material:"
          case Language.Cs => "Materiál:"
        }),
        HelpInfo(lang.map {
          case Language.En => "The material (paper or substrate) used for this component. Heavier weights (gsm) feel thicker and more premium. Choose based on the intended use and feel."
          case Language.Cs => "Materiál (papír nebo substrát) použitý pro tuto komponentu. Vyšší gramáže (g) působí silněji a prémiověji. Vybírejte podle zamýšleného použití a dojmu."
        }),
        HelpInfo.fromSignal(
          selectedMaterialId.combineWith(lang).map { case (matIdOpt, l) =>
            matIdOpt.flatMap(id => catalog.materials.get(id)).flatMap(_.description).map(_(l))
          }
        ),
      ),
      div(
        cls := "form-group__control",
        select(
          disabled <-- ProductBuilderViewModel.state.map(_.selectedCategoryId.isEmpty),
          children <-- availableMaterials.combineWith(lang, selectedMaterialId).map { case (materials, l, selOpt) =>
            val sel = selOpt.map(_.value).getOrElse("")
            val ph = l match
              case Language.En => "-- Select a material --"
              case Language.Cs => "-- Vyberte materiál --"
            val placeholderOpt = List(option(ph, value := "", com.raquo.laminar.api.L.selected := sel.isEmpty))
            placeholderOpt ++ materials.map { mat =>
              option(mat.name(l), value := mat.id.value, com.raquo.laminar.api.L.selected := (mat.id.value == sel))
            }
          },
          onChange.mapToValue --> Observer[String] { value =>
            if value.nonEmpty then
              ProductBuilderViewModel.selectMaterial(role, MaterialId.unsafe(value))
          },
        ),
        div(
          cls := "info-note",
          child.maybe <-- availableMaterials.combineWith(lang).map { case (materials, l) =>
            materials.headOption.map { _ =>
              span(
                l match
                  case Language.En => s"${materials.size} material(s) available"
                  case Language.Cs => s"${materials.size} materiál(ů) dostupných"
              )
            }
          },
        ),
      ),
    )
