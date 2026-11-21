package mpbuilder.customers
package core

import mpbuilder.shared.*
import zio.*
import java.time.Instant

/** Internal Customer aggregate (compile-time isolated via `private[customers]`).
  *
  * Enforces lifecycle transitions, B2B company profile requirement, and NetInvoice eligibility.
  */
private[customers] final case class Customer(
    id: CustomerId,
    segment: CustomerSegment,
    status: CustomerStatus,
    tier: CustomerTier,
    company: Option[CompanyProfile],
    contact: ContactPerson,
    billingAddress: Address,
    shippingAddresses: List[Address],
    paymentTerms: PaymentTerms,
    notes: List[AccountNote],
    tags: Set[String],
    createdAt: Instant,
):
  def approve: Either[CustomerError, Customer] =
    status match
      case CustomerStatus.PendingApproval | CustomerStatus.Suspended =>
        Right(copy(status = CustomerStatus.Active))
      case other =>
        Left(CustomerError.InvalidStatusTransition(id, other, CustomerStatus.Active))

  def suspend(reason: NonEmptyString, by: Option[EmployeeId], at: Instant): Either[CustomerError, Customer] =
    status match
      case CustomerStatus.Active =>
        Right(
          copy(
            status = CustomerStatus.Suspended,
            paymentTerms = PaymentTerms.PrepaidOnly,
            notes = notes :+ AccountNote(reason, at, by),
          )
        )
      case other =>
        Left(CustomerError.InvalidStatusTransition(id, other, CustomerStatus.Suspended))

  def withPaymentTerms(terms: PaymentTerms): Either[CustomerError, Customer] =
    terms match
      case PaymentTerms.PrepaidOnly =>
        Right(copy(paymentTerms = PaymentTerms.PrepaidOnly))
      case PaymentTerms.NetInvoice(days, limit) =>
        if status != CustomerStatus.Active then
          Left(CustomerError.NetInvoiceNotEligible(id, s"Customer status is $status (must be Active)"))
        else if segment == CustomerSegment.Individual then
          Left(CustomerError.NetInvoiceNotEligible(id, "Individual customers cannot use NetInvoice terms"))
        else if days <= 0 || days > 90 then
          Left(CustomerError.InvalidCreditTerms(s"dueDays must be between 1 and 90, got $days"))
        else if !limit.amount.isPositive then
          Left(CustomerError.InvalidCreditTerms(s"creditLimit must be positive, got ${limit.amount.value}"))
        else
          Right(copy(paymentTerms = terms))

  def toView: CustomerView =
    CustomerView(
      id = id,
      segment = segment,
      status = status,
      tier = tier,
      company = company,
      contact = contact,
      billingAddress = billingAddress,
      shippingAddresses = shippingAddresses,
      paymentTerms = paymentTerms,
      tags = tags,
      createdAt = createdAt,
    )

private[customers] object Customer:
  def register(cmd: RegisterCustomerCmd, now: Instant): Either[CustomerError, Customer] =
    val needsCompany = cmd.segment == CustomerSegment.Business || cmd.segment == CustomerSegment.Agency
    if needsCompany && cmd.company.isEmpty then
      Left(CustomerError.CompanyProfileRequired(cmd.segment))
    else
      val initialStatus =
        if cmd.segment == CustomerSegment.Individual then CustomerStatus.Active
        else CustomerStatus.PendingApproval
      Right(
        Customer(
          id = cmd.id,
          segment = cmd.segment,
          status = initialStatus,
          tier = CustomerTier.Standard,
          company = cmd.company,
          contact = cmd.contact,
          billingAddress = cmd.billingAddress,
          shippingAddresses = List.empty,
          paymentTerms = PaymentTerms.PrepaidOnly,
          notes = List.empty,
          tags = Set.empty,
          createdAt = now,
        )
      )

private[customers] trait CustomerRepository:
  def findById(id: CustomerId): UIO[Option[Customer]]
  def findByEmail(email: EmailAddress): UIO[Option[Customer]]
  def findByRegistrationNumber(regNo: NonEmptyString): UIO[Option[Customer]]
  def findByVatId(vatId: NonEmptyString): UIO[Option[Customer]]
  def save(customer: Customer): UIO[Unit]

private[customers] final class CustomerServiceLive(
    repo: CustomerRepository,
    eventBus: EventBus,
) extends CustomerService:

  override def register(cmd: RegisterCustomerCmd): IO[CustomerError, CustomerView] =
    for
      now      <- Clock.instant
      existing <- repo.findByEmail(cmd.contact.email)
      _        <- ZIO.fail(CustomerError.EmailAlreadyRegistered(cmd.contact.email)).when(existing.isDefined)
      customer <- ZIO.fromEither(Customer.register(cmd, now))
      _        <- repo.save(customer)
      _        <- eventBus.publish(CustomerRegistered(customer.id, customer.segment, customer.tier, now))
    yield customer.toView

  override def getById(id: CustomerId): IO[CustomerError.NotFound, CustomerView] =
    repo.findById(id).someOrFail(new CustomerError.NotFound(id)).map(_.toView)

  override def findByIdentifier(
      identifier: CustomerIdentifier,
  ): IO[CustomerError.NotFoundByIdentifier, CustomerView] =
    val lookup = identifier match
      case CustomerIdentifier.Email(email)       => repo.findByEmail(email)
      case CustomerIdentifier.CompanyRegNo(regNo) => repo.findByRegistrationNumber(regNo)
      case CustomerIdentifier.VatId(vatId)       => repo.findByVatId(vatId)
    lookup.someOrFail(new CustomerError.NotFoundByIdentifier(identifier)).map(_.toView)

  override def approve(id: CustomerId): IO[CustomerError, CustomerView] =
    for
      now      <- Clock.instant
      customer <- repo.findById(id).someOrFail(CustomerError.NotFound(id))
      updated  <- ZIO.fromEither(customer.approve)
      _        <- repo.save(updated)
      _        <- eventBus.publish(CustomerApproved(updated.id, now))
    yield updated.toView

  override def suspend(id: CustomerId, reason: NonEmptyString, by: Option[EmployeeId]): IO[CustomerError, CustomerView] =
    for
      now      <- Clock.instant
      customer <- repo.findById(id).someOrFail(CustomerError.NotFound(id))
      updated  <- ZIO.fromEither(customer.suspend(reason, by, now))
      _        <- repo.save(updated)
    yield updated.toView

  override def changeTier(id: CustomerId, newTier: CustomerTier): IO[CustomerError, CustomerView] =
    for
      now      <- Clock.instant
      customer <- repo.findById(id).someOrFail(CustomerError.NotFound(id))
      prev      = customer.tier
      updated   = customer.copy(tier = newTier)
      _        <- repo.save(updated)
      _        <- eventBus.publish(CustomerTierChanged(updated.id, prev, newTier, now)).when(prev != newTier)
    yield updated.toView

  override def grantPaymentTerms(id: CustomerId, terms: PaymentTerms): IO[CustomerError, CustomerView] =
    for
      customer <- repo.findById(id).someOrFail(CustomerError.NotFound(id))
      updated  <- ZIO.fromEither(customer.withPaymentTerms(terms))
      _        <- repo.save(updated)
    yield updated.toView

private[customers] object CustomerServiceLive:
  val layer: ZLayer[CustomerRepository & EventBus, Nothing, CustomerService] =
    ZLayer.fromFunction(CustomerServiceLive.apply)
