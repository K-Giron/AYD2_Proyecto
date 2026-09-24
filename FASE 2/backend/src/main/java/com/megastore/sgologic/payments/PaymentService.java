package com.megastore.sgologic.payments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implementación del CDU-03 y de la continuidad definida por ADR-003. */
@Service
public class PaymentService implements PaymentProcessingPort {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final long FIRST_RECONCILIATION_SECONDS = 30L;

    private final PaymentAttemptRepository repository;
    private final PayKlikGatewayPort gateway;
    private final PaymentCircuitBreaker circuitBreaker;
    private final TaskScheduler scheduler;
    private final Clock clock;

    public PaymentService(PaymentAttemptRepository repository,
                          PayKlikGatewayPort gateway,
                          PaymentCircuitBreaker circuitBreaker,
                          @Qualifier("paymentReconciliationScheduler") TaskScheduler scheduler,
                          Clock clock) {
        this.repository = repository;
        this.gateway = gateway;
        this.circuitBreaker = circuitBreaker;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PaymentProcessingResult process(PaymentProcessingRequest request) {
        validate(request);
        Optional<PaymentAttempt> repeated = repository.findByIdempotencyKey(request.idempotencyKey());
        if (repeated.isPresent()) return repeated.get().toResult();

        PaymentAttempt attempt = repository.saveAndFlush(PaymentAttempt.pending(request, clock.instant()));
        if (!circuitBreaker.permitsCall()) {
            return preserveAndSchedule(attempt, "PAYKLIK_CIRCUIT_OPEN",
                    "PayKlik está temporalmente aislado por el circuit breaker");
        }

        try {
            PayKlikGatewayPort.AuthorizationResult result = gateway.authorize(
                    new PayKlikGatewayPort.AuthorizationRequest(request.orderId(), request.amount(),
                            request.currency(), request.paymentReference(), request.correlationId(),
                            request.idempotencyKey()));
            circuitBreaker.success();
            if (result.approved()) attempt.approve(result.providerReference(), result.processedAt());
            else attempt.reject(result.rejectionCode(), result.processedAt());
            return repository.save(attempt).toResult();
        } catch (PayKlikGatewayPort.PayKlikTimeoutException | PayKlikGatewayPort.PayKlikUnavailableException failure) {
            circuitBreaker.failure();
            return preserveAndSchedule(attempt, "PAYKLIK_INDETERMINATE", failure.getMessage());
        }
    }

    private PaymentProcessingResult preserveAndSchedule(PaymentAttempt attempt, String code, String message) {
        Instant reconciliationAt = clock.instant().plusSeconds(FIRST_RECONCILIATION_SECONDS);
        attempt.markIndeterminate(code, message, clock.instant(), reconciliationAt);
        repository.save(attempt);
        scheduler.schedule(() -> reconcile(attempt.getId()), reconciliationAt);
        return attempt.toResult();
    }

    @Transactional
    public void reconcile(UUID attemptId) {
        repository.findById(attemptId).filter(a -> a.getStatus() == PaymentStatus.INDETERMINATE)
                .ifPresent(attempt -> {
                    gateway.findByIdempotencyKey(attempt.getIdempotencyKey()).ifPresentOrElse(result -> {
                        if (result.approved()) attempt.approve(result.providerReference(), result.processedAt());
                        else attempt.reject(result.rejectionCode(), result.processedAt());
                        repository.save(attempt);
                    }, () -> log.warn("Pago {} continúa indeterminado; no se reintentó el cobro.", attemptId));
                });
    }

    @Override
    public Optional<PaymentProcessingResult> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(PaymentAttempt::toResult);
    }

    public PaymentAttempt getAttempt(UUID id) {
        return repository.findById(id).orElseThrow(() -> new PaymentAttemptNotFoundException(id));
    }

    public List<PaymentAttempt> getAttemptsForOrder(UUID orderId) {
        return repository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    private void validate(PaymentProcessingRequest request) {
        if (request == null || request.orderId() == null || request.correlationId() == null
                || request.method() == null || request.amount() == null || request.amount().signum() <= 0
                || blank(request.channel()) || blank(request.currency()) || blank(request.paymentReference())
                || blank(request.idempotencyKey())) {
            throw new IllegalArgumentException("El intento de pago no contiene todos los datos obligatorios.");
        }
        if (request.method() != PaymentMethod.CARD) {
            throw new UnsupportedPaymentMethodException(request.method());
        }
        String compact = request.paymentReference().replace(" ", "");
        if (compact.matches("\\d{13,19}") || compact.toLowerCase().contains("cvv")) {
            throw new IllegalArgumentException("Solo se acepta una referencia tokenizada; PAN/CVV están prohibidos.");
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
