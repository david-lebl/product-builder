package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.{ProductBuilderViewModel, LoginState}
import mpbuilder.domain.model.*

/** Compact login widget displayed in the product builder header area.
  *
  * States: logged out → entering identifier → entering OTP → logged in.
  * Logged-in state shows company name, tier badge, and logout button.
  */
object LoginWidget:

  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "login-widget",
      child <-- ProductBuilderViewModel.loginState.combineWith(lang).map { case (loginState, l) =>
        loginState match
          case LoginState.LoggedOut =>
            loggedOutView(l)
          case LoginState.EnteringIdentifier(identifier, identifierType, error) =>
            identifierView(identifier, identifierType, error, l)
          case LoginState.EnteringOtp(customer, _, otpInput, error) =>
            otpView(customer, otpInput, error, l)
          case LoginState.LoggedIn(customer, _) =>
            loggedInView(customer, l)
      },
    )

  private def loggedOutView(l: Language): Element =
    button(
      cls := "login-widget-btn",
      l match
        case Language.En => "🔑 Agency Login"
        case Language.Cs => "🔑 Přihlášení agentury",
      onClick --> { _ => ProductBuilderViewModel.startLogin() },
    )

  private def identifierView(
      identifier: String,
      identifierType: IdentifierType,
      error: Option[String],
      l: Language,
  ): Element =
    val identifierVar = Var(identifier)
    val typeVar = Var(identifierType)

    div(
      cls := "login-widget-form",
      div(
        cls := "login-widget-title",
        l match
          case Language.En => "Agency Login"
          case Language.Cs => "Přihlášení agentury",
      ),
      select(
        cls := "login-widget-select",
        value <-- typeVar.signal.map(_.toString),
        IdentifierType.values.map { t =>
          option(
            value := t.toString,
            t.displayName(l),
          )
        }.toSeq,
        onChange.mapToValue --> { v =>
          typeVar.set(IdentifierType.valueOf(v))
        },
      ),
      input(
        cls := "login-widget-input",
        typ := "text",
        placeholder := (l match
          case Language.En => identifierType match
            case IdentifierType.Email      => "your@email.com"
            case IdentifierType.BusinessId => "IČO (e.g. 12345678)"
            case IdentifierType.VatId      => "DIČ (e.g. CZ12345678)"
          case Language.Cs => identifierType match
            case IdentifierType.Email      => "vas@email.cz"
            case IdentifierType.BusinessId => "IČO (např. 12345678)"
            case IdentifierType.VatId      => "DIČ (např. CZ12345678)"
        ),
        controlled(
          value <-- identifierVar.signal,
          onInput.mapToValue --> identifierVar.writer,
        ),
      ),
      error.map(e => div(cls := "login-widget-error", e)).getOrElse(emptyNode),
      div(
        cls := "login-widget-actions",
        button(
          cls := "login-widget-submit",
          l match
            case Language.En => "Send OTP →"
            case Language.Cs => "Odeslat OTP →",
          onClick --> { _ =>
            ProductBuilderViewModel.submitIdentifier(identifierVar.now(), typeVar.now())
          },
        ),
        button(
          cls := "login-widget-cancel",
          l match
            case Language.En => "Cancel"
            case Language.Cs => "Zrušit",
          onClick --> { _ => ProductBuilderViewModel.cancelLogin() },
        ),
      ),
    )

  private def otpView(
      customer: Customer,
      otpInput: String,
      error: Option[String],
      l: Language,
  ): Element =
    val otpVar = Var(otpInput)
    val companyName = customer.companyInfo.map(_.companyName).getOrElse(customer.contactInfo.email)

    div(
      cls := "login-widget-form",
      div(
        cls := "login-widget-title",
        l match
          case Language.En => s"OTP sent to $companyName"
          case Language.Cs => s"OTP zasláno: $companyName",
      ),
      div(
        cls := "login-widget-hint",
        l match
          case Language.En => "Enter the 6-digit code:"
          case Language.Cs => "Zadejte 6místný kód:",
      ),
      input(
        cls := "login-widget-input login-widget-otp-input",
        typ := "text",
        maxLength := 6,
        placeholder := "000000",
        controlled(
          value <-- otpVar.signal,
          onInput.mapToValue --> otpVar.writer,
        ),
      ),
      error.map(e => div(cls := "login-widget-error", e)).getOrElse(emptyNode),
      div(
        cls := "login-widget-actions",
        button(
          cls := "login-widget-submit",
          l match
            case Language.En => "Verify →"
            case Language.Cs => "Ověřit →",
          onClick --> { _ =>
            ProductBuilderViewModel.submitOtp(otpVar.now())
          },
        ),
        button(
          cls := "login-widget-cancel",
          l match
            case Language.En => "Cancel"
            case Language.Cs => "Zrušit",
          onClick --> { _ => ProductBuilderViewModel.cancelLogin() },
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
