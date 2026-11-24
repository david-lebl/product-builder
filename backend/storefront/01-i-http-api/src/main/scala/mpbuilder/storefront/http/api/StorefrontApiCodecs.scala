package mpbuilder.storefront
package http.api

import kyo.*

import mpbuilder.artwork.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*

/** kyo-schema codecs for Storefront BFF models, request bodies and the error envelope. */
object StorefrontApiCodecs:
  export mpbuilder.api.ApiKernelCodecs.given

  given Schema[StorefrontError]            = Schema.derived
  given Schema[ArtworkUploadPayload]       = Schema.derived
  given Schema[ProductBuilderQuoteResult]  = Schema.derived
  given Schema[AuthResult]                 = Schema.derived
  given Schema[CheckoutSource]             = Schema.derived
  given Schema[CheckoutOptionsView]        = Schema.derived
  given Schema[CheckoutPreview]            = Schema.derived
  given Schema[SubmitCheckoutCmd]          = Schema.derived
  given Schema[CheckoutConfirmation]       = Schema.derived
  given Schema[CompatibleFinishesRequest]    = Schema.derived
  given Schema[FinishLimitsRequest]          = Schema.derived
  given Schema[ConfigureAndQuoteRequest]     = Schema.derived
  given Schema[PricePresetRequest]           = Schema.derived
  given Schema[PasswordLoginRequest]         = Schema.derived
  given Schema[OtpRequest]                   = Schema.derived
  given Schema[OtpVerifyRequest]             = Schema.derived
  given Schema[CreateCartRequest]            = Schema.derived
  given Schema[AddToCartRequest]             = Schema.derived
  given Schema[UpdateCopiesRequest]          = Schema.derived
  given Schema[ApplyPromoRequest]            = Schema.derived
  given Schema[CheckoutOptionsRequest]       = Schema.derived
  given Schema[CheckoutPreviewRequest]       = Schema.derived
  given Schema[UpdateAddressesRequest]       = Schema.derived
  given Schema[DraftArtworkRequest]          = Schema.derived
  given Schema[OrderLineArtworkRequest]      = Schema.derived
  given Schema[ReuploadArtworkRequest]       = Schema.derived
