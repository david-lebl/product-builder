package mpbuilder.httpclient

import kyo.*

/** JVM transport on kyo-http's client (non-2xx responses are returned, not raised). */
object PlatformHttpTransport extends HttpTransport:
  override def send(method: String, url: String, headers: Seq[(String, String)], body: Option[String])(using
      Frame
  ): HttpTransport.Response < Async =
    val payload = body.getOrElse("")
    val response = method match
      case "GET"    => HttpClient.getTextResponse(url, headers, failOnError = false)
      case "POST"   => HttpClient.postTextResponse(url, payload, headers, failOnError = false)
      case "PUT"    => HttpClient.putTextResponse(url, payload, headers, failOnError = false)
      case "PATCH"  => HttpClient.patchTextResponse(url, payload, headers, failOnError = false)
      case "DELETE" => HttpClient.deleteTextResponse(url, headers, failOnError = false)
      case other    => Abort.panic(new IllegalArgumentException(s"Unsupported HTTP method $other"))
    response.map(r => HttpTransport.Response(r.status.code, r.fields.body)).orPanic
