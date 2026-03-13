package mpbuilder.ui.catalog.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.catalog.*

/** Editor view for managing Finishes in the catalog. */
object FinishEditorView:

  def apply(): HtmlElement =
    div(
      cls := "catalog-section",
      div(
        cls := "catalog-section-header",
        h3("Finishes"),
        FormComponents.actionButton("+ Add Finish", () =>
          CatalogEditorViewModel.setEditState(EditState.CreatingFinish)
        ),
      ),

      div(
        cls := "catalog-entity-list",
        children <-- CatalogEditorViewModel.finishes.map { fins =>
          if fins.isEmpty then List(p(cls := "empty-message", "No finishes defined."))
          else fins.map(finishRow)
        },
      ),

      child <-- CatalogEditorViewModel.editState.combineWith(CatalogEditorViewModel.catalog).map {
        case (EditState.CreatingFinish, _) => finishForm(None)
        case (EditState.EditingFinish(id), cat) =>
          cat.finishes.get(id).map(f => finishForm(Some(f))).getOrElse(emptyNode)
        case _ => emptyNode
      },
    )

  private def finishRow(fin: Finish): HtmlElement =
    div(
      cls := "catalog-entity-row",
      div(
        cls := "entity-info",
        span(cls := "entity-id", fin.id.value),
        span(cls := "entity-name", fin.name.value),
        span(cls := "entity-detail", s"${fin.finishType} · ${fin.side}"),
      ),
      div(
        cls := "entity-actions",
        button(cls := "btn btn-sm", "Edit", onClick --> { _ =>
          CatalogEditorViewModel.setEditState(EditState.EditingFinish(fin.id))
        }),
        button(cls := "btn btn-sm btn-danger", "Remove", onClick --> { _ =>
          CatalogEditorViewModel.removeFinish(fin.id)
        }),
      ),
    )

  private def finishForm(existing: Option[Finish]): HtmlElement =
    val idVar = Var(existing.map(_.id.value).getOrElse(""))
    val nameEnVar = Var(existing.map(_.name(Language.En)).getOrElse(""))
    val nameCsVar = Var(existing.map(_.name(Language.Cs)).getOrElse(""))
    val finishTypeVar = Var(existing.map(_.finishType).getOrElse(FinishType.Lamination))
    val sideVar = Var(existing.map(_.side).getOrElse(FinishSide.Both))

    div(
      cls := "catalog-edit-form",
      h4(if existing.isDefined then "Edit Finish" else "New Finish"),

      FormComponents.textField("ID", idVar.signal, idVar.writer, "e.g. matte-lamination"),
      FormComponents.textField("Name (EN)", nameEnVar.signal, nameEnVar.writer),
      FormComponents.textField("Name (CS)", nameCsVar.signal, nameCsVar.writer),

      FormComponents.enumSelectRequired[FinishType](
        "Finish Type", FinishType.values, finishTypeVar.signal, finishTypeVar.writer,
      ),

      FormComponents.enumSelectRequired[FinishSide](
        "Side", FinishSide.values, sideVar.signal, sideVar.writer,
      ),

      div(
        cls := "form-actions",
        FormComponents.actionButton("Save", () => {
          val id = idVar.now()
          if id.nonEmpty && nameEnVar.now().nonEmpty then
            val fin = Finish(
              id = FinishId.unsafe(id),
              name = LocalizedString(nameEnVar.now(), nameCsVar.now()),
              finishType = finishTypeVar.now(),
              side = sideVar.now(),
            )
            if existing.isDefined then CatalogEditorViewModel.updateFinish(fin)
            else CatalogEditorViewModel.addFinish(fin)
        }),
        FormComponents.dangerButton("Cancel", () =>
          CatalogEditorViewModel.setEditState(EditState.None)
        ),
      ),
    )
