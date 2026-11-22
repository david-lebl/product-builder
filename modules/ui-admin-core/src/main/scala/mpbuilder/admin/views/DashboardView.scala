package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.ordering.OrderStatus
import mpbuilder.shared.Language
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Live Operations Dashboard with intake KPI cards, urgent order intake queue, and pending B2B customer approvals. */
object DashboardView:

  def render(state: BackofficeState): UI =
    val combined =
      state.dashboard
        .combineLatest(state.auth)
        .combineLatest(state.i18n.language)

    combined.render { case ((dashOpt, authOpt), lang) =>
      val canIntake = state.canIntake(authOpt)
      val canManage = state.canManage(authOpt)

      dashOpt match
        case None =>
          div.cssClass("mp-card")(
            span(if lang == Language.Cs then "Načítání přehledu..." else "Loading dashboard...").style(mutedText),
          )

        case Some(d) =>
          div.style(stackLg.width(100.pct))(
            // Title header
            div.style(rowBetween.flexWrap(FlexWrap.wrap))(
              div.style(stackXs)(
                h1(state.i18n.t("adm-dash-title")).style(Style.margin(0.px).fontSize(24.px).fontWeight(FontWeight.w800).color(ink)),
                p(state.i18n.t("adm-dash-subtitle")).style(mutedText.margin(0.px)),
              ),
              button
                .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                .onClick(state.navigate(AdminRoute.Orders))(state.i18n.t("adm-dash-view-all-orders")),
            ),

            // KPI Stat Grid
            div.cssClass("mp-kpi-grid")(
              kpiCard(state.i18n.t("adm-kpi-awaiting-intake"), d.ordersAwaitingIntake.toString, BadgeTone.Amber, () => state.navigate(AdminRoute.Orders)),
              kpiCard(state.i18n.t("adm-kpi-awaiting-payment"), d.ordersAwaitingPayment.toString, BadgeTone.Amber, () => state.navigate(AdminRoute.Orders)),
              kpiCard(state.i18n.t("adm-kpi-accepted"), d.ordersAccepted.toString, BadgeTone.Emerald, () => state.navigate(AdminRoute.Orders)),
              kpiCard(state.i18n.t("adm-kpi-in-production"), d.ordersInProduction.toString, BadgeTone.Brand, () => state.navigate(AdminRoute.Orders)),
              kpiCard(state.i18n.t("adm-kpi-ready-dispatch"), d.ordersReadyForDispatch.toString, BadgeTone.Brand, () => state.navigate(AdminRoute.Orders)),
              kpiCard(state.i18n.t("adm-kpi-on-hold"), d.ordersOnHold.toString, if d.ordersOnHold > 0 then BadgeTone.Rose else BadgeTone.Neutral, () => state.navigate(AdminRoute.Orders)),
              kpiCard(state.i18n.t("adm-kpi-pending-b2b"), d.pendingB2BCustomers.toString, if d.pendingB2BCustomers > 0 then BadgeTone.Amber else BadgeTone.Emerald, () => state.navigate(AdminRoute.Customers)),
              kpiCard(state.i18n.t("adm-kpi-active-promos"), d.activePromoCodes.toString, BadgeTone.Neutral, () => state.navigate(AdminRoute.Pricing)),
              kpiCard(state.i18n.t("adm-kpi-catalog"), s"${d.catalogCategoryCount} / ${d.catalogMaterialCount}", BadgeTone.Neutral, () => state.navigate(AdminRoute.Catalog)),
            ),

            // Two-column operational lists
            div.cssClass("mp-admin-split")(
              // Left: Recent & Awaiting Intake Orders
              div.cssClass("mp-admin-split-right")(
                div.cssClass("mp-card")(
                  div.style(rowBetween)(
                    h2(state.i18n.t("adm-dash-urgent-orders")).style(sectionTitle.margin(0.px)),
                    badge(d.totalOrders.toString, BadgeTone.Brand),
                  ),
                  if d.recentOrders.isEmpty then
                    p(state.i18n.t("adm-dash-no-orders")).style(mutedText.margin(0.px))
                  else
                    div.style(stackSm)(
                      d.recentOrders.map { ord =>
                        val needsAccept = ord.status match
                          case OrderStatus.Placed | OrderStatus.Confirmed => true
                          case _                                          => false
                        div.style(
                          Style.row
                            .flexWrap(FlexWrap.wrap)
                            .justify(Justification.spaceBetween)
                            .align(Alignment.center)
                            .gap(10.px)
                            .padding(12.px, 14.px)
                            .rounded(12.px)
                            .bg(surfaceAlt)
                            .border(1.px, Palette.border),
                        )(
                          div.style(stackXs)(
                            div.style(rowSm.flexWrap(FlexWrap.wrap))(
                              span(ord.orderNumber.value).style(Style.fontWeight(FontWeight.w800).fontSize(14.px).color(ink)),
                              badge(orderStatusLabel(ord.status, lang), orderStatusTone(ord.status)),
                              badge(ord.payment.status.toString, BadgeTone.Neutral),
                            ),
                            div.style(smallText)(
                              s"${ord.customer.companyName.map(_.value).getOrElse(ord.customer.fullName.value)} · ${ord.lines.size} ${if lang == Language.Cs then "pol." else "lines"} · ${formatInstant(ord.placedAt)}"
                            ),
                          ),
                          div.style(rowSm)(
                            span(formatMoney(ord.totals.grandTotalGross, ord.totals.currency, lang))
                              .style(Style.fontWeight(FontWeight.w700).fontSize(14.px).color(ink)),
                            if canIntake && needsAccept then
                              button
                                .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                                .onClick(state.acceptOrder(ord.id))(state.i18n.t("adm-od-accept"))
                            else UI.empty,
                            button
                              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                              .onClick(state.navigate(AdminRoute.OrderDetail(ord.id)))(state.i18n.t("adm-orders-open")),
                          ),
                        )
                      }*
                    ),
                ),
              ),

              // Right: Pending B2B Approvals
              div.cssClass("mp-admin-split-left")(
                div.cssClass("mp-card")(
                  div.style(rowBetween)(
                    h2(state.i18n.t("adm-dash-pending-customers")).style(sectionTitle.margin(0.px)),
                    badge(d.pendingB2BCustomers.toString, if d.pendingB2BCustomers > 0 then BadgeTone.Amber else BadgeTone.Emerald),
                  ),
                  if d.pendingCustomers.isEmpty then
                    p(state.i18n.t("adm-dash-no-pending-customers")).style(mutedText.margin(0.px))
                  else
                    div.style(stackSm)(
                      d.pendingCustomers.map { cust =>
                        div.style(
                          Style.column
                            .gap(8.px)
                            .padding(12.px, 14.px)
                            .rounded(12.px)
                            .bg(amberSoft)
                            .border(1.px, amberBorder),
                        )(
                          div.style(rowBetween)(
                            span(cust.company.map(_.legalName.value).getOrElse(s"${cust.contact.firstName.value} ${cust.contact.lastName.value}"))
                              .style(Style.fontWeight(FontWeight.w700).fontSize(14.px).color(ink)),
                            badge(cust.segment.toString, BadgeTone.Amber),
                          ),
                          div.style(smallText)(
                            s"${cust.contact.email.value}${cust.company.map(c => s" · IČO ${c.registrationNumber.value}").getOrElse("")}"
                          ),
                          div.style(rowSm)(
                            if canManage then
                              button
                                .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                                .onClick(state.approveCustomer(cust.id))(state.i18n.t("adm-cust-approve"))
                            else UI.empty,
                            button
                              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                              .onClick(state.selectCustomer(cust.id).andThen(state.navigate(AdminRoute.Customers)))(state.i18n.t("adm-orders-open")),
                          ),
                        )
                      }*
                    ),
                ),
              ),
            ),
          )
    }

  private def kpiCard(
      labelNode: UI,
      value: String,
      tone: BadgeTone,
      onClickAction: () => Unit < Async,
  ): UI =
    val accentColor = tone match
      case BadgeTone.Brand   => brand
      case BadgeTone.Emerald => emerald
      case BadgeTone.Amber   => amber
      case BadgeTone.Rose    => rose
      case BadgeTone.Neutral => ink
    button
      .cssClass("mp-kpi-card")
      .style(Style.cursor(Cursor.pointer).textAlign(TextAlign.left))
      .onClick(onClickAction())(
        span(labelNode).style(smallText.fontWeight(FontWeight.w600)),
        span(value).style(Style.fontSize(26.px).fontWeight(FontWeight.w800).color(accentColor).lineHeight(1.1)),
      )
