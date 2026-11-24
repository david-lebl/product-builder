package mpbuilder.pricing
package db

import core.*
import kyo.*
import kyo.test.*
import mpbuilder.platform.PostgresTestSupport
import mpbuilder.pricing.sample.*
import mpbuilder.shared.*

class PricingRepositoryContractSpec extends Test[Any]:

  private final case class Repos(
      pricelists: PricelistRepository,
      agreements: CustomerAgreementRepository,
      promoCodes: PromoCodeRepository,
      costSheets: CostSheetRepository,
  )

  private type Fresh = Repos < (Async & Scope & Abort[Throwable])

  private def inMemory: Fresh =
    for
      pl <- PricingSampleWiring.pricelistRepository(Map.empty)
      ag <- PricingSampleWiring.agreementRepository(Nil)
      pc <- PricingSampleWiring.promoRepository(Nil)
      cs <- PricingSampleWiring.costSheetRepository(Map.empty)
    yield Repos(pl, ag, pc, cs)

  private def postgres: Fresh =
    PostgresTestSupport.database(List(PricingDb.migrations)).map { db =>
      val r = PricingDb.repositories(db)
      Repos(r.pricelists, r.agreements, r.promoCodes, r.costSheets)
    }

  private def contract(label: String, fresh: => Fresh): Unit =
    label - {
      "pricelists: one active per currency, missing currency fails" in {
        for
          repo <- fresh.map(_.pricelists)
          missing <- repo.findActive(Currency.USD).either
          _       <- repo.save(SamplePricelist.pricelistCzk)
          _       <- repo.save(SamplePricelist.pricelistCzkSheet)
          _       <- repo.save(SamplePricelist.pricelistUsd)
          czk     <- repo.findActive(Currency.CZK)
          all     <- repo.listAll
        yield {
          assert(missing.isLeft)
          assert(czk == SamplePricelist.pricelistCzkSheet)
          assert(all.size == 2)
        }
      }

      "agreements: upsert by (target, currency), lookup by customer & tier, delete" in {
        for
          repo <- fresh.map(_.agreements)
          _     <- Kyo.foreachDiscard(SampleAgreements.all)(repo.save)
          all   <- repo.listAll
          first  = SampleAgreements.all.head
          _     <- repo.save(first.copy(globalDiscount = Some(Percentage.unsafe(BigDecimal(42)))))
          again <- repo.listAll
          byCust <- Kyo.foreach(SampleAgreements.all.flatMap(a => a.target match
                      case AgreementTarget.SpecificCustomer(c) => List((c, a.currency))
                      case _                                   => Nil
                    ))((c, cur) => repo.findForCustomer(c, cur))
          byTier <- Kyo.foreach(SampleAgreements.all.flatMap(a => a.target match
                      case AgreementTarget.TierDefault(t) => List((t, a.currency))
                      case _                              => Nil
                    ))((t, cur) => repo.findForTier(t, cur))
          _     <- repo.delete(first.target, first.currency)
          after <- repo.listAll
        yield {
          assert(all.toSet == SampleAgreements.all.toSet)
          assert(again.size == all.size)
          assert(again.exists(_.globalDiscount.contains(Percentage.unsafe(BigDecimal(42)))))
          assert(byCust.forall(_.isDefined))
          assert(byTier.forall(_.isDefined))
          assert(after.size == all.size - 1)
        }
      }

      "promo codes: case-insensitive lookup, atomic usage increment, replace by id, delete" in {
        val promo = SamplePromoCodes.all.head
        for
          repo <- fresh.map(_.promoCodes)
          _     <- Kyo.foreachDiscard(SamplePromoCodes.all)(repo.save)
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
        yield {
          assert(byCode.map(_.id).contains(promo.id))
          assert(used.map(_.constraints.currentUses).contains(promo.constraints.currentUses + 2))
          assert(old.isEmpty)
          assert(newer.map(_.id).contains(promo.id))
          assert(gone.isEmpty)
          assert(all.size == SamplePromoCodes.all.size - 1)
        }
      }

      "cost sheets: one per currency" in {
        for
          repo <- fresh.map(_.costSheets)
          _    <- repo.save(SampleCostSheets.costSheetUsd)
          _    <- repo.save(SampleCostSheets.costSheetCzk)
          usd  <- repo.findActive(Currency.USD)
          all  <- repo.listAll
        yield {
          assert(usd.contains(SampleCostSheets.costSheetUsd))
          assert(all.size == 2)
        }
      }

    }

  "Pricing repository contract" - {
    contract("in-memory", inMemory)
    contract("postgres", postgres)
  }
