package mpbuilder.ui.persistence

import zio.json.*
import mpbuilder.ui.visualeditor.*

/** JSON codecs for all visual-editor model types, derived automatically from the model structure.
  *
  * Simple enums (no parameters) are encoded as their case-name string via `toString`/`valueOf`.
  * Sum types (`PageBackground`, `CanvasElement`) use zio-json's discriminated union encoding,
  * where each variant is wrapped in an object keyed by its class name, e.g.:
  *   `{"SolidColor": {"color": "#fff"}}`
  *   `{"PhotoElement": {"id": "...", "imageData": "...", ...}}`
  *
  * Usage:
  * {{{
  * import mpbuilder.ui.persistence.EditorCodecs.given
  * val json: String   = session.toJson
  * val back: EditorSession = json.fromJson[EditorSession].getOrElse(???)
  * }}}
  */
object EditorCodecs:

  // ── Leaf types ───────────────────────────────────────────────────────────

  given JsonCodec[Position] = DeriveJsonCodec.gen[Position]
  given JsonCodec[Size]     = DeriveJsonCodec.gen[Size]

  // ── Simple enums — encoded as their .toString case name ──────────────────

  given JsonEncoder[TextAlignment]    = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[TextAlignment]    = JsonDecoder[String].map(TextAlignment.valueOf)

  given JsonEncoder[ShapeType]        = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[ShapeType]        = JsonDecoder[String].map(ShapeType.valueOf)

  given JsonEncoder[PageTemplateType] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[PageTemplateType] = JsonDecoder[String].map(PageTemplateType.valueOf)

  given JsonEncoder[VisualProductType] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[VisualProductType] = JsonDecoder[String].map(VisualProductType.valueOf)

  // ── Simple case classes ───────────────────────────────────────────────────

  given JsonCodec[ProductFormat]     = DeriveJsonCodec.gen[ProductFormat]
  given JsonCodec[TemplateTextField] = DeriveJsonCodec.gen[TemplateTextField]

  // ── PageBackground sum type ───────────────────────────────────────────────
  // Encodes each variant wrapped in its case name, e.g.:
  //   SolidColor(color)   → {"SolidColor":  {"color":     "#fff"}}
  //   BackgroundImage(url) → {"BackgroundImage": {"imageData": "data:..."}}

  given JsonCodec[PageBackground] = DeriveJsonCodec.gen[PageBackground]

  // ── Canvas element case classes (concrete subtypes first) ─────────────────

  given JsonCodec[PhotoElement]   = DeriveJsonCodec.gen[PhotoElement]
  given JsonCodec[TextElement]    = DeriveJsonCodec.gen[TextElement]
  given JsonCodec[ShapeElement]   = DeriveJsonCodec.gen[ShapeElement]
  given JsonCodec[ClipartElement] = DeriveJsonCodec.gen[ClipartElement]

  // Sealed trait — variant discriminated by class-name wrapper key, e.g.:
  //   {"PhotoElement": {"id": "x", "imageData": "...", ...}}

  given JsonCodec[CanvasElement] = DeriveJsonCodec.gen[CanvasElement]

  // ── Page / editor state ───────────────────────────────────────────────────

  given JsonCodec[PageTemplate] = DeriveJsonCodec.gen[PageTemplate]
  given JsonCodec[EditorPage]   = DeriveJsonCodec.gen[EditorPage]
  given JsonCodec[EditorState]  = DeriveJsonCodec.gen[EditorState]

  // ── Session types ─────────────────────────────────────────────────────────

  given JsonCodec[ProductContext] = DeriveJsonCodec.gen[ProductContext]
  given JsonCodec[EditorSession]  = DeriveJsonCodec.gen[EditorSession]

  // ── Image reference ───────────────────────────────────────────────────────

  given JsonCodec[ImageReference] = DeriveJsonCodec.gen[ImageReference]
