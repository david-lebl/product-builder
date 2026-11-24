package mpbuilder.app

import kyo.*
import mpbuilder.app.http.ApiHttpServer

/** Composition root: `./mill backend.app.run --storage=memory|postgres`. */
object Main extends KyoApp:

  run {
    for
      cfg    <- AppConfig.load(args)
      _      <- Log.info(s"Starting with storage=${cfg.storage}, blobs=${cfg.blobStorage.label}, port=${cfg.http.port}")
      rt     <- AppRuntime.fromConfig(cfg)
      server <- ApiHttpServer.start(
                  ApiHttpServer.handlers(rt.storefront, rt.backoffice, rt.blobs, rt.storageLabel, rt.readiness()),
                  cfg.http,
                )
      _      <- server.await
    yield ()
  }
