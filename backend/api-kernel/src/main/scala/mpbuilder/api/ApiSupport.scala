package mpbuilder.api

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import mpbuilder.identity.SessionToken
import mpbuilder.shared.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.jsoniter.*
import zio.prelude.Validation

import scala.reflect.ClassTag

/** Shared Tapir building blocks: auth inputs, opaque path codecs, typed error outputs. */
object ApiSupport:

  val apiVersion = "v1"

  // ── Auth: `Authorization: Bearer <SessionToken>` ───────────────────────────

  val bearer: EndpointInput[SessionToken] =
    auth.bearer[String]().mapDecode(raw => fromValidation(SessionToken(raw)))(_.value)
      .description("Session token returned by a login endpoint")

  val optionalBearer: EndpointInput[Option[SessionToken]] =
    auth.bearer[Option[String]]().map(_.flatMap(raw => SessionToken(raw).toOption))(_.map(_.value))
      .description("Optional customer session token (enables B2B pricing & prefill)")

  // ── Opaque-type path/query codecs (validated through smart constructors) ───

  def fromValidation[A](v: Validation[String, A]): DecodeResult[A] =
    v.toEither match
      case Right(a)   => DecodeResult.Value(a)
      case Left(errs) => DecodeResult.Error(errs.mkString("; "), new IllegalArgumentException(errs.mkString("; ")))

  def opaqueCodec[A](decode: String => Validation[String, A])(encode: A => String)(using Schema[A]): Codec[String, A, CodecFormat.TextPlain] =
    Codec.string.mapDecode(raw => fromValidation(decode(raw)))(encode).schema(summon[Schema[A]])

  def enumCodec[E <: scala.reflect.Enum](values: Array[E])(using Schema[E]): Codec[String, E, CodecFormat.TextPlain] =
    Codec.string.mapDecode(raw =>
      values.find(_.toString == raw) match
        case Some(e) => DecodeResult.Value(e)
        case None    => DecodeResult.Mismatch(values.mkString("|"), raw)
    )(_.toString)

  // ── JSON bodies (jsoniter codecs + named OpenAPI schemas) ─────────────────

  def body[T: JsonValueCodec: ClassTag]: EndpointIO.Body[String, T] =
    jsonBody[T](using summon, ApiSchemas.namedOf[T])

  def jsonList[T: ClassTag](using JsonValueCodec[List[T]]): EndpointIO.Body[String, List[T]] =
    jsonBody[List[T]](using summon, ApiSchemas.listOf[T])

  def jsonNamed[T: JsonValueCodec](name: String): EndpointIO.Body[String, T] =
    jsonBody[T](using summon, ApiSchemas.named[T](name))

  // ── Typed error outputs ────────────────────────────────────────────────────

  /** Maps a facade error to its HTTP status by inspecting the (possibly wrapped) error case name. */
  def statusOf(error: Product): StatusCode =
    val name = error.productPrefix
    val nested = error.productIterator.collectFirst {
      case p: Product if error.productArity == 1 && p.getClass.getName.contains("Error") => p
    }
    nested match
      case Some(inner) => statusOf(inner)
      case None =>
        if Set("Unauthenticated", "InvalidCredentials", "SessionExpired", "SessionNotFound", "SessionRevoked").exists(name.contains) then StatusCode.Unauthorized
        else if Set("Forbidden", "NotOwned", "NotACustomerSession", "NotAnEmployeeSession", "AccountNotActive").exists(name.contains) then StatusCode.Forbidden
        else if name.contains("NotFound") then StatusCode.NotFound
        else if Set("Already", "InUse", "Conflict", "Exists", "Locked").exists(name.contains) then StatusCode.Conflict
        else StatusCode.BadRequest

  /** `<status> + {"message": "...", "error": {...}}`; decodes back to the exact typed error on the client. */
  def typedErrorOut[E <: Product](message: E => String, name: String)(using JsonValueCodec[ApiError[E]]): EndpointOutput[E] =
    val envelope = ApiSchemas.named[ApiError[E]](
      s"${name}Envelope",
      s"Error envelope: `message` (human readable) + `error` (typed `$name`, discriminated by `type`)",
    )
    statusCode
      .and(jsonBody[ApiError[E]](using summon, envelope))
      .map[E]((_: StatusCode, env: ApiError[E]) => env.error)((e: E) => (statusOf(e), ApiError(message(e), e)))
