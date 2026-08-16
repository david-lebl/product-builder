package mpbuilder.commons

enum Language:
  case En, Cs

  def toCode: String = this match
    case En => "en"
    case Cs => "cs"

object Language:
  def fromCode(code: String): Language =
    code.toLowerCase match
      case "cs" => Cs
      case _    => En

/** A string with a translation per [[Language]], falling back to English. */
opaque type LocalizedString = Map[Language, String]

object LocalizedString:
  def apply(en: String): LocalizedString =
    Map(Language.En -> en)

  def apply(en: String, cs: String): LocalizedString =
    Map(Language.En -> en, Language.Cs -> cs)

  def apply(translations: Map[Language, String]): LocalizedString =
    translations

  extension (ls: LocalizedString)
    def apply(lang: Language): String =
      ls.getOrElse(lang, ls.getOrElse(Language.En, ""))

    def value: String = apply(Language.En)
