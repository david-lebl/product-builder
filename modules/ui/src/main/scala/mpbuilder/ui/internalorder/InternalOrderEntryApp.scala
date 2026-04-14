package mpbuilder.ui.internalorder

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.{ProductBuilderViewModel, ArtworkMode, LoginState}
import mpbuilder.ui.productbuilder.components.*
import mpbuilder.ui.{AppRouter, AppRoute}
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.{Money, Currency}
import mpbuilder.uikit.fields.{SelectField, SelectOption}
import mpbuilder.uikit.util.Visibility

/** Compact internal order entry view — same functionality as Product Parameters
  * but designed for experienced employees:
  * - Inline (label-on-left) field layout instead of label-above
  * - Help/info elements are hidden
  * - Customer selector at the top to choose the customer for this order
  */
object InternalOrderEntryApp:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val customers = ProductBuilderViewModel.allCustomers

    div(
      cls := "ioe-page",

      // ── Header ──────────────────────────────────────────────────────────
      div(
        cls := "ioe-header card",
        div(
          cls := "ioe-header-row",
          h2(child.text <-- lang.map {
            case Language.En => "Internal Order Entry"
            case Language.Cs => "Interní zadávání objednávky"
          }),
          // Customer selector
          div(
            cls := "ioe-customer-selector",
            label(
              cls := "ioe-customer-label",
              child.text <-- lang.map {
                case Language.En => "Customer:"
                case Language.Cs => "Zákazník:"
              },
            ),
            select(
              cls := "ioe-customer-select",
              children <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
                val placeholder = option(
                  l match
                    case Language.En => "-- No customer (list price) --"
                    case Language.Cs => "-- Žádný zákazník (ceníková cena) --",
                  value := "",
                  selected := state.internalOrderCustomerId.isEmpty,
                )
                val opts = customers.map { c =>
                  val name = c.companyInfo.map(_.companyName)
                    .getOrElse(s"${c.contactInfo.firstName} ${c.contactInfo.lastName}")
                  val tier = c.tier.displayName(l)
                  option(
                    s"$name [$tier]",
                    value := c.id.value,
                    selected := state.internalOrderCustomerId.contains(c.id),
                  )
                }
                placeholder :: opts
              },
              onChange.mapToValue --> { v =>
                if v.isEmpty then ProductBuilderViewModel.setInternalOrderCustomer(None)
                else ProductBuilderViewModel.setInternalOrderCustomer(Some(CustomerId.unsafe(v)))
              },
            ),
            // Customer badge — shows tier when a customer is selected
            child.maybe <-- ProductBuilderViewModel.currentCustomer.combineWith(lang).map {
              case (Some(c), l) =>
                val tierCls = c.tier match
                  case CustomerTier.Platinum => "ioe-tier-badge ioe-tier-badge--platinum"
                  case CustomerTier.Gold     => "ioe-tier-badge ioe-tier-badge--gold"
                  case CustomerTier.Silver   => "ioe-tier-badge ioe-tier-badge--silver"
                  case CustomerTier.Standard => "ioe-tier-badge ioe-tier-badge--standard"
                Some(span(cls := tierCls, c.tier.displayName(l)))
              case _ => None
            },
          ),
        ),
        // Discount indicator when a customer with pricing is active
        child.maybe <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
          state.internalOrderCustomerId.flatMap { id =>
            customers.find(_.id == id)
          }.flatMap { c =>
            c.pricing.globalDiscount.map { pct =>
              div(
                cls := "ioe-discount-notice",
                l match
                  case Language.En => s"Global discount: ${pct.value}% applied to this customer"
                  case Language.Cs => s"Globální sleva: ${pct.value}% platí pro tohoto zákazníka"
              )
            }
          }
        },
      ),

      // ── Main layout ─────────────────────────────────────────────────────
      div(
        cls := "ioe-layout",

        // Left: compact configuration form
        div(
          cls := "card compact-form ioe-form",

          // 1. Category
          div(
            cls := "form-section",
            h3(child.text <-- lang.map {
              case Language.En => "Category & Preset"
              case Language.Cs => "Kategorie a preset"
            }),
            CategorySelector(),
            PresetSelector(),
          ),

          // 2. Specifications
          div(
            cls := "form-section",
            h3(child.text <-- lang.map {
              case Language.En => "Specifications"
              case Language.Cs => "Specifikace"
            }),
            SpecificationForm(),
          ),

          // 3. Printing Method
          div(
            cls := "form-section",
            h3(child.text <-- lang.map {
              case Language.En => "Printing Method"
              case Language.Cs => "Tisková metoda"
            }),
            PrintingMethodSelector(),
          ),

          // 4. Components
          div(
            cls := "form-section",
            h3(child.text <-- lang.map {
              case Language.En => "Components"
              case Language.Cs => "Komponenty"
            }),
            children <-- ProductBuilderViewModel.componentRoles
              .combineWith(ProductBuilderViewModel.linkedComponents, lang)
              .map { case (roles, linked, l) =>
              if roles.isEmpty then List.empty
              else if roles.size == 1 && roles.head == ComponentRole.Main then
                List(componentSection(ComponentRole.Main))
              else
                import mpbuilder.uikit.fields.CheckboxField
                val toggle = div(
                  cls := "linked-components-toggle",
                  CheckboxField(
                    label = ProductBuilderViewModel.currentLanguage.map {
                      case Language.En => "Same material and printing for all components"
                      case Language.Cs => "Stejný materiál a tisk pro všechny komponenty"
                    },
                    checked = ProductBuilderViewModel.linkedComponents,
                    onChange = Observer[Boolean](v => ProductBuilderViewModel.setLinkedComponents(v)),
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

          // 5. Manufacturing Speed
          div(
            cls := "form-section",
            h3(child.text <-- lang.map {
              case Language.En => "Manufacturing Speed"
              case Language.Cs => "Rychlost výroby"
            }),
            SpecificationForm.manufacturingSpeedSection(),
          ),

          // Validate button
          div(
            cls := "form-section",
            button(
              child.text <-- lang.map {
                case Language.En => "Validate price"
                case Language.Cs => "Ověřit cenu"
              },
              onClick --> { _ => ProductBuilderViewModel.validateConfiguration() },
            ),
          ),

          // Add to basket
          div(
            cls := "form-section",
            div(
              cls := "add-to-basket-section",
              label(child.text <-- lang.map {
                case Language.En => "Quantity to add:"
                case Language.Cs => "Množství k přidání:"
              }),
              input(
                typ := "number",
                minAttr := "1",
                value := "1",
                cls := "basket-quantity-input",
                idAttr := "ioe-basket-qty-input",
              ),
              button(
                cls := "add-to-basket-btn",
                disabled <-- ProductBuilderViewModel.state.map(_.configuration.isEmpty),
                child.text <-- lang.map {
                  case Language.En => "Add to Basket"
                  case Language.Cs => "Přidat do košíku"
                },
                onClick --> { _ =>
                  val qtyInput = org.scalajs.dom.document
                    .getElementById("ioe-basket-qty-input")
                    .asInstanceOf[org.scalajs.dom.html.Input]
                  val qty = qtyInput.value.toIntOption.getOrElse(1)
                  ProductBuilderViewModel.addToBasket(qty)
                },
              ),
            ),
          ),
        ),

        // Right: price + validation
        div(
          cls := "ioe-sidebar",
          PricePreview(),
          ValidationMessages(),
        ),
      ),

      // Basket drawer — slides in from the right
      div(
        cls <-- AppRouter.basketOpen.signal.map(o =>
          if o then "basket-drawer open" else "basket-drawer"
        ),
        div(
          cls := "basket-drawer-close-row",
          button(
            cls := "basket-drawer-close",
            "×",
            onClick --> { _ => AppRouter.basketOpen.set(false) },
          ),
        ),
        BasketView(),
      ),

      // Backdrop overlay
      div(
        cls <-- AppRouter.basketOpen.signal.map(o =>
          if o then "basket-overlay visible" else "basket-overlay"
        ),
        onClick --> { _ => AppRouter.basketOpen.set(false) },
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
      case ComponentRole.Main  => lang match { case Language.En => "Main Component"; case Language.Cs => "Hlavní komponent" }
      case ComponentRole.Cover => lang match { case Language.En => "Cover"; case Language.Cs => "Obálka" }
      case ComponentRole.Body  => lang match { case Language.En => "Body / Inner Pages"; case Language.Cs => "Vnitřní část / stránky" }
      case ComponentRole.Stand => lang match { case Language.En => "Stand / Platform"; case Language.Cs => "Stojánek / platforma" }
