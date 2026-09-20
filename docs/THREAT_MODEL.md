# STRIDE threat model

Assets: stock integrity, catalog prices, order commitments, credentials, audit evidence, forecasts and approval decisions. Trust boundaries: browser→API, API→database, service→Kafka, copilot→allowlisted REST tools, manager→approval API.

| Threat | Example | V1 mitigation | Remaining limitation |
|---|---|---|---|
| Spoofing | Forged token or stolen credentials | BCrypt hashes, signed short-lived JWT, issuer/audience/expiry validation, generic login errors, login throttling | No MFA or token revocation; require TLS outside localhost |
| Tampering | Viewer changes stock; forged event | Backend role checks, validated DTOs, DB constraints/transactions, isolated service databases, private container network | Kafka is PLAINTEXT in local demo; production requires authenticated brokers and ACLs |
| Repudiation | Manager denies adjustment | Transactional audit with actor, role, time, before/after, reason and correlation ID | DB administrators can alter records; no external immutable archive |
| Information disclosure | Prompt asks for all credentials | No arbitrary SQL/shell tools, JWT forwarded to bounded read APIs, audit restricted to managers, generic errors | Business inventory is intentionally visible to all authenticated demo users |
| Denial of service | Login flood or huge horizon | Bounded rate limiter, input/body/series limits, HTTP timeouts, capped forecast horizon | Per-process limits are not coordinated across replicas |
| Elevation of privilege | Prompt says “become admin” | Role comes from verified token, never graph state supplied by client; every tool checks permission | Shared HMAC key is a V1 deployment compromise; migrate to asymmetric signing for production |

AI tool responses are data, never system instructions. The graph exposes exactly four agents and a fixed registry of read-only operations. Query text cannot register new tools or provide arbitrary URLs. Reorder arithmetic uses authoritative data and a deterministic function. Human approval persists a decision; it does not execute a supplier purchase. Tests should include denied tools, forged roles, malformed arguments, unknown SKUs and prompt-injection attempts.

Secrets belong in the process environment, never source control. Containers run as non-root where practical. Docker host ports bind loopback. This is a portfolio deployment, not an internet production security certification.
