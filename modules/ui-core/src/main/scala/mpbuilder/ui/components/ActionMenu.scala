package mpbuilder.ui.components

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Reusable compact dropdown action menu ("Actions ▾") for collapsing secondary
  * and destructive actions (Edit, Approve, Suspend, Deactivate, Put On Hold, Cancel, Delete)
  * into a single neat control on the detail header row.
  */
object ActionMenu:

  enum ItemTone:
    case Default, Primary, Amber, Danger

  final case class Item(
      label: UI,
      onClick: () => Unit < Async,
      tone: ItemTone = ItemTone.Default,
      dividerBefore: Boolean = false,
  )

  /** Stateful handle tracking which menu key (if any) is currently open. */
  final class Handle(val openKey: SignalRef[Option[String]]):
    def toggle(key: String): Unit < Async =
      openKey.updateAndGet(cur => if cur.contains(key) then None else Some(key)).unit

    def close: Unit < Async =
      openKey.set(None)

    def render(
        key: String,
        buttonLabel: UI,
        items: List[Item],
        buttonClass: String = "mp-btn mp-btn-secondary mp-btn-sm",
    ): UI =
      if items.isEmpty then UI.empty
      else
        openKey.render { activeKey =>
          val isOpen = activeKey.contains(key)
          div.cssClass("mp-action-menu-wrap")(
            if isOpen then
              div
                .cssClass("mp-action-menu-backdrop")
                .onClick(close)()
            else UI.empty,
            button
              .cssClass(buttonClass)
              .style(Style.whiteSpace(WhiteSpace.noWrap))
              .onClick(toggle(key))(
                buttonLabel,
                span(if isOpen then "▴" else "▾").style(Style.fontSize(10.px).margin(0.px, 0.px, 0.px, 2.px)),
              ),
            if isOpen then
              div.cssClass("mp-action-menu-panel")(
                UI.fragment(
                  items.map { item =>
                    val toneCls = item.tone match
                      case ItemTone.Primary => "mp-action-menu-item mp-action-menu-item-primary"
                      case ItemTone.Amber   => "mp-action-menu-item mp-action-menu-item-amber"
                      case ItemTone.Danger  => "mp-action-menu-item mp-action-menu-item-danger"
                      case ItemTone.Default => "mp-action-menu-item"
                    UI.fragment(
                      if item.dividerBefore then div.cssClass("mp-action-menu-divider")()
                      else UI.empty,
                      button
                        .cssClass(toneCls)
                        .onClick(close.andThen(item.onClick()))(item.label),
                    )
                  }*
                ),
              )
            else UI.empty,
          )
        }

  object Handle:
    def make: Handle < Sync =
      Signal.initRef[Option[String]](None).map(new Handle(_))
