package mpbuilder.customers
package core

import mpbuilder.shared.*
import kyo.*
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
    additionalContacts: List[ContactPerson] = Nil,
    defaultShippingIndex: Option[Int] = None,
    auditLog: List[CustomerAuditEntry] = Nil,
):
  private def appendAudit(action: String, summary: String, actor: CustomerActor, at: Instant): List[CustomerAuditEntry] =
    auditLog :+ CustomerAuditEntry(action, summary, actor, at)

  private def normalizeDefaultIndex(addrs: List[Address], idx: Option[Int]): Option[Int] =
    if addrs.isEmpty then None
    else idx.filter(i => i >= 0 && i < addrs.size).orElse(Some(0))

  def approve(actor: CustomerActor, at: Instant): Either[CustomerError, Customer] =
    status match
      case CustomerStatus.PendingApproval | CustomerStatus.Suspended =>
        Right(
          copy(
            status = CustomerStatus.Active,
            auditLog = appendAudit("Approved", s"Status changed from $status to Active", actor, at),
          )
        )
      case other =>
        Left(CustomerError.InvalidStatusTransition(id, other, CustomerStatus.Active))

  def suspend(
      reason: NonEmptyString,
      by: Option[EmployeeId],
      byName: Option[NonEmptyString],
      at: Instant,
  ): Either[CustomerError, Customer] =
    val actor = by.map(id => CustomerActor.Employee(id, byName)).getOrElse(CustomerActor.System)
    status match
      case CustomerStatus.Active =>
        Right(
          copy(
            status = CustomerStatus.Suspended,
            paymentTerms = PaymentTerms.PrepaidOnly,
            notes = notes :+ AccountNote(reason, at, by),
            auditLog = appendAudit("Suspended", s"Account suspended: ${reason.value}", actor, at),
          )
        )
      case other =>
        Left(CustomerError.InvalidStatusTransition(id, other, CustomerStatus.Suspended))

  def deactivate(
      reason: NonEmptyString,
      by: Option[EmployeeId],
      byName: Option[NonEmptyString],
      at: Instant,
  ): Either[CustomerError, Customer] =
    val actor = by.map(id => CustomerActor.Employee(id, byName)).getOrElse(CustomerActor.System)
    status match
      case CustomerStatus.Deactivated =>
        Left(CustomerError.InvalidStatusTransition(id, CustomerStatus.Deactivated, CustomerStatus.Deactivated))
      case _ =>
        Right(
          copy(
            status = CustomerStatus.Deactivated,
            paymentTerms = PaymentTerms.PrepaidOnly,
            notes = notes :+ AccountNote(reason, at, by),
            auditLog = appendAudit("Deactivated", s"Account deactivated: ${reason.value}", actor, at),
          )
        )

  def addNote(text: NonEmptyString, by: Option[EmployeeId], byName: Option[NonEmptyString], at: Instant): Customer =
    val actor = by.map(id => CustomerActor.Employee(id, byName)).getOrElse(CustomerActor.System)
    copy(
      notes = notes :+ AccountNote(text, at, by),
      auditLog = appendAudit("NoteAdded", s"Internal note added", actor, at),
    )

  def updateProfile(cmd: UpdateCustomerProfileCmd, at: Instant): Either[CustomerError, Customer] =
    val isB2B = segment == CustomerSegment.Business || segment == CustomerSegment.Agency
    if isB2B && cmd.company.isEmpty then Left(CustomerError.CompanyProfileRequired(segment))
    else if !isB2B && cmd.additionalContacts.exists(_.nonEmpty) then
      Left(CustomerError.MultipleContactsNotAllowed(segment))
    else
      val nextContacts = cmd.additionalContacts.getOrElse(additionalContacts)
      val nextShipping = cmd.shippingAddresses.getOrElse(shippingAddresses)
      val nextDefIdx   = normalizeDefaultIndex(nextShipping, cmd.defaultShippingIndex.orElse(defaultShippingIndex))
      val diffs = List(
        Option.when(cmd.company != company)("company details"),
        Option.when(cmd.contact != contact)("primary contact"),
        Option.when(nextContacts != additionalContacts)(s"additional contacts (${nextContacts.size})"),
        Option.when(cmd.billingAddress != billingAddress)("billing address"),
        Option.when(nextShipping != shippingAddresses || nextDefIdx != defaultShippingIndex)(s"delivery addresses (${nextShipping.size})"),
        Option.when(cmd.tags != tags)("tags"),
      ).flatten
      val summary = if diffs.nonEmpty then s"Updated ${diffs.mkString(", ")}" else "Profile saved (no field changes)"
      Right(
        copy(
          company = cmd.company,
          contact = cmd.contact,
          billingAddress = cmd.billingAddress,
          shippingAddresses = nextShipping,
          defaultShippingIndex = nextDefIdx,
          additionalContacts = nextContacts,
          tags = cmd.tags,
          auditLog = appendAudit("ProfileUpdated", summary, cmd.actor, at),
        )
      )

  def updateAddresses(cmd: UpdateCustomerAddressesCmd, at: Instant): Customer =
    val nextDefIdx = normalizeDefaultIndex(cmd.shippingAddresses, cmd.defaultShippingIndex)
    val summary =
      s"Billing: ${cmd.billingAddress.street.value}, ${cmd.billingAddress.city.value}; ${cmd.shippingAddresses.size} delivery address(es)"
    copy(
      billingAddress = cmd.billingAddress,
      shippingAddresses = cmd.shippingAddresses,
      defaultShippingIndex = nextDefIdx,
      auditLog = appendAudit("AddressesUpdated", summary, cmd.actor, at),
    )

  def withTier(newTier: CustomerTier, actor: CustomerActor, at: Instant): Customer =
    if newTier == tier then this
    else
      copy(
        tier = newTier,
        auditLog = appendAudit("TierChanged", s"Tier changed from $tier to $newTier", actor, at),
      )

  def withPaymentTerms(terms: PaymentTerms, actor: CustomerActor, at: Instant): Either[CustomerError, Customer] =
    terms match
      case PaymentTerms.PrepaidOnly =>
        Right(
          copy(
            paymentTerms = PaymentTerms.PrepaidOnly,
            auditLog = appendAudit("PaymentTermsChanged", "Payment terms set to PrepaidOnly", actor, at),
          )
        )
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
          Right(
            copy(
              paymentTerms = terms,
              auditLog = appendAudit(
                "PaymentTermsChanged",
                s"Granted Net-$days invoice terms (limit ${limit.amount.value} ${limit.currency})",
                actor,
                at,
              ),
            )
          )

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
      notes = notes,
      additionalContacts = additionalContacts,
      defaultShippingIndex = defaultShippingIndex,
      auditLog = auditLog,
    )

