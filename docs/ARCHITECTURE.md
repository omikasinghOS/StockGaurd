# StockGuard AI architecture

Four business services: Product (catalog and authentication), Inventory (stock, warehouses, audit), Order (order lifecycle), AI (forecasting, controlled LangGraph tools and approval records). A shared Java library contains only transport/security plumbing. PostgreSQL hosts isolated databases; services do not query each other's tables. REST serves synchronous reads. Kafka carries reservation requests and outcomes. React uses a same-origin reverse proxy.

```mermaid
flowchart LR
  UI[React dashboard] --> P[Product / Auth]
  UI --> I[Inventory]
  UI --> O[Orders]
  UI --> A[FastAPI / LangGraph]
  O -->|transactional outbox: OrderCreated| K[Kafka]
  K --> I
  I -->|outbox: reservation outcome + low stock| K
  K --> O
  A -->|read-only authorized REST tools| P
  A -->|read-only authorized REST tools| I
  A -->|read-only authorized REST tools| O
  P --> PD[(Product DB)]
  I --> ID[(Inventory DB)]
  O --> OD[(Order DB)]
  A --> AD[(AI approval DB)]
```
