package mpbuilder.ui.productbuilder.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.*
import mpbuilder.domain.service.ManufacturingSpeedValidator
import mpbuilder.domain.sample.SamplePricelist
import mpbuilder.uikit.fields.{RadioGroup, RadioOption}
import mpbuilder.uikit.feedback.HelpInfo

object ManufacturingSpeedSelector:
  def apply(): Element =
    val lang = ProductBuilderViewModel.currentLanguage
    val selectedSpeed = ProductBuilderViewModel.state.map { s =>
      s.specifications.collectFirst {
        case SpecValue.ManufacturingSpeedSpec(speed) => speed.toString
      }.getOrElse(ManufacturingSpeed.Standard.toString)
    }

    div(
      cls := "form-group manufacturing-speed-selector",
      div(
        cls := "selector-with-help",
        RadioGroup(
          label = lang.map {
            case Language.En => "Manufacturing Speed:"
            case Language.Cs => "Rychlost výroby:"
          },
          name = "manufacturing-speed",
          options = lang.map { l =>
            List(
              RadioOption(ManufacturingSpeed.Express.toString, Val(l match
                case Language.En => "⚡ Express (+35%)"
                case Language.Cs => "⚡ Expresní (+35%)"
              )),
              RadioOption(ManufacturingSpeed.Standard.toString, Val(l match
                case Language.En => "● Standard (recommended)"
                case Language.Cs => "● Standardní (doporučeno)"
              )),
              RadioOption(ManufacturingSpeed.Economy.toString, Val(l match
                case Language.En => "\uD83D\uDC22 Economy (−15%)"
                case Language.Cs => "\uD83D\uDC22 Ekonomická (−15%)"
              )),
            )
          },
          selected = selectedSpeed,
          onChange = Observer[String] { value =>
            ManufacturingSpeed.values.find(_.toString == value).foreach { speed =>
              ProductBuilderViewModel.replaceSpecification(
                SpecValue.ManufacturingSpeedSpec(speed)
              )
            }
          },
        ),
        div(
          cls := "selector-help-buttons",
          HelpInfo(lang.map {
            case Language.En => "Choose how fast your order is manufactured. Express orders are prioritized and completed faster, but cost more. Economy is the best value for non-urgent orders."
            case Language.Cs => "Zvolte, jak rychle bude vaše objednávka vyrobena. Expresní objednávky jsou prioritní a dokončeny rychleji, ale stojí více. Ekonomická je nejlepší volba pro neurgentní objednávky."
          }),
        ),
      ),

      // Tier availability warning
      child.maybe <-- ProductBuilderViewModel.state.combineWith(lang).map { case (state, l) =>
        val speed = state.specifications.collectFirst {
          case SpecValue.ManufacturingSpeedSpec(s) => s
        }
        speed.flatMap { s =>
          state.configuration.flatMap { config =>
            val validation = ManufacturingSpeedValidator.validate(
              config, s, SamplePricelist.tierRestrictions,
            )
            validation.fold(
              errors => Some(
                div(
                  cls := "speed-tier-warning",
                  errors.map { err =>
                    div(cls := "warning-message", s"⚠ ${err.message(l)}")
                  }.toList,
                )
              ),
              _ => None,
            )
          }
        }
      },
    )
