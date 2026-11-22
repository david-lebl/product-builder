package mpbuilder.customers
package db

import core.*
import mpbuilder.customers.sample.{CustomerSampleLayer, SampleCustomers}
import mpbuilder.platform.PostgresTestSupport
import mpbuilder.platform.db.Database
import mpbuilder.shared.*
import zio.*
import zio.test.*

object CustomerRepositoryContractSpec extends ZIOSpecDefault:

  val inMemory: ULayer[CustomerRepository] = CustomerSampleLayer.repositoryLayer(Nil)

  val postgres: URLayer[Database, CustomerRepository] =
    ZLayer.fromZIO(ZIO.serviceWithZIO[Database](PostgresTestSupport.truncateSchemas(_, List("customers")))) >>> CustomersDb.repository

  def contract[R](label: String, fresh: ZLayer[R, Nothing, CustomerRepository]) = suite(label)(
    test("all sample customers round-trip and list sorted by id") {
      for
        repo <- ZIO.service[CustomerRepository]
        _    <- ZIO.foreachDiscard(SampleCustomers.all)(repo.save)
        all  <- repo.listAll
        byId <- ZIO.foreach(SampleCustomers.all)(c => repo.findById(c.id))
      yield assertTrue(
        all == SampleCustomers.all.sortBy(_.id.value),
        byId == SampleCustomers.all.map(Some(_)),
      )
    },
    test("lookup by email, whitespace-insensitive IČO and case-insensitive DIČ") {
      val b2b = SampleCustomers.all.find(_.company.exists(_.vatId.isDefined)).get
      val co  = b2b.company.get
      for
        repo  <- ZIO.service[CustomerRepository]
        _     <- ZIO.foreachDiscard(SampleCustomers.all)(repo.save)
        email <- repo.findByEmail(b2b.contact.email)
        regNo <- repo.findByRegistrationNumber(NonEmptyString.unsafe(" " + co.registrationNumber.value.flatMap(ch => s"$ch ") ))
        vat   <- repo.findByVatId(NonEmptyString.unsafe(co.vatId.get.value.toLowerCase))
        none  <- repo.findByEmail(EmailAddress.unsafe("missing@nowhere.cz"))
      yield assertTrue(email.map(_.id).contains(b2b.id), regNo.map(_.id).contains(b2b.id), vat.map(_.id).contains(b2b.id), none.isEmpty)
    },
    test("save overwrites an existing customer (status / tier changes)") {
      val c = SampleCustomers.all.head
      for
        repo <- ZIO.service[CustomerRepository]
        _    <- repo.save(c)
        _    <- repo.save(c.copy(tier = CustomerTier.Platinum, tags = Set("vip")))
        back <- repo.findById(c.id)
        all  <- repo.listAll
      yield assertTrue(back.map(_.tier).contains(CustomerTier.Platinum), back.map(_.tags).contains(Set("vip")), all.size == 1)
    },
  ).provideSomeLayer[R](fresh)

  def spec = suite("Customer repository contract")(
    contract("in-memory", inMemory),
    contract("postgres", postgres).provideShared(PostgresTestSupport.database(List(CustomersDb.migrations))),
  ) @@ TestAspect.sequential
