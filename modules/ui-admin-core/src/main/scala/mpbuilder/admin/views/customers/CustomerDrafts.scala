package mpbuilder.admin.views.customers

import mpbuilder.customers.*
import mpbuilder.shared.*

/** Plain-string form drafts for customer creation & editing, with the same rules the customers core enforces.
  *
  * Validation returns a `Map[fieldKey, message]` so the view can mark each field inline. Field keys are dotted paths
  * (`primary.email`, `extras.1.firstName`, `shipping.0.city`, ...).
  */
private[views] object CustomerDrafts:

  type Errors = Map[String, String]

  private final class Msg(isCs: Boolean):
    def required: String     = if isCs then "Povinné pole." else "Required."
    def email: String        = if isCs then "Neplatná e-mailová adresa." else "Enter a valid email address."
    def emailTaken(owner: String): String =
      if isCs then s"E-mail už používá zákazník „$owner“." else s"Email is already used by “$owner”."
    def emailDuplicate: String = if isCs then "Tento e-mail už má jiná kontaktní osoba." else "Another contact already uses this email."
    def idTaken: String        = if isCs then "Toto ID už existuje." else "This ID is already in use."
    def idFormat: String       = if isCs then "Použijte malá písmena, číslice a pomlčky." else "Use lowercase letters, digits and dashes."
    def dueDays: String        = if isCs then "Zadejte 1 až 90 dní." else "Enter 1 to 90 days."
    def limit: String          = if isCs then "Zadejte kladnou částku." else "Enter a positive amount."

  private def req(v: String): Boolean = v.trim.nonEmpty
  private def nes(v: String): NonEmptyString = NonEmptyString.unsafe(v.trim)
  private def optNes(v: String): Option[NonEmptyString] = Option.when(v.trim.nonEmpty)(nes(v))
  def parseEmail(v: String): Option[EmailAddress] = EmailAddress(v.trim).toEither.toOption

  // ── Contacts ───────────────────────────────────────────────────────────────

  final case class ContactDraft(firstName: String, lastName: String, email: String, phone: String, roleTitle: String)

  object ContactDraft:
    val empty: ContactDraft = ContactDraft("", "", "", "", "")
    def from(cp: ContactPerson): ContactDraft = ContactDraft(
      cp.firstName.value,
      cp.lastName.value,
      cp.email.value,
      cp.phone.map(_.value).getOrElse(""),
      cp.roleTitle.map(_.value).getOrElse(""),
    )

  private def validateContact(prefix: String, d: ContactDraft, m: Msg): Errors =
    List(
      Option.when(!req(d.firstName))(s"$prefix.firstName" -> m.required),
      Option.when(!req(d.lastName))(s"$prefix.lastName" -> m.required),
      if !req(d.email) then Some(s"$prefix.email" -> m.required)
      else Option.when(parseEmail(d.email).isEmpty)(s"$prefix.email" -> m.email),
    ).flatten.toMap

  private def toContact(d: ContactDraft): ContactPerson = ContactPerson(
    firstName = nes(d.firstName),
    lastName = nes(d.lastName),
    email = parseEmail(d.email).getOrElse(EmailAddress.unsafe(d.email.trim.toLowerCase)),
    phone = optNes(d.phone),
    roleTitle = optNes(d.roleTitle),
  )

  // ── Addresses ──────────────────────────────────────────────────────────────

  final case class AddressDraft(street: String, city: String, postalCode: String, countryCode: String)

  object AddressDraft:
    val empty: AddressDraft = AddressDraft("", "", "", "CZ")
    def from(a: Address): AddressDraft = AddressDraft(a.street.value, a.city.value, a.postalCode.value, a.countryCode.value)

  private def validateAddress(prefix: String, d: AddressDraft, m: Msg): Errors =
    List(
      Option.when(!req(d.street))(s"$prefix.street" -> m.required),
      Option.when(!req(d.city))(s"$prefix.city" -> m.required),
      Option.when(!req(d.postalCode))(s"$prefix.postalCode" -> m.required),
      Option.when(!req(d.countryCode))(s"$prefix.countryCode" -> m.required),
    ).flatten.toMap

  private def toAddress(d: AddressDraft): Address =
    Address(nes(d.street), nes(d.city), nes(d.postalCode), nes(d.countryCode.toUpperCase))

  /** Emails of every other customer's contacts → that customer's display name (duplicate-email guard). */
  def emailOwners(customers: List[CustomerView], except: Option[CustomerId]): Map[String, String] =
    customers
      .filterNot(c => except.contains(c.id))
      .flatMap(c => (c.contact :: c.additionalContacts).map(_.email.value.toLowerCase -> CustomerUi.displayName(c)))
      .toMap

  private def validateEmailsUnique(contacts: List[(String, ContactDraft)], owners: Map[String, String], m: Msg): Errors =
    val normalized = contacts.map { case (k, d) => k -> d.email.trim.toLowerCase }.filter(_._2.nonEmpty)
    val taken = normalized.flatMap { case (k, e) => owners.get(e).map(o => s"$k.email" -> m.emailTaken(o)) }
    val dupes = normalized
      .groupBy(_._2)
      .values
      .filter(_.size > 1)
      .flatMap(_.drop(1).map { case (k, _) => s"$k.email" -> m.emailDuplicate })
    (taken ++ dupes).toMap

  // ── Profile editing ────────────────────────────────────────────────────────

  enum Section:
    case Company, Contacts, Addresses, Tags

  final case class ProfileDraft(
      customerId: CustomerId,
      section: Section,
      companyName: String,
      regNo: String,
      vatId: String,
      primary: ContactDraft,
      extras: List[ContactDraft],
      billing: AddressDraft,
      shipping: List[AddressDraft],
      defaultShipping: Int,
      tags: String,
      showErrors: Boolean = false,
  )

  object ProfileDraft:
    def from(c: CustomerView, section: Section): ProfileDraft = ProfileDraft(
      customerId = c.id,
      section = section,
      companyName = c.company.map(_.legalName.value).getOrElse(""),
      regNo = c.company.map(_.registrationNumber.value).getOrElse(""),
      vatId = c.company.flatMap(_.vatId.map(_.value)).getOrElse(""),
      primary = ContactDraft.from(c.contact),
      extras = c.additionalContacts.map(ContactDraft.from),
      billing = AddressDraft.from(c.billingAddress),
      shipping = c.shippingAddresses.map(AddressDraft.from),
      defaultShipping = c.defaultShippingIndex.getOrElse(0),
      tags = c.tags.toList.sorted.mkString(", "),
    )

  def parseTags(raw: String): Set[String] = raw.split(",").map(_.trim).filter(_.nonEmpty).toSet

  def validateProfile(d: ProfileDraft, segment: CustomerSegment, owners: Map[String, String], isCs: Boolean): Errors =
    val m = Msg(isCs)
    val company =
      if CustomerUi.isB2B(segment) then
        List(
          Option.when(!req(d.companyName))("companyName" -> m.required),
          Option.when(!req(d.regNo))("regNo" -> m.required),
        ).flatten.toMap
      else Map.empty
    val contacts = ("primary" -> d.primary) :: d.extras.zipWithIndex.map { case (cd, i) => s"extras.$i" -> cd }
    company ++
      contacts.flatMap { case (k, cd) => validateContact(k, cd, m) } ++
      validateAddress("billing", d.billing, m) ++
      d.shipping.zipWithIndex.flatMap { case (a, i) => validateAddress(s"shipping.$i", a, m) } ++
      validateEmailsUnique(contacts, owners, m)

  /** Current customer state as an update command (base for quick actions such as "Make primary"). */
  def currentCmd(c: CustomerView): UpdateCustomerProfileCmd = UpdateCustomerProfileCmd(
    id = c.id,
    company = c.company,
    contact = c.contact,
    billingAddress = c.billingAddress,
    tags = c.tags,
    additionalContacts = Option.when(CustomerUi.isB2B(c.segment))(c.additionalContacts),
    shippingAddresses = Some(c.shippingAddresses),
    defaultShippingIndex = c.defaultShippingIndex,
  )

  /** Builds the update command from a draft that passed [[validateProfile]]. */
  def profileCmd(d: ProfileDraft, c: CustomerView): UpdateCustomerProfileCmd =
    val primary  = toContact(d.primary)
    val shipping = d.shipping.map(toAddress)
    UpdateCustomerProfileCmd(
      id = c.id,
      company = Option.when(CustomerUi.isB2B(c.segment))(
        CompanyProfile(
          legalName = nes(d.companyName),
          registrationNumber = nes(d.regNo),
          vatId = optNes(d.vatId),
          contactPerson = NonEmptyString.unsafe(CustomerUi.personName(primary)),
        )
      ),
      contact = primary,
      billingAddress = toAddress(d.billing),
      tags = parseTags(d.tags),
      additionalContacts = Option.when(CustomerUi.isB2B(c.segment))(d.extras.map(toContact)),
      shippingAddresses = Some(shipping),
      defaultShippingIndex = Option.when(shipping.nonEmpty)(d.defaultShipping.max(0).min(shipping.size - 1)),
    )

  /** Promotes additional contact `idx` to primary; the former primary becomes an additional contact. */
  def promoteContactCmd(c: CustomerView, idx: Int): Option[UpdateCustomerProfileCmd] =
    c.additionalContacts.lift(idx).map { promoted =>
      val rest = c.contact :: c.additionalContacts.patch(idx, Nil, 1)
      currentCmd(c).copy(
        contact = promoted,
        additionalContacts = Some(rest),
        company = c.company.map(_.copy(contactPerson = NonEmptyString.unsafe(CustomerUi.personName(promoted)))),
      )
    }

  def removeContactCmd(c: CustomerView, idx: Int): UpdateCustomerProfileCmd =
    currentCmd(c).copy(additionalContacts = Some(c.additionalContacts.patch(idx, Nil, 1)))

  def setDefaultShippingCmd(c: CustomerView, idx: Int): UpdateCustomerProfileCmd =
    currentCmd(c).copy(defaultShippingIndex = Some(idx))

  def removeShippingCmd(c: CustomerView, idx: Int): UpdateCustomerProfileCmd =
    val next   = c.shippingAddresses.patch(idx, Nil, 1)
    val curDef = c.defaultShippingIndex.getOrElse(0)
    val nextDef =
      if next.isEmpty then None
      else if idx < curDef then Some(curDef - 1)
      else if idx == curDef then Some(0)
      else Some(curDef)
    currentCmd(c).copy(shippingAddresses = Some(next), defaultShippingIndex = nextDef)

  // ── Registration ───────────────────────────────────────────────────────────

  final case class RegisterDraft(
      segment: CustomerSegment = CustomerSegment.Business,
      companyName: String = "",
      regNo: String = "",
      vatId: String = "",
      contact: ContactDraft = ContactDraft.empty,
      billing: AddressDraft = AddressDraft.empty,
      customId: String = "",
      showErrors: Boolean = false,
  )

  /** Customer IDs: lowercase letters, digits and dashes, starting with a letter or digit.
    *
    * Deliberately regex-free: this code runs in the browser, where Scala.js (targeting ES2015) cannot compile
    * Unicode property classes such as `\p{M}`, and a pattern error thrown while rendering freezes the UI.
    */
  private def isIdChar(c: Char): Boolean = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')

  def isValidId(id: String): Boolean = id.nonEmpty && isIdChar(id.head) && id.forall(c => isIdChar(c) || c == '-')

  /** `"Ateliér Nová s.r.o."` → `"atelier-nova-s-r-o"`: strip diacritics (NFD + drop combining marks), lowercase,
    * collapse every other run of characters into a single dash, max 40 characters.
    */
  private[customers] def slug(raw: String): String =
    val decomposed = java.text.Normalizer.normalize(raw.toLowerCase, java.text.Normalizer.Form.NFD)
    val sb         = new StringBuilder
    decomposed.foreach { c =>
      if c >= '\u0300' && c <= '\u036f' then () // combining diacritical mark
      else if isIdChar(c) then sb.append(c)
      else if sb.nonEmpty && sb.last != '-' then sb.append('-')
    }
    val trimmed = sb.result().take(40)
    trimmed.reverse.dropWhile(_ == '-').reverse

  /** Readable, collision-free id derived from company or person name (`cust-atelier-nova`, `cust-atelier-nova-2`, ...). */
  def suggestedId(d: RegisterDraft, existing: Set[String]): String =
    val source =
      if CustomerUi.isB2B(d.segment) && d.companyName.trim.nonEmpty then d.companyName
      else s"${d.contact.firstName} ${d.contact.lastName}".trim match
        case ""   => d.contact.email.takeWhile(_ != '@')
        case name => name
    val base = s"cust-${Option(slug(source)).filter(_.nonEmpty).getOrElse("new")}"
    if !existing.contains(base) then base
    else Iterator.from(2).map(n => s"$base-$n").find(id => !existing.contains(id)).getOrElse(base)

  def validateRegister(d: RegisterDraft, existingIds: Set[String], owners: Map[String, String], isCs: Boolean): Errors =
    val m = Msg(isCs)
    val company =
      if CustomerUi.isB2B(d.segment) then
        List(
          Option.when(!req(d.companyName))("companyName" -> m.required),
          Option.when(!req(d.regNo))("regNo" -> m.required),
        ).flatten.toMap
      else Map.empty
    val id = d.customId.trim
    val idErr =
      if id.isEmpty then None
      else if !isValidId(id) then Some("customId" -> m.idFormat)
      else Option.when(existingIds.contains(id))("customId" -> m.idTaken)
    company ++
      validateContact("contact", d.contact, m) ++
      validateAddress("billing", d.billing, m) ++
      idErr.toMap ++
      validateEmailsUnique(List("contact" -> d.contact), owners, m)

  def registerCmd(d: RegisterDraft, existingIds: Set[String]): RegisterCustomerCmd =
    val contact = toContact(d.contact)
    RegisterCustomerCmd(
      id = CustomerId.unsafe(if d.customId.trim.nonEmpty then d.customId.trim else suggestedId(d, existingIds)),
      segment = d.segment,
      company = Option.when(CustomerUi.isB2B(d.segment))(
        CompanyProfile(
          legalName = nes(d.companyName),
          registrationNumber = nes(d.regNo),
          vatId = optNes(d.vatId),
          contactPerson = NonEmptyString.unsafe(CustomerUi.personName(contact)),
        )
      ),
      contact = contact,
      billingAddress = toAddress(d.billing),
    )

  // ── Payment terms ──────────────────────────────────────────────────────────

  enum TermsKind:
    case Prepaid, Net

  final case class TermsDraft(
      customerId: CustomerId,
      kind: TermsKind,
      dueDays: String,
      limit: String,
      currency: Currency,
      showErrors: Boolean = false,
  ):
    /** Compares only the editable values (ignores the error-display flag). */
    def sameAs(other: TermsDraft): Boolean =
      kind == other.kind && (kind == TermsKind.Prepaid ||
        (dueDays.trim == other.dueDays.trim && parseAmount(limit) == parseAmount(other.limit) && currency == other.currency))

  private def parseAmount(raw: String): Option[BigDecimal] =
    scala.util.Try(BigDecimal(raw.trim.replace(" ", "").replace(",", "."))).toOption

  object TermsDraft:
    def from(c: CustomerView): TermsDraft = c.paymentTerms match
      case PaymentTerms.PrepaidOnly =>
        TermsDraft(c.id, TermsKind.Prepaid, "14", "50000", Currency.CZK)
      case PaymentTerms.NetInvoice(d, lim) =>
        TermsDraft(c.id, TermsKind.Net, d.toString, lim.amount.value.bigDecimal.stripTrailingZeros.toPlainString, lim.currency)

  def validateTerms(d: TermsDraft, isCs: Boolean): Errors =
    if d.kind == TermsKind.Prepaid then Map.empty
    else
      val m = Msg(isCs)
      List(
        Option.when(!d.dueDays.trim.toIntOption.exists(n => n >= 1 && n <= 90))("dueDays" -> m.dueDays),
        Option.when(!parseAmount(d.limit).exists(_ > 0))("limit" -> m.limit),
      ).flatten.toMap

  def termsValue(d: TermsDraft): PaymentTerms = d.kind match
    case TermsKind.Prepaid => PaymentTerms.PrepaidOnly
    case TermsKind.Net =>
      PaymentTerms.NetInvoice(d.dueDays.trim.toInt, Price(Money(parseAmount(d.limit).getOrElse(BigDecimal(1))), d.currency))

end CustomerDrafts
