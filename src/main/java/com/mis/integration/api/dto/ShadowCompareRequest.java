package com.mis.integration.api.dto;

import com.mis.integration.domain.model.LegacyAdapterOutput;
import jakarta.validation.constraints.NotNull;

public record ShadowCompareRequest(
        @NotNull LegacyErpIngestRequest erpPayload,
        @NotNull LegacyAdapterOutput legacyOutput
) {
}
