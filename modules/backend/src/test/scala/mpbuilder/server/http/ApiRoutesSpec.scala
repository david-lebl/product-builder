package mpbuilder.server.http

import java.util.UUID

import mpbuilder.api.*
import mpbuilder.domain.*
import mpbuilder.domain.catalog.ComponentRole
import mpbuilder.domain.config.*
import mpbuilder.domain.sample.SampleIds.{category as cat, ink as inks, material as mat, method as met}
import mpbuilder.server.persistence.{Order, OrderRepository}
import mpbuilder.server.service.OrderService
import zio.*
import zio.http.*
import zio.json.*
import zio.test.*

object ApiRoutesSpec extends ZIOSpecDefault:

  private final class InMemoryRepo(ref: Ref[Map[UUID, Order]]) extends OrderRepository:
    def insert(order: Order): Task[Unit]  = ref.update(_ + (order.id -> order))
    def byId(id: UUID): Task[Option[Order]] = ref.get.map(_.get(id))
    def list(limit: Int): Task[List[OrderSummary]] =
      ref.get.map(
        _.values.toList
          .sortBy(_.createdAt)
          .reverse
          .take(limit)
          .map(o =>
            OrderSummary(o.id, o.createdAt, o.contact.name, "x", o.totalAmount, o.currency, o.status)
          )
      )

  private val repoLayer: ULayer[OrderRepository] =
    ZLayer(Ref.make(Map.empty[UUID, Order]).map(InMemoryRepo(_)))

  private val env = repoLayer >>> OrderService.live

  private val validRequest = CreateOrderRequest(
    contact = CustomerContact("Jan Novák", "jan@example.com", "+420 123 456 789", Some("Example s.r.o.")),
    configuration = ProductConfiguration(
      categoryId = cat.businessCards,
      printingMethodId = met.digital,
      inkConfigurationId = inks.fullColorBoth,
      components = List(
        ComponentConfiguration(ComponentRole.Main, mat.coatedMatte(350), List(SelectedFinish(mpbuilder.domain.sample.SampleIds.finish.matteLamination)))
      ),
      details = ProductDetails(size = Some(DimensionsMm(85, 55)), quantity = Some(200)),
    ),
  )

  private def post(request: CreateOrderRequest) =
    Request.post("/api/orders", Body.fromString(request.toJson))

  def spec = suite("ApiRoutes")(

    test("POST /api/orders with a valid configuration returns 201 with the authoritative breakdown") {
      for
        response <- ApiRoutes.routes.runZIO(post(validRequest))
        body     <- response.body.asString
        parsed = body.fromJson[OrderResponse]
      yield assertTrue(
        response.status == Status.Created,
        parsed.exists(_.status == OrderStatus.Placed),
        parsed.exists(_.breakdown.total.amount > 0),
        parsed.exists(_.breakdown.currency == Currency.CZK),
      )
    },

    test("a placed order can be fetched back by id and appears in the queue list") {
      for
        created  <- ApiRoutes.routes.runZIO(post(validRequest))
        id       <- created.body.asString.map(_.fromJson[OrderResponse].toOption.get.id)
        fetched  <- ApiRoutes.routes.runZIO(Request.get(s"/api/orders/$id"))
        list     <- ApiRoutes.routes.runZIO(Request.get("/api/orders"))
        listBody <- list.body.asString
      yield assertTrue(
        fetched.status == Status.Ok,
        list.status == Status.Ok,
        listBody.fromJson[List[OrderSummary]].exists(_.exists(_.id == id)),
      )
    },

    test("POST with an invalid configuration returns 422 carrying every accumulated error") {
      val invalid = validRequest.copy(
        contact = CustomerContact("", "not-an-email", ""),
        configuration = validRequest.configuration.copy(
          printingMethodId = met.screenPrinting, // not allowed for business cards
          details = ProductDetails(size = Some(DimensionsMm(85, 55)), quantity = None), // missing quantity
        ),
      )
      for
        response <- ApiRoutes.routes.runZIO(post(invalid))
        body     <- response.body.asString
        errors = body.fromJson[ApiError].map(_.errors).getOrElse(Nil)
      yield assertTrue(
        response.status == Status.UnprocessableEntity,
        errors.exists(_.code == "PrintingMethodNotAllowed"),
        errors.exists(_.code == "MissingDetail"),
        errors.exists(_.code == "ContactInvalid"),
        errors.size >= 5, // method + quantity + 3 contact fields
      )
    },

    test("POST with malformed JSON returns 400") {
      for response <- ApiRoutes.routes.runZIO(
          Request.post("/api/orders", Body.fromString("{not json"))
        )
      yield assertTrue(response.status == Status.BadRequest)
    },

    test("GET /api/catalog returns the full catalog bundle") {
      for
        response <- ApiRoutes.routes.runZIO(Request.get("/api/catalog"))
        body     <- response.body.asString
        parsed = body.fromJson[CatalogResponse]
      yield assertTrue(
        response.status == Status.Ok,
        parsed.exists(_.catalog.categories.size == 15),
        parsed.exists(_.presets.nonEmpty),
        parsed.exists(_.pricelist.currency == Currency.CZK),
      )
    },

    test("GET /api/orders/:id with an unknown id returns 404") {
      for response <- ApiRoutes.routes.runZIO(
          Request.get(s"/api/orders/${UUID.randomUUID()}")
        )
      yield assertTrue(response.status == Status.NotFound)
    },
  ).provide(env)
