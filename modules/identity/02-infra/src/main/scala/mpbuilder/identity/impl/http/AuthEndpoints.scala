package mpbuilder.identity
package impl
package http

import mpbuilder.commons.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import zio.json.*

/** Wire shapes and endpoint descriptions for authentication.
  *
  * Kept apart from the server logic so the same descriptions can generate the OpenAPI document and,
  * later, a typed client for the Laminar front end.
  */
private[identity] object AuthEndpoints:

  // ── Wire types ───────────────────────────────────────────────────────────

  final case class RegisterRequest(email: String, password: String, customerId: Option[String] = None)
  final case class LoginRequest(email: String, password: String)
  final case class RefreshRequest(refreshToken: String)
  final case class OtpRequestBody(email: String)
  final case class OtpVerifyRequest(challengeId: String, code: String)

  final case class TokensResponse(accessToken: String, refreshToken: String, expiresInSeconds: Long)
  final case class ChallengeResponse(challengeId: String, deliveredTo: String, expiresInSeconds: Long)
  final case class MeResponse(
      id: String,
      email: String,
      roles: Set[String],
      customerId: Option[String],
      status: String,
  )

  /** One error shape for the whole API.
    *
    * `errors` is a list because domain validation accumulates: a weak password reports every rule
    * it broke in one response, rather than one per attempt.
    */
  final case class ErrorResponse(errors: List[ErrorItem])
  final case class ErrorItem(code: String, message: String)

  given JsonCodec[RegisterRequest] = DeriveJsonCodec.gen
  given JsonCodec[LoginRequest] = DeriveJsonCodec.gen
  given JsonCodec[RefreshRequest] = DeriveJsonCodec.gen
  given JsonCodec[OtpRequestBody] = DeriveJsonCodec.gen
  given JsonCodec[OtpVerifyRequest] = DeriveJsonCodec.gen
  given JsonCodec[TokensResponse] = DeriveJsonCodec.gen
  given JsonCodec[ChallengeResponse] = DeriveJsonCodec.gen
  given JsonCodec[MeResponse] = DeriveJsonCodec.gen
  given JsonCodec[ErrorItem] = DeriveJsonCodec.gen
  given JsonCodec[ErrorResponse] = DeriveJsonCodec.gen

  // ── Endpoints ────────────────────────────────────────────────────────────

  private val base = endpoint
    .in("api" / "v1" / "auth")
    .errorOut(
      oneOf[(StatusCode, ErrorResponse)](
        oneOfVariantValueMatcher(statusCode.and(jsonBody[ErrorResponse]))({ case _ => true })
      )
    )
    .tag("auth")

  val register: PublicEndpoint[RegisterRequest, (StatusCode, ErrorResponse), TokensResponse, Any] =
    base.post
      .in("register")
      .in(jsonBody[RegisterRequest])
      .out(jsonBody[TokensResponse])
      .summary("Create an account and sign in")

  val login: PublicEndpoint[LoginRequest, (StatusCode, ErrorResponse), TokensResponse, Any] =
    base.post
      .in("login")
      .in(jsonBody[LoginRequest])
      .out(jsonBody[TokensResponse])
      .summary("Sign in with e-mail and password")

  val refresh: PublicEndpoint[RefreshRequest, (StatusCode, ErrorResponse), TokensResponse, Any] =
    base.post
      .in("refresh")
      .in(jsonBody[RefreshRequest])
      .out(jsonBody[TokensResponse])
      .summary("Exchange a refresh token for a new token pair")

  val requestOtp: PublicEndpoint[OtpRequestBody, (StatusCode, ErrorResponse), ChallengeResponse, Any] =
    base.post
      .in("otp" / "request")
      .in(jsonBody[OtpRequestBody])
      .out(jsonBody[ChallengeResponse])
      .summary("Request a one-time sign-in code")
      .description(
        "Always succeeds for a well-formed address, whether or not an account exists, so this " +
          "endpoint cannot be used to discover which addresses are registered."
      )

  val verifyOtp: PublicEndpoint[OtpVerifyRequest, (StatusCode, ErrorResponse), TokensResponse, Any] =
    base.post
      .in("otp" / "verify")
      .in(jsonBody[OtpVerifyRequest])
      .out(jsonBody[TokensResponse])
      .summary("Exchange a one-time code for tokens")

  val me: Endpoint[String, Unit, (StatusCode, ErrorResponse), MeResponse, Any] =
    base.get
      .securityIn(auth.bearer[String]())
      .in("me")
      .out(jsonBody[MeResponse])
      .summary("The signed-in user")

  val all = List(register, login, refresh, requestOtp, verifyOtp, me)
