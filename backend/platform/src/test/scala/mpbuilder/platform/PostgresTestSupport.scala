package mpbuilder.platform

import kyo.*
import mpbuilder.platform.db.*

import java.util.concurrent.atomic.AtomicInteger

/** Shared PostgreSQL fixture for `02-o-db` contract tests and app E2E tests.
  *
  * By default one throwaway PostgreSQL 17 container per test JVM is started with kyo-pod (lazily,
  * removed on JVM exit). Set `TEST_DATABASE_URL` (e.g. `postgresql://user:pw@127.0.0.1:5432/postgres`,
  * a role allowed to `CREATE DATABASE`) to use an existing server instead — handy in CI or when the
  * local Docker VM cannot forward published ports.
  *
  * Every [[database]] call creates a brand-new, freshly migrated database (dropped again on JVM exit),
  * so tests are isolated without paying a container start each.
  */
object PostgresTestSupport:

  val image    = "postgres:17-alpine"
  val user     = "printcraft"
  val password = "printcraft"

  final case class Server(host: String, port: Int, user: String, password: String):
    def config(database: String): DbConfig = DbConfig(s"postgresql://$host:$port/$database", user, password, maxPoolSize = 4)

  private val dbCounter = new AtomicInteger(0)
  private val runId     = java.lang.Long.toString(java.lang.System.nanoTime() & 0xffffffffL, 36)
  private val created   = new java.util.concurrent.ConcurrentLinkedQueue[String]()

  /** `TEST_DATABASE_URL` → an existing server (credentials in the URL's user-info). */
  private def externalServer: Option[Server] =
    sys.env.get("TEST_DATABASE_URL").map(_.trim).filter(_.nonEmpty).map { raw =>
      val uri          = new java.net.URI(raw.stripPrefix("jdbc:"))
      val (usr, pwd)   = Option(uri.getRawUserInfo).map(_.split(":", 2)) match
        case Some(Array(u, p)) => (java.net.URLDecoder.decode(u, "UTF-8"), java.net.URLDecoder.decode(p, "UTF-8"))
        case Some(Array(u))    => (java.net.URLDecoder.decode(u, "UTF-8"), "")
        case _                 => (user, password)
      Server(uri.getHost, if uri.getPort > 0 then uri.getPort else 5432, usr, pwd)
    }

  private def startServer(using Frame): Server < (Async & Abort[Throwable]) =
    val config = Container.Config(image)
      .env("POSTGRES_USER", user)
      .env("POSTGRES_PASSWORD", password)
      .env("POSTGRES_DB", "postgres")
      .port(5432)
      .healthCheck(Container.HealthCheck.exec(Command("pg_isready", "-U", user, "-h", "127.0.0.1"), Absent))
    for
      container <- Container.initUnscoped(config)
      _         <- container.awaitHealthy
      port      <- container.mappedPort(5432)
      _         <- Sync.defer {
                     java.lang.Runtime.getRuntime.addShutdownHook(new Thread(() =>
                       import AllowUnsafe.embrace.danger
                       val _ = KyoApp.Unsafe.runAndBlock(30.seconds)(container.remove(force = true))
                     ))
                   }
      server     = Server("127.0.0.1", port, user, password)
      _         <- awaitAcceptingConnections(server)
    yield server

  /** The image's init scripts restart PostgreSQL once; retry until a real query succeeds. */
  private def awaitAcceptingConnections(server: Server)(using Frame): Unit < (Async & Abort[Throwable]) =
    Retry[Throwable](Schedule.fixed(500.millis).take(60)) {
      Scope.run {
        SqlClient.init(server.config("postgres").connectionUrl).map(c => DB.run(c)(DB.executeRaw("SELECT 1")).unit)
      }
    }

  private def admin(srv: Server, statement: String)(using Frame): Unit < (Async & Abort[Throwable]) =
    Scope.run {
      SqlClient.init(srv.config("postgres").connectionUrl).map(c => DB.run(c)(DB.executeRaw(statement)).unit)
    }

  /** The JVM-wide server (external, or a container started on first use). */
  lazy val server: Server =
    import AllowUnsafe.embrace.danger
    val srv = externalServer.getOrElse(KyoApp.Unsafe.runAndBlock(5.minutes)(startServer).getOrThrow)
    java.lang.Runtime.getRuntime.addShutdownHook(new Thread(() =>
      created.forEach { name =>
        val _ = KyoApp.Unsafe.runAndBlock(30.seconds)(admin(srv, s"DROP DATABASE IF EXISTS $name WITH (FORCE)"))
      }
    ))
    srv

  /** Creates a brand-new empty database and returns its connection settings. */
  def freshDatabaseConfig(using Frame): DbConfig < (Async & Abort[Throwable]) =
    for
      srv <- Sync.defer(server)
      name = s"printcraft_test_${runId}_${dbCounter.incrementAndGet()}"
      _   <- admin(srv, s"CREATE DATABASE $name")
      _   <- Sync.defer(created.add(name))
    yield srv.config(name)

  /** A fresh database with `sets` migrated, closed with the enclosing `Scope`. */
  def database(sets: List[MigrationSet])(using Frame): Database < (Async & Scope & Abort[Throwable]) =
    freshDatabaseConfig.map(cfg => Database.init(cfg, sets).mapAbort(e => new RuntimeException(e.message)))
