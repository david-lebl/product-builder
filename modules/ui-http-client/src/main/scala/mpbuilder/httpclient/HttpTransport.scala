package mpbuilder.httpclient

import kyo.*
import mpbuilder.api.ApiError
import mpbuilder.api.ApiKernelCodecs.given
import mpbuilder.identity.SessionToken
import mpbuilder.json.JsonSupport

/** A raw HTTP exchange. Platform-specific: kyo-http's client on the JVM, `window.fetch` in the browser.
  * Transport failures (connection refused, …) are panics — the UI renders them as unexpected errors.
  */
trait HttpTransport:
  def send(method: String, url: String, headers: Seq[(String, String)], body: Option[String])(using Frame): HttpTransport.Response < Async

object HttpTransport:
  final case class Response(status: Int, body: String)

  /** The platform's default transport (see `scala-jvm` / `scala-js`). */
  def default: HttpTransport = PlatformHttpTransport

/** Calls one BFF of the `/api/v1` JSON API: JSON bodies via kyo-schema, `Authorization: Bearer` sessions and
  * the typed error envelope (`{"message": …, "error": E}`) decoded back into the facade error `E`.
  */
final class ApiCaller[E](transport: HttpTransport, baseUrl: String)(using errorSchema: Schema[E]):

  private val root = baseUrl.stripSuffix("/")

  def json[B](body: B)(using Schema[B], Frame): Option[String] = Some(JsonSupport.toJson(body))

  private def exchange(method: String, path: String, token: Option[SessionToken], body: Option[String])(using
      Frame
  ): HttpTransport.Response < (Async & Abort[E]) =
    val headers =
      Seq("Accept" -> "application/json") ++
        body.map(_ => "Content-Type" -> "application/json") ++
        token.map(t => "Authorization" -> s"Bearer ${t.value}")
    transport.send(method, root + path, headers, body).map { resp =>
      if resp.status >= 200 && resp.status < 300 then resp
      else
        JsonSupport.fromJson[ApiError[E]](resp.body) match
          case Right(envelope) => Abort.fail(envelope.error)
          case Left(_)         => Abort.panic(new RuntimeException(s"$method $path failed with HTTP ${resp.status}: ${resp.body.take(500)}"))
    }

  /** A call answering with a JSON body of type `A`. */
  def call[A](method: String, path: String, token: Option[SessionToken] = None, body: Option[String] = None)(using
      Schema[A],
      Frame,
  ): A < (Async & Abort[E]) =
    exchange(method, path, token, body).map { resp =>
      JsonSupport.fromJson[A](resp.body) match
        case Right(a)  => a
        case Left(err) => Abort.panic(new RuntimeException(s"$method $path: undecodable response ($err)"))
    }

  /** A call answering `204 No Content`. */
  def callUnit(method: String, path: String, token: Option[SessionToken] = None, body: Option[String] = None)(using
      Frame
  ): Unit < (Async & Abort[E]) =
    exchange(method, path, token, body).unit
