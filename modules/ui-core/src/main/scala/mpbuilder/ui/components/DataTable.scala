package mpbuilder.ui.components

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.shared.Language
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Universal responsive DataTable component for Storefront & Backoffice views.
  *
  * Includes:
  *   - Live text search input + optional right-side toolbar slot
  *   - Multi-section grouped quick filters with live item counts
  *   - Clickable sortable column headers (string or numeric sort keys)
  *   - Pagination controls (Previous / Next / Page Size)
  *   - Selectable rows with inline expandable detail panel
  */
object DataTable:

  final case class Column[T](
      id: String,
      header: String,
      cell: T => UI,
      sortKey: Option[T => String] = None,
      numericSortKey: Option[T => BigDecimal] = None,
      alignRight: Boolean = false,
  ):
    def isSortable: Boolean = sortKey.isDefined || numericSortKey.isDefined

  final case class FilterOption[T](
      id: String,
      label: String,
      predicate: T => Boolean,
  )

  final case class FilterGroup[T](
      id: String,
      label: String,
      options: List[FilterOption[T]],
  )

  final case class SortSpec(
      columnId: String,
      ascending: Boolean,
  )

  final case class State[K](
      search: String = "",
      activeFilters: Map[String, String] = Map.empty,
      sort: Option[SortSpec] = None,
      page: Int = 0,
      pageSize: Int = 10,
      selectedKey: Option[K] = None,
  )

  def makeState[K](
      defaultSort: Option[SortSpec] = None,
      pageSize: Int = 10,
  ): SignalRef[State[K]] < Sync =
    Signal.initRef(State[K](sort = defaultSort, pageSize = pageSize))

  def render[T, K](
      rows: List[T],
      rowKey: T => K,
      columns: List[Column[T]],
      state: SignalRef[State[K]],
      searchPlaceholder: String,
      matchesSearch: (T, String) => Boolean,
      filterGroups: List[FilterGroup[T]] = Nil,
      renderDetail: Option[T => UI] = None,
      emptyMessage: String = "No items found",
      toolbarExtra: UI = UI.empty,
      lang: Language = Language.En,
  ): UI =
    val isCs = lang == Language.Cs
    val allLabel = if isCs then "Vše" else "All"

    state.render { st =>
      // 1. Apply search filter
      val q = st.search.trim.toLowerCase
      val afterSearch =
        if q.isEmpty then rows
        else rows.filter(r => matchesSearch(r, q))

      // 2. Apply active group filters
      val filtered = filterGroups.foldLeft(afterSearch) { (acc, group) =>
        st.activeFilters.get(group.id) match
          case None => acc
          case Some(optId) =>
            group.options.find(_.id == optId) match
              case None      => acc
              case Some(opt) => acc.filter(opt.predicate)
      }

      // 3. Apply sorting
      val sorted = st.sort match
        case None => filtered
        case Some(SortSpec(colId, asc)) =>
          columns.find(_.id == colId) match
            case Some(col) if col.numericSortKey.isDefined =>
              val keyFn = col.numericSortKey.get
              val base  = filtered.sortBy(keyFn)
              if asc then base else base.reverse
            case Some(col) if col.sortKey.isDefined =>
              val keyFn = col.sortKey.get
              val base  = filtered.sortBy(r => keyFn(r).toLowerCase)
              if asc then base else base.reverse
            case _ => filtered

      // 4. Pagination
      val pageSize   = st.pageSize.max(1)
      val total      = sorted.size
      val totalPages = ((total + pageSize - 1) / pageSize).max(1)
      val safePage   = st.page.max(0).min(totalPages - 1)
      val startIdx   = safePage * pageSize
      val pageRows   = sorted.slice(startIdx, startIdx + pageSize)
      val hasDetail  = renderDetail.isDefined
      val colCount   = columns.size + (if hasDetail then 1 else 0)

      // Helper to compute count for a filter option given other active groups + search
      def countForOption(groupId: String, pred: T => Boolean): Int =
        val otherFiltered = filterGroups.filterNot(_.id == groupId).foldLeft(afterSearch) { (acc, grp) =>
          st.activeFilters.get(grp.id) match
            case None => acc
            case Some(optId) =>
              grp.options.find(_.id == optId).fold(acc)(o => acc.filter(o.predicate))
        }
        otherFiltered.count(pred)

      val headerCells: List[UI] =
        (if hasDetail then List(th.cssClass("mp-dt-th").style(Style.width(34.px).padding(10.px, 8.px))("")) else Nil) ++
          columns.map { col =>
            val activeSort = st.sort.filter(_.columnId == col.id)
            val indicator = activeSort match
              case Some(SortSpec(_, true))  => " ↑"
              case Some(SortSpec(_, false)) => " ↓"
              case None if col.isSortable   => " ↕"
              case _                        => ""
            val alignStyle =
              if col.alignRight then Style.textAlign(TextAlign.right)
              else Style.textAlign(TextAlign.left)

            th.cssClass("mp-dt-th").style(alignStyle)(
              if col.isSortable then
                button
                  .cssClass("mp-dt-th-btn")
                  .style(
                    if col.alignRight then Style.justify(Justification.end)
                    else Style.justify(Justification.start),
                  )
                  .onClick(
                    state.updateAndGet { cur =>
                      val nextSort = cur.sort match
                        case Some(SortSpec(id, true)) if id == col.id  => Some(SortSpec(col.id, ascending = false))
                        case Some(SortSpec(id, false)) if id == col.id => None
                        case _                                         => Some(SortSpec(col.id, ascending = true))
                      cur.copy(sort = nextSort, page = 0)
                    }.unit,
                  )(s"${col.header}$indicator")
              else span(col.header),
            )
          }

      val headerRow: UI = tr(UI.fragment(headerCells*))

      val dataRows: List[UI] =
        if pageRows.isEmpty then
          List(
            tr(
              td.colspan(colCount).cssClass("mp-dt-td").style(Style.textAlign(TextAlign.center).padding(28.px, 16.px).color(inkMuted))(
                emptyMessage,
              ),
            )
          )
        else
          pageRows.flatMap { row =>
            val key        = rowKey(row)
            val isSelected = hasDetail && st.selectedKey.contains(key)
            val rowClass =
              if isSelected then "mp-dt-row mp-dt-row-selected"
              else if hasDetail then "mp-dt-row"
              else ""

            val toggleClick: Unit < Async =
              if hasDetail then
                state.updateAndGet { cur =>
                  val nextSel = if cur.selectedKey.contains(key) then None else Some(key)
                  cur.copy(selectedKey = nextSel)
                }.unit
              else Kyo.unit

            val rowCells: List[UI] =
              (if hasDetail then
                 List(
                   td.cssClass("mp-dt-td").style(
                     Style.width(34.px).textAlign(TextAlign.center).color(if isSelected then brand else inkMuted).fontWeight(FontWeight.w700),
                   )(
                     if isSelected then "▾" else "▸",
                   )
                 )
               else Nil) ++
                columns.map { col =>
                  val alignStyle =
                    if col.alignRight then Style.textAlign(TextAlign.right)
                    else Style.textAlign(TextAlign.left)
                  td.cssClass("mp-dt-td").style(alignStyle)(col.cell(row))
                }

            val mainTr: UI = tr
              .cssClass(rowClass)
              .onClick(toggleClick)(UI.fragment(rowCells*))

            val detailTrOpt: List[UI] =
              if isSelected then
                renderDetail.toList.map { detailFn =>
                  tr(
                    td.colspan(colCount).cssClass("mp-dt-detail-cell")(
                      detailFn(row),
                    )
                  )
                }
              else Nil

            mainTr :: detailTrOpt
          }

      div.cssClass("mp-dt-wrap")(
        // Toolbar: Search + Extra + Count
        div.cssClass("mp-dt-toolbar")(
          searchInput
            .cssClass("mp-input")
            .style(Style.maxWidth(360.px).flexGrow(1.0))
            .value(st.search)
            .placeholder(searchPlaceholder)
            .onInput(v => state.updateAndGet(_.copy(search = v, page = 0)).unit),
          div.style(Style.row.align(Alignment.center).gap(10.px).flexWrap(FlexWrap.wrap))(
            span(
              if isCs then s"Záznamů: $total" else s"$total records",
            ).style(smallText.fontWeight(FontWeight.w700)),
            toolbarExtra,
          ),
        ),

        // Grouped quick filter sections
        if filterGroups.nonEmpty then
          div.style(Style.column.gap(8.px))(
            filterGroups.map { group =>
              val activeOpt = st.activeFilters.get(group.id)
              val allCount  = countForOption(group.id, _ => true)
              div.cssClass("mp-dt-filter-group")(
                span(s"${group.label}:").cssClass("mp-dt-filter-label"),
                button
                  .cssClass(if activeOpt.isEmpty then "mp-pill-active" else "mp-pill")
                  .style(Style.padding(4.px, 11.px).fontSize(12.px))
                  .onClick(
                    state.updateAndGet(cur => cur.copy(activeFilters = cur.activeFilters - group.id, page = 0)).unit,
                  )(s"$allLabel ($allCount)"),
                UI.fragment(
                  group.options.map { opt =>
                    val cnt = countForOption(group.id, opt.predicate)
                    button
                      .cssClass(if activeOpt.contains(opt.id) then "mp-pill-active" else "mp-pill")
                      .style(Style.padding(4.px, 11.px).fontSize(12.px))
                      .onClick(
                        state.updateAndGet(cur => cur.copy(activeFilters = cur.activeFilters + (group.id -> opt.id), page = 0)).unit,
                      )(s"${opt.label} ($cnt)")
                  }*
                ),
              )
            }*
          )
        else UI.empty,

        // Table
        div.cssClass("mp-dt-table-scroll")(
          UI.table.cssClass("mp-dt-table")(
            tbody(
              UI.fragment((headerRow :: dataRows)*),
            ),
          ),
        ),

        // Pagination footer
        if total > 0 then
          val fromNum = startIdx + 1
          val toNum   = (startIdx + pageRows.size).min(total)
          div.cssClass("mp-dt-pager")(
            span(
              if isCs then s"Zobrazeno $fromNum–$toNum z $total"
              else s"Showing $fromNum–$toNum of $total",
            ).style(smallText),
            div.style(Style.row.align(Alignment.center).gap(8.px).flexWrap(FlexWrap.wrap))(
              div.style(Style.row.align(Alignment.center).gap(4.px))(
                span(if isCs then "Na stránku:" else "Per page:").style(smallText),
                UI.fragment(
                  List(10, 25, 50).map { ps =>
                    button
                      .cssClass(if pageSize == ps then "mp-btn mp-btn-primary mp-btn-sm" else "mp-btn mp-btn-ghost mp-btn-sm")
                      .style(Style.padding(3.px, 8.px).fontSize(12.px))
                      .onClick(state.updateAndGet(_.copy(pageSize = ps, page = 0)).unit)(ps.toString)
                  }*
                ),
              ),
              if totalPages > 1 then
                div.style(Style.row.align(Alignment.center).gap(6.px))(
                  button
                    .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                    .disabled(safePage <= 0)
                    .onClick(state.updateAndGet(cur => cur.copy(page = (cur.page - 1).max(0))).unit)(
                      if isCs then "← Předchozí" else "← Prev",
                    ),
                  span(s"${safePage + 1} / $totalPages").style(smallText.fontWeight(FontWeight.w700)),
                  button
                    .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                    .disabled(safePage >= totalPages - 1)
                    .onClick(state.updateAndGet(cur => cur.copy(page = (cur.page + 1).min(totalPages - 1))).unit)(
                      if isCs then "Další →" else "Next →",
                    ),
                )
              else UI.empty,
            ),
          )
        else UI.empty,
      )
    }
