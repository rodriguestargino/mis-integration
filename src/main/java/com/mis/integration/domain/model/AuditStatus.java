package com.mis.integration.domain.model;

/**
 * Lifecycle statuses recorded in the integration audit log.
 */
public enum AuditStatus {
    PUBLISHED,
    DLQ_REJECTED,
    SHADOW_DIFF
}
