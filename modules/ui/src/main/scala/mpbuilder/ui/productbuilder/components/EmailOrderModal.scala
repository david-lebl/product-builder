package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.{ProductBuilderViewModel, BuilderState, LoginState}
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.{Money, Currency}
import org.scalajs.dom
import scala.scalajs.js.URIUtils

object EmailOrderModal:

  private val isOpen: Var[Boolean]  = Var(false)
  private val nameVar: Var[String]  = Var("")
  private val emailVar: Var[String] = Var("")
  private val textVar: Var[String]  = Var("")

  /** Open the modal and pre-fill fields from current builder state. */
  def open(): Unit =
    val state = ProductBuilderViewModel.stateVar.now()
    val lang  = state.language

    // Pre-fill name / email from logged-in customer
    state.loginState match
      case LoginState.LoggedIn(customer, _) =>
        val fullName = s"${customer.contactInfo.firstName} ${customer.contactInfo.lastName}".trim
        nameVar.set(fullName)
        emailVar.set(customer.contactInfo.email)
      case _ =>
        ()

    textVar.set(buildEmailText(state, lang))
    isOpen.set(true)

  def close(): Unit = isOpen.set(false)

  // ── Trigger button ───────────────────────────────────────────────────────

  def triggerButton(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    button(
      cls := "email-order-btn",
      child.text <-- lang.map {
        case Language.En => "✉ Order via Email"
        case Language.Cs => "✉ Objednat e-mailem"
      },
      onClick --> { _ => open() },
    )

  // ── Modal rendering ──────────────────────────────────────────────────────

  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    div(
      // Backdrop
      div(
        cls <-- isOpen.signal.map(o =>
          if o then "email-modal-backdrop email-modal-backdrop--visible" else "email-modal-backdrop"
        ),
        onClick --> { _ => close() },
      ),

      // Dialog
      div(
        cls <-- isOpen.signal.map(o =>
          if o then "email-modal-dialog email-modal-dialog--open" else "email-modal-dialog"
        ),

        // Header
        div(
          cls := "email-modal-header",
          h3(
            cls := "email-modal-title",
            child.text <-- lang.map {
              case Language.En => "Request Order via Email"
              case Language.Cs => "Poptávka objednávky e-mailem"
            },
          ),
          button(
            cls := "email-modal-close",
            "×",
            title := "Close",
            onClick --> { _ => close() },
          ),
        ),

        // Body
        div(
          cls := "email-modal-body",

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
                value <-- nameVar.signal,
                onInput.mapToValue --> nameVar,
              ),
              placeholder <-- lang.map {
                case Language.En => "Full name"
                case Language.Cs => "Celé jméno"
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
                value <-- emailVar.signal,
                onInput.mapToValue --> emailVar,
              ),
              placeholder <-- lang.map {
                case Language.En => "your@email.com"
                case Language.Cs => "vas@email.cz"
              },
            ),
          ),

          // Message text area
          div(
            cls := "form-group",
            label(child.text <-- lang.map {
              case Language.En => "Message (pre-filled with your configuration)"
              case Language.Cs => "Zpráva (předvyplněná konfigurací)"
            }),
            textArea(
              cls := "email-modal-textarea",
              controlled(
                value <-- textVar.signal,
                onInput.mapToValue --> textVar,
              ),
            ),
          ),

          // Info note
          p(
            cls := "info-note",
            child.text <-- lang.map {
              case Language.En => "Clicking 'Open Email Client' will open your default email application with the details pre-filled. Please add the shop's email address as the recipient."
              case Language.Cs => "Kliknutím na 'Otevřít e-mailový klient' se otevře váš výchozí e-mailový program s předvyplněnými údaji. Jako příjemce zadejte prosím e-mailovou adresu obchodu."
            },
          ),
        ),

        // Footer buttons
        div(
          cls := "email-modal-footer",
          button(
            cls := "btn-secondary",
            child.text <-- lang.map {
              case Language.En => "Cancel"
              case Language.Cs => "Zrušit"
            },
            onClick --> { _ => close() },
          ),
          button(
            cls := "email-modal-send-btn",
            child.text <-- lang.map {
              case Language.En => "Open Email Client ✉"
              case Language.Cs => "Otevřít e-mailový klient ✉"
            },
            onClick --> { _ =>
              val name  = nameVar.now()
              val email = emailVar.now()
              val text  = textVar.now()
              val lang  = ProductBuilderViewModel.stateVar.now().language

              val state = ProductBuilderViewModel.stateVar.now()
              val subject = state.language match
                case Language.En =>
                  val cat = categoryName(state, Language.En)
                  s"Product Order Inquiry${if cat.nonEmpty then s" - $cat" else ""}"
                case Language.Cs =>
                  val cat = categoryName(state, Language.Cs)
                  s"Poptavka objednavky${if cat.nonEmpty then s" - $cat" else ""}"

              // Build the full message body including name/email signature
              val signature = buildSignature(name, email, lang)
              val body      = s"$text\n$signature"

              val mailtoUri = buildMailtoUri(subject, body)
              dom.window.location.href = mailtoUri
            },
          ),
        ),
      ),
    )

  // ── Private helpers ──────────────────────────────────────────────────────

  private def categoryName(state: BuilderState, lang: Language): String =
    state.selectedCategoryId
      .flatMap(id => ProductBuilderViewModel.catalog.categories.get(id))
      .map(_.name(lang))
      .getOrElse("")

  private def buildSignature(name: String, email: String, lang: Language): String =
    val nameLine  = if name.nonEmpty then s"\n$name" else ""
    val emailLine = if email.nonEmpty then s"\n$email" else ""
    lang match
      case Language.En => s"\n---\nThank you$nameLine$emailLine"
      case Language.Cs => s"\n---\nDěkuji$nameLine$emailLine"

  private def buildEmailText(state: BuilderState, lang: Language): String =
    val sb = new StringBuilder

    // Greeting line
    lang match
      case Language.En => sb.append("Hello,\n\nI would like to inquire about ordering the following product:\n\n")
      case Language.Cs => sb.append("Dobrý den,\n\nRád/ráda bych se poptala/poptával na objednávku následujícího produktu:\n\n")

    // Category
    val catNameStr = categoryName(state, lang)
    if catNameStr.nonEmpty then
      lang match
        case Language.En => sb.append(s"Category: $catNameStr\n")
        case Language.Cs => sb.append(s"Kategorie: $catNameStr\n")

    // Preset
    val presetName = for
      presetId   <- state.selectedPresetId
      categoryId <- state.selectedCategoryId
      category   <- ProductBuilderViewModel.catalog.categories.get(categoryId)
      preset     <- category.presetById(presetId)
    yield preset.name(lang)
    presetName.foreach { name =>
      lang match
        case Language.En => sb.append(s"Variant: $name\n")
        case Language.Cs => sb.append(s"Varianta: $name\n")
    }

    // Printing method
    val methodName = state.selectedPrintingMethodId
      .flatMap(id => ProductBuilderViewModel.catalog.printingMethods.get(id))
      .map(_.name(lang))
    methodName.foreach { name =>
      lang match
        case Language.En => sb.append(s"Printing Method: $name\n")
        case Language.Cs => sb.append(s"Tisková metoda: $name\n")
    }

    sb.append("\n")

    // Specifications
    if state.specifications.nonEmpty then
      lang match
        case Language.En => sb.append("Specifications:\n")
        case Language.Cs => sb.append("Specifikace:\n")
      state.specifications.foreach { spec =>
        val line = formatSpec(spec, lang)
        if line.nonEmpty then sb.append(s"  - $line\n")
      }
      sb.append("\n")

    // Components
    if state.componentStates.nonEmpty then
      lang match
        case Language.En => sb.append("Components:\n")
        case Language.Cs => sb.append("Komponenty:\n")
      state.componentStates.toList.sortBy(_._1.ordinal).foreach { case (role, cs) =>
        val roleLabel = lang match
          case Language.En => role match
            case ComponentRole.Main  => "Main"
            case ComponentRole.Cover => "Cover"
            case ComponentRole.Body  => "Body"
            case ComponentRole.Stand => "Stand"
          case Language.Cs => role match
            case ComponentRole.Main  => "Hlavní"
            case ComponentRole.Cover => "Obálka"
            case ComponentRole.Body  => "Vnitřní část"
            case ComponentRole.Stand => "Stojánek"

        val matName = cs.selectedMaterialId
          .flatMap(id => ProductBuilderViewModel.catalog.materials.get(id))
          .map(_.name(lang))
          .getOrElse(lang match
            case Language.En => "(not selected)"
            case Language.Cs => "(nevybráno)"
          )

        val inkDesc = cs.selectedInkConfig.map { ink =>
          s"${ink.front}+${ink.back}"
        }.getOrElse(lang match
          case Language.En => "(not selected)"
          case Language.Cs => "(nevybráno)"
        )

        val finishNames = cs.selectedFinishes.keys.toList
          .flatMap(id => ProductBuilderViewModel.catalog.finishes.get(id))
          .map(_.name(lang))

        val finishDesc =
          if finishNames.nonEmpty then
            lang match
              case Language.En => s", Finishes: ${finishNames.mkString(", ")}"
              case Language.Cs => s", Úpravy: ${finishNames.mkString(", ")}"
          else ""

        sb.append(s"  - $roleLabel: $matName, $inkDesc$finishDesc\n")
      }
      sb.append("\n")

    // Price
    state.priceBreakdown.foreach { bd =>
      val priceStr = formatMoney(bd.total, bd.currency)
      lang match
        case Language.En => sb.append(s"Calculated Price: $priceStr\n\n")
        case Language.Cs => sb.append(s"Vypočtená cena: $priceStr\n\n")
    }

    // Validation issues (if any)
    if state.validationErrors.nonEmpty then
      lang match
        case Language.En =>
          sb.append("Note – the online configurator reported the following issues for this selection:\n")
          state.validationErrors.foreach(e => sb.append(s"  - $e\n"))
          sb.append("\n")
        case Language.Cs =>
          sb.append("Poznámka – online konfigurátor hlásí pro tuto kombinaci problémy:\n")
          state.validationErrors.foreach(e => sb.append(s"  - $e\n"))
          sb.append("\n")

    lang match
      case Language.En => sb.append("Please contact me to discuss and complete this order.")
      case Language.Cs => sb.append("Prosím, kontaktujte mě pro dokončení a upřesnění objednávky.")

    sb.toString()

  private def formatSpec(spec: SpecValue, lang: Language): String =
    spec match
      case SpecValue.QuantitySpec(qty) =>
        lang match
          case Language.En => s"Quantity: ${qty.value}"
          case Language.Cs => s"Počet: ${qty.value}"
      case SpecValue.SizeSpec(dim) =>
        lang match
          case Language.En => s"Size: ${dim.widthMm}×${dim.heightMm} mm"
          case Language.Cs => s"Rozměr: ${dim.widthMm}×${dim.heightMm} mm"
      case SpecValue.OrientationSpec(orientation) =>
        val v = orientation match
          case Orientation.Portrait  => lang match { case Language.En => "Portrait";  case Language.Cs => "Na výšku" }
          case Orientation.Landscape => lang match { case Language.En => "Landscape"; case Language.Cs => "Na šířku" }
        lang match
          case Language.En => s"Orientation: $v"
          case Language.Cs => s"Orientace: $v"
      case SpecValue.FoldTypeSpec(fold) =>
        val v = fold match
          case FoldType.Half       => lang match { case Language.En => "Half fold";       case Language.Cs => "Přeložení napůl" }
          case FoldType.Tri        => lang match { case Language.En => "Tri fold";        case Language.Cs => "Trojsložení" }
          case FoldType.ZFold      => lang match { case Language.En => "Z fold";          case Language.Cs => "Z-složení" }
          case FoldType.Gate       => lang match { case Language.En => "Gate fold";       case Language.Cs => "Bránové složení" }
          case FoldType.Accordion  => lang match { case Language.En => "Accordion fold";  case Language.Cs => "Harmonikové složení" }
          case FoldType.RollFold   => lang match { case Language.En => "Roll fold";       case Language.Cs => "Válcové složení" }
          case FoldType.FrenchFold => lang match { case Language.En => "French fold";     case Language.Cs => "Francouzské složení" }
          case FoldType.CrossFold  => lang match { case Language.En => "Cross fold";      case Language.Cs => "Křížové složení" }
        lang match
          case Language.En => s"Fold type: $v"
          case Language.Cs => s"Typ přeložení: $v"
      case SpecValue.BindingMethodSpec(binding) =>
        val v = binding match
          case BindingMethod.SaddleStitch   => lang match { case Language.En => "Saddle stitch";   case Language.Cs => "Sešití na hřbet" }
          case BindingMethod.PerfectBinding => lang match { case Language.En => "Perfect binding"; case Language.Cs => "Lepená vazba" }
          case BindingMethod.SpiralBinding  => lang match { case Language.En => "Spiral binding";  case Language.Cs => "Spirálová vazba" }
          case BindingMethod.WireOBinding   => lang match { case Language.En => "Wire-O binding";  case Language.Cs => "Wire-O vazba" }
          case BindingMethod.CaseBinding    => lang match { case Language.En => "Case binding";    case Language.Cs => "Tvrdá vazba" }
        lang match
          case Language.En => s"Binding: $v"
          case Language.Cs => s"Vazba: $v"
      case SpecValue.PagesSpec(count) =>
        lang match
          case Language.En => s"Pages: $count"
          case Language.Cs => s"Počet stran: $count"
      case SpecValue.ManufacturingSpeedSpec(speed) =>
        val v = speed match
          case ManufacturingSpeed.Standard => lang match { case Language.En => "Standard"; case Language.Cs => "Standardní" }
          case ManufacturingSpeed.Express  => lang match { case Language.En => "Express";  case Language.Cs => "Expres" }
          case ManufacturingSpeed.Economy  => lang match { case Language.En => "Economy";  case Language.Cs => "Ekonomický" }
        lang match
          case Language.En => s"Manufacturing speed: $v"
          case Language.Cs => s"Rychlost výroby: $v"

  private def formatMoney(money: Money, currency: Currency): String =
    currency match
      case Currency.CZK => f"${money.value}%.2f Kč"
      case Currency.USD => f"$$${money.value}%.2f"
      case Currency.EUR => f"€${money.value}%.2f"
      case Currency.GBP => f"£${money.value}%.2f"

  private def buildMailtoUri(subject: String, body: String): String =
    val encodedSubject = URIUtils.encodeURIComponent(subject)
    val encodedBody    = URIUtils.encodeURIComponent(body)
    s"mailto:?subject=$encodedSubject&body=$encodedBody"
