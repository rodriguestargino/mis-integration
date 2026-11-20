package com.mis.integration.infrastructure.persistence;

import com.mis.integration.domain.model.AuditStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integration_audit_log")
public class IntegrationAuditEntity {

    @Id
    private UUID id;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @Column(name = "partner_id", nullable = false, length = 100)
    private String partnerId;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AuditStatus status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "canonical_payload", columnDefinition = "TEXT")
    private String canonicalPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IntegrationAuditEntity() {
    }

    public IntegrationAuditEntity(
            UUID id,
            String traceId,
            String partnerId,
            String sku,
            String payloadHash,
            AuditStatus status,
            String failureReason,
            String rawPayload,
            String canonicalPayload
    ) {
        this.id = id;
        this.traceId = traceId;
        this.partnerId = partnerId;
        this.sku = sku;
        this.payloadHash = payloadHash;
        this.status = status;
        this.failureReason = failureReason;
        this.rawPayload = rawPayload;
        this.canonicalPayload = canonicalPayload;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public String getSku() {
        return sku;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public AuditStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public String getCanonicalPayload() {
        return canonicalPayload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markDlqRejected(String reason) {
        this.status = AuditStatus.DLQ_REJECTED;
        this.failureReason = reason;
    }
}
