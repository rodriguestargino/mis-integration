package com.mis.integration.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mis.integration.application.port.AuditPort;
import com.mis.integration.domain.model.AuditStatus;
import com.mis.integration.domain.model.LegacyAdapterOutput;
import com.mis.integration.domain.model.RawErpPayload;
import com.mis.integration.domain.model.ShadowDiffReport;
import com.mis.integration.domain.service.ErpPayloadValidator;
import com.mis.integration.domain.service.ErpToCanonicalMapper;
import com.mis.integration.domain.service.ShadowMigrationValidator;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShadowComparisonUseCaseTest {

    @Mock
    private AuditPort auditPort;

    private ShadowComparisonUseCase useCase;

    @BeforeEach
    void setUp() {
        ErpToCanonicalMapper mapper = new ErpToCanonicalMapper(
                new ErpPayloadValidator(),
                new TaxCalculator(),
                Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC)
        );
        useCase = new ShadowComparisonUseCase(
                mapper,
                new ShadowMigrationValidator(),
                auditPort,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void shouldNotSaveShadowDiffWhenOutputsMatch() {
        RawErpPayload raw = validPayload();
        LegacyAdapterOutput legacy = new LegacyAdapterOutput(
                "SKU-100", new BigDecimal("125.00"), 10, "acc-1", "dc-1"
        );

        ShadowDiffReport report = useCase.compare(raw, legacy, "trace-match");

        assertThat(report.matched()).isTrue();
        verify(auditPort, never()).saveAudit(any());
    }

    @Test
    void shouldSaveShadowDiffAuditWhenOutputsDiverge() {
        RawErpPayload raw = validPayload();
        LegacyAdapterOutput legacy = new LegacyAdapterOutput(
                "SKU-100", new BigDecimal("119.00"), 10, "acc-1", "dc-2"
        );

        ShadowDiffReport report = useCase.compare(raw, legacy, "trace-diff");

        assertThat(report.matched()).isFalse();
        verify(auditPort).saveAudit(argThat(entry ->
                entry.status() == AuditStatus.SHADOW_DIFF
                        && entry.failureReason().contains("finalPrice")
                        && entry.failureReason().contains("deliveryCenterId")
                        && entry.canonicalPayload().contains("SHADOW_DIFF") == false
        ));
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
