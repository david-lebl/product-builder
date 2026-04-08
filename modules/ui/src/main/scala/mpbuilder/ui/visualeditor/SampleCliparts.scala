package mpbuilder.ui.visualeditor

/** Categories used to group and filter cliparts in the gallery panel. */
enum ClipartCategory:
  case Shapes, Symbols, Nature, Decorative, Holiday

/** A single clipart entry. `svgDataUrl` is a self-contained `data:image/svg+xml,...`
  * URL ready to be assigned to an `<img src>` or used as a `ClipartElement`'s
  * `imageData`.
  *
  * SVGs are intentionally simple inline strings — no external file dependency,
  * easy to extend later. Special characters (`<`, `>`, `#`, `"`) are
  * URL-encoded so the value is a valid data URL.
  */
case class ClipartItem(
  id: String,
  nameEn: String,
  nameCs: String,
  svgDataUrl: String,
  category: ClipartCategory,
  keywords: List[String],
)

object SampleCliparts:

  /** Build a data URL from a raw SVG body. */
  private inline def svg(body: String): String =
    "data:image/svg+xml," + body
      .replace("#", "%23")
      .replace("<", "%3C")
      .replace(">", "%3E")
      .replace("\"", "%22")
      .replace(" ", "%20")

  // ─── Shapes ──────────────────────────────────────────────────────
  private val circle = ClipartItem(
    "shape-circle", "Circle", "Kruh",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="50" cy="50" r="42" fill="#667eea" stroke="#3b3f8a" stroke-width="4"/></svg>"""),
    ClipartCategory.Shapes, List("circle", "round", "dot", "kruh"))

  private val square = ClipartItem(
    "shape-square", "Square", "Čtverec",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect x="10" y="10" width="80" height="80" fill="#4caf50" stroke="#1b5e20" stroke-width="4"/></svg>"""),
    ClipartCategory.Shapes, List("square", "rectangle", "box", "ctverec"))

  private val triangle = ClipartItem(
    "shape-triangle", "Triangle", "Trojúhelník",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><polygon points="50,10 90,90 10,90" fill="#ff9800" stroke="#e65100" stroke-width="4"/></svg>"""),
    ClipartCategory.Shapes, List("triangle", "trojuhelnik"))

  private val diamond = ClipartItem(
    "shape-diamond", "Diamond", "Kosočtverec",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><polygon points="50,10 90,50 50,90 10,50" fill="#9c27b0" stroke="#4a148c" stroke-width="4"/></svg>"""),
    ClipartCategory.Shapes, List("diamond", "rhombus", "kosoctverec"))

  private val pentagon = ClipartItem(
    "shape-pentagon", "Pentagon", "Pětiúhelník",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><polygon points="50,10 92,40 76,90 24,90 8,40" fill="#e91e63" stroke="#880e4f" stroke-width="4"/></svg>"""),
    ClipartCategory.Shapes, List("pentagon", "petiuhelnik"))

  private val hexagon = ClipartItem(
    "shape-hexagon", "Hexagon", "Šestiúhelník",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><polygon points="50,8 88,30 88,70 50,92 12,70 12,30" fill="#00bcd4" stroke="#006064" stroke-width="4"/></svg>"""),
    ClipartCategory.Shapes, List("hexagon", "honeycomb", "sestiuhelnik"))

  private val star = ClipartItem(
    "shape-star", "Star", "Hvězda",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><polygon points="50,5 61,38 96,38 68,58 79,92 50,72 21,92 32,58 4,38 39,38" fill="#ffc107" stroke="#ff6f00" stroke-width="3"/></svg>"""),
    ClipartCategory.Shapes, List("star", "favorite", "hvezda"))

  private val heartShape = ClipartItem(
    "shape-heart", "Heart", "Srdce",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M50 88 C20 60 5 38 25 22 C40 12 50 28 50 28 C50 28 60 12 75 22 C95 38 80 60 50 88 Z" fill="#f44336" stroke="#b71c1c" stroke-width="3"/></svg>"""),
    ClipartCategory.Shapes, List("heart", "love", "srdce", "love"))

  // ─── Symbols ─────────────────────────────────────────────────────
  private val arrowRight = ClipartItem(
    "symbol-arrow-right", "Arrow Right", "Šipka vpravo",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M10 40 L60 40 L60 20 L95 50 L60 80 L60 60 L10 60 Z" fill="#3f51b5" stroke="#1a237e" stroke-width="3"/></svg>"""),
    ClipartCategory.Symbols, List("arrow", "right", "next", "sipka"))

  private val arrowLeft = ClipartItem(
    "symbol-arrow-left", "Arrow Left", "Šipka vlevo",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M90 40 L40 40 L40 20 L5 50 L40 80 L40 60 L90 60 Z" fill="#3f51b5" stroke="#1a237e" stroke-width="3"/></svg>"""),
    ClipartCategory.Symbols, List("arrow", "left", "back", "sipka"))

  private val arrowUp = ClipartItem(
    "symbol-arrow-up", "Arrow Up", "Šipka nahoru",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M40 90 L40 40 L20 40 L50 5 L80 40 L60 40 L60 90 Z" fill="#3f51b5" stroke="#1a237e" stroke-width="3"/></svg>"""),
    ClipartCategory.Symbols, List("arrow", "up", "sipka"))

  private val checkmark = ClipartItem(
    "symbol-check", "Check", "Zaškrtnutí",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="50" cy="50" r="45" fill="#4caf50"/><path d="M30 50 L45 65 L72 35" fill="none" stroke="white" stroke-width="8" stroke-linecap="round" stroke-linejoin="round"/></svg>"""),
    ClipartCategory.Symbols, List("check", "ok", "yes", "done", "zaskrtnuti"))

  private val cross = ClipartItem(
    "symbol-cross", "Cross", "Křížek",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="50" cy="50" r="45" fill="#f44336"/><path d="M30 30 L70 70 M70 30 L30 70" stroke="white" stroke-width="8" stroke-linecap="round"/></svg>"""),
    ClipartCategory.Symbols, List("cross", "x", "no", "delete", "krizek"))

  private val plus = ClipartItem(
    "symbol-plus", "Plus", "Plus",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="50" cy="50" r="45" fill="#2196f3"/><path d="M50 25 L50 75 M25 50 L75 50" stroke="white" stroke-width="10" stroke-linecap="round"/></svg>"""),
    ClipartCategory.Symbols, List("plus", "add", "new"))

  private val info = ClipartItem(
    "symbol-info", "Info", "Info",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="50" cy="50" r="45" fill="#03a9f4"/><circle cx="50" cy="28" r="6" fill="white"/><rect x="44" y="42" width="12" height="36" rx="2" fill="white"/></svg>"""),
    ClipartCategory.Symbols, List("info", "i", "information"))

  private val warning = ClipartItem(
    "symbol-warning", "Warning", "Varování",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><polygon points="50,8 95,90 5,90" fill="#ff9800" stroke="#e65100" stroke-width="3"/><rect x="46" y="36" width="8" height="30" fill="white"/><circle cx="50" cy="76" r="5" fill="white"/></svg>"""),
    ClipartCategory.Symbols, List("warning", "alert", "caution", "varovani"))

  // ─── Nature ──────────────────────────────────────────────────────
  private val sun = ClipartItem(
    "nature-sun", "Sun", "Slunce",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><g stroke="#ffa000" stroke-width="4" stroke-linecap="round"><line x1="50" y1="5" x2="50" y2="20"/><line x1="50" y1="80" x2="50" y2="95"/><line x1="5" y1="50" x2="20" y2="50"/><line x1="80" y1="50" x2="95" y2="50"/><line x1="18" y1="18" x2="28" y2="28"/><line x1="72" y1="72" x2="82" y2="82"/><line x1="18" y1="82" x2="28" y2="72"/><line x1="72" y1="28" x2="82" y2="18"/></g><circle cx="50" cy="50" r="22" fill="#ffeb3b" stroke="#ffa000" stroke-width="3"/></svg>"""),
    ClipartCategory.Nature, List("sun", "summer", "weather", "slunce"))

  private val cloud = ClipartItem(
    "nature-cloud", "Cloud", "Mrak",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M25 70 Q10 70 12 55 Q14 42 28 44 Q30 25 50 25 Q72 25 76 45 Q92 45 92 60 Q92 75 75 75 L25 75 Z" fill="#e3f2fd" stroke="#90caf9" stroke-width="3"/></svg>"""),
    ClipartCategory.Nature, List("cloud", "weather", "sky", "mrak"))

  private val tree = ClipartItem(
    "nature-tree", "Tree", "Strom",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect x="44" y="60" width="12" height="32" fill="#6d4c41"/><circle cx="50" cy="40" r="28" fill="#43a047" stroke="#1b5e20" stroke-width="3"/></svg>"""),
    ClipartCategory.Nature, List("tree", "plant", "nature", "strom"))

  private val leaf = ClipartItem(
    "nature-leaf", "Leaf", "List",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M20 80 Q15 30 60 15 Q90 10 85 50 Q75 90 30 90 Q22 88 20 80 Z" fill="#66bb6a" stroke="#2e7d32" stroke-width="3"/><path d="M30 80 Q50 50 75 30" stroke="#2e7d32" stroke-width="2" fill="none"/></svg>"""),
    ClipartCategory.Nature, List("leaf", "plant", "nature", "list"))

  private val flower = ClipartItem(
    "nature-flower", "Flower", "Květina",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="50" cy="30" r="14" fill="#f8bbd0"/><circle cx="30" cy="50" r="14" fill="#f8bbd0"/><circle cx="70" cy="50" r="14" fill="#f8bbd0"/><circle cx="50" cy="70" r="14" fill="#f8bbd0"/><circle cx="50" cy="50" r="10" fill="#fff176" stroke="#fbc02d" stroke-width="2"/></svg>"""),
    ClipartCategory.Nature, List("flower", "bloom", "nature", "kvetina"))

  private val mountain = ClipartItem(
    "nature-mountain", "Mountain", "Hora",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><polygon points="10,85 35,40 50,60 65,30 90,85" fill="#78909c" stroke="#37474f" stroke-width="3"/><polygon points="28,52 35,40 42,52 35,55" fill="white"/><polygon points="58,42 65,30 72,42 65,45" fill="white"/></svg>"""),
    ClipartCategory.Nature, List("mountain", "peak", "nature", "hora"))

  // ─── Decorative ──────────────────────────────────────────────────
  private val ribbon = ClipartItem(
    "deco-ribbon", "Ribbon", "Stuha",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect x="15" y="35" width="70" height="30" fill="#ec407a" stroke="#ad1457" stroke-width="3"/><polygon points="15,35 5,50 15,65" fill="#c2185b"/><polygon points="85,35 95,50 85,65" fill="#c2185b"/></svg>"""),
    ClipartCategory.Decorative, List("ribbon", "banner", "decoration", "stuha"))

  private val sparkle = ClipartItem(
    "deco-sparkle", "Sparkle", "Třpyt",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M50 5 L55 45 L95 50 L55 55 L50 95 L45 55 L5 50 L45 45 Z" fill="#fff59d" stroke="#f9a825" stroke-width="2"/></svg>"""),
    ClipartCategory.Decorative, List("sparkle", "shine", "star", "trpyt"))

  private val crown = ClipartItem(
    "deco-crown", "Crown", "Koruna",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M15 70 L20 30 L35 50 L50 20 L65 50 L80 30 L85 70 Z" fill="#ffd54f" stroke="#f57f17" stroke-width="3"/><rect x="15" y="70" width="70" height="10" fill="#ffb300" stroke="#f57f17" stroke-width="3"/><circle cx="20" cy="30" r="4" fill="#e53935"/><circle cx="50" cy="20" r="4" fill="#1e88e5"/><circle cx="80" cy="30" r="4" fill="#43a047"/></svg>"""),
    ClipartCategory.Decorative, List("crown", "king", "queen", "koruna"))

  private val bow = ClipartItem(
    "deco-bow", "Bow", "Mašle",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><path d="M50 50 Q15 30 15 50 Q15 70 50 50 Q85 30 85 50 Q85 70 50 50 Z" fill="#ef5350" stroke="#b71c1c" stroke-width="3"/><circle cx="50" cy="50" r="8" fill="#c62828"/></svg>"""),
    ClipartCategory.Decorative, List("bow", "ribbon", "gift", "masle"))

  private val musicNote = ClipartItem(
    "deco-music", "Music Note", "Nota",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect x="55" y="15" width="6" height="55" fill="#212121"/><ellipse cx="45" cy="72" rx="14" ry="10" fill="#212121"/><path d="M55 15 Q80 18 80 35 Q70 25 55 30 Z" fill="#212121"/></svg>"""),
    ClipartCategory.Decorative, List("music", "note", "song", "nota"))

  // ─── Holiday ─────────────────────────────────────────────────────
  private val snowflake = ClipartItem(
    "holiday-snowflake", "Snowflake", "Sněhová vločka",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><g stroke="#4fc3f7" stroke-width="4" stroke-linecap="round" fill="none"><line x1="50" y1="10" x2="50" y2="90"/><line x1="10" y1="50" x2="90" y2="50"/><line x1="22" y1="22" x2="78" y2="78"/><line x1="78" y1="22" x2="22" y2="78"/><line x1="50" y1="20" x2="42" y2="28"/><line x1="50" y1="20" x2="58" y2="28"/><line x1="50" y1="80" x2="42" y2="72"/><line x1="50" y1="80" x2="58" y2="72"/></g></svg>"""),
    ClipartCategory.Holiday, List("snowflake", "winter", "christmas", "vlocka"))

  private val christmasTree = ClipartItem(
    "holiday-xmas-tree", "Christmas Tree", "Vánoční stromek",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><polygon points="50,8 70,35 60,35 78,60 65,60 85,85 15,85 35,60 22,60 40,35 30,35" fill="#2e7d32" stroke="#1b5e20" stroke-width="3"/><rect x="44" y="85" width="12" height="10" fill="#6d4c41"/><polygon points="50,5 53,11 60,11 55,15 57,22 50,18 43,22 45,15 40,11 47,11" fill="#fdd835" stroke="#f57f17" stroke-width="1"/></svg>"""),
    ClipartCategory.Holiday, List("christmas", "tree", "xmas", "stromek"))

  private val gift = ClipartItem(
    "holiday-gift", "Gift", "Dárek",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect x="15" y="40" width="70" height="50" fill="#e53935" stroke="#b71c1c" stroke-width="3"/><rect x="44" y="40" width="12" height="50" fill="#fdd835"/><rect x="15" y="40" width="70" height="10" fill="#fdd835"/><path d="M50 40 Q35 25 35 18 Q35 12 45 15 Q50 20 50 30 Q50 20 55 15 Q65 12 65 18 Q65 25 50 40" fill="#fdd835" stroke="#f57f17" stroke-width="2"/></svg>"""),
    ClipartCategory.Holiday, List("gift", "present", "birthday", "darek"))

  private val balloon = ClipartItem(
    "holiday-balloon", "Balloon", "Balónek",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><ellipse cx="50" cy="38" rx="28" ry="32" fill="#ec407a" stroke="#ad1457" stroke-width="3"/><polygon points="46,68 54,68 50,75" fill="#ad1457"/><path d="M50 75 Q45 88 50 95" stroke="#555" stroke-width="2" fill="none"/></svg>"""),
    ClipartCategory.Holiday, List("balloon", "party", "birthday", "balonek"))

  private val cake = ClipartItem(
    "holiday-cake", "Cake", "Dort",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect x="15" y="55" width="70" height="30" fill="#f8bbd0" stroke="#c2185b" stroke-width="3"/><path d="M15 55 Q25 45 35 55 Q45 45 55 55 Q65 45 75 55 Q82 50 85 55" fill="#fff0f6" stroke="#c2185b" stroke-width="3"/><rect x="48" y="35" width="4" height="15" fill="#fdd835"/><circle cx="50" cy="32" r="3" fill="#ff5722"/></svg>"""),
    ClipartCategory.Holiday, List("cake", "birthday", "party", "dort"))

  private val pumpkin = ClipartItem(
    "holiday-pumpkin", "Pumpkin", "Dýně",
    svg("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><ellipse cx="50" cy="58" rx="38" ry="30" fill="#ff6f00" stroke="#bf360c" stroke-width="3"/><path d="M30 35 Q25 30 30 25 Q35 22 38 28" fill="#388e3c" stroke="#1b5e20" stroke-width="2"/><path d="M50 30 L50 86 M30 35 L30 80 M70 35 L70 80" stroke="#bf360c" stroke-width="2" fill="none"/></svg>"""),
    ClipartCategory.Holiday, List("pumpkin", "halloween", "autumn", "dyne"))

  // ─── Catalog ─────────────────────────────────────────────────────
  val all: List[ClipartItem] = List(
    // Shapes
    circle, square, triangle, diamond, pentagon, hexagon, star, heartShape,
    // Symbols
    arrowRight, arrowLeft, arrowUp, checkmark, cross, plus, info, warning,
    // Nature
    sun, cloud, tree, leaf, flower, mountain,
    // Decorative
    ribbon, sparkle, crown, bow, musicNote,
    // Holiday
    snowflake, christmasTree, gift, balloon, cake, pumpkin,
  )

  /** Filter by category. */
  def byCategory(c: ClipartCategory): List[ClipartItem] = all.filter(_.category == c)

  /** Search by name (per language) and keywords. Empty query returns all items. */
  def search(query: String, lang: String): List[ClipartItem] = {
    val q = query.trim.toLowerCase
    if q.isEmpty then all
    else
      all.filter { item =>
        val name = if lang == "cs" then item.nameCs else item.nameEn
        name.toLowerCase.contains(q) || item.keywords.exists(_.toLowerCase.contains(q))
      }
  }

  /** Localised display name for a category. */
  def categoryLabel(c: ClipartCategory, lang: String): String =
    (c, lang) match {
      case (ClipartCategory.Shapes,     "cs") => "Tvary"
      case (ClipartCategory.Shapes,     _)    => "Shapes"
      case (ClipartCategory.Symbols,    "cs") => "Symboly"
      case (ClipartCategory.Symbols,    _)    => "Symbols"
      case (ClipartCategory.Nature,     "cs") => "Příroda"
      case (ClipartCategory.Nature,     _)    => "Nature"
      case (ClipartCategory.Decorative, "cs") => "Dekorace"
      case (ClipartCategory.Decorative, _)    => "Decorative"
      case (ClipartCategory.Holiday,    "cs") => "Svátky"
      case (ClipartCategory.Holiday,    _)    => "Holiday"
    }
