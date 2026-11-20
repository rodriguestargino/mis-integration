package com.mis.integration.domain.service;

import com.mis.integration.domain.model.CanonicalSyncEvent.PricingDetail;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TaxCalculatorTest {

    private final TaxCalculator calculator = new TaxCalculator();

    @Test
    void shouldCalculateTaxesAndFinalPriceWithHalfUpRounding() {
        PricingDetail detail = calculator.calculate(
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                new BigDecimal("5.50"),
                new BigDecimal("4.25")
        );

        assertThat(detail.basePrice()).isEqualByComparingTo("100.00");
        assertThat(detail.icmsAmount()).isEqualByComparingTo("18.00");
        assertThat(detail.ipiAmount()).isEqualByComparingTo("5.50");
        assertThat(detail.stAmount()).isEqualByComparingTo("4.25");
        assertThat(detail.totalTaxAmount()).isEqualByComparingTo("27.75");
        assertThat(detail.finalPrice()).isEqualByComparingTo("127.75");
    }

    @Test
    void shouldHandleNullTaxesAsZero() {
        PricingDetail detail = calculator.calculate(
                new BigDecimal("50.126"),
                null,
                BigDecimal.ZERO,
                null
        );

        assertThat(detail.basePrice()).isEqualByComparingTo("50.13");
        assertThat(detail.totalTaxAmount()).isEqualByComparingTo("0.00");
        assertThat(detail.finalPrice()).isEqualByComparingTo("50.13");
    }
}
