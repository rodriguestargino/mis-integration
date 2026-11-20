package com.mis.integration.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mis.integration.application.port.AuditPort;
import com.mis.integration.application.port.AuditPort.AuditRecordSummary;
import com.mis.integration.application.port.EventPublisherPort;
import com.mis.integration.application.service.ErpIngestUseCase.IngestOutcome;
import com.mis.integration.application.service.ErpIngestUseCase.IngestResult;
import com.mis.integration.domain.model.AuditStatus;
import com.mis.integration.domain.model.RawErpPayload;
import com.mis.integration.domain.service.ErpPayloadValidator;
import com.mis.integration.domain.service.ErpToCanonicalMapper;
import com.mis.integration.domain.service.TaxCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpIngestUseCaseTest {

    @Mock
    private AuditPort auditPort;

    @Mock
    private EventPublisherPort publisherPort;

    private ErpIngestUseCase useCase;

    @BeforeEach
    void setUp() {
        PayloadHashService hashService = new PayloadHashService();
        ErpToCanonicalMapper mapper = new ErpToCanonicalMapper(
                new ErpPayloadValidator(),
                new TaxCalculator(),
                Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC)
        );
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        useCase = new ErpIngestUseCase(hashService, mapper, auditPort, publisherPort, objectMapper);
    }

    @Test
    void shouldPublishValidPayloadAndSaveAudit() {
        RawErpPayload payload = validPayload();
        UUID auditId = UUID.randomUUID();
        when(auditPort.findByPartnerAndHash(eq("partner-1"), any())).thenReturn(Optional.empty());
        when(auditPort.saveAudit(any())).thenReturn(auditId);
        when(publisherPort.publishEntity(any(), eq(auditId), any())).thenReturn(true);

        IngestResult result = useCase.ingest(payload, "trace-001");

        assertThat(result.outcome()).isEqualTo(IngestOutcome.PUBLISHED);
        assertThat(result.sku()).isEqualTo("SKU-100");
        verify(auditPort).saveAudit(argThat(entry -> entry.status() == AuditStatus.PUBLISHED));
        verify(publisherPort).publishEntity(any(), eq(auditId), any());
        verify(publisherPort, never()).publishToDlq(any(), any(), any(), any(), any());
    }

    @Test
    void shouldShortCircuitDuplicatePayloadWithoutPublishing() {
        RawErpPayload payload = validPayload();
        when(auditPort.findByPartnerAndHash(eq("partner-1"), any()))
                .thenReturn(Optional.of(new AuditRecordSummary(UUID.randomUUID(), "trace-original", AuditStatus.PUBLISHED)));

        IngestResult result = useCase.ingest(payload, "trace-retry");

        assertThat(result.outcome()).isEqualTo(IngestOutcome.DUPLICATE);
        assertThat(result.message()).contains("trace-original");
        verify(auditPort, never()).saveAudit(any());
        verify(publisherPort, never()).publishEntity(any(), any(), any());
        verify(publisherPort, never()).publishToDlq(any(), any(), any(), any(), any());
    }

    @Test
    void shouldRouteInvalidPayloadToDlqAndRecordFailureReason() {
        RawErpPayload invalid = new RawErpPayload(
                "partner-1", "b1", null, "Desc",
                BigDecimal.ZERO, null, null, null, 5,
                null, "dc-1", null
        );
        when(auditPort.findByPartnerAndHash(eq("partner-1"), any())).thenReturn(Optional.empty());

        IngestResult result = useCase.ingest(invalid, "trace-bad");

        assertThat(result.outcome()).isEqualTo(IngestOutcome.DLQ_REJECTED);
        assertThat(result.message()).contains("SKU_MISSING");
        verify(auditPort).saveAudit(argThat(entry ->
                entry.status() == AuditStatus.DLQ_REJECTED && entry.failureReason().contains("SKU_MISSING")));
        verify(publisherPort).publishToDlq(eq("trace-bad"), eq("partner-1"), eq(null), any(), any());
        verify(publisherPort, never()).publishEntity(any(), any(), any());
    }

    @Test
    void shouldReturnDlqRejectedWhenBrokerDeliveryFails() {
        RawErpPayload payload = validPayload();
        UUID auditId = UUID.randomUUID();
        when(auditPort.findByPartnerAndHash(eq("partner-1"), any())).thenReturn(Optional.empty());
        when(auditPort.saveAudit(any())).thenReturn(auditId);
        when(publisherPort.publishEntity(any(), eq(auditId), any())).thenReturn(false);

        IngestResult result = useCase.ingest(payload, "trace-kafka-down");

        assertThat(result.outcome()).isEqualTo(IngestOutcome.DLQ_REJECTED);
        assertThat(result.message()).contains("DLQ");
    }

    private RawErpPayload validPayload() {
        return new RawErpPayload(
                "partner-1", "batch-1", "sku-100", "Produto A",
                new BigDecimal("100.00"), new BigDecimal("18.0"),
                new BigDecimal("5.0"), new BigDecimal("2.00"),
                10, "acc-1", "dc-1", "2026-11-20T10:00:00Z"
        );
    }
}
