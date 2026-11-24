package mpbuilder.json

import kyo.*
import mpbuilder.identity.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.shared.*
import mpbuilder.json.SharedCodecs.given
import mpbuilder.json.JsonSupport.given

/** Schemas for the public `identity` model. */
object IdentityCodecs:
  given Schema[SessionToken]   = stringOpaque("SessionToken")(SessionToken(_))(_.value)
  given Schema[OtpChallengeId] = stringOpaque("OtpChallengeId")(OtpChallengeId(_))(_.value)
  given Schema[OtpCode]        = stringOpaque("OtpCode")(OtpCode(_))(_.value)

  given Schema[EmployeeRole]     = Schema.derived
  given Schema[Principal]        = Schema.derived
  given Schema[LoginIdentifier]  = Schema.derived
  given Schema[AuthMethod]       = Schema.derived
  given Schema[AuthSession]      = Schema.derived
  given Schema[OtpChallengeView] = Schema.derived
  given Schema[StaffMemberView]  = Schema.derived
  given Schema[CreateStaffCmd]   = Schema.derived
  given Schema[IdentityError]    = Schema.derived
