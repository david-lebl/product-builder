package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.artwork.{ArtworkStatus, CheckOutcome}
import mpbuilder.ordering.*
import mpbuilder.shared.Language
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Order Detail & Manual Intake Workspace:
  *   - Staff acceptance, payment confirmation, hold/resume, manual stage progression & cancellation
  *   - Frozen line specifications, imposition sheet counts & commercial totals
  *   - Artwork & preflight review actions
  *   - Internal staff notes & full chronological status audit trail
  */
object OrderDetailView:

  def render(state: BackofficeState): UI < Sync =
    for
      actionNoteRef   <- Signal.initRef("")
      holdReasonRef   <- Signal.initRef("")
      cancelReasonRef <- Signal.initRef("")
      internalNoteRef <- Signal.initRef("")
      artworkNotesRef <- Signal.initRef("")
    yield
      val combined =
        state.selectedOrder
          .combineLatest(state.selectedOrderArtwork)
          .combineLatest(state.auth)
          .combineLatest(state.i18n.language)

      combined.render { case (((orderOpt, artworks), authOpt), lang) =>
        val canIntake = state.canIntake(authOpt)

        orderOpt match
          case None =>
            div.cssClass("mp-card")(
              div.style(rowBetween)(
                span(if lang == Language.Cs then "Objednávka nebyla nalezena." else "Order not found.").style(mutedText),
                button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(state.navigate(AdminRoute.Orders))(state.i18n.t("adm-od-back")),
              ),
            )

          case Some(ord) =>
            val canConfirmPayment = ord.status == OrderStatus.AwaitingPayment
            val canAccept = ord.status match
              case OrderStatus.Placed | OrderStatus.AwaitingPayment | OrderStatus.Confirmed => true
              case _                                                                         => false
            val isOnHold = ord.status.isInstanceOf[OrderStatus.OnHold]
            val canPutOnHold = ord.status match
              case OrderStatus.Placed | OrderStatus.AwaitingPayment | OrderStatus.Confirmed |
                  OrderStatus.Accepted | OrderStatus.InProduction | OrderStatus.ReadyForDispatch =>
                true
              case _ => false
            val canCancel = ord.status match
              case OrderStatus.Placed | OrderStatus.AwaitingPayment | OrderStatus.Confirmed |
                  OrderStatus.Accepted | OrderStatus.OnHold(_) =>
                true
              case _ => false
            val nextStages: List[(OrderStatus, String)] = ord.status match
              case OrderStatus.Accepted =>
                List(OrderStatus.InProduction -> "adm-od-stage-inprod")
              case OrderStatus.InProduction =>
                List(OrderStatus.ReadyForDispatch -> "adm-od-stage-ready")
              case OrderStatus.ReadyForDispatch =>
                List(OrderStatus.Dispatched -> "adm-od-stage-dispatched")
              case OrderStatus.Dispatched =>
                List(OrderStatus.Completed -> "adm-od-stage-completed")
              case _ => Nil

            div.style(stackMd.width(100.pct))(
              // Top bar
              div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                div.style(rowSm.flexWrap(FlexWrap.wrap))(
                  button
                    .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                    .onClick(state.navigate(AdminRoute.Orders))(state.i18n.t("adm-od-back")),
                  h1(ord.orderNumber.value).style(Style.margin(0.px).fontSize(24.px).fontWeight(FontWeight.w800).color(ink)),
                  badge(orderStatusLabel(ord.status, lang), orderStatusTone(ord.status)),
                  badge(s"${ord.payment.method} · ${ord.payment.status}", BadgeTone.Neutral),
                ),
                span(formatMoney(ord.totals.grandTotalGross, ord.totals.currency, lang))
                  .style(Style.fontSize(22.px).fontWeight(FontWeight.w800).color(brand)),
              ),

              // OnHold / Cancelled banner if active
              ord.status match
                case OrderStatus.OnHold(reason) =>
                  alertBanner(BadgeTone.Amber, span(orderStatusLabel(ord.status, lang)), List(reason.value))
                case OrderStatus.Cancelled(reason) =>
                  alertBanner(BadgeTone.Rose, span(orderStatusLabel(ord.status, lang)), List(reason.value))
                case _ => UI.empty,

              // Main 2-column layout
              div.cssClass("mp-admin-split")(
                // Left primary column: Actions + Lines + Artwork
                div.cssClass("mp-admin-split-right")(
                  // 1. Staff Intake & Manual Stage Controls
                  div.cssClass("mp-card")(
                    h2(state.i18n.t("adm-od-actions-title")).style(sectionTitle.margin(0.px)),
                    if !canIntake then
                      alertBanner(
                        BadgeTone.Neutral,
                        span(if lang == Language.Cs then "Pouze čtení (Prepress)" else "Read-only Intake (Prepress)"),
                        List(
                          if lang == Language.Cs then "Přijetí objednávky a posun stavů vyžaduje roli Operator, Manager nebo Admin."
                          else "Accepting orders and advancing stages requires Operator, Manager, or Admin role.",
                        ),
                      )
                    else
                      div.style(stackSm)(
                        input
                          .cssClass("mp-input")
                          .placeholder(if lang == Language.Cs then "Volitelná interní poznámka k přechodu stavu..." else "Optional staff note for this transition...")
                          .value(actionNoteRef)
                          .onInput(v => actionNoteRef.set(v)),
                        div.style(rowWrap)(
                          if canConfirmPayment then
                            button
                              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                              .onClick(
                                actionNoteRef.get.flatMap(n => state.confirmOrderPayment(ord.id, n).andThen(actionNoteRef.set("")))
                              )(state.i18n.t("adm-od-confirm-payment"))
                          else UI.empty,
                          if canAccept then
                            button
                              .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                              .onClick(
                                actionNoteRef.get.flatMap(n => state.acceptOrder(ord.id, n).andThen(actionNoteRef.set("")))
                              )(state.i18n.t("adm-od-accept"))
                          else UI.empty,
                          if isOnHold then
                            button
                              .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                              .onClick(
                                actionNoteRef.get.flatMap(n => state.resumeOrderFromHold(ord.id, n).andThen(actionNoteRef.set("")))
                              )(state.i18n.t("adm-od-resume"))
                          else UI.empty,
                          UI.fragment(
                            nextStages.map { case (target, i18nKey) =>
                              button
                                .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                                .onClick(
                                  actionNoteRef.get.flatMap(n => state.advanceOrderStage(ord.id, target, n).andThen(actionNoteRef.set("")))
                                )(state.i18n.t(i18nKey))
                            }*
                          ),
                        ),
                        // Hold & Cancel controls
                        if canPutOnHold || canCancel then
                          div.style(Style.column.gap(8.px).padding(10.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
                            if canPutOnHold then
                              div.style(rowSm)(
                                input
                                  .cssClass("mp-input")
                                  .placeholder(if lang == Language.Cs then "Důvod pozastavení..." else "Reason for putting on hold...")
                                  .value(holdReasonRef)
                                  .onInput(v => holdReasonRef.set(v)),
                                button
                                  .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                                  .style(Style.flexShrink(0.0))
                                  .onClick(
                                    holdReasonRef.get.flatMap(r => state.putOrderOnHold(ord.id, r).andThen(holdReasonRef.set("")))
                                  )(state.i18n.t("adm-od-put-hold")),
                              )
                            else UI.empty,
                            if canCancel then
                              div.style(rowSm)(
                                input
                                  .cssClass("mp-input")
                                  .placeholder(if lang == Language.Cs then "Důvod zrušení objednávky..." else "Cancellation reason...")
                                  .value(cancelReasonRef)
                                  .onInput(v => cancelReasonRef.set(v)),
                                button
                                  .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                                  .style(Style.flexShrink(0.0))
                                  .onClick(
                                    cancelReasonRef.get.flatMap(r => state.cancelOrder(ord.id, r).andThen(cancelReasonRef.set("")))
                                  )(state.i18n.t("adm-od-cancel")),
                              )
                            else UI.empty,
                          )
                        else UI.empty,
                      ),
                  ),

                  // 2. Frozen Order Lines
                  div.cssClass("mp-card")(
                    div.style(rowBetween)(
                      h2(state.i18n.t("adm-od-lines-card")).style(sectionTitle.margin(0.px)),
                      badge(ord.lines.size.toString, BadgeTone.Neutral),
                    ),
                    div.style(stackSm)(
                      ord.lines.toList.map { ln =>
                        div.style(Style.column.gap(8.px).padding(14.px).rounded(12.px).bg(surfaceAlt).border(1.px, Palette.border))(
                          div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                            div.style(rowSm.flexWrap(FlexWrap.wrap))(
                              span(ln.product.categoryName(lang)).style(Style.fontWeight(FontWeight.w800).fontSize(15.px).color(ink)),
                              badge(s"${ln.runQuantity.value} pcs × ${ln.copies.value}", BadgeTone.Brand),
                              badge(speedLabel(ln.speed, lang), BadgeTone.Neutral),
                            ),
                            span(formatMoney(ln.lineGrossTotal, ln.price.currency, lang))
                              .style(Style.fontWeight(FontWeight.w800).fontSize(15.px).color(ink)),
                          ),
                          div.style(stackXs)(
                            ln.product.components.toList.map { comp =>
                              div.style(Style.fontSize(12.px).color(inkSecondary))(
                                s"• ${comp.key.value}: ${comp.materialName(lang)} · ${formatDimension(comp.finishedSize)}" +
                                  comp.inkNotation.map(i => s" · $i").getOrElse("") +
                                  s" · ${comp.sheetsUsed} ${if lang == Language.Cs then "archů" else "sheets"}" +
                                  (if comp.finishIds.nonEmpty then s" · Finishes: ${comp.finishIds.map(_.value).mkString(", ")}" else "")
                              )
                            }*
                          ),
                          ln.product.summaryNotes.map(n => div.style(smallText)(n)).getOrElse(UI.empty),
                        )
                      }*
                    ),
                  ),

                  // 3. Artwork & Preflight Review
                  div.cssClass("mp-card")(
                    div.style(rowBetween)(
                      h2(state.i18n.t("adm-od-artwork-card")).style(sectionTitle.margin(0.px)),
                      badge(artworks.size.toString, BadgeTone.Neutral),
                    ),
                    if artworks.isEmpty then
                      p(state.i18n.t("adm-od-artwork-empty")).style(mutedText.margin(0.px))
                    else
                      div.style(stackSm)(
                        input
                          .cssClass("mp-input")
                          .placeholder(if lang == Language.Cs then "Popište požadované opravy tiskových dat..." else "Describe required artwork fixes...")
                          .value(artworkNotesRef)
                          .onInput(v => artworkNotesRef.set(v)),
                        UI.fragment(
                          artworks.map { art =>
                            val artTone = art.status match
                              case _: ArtworkStatus.OperatorApproved | ArtworkStatus.PreflightPassed => BadgeTone.Emerald
                              case ArtworkStatus.PreflightFailed | _: ArtworkStatus.Rejected         => BadgeTone.Rose
                              case _                                                                 => BadgeTone.Amber
                            val rev = art.latestRevision
                            div.style(Style.column.gap(8.px).padding(12.px).rounded(12.px).bg(surfaceAlt).border(1.px, Palette.border))(
                              div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                                span(s"${rev.file.fileName.value} (rev ${rev.revisionNumber}) · ${art.componentKey.value}")
                                  .style(Style.fontWeight(FontWeight.w700).color(ink)),
                                badge(art.status.toString, artTone),
                              ),
                              div.style(smallText)(
                                s"${rev.extractedSpecs.resolutionDpi} DPI · ${formatDimension(rev.extractedSpecs.trimSize)} · ${rev.extractedSpecs.colorSpace} · ${rev.extractedSpecs.pageCount}p"
                              ),
                              rev.preflight match
                                case Some(rep) =>
                                  div.style(rowWrap)(
                                    rep.checks.map { ch =>
                                      val t = ch.outcome match
                                        case CheckOutcome.Passed     => BadgeTone.Emerald
                                        case _: CheckOutcome.Warning => BadgeTone.Amber
                                        case _: CheckOutcome.Failed  => BadgeTone.Rose
                                      badge(s"${ch.kind}: ${ch.outcome}", t)
                                    }*
                                  )
                                case None => UI.empty,
                              div.style(rowWrap)(
                                button
                                  .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                                  .onClick(state.approveArtwork(ord.id, art.id, overrideFailures = false))(state.i18n.t("adm-od-artwork-approve")),
                                button
                                  .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                                  .onClick(state.approveArtwork(ord.id, art.id, overrideFailures = true))(state.i18n.t("adm-od-artwork-override")),
                                button
                                  .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                                  .onClick(
                                    artworkNotesRef.get.flatMap(n => state.requestArtworkChanges(ord.id, art.id, n).andThen(artworkNotesRef.set("")))
                                  )(state.i18n.t("adm-od-artwork-request-changes")),
                              ),
                            )
                          }*
                        ),
                      ),
                  ),
                ),

                // Right secondary column: Customer/Totals + Staff Notes + Audit Timeline
                div.cssClass("mp-admin-split-left")(
                  // Customer & Delivery Card
                  div.cssClass("mp-card")(
                    h2(state.i18n.t("adm-od-customer-card")).style(sectionTitle.margin(0.px)),
                    infoRow(if lang == Language.Cs then "Zákazník" else "Customer", s"${ord.customer.fullName.value} (${ord.customer.segment})"),
                    ord.customer.companyName
                      .map(cn => infoRow(if lang == Language.Cs then "Firma" else "Company", s"${cn.value}${ord.customer.companyRegNo.map(r => s" · IČO ${r.value}").getOrElse("")}${ord.customer.vatId.map(v => s" · DIČ ${v.value}").getOrElse("")}"))
                      .getOrElse(UI.empty),
                    infoRow("Email", ord.customer.email.value),
                    infoRow(
                      if lang == Language.Cs then "Fakturační adresa" else "Billing Address",
                      s"${ord.billingAddress.street.value}, ${ord.billingAddress.postalCode.value} ${ord.billingAddress.city.value}, ${ord.billingAddress.countryCode.value}",
                    ),
                    infoRow(
                      if lang == Language.Cs then "Doručení" else "Delivery",
                      ord.delivery match
                        case DeliverySelection.ShopPickup(locId) => s"Shop Pickup (${locId.value})"
                        case DeliverySelection.Courier(svcId, addr, _, _) =>
                          s"Courier (${svcId.value}) → ${addr.street.value}, ${addr.city.value}",
                    ),
                    ord.customerNote
                      .map(cn => alertBanner(BadgeTone.Brand, state.i18n.t("adm-od-customer-note"), List(cn)))
                      .getOrElse(UI.empty),
                  ),

                  // Totals Card
                  div.cssClass("mp-card")(
                    h2(state.i18n.t("adm-od-totals-card")).style(sectionTitle.margin(0.px)),
                    totalRow(state.i18n.t("adm-od-subtotal"), formatMoney(ord.totals.linesNetTotal, ord.totals.currency, lang)),
                    if ord.totals.promoDiscountNet.isPositive then
                      totalRow(
                        span(s"${ord.appliedPromo.map(_.code.value).getOrElse("Promo")}"),
                        s"−${formatMoney(ord.totals.promoDiscountNet, ord.totals.currency, lang)}",
                        emeraldInk,
                      )
                    else UI.empty,
                    totalRow(state.i18n.t("adm-od-shipping"), formatMoney(ord.totals.shippingNet, ord.totals.currency, lang)),
                    totalRow(state.i18n.t("adm-od-vat"), formatMoney(ord.totals.taxTotal, ord.totals.currency, lang)),
                    hr.style(dividerStyle),
                    div.style(rowBetween)(
                      span(state.i18n.t("adm-od-grand-total")).style(Style.fontWeight(FontWeight.w800).fontSize(15.px).color(ink)),
                      span(formatMoney(ord.totals.grandTotalGross, ord.totals.currency, lang))
                        .style(Style.fontWeight(FontWeight.w800).fontSize(18.px).color(brand)),
                    ),
                  ),

                  // Internal Staff Notes
                  div.cssClass("mp-card")(
                    div.style(rowBetween)(
                      h2(state.i18n.t("adm-od-notes-card")).style(sectionTitle.margin(0.px)),
                      badge(ord.internalNotes.size.toString, BadgeTone.Neutral),
                    ),
                    if ord.internalNotes.isEmpty then
                      p(state.i18n.t("adm-od-notes-empty")).style(smallText.margin(0.px))
                    else
                      div.style(stackSm)(
                        ord.internalNotes.map { n =>
                          div.style(Style.column.gap(3.px).padding(10.px, 12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                            div.style(rowBetween)(
                              span(n.authorName.map(_.value).getOrElse(n.authorId.value)).style(Style.fontWeight(FontWeight.w700).fontSize(12.px).color(ink)),
                              span(formatInstant(n.createdAt)).style(smallText),
                            ),
                            span(n.text.value).style(Style.fontSize(13.px).color(inkSecondary)),
                          )
                        }*
                      ),
                    div.style(rowSm)(
                      input
                        .cssClass("mp-input")
                        .placeholder(if lang == Language.Cs then "Interní poznámka pro tým..." else "Write an internal staff note...")
                        .value(internalNoteRef)
                        .onInput(v => internalNoteRef.set(v)),
                      button
                        .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                        .style(Style.flexShrink(0.0))
                        .onClick(
                          internalNoteRef.get.flatMap(t => state.addOrderNote(ord.id, t).andThen(internalNoteRef.set("")))
                        )(state.i18n.t("adm-od-notes-add")),
                    ),
                  ),

                  // Status Audit Trail Timeline
                  div.cssClass("mp-card")(
                    h2(state.i18n.t("adm-od-timeline-card")).style(sectionTitle.margin(0.px)),
                    div.cssClass("mp-timeline")(
                      ord.statusHistory.map { ev =>
                        val actorText = ev.actor match
                          case OrderActor.System                 => "System"
                          case OrderActor.Customer(_, name)      => s"Customer: ${name.value}"
                          case OrderActor.Employee(id, nameOpt)  => s"Staff: ${nameOpt.map(_.value).getOrElse(id.value)}"
                        div.style(stackXs)(
                          div.style(rowSm.flexWrap(FlexWrap.wrap))(
                            ev.from.map(f => span(s"${orderStatusLabel(f, lang)} → ").style(smallText)).getOrElse(UI.empty),
                            badge(orderStatusLabel(ev.to, lang), orderStatusTone(ev.to)),
                          ),
                          div.style(smallText)(s"$actorText · ${formatInstant(ev.occurredAt)}"),
                          ev.note.map(n => div.style(Style.fontSize(12.px).color(inkSecondary))(s"“$n”")).getOrElse(UI.empty),
                        )
                      }*
                    ),
                  ),
                ),
              ),
            )
      }

  private def infoRow(labelText: String, valueText: String): UI =
    div.style(Style.column.gap(2.px))(
      span(labelText).style(smallText.fontWeight(FontWeight.w700)),
      span(valueText).style(Style.fontSize(13.px).color(ink)),
    )

  private def totalRow(labelNode: UI, amountText: String, valueColor: Color = ink): UI =
    div.style(rowBetween.fontSize(13.px))(
      span(labelNode).style(Style.color(inkSecondary)),
      span(amountText).style(Style.fontWeight(FontWeight.w700).color(valueColor)),
    )
