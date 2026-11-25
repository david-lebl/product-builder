package mpbuilder.api
package server

import kyo.*
import mpbuilder.identity.SessionToken
import mpbuilder.shared.Validation

/** kyo-http building blocks shared by the BFF route modules (`02-i-http`).
  *
  * Facade errors travel as the [[ApiError]] envelope (`{"message": …, "error": <typed error>}`) with a status
  * derived from the error case ([[ApiConventions.statusOf]]). kyo-http maps one concrete error *type* to one
  * status, so each BFF route module declares one envelope case class per status (same wire shape).
  */
object ApiServer:

  /** Base path of every versioned API: `/api/v1`. */
  val api: HttpPath[Any] = "api" / ApiConventions.apiVersion

  /** Path / query codec for validated opaque types. */
  inline def opaqueCodec[A](inline decode: String => Validation[String, A], inline encode: A => String): HttpCodec[A] =
    HttpCodec[A](
      encode,
      raw => decode(raw).fold(errs => throw new IllegalArgumentException(errs.mkString("; ")), identity),
    )

  /** Path codec for singleton-case enums (by case name). */
  def enumCodec[E <: scala.reflect.Enum](values: Array[E]): HttpCodec[E] =
    HttpCodec[E](_.toString, raw => values.find(_.toString == raw).getOrElse(throw new IllegalArgumentException(s"unknown value '$raw'")))

  /** `Authorization: Bearer <token>` → session token (absent or malformed → `None`). */
  def bearerToken(header: Maybe[String]): Option[SessionToken] =
    header.toOption
      .map(_.trim)
      .filter(_.regionMatches(true, 0, "Bearer ", 0, 7))
      .flatMap(h => SessionToken(h.substring(7).trim).toOption)

  extension [In, Out, E](route: HttpRoute[In, Out, E])

    /** OpenAPI tag + summary. */
    def doc(tag: String, summary: String): HttpRoute[In, Out, E] =
      route.metadata(_.tag(tag).summary(summary))
