package mpbuilder.uikit.form

enum InputKind:
  case Text(inputType: String)
  case Checkbox
  case Select(options: List[(String, String)])

trait FieldType[A]:
  def defaultValue: A
  def parse(raw: String): Either[String, A]
  def serialize(value: A): String
  def inputKind: InputKind

object FieldType:
  given FieldType[String] with
    def defaultValue: String = ""
    def parse(raw: String): Either[String, String] = Right(raw)
    def serialize(value: String): String = value
    def inputKind: InputKind = InputKind.Text("text")

  given FieldType[Int] with
    def defaultValue: Int = 0
    def parse(raw: String): Either[String, Int] =
      raw.toIntOption.toRight("Must be a whole number")
    def serialize(value: Int): String = value.toString
    def inputKind: InputKind = InputKind.Text("number")

  given FieldType[Double] with
    def defaultValue: Double = 0.0
    def parse(raw: String): Either[String, Double] =
      raw.toDoubleOption.toRight("Must be a number")
    def serialize(value: Double): String = value.toString
    def inputKind: InputKind = InputKind.Text("number")

  given FieldType[Boolean] with
    def defaultValue: Boolean = false
    def parse(raw: String): Either[String, Boolean] = Right(raw == "true")
    def serialize(value: Boolean): String = value.toString
    def inputKind: InputKind = InputKind.Checkbox

  given FieldType[Option[String]] with
    def defaultValue: Option[String] = None
    def parse(raw: String): Either[String, Option[String]] =
      Right(if raw.trim.isEmpty then None else Some(raw))
    def serialize(value: Option[String]): String = value.getOrElse("")
    def inputKind: InputKind = InputKind.Text("text")
