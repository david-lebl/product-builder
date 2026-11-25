package mpbuilder.ui.configurator

import mpbuilder.catalog.*
import mpbuilder.shared.*
import mpbuilder.ui.components.ComboBox
import mpbuilder.ui.components.CommonComponents.*

/** Pure, cross-platform material search used by the configurator's material combo box.
  *
  * A query is split on whitespace into terms that must ALL match (AND):
  *   - text terms match (case- and diacritic-insensitive) anywhere in the material's name, description,
  *     family, surface, properties or weight — in both EN and CS, plus common synonyms (gloss/lesk, matt/mat…)
  *   - weight terms filter by grammage (g/m²): `200-300`, `>=250`, `>250`, `<200`, `<=170`
  *     (an optional `g` / `gsm` / `g/m2` suffix is accepted). Plain numbers such as `350` match as text,
  *     so they also find `350 g/m²` while the customer is still typing.
  */
object MaterialSearch:

  enum Term:
    case Text(value: String)
    case GsmRange(min: Int, max: Int)

  private val unitSuffix = "(?:g|gsm|g/m2|g/m²)?"
  private val RangeRe    = s"""(\\d{1,4})$unitSuffix[-–](\\d{1,4})$unitSuffix""".r
  private val CmpRe      = s"""(>=|<=|>|<)(\\d{1,4})$unitSuffix""".r

  def parse(query: String): List[Term] =
    // Join "200 - 300" style ranges before tokenizing
    val normalized = ComboBox.fold(query).replaceAll("""\s*[-–]\s*(?=\d)""", "-").replaceAll("""(>=|<=|>|<)\s+""", "$1")
    normalized.trim.split("\\s+").toList.filter(_.nonEmpty).map {
      case RangeRe(a, b) =>
        val (lo, hi) = (a.toInt, b.toInt)
        Term.GsmRange(lo.min(hi), lo.max(hi))
      case CmpRe(op, n) =>
        val v = n.toInt
        op match
          case ">=" => Term.GsmRange(v, Int.MaxValue)
          case ">"  => Term.GsmRange(v + 1, Int.MaxValue)
          case "<=" => Term.GsmRange(0, v)
          case _    => Term.GsmRange(0, v - 1)
      case other => Term.Text(other)
    }

  /** Text fragments of the query worth highlighting in labels (weight-range terms are not). */
  def highlightTerms(query: String): List[String] =
    parse(query).collect { case Term.Text(t) => t }

  private def surfaceSynonyms(c: SurfaceCoating): List[String] = c match
    case SurfaceCoating.CoatedGloss     => List("glossy", "coated", "lesk", "kridovy", "natirany")
    case SurfaceCoating.CoatedSilk      => List("satin", "coated", "kridovy", "saten", "natirany")
    case SurfaceCoating.CoatedMatte     => List("matt", "mat", "coated", "kridovy", "natirany")
    case SurfaceCoating.Uncoated        => List("offset", "bond", "natural", "prirodni")
    case SurfaceCoating.NaturalTextured => List("eco", "structure", "struktura")
    case SurfaceCoating.TransparentFilm => List("clear", "cirý", "pruhledny")
    case SurfaceCoating.Metallic        => List("metal", "kov")
    case SurfaceCoating.Glazed          => List("glaze", "glazura")

  private def weightTokens(w: MaterialWeight): List[String] = w match
    case MaterialWeight.Grammage(pw) =>
      List(s"${pw.gsm}g", s"${pw.gsm}gsm", s"${pw.gsm} g/m2", "gsm", "g/m2")
    case MaterialWeight.Thickness(mm, dens) =>
      val mmStr = mm.value.bigDecimal.stripTrailingZeros.toPlainString
      List(s"${mmStr}mm", s"$mmStr mm") ++ dens.toList.flatMap(d => List(s"${d.gsm}g", s"${d.gsm}gsm", s"${d.gsm} g/m2"))
    case MaterialWeight.UnitWeight(g) =>
      List(s"${g.value.bigDecimal.stripTrailingZeros.toPlainString} g")

  /** Folded, space-joined search haystack covering every searchable facet in all languages. */
  def haystack(m: Material): String =
    val langs = Language.values.toList
    val parts =
      langs.map(m.name(_)) ++
        m.description.toList.flatMap(d => langs.map(d(_))) ++
        langs.map(materialFamilyLabel(m.family, _)) ++
        langs.map(surfaceCoatingLabel(m.surfaceCoating, _)) ++
        surfaceSynonyms(m.surfaceCoating) ++
        m.properties.toList.flatMap(p => langs.map(materialPropertyLabel(p, _))) ++
        weightTokens(m.weight) ++
        List(formatMaterialWeight(m.weight), m.id.value)
    ComboBox.fold(parts.mkString(" "))

  def matches(m: Material, terms: List[Term]): Boolean =
    lazy val hay = haystack(m)
    terms.forall {
      case Term.Text(t)          => hay.contains(t)
      case Term.GsmRange(lo, hi) => m.weight.grammageGsm.exists(g => g >= lo && g <= hi)
    }

  /** Display ordering: family (catalog enum order) → surface → weight → localized name. */
  def displayOrder(materials: List[Material], lang: Language): List[Material] =
    materials.sortBy(m => (m.family.ordinal, m.surfaceCoating.ordinal, weightSortKey(m.weight), m.name(lang)))

  private def weightSortKey(w: MaterialWeight): BigDecimal = w match
    case MaterialWeight.Grammage(pw)       => BigDecimal(pw.gsm)
    case MaterialWeight.Thickness(mm, _)   => mm.value * 1000
    case MaterialWeight.UnitWeight(g)      => g.value

  def search(materials: List[Material], query: String, lang: Language): List[Material] =
    val terms = parse(query)
    displayOrder(if terms.isEmpty then materials else materials.filter(matches(_, terms)), lang)

  /** Same paper line in other grammages (same family + surface, name identical once weights are stripped),
    * so the configurator can offer one-click weight switching next to the combo box. Includes `m` itself.
    */
  def weightVariants(m: Material, pool: List[Material]): List[Material] =
    def stem(x: Material): String =
      ComboBox.fold(x.name(Language.En)).replaceAll("""\d+\s*(gsm|g/m2|g)?""", "").replaceAll("\\s+", " ").trim
    m.weight match
      case MaterialWeight.Grammage(_) =>
        val s = stem(m)
        (m :: pool.filter(x =>
          x.family == m.family && x.surfaceCoating == m.surfaceCoating &&
            x.weight.isInstanceOf[MaterialWeight.Grammage] && stem(x) == s
        ))
          .distinctBy(_.weight.grammageGsm)
          .sortBy(_.weight.grammageGsm.getOrElse(0))
      case _ => List(m)
end MaterialSearch
