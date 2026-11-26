# Backoffice — Customer Management

> What staff can do with customer accounts in the Backoffice (`#/admin/customers`). Read-only for `Operator` / `Prepress`;
> all mutations need `Manager` or `Admin`.

## Customer list

- Master-detail table: Customer / Company, Segment, Tier, Status, Payment Terms, Contacts.
- Search: contact names & emails (primary and additional), company name, IČO, customer ID, tags.
- Filters: Status, Tier, Segment.
- Arriving from the dashboard's pending-approval list ("Open") opens that customer's detail directly.

## Creating a customer (`+ New Customer`)

- **Customer type** segmented control: Business, Agency, Individual.
  - Business / Agency: company block (name*, IČO*, DIČ) is shown; account starts as *Pending approval*.
  - Individual: no company block; account is *Active* immediately and is always prepaid.
- **Primary contact**: first name*, last name*, email*, phone, role (role for B2B only).
- **Billing address**: street*, city*, postal code*, country code* (default `CZ`).
- **Customer ID** is optional. When it is empty, the system generates a readable ID from the company or person name
  (`cust-atelier-nova`, then `-2`, `-3` … if that ID is taken).
- Validation is shown inline after the first save attempt: required fields, email format, an email already used by
  another customer (names that customer), and an ID that is malformed or already taken.
- After saving, the new customer opens in the detail pane.
- The backend also rejects a duplicate customer ID (`CustomerIdAlreadyExists`, HTTP 409), so an existing account is
  never overwritten.

## Customer detail

### Header

- Avatar, display name, primary contact and email; badges for status, tier, segment and IČO; ID and "since" date.
- **Buttons that are always visible** (Manager / Admin):
  - *Approve Account* (pending) or *Reactivate* (suspended). Both ask for confirmation first.
  - *Add note*, which jumps to Notes & Activity.
- **More** menu: edit company / contacts / addresses (opens that section's editor), change tier & payment terms,
  manage agreements in Pricing, *Suspend* (active accounts only, reason required), and *Deactivate* (reason required,
  cannot be undone, revokes invoice terms).
- **Status banner**:
  - Pending: a reminder to verify the company details; invoice terms need approval first.
  - Suspended: the latest suspension reason and date.
  - Deactivated: the account is read-only.
- **KPI tiles**: Orders (with in-progress count), Lifetime value (non-cancelled orders, per currency, last order date),
  Unpaid (non-cancelled orders that are not paid or refunded), Payment terms. Clicking a tile opens the matching tab.

### Tabs

| Tab | Content |
|-----|---------|
| Profile | Cards for **Company** (B2B), **Contacts**, **Addresses**, **Tags**. Each card has its own *Edit* button and opens an inline editor (one section at a time) with *Save changes* / *Cancel*. |
| Pricing & Terms | **Pricing tier**, **Payment terms**, **Price agreements** (see below). |
| Orders | Customer's orders, newest first, with status, payment status and total. Each row opens Order Detail. |
| Artworks | Artwork files uploaded on the customer's orders (preview, download, copy link). |
| Notes & Activity | Internal note composer and notes (newest first), plus the audit trail (newest first). |

**Contacts** (Business / Agency):
- *Add contact* starts editing with a new empty contact.
- *Make primary* swaps an additional contact with the primary contact and updates the company's contact person.
- *Remove* asks for confirmation.
- Two contacts on one account cannot share an email.
- Individuals have exactly one contact.

**Addresses**:
- The billing address and any number of delivery addresses.
- *Add address* starts editing with a new empty delivery address.
- *Set as default* and *Remove* (asks for confirmation) work without opening the editor.
- With no delivery address, the billing address is used.

**Pricing tier**:
- Choosing a tier only previews it: the card shows that tier's default agreement, or notes that the public pricelist
  applies.
- *Apply tier change* saves it and records an audit entry.

**Payment terms**:
- Prepaid / Net invoice selector. The editor starts from the customer's current terms.
- Net invoice has due days (1–90, with quick picks 7 / 14 / 30 / 60), a credit limit (> 0) and a currency.
- *Save* appears only when something has changed.
- Net invoice is available only for **Active Business / Agency** accounts. Otherwise the card explains why. If an
  ineligible account still has invoice terms, it offers a switch to prepaid.
- With invoice terms, a **credit usage** meter compares unpaid orders in the limit's currency against the limit
  (amber at 80 %, rose above 100 %).

**Price agreements**:
- Lists customer-specific agreements and the default agreement inherited from the tier.
- A customer-specific agreement takes precedence over the tier default in the same currency.
- The *Create custom agreement* / *Manage agreements in Pricing* button opens Pricing → Agreements, filtered to this
  customer.

Deactivated accounts are read-only: no edit buttons, no terms editing.
