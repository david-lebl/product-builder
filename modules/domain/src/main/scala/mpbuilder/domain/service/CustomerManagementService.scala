package mpbuilder.domain.service

import mpbuilder.domain.model.*
import zio.prelude.*

/** Pure service for managing customers — CRUD operations with validation. */
object CustomerManagementService:

  /** Add a new customer. */
  def addCustomer(
      customers: List[Customer],
      customer: Customer,
  ): Validation[CustomerManagementError, List[Customer]] =
    for
      _ <- validateRequiredFields(customer)
      _ <- validateUniqueBusinessId(customers, customer)
      _ <- validateUniqueEmail(customers, customer)
    yield customers :+ customer

  /** Update an existing customer's core fields. */
  def updateCustomer(
      customers: List[Customer],
      customerId: CustomerId,
      companyInfo: Option[CompanyInfo],
      contactInfo: ContactInfo,
      address: Address,
  ): Validation[CustomerManagementError, List[Customer]] =
    for
      _ <- validateCustomerExists(customers, customerId)
      _ <- validateContactInfoFields(contactInfo)
    yield customers.map { c =>
      if c.id == customerId then c.copy(companyInfo = companyInfo, contactInfo = contactInfo, address = address)
      else c
    }

  /** Update a customer's status. */
  def updateStatus(
      customers: List[Customer],
      customerId: CustomerId,
      newStatus: CustomerStatus,
  ): Validation[CustomerManagementError, List[Customer]] =
    for _ <- validateCustomerExists(customers, customerId)
    yield customers.map { c =>
      if c.id == customerId then c.copy(status = newStatus) else c
    }

  /** Update a customer's tier. */
  def updateTier(
      customers: List[Customer],
      customerId: CustomerId,
      newTier: CustomerTier,
  ): Validation[CustomerManagementError, List[Customer]] =
    for _ <- validateCustomerExists(customers, customerId)
    yield customers.map { c =>
      if c.id == customerId then c.copy(tier = newTier) else c
    }

  /** Add an internal note to a customer. */
  def addNote(
      customers: List[Customer],
      customerId: CustomerId,
      note: CustomerNote,
  ): Validation[CustomerManagementError, List[Customer]] =
    for
      _ <- validateCustomerExists(customers, customerId)
      _ <- validateNoteText(note.text)
    yield customers.map { c =>
      if c.id == customerId then c.copy(internalNotes = c.internalNotes :+ note) else c
    }

  /** Remove a customer from the list. */
  def removeCustomer(
      customers: List[Customer],
      customerId: CustomerId,
  ): Validation[CustomerManagementError, List[Customer]] =
    for _ <- validateCustomerExists(customers, customerId)
    yield customers.filterNot(_.id == customerId)

  // --- Validation helpers ---

  private def validateCustomerExists(customers: List[Customer], id: CustomerId): Validation[CustomerManagementError, Unit] =
    if customers.exists(_.id == id) then Validation.unit
    else Validation.fail(CustomerManagementError.CustomerNotFound(id))

  private def validateRequiredFields(customer: Customer): Validation[CustomerManagementError, Unit] =
    val errors = List.newBuilder[CustomerManagementError]
    if customer.contactInfo.email.trim.isEmpty then
      errors += CustomerManagementError.MissingRequiredField("email")
    if customer.contactInfo.firstName.trim.isEmpty then
      errors += CustomerManagementError.MissingRequiredField("firstName")
    if customer.contactInfo.lastName.trim.isEmpty then
      errors += CustomerManagementError.MissingRequiredField("lastName")
    if customer.customerType == CustomerType.Agency then
      customer.companyInfo match
        case None => errors += CustomerManagementError.MissingRequiredField("companyInfo")
        case Some(ci) =>
          if ci.companyName.trim.isEmpty then
            errors += CustomerManagementError.MissingRequiredField("companyName")
          if ci.businessId.trim.isEmpty then
            errors += CustomerManagementError.MissingRequiredField("businessId")
    val errs = errors.result()
    if errs.isEmpty then Validation.unit
    else Validation.failNonEmptyChunk(zio.NonEmptyChunk.fromIterable(errs.head, errs.tail))

  private def validateContactInfoFields(contactInfo: ContactInfo): Validation[CustomerManagementError, Unit] =
    val errors = List.newBuilder[CustomerManagementError]
    if contactInfo.email.trim.isEmpty then
      errors += CustomerManagementError.MissingRequiredField("email")
    if contactInfo.firstName.trim.isEmpty then
      errors += CustomerManagementError.MissingRequiredField("firstName")
    if contactInfo.lastName.trim.isEmpty then
      errors += CustomerManagementError.MissingRequiredField("lastName")
    val errs = errors.result()
    if errs.isEmpty then Validation.unit
    else Validation.failNonEmptyChunk(zio.NonEmptyChunk.fromIterable(errs.head, errs.tail))

  private def validateUniqueBusinessId(customers: List[Customer], customer: Customer): Validation[CustomerManagementError, Unit] =
    customer.companyInfo match
      case Some(ci) if ci.businessId.trim.nonEmpty =>
        if customers.exists(c => c.id != customer.id && c.companyInfo.exists(_.businessId == ci.businessId)) then
          Validation.fail(CustomerManagementError.DuplicateBusinessId(ci.businessId))
        else Validation.unit
      case _ => Validation.unit

  private def validateUniqueEmail(customers: List[Customer], customer: Customer): Validation[CustomerManagementError, Unit] =
    val email = customer.contactInfo.email.trim.toLowerCase
    if email.nonEmpty && customers.exists(c => c.id != customer.id && c.contactInfo.email.trim.toLowerCase == email) then
      Validation.fail(CustomerManagementError.DuplicateEmail(customer.contactInfo.email))
    else Validation.unit

  private def validateNoteText(text: String): Validation[CustomerManagementError, Unit] =
    if text.trim.nonEmpty then Validation.unit
    else Validation.fail(CustomerManagementError.MissingRequiredField("note text"))
