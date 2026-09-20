# Kafka delivery and consistency

Envelope: `eventId` (UUID), `eventType`, `timestamp` (UTC), `correlationId`, `payload`.

| Topic | Producer | Consumer group | Key |
|---|---|---|---|
| stockguard.OrderCreated | Order outbox | inventory-reservations-v1 | orderId |
| stockguard.InventoryReserved | Inventory outbox | order-results-v1 | orderId |
| stockguard.InventoryReservationFailed | Inventory outbox | order-results-v1 | orderId |
| stockguard.LowStockDetected | Inventory outbox | reserved for future notifications | productId |

Each has three partitions and a matching `.DLT` with three partitions. Partition keys preserve order within a topic/partition, not across topics. Low-stock events are produced when successful stock changes leave quantity at/below the reorder point; no artificial consumer is added just to use Kafka.

Order creation writes the order and outbox row in one PostgreSQL transaction. The relay locks up to ten unpublished rows with `SKIP LOCKED`, waits for broker acknowledgment, then marks them published. On crash between publish and commit, it resends the same event ID. Kafka producer idempotence reduces retries within one producer session; it does not replace application deduplication.

Consumers claim `processed_events.event_id` with `INSERT ... ON CONFLICT DO NOTHING` inside the business transaction. Duplicate IDs return without repeating changes. A failed transaction rolls back the claim so retry can succeed. Reservations additionally deduplicate by order ID and reject changed payloads. Record offsets commit only after the listener returns. Technical failures retry three times (one-second intervals) then publish to `.DLT`; malformed events go directly to `.DLT`. Failed DLT publishing throws so the source record is not silently discarded.

Successful orders transition `PENDING → INVENTORY_RESERVED → CONFIRMED` in one result transaction; the middle state is conceptual, not a separate durable workflow stage. Failure produces `REJECTED`. Subsequent terminal outcomes cannot reopen an order. Client retries must reuse `Idempotency-Key`; keys are globally unique and must also be checked against owner and payload.

V1 limitations: one broker (no HA), trusted internal producers, demo plaintext broker transport, no automatic DLT replay or retention janitor. Before replay, repair the cause, review payload/authorization and publish the original event preserving its ID. Monitor outbox age and DLTs. Pruning inbox entries before the replay horizon risks duplicate processing. A dead-lettered order can remain PENDING until operator intervention; no pretend success or compensating workflow is introduced.
