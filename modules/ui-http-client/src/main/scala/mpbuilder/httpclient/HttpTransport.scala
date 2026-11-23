package mpbuilder.httpclient

import sttp.client4.{Backend, DefaultFutureBackend}
import sttp.model.Uri
import sttp.tapir.PublicEndpoint
import sttp.tapir.client.sttp4.SttpClientInterpreter
import zio.*

import scala.concurrent.{ExecutionContext, Future}

/** Executes Tapir endpoint contracts over sttp (Fetch on Scala.js, java.net.http on the JVM).
  *
  * Typed facade errors (`StorefrontError` / `BackofficeError`) decode back from the wire envelope;
  * transport failures and undecodable responses surface as defects (the UI renders them as panics).
  */
final class HttpTransport(
    val baseUri: Uri,
    backend: Backend[Future],
)(using ec: ExecutionContext):

  private val interpreter = SttpClientInterpreter()

  def call[I, E, O](e: PublicEndpoint[I, E, O, Any])(input: I): IO[E, O] =
    ZIO
      .fromFuture(_ => interpreter.toClientThrowDecodeFailures(e, Some(baseUri), backend).apply(input))
      .orDie
      .absolve

object HttpTransport:
  def apply(baseUrl: String)(using ec: ExecutionContext = ExecutionContext.global): HttpTransport =
    new HttpTransport(Uri.unsafeParse(baseUrl.stripSuffix("/")), DefaultFutureBackend())
