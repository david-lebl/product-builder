package mpbuilder.customers
package wiring

import db.CustomersDb
import kyo.*
import mpbuilder.customers.sample.SampleCustomers
import mpbuilder.platform.db.Database

object CustomerWiring:
  /** Seeds the sample customer directory when no customer exists yet. Returns `true` when seeded. */
  def seedIfEmpty(db: Database): Boolean < Async =
    val repo = CustomersDb.repository(db)
    repo.listAll.map { existing =>
      if existing.nonEmpty then false
      else Kyo.foreachDiscard(SampleCustomers.all)(repo.save).andThen(true)
    }
