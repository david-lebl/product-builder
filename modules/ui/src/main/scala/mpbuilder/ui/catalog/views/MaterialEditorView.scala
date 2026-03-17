package mpbuilder.ui.catalog.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.ui.catalog.*
import mpbuilder.uikit.containers.*

/** Editor view for managing Materials in the catalog using SplitTableView. */
object MaterialEditorView:

  def apply(): HtmlElement =
    val searchVar = Var("")
    val selectedId: Var[Option[String]] = Var(None)

    val filteredMaterials: Signal[List[Material]] =
      CatalogEditorViewModel.materials.combineWith(searchVar.signal).map { case (mats, query) =>
        val q = query.trim.toLowerCase
        if q.isEmpty then mats
        else mats.filter { m =>
          m.id.value.toLowerCase.contains(q) ||
          m.name.value.toLowerCase.contains(q) ||
          m.family.toString.toLowerCase.contains(q)
        }
      }

    val tableConfig = SplitTableConfig[Material](
      columns = List(
        ColumnDef("ID", m => span(cls := "entity-id", m.id.value), Some(_.id.value), Some("140px")),
        ColumnDef("Name", m => span(m.name.value), Some(_.name.value)),
        ColumnDef("Family", m => span(m.family.toString), Some(_.family.toString), Some("100px")),
        ColumnDef("Weight", m => span(m.weight.map(w => s"${w.gsm}gsm").getOrElse("—")), width = Some("80px")),
        ColumnDef("Properties", m => span(cls := "entity-tags", m.properties.map(_.toString).mkString(", "))),
        ColumnDef("", m => div(
          cls := "entity-actions",
          button(cls := "btn btn-sm btn-danger", "✕", onClick.stopPropagation --> { _ =>
            CatalogEditorViewModel.removeMaterial(m.id)
          }),
        ), width = Some("50px")),
      ),
      rowKey = _.id.value,
      searchPlaceholder = "Search materials…",
      onRowSelect = Some(m => {
        selectedId.set(Some(m.id.value))
        CatalogEditorViewModel.setEditState(EditState.EditingMaterial(m.id))
      }),
      emptyMessage = "No materials defined. Add one or load sample data.",
    )

    val sidePanel: Signal[Option[HtmlElement]] =
      CatalogEditorViewModel.editState.combineWith(CatalogEditorViewModel.catalog).map {
        case (EditState.CreatingMaterial, _) =>
          Some(materialForm(None))
        case (EditState.EditingMaterial(id), cat) =>
          cat.materials.get(id).map(m => materialForm(Some(m)))
        case _ => None
      }

    div(
      cls := "catalog-section",
      h2(cls := "manufacturing-view-title", "Materials"),
      SplitTableView(
        config = tableConfig,
        items = filteredMaterials,
        selectedKey = selectedId.signal,
        searchQuery = searchVar,
        sidePanel = sidePanel,
        headerActions = Some(
          FormComponents.actionButton("+ Add Material", () => {
            selectedId.set(None)
            CatalogEditorViewModel.setEditState(EditState.CreatingMaterial)
          })
        ),
      ),
    )

  private def materialForm(existing: Option[Material]): HtmlElement =
    val idVar = Var(existing.map(_.id.value).getOrElse(""))
    val nameEnVar = Var(existing.map(_.name(Language.En)).getOrElse(""))
    val nameCsVar = Var(existing.map(_.name(Language.Cs)).getOrElse(""))
    val familyVar = Var(existing.map(_.family).getOrElse(MaterialFamily.Paper))
    val weightVar = Var(existing.flatMap(_.weight).map(_.gsm.toString).getOrElse(""))
    val propsVar = Var(existing.map(_.properties).getOrElse(Set.empty[MaterialProperty]))
    val descEnVar = Var(existing.flatMap(_.description).map(_(Language.En)).getOrElse(""))
    val descCsVar = Var(existing.flatMap(_.description).map(_(Language.Cs)).getOrElse(""))

    div(
      cls := "catalog-detail-panel",

      button(cls := "detail-panel-close", "×", onClick --> { _ =>
        CatalogEditorViewModel.setEditState(EditState.None)
      }),

      div(cls := "detail-panel-header",
        h3(if existing.isDefined then "Edit Material" else "New Material"),
      ),

      div(cls := "detail-panel-section",
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

        h4("Help Description"),
        FormComponents.textAreaField("Description (EN)", descEnVar.signal, descEnVar.writer, "Describe this material for customers..."),
        FormComponents.textAreaField("Description (CS)", descCsVar.signal, descCsVar.writer, "Popis materiálu pro zákazníky..."),
      ),

      div(
        cls := "detail-panel-actions",
        FormComponents.actionButton("Save", () => {
          val id = idVar.now()
          if id.nonEmpty && nameEnVar.now().nonEmpty then
            val desc = if descEnVar.now().nonEmpty then Some(LocalizedString(descEnVar.now(), descCsVar.now())) else None
            val mat = Material(
              id = MaterialId.unsafe(id),
              name = LocalizedString(nameEnVar.now(), nameCsVar.now()),
              family = familyVar.now(),
              weight = weightVar.now().toIntOption.map(PaperWeight.unsafe),
              properties = propsVar.now(),
              description = desc,
            )
            if existing.isDefined then CatalogEditorViewModel.updateMaterial(mat)
            else CatalogEditorViewModel.addMaterial(mat)
        }),
        FormComponents.dangerButton("Cancel", () =>
          CatalogEditorViewModel.setEditState(EditState.None)
        ),
      ),
    )
