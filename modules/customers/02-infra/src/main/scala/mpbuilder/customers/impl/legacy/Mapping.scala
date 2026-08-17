package mpbuilder.customers
package impl
package legacy

import mpbuilder.commons.*
import mpbuilder.domain.model as dm
import mpbuilder.domain.pricing as dp
import mpbuilder.domain.service.CustomerManagementError

/** Translation between the public customer contract and the legacy domain model. */
private[customers] object Mapping:

  def toSummary(c: dm.Customer): CustomerSummary =
    CustomerSummary(
      id = c.id.value,
      displayName = displayName(c),
      email = c.contactInfo.email,
      company = c.companyInfo.map(ci =>
        CompanyDetails(
          name = ci.companyName,
          businessId = Some(ci.businessId).filter(_.nonEmpty),
          vatId = ci.vatId,
        )
      ),
      customerType = c.customerType.toString,
      status = c.status.toString,
      tier = c.tier.toString,
      isActive = c.status == dm.CustomerStatus.Active,
      // Spec §9.1: only an approved corporate account may be invoiced. Derived once, here,
      // so no other context has to remember the rule.
      canPayOnAccount =
        c.status == dm.CustomerStatus.Active && c.customerType == dm.CustomerType.RegisteredCorporate,
    )

  private def displayName(c: dm.Customer): String =
    c.companyInfo.map(_.companyName).filter(_.nonEmpty).getOrElse {
      s"${c.contactInfo.firstName} ${c.contactInfo.lastName}".trim match
        case ""   => c.contactInfo.email
        case name => name
    }

  def toDomain(input: RegisterCustomer, now: Timestamp): dm.Customer =
    dm.Customer(
      id = dm.CustomerId.unsafe(java.util.UUID.randomUUID().toString),
      // A customer who registers with company details is a corporate account applicant; approval
      // is a staff decision, so they start as PendingApproval rather than being granted invoicing.
      customerType =
        if input.company.isDefined then dm.CustomerType.RegisteredCorporate
        else dm.CustomerType.Registered,
      status =
        if input.company.isDefined then dm.CustomerStatus.PendingApproval
        else dm.CustomerStatus.Active,
      tier = dm.CustomerTier.Standard,
      companyInfo = input.company.map(c =>
        dm.CompanyInfo(
          companyName = c.name,
          businessId = c.businessId.getOrElse(""),
          vatId = c.vatId,
          contactPerson = s"${input.firstName} ${input.lastName}".trim,
        )
      ),
      contactInfo = dm.ContactInfo(
        firstName = input.firstName,
        lastName = input.lastName,
        email = input.email,
        phone = input.phone,
        company = input.company.map(_.name),
        companyRegNo = input.company.flatMap(_.businessId),
        vatId = input.company.flatMap(_.vatId),
      ),
      address = dm.Address("", "", "", ""),
      pricing = dp.CustomerPricing(),
      internalNotes = Nil,
      createdAt = now.epochMillis,
      lastOrderAt = None,
      tags = Set.empty,
    )

  def toProblem(error: CustomerManagementError): Problem =
    Problem(
      code = error.toString.takeWhile(_ != '('),
      message = LocalizedString(error.message(Language.En), error.message(Language.Cs)),
    )
