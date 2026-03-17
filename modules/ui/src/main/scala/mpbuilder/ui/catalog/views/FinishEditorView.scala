package mpbuilder.ui.catalog.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.catalog.*
import mpbuilder.uikit.containers.*

/** Editor view for managing Finishes in the catalog using SplitTableView. */
object FinishEditorView:

  def apply(): HtmlElement =
    val searchVar = Var("")
    val selectedId: Var[Option[String]] = Var(None)

    val filteredFinishes: Signal[List[Finish]] =
      CatalogEditorViewModel.finishes.combineWith(searchVar.signal).map { case (fins, query) =>
        val q = query.trim.toLowerCase
        if q.isEmpty then fins
        else fins.filter { f =>
          f.id.value.toLowerCase.contains(q) ||
          f.name.value.toLowerCase.contains(q) ||
          f.finishType.toString.toLowerCase.contains(q)
        }
      }

    val tableConfig = SplitTableConfig[Finish](
      columns = List(
        ColumnDef("ID", f => span(cls := "entity-id", f.id.value), Some(_.id.value), Some("160px")),
        ColumnDef("Name", f => span(f.name.value), Some(_.name.value)),
        ColumnDef("Type", f => span(f.finishType.toString), Some(_.finishType.toString), Some("120px")),
        ColumnDef("Side", f => span(f.side.toString), Some(_.side.toString), Some("80px")),
        ColumnDef("", f => div(
          cls := "entity-actions",
          button(cls := "btn btn-sm btn-danger", "✕", onClick.stopPropagation --> { _ =>
            CatalogEditorViewModel.removeFinish(f.id)
          }),
        ), width = Some("50px")),
      ),
      rowKey = _.id.value,
      searchPlaceholder = "Search finishes…",
      onRowSelect = Some(f => {
        selectedId.set(Some(f.id.value))
        CatalogEditorViewModel.setEditState(EditState.EditingFinish(f.id))
      }),
      emptyMessage = "No finishes defined.",
    )

    val sidePanel: Signal[Option[HtmlElement]] =
      CatalogEditorViewModel.editState.combineWith(CatalogEditorViewModel.catalog).map {
        case (EditState.CreatingFinish, _) =>
          Some(finishForm(None))
        case (EditState.EditingFinish(id), cat) =>
          cat.finishes.get(id).map(f => finishForm(Some(f)))
        case _ => None
      }

    div(
      cls := "catalog-section",
      h2(cls := "manufacturing-view-title", "Finishes"),
      SplitTableView(
        config = tableConfig,
        items = filteredFinishes,
        selectedKey = selectedId.signal,
        searchQuery = searchVar,
        sidePanel = sidePanel,
        headerActions = Some(
          FormComponents.actionButton("+ Add Finish", () => {
            selectedId.set(None)
            CatalogEditorViewModel.setEditState(EditState.CreatingFinish)
          })
        ),
      ),
    )

  private def finishForm(existing: Option[Finish]): HtmlElement =
    val idVar = Var(existing.map(_.id.value).getOrElse(""))
    val nameEnVar = Var(existing.map(_.name(Language.En)).getOrElse(""))
    val nameCsVar = Var(existing.map(_.name(Language.Cs)).getOrElse(""))
    val finishTypeVar = Var(existing.map(_.finishType).getOrElse(FinishType.Lamination))
    val sideVar = Var(existing.map(_.side).getOrElse(FinishSide.Both))
    val descEnVar = Var(existing.flatMap(_.description).map(_(Language.En)).getOrElse(""))
    val descCsVar = Var(existing.flatMap(_.description).map(_(Language.Cs)).getOrElse(""))

    div(
      cls := "catalog-detail-panel",

      button(cls := "detail-panel-close", "×", onClick --> { _ =>
        CatalogEditorViewModel.setEditState(EditState.None)
      }),

      div(cls := "detail-panel-header",
        h3(if existing.isDefined then "Edit Finish" else "New Finish"),
      ),

      div(cls := "detail-panel-section",
        FormComponents.textField("ID", idVar.signal, idVar.writer, "e.g. matte-lamination"),
        FormComponents.textField("Name (EN)", nameEnVar.signal, nameEnVar.writer),
        FormComponents.textField("Name (CS)", nameCsVar.signal, nameCsVar.writer),

        FormComponents.enumSelectRequired[FinishType](
          "Finish Type", FinishType.values, finishTypeVar.signal, finishTypeVar.writer,
        ),

        FormComponents.enumSelectRequired[FinishSide](
          "Side", FinishSide.values, sideVar.signal, sideVar.writer,
        ),

        h4("Help Description"),
        FormComponents.textAreaField("Description (EN)", descEnVar.signal, descEnVar.writer, "Describe this finish for customers..."),
        FormComponents.textAreaField("Description (CS)", descCsVar.signal, descCsVar.writer, "Popis povrchové úpravy pro zákazníky..."),
      ),

      div(
        cls := "detail-panel-actions",
        FormComponents.actionButton("Save", () => {
          val id = idVar.now()
          if id.nonEmpty && nameEnVar.now().nonEmpty then
            val desc = if descEnVar.now().nonEmpty then Some(LocalizedString(descEnVar.now(), descCsVar.now())) else None
            val fin = Finish(
              id = FinishId.unsafe(id),
              name = LocalizedString(nameEnVar.now(), nameCsVar.now()),
              finishType = finishTypeVar.now(),
              side = sideVar.now(),
              description = desc,
            )
            if existing.isDefined then CatalogEditorViewModel.updateFinish(fin)
            else CatalogEditorViewModel.addFinish(fin)
        }),
        FormComponents.dangerButton("Cancel", () =>
          CatalogEditorViewModel.setEditState(EditState.None)
        ),
      ),
    )
