# Verification strategy

`mvn -f backend/pom.xml verify` compiles for Java 21 and runs JUnit + Spring MockMvc + real PostgreSQL Testcontainers. Testcontainers uses Docker API 1.44 for compatibility with current Docker Desktop. Docker must be running. No embedded H2 replacement is used for lock correctness.

Critical checks target invariants, not implementation details:

- Three simultaneous requests against one available unit produce one success and two unavailable results; available stock=0, reserved=1.
- Multi-line failure changes no stock; duplicate reservation requests return the stored outcome.
- Duplicate Kafka event IDs claim one inbox record; replay does not reserve twice.
- Unauthenticated callers get 401; viewers cannot adjust stock or read security audit; manager actions carry an audit actor.
- A real Compose scenario places HTTP orders, traverses Kafka, waits for terminal outcomes, and checks database stock.
- Python tests use synthetic data and offline graph execution: no paid language-model calls. Forecast evaluation uses chronological holdout; graph routing, tools, arithmetic, role boundaries and approval state transitions have direct assertions.

Latest executed results are recorded in `PROGRESS.md`; test presence alone is not evidence that a test passed.
