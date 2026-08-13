package mpbuilder.ui

import com.raquo.laminar.api.L.*
import mpbuilder.api.{CatalogResponse, OrderResponse}
import mpbuilder.ui.I18n.t
import mpbuilder.ui.configurator.*

object App:

  /** Loads the catalog bundle, then runs the configurator. */
  def view(): HtmlElement =
    val loaded: Var[Option[Either[String, CatalogResponse]]] = Var(None)

    div(
      cls := "app",
      onMountCallback { ctx =>
        import scala.concurrent.ExecutionContext.Implicits.global
        ApiClient.getCatalog.onComplete {
          case scala.util.Success(result) => loaded.set(Some(result))
          case scala.util.Failure(err)    => loaded.set(Some(Left(err.getMessage)))
        }
      },
      child <-- loaded.signal.map {
        case None               => div(cls := "loading", "Načítám katalog…")
        case Some(Left(error))  => div(cls := "error-panel", s"Katalog se nepodařilo načíst: $error")
        case Some(Right(bundle)) => configurator(new ConfiguratorState(bundle))
      },
    )

  private def configurator(state: ConfiguratorState): HtmlElement =
    div(
      headerTag(
        cls := "app-header",
        h1("Product Builder"),
        p(cls := "muted", "Kalkulace tiskovin a odeslání objednávky ke schválení"),
      ),
      child <-- state.confirmed.signal
        .combineWith(state.config)
        .map { (confirmed: Option[OrderResponse], config: Option[?]) =>
          confirmed match
            case Some(order) => confirmation(state, order)
            case None =>
              if config.isEmpty then CategoryStep.view(state)
              else configuratorLayout(state)
        },
    )

  private def configuratorLayout(state: ConfiguratorState): HtmlElement =
    div(
      button(
        cls := "btn btn-link",
        "← Změnit produkt",
        onClick --> (_ => state.clearCategory()),
      ),
      h2(child.text <-- state.category.map(_.map(c => t(c.name)).getOrElse(""))),
      div(
        cls := "configurator-grid",
        div(
          cls := "configurator-main",
          ComponentEditor.view(state),
          DetailsStep.view(state),
          ContactForm.view(state),
        ),
        div(
          cls := "configurator-side",
          PricePanel.view(state),
        ),
      ),
    )

  private def confirmation(state: ConfiguratorState, order: OrderResponse): HtmlElement =
    div(
      cls := "confirmation",
      h2("Objednávka přijata 🎉"),
      p(
        "Vaše objednávka byla zařazena do fronty ke schválení. Číslo objednávky: ",
        strong(order.id.toString),
      ),
      p(s"Celková cena: ", strong(Format.money(order.breakdown.total))),
      p(cls := "muted", s"Potvrzení pošleme na ${order.contact.email}."),
      button(
        cls := "btn btn-primary",
        "Nová objednávka",
        onClick --> { _ =>
          state.confirmed.set(None)
          state.clearCategory()
        },
      ),
    )
