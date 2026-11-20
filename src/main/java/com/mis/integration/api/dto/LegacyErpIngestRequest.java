package com.mis.integration.api.dto;

import java.math.BigDecimal;

/**
 * Inbound DTO simulating raw, potentially dirty or inconsistent payloads
 * sent by external distributor legacy ERPs.
 *
 * <p>Validation of business invariants (e.g. non-blank SKU, price &gt; 0,
 * mandatory segmentation scope) is intentionally handled in the domain
 * pipeline so invalid records can be audited and routed to the DLQ.</p>
 */
public record LegacyErpIngestRequest(
        String batchId,
        String sku,
        String productDescription,
        BigDecimal unitPrice,
        BigDecimal icmsRatePercent,
        BigDecimal ipiRatePercent,
        BigDecimal stFixedAmount,
        Integer stockQuantity,
        String accountId,
        String deliveryCenterId,
        String erpUpdatedAt
) {
}
