package mpbuilder.shared

import kyo.*

/** Non-empty, immutable sequence (accumulated validation errors, product components, workflow steps, …).
  *
  * A full `immutable.Seq`; operations that cannot empty it (`map`, `:+`, `+:`, `updated`, [[concat]]) keep the
  * `NonEmptyChunk` type, everything else (filter, init, tail, …) widens to `Seq` like ZIO's original did to `Chunk`.
  */
final class NonEmptyChunk[+A] private (private val underlying: Vector[A]) extends scala.collection.immutable.Seq[A]:
  override def apply(i: Int): A                                   = underlying(i)
  override def length: Int                                        = underlying.length
  override def iterator: Iterator[A]                              = underlying.iterator
  override def head: A                                            = underlying.head
  override def last: A                                            = underlying.last
  override def isEmpty: Boolean                                   = false
  override def toList: List[A]                                    = underlying.toList
  override def map[B](f: A => B): NonEmptyChunk[B]                = new NonEmptyChunk(underlying.map(f))
  override def appended[B >: A](elem: B): NonEmptyChunk[B]        = new NonEmptyChunk(underlying :+ elem)
  override def prepended[B >: A](elem: B): NonEmptyChunk[B]       = new NonEmptyChunk(elem +: underlying)
  override def updated[B >: A](index: Int, elem: B): NonEmptyChunk[B] = new NonEmptyChunk(underlying.updated(index, elem))
  override def zipWithIndex: NonEmptyChunk[(A @scala.annotation.unchecked.uncheckedVariance, Int)] =
    new NonEmptyChunk(underlying.zipWithIndex)
  def concat[B >: A](other: NonEmptyChunk[B]): NonEmptyChunk[B]   = new NonEmptyChunk(underlying ++ other.underlying)
  def toChunk: Chunk[A]                                           = Chunk.from(underlying)
  override def className: String                                  = "NonEmptyChunk"

object NonEmptyChunk:
  def apply[A](head: A, rest: A*): NonEmptyChunk[A] = new NonEmptyChunk(head +: rest.toVector)
  def single[A](a: A): NonEmptyChunk[A]             = new NonEmptyChunk(Vector(a))

  def fromIterableOption[A](it: Iterable[A]): Option[NonEmptyChunk[A]] =
    if it.isEmpty then None else Some(new NonEmptyChunk(it.toVector))

  def unapplySeq[A](nec: NonEmptyChunk[A]): Some[Seq[A]] = Some(nec)

/** Error-accumulating validation (the domain's applicative smart-constructor result).
  *
  * Kyo's `Result`/`Abort` short-circuit on the first failure; domain invariants (e.g. product configuration rules)
  * must report ALL violations at once, so this small type accumulates errors in a [[NonEmptyChunk]]. Bridge into
  * the Kyo effect world with [[Validation.toAbort]] / [[Validation.toResult]].
  */
