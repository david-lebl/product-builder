package mpbuilder.uikit.containers

import com.raquo.laminar.api.L.*

/** Column definition for the split table view. */
case class ColumnDef[T](
    header: String,
    accessor: T => HtmlElement,
    sortKey: Option[T => String] = None,
    width: Option[String] = None,
)

/** Filter definition for the filter bar. */
case class FilterDef(
    id: String,
    label: String,
    options: Signal[List[(String, String)]],
    selectedValues: Var[Set[String]],
    multiSelect: Boolean = true,
)

/** Configuration for the SplitTableView component. */
case class SplitTableConfig[T](
    columns: List[ColumnDef[T]],
    rowKey: T => String,
    filters: List[FilterDef] = Nil,
    searchPlaceholder: String = "Search…",
    onRowSelect: Option[T => Unit] = None,
    emptyMessage: String = "No items to display",
)

/** Domain-agnostic sortable table with search, filter bar, and resizable side panel.
  *
  * Used by all manufacturing views (Station Queue, Order Approval, Order Progress, etc.).
  */
object SplitTableView:

  def apply[T](
      config: SplitTableConfig[T],
      items: Signal[List[T]],
      selectedKey: Signal[Option[String]],
      searchQuery: Var[String],
      sidePanel: Signal[Option[HtmlElement]],
      headerActions: Option[HtmlElement] = None,
  ): HtmlElement =
    val sortColumn = Var(Option.empty[Int])
    val sortAsc = Var(true)

    // Filtered items based on search query and active filters
    // Items are expected to be pre-filtered by the caller.
    // Search query is exposed as a Var for the caller to react to.
    val filteredItems: Signal[List[T]] = items

    // Sorted items — combine all sort state into one derived signal
    val sortedItems: Signal[List[T]] =
      val combined = filteredItems.combineWith(sortColumn.signal, sortAsc.signal)
      combined.map { case (items, colIdx, asc) =>
        val sortKeyOpt: Option[T => String] = colIdx.flatMap { idx =>
          if idx < config.columns.size then config.columns(idx).sortKey else None
        }
        sortKeyOpt match
          case Some(key) =>
            val sorted = items.sortBy(key)
            if asc then sorted else sorted.reverse
          case None => items
      }

    div(
      cls := "split-table-view",

      // Header bar with search, filters, and optional actions
      div(
        cls := "split-table-header",
        div(
          cls := "split-table-search",
          input(
            cls := "split-table-search-input",
            typ := "text",
            placeholder := config.searchPlaceholder,
            controlled(
              value <-- searchQuery.signal,
              onInput.mapToValue --> searchQuery.writer,
            ),
          ),
        ),
        headerActions.map(el => div(cls := "split-table-actions", el)).getOrElse(emptyNode),
      ),

      // Filter bar
      if config.filters.nonEmpty then
        div(
          cls := "split-table-filters",
          config.filters.map { filter =>
            div(
              cls := "split-table-filter",
              span(cls := "split-table-filter-label", filter.label + ":"),
              children <-- filter.options.combineWith(filter.selectedValues.signal).map {
                case (opts, selected) =>
                  opts.map { case (value, label) =>
                    button(
                      cls := (if selected.contains(value) then "filter-chip filter-chip--active"
                              else "filter-chip"),
                      label,
                      onClick --> { _ =>
                        filter.selectedValues.update { current =>
                          if filter.multiSelect then
                            if current.contains(value) then current - value
                            else current + value
                          else
                            if current.contains(value) then Set.empty
                            else Set(value)
                        }
                      },
                    )
                  }
              },
            )
          },
        )
      else emptyNode,

      // Main content: table + side panel
      div(
        cls <-- sidePanel.map(sp =>
          if sp.isDefined then "split-table-content split-table-content--with-panel"
          else "split-table-content"
        ),

        // Table
        div(
          cls := "split-table-table-wrapper",
          table(
            cls := "split-table",
            thead(
              tr(
                config.columns.zipWithIndex.map { case (col, idx) =>
                  th(
                    cls := (if col.sortKey.isDefined then "split-table-th split-table-th--sortable"
                            else "split-table-th"),
                    col.width.map(w => width := w).getOrElse(emptyMod),
                    span(col.header),
                    col.sortKey.map { _ =>
                      span(
                        cls := "split-table-sort-icon",
                        child.text <-- sortColumn.signal.combineWith(sortAsc.signal).map {
                          case (Some(i), asc) if i == idx =>
                            if asc then " ▲" else " ▼"
                          case _ => " ⇅"
                        },
                      )
                    }.getOrElse(emptyNode),
                    col.sortKey.map { _ =>
                      onClick --> { _ =>
                        if sortColumn.now().contains(idx) then sortAsc.update(!_)
                        else
                          sortColumn.set(Some(idx))
                          sortAsc.set(true)
                      }
                    }.getOrElse(emptyMod),
                  )
                },
              ),
            ),
            tbody(
              children <-- sortedItems.combineWith(selectedKey).map { case (rows, selKey) =>
                if rows.isEmpty then
                  List(
                    tr(
                      td(
                        colSpan := config.columns.size,
                        cls := "split-table-empty",
                        config.emptyMessage,
                      ),
                    ),
                  )
                else
                  rows.map { item =>
                    val key = config.rowKey(item)
                    tr(
                      cls := (if selKey.contains(key) then "split-table-row split-table-row--selected"
                              else "split-table-row"),
                      config.onRowSelect.map { handler =>
                        onClick --> { _ => handler(item) }
                      }.getOrElse(emptyMod),
                      config.columns.map { col =>
                        td(cls := "split-table-td", col.accessor(item))
                      },
                    )
                  }
              },
            ),
          ),
        ),

        // Side panel
        child <-- sidePanel.map {
          case Some(panel) =>
            div(
              cls := "split-table-side-panel",
              panel,
            )
          case None => emptyNode
        },
      ),
    )
