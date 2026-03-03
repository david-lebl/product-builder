package mpbuilder.domain.model

import mpbuilder.domain.pricing.{Money, Currency}

/** How the customer is interacting with the shop */
enum CustomerType:
  case Guest, Registered, RegisteredCorporate

/** Delivery option selected by the customer */
enum DeliveryOption:
  case PickupAtShop(locationId: String)
  case CourierStandard
  case CourierExpress
  case CourierEconomy

/** Payment method selected by the customer */
enum PaymentMethod:
  /** Bank transfer with QR code — required for guests and non-approved users */
  case BankTransferQR
  /** Card payment — future implementation */
  case Card
  /** Invoice on account — available for approved corporate customers only */
  case InvoiceOnAccount

/** A physical shop / pickup location */
final case class ShopLocation(
    id: String,
    name: LocalizedString,
    address: LocalizedString,
)

/** A courier / shipping service option */
final case class CourierService(
    id: String,
    name: LocalizedString,
    estimatedDays: LocalizedString,
    surcharge: Money,
    currency: Currency,
)

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

/** Step in the checkout process */
enum CheckoutStep:
  case Authentication
  case ContactDetails
  case Delivery
  case Payment
  case Summary

/** All data collected during the checkout flow */
final case class CheckoutInfo(
    step: CheckoutStep = CheckoutStep.Authentication,
    customerType: CustomerType = CustomerType.Guest,
    loginEmail: String = "",
    loginPassword: String = "",
    contactInfo: ContactInfo = ContactInfo("", "", "", "", None, None, None),
    /** Invoice / billing address */
    invoiceAddress: Address = Address("", "", "", ""),
    /** When true, goods are shipped to the same address as the invoice address */
    shipToDifferentAddress: Boolean = false,
    /** Separate shipping address — only used when shipToDifferentAddress is true */
    shippingAddress: Option[Address] = None,
    discountCode: String = "",
    deliveryOption: Option[DeliveryOption] = None,
    paymentMethod: Option[PaymentMethod] = None,
    note: String = "",
)

/** A completed order */
final case class Order(
    id: OrderId,
    basket: Basket,
    checkoutInfo: CheckoutInfo,
    total: Money,
    currency: Currency,
)
