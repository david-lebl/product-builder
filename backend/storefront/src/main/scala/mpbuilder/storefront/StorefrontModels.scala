package mpbuilder.storefront

import mpbuilder.artwork.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.manufacturing.ManufacturingError
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*

// ── Errors ───────────────────────────────────────────────────────────────────

enum StorefrontError(val message: String):
  case CatalogFailure(error: CatalogError)             extends StorefrontError(error.message)
  case PricingFailure(error: PricingError)             extends StorefrontError(error.message)
  case CustomerFailure(error: CustomerError)           extends StorefrontError(error.message)
  case IdentityFailure(error: IdentityError)           extends StorefrontError(error.message)
  case ArtworkFailure(error: ArtworkError)             extends StorefrontError(error.message)
  case BlobFailure(error: BlobError)                   extends StorefrontError(error.message)
  case OrderFailure(error: OrderError)                 extends StorefrontError(error.message)
  case ManufacturingFailure(error: ManufacturingError) extends StorefrontError(error.message)
  case PresetNotFound(categoryId: CategoryId, presetId: PresetId)
      extends StorefrontError(s"Preset '${presetId.value}' not found in category '${categoryId.value}'")
  case NotACustomerSession
      extends StorefrontError("Authenticated principal is not a customer account")
  case OrderNotOwned(orderId: OrderId)
      extends StorefrontError(s"Order '${orderId.value}' does not belong to the signed-in customer")
  case OrderAlreadyAcceptedForArtwork(orderId: OrderId, status: OrderStatus)
      extends StorefrontError(s"Artwork uploads are locked for order '${orderId.value}' because it is already in status $status")
  case PaymentMethodNotAllowed(method: PaymentMethod, reason: String)
      extends StorefrontError(s"Payment method $method is not allowed: $reason")

// ── Artwork Upload Payload (In-Memory / URL-Backed) ──────────────────────────

/** Transport-neutral file payload for customer artwork upload (draft or order line).
  * Carries URL references (`downloadUrl`, `previewUrl`) rather than binary blobs so the
  * same contract works both in-memory (browser object/data URLs) and with future AWS S3 presigned URLs.
  */
final case class ArtworkUploadPayload(
    fileName: NonEmptyString,
    mimeType: NonEmptyString = NonEmptyString.unsafe("application/pdf"),
    sizeBytes: Long = 2_400_000L,
    downloadUrl: Option[NonEmptyString] = None,
    previewUrl: Option[NonEmptyString] = None,
    resolutionDpi: Int = 300,
    bleedMm: Int = 3,
    colorSpace: ColorSpace = ColorSpace.CMYK,
    pageCount: Option[Int] = None,
    fontsEmbedded: Boolean = true,
    hasUnflattenedTransparency: Boolean = false,
    trimSizeOverride: Option[Dimension2D] = None,
)

// ── Product Builder & Dual-Pricing Models ────────────────────────────────────

/** Speed tier option rendered as a card in the Product Builder form. */
final case class SpeedTierOption(
    speed: ManufacturingSpeed,
    available: Boolean,
    unavailableReason: Option[String],
    baseMultiplier: BigDecimal,
    effectiveMultiplier: BigDecimal,
    surchargePercent: BigDecimal,
)

/** Effective quote + optional walk-in comparison quote so logged-in B2B customers see their savings. */
final case class DualPriceQuote(
    effectiveQuote: PriceQuote,
    walkInBaseQuote: Option[PriceQuote],
    savingsNet: Option[Money],
    savingsPercent: Option[BigDecimal],
)

/** Localized presentation line derived from language-independent `ChargeKind` + `ProductCatalog`. */
final case class LocalizedChargeLine(
    label: String,
    componentKey: Option[ComponentKey],
    unitPrice: Money,
    quantity: BigDecimal,
    total: Money,
)

final case class ProductBuilderQuoteResult(
    configuration: ConfigurationResult,
    pricing: DualPriceQuote,
    speedOptions: List[SpeedTierOption],
    localizedBreakdown: List[LocalizedChargeLine],
)

// ── Auth Models ──────────────────────────────────────────────────────────────

final case class AuthResult(
    session: AuthSession,
    customer: Option[CustomerView],
)

// ── Checkout & Order History Models ──────────────────────────────────────────

/** Checkout either consumes a shopping cart (clearing it on submit) or orders a single quick item. */
enum CheckoutSource:
  case FromCart(cartId: CartId)
  case QuickSingleLine(line: CartLine)

final case class CheckoutPrefill(
    customer: Option[CustomerView],
    billingAddress: Option[Address],
    shippingAddresses: List[Address],
    invoiceApproved: Boolean,
    invoiceDueDays: Option[Int],
)

final case class CheckoutOptionsView(
    prefill: CheckoutPrefill,
    deliveryOptions: List[DeliveryOptionQuote],
    paymentOptions: List[PaymentOption],
)

final case class CheckoutPreview(
    lines: List[CartLine],
    currency: Currency,
    itemsNetTotal: Money,
    promoDiscountNet: Money,
    appliedPromo: Option[AppliedPromoSnapshot],
    delivery: Option[DeliverySelection],
    shippingNet: Money,
    taxTotal: Money,
    grandTotalGross: Money,
    totalWeightGrams: Option[BigDecimal],
)

