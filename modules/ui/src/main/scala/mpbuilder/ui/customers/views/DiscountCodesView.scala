package mpbuilder.ui.customers.views

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.Money
import mpbuilder.ui.customers.*
import mpbuilder.uikit.containers.*
import mpbuilder.uikit.form.FormComponents

/** Discount code management view using SplitTableView.
  *
  * Table columns: Code, Type, Value, Uses (current/max), Status
  * Filter chips: by type, by status (active/inactive)
  * Side panel: Code editing, constraint configuration, usage statistics
  */
object DiscountCodesView:

  def apply(): HtmlElement =
    val searchVar = Var("")
    val selectedId: Var[Option[String]] = Var(None)
    val statusFilterVar: Var[Set[String]] = Var(Set.empty)

    val filteredCodes: Signal[List[DiscountCode]] =
      CustomerManagementViewModel.discountCodes
        .combineWith(searchVar.signal, statusFilterVar.signal)
        .map { case (codes, query, statusFilter) =>
          val q = query.trim.toLowerCase
          codes
            .filter { dc =>
              if statusFilter.isEmpty then true
              else statusFilter.contains(codeStatus(dc))
            }
            .filter { dc =>
              if q.isEmpty then true
              else
                dc.code.toLowerCase.contains(q) ||
                discountTypeLabel(dc.discountType).toLowerCase.contains(q)
            }
        }

    val tableConfig = SplitTableConfig[DiscountCode](
      columns = List(
        ColumnDef("Code", dc => span(cls := "entity-id", dc.code), Some(_.code), Some("120px")),
        ColumnDef("Type", dc => span(discountTypeLabel(dc.discountType)),
          Some(dc => discountTypeLabel(dc.discountType)), Some("120px")),
        ColumnDef("Value", dc => span(discountValueLabel(dc.discountType)), width = Some("100px")),
        ColumnDef("Uses", dc => span(usesLabel(dc.constraints)), width = Some("100px")),
        ColumnDef("Status", dc => span(
          cls := s"entity-tag entity-tag--${codeStatus(dc).toLowerCase}",
          codeStatus(dc),
        ), Some(dc => codeStatus(dc)), Some("100px")),
        ColumnDef("", dc => div(
          cls := "entity-actions",
          button(
            cls := "btn btn-sm",
            if dc.isActive then "⏸" else "▶",
            onClick.stopPropagation --> { _ =>
              CustomerManagementViewModel.toggleDiscountCodeActive(dc.id)
            },
          ),
          button(cls := "btn btn-sm btn-danger", "✕", onClick.stopPropagation --> { _ =>
            CustomerManagementViewModel.removeDiscountCode(dc.id)
          }),
        ), width = Some("80px")),
      ),
      rowKey = _.id.value,
      filters = List(
        FilterDef(
          id = "status",
          label = "Status",
          options = Val(List("Active" -> "Active", "Inactive" -> "Inactive", "Expired" -> "Expired", "Exhausted" -> "Exhausted")),
          selectedValues = statusFilterVar,
        ),
      ),
      searchPlaceholder = "Search discount codes…",
      onRowSelect = Some(dc => {
        selectedId.set(Some(dc.id.value))
        CustomerManagementViewModel.setEditState(CustomerEditState.EditingDiscountCode(dc.id))
      }),
      emptyMessage = "No discount codes found.",
    )

    val sidePanel: Signal[Option[HtmlElement]] =
      CustomerManagementViewModel.editState.combineWith(CustomerManagementViewModel.discountCodes).map {
        case (CustomerEditState.CreatingDiscountCode, _) =>
          Some(discountCodeForm(None))
        case (CustomerEditState.EditingDiscountCode(id), codes) =>
          codes.find(_.id == id).map(dc => discountCodeForm(Some(dc)))
        case _ => None
      }

    div(
      cls := "catalog-section",
      h2(cls := "manufacturing-view-title", "Discount Codes"),
      SplitTableView(
        config = tableConfig,
        items = filteredCodes,
        selectedKey = selectedId.signal,
        searchQuery = searchVar,
        sidePanel = sidePanel,
        headerActions = Some(
          FormComponents.actionButton("+ Add Code", () => {
            selectedId.set(None)
            CustomerManagementViewModel.setEditState(CustomerEditState.CreatingDiscountCode)
          })
        ),
      ),
    )

  // ── Side panel form ────────────────────────────────────────────────────

  private def discountCodeForm(existing: Option[DiscountCode]): HtmlElement =
    val codeVar = Var(existing.map(_.code).getOrElse(""))
    val discountKindVar = Var(existing.map(discountKind).getOrElse("Percentage"))
    val percentageVar = Var(existing.flatMap {
      case dc => dc.discountType match
        case DiscountType.Percentage(v) => Some(v.toString)
        case _ => None
    }.getOrElse("10"))
    val fixedAmountVar = Var(existing.flatMap {
      case dc => dc.discountType match
        case DiscountType.FixedAmount(v) => Some(v.value.toString)
        case _ => None
    }.getOrElse("100"))
    val maxUsesVar = Var(existing.flatMap(_.constraints.maxUses))
    val minOrderVar = Var(existing.flatMap(_.constraints.minimumOrderValue).map(_.value.toString).getOrElse(""))
    val isActiveVar = Var(existing.map(_.isActive).getOrElse(true))

    div(
      cls := "catalog-detail-panel",

      button(cls := "detail-panel-close", "×", onClick --> { _ =>
        CustomerManagementViewModel.setEditState(CustomerEditState.None)
      }),

      div(cls := "detail-panel-header",
        h3(if existing.isDefined then s"Edit: ${existing.get.code}" else "New Discount Code"),
      ),

      div(
        cls := "detail-panel-section",

        FormComponents.textField("Code", codeVar.signal, codeVar.writer, "e.g. SAVE10"),

        FormComponents.enumSelectRequired[String](
          "Discount Type",
          Array("Percentage", "FixedAmount", "FreeDelivery"),
          discountKindVar.signal,
          discountKindVar.writer,
        ),

        child <-- discountKindVar.signal.map {
          case "Percentage" =>
            FormComponents.textField("Percentage (%)", percentageVar.signal, percentageVar.writer)
          case "FixedAmount" =>
            FormComponents.textField("Amount", fixedAmountVar.signal, fixedAmountVar.writer)
          case _ => emptyNode
        },

        FormComponents.sectionHeader("Constraints"),
        FormComponents.optionalNumberField("Max Uses", maxUsesVar.signal, maxUsesVar.writer),
        FormComponents.textField("Min. Order Value", minOrderVar.signal, minOrderVar.writer, "(optional)"),

        // Usage statistics for existing codes
        existing.map { dc =>
          div(
            cls := "discount-usage-stats",
            FormComponents.sectionHeader("Usage Statistics"),
            p(s"Current uses: ${dc.constraints.currentUses}"),
            dc.constraints.maxUses.map(max => p(s"Maximum uses: $max")).getOrElse(p("No usage limit")),
            p(s"Status: ${codeStatus(dc)}"),
            p(s"Created: ${formatTimestamp(dc.createdAt)}"),
          )
        }.getOrElse(emptyNode),

        div(
          cls := "form-group",
          com.raquo.laminar.api.L.label(
            cls := "checkbox-label",
            input(
              typ := "checkbox",
              checked <-- isActiveVar.signal,
              onChange.mapToChecked --> isActiveVar.writer,
            ),
            span("Active"),
          ),
        ),
      ),

      div(
        cls := "detail-panel-actions",
        FormComponents.actionButton(if existing.isDefined then "Save" else "Create", () => {
          val discountType = discountKindVar.now() match
            case "Percentage" =>
              scala.util.Try(BigDecimal(percentageVar.now())).toOption
                .map(DiscountType.Percentage(_))
                .getOrElse(DiscountType.Percentage(BigDecimal(10)))
            case "FixedAmount" =>
              scala.util.Try(BigDecimal(fixedAmountVar.now())).toOption
                .map(v => DiscountType.FixedAmount(Money(v)))
                .getOrElse(DiscountType.FixedAmount(Money(BigDecimal(100))))
            case _ => DiscountType.FreeDelivery

          val constraints = existing.map(_.constraints).getOrElse(DiscountConstraints()).copy(
            maxUses = maxUsesVar.now(),
            minimumOrderValue =
              if minOrderVar.now().trim.nonEmpty then
                scala.util.Try(BigDecimal(minOrderVar.now())).toOption.map(Money(_))
              else None,
          )

          val dc = DiscountCode(
            id = existing.map(_.id).getOrElse(DiscountCodeId.unsafe(s"dc-${System.currentTimeMillis()}")),
            code = codeVar.now().trim.toUpperCase,
            discountType = discountType,
            constraints = constraints,
            isActive = isActiveVar.now(),
            createdBy = existing.flatMap(_.createdBy),
            createdAt = existing.map(_.createdAt).getOrElse(System.currentTimeMillis()),
          )

          if existing.isDefined then CustomerManagementViewModel.updateDiscountCode(dc)
          else CustomerManagementViewModel.createDiscountCode(dc)
        }),
        FormComponents.dangerButton("Cancel", () =>
          CustomerManagementViewModel.setEditState(CustomerEditState.None)
        ),
      ),
    )

  // ── Helpers ─────────────────────────────────────────────────────────────

  private def discountTypeLabel(dt: DiscountType): String = dt match
    case DiscountType.Percentage(_)  => "Percentage"
    case DiscountType.FixedAmount(_) => "Fixed Amount"
    case DiscountType.FreeDelivery   => "Free Delivery"

  private def discountValueLabel(dt: DiscountType): String = dt match
    case DiscountType.Percentage(v)  => s"${v}%"
    case DiscountType.FixedAmount(v) => v.value.toString
    case DiscountType.FreeDelivery   => "—"

  private def discountKind(dc: DiscountCode): String = dc.discountType match
    case DiscountType.Percentage(_)  => "Percentage"
    case DiscountType.FixedAmount(_) => "FixedAmount"
    case DiscountType.FreeDelivery   => "FreeDelivery"

  private def usesLabel(c: DiscountConstraints): String =
    c.maxUses match
      case Some(max) => s"${c.currentUses}/$max"
      case None      => s"${c.currentUses}/∞"

  private def codeStatus(dc: DiscountCode): String =
    if !dc.isActive then "Inactive"
    else if dc.constraints.maxUses.exists(_ <= dc.constraints.currentUses) then "Exhausted"
    else if dc.constraints.validUntil.exists(_ < System.currentTimeMillis()) then "Expired"
    else "Active"

  private def formatTimestamp(ts: Long): String =
    val date = new scalajs.js.Date(ts.toDouble)
    s"${date.getFullYear()}-${padZero(date.getMonth().toInt + 1)}-${padZero(date.getDate().toInt)}"

  private def padZero(n: Int): String = if n < 10 then s"0$n" else n.toString
