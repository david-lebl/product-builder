package mpbuilder.ui.manufacturing.views

import com.raquo.laminar.api.L.*
import mpbuilder.ui.manufacturing.*
import mpbuilder.domain.model.Language

/** Side panel showing full order details, workflow diagram, and attached files.
  * Displayed when the user clicks on an order row in the Work Queue table.
  */
object OrderDetailPanel:

  def apply(order: ManufacturingOrder, state: ManufacturingState, l: Language): Element =
    div(
      cls := "mfg-wq-detail-pane",
      div(
        cls := "mfg-detail-panel",

        // Header with close button
        div(
          cls := "mfg-detail-header",
          div(
            span(cls := "mfg-detail-order-id", order.id),
            span(cls := s"mfg-status-badge ${order.status.cssClass}", order.status.label(l)),
            span(cls := s"mfg-priority-badge ${order.priority.cssClass}", order.priority.label(l)),
          ),
          button(
            cls := "mfg-detail-close",
            "×",
            onClick --> { _ => ManufacturingViewModel.selectOrder(None) },
          ),
        ),

        // Order info section
        div(
          cls := "mfg-detail-section",
          h4(if l == Language.Cs then "Informace o objednávce" else "Order Information"),
          div(
            cls := "mfg-detail-grid",
            detailRow(if l == Language.Cs then "Zákazník" else "Customer", order.customerName),
            detailRow(if l == Language.Cs then "Produkt" else "Product", order.productDescription),
            detailRow(if l == Language.Cs then "Množství" else "Quantity", order.quantity.toString),
            detailRow(if l == Language.Cs then "Priorita" else "Priority", order.priority.label(l)),
            detailRow(if l == Language.Cs then "Vytvořeno" else "Created", formatTime(order.createdAt)),
            detailRow(if l == Language.Cs then "Aktualizováno" else "Updated", formatTime(order.updatedAt)),
          ),
          if order.notes.nonEmpty then
            div(
              cls := "mfg-detail-notes",
              span(cls := "mfg-detail-notes-label", if l == Language.Cs then "Poznámky:" else "Notes:"),
              span(order.notes),
            )
          else emptyNode,
        ),

        // Workflow diagram section
        div(
          cls := "mfg-detail-section",
          h4(if l == Language.Cs then "Průběh výroby" else "Manufacturing Workflow"),
          workflowDiagram(order, state, l),
        ),

        // Action buttons
        div(
          cls := "mfg-detail-section mfg-detail-actions-section",
          orderActionButtons(order, state, l),
        ),

        // Attached files section
        div(
          cls := "mfg-detail-section",
          h4(if l == Language.Cs then "Přiložené soubory" else "Attached Files"),
          if order.attachedFiles.isEmpty then
            p(
              cls := "mfg-detail-no-files",
              l match
                case Language.En => "No files attached"
                case Language.Cs => "Žádné přiložené soubory"
            )
          else
            div(
              cls := "mfg-file-list",
              order.attachedFiles.map { file =>
                div(
                  cls := "mfg-file-item",
                  span(cls := "mfg-file-icon", fileIcon(file.fileType)),
                  div(
                    cls := "mfg-file-info",
                    span(cls := "mfg-file-name", file.name),
                    span(cls := "mfg-file-meta", s"${file.fileType.toUpperCase} · ${formatFileSize(file.sizeKb)}"),
                  ),
                  button(
                    cls := "mfg-btn mfg-btn-sm mfg-btn-secondary",
                    if l == Language.Cs then "Otevřít" else "Open",
                  ),
                )
              }
            ),
        ),
      ),
    )

  /** Visual workflow diagram showing completed / current / remaining steps */
  private def workflowDiagram(order: ManufacturingOrder, state: ManufacturingState, l: Language): Element =
    val steps = order.requiredStationTypes
    val completed = order.completedStationTypes.toSet
    val currentStationType = order.currentStationId
      .flatMap(sid => state.stations.find(_.id == sid))
      .map(_.stationType)
    val currentStationName = order.currentStationId
      .flatMap(sid => state.stations.find(_.id == sid))
      .map(_.name)

    if steps.isEmpty then
      p(cls := "mfg-detail-no-workflow", l match
        case Language.En => "No workflow steps defined"
        case Language.Cs => "Žádné kroky workflow"
      )
    else
      div(
        cls := "mfg-workflow",
        steps.zipWithIndex.map { case (stType, idx) =>
          val isCompleted = completed.contains(stType)
          val isCurrent   = currentStationType.contains(stType) && !isCompleted
          val isPending   = !isCompleted && !isCurrent

          val stepClass = if isCompleted then "mfg-wf-step-completed"
                          else if isCurrent then "mfg-wf-step-current"
                          else "mfg-wf-step-pending"

          div(
            cls := "mfg-wf-step-container",

            // Connector line before (except for first step)
            if idx > 0 then
              div(cls := s"mfg-wf-connector ${if isCompleted then "completed" else if isCurrent then "current" else ""}")
            else emptyNode,

            // Step node
            div(
              cls := s"mfg-wf-step $stepClass",
              div(
                cls := "mfg-wf-step-icon",
                if isCompleted then "✓"
                else if isCurrent then "●"
                else s"${idx + 1}",
              ),
              div(
                cls := "mfg-wf-step-label",
                span(cls := "mfg-wf-step-type", s"${stType.icon} ${stType.label(l)}"),
                if isCurrent then
                  span(cls := "mfg-wf-step-station", s"@ ${currentStationName.getOrElse("?")}")
                else if isCompleted then
                  span(cls := "mfg-wf-step-done", if l == Language.Cs then "Hotovo" else "Done")
                else
                  span(cls := "mfg-wf-step-waiting", if l == Language.Cs then "Čeká" else "Pending"),
              ),
            ),
          )
        }
      )

  /** Action buttons in the detail panel */
  private def orderActionButtons(order: ManufacturingOrder, s: ManufacturingState, l: Language): Element =
    div(
      cls := "mfg-detail-action-bar",
      order.status match
        case OrderStatus.Pending | OrderStatus.InProgress =>
          val nextRequired = order.requiredStationTypes.filterNot(order.completedStationTypes.contains).headOption
          val hasAvailableStation = nextRequired.exists { stType =>
            s.stations.exists(st => st.stationType == stType && st.currentOrderId.isEmpty && st.isActive)
          }
          div(
            cls := "mfg-detail-action-buttons",
            if hasAvailableStation then
              button(
                cls := "mfg-btn mfg-btn-primary",
                l match
                  case Language.En => s"▶ Pick up — ${nextRequired.map(_.label(l)).getOrElse("")}"
                  case Language.Cs => s"▶ Převzít — ${nextRequired.map(_.label(l)).getOrElse("")}",
                onClick --> { _ => ManufacturingViewModel.pickupAndStart(order.id) },
              )
            else
              span(
                cls := "mfg-waiting-label",
                l match
                  case Language.En => s"Waiting for ${nextRequired.map(_.label(l)).getOrElse("next station")}"
                  case Language.Cs => s"Čeká na ${nextRequired.map(_.label(l)).getOrElse("další stanici")}"
              ),
            button(
              cls := "mfg-btn mfg-btn-danger",
              if l == Language.Cs then "Zrušit objednávku" else "Cancel Order",
              onClick --> { _ => ManufacturingViewModel.cancelOrder(order.id) },
            ),
          )
        case OrderStatus.AtStation =>
          val stationName = order.currentStationId.flatMap(sid => s.stations.find(_.id == sid)).map(_.name).getOrElse("?")
          div(
            cls := "mfg-detail-action-buttons",
            button(
              cls := "mfg-btn mfg-btn-success",
              l match
                case Language.En => s"✓ Complete — $stationName"
                case Language.Cs => s"✓ Dokončit — $stationName",
              onClick --> { _ => ManufacturingViewModel.completeAndAdvance(order.id) },
            ),
            button(
              cls := "mfg-btn mfg-btn-danger",
              if l == Language.Cs then "Zrušit objednávku" else "Cancel Order",
              onClick --> { _ => ManufacturingViewModel.cancelOrder(order.id) },
            ),
          )
        case OrderStatus.Completed =>
          div(
            cls := "mfg-detail-completed-msg",
            span("✅ "),
            span(if l == Language.Cs then "Objednávka dokončena" else "Order completed"),
          )
        case OrderStatus.Cancelled =>
          div(
            cls := "mfg-detail-cancelled-msg",
            span(if l == Language.Cs then "Objednávka zrušena" else "Order cancelled"),
          )
    )

  private def detailRow(label: String, value: String): Element =
    div(
      cls := "mfg-detail-row",
      span(cls := "mfg-detail-label", label),
      span(cls := "mfg-detail-value", value),
    )

  private def formatTime(epochMillis: Long): String =
    val d = new scalajs.js.Date(epochMillis.toDouble)
    val pad = (n: Double) => if n < 10 then s"0${n.toInt}" else n.toInt.toString
    s"${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}"

  private def fileIcon(fileType: String): String = fileType.toLowerCase match
    case "pdf"             => "📄"
    case "jpg" | "jpeg" | "png" | "gif" => "🖼️"
    case "ai" | "eps" | "svg"           => "🎨"
    case _                 => "📎"

  private def formatFileSize(sizeKb: Int): String =
    if sizeKb >= 1024 then s"${(sizeKb / 1024.0 * 10).round / 10.0} MB"
    else s"$sizeKb KB"
