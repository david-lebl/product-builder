package mpbuilder.ui.storefront

import kyo.*
import kyo.UI.*
import kyo.Style.*
import mpbuilder.identity.*
import mpbuilder.shared.*
import mpbuilder.storefront.*
import mpbuilder.ui.components.CommonComponents.*
import mpbuilder.ui.theme.AppTheme.*
import mpbuilder.ui.theme.AppTheme.Palette.*

/** Responsive Authentication Modal supporting Passwordless OTP (Email / IČO / DIČ + Demo Inbox) and Password login. */
object AuthModal:

  private enum AuthTab:
    case Otp, Password

  private enum IdKind:
    case Email, Ico, Dic

  private final case class DemoAccount(
      label: String,
      tier: String,
      email: String,
      ico: Option[String],
      dic: Option[String],
  )

  private val demoAccounts: List[DemoAccount] = List(
    DemoAccount("Print Shop Pro", "Gold · Net-14", "jan@printshoppro.cz", Some("12345678"), Some("CZ12345678")),
    DemoAccount("MegaCorp", "Platinum · Custom", "petr@megacorp.cz", Some("11223344"), Some("CZ11223344")),
    DemoAccount("Graphic Design Co.", "Silver", "marie@graphicdesign.cz", Some("87654321"), Some("CZ87654321")),
    DemoAccount("Local Print", "Standard", "eva@localprint.cz", None, None),
  )

  def render(state: StorefrontState): UI < Async =
    for
      tabRef       <- Signal.initRef[AuthTab](AuthTab.Otp)
      idKindRef    <- Signal.initRef[IdKind](IdKind.Email)
      idInputRef   <- Signal.initRef("jan@printshoppro.cz")
      otpCodeRef   <- Signal.initRef("")
      challengeRef <- Signal.initRef[Option[OtpChallengeView]](None)
      demoOtpRef   <- Signal.initRef[Option[OtpCode]](None)
      emailRef     <- Signal.initRef("jan@printshoppro.cz")
      passwordRef  <- Signal.initRef("demo1234")
      errorRef     <- Signal.initRef[Option[String]](None)
      busyRef      <- Signal.initRef(false)
    yield
      def resetAndClose: Unit < Sync =
        for
          _ <- errorRef.set(None)
          _ <- challengeRef.set(None)
          _ <- demoOtpRef.set(None)
          _ <- otpCodeRef.set("")
          _ <- state.authModalOpen.set(false)
        yield ()

      def requestOtp: Unit < Async =
        for
          _    <- errorRef.set(None)
          _    <- busyRef.set(true)
          kind <- idKindRef.get
          raw  <- idInputRef.get.map(_.trim)
          idEither: Either[String, LoginIdentifier] = kind match
            case IdKind.Email =>
              EmailAddress(raw).toEither.left.map(_.mkString(", ")).map(LoginIdentifier.Email.apply)
            case IdKind.Ico =>
              NonEmptyString(raw).toEither.left.map(_.mkString(", ")).map(LoginIdentifier.CompanyRegNo.apply)
            case IdKind.Dic =>
              NonEmptyString(raw).toEither.left.map(_.mkString(", ")).map(LoginIdentifier.VatId.apply)
          _ <- idEither match
                 case Left(msg) => errorRef.set(Some(msg))
                 case Right(ident) =>
                   Abort.run[StorefrontError](state.client.requestCustomerOtp(ident)).flatMap {
                     case Result.Success(ch) =>
                       for
                         _       <- challengeRef.set(Some(ch))
                         codeOpt <- state.client.demoOtpCode(ch.challengeId)
                         _       <- demoOtpRef.set(codeOpt)
                       yield ()
                     case Result.Failure(err) => errorRef.set(Some(err.message))
                     case Result.Panic(ex)    => errorRef.set(Some(Option(ex.getMessage).getOrElse("OTP error")))
                   }
          _ <- busyRef.set(false)
        yield ()

      def verifyOtp: Unit < Async =
        for
          _     <- errorRef.set(None)
          _     <- busyRef.set(true)
          chOpt <- challengeRef.get
          raw   <- otpCodeRef.get.map(_.trim)
          _ <- (chOpt, OtpCode(raw).toEither) match
                 case (None, _) => errorRef.set(Some("No active OTP challenge"))
                 case (_, Left(errs)) => errorRef.set(Some(errs.mkString(", ")))
                 case (Some(ch), Right(code)) =>
                   Abort.run[StorefrontError](state.client.verifyOtp(ch.challengeId, code)).flatMap {
                     case Result.Success(res) =>
                       for
                         _ <- challengeRef.set(None)
                         _ <- demoOtpRef.set(None)
                         _ <- otpCodeRef.set("")
                         _ <- state.completeAuth(res)
                       yield ()
                     case Result.Failure(err) => errorRef.set(Some(err.message))
                     case Result.Panic(ex)    => errorRef.set(Some(Option(ex.getMessage).getOrElse("Verify error")))
                   }
          _ <- busyRef.set(false)
        yield ()

      def loginPassword: Unit < Async =
        for
          _   <- errorRef.set(None)
          _   <- busyRef.set(true)
          em  <- emailRef.get.map(_.trim)
          pw  <- passwordRef.get
          _ <- (EmailAddress(em).toEither, NonEmptyString(pw).toEither) match
                 case (Left(e), _) => errorRef.set(Some(e.mkString(", ")))
                 case (_, Left(e)) => errorRef.set(Some(e.mkString(", ")))
                 case (Right(email), Right(pass)) =>
                   Abort.run[StorefrontError](state.client.loginWithPassword(email, pass)).flatMap {
                     case Result.Success(res) => state.completeAuth(res)
                     case Result.Failure(err) => errorRef.set(Some(err.message))
                     case Result.Panic(ex)    => errorRef.set(Some(Option(ex.getMessage).getOrElse("Login error")))
                   }
          _ <- busyRef.set(false)
        yield ()

      val viewState =
        tabRef
          .combineLatest(idKindRef)
          .combineLatest(challengeRef)
          .combineLatest(demoOtpRef)
          .combineLatest(errorRef)
          .combineLatest(busyRef)

      UI.when(state.authModalOpen)(
        div.cssClass("mp-modal-backdrop")(
          div.cssClass("mp-modal")(
            // Modal header
            div.style(rowBetween)(
              div.style(Style.column.gap(2.px))(
                h2(state.i18n.t("auth-title")).style(sectionTitle.margin(0.px)),
                p(state.i18n.t("auth-subtitle")).style(smallText.margin(0.px)),
              ),
              button.cssClass("mp-btn mp-btn-ghost mp-btn-sm").onClick(resetAndClose)(state.i18n.t("nav-close")),
            ),

            viewState.render { case (((((tab, idKind), chOpt), demoCodeOpt), errOpt), busy) =>
              div.style(Style.column.gap(16.px))(
                // Mode Tabs
                div.style(Style.row.gap(8.px))(
                  button
                    .cssClass(if tab == AuthTab.Otp then "mp-pill-active" else "mp-pill")
                    .style(Style.flexGrow(1.0).justify(Justification.center))
                    .onClick(tabRef.set(AuthTab.Otp).andThen(errorRef.set(None)))(state.i18n.t("auth-tab-otp")),
                  button
                    .cssClass(if tab == AuthTab.Password then "mp-pill-active" else "mp-pill")
                    .style(Style.flexGrow(1.0).justify(Justification.center))
                    .onClick(tabRef.set(AuthTab.Password).andThen(errorRef.set(None)))(state.i18n.t("auth-tab-password")),
                ),

                // Error Banner
                errOpt
                  .map(msg => alertBanner(BadgeTone.Rose, span("Error"), List(msg)))
                  .getOrElse(UI.empty),

                tab match
                  case AuthTab.Otp =>
                    chOpt match
                      case None =>
                        div.style(Style.column.gap(12.px))(
                          div.style(Style.column.gap(6.px))(
                            label.style(labelStyle)(state.i18n.t("auth-id-type")),
                            div.style(Style.row.flexWrap(FlexWrap.wrap).gap(6.px))(
                              button
                                .cssClass(if idKind == IdKind.Email then "mp-pill-active" else "mp-pill")
                                .onClick(idKindRef.set(IdKind.Email).andThen(idInputRef.set("jan@printshoppro.cz")))(
                                  state.i18n.t("auth-id-email"),
                                ),
                              button
                                .cssClass(if idKind == IdKind.Ico then "mp-pill-active" else "mp-pill")
                                .onClick(idKindRef.set(IdKind.Ico).andThen(idInputRef.set("12345678")))(
                                  state.i18n.t("auth-id-ico"),
                                ),
                              button
                                .cssClass(if idKind == IdKind.Dic then "mp-pill-active" else "mp-pill")
                                .onClick(idKindRef.set(IdKind.Dic).andThen(idInputRef.set("CZ12345678")))(
                                  state.i18n.t("auth-id-dic"),
                                ),
                            ),
                          ),
                          div.cssClass("mp-field")(
                            input
                              .cssClass("mp-input")
                              .value(idInputRef)
                              .placeholder("jan@printshoppro.cz / 12345678 / CZ12345678"),
                          ),
                          button
                            .cssClass("mp-btn mp-btn-primary mp-btn-lg")
                            .disabled(busy)
                            .onClick(requestOtp)(state.i18n.t("auth-send-otp")),
                        )

                      case Some(ch) =>
                        div.style(Style.column.gap(12.px))(
                          div.style(Style.padding(12.px).rounded(10.px).bg(brandSoft).color(brandDark).fontSize(13.px))(
                            state.i18n.t("auth-otp-sent", Dict("dest" -> ch.maskedDestination)),
                          ),
                          // Static Demo OTP Inbox box
                          demoCodeOpt match
                            case Some(code) =>
                              div.style(
                                Style.row
                                  .justify(Justification.spaceBetween)
                                  .align(Alignment.center)
                                  .gap(10.px)
                                  .padding(10.px, 12.px)
                                  .rounded(10.px)
                                  .bg(emeraldSoft)
                                  .border(1.px, emeraldBorder),
                              )(
                                div.style(Style.column.gap(2.px))(
                                  span(state.i18n.t("auth-otp-demo-title")).style(Style.fontSize(11.px).fontWeight(FontWeight.w700).color(emeraldInk)),
                                  span(state.i18n.t("auth-otp-demo-hint", Dict("code" -> code.value))).style(Style.fontSize(14.px).fontWeight(FontWeight.w800).color(ink)),
                                ),
                                button
                                  .cssClass("mp-btn mp-btn-secondary mp-btn-sm")
                                  .onClick(otpCodeRef.set(code.value))(state.i18n.t("auth-otp-demo-fill")),
                              )
                            case None => UI.empty,
                          div.cssClass("mp-field")(
                            label.style(labelStyle)(state.i18n.t("auth-otp-code")),
                            input
                              .cssClass("mp-input")
                              .value(otpCodeRef)
                              .placeholder("123456"),
                          ),
                          div.style(Style.row.gap(8.px))(
                            button
                              .cssClass("mp-btn mp-btn-secondary")
                              .onClick(challengeRef.set(None).andThen(demoOtpRef.set(None)).andThen(errorRef.set(None)))(
                                state.i18n.t("auth-otp-back"),
                              ),
                            button
                              .cssClass("mp-btn mp-btn-primary")
                              .style(Style.flexGrow(1.0))
                              .disabled(busy)
                              .onClick(verifyOtp)(state.i18n.t("auth-otp-verify")),
                          ),
                        )

                  case AuthTab.Password =>
                    div.style(Style.column.gap(12.px))(
                      div.cssClass("mp-field")(
                        label.style(labelStyle)(state.i18n.t("auth-email-label")),
                        emailInput.cssClass("mp-input").value(emailRef).placeholder("jan@printshoppro.cz"),
                      ),
                      div.cssClass("mp-field")(
                        label.style(labelStyle)(state.i18n.t("auth-password-label")),
                        passwordInput.cssClass("mp-input").value(passwordRef).placeholder("••••••••"),
                      ),
                      button
                        .cssClass("mp-btn mp-btn-primary mp-btn-lg")
                        .disabled(busy)
                        .onClick(loginPassword)(state.i18n.t("auth-submit-password")),
                    ),

                // Quick Demo Accounts
                div.style(Style.column.gap(8.px).padding(12.px, 0.px, 0.px, 0.px).borderTop(1.px, Palette.border))(
                  span(state.i18n.t("auth-demo-accounts")).style(smallText.fontWeight(FontWeight.w700)),
                  div.cssClass("mp-option-grid")(
                    demoAccounts.map { acc =>
                      button
                        .cssClass("mp-option-card")
                        .style(Style.padding(8.px, 10.px).flexBasis(200.px))
                        .onClick {
                          for
                            _ <- emailRef.set(acc.email)
                            _ <- passwordRef.set("demo1234")
                            _ <- idKindRef.set(IdKind.Email)
                            _ <- idInputRef.set(acc.email)
                            _ <- challengeRef.set(None)
                            _ <- errorRef.set(None)
                          yield ()
                        }(
                          div.style(rowBetween)(
                            span(acc.label).style(Style.fontWeight(FontWeight.w700).fontSize(12.px).color(ink)),
                            badge(acc.tier, BadgeTone.Brand),
                          ),
                          span(acc.email).style(smallText),
                        )
                    }*
                  ),
                ),
              )
            },
          ),
        ),
      )
