package mpbuilder.customers
package db

import core.*
import kyo.*
import kyo.test.*
import mpbuilder.customers.sample.{CustomerSampleWiring, SampleCustomers}
import mpbuilder.platform.PostgresTestSupport
import mpbuilder.shared.*

class CustomerRepositoryContractSpec extends Test[Any]:

  private type Fresh = CustomerRepository < (Async & Scope & Abort[Throwable])

  private def inMemory: Fresh = CustomerSampleWiring.repository(Nil)
  private def postgres: Fresh = PostgresTestSupport.database(List(CustomersDb.migrations)).map(CustomersDb.repository)

  private def contract(label: String, fresh: => Fresh): Unit =
    label - {
      "all sample customers round-trip and list sorted by id" in {
        for
          repo <- fresh
          _    <- Kyo.foreachDiscard(SampleCustomers.all)(repo.save)
          all  <- repo.listAll
          byId <- Kyo.foreach(SampleCustomers.all)(c => repo.findById(c.id))
        yield
          assert(all == SampleCustomers.all.sortBy(_.id.value))
          assert(byId == SampleCustomers.all.map(Some(_)))
      }

      "lookup by email, whitespace-insensitive IČO and case-insensitive DIČ" in {
        val b2b = SampleCustomers.all.find(_.company.exists(_.vatId.isDefined)).get
        val co  = b2b.company.get
        for
          repo  <- fresh
          _     <- Kyo.foreachDiscard(SampleCustomers.all)(repo.save)
          email <- repo.findByEmail(b2b.contact.email)
          regNo <- repo.findByRegistrationNumber(NonEmptyString.unsafe(" " + co.registrationNumber.value.flatMap(ch => s"$ch ")))
          vat   <- repo.findByVatId(NonEmptyString.unsafe(co.vatId.get.value.toLowerCase))
          none  <- repo.findByEmail(EmailAddress.unsafe("missing@nowhere.cz"))
        yield
          assert(email.map(_.id).contains(b2b.id))
          assert(regNo.map(_.id).contains(b2b.id))
          assert(vat.map(_.id).contains(b2b.id))
          assert(none.isEmpty)
      }

      "save overwrites an existing customer (status / tier changes)" in {
        val c = SampleCustomers.all.head
        for
          repo <- fresh
          _    <- repo.save(c)
          _    <- repo.save(c.copy(tier = CustomerTier.Platinum, tags = Set("vip")))
          back <- repo.findById(c.id)
          all  <- repo.listAll
        yield
          assert(back.map(_.tier).contains(CustomerTier.Platinum))
          assert(back.map(_.tags).contains(Set("vip")))
          assert(all.size == 1)
      }
    }

  "Customer repository contract" - {
    contract("in-memory", inMemory)
    contract("postgres", postgres)
  }
