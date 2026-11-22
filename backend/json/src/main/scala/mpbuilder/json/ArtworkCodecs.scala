package mpbuilder.json

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import mpbuilder.artwork.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.json.SharedCodecs.given

/** Codecs for the public `artwork` model. */
object ArtworkCodecs:
  given JsonValueCodec[ArtworkOwner]          = JsonCodecMaker.make(config)
  given JsonValueCodec[ColorSpace]            = JsonCodecMaker.make(config)
  given JsonValueCodec[AssetFileMetadata]     = JsonCodecMaker.make(config)
  given JsonValueCodec[ExtractedFileSpecs]    = JsonCodecMaker.make(config)
  given JsonValueCodec[PreflightRequirement]  = JsonCodecMaker.make(config)
  given JsonValueCodec[PreflightReport]       = JsonCodecMaker.make(config)
  given JsonValueCodec[ArtworkStatus]         = JsonCodecMaker.make(config)
  given JsonValueCodec[ArtworkRevision]       = JsonCodecMaker.make(config)
  given JsonValueCodec[ArtworkSubmissionView] = JsonCodecMaker.make(config)
  given JsonValueCodec[ArtworkError]          = JsonCodecMaker.make(config)
