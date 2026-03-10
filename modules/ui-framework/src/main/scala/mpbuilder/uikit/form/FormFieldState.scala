package mpbuilder.uikit.form

import com.raquo.laminar.api.L.*

class FormFieldState[A](
  val name: String,
  val rawVar: Var[String],
  val fieldType: FieldType[A],
  val validators: List[FieldValidator[A]],
):
  private val touchedVar: Var[Boolean] = Var(false)

  val parsed: Signal[Either[List[String], A]] =
    rawVar.signal.map { raw =>
      fieldType.parse(raw) match
        case Left(parseErr) => Left(List(parseErr))
        case Right(value) =>
          val errors = validators.flatMap(_.validate(value))
          if errors.nonEmpty then Left(errors) else Right(value)
    }

  /** Writer that marks the field as touched and updates the raw value. */
  val touchedWriter: Observer[String] = Observer[String] { v =>
    rawVar.set(v)
    touchedVar.set(true)
  }

  /** Shows the first error only after the field has been touched. */
  val firstError: Signal[Option[String]] =
    parsed.combineWith(touchedVar.signal).map {
      case (Left(errs), true) => errs.headOption
      case _                  => None
    }

  val value: Signal[A] =
    parsed.map(_.getOrElse(fieldType.defaultValue))

  def touch(): Unit = touchedVar.set(true)

object FormFieldState:
  def apply[A](name: String, validators: FieldValidator[A]*)(using ft: FieldType[A]): FormFieldState[A] =
    new FormFieldState[A](name, Var(ft.serialize(ft.defaultValue)), ft, validators.toList)
