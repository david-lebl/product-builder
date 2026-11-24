package mpbuilder.json

import kyo.*
import mpbuilder.shared.{NonEmptyChunk, Validation}

/** Building blocks for the cross-compiled kyo-schema codecs.
  *
  * Domain cores stay free of serialization libraries: all `Schema` instances live here (and in the
  * per-context `02-o-db` adapters for `private[context]` aggregates). Opaque types are decoded
  * through their validating smart constructors, so neither HTTP requests nor database rows can
  * bypass domain invariants — a rejected value surfaces as a kyo-schema `ConstructorRejectedException`.
  */
object JsonSupport:

  private def rejected(typeName: String, reason: String)(using Frame): Nothing =
    throw ConstructorRejectedException(Seq.empty, typeName, reason)

  /** A schema for `A` stored as `R` on the wire, decoded through a validating smart constructor. */
  def validated[R, A](typeName: String, base: Schema[R])(decode: R => Validation[String, A])(encode: A => R)(using
      Frame
  ): Schema[A] =
    base.transform[A](raw => decode(raw).fold(errs => rejected(typeName, errs.mkString("; ")), identity))(encode)

  // ── Opaque value schemas ───────────────────────────────────────────────────

  def stringOpaque[A](typeName: String)(decode: String => Validation[String, A])(encode: A => String)(using Frame): Schema[A] =
    validated(typeName, Schema.stringSchema)(decode)(encode)

  def bigDecimalOpaque[A](typeName: String)(decode: BigDecimal => Validation[String, A])(encode: A => BigDecimal)(using
      Frame
  ): Schema[A] =
    validated(typeName, Schema.bigDecimalSchema)(decode)(encode)

  def intOpaque[A](typeName: String)(decode: Int => Validation[String, A])(encode: A => Int)(using Frame): Schema[A] =
    validated(typeName, Schema.intSchema)(decode)(encode)

  // ── Collections ────────────────────────────────────────────────────────────

  /** `NonEmptyChunk[A]` as a non-empty JSON array. */
  given nonEmptyChunkSchema[A](using inner: Schema[A], frame: Frame): Schema[NonEmptyChunk[A]] =
    Schema.listSchema[A](using inner).transform[NonEmptyChunk[A]](list =>
      NonEmptyChunk.fromIterableOption(list).getOrElse(rejected("NonEmptyChunk", "must contain at least one element"))
    )(_.toList)

  /** A `Map` whose keys have a string form, encoded as a JSON object (`{"<key>": value}`). */
  def keyedMap[K, V](typeName: String)(decodeKey: String => Validation[String, K])(encodeKey: K => String)(using
      values: Schema[V],
      frame: Frame,
  ): Schema[Map[K, V]] =
    Schema.stringMapSchema[V](using values).transform[Map[K, V]](raw =>
      raw.map((k, v) => decodeKey(k).fold(errs => rejected(typeName, errs.mkString("; ")), identity) -> v)
    )(_.map((k, v) => encodeKey(k) -> v))

  /** Key decoding for singleton-case Scala 3 enums used as `Map` keys. */
  def enumKey[E <: scala.reflect.Enum](typeName: String, values: Array[E]): String => Validation[String, E] =
    val byName = values.map(v => v.toString -> v).toMap
    raw => Validation.fromOptionWith(s"Unknown $typeName key '$raw'")(byName.get(raw))

  // ── Convenience ────────────────────────────────────────────────────────────

  def toJson[A](value: A)(using Schema[A], Frame): String = Json.encode(value)

  def fromJson[A](json: String)(using Schema[A], Frame): Either[String, A] =
    Json.decode[A](json).toEither.left.map(e => String.valueOf(e.getMessage))

  /** Decodes trusted persisted JSON, throwing (a defect) on malformed rows. */
  def fromJsonUnsafe[A](json: String)(using Schema[A], Frame): A =
    Json.decode[A](json).getOrThrow
