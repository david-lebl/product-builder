package mpbuilder.ui.customers.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.ui.customers.*
import mpbuilder.uikit.containers.*
import mpbuilder.uikit.form.FormComponents

/** Dedicated customer pricing editor view.
  *
  * Provides a focused interface for editing all customer pricing configuration:
  * - Global discount percentage
  * - Category-level percentage discounts (add/remove)
  * - Material-level percentage discounts (add/remove)
  * - Fixed material prices (add/remove)
  * - Finish-level percentage discounts (add/remove)
  * - Minimum order override
  *
  * Separated from the main customer detail panel to avoid complexity
  * and provide a full-width editing experience.
  */
object CustomerPricingView:

  def apply(): HtmlElement =
    val searchVar = Var("")
    val selectedId: Var[Option[String]] = Var(None)

    val filteredCustomers: Signal[List[Customer]] =
      CustomerManagementViewModel.customers.combineWith(searchVar.signal).map { case (custs, query) =>
        val q = query.trim.toLowerCase
        if q.isEmpty then custs
        else custs.filter { c =>
          c.companyInfo.exists(_.companyName.toLowerCase.contains(q)) ||
          c.contactInfo.email.toLowerCase.contains(q) ||
          s"${c.contactInfo.firstName} ${c.contactInfo.lastName}".toLowerCase.contains(q)
        }
      }

    val tableConfig = SplitTableConfig[Customer](
      columns = List(
        ColumnDef("Company", c => span(
          c.companyInfo.map(_.companyName).getOrElse(s"${c.contactInfo.firstName} ${c.contactInfo.lastName}"),
        ), Some(c => c.companyInfo.map(_.companyName).getOrElse(c.contactInfo.lastName))),
        ColumnDef("Tier", c => tierBadge(c.tier), Some(_.tier.toString), Some("90px")),
        ColumnDef("Global %", c => span(
          c.pricing.globalDiscount.map(d => s"${d.value}%").getOrElse("—"),
        ), width = Some("80px")),
        ColumnDef("Rules", c => span(
          s"${pricingRuleCount(c.pricing)}",
        ), width = Some("60px")),
      ),
      rowKey = _.id.value,
      searchPlaceholder = "Search customers…",
      onRowSelect = Some(c => {
        selectedId.set(Some(c.id.value))
        CustomerManagementViewModel.setEditState(CustomerEditState.EditingCustomer(c.id))
      }),
      emptyMessage = "No customers found.",
    )

    val sidePanel: Signal[Option[HtmlElement]] =
      CustomerManagementViewModel.editState.combineWith(CustomerManagementViewModel.customers).map {
        case (CustomerEditState.EditingCustomer(id), custs) =>
          custs.find(_.id == id).map(c => pricingEditor(c))
        case _ => None
      }

    div(
      cls := "catalog-section",
      h2(cls := "manufacturing-view-title", "Customer Pricing"),
      SplitTableView(
        config = tableConfig,
        items = filteredCustomers,
        selectedKey = selectedId.signal,
        searchQuery = searchVar,
        sidePanel = sidePanel,
      ),
    )

  // ── Pricing editor panel ───────────────────────────────────────────────

  private def pricingEditor(customer: Customer): HtmlElement =
    val globalDiscountVar = Var(customer.pricing.globalDiscount.map(_.value.toString).getOrElse(""))
    val minOrderVar = Var(customer.pricing.minimumOrderOverride.map(_.value.toString).getOrElse(""))

    // Category discount editing state
    val newCatIdVar = Var("")
    val newCatPctVar = Var("")

    // Material discount editing state
    val newMatIdVar = Var("")
    val newMatPctVar = Var("")

    // Fixed material price editing state
    val newFixedMatIdVar = Var("")
    val newFixedMatPriceVar = Var("")

    // Finish discount editing state
    val newFinishIdVar = Var("")
    val newFinishPctVar = Var("")

    div(
      cls := "catalog-detail-panel",

      button(cls := "detail-panel-close", "×", onClick --> { _ =>
        CustomerManagementViewModel.setEditState(CustomerEditState.None)
      }),

      div(cls := "detail-panel-header",
        h3(s"Pricing: ${customer.companyInfo.map(_.companyName).getOrElse(customer.contactInfo.email)}"),
        tierBadge(customer.tier),
      ),

      div(
        cls := "detail-panel-section",

        // ── Global Discount ────────────────────────────────────────────
        FormComponents.sectionHeader("Global Discount"),
        FormComponents.textField("Percentage (%)", globalDiscountVar.signal, globalDiscountVar.writer, "e.g. 10"),
        div(
          cls := "detail-panel-actions",
          FormComponents.actionButton("Update Global Discount", () => {
            val pctStr = globalDiscountVar.now().trim
            val newGlobal = if pctStr.isEmpty then None
              else scala.util.Try(BigDecimal(pctStr)).toOption.map(Percentage.unsafe)
            savePricing(customer, customer.pricing.copy(globalDiscount = newGlobal))
          }),
        ),

        // ── Category Discounts ─────────────────────────────────────────
        FormComponents.sectionHeader("Category Discounts"),
        if customer.pricing.categoryDiscounts.nonEmpty then
          div(
            cls := "pricing-rules-list",
            customer.pricing.categoryDiscounts.toList.map { case (catId, pct) =>
              div(cls := "pricing-rule-row",
                span(cls := "pricing-rule-label", catId.value),
                span(cls := "badge badge-info", s"${pct.value}%"),
                button(cls := "btn btn-sm btn-danger", "✕", onClick --> { _ =>
                  savePricing(customer, customer.pricing.copy(
                    categoryDiscounts = customer.pricing.categoryDiscounts - catId,
                  ))
                }),
              )
            },
          )
        else p(cls := "text-muted", "No category discounts configured."),
        div(
          cls := "pricing-add-row",
          FormComponents.textField("Category ID", newCatIdVar.signal, newCatIdVar.writer, "e.g. business-cards"),
          FormComponents.textField("Discount (%)", newCatPctVar.signal, newCatPctVar.writer, "e.g. 15"),
          FormComponents.actionButton("+ Add", () => {
            val catId = newCatIdVar.now().trim
            val pct = scala.util.Try(BigDecimal(newCatPctVar.now().trim)).toOption
            if catId.nonEmpty && pct.isDefined then
              savePricing(customer, customer.pricing.copy(
                categoryDiscounts = customer.pricing.categoryDiscounts + (CategoryId.unsafe(catId) -> Percentage.unsafe(pct.get)),
              ))
              newCatIdVar.set("")
              newCatPctVar.set("")
          }),
        ),

        // ── Material Discounts ─────────────────────────────────────────
        FormComponents.sectionHeader("Material Discounts"),
        if customer.pricing.materialDiscounts.nonEmpty then
          div(
            cls := "pricing-rules-list",
            customer.pricing.materialDiscounts.toList.map { case (matId, pct) =>
              div(cls := "pricing-rule-row",
                span(cls := "pricing-rule-label", matId.value),
                span(cls := "badge badge-info", s"${pct.value}%"),
                button(cls := "btn btn-sm btn-danger", "✕", onClick --> { _ =>
                  savePricing(customer, customer.pricing.copy(
                    materialDiscounts = customer.pricing.materialDiscounts - matId,
                  ))
                }),
              )
            },
          )
        else p(cls := "text-muted", "No material discounts configured."),
        div(
          cls := "pricing-add-row",
          FormComponents.textField("Material ID", newMatIdVar.signal, newMatIdVar.writer, "e.g. coated-300gsm"),
          FormComponents.textField("Discount (%)", newMatPctVar.signal, newMatPctVar.writer, "e.g. 10"),
          FormComponents.actionButton("+ Add", () => {
            val matId = newMatIdVar.now().trim
            val pct = scala.util.Try(BigDecimal(newMatPctVar.now().trim)).toOption
            if matId.nonEmpty && pct.isDefined then
              savePricing(customer, customer.pricing.copy(
                materialDiscounts = customer.pricing.materialDiscounts + (MaterialId.unsafe(matId) -> Percentage.unsafe(pct.get)),
              ))
              newMatIdVar.set("")
              newMatPctVar.set("")
          }),
        ),

        // ── Fixed Material Prices ──────────────────────────────────────
        FormComponents.sectionHeader("Fixed Material Prices"),
        if customer.pricing.fixedMaterialPrices.nonEmpty then
          div(
            cls := "pricing-rules-list",
            customer.pricing.fixedMaterialPrices.toList.map { case (matId, price) =>
              div(cls := "pricing-rule-row",
                span(cls := "pricing-rule-label", matId.value),
                span(cls := "badge badge-warning", s"${price.amount.value} ${price.currency}"),
                button(cls := "btn btn-sm btn-danger", "✕", onClick --> { _ =>
                  savePricing(customer, customer.pricing.copy(
                    fixedMaterialPrices = customer.pricing.fixedMaterialPrices - matId,
                  ))
                }),
              )
            },
          )
        else p(cls := "text-muted", "No fixed material prices configured."),
        div(
          cls := "pricing-add-row",
          FormComponents.textField("Material ID", newFixedMatIdVar.signal, newFixedMatIdVar.writer, "e.g. coated-300gsm"),
          FormComponents.textField("Price (CZK)", newFixedMatPriceVar.signal, newFixedMatPriceVar.writer, "e.g. 2.50"),
          FormComponents.actionButton("+ Add", () => {
            val matId = newFixedMatIdVar.now().trim
            val priceVal = scala.util.Try(BigDecimal(newFixedMatPriceVar.now().trim)).toOption
            if matId.nonEmpty && priceVal.isDefined then
              savePricing(customer, customer.pricing.copy(
                fixedMaterialPrices = customer.pricing.fixedMaterialPrices +
                  (MaterialId.unsafe(matId) -> Price(Money(priceVal.get), Currency.CZK)),
              ))
              newFixedMatIdVar.set("")
              newFixedMatPriceVar.set("")
          }),
        ),

        // ── Finish Discounts ───────────────────────────────────────────
        FormComponents.sectionHeader("Finish Discounts"),
        if customer.pricing.finishDiscounts.nonEmpty then
          div(
            cls := "pricing-rules-list",
            customer.pricing.finishDiscounts.toList.map { case (finId, pct) =>
              div(cls := "pricing-rule-row",
                span(cls := "pricing-rule-label", finId.value),
                span(cls := "badge badge-info", s"${pct.value}%"),
                button(cls := "btn btn-sm btn-danger", "✕", onClick --> { _ =>
                  savePricing(customer, customer.pricing.copy(
                    finishDiscounts = customer.pricing.finishDiscounts - finId,
                  ))
                }),
              )
            },
          )
        else p(cls := "text-muted", "No finish discounts configured."),
        div(
          cls := "pricing-add-row",
          FormComponents.textField("Finish ID", newFinishIdVar.signal, newFinishIdVar.writer, "e.g. gloss-lamination"),
          FormComponents.textField("Discount (%)", newFinishPctVar.signal, newFinishPctVar.writer, "e.g. 20"),
          FormComponents.actionButton("+ Add", () => {
            val finId = newFinishIdVar.now().trim
            val pct = scala.util.Try(BigDecimal(newFinishPctVar.now().trim)).toOption
            if finId.nonEmpty && pct.isDefined then
              savePricing(customer, customer.pricing.copy(
                finishDiscounts = customer.pricing.finishDiscounts + (FinishId.unsafe(finId) -> Percentage.unsafe(pct.get)),
              ))
              newFinishIdVar.set("")
              newFinishPctVar.set("")
          }),
        ),

        // ── Minimum Order Override ─────────────────────────────────────
        FormComponents.sectionHeader("Minimum Order Override"),
        FormComponents.textField("Minimum Order Value", minOrderVar.signal, minOrderVar.writer, "(leave empty for default)"),
        div(
          cls := "detail-panel-actions",
          FormComponents.actionButton("Update Minimum", () => {
            val minStr = minOrderVar.now().trim
            val newMin = if minStr.isEmpty then None
              else scala.util.Try(BigDecimal(minStr)).toOption.map(Money(_))
            savePricing(customer, customer.pricing.copy(minimumOrderOverride = newMin))
          }),
        ),
      ),
    )

  private def savePricing(customer: Customer, pricing: CustomerPricing): Unit =
    CustomerManagementViewModel.updateCustomerPricing(customer.id, pricing)

  private def pricingRuleCount(p: CustomerPricing): Int =
    p.categoryDiscounts.size + p.materialDiscounts.size +
    p.fixedMaterialPrices.size + p.finishDiscounts.size +
    (if p.globalDiscount.isDefined then 1 else 0)

  private def tierBadge(tier: CustomerTier): HtmlElement =
    val cls_ = tier match
      case CustomerTier.Standard => "badge badge-muted"
      case CustomerTier.Silver   => "badge badge-info"
      case CustomerTier.Gold     => "badge badge-warning"
      case CustomerTier.Platinum => "badge badge-active"
    span(cls := cls_, tier.displayName.value)
