package mpbuilder.json

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.shared.*
import zio.prelude.Validation

/** Codecs for `backend/shared` opaque types, value objects and enums. */
object SharedCodecs:

  // ── Identifiers ────────────────────────────────────────────────────────────
  given JsonValueCodec[CategoryId]       = stringCodec("CategoryId")(CategoryId(_))(_.value)
  given JsonValueCodec[MaterialId]       = stringCodec("MaterialId")(MaterialId(_))(_.value)
  given JsonValueCodec[FinishId]         = stringCodec("FinishId")(FinishId(_))(_.value)
  given JsonValueCodec[PrintingMethodId] = stringCodec("PrintingMethodId")(PrintingMethodId(_))(_.value)
  given JsonValueCodec[StockFormatId]    = stringCodec("StockFormatId")(StockFormatId(_))(_.value)
  given JsonValueCodec[PricelistId]      = stringCodec("PricelistId")(PricelistId(_))(_.value)
  given JsonValueCodec[QuoteId]          = stringCodec("QuoteId")(QuoteId(_))(_.value)
  given JsonValueCodec[CustomerId]       = stringCodec("CustomerId")(CustomerId(_))(_.value)
  given JsonValueCodec[CartId]           = stringCodec("CartId")(CartId(_))(_.value)
  given JsonValueCodec[OrderId]          = stringCodec("OrderId")(OrderId(_))(_.value)
  given JsonValueCodec[ArtworkId]        = stringCodec("ArtworkId")(ArtworkId(_))(_.value)
  given JsonValueCodec[DiscountCodeId]   = stringCodec("DiscountCodeId")(DiscountCodeId(_))(_.value)
  given JsonValueCodec[JobId]            = stringCodec("JobId")(JobId(_))(_.value)
  given JsonValueCodec[EmployeeId]       = stringCodec("EmployeeId")(EmployeeId(_))(_.value)
  given JsonValueCodec[MachineId]        = stringCodec("MachineId")(MachineId(_))(_.value)
  given JsonValueCodec[ComponentKey]     = stringCodec("ComponentKey")(ComponentKey(_))(_.value)

  given JsonKeyCodec[CategoryId]       = stringKeyCodec("CategoryId")(CategoryId(_))(_.value)
  given JsonKeyCodec[MaterialId]       = stringKeyCodec("MaterialId")(MaterialId(_))(_.value)
  given JsonKeyCodec[FinishId]         = stringKeyCodec("FinishId")(FinishId(_))(_.value)
  given JsonKeyCodec[PrintingMethodId] = stringKeyCodec("PrintingMethodId")(PrintingMethodId(_))(_.value)
  given JsonKeyCodec[ComponentKey]     = stringKeyCodec("ComponentKey")(ComponentKey(_))(_.value)
  given JsonKeyCodec[CustomerId]       = stringKeyCodec("CustomerId")(CustomerId(_))(_.value)

  // ── Strings & text ─────────────────────────────────────────────────────────
  given JsonValueCodec[NonEmptyString] = stringCodec("NonEmptyString")(NonEmptyString(_))(_.value)
  given JsonValueCodec[EmailAddress]   = stringCodec("EmailAddress")(EmailAddress(_))(_.value)
  given JsonValueCodec[Version]        = stringCodec("Version")(Version(_))(_.value)

  given JsonValueCodec[Language] = JsonCodecMaker.make(config)
  given JsonKeyCodec[Language]   = enumKeyCodec("Language", Language.values)

  /** `LocalizedString` as `{"En": "...", "Cs": "..."}`. */
  given JsonValueCodec[LocalizedString] =
    val mapCodec: JsonValueCodec[Map[Language, String]] = JsonCodecMaker.make(config)
    new JsonValueCodec[LocalizedString]:
      override def decodeValue(in: JsonReader, default: LocalizedString): LocalizedString =
        LocalizedString.fromMap(mapCodec.decodeValue(in, Map.empty)).toEither match
          case Right(ls)  => ls
          case Left(errs) => in.decodeError(s"Invalid LocalizedString: ${errs.mkString("; ")}")
      override def encodeValue(x: LocalizedString, out: JsonWriter): Unit = mapCodec.encodeValue(x.toMap, out)
      override def nullValue: LocalizedString                             = null.asInstanceOf[LocalizedString]

  // ── Money & rates ──────────────────────────────────────────────────────────
  given JsonValueCodec[Money]      = bigDecimalCodec("Money")(v => Validation.succeed(Money(v)))(_.value)
  given JsonValueCodec[Percentage] = bigDecimalCodec("Percentage")(Percentage(_))(_.value)
  given JsonValueCodec[TaxRate]    = bigDecimalCodec("TaxRate")(TaxRate(_))(_.value)

  given JsonValueCodec[Currency] = JsonCodecMaker.make(config)
  given JsonKeyCodec[Currency]   = enumKeyCodec("Currency", Currency.values)

  // ── Measurements ───────────────────────────────────────────────────────────
  given JsonValueCodec[PositiveMm]    = bigDecimalCodec("PositiveMm")(PositiveMm(_))(_.value)
  given JsonValueCodec[NonNegativeMm] = bigDecimalCodec("NonNegativeMm")(NonNegativeMm(_))(_.value)
  given JsonValueCodec[SquareMeters]  = bigDecimalCodec("SquareMeters")(SquareMeters(_))(_.value)
  given JsonValueCodec[Grams]         = bigDecimalCodec("Grams")(Grams(_))(_.value)
  given JsonValueCodec[PaperWeight]   = intCodec("PaperWeight")(PaperWeight(_))(_.gsm)
  given JsonValueCodec[Quantity]      = intCodec("Quantity")(Quantity(_))(_.value)
  given JsonValueCodec[VersionCount]  = intCodec("VersionCount")(VersionCount(_))(_.value)

  // ── Shared enums ───────────────────────────────────────────────────────────
  given JsonValueCodec[ManufacturingSpeed] = JsonCodecMaker.make(config)
  given JsonKeyCodec[ManufacturingSpeed]   = enumKeyCodec("ManufacturingSpeed", ManufacturingSpeed.values)
  given JsonValueCodec[CustomerTier]       = JsonCodecMaker.make(config)
  given JsonKeyCodec[CustomerTier]         = enumKeyCodec("CustomerTier", CustomerTier.values)
  given JsonValueCodec[CustomerSegment]    = JsonCodecMaker.make(config)

  given JsonValueCodec[Address]     = JsonCodecMaker.make(config)
  given JsonValueCodec[Dimension2D] = JsonCodecMaker.make(config)
