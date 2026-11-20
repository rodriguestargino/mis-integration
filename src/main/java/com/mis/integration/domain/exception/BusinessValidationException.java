package com.mis.integration.domain.exception;

import java.util.List;

/**
 * Thrown when an inbound ERP payload violates mandatory business invariants
 * and must be routed immediately to the DLQ without infinite retries.
 */
public class BusinessValidationException extends RuntimeException {

    private final List<String> violations;

    public BusinessValidationException(List<String> violations) {
        super(String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
