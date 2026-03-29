package mpbuilder.ui.orders

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*

/** Compact customer selection bar shown at the top of the order entry form.
  *
  * Provides a searchable dropdown to select a customer. Once selected,
  * shows customer name, tier badge, and discount summary with a clear button.
  */
object CustomerSelectionBar:

  def apply(): HtmlElement =
    val searchVar = Var("")
    val dropdownOpen = Var(false)

    div(
      cls := "order-customer-bar",

      child <-- InternalOrderEntryViewModel.selectedCustomer.map {
        case Some(customer) => selectedCustomerDisplay(customer)
        case None           => customerSearch(searchVar, dropdownOpen)
      },
    )

  private def customerSearch(searchVar: Var[String], dropdownOpen: Var[Boolean]): HtmlElement =
    val filteredCustomers: Signal[List[Customer]] =
      InternalOrderEntryViewModel.allCustomers.combineWith(searchVar.signal).map { case (custs, query) =>
        val q = query.trim.toLowerCase
        if q.isEmpty then custs.filter(_.status == CustomerStatus.Active)
        else custs.filter { c =>
          (c.status == CustomerStatus.Active) && (
            c.companyInfo.exists(_.companyName.toLowerCase.contains(q)) ||
            c.companyInfo.exists(_.businessId.toLowerCase.contains(q)) ||
            c.contactInfo.email.toLowerCase.contains(q) ||
            s"${c.contactInfo.firstName} ${c.contactInfo.lastName}".toLowerCase.contains(q)
          )
        }
      }

    div(
      cls := "order-customer-search",
      span(cls := "order-customer-label", "👤 Customer:"),
      div(
        cls := "order-customer-dropdown-wrapper",
        input(
          cls := "order-customer-input",
          placeholder := "Search customer by name, IČO, or email…",
          controlled(
            value <-- searchVar.signal,
            onInput.mapToValue --> searchVar.writer,
          ),
          onFocus --> { _ => dropdownOpen.set(true) },
          onBlur --> { _ => dropdownOpen.set(false) },
        ),
        child <-- dropdownOpen.signal.combineWith(filteredCustomers).map { case (open, custs) =>
          if open && custs.nonEmpty then
            div(
              cls := "order-customer-dropdown",
              custs.take(8).map { customer =>
                div(
                  cls := "order-customer-option",
                  div(
                    cls := "order-customer-option-main",
                    span(cls := "order-customer-option-name",
                      customer.companyInfo.map(_.companyName).getOrElse(
                        s"${customer.contactInfo.firstName} ${customer.contactInfo.lastName}"
                      ),
                    ),
                    span(cls := s"badge badge-${customer.tier.toString.toLowerCase}", customer.tier.toString),
                  ),
                  div(
                    cls := "order-customer-option-detail",
                    span(customer.contactInfo.email),
                    customer.companyInfo.map(ci => span(s" · IČO: ${ci.businessId}")).getOrElse(emptyNode),
                  ),
                  onMouseDown.preventDefault --> { _ =>
                    InternalOrderEntryViewModel.selectCustomer(customer)
                    searchVar.set("")
                    dropdownOpen.set(false)
                  },
                )
              },
            )
          else emptyNode
        },
      ),
    )

  private def selectedCustomerDisplay(customer: Customer): HtmlElement =
    val discountSummary = customer.pricing.globalDiscount match
      case Some(pct) => s"${pct.value}% global discount"
      case None      => "No global discount"

    div(
      cls := "order-customer-selected",
      span(cls := "order-customer-label", "👤 Customer:"),
      div(
        cls := "order-customer-info",
        span(cls := "order-customer-name",
          customer.companyInfo.map(_.companyName).getOrElse(
            s"${customer.contactInfo.firstName} ${customer.contactInfo.lastName}"
          ),
        ),
        span(cls := s"badge badge-${customer.tier.toString.toLowerCase}", customer.tier.toString),
        span(cls := "order-customer-discount", discountSummary),
        span(cls := "order-customer-contact", customer.contactInfo.email),
      ),
      button(
        cls := "btn btn-sm btn-danger",
        "✕",
        onClick --> { _ => InternalOrderEntryViewModel.clearCustomer() },
      ),
    )
