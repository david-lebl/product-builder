package mpbuilder.pricing
package db

import core.*
import kyo.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.platform.db.{Database, MigrationSet}
import mpbuilder.shared.*

private[pricing] object PricingDbKeys:
  given SqlSchema.Column[Pricelist]              = Sql.jsonColumn
  given SqlSchema.Column[CustomerPriceAgreement] = Sql.jsonColumn
  given SqlSchema.Column[PromoCode]              = Sql.jsonColumn
  given SqlSchema.Column[ProductionCostSheet]    = Sql.jsonColumn

  def target(t: AgreementTarget): (String, String) = t match
    case AgreementTarget.SpecificCustomer(id) => ("SpecificCustomer", id.value)
    case AgreementTarget.TierDefault(tier)    => ("TierDefault", tier.toString)

  def normalizeCode(raw: String): String = raw.trim.toUpperCase

import PricingDbKeys.{given, *}

private[pricing] final class PostgresPricelistRepository(db: Database) extends PricelistRepository:

  override def findActive(currency: Currency): Pricelist < (Async & Abort[PricingError.PricelistNotFound]) =
    db.run(sql"SELECT doc FROM pricing.pricelists WHERE currency = ${currency.toString}".as[Pricelist].run.map(_.headOption))
      .someOrFail(new PricingError.PricelistNotFound(currency))

  override def listAll: List[Pricelist] < Async =
    db.run(sql"SELECT doc FROM pricing.pricelists ORDER BY currency".as[Pricelist].run.map(_.toList))

  override def save(p: Pricelist): Unit < Async =
    db.tx(
      sql"""INSERT INTO pricing.pricelists (currency, id, version, doc)
            VALUES (${p.currency.toString}, ${p.id.value}, ${p.version.value}, $p)
            ON CONFLICT (currency) DO UPDATE SET
              id = EXCLUDED.id, version = EXCLUDED.version, doc = EXCLUDED.doc, updated_at = now()""".execute
    ).unit

private[pricing] final class PostgresCustomerAgreementRepository(db: Database) extends CustomerAgreementRepository:

  private def find(kind: String, ref: String, currency: Currency): Option[CustomerPriceAgreement] < Async =
    db.run(
      sql"""SELECT doc FROM pricing.customer_agreements
            WHERE target_kind = $kind AND target_ref = $ref AND currency = ${currency.toString}""".as[CustomerPriceAgreement].run.map(_.headOption)
    )

  override def findForCustomer(customerId: CustomerId, currency: Currency): Option[CustomerPriceAgreement] < Async =
    find("SpecificCustomer", customerId.value, currency)

  override def findForTier(tier: CustomerTier, currency: Currency): Option[CustomerPriceAgreement] < Async =
    find("TierDefault", tier.toString, currency)

  override def listAll: List[CustomerPriceAgreement] < Async =
    db.run(
      sql"SELECT doc FROM pricing.customer_agreements ORDER BY position".as[CustomerPriceAgreement].run.map(_.toList)
    )

  override def save(a: CustomerPriceAgreement): Unit < Async =
    val (kind, ref) = target(a.target)
    db.tx(
      sql"""INSERT INTO pricing.customer_agreements (target_kind, target_ref, currency, doc)
            VALUES ($kind, $ref, ${a.currency.toString}, $a)
            ON CONFLICT (target_kind, target_ref, currency) DO UPDATE SET doc = EXCLUDED.doc, updated_at = now()"""
        .execute
    ).unit

  override def delete(t: AgreementTarget, currency: Currency): Unit < Async =
    val (kind, ref) = target(t)
    db.tx(
      sql"""DELETE FROM pricing.customer_agreements
            WHERE target_kind = $kind AND target_ref = $ref AND currency = ${currency.toString}""".execute
    ).unit

