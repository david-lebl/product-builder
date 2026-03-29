package mpbuilder.ui.orders

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*

/** Order summary view — displays order totals, customer info, discount summary,
  * and per-item breakdown with margins.
  */
object OrderSummaryView:

  private val catalog = InternalOrderEntryViewModel.getCatalog

  def apply(): HtmlElement =
    div(
      cls := "order-summary-section",
      h2(cls := "manufacturing-view-title", "Order Summary"),

      // Customer info card
      child <-- InternalOrderEntryViewModel.selectedCustomer.map {
        case Some(customer) => customerCard(customer)
        case None =>
          div(cls := "order-summary-warning", "⚠ No customer selected. Please select a customer first.")
      },

      // Items breakdown
      div(
        cls := "order-summary-items",
        h3("Items"),
        children <-- InternalOrderEntryViewModel.lineItems.map { items =>
          if items.isEmpty then List(div(cls := "order-summary-empty", "No items added."))
          else items.zipWithIndex.map { case (item, idx) => summaryItemRow(item, idx + 1) }
        },
      ),

      // Notes
      div(
        cls := "order-summary-notes",
        h3("Order Notes"),
        textArea(
          cls := "order-notes-textarea",
          placeholder := "Internal notes for this order…",
          controlled(
            value <-- InternalOrderEntryViewModel.state.map(_.orderNotes),
            onInput.mapToValue --> { v => InternalOrderEntryViewModel.setOrderNotes(v) },
          ),
        ),
      ),

      // Totals
      div(
        cls := "order-summary-totals",
        h3("Totals"),
        div(
          cls := "order-summary-totals-grid",
          summaryRow("Subtotal (calculated):",
            InternalOrderEntryViewModel.lineItems.map { items =>
              val calcTotal = items.foldLeft(Money.zero) { (acc, item) =>
                Money(acc.value + item.priceBreakdown.map(_.total).getOrElse(Money.zero).value)
              }
              s"${formatMoney(calcTotal)} CZK"
            },
          ),
          summaryRow("Total (with overrides):",
            InternalOrderEntryViewModel.orderTotal.map(t => s"${formatMoney(t)} CZK"),
          ),
          summaryRow("Estimated Mfg. Cost:",
            InternalOrderEntryViewModel.orderCost.map(c => s"${formatMoney(c)} CZK"),
          ),
          div(
            cls := "order-summary-total-row order-summary-highlight",
            span(cls := "order-summary-total-label", "Margin:"),
            span(cls := "order-summary-total-value order-margin",
              child.text <-- InternalOrderEntryViewModel.orderMargin.combineWith(
                InternalOrderEntryViewModel.orderMarginPercent
              ).map { case (margin, pct) =>
                s"${formatMoney(margin)} CZK (${pct}%)"
              },
            ),
          ),
        ),
      ),

      // Actions
      div(
        cls := "order-summary-actions",
        button(
          cls := "btn btn-primary btn-lg",
          "Create Order",
          disabled <-- InternalOrderEntryViewModel.lineItems.combineWith(
            InternalOrderEntryViewModel.selectedCustomer
          ).map { case (items, cust) =>
            items.isEmpty || cust.isEmpty
          },
          onClick --> { _ =>
            org.scalajs.dom.window.alert("Order created! (placeholder)")
          },
        ),
        button(
          cls := "btn btn-lg",
          "Reset",
          onClick --> { _ => InternalOrderEntryViewModel.resetOrder() },
        ),
      ),
    )

  private def customerCard(customer: Customer): HtmlElement =
    div(
      cls := "order-summary-customer-card",
      div(
        cls := "order-summary-customer-header",
        span(cls := "order-summary-customer-name",
          customer.companyInfo.map(_.companyName).getOrElse(
            s"${customer.contactInfo.firstName} ${customer.contactInfo.lastName}"
          ),
        ),
        span(cls := s"badge badge-${customer.tier.toString.toLowerCase}", customer.tier.toString),
      ),
      div(
        cls := "order-summary-customer-details",
        div(span("Email: "), span(customer.contactInfo.email)),
        customer.companyInfo.map(ci => div(span("IČO: "), span(ci.businessId))).getOrElse(emptyNode),
        customer.companyInfo.flatMap(_.vatId).map(vat => div(span("DIČ: "), span(vat))).getOrElse(emptyNode),
        div(span("Address: "), span(s"${customer.address.street}, ${customer.address.city} ${customer.address.zip}")),
      ),
      customer.pricing.globalDiscount match
        case Some(pct) =>
          div(cls := "order-summary-customer-discount",
            s"Global discount: ${pct.value}%",
            customer.pricing.categoryDiscounts.toList.map { case (catId, pct) =>
              val catName = catalog.categories.get(catId).map(_.name.value).getOrElse(catId.value)
              span(s" · $catName: ${pct.value}%")
            },
          )
        case None => emptyNode
    )

  private def summaryItemRow(item: OrderLineItem, index: Int): HtmlElement =
    val desc = item.description(catalog)
    val calcPrice = item.priceBreakdown.map(bd => formatMoney(bd.total)).getOrElse("—")
    val effPrice = if item.priceBreakdown.isDefined then formatMoney(item.effectivePrice) else "—"
    val cost = if item.priceBreakdown.isDefined then formatMoney(item.estimatedCost) else "—"
    val margin = if item.priceBreakdown.isDefined then s"${formatMoney(item.margin)} (${item.marginPercent}%)" else "—"
    val hasOverride = item.customPriceOverride.isDefined

    div(
      cls := "order-summary-item-row",
      span(cls := "order-summary-item-num", s"$index."),
      span(cls := "order-summary-item-desc", desc),
      span(cls := "order-summary-item-calc",
        if hasOverride then s"$calcPrice → $effPrice" else calcPrice
      ),
      span(cls := "order-summary-item-cost", cost),
      span(cls := s"order-summary-item-margin${item.marginCssClass}", margin),
    )

  private def summaryRow(label: String, valueSignal: Signal[String]): HtmlElement =
    div(
      cls := "order-summary-total-row",
      span(cls := "order-summary-total-label", label),
      span(cls := "order-summary-total-value", child.text <-- valueSignal),
    )

  private def formatMoney(m: Money): String = OrderLineItem.formatMoney(m)
