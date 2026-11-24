package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.ordering.OrderStatus
import mpbuilder.shared.Language
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.components.{Controls, Icons}
import mpbuilder.ui.components.Icons.Icon
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
                h1(state.i18n.t("adm-dash-title")).cssClass("mp-page-title"),
                p(state.i18n.t("adm-dash-subtitle")).style(mutedText.margin(0.px)),
              ),
              button
                .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                .onClick(state.navigate(AdminRoute.Orders))(Icons(Icon.Inbox, 15), span(state.i18n.t("adm-dash-view-all-orders"))),
            ),

            // KPI Stat Grids — order pipeline, then business health
            div.cssClass("mp-kpi-grid")(
              kpiCard(state.i18n.t("adm-kpi-awaiting-intake"), d.ordersAwaitingIntake.toString, Icon.Inbox, BadgeTone.Amber, () => state.navigate(AdminRoute.Orders)),
              kpiCard(state.i18n.t("adm-kpi-awaiting-payment"), d.ordersAwaitingPayment.toString, Icon.CreditCard, BadgeTone.Amber, () => state.navigate(AdminRoute.Orders)),
              kpiCard(state.i18n.t("adm-kpi-accepted"), d.ordersAccepted.toString, Icon.CheckCircle, BadgeTone.Emerald, () => state.navigate(AdminRoute.Orders)),
              kpiCard(state.i18n.t("adm-kpi-in-production"), d.ordersInProduction.toString, Icon.Printer, BadgeTone.Brand, () => state.navigate(AdminRoute.Orders)),
              kpiCard(state.i18n.t("adm-kpi-ready-dispatch"), d.ordersReadyForDispatch.toString, Icon.Truck, BadgeTone.Brand, () => state.navigate(AdminRoute.Orders)),
              kpiCard(state.i18n.t("adm-kpi-on-hold"), d.ordersOnHold.toString, Icon.Pause, if d.ordersOnHold > 0 then BadgeTone.Rose else BadgeTone.Neutral, () => state.navigate(AdminRoute.Orders)),
            ),
            div.cssClass("mp-kpi-grid")(
              kpiCard(state.i18n.t("adm-kpi-pending-b2b"), d.pendingB2BCustomers.toString, Icon.Building, if d.pendingB2BCustomers > 0 then BadgeTone.Amber else BadgeTone.Emerald, () => state.navigate(AdminRoute.Customers)),
              kpiCard(state.i18n.t("adm-kpi-active-promos"), d.activePromoCodes.toString, Icon.Percent, BadgeTone.Brand, () => state.navigate(AdminRoute.Pricing)),
              kpiCard(state.i18n.t("adm-kpi-catalog"), s"${d.catalogCategoryCount} / ${d.catalogMaterialCount}", Icon.Layers, BadgeTone.Neutral, () => state.navigate(AdminRoute.Catalog)),
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
                        val custName = ord.customer.companyName.map(_.value).getOrElse(ord.customer.fullName.value)
                        val linesLbl =
                          if lang == Language.Cs then s"${ord.lines.size} pol."
                          else if ord.lines.size == 1 then "1 line"
                          else s"${ord.lines.size} lines"
                        div.cssClass("mp-list-row")(
                          div.style(Style.row.align(Alignment.center).gap(12.px).minWidth(0.px).flexGrow(1.0).flexBasis(280.px))(
                            Controls.avatar(custName),
                            div.style(stackXs.minWidth(0.px))(
                              div.style(rowSm.flexWrap(FlexWrap.wrap))(
                                span(ord.orderNumber.value).style(Style.fontWeight(FontWeight.w800).fontSize(14.px).color(ink)),
                                badge(orderStatusLabel(ord.status, lang), orderStatusTone(ord.status)),
                                paymentStatusBadge(ord.payment.status, lang),
                              ),
                              div.style(smallText)(s"$custName · $linesLbl · ${formatInstant(ord.placedAt)}"),
                            ),
                          ),
                          div.style(rowSm.flexShrink(0.0).margin(0.px, 0.px, 0.px, Length.Auto))(
                            span(formatMoney(ord.totals.grandTotalGross, ord.totals.currency, lang))
                              .style(Style.fontWeight(FontWeight.w700).fontSize(14.px).color(ink).whiteSpace(WhiteSpace.noWrap)),
                            if canIntake && needsAccept then
                              button
                                .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                                .onClick(state.acceptOrder(ord.id))(Icons(Icon.Check, 15), span(state.i18n.t("adm-od-accept")))
                            else UI.empty,
                            button
                              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                              .onClick(state.navigate(AdminRoute.OrderDetail(ord.id)))(span(state.i18n.t("adm-orders-open")), Icons(Icon.ChevronRight, 15)),
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
                                .onClick(state.approveCustomer(cust.id))(Icons(Icon.Check, 15), span(state.i18n.t("adm-cust-approve")))
                            else UI.empty,
                            button
                              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                              .onClick(state.selectCustomer(cust.id).andThen(state.navigate(AdminRoute.Customers)))(span(state.i18n.t("adm-orders-open")), Icons(Icon.ChevronRight, 15)),
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
      icon: Icon,
      tone: BadgeTone,
      onClickAction: () => Unit < Async,
  ): UI =
    val (accent, soft) = tone match
      case BadgeTone.Brand   => (brand, brandSoft)
      case BadgeTone.Emerald => (emerald, emeraldSoft)
      case BadgeTone.Amber   => (amber, amberSoft)
      case BadgeTone.Rose    => (rose, roseSoft)
      case BadgeTone.Neutral => (inkSecondary, surfaceAlt)
    button
      .cssClass("mp-kpi-card")
      .onClick(onClickAction())(
        div.style(Style.row.align(Alignment.start).justify(Justification.spaceBetween).gap(10.px))(
          span(labelNode).style(smallText.fontWeight(FontWeight.w600).color(inkSecondary).lineHeight(1.35)),
          div.cssClass("mp-kpi-icon").style(Style.bg(soft).color(accent))(Icons(icon, 17)),
        ),
        span(value).cssClass("mp-kpi-value").style(if tone == BadgeTone.Rose then Style.color(rose) else Style.color(ink)),
      )
