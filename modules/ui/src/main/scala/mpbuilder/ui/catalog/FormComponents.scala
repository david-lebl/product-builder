package mpbuilder.ui.catalog

import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.L

/** Abstract UI form components derived from Scala ADT patterns.
  * These provide generic editors for enums, text fields, numbers,
  * sets, and lists that can be composed to build type-safe forms.
  */
object FormComponents:

  /** Renders a text input field with a label. */
  def textField(
    labelText: String,
    currentValue: Signal[String],
    onUpdate: String => Unit,
    placeholderText: String = "",
  ): Element =
    div(
      cls := "form-group",
      L.label(labelText),
      input(
        typ := "text",
        placeholder := placeholderText,
        controlled(
          value <-- currentValue,
          onInput.mapToValue --> { v => onUpdate(v) },
        ),
      ),
    )

  /** Renders a numeric input field with a label. */
  def numberField(
    labelText: String,
    currentValue: Signal[String],
    onUpdate: String => Unit,
    step: String = "1",
    placeholderText: String = "",
  ): Element =
    div(
      cls := "form-group",
      L.label(labelText),
      input(
        typ := "number",
        stepAttr := step,
        placeholder := placeholderText,
        controlled(
          value <-- currentValue,
          onInput.mapToValue --> { v => onUpdate(v) },
        ),
      ),
    )

  /** Renders a dropdown for selecting a single enum value. */
  def enumSelect[E](
    labelText: String,
    values: Array[E],
    currentValue: Signal[Option[E]],
    onUpdate: Option[E] => Unit,
    display: E => String = (e: E) => e.toString,
    allowEmpty: Boolean = true,
    emptyLabel: String = "-- Select --",
  ): Element =
    div(
      cls := "form-group",
      L.label(labelText),
      select(
        children <-- currentValue.map { sel =>
          val currentStr = sel.map(_.toString).getOrElse("")
          val emptyOpt = if allowEmpty then
            List(option(emptyLabel, value := "", L.selected := currentStr.isEmpty))
          else Nil
          emptyOpt ++ values.map { e =>
            option(display(e), value := e.toString, L.selected := (e.toString == currentStr))
          }.toList
        },
        onChange.mapToValue --> { v =>
          if v.isEmpty then onUpdate(None)
          else onUpdate(values.find(_.toString == v))
        },
      ),
    )

  /** Renders a required enum select (no empty option). */
  def enumSelectRequired[E](
    labelText: String,
    values: Array[E],
    currentValue: Signal[E],
    onUpdate: E => Unit,
    display: E => String = (e: E) => e.toString,
  ): Element =
    div(
      cls := "form-group",
      L.label(labelText),
      select(
        children <-- currentValue.map { sel =>
          values.map { e =>
            option(display(e), value := e.toString, L.selected := (e == sel))
          }.toList
        },
        onChange.mapToValue --> { v =>
          values.find(_.toString == v).foreach(onUpdate)
        },
      ),
    )

  /** Renders a checkbox group for selecting a Set of enum values. */
  def enumCheckboxSet[E](
    labelText: String,
    values: Array[E],
    currentValues: Signal[Set[E]],
    onUpdate: Set[E] => Unit,
    display: E => String = (e: E) => e.toString,
  ): Element =
    div(
      cls := "form-group",
      L.label(labelText),
      div(
        cls := "catalog-checkbox-group",
        children <-- currentValues.map { currentSet =>
          values.map { e =>
            L.label(
              cls := "catalog-checkbox-label",
              input(
                typ := "checkbox",
                checked := currentSet.contains(e),
                onChange.mapToChecked --> { isChecked =>
                  val updated =
                    if isChecked then currentSet + e
                    else currentSet - e
                  onUpdate(updated)
                },
              ),
              span(display(e)),
            )
          }.toSeq
        },
      ),
    )

  /** Renders a set editor for string-based IDs with checkboxes. */
  def idCheckboxSet(
    labelText: String,
    availableItems: Signal[List[(String, String)]],
    currentIds: Signal[Set[String]],
    onUpdate: Set[String] => Unit,
  ): Element =
    div(
      cls := "form-group",
      L.label(labelText),
      div(
        cls := "catalog-checkbox-group",
        children <-- availableItems.combineWith(currentIds).map { case (items, currentSet) =>
          items.map { case (id, displayName) =>
            L.label(
              cls := "catalog-checkbox-label",
              input(
                typ := "checkbox",
                checked := currentSet.contains(id),
                onChange.mapToChecked --> { isChecked =>
                  val updated =
                    if isChecked then currentSet + id
                    else currentSet - id
                  onUpdate(updated)
                },
              ),
              span(displayName),
            )
          }
        },
      ),
    )

  /** Renders optional number input (None when empty). */
  def optionalNumberField(
    labelText: String,
    currentValue: Signal[Option[Int]],
    onUpdate: Option[Int] => Unit,
    placeholderText: String = "None",
  ): Element =
    div(
      cls := "form-group",
      L.label(labelText),
      input(
        typ := "number",
        placeholder := placeholderText,
        controlled(
          value <-- currentValue.map(_.map(_.toString).getOrElse("")),
          onInput.mapToValue --> { v =>
            if v.isEmpty then onUpdate(None)
            else v.toIntOption.foreach(n => onUpdate(Some(n)))
          },
        ),
      ),
    )

  /** Button with consistent styling. */
  def actionButton(
    text: String,
    onClickFn: () => Unit,
    cssClass: String = "",
  ): Element =
    L.button(
      cls := s"catalog-btn $cssClass".trim,
      text,
      L.onClick --> { _ => onClickFn() },
    )

  /** Danger button for destructive actions. */
  def dangerButton(
    text: String,
    onClickFn: () => Unit,
  ): Element =
    L.button(
      cls := "catalog-btn catalog-btn-danger",
      text,
      L.onClick --> { _ => onClickFn() },
    )

  /** Section header with optional action button. */
  def sectionHeader(
    title: String,
    actionLabel: Option[String] = None,
    onAction: () => Unit = () => (),
  ): Element =
    div(
      cls := "catalog-section-header",
      h3(title),
      actionLabel match
        case Some(lbl) => actionButton(lbl, onAction, "catalog-btn-small")
        case None => emptyNode,
    )

  /** Localized string editor (EN + CS fields). */
  def localizedStringEditor(
    labelText: String,
    enValue: Signal[String],
    csValue: Signal[String],
    onEnUpdate: String => Unit,
    onCsUpdate: String => Unit,
  ): Element =
    div(
      cls := "form-group",
      L.label(labelText),
      div(
        cls := "catalog-localized-group",
        div(
          cls := "catalog-localized-field",
          L.label("EN:"),
          input(
            typ := "text",
            controlled(
              value <-- enValue,
              onInput.mapToValue --> { v => onEnUpdate(v) },
            ),
          ),
        ),
        div(
          cls := "catalog-localized-field",
          L.label("CS:"),
          input(
            typ := "text",
            controlled(
              value <-- csValue,
              onInput.mapToValue --> { v => onCsUpdate(v) },
            ),
          ),
        ),
      ),
    )
