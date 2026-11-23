package mpbuilder.storefront
package http.api

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import mpbuilder.api.ApiError
import mpbuilder.artwork.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.json.JsonSupport.config

/** jsoniter codecs for Storefront BFF models, request bodies and the error envelope. */
object StorefrontApiCodecs:
  export mpbuilder.api.ApiKernelCodecs.given

  given JsonValueCodec[StorefrontError]            = JsonCodecMaker.make(config)
  given JsonValueCodec[ArtworkUploadPayload]       = JsonCodecMaker.make(config)
  given JsonValueCodec[ProductBuilderQuoteResult]  = JsonCodecMaker.make(config)
  given JsonValueCodec[AuthResult]                 = JsonCodecMaker.make(config)
  given JsonValueCodec[CheckoutSource]             = JsonCodecMaker.make(config)
  given JsonValueCodec[CheckoutOptionsView]        = JsonCodecMaker.make(config)
  given JsonValueCodec[CheckoutPreview]            = JsonCodecMaker.make(config)
  given JsonValueCodec[SubmitCheckoutCmd]          = JsonCodecMaker.make(config)
  given JsonValueCodec[CheckoutConfirmation]       = JsonCodecMaker.make(config)
  given JsonValueCodec[CompatibleFinishesRequest]    = JsonCodecMaker.make(config)
  given JsonValueCodec[FinishLimitsRequest]          = JsonCodecMaker.make(config)
  given JsonValueCodec[ConfigureAndQuoteRequest]     = JsonCodecMaker.make(config)
  given JsonValueCodec[PricePresetRequest]           = JsonCodecMaker.make(config)
  given JsonValueCodec[PasswordLoginRequest]         = JsonCodecMaker.make(config)
  given JsonValueCodec[OtpRequest]                   = JsonCodecMaker.make(config)
  given JsonValueCodec[OtpVerifyRequest]             = JsonCodecMaker.make(config)
  given JsonValueCodec[CreateCartRequest]            = JsonCodecMaker.make(config)
  given JsonValueCodec[AddToCartRequest]             = JsonCodecMaker.make(config)
  given JsonValueCodec[UpdateCopiesRequest]          = JsonCodecMaker.make(config)
  given JsonValueCodec[ApplyPromoRequest]            = JsonCodecMaker.make(config)
  given JsonValueCodec[CheckoutOptionsRequest]       = JsonCodecMaker.make(config)
  given JsonValueCodec[CheckoutPreviewRequest]       = JsonCodecMaker.make(config)
  given JsonValueCodec[UpdateAddressesRequest]       = JsonCodecMaker.make(config)
  given JsonValueCodec[DraftArtworkRequest]          = JsonCodecMaker.make(config)
  given JsonValueCodec[OrderLineArtworkRequest]      = JsonCodecMaker.make(config)
  given JsonValueCodec[ReuploadArtworkRequest]       = JsonCodecMaker.make(config)
  given mapFinishTypeBigDecimal: JsonValueCodec[Map[FinishType, BigDecimal]] = JsonCodecMaker.make(config)
  given apiErrorStorefrontError: JsonValueCodec[ApiError[StorefrontError]]   = JsonCodecMaker.make(config)
