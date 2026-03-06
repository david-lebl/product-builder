package mpbuilder.uicommon

import com.raquo.laminar.api.L.*

/** Reusable step indicator bar for multi-step wizards.
  *
  * Renders a horizontal bar of step circles with labels. Steps before the current
  * step show a checkmark and `--done` class; the current step is `--active`;
  * future steps have a neutral style.
  */
object StepIndicator:

  /** Describes a single step in the wizard. */
  case class Step(
    id: String,
    number: String,
    label: String,
  )

  /** Renders the step indicator bar.
    *
    * @param steps           Ordered list of steps (first = index 0)
    * @param currentIndex    Zero-based index of the currently active step
    * @param containerCls    CSS class for the outer container (default: `"checkout-steps"`)
    */
  def apply(
    steps: List[Step],
    currentIndex: Int,
    containerCls: String = "checkout-steps",
  ): HtmlElement =
    div(
      cls := containerCls,
      steps.zipWithIndex.map { case (step, idx) =>
        val stepCls =
          if idx < currentIndex then "checkout-step checkout-step--done"
          else if idx == currentIndex then "checkout-step checkout-step--active"
          else "checkout-step"
        div(
          cls := stepCls,
          span(cls := "checkout-step-num", if idx < currentIndex then "✓" else step.number),
          span(cls := "checkout-step-label", step.label),
        )
      },
    )

  /** Reactive step indicator driven by a signal of the current step index. */
  def reactive(
    steps: List[Step],
    currentIndexSignal: Signal[Int],
    containerCls: String = "checkout-steps",
  ): HtmlElement =
    div(
      cls := containerCls,
      steps.zipWithIndex.map { case (step, idx) =>
        div(
          cls <-- currentIndexSignal.map { currentIdx =>
            if idx < currentIdx then "checkout-step checkout-step--done"
            else if idx == currentIdx then "checkout-step checkout-step--active"
            else "checkout-step"
          },
          child.text <-- currentIndexSignal.map { currentIdx =>
            if idx < currentIdx then "✓" else step.number
          }.map(_ => ""),          // clear — we use spans instead
          span(
            cls := "checkout-step-num",
            child.text <-- currentIndexSignal.map { currentIdx =>
              if idx < currentIdx then "✓" else step.number
            },
          ),
          span(cls := "checkout-step-label", step.label),
        )
      },
    )
