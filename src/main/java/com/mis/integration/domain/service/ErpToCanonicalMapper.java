package com.mis.integration.domain.service;

import com.mis.integration.domain.model.CanonicalSyncEvent;
import com.mis.integration.domain.model.CanonicalSyncEvent.PricingDetail;
import com.mis.integration.domain.model.CanonicalSyncEvent.SegmentationScope;
import com.mis.integration.domain.model.RawErpPayload;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;

/**
 * Pure domain transformer that validates raw distributor ERP payloads,
 * computes tax breakdown, and produces a standardized {@link CanonicalSyncEvent}.
 */
public class ErpToCanonicalMapper {

    private final ErpPayloadValidator validator;
    private final TaxCalculator taxCalculator;
    private final Clock clock;

    public ErpToCanonicalMapper(
            ErpPayloadValidator validator,
            TaxCalculator taxCalculator,
            Clock clock
    ) {
        this.validator = validator;
        this.taxCalculator = taxCalculator;
        this.clock = clock;
    }

    public CanonicalSyncEvent transform(RawErpPayload payload, String traceId) {
        validator.validate(payload);

        PricingDetail pricing = taxCalculator.calculate(
                payload.unitPrice(),
                payload.icmsRatePercent(),
                payload.ipiRatePercent(),
                payload.stFixedAmount()
        );

        String normalizedSku = payload.sku().trim().toUpperCase(Locale.ROOT);
        String productName = (payload.productDescription() == null || payload.productDescription().isBlank())
                ? "UNKNOWN_PRODUCT"
                : payload.productDescription().trim();

        int availableStock = payload.stockQuantity() != null ? payload.stockQuantity() : 0;

        SegmentationScope segmentation = new SegmentationScope(
                payload.accountId().trim(),
                payload.deliveryCenterId().trim()
        );

        Instant now = Instant.now(clock);
        Instant sourceTimestamp = parseTimestamp(payload.erpUpdatedAt(), now);

        return new CanonicalSyncEvent(
                UUID.randomUUID().toString(),
                traceId,
                payload.partnerId().trim(),
                normalizedSku,
                productName,
                pricing,
                availableStock,
                segmentation,
                sourceTimestamp,
                now
        );
    }

    private Instant parseTimestamp(String erpUpdatedAt, Instant fallback) {
        if (erpUpdatedAt == null || erpUpdatedAt.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(erpUpdatedAt.trim());
        } catch (DateTimeParseException ex) {
            return fallback;
        }
    }
}
