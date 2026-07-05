package mpbuilder.customer

/** How the customer is interacting with the shop */
enum CustomerType:
  case Guest, Registered, RegisteredCorporate, Agency

/** Customer contact information */
final case class ContactInfo(
    firstName: String,
    lastName: String,
    email: String,
    phone: String,
    /** Company name — if set the customer is treated as a business customer */
    company: Option[String],
    /** Czech company registration number (IČO) — for business customers */
    companyRegNo: Option[String],
    /** Czech VAT number (DIČ) — for business customers */
    vatId: Option[String],
)

/** A postal address */
final case class Address(
    street: String,
    city: String,
    zip: String,
    country: String,
)
