package mpbuilder.ui.visualeditor.components

import com.raquo.laminar.api.L.*
import mpbuilder.ui.visualeditor.*
import mpbuilder.ui.productbuilder.ProductBuilderViewModel
import mpbuilder.domain.model.Language

/** Clipart gallery panel with predefined SVG cliparts, categories, and search */
object ClipartGalleryPanel {

  // Categories
  private val AllCategory = "all"

  case class ClipartItem(id: String, name: String, category: String, svgData: String)

  // Predefined clipart items (SVG data URLs)
  private val cliparts: List[ClipartItem] = List(
    // ─── Shapes ────────────────────────────────────────
    ClipartItem("star-1", "Star", "shapes", svgIcon("""<polygon points="50,5 63,35 95,35 69,57 79,90 50,70 21,90 31,57 5,35 37,35" fill="#FFD700" stroke="#DAA520" stroke-width="2"/>""")),
    ClipartItem("heart-1", "Heart", "shapes", svgIcon("""<path d="M50,90 C25,65 0,50 0,30 A25,25,0,0,1,50,30 A25,25,0,0,1,100,30 C100,50 75,65 50,90Z" fill="#FF4444" stroke="#CC0000" stroke-width="2"/>""")),
    ClipartItem("circle-1", "Circle", "shapes", svgIcon("""<circle cx="50" cy="50" r="45" fill="#667eea" stroke="#764ba2" stroke-width="3"/>""")),
    ClipartItem("diamond-1", "Diamond", "shapes", svgIcon("""<polygon points="50,5 95,50 50,95 5,50" fill="#00BCD4" stroke="#0097A7" stroke-width="2"/>""")),
    ClipartItem("hexagon-1", "Hexagon", "shapes", svgIcon("""<polygon points="50,5 93,25 93,75 50,95 7,75 7,25" fill="#9C27B0" stroke="#7B1FA2" stroke-width="2"/>""")),
    ClipartItem("triangle-1", "Triangle", "shapes", svgIcon("""<polygon points="50,10 95,90 5,90" fill="#FF9800" stroke="#F57C00" stroke-width="2"/>""")),

    // ─── Arrows ────────────────────────────────────────
    ClipartItem("arrow-right", "Arrow Right", "arrows", svgIcon("""<polygon points="10,35 65,35 65,15 95,50 65,85 65,65 10,65" fill="#4CAF50" stroke="#388E3C" stroke-width="2"/>""")),
    ClipartItem("arrow-left", "Arrow Left", "arrows", svgIcon("""<polygon points="90,35 35,35 35,15 5,50 35,85 35,65 90,65" fill="#2196F3" stroke="#1976D2" stroke-width="2"/>""")),
    ClipartItem("arrow-up", "Arrow Up", "arrows", svgIcon("""<polygon points="35,90 35,35 15,35 50,5 85,35 65,35 65,90" fill="#FF5722" stroke="#E64A19" stroke-width="2"/>""")),
    ClipartItem("arrow-down", "Arrow Down", "arrows", svgIcon("""<polygon points="35,10 35,65 15,65 50,95 85,65 65,65 65,10" fill="#795548" stroke="#5D4037" stroke-width="2"/>""")),
    ClipartItem("arrow-curved", "Curved Arrow", "arrows", svgIcon("""<path d="M20,80 C20,30 80,30 80,45" fill="none" stroke="#E91E63" stroke-width="4" stroke-linecap="round"/><polygon points="72,35 90,48 78,55" fill="#E91E63"/>""")),

    // ─── Decorations ──────────────────────────────────
    ClipartItem("ribbon-1", "Ribbon", "decorations", svgIcon("""<rect x="10" y="25" width="80" height="50" rx="5" fill="#FF4081" stroke="#C51162" stroke-width="2"/><polygon points="0,25 15,50 0,75" fill="#FF4081"/><polygon points="100,25 85,50 100,75" fill="#FF4081"/>""")),
    ClipartItem("banner-1", "Banner", "decorations", svgIcon("""<rect x="5" y="20" width="90" height="40" rx="3" fill="#FFC107" stroke="#FF8F00" stroke-width="2"/><polygon points="5,60 5,75 20,65" fill="#FF8F00"/><polygon points="95,60 95,75 80,65" fill="#FF8F00"/>""")),
    ClipartItem("frame-1", "Ornate Frame", "decorations", svgIcon("""<rect x="10" y="10" width="80" height="80" rx="3" fill="none" stroke="#8B7355" stroke-width="4"/><rect x="15" y="15" width="70" height="70" rx="2" fill="none" stroke="#8B7355" stroke-width="1"/>""")),
    ClipartItem("wreath-1", "Wreath", "decorations", svgIcon("""<circle cx="50" cy="50" r="40" fill="none" stroke="#4CAF50" stroke-width="6" stroke-dasharray="8 4"/><circle cx="50" cy="50" r="32" fill="none" stroke="#81C784" stroke-width="3" stroke-dasharray="5 3"/>""")),
    ClipartItem("flourish-1", "Flourish", "decorations", svgIcon("""<path d="M10,50 Q30,20 50,50 Q70,80 90,50" fill="none" stroke="#9C27B0" stroke-width="3"/><path d="M10,50 Q30,80 50,50 Q70,20 90,50" fill="none" stroke="#CE93D8" stroke-width="2"/>""")),

    // ─── Nature ───────────────────────────────────────
    ClipartItem("sun-1", "Sun", "nature", svgIcon("""<circle cx="50" cy="50" r="20" fill="#FFD700"/><g stroke="#FFD700" stroke-width="3"><line x1="50" y1="5" x2="50" y2="20"/><line x1="50" y1="80" x2="50" y2="95"/><line x1="5" y1="50" x2="20" y2="50"/><line x1="80" y1="50" x2="95" y2="50"/><line x1="18" y1="18" x2="29" y2="29"/><line x1="71" y1="71" x2="82" y2="82"/><line x1="82" y1="18" x2="71" y2="29"/><line x1="29" y1="71" x2="18" y2="82"/></g>""")),
    ClipartItem("cloud-1", "Cloud", "nature", svgIcon("""<circle cx="35" cy="55" r="20" fill="#B3E5FC"/><circle cx="55" cy="45" r="25" fill="#B3E5FC"/><circle cx="70" cy="55" r="18" fill="#B3E5FC"/><rect x="20" y="55" width="65" height="20" rx="10" fill="#B3E5FC"/>""")),
    ClipartItem("tree-1", "Tree", "nature", svgIcon("""<rect x="43" y="60" width="14" height="35" fill="#795548"/><polygon points="50,10 80,45 65,45 85,65 15,65 35,45 20,45" fill="#4CAF50"/>""")),
    ClipartItem("flower-1", "Flower", "nature", svgIcon("""<circle cx="50" cy="50" r="10" fill="#FFD700"/><ellipse cx="50" cy="28" rx="10" ry="16" fill="#FF4081"/><ellipse cx="72" cy="42" rx="10" ry="16" fill="#FF4081" transform="rotate(72,50,50)"/><ellipse cx="64" cy="68" rx="10" ry="16" fill="#FF4081" transform="rotate(144,50,50)"/><ellipse cx="36" cy="68" rx="10" ry="16" fill="#FF4081" transform="rotate(216,50,50)"/><ellipse cx="28" cy="42" rx="10" ry="16" fill="#FF4081" transform="rotate(288,50,50)"/>""")),
    ClipartItem("leaf-1", "Leaf", "nature", svgIcon("""<path d="M50,90 C20,60 10,30 50,10 C90,30 80,60 50,90Z" fill="#4CAF50" stroke="#2E7D32" stroke-width="2"/><line x1="50" y1="90" x2="50" y2="25" stroke="#2E7D32" stroke-width="2"/>""")),

    // ─── Symbols ──────────────────────────────────────
    ClipartItem("check-1", "Checkmark", "symbols", svgIcon("""<circle cx="50" cy="50" r="45" fill="#4CAF50"/><polyline points="25,50 42,68 75,32" fill="none" stroke="white" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>""")),
    ClipartItem("cross-1", "Cross", "symbols", svgIcon("""<circle cx="50" cy="50" r="45" fill="#F44336"/><line x1="30" y1="30" x2="70" y2="70" stroke="white" stroke-width="6" stroke-linecap="round"/><line x1="70" y1="30" x2="30" y2="70" stroke="white" stroke-width="6" stroke-linecap="round"/>""")),
    ClipartItem("info-1", "Info", "symbols", svgIcon("""<circle cx="50" cy="50" r="45" fill="#2196F3"/><text x="50" y="38" text-anchor="middle" fill="white" font-size="20" font-weight="bold">i</text><rect x="45" y="45" width="10" height="30" rx="3" fill="white"/>""")),
    ClipartItem("warning-1", "Warning", "symbols", svgIcon("""<polygon points="50,5 95,90 5,90" fill="#FF9800" stroke="#F57C00" stroke-width="2"/><text x="50" y="75" text-anchor="middle" fill="white" font-size="40" font-weight="bold">!</text>""")),
    ClipartItem("music-1", "Music Note", "symbols", svgIcon("""<circle cx="30" cy="75" r="12" fill="#333"/><rect x="40" y="20" width="4" height="58" fill="#333"/><path d="M44,20 Q60,10 70,25 Q60,15 44,30Z" fill="#333"/>""")),

    // ─── Emoji ────────────────────────────────────────
    ClipartItem("smile-1", "Smile", "emoji", svgIcon("""<circle cx="50" cy="50" r="45" fill="#FFEB3B" stroke="#FBC02D" stroke-width="2"/><circle cx="35" cy="40" r="5" fill="#333"/><circle cx="65" cy="40" r="5" fill="#333"/><path d="M30,60 Q50,80 70,60" fill="none" stroke="#333" stroke-width="3" stroke-linecap="round"/>""")),
    ClipartItem("sad-1", "Sad", "emoji", svgIcon("""<circle cx="50" cy="50" r="45" fill="#FFEB3B" stroke="#FBC02D" stroke-width="2"/><circle cx="35" cy="40" r="5" fill="#333"/><circle cx="65" cy="40" r="5" fill="#333"/><path d="M30,70 Q50,55 70,70" fill="none" stroke="#333" stroke-width="3" stroke-linecap="round"/>""")),
    ClipartItem("love-1", "Love", "emoji", svgIcon("""<circle cx="50" cy="50" r="45" fill="#FFEB3B" stroke="#FBC02D" stroke-width="2"/><path d="M30,37 C30,30 40,28 35,37 C30,28 20,30 20,37 C20,45 30,48 30,48Z" fill="#FF4444"/><path d="M80,37 C80,30 70,28 75,37 C80,28 90,30 90,37 C90,45 80,48 80,48Z" fill="#FF4444"/><path d="M30,60 Q50,80 70,60" fill="none" stroke="#333" stroke-width="3" stroke-linecap="round"/>""")),
    ClipartItem("wink-1", "Wink", "emoji", svgIcon("""<circle cx="50" cy="50" r="45" fill="#FFEB3B" stroke="#FBC02D" stroke-width="2"/><circle cx="35" cy="40" r="5" fill="#333"/><line x1="58" y1="40" x2="72" y2="40" stroke="#333" stroke-width="3" stroke-linecap="round"/><path d="M30,60 Q50,80 70,60" fill="none" stroke="#333" stroke-width="3" stroke-linecap="round"/>""")),

    // ─── Badges ───────────────────────────────────────
    ClipartItem("badge-circle", "Circle Badge", "badges", svgIcon("""<circle cx="50" cy="50" r="42" fill="#1565C0"/><circle cx="50" cy="50" r="36" fill="none" stroke="white" stroke-width="2"/>""")),
    ClipartItem("badge-shield", "Shield Badge", "badges", svgIcon("""<path d="M50,5 L90,25 L90,55 Q90,85 50,95 Q10,85 10,55 L10,25Z" fill="#1565C0" stroke="#0D47A1" stroke-width="2"/>""")),
    ClipartItem("badge-star", "Star Badge", "badges", svgIcon("""<polygon points="50,5 63,35 95,35 69,57 79,90 50,70 21,90 31,57 5,35 37,35" fill="#1565C0" stroke="#0D47A1" stroke-width="2"/><polygon points="50,20 58,40 80,40 63,52 70,72 50,60 30,72 37,52 20,40 42,40" fill="white"/>""")),
    ClipartItem("badge-ribbon", "Ribbon Badge", "badges", svgIcon("""<circle cx="50" cy="40" r="30" fill="#F44336" stroke="#C62828" stroke-width="2"/><polygon points="30,60 20,90 35,75 50,90 50,60" fill="#F44336"/><polygon points="70,60 80,90 65,75 50,90 50,60" fill="#F44336"/>""")),
  )

