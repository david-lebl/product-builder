package mpbuilder.uikit.form

import com.raquo.laminar.api.L.*

class FormFieldState[A](
  val name: String,
  val rawVar: Var[String],
  val fieldType: FieldType[A],
  val validators: List[FieldValidator[A]],
):
  val parsed: Signal[Either[List[String], A]] =
    rawVar.signal.map { raw =>
      if raw.isEmpty && fieldType.defaultValue == fieldType.parse("").getOrElse(fieldType.defaultValue) then
        Right(fieldType.defaultValue)
      else
        fieldType.parse(raw) match
          case Left(parseErr) => Left(List(parseErr))
          case Right(value) =>
            val errors = validators.flatMap(_.validate(value))
            if errors.nonEmpty then Left(errors) else Right(value)
    }

  val firstError: Signal[Option[String]] =
    parsed.map {
      case Left(errs) => errs.headOption
      case Right(_)   => None
    }

  val value: Signal[A] =
    parsed.map(_.getOrElse(fieldType.defaultValue))

object FormFieldState:
  def apply[A](name: String, validators: FieldValidator[A]*)(using ft: FieldType[A]): FormFieldState[A] =
    new FormFieldState[A](name, Var(ft.serialize(ft.defaultValue)), ft, validators.toList)
