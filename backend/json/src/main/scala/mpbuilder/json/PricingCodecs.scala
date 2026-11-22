package mpbuilder.json

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.json.SharedCodecs.given
import mpbuilder.pricing.*
import zio.NonEmptyChunk

/** Codecs for the public `pricing` model (pricelists, agreements, promos, cost sheets, quotes). */
object PricingCodecs:

  given JsonValueCodec[PriceEntryId] = stringCodec("PriceEntryId")(PriceEntryId(_))(_.value)

  given necAreaTier: JsonValueCodec[NonEmptyChunk[AreaTier]] =
    nonEmptyChunkCodec(JsonCodecMaker.make[List[AreaTier]](config), "AreaTier list")
  given necVolumeTier: JsonValueCodec[NonEmptyChunk[VolumeTier]] =
    nonEmptyChunkCodec(JsonCodecMaker.make[List[VolumeTier]](config), "VolumeTier list")
  given necPricingComponent: JsonValueCodec[NonEmptyChunk[PricingComponent]] =
    nonEmptyChunkCodec(JsonCodecMaker.make[List[PricingComponent]](config), "PricingComponent list")
  given necComponentQuote: JsonValueCodec[NonEmptyChunk[ComponentQuote]] =
    nonEmptyChunkCodec(JsonCodecMaker.make[List[ComponentQuote]](config), "ComponentQuote list")
  given necPricingError: JsonValueCodec[NonEmptyChunk[PricingError]] =
    nonEmptyChunkCodec(JsonCodecMaker.make[List[PricingError]](config), "PricingError list")

  given JsonValueCodec[Pricelist]              = JsonCodecMaker.make(config)
  given JsonValueCodec[AgreementTarget]        = JsonCodecMaker.make(config)
  given JsonValueCodec[CustomerPriceAgreement] = JsonCodecMaker.make(config)
  given JsonValueCodec[PromoCode]              = JsonCodecMaker.make(config)
  given JsonValueCodec[ProductionCostSheet]    = JsonCodecMaker.make(config)
  given JsonValueCodec[DynamicPricingContext]  = JsonCodecMaker.make(config)
  given JsonValueCodec[PricingSubject]         = JsonCodecMaker.make(config)
  given JsonValueCodec[PriceQuote]             = JsonCodecMaker.make(config)
  given JsonValueCodec[MarginAnalysis]         = JsonCodecMaker.make(config)
  given JsonValueCodec[PricingError]           = JsonCodecMaker.make(config)
