package mpbuilder.identity
package impl
package http

import mpbuilder.commons.*
import sttp.model.StatusCode
import sttp.tapir.ztapir.*
import sttp.tapir.server.ServerEndpoint
import zio.*

import AuthEndpoints.*

/** Server logic for the auth endpoints: translate wire → service call → wire.
  *
  * The interesting part is [[toResponse]]. HTTP status codes carry meaning to clients and to
  * anything watching the logs, so mapping every failure to 400 would be lazy — but so would
  * distinguishing them in a way that leaks. `InvalidCredentials` is 401 with a deliberately vague
  * message; nothing here tells a caller whether an account exists.
  */
private[identity] object AuthRoutes:

  def apply(auth: AuthService): List[ServerEndpoint[Any, Task]] =
    List(
      AuthEndpoints.register.zServerLogic { req =>
        auth
          .register(Register(req.email, req.password, req.customerId))
          .mapBoth(toResponse, toTokens)
      },
      AuthEndpoints.login.zServerLogic { req =>
        auth.login(LoginWithPassword(req.email, req.password)).mapBoth(toResponse, toTokens)
      },
      AuthEndpoints.refresh.zServerLogic { req =>
        auth.refresh(req.refreshToken).mapBoth(toResponse, toTokens)
      },
      AuthEndpoints.requestOtp.zServerLogic { req =>
        auth
          .requestOtp(req.email)
          .mapBoth(
            toResponse,
            c => ChallengeResponse(c.challengeId, c.deliveredTo, c.expiresInSeconds),
          )
      },
      AuthEndpoints.verifyOtp.zServerLogic { req =>
        auth.verifyOtp(req.challengeId, req.code).mapBoth(toResponse, toTokens)
      },
      AuthEndpoints.me
        .zServerSecurityLogic[Any, String](token => ZIO.succeed(token))
        .serverLogic { token => _ =>
          auth
            .me(token)
            .mapBoth(
              toResponse,
              u => MeResponse(u.id, u.email, u.roles, u.customerId, u.status),
            )
        },
    )

  private def toTokens(t: AuthTokens): TokensResponse =
    TokensResponse(t.accessToken, t.refreshToken, t.expiresInSeconds)

  private def toResponse(error: AuthError): (StatusCode, ErrorResponse) =
    val status = error match
      case AuthError.InvalidCredentials      => StatusCode.Unauthorized
      case AuthError.TokenExpired            => StatusCode.Unauthorized
      case AuthError.TokenInvalid            => StatusCode.Unauthorized
      case AuthError.OtpExpired              => StatusCode.Unauthorized
      case AuthError.OtpIncorrect            => StatusCode.Unauthorized
      case AuthError.AccountSuspended(_)     => StatusCode.Forbidden
      case AuthError.AccountNotApproved      => StatusCode.Forbidden
      case AuthError.NotAuthorised           => StatusCode.Forbidden
      case AuthError.EmailAlreadyRegistered(_) => StatusCode.Conflict
      case AuthError.InvalidEmail(_)         => StatusCode.BadRequest
      case AuthError.WeakPassword(_)         => StatusCode.BadRequest

    val items = error match
      // Accumulated problems stay accumulated on the wire — the form shows every broken rule at
      // once rather than making the user discover them one submission at a time.
      case AuthError.WeakPassword(problems) =>
        problems.toList.map(p => ErrorItem(p.code, p.message(Language.En)))
      case other =>
        List(ErrorItem(other.toString.takeWhile(_ != '('), other.message(Language.En)))

    (status, ErrorResponse(items))
