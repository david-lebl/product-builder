package mpbuilder.ui.storefront

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.customers.*
import mpbuilder.ordering.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Customer Account & Order History page displaying B2B tier, payment terms, billing address & past orders. */
object AccountView:

  def render(state: StorefrontState): UI =
    val combined =
      state.currentCustomer
        .combineLatest(state.orders)
        .combineLatest(state.currency)
        .combineLatest(state.i18n.language)

    combined.render { case (((custOpt, orders), cur), lang) =>
      custOpt match
        case None =>
          div.cssClass("mp-card").style(Style.align(Alignment.center).textAlign(TextAlign.center).padding(48.px, 24.px))(
            h1(state.i18n.t("account-title")).style(sectionTitle.margin(0.px)),
            p(state.i18n.t("account-not-signed-in")).style(mutedText.margin(0.px)),
            button
              .cssClass("mp-btn mp-btn-primary")
              .onClick(state.authModalOpen.set(true))(state.i18n.t("nav-sign-in")),
          )

        case Some(cust) =>
          div.style(Style.column.gap(22.px).width(100.pct))(
            div.style(rowBetween)(
              h1(state.i18n.t("account-title")).style(Style.margin(0.px).fontSize(26.px).fontWeight(FontWeight.w800).color(ink)),
              button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(state.signOut)(state.i18n.t("nav-sign-out")),
            ),

            // Organization & Billing Profile
            div.cssClass("mp-card")(
              div.style(rowBetween)(
                h2(state.i18n.t("account-profile-title")).style(sectionTitle.margin(0.px)),
                div.style(Style.row.gap(6.px))(
                  badge(cust.segment.toString, BadgeTone.Neutral),
                  badge(tierLabel(cust.tier), BadgeTone.Brand),
                ),
              ),
              div.cssClass("mp-field-row")(
                profileField(
                  if lang == Language.Cs then "Kontakt" else "Contact",
                  s"${cust.contact.firstName.value} ${cust.contact.lastName.value} · ${cust.contact.email.value}",
                ),
                cust.company match
                  case Some(comp) =>
                    profileField(
                      if lang == Language.Cs then "Společnost" else "Company",
                      s"${comp.legalName.value} (IČO ${comp.registrationNumber.value}${comp.vatId.map(v => s", DIČ ${v.value}").getOrElse("")})",
                    )
                  case None => UI.empty,
              ),
              div.cssClass("mp-field-row")(
                profileField(
                  if lang == Language.Cs then "Fakturační adresa" else "Billing Address",
                  s"${cust.billingAddress.street.value}, ${cust.billingAddress.postalCode.value} ${cust.billingAddress.city.value}, ${cust.billingAddress.countryCode.value}",
                ),
                div.cssClass("mp-field")(
                  span(state.i18n.t("account-payment-terms")).style(smallText.fontWeight(FontWeight.w700)),
                  cust.paymentTerms match
                    case PaymentTerms.PrepaidOnly =>
                      span(state.i18n.t("account-prepaid")).style(Style.fontSize(14.px).fontWeight(FontWeight.w600).color(ink))
                    case PaymentTerms.NetInvoice(days, limit) =>
                      span(
                        state.i18n.t(
                          "account-net-invoice",
                          Dict("days" -> days.toString, "limit" -> formatMoney(limit.amount, limit.currency, lang)),
                        ),
                      ).style(Style.fontSize(14.px).fontWeight(FontWeight.w700).color(emeraldInk)),
                ),
              ),
            ),

            // Order History
            div.cssClass("mp-card")(
              div.style(rowBetween)(
                h2(state.i18n.t("account-orders-title")).style(sectionTitle.margin(0.px)),
                badge(orders.size.toString, BadgeTone.Neutral),
              ),
              if orders.isEmpty then
                div.style(Style.column.align(Alignment.center).gap(10.px).padding(24.px, 0.px))(
                  p(state.i18n.t("account-orders-empty")).style(mutedText.margin(0.px)),
                  button
                    .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                    .onClick(state.openBuilder())(state.i18n.t("account-orders-cta")),
                )
              else
                div.style(Style.column.gap(12.px))(
                  orders.map { order =>
                    val statusTone = order.status match
                      case OrderStatus.Confirmed | OrderStatus.Completed | OrderStatus.Dispatched => BadgeTone.Emerald
                      case OrderStatus.AwaitingPayment                                            => BadgeTone.Amber
                      case OrderStatus.Cancelled(_)                                               => BadgeTone.Rose
                      case _                                                                      => BadgeTone.Brand

                    div.style(Style.column.gap(10.px).padding(16.px).rounded(12.px).border(1.px, Palette.border).bg(surfaceAlt))(
                      div.style(rowBetween)(
                        div.style(Style.row.align(Alignment.center).gap(8.px).flexWrap(FlexWrap.wrap))(
                          span(order.orderNumber.value).style(Style.fontWeight(FontWeight.w800).fontSize(15.px).color(ink)),
                          badge(orderStatusLabel(order.status, lang), statusTone),
                          badge(order.payment.method.toString, BadgeTone.Neutral),
                        ),
                        span(formatMoney(order.totals.grandTotalGross, order.totals.currency, lang))
                          .style(Style.fontWeight(FontWeight.w800).fontSize(17.px).color(ink)),
                      ),
                      div.style(smallText)(formatInstant(order.placedAt)),
                      div.style(Style.column.gap(6.px).padding(8.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
                        order.lines.toList.map { ln =>
                          div.style(rowBetween.fontSize(13.px))(
                            span(
                              s"${ln.copies.value}× ${ln.product.categoryName(lang)} (${ln.runQuantity.value} pcs, ${speedLabel(ln.speed, lang)})",
                            ).style(Style.color(inkSecondary)),
                            span(formatMoney(ln.lineGrossTotal, ln.price.currency, lang)).style(Style.fontWeight(FontWeight.w600)),
                          )
                        }*
                      ),
                    )
                  }*
                ),
            ),
          )
    }

  private def profileField(title: String, value: String): UI =
    div.cssClass("mp-field")(
      span(title).style(smallText.fontWeight(FontWeight.w700)),
      span(value).style(Style.fontSize(14.px).fontWeight(FontWeight.w600).color(ink)),
    )
