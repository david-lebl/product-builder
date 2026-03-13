package mpbuilder.ui.catalog.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.catalog.*

/** Editor view for managing Product Categories in the catalog.
  *
  * Categories are the most complex entity because they include ComponentTemplates
  * with cross-references to materials and finishes.
  */
object CategoryEditorView:

  def apply(): HtmlElement =
    div(
      cls := "catalog-section",
      div(
        cls := "catalog-section-header",
        h3("Product Categories"),
        FormComponents.actionButton("+ Add Category", () =>
          CatalogEditorViewModel.setEditState(EditState.CreatingCategory)
        ),
      ),

      div(
        cls := "catalog-entity-list",
        children <-- CatalogEditorViewModel.categories.map { cats =>
          if cats.isEmpty then List(p(cls := "empty-message", "No categories defined. Add one or load sample data."))
          else cats.map(categoryRow)
        },
      ),

      child <-- CatalogEditorViewModel.editState.combineWith(CatalogEditorViewModel.catalog).map {
        case (EditState.CreatingCategory, _) => categoryForm(None)
        case (EditState.EditingCategory(id), cat) =>
          cat.categories.get(id).map(c => categoryForm(Some(c))).getOrElse(emptyNode)
        case _ => emptyNode
      },
    )

  private def categoryRow(cat: ProductCategory): HtmlElement =
    div(
      cls := "catalog-entity-row",
      div(
        cls := "entity-info",
        span(cls := "entity-id", cat.id.value),
        span(cls := "entity-name", cat.name.value),
        span(cls := "entity-detail", s"${cat.components.size} component(s)"),
        span(cls := "entity-tags",
          s"Specs: ${cat.requiredSpecKinds.map(_.toString).mkString(", ")}"
        ),
      ),
      div(
        cls := "entity-actions",
        button(cls := "btn btn-sm", "Edit", onClick --> { _ =>
          CatalogEditorViewModel.setEditState(EditState.EditingCategory(cat.id))
        }),
        button(cls := "btn btn-sm btn-danger", "Remove", onClick --> { _ =>
          CatalogEditorViewModel.removeCategory(cat.id)
        }),
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
      cls := "catalog-edit-form",
      h4(if existing.isDefined then "Edit Category" else "New Category"),

      FormComponents.textField("ID", idVar.signal, idVar.writer, "e.g. business-cards"),
      FormComponents.textField("Name (EN)", nameEnVar.signal, nameEnVar.writer),
      FormComponents.textField("Name (CS)", nameCsVar.signal, nameCsVar.writer),

      FormComponents.enumCheckboxSet[SpecKind](
        "Required Specifications", SpecKind.values, specKindsVar.signal, specKindsVar.writer,
      ),

      // Printing method selection — show IDs from catalog
      FormComponents.idCheckboxSet[PrintingMethodId](
        "Allowed Printing Methods (empty = all)",
        CatalogEditorViewModel.printingMethods.map(_.map(pm => pm.id -> pm.name.value)),
        printingMethodIdsVar.signal,
        printingMethodIdsVar.writer,
        _.value,
        PrintingMethodId.unsafe,
      ),

      // Component templates
      div(
        cls := "form-group",
        com.raquo.laminar.api.L.label("Component Templates"),
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
        cls := "form-actions",
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
