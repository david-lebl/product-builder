package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.domain.manufacturing.*
import mpbuilder.domain.model.Language
import mpbuilder.ui.ProductBuilderViewModel

object StationView:

  def apply(stationId: StationId): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val station = ManufacturingViewModel.stations.find(_.id == stationId)

    station match
      case None => div("Station not found")
      case Some(st) =>
        div(
          cls := "mfg-station-view",

          div(
            cls := "mfg-page-header",
            h2(child.text <-- lang.map(l => st.name(l))),
          ),

          // Ready to pull section
          div(
            cls := "mfg-section",
            h3(
              cls := "mfg-section-title",
              child.text <-- lang.map {
                case Language.En => "Ready to Pull"
                case Language.Cs => "Pripraveno k prevzeti"
              },
            ),
            children <-- ManufacturingViewModel.ordersReadyForStation(stationId).combineWith(lang).map {
              case (orders, l) =>
                if orders.isEmpty then
                  List(div(cls := "mfg-empty-message",
                    if l == Language.En then "No orders ready to pull from previous station."
                    else "Zadne objednavky pripravene k prevzeti z predchozi stanice."
                  ))
                else
                  orders.map(order => orderCard(order, l, Some(
                    button(
                      cls := "mfg-btn mfg-btn-primary mfg-btn-sm",
                      if l == Language.En then "Pull" else "Prevzit",
                      onClick --> { _ =>
                        ManufacturingViewModel.pullOrder(order.id, stationId)
                      },
                    )
                  )))
            },
          ),

          // Current queue
          div(
            cls := "mfg-section",
            h3(
              cls := "mfg-section-title",
              child.text <-- lang.map {
                case Language.En => "Station Queue"
                case Language.Cs => "Fronta stanice"
              },
            ),
            children <-- ManufacturingViewModel.ordersAtStation(stationId).combineWith(lang).map {
              case (orders, l) =>
                if orders.isEmpty then
                  List(div(cls := "mfg-empty-message",
                    if l == Language.En then "No orders at this station."
                    else "Zadne objednavky na teto stanici."
                  ))
                else
                  orders.map { order =>
                    val status = order.currentStatus.getOrElse(OrderStatus.Queued)
                    orderCard(order, l, Some(actionButtons(order, status, l)))
                  }
            },
          ),
        )

  private def actionButtons(
      order: ManufacturingOrder,
      status: OrderStatus,
      lang: Language,
  ): Element =
    status match
      case OrderStatus.Queued =>
        div(
          cls := "mfg-order-actions",
          button(
            cls := "mfg-btn mfg-btn-success mfg-btn-sm",
            if lang == Language.En then "Start" else "Zahajit",
            onClick --> { _ => ManufacturingViewModel.startOrder(order.id) },
          ),
          button(
            cls := "mfg-btn mfg-btn-warning mfg-btn-sm",
            if lang == Language.En then "Hold" else "Pozastavit",
            onClick --> { _ => ManufacturingViewModel.holdOrder(order.id) },
          ),
        )
      case OrderStatus.InProgress =>
        div(
          cls := "mfg-order-actions",
          button(
            cls := "mfg-btn mfg-btn-success mfg-btn-sm",
            if lang == Language.En then "Complete" else "Dokoncit",
            onClick --> { _ => ManufacturingViewModel.completeOrder(order.id) },
          ),
          button(
            cls := "mfg-btn mfg-btn-warning mfg-btn-sm",
            if lang == Language.En then "Hold" else "Pozastavit",
            onClick --> { _ => ManufacturingViewModel.holdOrder(order.id) },
          ),
        )
      case OrderStatus.OnHold =>
        div(
          cls := "mfg-order-actions",
          button(
            cls := "mfg-btn mfg-btn-primary mfg-btn-sm",
            if lang == Language.En then "Resume" else "Obnovit",
            onClick --> { _ => ManufacturingViewModel.resumeOrder(order.id) },
          ),
        )
      case OrderStatus.Completed =>
        div(
          cls := "mfg-order-actions",
          span(
            cls := "mfg-status-badge mfg-status-completed",
            if lang == Language.En then "Completed" else "Dokonceno",
          ),
        )

  private[manufacturing] def orderCard(
      order: ManufacturingOrder,
      lang: Language,
      actions: Option[Element],
  ): Element =
    val status = order.currentStatus.getOrElse(OrderStatus.Completed)
    div(
      cls := s"mfg-order-card mfg-order-${statusClass(status)}",

      div(
        cls := "mfg-order-header",
        span(cls := "mfg-order-id", order.id.value),
        span(
          cls := s"mfg-status-badge mfg-status-${statusClass(status)}",
          statusLabel(status, lang),
        ),
      ),
      div(
        cls := "mfg-order-body",
        div(
          cls := "mfg-order-detail",
          span(cls := "mfg-detail-label",
            if lang == Language.En then "Customer:" else "Zakaznik:"),
          span(order.customerName),
        ),
        div(
          cls := "mfg-order-detail",
          span(cls := "mfg-detail-label",
            if lang == Language.En then "Product:" else "Produkt:"),
          span(order.configuration.category.name(lang)),
        ),
        div(
          cls := "mfg-order-detail",
          span(cls := "mfg-detail-label",
            if lang == Language.En then "Quantity:" else "Mnozstvi:"),
          span(order.quantity.toString),
        ),
        div(
          cls := "mfg-order-detail",
          span(cls := "mfg-detail-label",
            if lang == Language.En then "Progress:" else "Postup:"),
          div(
            cls := "mfg-progress-bar",
            div(
              cls := "mfg-progress-fill",
              styleAttr := s"width: ${progressPercent(order)}%",
            ),
          ),
          span(cls := "mfg-progress-text", s"${order.stepsCompleted}/${order.totalSteps}"),
        ),
      ),
      actions.map(a => div(cls := "mfg-order-footer", a)).getOrElse(emptyNode),
    )

  private def statusClass(status: OrderStatus): String = status match
    case OrderStatus.Queued     => "queued"
    case OrderStatus.InProgress => "progress"
    case OrderStatus.Completed  => "completed"
    case OrderStatus.OnHold     => "hold"

  private def statusLabel(status: OrderStatus, lang: Language): String = status match
    case OrderStatus.Queued     => if lang == Language.En then "Queued" else "Ve fronte"
    case OrderStatus.InProgress => if lang == Language.En then "In Progress" else "V procesu"
    case OrderStatus.Completed  => if lang == Language.En then "Completed" else "Dokonceno"
    case OrderStatus.OnHold     => if lang == Language.En then "On Hold" else "Pozastaveno"

  private def progressPercent(order: ManufacturingOrder): Int =
    if order.totalSteps == 0 then 0
    else (order.stepsCompleted * 100) / order.totalSteps
