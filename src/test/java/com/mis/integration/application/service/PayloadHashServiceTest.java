package com.mis.integration.application.service;

import com.mis.integration.domain.model.RawErpPayload;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadHashServiceTest {

    private final PayloadHashService hashService = new PayloadHashService();

    @Test
    void shouldProduceDeterministic64CharSha256HexHash() {
        RawErpPayload payload1 = new RawErpPayload(
                "p1", "b1", "SKU-1", "Desc",
                new BigDecimal("10.00"), null, null, null, null,
                "acc-1", "dc-1", null
        );
        RawErpPayload payload2 = new RawErpPayload(
                "p1", "b1", "SKU-1", "Desc",
                new BigDecimal("10.00"), null, null, null, null,
                "acc-1", "dc-1", null
        );
        RawErpPayload different = new RawErpPayload(
                "p1", "b1", "SKU-2", "Desc",
                new BigDecimal("10.00"), null, null, null, null,
                "acc-1", "dc-1", null
        );

        String hash1 = hashService.computeHash(payload1);
        String hash2 = hashService.computeHash(payload2);
        String hashDifferent = hashService.computeHash(different);

        assertThat(hash1).hasSize(64).isEqualTo(hash2).isNotEqualTo(hashDifferent);
    }
}
