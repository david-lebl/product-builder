package mpbuilder.uikit.form

import com.raquo.laminar.api.L.*
import mpbuilder.uikit.fields.*

/** Reusable ADT-derived form components for editing typed values.
  *
  * These components work with any Scala 3 enum or value type, making them
  * suitable for building editors across all UI modules (catalog, manufacturing,
  * customer management, etc.).
  */
object FormComponents:

  // ── Text & Number fields ─────────────────────────────────────────────────

  def textField(
    label: String,
    value: Signal[String],
    onInput: Observer[String],
    placeholder: String = "",
  ): HtmlElement =
    TextField(
      label = Val(label),
      value = value,
      onInput = onInput,
      placeholder = Val(placeholder),
    )

  def numberField(
    label: String,
    value: Signal[String],
    onInput: Observer[String],
  ): HtmlElement =
    TextField(
      label = Val(label),
      value = value,
      onInput = onInput,
      inputType = "number",
    )

  def textAreaField(
    label: String,
    value: Signal[String],
    onInput: Observer[String],
    placeholder: String = "",
  ): HtmlElement =
    TextAreaField(
      label = Val(label),
      value = value,
      onInput = onInput,
      placeholder = Val(placeholder),
    )

  def optionalNumberField(
    label: String,
    value: Signal[Option[Int]],
    onChange: Observer[Option[Int]],
  ): HtmlElement =
    TextField(
      label = Val(label),
      value = value.map(_.map(_.toString).getOrElse("")),
      onInput = Observer[String](s => onChange.onNext(s.toIntOption)),
      inputType = "number",
      placeholder = Val("(none)"),
    )

  // ── Enum-based select ────────────────────────────────────────────────────

  /** Dropdown for any Scala 3 enum type. Requires the enum values as input. */
  def enumSelect[E](
    label: String,
    values: Array[E],
    selected: Signal[Option[E]],
    onChange: Observer[Option[E]],
    display: E => String = (e: E) => e.toString,
  ): HtmlElement =
    val options = Val(
      values.toList.map(v => SelectOption(v.toString, display(v)))
    )
    SelectField(
      label = Val(label),
      options = options,
      selected = selected.map(_.map(_.toString).getOrElse("")),
      onChange = Observer[String] { s =>
        onChange.onNext(values.find(_.toString == s))
      },
      placeholder = Val("— select —"),
    )

  /** Required enum select — always has a value selected. */
  def enumSelectRequired[E](
    label: String,
    values: Array[E],
    selected: Signal[E],
    onChange: Observer[E],
    display: E => String = (e: E) => e.toString,
  ): HtmlElement =
    val options = Val(
      values.toList.map(v => SelectOption(v.toString, display(v)))
    )
    SelectField(
      label = Val(label),
      options = options,
      selected = selected.map(_.toString),
      onChange = Observer[String] { s =>
        values.find(_.toString == s).foreach(onChange.onNext)
      },
    )

  /** Checkbox set for selecting multiple enum values (Set[E]). */
  def enumCheckboxSet[E](
    label: String,
    values: Array[E],
    selected: Signal[Set[E]],
    onChange: Observer[Set[E]],
    display: E => String = (e: E) => e.toString,
  ): HtmlElement =
    val localVar = Var(Set.empty[E])
    div(
      cls := "form-group",
      com.raquo.laminar.api.L.label(label),
      selected --> localVar.writer,
      div(
        cls := "checkbox-set",
        values.toList.map { v =>
          com.raquo.laminar.api.L.label(
            cls := "checkbox-label",
            input(
              typ := "checkbox",
              checked <-- selected.map(_.contains(v)),
              com.raquo.laminar.api.L.onChange.mapToChecked --> { isChecked =>
                val current = localVar.now()
                val updated = if isChecked then current + v else current - v
                localVar.set(updated)
                onChange.onNext(updated)
              },
            ),
            span(display(v)),
          )
        },
      ),
    )

  // ── ID-based checkbox set ────────────────────────────────────────────────

  /** Checkbox set for selecting multiple IDs from a list of available options.
    * Works with any ID type — caller provides to/from string conversions.
    */
  def idCheckboxSet[Id](
    label: String,
    available: Signal[List[(Id, String)]],
    selected: Signal[Set[Id]],
    onChange: Observer[Set[Id]],
    idToString: Id => String,
    stringToId: String => Id,
  ): HtmlElement =
    div(
      cls := "form-group",
      com.raquo.laminar.api.L.label(label),
      div(
        cls := "checkbox-set",
        children <-- available.combineWith(selected).map { case (avail, sel) =>
          avail.map { case (id, display) =>
            com.raquo.laminar.api.L.label(
              cls := "checkbox-label",
              input(
                typ := "checkbox",
                checked := sel.contains(id),
                com.raquo.laminar.api.L.onChange.mapToChecked --> { isChecked =>
                  onChange.onNext(if isChecked then sel + id else sel - id)
                },
              ),
              span(display),
            )
          }
        },
      ),
    )

  // ── Action buttons ───────────────────────────────────────────────────────

  def actionButton(label: String, onClick: () => Unit): HtmlElement =
    button(
      cls := "btn btn-primary",
      label,
      com.raquo.laminar.api.L.onClick --> { _ => onClick() },
    )

  def dangerButton(label: String, onClick: () => Unit): HtmlElement =
    button(
      cls := "btn btn-danger",
      label,
      com.raquo.laminar.api.L.onClick --> { _ => onClick() },
    )

  def sectionHeader(title: String): HtmlElement =
    h3(cls := "section-header", title)