final case class SubmitCheckoutCmd(
    sessionToken: Option[SessionToken],
    source: CheckoutSource,
    customer: OrderCustomerSnapshot,
    billingAddress: Address,
    deliveryChoice: DeliveryOptionChoice,
    paymentMethod: PaymentMethod,
    promoCode: Option[String] = None,
    customerNote: Option[String] = None,
)

final case class CheckoutConfirmation(
    order: OrderView,
    paymentInstructions: PaymentInstructions,
)

// ── Pure Localized Charge Formatter (BFF Presentation Layer) ─────────────────

object ChargeFormatter:

  def formatQuote(
      quote: PriceQuote,
      catalog: ProductCatalog,
      lang: Language,
  ): List[LocalizedChargeLine] =
    val compLines = quote.componentQuotes.toList.flatMap { cq =>
      val lines = List(cq.materialLine) ++ cq.cuttingLine.toList ++ cq.inkLine.toList ++ cq.finishLines
      lines.map(toLocalized(_, catalog, lang))
    }
    val orderLines = quote.orderCharges.map(toLocalized(_, catalog, lang))
    val speedLines = quote.speedAdjustment.toList.map(toLocalized(_, catalog, lang))
    val setupLines = quote.setupFees.map(toLocalized(_, catalog, lang))
    val minLines   = quote.minimumAdjustment.toList.map(toLocalized(_, catalog, lang))
    val promoLines = quote.promoDiscount.toList.map(toLocalized(_, catalog, lang))

    compLines ++ orderLines ++ speedLines ++ setupLines ++ minLines ++ promoLines

  def toLocalized(line: ChargeLine, catalog: ProductCatalog, lang: Language): LocalizedChargeLine =
    LocalizedChargeLine(
      label = labelFor(line.kind, catalog, lang),
      componentKey = line.componentKey,
      unitPrice = line.unitPrice,
      quantity = line.quantity,
      total = line.total,
    )

  def labelFor(kind: ChargeKind, catalog: ProductCatalog, lang: Language): String =
    val isCs = lang == Language.Cs
    kind match
      case ChargeKind.MaterialCharge(matId, _) =>
        catalog.materials.get(matId).map(_.name(lang)).getOrElse(matId.value)
      case ChargeKind.CuttingCharge(cuts) =>
        if isCs then s"Řezání ($cuts řezů/arch)" else s"Guillotine cutting ($cuts cuts/sheet)"
      case ChargeKind.InkCharge(pmId, inkKey) =>
        val pm = catalog.printingMethods.get(pmId).map(_.name(lang)).getOrElse(pmId.value)
        if isCs then s"Tisk $pm (${inkKey.notation})" else s"$pm ink (${inkKey.notation})"
      case ChargeKind.FinishCharge(finId, finTypeKey) =>
        catalog.finishes.get(finId).map(_.name(lang)).getOrElse(finTypeKey)
      case ChargeKind.PrintingMethodCharge(pmId) =>
        catalog.printingMethods.get(pmId).map(_.name(lang)).getOrElse(pmId.value)
      case ChargeKind.PrintingProcessCharge(procKey) =>
        if isCs then s"Tiskový proces ($procKey)" else s"Printing process ($procKey)"
      case ChargeKind.CategoryCharge(catId) =>
        catalog.categories.get(catId).map(_.name(lang)).getOrElse(catId.value)
      case ChargeKind.FoldCharge(foldKey) =>
        if isCs then s"Skládání ($foldKey)" else s"Folding ($foldKey)"
      case ChargeKind.BindingCharge(bindingKey) =>
        if isCs then s"Vazba ($bindingKey)" else s"Binding ($bindingKey)"
      case ChargeKind.VersionChangeCharge(extra) =>
        if isCs then s"Změna grafické verze (+$extra)" else s"Artwork version change (+$extra)"
      case ChargeKind.SpeedAdjustment(speed, mult) =>
        val pct = ((mult - BigDecimal(1)) * BigDecimal(100)).setScale(0, BigDecimal.RoundingMode.HALF_UP)
        val sign = if pct >= 0 then s"+$pct%" else s"$pct%"
        if isCs then s"Rychlost výroby ($speed, $sign)" else s"Production speed ($speed, $sign)"
      case ChargeKind.SetupFee(target) =>
        val targetLabel = target match
          case PricingTarget.FinishRate(fid, _)       => catalog.finishes.get(fid).map(_.name(lang)).getOrElse(fid.value)
          case PricingTarget.FinishTypeRate(ft, _)    => ft
          case PricingTarget.PrintingMethodRate(pmId) => catalog.printingMethods.get(pmId).map(_.name(lang)).getOrElse(pmId.value)
          case PricingTarget.FoldRate(fk)             => fk
          case PricingTarget.BindingRate(bk)          => bk
          case other                                  => other.toString
        if isCs then s"Příprava výroby — $targetLabel" else s"Setup fee — $targetLabel"
      case ChargeKind.MinimumOrderAdjustment(floor, _) =>
        if isCs then s"Dorovnání minimální objednávky ($floor)" else s"Minimum order adjustment ($floor)"
      case ChargeKind.PromoDiscount(_, code) =>
        if isCs then s"Slevový kód $code" else s"Promo code $code"
