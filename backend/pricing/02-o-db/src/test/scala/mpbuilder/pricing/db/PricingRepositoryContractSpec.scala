package mpbuilder.pricing
package db

import core.*
import mpbuilder.platform.PostgresTestSupport
import mpbuilder.platform.db.Database
import mpbuilder.pricing.sample.*
import mpbuilder.shared.*
import zio.*
import zio.test.*

object PricingRepositoryContractSpec extends ZIOSpecDefault:

  type Repos = PricelistRepository & CustomerAgreementRepository & PromoCodeRepository & CostSheetRepository

  val inMemory: ULayer[Repos] =
    PricingSampleLayer.pricelistRepoLayer(Map.empty) ++ PricingSampleLayer.agreementRepoLayer(Nil) ++
      PricingSampleLayer.promoRepoLayer(Nil) ++ PricingSampleLayer.costSheetRepoLayer(Map.empty)

  val postgres: URLayer[Database, Repos] =
    ZLayer.fromZIO(ZIO.serviceWithZIO[Database](PostgresTestSupport.truncateSchemas(_, List("pricing")))) >>> PricingDb.repositories

  def contract[R](label: String, fresh: ZLayer[R, Nothing, Repos]) = suite(label)(
    test("pricelists: one active per currency, missing currency fails") {
      for
        repo    <- ZIO.service[PricelistRepository]
        missing <- repo.findActive(Currency.USD).either
        _       <- repo.save(SamplePricelist.pricelistCzk)
        _       <- repo.save(SamplePricelist.pricelistCzkSheet)
        _       <- repo.save(SamplePricelist.pricelistUsd)
        czk     <- repo.findActive(Currency.CZK)
        all     <- repo.listAll
      yield assertTrue(missing.isLeft, czk == SamplePricelist.pricelistCzkSheet, all.size == 2)
    },
    test("agreements: upsert by (target, currency), lookup by customer & tier, delete") {
      for
        repo  <- ZIO.service[CustomerAgreementRepository]
        _     <- ZIO.foreachDiscard(SampleAgreements.all)(repo.save)
        all   <- repo.listAll
        first  = SampleAgreements.all.head
        _     <- repo.save(first.copy(globalDiscount = Some(Percentage.unsafe(BigDecimal(42)))))
        again <- repo.listAll
        byCust <- ZIO.foreach(SampleAgreements.all.flatMap(a => a.target match
                    case AgreementTarget.SpecificCustomer(c) => List((c, a.currency))
                    case _                                   => Nil
                  ))((c, cur) => repo.findForCustomer(c, cur))
        byTier <- ZIO.foreach(SampleAgreements.all.flatMap(a => a.target match
                    case AgreementTarget.TierDefault(t) => List((t, a.currency))
                    case _                              => Nil
                  ))((t, cur) => repo.findForTier(t, cur))
        _     <- repo.delete(first.target, first.currency)
        after <- repo.listAll
      yield assertTrue(
        all.toSet == SampleAgreements.all.toSet,
        again.size == all.size,
        again.exists(_.globalDiscount.contains(Percentage.unsafe(BigDecimal(42)))),
        byCust.forall(_.isDefined),
        byTier.forall(_.isDefined),
        after.size == all.size - 1,
      )
    },
    test("promo codes: case-insensitive lookup, atomic usage increment, replace by id, delete") {
      val promo = SamplePromoCodes.all.head
      for
        repo  <- ZIO.service[PromoCodeRepository]
        _     <- ZIO.foreachDiscard(SamplePromoCodes.all)(repo.save)
        byCode <- repo.findByCode("  " + promo.code.value.toLowerCase + " ")
        _     <- repo.incrementUsage(promo.id)
        _     <- repo.incrementUsage(promo.id)
        used  <- repo.findById(promo.id)
        renamed = promo.copy(code = NonEmptyString.unsafe("RENAMED-CODE"))
        _     <- repo.save(renamed)
        old   <- repo.findByCode(promo.code.value)
        newer <- repo.findByCode("renamed-code")
        _     <- repo.delete(promo.id)
        gone  <- repo.findById(promo.id)
        all   <- repo.listAll
      yield assertTrue(
        byCode.map(_.id).contains(promo.id),
        used.map(_.constraints.currentUses).contains(promo.constraints.currentUses + 2),
        old.isEmpty,
        newer.map(_.id).contains(promo.id),
        gone.isEmpty,
        all.size == SamplePromoCodes.all.size - 1,
      )
    },
    test("cost sheets: one per currency") {
      for
        repo <- ZIO.service[CostSheetRepository]
        _    <- repo.save(SampleCostSheets.costSheetUsd)
        _    <- repo.save(SampleCostSheets.costSheetCzk)
        usd  <- repo.findActive(Currency.USD)
        all  <- repo.listAll
      yield assertTrue(usd.contains(SampleCostSheets.costSheetUsd), all.size == 2)
    },
  ).provideSomeLayer[R](fresh)

  def spec = suite("Pricing repository contract")(
    contract("in-memory", inMemory),
    contract("postgres", postgres).provideShared(PostgresTestSupport.database(List(PricingDb.migrations))),
  ) @@ TestAspect.sequential
