package com.mis.integration.api.controller;

import com.mis.integration.api.dto.LegacyErpIngestRequest;
import com.mis.integration.api.dto.ShadowCompareRequest;
import com.mis.integration.application.service.ShadowComparisonUseCase;
import com.mis.integration.domain.model.RawErpPayload;
import com.mis.integration.domain.model.ShadowDiffReport;
import io.micrometer.tracing.Tracer;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shadow")
public class ShadowMigrationController {

    private final ShadowComparisonUseCase shadowComparisonUseCase;
    private final Tracer tracer;

    public ShadowMigrationController(ShadowComparisonUseCase shadowComparisonUseCase, Tracer tracer) {
        this.shadowComparisonUseCase = shadowComparisonUseCase;
        this.tracer = tracer;
    }

    @PostMapping("/compare/{partnerId}")
    public ResponseEntity<ShadowDiffReport> compare(
            @PathVariable String partnerId,
            @Valid @RequestBody ShadowCompareRequest request
    ) {
        String traceId = resolveTraceId();
        MDC.put("partnerId", partnerId);
        try {
            LegacyErpIngestRequest erp = request.erpPayload();
            RawErpPayload rawPayload = new RawErpPayload(
                    partnerId,
                    erp.batchId(),
                    erp.sku(),
                    erp.productDescription(),
                    erp.unitPrice(),
                    erp.icmsRatePercent(),
                    erp.ipiRatePercent(),
                    erp.stFixedAmount(),
                    erp.stockQuantity(),
                    erp.accountId(),
                    erp.deliveryCenterId(),
                    erp.erpUpdatedAt()
            );

            ShadowDiffReport report = shadowComparisonUseCase.compare(rawPayload, request.legacyOutput(), traceId);
            return ResponseEntity.ok(report);
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
