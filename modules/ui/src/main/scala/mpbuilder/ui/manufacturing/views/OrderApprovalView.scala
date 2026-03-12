package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.model.ManufacturingOrder.*
import mpbuilder.domain.model.ArtworkCheck.*
import mpbuilder.domain.model.CheckStatus.*
import mpbuilder.domain.model.PaymentStatus.*
import mpbuilder.domain.pricing.Money
import mpbuilder.ui.manufacturing.*
import mpbuilder.uikit.containers.*

/** Order Approval View — manager/prepress view for reviewing and approving orders. */
object OrderApprovalView:

  def apply(): HtmlElement =
    val searchVar = Var("")
    val selectedId: Var[Option[String]] = Var(None)

    val statusFilterDef = FilterDef(
      id = "status",
      label = "Status",
      options = Val(ApprovalStatus.values.toList.map(s => (s.toString, s.toString))),
      selectedValues = Var(Set(ApprovalStatus.Placed.toString, ApprovalStatus.PendingChanges.toString)),
    )

    val paymentFilterDef = FilterDef(
      id = "payment",
      label = "Payment",
      options = Val(PaymentStatus.values.toList.map(s => (s.toString, s.displayName))),
      selectedValues = Var(PaymentStatus.values.toSet.map(_.toString)),
    )

    val filteredOrders: Signal[List[ManufacturingOrder]] =
      ManufacturingViewModel.orders
        .combineWith(statusFilterDef.selectedValues.signal, paymentFilterDef.selectedValues.signal, searchVar.signal)
        .map { case (ords, statuses, payStatuses, query) =>
          val q = query.trim.toLowerCase
          ords
            .filter(mo => statuses.contains(mo.approvalStatus.toString))
            .filter(mo => payStatuses.contains(mo.paymentStatus.toString))
            .filter { mo =>
              q.isEmpty ||
              mo.order.id.value.toLowerCase.contains(q) ||
              mo.customerName.toLowerCase.contains(q) ||
              mo.itemSummary.toLowerCase.contains(q)
            }
        }

    val tableConfig = SplitTableConfig[ManufacturingOrder](
      columns = List(
        ColumnDef("Order ID", mo => span(mo.order.id.value), Some(_.order.id.value), Some("100px")),
        ColumnDef("Date", mo => span(formatTimestamp(mo.createdAt)), Some(_.createdAt.toString)),
        ColumnDef("Customer", mo => span(mo.customerName), Some(_.customerName)),
        ColumnDef("Items", mo => span(mo.itemSummary)),
        ColumnDef("Total", mo => span(cls := "price-value", formatMoney(mo.order.total)),
          Some(mo => f"${mo.order.total.value}%.2f"), Some("100px")),
        ColumnDef("Payment", mo => paymentBadge(mo.paymentStatus), Some(_.paymentStatus.toString), Some("100px")),
        ColumnDef("Artwork", mo => artworkIndicator(mo.artworkCheck), width = Some("90px")),
        ColumnDef("Status", mo => approvalBadge(mo.approvalStatus), Some(_.approvalStatus.toString), Some("130px")),
        ColumnDef("Actions", mo => approvalActions(mo), width = Some("200px")),
      ),
      rowKey = _.order.id.value,
      filters = List(statusFilterDef, paymentFilterDef),
      searchPlaceholder = "Search orders, customers…",
      onRowSelect = Some(mo => selectedId.set(Some(mo.order.id.value))),
      emptyMessage = "No orders matching your filters",
    )

    val sidePanel: Signal[Option[HtmlElement]] =
      selectedId.signal.combineWith(ManufacturingViewModel.orders).map { case (selId, ords) =>
        selId.flatMap(id => ords.find(_.order.id.value == id)).map(renderApprovalPanel)
      }

    div(
      cls := "manufacturing-order-approval",
      h2(cls := "manufacturing-view-title", "Order Approval"),
      SplitTableView(
        config = tableConfig,
        items = filteredOrders,
        selectedKey = selectedId.signal,
        searchQuery = searchVar,
        sidePanel = sidePanel,
      ),
    )

  private def approvalBadge(status: ApprovalStatus): HtmlElement =
    val (text, cls_) = status match
      case ApprovalStatus.Placed         => ("Placed", "badge badge-pending")
      case ApprovalStatus.Approved       => ("Approved", "badge badge-completed")
      case ApprovalStatus.Rejected       => ("Rejected", "badge badge-error")
      case ApprovalStatus.PendingChanges => ("Pending Changes", "badge badge-warning")
      case ApprovalStatus.OnHold         => ("On Hold", "badge badge-warning")
    span(cls := cls_, text)

  private def paymentBadge(status: PaymentStatus): HtmlElement =
    val (text, cls_) = status match
      case PaymentStatus.Confirmed => ("✅ Paid", "badge badge-completed")
      case PaymentStatus.Pending   => ("⏳ Pending", "badge badge-pending")
      case PaymentStatus.Failed    => ("❌ Failed", "badge badge-error")
    span(cls := cls_, text)

  private def artworkIndicator(ac: ArtworkCheck): HtmlElement =
    if ac.isFullyPassed then span(cls := "badge badge-completed", "✅ OK")
    else if ac.hasIssues then span(cls := "badge badge-error", "❌ Issue")
    else if ac.hasWarnings then span(cls := "badge badge-warning", "⚠️ Warn")
    else span(cls := "badge badge-muted", "⬜ Unchecked")

  private def approvalActions(mo: ManufacturingOrder): HtmlElement =
    div(
      cls := "approval-actions",
      mo.approvalStatus match
        case ApprovalStatus.Placed | ApprovalStatus.PendingChanges =>
          List(
            button(
              cls := "btn-success btn-sm",
              "✓ Approve",
              onClick.stopPropagation --> { _ => ManufacturingViewModel.approveOrder(mo.order.id.value) },
            ),
            button(
              cls := "btn-danger btn-sm",
              "✗ Reject",
              onClick.stopPropagation --> { _ => ManufacturingViewModel.rejectOrder(mo.order.id.value) },
            ),
          )
        case ApprovalStatus.OnHold =>
          List(
            button(
              cls := "btn-success btn-sm",
              "✓ Approve",
              onClick.stopPropagation --> { _ => ManufacturingViewModel.approveOrder(mo.order.id.value) },
            ),
          )
        case _ => List(emptyNode),
    )

  private def renderApprovalPanel(mo: ManufacturingOrder): HtmlElement =
    div(
      cls := "approval-detail-panel",

      div(
        cls := "detail-panel-header",
        h3(mo.order.id.value),
        approvalBadge(mo.approvalStatus),
      ),

      // Priority & Deadline assignment
      div(
        cls := "detail-panel-section",
        h4("Priority & Deadline"),
        div(
          cls := "approval-priority-controls",
          span(cls := "approval-control-label", "Priority: "),
          Priority.values.toList.map { p =>
            button(
              cls := (if mo.priority == p then "priority-btn priority-btn--active" else "priority-btn"),
              cls := (p match
                case Priority.Rush   => " priority-btn--rush"
                case Priority.Normal => " priority-btn--normal"
                case Priority.Low    => " priority-btn--low"
              ),
              p.displayName,
              onClick --> { _ => ManufacturingViewModel.setOrderPriority(mo.order.id.value, p) },
            )
          },
        ),
        div(
          cls := "approval-deadline-display",
          span(cls := "approval-control-label", "Deadline: "),
          mo.deadline match
            case Some(dl) =>
              span(formatTimestamp(dl))
            case None =>
              span(cls := "text-muted", "Not set"),
        ),
      ),

      // Payment status
      div(
        cls := "detail-panel-section",
        h4("Payment"),
        div(
          cls := "approval-payment-controls",
          PaymentStatus.values.toList.map { ps =>
            button(
              cls := (if mo.paymentStatus == ps then "payment-btn payment-btn--active" else "payment-btn"),
              s"${ps.icon} ${ps.displayName}",
              onClick --> { _ => ManufacturingViewModel.setPaymentStatus(mo.order.id.value, ps) },
            )
          },
        ),
      ),

      // Artwork check section
      div(
        cls := "detail-panel-section",
        h4("Artwork Review"),
        div(
          cls := "artwork-check-grid",
          artworkCheckRow("Resolution", mo.artworkCheck.resolution, mo.order.id.value,
            (ac, s) => ac.copy(resolution = s)),
          artworkCheckRow("Bleed", mo.artworkCheck.bleed, mo.order.id.value,
            (ac, s) => ac.copy(bleed = s)),
          artworkCheckRow("Color Profile", mo.artworkCheck.colorProfile, mo.order.id.value,
            (ac, s) => ac.copy(colorProfile = s)),
        ),
        if mo.artworkCheck.notes.nonEmpty then
          p(cls := "artwork-notes", mo.artworkCheck.notes)
        else emptyNode,
      ),

      // Customer info
      div(
        cls := "detail-panel-section",
        h4("Customer"),
        div(cls := "detail-customer-info",
          p(strong("Name: "), mo.customerName),
          p(strong("Email: "), mo.order.checkoutInfo.contactInfo.email),
          mo.order.checkoutInfo.contactInfo.phone match
            case "" => emptyNode
            case phone => p(strong("Phone: "), phone)
          ,
          mo.order.checkoutInfo.contactInfo.company.map(c => p(strong("Company: "), c)).getOrElse(emptyNode),
        ),
      ),

      // Delivery info
      div(
        cls := "detail-panel-section",
        h4("Delivery"),
        p(mo.order.checkoutInfo.deliveryOption.map {
          case DeliveryOption.PickupAtShop(_) => "Pickup at Shop"
          case DeliveryOption.CourierStandard => "Courier Standard"
          case DeliveryOption.CourierExpress  => "Courier Express"
          case DeliveryOption.CourierEconomy  => "Courier Economy"
        }.getOrElse("Not selected")),
      ),

      // Order items detail
      div(
        cls := "detail-panel-section",
        h4("Order Items"),
        mo.order.basket.items.zipWithIndex.map { case (item, idx) =>
          div(cls := "approval-item-detail",
            div(cls := "approval-item-header",
              span(cls := "approval-item-name", s"${idx + 1}. ${item.configuration.category.name(Language.En)}"),
              span(cls := "approval-item-qty", s"× ${item.quantity}"),
            ),
            div(cls := "approval-item-specs",
              item.configuration.components.map { comp =>
                div(cls := "approval-item-component",
                  span(cls := "approval-component-role", comp.role.toString + ": "),
                  span(comp.material.name(Language.En)),
                  span(cls := "approval-component-ink", s" (${comp.inkConfiguration.notation})"),
                  if comp.finishes.nonEmpty then
                    span(cls := "approval-component-finishes",
                      s" + ${comp.finishes.map(_.name(Language.En)).mkString(", ")}")
                  else emptyNode,
                )
              },
            ),
            div(cls := "approval-item-price",
              span(cls := "price-value", formatMoney(item.priceBreakdown.total)),
            ),
          )
        },
      ),

      // Total
      div(
        cls := "detail-panel-section detail-panel-total",
        h4("Total"),
        span(cls := "price-value price-value--large", formatMoney(mo.order.total)),
      ),

      // Approval notes
      div(
        cls := "detail-panel-section",
        h4("Internal Notes"),
        if mo.approvalNotes.nonEmpty then
          p(cls := "approval-notes-text", mo.approvalNotes)
        else
          p(cls := "text-muted", "No notes"),
      ),

      // Actions
      if mo.approvalStatus == ApprovalStatus.Placed || mo.approvalStatus == ApprovalStatus.PendingChanges then
        div(
          cls := "detail-panel-actions",
          button(
            cls := "btn-success",
            "✓ Approve Order",
            onClick --> { _ => ManufacturingViewModel.approveOrder(mo.order.id.value) },
          ),
          button(
            cls := "btn-warning",
            "⏸ Hold",
            onClick --> { _ => ManufacturingViewModel.holdOrder(mo.order.id.value) },
          ),
          button(
            cls := "btn-danger",
            "✗ Reject Order",
            onClick --> { _ => ManufacturingViewModel.rejectOrder(mo.order.id.value) },
          ),
        )
      else if mo.approvalStatus == ApprovalStatus.OnHold then
        div(
          cls := "detail-panel-actions",
          button(
            cls := "btn-success",
            "✓ Approve Order",
            onClick --> { _ => ManufacturingViewModel.approveOrder(mo.order.id.value) },
          ),
          button(
            cls := "btn-warning",
            "📝 Request Changes",
            onClick --> { _ => ManufacturingViewModel.requestChanges(mo.order.id.value) },
          ),
        )
      else emptyNode,
    )

  private def artworkCheckRow(
      label: String,
      currentStatus: CheckStatus,
      orderId: String,
      updater: (ArtworkCheck, CheckStatus) => ArtworkCheck,
  ): HtmlElement =
    div(
      cls := "artwork-check-row",
      span(cls := "artwork-check-label", label),
      div(
        cls := "artwork-check-buttons",
        CheckStatus.values.toList.map { cs =>
          button(
            cls := (if currentStatus == cs then "artwork-btn artwork-btn--active" else "artwork-btn"),
            cls := (cs match
              case CheckStatus.Passed     => " artwork-btn--passed"
              case CheckStatus.Warning    => " artwork-btn--warning"
              case CheckStatus.Failed     => " artwork-btn--failed"
              case CheckStatus.NotChecked => " artwork-btn--unchecked"
            ),
            cs.icon,
            title := cs.displayName,
            onClick --> { _ =>
              ManufacturingViewModel.manufacturingOrders.now().find(_.order.id.value == orderId).foreach { mo =>
                val updated = updater(mo.artworkCheck, cs)
                ManufacturingViewModel.updateArtworkCheck(orderId, updated)
              }
            },
          )
        },
      ),
    )

  private def formatMoney(money: Money): String =
    f"${money.value}%.2f CZK"

  private def formatTimestamp(ts: Long): String =
    val d = new scalajs.js.Date(ts.toDouble)
    val day = f"${d.getDate().toInt}%02d"
    val month = f"${d.getMonth().toInt + 1}%02d"
    val year = d.getFullYear().toInt
    val hours = f"${d.getHours().toInt}%02d"
    val minutes = f"${d.getMinutes().toInt}%02d"
    s"$day.$month.$year $hours:$minutes"
