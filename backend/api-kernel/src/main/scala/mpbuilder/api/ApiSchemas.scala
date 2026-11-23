package mpbuilder.api

import mpbuilder.artwork.*
import mpbuilder.catalog.*
import mpbuilder.identity.*
import mpbuilder.ordering.*
import mpbuilder.pricing.*
import mpbuilder.shared.*
import sttp.tapir.{Schema, SchemaType}
import scala.reflect.ClassTag

/** Tapir schemas (OpenAPI documentation) for domain types.
  *
  * Opaque types are documented as their wire representation; sealed ADTs use the same `type`
  * discriminator as the jsoniter codecs. Everything else is derived via `sttp.tapir.generic.auto`.
  */
object ApiSchemas:

  private def str[A](description: String): Schema[A]  = Schema.string[A].description(description)
  private def dec[A](description: String): Schema[A]  = Schema.schemaForBigDecimal.as[A].description(description)
  private def int[A](description: String): Schema[A]  = Schema.schemaForInt.as[A].description(description)

  // ── Opaque identifiers & values ────────────────────────────────────────────
  given Schema[CategoryId]       = str("Category identifier")
  given Schema[MaterialId]       = str("Material identifier")
  given Schema[FinishId]         = str("Finish identifier")
  given Schema[PrintingMethodId] = str("Printing method identifier")
  given Schema[StockFormatId]    = str("Stock format identifier")
  given Schema[PricelistId]      = str("Pricelist identifier")
  given Schema[QuoteId]          = str("Quote identifier")
  given Schema[CustomerId]       = str("Customer identifier")
  given Schema[CartId]           = str("Cart identifier")
  given Schema[OrderId]          = str("Order identifier")
  given Schema[ArtworkId]        = str("Artwork identifier")
  given Schema[DiscountCodeId]   = str("Discount code identifier")
  given Schema[JobId]            = str("Production job identifier")
  given Schema[EmployeeId]       = str("Employee identifier")
  given Schema[MachineId]        = str("Machine identifier")
  given Schema[ComponentKey]     = str("Product component key")
  given Schema[RuleId]           = str("Compatibility rule identifier")
  given Schema[PresetId]         = str("Category preset identifier")
  given Schema[ConfigurationId]  = str("Configuration identifier")
  given Schema[PriceEntryId]     = str("Price entry identifier")
  given Schema[SessionToken]     = str("Opaque session token")
  given Schema[OtpChallengeId]   = str("OTP challenge identifier")
  given Schema[OtpCode]          = str("6-digit one-time code")
  given Schema[OrderLineId]      = str("Order line identifier")
  given Schema[OrderNumber]      = str("Human-friendly order number")
  given Schema[DeliveryOptionId] = str("Delivery option identifier")
  given Schema[mpbuilder.manufacturing.WorkflowId] = str("Production workflow identifier")
  given Schema[mpbuilder.manufacturing.StepId]     = str("Production step identifier")
  given Schema[NonEmptyString]   = str("Non-empty text")
  given Schema[EmailAddress]     = str("E-mail address").format("email")
  given Schema[Version]          = str("Version label")

  given Schema[Money]         = dec("Monetary amount (decimal, HALF_UP to 2 places)")
  given Schema[Percentage]    = dec("Percentage 0–100")
  given Schema[TaxRate]       = dec("Tax rate as a fraction (0.21 = 21 %)")
  given Schema[PositiveMm]    = dec("Length in millimetres (> 0)")
  given Schema[NonNegativeMm] = dec("Length in millimetres (≥ 0)")
  given Schema[SquareMeters]  = dec("Area in m²")
  given Schema[Grams]         = dec("Weight in grams")
  given Schema[PaperWeight]   = int("Paper grammage (g/m²)")
  given Schema[Quantity]      = int("Positive quantity")
  given Schema[VersionCount]  = int("Number of versions")

  given Schema[LocalizedString] = named[LocalizedString]("LocalizedString", "Translations keyed by language: {\"En\": \"…\", \"Cs\": \"…\"}")

  // ── Named body schemas ─────────────────────────────────────────────────────
  //
  // Request/response bodies are documented by their domain type name (`type: object`)
  // instead of fully-derived field schemas: fully deriving the catalog/pricing/order trees
  // with Magnolia exceeds JVM class-size limits and would bloat the Scala.js client bundle.
  // The exact wire format is defined by the jsoniter-scala codecs in `backend/json`.

  def named[T](name: String, description: String = ""): Schema[T] =
    val s = Schema[T](SchemaType.SProduct[T](Nil), Some(Schema.SName(s"mpbuilder.$name")))
    if description.isEmpty then s.description(s"Domain model `$name` (see backend/json codecs)") else s.description(description)

  def namedOf[T](using ct: ClassTag[T]): Schema[T] = named[T](ct.runtimeClass.getSimpleName.stripSuffix("$"))

  def listOf[T](using ct: ClassTag[T]): Schema[List[T]] = Schema.schemaForIterable[T, List](using namedOf[T])
