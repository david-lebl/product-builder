package mpbuilder.app

import zio.http.*

import java.io.File

/** Serving the built single-page app.
  *
  * The SPA and the API have to share an origin: the basket now travels over HTTP, and a browser
  * will not send a request to a different host without CORS — which would mean maintaining a list
  * of allowed origins to work around a split that buys nothing in a modular monolith.
  *
  * One route per file that actually exists, rather than a catch-all under `/`. A catch-all is the
  * obvious way to write this and it is wrong here: it matches `GET /api/v1/baskets/current` too,
  * and the API starts answering every read with the HTML page. It also removes any question of
  * path traversal, because no request path is ever used to name a file.
  *
  * The listing is taken once, at start-up. A static bundle does not change while the server runs,
  * and rebuilding it means restarting anyway.
  */
object Assets:

  private val entryPoint = "index.html"

  def routes(root: Option[File]): Routes[Any, Response] = root match
    case None => Routes.empty
    case Some(directory) =>
      val files = Option(directory.listFiles()).toList.flatten.filter(_.isFile)

      val perFile = files.map(file => Method.GET / file.getName -> Handler.fromFile(file))

      // `/` is the only path the browser is ever sent to: the SPA's router keeps the current page
      // in memory and never writes it to the URL, so there are no deep links to serve.
      val index =
        files.find(_.getName == entryPoint).map(file => Method.GET / "" -> Handler.fromFile(file))

      Routes.fromIterable(perFile ++ index).sandbox
