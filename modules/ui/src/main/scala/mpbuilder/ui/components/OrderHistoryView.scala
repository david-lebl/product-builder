package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.{ProductBuilderViewModel, LoginState}
import mpbuilder.ui.{AppRouter, AppRoute}
import mpbuilder.ui.manufacturing.ManufacturingViewModel
import mpbuilder.domain.model.*
import mpbuilder.domain.model.ManufacturingOrder.*
import mpbuilder.domain.pricing.{Money, Currency}

/** Customer-facing order status, mapped from internal manufacturing statuses. */
enum OrderHistoryStatus:
  case Placed, InProduction, Completed, Dispatched

  def label(l: Language): String = (this, l) match
    case (Placed, Language.En)        => "Placed"
    case (Placed, Language.Cs)        => "Přijato"
    case (InProduction, Language.En)  => "In Production"
    case (InProduction, Language.Cs)  => "Ve výrobě"
    case (Completed, Language.En)     => "Completed"
    case (Completed, Language.Cs)     => "Dokončeno"
    case (Dispatched, Language.En)    => "Dispatched"
    case (Dispatched, Language.Cs)    => "Odesláno"

  def badgeClass: String = this match
    case Placed       => "badge badge-pending"
    case InProduction => "badge badge-info"
    case Completed    => "badge badge-ready"
    case Dispatched   => "badge badge-completed"

object OrderHistoryStatus:
  def fromManufacturingOrder(mo: ManufacturingOrder): OrderHistoryStatus =
    if mo.isDispatched then OrderHistoryStatus.Dispatched
    else if mo.workflows.nonEmpty && mo.overallStatus == WorkflowStatus.Completed then OrderHistoryStatus.Completed
    else if mo.approvalStatus == ApprovalStatus.Approved &&
            (mo.overallStatus == WorkflowStatus.InProgress || mo.overallStatus == WorkflowStatus.Pending ||
             mo.workflows.isEmpty)
    then OrderHistoryStatus.InProduction
    else OrderHistoryStatus.Placed

