package mpbuilder.customers
package impl
package legacy

import mpbuilder.commons.*
import mpbuilder.domain.model as dm
import mpbuilder.domain.pricing as dp
import mpbuilder.domain.sample.SampleCustomers
import mpbuilder.domain.service.CustomerManagementService
import zio.*
import zio.prelude.Validation

/** [[CustomerService]] backed by the legacy customer model, over an in-memory store.
  *
  * The store is a `Ref` rather than a database because there is no database yet — but it is a real
  * store, not a stub: registering a customer during checkout and finding them again afterwards
  * works end to end. Phase 9 swaps the `Ref` for Postgres and nothing above this line changes.
  */
private[customers] final class LegacyCustomerService(
    store: Ref[List[dm.Customer]],
    now: UIO[Timestamp],
) extends CustomerService:

  def find(id: String): IO[CustomerError, Option[CustomerSummary]] =
    store.get.map(_.find(_.id.value == id).map(Mapping.toSummary))

  def findBy(identifier: Identifier): IO[CustomerError, Option[CustomerSummary]] =
    store.get.map { customers =>
      val found = identifier match
        case Identifier.Email(value) =>
          customers.find(_.contactInfo.email.equalsIgnoreCase(value.trim))
        case Identifier.BusinessId(value) =>
          customers.find(_.companyInfo.exists(_.businessId == value.trim))
        case Identifier.VatId(value) =>
          customers.find(_.companyInfo.exists(_.vatId.contains(value.trim)))

      // An account awaiting approval is reported as absent, so a sign-in form cannot be used to
      // discover that an application exists. Matches the legacy LoginService behaviour.
      found.filterNot(_.status == dm.CustomerStatus.PendingApproval).map(Mapping.toSummary)
    }

  def register(input: RegisterCustomer): IO[CustomerError, CustomerSummary] =
    for
      timestamp <- now
      existing <- store.get
      _ <- ZIO
        .fail(CustomerError.EmailAlreadyRegistered(input.email))
        .when(existing.exists(_.contactInfo.email.equalsIgnoreCase(input.email.trim)))
      candidate = Mapping.toDomain(input, timestamp)
      added <- toZIO(CustomerManagementService.addCustomer(existing, candidate))
      _ <- store.set(added)
    yield Mapping.toSummary(candidate)

  private def toZIO[A](
      v: Validation[mpbuilder.domain.service.CustomerManagementError, A]
  ): IO[CustomerError, A] =
    v.toEither match
      case Right(a) => ZIO.succeed(a)
      case Left(errors) =>
        val problems = NonEmptyChunk
          .fromIterableOption(errors.toList.map(Mapping.toProblem))
          .getOrElse(NonEmptyChunk(Problem("Unknown", "Customer is not valid", "Zákazník není platný")))
        ZIO.fail(CustomerError.Rejected(problems))

object LegacyCustomerService:

  val layer: ULayer[CustomerService] =
    ZLayer.fromZIO(seeded(SampleCustomers.all))

  /** A service over the given customers. Useful in tests, and the seam a real repository replaces. */
  def seeded(customers: List[dm.Customer]): UIO[CustomerService] =
    Ref
      .make(customers)
      .map(store => new LegacyCustomerService(store, Clock.instant.map(i => Timestamp(i.toEpochMilli))))
