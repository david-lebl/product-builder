package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.ordering.{OrderStatus, OrderStatusKind, PaymentStatus}
import mpbuilder.shared.Language
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Order Intake Queue screen with status filter pills, live counts, search bar, and responsive order table. */
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

  private def paymentTone(ps: PaymentStatus): BadgeTone = ps match
    case PaymentStatus.Confirmed | PaymentStatus.Authorized => BadgeTone.Emerald
    case PaymentStatus.Pending                              => BadgeTone.Amber
    case PaymentStatus.Failed | PaymentStatus.Refunded      => BadgeTone.Rose

  def render(state: BackofficeState): UI =
    val combined =
      state.orders
        .combineLatest(state.orderStatusFilter)
        .combineLatest(state.orderSearch)
        .combineLatest(state.i18n.language)

    combined.render { case (((allOrders, statusFilt), queryRaw), lang) =>
      val q = queryRaw.trim.toLowerCase
      val filtered = allOrders.filter { ord =>
        val matchesStatus = statusFilt.forall(_ == ord.status.kind)
        val matchesQuery =
          if q.isEmpty then true
          else
            ord.orderNumber.value.toLowerCase.contains(q) ||
              ord.customer.fullName.value.toLowerCase.contains(q) ||
              ord.customer.email.value.toLowerCase.contains(q) ||
              ord.customer.companyName.exists(_.value.toLowerCase.contains(q))
        matchesStatus && matchesQuery
      }

      div.style(stackMd.width(100.pct))(
        // Page Header
        div.style(rowBetween.flexWrap(FlexWrap.wrap))(
          div.style(stackXs)(
            h1(state.i18n.t("adm-orders-title")).style(Style.margin(0.px).fontSize(24.px).fontWeight(FontWeight.w800).color(ink)),
            p(state.i18n.t("adm-orders-subtitle")).style(mutedText.margin(0.px)),
          ),
          button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(state.refreshOrders)(state.i18n.t("adm-refresh")),
        ),

        // Search + Status Filter Pills
        div.cssClass("mp-card").style(Style.gap(12.px).padding(16.px))(
          searchInput
            .cssClass("mp-input")
            .placeholder(if lang == Language.Cs then "Hledat číslo objednávky, zákazníka, firmu nebo e-mail..." else "Search order #, customer name, company, or email...")
            .value(state.orderSearch)
            .onInput(v => state.orderSearch.set(v)),
          div.style(rowWrap)(
            button
              .cssClass(if statusFilt.isEmpty then "mp-pill-active" else "mp-pill")
              .onClick(state.orderStatusFilter.set(None))(
                span(state.i18n.t("adm-orders-filter-all")),
                span(s"(${allOrders.size})").style(Style.fontSize(11.px).opacity(0.85)),
              ),
            UI.fragment(
              queueStatuses.map { kind =>
                val cnt    = allOrders.count(_.status.kind == kind)
                val active = statusFilt.contains(kind)
                button
                  .cssClass(if active then "mp-pill-active" else "mp-pill")
                  .onClick(state.orderStatusFilter.set(Some(kind)))(
                    span(statusKindLabel(kind, lang)),
                    span(s"($cnt)").style(Style.fontSize(11.px).opacity(0.85)),
                  )
              }*
            ),
          ),
        ),

        // Orders Table / Empty State
        if filtered.isEmpty then
          div.cssClass("mp-card").style(Style.align(Alignment.center).padding(40.px, 20.px))(
            p(state.i18n.t("adm-orders-empty")).style(mutedText.margin(0.px)),
          )
        else
          div.cssClass("mp-table-wrap")(
            UI.table.cssClass("mp-table").style(Style.width(100.pct))(
              tbody(
                tr.style(Style.bg(surfaceAlt).borderBottom(1.px, Palette.border))(
                  thCell(state.i18n.t("adm-orders-col-number")),
                  thCell(state.i18n.t("adm-orders-col-customer")),
                  thCell(state.i18n.t("adm-orders-col-status")),
                  thCell(state.i18n.t("adm-orders-col-payment")),
                  thCell(state.i18n.t("adm-orders-col-items")),
                  thCell(state.i18n.t("adm-orders-col-total")),
                  thCell(state.i18n.t("adm-orders-col-placed")),
                  thCell(UI.empty),
                ),
                UI.fragment(
                  filtered.map { ord =>
                    val lineSummary = ord.lines.toList.map(l => s"${l.product.categoryName(lang)} ×${l.runQuantity.value}").mkString(", ")
                    tr.style(Style.borderBottom(1.px, Palette.border))(
                      tdCell(
                        span(ord.orderNumber.value).style(Style.fontWeight(FontWeight.w800).color(ink)),
                      ),
                      tdCell(
                        div.style(Style.column.gap(2.px))(
                          span(ord.customer.companyName.map(_.value).getOrElse(ord.customer.fullName.value))
                            .style(Style.fontWeight(FontWeight.w600).color(ink)),
                          span(ord.customer.email.value).style(smallText),
                        ),
                      ),
                      tdCell(badge(orderStatusLabel(ord.status, lang), orderStatusTone(ord.status))),
                      tdCell(
                        div.style(Style.column.gap(2.px))(
                          badge(ord.payment.status.toString, paymentTone(ord.payment.status)),
                          span(ord.payment.method.toString).style(smallText),
                        ),
                      ),
                      tdCell(
                        span(lineSummary).style(Style.fontSize(12.px).color(inkSecondary)),
                      ),
                      tdCell(
                        span(formatMoney(ord.totals.grandTotalGross, ord.totals.currency, lang))
                          .style(Style.fontWeight(FontWeight.w700).color(ink)),
                      ),
                      tdCell(
                        span(formatInstant(ord.placedAt)).style(smallText),
                      ),
                      tdCell(
                        button
                          .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                          .onClick(state.navigate(AdminRoute.OrderDetail(ord.id)))(state.i18n.t("adm-orders-open")),
                      ),
                    )
                  }*
                ),
              ),
            ),
          ),
      )
    }

  private def thCell(content: UI): UI =
    th.style(
      Style.padding(11.px, 14.px)
        .textAlign(TextAlign.left)
        .fontSize(12.px)
        .fontWeight(FontWeight.w700)
        .color(inkSecondary),
    )(content)

  private def tdCell(content: UI): UI =
    td.style(Style.padding(12.px, 14.px))(content)
