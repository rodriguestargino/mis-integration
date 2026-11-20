package com.mis.integration.application.port;

import com.mis.integration.domain.model.CanonicalSyncEvent;

import java.util.UUID;

/**
 * Outbound port for publishing canonical events and DLQ messages.
 */
public interface EventPublisherPort {

    /**
     * Publishes a valid canonical event to {@code saas-sync-entities}.
     * If transient delivery fails after configured retries, routes to {@code saas-sync-dlq}
     * and updates the audit log with the failure reason.
     */
    boolean publishEntity(CanonicalSyncEvent event, UUID auditId, String rawPayload);

    /**
     * Publishes a rejected/corrupted payload directly to {@code saas-sync-dlq}.
     */
    void publishToDlq(String traceId, String partnerId, String sku, String rawPayload, String reason);
}
