package com.mis.integration.api.dto;

import com.mis.integration.application.service.ErpIngestUseCase.IngestOutcome;

public record IngestResponse(
        String traceId,
        String partnerId,
        String sku,
        String payloadHash,
        IngestOutcome status,
        String message
) {
}
