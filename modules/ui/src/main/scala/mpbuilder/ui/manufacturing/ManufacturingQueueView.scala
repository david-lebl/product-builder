package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.{Money, Currency}
import mpbuilder.domain.service.ManufacturingService
import mpbuilder.ui.ProductBuilderViewModel

object ManufacturingQueueView:

  private enum Filter:
    case All, PendingApproval, Active, Completed

  def apply(): Element =
    val lang      = ProductBuilderViewModel.currentLanguage
    val filterVar = Var(Filter.All)

    div(
      cls := "mfg-queue",

      // Page header
      div(
        cls := "card",
        h2(child.text <-- lang.map {
          case Language.En => "Manufacturing Queue"
          case Language.Cs => "Výrobní fronta"
        }),

        // Status message
        div(
          child.maybe <-- ManufacturingViewModel.message.map { msgOpt =>
            msgOpt.map { msg =>
              div(
                cls := "info-box",
                msg,
                button(
                  cls := "close-msg-btn",
                  "×",
                  onClick --> { _ => ManufacturingViewModel.clearMessage() },
                ),
              )
            }
          },
        ),

        // Filter bar
        div(
          cls := "mfg-filter-bar",
          filterButton(filterVar, lang, Filter.All, "All", "Vše"),
          filterButton(filterVar, lang, Filter.PendingApproval, "Pending Approval", "Čeká na schválení"),
          filterButton(filterVar, lang, Filter.Active, "In Progress", "Zpracovávané"),
          filterButton(filterVar, lang, Filter.Completed, "Completed", "Dokončené"),
        ),

        // Empty state or order list
        div(
          cls := "mfg-order-list",
          children <-- ManufacturingViewModel.orders
            .combineWith(filterVar.signal)
            .map { case (orders: List[ManufacturingOrder], filter: Filter) =>
              filter match
                case Filter.All             => orders
                case Filter.PendingApproval => orders.filter(_.status == ManufacturingStatus.PendingApproval)
                case Filter.Active          =>
                  orders.filter(o => !o.status.isTerminal && o.status != ManufacturingStatus.PendingApproval)
                case Filter.Completed       => orders.filter(_.status.isTerminal)
            }
            .combineWith(lang)
            .map { case (sorted: List[ManufacturingOrder], l: Language) =>
              if sorted.isEmpty then
                List(p(cls := "mfg-empty", l match
                  case Language.En => "No orders to show."
                  case Language.Cs => "Žádné objednávky."
                ))
              else
                sorted.sortBy(_.createdAt).reverse.map(order => orderCard(order, l))
            },
        ),
      ),
    )

  private def filterButton(
      filterVar: Var[Filter],
      lang: Signal[Language],
      filter: Filter,
      en: String,
      cs: String,
  ): Element =
    button(
      cls <-- filterVar.signal.map(f => if f == filter then "mfg-filter-btn active" else "mfg-filter-btn"),
      child.text <-- lang.map {
        case Language.En => en
        case Language.Cs => cs
      },
      onClick --> { _ => filterVar.set(filter) },
    )

  private def orderCard(order: ManufacturingOrder, lang: Language): Element =
    val config       = order.item.configuration
    val statusCls    = order.status.toString.toLowerCase.replace("_", "-")
    val formatCls    = order.formatType.toString.toLowerCase.replace("_", "-")

    div(
      cls := s"mfg-order-card",

      // Header row: order ID, format badge, status badge
      div(
        cls := "mfg-order-header",
        div(
          cls := "mfg-order-id",
          strong(order.id.value),
        ),
        div(
          cls := "mfg-order-badges",
          span(
            cls := s"mfg-format-badge mfg-format-$formatCls",
            order.formatType.label(lang),
          ),
          span(
            cls := s"mfg-status-badge mfg-status-$statusCls",
            order.status.label(lang),
          ),
        ),
      ),

      // Product details
      div(
        cls := "mfg-order-details",
        div(
          cls := "mfg-order-product",
          strong(config.category.name(lang)),
          span(s" · ${config.printingMethod.name(lang)}"),
          span(s" · ${order.item.quantity}×"),
        ),
        div(
          cls := "mfg-order-price",
          formatMoney(order.item.priceBreakdown.total * order.item.quantity, order.item.priceBreakdown.currency),
        ),
        order.notes.map { note =>
          p(cls := "mfg-order-notes", s"ℹ $note")
        }.getOrElse(emptyNode),
      ),

      // Action buttons
      div(
        cls := "mfg-order-actions",
        // Approve / Reject — only for PendingApproval
        if order.status == ManufacturingStatus.PendingApproval then
          div(
            cls := "mfg-approval-btns",
            button(
              cls := "mfg-btn mfg-btn-approve",
              lang match
                case Language.En => "✓ Approve"
                case Language.Cs => "✓ Schválit",
              onClick --> { _ => ManufacturingViewModel.approve(order.id) },
            ),
            button(
              cls := "mfg-btn mfg-btn-reject",
              lang match
                case Language.En => "✗ Reject"
                case Language.Cs => "✗ Zamítnout",
              onClick --> { _ => ManufacturingViewModel.reject(order.id) },
            ),
          )
        else emptyNode,
        // Advance button — label varies by current status
        ManufacturingService.nextStepLabel(order.status, lang) match
          case Some(label) =>
            button(
              cls := "mfg-btn mfg-btn-advance",
              s"→ $label",
              onClick --> { _ => ManufacturingViewModel.advance(order.id) },
            )
          case None => emptyNode,
        // Cancel button — for any non-terminal order
        if !order.status.isTerminal then
          button(
            cls := "mfg-btn mfg-btn-cancel",
            lang match
              case Language.En => "Cancel Order"
              case Language.Cs => "Zrušit objednávku",
            onClick --> { _ => ManufacturingViewModel.cancel(order.id) },
          )
        else emptyNode,
      ),
    )

  private def formatMoney(money: Money, currency: Currency): String =
    currency match
      case Currency.CZK => f"${money.value}%.2f Kč"
      case Currency.USD => f"$$${money.value}%.2f"
      case Currency.EUR => f"€${money.value}%.2f"
      case Currency.GBP => f"£${money.value}%.2f"
