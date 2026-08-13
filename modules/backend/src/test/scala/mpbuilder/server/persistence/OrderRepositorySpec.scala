package mpbuilder.server.persistence

import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sql.DataSource

import com.dimafeng.testcontainers.PostgreSQLContainer
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import mpbuilder.api.OrderStatus
import mpbuilder.domain.*
import mpbuilder.domain.catalog.ComponentRole
import mpbuilder.domain.config.*
import mpbuilder.domain.pricing.PricingEngine
import mpbuilder.domain.sample.*
import mpbuilder.domain.sample.SampleIds.{category as cat, ink as inks, material as mat, method as met}
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.utility.DockerImageName
import zio.*
import zio.test.*

/** JSONB round-trip against a real Postgres (testcontainers). Requires a
  * running Docker daemon; excluded from the default test run via the
  * `integration` tag when Docker is unavailable.
  */
object OrderRepositorySpec extends ZIOSpecDefault:

  private val containerLayer: ZLayer[Any, Throwable, DataSource] =
    ZLayer.scoped {
      ZIO
        .acquireRelease(ZIO.attemptBlocking {
          val c = PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
          c.start()
          c
        })(c => ZIO.attemptBlocking(c.stop()).orDie)
        .map { c =>
          val ds = new PGSimpleDataSource()
          ds.setURL(c.jdbcUrl)
          ds.setUser(c.username)
          ds.setPassword(c.password)
          ds: DataSource
        }
        .tap(ds => Migrations.run.provide(ZLayer.succeed(ds)))
    }

  private val repoLayer: ZLayer[Any, Throwable, OrderRepository] =
    containerLayer >>> (Quill.Postgres.fromNamingStrategy(SnakeCase) >>> OrderRepository.live)

  private val sampleOrder: Order =
    val config = SamplePresets.presets.find(_.categoryId == cat.businessCards).get.configuration
    val breakdown =
      PricingEngine.price(SampleCatalog.catalog, SamplePricelistCzk.pricelist, config).toOption.get
    Order(
      id = UUID.randomUUID(),
      createdAt = java.time.Instant.now().truncatedTo(ChronoUnit.MICROS), // Postgres keeps µs precision
      contact = CustomerContact("Jana Nováková", "jana@example.com", "+420 777 888 999", None),
      configuration = config,
      breakdown = breakdown,
      status = OrderStatus.Placed,
    )

  def spec = suite("OrderRepository (Postgres)")(
    test("insert → byId round-trips the full order incl. JSONB config and breakdown") {
      for
        _       <- OrderRepository.insert(sampleOrder)
        fetched <- OrderRepository.byId(sampleOrder.id)
        listed  <- OrderRepository.list(10)
      yield assertTrue(
        fetched.contains(sampleOrder),
        listed.exists(_.id == sampleOrder.id),
        listed.find(_.id == sampleOrder.id).exists(_.totalAmount == sampleOrder.totalAmount),
      )
    }
  ).provideShared(repoLayer) @@ TestAspect.tag("integration") @@ TestAspect.withLiveClock
