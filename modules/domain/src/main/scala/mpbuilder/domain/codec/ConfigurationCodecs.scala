package mpbuilder.domain.codec

import mpbuilder.commons.*

import zio.json.*
import mpbuilder.domain.model.*

/** JSON codecs for a fully resolved [[ProductConfiguration]].
  *
  * These exist separately from [[DomainCodecs]] — which serializes the *catalog* (the set of things
  * that can be built) — because this is a different kind of document with a much longer life. An
  * order line stores the configuration a customer actually bought, and must keep meaning the same
  * thing years later, after the catalog has been edited underneath it.
  *
  * That is why a configuration snapshot embeds its resolved `Material`, `Finish`, `ProductCategory`
  * and `PrintingMethod` in full rather than referencing them by id. Re-resolving ids against a live
  * catalog would let a price change or a renamed finish silently rewrite history.
  *
  * Usage:
  * {{{
  * import mpbuilder.domain.codec.ConfigurationCodecs.given
  *
  * val json   = configuration.toJson
  * val parsed = json.fromJson[ProductConfiguration]
  * }}}
  */
object ConfigurationCodecs:

  // Everything below builds on the catalog codecs (Material, Finish, ProductCategory,
  // PrintingMethod, SpecValue, InkConfiguration, FinishParameters, …).
  export DomainCodecs.given

  // ── Component ────────────────────────────────────────────────────────────

  given JsonCodec[SelectedFinish] = DeriveJsonCodec.gen[SelectedFinish]
  given JsonCodec[ProductComponent] = DeriveJsonCodec.gen[ProductComponent]

  // ── Specifications ───────────────────────────────────────────────────────

  // `ProductSpecifications` wraps a Map keyed by an enum. zio-json needs the key rendered as a
  // JSON field name, so SpecKind gets field codecs rather than the usual value codecs.
  given JsonFieldEncoder[SpecKind] = JsonFieldEncoder[String].contramap(_.toString)
  given JsonFieldDecoder[SpecKind] = JsonFieldDecoder[String].mapOrFail { raw =>
    SpecKind.values.find(_.toString == raw).toRight(s"Unknown SpecKind: $raw")
  }

  given JsonCodec[ProductSpecifications] = DeriveJsonCodec.gen[ProductSpecifications]

  // ── Configuration ────────────────────────────────────────────────────────

  given JsonCodec[ProductConfiguration] = DeriveJsonCodec.gen[ProductConfiguration]
