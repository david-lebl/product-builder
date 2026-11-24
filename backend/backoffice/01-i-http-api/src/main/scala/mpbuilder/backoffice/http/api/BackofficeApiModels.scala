package mpbuilder.backoffice
package http.api

import mpbuilder.artwork.*
import mpbuilder.catalog.*
import mpbuilder.customers.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*

// ── Backoffice request bodies ────────────────────────────────────────────────

final case class StaffLoginRequest(email: EmailAddress, password: NonEmptyString)
final case class NoteRequest(note: Option[NonEmptyString] = None)
final case class ReasonRequest(reason: NonEmptyString)
final case class TextRequest(text: NonEmptyString)
final case class AdvanceStageRequest(target: OrderStatus, note: Option[NonEmptyString] = None)
final case class PaymentStatusRequest(status: PaymentStatus, note: Option[NonEmptyString] = None)
final case class ApproveArtworkRequest(overrideFailures: Boolean = false)

final case class StaffOrderLineArtworkRequest(
    lineIndex: Int,
    componentKey: ComponentKey,
    payload: StaffArtworkUploadPayload,
)

final case class DeleteAgreementRequest(target: AgreementTarget, currency: Currency)
final case class ActiveRequest(active: Boolean)
final case class TierRequest(tier: CustomerTier)
final case class RolesRequest(roles: Set[EmployeeRole])
final case class PasswordResetRequest(email: EmailAddress, newPassword: NonEmptyString)

