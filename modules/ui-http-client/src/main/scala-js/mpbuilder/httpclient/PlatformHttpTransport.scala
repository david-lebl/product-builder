package mpbuilder.httpclient

import kyo.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

/** Browser transport on `window.fetch` (kyo-http's Scala.js client targets Node sockets). */
object PlatformHttpTransport extends HttpTransport:
  override def send(method: String, url: String, headers: Seq[(String, String)], body: Option[String])(using
      Frame
  ): HttpTransport.Response < Async =
    val init = js.Dynamic.literal(
      method = method,
      headers = js.Dictionary(headers*),
      body = body.orUndefined,
    )
    Async.fromFuture {
      js.Dynamic.global.fetch(url, init).asInstanceOf[js.Promise[js.Dynamic]].toFuture.flatMap { res =>
        res.text().asInstanceOf[js.Promise[String]].toFuture.map(text => HttpTransport.Response(res.status.asInstanceOf[Int], text))
      }
    }
