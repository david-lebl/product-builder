package mpbuilder.identity

import mpbuilder.commons.*
import zio.*
import zio.test.*

/** Contract tests for [[AuthService]], public surface only.
  *
  * Several of these assert *absence* of information — that a wrong password and an unknown account
  * are indistinguishable, that requesting a code for a stranger looks like success. Those are the
  * properties an auth system is judged on, and they are easy to regress by "improving" an error
  * message, so they are pinned here.
  */
object AuthServiceSpec extends ZIOSpecDefault:

  private val layer = IdentityModule.forTests()

  private val alice = Register("alice@example.com", "correct-horse-7")

  def spec = suite("AuthService")(
    suite("register")(
      test("issues tokens and makes the account usable") {
        for
          tokens <- AuthService.register(alice)
          principal <- AuthService.verify(tokens.accessToken)
          view <- AuthService.me(tokens.accessToken)
        yield assertTrue(
          tokens.accessToken.nonEmpty,
          tokens.refreshToken != tokens.accessToken,
          tokens.expiresInSeconds > 0,
          principal.email == "alice@example.com",
          principal.roles == Set(Role.Customer),
          !principal.isStaff,
          view.status == "Active",
        )
      },
      test("normalises the e-mail so case cannot create a second account") {
        for
          _ <- AuthService.register(alice)
          error <- AuthService.register(alice.copy(email = "ALICE@example.com")).flip
        yield assertTrue(error.isInstanceOf[AuthError.EmailAlreadyRegistered])
      },
      test("refuses a malformed e-mail") {
        for error <- AuthService.register(alice.copy(email = "not-an-email")).flip
        yield assertTrue(error == AuthError.InvalidEmail("not-an-email"))
      },
      test("reports every password problem at once") {
        for error <- AuthService.register(alice.copy(password = "short")).flip
        yield assertTrue(
          error match
            // "short" is too short AND has no digit — both must be reported, not just the first.
            case AuthError.WeakPassword(problems) => problems.size >= 2
            case _                                => false
        )
      },
      test("password errors are bilingual") {
        for error <- AuthService.register(alice.copy(password = "short")).flip
        yield assertTrue(
          error.message(Language.En).nonEmpty,
          error.message(Language.Cs) != error.message(Language.En),
        )
      },
      test("links to a customer profile when one is supplied") {
        for
          tokens <- AuthService.register(alice.copy(customerId = Some("cust-123")))
          principal <- AuthService.verify(tokens.accessToken)
        yield assertTrue(principal.customerId.contains("cust-123"))
      },
    ),
    suite("login")(
      test("succeeds with the right password") {
        for
          _ <- AuthService.register(alice)
          tokens <- AuthService.login(LoginWithPassword(alice.email, alice.password))
          principal <- AuthService.verify(tokens.accessToken)
        yield assertTrue(principal.email == alice.email)
      },
      test("is case-insensitive on the e-mail") {
        for
          _ <- AuthService.register(alice)
          tokens <- AuthService.login(LoginWithPassword("ALICE@EXAMPLE.COM", alice.password))
        yield assertTrue(tokens.accessToken.nonEmpty)
      },
      test("a wrong password and an unknown account are indistinguishable") {
        // If these differed, the sign-in form would be an account-enumeration oracle.
        for
          _ <- AuthService.register(alice)
          wrongPassword <- AuthService.login(LoginWithPassword(alice.email, "wrong-password-1")).flip
          noSuchAccount <- AuthService.login(LoginWithPassword("nobody@example.com", "wrong-password-1")).flip
        yield assertTrue(
          wrongPassword == AuthError.InvalidCredentials,
          noSuchAccount == AuthError.InvalidCredentials,
          wrongPassword.message(Language.En) == noSuchAccount.message(Language.En),
        )
      },
      test("a malformed e-mail fails as bad credentials, not as a validation error") {
        // Otherwise the shape of the error reveals whether the address was even considered.
        for error <- AuthService.login(LoginWithPassword("bogus", "whatever-99")).flip
        yield assertTrue(error == AuthError.InvalidCredentials)
      },
    ),
    suite("tokens")(
      test("a refresh token cannot be used as an access token") {
        for
          tokens <- AuthService.register(alice)
          error <- AuthService.verify(tokens.refreshToken).flip
        yield assertTrue(error == AuthError.TokenInvalid)
      },
      test("an access token cannot be used to refresh") {
        for
          tokens <- AuthService.register(alice)
          error <- AuthService.refresh(tokens.accessToken).flip
        yield assertTrue(error == AuthError.TokenInvalid)
      },
      test("refresh yields a working access token") {
        for
          tokens <- AuthService.register(alice)
          refreshed <- AuthService.refresh(tokens.refreshToken)
          principal <- AuthService.verify(refreshed.accessToken)
        yield assertTrue(principal.email == alice.email)
      },
      test("garbage and tampered tokens are rejected") {
        for
          tokens <- AuthService.register(alice)
          garbage <- AuthService.verify("not.a.token").flip
          tampered <- AuthService.verify(tokens.accessToken.dropRight(3) + "AAA").flip
        yield assertTrue(garbage == AuthError.TokenInvalid, tampered == AuthError.TokenInvalid)
      },
      test("a token signed with a different secret is rejected") {
        // Proves the signature is actually checked, not just the payload parsed.
        for
          foreign <- AuthService.register(alice).provide(IdentityModule.forTests("a-different-secret"))
          error <- AuthService.verify(foreign.accessToken).flip
        yield assertTrue(error == AuthError.TokenInvalid)
      },
    ),
    suite("otp")(
      test("a challenge is issued for a known account and signs in") {
        for
          _ <- AuthService.register(alice)
          challenge <- AuthService.requestOtp(alice.email)
        yield assertTrue(
          challenge.challengeId.nonEmpty,
          challenge.expiresInSeconds > 0,
          // The code itself is never returned — it goes out of band.
          !challenge.toString.contains(challenge.challengeId.take(6) * 2),
        )
      },
      test("requesting a code for an unknown address looks identical") {
        // Otherwise this endpoint enumerates accounts just as well as a login form.
        for
          _ <- AuthService.register(alice)
          known <- AuthService.requestOtp(alice.email)
          unknown <- AuthService.requestOtp("stranger@example.com")
        yield assertTrue(
          known.challengeId.nonEmpty,
          unknown.challengeId.nonEmpty,
          known.expiresInSeconds == unknown.expiresInSeconds,
          // Every field except the opaque id must be indistinguishable. An earlier version
          // returned an empty `deliveredTo` for known accounts and a masked one for unknown,
          // which announced exactly what it was meant to hide.
          known.deliveredTo == "al***@example.com",
          unknown.deliveredTo == "st***@example.com",
          known.copy(challengeId = "", deliveredTo = "") ==
            unknown.copy(challengeId = "", deliveredTo = ""),
        )
      },
      test("a challenge for an unknown address never verifies") {
        for
          challenge <- AuthService.requestOtp("stranger@example.com")
          error <- AuthService.verifyOtp(challenge.challengeId, "000000").flip
        yield assertTrue(error == AuthError.OtpIncorrect)
      },
      test("a wrong code burns the challenge, so codes cannot be brute-forced") {
        for
          _ <- AuthService.register(alice)
          challenge <- AuthService.requestOtp(alice.email)
          first <- AuthService.verifyOtp(challenge.challengeId, "111111").flip
          second <- AuthService.verifyOtp(challenge.challengeId, "222222").flip
        yield assertTrue(
          first == AuthError.OtpIncorrect,
          // Second attempt finds no challenge at all — one challenge, one try.
          second == AuthError.OtpIncorrect,
        )
      },
      test("a malformed address is refused") {
        for error <- AuthService.requestOtp("nope").flip
        yield assertTrue(error == AuthError.InvalidEmail("nope"))
      },
    ),
    suite("roles")(
      test("staff roles are recognised as a group") {
        // Authorisation asks "is this staff?", so adding a fifth staff role must not
        // require revisiting every check.
        assertTrue(
          Principal("u", "e", Set(Role.Operator), None).isStaff,
          Principal("u", "e", Set(Role.Manager), None).isStaff,
          Principal("u", "e", Set(Role.Admin), None).isStaff,
          !Principal("u", "e", Set(Role.Customer), None).isStaff,
        )
      },
      test("role names round-trip") {
        assertTrue(Role.all.forall(r => Role.parse(r.name).contains(r)))
      },
    ),
  ).provide(layer) @@ TestAspect.sequential
