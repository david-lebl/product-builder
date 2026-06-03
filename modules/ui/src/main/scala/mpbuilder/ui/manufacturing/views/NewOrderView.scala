package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.sample.SampleCustomers
import mpbuilder.ui.manufacturing.ManufacturingViewModel
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.ui.productbuilder.components.*
import mpbuilder.uikit.fields.CheckboxField

/** Internal product-builder form for employees to configure a product on behalf of a customer.
  *
  * Reuses the same product-builder components as the customer-facing form but:
  *  - Adds a customer selector so customer-specific prices and discounts are applied.
  *  - Omits the artwork upload / visual editor sections (not relevant for internal orders).
  *  - Replaces "Add to Basket" with a "Create Internal Order" action that injects
  *    the resulting order directly into the manufacturing queue.
  */
object NewOrderView:

  private def customerDisplayName(c: Customer): String =
    c.companyInfo.map(_.companyName)
      .getOrElse(s"${c.contactInfo.firstName} ${c.contactInfo.lastName}".trim)

  def apply(): HtmlElement =
    val selectedCustomer: Var[Option[Customer]] = Var(None)
    val orderNotes: Var[String] = Var("")
    val submitMessage: Var[Option[String]] = Var(None)
    val submitError: Var[Option[String]] = Var(None)
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "manufacturing-view new-order-view",

      h2(cls := "manufacturing-view-title", "New Internal Order"),

      div(
        cls := "main-content",

        // ── Left: customer selector + product configuration form ──────────
        div(
          cls := "card",

          // 1. Customer selection
          div(
            cls := "form-section",
            h3("1. Select Customer"),
            div(
              cls := "new-order-customer-selector",
              select(
                cls := "form-control",
                option(value := "", "— Select a customer (optional) —"),
                SampleCustomers.all.map { c =>
                  option(
                    value := c.id.value,
                    s"${customerDisplayName(c)} (${c.tier.displayName(Language.En)})",
                  )
                },
                onChange.mapToValue --> { value =>
                  val customer =
                    if value.isEmpty then None
                    else SampleCustomers.all.find(_.id.value == value)
                  selectedCustomer.set(customer)
                  ProductBuilderViewModel.setInternalOrderCustomer(customer)
                },
              ),
              child <-- selectedCustomer.signal.map {
                case None    => emptyNode
                case Some(c) => customerPricingSummary(c)
              },
            ),
          ),

          // 2. Category selection
          div(
            cls := "form-section",
            h3(child.text <-- lang.map {
              case Language.En => "2. Select Product Category"
              case Language.Cs => "2. Vyberte kategorii produktu"
            }),
            CategorySelector(),
            PresetSelector(),
          ),

          // 3. Product Specifications
          div(
            cls := "form-section",
            h3(child.text <-- lang.map {
              case Language.En => "3. Product Specifications"
              case Language.Cs => "3. Specifikace produktu"
            }),
            SpecificationForm(),
          ),

          // 4. Printing Method
          div(
            cls := "form-section",
            h3(child.text <-- lang.map {
              case Language.En => "4. Select Printing Method"
              case Language.Cs => "4. Vyberte tiskovou metodu"
            }),
            PrintingMethodSelector(),
          ),

          // 5. Component Configuration
          div(
            cls := "form-section",
            h3(child.text <-- lang.map {
              case Language.En => "5. Configure Components"
              case Language.Cs => "5. Specifikace výroby"
            }),
            children <-- ProductBuilderViewModel.componentRoles
              .combineWith(ProductBuilderViewModel.linkedComponents, lang)
              .map { case (roles, linked, l) =>
                if roles.isEmpty then
                  List(
                    p(cls := "info-box",
                      l match
                        case Language.En => "Select a category to configure components"
                        case Language.Cs => "Vyberte kategorii pro konfiguraci komponentů"
                    ),
                  )
                else if roles.size == 1 && roles.head == ComponentRole.Main then
                  List(componentSection(ComponentRole.Main))
                else
                  val toggle = div(
                    cls := "linked-toggle",
                    CheckboxField(
                      label = ProductBuilderViewModel.currentLanguage.map {
                        case Language.En => "Same material and printing for all components"
                        case Language.Cs => "Stejný materiál a tisk pro všechny komponenty"
                      },
                      checked = ProductBuilderViewModel.linkedComponents,
                      onChange = Observer[Boolean](v => ProductBuilderViewModel.setLinkedComponents(v)),
                    ),
                    span(
                      cls := "linked-toggle__hint",
                      child.text <-- ProductBuilderViewModel.currentLanguage.map {
                        case Language.En => "Shared settings apply to all components at once"
                        case Language.Cs => "Sdílené nastavení se aplikuje na všechny komponenty najednou"
                      },
                    ),
                  )
                  if linked then
                    val sharedSection = div(
                      MaterialSelector(roles.head),
                      InkConfigSelector(roles.head),
                    )
                    val finishSections = roles.map { role =>
                      div(
                        cls := "component-section",
                        h4(componentRoleLabel(role, l)),
                        FinishSelector(role),
                      )
                    }
                    toggle :: sharedSection :: finishSections
                  else
                    toggle :: roles.map { role =>
                      div(
                        cls := "component-section",
                        h4(componentRoleLabel(role, l)),
                        componentSection(role),
                      )
                    }
              },
          ),

          // 6. Manufacturing Speed
          div(
            cls := "form-section",
            h3(child.text <-- lang.map {
              case Language.En => "6. Manufacturing Speed"
              case Language.Cs => "6. Rychlost výroby"
            }),
            SpecificationForm.manufacturingSpeedSection(),
          ),

          // 7. Notes
          div(
            cls := "form-section",
            h3("7. Internal Notes"),
            textarea(
              cls := "form-control",
              rows := 3,
              placeholder := "Internal notes for this order (optional)…",
              value <-- orderNotes.signal,
              onInput.mapToValue --> orderNotes.writer,
            ),
          ),

          // Submit section
          div(
            cls := "form-section",
            div(
              cls := "add-to-basket-section",
              button(
                cls := "add-to-basket-btn",
                disabled <-- ProductBuilderViewModel.state.map(s =>
                  s.configuration.isEmpty || s.validationErrors.nonEmpty,
                ),
                "Create Internal Order",
                onClick --> { _ =>
                  val state = ProductBuilderViewModel.stateVar.now()
                  val customer = selectedCustomer.now()
                  (state.configuration, state.priceBreakdown) match
                    case (Some(config), Some(breakdown)) =>
                      val customerName = customer
                        .map(customerDisplayName)
                        .getOrElse("Internal Order")
                      val qty = state.specifications.collectFirst {
                        case SpecValue.QuantitySpec(q) => q.value
                      }.getOrElse(1)
                      ManufacturingViewModel.createInternalOrder(
                        customerName = customerName,
                        customerId = customer.map(_.id),
                        config = config,
                        priceBreakdown = breakdown,
                        quantity = qty,
                        notes = orderNotes.now(),
                      )
                      submitMessage.set(Some(s"✓ Order created for $customerName — it appears in the Order Approval queue."))
                      submitError.set(None)
                    case _ =>
                      submitError.set(Some("Cannot create order: configuration is incomplete or has validation errors."))
                },
              ),
            ),

            child <-- submitMessage.signal.map {
              case Some(msg) => div(cls := "info-box", msg)
              case None      => emptyNode
            },
            child <-- submitError.signal.map {
              case Some(msg) => div(cls := "validation-errors", p(msg))
              case None      => emptyNode
            },
          ),
        ),

        // ── Right: price preview + validation ────────────────────────────
        div(
          cls := "price-section",
          PricePreview(),
          ValidationMessages(),
        ),
      ),
    )

  private def componentSection(role: ComponentRole): Element =
    div(
      MaterialSelector(role),
      InkConfigSelector(role),
      FinishSelector(role),
    )

  private def componentRoleLabel(role: ComponentRole, lang: Language): String =
    role match
      case ComponentRole.Main  => if lang == Language.Cs then "Hlavní komponent" else "Main Component"
      case ComponentRole.Cover => if lang == Language.Cs then "Obálka" else "Cover"
      case ComponentRole.Body  => if lang == Language.Cs then "Vnitřní část / stránky" else "Body / Inner Pages"
      case ComponentRole.Stand => if lang == Language.Cs then "Stojánek / platforma" else "Stand / Platform"

  private def customerPricingSummary(customer: Customer): HtmlElement =
    val tierCls = customer.tier match
      case CustomerTier.Standard => "badge badge-muted"
      case CustomerTier.Silver   => "badge badge-info"
      case CustomerTier.Gold     => "badge badge-warning"
      case CustomerTier.Platinum => "badge badge-active"

    val ruleCount =
      customer.pricing.categoryDiscounts.size +
      customer.pricing.materialDiscounts.size +
      customer.pricing.fixedMaterialPrices.size +
      customer.pricing.finishDiscounts.size

    div(
      cls := "new-order-customer-pricing-summary info-box",
      div(
        cls := "pricing-summary-badges",
        span(cls := tierCls, customer.tier.displayName(Language.En)),
        customer.pricing.globalDiscount.map { d =>
          span(cls := "badge badge-success", s"${d.value}% global discount")
        }.getOrElse(emptyNode),
        if ruleCount > 0 then
          span(cls := "badge badge-info", s"$ruleCount specific rule(s)")
        else emptyNode,
        if ruleCount == 0 && customer.pricing.globalDiscount.isEmpty then
          span(cls := "text-muted", "Standard pricing — no customer discounts configured")
        else emptyNode,
      ),
    )
