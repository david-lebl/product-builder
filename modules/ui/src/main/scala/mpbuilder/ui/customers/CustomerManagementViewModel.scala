package mpbuilder.ui.customers

import com.raquo.laminar.api.L.*
import mpbuilder.domain.model.*
import mpbuilder.domain.pricing.*
import mpbuilder.domain.service.*
import mpbuilder.domain.sample.*

/** Reactive state management for the customer management UI.
  *
  * All mutations go through this object. Views subscribe to signals derived from `state`.
  */
object CustomerManagementViewModel:

  private val stateVar: Var[CustomerManagementState] = Var(
    CustomerManagementState(
      customers = SampleCustomers.all,
      discountCodes = SampleDiscountCodes.all,
      activeSection = CustomerSection.Customers,
      editState = CustomerEditState.None,
      statusMessage = None,
    )
  )
  val state: Signal[CustomerManagementState] = stateVar.signal

  // ── Derived signals ────────────────────────────────────────────────────

  val customers: Signal[List[Customer]] = state.map(_.customers)
  val discountCodes: Signal[List[DiscountCode]] = state.map(_.discountCodes)
  val activeSection: Signal[CustomerSection] = state.map(_.activeSection)
  val editState: Signal[CustomerEditState] = state.map(_.editState)
  val statusMessage: Signal[Option[String]] = state.map(_.statusMessage)

  // ── Navigation ─────────────────────────────────────────────────────────

  def setSection(section: CustomerSection): Unit =
    stateVar.update(_.copy(activeSection = section, editState = CustomerEditState.None))

  def setEditState(es: CustomerEditState): Unit =
    stateVar.update(_.copy(editState = es))

  def clearStatus(): Unit =
    stateVar.update(_.copy(statusMessage = None))

  // ── Customer CRUD ──────────────────────────────────────────────────────

  def addCustomer(customer: Customer): Unit =
    val result = CustomerManagementService.addCustomer(stateVar.now().customers, customer)
    result.fold(
      errors => stateVar.update(_.copy(
        statusMessage = Some(s"Error: ${errors.map(_.message).mkString(", ")}"),
      )),
      updated => stateVar.update(_.copy(
        customers = updated,
        editState = CustomerEditState.None,
        statusMessage = Some(s"Customer '${customer.companyInfo.map(_.companyName).getOrElse(customer.contactInfo.email)}' added"),
      )),
    )

  def updateCustomer(
      customerId: CustomerId,
      companyInfo: Option[CompanyInfo],
      contactInfo: ContactInfo,
      address: Address,
  ): Unit =
    val result = CustomerManagementService.updateCustomer(
      stateVar.now().customers, customerId, companyInfo, contactInfo, address,
    )
    result.fold(
      errors => stateVar.update(_.copy(
        statusMessage = Some(s"Error: ${errors.map(_.message).mkString(", ")}"),
      )),
      updated => stateVar.update(_.copy(
        customers = updated,
        editState = CustomerEditState.None,
        statusMessage = Some("Customer updated"),
      )),
    )

  def updateCustomerStatus(customerId: CustomerId, newStatus: CustomerStatus): Unit =
    val result = CustomerManagementService.updateStatus(stateVar.now().customers, customerId, newStatus)
    result.fold(
      errors => stateVar.update(_.copy(
        statusMessage = Some(s"Error: ${errors.map(_.message).mkString(", ")}"),
      )),
      updated => stateVar.update(_.copy(
        customers = updated,
        statusMessage = Some(s"Customer status updated to ${newStatus.displayName.value}"),
      )),
    )

  def updateCustomerTier(customerId: CustomerId, newTier: CustomerTier): Unit =
    val result = CustomerManagementService.updateTier(stateVar.now().customers, customerId, newTier)
    result.fold(
      errors => stateVar.update(_.copy(
        statusMessage = Some(s"Error: ${errors.map(_.message).mkString(", ")}"),
      )),
      updated => stateVar.update(_.copy(
        customers = updated,
        statusMessage = Some(s"Customer tier updated to ${newTier.displayName.value}"),
      )),
    )

  def addCustomerNote(customerId: CustomerId, text: String): Unit =
    val note = CustomerNote(text, System.currentTimeMillis(), None)
    val result = CustomerManagementService.addNote(stateVar.now().customers, customerId, note)
    result.fold(
      errors => stateVar.update(_.copy(
        statusMessage = Some(s"Error: ${errors.map(_.message).mkString(", ")}"),
      )),
      updated => stateVar.update(_.copy(
        customers = updated,
        statusMessage = Some("Note added"),
      )),
    )

  def removeCustomer(customerId: CustomerId): Unit =
    val result = CustomerManagementService.removeCustomer(stateVar.now().customers, customerId)
    result.fold(
      errors => stateVar.update(_.copy(
        statusMessage = Some(s"Error: ${errors.map(_.message).mkString(", ")}"),
      )),
      updated => stateVar.update(_.copy(
        customers = updated,
        editState = CustomerEditState.None,
        statusMessage = Some("Customer removed"),
      )),
    )

  def updateCustomerPricing(customerId: CustomerId, pricing: CustomerPricing): Unit =
    stateVar.update { s =>
      s.copy(
        customers = s.customers.map { c =>
          if c.id == customerId then c.copy(pricing = pricing) else c
        },
        statusMessage = Some("Customer pricing updated"),
      )
    }

  // ── Discount Code CRUD ─────────────────────────────────────────────────

  def createDiscountCode(code: DiscountCode): Unit =
    val result = DiscountCodeService.createCode(stateVar.now().discountCodes, code)
    result.fold(
      errors => stateVar.update(_.copy(
        statusMessage = Some(s"Error: ${errors.map(_.message).mkString(", ")}"),
      )),
      updated => stateVar.update(_.copy(
        discountCodes = updated,
        editState = CustomerEditState.None,
        statusMessage = Some(s"Discount code '${code.code}' created"),
      )),
    )

  def updateDiscountCode(code: DiscountCode): Unit =
    val result = DiscountCodeService.updateCode(stateVar.now().discountCodes, code.id, code)
    result.fold(
      errors => stateVar.update(_.copy(
        statusMessage = Some(s"Error: ${errors.map(_.message).mkString(", ")}"),
      )),
      updated => stateVar.update(_.copy(
        discountCodes = updated,
        editState = CustomerEditState.None,
        statusMessage = Some(s"Discount code '${code.code}' updated"),
      )),
    )

  def toggleDiscountCodeActive(id: DiscountCodeId): Unit =
    stateVar.update { s =>
      val updated = s.discountCodes.map { dc =>
        if dc.id == id then dc.copy(isActive = !dc.isActive) else dc
      }
      val code = s.discountCodes.find(_.id == id)
      s.copy(
        discountCodes = updated,
        statusMessage = code.map(c =>
          if c.isActive then s"Discount code '${c.code}' deactivated"
          else s"Discount code '${c.code}' activated"
        ),
      )
    }

  def removeDiscountCode(id: DiscountCodeId): Unit =
    stateVar.update { s =>
      s.copy(
        discountCodes = s.discountCodes.filterNot(_.id == id),
        editState = CustomerEditState.None,
        statusMessage = Some("Discount code removed"),
      )
    }
