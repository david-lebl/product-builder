package mpbuilder.kernel

import zio.json.*

/** JSON codecs for shared-kernel types (IDs, i18n, money).
  *
  * Usage:
  * {{{
  * import mpbuilder.kernel.KernelCodecs.given
  * }}}
  */
object KernelCodecs:

  // ── Opaque type IDs ──────────────────────────────────────────────────────

  given JsonEncoder[CategoryId] = JsonEncoder[String].contramap(_.value)
  given JsonDecoder[CategoryId] = JsonDecoder[String].map(CategoryId.unsafe)

  given JsonEncoder[MaterialId] = JsonEncoder[String].contramap(_.value)
  given JsonDecoder[MaterialId] = JsonDecoder[String].map(MaterialId.unsafe)

  given JsonEncoder[FinishId] = JsonEncoder[String].contramap(_.value)
  given JsonDecoder[FinishId] = JsonDecoder[String].map(FinishId.unsafe)

  given JsonEncoder[PrintingMethodId] = JsonEncoder[String].contramap(_.value)
  given JsonDecoder[PrintingMethodId] = JsonDecoder[String].map(PrintingMethodId.unsafe)

  given JsonEncoder[ConfigurationId] = JsonEncoder[String].contramap(_.value)
  given JsonDecoder[ConfigurationId] = JsonDecoder[String].map(ConfigurationId.unsafe)

  // ── Language & LocalizedString ───────────────────────────────────────────

  given JsonEncoder[Language] = JsonEncoder[String].contramap(_.toCode)
  given JsonDecoder[Language] = JsonDecoder[String].map(Language.fromCode)

  given JsonEncoder[LocalizedString] =
    JsonEncoder[Map[String, String]].contramap { ls =>
      Language.values.flatMap(l => Option(ls(l)).filter(_.nonEmpty).map(l.toCode -> _)).toMap
    }
  given JsonDecoder[LocalizedString] =
    JsonDecoder[Map[String, String]].map { m =>
      LocalizedString(m.map { case (k, v) => Language.fromCode(k) -> v })
    }

  // ── Money & Currency ─────────────────────────────────────────────────────

  given JsonEncoder[Money] = JsonEncoder[BigDecimal].contramap(_.value)
  given JsonDecoder[Money] = JsonDecoder[BigDecimal].map(Money.apply)

  given JsonEncoder[Currency] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[Currency] = JsonDecoder[String].map(Currency.valueOf)

  given JsonCodec[Price] = DeriveJsonCodec.gen[Price]
