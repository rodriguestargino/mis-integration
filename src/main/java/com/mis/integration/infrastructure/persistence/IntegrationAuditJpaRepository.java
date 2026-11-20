package com.mis.integration.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationAuditJpaRepository extends JpaRepository<IntegrationAuditEntity, UUID> {

    Optional<IntegrationAuditEntity> findByPartnerIdAndPayloadHash(String partnerId, String payloadHash);

    List<IntegrationAuditEntity> findAllByTraceIdOrderByCreatedAtAsc(String traceId);
}
