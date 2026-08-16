package mpbuilder.commons

/** A point in time as epoch milliseconds (UTC).
  *
  * Exists to stop raw `Long`s from standing in for instants across the whole codebase — a `Long`
  * parameter says nothing about whether it holds millis, seconds, or a duration.
  */
opaque type Timestamp = Long
object Timestamp:
  def apply(epochMillis: Long): Timestamp = epochMillis

  extension (t: Timestamp)
    def epochMillis: Long = t
    def isBefore(other: Timestamp): Boolean = t < other
    def isAfter(other: Timestamp): Boolean = t > other
    def plusMillis(millis: Long): Timestamp = t + millis
    def plusSeconds(seconds: Long): Timestamp = t + seconds * 1000L
    def plusMinutes(minutes: Long): Timestamp = t + minutes * 60_000L
    def plusHours(hours: Long): Timestamp = t + hours * 3_600_000L
    def plusDays(days: Long): Timestamp = t + days * 86_400_000L

  given Ordering[Timestamp] = Ordering.Long
