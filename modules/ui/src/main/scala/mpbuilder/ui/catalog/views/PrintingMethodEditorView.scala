package mpbuilder.ui.catalog.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.catalog.*

/** Editor view for managing Printing Methods in the catalog. */
object PrintingMethodEditorView:

  def apply(): HtmlElement =
    div(
      cls := "catalog-section",
      div(
        cls := "catalog-section-header",
        h3("Printing Methods"),
        FormComponents.actionButton("+ Add Printing Method", () =>
          CatalogEditorViewModel.setEditState(EditState.CreatingPrintingMethod)
        ),
      ),

      div(
        cls := "catalog-entity-list",
        children <-- CatalogEditorViewModel.printingMethods.map { pms =>
          if pms.isEmpty then List(p(cls := "empty-message", "No printing methods defined."))
          else pms.map(pmRow)
        },
      ),

      child <-- CatalogEditorViewModel.editState.combineWith(CatalogEditorViewModel.catalog).map {
        case (EditState.CreatingPrintingMethod, _) => pmForm(None)
        case (EditState.EditingPrintingMethod(id), cat) =>
          cat.printingMethods.get(id).map(pm => pmForm(Some(pm))).getOrElse(emptyNode)
        case _ => emptyNode
      },
    )

  private def pmRow(pm: PrintingMethod): HtmlElement =
    div(
      cls := "catalog-entity-row",
      div(
        cls := "entity-info",
        span(cls := "entity-id", pm.id.value),
        span(cls := "entity-name", pm.name.value),
        span(cls := "entity-detail", s"${pm.processType} · max colors: ${pm.maxColorCount.map(_.toString).getOrElse("∞")}"),
      ),
      div(
        cls := "entity-actions",
        button(cls := "btn btn-sm", "Edit", onClick --> { _ =>
          CatalogEditorViewModel.setEditState(EditState.EditingPrintingMethod(pm.id))
        }),
        button(cls := "btn btn-sm btn-danger", "Remove", onClick --> { _ =>
          CatalogEditorViewModel.removePrintingMethod(pm.id)
        }),
      ),
    )

  private def pmForm(existing: Option[PrintingMethod]): HtmlElement =
    val idVar = Var(existing.map(_.id.value).getOrElse(""))
    val nameEnVar = Var(existing.map(_.name(Language.En)).getOrElse(""))
    val nameCsVar = Var(existing.map(_.name(Language.Cs)).getOrElse(""))
    val processTypeVar = Var(existing.map(_.processType).getOrElse(PrintingProcessType.Digital))
    val maxColorVar = Var(existing.flatMap(_.maxColorCount).map(_.toString).getOrElse(""))

    div(
      cls := "catalog-edit-form",
      h4(if existing.isDefined then "Edit Printing Method" else "New Printing Method"),

      FormComponents.textField("ID", idVar.signal, idVar.writer, "e.g. digital-printing"),
      FormComponents.textField("Name (EN)", nameEnVar.signal, nameEnVar.writer),
      FormComponents.textField("Name (CS)", nameCsVar.signal, nameCsVar.writer),

      FormComponents.enumSelectRequired[PrintingProcessType](
        "Process Type", PrintingProcessType.values, processTypeVar.signal, processTypeVar.writer,
      ),

      FormComponents.numberField("Max Color Count", maxColorVar.signal, maxColorVar.writer),

      div(
        cls := "form-actions",
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
