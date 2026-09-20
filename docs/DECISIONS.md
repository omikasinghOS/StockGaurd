# Decisions

- Java 21 / Spring Boot 3.5; three Maven application modules plus a small shared library (not a service).
- PostgreSQL row locks serialize reservations of the same stock row. Multi-item reservations lock rows in product-ID order and validate all lines before any decrement.
- Transactional outbox + transactional consumer inbox: at-least-once delivery without lost DB/event dual writes. Inventory also deduplicates by order ID.
- Single warehouse per order keeps V1 allocation understandable. No payment, supplier purchase execution, cross-warehouse allocation or cancellation after reservation in V1.
- Auth belongs to Product Service to avoid another business service. Every service verifies JWTs locally and enforces RBAC.
- No Redis: bounded in-process sensitive-endpoint throttling is sufficient for the single-replica demo, explicitly not a global distributed rate limiter.
- Deterministic offline routing and ML forecasts make demos reproducible without paid LLM calls. Any optional language-model explanation must have no tool credentials or authority over calculations.
- High-value reorder decisions are persisted for human approval; approval never automatically places a supplier order.
