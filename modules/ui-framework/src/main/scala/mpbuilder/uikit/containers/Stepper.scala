package mpbuilder.uikit.containers

import com.raquo.laminar.api.L.*

case class StepDef(
  id: String,
  label: Signal[String],
  content: () => HtmlElement,
  canProceed: Signal[Boolean] = Val(true),
)

object Stepper:
  def apply(
    steps: List[StepDef],
    currentStep: Var[String],
    prevLabel: Signal[String] = Val("← Back"),
    nextLabel: Signal[String] = Val("Continue →"),
    mods: Modifier[HtmlElement]*
  ): HtmlElement =
    val currentIndex = currentStep.signal.map(id => steps.indexWhere(_.id == id).max(0))

    div(
      cls := "stepper",
      // Step indicator bar
      div(
        cls := "stepper-bar",
        children <-- currentIndex.map { idx =>
          steps.zipWithIndex.map { case (step, i) =>
            val stepCls =
              if i < idx then "stepper-step stepper-step--done"
              else if i == idx then "stepper-step stepper-step--active"
              else "stepper-step"
            div(
              cls := stepCls,
              span(cls := "stepper-step-num", if i < idx then "✓" else (i + 1).toString),
              span(cls := "stepper-step-label", child.text <-- step.label),
            )
          }
        },
      ),
      // Active step content
      div(
        cls := "stepper-content",
        children <-- currentIndex.map { idx =>
          steps.lift(idx).map(_.content()).toList
        },
      ),
      // Navigation
      div(
        cls := "stepper-nav",
        child <-- currentIndex.map { idx =>
          if idx > 0 then
            button(
              cls := "btn-secondary",
              child.text <-- prevLabel,
              onClick --> { _ =>
                steps.lift(idx - 1).foreach(s => currentStep.set(s.id))
              },
            )
          else emptyNode
        },
        child <-- currentIndex.map { idx =>
          if idx < steps.size - 1 then
            val step = steps(idx)
            button(
              cls := "stepper-btn",
              disabled <-- step.canProceed.map(!_),
              child.text <-- nextLabel,
              onClick --> { _ =>
                steps.lift(idx + 1).foreach(s => currentStep.set(s.id))
              },
            )
          else emptyNode
        },
      ),
      mods,
    )
