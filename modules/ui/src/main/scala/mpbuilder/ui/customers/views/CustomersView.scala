package mpbuilder.ui.customers.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.ui.customers.*
import mpbuilder.ui.manufacturing.ManufacturingViewModel
import mpbuilder.uikit.containers.*
import mpbuilder.uikit.form.FormComponents

/** Customer list and management view using SplitTableView.
  *
  * Table columns: Company Name, Contact, Tier (badge), Status (badge), Email
  * Filter chips: by tier, by status
  * Search: company name, business ID, email
  * Side panel tabs: Details, Pricing, Notes, Orders
  */
object CustomersView:

  def apply(): HtmlElement =
    val searchVar = Var("")
    val selectedId: Var[Option[String]] = Var(None)
    val tierFilterVar: Var[Set[String]] = Var(Set.empty)
    val statusFilterVar: Var[Set[String]] = Var(Set.empty)

    val filteredCustomers: Signal[List[Customer]] =
      CustomerManagementViewModel.customers
        .combineWith(searchVar.signal, tierFilterVar.signal, statusFilterVar.signal)
        .map { case (custs, query, tierFilter, statusFilter) =>
          val q = query.trim.toLowerCase
          custs
            .filter { c =>
              if tierFilter.isEmpty then true
              else tierFilter.contains(c.tier.toString)
            }
            .filter { c =>
              if statusFilter.isEmpty then true
              else statusFilter.contains(c.status.toString)
            }
            .filter { c =>
              if q.isEmpty then true
              else
                c.companyInfo.exists(_.companyName.toLowerCase.contains(q)) ||
                c.companyInfo.exists(_.businessId.toLowerCase.contains(q)) ||
                c.contactInfo.email.toLowerCase.contains(q) ||
                s"${c.contactInfo.firstName} ${c.contactInfo.lastName}".toLowerCase.contains(q)
            }
        }

    val tableConfig = SplitTableConfig[Customer](
      columns = List(
        ColumnDef("Company", c => span(
          c.companyInfo.map(_.companyName).getOrElse(s"${c.contactInfo.firstName} ${c.contactInfo.lastName}"),
        ), Some(c => c.companyInfo.map(_.companyName).getOrElse(c.contactInfo.lastName))),
        ColumnDef("Contact", c => span(s"${c.contactInfo.firstName} ${c.contactInfo.lastName}"),
          Some(c => c.contactInfo.lastName)),
        ColumnDef("Tier", c => tierBadge(c.tier),
          Some(_.tier.toString), Some("90px")),
        ColumnDef("Status", c => statusBadge(c.status),
          Some(_.status.toString), Some("130px")),
        ColumnDef("Email", c => span(c.contactInfo.email), Some(_.contactInfo.email)),
        ColumnDef("", c => div(
          cls := "entity-actions",
          button(cls := "btn btn-sm btn-danger", "✕", onClick.stopPropagation --> { _ =>
            CustomerManagementViewModel.removeCustomer(c.id)
          }),
        ), width = Some("50px")),
      ),
      rowKey = _.id.value,
      filters = List(
        FilterDef(
          id = "tier",
          label = "Tier",
          options = Val(CustomerTier.values.toList.map(t => t.toString -> t.displayName.value)),
          selectedValues = tierFilterVar,
        ),
        FilterDef(
          id = "status",
          label = "Status",
          options = Val(CustomerStatus.values.toList.map(s => s.toString -> s.displayName.value)),
          selectedValues = statusFilterVar,
        ),
      ),
      searchPlaceholder = "Search customers…",
      onRowSelect = Some(c => {
        selectedId.set(Some(c.id.value))
        CustomerManagementViewModel.setEditState(CustomerEditState.EditingCustomer(c.id))
      }),
      emptyMessage = "No customers found.",
    )

    val sidePanel: Signal[Option[HtmlElement]] =
      CustomerManagementViewModel.editState.combineWith(CustomerManagementViewModel.customers).map {
        case (CustomerEditState.CreatingCustomer, _) =>
          Some(customerForm(None))
        case (CustomerEditState.EditingCustomer(id), custs) =>
          custs.find(_.id == id).map(c => customerDetailPanel(c))
        case _ => None
      }

    div(
      cls := "catalog-section",
      h2(cls := "manufacturing-view-title", "Customers"),
      SplitTableView(
        config = tableConfig,
        items = filteredCustomers,
        selectedKey = selectedId.signal,
        searchQuery = searchVar,
        sidePanel = sidePanel,
        headerActions = Some(
          FormComponents.actionButton("+ Add Customer", () => {
            selectedId.set(None)
            CustomerManagementViewModel.setEditState(CustomerEditState.CreatingCustomer)
          })
        ),
      ),
    )

  // ── Badge helpers ──────────────────────────────────────────────────────

  private def tierBadge(tier: CustomerTier): HtmlElement =
    val cls_ = tier match
      case CustomerTier.Standard => "badge badge-muted"
      case CustomerTier.Silver   => "badge badge-info"
      case CustomerTier.Gold     => "badge badge-warning"
      case CustomerTier.Platinum => "badge badge-active"
    span(cls := cls_, tier.displayName.value)

  private def statusBadge(status: CustomerStatus): HtmlElement =
    val cls_ = status match
      case CustomerStatus.Active          => "badge badge-completed"
      case CustomerStatus.Inactive        => "badge badge-muted"
      case CustomerStatus.Suspended       => "badge badge-error"
      case CustomerStatus.PendingApproval => "badge badge-pending"
    span(cls := cls_, status.displayName.value)

  // ── Detail panel with Tabs component from ui-framework ─────────────────

  private def customerDetailPanel(customer: Customer): HtmlElement =
    val activeTab = Var("details")

    div(
      cls := "catalog-detail-panel",

      button(cls := "detail-panel-close", "×", onClick --> { _ =>
        CustomerManagementViewModel.setEditState(CustomerEditState.None)
      }),

      div(cls := "detail-panel-header",
        h3(customer.companyInfo.map(_.companyName).getOrElse(
          s"${customer.contactInfo.firstName} ${customer.contactInfo.lastName}"
        )),
        div(
          cls := "detail-panel-badges",
          tierBadge(customer.tier),
          statusBadge(customer.status),
        ),
      ),

      // Use Tabs component from ui-framework
      Tabs(
        tabs = List(
          TabDef("details", Val("Details"), () => detailsTab(customer)),
          TabDef("pricing", Val("Pricing"), () => pricingTab(customer)),
          TabDef("notes", Val("Notes"), () => notesTab(customer)),
          TabDef("orders", Val("Orders"), () => ordersTab(customer)),
        ),
        activeTab = activeTab,
      ),
    )

  // ── Details tab ────────────────────────────────────────────────────────

  private def detailsTab(customer: Customer): HtmlElement =
    val companyNameVar = Var(customer.companyInfo.map(_.companyName).getOrElse(""))
    val businessIdVar = Var(customer.companyInfo.map(_.businessId).getOrElse(""))
    val vatIdVar = Var(customer.companyInfo.flatMap(_.vatId).getOrElse(""))
    val contactPersonVar = Var(customer.companyInfo.map(_.contactPerson).getOrElse(""))
    val firstNameVar = Var(customer.contactInfo.firstName)
    val lastNameVar = Var(customer.contactInfo.lastName)
    val emailVar = Var(customer.contactInfo.email)
    val phoneVar = Var(customer.contactInfo.phone)
    val streetVar = Var(customer.address.street)
    val cityVar = Var(customer.address.city)
    val zipVar = Var(customer.address.zip)
    val countryVar = Var(customer.address.country)
    val tierVar = Var(customer.tier)
    val statusVar = Var(customer.status)

    div(
      cls := "detail-panel-section",

      FormComponents.sectionHeader("Company Information"),
      FormComponents.textField("Company Name", companyNameVar.signal, companyNameVar.writer),
      FormComponents.textField("Business ID", businessIdVar.signal, businessIdVar.writer),
      FormComponents.textField("VAT ID", vatIdVar.signal, vatIdVar.writer),
      FormComponents.textField("Contact Person", contactPersonVar.signal, contactPersonVar.writer),

      FormComponents.sectionHeader("Contact"),
      FormComponents.textField("First Name", firstNameVar.signal, firstNameVar.writer),
      FormComponents.textField("Last Name", lastNameVar.signal, lastNameVar.writer),
      FormComponents.textField("Email", emailVar.signal, emailVar.writer),
      FormComponents.textField("Phone", phoneVar.signal, phoneVar.writer),

      FormComponents.sectionHeader("Address"),
      FormComponents.textField("Street", streetVar.signal, streetVar.writer),
      FormComponents.textField("City", cityVar.signal, cityVar.writer),
      FormComponents.textField("ZIP", zipVar.signal, zipVar.writer),
      FormComponents.textField("Country", countryVar.signal, countryVar.writer),

      FormComponents.sectionHeader("Account"),
      FormComponents.enumSelectRequired[CustomerTier](
        "Tier", CustomerTier.values, tierVar.signal,
        Observer[CustomerTier] { tier =>
          tierVar.set(tier)
          CustomerManagementViewModel.updateCustomerTier(customer.id, tier)
        },
        _.displayName.value,
      ),
      FormComponents.enumSelectRequired[CustomerStatus](
        "Status", CustomerStatus.values, statusVar.signal,
        Observer[CustomerStatus] { status =>
          statusVar.set(status)
          CustomerManagementViewModel.updateCustomerStatus(customer.id, status)
        },
        _.displayName.value,
      ),

      div(
        cls := "detail-panel-actions",
        FormComponents.actionButton("Save Details", () => {
          val companyInfo =
            if companyNameVar.now().trim.nonEmpty then
              Some(CompanyInfo(
                companyNameVar.now(), businessIdVar.now(),
                if vatIdVar.now().trim.nonEmpty then Some(vatIdVar.now()) else None,
                contactPersonVar.now(),
              ))
            else None
          val contactInfo = customer.contactInfo.copy(
            firstName = firstNameVar.now(),
            lastName = lastNameVar.now(),
            email = emailVar.now(),
            phone = phoneVar.now(),
          )
          val address = Address(streetVar.now(), cityVar.now(), zipVar.now(), countryVar.now())
          CustomerManagementViewModel.updateCustomer(customer.id, companyInfo, contactInfo, address)
        }),
      ),
    )

  // ── Pricing tab (read-only summary — edit via Customer Pricing sidebar section) ──

  private def pricingTab(customer: Customer): HtmlElement =
    div(
      cls := "detail-panel-section",

      FormComponents.sectionHeader("Customer Pricing Summary"),

      div(
        cls := "pricing-summary",
        customer.pricing.globalDiscount.map { d =>
          p(span(cls := "badge badge-info", s"Global: ${d.value}%"))
        }.getOrElse(p(cls := "text-muted", "No global discount configured")),
        if customer.pricing.categoryDiscounts.nonEmpty then
          div(
            h5("Category Discounts"),
            customer.pricing.categoryDiscounts.toList.map { case (catId, pct) =>
              div(cls := "pricing-rule-item",
                span(cls := "pricing-rule-label", catId.value),
                span(cls := "badge badge-info", s"${pct.value}%"),
              )
            },
          )
        else emptyNode,
        if customer.pricing.materialDiscounts.nonEmpty then
          div(
            h5("Material Discounts"),
            customer.pricing.materialDiscounts.toList.map { case (matId, pct) =>
              div(cls := "pricing-rule-item",
                span(cls := "pricing-rule-label", matId.value),
                span(cls := "badge badge-info", s"${pct.value}%"),
              )
            },
          )
        else emptyNode,
        if customer.pricing.fixedMaterialPrices.nonEmpty then
          div(
            h5("Fixed Material Prices"),
            customer.pricing.fixedMaterialPrices.toList.map { case (matId, price) =>
              div(cls := "pricing-rule-item",
                span(cls := "pricing-rule-label", matId.value),
                span(cls := "badge badge-warning", s"${price.amount.value} ${price.currency}"),
              )
            },
          )
        else emptyNode,
        if customer.pricing.finishDiscounts.nonEmpty then
          div(
            h5("Finish Discounts"),
            customer.pricing.finishDiscounts.toList.map { case (finId, pct) =>
              div(cls := "pricing-rule-item",
                span(cls := "pricing-rule-label", finId.value),
                span(cls := "badge badge-info", s"${pct.value}%"),
              )
            },
          )
        else emptyNode,
        customer.pricing.minimumOrderOverride.map { min =>
          div(
            h5("Minimum Order Override"),
            span(cls := "badge badge-warning", s"${min.value}"),
          )
        }.getOrElse(emptyNode),
      ),

      p(cls := "text-muted", "To edit pricing, use the Customer Pricing section in the sidebar."),
    )

  // ── Notes tab ──────────────────────────────────────────────────────────

  private def notesTab(customer: Customer): HtmlElement =
    val newNoteVar = Var("")

    div(
      cls := "detail-panel-section",

      FormComponents.sectionHeader("Internal Notes"),

      // Existing notes
      if customer.internalNotes.nonEmpty then
        div(
          cls := "notes-list",
          customer.internalNotes.reverse.map { note =>
            div(
              cls := "note-item",
              div(cls := "note-meta",
                span(cls := "note-date", formatTimestamp(note.createdAt)),
                note.createdBy.map(id => span(cls := "note-author", s" by ${id.value}")).getOrElse(emptyNode),
              ),
              p(cls := "note-text", note.text),
            )
          },
        )
      else p(cls := "empty-notes", "No notes yet."),

      // Add new note
      FormComponents.sectionHeader("Add Note"),
      div(
        cls := "form-group",
        htmlTag("textarea")(
          cls := "form-textarea",
          placeholder := "Enter note text…",
          controlled(
            value <-- newNoteVar.signal,
            onInput.mapToValue --> newNoteVar.writer,
          ),
        ),
      ),

      div(
        cls := "detail-panel-actions",
        FormComponents.actionButton("Add Note", () => {
          val text = newNoteVar.now().trim
          if text.nonEmpty then
            CustomerManagementViewModel.addCustomerNote(customer.id, text)
            newNoteVar.set("")
        }),
      ),
    )

  // ── Orders tab ─────────────────────────────────────────────────────────

  private def ordersTab(customer: Customer): HtmlElement =
    div(
      cls := "detail-panel-section",

      FormComponents.sectionHeader("Order History"),

      child <-- ManufacturingViewModel.orders.map { allOrders =>
        val customerOrders = allOrders.filter(_.order.customerId.contains(customer.id))
        if customerOrders.isEmpty then
          p(cls := "empty-notes", "No orders found for this customer.")
        else
          div(
            cls := "orders-list",
            customerOrders.sortBy(-_.createdAt).map { mo =>
              div(
                cls := "order-item",
                div(cls := "order-item-header",
                  span(cls := "order-id", mo.order.id.value),
                  orderStatusBadge(mo),
                ),
                div(cls := "order-item-details",
                  span(s"Items: ${mo.order.basket.items.size}"),
                  span(s"Total: ${mo.order.total.value}"),
                  span(formatTimestamp(mo.createdAt)),
                ),
              )
            },
          )
      },
    )

  private def orderStatusBadge(mo: ManufacturingOrder): HtmlElement =
    val (text, cls_) = mo.approvalStatus match
      case ApprovalStatus.Placed         => ("Placed", "badge badge-pending")
      case ApprovalStatus.Approved       => ("Approved", "badge badge-completed")
      case ApprovalStatus.Rejected       => ("Rejected", "badge badge-error")
      case ApprovalStatus.PendingChanges => ("Pending Changes", "badge badge-warning")
      case ApprovalStatus.OnHold         => ("On Hold", "badge badge-muted")
    span(cls := cls_, text)

  // ── New customer form ──────────────────────────────────────────────────

  private def customerForm(existing: Option[Customer]): HtmlElement =
    val companyNameVar = Var("")
    val businessIdVar = Var("")
    val vatIdVar = Var("")
    val contactPersonVar = Var("")
    val firstNameVar = Var("")
    val lastNameVar = Var("")
    val emailVar = Var("")
    val phoneVar = Var("")
    val streetVar = Var("")
    val cityVar = Var("")
    val zipVar = Var("")
    val countryVar = Var("CZ")

    div(
      cls := "catalog-detail-panel",

      button(cls := "detail-panel-close", "×", onClick --> { _ =>
        CustomerManagementViewModel.setEditState(CustomerEditState.None)
      }),

      div(cls := "detail-panel-header", h3("New Customer")),

      div(
        cls := "detail-panel-section",
        FormComponents.sectionHeader("Company Information"),
        FormComponents.textField("Company Name", companyNameVar.signal, companyNameVar.writer),
        FormComponents.textField("Business ID", businessIdVar.signal, businessIdVar.writer),
        FormComponents.textField("VAT ID", vatIdVar.signal, vatIdVar.writer),
        FormComponents.textField("Contact Person", contactPersonVar.signal, contactPersonVar.writer),

        FormComponents.sectionHeader("Contact"),
        FormComponents.textField("First Name", firstNameVar.signal, firstNameVar.writer),
        FormComponents.textField("Last Name", lastNameVar.signal, lastNameVar.writer),
        FormComponents.textField("Email", emailVar.signal, emailVar.writer),
        FormComponents.textField("Phone", phoneVar.signal, phoneVar.writer),

        FormComponents.sectionHeader("Address"),
        FormComponents.textField("Street", streetVar.signal, streetVar.writer),
        FormComponents.textField("City", cityVar.signal, cityVar.writer),
        FormComponents.textField("ZIP", zipVar.signal, zipVar.writer),
        FormComponents.textField("Country", countryVar.signal, countryVar.writer),
      ),

      div(
        cls := "detail-panel-actions",
        FormComponents.actionButton("Create Customer", () => {
          val companyInfo =
            if companyNameVar.now().trim.nonEmpty then
              Some(CompanyInfo(companyNameVar.now(), businessIdVar.now(),
                if vatIdVar.now().trim.nonEmpty then Some(vatIdVar.now()) else None,
                contactPersonVar.now()))
            else None
          val newCustomer = Customer(
            id = CustomerId.unsafe(s"cust-${System.currentTimeMillis()}"),
            customerType = CustomerType.Agency,
            status = CustomerStatus.PendingApproval,
            tier = CustomerTier.Standard,
            companyInfo = companyInfo,
            contactInfo = ContactInfo(firstNameVar.now(), lastNameVar.now(), emailVar.now(), phoneVar.now(),
              companyInfo.map(_.companyName), companyInfo.map(_.businessId), companyInfo.flatMap(_.vatId)),
            address = Address(streetVar.now(), cityVar.now(), zipVar.now(), countryVar.now()),
            pricing = CustomerPricing.empty,
            internalNotes = Nil,
            createdAt = System.currentTimeMillis(),
            lastOrderAt = None,
            tags = Set.empty,
          )
          CustomerManagementViewModel.addCustomer(newCustomer)
        }),
        FormComponents.dangerButton("Cancel", () =>
          CustomerManagementViewModel.setEditState(CustomerEditState.None)
        ),
      ),
    )

  private def formatTimestamp(ts: Long): String =
    val date = new scalajs.js.Date(ts.toDouble)
    s"${date.getFullYear()}-${padZero(date.getMonth().toInt + 1)}-${padZero(date.getDate().toInt)}"

  private def padZero(n: Int): String = if n < 10 then s"0$n" else n.toString
