package mpbuilder.commons

/** A value that may be unavailable — carrying *why* when it is.
  *
  * Prefer this to `Option` wherever the absence is itself meaningful to the caller. The standalone
  * calculator, for example, deliberately shows no completion dates; "no date" and "no date, because
  * this is an indicative quote rather than a scheduled order" are different facts, and only the
  * second one can be rendered as a disclaimer.
  */
enum Estimated[+A]:
  case Known(value: A)
  case Unavailable(reason: Estimated.Reason)

object Estimated:

  /** Why an estimate could not be produced. */
  enum Reason:
    /** The host deliberately does not promise dates (e.g. the embeddable calculator). */
    case IndicativeOnly

    /** No shop schedule / working hours are configured for this host. */
    case ScheduleUnknown

    /** The inputs needed for the estimate are incomplete. */
    case InsufficientData(what: String)

  def known[A](value: A): Estimated[A] = Known(value)
  def unavailable(reason: Reason): Estimated[Nothing] = Unavailable(reason)

  extension [A](e: Estimated[A])
    def toOption: Option[A] = e match
      case Known(value) => Some(value)
      case Unavailable(_) => None

    def map[B](f: A => B): Estimated[B] = e match
      case Known(value)      => Known(f(value))
      case Unavailable(why)  => Unavailable(why)

    def getOrElse[B >: A](default: => B): B = e match
      case Known(value)   => value
      case Unavailable(_) => default

    def reason: Option[Reason] = e match
      case Known(_)         => None
      case Unavailable(why) => Some(why)
