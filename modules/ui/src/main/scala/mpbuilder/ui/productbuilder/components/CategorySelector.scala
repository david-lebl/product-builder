package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.uikit.feedback.HelpInfo
import mpbuilder.uikit.fields.{ComboBoxField, ComboBoxOption}
import mpbuilder.domain.model.*
import mpbuilder.domain.sample.SampleShowcase

object CategorySelector:

  private val groupOrder: List[CatalogGroup] =
    List(
      CatalogGroup.Sheet,
      CatalogGroup.Bound,
      CatalogGroup.LargeFormat,
      CatalogGroup.Specialty,
      CatalogGroup.Promotional,
    )

  private def groupName(group: CatalogGroup, lang: Language): String =
    (group, lang) match
      case (CatalogGroup.Sheet,       Language.En) => "Sheet Products"
      case (CatalogGroup.Sheet,       Language.Cs) => "Tiskoviny"
      case (CatalogGroup.LargeFormat, Language.En) => "Large Format"
      case (CatalogGroup.LargeFormat, Language.Cs) => "Velkoformát"
      case (CatalogGroup.Bound,       Language.En) => "Bound Products"
      case (CatalogGroup.Bound,       Language.Cs) => "Vázané produkty"
      case (CatalogGroup.Specialty,   Language.En) => "Specialty"
      case (CatalogGroup.Specialty,   Language.Cs) => "Speciální produkty"
      case (CatalogGroup.Promotional, Language.En) => "Promotional"
      case (CatalogGroup.Promotional, Language.Cs) => "Reklamní předměty"

  def apply(): Element =
    val selectedCategoryId = ProductBuilderViewModel.state.map(_.selectedCategoryId)
    val lang               = ProductBuilderViewModel.currentLanguage
    val catalog            = ProductBuilderViewModel.catalog

    val options: Signal[List[ComboBoxOption]] = lang.map { l =>
      val allCats = ProductBuilderViewModel.allCategories

      val (withShowcase, withoutShowcase) =
        allCats.partition(c => SampleShowcase.forCategory(c.id).isDefined)

      val showcaseOptions = withShowcase
        .flatMap(c => SampleShowcase.forCategory(c.id).map(sp => (sp, c)))
        .sortBy { case (sp, _) => (groupOrder.indexOf(sp.group), sp.sortOrder) }
        .map { case (sp, c) =>
          ComboBoxOption(
            value   = c.id.value,
            display = c.name(l),
            icon    = if sp.icon.nonEmpty then Some(sp.icon) else None,
            group   = Some(groupName(sp.group, l)),
          )
        }

      val ungroupedOptions = withoutShowcase
        .sortBy(_.name(l))
        .map(c => ComboBoxOption(value = c.id.value, display = c.name(l)))

      showcaseOptions ++ ungroupedOptions
    }

    val selectedValue: Signal[String] =
      selectedCategoryId.map(_.map(_.value).getOrElse(""))

    val placeholder: Signal[String] = lang.map {
      case Language.En => "Select a category..."
      case Language.Cs => "Vyberte kategorii..."
    }

    val onChangeObserver: Observer[String] = Observer[String] { value =>
      if value.nonEmpty then
        ProductBuilderViewModel.selectCategory(CategoryId.unsafe(value))
    }

    div(
      cls := "form-group form-group--horizontal",
      div(
        cls := "label-with-help",
        label(child.text <-- lang.map {
          case Language.En => "Category:"
          case Language.Cs => "Kategorie:"
        }),
        HelpInfo(lang.map {
          case Language.En =>
            "The product category determines which materials, finishes, and printing methods are available. Each category has its own set of configuration options."
          case Language.Cs =>
            "Kategorie produktu určuje, které materiály, povrchové úpravy a tiskové metody jsou k dispozici. Každá kategorie má vlastní sadu konfiguračních možností."
        }),
        HelpInfo.fromSignal(
          selectedCategoryId.combineWith(lang).map { case (catIdOpt, l) =>
            catIdOpt.flatMap(id => catalog.categories.get(id)).flatMap(_.description).map(_(l))
          }
        ),
      ),
      div(
        cls := "form-group__control",
        ComboBoxField.widget(
          options     = options,
          selected    = selectedValue,
          onChange    = onChangeObserver,
          placeholder = placeholder,
        ),
      ),
    )
