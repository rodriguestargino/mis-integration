package com.mis.integration.domain.service;

import com.mis.integration.domain.exception.BusinessValidationException;
import com.mis.integration.domain.model.RawErpPayload;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErpPayloadValidatorTest {

    private final ErpPayloadValidator validator = new ErpPayloadValidator();

    @Test
    void shouldAcceptValidPayload() {
        RawErpPayload payload = validPayload();
        assertThatCode(() -> validator.validate(payload)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNullPayload() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("PAYLOAD_NULL");
    }

    @Test
    void shouldRejectMissingSkuZeroPriceAndMissingSegmentation() {
        RawErpPayload invalid = new RawErpPayload(
                "partner-1", "batch-1", "  ", "Desc",
                BigDecimal.ZERO, null, null, null, -5,
                null, "", null
        );

        assertThatThrownBy(() -> validator.validate(invalid))
                .isInstanceOf(BusinessValidationException.class)
                .satisfies(ex -> {
                    BusinessValidationException bve = (BusinessValidationException) ex;
                    assertThat(bve.getViolations())
                            .anyMatch(v -> v.contains("SKU_MISSING"))
                            .anyMatch(v -> v.contains("INVALID_PRICE"))
                            .anyMatch(v -> v.contains("MISSING_SEGMENTATION_SCOPE"))
                            .anyMatch(v -> v.contains("NEGATIVE_STOCK"));
                });
    }

    @Test
    void shouldRejectNegativeTaxRatesAndAmounts() {
        RawErpPayload invalid = new RawErpPayload(
                "partner-1", "batch-1", "SKU-1", "Desc",
                new BigDecimal("10.00"),
                new BigDecimal("-1.0"),
                new BigDecimal("-2.0"),
                new BigDecimal("-0.50"),
                10,
                "acc-1", "dc-1", null
        );

        assertThatThrownBy(() -> validator.validate(invalid))
                .isInstanceOf(BusinessValidationException.class)
                .satisfies(ex -> {
                    BusinessValidationException bve = (BusinessValidationException) ex;
                    assertThat(bve.getViolations()).hasSize(3);
                });
    }

    @Test
    void shouldAcceptZeroStockAndZeroTaxRatesBoundary() {
        RawErpPayload boundary = new RawErpPayload(
                "partner-1", "batch-1", "SKU-1", "Desc",
                new BigDecimal("0.01"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                "acc-1", "dc-1", null
        );
        assertThatCode(() -> validator.validate(boundary)).doesNotThrowAnyException();
    }

    private RawErpPayload validPayload() {
        return new RawErpPayload(
                "partner-1", "batch-1", "SKU-100", "Produto Teste",
                new BigDecimal("100.00"), new BigDecimal("18.0"),
                new BigDecimal("5.0"), new BigDecimal("2.50"),
                15, "acc-1", "dc-1", "2026-11-20T10:00:00Z"
        );
    }
}
