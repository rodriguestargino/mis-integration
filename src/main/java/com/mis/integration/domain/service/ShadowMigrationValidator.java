package com.mis.integration.domain.service;

import com.mis.integration.domain.model.CanonicalSyncEvent;
import com.mis.integration.domain.model.LegacyAdapterOutput;
import com.mis.integration.domain.model.ShadowDiffReport;
import com.mis.integration.domain.model.ShadowDiffReport.FieldDiff;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compares 100% of critical fields between the legacy adapter output and
 * the new MIS canonical output during shadow/parallel-run migrations.
 */
public class ShadowMigrationValidator {

    public ShadowDiffReport compare(LegacyAdapterOutput legacy, CanonicalSyncEvent canonical) {
        Objects.requireNonNull(legacy, "legacy output must not be null");
        Objects.requireNonNull(canonical, "canonical event must not be null");

        List<FieldDiff> diffs = new ArrayList<>();

        compareText("sku", legacy.sku(), canonical.sku(), diffs);
        comparePrice("finalPrice", legacy.finalPrice(), canonical.pricing().finalPrice(), diffs);
        compareText("availableStock",
                String.valueOf(legacy.availableStock()),
                String.valueOf(canonical.availableStock()),
                diffs);
        compareText("accountId", legacy.accountId(), canonical.segmentation().accountId(), diffs);
        compareText("deliveryCenterId", legacy.deliveryCenterId(), canonical.segmentation().deliveryCenterId(), diffs);

        return new ShadowDiffReport(
                canonical.traceId(),
                canonical.partnerId(),
                canonical.sku(),
                diffs.isEmpty(),
                List.copyOf(diffs)
        );
    }

    private void compareText(String field, String legacyValue, String misValue, List<FieldDiff> diffs) {
        if (!Objects.equals(legacyValue, misValue)) {
            diffs.add(new FieldDiff(field, Objects.toString(legacyValue, "null"), Objects.toString(misValue, "null")));
        }
    }

    private void comparePrice(String field, BigDecimal legacyPrice, BigDecimal misPrice, List<FieldDiff> diffs) {
        boolean equal = (legacyPrice == null && misPrice == null)
                || (legacyPrice != null && misPrice != null && legacyPrice.compareTo(misPrice) == 0);
        if (!equal) {
            diffs.add(new FieldDiff(
                    field,
                    legacyPrice != null ? legacyPrice.toPlainString() : "null",
                    misPrice != null ? misPrice.toPlainString() : "null"
            ));
        }
    }
}
