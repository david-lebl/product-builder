package mpbuilder.ui.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.{ProductBuilderViewModel, AppRouter, AppRoute, LoginState}
import mpbuilder.ui.manufacturing.ManufacturingViewModel
import mpbuilder.domain.model.*
import mpbuilder.domain.model.ManufacturingOrder.*
import mpbuilder.domain.pricing.{Money, Currency}

/** Customer-facing portal view with tile-based layout for order tracking and self-service actions. */
object CustomerPortalView:

  /** Which order detail is currently expanded (if any). */
  private val expandedOrderId: Var[Option[String]] = Var(None)

  /** Toast message shown after an action. */
  private val toastMessage: Var[Option[(String, Long)]] = Var(None)

  /** Time window for recent orders (30 days in milliseconds). */
  private val RecentOrderWindowMs = 30L * 24 * 60 * 60 * 1000

  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage

    // Derived signal: customer orders filtered by logged-in customer
    val customerOrdersSignal: Signal[List[ManufacturingOrder]] =
      ProductBuilderViewModel.currentCustomer.combineWith(ManufacturingViewModel.manufacturingOrders.signal).map {
        case (Some(customer), allOrders) =>
          allOrders.filter(_.order.customerId.contains(customer.id)).sortBy(-_.createdAt)
        case _ => List.empty
      }

    val now = System.currentTimeMillis()

    val activeOrdersSignal: Signal[List[ManufacturingOrder]] =
      customerOrdersSignal.map(_.filter(mo => !mo.isDispatched))

    val recentOrdersSignal: Signal[List[ManufacturingOrder]] =
      customerOrdersSignal.map(_.filter(mo =>
        mo.isDispatched && mo.createdAt > (now - RecentOrderWindowMs)
      ))

    div(
      cls := "portal-page",

      // Toast notification
      child <-- toastMessage.signal.map {
        case Some((msg, _)) =>
          div(cls := "portal-toast", msg)
        case None => emptyNode
      },

      child <-- ProductBuilderViewModel.loginState.combineWith(lang).map { case (loginState, l) =>
        loginState match
          case _: LoginState.LoggedIn =>
            div(
              cls := "portal-container",

              // Page header
              div(
                cls := "portal-header",
                h1(l match
                  case Language.En => "My Orders"
                  case Language.Cs => "Moje objednávky"
                ),
                p(cls := "portal-subtitle", l match
                  case Language.En => "Track your active orders, check payment and production status"
                  case Language.Cs => "Sledujte aktivní objednávky, platby a stav výroby"
                ),
              ),

              // Summary counters
              child <-- activeOrdersSignal.map { activeOrders =>
                summaryBar(activeOrders, l)
              },

              // Active Orders Section
              child <-- activeOrdersSignal.combineWith(expandedOrderId.signal).map { case (orders, expandedId) =>
                activeOrdersSection(orders, expandedId, l)
              },

              // Recent Orders Section
              child <-- recentOrdersSignal.combineWith(expandedOrderId.signal).map { case (orders, expandedId) =>
                recentOrdersSection(orders, expandedId, l)
              },
            )

          case _ =>
            div(
              cls := "portal-container",
              div(
                cls := "portal-login-prompt",
                div(cls := "portal-login-icon", "🔐"),
                h2(l match
                  case Language.En => "Sign in to view your orders"
                  case Language.Cs => "Přihlaste se pro zobrazení objednávek"
                ),
                p(l match
                  case Language.En => "Log in with your agency account to track orders, check payment status, and manage artwork."
                  case Language.Cs => "Přihlaste se firemním účtem pro sledování objednávek, plateb a správu podkladů."
                ),
                button(
                  cls := "btn btn-primary",
                  l match
                    case Language.En => "← Back to Shop"
                    case Language.Cs => "← Zpět do obchodu",
                  onClick --> { _ => AppRouter.navigateTo(AppRoute.ProductBuilder) },
                ),
              ),
            )
      },
    )

  // ── Summary Bar ──────────────────────────────────────────────────────

  private def summaryBar(activeOrders: List[ManufacturingOrder], l: Language): Element =
    val awaitingPayment = activeOrders.count(_.paymentStatus != PaymentStatus.Confirmed)
    val inApproval = activeOrders.count(mo =>
      mo.approvalStatus == ApprovalStatus.Placed || mo.approvalStatus == ApprovalStatus.PendingChanges || mo.approvalStatus == ApprovalStatus.OnHold
    )
    val inProduction = activeOrders.count(mo =>
      mo.approvalStatus == ApprovalStatus.Approved &&
        (mo.overallStatus == WorkflowStatus.InProgress || mo.overallStatus == WorkflowStatus.Pending)
    )
    val readyForDelivery = activeOrders.count(mo =>
      mo.workflows.nonEmpty && mo.overallStatus == WorkflowStatus.Completed && !mo.isDispatched
    )

    div(
      cls := "portal-summary",
      summaryTile("⏳", awaitingPayment.toString,
        if l == Language.En then "Awaiting Payment" else "Čeká na platbu",
        "portal-summary-tile--warning"),
      summaryTile("📋", inApproval.toString,
        if l == Language.En then "In Approval" else "Ve schvalování",
        "portal-summary-tile--info"),
      summaryTile("🏭", inProduction.toString,
        if l == Language.En then "In Production" else "Ve výrobě",
        "portal-summary-tile--primary"),
      summaryTile("📦", readyForDelivery.toString,
        if l == Language.En then "Ready for Delivery" else "Připraveno k odeslání",
        "portal-summary-tile--success"),
    )

  private def summaryTile(icon: String, count: String, label: String, variant: String): Element =
    div(
      cls := s"portal-summary-tile $variant",
      span(cls := "portal-summary-tile-icon", icon),
      div(
        cls := "portal-summary-tile-body",
        span(cls := "portal-summary-tile-count", count),
        span(cls := "portal-summary-tile-label", label),
      ),
    )

  // ── Active Orders Section ────────────────────────────────────────────

  private def activeOrdersSection(
      orders: List[ManufacturingOrder],
      expandedId: Option[String],
      l: Language,
  ): Element =
    div(
      cls := "portal-section",
      h2(cls := "portal-section-title",
        span(cls := "portal-section-icon", "📍"),
        span(l match
          case Language.En => "Active Orders"
          case Language.Cs => "Aktivní objednávky"
        ),
      ),
      if orders.isEmpty then
        div(
          cls := "portal-empty",
          div(cls := "portal-empty-icon", "🎉"),
          p(l match
            case Language.En => "No active orders — you're all caught up!"
            case Language.Cs => "Žádné aktivní objednávky — vše je vyřízeno!"
          ),
          button(
            cls := "btn btn-primary",
            l match
              case Language.En => "Place a New Order →"
              case Language.Cs => "Vytvořit novou objednávku →",
            onClick --> { _ => AppRouter.navigateTo(AppRoute.ProductBuilder) },
          ),
        )
      else
        div(
          cls := "portal-tiles",
          orders.map(mo => orderTile(mo, expandedId, isActive = true, l)),
        ),
    )

  // ── Recent Orders Section ────────────────────────────────────────────

  private def recentOrdersSection(
      orders: List[ManufacturingOrder],
      expandedId: Option[String],
      l: Language,
  ): Element =
    div(
      cls := "portal-section",
      h2(cls := "portal-section-title",
        span(cls := "portal-section-icon", "🕐"),
        span(l match
          case Language.En => "Recent Orders (Last 30 Days)"
          case Language.Cs => "Nedávné objednávky (posledních 30 dní)"
        ),
      ),
      if orders.isEmpty then
        div(
          cls := "portal-empty portal-empty--small",
          p(l match
            case Language.En => "No recent completed orders."
            case Language.Cs => "Žádné nedávné dokončené objednávky."
          ),
        )
      else
        div(
          cls := "portal-tiles",
          orders.map(mo => orderTile(mo, expandedId, isActive = false, l)),
        ),
    )

  // ── Order Tile ───────────────────────────────────────────────────────

  private def orderTile(
      mo: ManufacturingOrder,
      expandedId: Option[String],
      isActive: Boolean,
      l: Language,
  ): Element =
    val isExpanded = expandedId.contains(mo.order.id.value)
    val status = OrderHistoryStatus.fromManufacturingOrder(mo)

    div(
      cls := (if isExpanded then "portal-tile portal-tile--expanded" else "portal-tile"),

      // Tile header
      div(
        cls := "portal-tile-header",
        onClick --> { _ =>
          expandedOrderId.update {
            case Some(id) if id == mo.order.id.value => None
            case _ => Some(mo.order.id.value)
          }
        },
        div(
          cls := "portal-tile-header-left",
          span(cls := "portal-tile-order-id", s"#${mo.order.id.value}"),
          span(cls := "portal-tile-date", formatDate(mo.createdAt)),
        ),
        div(
          cls := "portal-tile-header-right",
          span(cls := status.badgeClass, status.label(l)),
          span(cls := "portal-tile-expand", if isExpanded then "▲" else "▼"),
        ),
      ),

      // Tile body — always visible summary
      div(
        cls := "portal-tile-body",

        // Items summary
        div(
          cls := "portal-tile-items",
          span(cls := "portal-tile-items-text", mo.itemSummary),
          span(cls := "portal-tile-total", formatMoney(mo.order.total, mo.order.currency)),
        ),

        // Status row
        div(
          cls := "portal-tile-statuses",
          paymentStatusBadge(mo.paymentStatus, l),
          approvalStatusBadge(mo.approvalStatus, l),
          artworkStatusBadge(mo.artworkCheck, l),
        ),

        // Production progress bar (for active orders)
        if isActive && mo.approvalStatus == ApprovalStatus.Approved then
          productionProgressBar(mo, l)
        else emptyNode,

        // Delivery progress (for orders in fulfilment)
        mo.fulfilment match
          case Some(fc) => deliveryProgress(fc, l)
          case None     => emptyNode,
      ),

      // Action buttons
      if isActive then
        div(
          cls := "portal-tile-actions",
          // Resend payment info — shown when payment is not confirmed
          if mo.paymentStatus != PaymentStatus.Confirmed then
            button(
              cls := "btn btn-sm btn-warning",
              l match
                case Language.En => "💳 Resend Payment Info"
                case Language.Cs => "💳 Znovu odeslat platební údaje",
              onClick --> { _ => showToast(
                if l == Language.En then s"Payment information resent for order #${mo.order.id.value}"
                else s"Platební údaje znovu odeslány pro objednávku #${mo.order.id.value}"
              )},
            )
          else emptyNode,

          // Re-upload artwork — shown when approval is PendingChanges
          if mo.approvalStatus == ApprovalStatus.PendingChanges then
            button(
              cls := "btn btn-sm btn-primary",
              l match
                case Language.En => "🎨 Re-upload Artwork"
                case Language.Cs => "🎨 Znovu nahrát podklady",
              onClick --> { _ => showToast(
                if l == Language.En then s"Artwork upload initiated for order #${mo.order.id.value}. Please check your email for instructions."
                else s"Nahrávání podkladů zahájeno pro objednávku #${mo.order.id.value}. Zkontrolujte prosím e-mail s instrukcemi."
              )},
            )
          else emptyNode,
        )
      else emptyNode,

      // Expanded detail
      if isExpanded then
        orderDetail(mo, status, l)
      else emptyNode,
    )

  // ── Status Badges ────────────────────────────────────────────────────

  private def paymentStatusBadge(ps: PaymentStatus, l: Language): Element =
    val (icon, label, cls_) = ps match
      case PaymentStatus.Pending   => ("⏳", if l == Language.En then "Payment Pending" else "Platba čeká", "portal-status portal-status--warning")
      case PaymentStatus.Confirmed => ("✅", if l == Language.En then "Payment Confirmed" else "Platba potvrzena", "portal-status portal-status--success")
      case PaymentStatus.Failed    => ("❌", if l == Language.En then "Payment Failed" else "Platba selhala", "portal-status portal-status--error")
    span(cls := cls_, s"$icon $label")

  private def approvalStatusBadge(as: ApprovalStatus, l: Language): Element =
    val (icon, label, cls_) = as match
      case ApprovalStatus.Placed         => ("📋", if l == Language.En then "Awaiting Approval" else "Čeká na schválení", "portal-status portal-status--muted")
      case ApprovalStatus.Approved       => ("✅", if l == Language.En then "Approved" else "Schváleno", "portal-status portal-status--success")
      case ApprovalStatus.Rejected       => ("🚫", if l == Language.En then "Rejected" else "Zamítnuto", "portal-status portal-status--error")
      case ApprovalStatus.PendingChanges => ("✏️", if l == Language.En then "Changes Requested" else "Požadovány změny", "portal-status portal-status--warning")
      case ApprovalStatus.OnHold         => ("⏸️", if l == Language.En then "On Hold" else "Pozastaveno", "portal-status portal-status--muted")
    span(cls := cls_, s"$icon $label")

  private def artworkStatusBadge(ac: ArtworkCheck, l: Language): Element =
    if ac.isFullyPassed then
      span(cls := "portal-status portal-status--success",
        s"🎨 ${if l == Language.En then "Artwork OK" else "Podklady OK"}")
    else if ac.hasIssues then
      span(cls := "portal-status portal-status--error",
        s"🎨 ${if l == Language.En then "Artwork Issues" else "Problémy s podklady"}")
    else if ac.hasWarnings then
      span(cls := "portal-status portal-status--warning",
        s"🎨 ${if l == Language.En then "Artwork Warnings" else "Varování podkladů"}")
    else
      span(cls := "portal-status portal-status--muted",
        s"🎨 ${if l == Language.En then "Artwork Pending" else "Podklady čekají"}")

  // ── Production Progress Bar ──────────────────────────────────────────

  private def productionProgressBar(mo: ManufacturingOrder, l: Language): Element =
    val ratio = mo.overallCompletionRatio
    val percent = (ratio * 100).toInt
    div(
      cls := "portal-progress",
      div(
        cls := "portal-progress-header",
        span(l match
          case Language.En => "Production Progress"
          case Language.Cs => "Průběh výroby"
        ),
        span(cls := "portal-progress-pct", s"$percent%"),
      ),
      div(
        cls := "portal-progress-bar",
        div(
          cls := "portal-progress-fill",
          width := s"$percent%",
        ),
      ),
      div(
        cls := "portal-progress-steps",
        span(l match
          case Language.En => s"${mo.completedStepCount} / ${mo.totalSteps} steps completed"
          case Language.Cs => s"${mo.completedStepCount} / ${mo.totalSteps} kroků dokončeno"
        ),
      ),
    )

  // ── Delivery Progress ────────────────────────────────────────────────

  private def deliveryProgress(fc: FulfilmentChecklist, l: Language): Element =
    div(
      cls := "portal-delivery",
      div(
        cls := "portal-delivery-header",
        span(l match
          case Language.En => "Delivery Progress"
          case Language.Cs => "Průběh doručení"
        ),
      ),
      div(
        cls := "portal-delivery-steps",
        deliveryStep("📋",
          if l == Language.En then "Items Collected" else "Položky sebrány",
          fc.allItemsCollected),
        deliveryStepConnector(fc.allItemsCollected),
        deliveryStep("✓",
          if l == Language.En then "Quality Check" else "Kontrola kvality",
          fc.isQualityPassed),
        deliveryStepConnector(fc.isQualityPassed),
        deliveryStep("📦",
          if l == Language.En then "Packaged" else "Zabaleno",
          fc.isPackaged),
        deliveryStepConnector(fc.isPackaged),
        deliveryStep("🚚",
          if l == Language.En then "Dispatched" else "Odesláno",
          fc.isDispatched),
      ),
    )

  private def deliveryStep(icon: String, label: String, done: Boolean): Element =
    div(
      cls := (if done then "portal-delivery-step portal-delivery-step--done" else "portal-delivery-step"),
      span(cls := "portal-delivery-step-icon", icon),
      span(cls := "portal-delivery-step-label", label),
    )

  private def deliveryStepConnector(isDone: Boolean): Element =
    div(cls := (if isDone then "portal-delivery-connector portal-delivery-connector--done" else "portal-delivery-connector"))

  // ── Order Detail (Expanded) ──────────────────────────────────────────

  private def orderDetail(
      mo: ManufacturingOrder,
      status: OrderHistoryStatus,
      l: Language,
  ): Element =
    div(
      cls := "portal-detail",

      // Order progress steps
      div(
        cls := "portal-detail-progress",
        h4(l match
          case Language.En => "Order Progress"
          case Language.Cs => "Průběh objednávky"
        ),
        div(
          cls := "portal-detail-steps",
          OrderHistoryStatus.values.map { s =>
            val isCurrent = s == status
            val isDone = s.ordinal < status.ordinal
            val stepCls =
              if isDone then "portal-step portal-step--done"
              else if isCurrent then "portal-step portal-step--current"
              else "portal-step"
            div(
              cls := stepCls,
              span(cls := "portal-step-dot", if isDone then "✓" else (s.ordinal + 1).toString),
              span(cls := "portal-step-label", s.label(l)),
            )
          }.toSeq,
        ),
      ),

      // Artwork check details (if there are issues)
      if mo.artworkCheck.hasIssues || mo.artworkCheck.hasWarnings then
        div(
          cls := "portal-detail-artwork",
          h4(l match
            case Language.En => "Artwork Check Details"
            case Language.Cs => "Detail kontroly podkladů"
          ),
          div(
            cls := "portal-artwork-checks",
            artworkCheckRow("Resolution", "Rozlišení", mo.artworkCheck.resolution, l),
            artworkCheckRow("Bleed", "Spadávka", mo.artworkCheck.bleed, l),
            artworkCheckRow("Color Profile", "Barevný profil", mo.artworkCheck.colorProfile, l),
          ),
          if mo.artworkCheck.notes.nonEmpty then
            div(cls := "portal-artwork-notes",
              strong(if l == Language.En then "Notes: " else "Poznámky: "),
              mo.artworkCheck.notes,
            )
          else emptyNode,
        )
      else emptyNode,

      // Items detail
      div(
        cls := "portal-detail-items",
        h4(l match
          case Language.En => "Order Items"
          case Language.Cs => "Položky objednávky"
        ),
        mo.order.basket.items.map { item =>
          div(
            cls := "portal-item",
            div(
              cls := "portal-item-info",
              strong(s"${item.quantity}× ${item.configuration.category.name(l)}"),
              span(cls := "portal-item-material",
                s" • ${item.configuration.components.map(_.material.name(l)).mkString(", ")}"
              ),
            ),
            div(cls := "portal-item-price",
              formatMoney(item.priceBreakdown.total * item.quantity, item.priceBreakdown.currency)
            ),
          )
        },
      ),

      // Total price
      div(
        cls := "portal-detail-total",
        span(l match
          case Language.En => "Total"
          case Language.Cs => "Celkem"
        ),
        span(cls := "portal-detail-total-value",
          formatMoney(mo.order.total, mo.order.currency)
        ),
      ),

      // Tracking number
      mo.fulfilment.flatMap(f =>
        if f.dispatchInfo.trackingNumber.nonEmpty then
          Some(div(
            cls := "portal-detail-tracking",
            h4(l match
              case Language.En => "Tracking"
              case Language.Cs => "Sledování zásilky"
            ),
            span(cls := "portal-tracking-number",
              s"📦 ${f.dispatchInfo.trackingNumber}"
            ),
          ))
        else None
      ).getOrElse(emptyNode),
    )

  private def artworkCheckRow(labelEn: String, labelCs: String, cs: CheckStatus, l: Language): Element =
    val statusCls = cs match
      case CheckStatus.Passed     => "portal-check--passed"
      case CheckStatus.Warning    => "portal-check--warning"
      case CheckStatus.Failed     => "portal-check--failed"
      case CheckStatus.NotChecked => "portal-check--pending"
    div(
      cls := s"portal-artwork-check-row $statusCls",
      span(if l == Language.En then labelEn else labelCs),
      span(cs.icon + " " + cs.displayName),
    )

  // ── Helpers ──────────────────────────────────────────────────────────

  private def showToast(message: String): Unit =
    val ts = System.currentTimeMillis()
    toastMessage.set(Some((message, ts)))
    // Auto-dismiss after 4 seconds via JS timeout
    scala.scalajs.js.timers.setTimeout(4000) {
      toastMessage.update {
        case Some((_, t)) if t == ts => None
        case other                   => other
      }
    }

  private def formatDate(timestamp: Long): String =
    val date = new scala.scalajs.js.Date(timestamp.toDouble)
    val day = f"${date.getDate().toInt}%02d"
    val month = f"${(date.getMonth() + 1).toInt}%02d"
    val year = date.getFullYear().toInt
    s"$day.$month.$year"

  private def formatMoney(money: Money, currency: Currency): String =
    currency match
      case Currency.CZK => f"${money.value}%.2f Kč"
      case Currency.USD => f"$$${money.value}%.2f"
      case Currency.EUR => f"€${money.value}%.2f"
      case Currency.GBP => f"£${money.value}%.2f"
