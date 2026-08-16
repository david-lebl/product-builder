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
