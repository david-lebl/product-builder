package mpbuilder.json

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import mpbuilder.identity.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.json.SharedCodecs.given

/** Codecs for the public `identity` model. */
object IdentityCodecs:
  given JsonValueCodec[SessionToken]   = stringCodec("SessionToken")(SessionToken(_))(_.value)
  given JsonValueCodec[OtpChallengeId] = stringCodec("OtpChallengeId")(OtpChallengeId(_))(_.value)
  given JsonValueCodec[OtpCode]        = stringCodec("OtpCode")(OtpCode(_))(_.value)

  given JsonValueCodec[EmployeeRole]     = JsonCodecMaker.make(config)
  given JsonValueCodec[Principal]        = JsonCodecMaker.make(config)
  given JsonValueCodec[LoginIdentifier]  = JsonCodecMaker.make(config)
  given JsonValueCodec[AuthMethod]       = JsonCodecMaker.make(config)
  given JsonValueCodec[AuthSession]      = JsonCodecMaker.make(config)
  given JsonValueCodec[OtpChallengeView] = JsonCodecMaker.make(config)
  given JsonValueCodec[StaffMemberView]  = JsonCodecMaker.make(config)
  given JsonValueCodec[CreateStaffCmd]   = JsonCodecMaker.make(config)
  given JsonValueCodec[IdentityError]    = JsonCodecMaker.make(config)
