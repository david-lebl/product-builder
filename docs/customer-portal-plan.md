# Customer Portal — Analysis & Implementation Plan

## Overview

A customer-facing portal for logged-in customers to track their active orders, check recent history, and perform self-service actions like resending payment information or re-uploading artwork.

The portal uses a **tile-based card layout** for a modern, friendly customer experience — deliberately avoiding the standard split-table pattern used in admin/manufacturing views.

---

## Current Implementation — Use Case Analysis

### Use Case 1: Active Order Tracking

**Actor:** Logged-in customer (agency)

**Precondition:** Customer has placed one or more orders that are not yet fully dispatched.

**Main Flow:**
1. Customer navigates to "My Orders" in the top bar
2. Portal shows a summary bar with counts: Awaiting Payment, In Approval, In Production, Ready for Delivery
3. Each active order appears as a visual **tile card** showing:
   - Order ID and placement date
   - Payment status (Pending / Confirmed / Failed) with icon
   - Approval status (Placed / Approved / PendingChanges / Rejected / OnHold)
   - Production progress bar (percentage from workflow completion ratio)
   - Delivery/fulfilment progress (collection → quality → packaging → dispatch)
4. Customer can expand a tile to see full item details, pricing, and per-item configuration

**Active Order Definition:** An order where `isDispatched == false` (i.e. not yet fully dispatched).

### Use Case 2: Order Actions — Resend Payment Information

**Actor:** Logged-in customer with an order in `PaymentStatus.Pending` or `PaymentStatus.Failed`

**Flow:**
1. On the active order tile, customer sees a "Resend Payment Info" action button
2. Clicking triggers a confirmation and simulates resending payment details
3. A toast/message confirms the action

### Use Case 3: Order Actions — Re-upload Artwork

**Actor:** Logged-in customer with an order in `ApprovalStatus.PendingChanges`

**Flow:**
1. On the active order tile, customer sees a "Re-upload Artwork" action button
2. Customer sees artwork check notes (resolution, bleed, color profile status)
3. Clicking simulates re-uploading corrected artwork files
4. Approval status resets to `Placed` (awaiting re-review)

### Use Case 4: Order Detail View

**Actor:** Logged-in customer

**Flow:**
1. Customer clicks "View Details" on any order tile
2. Expanded detail shows:
   - Full item list with quantities, materials, configurations, per-item pricing
   - Order progress step indicator (Placed → In Production → Completed → Dispatched)
   - Tracking number (if dispatched)
   - Total price breakdown

### Use Case 5: Recent Orders (Last 30 Days)

**Actor:** Logged-in customer

**Flow:**
1. Below active orders, portal shows "Recent Orders" section
2. Displays orders from the last 30 days that have been dispatched/completed
3. Each tile shows: date, items summary, total price, status, tracking number
4. Customer can expand for full detail including pricing
5. This is NOT a complete history — only the last 30 days

---

## Implementation Details

### New Files

| File | Purpose |
|------|---------|
| `modules/ui/src/main/scala/mpbuilder/ui/components/CustomerPortalView.scala` | Main tile-based customer portal |
| `modules/ui/src/main/resources/customer-portal.css` | Card/tile layout styles |

### Modified Files

| File | Change |
|------|--------|
| `modules/ui/src/main/scala/mpbuilder/ui/AppRouter.scala` | Add `CustomerPortal` route, nav link for logged-in users |
| `modules/ui/src/main/resources/index.html` | Add `<link>` for `customer-portal.css` |

### Route & Navigation

- New `AppRoute.CustomerPortal` added to the sealed trait
- "My Orders" / "Moje objednávky" navigation link shown only when customer is logged in
- Existing `AppRoute.OrderHistory` route still works (redirects to portal)

### Tile Card Design

Each order tile includes:
- **Header**: Order ID (monospace) + date
- **Status badges**: Payment ✅⏳❌, Approval (badge colors), Artwork ✓/⚠/✗
- **Production progress**: Visual progress bar with percentage
- **Delivery progress**: Step indicator (collected → quality → packaged → dispatched)
- **Actions**: Context-sensitive buttons (resend payment, re-upload artwork, view detail)
- **Expandable detail**: Item list, pricing, configuration summary

### Data Source