sealed trait Validation[+E, +A]:
  import Validation.*

  final def map[B](f: A => B): Validation[E, B] = this match
    case Success(a)    => Success(f(a))
    case f0: Failure[E] => f0

  final def flatMap[E1 >: E, B](f: A => Validation[E1, B]): Validation[E1, B] = this match
    case Success(a)    => f(a)
    case f0: Failure[E] => f0

  final def as[B](b: => B): Validation[E, B] = map(_ => b)
  final def unit: Validation[E, Unit]        = map(_ => ())

  final def mapError[E2](f: E => E2): Validation[E2, A] = this match
    case s: Success[A] => s
    case Failure(errs) => Failure(errs.map(f))

  final def fold[B](onFailure: NonEmptyChunk[E] => B, onSuccess: A => B): B = this match
    case Success(a)    => onSuccess(a)
    case Failure(errs) => onFailure(errs)

  final def toEither: Either[NonEmptyChunk[E], A] = fold(Left(_), Right(_))
  final def toOption: Option[A]                   = fold(_ => None, Some(_))
  final def toResult: Result[NonEmptyChunk[E], A] = fold(Result.fail(_), Result.succeed(_))
  final def isSuccess: Boolean                    = this.isInstanceOf[Success[?]]
  final def isFailure: Boolean                    = !isSuccess

  final def getOrElse[A1 >: A](default: => A1): A1 = fold(_ => default, identity)

  /** Fails with the accumulated errors through Kyo's `Abort` effect. */
  final def toAbort(using Frame): A < Abort[NonEmptyChunk[E]] = fold(Abort.fail(_), a => a)

  /** Combines two validations, accumulating the errors of both. */
  final def zipWith[E1 >: E, B, C](that: Validation[E1, B])(f: (A, B) => C): Validation[E1, C] =
    (this, that) match
      case (Success(a), Success(b))   => Success(f(a, b))
      case (Failure(e1), Failure(e2)) => Failure(e1.concat(e2))
      case (Failure(e1), _)           => Failure(e1)
      case (_, Failure(e2))           => Failure(e2)

  final def zipPar[E1 >: E, B](that: Validation[E1, B]): Validation[E1, (A, B)] = zipWith(that)((_, _))
  final def zipRight[E1 >: E, B](that: Validation[E1, B]): Validation[E1, B]     = zipWith(that)((_, b) => b)
  final def zipLeft[E1 >: E, B](that: Validation[E1, B]): Validation[E1, A]      = zipWith(that)((a, _) => a)
  final def *>[E1 >: E, B](that: Validation[E1, B]): Validation[E1, B]           = zipRight(that)
  final def <*[E1 >: E, B](that: Validation[E1, B]): Validation[E1, A]           = zipLeft(that)
  final def <&>[E1 >: E, B](that: Validation[E1, B]): Validation[E1, (A, B)]     = zipPar(that)

object Validation:
  final case class Success[+A](value: A)                 extends Validation[Nothing, A]
  final case class Failure[+E](errors: NonEmptyChunk[E]) extends Validation[E, Nothing]

  def succeed[A](a: A): Validation[Nothing, A] = Success(a)
  def fail[E](e: E): Validation[E, Nothing]    = Failure(NonEmptyChunk.single(e))
  val unit: Validation[Nothing, Unit]          = Success(())

  def fromEither[E, A](either: Either[E, A]): Validation[E, A] = either.fold(fail, succeed)

  def fromOptionWith[E, A](error: => E)(opt: Option[A]): Validation[E, A] =
    opt.fold(fail(error))(succeed)

  /** Validates every element, accumulating all failures. */
  def validateAll[E, A](vs: Iterable[Validation[E, A]]): Validation[E, List[A]] =
    vs.foldRight[Validation[E, List[A]]](succeed(Nil))((v, acc) => v.zipWith(acc)(_ :: _))

  def validateWith[E, A, B, C](a: Validation[E, A], b: Validation[E, B])(f: (A, B) => C): Validation[E, C] =
    a.zipWith(b)(f)

  def validateWith[E, A, B, C, D](a: Validation[E, A], b: Validation[E, B], c: Validation[E, C])(
      f: (A, B, C) => D
  ): Validation[E, D] =
    a.zipPar(b).zipWith(c) { case ((x, y), z) => f(x, y, z) }

  def validateWith[E, A, B, C, D, R](a: Validation[E, A], b: Validation[E, B], c: Validation[E, C], d: Validation[E, D])(
      f: (A, B, C, D) => R
  ): Validation[E, R] =
    a.zipPar(b).zipPar(c).zipWith(d) { case (((w, x), y), z) => f(w, x, y, z) }

  def validateWith[E, A, B, C, D, F, R](
      a: Validation[E, A],
      b: Validation[E, B],
      c: Validation[E, C],
      d: Validation[E, D],
      e: Validation[E, F],
  )(f: (A, B, C, D, F) => R): Validation[E, R] =
    a.zipPar(b).zipPar(c).zipPar(d).zipWith(e) { case ((((v, w), x), y), z) => f(v, w, x, y, z) }
