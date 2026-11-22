package mpbuilder.pricing
package db

import com.augustnagro.magnum.*
import core.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.json.JsonSupport.*
import mpbuilder.platform.db.{Database, MigrationSet}
import mpbuilder.shared.*
import zio.*

private[pricing] object PricingDbKeys:
  def target(t: AgreementTarget): (String, String) = t match
    case AgreementTarget.SpecificCustomer(id) => ("SpecificCustomer", id.value)
    case AgreementTarget.TierDefault(tier)    => ("TierDefault", tier.toString)

  def normalizeCode(raw: String): String = raw.trim.toUpperCase

import PricingDbKeys.*

private[pricing] final class PostgresPricelistRepository(db: Database) extends PricelistRepository:

  override def findActive(currency: Currency): IO[PricingError.PricelistNotFound, Pricelist] =
    db.run(sql"SELECT doc::text FROM pricing.pricelists WHERE currency = ${currency.toString}".query[String].run().headOption)
      .someOrFail(new PricingError.PricelistNotFound(currency))
      .map(fromJsonUnsafe[Pricelist])

  override def listAll: UIO[List[Pricelist]] =
    db.run(sql"SELECT doc::text FROM pricing.pricelists ORDER BY currency".query[String].run().toList.map(fromJsonUnsafe[Pricelist]))

  override def save(p: Pricelist): UIO[Unit] =
    val json = toJson(p)
    db.tx(
      sql"""INSERT INTO pricing.pricelists (currency, id, version, doc)
            VALUES (${p.currency.toString}, ${p.id.value}, ${p.version.value}, $json::jsonb)
            ON CONFLICT (currency) DO UPDATE SET
              id = EXCLUDED.id, version = EXCLUDED.version, doc = EXCLUDED.doc, updated_at = now()""".update.run()
    ).unit

private[pricing] final class PostgresCustomerAgreementRepository(db: Database) extends CustomerAgreementRepository:

  private def find(kind: String, ref: String, currency: Currency): UIO[Option[CustomerPriceAgreement]] =
    db.run(
      sql"""SELECT doc::text FROM pricing.customer_agreements
            WHERE target_kind = $kind AND target_ref = $ref AND currency = ${currency.toString}"""
        .query[String].run().headOption.map(fromJsonUnsafe[CustomerPriceAgreement])
    )

  override def findForCustomer(customerId: CustomerId, currency: Currency): UIO[Option[CustomerPriceAgreement]] =
    find("SpecificCustomer", customerId.value, currency)

  override def findForTier(tier: CustomerTier, currency: Currency): UIO[Option[CustomerPriceAgreement]] =
    find("TierDefault", tier.toString, currency)

  override def listAll: UIO[List[CustomerPriceAgreement]] =
    db.run(
      sql"SELECT doc::text FROM pricing.customer_agreements ORDER BY position"
        .query[String].run().toList.map(fromJsonUnsafe[CustomerPriceAgreement])
    )

  override def save(a: CustomerPriceAgreement): UIO[Unit] =
    val (kind, ref) = target(a.target)
    val json        = toJson(a)
    db.tx(
      sql"""INSERT INTO pricing.customer_agreements (target_kind, target_ref, currency, doc)
            VALUES ($kind, $ref, ${a.currency.toString}, $json::jsonb)
            ON CONFLICT (target_kind, target_ref, currency) DO UPDATE SET doc = EXCLUDED.doc, updated_at = now()"""
        .update.run()
    ).unit

  override def delete(t: AgreementTarget, currency: Currency): UIO[Unit] =
    val (kind, ref) = target(t)
    db.tx(
      sql"""DELETE FROM pricing.customer_agreements
            WHERE target_kind = $kind AND target_ref = $ref AND currency = ${currency.toString}""".update.run()
    ).unit

