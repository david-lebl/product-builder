package mpbuilder.uikit.containers

import com.raquo.laminar.api.L.*

case class ColumnDef[A](
    id: String,
    header: Signal[String],
    render: A => HtmlElement,
    sortKey: Option[A => String] = None,
    widthCls: String = "",
)

case class RowAction[A](
    label: Signal[String],
    onClick: A => Unit,
    isDestructive: Boolean = false,
)

object SplitTableView:

  def apply[A](
      rows: Signal[List[A]],
      columns: List[ColumnDef[A]],
      rowKey: A => String,
      searchable: Signal[String] => Signal[List[A]],
      filterBar: Option[HtmlElement] = None,
      detailPanel: Signal[Option[A]] => HtmlElement,
      rowActions: A => List[RowAction[A]],
      emptyMessage: Signal[String] = Val("No items"),
  ): HtmlElement =
    val searchVar: Var[String]              = Var("")
    val selectedKeyVar: Var[Option[String]] = Var(None)
    val sortVar: Var[(String, Boolean)]     = Var(("", true)) // (columnId, ascending)

    val filteredRows: Signal[List[A]] = searchable(searchVar.signal)

    val sortedRows: Signal[List[A]] = filteredRows.combineWith(sortVar.signal).map {
      (items: List[A], colId: String, asc: Boolean) =>
        columns.find(_.id == colId).flatMap(_.sortKey) match
          case None => items
          case Some(key) =>
            val sorted = items.sortBy(key)
            if asc then sorted else sorted.reverse
    }

    val selectedRow: Signal[Option[A]] =
      sortedRows.combineWith(selectedKeyVar.signal).map {
        (items: List[A], keyOpt: Option[String]) =>
          keyOpt.flatMap(k => items.find(r => rowKey(r) == k))
      }

    val panelOpen: Signal[Boolean] = selectedKeyVar.signal.map(_.isDefined)

    div(
      cls := "split-table-view",

      // Toolbar: search + custom filter bar
      div(
        cls := "split-table-toolbar",
        input(
          cls         := "split-table-search",
          typ         := "text",
          placeholder := "Search…",
          controlled(
            value    <-- searchVar.signal,
            onInput.mapToValue --> searchVar.writer,
          ),
        ),
        filterBar.getOrElse(emptyNode),
      ),

      // Body: table + side panel
      div(
        cls := "split-table-body",

        // Table
        div(
          cls <-- panelOpen.map(open =>
            if open then "split-table-table split-table-table--narrow" else "split-table-table"
          ),
          table(
            cls := "data-table",

            // Header
            thead(
              tr(
                columns.map { col =>
                  th(
                    cls := s"data-table-th ${col.widthCls}",
                    child.text <-- col.header,
                    col.sortKey.map { _ =>
                      onClick --> { _ =>
                        val (currentCol, currentAsc) = sortVar.now()
                        if currentCol == col.id then sortVar.set((col.id, !currentAsc))
                        else sortVar.set((col.id, true))
                      }
                    }.getOrElse(emptyMod),
                    child <-- sortVar.signal.map { case (sortCol, asc) =>
                      if sortCol == col.id then
                        span(cls := "sort-indicator", if asc then " ▲" else " ▼")
                      else emptyNode
                    },
                    cursor <-- Val(if col.sortKey.isDefined then "pointer" else "default"),
                  )
                },
                // Actions column header
                th(cls := "data-table-th data-table-th--actions", "Actions"),
              ),
            ),

            // Body
            tbody(
              children <-- sortedRows.combineWith(selectedKeyVar.signal).map {
                (items: List[A], selectedKey: Option[String]) =>
                  if items.isEmpty then
                    List(
                      tr(
                        td(
                          colSpan := (columns.size + 1),
                          cls     := "data-table-empty",
                          child.text <-- emptyMessage,
                        ),
                      )
                    )
                  else
                    items.map { row =>
                      val key = rowKey(row)
                      tr(
                        cls := (if selectedKey.contains(key) then "data-table-row data-table-row--selected"
                                else "data-table-row"),
                        onClick --> { _ => selectedKeyVar.set(Some(key)) },

                        columns.map { col =>
                          td(cls := s"data-table-td ${col.widthCls}", col.render(row))
                        },

                        // Actions cell
                        td(
                          cls     := "data-table-td data-table-td--actions",
                          onClick --> { e => e.stopPropagation() },
                          rowActions(row).map { action =>
                            button(
                              cls <-- action.label.map(l =>
                                if action.isDestructive then "row-action row-action--destructive"
                                else "row-action"
                              ),
                              child.text <-- action.label,
                              onClick --> { _ => action.onClick(row) },
                            )
                          },
                        ),
                      )
                    }
              },
            ),
          ),
        ),

        // Side panel
        div(
          cls <-- panelOpen.map(open =>
            if open then "split-table-panel split-table-panel--open" else "split-table-panel"
          ),
          div(
            cls := "split-table-panel-header",
            button(
              cls     := "split-table-panel-close",
              "×",
              onClick --> { _ => selectedKeyVar.set(None) },
            ),
          ),
          div(
            cls := "split-table-panel-body",
            child <-- selectedRow.map {
              case Some(_) => detailPanel(selectedRow)
              case None    => div()
            },
          ),
        ),
      ),
    )
