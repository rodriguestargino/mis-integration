package com.mis.integration.domain.model;

import java.math.BigDecimal;

/**
 * Output produced by the legacy ERP adapter during a parallel-run
 * shadow migration phase prior to cutover.
 */
public record LegacyAdapterOutput(
        String sku,
        BigDecimal finalPrice,
        int availableStock,
        String accountId,
        String deliveryCenterId
) {
}
