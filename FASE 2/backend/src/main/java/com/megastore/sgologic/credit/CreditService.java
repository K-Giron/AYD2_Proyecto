package com.megastore.sgologic.credit;

import com.megastore.sgologic.audit.api.AuditEntryRequest;
import com.megastore.sgologic.audit.api.AuditRecorder;
import com.megastore.sgologic.credit.api.CreditEvaluationRequest;
import com.megastore.sgologic.credit.api.CreditEvaluationResult;
import com.megastore.sgologic.credit.api.CreditOperationResult;
import com.megastore.sgologic.credit.api.CreditPort;
import com.megastore.sgologic.credit.api.CreditRequest;
import com.megastore.sgologic.integration.outbox.OutboxEventRequest;
import com.megastore.sgologic.integration.outbox.PublishEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
class CreditService implements CreditPort {

    private final CreditCustomerProfileRepository profiles;
    private final CreditDecisionRepository decisions;
    private final CreditOperationRepository operations;
    private final CreditEligibilityPolicy policy;
    private final CreditProperties properties;
    private final CreditConcurrencyGuard concurrency;
    private final PublishEvent events;
    private final CreditOperationEvents operationEvents;
    private final AuditRecorder audit;
    private final Clock clock;

    CreditService(CreditCustomerProfileRepository profiles,
                  CreditDecisionRepository decisions,
                  CreditOperationRepository operations,
                  CreditEligibilityPolicy policy,
                  CreditProperties properties,
                  CreditConcurrencyGuard concurrency,
                  PublishEvent events,
                  CreditOperationEvents operationEvents,
                  AuditRecorder audit,
                  Clock clock) {
        this.profiles = profiles;
        this.decisions = decisions;
        this.operations = operations;
        this.policy = policy;
        this.properties = properties;
        this.concurrency = concurrency;
        this.events = events;
        this.operationEvents = operationEvents;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreditEvaluationResult evaluate(CreditEvaluationRequest request) {
        requireRequest(request.customerId(), request.amount(), request.currency(), request.correlationId());
        String currency = normalizeCurrency(request.currency());
        CreditEligibilityPolicy.PolicyResult result = policy.evaluate(
                profileFor(request.customerId()), request.amount());
        CreditDecision decision = decisions.save(CreditDecision.create(
                request.customerId(), request.amount(), currency, result.eligible(),
                result.approvedLimit(), policy.ruleVersion(), result.reasonCode(), result.inputsSnapshot(),
                request.correlationId(), clock.instant()));
        publishDecision(decision, request.correlationId());
        return toEvaluation(decision);
    }

    @Override
    @Transactional
    public CreditOperationResult requestCredit(CreditRequest request) {
        requireRequest(request.customerId(), request.amount(), request.currency(), request.correlationId());
        if (request.orderId() == null) throw new IllegalArgumentException("orderId es obligatorio.");
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("idempotencyKey es obligatorio.");
        }
        // El lock del cliente evita que dos órdenes diferentes consuman el
        // mismo saldo disponible de forma simultánea.
        concurrency.lock(request.customerId(), request.orderId(), request.idempotencyKey());
        Optional<CreditOperation> repeated = operations.findByIdempotencyKey(request.idempotencyKey());
        if (repeated.isPresent()) {
            assertSameRequest(repeated.get(), request);
            return toOperation(repeated.get());
        }
        Optional<CreditOperation> existingOrder = operations.findByOrderId(request.orderId());
        if (existingOrder.isPresent()) {
            assertSameRequest(existingOrder.get(), request);
            return toOperation(existingOrder.get());
        }

        CreditEvaluationResult evaluation = evaluate(new CreditEvaluationRequest(
                request.customerId(), request.amount(), request.currency(), request.correlationId()));
        CreditDecision decision = decisions.findById(evaluation.decisionId()).orElseThrow();
        CreditCustomerProfile profile = profiles.findById(request.customerId()).orElse(null);
        if (evaluation.eligible()) {
            if (profile == null) {
                throw new IllegalStateException("No existe el perfil aprobado para consumir el crédito.");
            }
            profile.consumeAvailableCredit(request.amount());
            profiles.save(profile);
        }
        LocalDate dueDate = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC).plusDays(policy.termDays());
        CreditOperation operation = operations.save(CreditOperation.create(
                request.orderId(), request.customerId(), decision, request.amount(),
                normalizeCurrency(request.currency()), dueDate, request.idempotencyKey(),
                request.correlationId(), clock.instant()));
        operationEvents.publish(operation, profile == null ? BigDecimal.ZERO : profile.getAvailableCredit(),
                request.correlationId());
        return toOperation(operation);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CreditOperationResult> latestForOrder(UUID orderId) {
        return operations.findByOrderId(orderId).map(this::toOperation);
    }

