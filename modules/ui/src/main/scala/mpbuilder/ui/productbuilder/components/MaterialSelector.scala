package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.ui.components.HelpInfo
import mpbuilder.domain.model.*
import mpbuilder.uikit.fields.{SelectField, SelectOption}

object MaterialSelector:
  def apply(role: ComponentRole): Element =
    val availableMaterials = ProductBuilderViewModel.availableMaterials(role)
    val selectedMaterialId = ProductBuilderViewModel.selectedMaterialId(role)
    val lang = ProductBuilderViewModel.currentLanguage
    val catalog = ProductBuilderViewModel.catalog

    div(
      cls := "selector-with-help",
      SelectField(
        label = lang.map {
          case Language.En => "Material:"
          case Language.Cs => "Materiál:"
        },
        options = availableMaterials.combineWith(lang).map { case (materials, l) =>
          materials.map(mat => SelectOption(mat.id.value, mat.name(l)))
        },
        selected = selectedMaterialId.map(_.map(_.value).getOrElse("")),
        onChange = Observer[String] { value =>
          if value.nonEmpty then
            ProductBuilderViewModel.selectMaterial(role, MaterialId.unsafe(value))
        },
        placeholder = lang.map {
          case Language.En => "-- Select a material --"
          case Language.Cs => "-- Vyberte materiál --"
        },
        disabled = ProductBuilderViewModel.state.map(_.selectedCategoryId.isEmpty),
      ),
      div(
        cls := "selector-help-buttons",
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
        cls := "info-box",
        child.maybe <-- availableMaterials.combineWith(lang).map { case (materials, l) =>
          materials.headOption.map { _ =>
            span(
              l match
                case Language.En => s"${materials.size} material(s) available for this category"
                case Language.Cs => s"${materials.size} materiál(ů) dostupných pro tuto kategorii"
            )
          }
        },
      ),
    )
