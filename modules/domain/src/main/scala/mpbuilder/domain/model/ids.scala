package mpbuilder.domain.model

import zio.prelude.*

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

opaque type BasketId = String
object BasketId:
  def apply(value: String): Validation[String, BasketId] =
    if value.nonEmpty then Validation.succeed(value)
    else Validation.fail("BasketId must not be empty")

  def unsafe(value: String): BasketId = value

  extension (id: BasketId) def value: String = id
