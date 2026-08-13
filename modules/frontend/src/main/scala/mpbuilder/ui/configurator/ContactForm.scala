package mpbuilder.ui.configurator

import com.raquo.laminar.api.L.*
import mpbuilder.api.CreateOrderRequest
import mpbuilder.domain.config.CustomerContact
import mpbuilder.ui.ApiClient

object ContactForm:

  /** Contact details + the place-order action. Submits the exact configuration
    * the calculator priced; the server re-validates and re-prices it.
    */
  def view(state: ConfiguratorState): HtmlElement =
    val name    = Var("")
    val email   = Var("")
    val phone   = Var("")
    val company = Var("")

    val contactComplete: Signal[Boolean] =
      name.signal
        .combineWith(email.signal, phone.signal)
        .map((n: String, e: String, p: String) => n.trim.nonEmpty && e.contains("@") && p.trim.nonEmpty)

    val canSubmit: Signal[Boolean] =
      state.isValid
        .combineWith(contactComplete, state.submitting.signal)
        .map((valid: Boolean, contact: Boolean, submitting: Boolean) => valid && contact && !submitting)

    def submit(): Unit =
      state.draft.now().foreach { config =>
        val request = CreateOrderRequest(
          contact = CustomerContact(
            name = name.now().trim,
            email = email.now().trim,
            phone = phone.now().trim,
            company = Some(company.now().trim).filter(_.nonEmpty),
          ),
          configuration = config,
        )
        state.submitting.set(true)
        import scala.concurrent.ExecutionContext.Implicits.global
        ApiClient.submitOrder(request).onComplete { result =>
          state.submitting.set(false)
          result.toOption match
            case Some(Right(order))   => state.confirmed.set(Some(order))
            case Some(Left(apiError)) => state.serverErrors.set(apiError.errors)
            case None =>
              state.serverErrors.set(
                List(
                  mpbuilder.api.ErrorDto(
                    "Network",
                    mpbuilder.domain.LocalizedText("Network error — please retry", "Chyba sítě — zkuste to prosím znovu"),
                  )
                )
              )
        }
      }

    div(
      cls := "contact-form",
      h3("Kontaktní údaje"),
      div(cls := "field", label("Jméno *"), input(value <-- name, onInput.mapToValue --> name)),
      div(cls := "field", label("E-mail *"), input(typ := "email", value <-- email, onInput.mapToValue --> email)),
      div(cls := "field", label("Telefon *"), input(typ := "tel", value <-- phone, onInput.mapToValue --> phone)),
      div(cls := "field", label("Firma"), input(value <-- company, onInput.mapToValue --> company)),
      button(
        cls := "btn btn-primary btn-submit",
        disabled <-- canSubmit.map(!_),
        child.text <-- state.submitting.signal.map(s => if s then "Odesílám…" else "Odeslat objednávku ke schválení"),
        onClick --> (_ => submit()),
      ),
      p(
        cls := "muted",
        "Objednávka bude zařazena do fronty nových objednávek a počká na schválení tiskárnou.",
      ),
    )
