package com.megastore.inventoryservice.auditlog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-log")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<AuditLogEntryResponse> list(@RequestParam(required = false) String relatedFolio,
                                             @RequestParam(required = false) UUID pointOfSaleId) {
        return auditLogService.list(relatedFolio, pointOfSaleId).stream().map(AuditLogEntryResponse::from).toList();
    }
}
