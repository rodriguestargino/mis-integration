package com.mis.integration.domain.service;

import com.mis.integration.domain.model.CanonicalSyncEvent;
import com.mis.integration.domain.model.RawErpPayload;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ErpToCanonicalMapperTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-20T12:00:00Z");
    private final ErpToCanonicalMapper mapper = new ErpToCanonicalMapper(
            new ErpPayloadValidator(),
            new TaxCalculator(),
            Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
    );

    @Test
    void shouldNormalizeAndTransformValidPayload() {
        RawErpPayload payload = new RawErpPayload(
                " dist-alpha ",
                "batch-99",
                " sku-abc-123 ",
                "  Cerveja Pilsen 600ml  ",
                new BigDecimal("10.00"),
                new BigDecimal("12.0"),
                null,
                new BigDecimal("1.50"),
                42,
                " acc-01 ",
                " dc-sp-02 ",
                "2026-11-20T11:30:00Z"
        );

        CanonicalSyncEvent event = mapper.transform(payload, "trace-xyz");

        assertThat(event.eventId()).isNotBlank();
        assertThat(event.traceId()).isEqualTo("trace-xyz");
        assertThat(event.partnerId()).isEqualTo("dist-alpha");
        assertThat(event.sku()).isEqualTo("SKU-ABC-123");
        assertThat(event.productName()).isEqualTo("Cerveja Pilsen 600ml");
        assertThat(event.pricing().finalPrice()).isEqualByComparingTo("12.70");
        assertThat(event.availableStock()).isEqualTo(42);
        assertThat(event.segmentation().accountId()).isEqualTo("acc-01");
        assertThat(event.segmentation().deliveryCenterId()).isEqualTo("dc-sp-02");
        assertThat(event.sourceTimestamp()).isEqualTo(Instant.parse("2026-11-20T11:30:00Z"));
        assertThat(event.syncedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void shouldFallbackDefaultsWhenOptionalFieldsBlankOrInvalid() {
        RawErpPayload payload = new RawErpPayload(
                "dist-beta",
                null,
                "sku-1",
                " ",
                new BigDecimal("20.00"),
                null,
                null,
                null,
                null,
                "acc-2",
                "dc-2",
                "not-a-timestamp"
        );

        CanonicalSyncEvent event = mapper.transform(payload, "trace-fallback");

        assertThat(event.productName()).isEqualTo("UNKNOWN_PRODUCT");
        assertThat(event.availableStock()).isZero();
        assertThat(event.sourceTimestamp()).isEqualTo(FIXED_NOW);
    }
}
