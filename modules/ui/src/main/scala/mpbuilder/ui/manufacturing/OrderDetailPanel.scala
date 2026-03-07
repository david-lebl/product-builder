package mpbuilder.ui.manufacturing

import com.raquo.laminar.api.L.*
import mpbuilder.domain.manufacturing.*
import mpbuilder.domain.model.Language
import mpbuilder.ui.ProductBuilderViewModel
import scala.scalajs.js.Date

object OrderDetailPanel:

  def apply(
      order: ManufacturingOrder,
      stations: List[Station],
      lang: Language,
  ): Element =
    val status = order.currentStatus.getOrElse(OrderStatus.Completed)

    // Overlay + panel
    div(
      cls := "mfg-detail-overlay",
      onClick --> { e =>
        if e.target == e.currentTarget then
          ManufacturingViewModel.closeDetail()
      },

      div(
        cls := "mfg-detail-panel",

        // Header
        div(
          cls := "mfg-detail-header",
          div(
            h2(cls := "mfg-detail-title", order.id.value),
            span(
              cls := s"mfg-status-badge mfg-status-${statusClass(status)}",
              statusLabel(status, lang),
            ),
            span(
              cls := s"mfg-priority-badge mfg-priority-${order.priority.toString.toLowerCase}",
              priorityLabel(order.priority, lang),
            ),
          ),
          button(
            cls := "mfg-detail-close",
            "\u2715",
            onClick --> { _ => ManufacturingViewModel.closeDetail() },
          ),
        ),

        // Action bar
        div(
          cls := "mfg-detail-actions",
          actionButtons(order, status, lang),
        ),

        // Sections
        div(
          cls := "mfg-detail-body",

          // Order info
          div(
            cls := "mfg-detail-section",
            h3(if lang == Language.En then "Order Information" else "Informace o objednavce"),
            infoRow(if lang == Language.En then "Order ID" else "ID objednavky", order.orderId.value),
            infoRow(if lang == Language.En then "Customer" else "Zakaznik", order.customerName),
            infoRow(if lang == Language.En then "Product" else "Produkt", order.configuration.category.name(lang)),
            infoRow(if lang == Language.En then "Quantity" else "Mnozstvi", order.quantity.toString),
            infoRow(
              if lang == Language.En then "Printing Method" else "Metoda tisku",
              order.configuration.printingMethod.name(lang),
            ),
            infoRow(
              if lang == Language.En then "Material" else "Material",
              order.configuration.components.headOption.map(_.material.name(lang)).getOrElse("-"),
            ),
            infoRow(
              if lang == Language.En then "Total Price" else "Celkova cena",
              s"${order.priceBreakdown.total.value} ${order.priceBreakdown.currency}",
            ),
            infoRow(
              if lang == Language.En then "Created" else "Vytvoreno",
              formatTimestamp(order.createdAt),
            ),
            infoRow(
              if lang == Language.En then "Deadline" else "Termin",
              order.deadline.map(formatTimestamp).getOrElse("-"),
            ),
          ),

          // Priority & notes (editable)
          div(
            cls := "mfg-detail-section",
            h3(if lang == Language.En then "Priority & Notes" else "Priorita a poznamky"),
            div(
              cls := "mfg-detail-field",
              label(
                cls := "mfg-detail-field-label",
                if lang == Language.En then "Priority" else "Priorita",
              ),
              select(
                cls := "mfg-detail-select",
                option("Low", value := "Low", selected := (order.priority == OrderPriority.Low)),
                option("Normal", value := "Normal", selected := (order.priority == OrderPriority.Normal)),
                option("High", value := "High", selected := (order.priority == OrderPriority.High)),
                option("Urgent", value := "Urgent", selected := (order.priority == OrderPriority.Urgent)),
                onChange.mapToValue --> { v =>
                  val p = v match
                    case "Low"    => OrderPriority.Low
                    case "High"   => OrderPriority.High
                    case "Urgent" => OrderPriority.Urgent
                    case _        => OrderPriority.Normal
                  ManufacturingViewModel.updateOrderPriority(order.id, p)
                },
              ),
            ),
            div(
              cls := "mfg-detail-field",
              label(
                cls := "mfg-detail-field-label",
                if lang == Language.En then "Notes" else "Poznamky",
              ),
              textArea(
                cls := "mfg-detail-textarea",
                defaultValue := order.notes,
                placeholder := (if lang == Language.En then "Add notes..." else "Pridat poznamky..."),
                onBlur.mapToValue --> { v =>
                  ManufacturingViewModel.updateOrderNotes(order.id, v)
                },
              ),
            ),
          ),

          // Production progress
          div(
            cls := "mfg-detail-section",
            h3(if lang == Language.En then "Production Steps" else "Vyrobni kroky"),
            div(
              cls := "mfg-detail-steps",
              order.steps.map { step =>
                val station = stations.find(_.id == step.stationId)
                val stationName = station.map(_.name(lang)).getOrElse("-")
                val isCurrent = order.currentStationId.contains(step.stationId)

                div(
                  cls := s"mfg-detail-step mfg-detail-step-${statusClass(step.status)}",
                  cls := (if isCurrent then "mfg-detail-step-current" else ""),
                  div(
                    cls := "mfg-detail-step-indicator",
                    step.status match
                      case OrderStatus.Completed  => "\u2713"
                      case OrderStatus.InProgress => "\u25B6"
                      case OrderStatus.OnHold     => "\u23F8"
                      case OrderStatus.Queued     => if isCurrent then "\u25CB" else "\u25CB"
                  ),
                  div(
                    cls := "mfg-detail-step-info",
                    span(cls := "mfg-detail-step-name", stationName),
                    span(
                      cls := s"mfg-status-badge mfg-status-${statusClass(step.status)} mfg-status-badge-sm",
                      statusLabel(step.status, lang),
                    ),
                  ),
                  div(
                    cls := "mfg-detail-step-times",
                    step.startedAt.map(t =>
                      span(cls := "mfg-detail-step-time",
                        s"${if lang == Language.En then "Started" else "Zahajeno"}: ${formatTimestamp(t)}")
                    ).getOrElse(emptyNode),
                    step.completedAt.map(t =>
                      span(cls := "mfg-detail-step-time",
                        s"${if lang == Language.En then "Done" else "Hotovo"}: ${formatTimestamp(t)}")
                    ).getOrElse(emptyNode),
                  ),
                )
              },
            ),
          ),

          // Attachments
          div(
            cls := "mfg-detail-section",
            h3(
              if lang == Language.En then s"Attachments (${order.attachments.size})"
              else s"Prilohy (${order.attachments.size})"
            ),
            if order.attachments.isEmpty then
              div(cls := "mfg-detail-empty",
                if lang == Language.En then "No attachments" else "Zadne prilohy",
              )
            else
              div(
                cls := "mfg-detail-attachments",
                order.attachments.map { att =>
                  div(
                    cls := "mfg-detail-attachment",
                    span(cls := "mfg-detail-att-icon", fileIcon(att.fileType)),
                    div(
                      cls := "mfg-detail-att-info",
                      span(cls := "mfg-detail-att-name", att.name),
                      span(cls := "mfg-detail-att-meta", s"${formatFileSize(att.sizeBytes)} · ${formatTimestamp(att.uploadedAt)}"),
                    ),
                    button(
                      cls := "mfg-btn mfg-btn-sm mfg-btn-outline",
                      if lang == Language.En then "View" else "Zobrazit",
                    ),
                  )
                },
              ),
          ),
        ),
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
          cls := "mfg-detail-action-btns",
          button(
            cls := "mfg-btn mfg-btn-success",
            if lang == Language.En then "Start Work" else "Zahajit praci",
            onClick --> { _ => ManufacturingViewModel.startOrder(order.id) },
          ),
          button(
            cls := "mfg-btn mfg-btn-warning",
            if lang == Language.En then "Put On Hold" else "Pozastavit",
            onClick --> { _ => ManufacturingViewModel.holdOrder(order.id) },
          ),
        )
      case OrderStatus.InProgress =>
        div(
          cls := "mfg-detail-action-btns",
          button(
            cls := "mfg-btn mfg-btn-success",
            if lang == Language.En then "Complete & Advance" else "Dokoncit a posunout",
            onClick --> { _ => ManufacturingViewModel.completeAndAdvance(order.id) },
          ),
          button(
            cls := "mfg-btn mfg-btn-warning",
            if lang == Language.En then "Put On Hold" else "Pozastavit",
            onClick --> { _ => ManufacturingViewModel.holdOrder(order.id) },
          ),
        )
      case OrderStatus.OnHold =>
        div(
          cls := "mfg-detail-action-btns",
          button(
            cls := "mfg-btn mfg-btn-primary",
            if lang == Language.En then "Resume" else "Obnovit",
            onClick --> { _ => ManufacturingViewModel.resumeOrder(order.id) },
          ),
        )
      case OrderStatus.Completed =>
        div(
          cls := "mfg-detail-action-btns",
          span(
            cls := "mfg-status-badge mfg-status-completed",
            if lang == Language.En then "Fully Completed" else "Plne dokonceno",
          ),
        )

  private def infoRow(label: String, value: String): Element =
    div(
      cls := "mfg-detail-info-row",
      span(cls := "mfg-detail-info-label", label),
      span(cls := "mfg-detail-info-value", value),
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

  private def priorityLabel(p: OrderPriority, lang: Language): String = p match
    case OrderPriority.Low    => if lang == Language.En then "Low" else "Nizka"
    case OrderPriority.Normal => if lang == Language.En then "Normal" else "Normalni"
    case OrderPriority.High   => if lang == Language.En then "High" else "Vysoka"
    case OrderPriority.Urgent => if lang == Language.En then "Urgent" else "Pilne"

  private def formatTimestamp(ts: Long): String =
    val d = new Date(ts.toDouble)
    val day = d.getDate().toInt
    val month = (d.getMonth() + 1).toInt
    val hour = d.getHours().toInt
    val min = d.getMinutes().toInt
    f"$day%d.$month%d. $hour%02d:$min%02d"

  private def formatFileSize(bytes: Long): String =
    if bytes < 1024 then s"${bytes}B"
    else if bytes < 1024 * 1024 then s"${bytes / 1024}KB"
    else s"${bytes / (1024 * 1024)}MB"

  private def fileIcon(fileType: String): String =
    if fileType.contains("pdf") then "PDF"
    else if fileType.contains("image") then "IMG"
    else "FILE"
