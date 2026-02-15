package mpbuilder.domain.model

enum Language:
  case En, Cs

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
