# Event Contract: RabbitMQ

The order service is the sole in-scope publisher. There is no in-scope consumer in
this feature (Warehouse/Invoicing are future features per the constitution's domain
boundary) — the exchange and queue declared here exist so a future consumer can bind
without requiring a change to this service.

## Topology

| Element | Name | Type | Notes |
|---|---|---|---|
| Exchange | `order.events` | `topic`, durable | Declared by the order service on startup. |
| Routing key | `order.intaken` | | Used for the `OrderIntaken` event. |
| Queue | `order.events.intaken` | durable, bound to `order.events` with routing key `order.intaken` | Declared by the order service so the event is durably captured even before a real consumer exists; a future Warehouse service may bind its own queue to the same routing key instead of consuming this one directly. |

## OrderIntaken

Published exactly once per successfully created order, after the creating
transaction commits (see research.md #7 — `AFTER_COMMIT`, never published for an
order that failed to persist).

### Payload (JSON)

```json
{
  "eventType": "OrderIntaken",
  "eventId": "b3f1c2b0-...-uuid",
  "occurredAt": "2026-08-18T15:04:00Z",
  "orderId": "b3f1c2b0-...-uuid",
  "clientId": "ACME-001",
  "lineItems": [
    { "sku": "SKU-1001", "quantity": 25, "unitListPrice": 899.00 }
  ],
  "grossTotal": 22475.00,
  "appliedDiscountPercentage": 12.5,
  "netTotal": 19665.63
}
```

| Field | Type | Notes |
|---|---|---|
| `eventType` | string | Always `"OrderIntaken"`; future event types share this envelope shape. |
| `eventId` | uuid | Unique per publish, for consumer dedup. |
| `occurredAt` | ISO-8601 timestamp | When the order was accepted into Intake. |
| `orderId` | uuid | The `BulkOrder.id`. |
| `clientId` | string | Owning client identifier. |
| `lineItems` | array | SKU, quantity, and unit list price as accepted at intake. |
| `grossTotal` | decimal | Per FR-002. |
| `appliedDiscountPercentage` | decimal | The contract discount snapshot applied. |
| `netTotal` | decimal | Per FR-003. |

### Delivery semantics

- At-least-once delivery (standard RabbitMQ durable queue/publisher-confirms); a
  future consumer MUST be idempotent, keyed on `eventId` or `orderId`.
- No retry/redelivery contract is defined for consumers in this feature's scope
  since none exists yet; this is left for the consuming feature to define.
