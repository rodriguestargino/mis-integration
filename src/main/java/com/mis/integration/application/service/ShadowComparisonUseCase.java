package com.mis.integration.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mis.integration.application.port.AuditPort;
import com.mis.integration.application.port.AuditPort.AuditEntry;
import com.mis.integration.domain.model.AuditStatus;
import com.mis.integration.domain.model.CanonicalSyncEvent;
import com.mis.integration.domain.model.LegacyAdapterOutput;
import com.mis.integration.domain.model.RawErpPayload;
import com.mis.integration.domain.model.ShadowDiffReport;
import com.mis.integration.domain.service.ErpToCanonicalMapper;
import com.mis.integration.domain.service.ShadowMigrationValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * Executes parallel-run shadow validation comparing legacy adapter output
 * against the new MIS canonical output and records any {@code SHADOW_DIFF}.
 */
public class ShadowComparisonUseCase {

    private static final Logger log = LoggerFactory.getLogger(ShadowComparisonUseCase.class);

    private final ErpToCanonicalMapper mapper;
    private final ShadowMigrationValidator validator;
    private final AuditPort auditPort;
    private final ObjectMapper objectMapper;

    public ShadowComparisonUseCase(
            ErpToCanonicalMapper mapper,
            ShadowMigrationValidator validator,
            AuditPort auditPort,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.validator = validator;
        this.auditPort = auditPort;
        this.objectMapper = objectMapper;
    }

    public ShadowDiffReport compare(RawErpPayload rawPayload, LegacyAdapterOutput legacyOutput, String traceId) {
        CanonicalSyncEvent canonicalEvent = mapper.transform(rawPayload, traceId);
        ShadowDiffReport report = validator.compare(legacyOutput, canonicalEvent);

        if (!report.matched()) {
            String diffSummary = report.diffs().stream()
                    .map(d -> d.field() + "[legacy=" + d.legacyValue() + ", mis=" + d.misValue() + "]")
                    .collect(Collectors.joining("; "));

            log.warn("SHADOW_DIFF detected for partner={} sku={} traceId={} diffs={}",
                    canonicalEvent.partnerId(), canonicalEvent.sku(), traceId, diffSummary);

            String shadowHash = ("shadow-" + traceId).substring(0, Math.min(64, ("shadow-" + traceId).length()));
            auditPort.saveAudit(new AuditEntry(
                    traceId,
                    canonicalEvent.partnerId(),
                    canonicalEvent.sku(),
                    shadowHash,
                    AuditStatus.SHADOW_DIFF,
                    diffSummary,
                    toJson(rawPayload),
                    toJson(report)
            ));
        } else {
            log.info("Shadow comparison matched 100% for partner={} sku={} traceId={}",
                    canonicalEvent.partnerId(), canonicalEvent.sku(), traceId);
        }

        return report;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize shadow comparison payload", ex);
        }
    }
}
