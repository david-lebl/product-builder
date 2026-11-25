package mpbuilder.json

import kyo.*
import mpbuilder.customers.*
import mpbuilder.json.JsonSupport.*
import mpbuilder.shared.*
import mpbuilder.json.SharedCodecs.given
import mpbuilder.json.JsonSupport.given

/** Schemas for the public `customers` model. */
object CustomerCodecs:
  given Schema[CustomerStatus]             = Schema.derived
  given Schema[CompanyProfile]             = Schema.derived
  given Schema[ContactPerson]              = Schema.derived
  given Schema[PaymentTerms]               = Schema.derived
  given Schema[CustomerAuditEntry]         = Schema.derived
  given Schema[AccountNote]                = Schema.derived
  given Schema[CustomerView]               = Schema.derived
  given Schema[CustomerFilter]             = Schema.derived
  given Schema[RegisterCustomerCmd]        = Schema.derived
  given Schema[UpdateCustomerProfileCmd]   = Schema.derived
  given Schema[UpdateCustomerAddressesCmd] = Schema.derived
  given Schema[CustomerIdentifier]         = Schema.derived
  given Schema[CustomerError]              = Schema.derived
