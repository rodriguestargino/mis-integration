package com.mis.integration.domain.service;

import com.mis.integration.domain.model.CanonicalSyncEvent;
import com.mis.integration.domain.model.CanonicalSyncEvent.PricingDetail;
import com.mis.integration.domain.model.CanonicalSyncEvent.SegmentationScope;
import com.mis.integration.domain.model.LegacyAdapterOutput;
import com.mis.integration.domain.model.ShadowDiffReport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ShadowMigrationValidatorTest {

    private final ShadowMigrationValidator validator = new ShadowMigrationValidator();

    @Test
    void shouldMatchWhenAllCriticalFieldsAreEqualIncludingNumericScale() {
        LegacyAdapterOutput legacy = new LegacyAdapterOutput(
                "SKU-100", new BigDecimal("127.7"), 15, "acc-1", "dc-1"
        );
        CanonicalSyncEvent canonical = canonicalEvent("SKU-100", "127.70", 15, "acc-1", "dc-1");

        ShadowDiffReport report = validator.compare(legacy, canonical);

        assertThat(report.matched()).isTrue();
        assertThat(report.diffs()).isEmpty();
    }

    @Test
    void shouldReportAllDivergentCriticalFields() {
        LegacyAdapterOutput legacy = new LegacyAdapterOutput(
                "sku-100", new BigDecimal("120.00"), 10, "acc-old", "dc-2"
        );
        CanonicalSyncEvent canonical = canonicalEvent("SKU-100", "127.70", 15, "acc-1", "dc-1");

        ShadowDiffReport report = validator.compare(legacy, canonical);

        assertThat(report.matched()).isFalse();
        assertThat(report.diffs())
                .extracting(ShadowDiffReport.FieldDiff::field)
                .containsExactlyInAnyOrder("sku", "finalPrice", "availableStock", "accountId", "deliveryCenterId");
    }

    @Test
    void shouldReportNullLegacyPriceAsDiff() {
        LegacyAdapterOutput legacy = new LegacyAdapterOutput(
                "SKU-100", null, 15, "acc-1", "dc-1"
        );
        CanonicalSyncEvent canonical = canonicalEvent("SKU-100", "127.70", 15, "acc-1", "dc-1");

        ShadowDiffReport report = validator.compare(legacy, canonical);

        assertThat(report.matched()).isFalse();
        assertThat(report.diffs()).hasSize(1);
        assertThat(report.diffs().get(0).legacyValue()).isEqualTo("null");
        assertThat(report.diffs().get(0).misValue()).isEqualTo("127.70");
    }

    private CanonicalSyncEvent canonicalEvent(
            String sku, String finalPrice, int stock, String accountId, String dcId
    ) {
        return new CanonicalSyncEvent(
                "evt-1",
                "trace-1",
                "partner-1",
                sku,
                "Produto Teste",
                new PricingDetail(
                        new BigDecimal("100.00"),
                        new BigDecimal("18.00"),
                        new BigDecimal("5.00"),
                        new BigDecimal("4.70"),
                        new BigDecimal("27.70"),
                        new BigDecimal(finalPrice)
                ),
                stock,
                new SegmentationScope(accountId, dcId),
                Instant.parse("2026-11-20T10:00:00Z"),
                Instant.parse("2026-11-20T10:00:01Z")
        );
    }
}
