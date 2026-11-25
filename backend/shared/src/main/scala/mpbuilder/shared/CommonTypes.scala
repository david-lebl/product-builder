package mpbuilder.shared


enum Language:
  case En, Cs

  def code: String = this match
    case En => "en"
    case Cs => "cs"

object Language:
  def fromCode(code: String): Validation[String, Language] =
    code.trim.toLowerCase match
      case "en" => Validation.succeed(En)
      case "cs" => Validation.succeed(Cs)
      case other => Validation.fail(s"Unsupported language code: '$other'")

opaque type LocalizedString = Map[Language, String]
object LocalizedString:
  def apply(en: String): LocalizedString =
    Map(Language.En -> en)

  def apply(en: String, cs: String): LocalizedString =
    Map(Language.En -> en, Language.Cs -> cs)

  def fromMap(translations: Map[Language, String]): Validation[String, LocalizedString] =
    if translations.values.exists(_.trim.nonEmpty) then Validation.succeed(translations)
    else Validation.fail("LocalizedString must contain at least one non-empty translation")

  extension (ls: LocalizedString)
    def apply(lang: Language): String =
      ls.getOrElse(lang, ls.getOrElse(Language.En, ls.values.headOption.getOrElse("")))

    def toMap: Map[Language, String] = ls

opaque type NonEmptyString = String
object NonEmptyString:
  def apply(raw: String): Validation[String, NonEmptyString] =
    val trimmed = raw.trim
    if trimmed.nonEmpty then Validation.succeed(trimmed)
    else Validation.fail("String must not be empty")

  def unsafe(raw: String): NonEmptyString = raw

  extension (s: NonEmptyString) def value: String = s

opaque type EmailAddress = String
object EmailAddress:
  private val emailRegex = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$".r

  def apply(raw: String): Validation[String, EmailAddress] =
    val normalized = raw.trim.toLowerCase
    if emailRegex.matches(normalized) then Validation.succeed(normalized)
    else Validation.fail(s"Invalid email address: '$raw'")

  def unsafe(raw: String): EmailAddress = raw.trim.toLowerCase

  extension (e: EmailAddress) def value: String = e

opaque type Version = String
object Version:
  def apply(raw: String): Validation[String, Version] =
    val trimmed = raw.trim
    if trimmed.nonEmpty then Validation.succeed(trimmed)
    else Validation.fail("Version must not be empty")

  def unsafe(raw: String): Version = raw

  extension (v: Version) def value: String = v

/** Customer-facing production turnaround speed tier (used by ordering, pricing, manufacturing). */
enum ManufacturingSpeed:
  case Express, Standard, Economy

/** Commercial customer tier (owned by customers, referenced by pricing agreements). */
enum CustomerTier:
  case Standard, Silver, Gold, Platinum

/** Customer legal/commercial segment. */
enum CustomerSegment:
  case Individual, Business, Agency

final case class Address(
    street: NonEmptyString,
    city: NonEmptyString,
    postalCode: NonEmptyString,
    countryCode: NonEmptyString,
)
