# Implementation progress

Work proceeds in order; verification entries distinguish executed checks from unavailable checks.

1. Foundation — COMPLETE (Maven build; 5 PostgreSQL-backed tests passed): service scaffolding, schemas/seeds, CRUD, Compose, API docs.
2. Inventory concurrency — COMPLETE (8 total Java tests passed, including concurrent HTTP requests): row locks, atomic multi-line reservations, PostgreSQL concurrency tests.
3. Kafka — COMPLETE (10 Java tests; real Compose order→Kafka→inventory→Kafka→order concurrency smoke passed): outbox/inbox, retries/DLT, full workflow tests.
4. Security — IN PROGRESS: JWT/RBAC, audit, validation, throttling.
5. Forecasting — pending: synthetic series, leakage-free temporal evaluation, API/reorder math.
6. LangGraph — pending: exactly four agents, typed state, allowlisted tools, routing tests.
7. Approval/security — pending: persistent decision workflow, authorization and adversarial tests.
8. Frontend — pending: seven pages, operational cards/trend, role-aware actions.
9. Portfolio polish — pending: full integration checks, threat model, demo scripts, screenshots, README.

## Environment
Workspace initially empty. Host Java 25 can compile Java 21 targets; production images use Java 21. Docker Desktop initially stopped; starting it for real PostgreSQL/Kafka verification.
