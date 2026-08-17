package mpbuilder.commons

/** Base for every domain error ADT.
  *
  * Domain errors are data, not exceptions: each context defines a sealed ADT of the ways its
  * operations can fail, and the compiler forces callers to handle them exhaustively. Every error
  * must be able to explain itself to a user in any supported [[Language]]; the English rendering is
  * derived, so no ADT has to repeat it.
  */
trait DomainError:
  def message(lang: Language): String
  final def message: String = message(Language.En)

/** One thing wrong with a request, in a form any caller can render or key off.
  *
  * `code` is stable and machine-readable (`"MaterialNotFound"`); `message` is already localized, so
  * a caller in another bounded context can surface it without knowing the first thing about the
  * rules that produced it. This is the shape every context reports validation failures in — it is
  * an error-reporting convention, not a business concept, which is why it lives in the kernel.
  */
final case class Problem(code: String, message: LocalizedString)

object Problem:
  def apply(code: String, en: String, cs: String): Problem =
    Problem(code, LocalizedString(en, cs))
