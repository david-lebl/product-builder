package mpbuilder.design

enum FontCategory derives CanEqual:
  case Sans, Serif, Display, Script, Mono

/** A print-licensed typeface offered in the editor.
  *
  * Only fonts from this curated list can be used: the PDF renderer must embed the *same* font files the
  * browser displayed, so arbitrary system fonts are never allowed. All entries are SIL Open Font License
  * families from Google Fonts (embeddable in PDFs, full Czech `latin-ext` coverage).
  */
final case class FontFace(
    key: String,
    family: String,
    category: FontCategory,
    weights: List[Int],
    hasItalic: Boolean,
    fallback: String,
):
  /** CSS `font-family` value (quoted family + generic fallback). */
  def cssFamily: String = s"'$family', $fallback"
  def supportsWeight(w: Int): Boolean = weights.contains(w)
  /** Closest available weight (renderers never synthesize bold). */
  def nearestWeight(w: Int): Int = weights.minBy(x => math.abs(x - w))

object FontCatalog:
  val DefaultKey: String = "inter"

  val all: List[FontFace] = List(
    FontFace("inter", "Inter", FontCategory.Sans, List(400, 500, 600, 700, 800), hasItalic = true, "system-ui, sans-serif"),
    FontFace("montserrat", "Montserrat", FontCategory.Sans, List(400, 500, 600, 700, 800), hasItalic = true, "sans-serif"),
    FontFace("roboto-condensed", "Roboto Condensed", FontCategory.Sans, List(400, 700), hasItalic = true, "sans-serif"),
    FontFace("playfair", "Playfair Display", FontCategory.Serif, List(400, 600, 700, 800), hasItalic = true, "Georgia, serif"),
    FontFace("lora", "Lora", FontCategory.Serif, List(400, 500, 600, 700), hasItalic = true, "Georgia, serif"),
    FontFace("merriweather", "Merriweather", FontCategory.Serif, List(400, 700), hasItalic = true, "Georgia, serif"),
    FontFace("oswald", "Oswald", FontCategory.Display, List(400, 500, 600, 700), hasItalic = false, "Impact, sans-serif"),
    FontFace("bebas", "Bebas Neue", FontCategory.Display, List(400), hasItalic = false, "Impact, sans-serif"),
    FontFace("dancing", "Dancing Script", FontCategory.Script, List(400, 500, 600, 700), hasItalic = false, "cursive"),
    FontFace("caveat", "Caveat", FontCategory.Script, List(400, 500, 600, 700), hasItalic = false, "cursive"),
    FontFace("jetbrains-mono", "JetBrains Mono", FontCategory.Mono, List(400, 700), hasItalic = true, "ui-monospace, monospace"),
  )

  private val byKey: Map[String, FontFace] = all.map(f => f.key -> f).toMap

  def find(key: String): Option[FontFace] = byKey.get(key)
  def get(key: String): FontFace          = byKey.getOrElse(key, byKey(DefaultKey))
  def contains(key: String): Boolean      = byKey.contains(key)

  /** Google Fonts CSS2 URL loading every catalog face (used by browser hosts; the PDF renderer bundles the TTFs). */
  def googleFontsCssUrl: String =
    val families = all.map { f =>
      val name = f.family.replace(' ', '+')
      if f.hasItalic then
        val axes = (f.weights.map(w => s"0,$w") ++ f.weights.map(w => s"1,$w")).mkString(";")
        s"family=$name:ital,wght@$axes"
      else s"family=$name:wght@${f.weights.mkString(";")}"
    }
    s"https://fonts.googleapis.com/css2?${families.mkString("&")}&display=swap"
