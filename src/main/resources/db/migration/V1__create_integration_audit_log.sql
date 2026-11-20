CREATE TABLE IF NOT EXISTS integration_audit_log (
    id                UUID PRIMARY KEY,
    trace_id          VARCHAR(64)  NOT NULL,
    partner_id        VARCHAR(100) NOT NULL,
    sku               VARCHAR(100),
    payload_hash      VARCHAR(64)  NOT NULL,
    status            VARCHAR(32)  NOT NULL,
    failure_reason    TEXT,
    raw_payload       TEXT         NOT NULL,
    canonical_payload TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_integration_audit_partner_hash UNIQUE (partner_id, payload_hash)
);

CREATE INDEX IF NOT EXISTS idx_integration_audit_trace_id ON integration_audit_log (trace_id);
CREATE INDEX IF NOT EXISTS idx_integration_audit_status ON integration_audit_log (status);
