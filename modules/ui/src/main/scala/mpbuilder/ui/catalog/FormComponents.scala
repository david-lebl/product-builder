package mpbuilder.ui.catalog

import com.raquo.laminar.api.L.*
import mpbuilder.uikit.fields.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*

/** Domain-specific form components that extend the generic ui-framework FormComponents.
  *
  * Generic components (textField, numberField, optionalNumberField, enumSelect, enumSelectRequired,
  * enumCheckboxSet, idCheckboxSet, actionButton, dangerButton, sectionHeader) are re-exported
  * from `mpbuilder.uikit.form.FormComponents`.
  *
  * This object adds domain-aware components: `localizedStringEditor` and `moneyField`.
  */
object FormComponents:

  // Re-export all generic components
  export mpbuilder.uikit.form.FormComponents.{textField, numberField, optionalNumberField,
    enumSelect, enumSelectRequired, enumCheckboxSet, idCheckboxSet,
    actionButton, dangerButton, sectionHeader}

  // ── LocalizedString editor ───────────────────────────────────────────────

  def localizedStringEditor(
    label: String,
    value: Signal[LocalizedString],
    onChange: Observer[LocalizedString],
  ): HtmlElement =
    val enVar = Var("")
    val csVar = Var("")
    div(
      cls := "form-group localized-string-editor",
      com.raquo.laminar.api.L.label(label),
      value --> Observer[LocalizedString] { ls =>
        enVar.set(ls(Language.En))
        csVar.set(ls(Language.Cs))
      },
      div(
        cls := "localized-fields",
        div(
          span(cls := "lang-tag", "EN"),
          input(
            typ := "text",
            com.raquo.laminar.api.L.value <-- value.map(_(Language.En)),
            onInput.mapToValue --> { en =>
              enVar.set(en)
              onChange.onNext(LocalizedString(en, csVar.now()))
            },
          ),
        ),
        div(
          span(cls := "lang-tag", "CS"),
          input(
            typ := "text",
            com.raquo.laminar.api.L.value <-- value.map(_(Language.Cs)),
            onInput.mapToValue --> { cs =>
              csVar.set(cs)
              onChange.onNext(LocalizedString(enVar.now(), cs))
            },
          ),
        ),
      ),
    )

  // ── Money field ──────────────────────────────────────────────────────────

  def moneyField(
    label: String,
    value: Signal[Money],
    onChange: Observer[Money],
  ): HtmlElement =
    TextField(
      label = Val(label),
      value = value.map(_.value.toString),
      onInput = Observer[String] { s =>
        scala.util.Try(BigDecimal(s)).foreach(v => onChange.onNext(Money(v)))
      },
      inputType = "number",
    )
