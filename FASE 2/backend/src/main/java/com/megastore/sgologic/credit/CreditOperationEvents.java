package com.megastore.sgologic.credit;

import com.megastore.sgologic.audit.api.AuditEntryRequest;
import com.megastore.sgologic.audit.api.AuditRecorder;
import com.megastore.sgologic.integration.outbox.OutboxEventRequest;
import com.megastore.sgologic.integration.outbox.PublishEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Evento de negocio y evidencia de auditoría de cada cambio de estado de una operación. */
@Component
class CreditOperationEvents {

    private final PublishEvent events;
    private final AuditRecorder audit;

    CreditOperationEvents(PublishEvent events, AuditRecorder audit) {
        this.events = events;
        this.audit = audit;
    }

    /** Publica {@code CREDIT_<ESTADO>} (GRANTED, REJECTED, OVERDUE, SETTLED o CANCELLED). */
    void publish(CreditOperation operation, BigDecimal remainingAvailableCredit, UUID correlationId) {
        String eventType = "CREDIT_" + operation.getStatus().name();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", operation.getOrderId());
        payload.put("customerId", operation.getCustomerId());
        payload.put("creditOperationId", operation.getId());
        payload.put("decisionId", operation.getDecisionId());
        payload.put("status", operation.getStatus().name());
        payload.put("amount", operation.getAmount());
        payload.put("currency", operation.getCurrency());
        payload.put("approvedLimit", operation.getApprovedLimit());
        payload.put("remainingAvailableCredit", remainingAvailableCredit);
        payload.put("dueDate", operation.getDueDate());
        payload.put("ruleVersion", operation.getRuleVersion());
        payload.put("reasonCode", operation.getReasonCode());
        events.publish(new OutboxEventRequest("CREDIT_OPERATION", operation.getId(),
                eventType, 1, correlationId, correlationId, payload));
        audit.record(new AuditEntryRequest("CREDIT_OPERATION", operation.getId(), operation.getOrderId(),
                eventType, operation.getStatus().name(), null,
                "{\"reasonCode\":\"" + operation.getReasonCode() + "\"}",
                correlationId, correlationId, null));
    }
}
