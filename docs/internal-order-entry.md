# Internal Order Entry

> A compact alternative to the standard Product Parameters view, designed for experienced employees entering orders on behalf of customers.

## Purpose

The standard Product Parameters view is designed for customers — it explains each field with contextual help buttons, informational boxes, and descriptive text. Internal employees who know the product catalog well find all this extra information unnecessary and prefer a denser, faster-to-fill interface.

The Internal Order Entry view provides identical product configuration functionality in a compact, space-efficient layout.

## Route

`AppRoute.InternalOrderEntry` — accessible from the main navigation bar ("Internal Order Entry" / "Interní objednávka").

## Customer Selector

At the top of the form is a customer dropdown containing all customers from the system (`SampleCustomers.all`). When a customer is selected:

- Their customer tier badge is shown next to the selector (Standard / Silver / Gold / Platinum).
- Any global discount is shown in a notice below the selector.
- Customer-specific pricing is applied to the price calculation — the same pricing resolution used when a customer is logged in via the standard Product Parameters OTP flow.

Selecting "No customer (list price)" removes the customer override and prices at standard list rates.

## Compact Layout

The form is wrapped in a `.compact-form` CSS class that applies these overrides:

| Element | Standard layout | Compact layout |
|---------|----------------|----------------|
| `form-group` label | Block, above the input | Inline, 160 px wide, left of input |
| `help-info-wrapper` (? buttons) | Visible | Hidden |
| `info-box` elements | Visible | Hidden |
| `selector-help-buttons` | Visible | Hidden |
| Section spacing | 25 px margin | 12 px margin |
| Speed tier cards | Vertical stack | 3-column grid |

## Same Functionality

Every configuration option available in Product Parameters is also available in Internal Order Entry:

1. Category & Preset selection
2. Product Specifications (quantity, size, pages, orientation, fold type, binding method)
3. Printing Method
4. Components (material, ink configuration, finishes with parameters)
5. Manufacturing Speed (Express / Standard / Economy) with live completion estimates
6. Add to Basket

The same basket is shared between both views. Price validation is live (recalculates automatically on every configuration change).

## Technical Notes

- `InternalOrderEntryApp` lives in `mpbuilder.ui.internalorder` package.
- Customer selection is stored as `internalOrderCustomerId: Option[CustomerId]` in `BuilderState`. The `loginState` field (OTP-based login) takes priority over this field.
- A pre-computed `allCustomersById: Map[CustomerId, Customer]` is used for O(1) customer lookup in both the reactive signal and the pricing calculation.
