package mpbuilder.uikit.fields

import com.raquo.laminar.api.L.*
import org.scalajs.dom

case class ComboBoxOption(
  value: String,
  display: String,
  icon: Option[String] = None,
  group: Option[String] = None,
)

object ComboBoxField:

  /** Just the combobox widget — no FormGroup wrapper. Use when you need a
    * custom label area (e.g. with additional help buttons alongside the label).
    */
  def widget(
    options: Signal[List[ComboBoxOption]],
    selected: Signal[String],
    onChange: Observer[String],
    placeholder: Signal[String] = Val(""),
    disabled: Signal[Boolean] = Val(false),
  ): HtmlElement =
    val isOpen    = Var(false)
    val filterVar = Var("")

    val displayText: Signal[String] =
      options.combineWith(selected, placeholder).map { case (opts, sel, ph) =>
        if sel.isEmpty then ph
        else opts.find(_.value == sel).map(_.display).getOrElse(ph)
      }

    val selectedIcon: Signal[Option[String]] =
      options.combineWith(selected).map { case (opts, sel) =>
        opts.find(_.value == sel).flatMap(_.icon)
      }

    val filteredOptions: Signal[List[ComboBoxOption]] =
      options.combineWith(filterVar.signal).map { case (opts, f) =>
        val trimmed = f.trim.toLowerCase
        if trimmed.isEmpty then opts
        else opts.filter(_.display.toLowerCase.contains(trimmed))
      }

    // Group preserving insertion order
    val groupedOptions: Signal[List[(Option[String], List[ComboBoxOption])]] =
      filteredOptions.map { opts =>
        opts.foldLeft(List.empty[(Option[String], List[ComboBoxOption])]) { case (acc, opt) =>
          acc match
            case init :+ ((g, items)) if g == opt.group => init :+ (g, items :+ opt)
            case _                                       => acc :+ (opt.group, List(opt))
        }
      }

    val inputValue: Signal[String] =
      isOpen.signal.flatMapSwitch(open => if open then filterVar.signal else displayText)

    var containerEl: dom.Element = null

    div(
      cls := "combobox",
      cls("combobox--open") <-- isOpen.signal,
      cls("combobox--disabled") <-- disabled,
      onMountCallback { ctx => containerEl = ctx.thisNode.ref },
      documentEvents(_.onClick) --> { (e: dom.MouseEvent) =>
        if containerEl != null && !containerEl.contains(e.target.asInstanceOf[dom.Node]) then
          isOpen.set(false)
          filterVar.set("")
      },

      // Trigger row
      div(
        cls := "combobox__trigger",
        onClick --> { _ =>
          if !isOpen.now() then
            filterVar.set("")
            isOpen.set(true)
        },

        // Icon of selected item
        child.maybe <-- selectedIcon.map(_.map(ic =>
          span(cls := "combobox__trigger-icon", ic)
        )),

        // Input — always present, value switches between display text and filter
        input(
          typ := "text",
          cls := "combobox__trigger-input",
          com.raquo.laminar.api.L.disabled <-- disabled,
          value <-- inputValue,
          onInput.mapToValue --> { v =>
            filterVar.set(v)
            if !isOpen.now() then isOpen.set(true)
          },
          onFocus --> { _ =>
            filterVar.set("")
            isOpen.set(true)
          },
          onKeyDown --> { (e: dom.KeyboardEvent) =>
            if e.key == "Escape" then
              isOpen.set(false)
              filterVar.set("")
          },
        ),

        // Dropdown arrow
        span(
          cls := "combobox__arrow",
          child.text <-- isOpen.signal.map(if _ then "▴" else "▾"),
          onClick.stopPropagation --> { _ =>
            if isOpen.now() then
              isOpen.set(false)
              filterVar.set("")
            else
              filterVar.set("")
              isOpen.set(true)
          },
        ),
      ),

      // Dropdown panel
      div(
        cls := "combobox__dropdown",
        display <-- isOpen.signal.map(if _ then "block" else "none"),
        children <-- groupedOptions.map { groups =>
          if groups.isEmpty then
            List(div(cls := "combobox__empty", "No results"))
          else
            groups.flatMap { case (groupName, opts) =>
              val header = groupName.map(g =>
                div(cls := "combobox__group-header", g)
              ).toList
              val items = opts.map { opt =>
                div(
                  cls := "combobox__option",
                  cls("combobox__option--selected") <-- selected.map(_ == opt.value),
                  onClick.stopPropagation --> { _ =>
                    onChange.onNext(opt.value)
                    isOpen.set(false)
                    filterVar.set("")
                  },
                  opt.icon.map(ic => span(cls := "combobox__option-icon", ic)),
                  span(cls := "combobox__option-label", opt.display),
                )
              }
              header ++ items
            }
        },
      ),
    )

  /** Full form field: wraps the widget in a FormGroup with label and error. */
  def apply(
    label: Signal[String],
    options: Signal[List[ComboBoxOption]],
    selected: Signal[String],
    onChange: Observer[String],
    placeholder: Signal[String] = Val(""),
    disabled: Signal[Boolean] = Val(false),
    error: Signal[Option[String]] = Val(None),
    mods: Modifier[HtmlElement]*
  ): HtmlElement =
    FormGroup(label, error, mods*)(
      widget(options, selected, onChange, placeholder, disabled)
    )
