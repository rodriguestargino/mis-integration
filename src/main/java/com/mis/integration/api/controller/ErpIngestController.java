package com.mis.integration.api.controller;

import com.mis.integration.api.dto.IngestResponse;
import com.mis.integration.api.dto.LegacyErpIngestRequest;
import com.mis.integration.application.service.ErpIngestUseCase;
import com.mis.integration.application.service.ErpIngestUseCase.IngestResult;
import com.mis.integration.domain.model.RawErpPayload;
import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/erp-ingest")
public class ErpIngestController {

    private final ErpIngestUseCase ingestUseCase;
    private final Tracer tracer;

    public ErpIngestController(ErpIngestUseCase ingestUseCase, Tracer tracer) {
        this.ingestUseCase = ingestUseCase;
        this.tracer = tracer;
    }

    @PostMapping("/{partnerId}")
    public ResponseEntity<IngestResponse> ingest(
            @PathVariable String partnerId,
            @RequestBody LegacyErpIngestRequest request
    ) {
        String traceId = resolveTraceId();
        MDC.put("partnerId", partnerId);
        try {
            RawErpPayload rawPayload = new RawErpPayload(
                    partnerId,
                    request.batchId(),
                    request.sku(),
                    request.productDescription(),
                    request.unitPrice(),
                    request.icmsRatePercent(),
                    request.ipiRatePercent(),
                    request.stFixedAmount(),
                    request.stockQuantity(),
                    request.accountId(),
                    request.deliveryCenterId(),
                    request.erpUpdatedAt()
            );

            IngestResult result = ingestUseCase.ingest(rawPayload, traceId);
            IngestResponse response = new IngestResponse(
                    result.traceId(),
                    result.partnerId(),
                    result.sku(),
                    result.payloadHash(),
                    result.outcome(),
                    result.message()
            );

            HttpStatus status = switch (result.outcome()) {
                case PUBLISHED -> HttpStatus.ACCEPTED;
                case DUPLICATE -> HttpStatus.OK;
                case DLQ_REJECTED -> HttpStatus.UNPROCESSABLE_ENTITY;
            };

            return ResponseEntity.status(status).body(response);
        } finally {
            MDC.remove("partnerId");
        }
    }

    private String resolveTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
