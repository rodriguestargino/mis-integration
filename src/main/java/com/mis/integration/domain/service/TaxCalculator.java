package com.mis.integration.domain.service;

import com.mis.integration.domain.model.CanonicalSyncEvent.PricingDetail;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure domain tax calculator for distributor pricing using BigDecimal
 * with {@link RoundingMode#HALF_UP} and 2 decimal places.
 */
public class TaxCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public PricingDetail calculate(
            BigDecimal unitPrice,
            BigDecimal icmsRatePercent,
            BigDecimal ipiRatePercent,
            BigDecimal stFixedAmount
    ) {
        BigDecimal basePrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        BigDecimal icmsAmount = calculatePercent(basePrice, icmsRatePercent);
        BigDecimal ipiAmount = calculatePercent(basePrice, ipiRatePercent);
        BigDecimal stAmount = stFixedAmount != null
                ? stFixedAmount.setScale(2, RoundingMode.HALF_UP)
                : ZERO_MONEY;

        BigDecimal totalTaxAmount = icmsAmount
                .add(ipiAmount)
                .add(stAmount)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal finalPrice = basePrice
                .add(totalTaxAmount)
                .setScale(2, RoundingMode.HALF_UP);

        return new PricingDetail(basePrice, icmsAmount, ipiAmount, stAmount, totalTaxAmount, finalPrice);
    }

    private BigDecimal calculatePercent(BigDecimal basePrice, BigDecimal ratePercent) {
        if (ratePercent == null || ratePercent.compareTo(BigDecimal.ZERO) == 0) {
            return ZERO_MONEY;
        }
        return basePrice
                .multiply(ratePercent)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
