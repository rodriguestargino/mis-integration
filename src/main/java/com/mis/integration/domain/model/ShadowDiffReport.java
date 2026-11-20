package com.mis.integration.domain.model;

import java.util.List;

/**
 * Structured comparison report between legacy adapter output and the new MIS canonical output.
 */
public record ShadowDiffReport(
        String traceId,
        String partnerId,
        String sku,
        boolean matched,
        List<FieldDiff> diffs
) {
    public record FieldDiff(
            String field,
            String legacyValue,
            String misValue
    ) {
    }
}
