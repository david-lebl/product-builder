package mpbuilder.ui.catalog.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.catalog.*
import mpbuilder.uikit.containers.*

/** Editor view for managing Product Categories in the catalog using SplitTableView.
  *
  * Categories are the most complex entity because they include ComponentTemplates
  * with cross-references to materials and finishes.
  */
object CategoryEditorView:

  def apply(): HtmlElement =
    val searchVar = Var("")
    val selectedId: Var[Option[String]] = Var(None)

    val filteredCategories: Signal[List[ProductCategory]] =
      CatalogEditorViewModel.categories.combineWith(searchVar.signal).map { case (cats, query) =>
        val q = query.trim.toLowerCase
        if q.isEmpty then cats
        else cats.filter { c =>
          c.id.value.toLowerCase.contains(q) ||
          c.name.value.toLowerCase.contains(q)
        }
      }

    val tableConfig = SplitTableConfig[ProductCategory](
      columns = List(
        ColumnDef("ID", c => span(cls := "entity-id", c.id.value), Some(_.id.value), Some("150px")),
        ColumnDef("Name", c => span(c.name.value), Some(_.name.value)),
        ColumnDef("Components", c => span(s"${c.components.size}"), width = Some("100px")),
        ColumnDef("Specs", c => span(cls := "entity-tags", c.requiredSpecKinds.map(_.toString).mkString(", "))),
        ColumnDef("", c => div(
          cls := "entity-actions",
          button(cls := "btn btn-sm btn-danger", "✕", onClick.stopPropagation --> { _ =>
            CatalogEditorViewModel.removeCategory(c.id)
          }),
        ), width = Some("50px")),
      ),
      rowKey = _.id.value,
      searchPlaceholder = "Search categories…",
      onRowSelect = Some(c => {
        selectedId.set(Some(c.id.value))
        CatalogEditorViewModel.setEditState(EditState.EditingCategory(c.id))
      }),
      emptyMessage = "No categories defined. Add one or load sample data.",
    )

    val sidePanel: Signal[Option[HtmlElement]] =
      CatalogEditorViewModel.editState.combineWith(CatalogEditorViewModel.catalog).map {
        case (EditState.CreatingCategory, _) =>
          Some(categoryForm(None))
        case (EditState.EditingCategory(id), cat) =>
          cat.categories.get(id).map(c => categoryForm(Some(c)))
        case _ => None
      }

    div(
      cls := "catalog-section",
      h2(cls := "manufacturing-view-title", "Product Categories"),
      SplitTableView(
        config = tableConfig,
        items = filteredCategories,
        selectedKey = selectedId.signal,
        searchQuery = searchVar,
        sidePanel = sidePanel,
        headerActions = Some(
          FormComponents.actionButton("+ Add Category", () => {
            selectedId.set(None)
            CatalogEditorViewModel.setEditState(EditState.CreatingCategory)
          })
        ),
      ),
    )

  private def categoryForm(existing: Option[ProductCategory]): HtmlElement =
    val idVar = Var(existing.map(_.id.value).getOrElse(""))
    val nameEnVar = Var(existing.map(_.name(Language.En)).getOrElse(""))
    val nameCsVar = Var(existing.map(_.name(Language.Cs)).getOrElse(""))
    val specKindsVar = Var(existing.map(_.requiredSpecKinds).getOrElse(Set.empty[SpecKind]))
    val printingMethodIdsVar = Var(existing.map(_.allowedPrintingMethodIds).getOrElse(Set.empty[PrintingMethodId]))
    val componentsVar = Var(existing.map(_.components).getOrElse(
      List(ComponentTemplate(ComponentRole.Main, Set.empty, Set.empty))
    ))

    div(
      cls := "catalog-detail-panel",

      button(cls := "detail-panel-close", "×", onClick --> { _ =>
        CatalogEditorViewModel.setEditState(EditState.None)
      }),

      div(cls := "detail-panel-header",
        h3(if existing.isDefined then "Edit Category" else "New Category"),
      ),

      div(cls := "detail-panel-section",
        FormComponents.textField("ID", idVar.signal, idVar.writer, "e.g. business-cards"),
        FormComponents.textField("Name (EN)", nameEnVar.signal, nameEnVar.writer),
        FormComponents.textField("Name (CS)", nameCsVar.signal, nameCsVar.writer),

        FormComponents.enumCheckboxSet[SpecKind](
          "Required Specifications", SpecKind.values, specKindsVar.signal, specKindsVar.writer,
        ),

        FormComponents.idCheckboxSet[PrintingMethodId](
          "Allowed Printing Methods (empty = all)",
          CatalogEditorViewModel.printingMethods.map(_.map(pm => pm.id -> pm.name.value)),
          printingMethodIdsVar.signal,
          printingMethodIdsVar.writer,
          _.value,
          PrintingMethodId.unsafe,
        ),
      ),

      // Component templates
      div(
        cls := "detail-panel-section",
        h4("Component Templates"),
        children <-- componentsVar.signal.map { comps =>
          comps.zipWithIndex.map { case (comp, idx) =>
            componentTemplateEditor(comp, idx, componentsVar)
          }
        },
        button(
          cls := "btn btn-sm",
          "+ Add Component",
          onClick --> { _ =>
            componentsVar.update(_ :+ ComponentTemplate(ComponentRole.Body, Set.empty, Set.empty))
          },
        ),
      ),

      div(
        cls := "detail-panel-actions",
        FormComponents.actionButton("Save", () => {
          val id = idVar.now()
          if id.nonEmpty && nameEnVar.now().nonEmpty then
            val cat = ProductCategory(
              id = CategoryId.unsafe(id),
              name = LocalizedString(nameEnVar.now(), nameCsVar.now()),
              components = componentsVar.now(),
              requiredSpecKinds = specKindsVar.now(),
              allowedPrintingMethodIds = printingMethodIdsVar.now(),
            )
            if existing.isDefined then CatalogEditorViewModel.updateCategory(cat)
            else CatalogEditorViewModel.addCategory(cat)
        }),
        FormComponents.dangerButton("Cancel", () =>
          CatalogEditorViewModel.setEditState(EditState.None)
        ),
      ),
    )

  private def componentTemplateEditor(
    comp: ComponentTemplate,
    index: Int,
    componentsVar: Var[List[ComponentTemplate]],
  ): HtmlElement =
    val roleVar = Var(comp.role)
    val materialIdsVar = Var(comp.allowedMaterialIds)
    val finishIdsVar = Var(comp.allowedFinishIds)
    val optionalVar = Var(comp.optional)

    div(
      cls := "component-template-editor",
      h5(s"Component #${index + 1}"),

      FormComponents.enumSelectRequired[ComponentRole](
        "Role", ComponentRole.values, roleVar.signal,
        Observer[ComponentRole] { role =>
          roleVar.set(role)
          componentsVar.update { comps =>
            comps.updated(index, comps(index).copy(role = role))
          }
        },
      ),

      FormComponents.idCheckboxSet[MaterialId](
        "Allowed Materials",
        CatalogEditorViewModel.materials.map(_.map(m => m.id -> m.name.value)),
        materialIdsVar.signal,
        Observer[Set[MaterialId]] { ids =>
          materialIdsVar.set(ids)
          componentsVar.update { comps =>
            comps.updated(index, comps(index).copy(allowedMaterialIds = ids))
          }
        },
        _.value,
        MaterialId.unsafe,
      ),

      FormComponents.idCheckboxSet[FinishId](
        "Allowed Finishes",
        CatalogEditorViewModel.finishes.map(_.map(f => f.id -> f.name.value)),
        finishIdsVar.signal,
        Observer[Set[FinishId]] { ids =>
          finishIdsVar.set(ids)
          componentsVar.update { comps =>
            comps.updated(index, comps(index).copy(allowedFinishIds = ids))
          }
        },
        _.value,
        FinishId.unsafe,
      ),

      div(
        cls := "form-group",
        com.raquo.laminar.api.L.label(
          cls := "checkbox-label",
          input(
            typ := "checkbox",
            checked <-- optionalVar.signal,
            onChange.mapToChecked --> { v =>
              optionalVar.set(v)
              componentsVar.update { comps =>
                comps.updated(index, comps(index).copy(optional = v))
              }
            },
          ),
          span("Optional component"),
        ),
      ),

      if index > 0 then
        button(
          cls := "btn btn-sm btn-danger",
          "Remove",
          onClick --> { _ =>
            componentsVar.update(_.zipWithIndex.filterNot(_._2 == index).map(_._1))
          },
        )
      else emptyNode,
    )
