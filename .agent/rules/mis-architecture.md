---
trigger: always_on
description: MIS ERP Integration Hub architectural conventions, error handling, idempotency, and testing rules
---

# MIS ERP Integration Hub — Workspace Rules

1. **Hexagonal Boundaries**: Keep `com.mis.integration.domain` 100% pure Java 21 (no Spring, JPA, or Kafka imports).
2. **Financial Precision**: Always use `BigDecimal` with explicit `RoundingMode.HALF_UP` for pricing and tax calculations.
3. **Idempotency & Audit**: Every `/api/v1/erp-ingest/{partnerId}` request must be hashed (SHA-256) and audited in PostgreSQL before Kafka publication.
4. **DLQ & Retries**: Business validation failures (`SKU` null/blank, `price <= 0`, missing `accountId`/`deliveryCenterId`) route directly to `saas-sync-dlq` and record the exact reason in the audit table. Transient errors use exponential backoff.
5. **Observability**: Propagate OpenTelemetry `traceId` in Kafka headers and structured JSON logs.
6. **Testing**: Use Testcontainers (PostgreSQL + Kafka) for integration tests and maintain >85% PITest mutation coverage on `domain` and `application`.
