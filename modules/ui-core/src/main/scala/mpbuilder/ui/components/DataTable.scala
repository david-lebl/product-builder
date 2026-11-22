package mpbuilder.ui.components

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.shared.Language
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Universal responsive Master-Detail DataTable component (Vaadin Master-Detail style)
  * for Storefront & Backoffice views.
  *
  * Features:
  *   - External header toolbar above the Master-Detail surface: wrapping headline/subtitle on
  *     the left (`min-width: 0`) so descriptions wrap instead of pushing Search onto a new line;
  *     Search, extra controls, and Add button cleanly aligned on the right.
  *   - Single unified Master-Detail surface container (not two separate cards) with a draggable
  *     and keyboard-accessible vertical resizable splitter (`mp-dt-splitter`) between Master & Detail.
  *   - Multi-select filters with NO counts:
  *       - `<= multiSelectThreshold` (default 5) options: checkbox toggles
  *       - `> multiSelectThreshold` options: searchable multi-select tag textfield + dropdown checklist
  *   - Responsive Master-Detail:
  *       - Desktop (> 960px): 100% width table when unselected; resizable side-by-side split
  *         with independently scrollable single-column detail pane when a row is selected or creating.
  *       - Mobile (<= 960px): hides table & splitter and opens detail/create pane full-width with Back button.
  *   - Optional `onOpenFullScreen` adds a `⤢ Full View` navigation button in the detail bar.
  *   - Optional `createConfig` opens the new-item form directly inside the detail pane.
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

  /** Configuration for creating a new item inside the DataTable side detail pane. */
  final case class CreateConfig(
      buttonLabel: UI,
      title: UI,
      renderForm: (() => Unit < Async) => UI,
  )

  final case class State[K](
      search: String = "",
      activeFilters: Map[String, Set[String]] = Map.empty,
      filterSearch: Map[String, String] = Map.empty,
      openDropdown: Option[String] = None,
      sort: Option[SortSpec] = None,
      page: Int = 0,
      pageSize: Int = 10,
      selectedKey: Option[K] = None,
      isCreating: Boolean = false,
      splitPct: Int = 52,
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
      title: Option[UI] = None,
      subtitle: Option[UI] = None,
      filterGroups: List[FilterGroup[T]] = Nil,
      renderDetail: Option[T => UI] = None,
      detailTitle: Option[T => String] = None,
      onRowSelect: Option[T => Unit < Async] = None,
      onOpenFullScreen: Option[T => Unit < Async] = None,
      createConfig: Option[CreateConfig] = None,
      headerExtra: UI = UI.empty,
      emptyMessage: String = "No items found",
      multiSelectThreshold: Int = 5,
      lang: Language = Language.En,
  ): UI =
    val isCs = lang == Language.Cs

    state.render { st =>
      // 1. Apply search filter
      val q = st.search.trim.toLowerCase
      val afterSearch =
        if q.isEmpty then rows
        else rows.filter(r => matchesSearch(r, q))

      // 2. Apply active multi-select group filters (OR within group, AND across groups)
      val filtered = filterGroups.foldLeft(afterSearch) { (acc, group) =>
        val selectedIds = st.activeFilters.getOrElse(group.id, Set.empty)
        if selectedIds.isEmpty then acc
        else
          val activeOpts = group.options.filter(o => selectedIds.contains(o.id))
          if activeOpts.isEmpty then acc
          else acc.filter(row => activeOpts.exists(_.predicate(row)))
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
      val colCount   = columns.size.max(1)

      // 5. Detail / Create side pane state
      val selectedRow: Option[T] =
        if st.isCreating then None
        else st.selectedKey.flatMap(k => rows.find(r => rowKey(r) == k))
      val showCreatePane = st.isCreating && createConfig.isDefined
      val showDetailPane = showCreatePane || (selectedRow.isDefined && renderDetail.isDefined)
      val isRowClickable = renderDetail.isDefined || onRowSelect.isDefined

      def toggleFilterOption(groupId: String, optId: String): Unit < Async =
        state.updateAndGet { cur =>
          val curSet  = cur.activeFilters.getOrElse(groupId, Set.empty)
          val nextSet = if curSet.contains(optId) then curSet - optId else curSet + optId
          val nextMap = if nextSet.isEmpty then cur.activeFilters - groupId else cur.activeFilters + (groupId -> nextSet)
          cur.copy(activeFilters = nextMap, page = 0)
        }.unit

      def closeSidePane: Unit < Async =
        state.updateAndGet(_.copy(selectedKey = None, isCreating = false)).unit

      def renderFilterGroup(group: FilterGroup[T]): UI =
        val selectedIds = st.activeFilters.getOrElse(group.id, Set.empty)

        if group.options.size <= multiSelectThreshold then
          // Multi-select checkboxes (no counts)
          div.cssClass("mp-dt-filter-group")(
            span(s"${group.label}:").cssClass("mp-dt-filter-label"),
            UI.fragment(
              group.options.map { opt =>
                val checked = selectedIds.contains(opt.id)
                button
                  .cssClass(if checked then "mp-dt-check mp-dt-check-active" else "mp-dt-check")
                  .onClick(toggleFilterOption(group.id, opt.id))(
                    span(if checked then "✓" else "")
                      .cssClass(if checked then "mp-dt-check-box mp-dt-check-box-checked" else "mp-dt-check-box"),
                    span(opt.label),
                  )
              }*
            ),
          )
        else
          // Multi-select searchable textfield + dropdown checklist (> 5 options)
          val isOpen       = st.openDropdown.contains(group.id)
          val searchTxt    = st.filterSearch.getOrElse(group.id, "")
          val qOpt         = searchTxt.trim.toLowerCase
          val matchingOpts =
            if qOpt.isEmpty then group.options
            else group.options.filter(_.label.toLowerCase.contains(qOpt))
          val selectedOpts = group.options.filter(o => selectedIds.contains(o.id))

          div.cssClass("mp-dt-filter-group")(
            span(s"${group.label}:").cssClass("mp-dt-filter-label"),
            div.cssClass("mp-dt-ms-wrap")(
              div.cssClass("mp-dt-ms-box")(
                UI.fragment(
                  selectedOpts.map { opt =>
                    span.cssClass("mp-dt-ms-tag")(
                      span(opt.label),
                      button
                        .style(
                          Style.bg(Color.transparent)
                            .border(0.px, BorderStyle.none, Color.transparent)
                            .color(brandDark)
                            .padding(0.px, 2.px)
                            .cursor(Cursor.pointer)
                            .fontSize(11.px)
                            .fontWeight(FontWeight.w800),
                        )
                        .onClick(toggleFilterOption(group.id, opt.id))("×"),
                    )
                  }*
                ),
                input
                  .cssClass("mp-dt-ms-input")
                  .value(searchTxt)
                  .placeholder(
                    if selectedOpts.isEmpty then
                      if isCs then s"Filtrovat ${group.label.toLowerCase}..." else s"Filter ${group.label.toLowerCase}..."
                    else if isCs then "Přidat..."
                    else "Add...",
                  )
                  .onInput { v =>
                    state.updateAndGet { cur =>
                      cur.copy(
                        filterSearch = cur.filterSearch + (group.id -> v),
                        openDropdown = Some(group.id),
                      )
                    }.unit
                  },
                button
                  .style(
                    Style.bg(Color.transparent)
                      .border(0.px, BorderStyle.none, Color.transparent)
                      .color(inkMuted)
                      .padding(2.px, 4.px)
                      .cursor(Cursor.pointer)
                      .fontSize(11.px)
                      .fontWeight(FontWeight.w700),
                  )
                  .onClick(
                    state.updateAndGet { cur =>
                      val nextOpen = if cur.openDropdown.contains(group.id) then None else Some(group.id)
                      cur.copy(openDropdown = nextOpen)
                    }.unit,
                  )(if isOpen then "▴" else "▾"),
              ),
              if isOpen then
                div.cssClass("mp-dt-ms-menu")(
                  if matchingOpts.isEmpty then
                    span(if isCs then "Žádné možnosti" else "No matching options").style(smallText.padding(6.px, 8.px))
                  else
                    UI.fragment(
                      matchingOpts.map { opt =>
                        val checked = selectedIds.contains(opt.id)
                        button
                          .cssClass("mp-dt-ms-option")
                          .onClick(toggleFilterOption(group.id, opt.id))(
                            span(if checked then "✓" else "")
                              .cssClass(if checked then "mp-dt-check-box mp-dt-check-box-checked" else "mp-dt-check-box"),
                            span(opt.label),
                          )
                      }*
                    ),
                  div.style(rowBetween.padding(6.px, 4.px, 2.px, 4.px).borderTop(1.px, Palette.border))(
                    if selectedIds.nonEmpty then
                      button
                        .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                        .style(Style.padding(2.px, 8.px).fontSize(11.px))
                        .onClick(
                          state.updateAndGet(cur => cur.copy(activeFilters = cur.activeFilters - group.id, page = 0)).unit,
                        )(if isCs then "Vymazat" else "Clear")
                    else span(""),
                    button
                      .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                      .style(Style.padding(2.px, 10.px).fontSize(11.px))
                      .onClick(
                        state.updateAndGet(cur => cur.copy(openDropdown = None, filterSearch = cur.filterSearch - group.id)).unit,
                      )(if isCs then "Hotovo" else "Done"),
                  ),
                )
              else UI.empty,
            ),
          )

      val headerCells: List[UI] =
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
              td.colspan(colCount)
                .cssClass("mp-dt-td")
                .style(Style.textAlign(TextAlign.center).padding(28.px, 16.px).color(inkMuted))(
                  emptyMessage,
                ),
            )
          )
        else
          pageRows.map { row =>
            val key        = rowKey(row)
            val isSelected = !st.isCreating && renderDetail.isDefined && st.selectedKey.contains(key)
            val rowClass =
              if isSelected then "mp-dt-row mp-dt-row-selected"
              else if isRowClickable then "mp-dt-row"
              else ""

            val handleClick: Unit < Async =
              if renderDetail.isDefined then
                for
                  nextSt <- state.updateAndGet { cur =>
                              if !cur.isCreating && cur.selectedKey.contains(key) then cur.copy(selectedKey = None)
                              else cur.copy(selectedKey = Some(key), isCreating = false)
                            }
                  _ <- (onRowSelect, nextSt.selectedKey) match
                         case (Some(cb), Some(_)) => cb(row)
                         case _                   => Kyo.unit
                yield ()
              else
                onRowSelect match
                  case Some(cb) => cb(row)
                  case None     => Kyo.unit

            val rowCells: List[UI] =
              columns.map { col =>
                val alignStyle =
                  if col.alignRight then Style.textAlign(TextAlign.right)
                  else Style.textAlign(TextAlign.left)
                td.cssClass("mp-dt-td").style(alignStyle)(col.cell(row))
              }

            tr.cssClass(rowClass).onClick(handleClick)(UI.fragment(rowCells*))
          }

      val anyFilterActive = st.activeFilters.values.exists(_.nonEmpty)

      // External Header Bar (cleanly outside the Master-Detail surface)
      // Title/description block has flex: 1 1 0 & min-width: 0 so descriptions wrap cleanly
      // instead of pushing search/actions to a new line.
      val topToolbar: UI = div.cssClass("mp-dt-header-top")(
        title match
          case Some(t) =>
            div.cssClass("mp-dt-title-block")(
              h2(t).style(Style.margin(0.px).fontSize(20.px).fontWeight(FontWeight.w800).color(ink).lineHeight(1.25)),
              subtitle.map(sub => span(sub).style(smallText.lineHeight(1.4))).getOrElse(UI.empty),
            )
          case None => div.cssClass("mp-dt-title-block")(),
        div.cssClass("mp-dt-controls")(
          searchInput
            .cssClass("mp-input")
            .style(Style.width(230.px).minWidth(160.px).maxWidth(300.px).padding(7.px, 11.px).fontSize(13.px))
            .value(st.search)
            .placeholder(searchPlaceholder)
            .onInput(v => state.updateAndGet(_.copy(search = v, page = 0)).unit),
          headerExtra,
          createConfig match
            case Some(cfg) =>
              button
                .cssClass(if showCreatePane then "mp-btn mp-btn-secondary mp-btn-sm" else "mp-btn mp-btn-primary mp-btn-sm")
                .style(Style.whiteSpace(WhiteSpace.noWrap))
                .onClick(
                  state.updateAndGet { cur =>
                    if cur.isCreating then cur.copy(isCreating = false)
                    else cur.copy(isCreating = true, selectedKey = None)
                  }.unit,
                )(cfg.buttonLabel)
            case None => UI.empty,
        ),
      )

      val masterPaneContent: UI = UI.fragment(
        // Integrated Checkbox & Multi-Select Filters Bar at top of master pane
        if filterGroups.nonEmpty then
          div.cssClass("mp-dt-header-filters")(
            UI.fragment(filterGroups.map(renderFilterGroup)*),
            if anyFilterActive then
              button
                .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                .style(Style.padding(3.px, 8.px).fontSize(12.px).color(brand))
                .onClick(
                  state.updateAndGet(_.copy(activeFilters = Map.empty, filterSearch = Map.empty, openDropdown = None, page = 0)).unit,
                )(if isCs then "Zrušit filtry" else "Clear filters")
            else UI.empty,
          )
        else UI.empty,

        // Table Body
        div.cssClass("mp-dt-table-scroll")(
          UI.table.cssClass("mp-dt-table")(
            tbody(
              UI.fragment((headerRow :: dataRows)*),
            ),
          ),
        ),

        // Pagination Footer
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

      // Draggable & Keyboard-Accessible Resizable Splitter between Master and Detail
      val splitter: UI =
        if showDetailPane then
          div
            .cssClass("mp-dt-splitter")
            .role("separator")
            .tabIndex(0)
            .aria("orientation", "vertical")
            .aria("valuenow", st.splitPct.toString)
            .aria("label", if isCs then "Změnit šířku panelů" else "Resize master and detail panes")
            .data("mp-dt-splitter", "true")
            .onKeyDown { ev =>
              ev.key match
                case UI.Keyboard.ArrowLeft                 => state.updateAndGet(c => c.copy(splitPct = (c.splitPct - 5).max(28))).unit
                case UI.Keyboard.ArrowRight                => state.updateAndGet(c => c.copy(splitPct = (c.splitPct + 5).min(72))).unit
                case UI.Keyboard.Home | UI.Keyboard.Enter  => state.updateAndGet(_.copy(splitPct = 50)).unit
                case _                                     => Kyo.unit
            }(
              div.cssClass("mp-dt-splitter-grip")(),
            )
        else UI.empty

      // Integrated Right Detail / Create Pane (scrollable single-column body on desktop; full-screen on mobile)
      val sidePane: UI =
        if showCreatePane then
          createConfig match
            case Some(cfg) =>
              div.cssClass("mp-dt-detail-pane").data("mp-dt-detail", "true")(
                div.cssClass("mp-dt-detail-header")(
                  div.style(rowSm)(
                    button
                      .cssClass("mp-btn mp-btn-secondary mp-btn-sm mp-dt-mobile-back")
                      .onClick(closeSidePane)(if isCs then "← Zpět" else "← Back"),
                    h3(cfg.title).style(Style.margin(0.px).fontSize(15.px).fontWeight(FontWeight.w800).color(ink)),
                  ),
                  button
                    .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                    .style(Style.padding(4.px, 8.px))
                    .onClick(closeSidePane)("✕"),
                ),
                div.cssClass("mp-dt-detail-body")(
                  cfg.renderForm(() => closeSidePane),
                ),
              )
            case None => UI.empty
        else
          (selectedRow, renderDetail) match
            case (Some(row), Some(detailFn)) =>
              val paneHeading = detailTitle.map(_(row)).getOrElse(if isCs then "Detail" else "Detail")
              div.cssClass("mp-dt-detail-pane").data("mp-dt-detail", "true")(
                div.cssClass("mp-dt-detail-header")(
                  div.style(rowSm.minWidth(0.px))(
                    button
                      .cssClass("mp-btn mp-btn-secondary mp-btn-sm mp-dt-mobile-back")
                      .onClick(closeSidePane)(if isCs then "← Zpět" else "← Back"),
                    span(paneHeading).style(Style.fontSize(12.px).fontWeight(FontWeight.w700).color(inkMuted)),
                  ),
                  div.style(rowXs.flexShrink(0.0))(
                    onOpenFullScreen match
                      case Some(openFull) =>
                        button
                          .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                          .style(Style.padding(4.px, 9.px).fontSize(12.px).whiteSpace(WhiteSpace.noWrap))
                          .onClick(openFull(row))(if isCs then "⤢ Celá stránka" else "⤢ Full View")
                      case None => UI.empty,
                    button
                      .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                      .style(Style.padding(4.px, 8.px))
                      .onClick(closeSidePane)("✕"),
                  ),
                ),
                div.cssClass("mp-dt-detail-body")(
                  detailFn(row),
                ),
              )
            case _ => UI.empty

      val safeSplit = st.splitPct.max(28).min(72)

      div.cssClass("mp-dt-root")(
        topToolbar,
        div.cssClass("mp-dt-split").data("mp-dt-split-root", "true")(
          div
            .cssClass(if showDetailPane then "mp-dt-list-pane mp-dt-list-pane-split" else "mp-dt-list-pane")
            .style(if showDetailPane then Style.flexBasis(safeSplit.pct) else Style.empty)
            .data("mp-dt-master", "true")(
              masterPaneContent,
            ),
          splitter,
          sidePane,
        ),
      )
    }
