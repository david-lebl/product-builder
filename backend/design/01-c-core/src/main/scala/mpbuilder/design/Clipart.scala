package mpbuilder.design

/** Built-in vector decorations. Path data is authored in a 100×100 box and stretched to the element frame,
  * so the editor, thumbnails and the PDF renderer all draw identical vectors (no raster clip-art to license).
  */
enum ClipartKind derives CanEqual:
  case Heart, Star, Sparkle, Circle, Arrow, Check, Leaf, Sun, Snowflake, SpeechBubble, Crown, Flower, Badge, Wave

object Clipart:

  private def fmt(v: Double): String =
    val r = math.round(v * 100.0) / 100.0
    if r == r.toLong.toDouble then r.toLong.toString else r.toString

  private def polygon(points: Seq[(Double, Double)]): String =
    points.zipWithIndex.map { case ((x, y), i) => s"${if i == 0 then "M" else "L"}${fmt(x)} ${fmt(y)}" }.mkString(" ") + " Z"

  private def starPoints(points: Int, outer: Double, inner: Double, cx: Double = 50, cy: Double = 50): Seq[(Double, Double)] =
    (0 until points * 2).map { i =>
      val r     = if i % 2 == 0 then outer else inner
      val angle = math.Pi * i / points - math.Pi / 2
      (cx + r * math.cos(angle), cy + r * math.sin(angle))
    }

  private def circlePath(cx: Double, cy: Double, r: Double): String =
    s"M${fmt(cx - r)} ${fmt(cy)} A${fmt(r)} ${fmt(r)} 0 1 0 ${fmt(cx + r)} ${fmt(cy)} A${fmt(r)} ${fmt(r)} 0 1 0 ${fmt(cx - r)} ${fmt(cy)} Z"

  /** SVG path data (100×100 box, filled with the element colour, even-odd rule). */
  def path(kind: ClipartKind): String = kind match
    case ClipartKind.Heart =>
      "M50 88 C22 68 4 52 4 31 C4 16 15 6 29 6 C38 6 46 11 50 19 C54 11 62 6 71 6 C85 6 96 16 96 31 C96 52 78 68 50 88 Z"
    case ClipartKind.Star     => polygon(starPoints(5, 48, 20))
    case ClipartKind.Sparkle  => polygon(starPoints(4, 48, 10))
    case ClipartKind.Circle   => circlePath(50, 50, 46)
    case ClipartKind.Arrow    => "M4 40 L62 40 L62 18 L96 50 L62 82 L62 60 L4 60 Z"
    case ClipartKind.Check    => "M8 54 L22 40 L40 58 L80 16 L94 30 L40 86 Z"
    case ClipartKind.Leaf     => "M10 90 C10 40 40 10 92 8 C90 60 60 90 10 90 Z M18 82 C38 62 56 44 80 20 L84 24 C60 48 42 66 22 86 Z"
    case ClipartKind.Sun =>
      val rays = (0 until 12).map { i =>
        val a  = math.Pi * 2 * i / 12
        val a1 = a - 0.12
        val a2 = a + 0.12
        polygon(Seq((50 + 30 * math.cos(a1), 50 + 30 * math.sin(a1)), (50 + 48 * math.cos(a), 50 + 48 * math.sin(a)), (50 + 30 * math.cos(a2), 50 + 30 * math.sin(a2))))
      }
      (circlePath(50, 50, 24) +: rays).mkString(" ")
    case ClipartKind.Snowflake =>
      (0 until 6).map { i =>
        val a  = math.Pi * i / 3
        val dx = math.cos(a)
        val dy = math.sin(a)
        val nx = -dy * 3
        val ny = dx * 3
        polygon(Seq((50 + nx, 50 + ny), (50 + dx * 46 + nx, 50 + dy * 46 + ny), (50 + dx * 46 - nx, 50 + dy * 46 - ny), (50 - nx, 50 - ny)))
      }.mkString(" ") + " " + circlePath(50, 50, 9)
    case ClipartKind.SpeechBubble =>
      "M14 8 L86 8 C92 8 96 12 96 18 L96 62 C96 68 92 72 86 72 L40 72 L20 92 L24 72 L14 72 C8 72 4 68 4 62 L4 18 C4 12 8 8 14 8 Z"
    case ClipartKind.Crown => "M6 80 L12 26 L32 52 L50 14 L68 52 L88 26 L94 80 Z M6 86 L94 86 L94 94 L6 94 Z"
    case ClipartKind.Flower =>
      val petals = (0 until 6).map { i =>
        val a = math.Pi * 2 * i / 6
        circlePath(50 + 26 * math.cos(a), 50 + 26 * math.sin(a), 18)
      }
      (petals :+ circlePath(50, 50, 13)).mkString(" ")
    case ClipartKind.Badge => polygon(starPoints(16, 48, 40))
    case ClipartKind.Wave =>
      "M0 50 C12 34 25 34 37 50 C50 66 62 66 75 50 C87 34 100 34 100 34 L100 66 C88 66 87 82 75 82 C62 82 50 66 37 66 C25 66 12 82 0 66 Z"

  /** Whether the shape is drawn with the even-odd rule (cut-outs). */
  def evenOdd(kind: ClipartKind): Boolean = kind == ClipartKind.Leaf
