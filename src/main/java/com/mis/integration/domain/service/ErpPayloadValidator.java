package com.mis.integration.domain.service;

import com.mis.integration.domain.exception.BusinessValidationException;
import com.mis.integration.domain.model.RawErpPayload;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates mandatory business invariants on raw ERP payloads.
 */
public class ErpPayloadValidator {

    public void validate(RawErpPayload payload) {
        if (payload == null) {
            throw new BusinessValidationException(List.of("PAYLOAD_NULL: Payload must not be null"));
        }

        List<String> violations = new ArrayList<>();

        if (isBlank(payload.partnerId())) {
            violations.add("PARTNER_ID_MISSING: partnerId is required");
        }
        if (isBlank(payload.sku())) {
            violations.add("SKU_MISSING: sku must not be null or blank");
        }
        if (payload.unitPrice() == null || payload.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            violations.add("INVALID_PRICE: unitPrice must be greater than zero");
        }
        if (isBlank(payload.accountId()) || isBlank(payload.deliveryCenterId())) {
            violations.add("MISSING_SEGMENTATION_SCOPE: accountId and deliveryCenterId are required");
        }
        if (payload.stockQuantity() != null && payload.stockQuantity() < 0) {
            violations.add("NEGATIVE_STOCK: stockQuantity cannot be negative");
        }
        validateNonNegative(payload.icmsRatePercent(), "NEGATIVE_TAX_RATE: icmsRatePercent cannot be negative", violations);
        validateNonNegative(payload.ipiRatePercent(), "NEGATIVE_TAX_RATE: ipiRatePercent cannot be negative", violations);
        validateNonNegative(payload.stFixedAmount(), "NEGATIVE_TAX_AMOUNT: stFixedAmount cannot be negative", violations);

        if (!violations.isEmpty()) {
            throw new BusinessValidationException(violations);
        }
    }

    private void validateNonNegative(BigDecimal value, String message, List<String> violations) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            violations.add(message);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
