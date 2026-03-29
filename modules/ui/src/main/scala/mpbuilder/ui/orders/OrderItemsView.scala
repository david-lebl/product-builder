package mpbuilder.ui.orders

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*

/** Compact order items view — the main work area for adding/editing line items.
  *
  * Each line item is a compact row with product category, material, size, quantity,
  * calculated price, custom price override, estimated cost, and margin.
  */
object OrderItemsView:

  private val catalog = InternalOrderEntryViewModel.getCatalog

  def apply(): HtmlElement =
    div(
      cls := "order-items-section",

      // Header with add button
      div(
        cls := "order-items-header",
        h2(cls := "manufacturing-view-title", "Order Items"),
        button(
          cls := "btn btn-primary",
          "+ Add Item",
          onClick --> { _ => InternalOrderEntryViewModel.addLineItem() },
        ),
      ),

      // Line items table header
      div(
        cls := "order-items-table",
        div(
          cls := "order-items-table-header",
          span(cls := "col-category", "Category"),
          span(cls := "col-material", "Material"),
          span(cls := "col-method", "Method"),
          span(cls := "col-size", "Size (W×H)"),
          span(cls := "col-qty", "Qty"),
          span(cls := "col-calc-price", "Calc. Price"),
          span(cls := "col-custom-price", "Custom Price"),
          span(cls := "col-cost", "Est. Cost"),
          span(cls := "col-margin", "Margin"),
          span(cls := "col-actions", ""),
        ),

        // Line items
        children <-- InternalOrderEntryViewModel.lineItems.map { items =>
          if items.isEmpty then
            List(div(
              cls := "order-items-empty",
              "No items yet. Click \"+ Add Item\" to begin.",
            ))
          else items.map(item => lineItemRow(item))
        },
      ),

      // Totals bar
      div(
        cls := "order-totals-bar",
        div(
          cls := "order-totals-row",
          span(cls := "order-totals-label", "Total:"),
          span(cls := "order-totals-value",
            child.text <-- InternalOrderEntryViewModel.orderTotal.map(t => s"${formatMoney(t)} CZK"),
          ),
          span(cls := "order-totals-separator", "|"),
          span(cls := "order-totals-label", "Est. Cost:"),
          span(cls := "order-totals-value",
            child.text <-- InternalOrderEntryViewModel.orderCost.map(c => s"${formatMoney(c)} CZK"),
          ),
          span(cls := "order-totals-separator", "|"),
          span(cls := "order-totals-label", "Margin:"),
          span(cls := "order-totals-value order-margin",
            child.text <-- InternalOrderEntryViewModel.orderMargin.combineWith(
              InternalOrderEntryViewModel.orderMarginPercent
            ).map { case (margin, pct) =>
              s"${formatMoney(margin)} CZK (${pct}%)"
            },
          ),
        ),
      ),
    )

  private def lineItemRow(item: OrderLineItem): HtmlElement =
    val categoryVar = Var(item.categoryId)
    val materialVar = Var(item.materialId)
    val methodVar = Var(item.printingMethodId)
    val widthVar = Var(if item.width > 0 then item.width.toString else "")
    val heightVar = Var(if item.height > 0 then item.height.toString else "")
    val qtyVar = Var(if item.quantity > 0 then item.quantity.toString else "")
    val customPriceVar = Var(item.customPriceOverride.map(_.toString).getOrElse(""))

    def updateItem(): Unit =
      InternalOrderEntryViewModel.updateLineItem(item.lineId, existing =>
        existing.copy(
          categoryId = categoryVar.now(),
          printingMethodId = methodVar.now(),
          materialId = materialVar.now(),
          width = widthVar.now().toIntOption.getOrElse(0),
          height = heightVar.now().toIntOption.getOrElse(0),
          quantity = qtyVar.now().toIntOption.getOrElse(0),
        )
      )

    // Available materials and methods depend on selected category
    val availableMaterials: Signal[List[(MaterialId, Material)]] = categoryVar.signal.map {
      case Some(catId) => InternalOrderEntryViewModel.materialsForCategory(catId)
      case None        => Nil
    }

    val availableMethods: Signal[List[(PrintingMethodId, PrintingMethod)]] = categoryVar.signal.map {
      case Some(catId) => InternalOrderEntryViewModel.printingMethodsForCategory(catId)
      case None        => Nil
    }

    div(
      cls := "order-item-row",

      // Category select
      select(
        cls := "order-field col-category",
        controlled(
          value <-- categoryVar.signal.map(_.map(_.value).getOrElse("")),
          onChange.mapToValue --> { v =>
            val catId = if v.isEmpty then None else Some(CategoryId.unsafe(v))
            categoryVar.set(catId)
            // Reset dependent selections when category changes
            materialVar.set(None)
            methodVar.set(None)
            updateItem()
          },
        ),
        option("— Select —", value := ""),
        InternalOrderEntryViewModel.categoriesSorted.map { case (id, cat) =>
          option(cat.name.value, value := id.value)
        },
      ),

      // Material select
      select(
        cls := "order-field col-material",
        controlled(
          value <-- materialVar.signal.map(_.map(_.value).getOrElse("")),
          onChange.mapToValue --> { v =>
            materialVar.set(if v.isEmpty then None else Some(MaterialId.unsafe(v)))
            updateItem()
          },
        ),
        children <-- availableMaterials.map { mats =>
          option("— Select —", value := "") :: mats.map { case (id, mat) =>
            option(mat.name.value, value := id.value)
          }
        },
      ),

      // Printing method select
      select(
        cls := "order-field col-method",
        controlled(
          value <-- methodVar.signal.map(_.map(_.value).getOrElse("")),
          onChange.mapToValue --> { v =>
            methodVar.set(if v.isEmpty then None else Some(PrintingMethodId.unsafe(v)))
            updateItem()
          },
        ),
        children <-- availableMethods.map { methods =>
          option("— Select —", value := "") :: methods.map { case (id, pm) =>
            option(pm.name.value, value := id.value)
          }
        },
      ),

      // Size inputs (width × height)
      div(
        cls := "order-size-group col-size",
        input(
          cls := "order-size-input",
          placeholder := "W",
          typ := "number",
          controlled(
            value <-- widthVar.signal,
            onInput.mapToValue --> widthVar.writer,
          ),
          onChange --> { _ => updateItem() },
        ),
        span(cls := "order-size-x", "×"),
        input(
          cls := "order-size-input",
          placeholder := "H",
          typ := "number",
          controlled(
            value <-- heightVar.signal,
            onInput.mapToValue --> heightVar.writer,
          ),
          onChange --> { _ => updateItem() },
        ),
      ),

      // Quantity
      input(
        cls := "order-field order-qty-input col-qty",
        placeholder := "Qty",
        typ := "number",
        controlled(
          value <-- qtyVar.signal,
          onInput.mapToValue --> qtyVar.writer,
        ),
        onChange --> { _ => updateItem() },
      ),

      // Calculated price (read-only)
      span(
        cls := "order-field col-calc-price order-price-display",
        item.priceBreakdown match
          case Some(bd) => formatMoney(bd.total)
          case None     => "—"
      ),

      // Custom price override
      input(
        cls := "order-field order-custom-price-input col-custom-price",
        placeholder := "Override",
        typ := "number",
        stepAttr := "0.01",
        controlled(
          value <-- customPriceVar.signal,
          onInput.mapToValue --> customPriceVar.writer,
        ),
        onChange --> { _ =>
          val priceStr = customPriceVar.now().trim
          val price = if priceStr.isEmpty then None
                      else scala.util.Try(BigDecimal(priceStr)).toOption
          InternalOrderEntryViewModel.setCustomPrice(item.lineId, price)
        },
      ),

      // Estimated cost
      span(
        cls := "order-field col-cost order-cost-display",
        item.priceBreakdown match
          case Some(_) => formatMoney(item.estimatedCost)
          case None    => "—"
      ),

      // Margin
      span(
        cls := s"order-field col-margin order-margin-display${item.marginCssClass}",
        item.priceBreakdown match
          case Some(_) =>
            val m = item.margin
            val pct = item.marginPercent
            s"${formatMoney(m)} (${pct}%)"
          case None => "—"
      ),

      // Actions
      div(
        cls := "order-item-actions col-actions",
        button(
          cls := "btn btn-sm",
          title := "Duplicate",
          "⎘",
          onClick --> { _ => InternalOrderEntryViewModel.duplicateLineItem(item.lineId) },
        ),
        button(
          cls := "btn btn-sm btn-danger",
          title := "Remove",
          "✕",
          onClick --> { _ => InternalOrderEntryViewModel.removeLineItem(item.lineId) },
        ),
      ),
    )

  private def formatMoney(m: Money): String = OrderLineItem.formatMoney(m)
