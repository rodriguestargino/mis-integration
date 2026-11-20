package com.mis.integration.integration;

import com.mis.integration.api.dto.IngestResponse;
import com.mis.integration.api.dto.LegacyErpIngestRequest;
import com.mis.integration.api.dto.ShadowCompareRequest;
import com.mis.integration.application.port.AuditPort.AuditDetail;
import com.mis.integration.application.service.ErpIngestUseCase.IngestOutcome;
import com.mis.integration.domain.model.AuditStatus;
import com.mis.integration.domain.model.LegacyAdapterOutput;
import com.mis.integration.domain.model.ShadowDiffReport;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ErpIntegrationFlowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.3"));

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldPublishValidPayloadToKafkaVerifyIdempotencyAndQueryAuditByTraceId() {
        LegacyErpIngestRequest request = new LegacyErpIngestRequest(
                "batch-it-1",
                "sku-it-100",
                "Agua Mineral 500ml",
                new BigDecimal("100.00"),
                new BigDecimal("18.0"),
                new BigDecimal("5.0"),
                new BigDecimal("2.00"),
                25,
                "acc-it-1",
                "dc-it-1",
                "2026-11-20T10:00:00Z"
        );

        // 1. First ingestion -> 202 ACCEPTED + PUBLISHED
        ResponseEntity<IngestResponse> firstResponse = restTemplate.postForEntity(
                "/api/v1/erp-ingest/partner-it",
                request,
                IngestResponse.class
        );

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(firstResponse.getBody()).isNotNull();
        assertThat(firstResponse.getBody().status()).isEqualTo(IngestOutcome.PUBLISHED);
        String traceId = firstResponse.getBody().traceId();

        // 2. Verify Kafka message on saas-sync-entities with X-Trace-Id header
        ConsumerRecord<String, String> record = consumeSingleRecord("saas-sync-entities", traceId);
        assertThat(record).isNotNull();
        assertThat(headerValue(record, "X-Trace-Id")).isEqualTo(traceId);
        assertThat(headerValue(record, "X-Partner-Id")).isEqualTo("partner-it");
        assertThat(record.value()).contains("SKU-IT-100").contains("125.00");

        // 3. Idempotent replay -> 200 OK + DUPLICATE
        ResponseEntity<IngestResponse> replayResponse = restTemplate.postForEntity(
                "/api/v1/erp-ingest/partner-it",
                request,
                IngestResponse.class
        );
        assertThat(replayResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replayResponse.getBody()).isNotNull();
        assertThat(replayResponse.getBody().status()).isEqualTo(IngestOutcome.DUPLICATE);

        // 4. Query audit by traceId
        ResponseEntity<AuditDetail[]> auditResponse = restTemplate.getForEntity(
                "/api/v1/audit/trace/" + traceId,
                AuditDetail[].class
        );
        assertThat(auditResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(auditResponse.getBody()).hasSize(1);
        assertThat(auditResponse.getBody()[0].status()).isEqualTo(AuditStatus.PUBLISHED);
    }

    @Test
    void shouldRouteInvalidPayloadToDlqAndRecordAuditReason() {
        LegacyErpIngestRequest invalid = new LegacyErpIngestRequest(
                "batch-bad-1",
                null,
                "Invalid Item",
                BigDecimal.ZERO,
                null,
                null,
                null,
                5,
                null,
                "dc-it-1",
                null
        );

        ResponseEntity<IngestResponse> response = restTemplate.postForEntity(
                "/api/v1/erp-ingest/partner-dlq",
                invalid,
                IngestResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(IngestOutcome.DLQ_REJECTED);
        String traceId = response.getBody().traceId();

        ConsumerRecord<String, String> dlqRecord = consumeSingleRecord("saas-sync-dlq", traceId);
        assertThat(dlqRecord).isNotNull();
        assertThat(headerValue(dlqRecord, "X-Error-Reason")).contains("SKU_MISSING");

        ResponseEntity<AuditDetail[]> auditResponse = restTemplate.getForEntity(
                "/api/v1/audit/trace/" + traceId,
                AuditDetail[].class
        );
        assertThat(auditResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(auditResponse.getBody()).hasSize(1);
        assertThat(auditResponse.getBody()[0].status()).isEqualTo(AuditStatus.DLQ_REJECTED);
        assertThat(auditResponse.getBody()[0].failureReason()).contains("SKU_MISSING");
    }

    @Test
    void shouldPersistShadowDiffWhenLegacyAndMisOutputsDiverge() {
        LegacyErpIngestRequest erp = new LegacyErpIngestRequest(
                "batch-shadow-1",
                "sku-shd-1",
                "Refrigerante 2L",
                new BigDecimal("100.00"),
                new BigDecimal("18.0"),
                new BigDecimal("5.0"),
                new BigDecimal("2.00"),
                30,
                "acc-shd-1",
                "dc-shd-1",
                null
        );
        LegacyAdapterOutput legacy = new LegacyAdapterOutput(
                "SKU-SHD-1",
                new BigDecimal("118.00"),
                30,
                "acc-shd-1",
                "dc-shd-1"
        );

        ResponseEntity<ShadowDiffReport> response = restTemplate.postForEntity(
                "/api/v1/shadow/compare/partner-shadow",
                new ShadowCompareRequest(erp, legacy),
                ShadowDiffReport.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().matched()).isFalse();
        assertThat(response.getBody().diffs()).hasSize(1);
        assertThat(response.getBody().diffs().get(0).field()).isEqualTo("finalPrice");

        ResponseEntity<AuditDetail[]> auditResponse = restTemplate.getForEntity(
                "/api/v1/audit/trace/" + response.getBody().traceId(),
                AuditDetail[].class
        );
        assertThat(auditResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(auditResponse.getBody()).hasSize(1);
        assertThat(auditResponse.getBody()[0].status()).isEqualTo(AuditStatus.SHADOW_DIFF);
    }

    private ConsumerRecord<String, String> consumeSingleRecord(String topic, String expectedTraceId) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "it-consumer-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + 10_000;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    if (expectedTraceId.equals(headerValue(record, "X-Trace-Id"))) {
                        return record;
                    }
                }
            }
        }
        return null;
    }

    private String headerValue(ConsumerRecord<?, ?> record, String key) {
        var header = record.headers().lastHeader(key);
        return header != null ? new String(header.value(), StandardCharsets.UTF_8) : null;
    }
}
