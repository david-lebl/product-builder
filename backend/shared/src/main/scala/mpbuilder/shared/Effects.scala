package mpbuilder.shared

import kyo.*

/** Small Kyo combinators shared by every bounded context (kept deliberately tiny). */
extension [A](opt: Option[A])
  /** Lifts an `Option` into `Abort`, failing with `error` when empty. */
  inline def orFail[E](inline error: => E)(using inline frame: Frame): A < Abort[E] =
    opt match
      case Some(a) => a
      case None    => Abort.fail(error)

extension [A, S](effect: A < S)
  /** Replaces the result of `effect` with `value` (ZIO's `as`). */
  inline def as[B](inline value: => B)(using inline frame: Frame): B < S = effect.map(_ => value)

extension [A, S](effect: Option[A] < S)
  /** Fails with `error` when the effect yields `None`. */
  def someOrFail[E](error: => E)(using Frame): A < (S & Abort[E]) =
    effect.map {
      case Some(a) => a
      case None    => Abort.fail(error)
    }

extension [A](ref: AtomicRef[A])
  /** Atomically updates the value, discarding the result (ZIO's `Ref.update`). */
  inline def update(inline f: A => A)(using inline frame: Frame): Unit < Sync = ref.updateAndGet(f).unit

  /** Atomically computes a result and the next value in one step (ZIO's `Ref.modify`). */
  def modify[B](f: A => (B, A))(using Frame): B < Sync =
    Sync.defer {
      @scala.annotation.tailrec
      def loop(): B =
        import AllowUnsafe.embrace.danger
        val current      = ref.unsafe.get()
        val (out, next) = f(current)
        if ref.unsafe.compareAndSet(current, next) then out else loop()
      loop()
    }

extension [E, A](either: Either[E, A])
  /** Lifts an `Either` into `Abort`. */
  inline def orAbort(using inline frame: Frame): A < Abort[E] =
    either match
      case Right(a) => a
      case Left(e)  => Abort.fail(e)

extension [A, S, E](effect: A < (Abort[E] & S))
  /** Handles `Abort[E]`, exposing the outcome as an `Either` (panics keep propagating). */
  def either(using ConcreteTag[E], Frame): Either[E, A] < (S & Abort[Nothing]) =
    Abort.runPartial[E](effect).map(_.toEitherPartial)

  /** Handles `Abort[E]`, discarding both the failure and the result (panics keep propagating). */
  def ignore(using ConcreteTag[E], Frame): Unit < (S & Abort[Nothing]) =
    Abort.runPartial[E](effect).unit

  /** Handles `Abort[E]`, turning a failure into `None` (panics keep propagating). */
  def orNone(using ConcreteTag[E], Frame): Option[A] < (S & Abort[Nothing]) =
    Abort.runPartial[E](effect).map(_.toEitherPartial.toOption)

object Effects:

  /** Fails with `error` when `cond` holds. */
  inline def failWhen[E](cond: Boolean)(inline error: => E)(using inline frame: Frame): Unit < Abort[E] =
    if cond then Abort.fail(error) else ()

  /** Fails with `error` unless `cond` holds. */
  inline def failUnless[E](cond: Boolean)(inline error: => E)(using inline frame: Frame): Unit < Abort[E] =
    if cond then () else Abort.fail(error)

  /** Sequentially traverses an `Option`. */
  def traverse[A, B, S](opt: Option[A])(f: A => B < S)(using Frame): Option[B] < S =
    opt match
      case Some(a) => f(a).map(Some(_))
      case None    => None

  /** Sequentially traverses any collection, preserving its type and order. */
  def traverse[CC[+X] <: Iterable[X] & scala.collection.IterableOps[X, CC, CC[X]], A, B, S](source: CC[A])(f: A => B < S)(using
      Frame
  ): CC[B] < S =
    Kyo.foreach(source)(f)

  /** Runs `f` for every element, discarding results. */
  def foreachDiscard[A, S](items: Iterable[A])(f: A => Any < S)(using Frame): Unit < S =
    Kyo.foreachDiscard(items.toList)(f)

  /** Runs an effect only when `cond` holds. */
  def when[S](cond: Boolean)(f: => Any < S)(using Frame): Unit < S =
    if cond then f.unit else ()

  /** Current wall-clock time as a `java.time.Instant` (domain models use `java.time`). */
  def now(using Frame): java.time.Instant < Sync =
    Clock.now.map(_.toJava)

