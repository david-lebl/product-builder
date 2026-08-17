package mpbuilder.catalog

import mpbuilder.commons.*
import zio.NonEmptyChunk

enum CatalogError extends DomainError:

  /** The request named things that do not exist, or asked for a combination the compatibility
    * rules forbid. Carries *every* problem found, not just the first — the configurator shows them
    * all at once.
    */
  case Rejected(problems: NonEmptyChunk[Problem])

  /** A stored snapshot could not be read back. This means stored data is corrupt or was written by
    * an incompatible version — never a user error.
    */
  case MalformedSnapshot(detail: String)

  /** A DTO field carried a value outside its allowed set (e.g. `role = "sideways"`). */
  case UnknownValue(field: String, value: String)

  def message(lang: Language): String = this match
    case Rejected(problems) =>
      problems.map(_.message(lang)).mkString("; ")
    case MalformedSnapshot(detail) =>
      lang match
        case Language.En => s"Stored configuration could not be read: $detail"
        case Language.Cs => s"Uloženou konfiguraci nelze přečíst: $detail"
    case UnknownValue(field, value) =>
      lang match
        case Language.En => s"Unsupported value '$value' for $field"
        case Language.Cs => s"Nepodporovaná hodnota '$value' pro $field"

object CatalogError:
  def rejected(first: Problem, rest: Problem*): CatalogError =
    Rejected(NonEmptyChunk(first, rest*))
