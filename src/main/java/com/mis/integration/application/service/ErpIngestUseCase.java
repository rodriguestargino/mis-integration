package com.mis.integration.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mis.integration.application.port.AuditPort;
import com.mis.integration.application.port.AuditPort.AuditEntry;
import com.mis.integration.application.port.AuditPort.AuditRecordSummary;
import com.mis.integration.application.port.EventPublisherPort;
import com.mis.integration.domain.exception.BusinessValidationException;
import com.mis.integration.domain.model.AuditStatus;
import com.mis.integration.domain.model.CanonicalSyncEvent;
import com.mis.integration.domain.model.RawErpPayload;
import com.mis.integration.domain.service.ErpToCanonicalMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the MIS ETL pipeline:
 * Idempotency Check -> Domain Validation &amp; Tax Calculation -> Audit -> Kafka Delivery / DLQ.
 */
public class ErpIngestUseCase {

    private static final Logger log = LoggerFactory.getLogger(ErpIngestUseCase.class);

    private final PayloadHashService hashService;
    private final ErpToCanonicalMapper mapper;
    private final AuditPort auditPort;
    private final EventPublisherPort publisherPort;
    private final ObjectMapper objectMapper;

    public ErpIngestUseCase(
            PayloadHashService hashService,
            ErpToCanonicalMapper mapper,
            AuditPort auditPort,
            EventPublisherPort publisherPort,
            ObjectMapper objectMapper
    ) {
        this.hashService = hashService;
        this.mapper = mapper;
        this.auditPort = auditPort;
        this.publisherPort = publisherPort;
        this.objectMapper = objectMapper;
    }

    public IngestResult ingest(RawErpPayload payload, String traceId) {
        String payloadHash = hashService.computeHash(payload);
        String rawJson = toJson(payload);

        Optional<AuditRecordSummary> existing = auditPort.findByPartnerAndHash(payload.partnerId(), payloadHash);
        if (existing.isPresent()) {
            AuditRecordSummary record = existing.get();
            log.info("Duplicate payload detected for partner={} hash={} originalTraceId={}",
                    payload.partnerId(), payloadHash, record.traceId());
            return new IngestResult(
                    traceId,
                    payload.partnerId(),
                    payload.sku(),
                    payloadHash,
                    IngestOutcome.DUPLICATE,
                    "Payload already processed under traceId=" + record.traceId()
            );
        }

        try {
            CanonicalSyncEvent canonicalEvent = mapper.transform(payload, traceId);
            String canonicalJson = toJson(canonicalEvent);

            UUID auditId = auditPort.saveAudit(new AuditEntry(
                    traceId,
                    canonicalEvent.partnerId(),
                    canonicalEvent.sku(),
                    payloadHash,
                    AuditStatus.PUBLISHED,
                    null,
                    rawJson,
                    canonicalJson
            ));

            boolean delivered = publisherPort.publishEntity(canonicalEvent, auditId, rawJson);
            if (!delivered) {
                return new IngestResult(
                        traceId,
                        canonicalEvent.partnerId(),
                        canonicalEvent.sku(),
                        payloadHash,
                        IngestOutcome.DLQ_REJECTED,
                        "Broker delivery failed after retries; routed to DLQ"
                );
            }

            return new IngestResult(
                    traceId,
                    canonicalEvent.partnerId(),
                    canonicalEvent.sku(),
                    payloadHash,
                    IngestOutcome.PUBLISHED,
                    "Canonical event published to saas-sync-entities"
            );
        } catch (BusinessValidationException ex) {
            String reason = ex.getMessage();
            log.warn("Business validation failed for partner={} sku={} reason={}",
                    payload.partnerId(), payload.sku(), reason);

            auditPort.saveAudit(new AuditEntry(
                    traceId,
                    payload.partnerId(),
                    payload.sku(),
                    payloadHash,
                    AuditStatus.DLQ_REJECTED,
                    reason,
                    rawJson,
                    null
            ));

            publisherPort.publishToDlq(traceId, payload.partnerId(), payload.sku(), rawJson, reason);

            return new IngestResult(
                    traceId,
                    payload.partnerId(),
                    payload.sku(),
                    payloadHash,
                    IngestOutcome.DLQ_REJECTED,
                    reason
            );
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize payload for audit", ex);
        }
    }

    public enum IngestOutcome {
        PUBLISHED,
        DUPLICATE,
        DLQ_REJECTED
    }

    public record IngestResult(
            String traceId,
            String partnerId,
            String sku,
            String payloadHash,
            IngestOutcome outcome,
            String message
    ) {
    }
}
