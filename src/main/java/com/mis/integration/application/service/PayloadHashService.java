package com.mis.integration.application.service;

import com.mis.integration.domain.model.RawErpPayload;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Computes a deterministic SHA-256 hash for an inbound ERP payload
 * to enforce idempotency across partner retries.
 */
public class PayloadHashService {

    public String computeHash(RawErpPayload payload) {
        String canonicalRepresentation = String.join("|",
                nullSafe(payload.partnerId()),
                nullSafe(payload.batchId()),
                nullSafe(payload.sku()),
                nullSafe(payload.productDescription()),
                nullSafe(payload.unitPrice()),
                nullSafe(payload.icmsRatePercent()),
                nullSafe(payload.ipiRatePercent()),
                nullSafe(payload.stFixedAmount()),
                nullSafe(payload.stockQuantity()),
                nullSafe(payload.accountId()),
                nullSafe(payload.deliveryCenterId()),
                nullSafe(payload.erpUpdatedAt())
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(canonicalRepresentation.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encoded);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    private String nullSafe(Object value) {
        return Objects.toString(value, "");
    }
}
