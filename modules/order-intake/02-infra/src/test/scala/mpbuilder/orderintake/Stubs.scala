package mpbuilder.orderintake

import mpbuilder.catalog as cat
import mpbuilder.catalog.json.given
import mpbuilder.commons.*
import mpbuilder.customers as cus
import mpbuilder.pricing as pri
import zio.*
import zio.json.*

/** Stand-ins for the catalog and pricing contexts.
  *
  * Order-intake's core cannot see either implementation — `LegacyCatalogService` is
  * `private[catalog]`, and neither `domain` nor the other contexts' infra modules are on this
  * module's classpath. That is the boundary working, not an obstacle: this context is specified
  * against its dependencies' *contracts*, so these tests keep passing when those implementations
  * are replaced in Phases 7–8.
  *
  * Integration against the real services is verified where it belongs — in `app`, over HTTP.
  *
  * The stubs are deliberately faithful on the points the tests turn on: unknown ids are rejected,
  * withdrawn products stop revalidating, and price is non-linear in quantity so "re-priced" can be
  * told apart from "multiplied".
  */
object Stubs:

  /** @param withdrawn
    *   materials this catalog will not configure or revalidate at all
    * @param withdrawnLater
    *   materials that configure cleanly but no longer revalidate — the shape of time passing
    *   between adding something to a basket and checking out with it
    */
  final case class StubCatalog(
      withdrawn: Set[String] = Set.empty,
      withdrawnLater: Set[String] = Set.empty,
  ) extends cat.CatalogService:

    def currentVersion: UIO[cat.CatalogVersion] = ZIO.succeed(cat.CatalogVersion("stub-v1"))

    def configure(request: cat.ConfigurationRequestDto): IO[cat.CatalogError, cat.ConfigurationView] =
      val problems = List(
        Option.when(!request.categoryId.startsWith("cat-"))(
          Problem("CategoryNotFound", s"No category '${request.categoryId}'", s"Kategorie '${request.categoryId}' neexistuje")
        ),
        Option.when(request.components.isEmpty)(
          Problem("NoComponents", "At least one component is required", "Je vyžadována alespoň jedna komponenta")
        ),
        request.components
          .find(c => withdrawn.contains(c.materialId))
          .map(c => Problem("MaterialNotFound", s"No material '${c.materialId}'", s"Materiál '${c.materialId}' neexistuje")),
      ).flatten

      NonEmptyChunk.fromIterableOption(problems) match
        case Some(found) => ZIO.fail(cat.CatalogError.Rejected(found))
        case None =>
          // The snapshot embeds the request, so it stays self-contained and its fingerprint
          // changes exactly when the configuration does.
          ZIO.succeed(
            cat.ConfigurationView(
              snapshot = cat.ConfigurationSnapshot(request.toJson, cat.CatalogVersion("stub-v1")),
              description = LocalizedString(
                s"${request.specifications.quantity.getOrElse(1)}× Business Cards",
                s"${request.specifications.quantity.getOrElse(1)}× Vizitky",
              ),
            )
          )

    def revalidate(snapshot: cat.ConfigurationSnapshot): IO[cat.CatalogError, Unit] =
      ZIO
        .fromEither(snapshot.payload.fromJson[cat.ConfigurationRequestDto])
        .orElseFail(cat.CatalogError.MalformedSnapshot("unreadable"))
        .flatMap(request =>
          copy(withdrawn = withdrawn ++ withdrawnLater).configure(request)
        )
        .unit

    def describe(snapshot: cat.ConfigurationSnapshot, lang: Language): IO[cat.CatalogError, String] =
      ZIO.succeed("Business Cards")

  final case class StubPricing(unitPrice: BigDecimal = BigDecimal(10)) extends pri.PricingService:

    def quote(request: pri.QuoteRequest): IO[pri.PricingError, pri.PriceQuote] =
      ZIO
        .fromEither(request.spec.payload.fromJson[cat.ConfigurationRequestDto])
        .orElseFail(pri.PricingError.MalformedSpec("unreadable"))
        .flatMap { config =>
          if request.currency != Currency.CZK && request.currency != Currency.USD then
            ZIO.fail(pri.PricingError.NoPricelist(request.currency))
          else
            val quantity = config.specifications.quantity.getOrElse(1)
            // Deliberately non-linear: a volume discount above 100, so a test can tell
            // "re-quoted at the new quantity" apart from "old quote times the new quantity".
            val discount = if quantity > 100 then BigDecimal("0.8") else BigDecimal(1)
            val speedFactor = request.speed match
              case pri.ProductionSpeed.Express  => BigDecimal("1.35")
              case pri.ProductionSpeed.Standard => BigDecimal(1)
              case pri.ProductionSpeed.Economy  => BigDecimal("0.85")
            val total = Money(unitPrice * quantity * discount * speedFactor)
            ZIO.succeed(
              pri.PriceQuote(
                components = Nil,
                surcharges = Nil,
                setupFees = Nil,
                subtotal = total,
                quantityMultiplier = discount,
                speedSurcharge = None,
                minimumApplied = None,
                total = total,
                currency = request.currency,
                quantity = quantity,
                pricelistVersion = "stub-pricelist-v1",
              )
            )
        }

    def quoteAll(requests: List[pri.QuoteRequest]): IO[pri.PricingError, pri.BasketQuote] =
      ZIO.foreach(requests)(quote).map { quotes =>
        pri.BasketQuote(
          quotes,
          quotes.map(_.total).foldLeft(Money.zero)(_ + _),
          quotes.headOption.map(_.currency).getOrElse(Currency.CZK),
        )
      }

    def speedOffers(
        spec: pri.ProductSpec,
        currency: Currency,
    ): IO[pri.PricingError, NonEmptyChunk[pri.SpeedOffer]] =
      ZIO.succeed(
        NonEmptyChunk(
          pri.SpeedOffer.Available(pri.ProductionSpeed.Standard, Money.zero),
          // Unavailable rather than merely expensive, so a test can prove the reason survives the
          // boundary instead of being flattened into "not offered".
          pri.SpeedOffer.Unavailable(pri.ProductionSpeed.Express, pri.SpeedUnavailable.ShopSaturated),
          // A negative surcharge — waiting costs less. Non-zero so a test can tell a per-run
          // figure apart from a per-line one, and signed so the sign has to survive too.
          pri.SpeedOffer.Available(pri.ProductionSpeed.Economy, Money(-5)),
        )
      )

    /** Deliberately opinionated so the checkout tests can exercise all three shapes of answer
      * without a real discount engine: `TENOFF` takes 10 % off, `FREESHIP` waives delivery, and
      * `VIPONLY` is accepted only for a buyer with a customer id — the closest thing to a
      * customer-restricted code that a stub can honestly offer.
      */
    def applyDiscount(
        code: String,
        context: pri.DiscountContext,
    ): IO[pri.PricingError, pri.DiscountOutcome] =
      val orderValue = context.orderValue
      code.trim.toUpperCase match
        case "TENOFF" =>
          val off = (orderValue * BigDecimal("0.1")).rounded
          ZIO.succeed(
            pri.DiscountOutcome
              .Applied(code, pri.DiscountBenefit.Amount(off), Money(orderValue.value - off.value))
          )
        case "FREESHIP" =>
          ZIO.succeed(
            pri.DiscountOutcome.Applied(code, pri.DiscountBenefit.FreeDelivery, orderValue)
          )
        case "VIPONLY" if context.customerId.isDefined =>
          ZIO.succeed(
            pri.DiscountOutcome.Applied(
              code,
              pri.DiscountBenefit.Amount(Money(100)),
              Money(orderValue.value - 100),
            )
          )
        case _ =>
          ZIO.succeed(
            pri.DiscountOutcome.Refused(code, LocalizedString("No such code", "Kód neexistuje"))
          )

  /** Stand-in for the customers context.
    *
    * Carries one approved corporate customer and one that is not approved for invoicing, because
    * the payment rules turn on exactly that distinction.
    */
  final case class StubCustomers(customers: Map[String, cus.CustomerSummary] = StubCustomers.sample)
      extends cus.CustomerService:

    def find(id: String): IO[cus.CustomerError, Option[cus.CustomerSummary]] =
      ZIO.succeed(customers.get(id))

    def findBy(identifier: cus.Identifier): IO[cus.CustomerError, Option[cus.CustomerSummary]] =
      val email = identifier match
        case cus.Identifier.Email(value) => Some(value)
        case _                           => None
      ZIO.succeed(email.flatMap(e => customers.values.find(_.email == e)))

    def register(input: cus.RegisterCustomer): IO[cus.CustomerError, cus.CustomerSummary] =
      ZIO.fail(cus.CustomerError.EmailAlreadyRegistered(input.email))

  object StubCustomers:
    val approvedId = "cust-approved"
    val unapprovedId = "cust-unapproved"

    val approved: cus.CustomerSummary = cus.CustomerSummary(
      id = approvedId,
      displayName = "Approved Corp",
      email = "buyer@approved.example",
      company = Some(cus.CompanyDetails("Approved Corp", Some("10203040"), None)),
      customerType = "RegisteredCorporate",
      status = "Active",
      tier = "Gold",
      isActive = true,
      canPayOnAccount = true,
    )

    val unapproved: cus.CustomerSummary = approved.copy(
      id = unapprovedId,
      displayName = "Pending Ltd",
      email = "buyer@pending.example",
      customerType = "Agency",
      tier = "Standard",
      canPayOnAccount = false,
    )

    val sample: Map[String, cus.CustomerSummary] =
      Map(approvedId -> approved, unapprovedId -> unapproved)