All data derived from `ManufacturingOrder` list filtered by `Order.customerId`:
- Active orders: `!mo.isDispatched`
- Recent orders: `mo.isDispatched && mo.createdAt > (now - 30 days)`

---

## Future Plans & Proposed Features

### Near-Term (Next Release)

#### 1. Re-create Order from History
- "Reorder" button on recent/completed order tiles
- Loads the order's product configuration back into the product builder
- Pre-fills checkout info from customer profile
- **Benefit:** Repeat customers can quickly replicate previous orders

#### 2. Delivery Preferences
- Customer can set default delivery address and preferred courier service
- Saved in `Customer.deliveryPreferences` field (new domain model extension)
- Pre-populated during checkout
- **Benefit:** Faster checkout for returning customers

### Medium-Term (Upcoming Releases)

#### 3. Customer Personal Data Management
- View/edit contact info, company details, billing address
- GDPR-compliant data download (export all personal data as JSON/PDF)
- Account deletion request workflow
- **Benefit:** Self-service reduces support burden, GDPR compliance

#### 4. Invoice Access
- `invoiceNumber: Option[String]` already exists in domain model
- View/download invoices for completed orders
- PDF generation for invoice documents
- Monthly invoice summary
- **Benefit:** Customers can access financial documents independently

#### 5. Customer Dashboard with Statistics
- Order frequency chart (orders per month)
- Total spend over time
- Most frequently ordered products/materials
- Average order value
- Savings from customer pricing vs base pricing
- **Benefit:** Customer engagement, upselling opportunities

### Long-Term (Future Roadmap)

#### 6. Order Notifications
- Email/in-app notifications for status changes
- Payment reminders for pending orders
- Dispatch notifications with tracking links
- Artwork review feedback alerts
- **Benefit:** Proactive communication improves customer experience

#### 7. Saved Configurations / Templates
- Save product configurations as named templates
- Quick-order from saved templates
- Share templates between team members (same company)
- **Benefit:** Power users can build custom product catalogs

#### 8. Multi-User Company Accounts
- Multiple users under one company account
- Role-based permissions (admin, orderer, viewer)
- Shared order history and spending limits
- **Benefit:** Enterprise customer support

#### 9. Customer-Specific Product Recommendations
- Based on order history and customer tier
- Suggested materials/finishes based on past preferences
- Seasonal promotions and tier-based offers
- **Benefit:** Increased order value and customer retention

#### 10. Order Communication Thread
- Per-order messaging between customer and production team
- Artwork revision comments and file exchange
- Status update timeline with comments
- **Benefit:** Centralized communication reduces email overhead

#### 11. Bulk Order Management
- Upload CSV/spreadsheet with multiple order items
- Batch pricing calculation
- Progress tracking for all items in bulk order
- **Benefit:** Large agency customers with volume orders

#### 12. Customer API Access
- REST/GraphQL API for programmatic order placement
- Webhook notifications for order status changes
- Integration with customer's own systems (ERP, CRM)
- **Benefit:** Technical customers can automate their workflows

---

## Technical Notes

### Domain Model Compatibility

The customer portal relies entirely on existing domain models:
- `ManufacturingOrder` — contains all status fields (approval, payment, artwork, fulfilment)
- `PaymentStatus` — Pending / Confirmed / Failed
- `ApprovalStatus` — Placed / Approved / Rejected / PendingChanges / OnHold
- `ArtworkCheck` — resolution, bleed, colorProfile checks with CheckStatus
- `FulfilmentChecklist` — collectedItems, qualitySignOff, packagingInfo, dispatchInfo
- `WorkflowStatus` — Pending / InProgress / Completed / OnHold / Cancelled

No domain model changes are required for the initial implementation.

### UI Pattern — Tiles vs Tables

The customer portal deliberately uses a **tile/card layout** instead of the `SplitTableView` pattern used in admin views. Reasons:
- Customer-facing views should be visually warmer and less "admin-like"
- Tiles naturally accommodate varied content density (some orders have tracking, some don't)
- Mobile-friendly — tiles stack vertically without horizontal scrolling
- Each tile is self-contained with its own actions, reducing cognitive load

### i18n

All user-facing text supports EN/CS localization via the `Language` enum pattern used throughout the application.
