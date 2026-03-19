package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.{ProductBuilderViewModel, LoginState}
import mpbuilder.domain.model.*
import mpbuilder.domain.service.LoginService

/** Compact login widget displayed in the product builder header area.
  *
  * When logged out: shows "👤 Guest" with a small "🔑 Login" button that opens a popup.
  * When entering identifier / OTP: shows a dropdown popup form over the top bar.
  * When logged in: shows company name, tier badge, and logout button.
  */
object LoginWidget:

  /** Whether the login popup is open */
  private val popupOpen: Var[Boolean] = Var(false)

  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "login-widget",
      child <-- ProductBuilderViewModel.loginState.combineWith(lang).map { case (loginState, l) =>
        loginState match
          case LoginState.LoggedOut =>
            guestView(l)
          case LoginState.EnteringIdentifier(identifier, identifierType, error) =>
            guestWithPopup(identifierPopup(identifier, identifierType, error, l), l)
          case LoginState.EnteringOtp(customer, otpToken, otpInput, error) =>
            guestWithPopup(otpPopup(customer, otpToken, otpInput, error, l), l)
          case LoginState.LoggedIn(customer, _) =>
            loggedInView(customer, l)
      },
    )

  /** Stages of the checkout agency login mini-flow */
  private enum CheckoutLoginStage:
    case Identifier, Otp

  /** Agency login form that can be embedded in the checkout view. */
  def checkoutAgencyLogin(info: CheckoutInfo, l: Language): Element =
    val identifierVar = Var("")
    val typeVar = Var(IdentifierType.Email)
    val otpVar = Var("")
    val stageVar: Var[CheckoutLoginStage] = Var(CheckoutLoginStage.Identifier)
    val errorVar: Var[Option[String]] = Var(None)
    val otpHintVar: Var[Option[String]] = Var(None)

    div(
      cls := "checkout-auth-option checkout-agency-login",
      h3(if l == Language.Cs then "🔑 Přihlášení agentury" else "🔑 Agency Login"),
      p(cls := "checkout-auth-desc",
        if l == Language.Cs then "Pro zákazníky se smlouvou — přihlaste se IČO, DIČ nebo e-mailem a získejte smluvní ceny."
        else "For contract customers — sign in with Business ID, VAT ID, or email and get your negotiated prices."
      ),

      child <-- stageVar.signal.map {
        case CheckoutLoginStage.Identifier =>
          div(
            select(
              value <-- typeVar.signal.map(_.toString),
              IdentifierType.values.map { t =>
                option(value := t.toString, t.displayName(l))
              }.toSeq,
              onChange.mapToValue --> { v => typeVar.set(IdentifierType.valueOf(v)) },
            ),
            input(
              typ := "text",
              placeholder := sampleHint(l),
              controlled(
                value <-- identifierVar.signal,
                onInput.mapToValue --> identifierVar.writer,
              ),
            ),
            child <-- errorVar.signal.map {
              case Some(e) => div(cls := "login-widget-error", e)
              case None    => emptyNode
            },
            button(
              cls := "checkout-btn",
              if l == Language.Cs then "Odeslat OTP →" else "Send OTP →",
              onClick --> { _ =>
                val result = LoginService.lookupCustomer(identifierVar.now(), typeVar.now(), ProductBuilderViewModel.allCustomers)
                result.fold(
                  errors => errorVar.set(Some(errors.map(_.message(l)).toList.mkString(", "))),
                  customer => {
                    val now = System.currentTimeMillis()
                    val otp = LoginService.generateOtp(customer, now)
                    ProductBuilderViewModel.stateVar.update(_.copy(
                      loginState = LoginState.EnteringOtp(customer, otp, "", None),
                    ))
                    otpHintVar.set(Some(otp.token))
                    errorVar.set(None)
                    stageVar.set(CheckoutLoginStage.Otp)
                  },
                )
              },
            ),
          )

        case CheckoutLoginStage.Otp =>
          val currentOtpToken = ProductBuilderViewModel.stateVar.now().loginState match
            case LoginState.EnteringOtp(_, token, _, _) => Some(token)
            case _ => None

          div(
            div(cls := "login-widget-otp-hint",
              child <-- otpHintVar.signal.map {
                case Some(token) =>
                  if l == Language.Cs then s"Demo: kód je $token (nebo nechte prázdné)"
                  else s"Demo: code is $token (or leave empty)"
                case None =>
                  if l == Language.Cs then "Zadejte 6místný kód (nebo nechte prázdné)"
                  else "Enter 6-digit code (or leave empty)"
              },
            ),
            input(
              typ := "text",
              cls := "login-widget-otp-input",
              maxLength := 6,
              placeholder := "000000",
              controlled(
                value <-- otpVar.signal,
                onInput.mapToValue --> otpVar.writer,
              ),
            ),
            child <-- errorVar.signal.map {
              case Some(e) => div(cls := "login-widget-error", e)
              case None    => emptyNode
            },
            button(
              cls := "checkout-btn",
              if l == Language.Cs then "Ověřit a přihlásit →" else "Verify & Sign In →",
              onClick --> { _ =>
                currentOtpToken match
                  case Some(otpToken) =>
                    ProductBuilderViewModel.submitOtp(otpVar.now())
                    // Check if login succeeded
                    ProductBuilderViewModel.stateVar.now().loginState match
                      case LoginState.LoggedIn(customer, _) =>
                        val ci = customer.contactInfo
                        val addr = customer.address
                        ProductBuilderViewModel.updateCheckoutInfo(info.copy(
                          customerType = CustomerType.Agency,
                          contactInfo = ci,
                          invoiceAddress = addr,
                        ))
                        ProductBuilderViewModel.checkoutNextStep()
                      case LoginState.EnteringOtp(_, _, _, Some(err)) =>
                        errorVar.set(Some(err))
                      case _ =>
                        errorVar.set(Some(
                          if l == Language.Cs then "Přihlášení se nezdařilo" else "Login failed"
                        ))
                  case None =>
                    errorVar.set(Some(
                      if l == Language.Cs then "Žádný OTP token" else "No OTP token"
                    ))
              },
            ),
            button(
              cls := "btn-secondary",
              if l == Language.Cs then "← Zpět" else "← Back",
              onClick --> { _ =>
                stageVar.set(CheckoutLoginStage.Identifier)
                errorVar.set(None)
                otpVar.set("")
              },
            ),
          )
      },
    )

  // ── Private views ────────────────────────────────────────────────────

  private def guestView(l: Language): Element =
    div(
      cls := "login-widget-guest-row",
      div(
        cls := "login-widget-guest",
        span(cls := "login-widget-guest-icon", "👤"),
        span(cls := "login-widget-guest-label",
          l match
            case Language.En => "Guest"
            case Language.Cs => "Host"
        ),
      ),
      button(
        cls := "login-widget-login-trigger",
        l match
          case Language.En => "🔑 Login"
          case Language.Cs => "🔑 Přihlásit",
        onClick --> { _ =>
          ProductBuilderViewModel.startLogin()
          popupOpen.set(true)
        },
      ),
    )

  private def guestWithPopup(popup: Element, l: Language): Element =
    div(
      cls := "login-widget-guest-row",
      div(
        cls := "login-widget-guest",
        span(cls := "login-widget-guest-icon", "👤"),
        span(cls := "login-widget-guest-label",
          l match
            case Language.En => "Guest"
            case Language.Cs => "Host"
        ),
      ),
      button(
        cls := "login-widget-login-trigger",
        l match
          case Language.En => "🔑 Login"
          case Language.Cs => "🔑 Přihlásit",
      ),
      div(
        cls := "login-widget-popup-backdrop",
        onClick --> { _ =>
          ProductBuilderViewModel.cancelLogin()
          popupOpen.set(false)
        },
      ),
      popup,
    )

  private def identifierPopup(
      identifier: String,
      identifierType: IdentifierType,
      error: Option[String],
      l: Language,
  ): Element =
    val identifierVar = Var(identifier)
    val typeVar = Var(identifierType)

    div(
      cls := "login-widget-popup",
      div(cls := "login-widget-popup-title",
        l match
          case Language.En => "Agency Login"
          case Language.Cs => "Přihlášení agentury",
      ),
      p(cls := "login-widget-popup-hint",
        l match
          case Language.En => s"Demo: try jan@printshoppro.cz or IČO 12345678"
          case Language.Cs => s"Demo: zkuste jan@printshoppro.cz nebo IČO 12345678",
      ),
      select(
        value <-- typeVar.signal.map(_.toString),
        IdentifierType.values.map { t =>
          option(value := t.toString, t.displayName(l))
        }.toSeq,
        onChange.mapToValue --> { v => typeVar.set(IdentifierType.valueOf(v)) },
      ),
      input(
        typ := "text",
        placeholder := sampleHint(l),
        controlled(
          value <-- identifierVar.signal,
          onInput.mapToValue --> identifierVar.writer,
        ),
      ),
      error.map(e => div(cls := "login-widget-error", e)).getOrElse(emptyNode),
      div(
        cls := "login-widget-popup-actions",
        button(
          cls := "login-widget-popup-submit",
          l match
            case Language.En => "Send OTP →"
            case Language.Cs => "Odeslat OTP →",
          onClick --> { _ =>
            ProductBuilderViewModel.submitIdentifier(identifierVar.now(), typeVar.now())
          },
        ),
        button(
          cls := "login-widget-popup-cancel",
          l match
            case Language.En => "Cancel"
            case Language.Cs => "Zrušit",
          onClick --> { _ =>
            ProductBuilderViewModel.cancelLogin()
            popupOpen.set(false)
          },
        ),
      ),
    )

  private def otpPopup(
      customer: Customer,
      otpToken: OtpToken,
      otpInput: String,
      error: Option[String],
      l: Language,
  ): Element =
    val otpVar = Var(otpInput)
    val companyName = customer.companyInfo.map(_.companyName).getOrElse(customer.contactInfo.email)

    div(
      cls := "login-widget-popup",
      div(cls := "login-widget-popup-title",
        l match
          case Language.En => s"Verify: $companyName"
          case Language.Cs => s"Ověření: $companyName",
      ),
      div(cls := "login-widget-otp-hint",
        l match
          case Language.En => s"Demo: code is ${otpToken.token} (or leave empty)"
          case Language.Cs => s"Demo: kód je ${otpToken.token} (nebo nechte prázdné)",
      ),
      input(
        typ := "text",
        cls := "login-widget-otp-input",
        maxLength := 6,
        placeholder := "000000",
        controlled(
          value <-- otpVar.signal,
          onInput.mapToValue --> otpVar.writer,
        ),
      ),
      error.map(e => div(cls := "login-widget-error", e)).getOrElse(emptyNode),
      div(
        cls := "login-widget-popup-actions",
        button(
          cls := "login-widget-popup-submit",
          l match
            case Language.En => "Verify →"
            case Language.Cs => "Ověřit →",
          onClick --> { _ =>
            ProductBuilderViewModel.submitOtp(otpVar.now())
            ProductBuilderViewModel.stateVar.now().loginState match
              case _: LoginState.LoggedIn => popupOpen.set(false)
              case _ => ()
          },
        ),
        button(
          cls := "login-widget-popup-cancel",
          l match
            case Language.En => "Cancel"
            case Language.Cs => "Zrušit",
          onClick --> { _ =>
            ProductBuilderViewModel.cancelLogin()
            popupOpen.set(false)
          },
        ),
      ),
    )

  private def loggedInView(customer: Customer, l: Language): Element =
    val companyName = customer.companyInfo.map(_.companyName).getOrElse(customer.contactInfo.email)
    val tierLabel = customer.tier.displayName(l)
    val tierBadgeCls = customer.tier match
      case CustomerTier.Standard => "badge badge-muted"
      case CustomerTier.Silver   => "badge badge-info"
      case CustomerTier.Gold     => "badge badge-warning"
      case CustomerTier.Platinum => "badge badge-active"

    div(
      cls := "login-widget-logged-in",
      span(cls := "login-widget-company", companyName),
      span(cls := tierBadgeCls, tierLabel),
      button(
        cls := "login-widget-logout",
        l match
          case Language.En => "Logout"
          case Language.Cs => "Odhlásit",
        onClick --> { _ => ProductBuilderViewModel.logoutCustomer() },
      ),
    )

  private def sampleHint(l: Language): String =
    l match
      case Language.En => "e.g. jan@printshoppro.cz"
      case Language.Cs => "např. jan@printshoppro.cz"
