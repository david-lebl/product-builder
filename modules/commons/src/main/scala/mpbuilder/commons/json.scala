package mpbuilder.commons

import zio.json.*

/** JSON codecs for the kernel types.
  *
  * Here rather than in each context, because these types appear in every context's wire shapes and
  * six divergent encodings of `Money` would be a genuine interoperability bug — the sort that only
  * shows up once two services exchange a payload.
  *
  * `import mpbuilder.commons.json.given` wherever a DTO mentions one.
  */
object json:

  // Money is a BigDecimal, and is encoded as one: a JSON number, full precision, no rounding.
  // Encoding it as a float would silently lose money; encoding it as a string would make every
  // consumer parse it by hand.
  given JsonEncoder[Money] = JsonEncoder[BigDecimal].contramap(_.value)
  given JsonDecoder[Money] = JsonDecoder[BigDecimal].map(Money(_))

  given JsonEncoder[Currency] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[Currency] = JsonDecoder[String].mapOrFail { raw =>
    Currency.values.find(_.toString.equalsIgnoreCase(raw)).toRight(s"Unknown currency: $raw")
  }

  given JsonEncoder[Language] = JsonEncoder[String].contramap(_.toCode)
  given JsonDecoder[Language] = JsonDecoder[String].map(Language.fromCode)

  // As a plain object of language code -> text, so a client can pick its own language without
  // the server having to know which one it wants.
  given JsonEncoder[LocalizedString] =
    JsonEncoder[Map[String, String]].contramap(ls =>
      Language.values.map(l => l.toCode -> ls(l)).toMap
    )
  given JsonDecoder[LocalizedString] =
    JsonDecoder[Map[String, String]].map(m =>
      LocalizedString(m.flatMap((k, v) => Language.values.find(_.toCode == k).map(_ -> v)).toMap)
    )

  // Epoch milliseconds. Unambiguous across languages and time zones, unlike a formatted string.
  given JsonEncoder[Timestamp] = JsonEncoder[Long].contramap(_.epochMillis)
  given JsonDecoder[Timestamp] = JsonDecoder[Long].map(Timestamp(_))

  given JsonEncoder[Quantity] = JsonEncoder[Int].contramap(_.value)
  given JsonDecoder[Quantity] = JsonDecoder[Int].mapOrFail(Quantity(_).toEither.left.map(_.head))

  given JsonEncoder[Percentage] = JsonEncoder[BigDecimal].contramap(_.value)
  given JsonDecoder[Percentage] =
    JsonDecoder[BigDecimal].mapOrFail(Percentage(_).toEither.left.map(_.head))

  given JsonCodec[Dimension] = DeriveJsonCodec.gen[Dimension]
  given JsonCodec[Problem] = DeriveJsonCodec.gen[Problem]
