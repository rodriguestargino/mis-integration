package com.mis.integration.api.controller;

import com.mis.integration.application.port.AuditPort;
import com.mis.integration.application.port.AuditPort.AuditDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditQueryController {

    private final AuditPort auditPort;

    public AuditQueryController(AuditPort auditPort) {
        this.auditPort = auditPort;
    }

    @GetMapping("/trace/{traceId}")
    public ResponseEntity<List<AuditDetail>> findByTraceId(@PathVariable String traceId) {
        List<AuditDetail> entries = auditPort.findByTraceId(traceId);
        if (entries.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(entries);
    }
}
