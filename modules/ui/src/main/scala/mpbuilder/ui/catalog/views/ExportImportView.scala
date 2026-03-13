package mpbuilder.ui.catalog.views

import com.raquo.laminar.api.L.*
import mpbuilder.ui.catalog.*

/** Export/Import view for catalog data as JSON.
  *
  * Allows exporting the current catalog, ruleset, and pricelists to JSON,
  * and importing from a JSON string.
  */
object ExportImportView:

  def apply(): HtmlElement =
    val jsonVar = Var("")

    div(
      cls := "catalog-section",
      h3("Export / Import"),

      div(
        cls := "export-import-actions",
        FormComponents.actionButton("Export to JSON", () => {
          val json = CatalogEditorViewModel.exportJson()
          jsonVar.set(json)
        }),
        FormComponents.actionButton("Import from JSON", () => {
          val json = jsonVar.now()
          if json.trim.nonEmpty then
            CatalogEditorViewModel.importJson(json)
        }),
        FormComponents.actionButton("Load Sample Data", () => {
          CatalogEditorViewModel.loadSampleData()
          val json = CatalogEditorViewModel.exportJson()
          jsonVar.set(json)
        }),
      ),

      div(
        cls := "form-group",
        com.raquo.laminar.api.L.label("JSON Data"),
        textArea(
          cls := "json-textarea",
          rows := 20,
          placeholder := "Paste JSON here to import, or click Export to generate JSON",
          controlled(
            value <-- jsonVar.signal,
            onInput.mapToValue --> jsonVar.writer,
          ),
        ),
      ),

      // Stats
      child <-- CatalogEditorViewModel.state.map { s =>
        div(
          cls := "export-stats",
          h4("Current Catalog Stats"),
          ul(
            li(s"Categories: ${s.catalog.categories.size}"),
            li(s"Materials: ${s.catalog.materials.size}"),
            li(s"Finishes: ${s.catalog.finishes.size}"),
            li(s"Printing Methods: ${s.catalog.printingMethods.size}"),
            li(s"Compatibility Rules: ${s.ruleset.rules.size}"),
            li(s"Pricelists: ${s.pricelists.size}"),
            li(s"Total Pricing Rules: ${s.pricelists.map(_.rules.size).sum}"),
          ),
        )
      },
    )
