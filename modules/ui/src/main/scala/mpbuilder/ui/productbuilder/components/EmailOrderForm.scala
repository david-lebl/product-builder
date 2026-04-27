package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.{Language, SpecValue, ManufacturingSpeed}
import mpbuilder.domain.pricing.{Money, Currency}
import org.scalajs.dom

object EmailOrderForm:

  /** Contact email address that order requests are sent to. */
  private val ContactEmail = "orders@example.com"

  def apply(): Element =
    val lang  = ProductBuilderViewModel.currentLanguage
    val open  = Var(false)
    val name  = Var("")
    val email = Var("")
    val notes = Var("")

    div(
      cls := "email-order-section",

      // Toggle button
      button(
        cls := "email-order-toggle-btn",
        child.text <-- lang.map {
          case Language.En => "📧 Order via Email"
          case Language.Cs => "📧 Objednat e-mailem"
        },
        onClick --> { _ => open.update(!_) },
      ),

      // Collapsible form body
      div(
        cls <-- open.signal.map(o => if o then "email-order-form" else "email-order-form email-order-form-collapsed"),

        p(
          cls := "email-order-hint",
          child.text <-- lang.map {
            case Language.En => "Couldn't find everything you need? Fill in the form below and we'll get back to you."
            case Language.Cs => "Nenašli jste vše, co potřebujete? Vyplňte formulář níže a my se vám ozveme."
          },
        ),

        // Name field
        div(
          cls := "form-group",
          label(child.text <-- lang.map {
            case Language.En => "Your name"
            case Language.Cs => "Vaše jméno"
          }),
          input(
            typ := "text",
            controlled(
              value <-- name.signal,
              onInput.mapToValue --> name.writer,
            ),
            placeholder <-- lang.map {
              case Language.En => "e.g. Jan Novák"
              case Language.Cs => "např. Jan Novák"
            },
          ),
        ),

        // Email field
        div(
          cls := "form-group",
          label(child.text <-- lang.map {
            case Language.En => "Your email"
            case Language.Cs => "Váš e-mail"
          }),
          input(
            typ := "email",
            controlled(
              value <-- email.signal,
              onInput.mapToValue --> email.writer,
            ),
            placeholder := "you@example.com",
          ),
        ),

        // Notes / description field — pre-filled with configuration summary on open
        div(
          cls := "form-group",
          label(child.text <-- lang.map {
            case Language.En => "Notes / what you need"
            case Language.Cs => "Poznámky / co potřebujete"
          }),
          // Regenerate notes summary whenever the form is opened
          child <-- open.signal.combineWith(lang).map { case (isOpen, l) =>
            if isOpen then
              textArea(
                cls := "email-order-notes",
                rows := 5,
                controlled(
                  value <-- notes.signal,
                  onInput.mapToValue --> notes.writer,
                ),
                // Pre-fill with current configuration summary the first time it is opened
                onMountCallback { _ =>
                  if notes.now().isEmpty then
                    notes.set(buildConfigSummary(l))
                },
              )
            else
              // Placeholder element while collapsed
              textArea(
                cls := "email-order-notes",
                rows := 5,
                controlled(
                  value <-- notes.signal,
                  onInput.mapToValue --> notes.writer,
                ),
              )
          },
        ),

        // Send button
        button(
          cls := "email-order-send-btn",
          child.text <-- lang.map {
            case Language.En => "Open Email Client →"
            case Language.Cs => "Otevřít e-mailového klienta →"
          },
          disabled <-- email.signal.map(_.trim.isEmpty),
          onClick --> { _ =>
            val l           = ProductBuilderViewModel.stateVar.now().language
            val senderName  = name.now().trim
            val senderEmail = email.now().trim
            val noteText    = notes.now().trim
            val subject     = buildSubject(l)
            val body        = buildBody(l, senderName, senderEmail, noteText)
            val mailto      = s"mailto:$ContactEmail?subject=${encodeURI(subject)}&body=${encodeURI(body)}"
            dom.window.location.href = mailto
          },
        ),
      ),
    )

  // ── Helpers ───────────────────────────────────────────────────────────

  private def buildSubject(lang: Language): String =
    val state      = ProductBuilderViewModel.stateVar.now()
    val catalog    = ProductBuilderViewModel.catalog
    val categoryName = state.selectedCategoryId
      .flatMap(catalog.categories.get)
      .map(_.name(lang))
      .getOrElse(lang match { case Language.En => "Unspecified product"; case Language.Cs => "Nespecifikovaný produkt" })
    lang match
      case Language.En => s"Product Order Request — $categoryName"
      case Language.Cs => s"Požadavek na objednávku — $categoryName"

  private def buildBody(lang: Language, senderName: String, senderEmail: String, noteText: String): String =
    val summary = buildConfigSummary(lang)
    val nameLabel  = lang match { case Language.En => "Name";  case Language.Cs => "Jméno" }
    val emailLabel = lang match { case Language.En => "Email"; case Language.Cs => "E-mail" }
    val configLabel = lang match { case Language.En => "Configuration summary"; case Language.Cs => "Přehled konfigurace" }
    val notesLabel  = lang match { case Language.En => "Additional notes"; case Language.Cs => "Další poznámky" }
    val parts = List.newBuilder[String]
    if senderName.nonEmpty  then parts += s"$nameLabel: $senderName"
    if senderEmail.nonEmpty then parts += s"$emailLabel: $senderEmail"
    parts += ""
    parts += s"--- $configLabel ---"
    parts += summary
    if noteText.nonEmpty then
      parts += ""
      parts += s"--- $notesLabel ---"
      parts += noteText
    parts.result().mkString("\n")

  private def buildConfigSummary(lang: Language): String =
    val state   = ProductBuilderViewModel.stateVar.now()
    val catalog = ProductBuilderViewModel.catalog
    val lines   = List.newBuilder[String]

    // Category
    state.selectedCategoryId.flatMap(catalog.categories.get).foreach { cat =>
      val label = lang match { case Language.En => "Category"; case Language.Cs => "Kategorie" }
      lines += s"$label: ${cat.name(lang)}"
    }

    // Printing method
    state.selectedPrintingMethodId.flatMap(catalog.printingMethods.get).foreach { pm =>
      val label = lang match { case Language.En => "Printing method"; case Language.Cs => "Tisková metoda" }
      lines += s"$label: ${pm.name(lang)}"
    }

    // Specifications
    state.specifications.foreach {
      case SpecValue.QuantitySpec(q) =>
        val label = lang match { case Language.En => "Quantity"; case Language.Cs => "Množství" }
        lines += s"$label: ${q.value}"
      case SpecValue.SizeSpec(d) =>
        val label = lang match { case Language.En => "Size"; case Language.Cs => "Rozměr" }
        lines += s"$label: ${d.widthMm.toInt} × ${d.heightMm.toInt} mm"
      case SpecValue.PagesSpec(n) =>
        val label = lang match { case Language.En => "Pages"; case Language.Cs => "Počet stran" }
        lines += s"$label: $n"
      case SpecValue.OrientationSpec(o) =>
        val label = lang match { case Language.En => "Orientation"; case Language.Cs => "Orientace" }
        val value = lang match
          case Language.En => o.toString
          case Language.Cs => o match
            case mpbuilder.domain.model.Orientation.Portrait  => "Na výšku"
            case mpbuilder.domain.model.Orientation.Landscape => "Na šířku"
        lines += s"$label: $value"
      case SpecValue.FoldTypeSpec(f) =>
        val label = lang match { case Language.En => "Fold type"; case Language.Cs => "Typ přeložení" }
        lines += s"$label: ${f.toString}"
      case SpecValue.BindingMethodSpec(b) =>
        val label = lang match { case Language.En => "Binding method"; case Language.Cs => "Způsob vazby" }
        lines += s"$label: ${b.toString}"
      case SpecValue.ManufacturingSpeedSpec(s) =>
        val label = lang match { case Language.En => "Manufacturing speed"; case Language.Cs => "Rychlost výroby" }
        val value = s match
          case ManufacturingSpeed.Express  => lang match { case Language.En => "Express"; case Language.Cs => "Expresní" }
          case ManufacturingSpeed.Standard => lang match { case Language.En => "Standard"; case Language.Cs => "Standardní" }
          case ManufacturingSpeed.Economy  => lang match { case Language.En => "Economy";  case Language.Cs => "Ekonomická" }
        lines += s"$label: $value"
      case _ => ()
    }

    // Materials (first component)
    state.componentStates.values.headOption.flatMap(_.selectedMaterialId).flatMap(catalog.materials.get).foreach { mat =>
      val label = lang match { case Language.En => "Material"; case Language.Cs => "Materiál" }
      lines += s"$label: ${mat.name(lang)}"
    }

    // Price (if calculated)
    state.priceBreakdown.foreach { bd =>
      val label = lang match { case Language.En => "Estimated price"; case Language.Cs => "Orientační cena" }
      lines += s"$label: ${formatMoney(bd.total, bd.currency)}"
    }

    if lines.result().isEmpty then
      lang match
        case Language.En => "(no configuration selected)"
        case Language.Cs => "(žádná konfigurace nevybrána)"
    else
      lines.result().mkString("\n")

  private def formatMoney(money: Money, currency: Currency): String =
    currency match
      case Currency.CZK => f"${money.value}%.2f Kč"
      case Currency.USD => f"$$${money.value}%.2f"
      case Currency.EUR => f"€${money.value}%.2f"
      case Currency.GBP => f"£${money.value}%.2f"

  /** JS-compatible percent-encoding for mailto body/subject components. */
  private def encodeURI(s: String): String =
    scala.scalajs.js.URIUtils.encodeURIComponent(s)
