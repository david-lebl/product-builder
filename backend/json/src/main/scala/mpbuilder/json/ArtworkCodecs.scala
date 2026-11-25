package mpbuilder.json

import kyo.*
import mpbuilder.artwork.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.shared.*
import mpbuilder.json.SharedCodecs.given
import mpbuilder.json.JsonSupport.given

/** Schemas for the public `artwork` model. */
object ArtworkCodecs:
  given Schema[ArtworkOwner]          = Schema.derived
  given Schema[ColorSpace]            = Schema.derived
  given Schema[AssetFileMetadata]     = Schema.derived
  given Schema[ExtractedFileSpecs]    = Schema.derived
  given Schema[PreflightRequirement]  = Schema.derived
  given Schema[PreflightReport]       = Schema.derived
  given Schema[ArtworkStatus]         = Schema.derived
  given Schema[ArtworkRevision]       = Schema.derived
  given Schema[ArtworkSubmissionView] = Schema.derived
  given Schema[ArtworkError]          = Schema.derived
