package com.mis.integration.infrastructure.persistence;

import com.mis.integration.application.port.AuditPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresAuditAdapter implements AuditPort {

    private final IntegrationAuditJpaRepository repository;

    public PostgresAuditAdapter(IntegrationAuditJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuditRecordSummary> findByPartnerAndHash(String partnerId, String payloadHash) {
        return repository.findByPartnerIdAndPayloadHash(partnerId, payloadHash)
                .map(entity -> new AuditRecordSummary(entity.getId(), entity.getTraceId(), entity.getStatus()));
    }

    @Override
    @Transactional
    public UUID saveAudit(AuditEntry entry) {
        UUID id = UUID.randomUUID();
        IntegrationAuditEntity entity = new IntegrationAuditEntity(
                id,
                entry.traceId(),
                entry.partnerId(),
                entry.sku(),
                entry.payloadHash(),
                entry.status(),
                entry.failureReason(),
                entry.rawPayload(),
                entry.canonicalPayload()
        );
        return repository.save(entity).getId();
    }

    @Override
    @Transactional
    public void markDlqFallback(UUID auditId, String failureReason) {
        repository.findById(auditId).ifPresent(entity -> {
            entity.markDlqRejected(failureReason);
            repository.save(entity);
        });
    }
}
