package mpbuilder.domain

import zio.json.*

/** Stable catalog identifiers. Opaque strings so they cost nothing on Scala.js
  * and serialize as plain JSON strings.
  */
object ids:

  opaque type CategoryId = String
  object CategoryId:
    def apply(value: String): CategoryId = value
    given JsonCodec[CategoryId] = JsonCodec.string
    given JsonFieldEncoder[CategoryId] = JsonFieldEncoder.string
    given JsonFieldDecoder[CategoryId] = JsonFieldDecoder.string

  opaque type MaterialId = String
  object MaterialId:
    def apply(value: String): MaterialId = value
    given JsonCodec[MaterialId] = JsonCodec.string
    given JsonFieldEncoder[MaterialId] = JsonFieldEncoder.string
    given JsonFieldDecoder[MaterialId] = JsonFieldDecoder.string

  opaque type FinishId = String
  object FinishId:
    def apply(value: String): FinishId = value
    given JsonCodec[FinishId] = JsonCodec.string
    given JsonFieldEncoder[FinishId] = JsonFieldEncoder.string
    given JsonFieldDecoder[FinishId] = JsonFieldDecoder.string

  opaque type PrintingMethodId = String
  object PrintingMethodId:
    def apply(value: String): PrintingMethodId = value
    given JsonCodec[PrintingMethodId] = JsonCodec.string
    given JsonFieldEncoder[PrintingMethodId] = JsonFieldEncoder.string
    given JsonFieldDecoder[PrintingMethodId] = JsonFieldDecoder.string

  opaque type InkConfigId = String
  object InkConfigId:
    def apply(value: String): InkConfigId = value
    given JsonCodec[InkConfigId] = JsonCodec.string
    given JsonFieldEncoder[InkConfigId] = JsonFieldEncoder.string
    given JsonFieldDecoder[InkConfigId] = JsonFieldDecoder.string

  opaque type PresetId = String
  object PresetId:
    def apply(value: String): PresetId = value
    given JsonCodec[PresetId] = JsonCodec.string
    given JsonFieldEncoder[PresetId] = JsonFieldEncoder.string
    given JsonFieldDecoder[PresetId] = JsonFieldDecoder.string

  extension (id: CategoryId | MaterialId | FinishId | PrintingMethodId | InkConfigId | PresetId)
    def raw: String = id.asInstanceOf[String]
