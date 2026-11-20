package com.mis.integration.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mis.integration.application.port.AuditPort;
import com.mis.integration.application.port.EventPublisherPort;
import com.mis.integration.application.service.ErpIngestUseCase;
import com.mis.integration.application.service.PayloadHashService;
import com.mis.integration.domain.service.ErpPayloadValidator;
import com.mis.integration.domain.service.ErpToCanonicalMapper;
import com.mis.integration.domain.service.TaxCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class MisBeanConfig {

    @Bean
    public Clock misClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ErpPayloadValidator erpPayloadValidator() {
        return new ErpPayloadValidator();
    }

    @Bean
    public TaxCalculator taxCalculator() {
        return new TaxCalculator();
    }

    @Bean
    public ErpToCanonicalMapper erpToCanonicalMapper(
            ErpPayloadValidator validator,
            TaxCalculator taxCalculator,
            Clock misClock
    ) {
        return new ErpToCanonicalMapper(validator, taxCalculator, misClock);
    }

    @Bean
    public PayloadHashService payloadHashService() {
        return new PayloadHashService();
    }

    @Bean
    public ErpIngestUseCase erpIngestUseCase(
            PayloadHashService hashService,
            ErpToCanonicalMapper mapper,
            AuditPort auditPort,
            EventPublisherPort publisherPort,
            ObjectMapper objectMapper
    ) {
        return new ErpIngestUseCase(hashService, mapper, auditPort, publisherPort, objectMapper);
    }
}
