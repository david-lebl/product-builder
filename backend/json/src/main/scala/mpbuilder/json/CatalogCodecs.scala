package mpbuilder.json

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import mpbuilder.catalog.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.json.SharedCodecs.given
import zio.NonEmptyChunk

/** Codecs for the public `catalog` model (catalog, rules, configuration). */
object CatalogCodecs:

  given JsonValueCodec[RuleId]          = stringCodec("RuleId")(RuleId(_))(_.value)
  given JsonValueCodec[PresetId]        = stringCodec("PresetId")(PresetId(_))(_.value)
  given JsonValueCodec[ConfigurationId] = stringCodec("ConfigurationId")(ConfigurationId(_))(_.value)

  given JsonValueCodec[FinishType] = JsonCodecMaker.make(config)
  given JsonKeyCodec[FinishType]   = enumKeyCodec("FinishType", FinishType.values)

  given necMediaFormat: JsonValueCodec[NonEmptyChunk[MediaFormat]] =
    nonEmptyChunkCodec(JsonCodecMaker.make[List[MediaFormat]](config), "MediaFormat list")
  given necComponentTemplate: JsonValueCodec[NonEmptyChunk[ComponentTemplate]] =
    nonEmptyChunkCodec(JsonCodecMaker.make[List[ComponentTemplate]](config), "ComponentTemplate list")
  given necComponentPreset: JsonValueCodec[NonEmptyChunk[ComponentPreset]] =
    nonEmptyChunkCodec(JsonCodecMaker.make[List[ComponentPreset]](config), "ComponentPreset list")
  given necComponentSelection: JsonValueCodec[NonEmptyChunk[ComponentSelection]] =
    nonEmptyChunkCodec(JsonCodecMaker.make[List[ComponentSelection]](config), "ComponentSelection list")
  given necConfiguredComponent: JsonValueCodec[NonEmptyChunk[ConfiguredComponent]] =
    nonEmptyChunkCodec(JsonCodecMaker.make[List[ConfiguredComponent]](config), "ConfiguredComponent list")
  given necComponentWeight: JsonValueCodec[NonEmptyChunk[ComponentWeight]] =
    nonEmptyChunkCodec(JsonCodecMaker.make[List[ComponentWeight]](config), "ComponentWeight list")
  given necCatalogError: JsonValueCodec[NonEmptyChunk[CatalogError]] =
    nonEmptyChunkCodec(JsonCodecMaker.make[List[CatalogError]](config), "CatalogError list")

  given JsonValueCodec[Material]             = JsonCodecMaker.make(config)
  given JsonValueCodec[Finish]               = JsonCodecMaker.make(config)
  given JsonValueCodec[PrintingMethod]       = JsonCodecMaker.make(config)
  given JsonValueCodec[ProductSpecs]         = JsonCodecMaker.make(config)
  given JsonValueCodec[ProductCategory]      = JsonCodecMaker.make(config)
  given JsonValueCodec[ShowcaseListing]      = JsonCodecMaker.make(config)
  given JsonValueCodec[ProductCatalog]       = JsonCodecMaker.make(config)
  given JsonValueCodec[CompatibilityRule]    = JsonCodecMaker.make(config)
  given JsonValueCodec[CompatibilityRuleset] = JsonCodecMaker.make(config)
  given JsonValueCodec[ConfigureProductCmd]  = JsonCodecMaker.make(config)
  given JsonValueCodec[ConfiguredProduct]    = JsonCodecMaker.make(config)
  given JsonValueCodec[ConfigurationResult]  = JsonCodecMaker.make(config)
  given JsonValueCodec[CatalogError]         = JsonCodecMaker.make(config)
