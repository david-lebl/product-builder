package mpbuilder.admin.views

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.admin.state.{AdminRoute, BackofficeState}
import mpbuilder.artwork.{ArtworkOwner, ArtworkStatus, CheckOutcome}
import mpbuilder.ordering.*
import mpbuilder.shared.*
import mpbuilder.ui.components.Icons
import mpbuilder.ui.components.Icons.Icon
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.components.{ActionMenu, ArtworkComponents, ConfirmDialog, TabBar}
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Order Detail & Manual Intake Workspace:
  *   - Single-line detail header: Headline on left, separated clearly-labeled Status Proceed button
  *     + collapsed `ActionMenu` dropdown (Payment toggle, Put On Hold, Cancel) on right;
  *     status, payment, priority & deadline badges cleanly underneath
  *   - Embedded mode (Master-Detail side pane): single-column scrollable layout with compact sub-tabs
  *     (Items & Summary | Artwork | Customer & Payment | Notes & History)
  *   - Full-screen mode (`AdminRoute.OrderDetail`): spacious two-column workspace with `← Back` nav
  */
object OrderDetailView:

  private enum DetailTab:
    case Items, Artwork, CustomerPayment, NotesHistory

  private final case class LineEditDraft(
      lineId: OrderLineId,
      runQty: String,
      copies: String,
      speed: ManufacturingSpeed,
      specNote: String,
  )

  def render(state: BackofficeState, embedded: Boolean = false): UI < Sync =
    for
      dialog               <- ConfirmDialog.Handle.make
      actionMenu           <- ActionMenu.Handle.make
      detailTabRef         <- Signal.initRef[DetailTab](DetailTab.Items)
      expandedBreakdownRef <- Signal.initRef[Set[OrderLineId]](Set.empty)
      expandedRevisionsRef <- Signal.initRef[Set[ArtworkId]](Set.empty)
      uploadFixTargetRef   <- Signal.initRef[Option[ArtworkId]](None)
      lineEditRef          <- Signal.initRef[Option[LineEditDraft]](None)
      internalNoteRef      <- Signal.initRef("")
    yield
      val combined =
        state.selectedOrder
          .combineLatest(state.selectedOrderArtwork)
          .combineLatest(expandedBreakdownRef)
          .combineLatest(expandedRevisionsRef)
          .combineLatest(uploadFixTargetRef)
          .combineLatest(lineEditRef)
          .combineLatest(detailTabRef)
          .combineLatest(state.auth)
          .combineLatest(state.i18n.language)

      UI.fragment(
        combined.render { case ((((((((orderOpt, artworks), expandedBreakdowns), expandedRevs), uploadFixTarget), lineEditOpt), activeDetailTab), authOpt), lang) =>
          val isCs      = lang == Language.Cs
          val canIntake = state.canIntake(authOpt)
          val canManage = state.canManage(authOpt)

          orderOpt match
            case None =>
              div.cssClass("mp-card")(
                div.style(rowBetween)(
                  span(if isCs then "Načítání objednávky..." else "Loading order...").style(mutedText),
                  if !embedded then
                    button.cssClass("mp-btn mp-btn-secondary mp-btn-sm").onClick(state.navigate(AdminRoute.Orders))(Icons(Icon.ArrowLeft, 15), span(state.i18n.t("adm-od-back")))
                  else UI.empty,
                ),
              )

            case Some(ord) =>
              val canAccept = ord.status match
                case OrderStatus.Placed | OrderStatus.AwaitingPayment | OrderStatus.Confirmed => true
                case _                                                                        => false
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
              val canEditLines = canIntake && (ord.status match
                case OrderStatus.Placed | OrderStatus.AwaitingPayment | OrderStatus.Confirmed |
                    OrderStatus.Accepted | OrderStatus.OnHold(_) =>
                  true
                case _ => false)

              // Primary separated Status Proceed button (with explicit stage label)
              val primaryProceedBtn: UI =
                if !canIntake then UI.empty
                else if canAccept then
                  button
                    .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                    .style(Style.whiteSpace(WhiteSpace.noWrap))
                    .onClick(
                      dialog.open(
                        ConfirmDialog.Config(
                          title = if isCs then "Přijmout objednávku do výroby" else "Accept Order",
                          message =
                            if isCs then s"Přijmout objednávku ${ord.orderNumber.value}?${if !ord.isPaid then " (Pozor: platba zatím není uhrazena — výroba může pokračovat nezávisle.)" else ""}"
                            else s"Accept order ${ord.orderNumber.value} into production?${if !ord.isPaid then " (Note: payment is still unpaid — production can proceed independently.)" else ""}",
                          confirmLabel = if isCs then "Přijmout" else "Accept Order",
                          cancelLabel = if isCs then "Zrušit" else "Cancel",
                          tone = ConfirmDialog.DialogTone.Primary,
                          reasonLabel = Some(if isCs then "Volitelná poznámka" else "Optional staff note"),
                          reasonPlaceholder = Some(if isCs then "Např. data zkontrolována, předáno tisku..." else "e.g. Verified and queued for press..."),
                          reasonRequired = false,
                          onConfirm = noteOpt => state.acceptOrder(ord.id, noteOpt.getOrElse("")),
                        )
                      )
                    )(if isCs then "Další krok: Přijmout →" else "Next stage: Accept →")
                else if isOnHold then
                  button
                    .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                    .style(Style.whiteSpace(WhiteSpace.noWrap))
                    .onClick(
                      dialog.open(
                        ConfirmDialog.Config(
                          title = if isCs then "Obnovit z pozastavení" else "Resume from Hold",
                          message = if isCs then s"Obnovit objednávku ${ord.orderNumber.value} zpět do zpracování?" else s"Resume order ${ord.orderNumber.value} from hold?",
                          confirmLabel = if isCs then "Obnovit" else "Resume",
                          cancelLabel = if isCs then "Zrušit" else "Cancel",
                          tone = ConfirmDialog.DialogTone.Primary,
                          reasonLabel = Some(if isCs then "Volitelná poznámka" else "Optional staff note"),
                          reasonRequired = false,
                          onConfirm = noteOpt => state.resumeOrderFromHold(ord.id, noteOpt.getOrElse("")),
                        )
                      )
                    )(if isCs then "Obnovit objednávku →" else "Resume Order →")
                else
                  val nextStageOpt: Option[(OrderStatus, String, String)] = ord.status match
                    case OrderStatus.Accepted =>
                      Some((OrderStatus.InProduction, if isCs then "Další krok: Ve výrobě →" else "Next stage: In Production →", if isCs then "Označit: Ve výrobě" else "Mark In Production"))
                    case OrderStatus.InProduction =>
                      Some((OrderStatus.ReadyForDispatch, if isCs then "Další krok: K expedici →" else "Next stage: Ready to Dispatch →", if isCs then "Označit: K expedici" else "Mark Ready for Dispatch"))
                    case OrderStatus.ReadyForDispatch =>
                      Some((OrderStatus.Dispatched, if isCs then "Další krok: Expedováno →" else "Next stage: Dispatched →", if isCs then "Označit: Expedováno" else "Mark Dispatched"))
                    case OrderStatus.Dispatched =>
                      Some((OrderStatus.Completed, if isCs then "Další krok: Dokončeno →" else "Next stage: Completed →", if isCs then "Označit: Dokončeno" else "Mark Completed"))
                    case _ => None

                  nextStageOpt match
                    case Some((target, btnLabel, dialogTitle)) =>
                      button
                        .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                        .style(Style.whiteSpace(WhiteSpace.noWrap))
                        .onClick(
                          dialog.open(
                            ConfirmDialog.Config(
                              title = dialogTitle,
                              message =
                                if isCs then s"Posunout objednávku ${ord.orderNumber.value} do stavu „${orderStatusLabel(target, lang)}“?"
                                else s"Move order ${ord.orderNumber.value} to stage “${orderStatusLabel(target, lang)}”?",
                              confirmLabel = dialogTitle,
                              cancelLabel = if isCs then "Zrušit" else "Cancel",
                              tone = ConfirmDialog.DialogTone.Primary,
                              reasonLabel = Some(if isCs then "Volitelná poznámka" else "Optional note"),
                              reasonRequired = false,
                              onConfirm = noteOpt => state.advanceOrderStage(ord.id, target, noteOpt.getOrElse("")),
                            )
                          )
                        )(btnLabel)
                    case None => UI.empty

              // Secondary & destructive actions collapsed into ActionMenu dropdown
              val paymentMenuItem: List[ActionMenu.Item] =
                if !canManage then Nil
                else if !ord.isPaid then
                  List(
                    ActionMenu.Item(
                      label = span(state.i18n.t("adm-od-confirm-payment")),
                      tone = ActionMenu.ItemTone.Primary,
                      onClick = () =>
                        dialog.open(
                          ConfirmDialog.Config(
                            title = if isCs then "Označit objednávku jako zaplacenou" else "Mark Order as Paid",
                            message = if isCs then s"Potvrdit úhradu objednávky ${ord.orderNumber.value}?" else s"Mark order ${ord.orderNumber.value} as Paid?",
                            confirmLabel = if isCs then "Označit jako zaplaceno" else "Mark Paid",
                            cancelLabel = if isCs then "Zrušit" else "Cancel",
                            tone = ConfirmDialog.DialogTone.Primary,
                            reasonLabel = Some(if isCs then "Volitelná poznámka (např. číslo výpisu)" else "Optional note (e.g. bank ref)"),
                            reasonRequired = false,
                            onConfirm = noteOpt => state.markOrderPaymentStatus(ord.id, PaymentStatus.Confirmed, noteOpt.getOrElse("")),
                          )
                        ),
                    )
                  )
                else
                  List(
                    ActionMenu.Item(
                      label = span(state.i18n.t("adm-od-mark-unpaid")),
                      tone = ActionMenu.ItemTone.Amber,
                      onClick = () =>
                        dialog.open(
                          ConfirmDialog.Config(
                            title = if isCs then "Označit jako nezaplaceno" else "Mark Order as Unpaid",
                            message = if isCs then s"Vrátit stav platby objednávky ${ord.orderNumber.value} na Nezaplaceno?" else s"Revert payment status of ${ord.orderNumber.value} to Unpaid?",
                            confirmLabel = if isCs then "Označit nezaplaceno" else "Mark Unpaid",
                            cancelLabel = if isCs then "Zrušit" else "Cancel",
                            tone = ConfirmDialog.DialogTone.Amber,
                            reasonLabel = Some(if isCs then "Důvod změny" else "Reason"),
                            reasonRequired = false,
                            onConfirm = noteOpt => state.markOrderPaymentStatus(ord.id, PaymentStatus.Pending, noteOpt.getOrElse("")),
                          )
                        ),
                    )
                  )

              val holdMenuItem: List[ActionMenu.Item] =
                if canIntake && canPutOnHold then
                  List(
                    ActionMenu.Item(
                      label = span(state.i18n.t("adm-od-put-hold")),
                      tone = ActionMenu.ItemTone.Amber,
                      dividerBefore = paymentMenuItem.nonEmpty,
                      onClick = () =>
                        dialog.open(
                          ConfirmDialog.Config(
                            title = if isCs then "Pozastavit objednávku" else "Put Order On Hold",
                            message = if isCs then s"Pozastavit zpracování objednávky ${ord.orderNumber.value}?" else s"Put order ${ord.orderNumber.value} on hold?",
                            confirmLabel = if isCs then "Pozastavit" else "Put On Hold",
                            cancelLabel = if isCs then "Zrušit" else "Cancel",
                            tone = ConfirmDialog.DialogTone.Amber,
                            reasonLabel = Some(if isCs then "Důvod pozastavení" else "Hold reason"),
                            reasonPlaceholder = Some(if isCs then "Např. čekání na opravená tisková data..." else "e.g. Waiting for corrected artwork PDF..."),
                            reasonRequired = true,
                            reasonErrorText = if isCs then "Zadejte důvod pozastavení." else "Please enter a hold reason.",
                            onConfirm = reasonOpt => state.putOrderOnHold(ord.id, reasonOpt.getOrElse("")),
                          )
                        ),
                    )
                  )
                else Nil

              val cancelMenuItem: List[ActionMenu.Item] =
                if canIntake && canCancel then
                  List(
                    ActionMenu.Item(
                      label = span(state.i18n.t("adm-od-cancel")),
                      tone = ActionMenu.ItemTone.Danger,
                      dividerBefore = holdMenuItem.isEmpty && paymentMenuItem.nonEmpty,
                      onClick = () =>
                        dialog.open(
                          ConfirmDialog.Config(
                            title = if isCs then "Zrušit objednávku" else "Cancel Order",
                            message = if isCs then s"Opravdu chcete trvale zrušit objednávku ${ord.orderNumber.value}?" else s"Permanently cancel order ${ord.orderNumber.value}?",
                            confirmLabel = if isCs then "Zrušit objednávku" else "Cancel Order",
                            cancelLabel = if isCs then "Zpět" else "Back",
                            tone = ConfirmDialog.DialogTone.Danger,
                            reasonLabel = Some(if isCs then "Důvod zrušení" else "Cancellation reason"),
                            reasonPlaceholder = Some(if isCs then "Např. storno na žádost zákazníka..." else "e.g. Cancelled at customer request..."),
                            reasonRequired = true,
                            reasonErrorText = if isCs then "Zadejte důvod zrušení." else "Please enter a cancellation reason.",
                            onConfirm = reasonOpt => state.cancelOrder(ord.id, reasonOpt.getOrElse("")),
                          )
                        ),
                    )
                  )
                else Nil

              val secondaryMenuItems = paymentMenuItem ++ holdMenuItem ++ cancelMenuItem

              // Reusable section blocks (rendered either in compact single-column tabs for embedded Master-Detail
              // or side-by-side in full-screen OrderDetail route)
              val statusBanners: UI = ord.status match
                case OrderStatus.OnHold(reason) =>
                  alertBanner(BadgeTone.Amber, span(orderStatusLabel(ord.status, lang)), List(reason.value))
                case OrderStatus.Cancelled(reason) =>
                  alertBanner(BadgeTone.Rose, span(orderStatusLabel(ord.status, lang)), List(reason.value))
                case _ => UI.empty

              val summaryStrip: UI =
                div.style(Style.column.gap(6.px).padding(12.px, 14.px).rounded(12.px).bg(surfaceAlt).border(1.px, Palette.border))(
                  div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                    div.style(rowSm.flexWrap(FlexWrap.wrap))(
                      span(state.i18n.t("adm-od-priority-deadline")).style(Style.fontWeight(FontWeight.w700).fontSize(13.px).color(inkSecondary)),
                      span(s"⏱ ${formatInstant(ord.calculatedDeadline)}").style(Style.fontWeight(FontWeight.w800).fontSize(13.px).color(brand)),
                    ),
                    span(formatMoney(ord.totals.grandTotalGross, ord.totals.currency, lang))
                      .style(Style.fontSize(18.px).fontWeight(FontWeight.w800).color(ink)),
                  ),
                  if ord.hasMixedSpeeds then
                    span(state.i18n.t("adm-od-mixed-speeds")).style(Style.fontSize(12.px).fontWeight(FontWeight.w600).color(amberInk))
                  else UI.empty,
                )

              val linesSection: UI =
                div.style(stackSm)(
                  div.style(rowBetween)(
                    h2(state.i18n.t("adm-od-lines-card")).style(sectionTitle.margin(0.px)),
                    badge(ord.lines.size.toString, BadgeTone.Neutral),
                  ),
                      div.style(stackSm)(
                        ord.lines.toList.zipWithIndex.map { case (ln, lineIdx) =>
                          val showBreakdown = expandedBreakdowns.contains(ln.lineId)
                          val isEditingLine = lineEditOpt.exists(_.lineId == ln.lineId)
                          val lineArts = artworks.filter { a =>
                            a.owner match
                              case ArtworkOwner.OrderLineRef(oid, idx) => oid == ord.id && idx == lineIdx
                              case _                                   => false
                          }
                          val lineArtworkBadge: UI =
                            if lineArts.isEmpty then
                              reactiveBadge(state.i18n.t("adm-od-artwork-missing-badge"), BadgeTone.Rose)
                            else if lineArts.forall(_.status.isInstanceOf[ArtworkStatus.OperatorApproved]) then
                              badge(if isCs then s"✓ Data schválena (${lineArts.size})" else s"✓ Artwork Approved (${lineArts.size})", BadgeTone.Emerald)
                            else
                              badge(if isCs then s"⚠ Data ke kontrole (${lineArts.size})" else s"⚠ Artwork Review (${lineArts.size})", BadgeTone.Amber)

                          div.style(Style.column.gap(10.px).padding(14.px).rounded(12.px).bg(surfaceAlt).border(1.px, Palette.border))(
                            // Line header
                            div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                              div.style(rowSm.flexWrap(FlexWrap.wrap))(
                                span(s"#${lineIdx + 1} ${ln.product.categoryName(lang)}").style(Style.fontWeight(FontWeight.w800).fontSize(15.px).color(ink)),
                                badge(s"${ln.runQuantity.value} pcs × ${ln.copies.value}", BadgeTone.Brand),
                                badge(speedLabel(ln.speed, lang), if ln.speed == ManufacturingSpeed.Express then BadgeTone.Rose else BadgeTone.Neutral),
                                lineArtworkBadge,
                                span(s"⏱ ${formatInstant(ln.estimatedDeadline(ord.placedAt))}").style(smallText),
                              ),
                              div.style(rowSm)(
                                span(formatMoney(ln.lineGrossTotal, ln.price.currency, lang))
                                  .style(Style.fontWeight(FontWeight.w800).fontSize(15.px).color(ink)),
                                button
                                  .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                                  .onClick(
                                    expandedBreakdownRef.updateAndGet { cur =>
                                      if cur.contains(ln.lineId) then cur - ln.lineId else cur + ln.lineId
                                    }.unit
                                  )(Icons(if showBreakdown then Icon.ChevronDown else Icon.ChevronRight, 15), span(if isCs then "Rozpis ceny" else "Price Breakdown")),
                                if canEditLines then
                                  button
                                    .cssClass(if isEditingLine then "mp-btn mp-btn-primary mp-btn-sm" else "mp-btn mp-btn-secondary mp-btn-sm")
                                    .onClick(
                                      if isEditingLine then lineEditRef.set(None)
                                      else
                                        lineEditRef.set(
                                          Some(
                                            LineEditDraft(
                                              lineId = ln.lineId,
                                              runQty = ln.runQuantity.value.toString,
                                              copies = ln.copies.value.toString,
                                              speed = ln.speed,
                                              specNote = ln.specNote.getOrElse(""),
                                            )
                                          )
                                        )
                                    )(state.i18n.t("adm-od-line-edit"))
                                else UI.empty,
                              ),
                            ),

                            // Full technical specification per component
                            div.style(stackXs)(
                              ln.product.components.toList.map { comp =>
                                div.style(Style.column.gap(2.px).padding(8.px, 10.px).rounded(8.px).bg(surface).border(1.px, Palette.border))(
                                  div.style(rowWrap)(
                                    span(s"${comp.key.value}: ${comp.materialName(lang)}").style(Style.fontWeight(FontWeight.w700).fontSize(12.px).color(ink)),
                                    comp.printingMethodId.map(pm => badge(pm.value, BadgeTone.Neutral)).getOrElse(UI.empty),
                                    comp.inkNotation.map(inkN => badge(inkN, BadgeTone.Brand)).getOrElse(UI.empty),
                                  ),
                                  span(
                                    s"${if isCs then "Čistý rozměr" else "Finished"}: ${formatDimension(comp.finishedSize)} · " +
                                      s"${if isCs then "Rozložený" else "Flat"}: ${formatDimension(comp.flatSize)} · " +
                                      s"${comp.sheetsUsed} ${if isCs then "archů" else "sheets"}" +
                                      (if comp.finishIds.nonEmpty then s" · ${if isCs then "Zušlechtění" else "Finishes"}: ${comp.finishIds.map(_.value).mkString(", ")}" else ""),
                                  ).style(smallText),
                                )
                              }*
                            ),

                            // Product summary & plain-text staff specification note
                            ln.product.summaryNotes.map(n => div.style(smallText)(n)).getOrElse(UI.empty),
                            ln.specNote match
                              case Some(sn) =>
                                div.style(Style.padding(8.px, 10.px).rounded(8.px).bg(brandSoft).border(1.px, brandBorder).fontSize(12.px).color(brandDark))(
                                  s"📝 ${if isCs then "Doplňující specifikace" else "Spec Note"}: $sn",
                                )
                              case None => UI.empty,

                            // Expandable Price Breakdown
                            if showBreakdown then
                              div.style(Style.column.gap(6.px).padding(10.px, 12.px).rounded(10.px).bg(surface).border(1.px, Palette.border))(
                                span(state.i18n.t("adm-od-line-breakdown")).style(labelStyle),
                                totalRow(span(if isCs then "Jednotková cena za sadu (bez DPH)" else "Per-copy Net"), formatMoney(ln.price.netAmount, ln.price.currency, lang)),
                                totalRow(span(s"${if isCs then "DPH za sadu" else "Per-copy VAT"} (${ln.price.taxRate.value}%)"), formatMoney(ln.price.taxAmount, ln.price.currency, lang)),
                                totalRow(span(if isCs then "Počet sad / kopií" else "Copies"), s"× ${ln.copies.value}"),
                                hr.style(dividerStyle),
                                totalRow(span(if isCs then "Celkem položka bez DPH" else "Line Net Total"), formatMoney(ln.lineNetTotal, ln.price.currency, lang)),
                                totalRow(span(if isCs then "Celkem DPH" else "Line VAT Total"), formatMoney(ln.lineTaxTotal, ln.price.currency, lang)),
                                totalRow(span(if isCs then "Celkem položka s DPH" else "Line Gross Total"), formatMoney(ln.lineGrossTotal, ln.price.currency, lang), brand),
                                span(s"Pricelist: ${ln.price.pricelistId.value} v${ln.price.pricelistVersion.value} · Quote: ${ln.price.quoteId.value}").style(smallText),
                              )
                            else UI.empty,

                            // Inline Edit Line form
                            if isEditingLine && canEditLines then
                              lineEditOpt match
                                case Some(draft) =>
                                  div.style(Style.column.gap(10.px).padding(12.px).rounded(10.px).bg(surface).border(1.px, brandBorder))(
                                    span(state.i18n.t("adm-od-line-edit")).style(labelStyle),
                                    div.cssClass("mp-field-row")(
                                      div.cssClass("mp-field")(
                                        label(if isCs then "Množství (ks)" else "Run Quantity (pcs)").style(smallText),
                                        input.cssClass("mp-input").inputFilter(InputFilter.Digits).value(draft.runQty).onInput(v => lineEditRef.set(Some(draft.copy(runQty = v)))),
                                      ),
                                      div.cssClass("mp-field")(
                                        label(if isCs then "Počet sad (kopií)" else "Copies").style(smallText),
                                        input.cssClass("mp-input").inputFilter(InputFilter.Digits).value(draft.copies).onInput(v => lineEditRef.set(Some(draft.copy(copies = v)))),
                                      ),
                                      div.cssClass("mp-field")(
                                        label(if isCs then "Rychlost výroby" else "Production Speed").style(smallText),
                                        select
                                          .cssClass("mp-input")
                                          .value(draft.speed.toString)
                                          .onChange(v =>
                                            ManufacturingSpeed.values.find(_.toString == v).map(sp => lineEditRef.set(Some(draft.copy(speed = sp)))).getOrElse(Kyo.unit)
                                          )(
                                            ManufacturingSpeed.values.toList.map(s => option.value(s.toString)(speedLabel(s, lang)))*
                                          ),
                                      ),
                                    ),
                                    div.cssClass("mp-field")(
                                      label(state.i18n.t("adm-od-line-spec-note")).style(smallText),
                                      textarea
                                        .cssClass("mp-input")
                                        .style(Style.minHeight(64.px))
                                        .placeholder(if isCs then "Volný text pro výrobu (např. barva Pantone, speciální balení)..." else "Plain-text specification override (e.g. Pantone swatch, special packaging)...")
                                        .value(draft.specNote)
                                        .onInput(v => lineEditRef.set(Some(draft.copy(specNote = v)))),
                                    ),
                                    div.style(rowSm)(
                                      button
                                        .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                                        .onClick {
                                          val qtyOpt = draft.runQty.trim.toIntOption.filter(_ > 0).map(Quantity.unsafe)
                                          val copOpt = draft.copies.trim.toIntOption.filter(_ > 0).map(Quantity.unsafe)
                                          val noteOpt: Option[String] = Option.when(draft.specNote.trim.nonEmpty)(draft.specNote.trim)
                                          val cmd = EditOrderLineCmd(
                                            orderId = ord.id,
                                            lineId = ln.lineId,
                                            runQuantity = qtyOpt,
                                            copies = copOpt,
                                            speed = Some(draft.speed),
                                            specNote = Some(noteOpt),
                                          )
                                          state.editOrderLine(cmd).andThen(lineEditRef.set(None))
                                        }(state.i18n.t("adm-cat-save")),
                                      button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(lineEditRef.set(None))(state.i18n.t("adm-cat-cancel")),
                                    ),
                                  )
                                case None => UI.empty
                            else UI.empty,
                          )
                        }*
                      ),
                )

              // Identify printable line/component slots that are still missing artwork
              val missingArtworkSlots: List[(Int, OrderLine, ComponentSnapshot)] =
                ord.lines.toList.zipWithIndex.flatMap { case (ln, lineIdx) =>
                  val printable = ln.product.components.toList.filter(_.printingMethodId.isDefined)
                  val comps     = if printable.nonEmpty then printable else ln.product.components.toList.take(1)
                  comps.filterNot { comp =>
                    artworks.exists { a =>
                      a.componentKey == comp.key && (a.owner match
                        case ArtworkOwner.OrderLineRef(oid, idx) => oid == ord.id && idx == lineIdx
                        case _                                   => false)
                    }
                  }.map(comp => (lineIdx, ln, comp))
                }

              val artworkSection: UI =
                div.style(stackSm)(
                  div.style(rowBetween)(
                    h2(state.i18n.t("adm-od-artwork-card")).style(sectionTitle.margin(0.px)),
                    div.style(rowXs)(
                      badge(artworks.size.toString, BadgeTone.Neutral),
                      if missingArtworkSlots.nonEmpty then
                        badge(
                          if isCs then s"Chybí: ${missingArtworkSlots.size}" else s"Missing: ${missingArtworkSlots.size}",
                          BadgeTone.Rose,
                        )
                      else UI.empty,
                    ),
                  ),
                  if artworks.isEmpty then
                    p(state.i18n.t("adm-od-artwork-empty")).style(mutedText.margin(0.px))
                  else
                    div.style(stackSm)(
                      artworks.map { art =>
                        val rev = art.latestRevision
                        val hasIssues = rev.preflight.exists(rep =>
                          rep.checks.exists(ch => ch.outcome != CheckOutcome.Passed)
                        )
                        val lineContext: String = art.owner match
                          case ArtworkOwner.OrderLineRef(_, idx) if idx >= 0 && idx < ord.lines.size =>
                            s"#${idx + 1} ${ord.lines(idx).product.categoryName(lang)}"
                          case _ => ord.orderNumber.value
                        val ctxTitle       = Some(s"${ord.orderNumber.value} · $lineContext")
                        val showRevs       = expandedRevs.contains(art.id)
                        val showUploadFix  = uploadFixTarget.contains(art.id)

                        div.style(Style.column.gap(10.px).padding(14.px).rounded(12.px).bg(surfaceAlt).border(1.px, Palette.border))(
                          // Header with thumbnail, file details, status badge, and quick file actions
                          div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                            div.style(Style.row.align(Alignment.center).gap(12.px).minWidth(0.px).flexGrow(1.0))(
                              ArtworkComponents.renderThumbnail(
                                rev,
                                sizePx = 56,
                                onClick = Some(() => state.artworkPreview.open(art, contextTitle = ctxTitle)),
                              ),
                              div.style(stackXs.minWidth(0.px))(
                                div.style(rowXs.flexWrap(FlexWrap.wrap))(
                                  span(s"${rev.file.fileName.value} (v${rev.revisionNumber})")
                                    .style(Style.fontWeight(FontWeight.w700).fontSize(14.px).color(ink)),
                                  artworkStatusBadge(art.status, lang),
                                ),
                                span(s"$lineContext · ${art.componentKey.value} · ${ArtworkComponents.formatFileSize(rev.file.sizeBytes)} · ${formatInstant(rev.uploadedAt)}")
                                  .style(smallText),
                                div.style(rowXs.flexWrap(FlexWrap.wrap))(
                                  badge(s"${rev.extractedSpecs.resolutionDpi} DPI", BadgeTone.Neutral),
                                  badge(formatDimension(rev.extractedSpecs.trimSize), BadgeTone.Neutral),
                                  badge(rev.extractedSpecs.colorSpace.toString, BadgeTone.Brand),
                                  badge(s"${rev.extractedSpecs.bleed.value} mm ${if isCs then "spad." else "bleed"}", BadgeTone.Neutral),
                                  badge(s"${rev.extractedSpecs.pageCount}p", BadgeTone.Neutral),
                                ),
                              ),
                            ),
                            div.style(rowXs.flexWrap(FlexWrap.wrap))(
                              button
                                .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                                .onClick(state.artworkPreview.open(art, contextTitle = ctxTitle))(
                                  state.i18n.t("adm-od-artwork-preview"),
                                ),
                              ArtworkComponents.downloadButton(rev, lang),
                              ArtworkComponents.copyLinkButton(rev, lang, onCopied = () => state.notifyLinkCopied(rev.file.shareableUrl)),
                            ),
                          ),

                          // Preflight checks
                          rev.preflight match
                            case Some(rep) =>
                              div.style(stackXs)(
                                rep.checks.map { ch =>
                                  val (tone, detail) = ch.outcome match
                                    case CheckOutcome.Passed       => (BadgeTone.Emerald, "OK")
                                    case CheckOutcome.Warning(msg) => (BadgeTone.Amber, msg)
                                    case CheckOutcome.Failed(msg)  => (BadgeTone.Rose, msg)
                                  div.style(rowSm.flexWrap(FlexWrap.wrap))(
                                    badge(ch.kind.toString, tone),
                                    span(detail).style(Style.fontSize(12.px).color(inkSecondary)),
                                  )
                                }*
                              )
                            case None => UI.empty,

                          art.status match
                            case ArtworkStatus.ChangesRequested(notes) =>
                              alertBanner(BadgeTone.Amber, span(if isCs then "Požadavek na opravu" else "Re-upload Feedback"), List(notes.value))
                            case ArtworkStatus.Rejected(reason) =>
                              alertBanner(BadgeTone.Rose, span(if isCs then "Důvod zamítnutí" else "Rejection Reason"), List(reason.value))
                            case _ => UI.empty,

                          // Staff review & prepress utility actions
                          div.cssClass("mp-action-bar")(
                            if !hasIssues then
                              button
                                .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                                .onClick(
                                  dialog.open(
                                    ConfirmDialog.Config(
                                      title = if isCs then "Schválit tisková data" else "Approve Artwork",
                                      message = if isCs then s"Schválit soubor „${rev.file.fileName.value}“ pro výrobu?" else s"Approve “${rev.file.fileName.value}” for production?",
                                      confirmLabel = if isCs then "Schválit" else "Approve",
                                      cancelLabel = if isCs then "Zrušit" else "Cancel",
                                      tone = ConfirmDialog.DialogTone.Primary,
                                      onConfirm = _ => state.approveArtwork(ord.id, art.id, overrideFailures = false),
                                    )
                                  )
                                )(state.i18n.t("adm-od-artwork-approve"))
                            else UI.empty,
                            button
                              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                              .style(if hasIssues then Style.bg(amberSoft).color(amberInk).border(1.px, amberBorder) else Style.empty)
                              .onClick(
                                dialog.open(
                                  ConfirmDialog.Config(
                                    title = if isCs then "Schválit s upozorněním (výjimka)" else "Approve with Warning Confirmation",
                                    message =
                                      if isCs then s"Soubor „${rev.file.fileName.value}“ má preflight upozornění/chyby. Potvrďte, že přesto schvalujete data pro tisk."
                                      else s"“${rev.file.fileName.value}” has preflight warnings or failures. Confirm that you want to override and approve it for press anyway.",
                                    confirmLabel = if isCs then "Potvrdit a schválit" else "Confirm & Approve",
                                    cancelLabel = if isCs then "Zrušit" else "Cancel",
                                    tone = ConfirmDialog.DialogTone.Amber,
                                    onConfirm = _ => state.approveArtwork(ord.id, art.id, overrideFailures = true),
                                  )
                                )
                              )(state.i18n.t("adm-od-artwork-override")),
                            button
                              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                              .onClick(
                                dialog.open(
                                  ConfirmDialog.Config(
                                    title = if isCs then "Vyžádat nové nahrání dat" else "Request Artwork Re-upload",
                                    message = if isCs then s"Zákazník uvidí tuto zprávu u objednávky a bude moci nahrát opravený soubor." else s"The customer will see this feedback on their order and be prompted to re-upload a corrected file.",
                                    confirmLabel = if isCs then "Odeslat požadavek" else "Request Re-upload",
                                    cancelLabel = if isCs then "Zrušit" else "Cancel",
                                    tone = ConfirmDialog.DialogTone.Amber,
                                    reasonLabel = Some(if isCs then "Instrukce pro zákazníka" else "Feedback for customer"),
                                    reasonPlaceholder = Some(if isCs then "Např. nahrajte prosím PDF 300 DPI CMYK se spadávkou 3 mm..." else "e.g. Please re-upload at 300 DPI CMYK with 3mm bleed..."),
                                    reasonRequired = true,
                                    reasonErrorText = if isCs then "Popište prosím požadované úpravy." else "Please describe the required fixes.",
                                    onConfirm = msgOpt => state.requestArtworkChanges(ord.id, art.id, msgOpt.getOrElse("")),
                                  )
                                )
                              )(state.i18n.t("adm-od-artwork-request-changes")),
                            button
                              .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                              .onClick(
                                dialog.open(
                                  ConfirmDialog.Config(
                                    title = if isCs then "Zamítnout tisková data" else "Reject Artwork",
                                    message = if isCs then s"Zamítnout soubor „${rev.file.fileName.value}“ jako nepoužitelný pro výrobu?" else s"Reject “${rev.file.fileName.value}” as unsuitable for production?",
                                    confirmLabel = if isCs then "Zamítnout" else "Reject",
                                    cancelLabel = if isCs then "Zrušit" else "Cancel",
                                    tone = ConfirmDialog.DialogTone.Danger,
                                    reasonLabel = Some(if isCs then "Důvod zamítnutí" else "Rejection reason"),
                                    reasonPlaceholder = Some(if isCs then "Např. poškozený soubor / nekompatibilní formát..." else "e.g. Corrupted file / incompatible format..."),
                                    reasonRequired = true,
                                    reasonErrorText = if isCs then "Zadejte důvod zamítnutí." else "Please enter a rejection reason.",
                                    onConfirm = rOpt => state.rejectArtwork(ord.id, art.id, rOpt.getOrElse("")),
                                  )
                                )
                              )(state.i18n.t("adm-od-artwork-reject")),
                            if canIntake then
                              button
                                .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                                .onClick(state.rerunArtworkPreflight(ord.id, art.id))(
                                  state.i18n.t("adm-od-artwork-rerun-preflight"),
                                )
                            else UI.empty,
                            if canIntake then
                              button
                                .cssClass(if showUploadFix then "mp-btn mp-btn-primary mp-btn-sm" else "mp-btn mp-btn-ghost mp-btn-sm")
                                .onClick(uploadFixTargetRef.set(if showUploadFix then None else Some(art.id)))(
                                  state.i18n.t("adm-od-artwork-upload-fix"),
                                )
                            else UI.empty,
                            button
                              .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                              .onClick(
                                expandedRevisionsRef.updateAndGet(cur => if cur.contains(art.id) then cur - art.id else cur + art.id).unit
                              )(
                                Icons(if showRevs then Icon.ChevronDown else Icon.ChevronRight, 15),
                                span(state.i18n.t("adm-od-artwork-revisions", Dict("count" -> art.allRevisions.size.toString))),
                              ),
                          ),

                          // Staff Prepress Fix / New Revision Drag & Drop Zone
                          if showUploadFix && canIntake then
                            div.style(Style.padding(10.px).rounded(10.px).bg(surface).border(1.px, brandBorder))(
                              ArtworkComponents.dropZone(
                                zoneId = s"staff-fix-${ord.id.value}-${art.id.value}",
                                expectedTrim = rev.extractedSpecs.trimSize,
                                componentKey = art.componentKey,
                                lang = lang,
                                onFilePicked = file =>
                                  state.uploadStaffArtworkRevision(ord.id, art.id, rev.extractedSpecs.trimSize, file).andThen(uploadFixTargetRef.set(None)),
                                titleOpt = Some(
                                  if isCs then "Přetáhněte opravený tiskový soubor (nová revize)"
                                  else "Drop prepress-fixed artwork file (uploads as new revision)"
                                ),
                                compact = true,
                                showDemoQuickAttach = true,
                              ),
                            )
                          else UI.empty,

                          // Collapsible Full Revision History
                          if showRevs then
                            div.style(Style.column.gap(6.px).padding(10.px).rounded(10.px).bg(surface).border(1.px, Palette.border))(
                              art.allRevisions.reverse.map { r =>
                                div.style(rowBetween.flexWrap(FlexWrap.wrap).padding(6.px, 8.px).rounded(8.px).bg(surfaceAlt))(
                                  div.style(rowXs.flexWrap(FlexWrap.wrap))(
                                    badge(s"v${r.revisionNumber}", if r.revisionNumber == rev.revisionNumber then BadgeTone.Brand else BadgeTone.Neutral),
                                    span(r.file.fileName.value).style(Style.fontSize(12.px).fontWeight(FontWeight.w600).color(ink)),
                                    span(s"${ArtworkComponents.formatFileSize(r.file.sizeBytes)} · ${formatInstant(r.uploadedAt)}").style(smallText),
                                  ),
                                  div.style(rowXs)(
                                    button
                                      .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                                      .onClick(state.artworkPreview.open(art, revisionNumber = Some(r.revisionNumber), contextTitle = ctxTitle))(
                                        state.i18n.t("adm-od-artwork-preview"),
                                      ),
                                    ArtworkComponents.downloadButton(r, lang),
                                  ),
                                )
                              }*
                            )
                          else UI.empty,
                        )
                      }*
                    ),

                  // Missing Artwork Slots — allow staff to attach artwork directly to any line
                  if missingArtworkSlots.nonEmpty && canIntake then
                    div.style(stackSm.padding(10.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
                      missingArtworkSlots.map { case (lineIdx, ln, comp) =>
                        div.style(stackXs.padding(12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                          div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                            span(s"#${lineIdx + 1} ${ln.product.categoryName(lang)} · ${comp.key.value}").style(Style.fontSize(13.px).fontWeight(FontWeight.w700).color(ink)),
                            reactiveBadge(state.i18n.t("adm-od-artwork-missing-badge"), BadgeTone.Rose),
                          ),
                          ArtworkComponents.dropZone(
                            zoneId = s"staff-missing-${ord.id.value}-$lineIdx-${comp.key.value}",
                            expectedTrim = comp.finishedSize,
                            componentKey = comp.key,
                            lang = lang,
                            onFilePicked = file =>
                              state.uploadStaffOrderLineArtwork(ord.id, lineIdx, comp.key, comp.finishedSize, file),
                            titleOpt = Some(
                              if isCs then s"Nahrát data pro #${lineIdx + 1} ${ln.product.categoryName(lang)} (${comp.key.value})"
                              else s"Upload artwork for #${lineIdx + 1} ${ln.product.categoryName(lang)} (${comp.key.value})"
                            ),
                            compact = true,
                            showDemoQuickAttach = true,
                          ),
                        )
                      }*
                    )
                  else UI.empty,
                )

              val paymentSection: UI =
                div.style(stackSm)(
                  div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                    h2(state.i18n.t("adm-od-payment-card")).style(sectionTitle.margin(0.px)),
                    paymentStatusBadge(ord.payment.status, lang),
                  ),
                  span(state.i18n.t("adm-od-payment-b2b-hint")).style(smallText),
                  infoRow(if isCs then "Platební metoda" else "Payment Method", paymentMethodLabel(ord.payment.method, lang)),
                  ord.payment.externalReference.map(r => infoRow(if isCs then "Reference" else "Reference", r)).getOrElse(UI.empty),
                  ord.payment.confirmedAt.map(t => infoRow(if isCs then "Potvrzeno dne" else "Confirmed At", formatInstant(t))).getOrElse(UI.empty),
                  if canManage then
                    div.style(rowWrap)(
                      if !ord.isPaid then
                        button
                          .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                          .onClick(
                            dialog.open(
                              ConfirmDialog.Config(
                                title = if isCs then "Označit objednávku jako zaplacenou" else "Mark Order as Paid",
                                message = if isCs then s"Potvrdit úhradu objednávky ${ord.orderNumber.value}?" else s"Mark order ${ord.orderNumber.value} as Paid?",
                                confirmLabel = if isCs then "Označit jako zaplaceno" else "Mark Paid",
                                cancelLabel = if isCs then "Zrušit" else "Cancel",
                                tone = ConfirmDialog.DialogTone.Primary,
                                reasonLabel = Some(if isCs then "Volitelná poznámka (např. číslo výpisu)" else "Optional note (e.g. bank ref)"),
                                reasonRequired = false,
                                onConfirm = noteOpt => state.markOrderPaymentStatus(ord.id, PaymentStatus.Confirmed, noteOpt.getOrElse("")),
                              )
                            )
                          )(state.i18n.t("adm-od-confirm-payment"))
                      else
                        button
                          .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                          .onClick(
                            dialog.open(
                              ConfirmDialog.Config(
                                title = if isCs then "Označit jako nezaplaceno" else "Mark Order as Unpaid",
                                message = if isCs then s"Vrátit stav platby objednávky ${ord.orderNumber.value} na Nezaplaceno?" else s"Revert payment status of ${ord.orderNumber.value} to Unpaid?",
                                confirmLabel = if isCs then "Označit nezaplaceno" else "Mark Unpaid",
                                cancelLabel = if isCs then "Zrušit" else "Cancel",
                                tone = ConfirmDialog.DialogTone.Amber,
                                reasonLabel = Some(if isCs then "Důvod změny" else "Reason"),
                                reasonRequired = false,
                                onConfirm = noteOpt => state.markOrderPaymentStatus(ord.id, PaymentStatus.Pending, noteOpt.getOrElse("")),
                              )
                            )
                          )(state.i18n.t("adm-od-mark-unpaid")),
                    )
                  else UI.empty,
                )

              val customerSection: UI =
                div.style(stackSm)(
                  h2(state.i18n.t("adm-od-customer-card")).style(sectionTitle.margin(0.px)),
                  infoRow(if isCs then "Zákazník" else "Customer", s"${ord.customer.fullName.value} (${ord.customer.segment})"),
                  ord.customer.companyName
                    .map(cn => infoRow(if isCs then "Firma" else "Company", s"${cn.value}${ord.customer.companyRegNo.map(r => s" · IČO ${r.value}").getOrElse("")}${ord.customer.vatId.map(v => s" · DIČ ${v.value}").getOrElse("")}"))
                    .getOrElse(UI.empty),
                  infoRow("Email", ord.customer.email.value),
                  infoRow(
                    if isCs then "Fakturační adresa" else "Billing Address",
                    s"${ord.billingAddress.street.value}, ${ord.billingAddress.postalCode.value} ${ord.billingAddress.city.value}, ${ord.billingAddress.countryCode.value}",
                  ),
                  infoRow(
                    if isCs then "Doručení" else "Delivery",
                    ord.delivery match
                      case DeliverySelection.ShopPickup(locId) => s"Shop Pickup (${locId.value})"
                      case DeliverySelection.Courier(svcId, addr, _, _) =>
                        s"Courier (${svcId.value}) → ${addr.street.value}, ${addr.city.value}",
                  ),
                  ord.customerNote
                    .map(cn => alertBanner(BadgeTone.Brand, state.i18n.t("adm-od-customer-note"), List(cn)))
                    .getOrElse(UI.empty),
                )

              val totalsSection: UI =
                div.style(stackSm)(
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
                )

              val notesSection: UI =
                div.style(stackSm)(
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
                      .placeholder(if isCs then "Interní poznámka pro tým..." else "Write an internal staff note...")
                      .value(internalNoteRef)
                      .onInput(v => internalNoteRef.set(v)),
                    button
                      .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                      .style(Style.flexShrink(0.0))
                      .onClick(
                        internalNoteRef.get.flatMap(t => state.addOrderNote(ord.id, t).andThen(internalNoteRef.set("")))
                      )(state.i18n.t("adm-od-notes-add")),
                  ),
                )

              val timelineSection: UI =
                div.style(stackSm)(
                  h2(state.i18n.t("adm-od-timeline-card")).style(sectionTitle.margin(0.px)),
                  div.cssClass("mp-timeline")(
                    ord.statusHistory.map { ev =>
                      val actorText = ev.actor match
                        case OrderActor.System                => "System"
                        case OrderActor.Customer(_, name)     => s"Customer: ${name.value}"
                        case OrderActor.Employee(id, nameOpt) => s"Staff: ${nameOpt.map(_.value).getOrElse(id.value)}"
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
                )

              // Single-line Detail Header: Headline + separated Status Proceed button + Actions dropdown;
              // Status / Payment / Priority badges cleanly below the headline
              val detailHeader: UI =
                div.cssClass("mp-detail-head")(
                  div.cssClass("mp-detail-head-row")(
                    div.style(rowSm.minWidth(0.px).flexGrow(1.0))(
                      if !embedded then
                        button
                          .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                          .style(Style.flexShrink(0.0))
                          .onClick(state.navigate(AdminRoute.Orders))(Icons(Icon.ArrowLeft, 15), span(state.i18n.t("adm-od-back")))
                      else UI.empty,
                      h1(ord.orderNumber.value).style(
                        Style.margin(0.px).fontSize(if embedded then 19.px else 24.px).fontWeight(FontWeight.w800).color(ink),
                      ),
                    ),
                    if canIntake || canManage then
                      div.cssClass("mp-action-bar")(
                        primaryProceedBtn,
                        actionMenu.render(
                          key = s"ord-${ord.id.value}",
                          buttonLabel = span(if isCs then "Akce" else "Actions"),
                          items = secondaryMenuItems,
                        ),
                      )
                    else
                      badge(if isCs then "Pouze čtení (Prepress)" else "Read-only Intake (Prepress)", BadgeTone.Neutral),
                  ),
                  div.cssClass("mp-detail-tags-row")(
                    badge(orderStatusLabel(ord.status, lang), orderStatusTone(ord.status)),
                    paymentStatusBadge(ord.payment.status, lang),
                    priorityBadge(ord.priority, lang),
                    span(s"· ${ord.customer.companyName.map(_.value).getOrElse(ord.customer.fullName.value)}").style(smallText),
                  ),
                )

              val embeddedTabs = List(
                TabBar.TabItem(DetailTab.Items, if isCs then "Položky" else "Items", Some(ord.lines.size)),
                TabBar.TabItem(DetailTab.Artwork, if isCs then "Tisková data" else "Artwork", Some(artworks.size)),
                TabBar.TabItem(DetailTab.CustomerPayment, if isCs then "Zákazník a platba" else "Customer & Pay", None),
                TabBar.TabItem(
                  DetailTab.NotesHistory,
                  if isCs then "Poznámky a historie" else "Notes & History",
                  Some(ord.internalNotes.size + ord.statusHistory.size),
                ),
              )

              div.style(stackMd.width(100.pct))(
                detailHeader,
                statusBanners,
                summaryStrip,
                if embedded then
                  // Master-Detail side pane: clean single-column scrollable layout with compact sub-tabs
                  div.style(stackMd)(
                    TabBar.render[DetailTab](
                      items = embeddedTabs,
                      active = activeDetailTab,
                      onSelect = t => detailTabRef.set(t),
                      compact = true,
                    ),
                    activeDetailTab match
                      case DetailTab.Items =>
                        div.style(stackMd)(
                          linesSection,
                          div.style(Style.padding(12.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(totalsSection),
                        )
                      case DetailTab.Artwork =>
                        artworkSection
                      case DetailTab.CustomerPayment =>
                        div.style(stackMd)(
                          paymentSection,
                          div.style(Style.padding(12.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(customerSection),
                        )
                      case DetailTab.NotesHistory =>
                        div.style(stackMd)(
                          notesSection,
                          div.style(Style.padding(12.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(timelineSection),
                        ),
                  )
                else
                  // Full-screen dedicated OrderDetail route: spacious 2-column layout with all cards visible
                  div.cssClass("mp-admin-split")(
                    div.cssClass("mp-admin-split-right")(
                      div.cssClass("mp-card")(linesSection),
                      div.cssClass("mp-card")(artworkSection),
                    ),
                    div.cssClass("mp-admin-split-left")(
                      div.cssClass("mp-card")(paymentSection),
                      div.cssClass("mp-card")(customerSection),
                      div.cssClass("mp-card")(totalsSection),
                      div.cssClass("mp-card")(notesSection),
                      div.cssClass("mp-card")(timelineSection),
                    ),
                  ),
              )
        },
        dialog.render,
      )

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
