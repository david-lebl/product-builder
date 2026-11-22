package mpbuilder.json

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import zio.{Chunk, NonEmptyChunk}
import zio.prelude.Validation

/** Building blocks for the cross-compiled jsoniter-scala codecs.
  *
  * Domain cores stay free of serialization libraries: all codecs live here (and in the
  * per-context `02-o-db` adapters for `private[context]` aggregates). Opaque types are
  * decoded through their validating smart constructors, so neither HTTP requests nor
  * database rows can bypass domain invariants.
  */
object JsonSupport:

  /** Shared derivation config.
    *   - recursive ADTs (rule predicates) allowed
    *   - defaults/empties are always written, so persisted JSON stays explicit and stable
    *   - missing fields with defaults decode to the default (painless schema evolution)
    *   - the ADT discriminator may appear anywhere in an object: PostgreSQL `JSONB` re-orders keys
    */
  inline def config: CodecMakerConfig =
    CodecMakerConfig
      .withAllowRecursiveTypes(true)
      .withTransientDefault(false)
      .withTransientEmpty(false)
      .withRequireDefaultFields(false)
      .withMapMaxInsertNumber(65536)
      .withSetMaxInsertNumber(65536)
      .withDiscriminatorFieldName(Some("type"))
      .withRequireDiscriminatorFirst(false)

  private def fail(in: JsonReader, typeName: String, errors: NonEmptyChunk[String]): Nothing =
    in.decodeError(s"Invalid $typeName: ${errors.mkString("; ")}")

  // ── Opaque value codecs ────────────────────────────────────────────────────

  def stringCodec[A](typeName: String)(decode: String => Validation[String, A])(encode: A => String): JsonValueCodec[A] =
    new JsonValueCodec[A]:
      override def decodeValue(in: JsonReader, default: A): A =
        val raw = in.readString(null)
        if raw == null then in.decodeError(s"$typeName must not be null")
        decode(raw).toEither match
          case Right(a)   => a
          case Left(errs) => fail(in, typeName, errs)
      override def encodeValue(x: A, out: JsonWriter): Unit = out.writeVal(encode(x))
      override def nullValue: A                             = null.asInstanceOf[A]

  def bigDecimalCodec[A](typeName: String)(decode: BigDecimal => Validation[String, A])(encode: A => BigDecimal): JsonValueCodec[A] =
    new JsonValueCodec[A]:
      override def decodeValue(in: JsonReader, default: A): A =
        val raw = in.readBigDecimal(null)
        if raw == null then in.decodeError(s"$typeName must not be null")
        decode(raw).toEither match
          case Right(a)   => a
          case Left(errs) => fail(in, typeName, errs)
      override def encodeValue(x: A, out: JsonWriter): Unit = out.writeVal(encode(x))
      override def nullValue: A                             = null.asInstanceOf[A]

  def intCodec[A](typeName: String)(decode: Int => Validation[String, A])(encode: A => Int): JsonValueCodec[A] =
    new JsonValueCodec[A]:
      override def decodeValue(in: JsonReader, default: A): A =
        decode(in.readInt()).toEither match
          case Right(a)   => a
          case Left(errs) => fail(in, typeName, errs)
      override def encodeValue(x: A, out: JsonWriter): Unit = out.writeVal(encode(x))
      override def nullValue: A                             = null.asInstanceOf[A]

  def stringKeyCodec[A](typeName: String)(decode: String => Validation[String, A])(encode: A => String): JsonKeyCodec[A] =
    new JsonKeyCodec[A]:
      override def decodeKey(in: JsonReader): A =
        decode(in.readKeyAsString()).toEither match
          case Right(a)   => a
          case Left(errs) => fail(in, typeName, errs)
      override def encodeKey(x: A, out: JsonWriter): Unit = out.writeKey(encode(x))

  /** Key codec for simple (singleton-case) Scala 3 enums used as `Map` keys. */
  def enumKeyCodec[E <: scala.reflect.Enum](typeName: String, values: Array[E]): JsonKeyCodec[E] =
    val byName = values.map(v => v.toString -> v).toMap
    new JsonKeyCodec[E]:
      override def decodeKey(in: JsonReader): E =
        val raw = in.readKeyAsString()
        byName.getOrElse(raw, in.decodeError(s"Unknown $typeName key '$raw'"))
      override def encodeKey(x: E, out: JsonWriter): Unit = out.writeKey(x.toString)

  // ── Collections ────────────────────────────────────────────────────────────

  /** `NonEmptyChunk[A]` as a non-empty JSON array (built from a derived `List[A]` codec). */
  def nonEmptyChunkCodec[A](listCodec: JsonValueCodec[List[A]], typeName: String): JsonValueCodec[NonEmptyChunk[A]] =
    new JsonValueCodec[NonEmptyChunk[A]]:
      override def decodeValue(in: JsonReader, default: NonEmptyChunk[A]): NonEmptyChunk[A] =
        listCodec.decodeValue(in, Nil) match
          case head :: tail => NonEmptyChunk(head, tail*)
          case Nil          => in.decodeError(s"$typeName must contain at least one element")
      override def encodeValue(x: NonEmptyChunk[A], out: JsonWriter): Unit =
        listCodec.encodeValue(x.toList, out)
      override def nullValue: NonEmptyChunk[A] = null

  def chunkCodec[A](listCodec: JsonValueCodec[List[A]]): JsonValueCodec[Chunk[A]] =
    new JsonValueCodec[Chunk[A]]:
      override def decodeValue(in: JsonReader, default: Chunk[A]): Chunk[A] =
        Chunk.fromIterable(listCodec.decodeValue(in, Nil))
      override def encodeValue(x: Chunk[A], out: JsonWriter): Unit =
        listCodec.encodeValue(x.toList, out)
      override def nullValue: Chunk[A] = Chunk.empty

  // ── Convenience ────────────────────────────────────────────────────────────

  private val compactWriter = WriterConfig

  def toJson[A](value: A)(using codec: JsonValueCodec[A]): String =
    writeToString(value, compactWriter)

  def fromJson[A](json: String)(using codec: JsonValueCodec[A]): Either[String, A] =
    try Right(readFromString[A](json))
    catch case e: JsonReaderException => Left(e.getMessage)

  /** Decodes trusted persisted JSON, dying (defect) on malformed rows. */
  def fromJsonUnsafe[A](json: String)(using codec: JsonValueCodec[A]): A =
    readFromString[A](json)
