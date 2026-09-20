# Inventory transaction design

Two clients can both read quantity=1 before either writes. A read-check-write sequence at PostgreSQL READ COMMITTED without a lock can approve both purchases. Java `synchronized` only covers one JVM and cannot protect separate replicas.

Reservations use `SELECT ... FOR UPDATE` inside one database transaction. All product rows are locked in UUID order to reduce deadlocks. The transaction verifies every requested quantity before decrementing any row. Insufficient stock is a business outcome, with no partial reservation. A database CHECK constraint independently prohibits negative available/reserved quantities. JPA `@Version` also detects stale updates, but reservation correctness relies on row locks, not a retry loop.

The transaction includes: the event inbox claim (when Kafka-triggered), per-order reservation claim, stock mutation, audit records, and outgoing result/low-stock outbox records. A crash rolls these all back together. Outbox publishing is outside the request transaction and can be repeated after a crash; consumer inbox IDs make replay harmless.

Different products can reserve in parallel. A hot SKU serializes at the database and may become a throughput bottleneck. Transactions must stay short; no network call belongs inside the locked inventory transaction. PostgreSQL lock/statement timeouts bound waiting, and Kafka retries technical failures. The fixed demo warehouses do not implement cross-warehouse allocation.

Orders model accepted demand: `availableQuantity` excludes reserved units; `reservedQuantity` records outstanding commitments. Shipping/fulfillment and post-reservation cancellation/release are deliberately outside V1. Rejected orders do not reserve anything. Order transitions and result handling must reject contradictory or duplicate terminal outcomes.
