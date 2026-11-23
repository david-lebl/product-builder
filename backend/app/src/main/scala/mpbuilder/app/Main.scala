package mpbuilder.app

import mpbuilder.app.http.HttpServer
import zio.*

/** Composition root: `./mill backend.app.run --storage=memory|postgres`. */
object Main extends ZIOAppDefault:

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    for
      args <- ZIOAppArgs.getArgs
      cfg  <- AppConfig.load(args).tapError(msg => Console.printLineError(msg).orDie)
      _    <- ZIO.logInfo(s"Starting with storage=${cfg.storage}, port=${cfg.http.port}")
      _    <- ZIO
                .serviceWithZIO[AppRuntime] { rt =>
                  HttpServer.serve(
                    HttpServer.serverEndpoints(rt.storefront, rt.backoffice, rt.blobs, rt.storageLabel, rt.readiness),
                    cfg.http,
                  )
                }
                .provide(AppRuntime.fromConfig(cfg))
    yield ()
