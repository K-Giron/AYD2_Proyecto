package com.megastore.sgologic.payments;

import com.megastore.sgologic.payments.api.InvalidPaymentRequestException;
import com.megastore.sgologic.payments.api.PaymentMethod;
import com.megastore.sgologic.payments.api.PaymentPort;
import com.megastore.sgologic.payments.api.PaymentRequest;
import com.megastore.sgologic.payments.api.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PAY-01/PAY-02 / CDU-03: registra el intento con orden, canal, fecha,
 * referencia y resultado usando el adaptador PayKlik de BB-06, sin
 * almacenar PAN ni CVV.
 *
 * <p>Idempotente por {@code idempotencyKey}: una repetición devuelve el
 * intento existente sin volver a llamar al proveedor, incluso si dos
 * solicitudes compiten (la restricción única resuelve el empate). El
 * adaptador está envuelto con límite de tiempo y circuit breaker
 * ({@link ResilientPayKlikClient}); un timeout, una caída o el circuito
 * abierto dejan el intento {@code INDETERMINATE} y publican
 * {@code PaymentIndeterminate} por Outbox, nunca una aprobación (ADR-003).
 * La conciliación ≤ 60 s pertenece a PAY-03.</p>
 */
@Service
public class PaymentAttemptService implements PaymentPort {

    private static final Logger log = LoggerFactory.getLogger(PaymentAttemptService.class);

    private final PaymentAttemptRepository repository;
    private final ResilientPayKlikClient payKlikClient;
    private final PaymentAttemptWriter writer;

    public PaymentAttemptService(PaymentAttemptRepository repository, ResilientPayKlikClient payKlikClient,
                                 PaymentAttemptWriter writer) {
        this.repository = repository;
        this.payKlikClient = payKlikClient;
        this.writer = writer;
    }

    @Override
    public PaymentResult requestPayment(PaymentRequest request) {
        return toResult(requestAttempt(request).attempt());
    }

    PaymentAttemptOutcome requestAttempt(PaymentRequest request) {
        Optional<PaymentAttempt> existing = repository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Intento de pago repetido para orderId={} con idempotencyKey conocida; "
                    + "se devuelve el intento existente sin llamar a PayKlik.", request.orderId());
            return new PaymentAttemptOutcome(existing.get(), false);
        }

        PaymentChannel channel = parseChannel(request.channel());
        PaymentMethod method = parseMethod(request.method());
        PaymentAttempt attempt;
        try {
            attempt = writer.create(request.orderId(), channel, method, request.amount(), request.currency(),
                    request.correlationId(), request.idempotencyKey(), request.paymentReference());
        } catch (DataIntegrityViolationException duplicate) {
            return repository.findByIdempotencyKey(request.idempotencyKey())
                    .map(stored -> new PaymentAttemptOutcome(stored, false))
                    .orElseThrow(() -> duplicate);
        }

        try {
            PayKlikGatewayPort.AuthorizationResult result = payKlikClient.authorize(
                    new PayKlikGatewayPort.AuthorizationRequest(request.orderId(), request.amount(),
                            request.currency(), request.paymentReference(), request.correlationId(),
                            request.idempotencyKey()));
            if (result.approved()) {
                return new PaymentAttemptOutcome(writer.markApproved(attempt.getId(),
                        result.providerReference(), result.paymentAttemptId()), true);
            }
            return new PaymentAttemptOutcome(writer.markRejected(attempt.getId(),
                    result.rejectionCode(), null, result.paymentAttemptId()), true);
        } catch (PayKlikGatewayPort.PayKlikTimeoutException failure) {
            return indeterminate(attempt, request.orderId(), "PAYKLIK_TIMEOUT", failure.getMessage());
        } catch (PayKlikGatewayPort.PayKlikUnavailableException failure) {
            return indeterminate(attempt, request.orderId(), "PAYKLIK_UNAVAILABLE", failure.getMessage());
        } catch (PayKlikCircuitOpenException failure) {
            return indeterminate(attempt, request.orderId(), "PAYKLIK_CIRCUIT_OPEN", failure.getMessage());
        }
    }

    public PaymentAttempt getAttempt(UUID id) {
        return repository.findById(id).orElseThrow(() -> new PaymentAttemptNotFoundException(id));
    }

    public List<PaymentAttempt> getAttemptsForOrder(UUID orderId) {
        return repository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    @Override
    public Optional<PaymentResult> latestForOrder(UUID orderId) {
        return getAttemptsForOrder(orderId).stream().findFirst().map(this::toResult);
    }

    private PaymentAttemptOutcome indeterminate(PaymentAttempt attempt, UUID orderId, String code, String message) {
        log.warn("Pago indeterminado para orderId={} (intento {}): {}. La orden no se pierde; "
                + "la conciliación de PAY-03 lo resolverá.", orderId, attempt.getId(), code);
        return new PaymentAttemptOutcome(writer.markIndeterminate(attempt.getId(), code, message), true);
    }

    private PaymentChannel parseChannel(String value) {
        try {
            return PaymentChannel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException invalid) {
            throw new InvalidPaymentRequestException("Canal de pago inválido: " + value + ".");
        }
    }

    private PaymentMethod parseMethod(String value) {
        try {
            return PaymentMethod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException invalid) {
            throw new InvalidPaymentRequestException("Método de pago inválido: " + value + ".");
        }
    }

    private PaymentResult toResult(PaymentAttempt attempt) {
        return new PaymentResult(attempt.getId(), attempt.getStatus().name(),
                attempt.getProviderReference(), attempt.getUpdatedAt());
    }
}
