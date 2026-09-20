# StockGuard AI

Secure inventory operations with PostgreSQL concurrency control, Kafka reservation events, and a controlled LangGraph operations copilot.

Implementation is in progress. See [progress](docs/PROGRESS.md), [architecture](docs/ARCHITECTURE.md), and [decisions](docs/DECISIONS.md).

## Run locally

Requires Docker Desktop / Docker Compose. Run `./scripts/dev.sh` to enter secrets interactively, or export the variables listed in `.env.example` then run `docker compose up --build`. No real `.env` is created. Reuse the same database passwords when restarting an existing PostgreSQL volume.

Service endpoints: Product `http://localhost:8081`, Inventory `http://localhost:8082`, Orders `http://localhost:8083`. Each exposes `/swagger-ui/index.html`, `/v3/api-docs` and `/actuator/health`.

Java checks: `mvn -f backend/pom.xml verify` (Java 21+, Maven, and running Docker required). PostgreSQL integration tests deliberately require Docker rather than silently substituting H2 or skipping concurrency checks.
