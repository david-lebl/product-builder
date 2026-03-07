package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.domain.manufacturing.*
import mpbuilder.domain.model.Language
import mpbuilder.ui.ProductBuilderViewModel

object AllOrdersView:

  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "mfg-orders-view",

      div(
        cls := "mfg-page-header",
        h2(child.text <-- lang.map {
          case Language.En => "All Orders"
          case Language.Cs => "Vsechny objednavky"
        }),
        button(
          cls := "mfg-btn mfg-btn-primary",
          child.text <-- lang.map {
            case Language.En => "+ Add Sample Order"
            case Language.Cs => "+ Pridat testovaci objednavku"
          },
          onClick --> { _ => ManufacturingViewModel.addSampleOrder() },
        ),
      ),

      children <-- ManufacturingViewModel.state.combineWith(lang).map { case (state, l) =>
        if state.orders.isEmpty then
          List(div(cls := "mfg-empty-message",
            if l == Language.En then "No orders yet. Add a sample order to get started."
            else "Zatim zadne objednavky. Pridejte testovaci objednavku."
          ))
        else
          state.orders.sortBy(-_.createdAt).map { order =>
            val currentStationName = order.currentStationId
              .flatMap(sid => state.stations.find(_.id == sid))
              .map(_.name(l))
              .getOrElse(if l == Language.En then "Done" else "Hotovo")

            div(
              cls := "mfg-order-row",
              span(cls := "mfg-order-row-id", order.id.value),
              span(cls := "mfg-order-row-customer", order.customerName),
              span(cls := "mfg-order-row-product", order.configuration.category.name(l)),
              span(cls := "mfg-order-row-qty", order.quantity.toString),
              span(cls := "mfg-order-row-station", currentStationName),
              div(
                cls := "mfg-progress-bar mfg-progress-bar-sm",
                div(
                  cls := "mfg-progress-fill",
                  styleAttr := s"width: ${progressPercent(order)}%",
                ),
              ),
            )
          }
      },
    )

  private def progressPercent(order: ManufacturingOrder): Int =
    if order.totalSteps == 0 then 0
    else (order.stepsCompleted * 100) / order.totalSteps
