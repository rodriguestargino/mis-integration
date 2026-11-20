package com.mis.integration.application.port;

import com.mis.integration.domain.model.AuditStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for idempotency checks and integration payload auditing.
 */
public interface AuditPort {

    Optional<AuditRecordSummary> findByPartnerAndHash(String partnerId, String payloadHash);

    List<AuditDetail> findByTraceId(String traceId);

    UUID saveAudit(AuditEntry entry);

    void markDlqFallback(UUID auditId, String failureReason);

    record AuditRecordSummary(
            UUID id,
            String traceId,
            AuditStatus status
    ) {
    }

    record AuditDetail(
            UUID id,
            String traceId,
            String partnerId,
            String sku,
            String payloadHash,
            AuditStatus status,
            String failureReason,
            String rawPayload,
            String canonicalPayload,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    record AuditEntry(
            String traceId,
            String partnerId,
            String sku,
            String payloadHash,
            AuditStatus status,
            String failureReason,
            String rawPayload,
            String canonicalPayload
    ) {
    }
}
