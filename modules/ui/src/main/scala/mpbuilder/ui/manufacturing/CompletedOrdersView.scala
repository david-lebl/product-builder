package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.domain.manufacturing.*
import mpbuilder.domain.model.Language
import mpbuilder.ui.ProductBuilderViewModel

object CompletedOrdersView:

  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "mfg-orders-view",

      div(
        cls := "mfg-page-header",
        h2(child.text <-- lang.map {
          case Language.En => "Completed Orders"
          case Language.Cs => "Dokoncene objednavky"
        }),
      ),

      children <-- ManufacturingViewModel.completedOrders.combineWith(lang).map { case (orders, l) =>
        if orders.isEmpty then
          List(div(cls := "mfg-empty-message",
            if l == Language.En then "No completed orders yet."
            else "Zatim zadne dokoncene objednavky."
          ))
        else
          orders.sortBy(-_.createdAt).map { order =>
            StationView.orderCard(order, l, None)
          }
      },
    )
