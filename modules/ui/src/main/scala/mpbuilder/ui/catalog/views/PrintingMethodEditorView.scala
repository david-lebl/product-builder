package mpbuilder.ui.catalog.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.catalog.*
import mpbuilder.uikit.containers.*

/** Editor view for managing Printing Methods in the catalog using SplitTableView. */
object PrintingMethodEditorView:

  def apply(): HtmlElement =
    val searchVar = Var("")
    val selectedId: Var[Option[String]] = Var(None)

    val filteredMethods: Signal[List[PrintingMethod]] =
      CatalogEditorViewModel.printingMethods.combineWith(searchVar.signal).map { case (pms, query) =>
        val q = query.trim.toLowerCase
        if q.isEmpty then pms
        else pms.filter { pm =>
          pm.id.value.toLowerCase.contains(q) ||
          pm.name.value.toLowerCase.contains(q) ||
          pm.processType.toString.toLowerCase.contains(q)
        }
      }

    val tableConfig = SplitTableConfig[PrintingMethod](
      columns = List(
        ColumnDef("ID", pm => span(cls := "entity-id", pm.id.value), Some(_.id.value), Some("160px")),
        ColumnDef("Name", pm => span(pm.name.value), Some(_.name.value)),
        ColumnDef("Process", pm => span(pm.processType.toString), Some(_.processType.toString), Some("100px")),
        ColumnDef("Max Colors", pm => span(pm.maxColorCount.map(_.toString).getOrElse("∞")), width = Some("100px")),
        ColumnDef("", pm => div(
          cls := "entity-actions",
          button(cls := "btn btn-sm btn-danger", "✕", onClick.stopPropagation --> { _ =>
            CatalogEditorViewModel.removePrintingMethod(pm.id)
          }),
        ), width = Some("50px")),
      ),
      rowKey = _.id.value,
      searchPlaceholder = "Search printing methods…",
      onRowSelect = Some(pm => {
        selectedId.set(Some(pm.id.value))
        CatalogEditorViewModel.setEditState(EditState.EditingPrintingMethod(pm.id))
      }),
      emptyMessage = "No printing methods defined.",
    )

    val sidePanel: Signal[Option[HtmlElement]] =
      CatalogEditorViewModel.editState.combineWith(CatalogEditorViewModel.catalog).map {
        case (EditState.CreatingPrintingMethod, _) =>
          Some(pmForm(None))
        case (EditState.EditingPrintingMethod(id), cat) =>
          cat.printingMethods.get(id).map(pm => pmForm(Some(pm)))
        case _ => None
      }

    div(
      cls := "catalog-section",
      h2(cls := "manufacturing-view-title", "Printing Methods"),
      SplitTableView(
        config = tableConfig,
        items = filteredMethods,
        selectedKey = selectedId.signal,
        searchQuery = searchVar,
        sidePanel = sidePanel,
        headerActions = Some(
          FormComponents.actionButton("+ Add Printing Method", () => {
            selectedId.set(None)
            CatalogEditorViewModel.setEditState(EditState.CreatingPrintingMethod)
          })
        ),
      ),
    )

  private def pmForm(existing: Option[PrintingMethod]): HtmlElement =
    val idVar = Var(existing.map(_.id.value).getOrElse(""))
    val nameEnVar = Var(existing.map(_.name(Language.En)).getOrElse(""))
    val nameCsVar = Var(existing.map(_.name(Language.Cs)).getOrElse(""))
    val processTypeVar = Var(existing.map(_.processType).getOrElse(PrintingProcessType.Digital))
    val maxColorVar = Var(existing.flatMap(_.maxColorCount).map(_.toString).getOrElse(""))

    div(
      cls := "catalog-detail-panel",

      button(cls := "detail-panel-close", "×", onClick --> { _ =>
        CatalogEditorViewModel.setEditState(EditState.None)
      }),

      div(cls := "detail-panel-header",
        h3(if existing.isDefined then "Edit Printing Method" else "New Printing Method"),
      ),

      div(cls := "detail-panel-section",
        FormComponents.textField("ID", idVar.signal, idVar.writer, "e.g. digital-printing"),
        FormComponents.textField("Name (EN)", nameEnVar.signal, nameEnVar.writer),
        FormComponents.textField("Name (CS)", nameCsVar.signal, nameCsVar.writer),

        FormComponents.enumSelectRequired[PrintingProcessType](
          "Process Type", PrintingProcessType.values, processTypeVar.signal, processTypeVar.writer,
        ),

        FormComponents.numberField("Max Color Count", maxColorVar.signal, maxColorVar.writer),
      ),

      div(
        cls := "detail-panel-actions",
        FormComponents.actionButton("Save", () => {
          val id = idVar.now()
          if id.nonEmpty && nameEnVar.now().nonEmpty then
            val pm = PrintingMethod(
              id = PrintingMethodId.unsafe(id),
              name = LocalizedString(nameEnVar.now(), nameCsVar.now()),
              processType = processTypeVar.now(),
              maxColorCount = maxColorVar.now().toIntOption,
            )
            if existing.isDefined then CatalogEditorViewModel.updatePrintingMethod(pm)
            else CatalogEditorViewModel.addPrintingMethod(pm)
        }),
        FormComponents.dangerButton("Cancel", () =>
          CatalogEditorViewModel.setEditState(EditState.None)
        ),
      ),
    )
