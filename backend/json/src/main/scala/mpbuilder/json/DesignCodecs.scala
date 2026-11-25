package mpbuilder.json

import kyo.*
import mpbuilder.design.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.json.JsonSupport.given
import mpbuilder.json.SharedCodecs.given
import mpbuilder.shared.*

/** Schemas for the `design` context: the Visual Editor document (browser IndexedDB, HTTP sync, JSONB rows)
  * and the project envelope.
  *
  * Evolution rule: new fields get a default value (old documents keep decoding); anything else bumps
  * `DesignDocument.formatVersion` and adds a migration step in [[DesignCodecs.decodeDocument]].
  */
object DesignCodecs:
  given Schema[ElementId]   = stringOpaque("ElementId")(ElementId(_))(_.value)
  given Schema[PageId]      = stringOpaque("PageId")(PageId(_))(_.value)
  given Schema[AssetId]     = stringOpaque("AssetId")(AssetId(_))(_.value)
  given Schema[DesignColor] = stringOpaque("DesignColor")(DesignColor.parse)(_.hex)

  given Schema[Fill]              = Schema.derived
  given Schema[StrokeDash]        = Schema.derived
  given Schema[Stroke]            = Schema.derived
  given Schema[Placement]             = Schema.derived
  given Schema[TextHAlign]        = Schema.derived
  given Schema[TextVAlign]        = Schema.derived
  given Schema[TextStyle]         = Schema.derived
  given Schema[ImageFit]          = Schema.derived
  given Schema[ImageFilter]       = Schema.derived
  given Schema[ImageCrop]         = Schema.derived
  given Schema[ShapeKind]         = Schema.derived
  given Schema[ClipartKind]       = Schema.derived
  given Schema[WeekStart]         = Schema.derived
  given Schema[HolidaySet]        = Schema.derived
  given Schema[CalendarSpec]      = Schema.derived
  given Schema[ElementContent]    = Schema.derived
  given Schema[DesignElement]     = Schema.derived
  given Schema[PageRole]          = Schema.derived
  given Schema[DesignPage]        = Schema.derived
  given Schema[BindingEdge]       = Schema.derived
  given Schema[BindingGuide]      = Schema.derived
  given Schema[DesignFormat]      = Schema.derived
  given Schema[PagePolicy]        = Schema.derived
  given Schema[DesignProductKind] = Schema.derived
  given Schema[ProductBinding]    = Schema.derived
  given Schema[AssetStorage]      = Schema.derived
  given Schema[ImageAsset]        = Schema.derived
  given Schema[DesignDocument]    = Schema.derived

  given Schema[DesignOwner]   = Schema.derived
  given Schema[DesignStatus]  = Schema.derived
  given Schema[DesignProject] = Schema.derived
  given Schema[DesignSummary] = Schema.derived
  given Schema[DesignError]   = Schema.derived

  def encodeDocument(doc: DesignDocument): String = Json.encode(doc)

  /** Decodes a stored document, applying format migrations, then the structural validation. */
  def decodeDocument(json: String): Either[String, DesignDocument] =
    Json.decode[DesignDocument](json).toEither.left.map(e => String.valueOf(e.getMessage)).flatMap { doc =>
      DesignValidation.validate(doc).toEither.left.map(_.toList.mkString("; "))
    }

  def encodeProject(project: DesignProject): String = Json.encode(project)

  def decodeProject(json: String): Either[String, DesignProject] =
    Json.decode[DesignProject](json).toEither.left.map(e => String.valueOf(e.getMessage))
