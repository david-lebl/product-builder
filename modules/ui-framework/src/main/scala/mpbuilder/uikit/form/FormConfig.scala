package mpbuilder.uikit.form

import com.raquo.laminar.api.L.*

case class FieldConfig(
  label: Option[Signal[String]] = None,
  placeholder: Option[Signal[String]] = None,
  inputType: Option[String] = None,
)