/** Customer-facing order history view, accessible when logged in. */
object OrderHistoryView:

  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val expandedOrderId: Var[Option[String]] = Var(None)

    // Derived signal: customer orders filtered by logged-in customer
    val customerOrdersSignal: Signal[List[ManufacturingOrder]] =
      ProductBuilderViewModel.currentCustomer.combineWith(ManufacturingViewModel.manufacturingOrders.signal).map {
        case (Some(customer), allOrders) =>
          allOrders.filter(_.order.customerId.contains(customer.id)).sortBy(-_.createdAt)
        case _ => List.empty
      }

    div(
      cls := "order-history-page",

      child <-- ProductBuilderViewModel.loginState.combineWith(lang).map { case (loginState, l) =>
        loginState match
          case _: LoginState.LoggedIn =>
            div(
              cls := "card",
              h2(child.text <-- lang.map {
                case Language.En => "Order History"
                case Language.Cs => "Historie objednávek"
              }),

              // Content: either empty state or orders list
              child <-- customerOrdersSignal.combineWith(expandedOrderId.signal).map { case (orders, expandedId) =>
                val l2 = ProductBuilderViewModel.stateVar.now().language
                  if orders.isEmpty then
                    div(
                      cls := "order-history-empty",
                      p(l2 match
                        case Language.En => "You have no orders yet."
                        case Language.Cs => "Zatím nemáte žádné objednávky."
                      ),
                      button(
                        cls := "checkout-btn",
                        l2 match
                          case Language.En => "Start Shopping →"
                          case Language.Cs => "Začít nakupovat →",
                        onClick --> { _ => AppRouter.navigateTo(AppRoute.ProductBuilder) },
                      ),
                    )
                  else
                    ordersListView(orders, expandedId, expandedOrderId, l2)
              },
            )

          case _ =>
            div(
              cls := "card",
              p(l match
                case Language.En => "Please log in to view your order history."
                case Language.Cs => "Pro zobrazení historie objednávek se prosím přihlaste."
              ),
              button(
                cls := "checkout-btn",
                l match
                  case Language.En => "← Back to Shop"
                  case Language.Cs => "← Zpět do obchodu",
                onClick --> { _ => AppRouter.navigateTo(AppRoute.ProductBuilder) },
              ),
            )
      },
    )

  private def ordersListView(
      orders: List[ManufacturingOrder],
      expandedId: Option[String],
      expandedOrderId: Var[Option[String]],
      l: Language,
  ): Element =
    div(
      cls := "order-history-list",

      // Status summary bar
      div(
        cls := "order-history-summary",
        statusSummaryCard(orders, OrderHistoryStatus.Placed, l),
        statusSummaryCard(orders, OrderHistoryStatus.InProduction, l),
        statusSummaryCard(orders, OrderHistoryStatus.Completed, l),
        statusSummaryCard(orders, OrderHistoryStatus.Dispatched, l),
      ),

      // Orders table
      table(
        cls := "order-history-table",
        thead(
          tr(
            th(l match { case Language.En => "Order"; case Language.Cs => "Objednávka" }),
            th(l match { case Language.En => "Date"; case Language.Cs => "Datum" }),
            th(l match { case Language.En => "Items"; case Language.Cs => "Položky" }),
            th(l match { case Language.En => "Total"; case Language.Cs => "Celkem" }),
            th(l match { case Language.En => "Status"; case Language.Cs => "Stav" }),
            th(l match { case Language.En => "Tracking"; case Language.Cs => "Sledování" }),
            th(""),
          ),
        ),
        tbody(
          orders.flatMap { mo =>
            val status = OrderHistoryStatus.fromManufacturingOrder(mo)
            val isExpanded = expandedId.contains(mo.order.id.value)
            val dateStr = formatDate(mo.createdAt)

            val mainRow = tr(
              cls := (if isExpanded then "order-history-row order-history-row--expanded" else "order-history-row"),
              onClick --> { _ =>
                expandedOrderId.update {
                  case Some(id) if id == mo.order.id.value => None
                  case _ => Some(mo.order.id.value)
                }
              },
              td(cls := "order-id-cell", mo.order.id.value),
              td(dateStr),
              td(mo.itemSummary),
              td(formatMoney(mo.order.total, mo.order.currency)),
              td(span(cls := status.badgeClass, status.label(l))),
              td(
                mo.fulfilment.flatMap(f =>
                  if f.dispatchInfo.trackingNumber.nonEmpty then Some(f.dispatchInfo.trackingNumber)
                  else None
                ).getOrElse("—")
              ),
              td(
                span(cls := "order-expand-icon", if isExpanded then "▲" else "▼"),
              ),
            )

            val detailRow = if isExpanded then
              List(orderDetailRow(mo, status, l))
            else List.empty

            mainRow :: detailRow
          },
        ),
      ),
    )

  private def statusSummaryCard(
      orders: List[ManufacturingOrder],
      status: OrderHistoryStatus,
      l: Language,
  ): Element =
    val count = orders.count(mo => OrderHistoryStatus.fromManufacturingOrder(mo) == status)
    div(
      cls := "order-history-summary-card",
      div(cls := "order-history-summary-count", count.toString),
      div(cls := "order-history-summary-label", status.label(l)),
    )

  private def orderDetailRow(
      mo: ManufacturingOrder,
      status: OrderHistoryStatus,
      l: Language,
  ): Element =
    tr(
      cls := "order-history-detail-row",
      td(
        colSpan := 7,
        div(
          cls := "order-history-detail",

          // Status progression
          div(
            cls := "order-history-progress",
            h4(l match
              case Language.En => "Order Progress"
              case Language.Cs => "Průběh objednávky"
            ),
            div(
              cls := "order-history-status-steps",
              OrderHistoryStatus.values.map { s =>
                val isCurrent = s == status
                val isDone = s.ordinal < status.ordinal
                val cls_ =
                  if isDone then "order-status-step order-status-step--done"
                  else if isCurrent then "order-status-step order-status-step--current"
                  else "order-status-step"
                div(
                  cls := cls_,
                  span(cls := "order-status-step-dot", if isDone then "✓" else (s.ordinal + 1).toString),
                  span(cls := "order-status-step-label", s.label(l)),
                )
              }.toSeq,
            ),
          ),

          // Order items detail
          div(
            cls := "order-history-items",
            h4(l match
              case Language.En => "Items"
              case Language.Cs => "Položky"
            ),
            mo.order.basket.items.map { item =>
              div(
                cls := "order-history-item-detail",
                div(
                  cls := "order-history-item-name",
                  strong(s"${item.quantity}× ${item.configuration.category.name(l)}"),
                  span(cls := "order-history-item-material",
                    s" • ${item.configuration.components.map(_.material.name(l)).mkString(", ")}"
                  ),
                ),
                div(cls := "order-history-item-price",
                  formatMoney(item.priceBreakdown.total * item.quantity, item.priceBreakdown.currency)
                ),
              )
            },
          ),

          // Tracking number if dispatched
          mo.fulfilment.flatMap(f =>
            if f.dispatchInfo.trackingNumber.nonEmpty then
              Some(div(
                cls := "order-history-tracking",
                h4(l match
                  case Language.En => "Tracking"
                  case Language.Cs => "Sledování zásilky"
                ),
                div(
                  cls := "order-history-tracking-number",
                  s"📦 ${f.dispatchInfo.trackingNumber}",
                ),
              ))
            else None
          ).getOrElse(emptyNode),

          // Reorder button
          div(
            cls := "order-history-actions",
            button(
              cls := "checkout-btn",
              l match
                case Language.En => "🔄 Reorder"
                case Language.Cs => "🔄 Objednat znovu",
              onClick --> { _ =>
                // Load the first item's configuration into the builder as a starting point
                mo.order.basket.items.headOption.foreach { item =>
                  // Reset and navigate to builder
                  ProductBuilderViewModel.resetProductForm()
                  AppRouter.navigateTo(AppRoute.ProductBuilder)
                }
              },
            ),
          ),
        ),
      ),
    )

  private def formatDate(timestamp: Long): String =
    val date = new scala.scalajs.js.Date(timestamp.toDouble)
    val day = f"${date.getDate().toInt}%02d"
    val month = f"${(date.getMonth() + 1).toInt}%02d"
    val year = date.getFullYear().toInt
    s"$day.$month.$year"

  private def formatMoney(money: Money, currency: Currency): String =
    currency match
      case Currency.CZK => f"${money.value}%.2f Kč"
      case Currency.USD => f"$$${money.value}%.2f"
      case Currency.EUR => f"€${money.value}%.2f"
      case Currency.GBP => f"£${money.value}%.2f"