private[pricing] final class PostgresPromoCodeRepository(db: Database) extends PromoCodeRepository:

  override def findByCode(code: String): UIO[Option[PromoCode]] =
    db.run(
      sql"SELECT doc::text FROM pricing.promo_codes WHERE code_upper = ${normalizeCode(code)}"
        .query[String].run().headOption.map(fromJsonUnsafe[PromoCode])
    )

  override def findById(id: DiscountCodeId): UIO[Option[PromoCode]] =
    db.run(
      sql"SELECT doc::text FROM pricing.promo_codes WHERE id = ${id.value}"
        .query[String].run().headOption.map(fromJsonUnsafe[PromoCode])
    )

  override def listAll: UIO[List[PromoCode]] =
    db.run(sql"SELECT doc::text FROM pricing.promo_codes ORDER BY code_upper".query[String].run().toList.map(fromJsonUnsafe[PromoCode]))

  /** Mirrors the in-memory semantics: a promo replaces any existing one with the same id OR code. */
  override def save(p: PromoCode): UIO[Unit] =
    val code = normalizeCode(p.code.value)
    val json = toJson(p)
    db.tx {
      sql"DELETE FROM pricing.promo_codes WHERE id = ${p.id.value} OR code_upper = $code".update.run()
      sql"""INSERT INTO pricing.promo_codes (id, code_upper, is_active, doc)
            VALUES (${p.id.value}, $code, ${p.isActive}, $json::jsonb)""".update.run()
      ()
    }

  override def delete(id: DiscountCodeId): UIO[Unit] =
    db.tx(sql"DELETE FROM pricing.promo_codes WHERE id = ${id.value}".update.run()).unit

  /** Atomic, race-free redemption counter (no read-modify-write in the application). */
  override def incrementUsage(codeId: DiscountCodeId): UIO[Unit] =
    db.tx(
      sql"""UPDATE pricing.promo_codes
            SET doc = jsonb_set(doc, '{constraints,currentUses}',
                                to_jsonb(COALESCE((doc #>> '{constraints,currentUses}')::int, 0) + 1)),
                updated_at = now()
            WHERE id = ${codeId.value}""".update.run()
    ).unit

private[pricing] final class PostgresCostSheetRepository(db: Database) extends CostSheetRepository:

  override def findActive(currency: Currency): UIO[Option[ProductionCostSheet]] =
    db.run(
      sql"SELECT doc::text FROM pricing.cost_sheets WHERE currency = ${currency.toString}"
        .query[String].run().headOption.map(fromJsonUnsafe[ProductionCostSheet])
    )

  override def listAll: UIO[List[ProductionCostSheet]] =
    db.run(sql"SELECT doc::text FROM pricing.cost_sheets ORDER BY currency".query[String].run().toList.map(fromJsonUnsafe[ProductionCostSheet]))

  override def save(s: ProductionCostSheet): UIO[Unit] =
    val json = toJson(s)
    db.tx(
      sql"""INSERT INTO pricing.cost_sheets (currency, doc) VALUES (${s.currency.toString}, $json::jsonb)
            ON CONFLICT (currency) DO UPDATE SET doc = EXCLUDED.doc, updated_at = now()""".update.run()
    ).unit

/** Adapter turning any `UIO[DynamicPricingContext]` supplier into the `private[pricing]` capacity port. */
private[pricing] final class FetchShopCapacityPort(fetch: UIO[DynamicPricingContext]) extends ShopCapacityPort:
  override def currentPricingContext: UIO[DynamicPricingContext] = fetch

object PricingDb:
  val migrations: MigrationSet = MigrationSet("pricing")

  private[pricing] val repositories: URLayer[
    Database,
    PricelistRepository & CustomerAgreementRepository & PromoCodeRepository & CostSheetRepository,
  ] =
    ZLayer.fromZIOEnvironment(
      ZIO.serviceWith[Database](db =>
        ZEnvironment[PricelistRepository, CustomerAgreementRepository, PromoCodeRepository, CostSheetRepository](
          new PostgresPricelistRepository(db),
          new PostgresCustomerAgreementRepository(db),
          new PostgresPromoCodeRepository(db),
          new PostgresCostSheetRepository(db),
        )
      )
    )

  /** `PricingService` backed by PostgreSQL, with shop-floor load supplied by any cross-context effect. */
  def service[R: Tag](capacity: URIO[R, DynamicPricingContext]): ZLayer[R & Database & EventBus, Nothing, PricingService] =
    val capLayer: ZLayer[R, Nothing, ShopCapacityPort] =
      ZLayer.fromZIO(ZIO.environment[R].map(env => new FetchShopCapacityPort(capacity.provideEnvironment(env))))
    (repositories ++ capLayer ++ ZLayer.service[EventBus]) >>> PricingServiceLive.layer

  /** Seeds pricing master data when no pricelist exists yet. Returns `true` when seeded. */
  def seedIfEmpty(
      pricelists: List[Pricelist],
      agreements: List[CustomerPriceAgreement],
      promoCodes: List[PromoCode],
      costSheets: List[ProductionCostSheet],
  ): URIO[Database, Boolean] =
    ZIO.serviceWithZIO[Database] { db =>
      val pl = new PostgresPricelistRepository(db)
      pl.listAll.flatMap { existing =>
        ZIO
          .when(existing.isEmpty) {
            val ag = new PostgresCustomerAgreementRepository(db)
            val pc = new PostgresPromoCodeRepository(db)
            val cs = new PostgresCostSheetRepository(db)
            ZIO.foreachDiscard(pricelists)(pl.save) *>
              ZIO.foreachDiscard(agreements)(ag.save) *>
              ZIO.foreachDiscard(promoCodes)(pc.save) *>
              ZIO.foreachDiscard(costSheets)(cs.save)
          }
          .map(_.isDefined)
      }
    }
