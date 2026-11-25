package mpbuilder.ui.components

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Rich, searchable single-select combo box (Vaadin-style).
  *
  * Closed, it shows the selected item as a rich trigger (leading visual, label, meta line, trailing badge).
  * Open, it shows a search input plus a grouped listbox of rich options with match highlighting, keyboard
  * navigation (↑ / ↓ / Home / End / Enter / Esc / Tab) and a result counter. Filtering is supplied by the
  * caller (`search`), so domain-specific matching (e.g. paper weight ranges) stays outside the component.
  *
  * The trigger is rendered inside a reactive region keyed on [[Handle.open]] only, and the result list in a
  * nested region keyed on the query + active index, so typing never re-creates (and never blurs) the input.
  */
object ComboBox:

  final case class Item[K](
      key: K,
      label: String,
      group: Option[String] = None,
      leading: Option[UI] = None,
      meta: Option[UI] = None,
      trailing: Option[UI] = None,
  )

  /** Localized copy. Plain signals so `kyo-i18n` bundles can drive them reactively. */
  final case class Labels(
      searchPlaceholder: Signal[String],
      emptyTrigger: Signal[String],
      noResults: Signal[String],
      clearSearch: Signal[String],
      keyboardHint: Signal[String],
      resultCount: (Int, Int) => Signal[String],
  )

  /** Per-instance open / query / active-index state (lives in the owning controller so it survives re-renders). */
  final class Handle(
      val open: SignalRef[Boolean],
      val query: SignalRef[String],
      val active: SignalRef[Int],
  ):
    def openAt(index: Int): Unit < Async =
      query.set("").andThen(active.set(index.max(0))).andThen(open.set(true))

    def close: Unit < Async =
      open.set(false).andThen(query.set(""))

  object Handle:
    def make: Handle < Sync =
      for
        open   <- Signal.initRef(false)
        query  <- Signal.initRef("")
        active <- Signal.initRef(0)
      yield new Handle(open, query, active)

  // ── Text folding & highlighting ────────────────────────────────────────────

  private val diacritics: Map[Char, Char] =
    List(
      "aáàâäãåā", "cčćç", "dď", "eéèêëěē", "iíìîïī", "lľĺł", "nňńñ", "oóòôöõő",
      "rřŕ", "sšś", "tť", "uúùûüůűū", "yýÿ", "zžźż", "2²", "3³",
    ).flatMap(group => group.tail.map(_ -> group.head)).toMap

  /** Lower-cases and strips diacritics **char-for-char**, so indices into the folded string map back 1:1. */
  def fold(s: String): String =
    s.map { ch =>
      val lc = Character.toLowerCase(ch)
      diacritics.getOrElse(lc, lc)
    }

  /** Splits `text` into (segment, isMatch) runs for every (diacritic/case-insensitive) occurrence of any term. */
  def highlightSegments(text: String, terms: List[String]): List[(String, Boolean)] =
    val folded = fold(text)
    val needles = terms.map(fold).filter(_.nonEmpty).distinct
    val marks   = Array.fill(text.length)(false)
    needles.foreach { n =>
      var from = folded.indexOf(n)
      while from >= 0 do
        (from until (from + n.length)).foreach(marks(_) = true)
        from = folded.indexOf(n, from + 1)
    }
    if text.isEmpty then Nil
    else
      val out   = List.newBuilder[(String, Boolean)]
      var start = 0
      (1 to text.length).foreach { i =>
        if i == text.length || marks(i) != marks(start) then
          out += (text.substring(start, i) -> marks(start))
          start = i
      }
      out.result()

  def highlight(text: String, terms: List[String]): UI =
    UI.fragment(
      highlightSegments(text, terms).map { case (seg, hit) =>
        if hit then span.cssClass("mp-combo-mark")(seg) else span(seg)
      }*
    )

  // ── Rendering ──────────────────────────────────────────────────────────────

  /** @param search        returns the filtered, display-ordered items for a query (empty query → all items)
    * @param totalCount    number of items available without a query (for the "x of y" counter)
    * @param highlightTerms text fragments of a query to highlight in labels
    */
  def render[K](
      handle: Handle,
      selected: Option[Item[K]],
      search: String => List[Item[K]],
      totalCount: Int,
      onSelect: K => Any < Async,
      labels: Labels,
      highlightTerms: String => List[String] = q => q.trim.split("\\s+").toList.filter(_.nonEmpty),
      ariaLabel: String = "",
      idKey: String = "combo",
  )(using CanEqual[K, K]): UI =
    val listboxId = s"mp-combo-list-$idKey"

    def selectedIndexIn(items: List[Item[K]]): Int =
      selected.map(s => items.indexWhere(_.key == s.key)).filter(_ >= 0).getOrElse(0)

    def choose(key: K): Unit < Async =
      handle.close.andThen(onSelect(key)).unit

    def move(delta: Int): Unit < Async =
      for
        q   <- handle.query.get
        n    = search(q).size
        cur <- handle.active.get
        _   <- if n == 0 then Kyo.unit else handle.active.set(((cur + delta) % n + n) % n)
      yield ()

    def chooseActive: Unit < Async =
      for
        q   <- handle.query.get
        cur <- handle.active.get
        _   <- search(q).lift(cur) match
                 case Some(it) => choose(it.key)
                 case None     => Kyo.unit
      yield ()

    def onKey(ev: UI.KeyboardEvent): Unit < Async =
      ev.key match
        case UI.Keyboard.ArrowDown => move(1)
        case UI.Keyboard.ArrowUp   => move(-1)
        case UI.Keyboard.PageDown  => move(5)
        case UI.Keyboard.PageUp    => move(-5)
        case UI.Keyboard.Enter     => chooseActive
        case UI.Keyboard.Escape    => handle.close
        case UI.Keyboard.Tab       => handle.close
        case _                     => Kyo.unit

    def itemBody(item: Item[K], terms: List[String]): UI =
      UI.fragment(
        item.leading.map(l => div.cssClass("mp-combo-leading")(l)).getOrElse(UI.empty),
        div.cssClass("mp-combo-text")(
          div.cssClass("mp-combo-label")(highlight(item.label, terms)),
          item.meta.map(m => div.cssClass("mp-combo-meta")(m)).getOrElse(UI.empty),
        ),
        item.trailing.map(t => div.cssClass("mp-combo-trailing")(t)).getOrElse(UI.empty),
      )

    def trigger: UI =
      button
        .cssClass("mp-combo-trigger")
        .aria("haspopup", "listbox")
        .aria("expanded", "false")
        .aria("label", ariaLabel)
        .onKeyDown { ev =>
          ev.key match
            case UI.Keyboard.ArrowDown | UI.Keyboard.Enter => handle.openAt(selectedIndexIn(search("")))
            case _                                         => Kyo.unit
        }
        .onClick(handle.openAt(selectedIndexIn(search(""))))(
          selected match
            case Some(it) => itemBody(it, Nil)
            case None     => div.cssClass("mp-combo-text")(span.style(Style.color(inkMuted))(labels.emptyTrigger)),
          span.cssClass("mp-combo-chevron")(Icons(Icons.Icon.ChevronDown, 16)),
        )

    def results(query: String, active: Int): UI =
      val items = search(query)
      val terms = highlightTerms(query)
      if items.isEmpty then
        div.cssClass("mp-combo-empty")(
          span(labels.noResults),
          button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(handle.query.set("").andThen(handle.active.set(0)))(labels.clearSearch),
        )
      else
        val indexed = items.zipWithIndex
        // Group consecutive items sharing a group label (caller controls ordering)
        val groups: List[(Option[String], List[(Item[K], Int)])] =
          indexed.foldRight(List.empty[(Option[String], List[(Item[K], Int)])]) { case (entry @ (it, _), acc) =>
            acc match
              case (g, xs) :: rest if g == it.group => (g, entry :: xs) :: rest
              case _                                => (it.group, List(entry)) :: acc
          }
        div.cssClass("mp-combo-scroll").id(listboxId).role("listbox")(
          groups.map { case (g, entries) =>
            div.role("group").aria("label", g.getOrElse(""))(
              g.map(lbl => div.cssClass("mp-combo-group")(lbl)).getOrElse(UI.empty),
              UI.fragment(
                entries.map { case (it, idx) =>
                  val isSel    = selected.exists(_.key == it.key)
                  val isActive = idx == active
                  val cls =
                    List("mp-combo-option") ++
                      Option.when(isActive)("mp-combo-option-active") ++
                      Option.when(isSel)("mp-combo-option-selected")
                  button
                    .cssClass(cls.mkString(" "))
                    .role("option")
                    .aria("selected", isSel.toString)
                    .tabIndex(-1)
                    .onClick(choose(it.key))(
                      itemBody(it, terms),
                      span.cssClass("mp-combo-check")(if isSel then Icons(Icons.Icon.Check, 16) else UI.empty),
                    )
                }*
              ),
            )
          }*
        )
      end if
    end results

    handle.open.render { isOpen =>
      div.cssClass("mp-combo")(
        if !isOpen then trigger
        else
          UI.fragment(
            div.cssClass("mp-action-menu-backdrop").onClick(handle.close)(),
            div.cssClass("mp-combo-open")(
              div.cssClass("mp-combo-search")(
                span.style(Style.row.color(inkMuted).flexShrink(0.0))(Icons(Icons.Icon.Search, 16)),
                labels.searchPlaceholder.render { ph =>
                  input
                    .cssClass("mp-combo-input")
                    .role("combobox")
                    .aria("expanded", "true")
                    .aria("controls", listboxId)
                    .aria("autocomplete", "list")
                    .aria("label", ariaLabel)
                    .value(handle.query)
                    .placeholder(ph)
                    .focusAuto(true)
                    .onInput(_ => handle.active.set(0))
                    .onKeyDown(onKey)
                },
                button
                  .cssClass("mp-icon-btn mp-icon-btn-sm")
                  .aria("label", "Close")
                  .onClick(handle.close)(Icons(Icons.Icon.Close, 14)),
              ),
              div.cssClass("mp-combo-panel")(
                handle.query.combineLatest(handle.active).render { case (q, a) => results(q, a) },
                div.cssClass("mp-combo-footer")(
                  handle.query.render(q => span(labels.resultCount(search(q).size, totalCount))),
                  span.cssClass("mp-hide-sm")(labels.keyboardHint),
                ),
              ),
            ),
          ),
      )
    }
  end render
end ComboBox
