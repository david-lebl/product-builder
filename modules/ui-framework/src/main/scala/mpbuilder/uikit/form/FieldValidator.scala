package mpbuilder.uikit.form

trait FieldValidator[A]:
  def validate(value: A): List[String]

object FieldValidator:
  def required: FieldValidator[String] = v =>
    if v.trim.isEmpty then List("This field is required") else Nil

  def minLength(n: Int): FieldValidator[String] = v =>
    if v.length < n then List(s"Must be at least $n characters") else Nil

  def maxLength(n: Int): FieldValidator[String] = v =>
    if v.length > n then List(s"Must be at most $n characters") else Nil

  def min(n: Int): FieldValidator[Int] = v =>
    if v < n then List(s"Must be at least $n") else Nil

  def max(n: Int): FieldValidator[Int] = v =>
    if v > n then List(s"Must be at most $n") else Nil

  def regex(pattern: String, msg: String): FieldValidator[String] = v =>
    if v.matches(pattern) then Nil else List(msg)

  def all[A](vs: FieldValidator[A]*): FieldValidator[A] = v =>
    vs.flatMap(_.validate(v)).toList
