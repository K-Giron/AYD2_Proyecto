package com.megastore.sgologic.credit;

import com.megastore.sgologic.credit.api.CreditOperationResult;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Operaciones de Finanzas sobre una obligación de crédito; el rol FINANCE lo fija SecurityConfig. */
@RestController
@RequestMapping("/api/v1/credit/operations")
class CreditOperationsController {

    private final CreditLifecycleService lifecycle;

    CreditOperationsController(CreditLifecycleService lifecycle) {
        this.lifecycle = lifecycle;
    }

    /** Registra el pago del cliente. Es idempotente: pagar dos veces deja un solo efecto. */
    @PostMapping("/{operationId}/settle")
    ResponseEntity<CreditOperationResult> settle(@PathVariable UUID operationId, HttpServletRequest request) {
        return ResponseEntity.ok(lifecycle.settle(operationId, correlationOf(request)));
    }

    private static UUID correlationOf(HttpServletRequest request) {
        Object attribute = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        try {
            return attribute == null ? UUID.randomUUID() : UUID.fromString(attribute.toString());
        } catch (IllegalArgumentException invalid) {
            return UUID.randomUUID();
        }
    }
}
