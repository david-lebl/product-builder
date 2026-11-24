package mpbuilder.backoffice
package http.api

import kyo.*

import mpbuilder.artwork.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*

/** kyo-schema codecs for Backoffice BFF models, request bodies and the error envelope. */
object BackofficeApiCodecs:
  export mpbuilder.api.ApiKernelCodecs.given

  given Schema[BackofficeError]            = Schema.derived
  given Schema[StaffArtworkUploadPayload]  = Schema.derived
  given Schema[StaffAuthResult]            = Schema.derived
  given Schema[BackofficeDashboardSummary] = Schema.derived
  given Schema[CustomerDetailView]         = Schema.derived
  given Schema[MarginSimulationCmd]        = Schema.derived
  given Schema[MarginSimulationResult]     = Schema.derived
  given Schema[StoreDeliverySettingsView]  = Schema.derived
  given Schema[StaffLoginRequest]            = Schema.derived
  given Schema[NoteRequest]                  = Schema.derived
  given Schema[ReasonRequest]                = Schema.derived
  given Schema[TextRequest]                  = Schema.derived
  given Schema[AdvanceStageRequest]          = Schema.derived
  given Schema[PaymentStatusRequest]         = Schema.derived
  given Schema[ApproveArtworkRequest]        = Schema.derived
  given Schema[StaffOrderLineArtworkRequest] = Schema.derived
  given Schema[DeleteAgreementRequest]       = Schema.derived
  given Schema[ActiveRequest]                = Schema.derived
  given Schema[TierRequest]                  = Schema.derived
  given Schema[RolesRequest]                 = Schema.derived
  given Schema[PasswordResetRequest]         = Schema.derived
