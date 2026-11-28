package mpbuilder.storefront
package http.api

import mpbuilder.artwork.ColorSpace
import mpbuilder.catalog.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.shared.*

// ── Storefront request bodies ────────────────────────────────────────────────

final case class CompatibleFinishesRequest(
    categoryId: CategoryId,
    componentKey: ComponentKey,
    materialId: MaterialId,
    printingMethodId: Option[PrintingMethodId] = None,
    alreadySelectedFinishIds: Set[FinishId] = Set.empty,
)

final case class FinishLimitsRequest(
    categoryId: CategoryId,
    componentKey: ComponentKey,
    materialId: MaterialId,
    printingMethodId: Option[PrintingMethodId] = None,
)

final case class ConfigureAndQuoteRequest(
    cmd: ConfigureProductCmd,
    currency: Currency,
    speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
    promoCode: Option[String] = None,
    lang: Language = Language.En,
)

final case class PricePresetRequest(
    categoryId: CategoryId,
    presetId: PresetId,
    currency: Currency,
    speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
    specsOverride: Option[ProductSpecs] = None,
    lang: Language = Language.En,
)

final case class PriceMatrixRequest(
    cmd: ConfigureProductCmd,
    quantities: List[Quantity],
    currency: Currency,
    promoCode: Option[String] = None,
)

final case class PasswordLoginRequest(email: EmailAddress, password: NonEmptyString)
final case class OtpRequest(identifier: LoginIdentifier)
final case class OtpVerifyRequest(challengeId: OtpChallengeId, code: OtpCode)

final case class CreateCartRequest(cartId: CartId, currency: Currency)

final case class AddToCartRequest(
    cmd: ConfigureProductCmd,
    currency: Currency,
    speed: ManufacturingSpeed = ManufacturingSpeed.Standard,
    copies: Quantity = Quantity.one,
    artworkIds: Map[ComponentKey, ArtworkId] = Map.empty,
    productionNote: Option[String] = None,
)

final case class UpdateCopiesRequest(copies: Quantity)

/** Sets (or clears, with `None` / blank) a cart or order line's production instructions. */
final case class ProductionNoteRequest(note: Option[String] = None)
final case class ApplyPromoRequest(code: String)

final case class CheckoutOptionsRequest(
    source: CheckoutSource,
    currency: Currency,
    promoCode: Option[String] = None,
)

final case class CheckoutPreviewRequest(
    source: CheckoutSource,
    currency: Currency,
    deliveryChoice: Option[DeliveryOptionChoice],
    promoCode: Option[String] = None,
)

final case class UpdateAddressesRequest(
    billingAddress: Address,
    shippingAddresses: List[Address],
    defaultShippingIndex: Option[Int],
)

final case class DraftArtworkRequest(
    sessionId: NonEmptyString,
    componentKey: ComponentKey,
    expectedTrimSize: Dimension2D,
    expectedPageCount: Option[Int] = None,
    payload: ArtworkUploadPayload,
)

final case class OrderLineArtworkRequest(
    lineIndex: Int,
    componentKey: ComponentKey,
    payload: ArtworkUploadPayload,
)

final case class ReuploadArtworkRequest(
    fileName: NonEmptyString,
    resolutionDpi: Int = 300,
    bleedMm: Int = 3,
    colorSpace: ColorSpace = ColorSpace.CMYK,
)

