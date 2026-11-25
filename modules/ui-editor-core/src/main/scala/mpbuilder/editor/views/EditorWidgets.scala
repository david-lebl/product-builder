package mpbuilder.editor.views

import kyo.*
import kyo.UI.*
import mpbuilder.design.*
import mpbuilder.ui.components.Icons
import mpbuilder.ui.components.Icons.Icon

/** Small form controls used by the editor panels (all styled by `EditorTheme.css`). */
object EditorWidgets:

  def fmt(v: Double, decimals: Int = 1): String =
    val p = math.pow(10, decimals)
    val r = math.round(v * p) / p
    if r == r.toLong.toDouble then r.toLong.toString else r.toString

  def panelSection(title: Signal[String], children: UI*): UI =
    div.cssClass("ve-section")(
      h4(title).cssClass("ve-section-title"),
      UI.fragment(children*),
    )

  def field(label: Signal[String], control: UI): UI =
    label_(label, control)

  private def label_(label: Signal[String], control: UI): UI =
    UI.label.cssClass("ve-field")(span(label).cssClass("ve-field-label"), control)

  /** Numeric input that commits on change (blur / Enter). */
  def number(value: Double, onCommit: Double => Unit < Async, step: Double = 0.5, min: Double = -10000, max: Double = 10000, decimals: Int = 1): UI =
    numberInput
      .cssClass("mp-input ve-num")
      .value(fmt(value, decimals))
      .step(step)
      .min(min)
      .max(max)
      .onChange(raw => raw.replace(',', '.').toDoubleOption.filter(v => v >= min && v <= max) match
        case Some(v) => onCommit(v)
        case None    => Kyo.unit
      )

  def slider(value: Double, min: Double, max: Double, step: Double, onInput: Double => Unit < Async): UI =
    rangeInput.cssClass("ve-range").min(min).max(max).step(step).value(value).onChange(onInput)

  def iconToggle(icon: Icon, label: String, active: Boolean, onClick: => Unit < Async): UI =
    button
      .cssClass(if active then "ve-tool ve-tool-active" else "ve-tool")
      .aria("label", label)
      .aria("pressed", active.toString)
      .data("tooltip", label)
      .onClick(onClick)(Icons(icon, 16))

  def textToggle(text: String, label: String, active: Boolean, onClick: => Unit < Async, extraClass: String = ""): UI =
    button
      .cssClass((if active then "ve-tool ve-tool-active" else "ve-tool") + (if extraClass.nonEmpty then s" $extraClass" else ""))
      .aria("label", label)
      .aria("pressed", active.toString)
      .data("tooltip", label)
      .onClick(onClick)(text)

  def toolButton(icon: Icon, label: String, onClick: => Unit < Async, disabled: Boolean = false): UI =
    button
      .cssClass("ve-tool")
      .aria("label", label)
      .data("tooltip", label)
      .disabled(disabled)
      .onClick(onClick)(Icons(icon, 16))

  val Swatches: List[String] = List(
    "#ffffff", "#f5efe6", "#e5e7eb", "#9ca3af", "#1f2937", "#000000",
    "#dc2626", "#f97316", "#eab308", "#16a34a", "#0d9488", "#0284c7",
    "#4f46e5", "#7c3aed", "#db2777", "#1e1b4b", "#166534", "#7f1d1d",
  )

  /** Colour picker: swatch grid + native colour input. */
  def colorPicker(current: Option[DesignColor], onPick: DesignColor => Unit < Async): UI =
    div.cssClass("ve-colors")(
      div.cssClass("ve-swatches")(
        Swatches.map { hex =>
          val active = current.exists(_.hex == hex)
          button
            .cssClass(if active then "ve-swatch ve-swatch-active" else "ve-swatch")
            .aria("label", hex)
            .style(Style.bg(Style.Color.hex(hex).getOrElse(Style.Color.white)))
            .onClick(onPick(DesignColor.unsafe(hex)))
        }*
      ),
      colorInput
        .cssClass("ve-color-input")
        .value(current.map(_.hex).getOrElse("#000000"))
        .onChange(raw => DesignColor.parse(raw).toOption.fold(Kyo.unit: Unit < Async)(onPick)),
    )

  def selectBox[A](options: Seq[(A, String)], current: A, onSelect: A => Unit < Async)(using CanEqual[A, A]): UI =
    val indexed = options.zipWithIndex
    select
      .cssClass("mp-input ve-select")
      .value(indexed.find(_._1._1 == current).map(_._2.toString).getOrElse("0"))
      .onChange(raw => raw.toIntOption.flatMap(i => options.lift(i)).fold(Kyo.unit: Unit < Async)(o => onSelect(o._1)))(
        indexed.map { case ((a, label), i) => option.value(i.toString).selected(a == current)(label) }*
      )

  /** Live SVG render of a page (thumbnails, template cards, review). */
  def pageArt(svg: String, extraClass: String = ""): UI =
    div.cssClass(if extraClass.isEmpty then "ve-art" else s"ve-art $extraClass")(UI.rawHtml(svg))
