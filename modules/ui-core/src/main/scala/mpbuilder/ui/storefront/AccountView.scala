package mpbuilder.ui.storefront

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.artwork.*
import mpbuilder.customers.*
import mpbuilder.ordering.*
import mpbuilder.shared.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Customer Account & Order History page displaying:
  *   - Organization & Billing Profile with color-coded tier badge
  *   - Editable Billing Address & Multiple Delivery Addresses (with default selection)
  *   - Order History with 5-step visual progress bar, separate Payment Status badge,
  *     "View Payment Options" modal for unpaid orders, and Artwork Preflight status + Re-upload
  */
object AccountView:

  private final case class AddrDraft(
      street: String,
      city: String,
      postalCode: String,
      countryCode: String,
  ):
    def toAddress: Option[Address] =
      for
        s <- Option.when(street.trim.nonEmpty)(NonEmptyString.unsafe(street.trim))
        c <- Option.when(city.trim.nonEmpty)(NonEmptyString.unsafe(city.trim))
        p <- Option.when(postalCode.trim.nonEmpty)(NonEmptyString.unsafe(postalCode.trim))
        k <- Option.when(countryCode.trim.nonEmpty)(NonEmptyString.unsafe(countryCode.trim))
      yield Address(s, c, p, k)

  private object AddrDraft:
    def from(a: Address): AddrDraft =
      AddrDraft(a.street.value, a.city.value, a.postalCode.value, a.countryCode.value)
    val empty: AddrDraft = AddrDraft("", "", "", "CZ")

  private final case class AddressEditorState(
      editing: Boolean = false,
      billing: AddrDraft = AddrDraft.empty,
      deliveries: List[AddrDraft] = Nil,
      defaultIndex: Option[Int] = None,
      error: Option[String] = None,
  )

  def render(state: StorefrontState): UI < Sync =
    for addrEditor <- Signal.initRef(AddressEditorState())
    yield
      val combined =
        state.currentCustomer
          .combineLatest(state.orders)
          .combineLatest(state.orderArtworks)
          .combineLatest(state.paymentModal)
          .combineLatest(addrEditor)
          .combineLatest(state.i18n.language)

      combined.render { case (((((custOpt, orders), artworksMap), payModalOpt), editor), lang) =>
        val isCs = lang == Language.Cs
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

              // 1. Organization & Billing Profile
              div.cssClass("mp-card")(
                div.style(rowBetween)(
                  h2(state.i18n.t("account-profile-title")).style(sectionTitle.margin(0.px)),
                  div.style(Style.row.gap(6.px))(
                    badge(cust.segment.toString, BadgeTone.Neutral),
                    tierBadge(cust.tier),
                  ),
                ),
                div.cssClass("mp-field-row")(
                  profileField(
                    if isCs then "Kontakt" else "Contact",
                    s"${cust.contact.firstName.value} ${cust.contact.lastName.value} · ${cust.contact.email.value}",
                  ),
                  cust.company match
                    case Some(comp) =>
                      profileField(
                        if isCs then "Společnost" else "Company",
                        s"${comp.legalName.value} (IČO ${comp.registrationNumber.value}${comp.vatId.map(v => s", DIČ ${v.value}").getOrElse("")})",
                      )
                    case None => UI.empty,
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

              // 2. Billing & Multiple Delivery Addresses (Editable)
              renderAddressesCard(state, cust, editor, addrEditor, isCs),

              // 3. Order History with Visual Stepper, Decoupled Payment & Artwork Preflight
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
                  div.style(Style.column.gap(14.px))(
                    orders.map { order =>
                      renderOrderCard(state, order, artworksMap.getOrElse(order.id, Nil), lang)
                    }*
                  ),
              ),

              // 4. Payment Options Modal
              payModalOpt match
                case Some((ord, instr)) => renderPaymentModal(state, ord, instr, lang)
                case None               => UI.empty,
            )
      }

  private def renderAddressesCard(
      state: StorefrontState,
      cust: CustomerView,
      editor: AddressEditorState,
      editorRef: SignalRef[AddressEditorState],
      isCs: Boolean,
  ): UI =
    div.cssClass("mp-card")(
      div.style(rowBetween)(
        h2(state.i18n.t("account-addresses-title")).style(sectionTitle.margin(0.px)),
        if !editor.editing then
          button
            .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
            .onClick(
              editorRef.set(
                AddressEditorState(
                  editing = true,
                  billing = AddrDraft.from(cust.billingAddress),
                  deliveries = cust.shippingAddresses.map(AddrDraft.from),
                  defaultIndex = cust.defaultShippingIndex,
                )
              )
            )(state.i18n.t("account-edit-addresses"))
        else
          div.style(rowXs)(
            button
              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
              .onClick(editorRef.set(AddressEditorState()))(state.i18n.t("account-cancel-edit")),
            button
              .cssClass("mp-btn mp-btn-primary mp-btn-sm")
              .onClick {
                (editor.billing.toAddress, editor.deliveries.map(_.toAddress)) match
                  case (Some(bill), delivOpts) if delivOpts.forall(_.isDefined) =>
                    val delivs = delivOpts.flatten
                    val defIdx = editor.defaultIndex.filter(i => i >= 0 && i < delivs.size).orElse(Option.when(delivs.nonEmpty)(0))
                    for
                      _ <- state.updateMyAddresses(bill, delivs, defIdx)
                      _ <- editorRef.set(AddressEditorState())
                    yield ()
                  case _ =>
                    editorRef.updateAndGet(_.copy(error = Some(if isCs then "Vyplňte všechna pole adresy." else "Please fill in all address fields."))).unit
              }(state.i18n.t("account-save-addresses")),
          ),
      ),
      if !editor.editing then
        div.style(stackMd)(
          profileField(
            if isCs then "Fakturační adresa" else "Billing Address",
            formatAddress(cust.billingAddress),
          ),
          div.style(stackSm)(
            span(state.i18n.t("account-delivery-addresses")).style(smallText.fontWeight(FontWeight.w700)),
            if cust.shippingAddresses.isEmpty then
              span(if isCs then "Stejná jako fakturační adresa" else "Same as billing address").style(mutedText)
            else
              div.style(stackSm)(
                cust.shippingAddresses.zipWithIndex.map { case (addr, idx) =>
                  val isDef = cust.defaultShippingIndex.contains(idx) || (cust.defaultShippingIndex.isEmpty && idx == 0)
                  div.style(
                    Style.row
                      .justify(Justification.spaceBetween)
                      .align(Alignment.center)
                      .gap(8.px)
                      .padding(10.px, 12.px)
                      .rounded(10.px)
                      .bg(if isDef then brandSoft else surfaceAlt)
                      .border(1.px, if isDef then brandBorder else Palette.border),
                  )(
                    span(formatAddress(addr)).style(Style.fontSize(13.px).fontWeight(FontWeight.w600).color(ink)),
                    if isDef then badge(if isCs then "Výchozí" else "Default", BadgeTone.Brand) else UI.empty,
                  )
                }*
              ),
          ),
        )
      else
        div.style(stackMd)(
          editor.error match
            case Some(err) => alertBanner(BadgeTone.Rose, span(err), Nil)
            case None      => UI.empty,
          // Billing editor
          div.style(stackSm.padding(12.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
            span(state.i18n.t("account-billing-address")).style(labelStyle),
            addressInputs(
              editor.billing,
              isCs,
              next => editorRef.updateAndGet(_.copy(billing = next, error = None)).unit,
            ),
          ),
          // Multiple delivery addresses editor
          div.style(stackSm)(
            div.style(rowBetween)(
              span(state.i18n.t("account-delivery-addresses")).style(labelStyle),
              button
                .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                .onClick(
                  editorRef
                    .updateAndGet(cur =>
                      cur.copy(
                        deliveries = cur.deliveries :+ AddrDraft.empty,
                        defaultIndex = cur.defaultIndex.orElse(Some(0)),
                      )
                    )
                    .unit
                )(state.i18n.t("account-add-delivery")),
            ),
            if editor.deliveries.isEmpty then
              span(if isCs then "Žádné uložené doručovací adresy." else "No saved delivery addresses yet.").style(mutedText)
            else
              div.style(stackSm)(
                editor.deliveries.zipWithIndex.map { case (draft, idx) =>
                  val isDef = editor.defaultIndex.contains(idx)
                  div.style(
                    Style.column
                      .gap(8.px)
                      .padding(12.px)
                      .rounded(10.px)
                      .bg(if isDef then brandSoft else surfaceAlt)
                      .border(1.px, if isDef then brandBorder else Palette.border),
                  )(
                    div.style(rowBetween)(
                      div.style(rowXs)(
                        span(s"#${idx + 1}").style(Style.fontWeight(FontWeight.w700).fontSize(13.px)),
                        if isDef then badge(if isCs then "Výchozí" else "Default", BadgeTone.Brand)
                        else
                          button
                            .cssClass("mp-btn mp-btn-ghost mp-btn-sm")
                            .onClick(editorRef.updateAndGet(_.copy(defaultIndex = Some(idx))).unit)(
                              state.i18n.t("account-set-default"),
                            ),
                      ),
                      button
                        .cssClass("mp-btn mp-btn-danger mp-btn-sm")
                        .onClick(
                          editorRef
                            .updateAndGet { cur =>
                              val remaining = cur.deliveries.patch(idx, Nil, 1)
                              val nextDef = cur.defaultIndex match
                                case Some(d) if d == idx => Option.when(remaining.nonEmpty)(0)
                                case Some(d) if d > idx  => Some(d - 1)
                                case other               => other
                              cur.copy(deliveries = remaining, defaultIndex = nextDef)
                            }
                            .unit
                        )(state.i18n.t("account-remove-address")),
                    ),
                    addressInputs(
                      draft,
                      isCs,
                      next =>
                        editorRef
                          .updateAndGet(cur => cur.copy(deliveries = cur.deliveries.updated(idx, next), error = None))
                          .unit,
                    ),
                  )
                }*
              ),
          ),
        ),
    )

  private def addressInputs(draft: AddrDraft, isCs: Boolean, onChange: AddrDraft => Unit < Async): UI =
    div.cssClass("mp-field-row")(
      div.cssClass("mp-field")(
        label(if isCs then "Ulice a č. p." else "Street").style(smallText),
        input.cssClass("mp-input").value(draft.street).onInput(v => onChange(draft.copy(street = v))),
      ),
      div.cssClass("mp-field")(
        label(if isCs then "Město" else "City").style(smallText),
        input.cssClass("mp-input").value(draft.city).onInput(v => onChange(draft.copy(city = v))),
      ),
      div.cssClass("mp-field").style(Style.maxWidth(140.px))(
        label(if isCs then "PSČ" else "Postal Code").style(smallText),
        input.cssClass("mp-input").value(draft.postalCode).onInput(v => onChange(draft.copy(postalCode = v))),
      ),
      div.cssClass("mp-field").style(Style.maxWidth(96.px))(
        label(if isCs then "Země" else "Country").style(smallText),
        input.cssClass("mp-input").value(draft.countryCode).onInput(v => onChange(draft.copy(countryCode = v))),
      ),
    )

  private def renderOrderCard(
      state: StorefrontState,
      order: OrderView,
      artworks: List[ArtworkSubmissionView],
      lang: Language,
  ): UI =
    val isCs       = lang == Language.Cs
    val statusTone = orderStatusTone(order.status)

    div.style(Style.column.gap(12.px).padding(16.px).rounded(12.px).border(1.px, Palette.border).bg(surfaceAlt))(
      // Top header row: Order number, Stage badge, Decoupled Payment badge, Total + Payment options button
      div.style(rowBetween.flexWrap(FlexWrap.wrap))(
        div.style(Style.row.align(Alignment.center).gap(8.px).flexWrap(FlexWrap.wrap))(
          span(order.orderNumber.value).style(Style.fontWeight(FontWeight.w800).fontSize(15.px).color(ink)),
          badge(orderStatusLabel(order.status, lang), statusTone),
          paymentStatusBadge(order.payment.status, lang),
          badge(order.payment.method.toString, BadgeTone.Neutral),
        ),
        div.style(Style.row.align(Alignment.center).gap(10.px).flexWrap(FlexWrap.wrap))(
          if !order.isPaid && !order.status.isTerminal then
            button
              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
              .onClick(state.openPaymentInstructions(order))(state.i18n.t("account-view-payment"))
          else UI.empty,
          span(formatMoney(order.totals.grandTotalGross, order.totals.currency, lang))
            .style(Style.fontWeight(FontWeight.w800).fontSize(17.px).color(ink)),
        ),
      ),

      div.style(rowBetween.flexWrap(FlexWrap.wrap))(
        span(formatInstant(order.placedAt)).style(smallText),
        span(
          s"${if isCs then "Odhadovaný termín" else "Estimated target"}: ${formatInstant(order.calculatedDeadline)}",
        ).style(smallText.fontWeight(FontWeight.w600)),
      ),

      // 5-Step Visual Order Progress Bar
      div.style(Style.padding(8.px, 12.px).rounded(10.px).bg(surface).border(1.px, Palette.border))(
        orderProgressStepper(order.status, lang),
      ),

      // Order Lines & Per-Line Print Artwork (upload/replace before acceptance or on changes requested)
      div.style(Style.column.gap(10.px).padding(10.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
        div.style(rowBetween.flexWrap(FlexWrap.wrap))(
          span(state.i18n.t("account-artwork-title")).style(smallText.fontWeight(FontWeight.w800)),
          if !order.status.allowsCustomerArtworkUpload then
            badge(
              s"🔒 ${if isCs then "Objednávka přijata — nahrávání uzamčeno" else "Order accepted — uploads locked"}",
              BadgeTone.Neutral,
            )
          else UI.empty,
        ),
        div.style(stackSm)(
          order.lines.toList.zipWithIndex.map { case (ln, lineIdx) =>
            val lineArts = artworks.filter { a =>
              a.owner match
                case ArtworkOwner.OrderLineRef(oid, idx) => oid == order.id && idx == lineIdx
                case _                                   => false
            }
            val artsByComp = lineArts.map(a => a.componentKey -> a).toMap
            val printableComps = ln.product.components.toList.filter(_.printingMethodId.isDefined)
            val fallbackComps  = if printableComps.nonEmpty then printableComps else ln.product.components.toList.take(1)

            div.style(Style.column.gap(8.px).padding(12.px).rounded(10.px).bg(surface).border(1.px, Palette.border))(
              div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                div.style(stackXs)(
                  span(
                    s"${ln.copies.value}× ${ln.product.categoryName(lang)} (${ln.runQuantity.value} pcs, ${speedLabel(ln.speed, lang)})",
                  ).style(Style.fontSize(13.px).fontWeight(FontWeight.w700).color(ink)),
                  span(
                    ln.product.components.toList
                      .map(c => s"${c.materialName(lang)} · ${formatDimension(c.finishedSize)}")
                      .mkString(" / "),
                  ).style(smallText),
                ),
                span(formatMoney(ln.lineGrossTotal, ln.price.currency, lang)).style(Style.fontWeight(FontWeight.w700).fontSize(14.px)),
              ),

              // Component artwork slots for this order line
              div.style(stackSm)(
                fallbackComps.map { comp =>
                  val expectedTrim  = comp.finishedSize
                  val expectedPages = None
                  artsByComp.get(comp.key) match
                    case Some(art) =>
                      val rev = art.latestRevision
                      val needsReupload = art.status match
                        case ArtworkStatus.ChangesRequested(_) | ArtworkStatus.PreflightFailed => true
                        case _                                                                 => false
                      val canUploadOrReplace = order.status.allowsCustomerArtworkUpload || needsReupload
                      val noteOpt = art.status match
                        case ArtworkStatus.ChangesRequested(n) => Some(n.value)
                        case ArtworkStatus.Rejected(r)         => Some(r.value)
                        case _                                 => None
                      val ctxTitle = Some(s"${order.orderNumber.value} · ${ln.product.categoryName(lang)}")

                      div.style(stackSm.padding(10.px).rounded(10.px).bg(surfaceAlt).border(1.px, Palette.border))(
                        div.style(rowBetween.flexWrap(FlexWrap.wrap))(
                          div.style(Style.row.align(Alignment.center).gap(10.px).minWidth(0.px).flexGrow(1.0))(
                            mpbuilder.ui.components.ArtworkComponents.renderThumbnail(
                              rev,
                              sizePx = 48,
                              onClick = Some(() => state.artworkPreview.open(art, contextTitle = ctxTitle)),
                            ),
                            div.style(stackXs.minWidth(0.px))(
                              div.style(rowXs.flexWrap(FlexWrap.wrap))(
                                span(s"${rev.file.fileName.value} (v${art.revisionCount})")
                                  .style(Style.fontSize(13.px).fontWeight(FontWeight.w700).color(ink)),
                                artworkStatusBadge(art.status, lang),
                              ),
                              div.style(rowXs.flexWrap(FlexWrap.wrap))(
                                span(mpbuilder.ui.components.ArtworkComponents.formatFileSize(rev.file.sizeBytes)).style(smallText),
                                badge(s"${rev.extractedSpecs.resolutionDpi} DPI", BadgeTone.Neutral),
                                badge(rev.extractedSpecs.colorSpace.toString, BadgeTone.Brand),
                                span(formatInstant(rev.uploadedAt)).style(smallText),
                              ),
                            ),
                          ),
                          div.style(rowXs.flexWrap(FlexWrap.wrap))(
                            button
                              .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                              .onClick(state.artworkPreview.open(art, contextTitle = ctxTitle))(
                                state.i18n.t("cfg-artwork-preview"),
                              ),
                            mpbuilder.ui.components.ArtworkComponents.downloadButton(rev, lang),
                            mpbuilder.ui.components.ArtworkComponents.copyLinkButton(
                              rev,
                              lang,
                              onCopied = () => state.notifyLinkCopied(rev.file.shareableUrl),
                            ),
                            if needsReupload then
                              button
                                .cssClass("mp-btn mp-btn-primary mp-btn-sm")
                                .onClick(
                                  state.reuploadOrderArtwork(
                                    order.id,
                                    art.id,
                                    NonEmptyString.unsafe(
                                      s"${rev.file.fileName.value.stripSuffix(".pdf")}-v${art.revisionCount + 1}-cmyk.pdf"
                                    ),
                                  )
                                )(state.i18n.t("account-artwork-reupload"))
                            else UI.empty,
                          ),
                        ),
                        noteOpt match
                          case Some(msg) =>
                            div.style(
                              Style.padding(6.px, 10.px).rounded(8.px).bg(amberSoft).color(amberInk).border(1.px, amberBorder).fontSize(12.px),
                            )(s"⚠ $msg")
                          case None => UI.empty,
                        if canUploadOrReplace then
                          mpbuilder.ui.components.ArtworkComponents.dropZone(
                            zoneId = s"acct-replace-${order.id.value}-$lineIdx-${comp.key.value}",
                            expectedTrim = expectedTrim,
                            componentKey = comp.key,
                            lang = lang,
                            onFilePicked = file =>
                              state.reuploadOrderArtworkFile(order.id, art.id, expectedTrim, expectedPages, file),
                            titleOpt = Some(
                              if isCs then s"Nahradit tisková data (${comp.key.value}) — přetáhněte soubor"
                              else s"Replace artwork (${comp.key.value}) — drag & drop or browse"
                            ),
                            compact = true,
                            showDemoQuickAttach = true,
                          )
                        else UI.empty,
                      )

                    case None =>
                      if order.status.allowsCustomerArtworkUpload then
                        mpbuilder.ui.components.ArtworkComponents.dropZone(
                          zoneId = s"acct-upload-${order.id.value}-$lineIdx-${comp.key.value}",
                          expectedTrim = expectedTrim,
                          componentKey = comp.key,
                          lang = lang,
                          onFilePicked = file =>
                            state.uploadOrderLineArtwork(order.id, lineIdx, comp.key, expectedTrim, expectedPages, file),
                          titleOpt = Some(
                            if isCs then s"Nahrát tisková data pro ${comp.key.value} — přetáhněte nebo klikněte"
                            else s"Upload print artwork for ${comp.key.value} — drag & drop or browse"
                          ),
                          compact = true,
                          showDemoQuickAttach = true,
                        )
                      else
                        div.style(
                          Style.row
                            .align(Alignment.center)
                            .gap(8.px)
                            .padding(8.px, 10.px)
                            .rounded(8.px)
                            .bg(surfaceAlt)
                            .border(1.px, Palette.border)
                            .fontSize(12.px)
                            .color(inkMuted),
                        )(
                          span(state.i18n.t("account-artwork-no-files")),
                        )
                }*
              ),
            )
          }*
        ),
      ),
    )

  private def renderPaymentModal(
      state: StorefrontState,
      order: OrderView,
      instr: PaymentInstructions,
      lang: Language,
  ): UI =
    div.cssClass("mp-modal-backdrop")(
      div.cssClass("mp-modal")(
        div.style(rowBetween)(
          div.style(stackXs)(
            h3(state.i18n.t("account-payment-modal-title")).style(Style.margin(0.px).fontSize(18.px).fontWeight(FontWeight.w800).color(ink)),
            span(s"${order.orderNumber.value} · ${formatMoney(order.totals.grandTotalGross, order.totals.currency, lang)}").style(mutedText),
          ),
          button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(state.closePaymentInstructions)("✕"),
        ),
        instr match
          case PaymentInstructions.BankTransferQR(iban, bicOpt, holder, vs, amount, cur, qrPayload) =>
            div.style(stackMd)(
              div.style(Style.row.flexWrap(FlexWrap.wrap).gap(16.px).align(Alignment.center))(
                qrCodeVisual(qrPayload, 148),
                div.style(stackXs.flexGrow(1.0))(
                  profileField(if lang == Language.Cs then "IBAN" else "IBAN", iban.value),
                  bicOpt.fold(UI.empty: UI)(b => profileField("BIC / SWIFT", b.value)),
                  profileField(if lang == Language.Cs then "Majitel účtu" else "Account Holder", holder.value),
                  profileField(if lang == Language.Cs then "Variabilní symbol" else "Variable Symbol", vs.value),
                  profileField(if lang == Language.Cs then "Částka" else "Amount Due", formatMoney(amount, cur, lang)),
                ),
              ),
              div.style(Style.padding(8.px, 10.px).rounded(8.px).bg(surfaceAlt).fontSize(11.px).color(inkSecondary))(qrPayload),
            )
          case PaymentInstructions.InvoiceScheduled(dueDays, ref) =>
            alertBanner(
              BadgeTone.Emerald,
              span(state.i18n.t("confirm-invoice-title")),
              List(
                if lang == Language.Cs then s"Faktura č. ${ref.value} se splatností $dueDays dní."
                else s"Invoice ${ref.value} scheduled with $dueDays-day net terms."
              ),
            )
          case PaymentInstructions.CardPending(_, message) =>
            alertBanner(
              BadgeTone.Brand,
              span(state.i18n.t("confirm-card-title")),
              List(message),
            ),
        div.style(Style.row.justify(Justification.end))(
          button.cssClass("mp-btn mp-btn-primary mp-btn-sm").onClick(state.closePaymentInstructions)(state.i18n.t("account-close")),
        ),
      ),
    )

  private def formatAddress(a: Address): String =
    s"${a.street.value}, ${a.postalCode.value} ${a.city.value}, ${a.countryCode.value}"

  private def profileField(title: String, value: String): UI =
    div.cssClass("mp-field")(
      span(title).style(smallText.fontWeight(FontWeight.w700)),
      span(value).style(Style.fontSize(14.px).fontWeight(FontWeight.w600).color(ink)),
    )
