package com.megastore.sgologic.audit;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
class AuditController {

    private final AuditService service;

    AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping("/events")
    List<AuditEventResponse> search(
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) UUID transactionId,
            @RequestHeader("X-Actor-Id") String requester,
            @RequestHeader("X-Actor-Role") String role) {
        if ((orderId == null) == (transactionId == null)) {
            throw new IllegalArgumentException("Proporcione exactamente orderId o transactionId.");
        }
        return orderId != null
                ? service.searchByOrder(orderId, requester, role, false)
                : service.searchByTransaction(transactionId, requester, role);
    }

    @GetMapping(value = "/exports", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<AuditEventResponse>> export(
            @RequestParam UUID orderId,
            @RequestHeader("X-Actor-Id") String requester,
            @RequestHeader("X-Actor-Role") String role) {
        List<AuditEventResponse> evidence = service.searchByOrder(orderId, requester, role, true);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=order-" + orderId + "-audit.json")
                .body(evidence);
    }
}
