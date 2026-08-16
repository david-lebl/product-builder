package mpbuilder.calculator

import mpbuilder.commons.*

import com.raquo.laminar.api.L.*
import mpbuilder.domain.pricing.PricingContext
import mpbuilder.ui.productbuilder.{BasketPrimaryAction, BuilderEnvironment, QuickOrderAction}
import mpbuilder.ui.productbuilder.components.EmailOrderModal

/** The product configurator as hosted by the standalone calculator.
  *
  * The widget runs on a customer's website with no link to the print shop's
  * production system, so:
  *
  *   - pricing uses the base multipliers only — there is no queue to surge on;
  *   - no completion date is offered, because none could be honoured. The tier
  *     cards fall back to indicative ranges plus an explicit disclaimer;
  *   - Express is never gated on shop load (product-level tier restrictions
  *     still apply — those are real constraints, not queue state);
  *   - there is no visual editor and no way to transfer a file, so the artwork
  *     step is omitted and the order e-mail says artwork is arranged in reply;
  *   - the basket leads to an e-mail order request instead of a checkout.
  */
object CalculatorEnvironment:

  /** Basket drawer state — owned here rather than by a router, since the widget
    * has no routing.
    */
  val basketOpen: Var[Boolean] = Var(false)

  def environment(config: CalculatorConfig): BuilderEnvironment =
    BuilderEnvironment(
      pricingContext = () => PricingContext.default,
      completionText = (_, _, _) => None,
      expressAvailable = Val(true),
      artwork = None,
      basketOpen = basketOpen,
      basketPrimaryAction = BasketPrimaryAction(
        label = {
          case Language.En => "Send order by e-mail ✉"
          case Language.Cs => "Odeslat objednávku e-mailem ✉"
        },
        onClick = () => {
          basketOpen.set(false)
          EmailOrderModal.openForBasket()
        },
      ),
      quickOrderAction = QuickOrderAction(
        label = {
          case Language.En => "✉ Order this by e-mail"
          case Language.Cs => "✉ Objednat e-mailem"
        },
        enabled = Val(true),
        onClick = quantity => EmailOrderModal.open(quantity),
      ),
      orderEmail = config.orderEmail,
    )
