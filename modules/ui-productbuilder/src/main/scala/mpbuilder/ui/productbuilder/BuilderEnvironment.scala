package mpbuilder.ui.productbuilder

import mpbuilder.commons.*

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.{ManufacturingSpeed, ProductConfiguration}
import mpbuilder.domain.pricing.PricingContext
import mpbuilder.ui.productbuilder.basket.{BasketBackend, LocalBasketBackend}

/** How the host app lets the customer supply artwork.
  *
  * The full SPA implements this on top of the visual editor; the standalone
  * calculator supplies `None` and the artwork section is not rendered at all.
  */
trait ArtworkIntegration:
  /** Rendered inside the "Provide Artwork" form section. */
  def render(): Element

  /** Rendered on each basket line to show which artwork belongs to that item.
    *
    * Keyed by the basket line's id, not by the configuration's. Two customers ordering the same
    * product supply different artwork, and once the basket lives on the server two identical
    * configurations are deliberately one value — so a configuration id cannot identify a line.
    */
  def renderBasketItem(lineId: String, lang: Language): Element

  /** Called when a configuration is added to the basket, with the id of the line it landed on, so
    * the host can remember which artwork belongs to which basket line.
    */
  def onAddedToBasket(lineId: String, config: ProductConfiguration): Unit

  /** Called when a basket line is removed. */
  def onRemovedFromBasket(lineId: String): Unit

  /** Called when the basket is cleared. */
  def onBasketCleared(): Unit

  /** Called when the product form is reset, so the in-progress artwork choice
    * is cleared along with the rest of the form.
    */
  def onFormReset(): Unit

  /** A one-line artwork summary for the order e-mail, if any. */
  def emailSummary(lineId: String, lang: Language): Option[String]

/** The basket's primary call to action.
  *
  * Full SPA: "Proceed to Checkout" → the checkout wizard.
  * Calculator: "Send order by e-mail" → the e-mail order modal in basket mode.
  */
final case class BasketPrimaryAction(
    label: Language => String,
    onClick: () => Unit,
)

/** The configuration form's "order this one now" button — a quick path for a
  * single item that skips having to open the basket drawer at all.
  *
  * Full SPA: jumps straight into the checkout wizard scoped to just this item.
  * Calculator: opens the e-mail order modal, since there is no real checkout
  * to jump into.
  *
  * @param enabled whether the action can currently be invoked. The full SPA
  *                needs a valid configuration to check out; the calculator's
  *                e-mail request is deliberately always enabled, since covering
  *                what the configurator itself cannot validate is the point.
  * @param onClick receives the quantity from the "Quantity to add" field.
  */
final case class QuickOrderAction(
    label: Language => String,
    enabled: Signal[Boolean],
    onClick: Int => Unit,
)

/** Everything the product configurator needs from its host application.
  *
  * This is the seam that lets `ui-productbuilder` stay free of any dependency on
  * the full SPA (routing, visual editor, shop-floor queue simulation) while still
  * being reused by it verbatim.
  *
  * The defaults describe the standalone calculator: no production queue, so no
  * surge pricing, no shop-load gate on Express and no concrete completion dates.
  */
final case class BuilderEnvironment(
    /** Queue surge / busy-period context handed to `PriceCalculator`. */
    pricingContext: () => PricingContext = () => PricingContext.default,

    /** Concrete completion estimate for a tier, formatted for display.
      *
      * `None` means the host has no production-queue data, and the tier card
      * falls back to an indicative range plus a "not a promise" disclaimer.
      */
    completionText: (ManufacturingSpeed, BuilderState, Language) => Option[String] =
      (_, _, _) => None,

    /** Whether Express is currently sellable given shop load. */
    expressAvailable: Signal[Boolean] = Val(true),

    /** Artwork step. `None` hides the section entirely. */
    artwork: Option[ArtworkIntegration] = None,

    /** Where the basket lives.
      *
      * The default keeps it in the browser, which is what the embeddable calculator needs and what
      * every host gets until it says otherwise. The full SPA installs an HTTP backend, and the
      * basket then survives a reload and follows the customer between devices.
      */
    basketBackend: BasketBackend = LocalBasketBackend(() =>
      mpbuilder.domain.sample.SamplePricelist.pricelistCzkSheet
    ),

    /** Basket drawer open/closed — owned by the host shell, which also renders
      * the button that toggles it.
      */
    basketOpen: Var[Boolean] = Var(false),

    /** What the basket's primary button says and does. */
    basketPrimaryAction: BasketPrimaryAction = BasketPrimaryAction(
      label = {
        case Language.En => "Send order by e-mail"
        case Language.Cs => "Odeslat objednávku e-mailem"
      },
      onClick = () => (),
    ),

    /** What the configuration form's "order this one now" button says and does. */
    quickOrderAction: QuickOrderAction = QuickOrderAction(
      label = {
        case Language.En => "Order this now"
        case Language.Cs => "Objednat nyní"
      },
      enabled = Val(true),
      onClick = _ => (),
    ),

    /** Recipient of the `mailto:` order. Empty leaves the To field blank so the
      * customer picks the address themselves (the full SPA's behaviour).
      */
    orderEmail: String = "",
)

object BuilderEnvironment:
  private var current: BuilderEnvironment = BuilderEnvironment()

  /** Install the host's environment. Must be called before the first render. */
  def init(env: BuilderEnvironment): Unit = current = env

  def get: BuilderEnvironment = current
