package mpbuilder.shared


/** Cross-context opaque identifiers and component keys.
  *
  * Only IDs referenced across bounded-context boundaries live in `shared`.
  * Context-local IDs (e.g. `RuleId`, `PriceEntryId`, `StepId`, `PresetId`)
  * stay inside their owning context's root package.
  */
private object IdValidation:
  inline def nonEmpty(name: String, raw: String): Validation[String, String] =
    val trimmed = raw.trim
    if trimmed.nonEmpty then Validation.succeed(trimmed)
    else Validation.fail(s"$name must not be empty")

opaque type CategoryId = String
object CategoryId:
  def apply(value: String): Validation[String, CategoryId] = IdValidation.nonEmpty("CategoryId", value)
  def unsafe(value: String): CategoryId = value
  extension (id: CategoryId) def value: String = id

opaque type MaterialId = String
object MaterialId:
  def apply(value: String): Validation[String, MaterialId] = IdValidation.nonEmpty("MaterialId", value)
  def unsafe(value: String): MaterialId = value
  extension (id: MaterialId) def value: String = id

opaque type FinishId = String
object FinishId:
  def apply(value: String): Validation[String, FinishId] = IdValidation.nonEmpty("FinishId", value)
  def unsafe(value: String): FinishId = value
  extension (id: FinishId) def value: String = id

opaque type PrintingMethodId = String
object PrintingMethodId:
  def apply(value: String): Validation[String, PrintingMethodId] = IdValidation.nonEmpty("PrintingMethodId", value)
  def unsafe(value: String): PrintingMethodId = value
  extension (id: PrintingMethodId) def value: String = id

opaque type StockFormatId = String
object StockFormatId:
  def apply(value: String): Validation[String, StockFormatId] = IdValidation.nonEmpty("StockFormatId", value)
  def unsafe(value: String): StockFormatId = value
  extension (id: StockFormatId) def value: String = id

opaque type PricelistId = String
object PricelistId:
  def apply(value: String): Validation[String, PricelistId] = IdValidation.nonEmpty("PricelistId", value)
  def unsafe(value: String): PricelistId = value
  extension (id: PricelistId) def value: String = id

opaque type QuoteId = String
object QuoteId:
  def apply(value: String): Validation[String, QuoteId] = IdValidation.nonEmpty("QuoteId", value)
  def unsafe(value: String): QuoteId = value
  extension (id: QuoteId) def value: String = id

opaque type CustomerId = String
object CustomerId:
  def apply(value: String): Validation[String, CustomerId] = IdValidation.nonEmpty("CustomerId", value)
  def unsafe(value: String): CustomerId = value
  extension (id: CustomerId) def value: String = id

opaque type CartId = String
object CartId:
  def apply(value: String): Validation[String, CartId] = IdValidation.nonEmpty("CartId", value)
  def unsafe(value: String): CartId = value
  extension (id: CartId) def value: String = id

opaque type OrderId = String
object OrderId:
  def apply(value: String): Validation[String, OrderId] = IdValidation.nonEmpty("OrderId", value)
  def unsafe(value: String): OrderId = value
  extension (id: OrderId) def value: String = id

opaque type ArtworkId = String
object ArtworkId:
  def apply(value: String): Validation[String, ArtworkId] = IdValidation.nonEmpty("ArtworkId", value)
  def unsafe(value: String): ArtworkId = value
  extension (id: ArtworkId) def value: String = id

opaque type DiscountCodeId = String
object DiscountCodeId:
  def apply(value: String): Validation[String, DiscountCodeId] = IdValidation.nonEmpty("DiscountCodeId", value)
  def unsafe(value: String): DiscountCodeId = value
  extension (id: DiscountCodeId) def value: String = id

opaque type JobId = String
object JobId:
  def apply(value: String): Validation[String, JobId] = IdValidation.nonEmpty("JobId", value)
  def unsafe(value: String): JobId = value
  extension (id: JobId) def value: String = id

opaque type EmployeeId = String
object EmployeeId:
  def apply(value: String): Validation[String, EmployeeId] = IdValidation.nonEmpty("EmployeeId", value)
  def unsafe(value: String): EmployeeId = value
  extension (id: EmployeeId) def value: String = id

opaque type MachineId = String
object MachineId:
  def apply(value: String): Validation[String, MachineId] = IdValidation.nonEmpty("MachineId", value)
  def unsafe(value: String): MachineId = value
  extension (id: MachineId) def value: String = id

/** Extensible key identifying a physical part of a product (e.g. cover, body, stand, pocket, lid).
  *
  * Replaces the closed 4-case `ComponentRole` enum so complex multi-part products
  * (folders with inserts, rigid boxes with base+lid+sleeve, multi-zone apparel) can be
  * modeled without modifying core enums.
  */
opaque type ComponentKey = String
object ComponentKey:
  def apply(value: String): Validation[String, ComponentKey] = IdValidation.nonEmpty("ComponentKey", value)
  def unsafe(value: String): ComponentKey = value

  val Main: ComponentKey       = "main"
  val Cover: ComponentKey      = "cover"
  val Body: ComponentKey       = "body"
  val Stand: ComponentKey      = "stand"
  val Insert: ComponentKey     = "insert"
  val BoxBase: ComponentKey    = "box-base"
  val BoxLid: ComponentKey     = "box-lid"
  val FrontPrint: ComponentKey = "front-print"
  val BackPrint: ComponentKey  = "back-print"

  extension (key: ComponentKey) def value: String = key
