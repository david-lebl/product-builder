package mpbuilder.domain.service

/**
 * Predefined discount codes mapped to their percentage (integer, 5–50).
 * A code is looked up case-insensitively.
 */
object DiscountService:

  /** All valid discount codes and their percentage off (5–50 %). */
  val validCodes: Map[String, Int] = Map(
    "SAVE5"     ->  5,
    "SAVE10"    -> 10,
    "SAVE15"    -> 15,
    "SAVE20"    -> 20,
    "SAVE25"    -> 25,
    "SAVE30"    -> 30,
    "SAVE40"    -> 40,
    "SAVE50"    -> 50,
    "WELCOME20" -> 20,
    "SUMMER10"  -> 10,
    "PRINT15"   -> 15,
  )

  /**
   * Returns the discount percentage for the given code, or `None` if the
   * code is empty or not recognised.  Lookup is case-insensitive.
   */
  def lookupPercent(code: String): Option[Int] =
    if code.trim.isEmpty then None
    else validCodes.get(code.trim.toUpperCase)