private[customers] object Customer:
  def register(cmd: RegisterCustomerCmd, now: Instant): Either[CustomerError, Customer] =
    val isB2B = cmd.segment == CustomerSegment.Business || cmd.segment == CustomerSegment.Agency
    if isB2B && cmd.company.isEmpty then
      Left(CustomerError.CompanyProfileRequired(cmd.segment))
    else if !isB2B && cmd.additionalContacts.nonEmpty then
      Left(CustomerError.MultipleContactsNotAllowed(cmd.segment))
    else
      val initialStatus =
        if cmd.segment == CustomerSegment.Individual then CustomerStatus.Active
        else CustomerStatus.PendingApproval
      val normDefIdx =
        if cmd.shippingAddresses.isEmpty then None
        else cmd.defaultShippingIndex.filter(i => i >= 0 && i < cmd.shippingAddresses.size).orElse(Some(0))
      Right(
        Customer(
          id = cmd.id,
          segment = cmd.segment,
          status = initialStatus,
          tier = CustomerTier.Standard,
          company = cmd.company,
          contact = cmd.contact,
          billingAddress = cmd.billingAddress,
          shippingAddresses = cmd.shippingAddresses,
          paymentTerms = PaymentTerms.PrepaidOnly,
          notes = List.empty,
          tags = Set.empty,
          createdAt = now,
          additionalContacts = cmd.additionalContacts,
          defaultShippingIndex = normDefIdx,
          auditLog = List(
            CustomerAuditEntry(
              action = "Registered",
              summary = s"Account registered (${cmd.segment}, initial status $initialStatus)",
              actor = CustomerActor.System,
              occurredAt = now,
            )
          ),
        )
      )

