package mpbuilder.backoffice
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

/** jsoniter codecs for Backoffice BFF models, request bodies and the error envelope. */
object BackofficeApiCodecs:
  export mpbuilder.api.ApiKernelCodecs.given

  given JsonValueCodec[BackofficeError]            = JsonCodecMaker.make(config)
  given JsonValueCodec[StaffArtworkUploadPayload]  = JsonCodecMaker.make(config)
  given JsonValueCodec[StaffAuthResult]            = JsonCodecMaker.make(config)
  given JsonValueCodec[BackofficeDashboardSummary] = JsonCodecMaker.make(config)
  given JsonValueCodec[CustomerDetailView]         = JsonCodecMaker.make(config)
  given JsonValueCodec[MarginSimulationCmd]        = JsonCodecMaker.make(config)
  given JsonValueCodec[MarginSimulationResult]     = JsonCodecMaker.make(config)
  given JsonValueCodec[StoreDeliverySettingsView]  = JsonCodecMaker.make(config)
  given JsonValueCodec[StaffLoginRequest]            = JsonCodecMaker.make(config)
  given JsonValueCodec[NoteRequest]                  = JsonCodecMaker.make(config)
  given JsonValueCodec[ReasonRequest]                = JsonCodecMaker.make(config)
  given JsonValueCodec[TextRequest]                  = JsonCodecMaker.make(config)
  given JsonValueCodec[AdvanceStageRequest]          = JsonCodecMaker.make(config)
  given JsonValueCodec[PaymentStatusRequest]         = JsonCodecMaker.make(config)
  given JsonValueCodec[ApproveArtworkRequest]        = JsonCodecMaker.make(config)
  given JsonValueCodec[StaffOrderLineArtworkRequest] = JsonCodecMaker.make(config)
  given JsonValueCodec[DeleteAgreementRequest]       = JsonCodecMaker.make(config)
  given JsonValueCodec[ActiveRequest]                = JsonCodecMaker.make(config)
  given JsonValueCodec[TierRequest]                  = JsonCodecMaker.make(config)
  given JsonValueCodec[RolesRequest]                 = JsonCodecMaker.make(config)
  given JsonValueCodec[PasswordResetRequest]         = JsonCodecMaker.make(config)
  given apiErrorBackofficeError: JsonValueCodec[ApiError[BackofficeError]] = JsonCodecMaker.make(config)
