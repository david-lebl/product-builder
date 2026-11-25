package mpbuilder.ui.calculator

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.catalog.*
import mpbuilder.shared.*
import mpbuilder.storefront.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.configurator.*
import mpbuilder.ui.i18n.AppI18n
import mpbuilder.ui.port.StorefrontClient
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

final case class CalculatorConfig(
    initialLanguage: Language = Language.En,
    initialCurrency: Currency = Currency.CZK,
    initialCategoryId: Option[CategoryId] = None,
    orderEmail: Option[String] = None,
)

/** Standalone embeddable Price Calculator Widget built on the same Kyo UI `ConfiguratorView`. */
object CalculatorView:

  def render(
      client: StorefrontClient,
      i18n: AppI18n,
      config: CalculatorConfig = CalculatorConfig(),
      onCopyText: String => Unit < Sync = _ => Kyo.unit,
      onOpenMailto: (String, String, String) => Unit < Sync = (_, _, _) => Kyo.unit,
  ): UI < Async =
    for
      catalog      <- client.getCatalog
      currencyRef  <- Signal.initRef[Currency](config.initialCurrency)
      noSession    <- Signal.initRef[Option[mpbuilder.identity.SessionToken]](None)
      ctrl         <- ConfiguratorController.init(
                        client = client,
                        catalog = catalog,
                        i18n = i18n,
                        currency = currencyRef,
                        sessionToken = noSession,
                        initialCategoryId = config.initialCategoryId,
                      )
      emailModal   <- Signal.initRef[Option[(DraftConfig, ProductBuilderQuoteResult)]](None)
      recipientRef <- Signal.initRef(config.orderEmail.getOrElse("orders@printcraft.studio"))
      senderName   <- Signal.initRef("")
      senderEmail  <- Signal.initRef("")
      noteRef      <- Signal.initRef("")
      copiedRef    <- Signal.initRef(false)
    yield
      val host = ConfiguratorHost.Calculator(
        orderEmail = config.orderEmail,
        onRequestEmailOrder = (draft, quoteRes) =>
          copiedRef.set(false).andThen(emailModal.set(Some((draft, quoteRes)))),
      )

      def switchLanguage(lang: Language): Unit < Async =
        i18n.setLanguage(lang).andThen(ctrl.recompute)

      div.cssClass("mp-calc-root")(
        // Top Widget Header
        div.style(Style.row.flexWrap(FlexWrap.wrap).justify(Justification.spaceBetween).align(Alignment.center).gap(12.px))(
          div.style(Style.column.gap(2.px))(
            h2(i18n.t("calc-title")).style(Style.margin(0.px).fontSize(20.px).fontWeight(FontWeight.w800).color(ink)),
            span(i18n.t("brand-name")).style(smallText),
          ),
          i18n.language.render { lang =>
            div.style(Style.row.align(Alignment.center).gap(8.px).flexWrap(FlexWrap.wrap))(
              div.style(Style.row.gap(2.px).padding(3.px).rounded(10.px).bg(surface).border(1.px, Palette.border))(
                List(Language.En -> "EN", Language.Cs -> "CS").map { case (l, code) =>
                  button
                    .cssClass(if lang == l then "mp-btn mp-btn-primary mp-btn-sm" else "mp-btn mp-btn-ghost mp-btn-sm")
                    .style(Style.padding(4.px, 8.px).fontSize(12.px))
                    .onClick(switchLanguage(l))(code)
                }*
              ),
            )
          },
        ),

        // Indicative turnaround notice
        div.style(Style.padding(10.px, 14.px).rounded(10.px).bg(brandSoft).border(1.px, brandBorder).color(brandDark).fontSize(13.px))(
          i18n.t("calc-disclaimer"),
        ),

        // Shared Product Configurator
        ConfiguratorView.render(ctrl, host, showHeader = false),

        // Email Order Modal
        emailModal
          .combineLatest(i18n.language)
          .combineLatest(copiedRef)
          .render[UI] { case ((modalOpt, lang), copied) =>
            modalOpt match
              case None => UI.empty
              case Some((draft, quoteRes)) =>
                val summaryText = formatEmailSummary(catalog, draft, quoteRes, quoteRes.pricing.effectiveQuote.currency, lang)
                val catName     = catalog.categories.get(draft.categoryId).map(_.name(lang)).getOrElse(draft.categoryId.value)
                val subject     = if lang == Language.Cs then s"Poptávka tisku: $catName (${draft.specs.quantity.value} ks)" else s"Print Quote Request: $catName (${draft.specs.quantity.value} pcs)"

                div.cssClass("mp-modal-backdrop")(
                  div.cssClass("mp-modal")(
                    div.style(rowBetween)(
                      h2(i18n.t("calc-email-title")).style(sectionTitle.margin(0.px)),
                      button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(emailModal.set(None))(i18n.t("nav-close")),
                    ),
                    div.cssClass("mp-field-row")(
                      div.cssClass("mp-field")(
                        label.style(labelStyle)(i18n.t("calc-email-recipient")),
                        emailInput.cssClass("mp-input").value(recipientRef),
                      ),
                      div.cssClass("mp-field")(
                        label.style(labelStyle)(i18n.t("calc-email-your-name")),
                        input.cssClass("mp-input").value(senderName).placeholder("Jan Novák"),
                      ),
                    ),
                    div.cssClass("mp-field")(
                      label.style(labelStyle)(i18n.t("calc-email-your-email")),
                      emailInput.cssClass("mp-input").value(senderEmail).placeholder("jan@example.com"),
                    ),
                    div.cssClass("mp-field")(
                      label.style(labelStyle)(i18n.t("calc-email-note")),
                      textarea.cssClass("mp-input").value(noteRef).style(Style.minHeight(68.px)),
                    ),
                    div.style(Style.column.gap(6.px))(
                      label.style(labelStyle)(i18n.t("calc-email-preview")),
                      pre(summaryText).style(
                        Style.margin(0.px)
                          .padding(12.px)
                          .rounded(10.px)
                          .bg(surfaceAlt)
                          .border(1.px, Palette.border)
                          .fontSize(12.px)
                          .fontFamily(FontFamily.Monospace)
                          .overflowX(Overflow.auto),
                      ),
                    ),
                    div.style(Style.row.flexWrap(FlexWrap.wrap).gap(8.px))(
                      button
                        .cssClass("mp-btn mp-btn-primary")
                        .style(Style.flexGrow(1.0))
                        .onClick {
                          for
                            to   <- recipientRef.get
                            name <- senderName.get
                            from <- senderEmail.get
                            note <- noteRef.get
                            fullBody =
                              val header = List(
                                Option(name.trim).filter(_.nonEmpty).map(n => s"From: $n"),
                                Option(from.trim).filter(_.nonEmpty).map(e => s"Reply-To: $e"),
                                Option(note.trim).filter(_.nonEmpty).map(n => s"Note: $n"),
                              ).flatten
                              if header.isEmpty then summaryText else header.mkString("\n") + "\n\n" + summaryText
                            _ <- onOpenMailto(to.trim, subject, fullBody)
                          yield ()
                        }(i18n.t("calc-email-open-client")),
                      button
                        .cssClass("mp-btn mp-btn-secondary")
                        .onClick(onCopyText(summaryText).andThen(copiedRef.set(true)))(
                          if copied then i18n.t("calc-email-copied") else i18n.t("calc-email-copy"),
                        ),
                    ),
                  ),
                )
          },
      )

  def formatEmailSummary(
      catalog: ProductCatalog,
      draft: DraftConfig,
      res: ProductBuilderQuoteResult,
      currency: Currency,
      lang: Language,
  ): String =
    val prod = res.configuration.product
    val q    = res.pricing.effectiveQuote
    val compLines = prod.components.toList.map { c =>
      val printStr = c.print.map(p => s", ${p.printingMethod.name(lang)} (${p.ink.notation})").getOrElse("")
      val finStr =
        if c.finishes.isEmpty then ""
        else ", " + c.finishes.map(_.finish.name(lang)).mkString(", ")
      s"  - ${c.key.value}: ${c.material.name(lang)}$printStr$finStr"
    }
    val breakdown = res.localizedBreakdown.map(l => s"  - ${l.label}: ${formatMoney(l.total, currency, lang)}")

    (List(
      s"Product: ${prod.categoryName(lang)}",
      s"Dimensions: ${formatDimension(prod.specs.dimensions.finishedBoundingBox)}",
      s"Quantity: ${prod.specs.quantity.value} pcs (Versions: ${prod.specs.versionCount.value})",
      s"Speed: ${speedLabel(draft.speed, lang)}",
      "Components:",
    ) ++ compLines ++ List(
      "Price Breakdown:",
    ) ++ breakdown ++ List(
      s"Net Total: ${formatMoney(q.netTotal, currency, lang)}",
      s"Gross Total (incl. VAT): ${formatMoney(q.grossTotal, currency, lang)}",
    )).mkString("\n")
