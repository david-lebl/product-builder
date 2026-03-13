package mpbuilder.domain

import zio.test.*
import mpbuilder.domain.model.*
import mpbuilder.domain.service.*
import mpbuilder.domain.sample.SampleCustomers

object LoginServiceSpec extends ZIOSpecDefault:

  private val customers = SampleCustomers.all
  private val now = 1700000000000L

  def spec = suite("LoginService")(
    suite("lookupCustomer")(
      test("finds customer by business ID") {
        val result = LoginService.lookupCustomer("12345678", IdentifierType.BusinessId, customers)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.id == SampleCustomers.printShopProId,
        )
      },
      test("finds customer by VAT ID") {
        val result = LoginService.lookupCustomer("CZ12345678", IdentifierType.VatId, customers)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.id == SampleCustomers.printShopProId,
        )
      },
      test("finds customer by email (case-insensitive)") {
        val result = LoginService.lookupCustomer("JAN@PRINTSHOPPRO.CZ", IdentifierType.Email, customers)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.id == SampleCustomers.printShopProId,
        )
      },
      test("trims whitespace from identifier") {
        val result = LoginService.lookupCustomer("  12345678  ", IdentifierType.BusinessId, customers)
        assertTrue(result.toEither.isRight)
      },
      test("fails when customer not found") {
        val result = LoginService.lookupCustomer("NONEXISTENT", IdentifierType.BusinessId, customers)
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[LoginError.CustomerNotFound],
        )
      },
      test("rejects inactive customer") {
        // packagingLtd has status Inactive
        val result = LoginService.lookupCustomer("44556677", IdentifierType.BusinessId, customers)
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[LoginError.CustomerInactive],
        )
      },
      test("rejects suspended customer") {
        // suspendedCo has status Suspended
        val result = LoginService.lookupCustomer("66778899", IdentifierType.BusinessId, customers)
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[LoginError.CustomerSuspended],
        )
      },
      test("rejects pending-approval customer as not found") {
        // pendingCo has status PendingApproval
        val result = LoginService.lookupCustomer("11009988", IdentifierType.BusinessId, customers)
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[LoginError.CustomerNotFound],
        )
      },
    ),
    suite("generateOtp")(
      test("generates a 6-digit OTP token") {
        val otp = LoginService.generateOtp(SampleCustomers.printShopPro, now)
        assertTrue(
          otp.customerId == SampleCustomers.printShopProId,
          otp.token.length == 6,
          otp.token.forall(_.isDigit),
          otp.createdAt == now,
          otp.expiresAt == now + LoginService.otpValidityMillis,
        )
      },
      test("generates deterministic token for same input") {
        val otp1 = LoginService.generateOtp(SampleCustomers.printShopPro, now)
        val otp2 = LoginService.generateOtp(SampleCustomers.printShopPro, now)
        assertTrue(otp1.token == otp2.token)
      },
      test("generates different tokens for different timestamps") {
        val otp1 = LoginService.generateOtp(SampleCustomers.printShopPro, now)
        val otp2 = LoginService.generateOtp(SampleCustomers.printShopPro, now + 1000L)
        assertTrue(otp1.token != otp2.token)
      },
    ),
    suite("validateOtp")(
      test("validates correct OTP and returns session") {
        val otp = LoginService.generateOtp(SampleCustomers.printShopPro, now)
        val result = LoginService.validateOtp(otp.token, otp, now + 60000L)
        assertTrue(
          result.toEither.isRight,
          result.toEither.toOption.get.customerId == SampleCustomers.printShopProId,
          result.toEither.toOption.get.expiresAt == now + 60000L + LoginService.sessionValidityMillis,
        )
      },
      test("rejects expired OTP") {
        val otp = LoginService.generateOtp(SampleCustomers.printShopPro, now)
        val result = LoginService.validateOtp(otp.token, otp, otp.expiresAt + 1L)
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[LoginError.OtpExpired],
        )
      },
      test("rejects wrong OTP token") {
        val otp = LoginService.generateOtp(SampleCustomers.printShopPro, now)
        val result = LoginService.validateOtp("000000", otp, now + 60000L)
        assertTrue(
          result.toEither.isLeft,
          result.toEither.swap.toOption.get.head.isInstanceOf[LoginError.OtpInvalid],
        )
      },
      test("trims whitespace from input token") {
        val otp = LoginService.generateOtp(SampleCustomers.printShopPro, now)
        val result = LoginService.validateOtp(s"  ${otp.token}  ", otp, now + 60000L)
        assertTrue(result.toEither.isRight)
      },
    ),
    suite("isSessionValid")(
      test("returns true for valid session") {
        val session = LoginSession(
          sessionId = SessionId.unsafe("session-1"),
          customerId = SampleCustomers.printShopProId,
          createdAt = now,
          expiresAt = now + LoginService.sessionValidityMillis,
        )
        assertTrue(LoginService.isSessionValid(session, now + 1000L))
      },
      test("returns false for expired session") {
        val session = LoginSession(
          sessionId = SessionId.unsafe("session-1"),
          customerId = SampleCustomers.printShopProId,
          createdAt = now,
          expiresAt = now + LoginService.sessionValidityMillis,
        )
        assertTrue(!LoginService.isSessionValid(session, session.expiresAt + 1L))
      },
      test("returns true at exact expiry time") {
        val session = LoginSession(
          sessionId = SessionId.unsafe("session-1"),
          customerId = SampleCustomers.printShopProId,
          createdAt = now,
          expiresAt = now + LoginService.sessionValidityMillis,
        )
        assertTrue(LoginService.isSessionValid(session, session.expiresAt))
      },
    ),
    suite("bilingual messages")(
      test("IdentifierType display names are bilingual") {
        assertTrue(
          IdentifierType.BusinessId.displayName(Language.En) == "Business ID (IČO)",
          IdentifierType.BusinessId.displayName(Language.Cs) == "IČO",
          IdentifierType.VatId.displayName(Language.En) == "VAT ID (DIČ)",
          IdentifierType.VatId.displayName(Language.Cs) == "DIČ",
          IdentifierType.Email.displayName(Language.En) == "Email",
          IdentifierType.Email.displayName(Language.Cs) == "Email",
        )
      },
      test("LoginError messages are bilingual") {
        val notFound = LoginError.CustomerNotFound("12345", IdentifierType.BusinessId)
        val expired = LoginError.OtpExpired(SampleCustomers.printShopProId)
        val invalid = LoginError.OtpInvalid(SampleCustomers.printShopProId)
        val inactive = LoginError.CustomerInactive(SampleCustomers.printShopProId)
        val suspended = LoginError.CustomerSuspended(SampleCustomers.printShopProId)
        val sessionExpired = LoginError.SessionExpired(SessionId.unsafe("session-1"))
        assertTrue(
          notFound.message(Language.En).contains("No customer found"),
          notFound.message(Language.Cs).contains("nebyl nalezen"),
          expired.message(Language.En).contains("expired"),
          expired.message(Language.Cs).contains("vypršel"),
          invalid.message(Language.En).contains("invalid"),
          invalid.message(Language.Cs).contains("neplatný"),
          inactive.message(Language.En).contains("inactive"),
          inactive.message(Language.Cs).contains("neaktivní"),
          suspended.message(Language.En).contains("suspended"),
          suspended.message(Language.Cs).contains("pozastaven"),
          sessionExpired.message(Language.En).contains("expired"),
          sessionExpired.message(Language.Cs).contains("vypršela"),
        )
      },
    ),
  )
