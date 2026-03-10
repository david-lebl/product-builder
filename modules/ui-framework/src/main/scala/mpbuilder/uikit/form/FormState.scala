package mpbuilder.uikit.form

import com.raquo.laminar.api.L.*
import scala.deriving.Mirror
import scala.compiletime.*

class FormState[T](
  val fields: Map[String, FormFieldState[?]],
  val fieldOrder: List[String],
  private val construct: Map[String, Any] => T,
):
  def field(name: String): FormFieldState[?] =
    fields(name)

  def fieldAs[A](name: String): FormFieldState[A] =
    fields(name).asInstanceOf[FormFieldState[A]]

  /** Returns a new FormState with the given validators attached to the named field. */
  def withValidators[A](fieldName: String, validators: FieldValidator[A]*): FormState[T] =
    val existing = fieldAs[A](fieldName)
    val updated = new FormFieldState[A](
      existing.name,
      existing.rawVar,
      existing.fieldType,
      existing.validators ++ validators.toList,
    )
    new FormState[T](fields.updated(fieldName, updated), fieldOrder, construct)

  /** Marks all fields as touched, causing all validation errors to become visible. */
  def touchAll(): Unit =
    fields.values.foreach(_.asInstanceOf[FormFieldState[Any]].touch())

  val validated: Signal[Either[Map[String, List[String]], T]] =
    val fieldSignals: List[Signal[(String, Either[List[String], Any])]] =
      fieldOrder.map { name =>
        fields(name).parsed.map(name -> _)
      }
    Signal.sequence(fieldSignals).map { results =>
      val errors = results.collect { case (name, Left(errs)) => name -> errs }.toMap
      if errors.nonEmpty then Left(errors)
      else
        val values = results.collect { case (name, Right(v)) => name -> v }.toMap
        Right(construct(values))
    }

  /** All current validation error messages across all fields (flattened). */
  val allErrors: Signal[List[String]] =
    validated.map {
      case Right(_)     => Nil
      case Left(errors) => errors.values.flatten.toList
    }

object FormState:
  inline def create[T <: Product](using m: Mirror.ProductOf[T]): FormState[T] =
    val names = constValueTuple[m.MirroredElemLabels].toList.asInstanceOf[List[String]]
    val fieldTypes = summonFieldTypes[m.MirroredElemTypes]
    val fieldStates = names.zip(fieldTypes).map { case (name, ft) =>
      name -> createFieldState(name, ft)
    }
    val fieldMap = fieldStates.toMap

    val construct: Map[String, Any] => T = values =>
      val tuple = Tuple.fromArray(names.map(values(_)).toArray)
      m.fromTuple(tuple.asInstanceOf[m.MirroredElemTypes])

    new FormState[T](fieldMap, names, construct)

  private def createFieldState(name: String, ft: FieldType[?]): FormFieldState[?] =
    new FormFieldState[Any](
      name,
      Var(ft.asInstanceOf[FieldType[Any]].serialize(ft.defaultValue.asInstanceOf[Any])),
      ft.asInstanceOf[FieldType[Any]],
      Nil,
    )

  private inline def summonFieldTypes[T <: Tuple]: List[FieldType[?]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (h *: t)   => summonInline[FieldType[h]] :: summonFieldTypes[t]
