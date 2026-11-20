---
name: diagnose-dlq
description: Diagnostic workflow for investigating failed ERP ingestions, DLQ messages, and SHADOW_DIFF reports by traceId or partnerId.
---

# DLQ & Integration Diagnostics Skill

Use this workflow when investigating an ERP integration failure or shadow migration discrepancy:

1. **Trace Lookup**: Query the MIS Audit API (`GET /api/v1/audit/trace/{traceId}`) or PostgreSQL `integration_audit_log` table to inspect `status`, `payload_hash`, and `failure_reason`.
2. **DLQ Inspection**: Check the `saas-sync-dlq` topic in Kafka UI (`http://localhost:8081`) filtering by `X-Trace-Id` or `X-Partner-Id` headers.
3. **Distributed Trace**: Open Jaeger UI (`http://localhost:16686`) and search by `traceId` to inspect span timings across REST -> Postgres -> Kafka.
4. **Shadow Diff**: If investigating a migration divergence, inspect `SHADOW_DIFF` entries in the audit log to compare legacy adapter vs. MIS canonical output fields (`finalPrice`, `sku`, `accountId`, `deliveryCenterId`).