  private def svgIcon(inner: String): String =
    s"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E${urlEncode(inner)}%3C/svg%3E"

  private def urlEncode(s: String): String =
    s.replace("<", "%3C")
     .replace(">", "%3E")
     .replace("\"", "%22")
     .replace("#", "%23")
     .replace(" ", "%20")
     .replace("=", "%3D")
     .replace("/", "%2F")
     .replace("'", "%27")
     .replace("&", "%26")
     .replace("(", "%28")
     .replace(")", "%29")
     .replace(",", "%2C")

  private val categories: List[(String, String, String)] = List(
    ("all", "All", "Vše"),
    ("shapes", "Shapes", "Tvary"),
    ("arrows", "Arrows", "Šipky"),
    ("decorations", "Decorations", "Dekorace"),
    ("nature", "Nature", "Příroda"),
    ("symbols", "Symbols", "Symboly"),
    ("emoji", "Emoji", "Emoji"),
    ("badges", "Badges", "Odznaky"),
  )

  // State
  private val searchVar: Var[String] = Var("")
  private val categoryVar: Var[String] = Var(AllCategory)

  def apply(): Element = {
    val lang = ProductBuilderViewModel.currentLanguage

    div(
      cls := "clipart-gallery-section",

      h4(child.text <-- lang.map {
        case Language.En => "Clipart Gallery"
        case Language.Cs => "Galerie klipartů"
      }),

      p(
        cls := "gallery-description",
        child.text <-- lang.map {
          case Language.En => "Click any clipart to add it to your page."
          case Language.Cs => "Kliknutím přidáte klipart na stránku."
        },
      ),

      // Search
      div(
        cls := "clipart-search-row",
        input(
          typ := "text",
          cls := "clipart-search-input",
          placeholder <-- lang.map {
            case Language.En => "Search cliparts..."
            case Language.Cs => "Hledat kliparty..."
          },
          controlled(
            value <-- searchVar.signal,
            onInput.mapToValue --> { v => searchVar.set(v) }
          ),
        ),
      ),

      // Category filter buttons
      div(
        cls := "clipart-categories",
        children <-- lang.map { language =>
          categories.map { case (catId, nameEn, nameCs) =>
            button(
              cls := "clipart-category-btn",
              cls <-- categoryVar.signal.map(c => if c == catId then "active" else ""),
              language match {
                case Language.En => nameEn
                case Language.Cs => nameCs
              },
              onClick --> { _ => categoryVar.set(catId) },
            )
          }
        },
      ),

      // Clipart grid
      div(
        cls := "clipart-grid",
        children <-- searchVar.signal.combineWith(categoryVar.signal).map { (search: String, cat: String) =>
          val filtered = cliparts.filter { item =>
            val matchesCat = cat == AllCategory || item.category == cat
            val matchesSearch = search.isEmpty || item.name.toLowerCase.contains(search.toLowerCase)
            matchesCat && matchesSearch
          }

          if filtered.isEmpty then
            List(div(cls := "clipart-empty", child.text <-- lang.map {
              case Language.En => "No cliparts found"
              case Language.Cs => "Žádné kliparty nenalezeny"
            }))
          else
            filtered.map { item =>
              div(
                cls := "clipart-item",
                title := item.name,
                img(
                  src := item.svgData,
                  styleAttr := "width: 100%; height: 100%; object-fit: contain;",
                  draggable := false,
                ),
                onClick --> { _ =>
                  VisualEditorViewModel.addClipart(item.svgData)
                },
              )
            }
        },
      ),
    )
  }
}
