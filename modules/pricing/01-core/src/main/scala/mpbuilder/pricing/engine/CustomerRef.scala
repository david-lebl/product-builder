package mpbuilder.pricing.engine

import mpbuilder.commons.*
import zio.prelude.Validation

/** How pricing refers to a customer.
  *
  * Its own type, not the customers context's `CustomerRef`. Pricing needs to key a negotiated rate
  * or a restricted discount code to somebody, and that is all — it has no business compiling
  * against another context's identity type, which is exactly the coupling that kept the engine in
  * the legacy module.
  *
  * Customer *type* is carried as a plain string for the same reason: "who may use this code" is a
  * label pricing checks, not a concept it owns.
  */
opaque type CustomerRef = String
object CustomerRef:
  def apply(value: String): Validation[String, CustomerRef] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("CustomerRef must not be empty")

  def unsafe(value: String): CustomerRef = value

  extension (ref: CustomerRef) def value: String = ref
