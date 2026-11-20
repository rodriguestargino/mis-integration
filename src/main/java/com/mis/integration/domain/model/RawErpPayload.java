package com.mis.integration.domain.model;

import java.math.BigDecimal;

/**
 * Pure domain representation of the raw inbound ERP payload,
 * decoupled from HTTP/API transport DTOs.
 */
public record RawErpPayload(
        String partnerId,
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
