package mpbuilder.ui.catalog.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.catalog.*

/** Editor view for managing Materials in the catalog. */
object MaterialEditorView:

  def apply(): HtmlElement =
    div(
      cls := "catalog-section",
      div(
        cls := "catalog-section-header",
        h3("Materials"),
        FormComponents.actionButton("+ Add Material", () =>
          CatalogEditorViewModel.setEditState(EditState.CreatingMaterial)
        ),
      ),

      // Material list
      div(
        cls := "catalog-entity-list",
        children <-- CatalogEditorViewModel.materials.map { mats =>
          if mats.isEmpty then List(p(cls := "empty-message", "No materials defined. Add one or load sample data."))
          else mats.map(materialRow)
        },
      ),

      // Edit form
      child <-- CatalogEditorViewModel.editState.combineWith(CatalogEditorViewModel.catalog).map {
        case (EditState.CreatingMaterial, _) => materialForm(None)
        case (EditState.EditingMaterial(id), cat) =>
          cat.materials.get(id).map(m => materialForm(Some(m))).getOrElse(emptyNode)
        case _ => emptyNode
      },
    )

  private def materialRow(mat: Material): HtmlElement =
    div(
      cls := "catalog-entity-row",
      div(
        cls := "entity-info",
        span(cls := "entity-id", mat.id.value),
        span(cls := "entity-name", mat.name.value),
        span(cls := "entity-detail", s"${mat.family} ${mat.weight.map(w => s"${w.gsm}gsm").getOrElse("")}"),
        span(cls := "entity-tags", mat.properties.map(_.toString).mkString(", ")),
      ),
      div(
        cls := "entity-actions",
        button(cls := "btn btn-sm", "Edit", onClick --> { _ =>
          CatalogEditorViewModel.setEditState(EditState.EditingMaterial(mat.id))
        }),
        button(cls := "btn btn-sm btn-danger", "Remove", onClick --> { _ =>
          CatalogEditorViewModel.removeMaterial(mat.id)
        }),
      ),
    )

  private def materialForm(existing: Option[Material]): HtmlElement =
    val idVar = Var(existing.map(_.id.value).getOrElse(""))
    val nameEnVar = Var(existing.map(_.name(Language.En)).getOrElse(""))
    val nameCsVar = Var(existing.map(_.name(Language.Cs)).getOrElse(""))
    val familyVar = Var(existing.map(_.family).getOrElse(MaterialFamily.Paper))
    val weightVar = Var(existing.flatMap(_.weight).map(_.gsm.toString).getOrElse(""))
    val propsVar = Var(existing.map(_.properties).getOrElse(Set.empty[MaterialProperty]))

    div(
      cls := "catalog-edit-form",
      h4(if existing.isDefined then "Edit Material" else "New Material"),

      FormComponents.textField("ID", idVar.signal, idVar.writer, "e.g. coated-300gsm"),

      FormComponents.textField("Name (EN)", nameEnVar.signal, nameEnVar.writer),
      FormComponents.textField("Name (CS)", nameCsVar.signal, nameCsVar.writer),

      FormComponents.enumSelectRequired[MaterialFamily](
        "Family", MaterialFamily.values, familyVar.signal, familyVar.writer,
      ),

      FormComponents.numberField("Weight (gsm)", weightVar.signal, weightVar.writer),

      FormComponents.enumCheckboxSet[MaterialProperty](
        "Properties", MaterialProperty.values, propsVar.signal, propsVar.writer,
      ),

      div(
        cls := "form-actions",
        FormComponents.actionButton("Save", () => {
          val id = idVar.now()
          if id.nonEmpty && nameEnVar.now().nonEmpty then
            val mat = Material(
              id = MaterialId.unsafe(id),
              name = LocalizedString(nameEnVar.now(), nameCsVar.now()),
              family = familyVar.now(),
              weight = weightVar.now().toIntOption.map(PaperWeight.unsafe),
              properties = propsVar.now(),
            )
            if existing.isDefined then CatalogEditorViewModel.updateMaterial(mat)
            else CatalogEditorViewModel.addMaterial(mat)
        }),
        FormComponents.dangerButton("Cancel", () =>
          CatalogEditorViewModel.setEditState(EditState.None)
        ),
      ),
    )