private[customers] trait CustomerRepository:
  def findById(id: CustomerId): Option[Customer] < Async
  def findByEmail(email: EmailAddress): Option[Customer] < Async
  def findByRegistrationNumber(regNo: NonEmptyString): Option[Customer] < Async
  def findByVatId(vatId: NonEmptyString): Option[Customer] < Async
  def listAll: List[Customer] < Async = Kyo.lift(Nil)
  def save(customer: Customer): Unit < Async

private[customers] final class CustomerServiceLive(
    repo: CustomerRepository,
    eventBus: EventBus,
) extends CustomerService:

  override def register(cmd: RegisterCustomerCmd): CustomerView < (Async & Abort[CustomerError]) =
    for
      now      <- Effects.now
      existing <- repo.findByEmail(cmd.contact.email)
      _        <- Effects.failWhen(existing.isDefined)(CustomerError.EmailAlreadyRegistered(cmd.contact.email))
      customer <- (Customer.register(cmd, now)).orAbort
      _        <- repo.save(customer)
      _        <- eventBus.publish(CustomerRegistered(customer.id, customer.segment, customer.tier, now))
    yield customer.toView

  override def getById(id: CustomerId): CustomerView < (Async & Abort[CustomerError.NotFound]) =
    repo.findById(id).someOrFail(new CustomerError.NotFound(id)).map(_.toView)

  override def findByIdentifier(
      identifier: CustomerIdentifier,
  ): CustomerView < (Async & Abort[CustomerError.NotFoundByIdentifier]) =
    val lookup = identifier match
      case CustomerIdentifier.Email(email)       => repo.findByEmail(email)
      case CustomerIdentifier.CompanyRegNo(regNo) => repo.findByRegistrationNumber(regNo)
      case CustomerIdentifier.VatId(vatId)       => repo.findByVatId(vatId)
    lookup.someOrFail(new CustomerError.NotFoundByIdentifier(identifier)).map(_.toView)

  override def listCustomers(filter: CustomerFilter = CustomerFilter()): List[CustomerView] < Async =
    repo.listAll.map { all =>
      val q = filter.query.map(_.trim.toLowerCase).filter(_.nonEmpty)
      all
        .filter { c =>
          filter.status.forall(_ == c.status) &&
          filter.segment.forall(_ == c.segment) &&
          filter.tier.forall(_ == c.tier) &&
          q.forall { needle =>
            c.id.value.toLowerCase.contains(needle) ||
            c.contact.email.value.toLowerCase.contains(needle) ||
            s"${c.contact.firstName.value} ${c.contact.lastName.value}".toLowerCase.contains(needle) ||
            c.additionalContacts.exists(ac =>
              ac.email.value.toLowerCase.contains(needle) ||
                s"${ac.firstName.value} ${ac.lastName.value}".toLowerCase.contains(needle)
            ) ||
            c.company.exists(co =>
              co.legalName.value.toLowerCase.contains(needle) ||
                co.registrationNumber.value.toLowerCase.contains(needle) ||
                co.vatId.exists(_.value.toLowerCase.contains(needle))
            ) ||
            c.tags.exists(_.toLowerCase.contains(needle))
          }
        }
        .sortBy(_.createdAt)(using Ordering[Instant].reverse)
        .map(_.toView)
    }

  override def approve(id: CustomerId, actor: CustomerActor = CustomerActor.System): CustomerView < (Async & Abort[CustomerError]) =
    for
      now      <- Effects.now
      customer <- repo.findById(id).someOrFail(CustomerError.NotFound(id))
      updated  <- (customer.approve(actor, now)).orAbort
      _        <- repo.save(updated)
      _        <- eventBus.publish(CustomerApproved(updated.id, now))
    yield updated.toView

  override def suspend(
      id: CustomerId,
      reason: NonEmptyString,
      by: Option[EmployeeId],
      byName: Option[NonEmptyString] = None,
  ): CustomerView < (Async & Abort[CustomerError]) =
    for
      now      <- Effects.now
      customer <- repo.findById(id).someOrFail(CustomerError.NotFound(id))
      updated  <- (customer.suspend(reason, by, byName, now)).orAbort
      _        <- repo.save(updated)
    yield updated.toView

  override def deactivate(
      id: CustomerId,
      reason: NonEmptyString,
      by: Option[EmployeeId],
      byName: Option[NonEmptyString] = None,
  ): CustomerView < (Async & Abort[CustomerError]) =
    for
      now      <- Effects.now
      customer <- repo.findById(id).someOrFail(CustomerError.NotFound(id))
      updated  <- (customer.deactivate(reason, by, byName, now)).orAbort
      _        <- repo.save(updated)
    yield updated.toView

  override def changeTier(
      id: CustomerId,
      newTier: CustomerTier,
      actor: CustomerActor = CustomerActor.System,
  ): CustomerView < (Async & Abort[CustomerError]) =
    for
      now      <- Effects.now
      customer <- repo.findById(id).someOrFail(CustomerError.NotFound(id))
      prev      = customer.tier
      updated   = customer.withTier(newTier, actor, now)
      _        <- repo.save(updated)
      _        <- eventBus.publish(CustomerTierChanged(updated.id, prev, newTier, now)).when(prev != newTier)
    yield updated.toView

  override def grantPaymentTerms(
      id: CustomerId,
      terms: PaymentTerms,
      actor: CustomerActor = CustomerActor.System,
  ): CustomerView < (Async & Abort[CustomerError]) =
    for
      now      <- Effects.now
      customer <- repo.findById(id).someOrFail(CustomerError.NotFound(id))
      updated  <- (customer.withPaymentTerms(terms, actor, now)).orAbort
      _        <- repo.save(updated)
    yield updated.toView

  override def updateProfile(cmd: UpdateCustomerProfileCmd): CustomerView < (Async & Abort[CustomerError]) =
    for
      now      <- Effects.now
      customer <- repo.findById(cmd.id).someOrFail(CustomerError.NotFound(cmd.id))
      _        <- Effects.when(cmd.contact.email != customer.contact.email) {
                      repo
                        .findByEmail(cmd.contact.email)
                        .flatMap(existing =>
                          Effects.failWhen(existing.exists(_.id != cmd.id))(CustomerError.EmailAlreadyRegistered(cmd.contact.email))
                        )
                    }
      updated  <- (customer.updateProfile(cmd, now)).orAbort
      _        <- repo.save(updated)
    yield updated.toView

  override def updateAddresses(cmd: UpdateCustomerAddressesCmd): CustomerView < (Async & Abort[CustomerError]) =
    for
      now      <- Effects.now
      customer <- repo.findById(cmd.id).someOrFail(CustomerError.NotFound(cmd.id))
      updated   = customer.updateAddresses(cmd, now)
      _        <- repo.save(updated)
    yield updated.toView

  override def addNote(
      id: CustomerId,
      text: NonEmptyString,
      by: Option[EmployeeId],
      byName: Option[NonEmptyString] = None,
  ): CustomerView < (Async & Abort[CustomerError]) =
    for
      now      <- Effects.now
      customer <- repo.findById(id).someOrFail(CustomerError.NotFound(id))
      updated   = customer.addNote(text, by, byName, now)
      _        <- repo.save(updated)
    yield updated.toView

private[customers] object CustomerServiceLive:
  def make(repo: CustomerRepository, eventBus: EventBus): CustomerService = CustomerServiceLive(repo, eventBus)
