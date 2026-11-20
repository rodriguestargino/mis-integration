package com.mis.integration.infrastructure.messaging;

import com.mis.integration.application.port.AuditPort;
import com.mis.integration.application.port.EventPublisherPort;
import com.mis.integration.domain.model.CanonicalSyncEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaEventPublisherAdapter implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisherAdapter.class);

    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_PARTNER_ID = "X-Partner-Id";
    public static final String HEADER_EVENT_ID = "X-Event-Id";
    public static final String HEADER_ERROR_REASON = "X-Error-Reason";
    public static final String HEADER_ORIGINAL_TOPIC = "X-Original-Topic";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AuditPort auditPort;
    private final String entitiesTopic;
    private final String dlqTopic;
    private final RetryTemplate retryTemplate;

    public KafkaEventPublisherAdapter(
            KafkaTemplate<String, Object> kafkaTemplate,
            AuditPort auditPort,
            @Value("${mis.kafka.topics.entities:saas-sync-entities}") String entitiesTopic,
            @Value("${mis.kafka.topics.dlq:saas-sync-dlq}") String dlqTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.auditPort = auditPort;
        this.entitiesTopic = entitiesTopic;
        this.dlqTopic = dlqTopic;
        this.retryTemplate = RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(200, 2.0, 2000)
                .build();
    }

    @Override
    public boolean publishEntity(CanonicalSyncEvent event, UUID auditId, String rawPayload) {
        String key = event.partnerId() + ":" + event.sku();
        ProducerRecord<String, Object> record = new ProducerRecord<>(entitiesTopic, key, event);
        addHeader(record, HEADER_TRACE_ID, event.traceId());
        addHeader(record, HEADER_PARTNER_ID, event.partnerId());
        addHeader(record, HEADER_EVENT_ID, event.eventId());

        try {
            retryTemplate.execute(context -> {
                kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
                return null;
            });
            log.info("Published CanonicalSyncEvent to {} key={} traceId={}", entitiesTopic, key, event.traceId());
            return true;
        } catch (Exception ex) {
            String reason = "Kafka publish failed after retries: " + ex.getMessage();
            log.error("Failed publishing to {} traceId={} reason={}", entitiesTopic, event.traceId(), reason, ex);
            auditPort.markDlqFallback(auditId, reason);
            publishToDlq(event.traceId(), event.partnerId(), event.sku(), rawPayload, reason);
            return false;
        }
    }

    @Override
    public void publishToDlq(String traceId, String partnerId, String sku, String rawPayload, String reason) {
        String key = partnerId + ":" + (sku != null ? sku : "UNKNOWN_SKU");
        ProducerRecord<String, Object> record = new ProducerRecord<>(dlqTopic, key, rawPayload);
        addHeader(record, HEADER_TRACE_ID, traceId);
        addHeader(record, HEADER_PARTNER_ID, partnerId);
        addHeader(record, HEADER_ERROR_REASON, reason);
        addHeader(record, HEADER_ORIGINAL_TOPIC, entitiesTopic);

        try {
            kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
            log.warn("Routed payload to DLQ {} key={} traceId={} reason={}", dlqTopic, key, traceId, reason);
        } catch (Exception ex) {
            log.error("CRITICAL: Failed to publish to DLQ {} traceId={}", dlqTopic, traceId, ex);
        }
    }

    private void addHeader(ProducerRecord<String, Object> record, String key, String value) {
        if (value != null) {
            record.headers().add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8)));
        }
    }
}
