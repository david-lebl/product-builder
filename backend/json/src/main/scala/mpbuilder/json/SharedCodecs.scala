package mpbuilder.json

import kyo.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.shared.*

/** Schemas for `backend/shared` opaque types, value objects and enums. */
object SharedCodecs:

  // ── Identifiers ────────────────────────────────────────────────────────────
  given Schema[CategoryId]       = stringOpaque("CategoryId")(CategoryId(_))(_.value)
  given Schema[MaterialId]       = stringOpaque("MaterialId")(MaterialId(_))(_.value)
  given Schema[FinishId]         = stringOpaque("FinishId")(FinishId(_))(_.value)
  given Schema[PrintingMethodId] = stringOpaque("PrintingMethodId")(PrintingMethodId(_))(_.value)
  given Schema[StockFormatId]    = stringOpaque("StockFormatId")(StockFormatId(_))(_.value)
  given Schema[PricelistId]      = stringOpaque("PricelistId")(PricelistId(_))(_.value)
  given Schema[QuoteId]          = stringOpaque("QuoteId")(QuoteId(_))(_.value)
  given Schema[CustomerId]       = stringOpaque("CustomerId")(CustomerId(_))(_.value)
  given Schema[CartId]           = stringOpaque("CartId")(CartId(_))(_.value)
  given Schema[OrderId]          = stringOpaque("OrderId")(OrderId(_))(_.value)
  given Schema[ArtworkId]        = stringOpaque("ArtworkId")(ArtworkId(_))(_.value)
  given Schema[DiscountCodeId]   = stringOpaque("DiscountCodeId")(DiscountCodeId(_))(_.value)
  given Schema[JobId]            = stringOpaque("JobId")(JobId(_))(_.value)
  given Schema[EmployeeId]       = stringOpaque("EmployeeId")(EmployeeId(_))(_.value)
  given Schema[MachineId]        = stringOpaque("MachineId")(MachineId(_))(_.value)
  given Schema[ComponentKey]     = stringOpaque("ComponentKey")(ComponentKey(_))(_.value)

  // ── Strings & text ─────────────────────────────────────────────────────────
  given Schema[NonEmptyString] = stringOpaque("NonEmptyString")(NonEmptyString(_))(_.value)
  given Schema[EmailAddress]   = stringOpaque("EmailAddress")(EmailAddress(_))(_.value)
  given Schema[Version]        = stringOpaque("Version")(Version(_))(_.value)

  given Schema[Language] = Schema.derived

  /** `LocalizedString` as `{"En": "...", "Cs": "..."}`. */
  given Schema[LocalizedString] =
    validated("LocalizedString", languageMap[String])(LocalizedString.fromMap)(_.toMap)

  // ── Money & rates ──────────────────────────────────────────────────────────
  given Schema[Money]      = Schema.bigDecimalSchema.transform(Money(_))(_.value)
  given Schema[Percentage] = bigDecimalOpaque("Percentage")(Percentage(_))(_.value)
  given Schema[TaxRate]    = bigDecimalOpaque("TaxRate")(TaxRate(_))(_.value)
  given Schema[Currency]   = Schema.derived

  // ── Measurements ───────────────────────────────────────────────────────────
  given Schema[PositiveMm]    = bigDecimalOpaque("PositiveMm")(PositiveMm(_))(_.value)
  given Schema[NonNegativeMm] = bigDecimalOpaque("NonNegativeMm")(NonNegativeMm(_))(_.value)
  given Schema[SquareMeters]  = bigDecimalOpaque("SquareMeters")(SquareMeters(_))(_.value)
  given Schema[Grams]         = bigDecimalOpaque("Grams")(Grams(_))(_.value)
  given Schema[PaperWeight]   = intOpaque("PaperWeight")(PaperWeight(_))(_.gsm)
  given Schema[Quantity]      = intOpaque("Quantity")(Quantity(_))(_.value)
  given Schema[VersionCount]  = intOpaque("VersionCount")(VersionCount(_))(_.value)

  // ── Shared enums ───────────────────────────────────────────────────────────
  given Schema[ManufacturingSpeed] = Schema.derived
  given Schema[CustomerTier]       = Schema.derived
  given Schema[CustomerSegment]    = Schema.derived

  given Schema[Address]     = Schema.derived
  given Schema[Dimension2D] = Schema.derived

  // ── Maps keyed by identifiers / enums (JSON objects rather than key-value arrays) ──
  given categoryIdMap[V: Schema]: Schema[Map[CategoryId, V]] = keyedMap("CategoryId")(CategoryId(_))(_.value)
  given materialIdMap[V: Schema]: Schema[Map[MaterialId, V]] = keyedMap("MaterialId")(MaterialId(_))(_.value)
  given finishIdMap[V: Schema]: Schema[Map[FinishId, V]]     = keyedMap("FinishId")(FinishId(_))(_.value)
  given printingMethodIdMap[V: Schema]: Schema[Map[PrintingMethodId, V]] =
    keyedMap("PrintingMethodId")(PrintingMethodId(_))(_.value)
  given componentKeyMap[V: Schema]: Schema[Map[ComponentKey, V]] = keyedMap("ComponentKey")(ComponentKey(_))(_.value)
  given customerIdMap[V: Schema]: Schema[Map[CustomerId, V]]     = keyedMap("CustomerId")(CustomerId(_))(_.value)
  given languageMap[V: Schema]: Schema[Map[Language, V]]         = keyedMap("Language")(enumKey("Language", Language.values))(_.toString)
  given currencyMap[V: Schema]: Schema[Map[Currency, V]]         = keyedMap("Currency")(enumKey("Currency", Currency.values))(_.toString)
  given speedMap[V: Schema]: Schema[Map[ManufacturingSpeed, V]] =
    keyedMap("ManufacturingSpeed")(enumKey("ManufacturingSpeed", ManufacturingSpeed.values))(_.toString)
  given tierMap[V: Schema]: Schema[Map[CustomerTier, V]] =
    keyedMap("CustomerTier")(enumKey("CustomerTier", CustomerTier.values))(_.toString)
