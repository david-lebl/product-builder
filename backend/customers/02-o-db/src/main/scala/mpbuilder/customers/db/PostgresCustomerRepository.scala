package mpbuilder.customers
package db

import core.*
import kyo.*
import mpbuilder.json.DomainCodecs.given
import mpbuilder.platform.db.{Database, MigrationSet}
import mpbuilder.shared.*

private[customers] object CustomerDbCodecs:
  given Schema[Customer]           = Schema.derived
  given SqlSchema.Column[Customer] = Sql.jsonColumn

  /** Same normalization as the in-memory adapter, so lookups behave identically. */
  def normalizeRegNo(raw: String): String = raw.trim.replaceAll("\\s+", "")
  def normalizeVatId(raw: String): String = raw.trim.replaceAll("\\s+", "").toUpperCase

import CustomerDbCodecs.{given, *}

private[customers] final class PostgresCustomerRepository(db: Database) extends CustomerRepository:

  private def one(frag: Sql.Fragment[Any]): Option[Customer] < Async =
    db.run(frag.as[Customer].run.map(_.headOption))

  override def findById(id: CustomerId): Option[Customer] < Async =
    one(sql"SELECT doc FROM customers.customers WHERE id = ${id.value}")

  override def findByEmail(email: EmailAddress): Option[Customer] < Async =
    one(sql"SELECT doc FROM customers.customers WHERE email = ${email.value}")

  override def findByRegistrationNumber(regNo: NonEmptyString): Option[Customer] < Async =
    one(sql"SELECT doc FROM customers.customers WHERE reg_no_normalized = ${normalizeRegNo(regNo.value)} ORDER BY id LIMIT 1")

  override def findByVatId(vatId: NonEmptyString): Option[Customer] < Async =
    one(sql"SELECT doc FROM customers.customers WHERE vat_id_normalized = ${normalizeVatId(vatId.value)} ORDER BY id LIMIT 1")

  override def listAll: List[Customer] < Async =
    db.run(sql"SELECT doc FROM customers.customers ORDER BY id".as[Customer].run.map(_.toList))

  override def save(c: Customer): Unit < Async =
    val regNo = c.company.map(co => normalizeRegNo(co.registrationNumber.value))
    val vatId = c.company.flatMap(_.vatId).map(v => normalizeVatId(v.value))
    db.tx(
      sql"""INSERT INTO customers.customers
              (id, email, reg_no_normalized, vat_id_normalized, segment, status, tier, created_at, doc)
            VALUES (${c.id.value}, ${c.contact.email.value}, $regNo, $vatId, ${c.segment.toString},
                    ${c.status.productPrefix}, ${c.tier.toString}, ${c.createdAt}, $c)
            ON CONFLICT (id) DO UPDATE SET
              email             = EXCLUDED.email,
              reg_no_normalized = EXCLUDED.reg_no_normalized,
              vat_id_normalized = EXCLUDED.vat_id_normalized,
              segment           = EXCLUDED.segment,
              status            = EXCLUDED.status,
              tier              = EXCLUDED.tier,
              doc               = EXCLUDED.doc,
              updated_at        = now()""".execute
    ).unit

object CustomersDb:
  val migrations: MigrationSet = MigrationSet("customers", "V1__customers.sql")

  /** `CustomerService` backed by PostgreSQL. */
  def service(db: Database, eventBus: EventBus): CustomerService =
    CustomerServiceLive.make(new PostgresCustomerRepository(db), eventBus)

  private[customers] def repository(db: Database): CustomerRepository = new PostgresCustomerRepository(db)