private[pricing] final class PostgresPromoCodeRepository(db: Database) extends PromoCodeRepository:

  override def findByCode(code: String): Option[PromoCode] < Async =
    db.run(
      sql"SELECT doc FROM pricing.promo_codes WHERE code_upper = ${normalizeCode(code)}".as[PromoCode].run.map(_.headOption)
    )

  override def findById(id: DiscountCodeId): Option[PromoCode] < Async =
    db.run(
      sql"SELECT doc FROM pricing.promo_codes WHERE id = ${id.value}".as[PromoCode].run.map(_.headOption)
    )

  override def listAll: List[PromoCode] < Async =
    db.run(sql"SELECT doc FROM pricing.promo_codes ORDER BY code_upper".as[PromoCode].run.map(_.toList))

  /** Mirrors the in-memory semantics: a promo replaces any existing one with the same id OR code. */
  override def save(p: PromoCode): Unit < Async =
    val code = normalizeCode(p.code.value)
    db.tx {
      for
        _ <- sql"DELETE FROM pricing.promo_codes WHERE id = ${p.id.value} OR code_upper = $code".execute
        _ <- sql"""INSERT INTO pricing.promo_codes (id, code_upper, is_active, doc)
                   VALUES (${p.id.value}, $code, ${p.isActive}, $p)""".execute
      yield ()
    }

  override def delete(id: DiscountCodeId): Unit < Async =
    db.tx(sql"DELETE FROM pricing.promo_codes WHERE id = ${id.value}".execute).unit

  /** Atomic, race-free redemption counter (no read-modify-write in the application). */
  override def incrementUsage(codeId: DiscountCodeId): Unit < Async =
    db.tx(
      sql"""UPDATE pricing.promo_codes
            SET doc = jsonb_set(doc, '{constraints,currentUses}',
                                to_jsonb(COALESCE((doc #>> '{constraints,currentUses}')::int, 0) + 1)),
                updated_at = now()
            WHERE id = ${codeId.value}""".execute
    ).unit

private[pricing] final class PostgresCostSheetRepository(db: Database) extends CostSheetRepository:

  override def findActive(currency: Currency): Option[ProductionCostSheet] < Async =
    db.run(
      sql"SELECT doc FROM pricing.cost_sheets WHERE currency = ${currency.toString}".as[ProductionCostSheet].run.map(_.headOption)
    )

  override def listAll: List[ProductionCostSheet] < Async =
    db.run(sql"SELECT doc FROM pricing.cost_sheets ORDER BY currency".as[ProductionCostSheet].run.map(_.toList))

  override def save(s: ProductionCostSheet): Unit < Async =
    db.tx(
      sql"""INSERT INTO pricing.cost_sheets (currency, doc) VALUES (${s.currency.toString}, $s)
            ON CONFLICT (currency) DO UPDATE SET doc = EXCLUDED.doc, updated_at = now()""".execute
    ).unit

/** Adapter turning any `DynamicPricingContext` supplier into the `private[pricing]` capacity port. */
private[pricing] final class FetchShopCapacityPort(fetch: => DynamicPricingContext < Async) extends ShopCapacityPort:
  override def currentPricingContext: DynamicPricingContext < Async = fetch

/** PostgreSQL-backed pricing repositories. */
private[pricing] final case class PricingRepositories(
    pricelists: PostgresPricelistRepository,
    agreements: PostgresCustomerAgreementRepository,
    promoCodes: PostgresPromoCodeRepository,
    costSheets: PostgresCostSheetRepository,
)

object PricingDb:
  val migrations: MigrationSet = MigrationSet("pricing", "V1__pricing.sql")

  private[pricing] def repositories(db: Database): PricingRepositories =
    PricingRepositories(
      new PostgresPricelistRepository(db),
      new PostgresCustomerAgreementRepository(db),
      new PostgresPromoCodeRepository(db),
      new PostgresCostSheetRepository(db),
    )

  /** `PricingService` backed by PostgreSQL, with shop-floor load supplied by any cross-context effect. */
  def service(db: Database, eventBus: EventBus, capacity: => DynamicPricingContext < Async): PricingService =
    val r = repositories(db)
    PricingServiceLive.make(r.pricelists, r.agreements, r.promoCodes, r.costSheets, new FetchShopCapacityPort(capacity), eventBus)

  /** Seeds pricing master data when no pricelist exists yet. Returns `true` when seeded. */
  def seedIfEmpty(
      db: Database,
      pricelists: List[Pricelist],
      agreements: List[CustomerPriceAgreement],
      promoCodes: List[PromoCode],
      costSheets: List[ProductionCostSheet],
  ): Boolean < Async =
    val r = repositories(db)
    r.pricelists.listAll.map { existing =>
      if existing.nonEmpty then false
      else
        for
          _ <- Kyo.foreachDiscard(pricelists)(r.pricelists.save)
          _ <- Kyo.foreachDiscard(agreements)(r.agreements.save)
          _ <- Kyo.foreachDiscard(promoCodes)(r.promoCodes.save)
          _ <- Kyo.foreachDiscard(costSheets)(r.costSheets.save)
        yield true
    }
