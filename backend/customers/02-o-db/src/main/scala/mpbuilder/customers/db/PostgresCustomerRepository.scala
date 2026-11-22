package mpbuilder.customers
package db

import com.augustnagro.magnum.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import core.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.json.JsonSupport.*
import mpbuilder.platform.db.{Database, MigrationSet}
import mpbuilder.shared.*
import zio.*

private[customers] object CustomerDbCodecs:
  given JsonValueCodec[Customer] = JsonCodecMaker.make(config)

  /** Same normalization as the in-memory adapter, so lookups behave identically. */
  def normalizeRegNo(raw: String): String = raw.trim.replaceAll("\\s+", "")
  def normalizeVatId(raw: String): String = raw.trim.replaceAll("\\s+", "").toUpperCase

import CustomerDbCodecs.{given, *}

private[customers] final class PostgresCustomerRepository(db: Database) extends CustomerRepository:

  private def one(frag: Frag): UIO[Option[Customer]] =
    db.run(frag.query[String].run().headOption.map(fromJsonUnsafe[Customer]))

  override def findById(id: CustomerId): UIO[Option[Customer]] =
    one(sql"SELECT doc::text FROM customers.customers WHERE id = ${id.value}")

  override def findByEmail(email: EmailAddress): UIO[Option[Customer]] =
    one(sql"SELECT doc::text FROM customers.customers WHERE email = ${email.value}")

  override def findByRegistrationNumber(regNo: NonEmptyString): UIO[Option[Customer]] =
    one(sql"SELECT doc::text FROM customers.customers WHERE reg_no_normalized = ${normalizeRegNo(regNo.value)} ORDER BY id LIMIT 1")

  override def findByVatId(vatId: NonEmptyString): UIO[Option[Customer]] =
    one(sql"SELECT doc::text FROM customers.customers WHERE vat_id_normalized = ${normalizeVatId(vatId.value)} ORDER BY id LIMIT 1")

  override def listAll: UIO[List[Customer]] =
    db.run(sql"SELECT doc::text FROM customers.customers ORDER BY id".query[String].run().toList.map(fromJsonUnsafe[Customer]))

  override def save(c: Customer): UIO[Unit] =
    val regNo = c.company.map(co => normalizeRegNo(co.registrationNumber.value))
    val vatId = c.company.flatMap(_.vatId).map(v => normalizeVatId(v.value))
    val json  = toJson(c)
    db.tx(
      sql"""INSERT INTO customers.customers
              (id, email, reg_no_normalized, vat_id_normalized, segment, status, tier, created_at, doc)
            VALUES (${c.id.value}, ${c.contact.email.value}, $regNo, $vatId, ${c.segment.toString},
                    ${c.status.productPrefix}, ${c.tier.toString}, ${c.createdAt}, $json::jsonb)
            ON CONFLICT (id) DO UPDATE SET
              email             = EXCLUDED.email,
              reg_no_normalized = EXCLUDED.reg_no_normalized,
              vat_id_normalized = EXCLUDED.vat_id_normalized,
              segment           = EXCLUDED.segment,
              status            = EXCLUDED.status,
              tier              = EXCLUDED.tier,
              doc               = EXCLUDED.doc,
              updated_at        = now()""".update.run()
    ).unit

object CustomersDb:
  val migrations: MigrationSet = MigrationSet("customers")

  /** `CustomerService` backed by PostgreSQL. */
  val service: URLayer[Database & EventBus, CustomerService] =
    ZLayer.fromFunction((db: Database) => new PostgresCustomerRepository(db): CustomerRepository) >>>
      CustomerServiceLive.layer

  private[customers] val repository: URLayer[Database, CustomerRepository] =
    ZLayer.fromFunction((db: Database) => new PostgresCustomerRepository(db))
