package mpbuilder.product.model

import zio.prelude.Validation
import mpbuilder.commons.*

/** The ids catalog owns.
  *
  * They used to live in one file with ten ids spanning every context — order, customer, artwork,
  * discount — which meant every context compiled against every other context's identity types.
  * Splitting them by owner is what lets a context be extracted without dragging the rest along.
  */

opaque type CategoryId = String
object CategoryId:
  def apply(value: String): Validation[String, CategoryId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("CategoryId must not be empty")

  def unsafe(value: String): CategoryId = value

  extension (id: CategoryId) def value: String = id

opaque type MaterialId = String
object MaterialId:
  def apply(value: String): Validation[String, MaterialId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("MaterialId must not be empty")

  def unsafe(value: String): MaterialId = value

  extension (id: MaterialId) def value: String = id

opaque type FinishId = String
object FinishId:
  def apply(value: String): Validation[String, FinishId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("FinishId must not be empty")

  def unsafe(value: String): FinishId = value

  extension (id: FinishId) def value: String = id

opaque type PrintingMethodId = String
object PrintingMethodId:
  def apply(value: String): Validation[String, PrintingMethodId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("PrintingMethodId must not be empty")

  def unsafe(value: String): PrintingMethodId = value

  extension (id: PrintingMethodId) def value: String = id

opaque type ConfigurationId = String
object ConfigurationId:
  def apply(value: String): Validation[String, ConfigurationId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("ConfigurationId must not be empty")

  def unsafe(value: String): ConfigurationId = value

  extension (id: ConfigurationId) def value: String = id
