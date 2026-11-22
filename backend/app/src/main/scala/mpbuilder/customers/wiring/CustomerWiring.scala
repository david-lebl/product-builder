package mpbuilder.customers
package wiring

import db.CustomersDb
import mpbuilder.customers.sample.SampleCustomers
import mpbuilder.platform.db.Database
import zio.*

object CustomerWiring:
  /** Seeds the sample customer directory when no customer exists yet. Returns `true` when seeded. */
  val seedIfEmpty: URIO[Database, Boolean] =
    ZIO.serviceWithZIO[core.CustomerRepository] { repo =>
      repo.listAll.flatMap(existing => ZIO.when(existing.isEmpty)(ZIO.foreachDiscard(SampleCustomers.all)(repo.save)).map(_.isDefined))
    }.provideSomeLayer[Database](CustomersDb.repository)
