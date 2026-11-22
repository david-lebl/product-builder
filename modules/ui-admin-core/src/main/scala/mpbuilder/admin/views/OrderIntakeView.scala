package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.ordering.*
import mpbuilder.shared.{Language, OrderId}
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.components.DataTable
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Order Intake Queue screen powered by Vaadin-style Master-Detail `DataTable`:
  *   - External header with wrapping subtitle, Search, and Refresh button
  *   - Multi-select filters: Intake Status (> 5 options → searchable multi-select textfield),
  *     Payment Status & Priority (checkboxes, no counts)
  *   - Priority & calculated production deadline column (with mixed-speed badge)
  *   - Clicking any row opens OrderDetailView in the resizable right Master-Detail pane,
  *     with a Full View button (`AdminRoute.OrderDetail`) for dedicated full-screen workspace
  */
object OrderIntakeView:

  private val queueStatuses: List[OrderStatusKind] = List(
    OrderStatusKind.AwaitingPayment,
    OrderStatusKind.Confirmed,
    OrderStatusKind.Accepted,
    OrderStatusKind.InProduction,
    OrderStatusKind.ReadyForDispatch,
    OrderStatusKind.Dispatched,
    OrderStatusKind.OnHold,
    OrderStatusKind.Completed,
    OrderStatusKind.Cancelled,
  )

  private def statusKindLabel(kind: OrderStatusKind, lang: Language): String =
    val sampleStatus = kind match
      case OrderStatusKind.Placed           => OrderStatus.Placed
      case OrderStatusKind.AwaitingPayment  => OrderStatus.AwaitingPayment
      case OrderStatusKind.Confirmed        => OrderStatus.Confirmed
      case OrderStatusKind.Accepted         => OrderStatus.Accepted
      case OrderStatusKind.InProduction     => OrderStatus.InProduction
      case OrderStatusKind.ReadyForDispatch => OrderStatus.ReadyForDispatch
      case OrderStatusKind.Dispatched       => OrderStatus.Dispatched
      case OrderStatusKind.Completed        => OrderStatus.Completed
      case OrderStatusKind.OnHold           => OrderStatus.OnHold(mpbuilder.shared.NonEmptyString.unsafe("hold"))
      case OrderStatusKind.Cancelled        => OrderStatus.Cancelled(mpbuilder.shared.NonEmptyString.unsafe("cancel"))
    orderStatusLabel(sampleStatus, lang)

  def render(state: BackofficeState): UI < Sync =
    for
      tableState       <- DataTable.makeState[OrderId](
                            defaultSort = Some(DataTable.SortSpec("deadline", ascending = true)),
                            pageSize = 10,
                          )
      embeddedDetailUI <- OrderDetailView.render(state, embedded = true)
    yield
      val combined = state.orders.combineLatest(state.i18n.language)

      combined.render { case (allOrders, lang) =>
        val isCs = lang == Language.Cs

        val columns: List[DataTable.Column[OrderView]] = List(
          DataTable.Column[OrderView](
            id = "number",
            header = if isCs then "Číslo obj." else "Order #",
            sortKey = Some(_.orderNumber.value),
            cell = ord =>
              div.style(Style.column.gap(2.px))(
                span(ord.orderNumber.value).style(Style.fontWeight(FontWeight.w800).color(ink)),
                span(formatInstant(ord.placedAt)).style(smallText),
              ),
          ),
          DataTable.Column[OrderView](
            id = "customer",
            header = if isCs then "Zákazník" else "Customer",
            sortKey = Some(o => o.customer.companyName.map(_.value).getOrElse(o.customer.fullName.value)),
            cell = ord =>
              div.style(Style.column.gap(2.px))(
                span(ord.customer.companyName.map(_.value).getOrElse(ord.customer.fullName.value))
                  .style(Style.fontWeight(FontWeight.w600).color(ink)),
                span(ord.customer.email.value).style(smallText),
              ),
          ),
          DataTable.Column[OrderView](
            id = "deadline",
            header = if isCs then "Priorita a termín" else "Priority & Deadline",
            numericSortKey = Some(o => BigDecimal(o.calculatedDeadline.toEpochMilli)),
            cell = ord =>
              div.style(Style.column.gap(3.px))(
                div.style(rowXs.flexWrap(FlexWrap.wrap))(
                  priorityBadge(ord.priority, lang),
                  if ord.hasMixedSpeeds then badge(if isCs then "Smíšené" else "Mixed", BadgeTone.Amber) else UI.empty,
                ),
                span(formatInstant(ord.calculatedDeadline)).style(smallText.fontWeight(FontWeight.w600)),
              ),
          ),
          DataTable.Column[OrderView](
            id = "status",
            header = if isCs then "Stav příjmu" else "Intake Status",
            sortKey = Some(o => orderStatusLabel(o.status, lang)),
            cell = ord => badge(orderStatusLabel(ord.status, lang), orderStatusTone(ord.status)),
          ),
          DataTable.Column[OrderView](
            id = "payment",
            header = if isCs then "Platba" else "Payment",
            sortKey = Some(_.payment.status.toString),
            cell = ord =>
              div.style(Style.column.gap(2.px))(
                paymentStatusBadge(ord.payment.status, lang),
                span(ord.payment.method.toString).style(smallText),
              ),
          ),
          DataTable.Column[OrderView](
            id = "items",
            header = if isCs then "Položky" else "Items",
            numericSortKey = Some(o => BigDecimal(o.lines.size)),
            cell = ord =>
              val lineSummary = ord.lines.toList.map(l => s"${l.product.categoryName(lang)} ×${l.runQuantity.value}").mkString(", ")
              span(lineSummary).style(Style.fontSize(12.px).color(inkSecondary)),
          ),
          DataTable.Column[OrderView](
            id = "total",
            header = if isCs then "Celkem" else "Total",
            numericSortKey = Some(_.totals.grandTotalGross.value),
            alignRight = true,
            cell = ord =>
              span(formatMoney(ord.totals.grandTotalGross, ord.totals.currency, lang))
                .style(Style.fontWeight(FontWeight.w800).color(ink)),
          ),
        )

        val filterGroups: List[DataTable.FilterGroup[OrderView]] = List(
          DataTable.FilterGroup[OrderView](
            id = "status",
            label = if isCs then "Stav příjmu" else "Intake Status",
            options = queueStatuses.map { kind =>
              DataTable.FilterOption[OrderView](kind.toString, statusKindLabel(kind, lang), _.status.kind == kind)
            },
          ),
          DataTable.FilterGroup[OrderView](
            id = "payment",
            label = if isCs then "Stav platby" else "Payment Status",
            options = List(
              DataTable.FilterOption[OrderView]("Paid", if isCs then "Zaplaceno" else "Paid", _.payment.status == PaymentStatus.Confirmed),
              DataTable.FilterOption[OrderView]("Unpaid", if isCs then "Nezaplaceno" else "Unpaid", o => o.payment.status == PaymentStatus.Pending || o.payment.status == PaymentStatus.Authorized),
              DataTable.FilterOption[OrderView]("Failed", if isCs then "Selhalo" else "Failed", _.payment.status == PaymentStatus.Failed),
            ),
          ),
          DataTable.FilterGroup[OrderView](
            id = "priority",
            label = if isCs then "Priorita" else "Priority",
            options = OrderPriority.values.toList.map { pr =>
              DataTable.FilterOption[OrderView](pr.toString, priorityLabel(pr, lang), _.priority == pr)
            },
          ),
        )

        DataTable.render[OrderView, OrderId](
          title = Some(span(state.i18n.t("adm-orders-title"))),
          subtitle = Some(span(state.i18n.t("adm-orders-subtitle"))),
          rows = allOrders,
          rowKey = _.id,
          columns = columns,
          state = tableState,
          searchPlaceholder = if isCs then "Hledat číslo objednávky, zákazníka, firmu nebo e-mail..." else "Search order #, customer name, company, or email...",
          matchesSearch = (ord, q) =>
            ord.orderNumber.value.toLowerCase.contains(q) ||
              ord.customer.fullName.value.toLowerCase.contains(q) ||
              ord.customer.email.value.toLowerCase.contains(q) ||
              ord.customer.companyName.exists(_.value.toLowerCase.contains(q)),
          filterGroups = filterGroups,
          onRowSelect = Some(ord => state.selectedOrder.set(Some(ord)).andThen(state.refreshOrderDetail(ord.id))),
          renderDetail = Some(_ => embeddedDetailUI),
          detailTitle = Some(ord => s"${if isCs then "Objednávka" else "Order"} · ${ord.orderNumber.value}"),
          onOpenFullScreen = Some(ord => state.navigate(AdminRoute.OrderDetail(ord.id))),
          headerExtra = button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(state.refreshOrders)(state.i18n.t("adm-refresh")),
          emptyMessage = if isCs then "Aktuálním filtrům neodpovídají žádné objednávky." else "No orders match the current filters.",
          lang = lang,
        )
      }
