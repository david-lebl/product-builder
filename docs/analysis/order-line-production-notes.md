# Order-Line Production Notes vs. Customer ↔ Backoffice Conversation

> Decision record for how a customer gives optional manufacturing instructions for one order line, and how
> that relates to the existing artwork review feedback and a future order conversation.

## Question

When adding print artwork, a customer needs to attach an optional instruction for manufacturing to one order
line. The backoffice can already comment on artwork (request changes / reject) and the customer sees that.
Should the line note be built by extending that artwork feedback into a per-line customer ↔ staff
conversation, given that a general customer ↔ backoffice order conversation is planned for later?

## What exists today

| Mechanism | Owner | Shape | Visible to customer |
|---|---|---|---|
| `ArtworkStatus.ChangesRequested(notes)` / `Rejected(reason)` | staff | one reason per artwork, part of the review **state machine**, replaced by the next status | yes (amber box under the file) |
| `OrderLine.specNote` | staff | single internal spec override, edited via *Edit line* | no |
| `OrderView.customerNote` | customer | one order-level note entered at checkout (delivery / PO reference) | yes |
| `OrderInternalNote` | staff | append-only internal notes | no |

## Options

1. **Extend artwork feedback into a per-line/per-artwork thread** and let the customer's first message be the
   manufacturing instruction.
2. **Dedicated `productionNote` field on the line** (customer-authored spec input) — and, later, one order-level
   conversation whose messages may reference a line or an artwork.

## Decision: option 2

A production instruction and a conversation are different things:

- **A note is a spec, a conversation is history.** Prepress and manufacturing need *the current instruction*,
  as one value next to the product spec — not "scroll the thread and work out which message still applies".
  A thread can contain the instruction, but it can't *be* the instruction.
- **Threads at two levels would collide.** A per-line thread now plus an order-level thread later means two
  inboxes, two unread states and the question "where do I reply?". A single future order conversation whose
  messages carry an optional reference (`line`, `artwork`) covers per-line discussion without a second channel.
- **Artwork review stays a state machine.** `ChangesRequested` drives what the customer may do (re-upload after
  acceptance). Turning it into a thread would mix workflow state with chat. When the conversation arrives, a
  status change can additionally *post* a system message into it.
- **Cheapest to build and to remove nothing later.** One optional field with a default: no migration (JSONB
  documents decode the missing field as `None`), and it stays valid after the conversation is introduced.

## Resulting model (implemented)

- `CartLine.productionNote` / `OrderLine.productionNote: Option[String]` — customer-owned, normalised by
  `ProductionNote.normalize` (trimmed, blank → `None`, max 1000 chars). Separate from the staff-owned `specNote`.
- Entered in the configurator's artwork step, editable in the cart drawer, and editable in *My orders* while
  `OrderStatus.allowsCustomerArtworkUpload` (before acceptance) — the same window as free artwork uploads.
  Afterwards the API returns `OrderError.ProductionNoteLocked` (HTTP 409); changes go through staff.
- Each customer edit on a placed order appends an `OrderStatusEvent` to the audit timeline.
- Backoffice order detail shows the note per line as *Customer production instructions*.
- `ProductionSpec.customerInstructions` carries it into manufacturing; the generated Prepress step's notes
  include it.

## Future order conversation (sketch, not implemented)

```
OrderMessage(id, orderId, author: Customer | Employee | System, body,
             ref: Option[OrderLineId | ArtworkId], createdAt, readBy…)
```

- One thread per order; line/artwork filters are just views on `ref`.
- `ChangesRequested` / `Rejected` post a `System` message with `ref = artwork`.
- The production note remains a field; a message may *propose* a change, staff can then update the line
  (via `specNote`) or reopen the note.
