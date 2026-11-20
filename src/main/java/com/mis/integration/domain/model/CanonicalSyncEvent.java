package com.mis.integration.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Standardized canonical contract published to {@code saas-sync-entities}
 * for consumption by the central B2B SaaS platform.
 */
public record CanonicalSyncEvent(
        String eventId,
        String traceId,
        String partnerId,
        String sku,
        String productName,
        PricingDetail pricing,
        int availableStock,
        SegmentationScope segmentation,
        Instant sourceTimestamp,
        Instant syncedAt
) {
    public CanonicalSyncEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(partnerId, "partnerId must not be null");
        Objects.requireNonNull(sku, "sku must not be null");
        Objects.requireNonNull(pricing, "pricing must not be null");
        Objects.requireNonNull(segmentation, "segmentation must not be null");
    }

    public record PricingDetail(
            BigDecimal basePrice,
            BigDecimal icmsAmount,
            BigDecimal ipiAmount,
            BigDecimal stAmount,
            BigDecimal totalTaxAmount,
            BigDecimal finalPrice
    ) {
    }

    public record SegmentationScope(
            String accountId,
            String deliveryCenterId
    ) {
    }
}
