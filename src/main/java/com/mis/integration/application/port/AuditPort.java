package com.mis.integration.application.port;

import com.mis.integration.domain.model.AuditStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for idempotency checks and integration payload auditing.
 */
public interface AuditPort {

    Optional<AuditRecordSummary> findByPartnerAndHash(String partnerId, String payloadHash);

    UUID saveAudit(AuditEntry entry);

    void markDlqFallback(UUID auditId, String failureReason);

    record AuditRecordSummary(
            UUID id,
            String traceId,
            AuditStatus status
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