    /**
     * Toda identidad de cliente dispone de una línea inicial propia. El INSERT
     * idempotente también cubre cuentas creadas después de ejecutar Flyway y
     * evita compartir el saldo entre clientes concurrentes.
     */
    private CreditCustomerProfile profileFor(UUID customerId) {
        profiles.initializeIfMissing(customerId, properties.getInitialLimit(), clock.instant());
        return profiles.findById(customerId)
                .orElseThrow(() -> new IllegalStateException("No se pudo inicializar el crédito del cliente."));
    }

    private void requireRequest(UUID customerId, BigDecimal amount, String currency, UUID correlationId) {
        if (customerId == null || correlationId == null) {
            throw new IllegalArgumentException("customerId y correlationId son obligatorios.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero.");
        }
        normalizeCurrency(currency);
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || !currency.matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("La moneda debe usar tres letras.");
        }
        return currency.toUpperCase();
    }

    private void assertSameRequest(CreditOperation operation, CreditRequest request) {
        if (!operation.getOrderId().equals(request.orderId())
                || !operation.getCustomerId().equals(request.customerId())
                || operation.getAmount().compareTo(request.amount()) != 0
                || !operation.getCurrency().equalsIgnoreCase(request.currency())) {
            throw new IllegalArgumentException("La llave idempotente ya pertenece a otra solicitud de crédito.");
        }
    }

    private void publishDecision(CreditDecision decision, UUID correlationId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("decisionId", decision.getId());
        payload.put("customerId", decision.getCustomerId());
        payload.put("eligible", decision.isEligible());
        payload.put("approvedLimit", decision.getApprovedLimit());
        payload.put("ruleVersion", decision.getRuleVersion());
        payload.put("reasonCode", decision.getReasonCode());
        events.publish(new OutboxEventRequest("CREDIT_DECISION", decision.getId(),
                "CREDIT_DECISION_RECORDED", 1, correlationId, correlationId, payload));
        audit.record(new AuditEntryRequest("CREDIT_DECISION", decision.getId(), decision.getCustomerId(),
                "CREDIT_DECISION_RECORDED", decision.isEligible() ? "APPROVED" : "REJECTED",
                null, "{\"reasonCode\":\"" + decision.getReasonCode() + "\",\"inputs\":"
                + decision.getInputsSnapshot() + "}",
                correlationId, correlationId, null));
    }

    private CreditEvaluationResult toEvaluation(CreditDecision decision) {
        return new CreditEvaluationResult(decision.getId(), decision.isEligible(),
                decision.getApprovedLimit(), decision.getRuleVersion(),
                decision.getReasonCode(), policy.termDays(), decision.getEvaluatedAt());
    }

    private CreditOperationResult toOperation(CreditOperation operation) {
        return new CreditOperationResult(operation.getId(), operation.getDecisionId(),
                operation.getStatus(), operation.getApprovedLimit(), operation.getDueDate(),
                operation.getRuleVersion(), operation.getReasonCode(), operation.getCreatedAt());
    }
}
