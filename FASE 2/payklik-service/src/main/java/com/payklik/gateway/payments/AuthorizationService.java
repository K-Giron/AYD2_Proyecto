package com.payklik.gateway.payments;

import com.payklik.gateway.operation.OperationMode;
import com.payklik.gateway.shared.GatewayException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthorizationService {

    private final PaymentTransactionRepository repository;
    private final Clock clock;

    AuthorizationService(PaymentTransactionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public record Command(String merchant, String idempotencyKey, String orderReference, BigDecimal amount,
                          String currency, String paymentReference) {
    }

    public record Outcome(AuthorizationResponse authorization, boolean created) {
    }

    /**
     * Autoriza una sola vez por llave del comercio: si la llave ya existe,
     * devuelve el mismo resultado sin volver a cobrar.
     */
    @Transactional
    public Outcome authorize(Command command, OperationMode mode) {
        Optional<PaymentTransaction> existing = repository.findByMerchantAndIdempotencyKey(command.merchant(), command.idempotencyKey());
        if (existing.isPresent()) {
            return new Outcome(AuthorizationResponse.of(existing.get()), false);
        }
        rejectSensitiveData(command.paymentReference());
        String reference = command.paymentReference().trim();
        String method = reference.toLowerCase(Locale.ROOT).startsWith("tok_") ? "TARJETA" : "TRANSFERENCIA";
        String currency = command.currency().trim().toUpperCase(Locale.ROOT);
        PaymentTransaction tx = mode == OperationMode.RECHAZAR
                ? PaymentTransaction.rejected(command.merchant(), command.idempotencyKey(), command.orderReference(),
                        command.amount(), currency, method, reference, "DECLINADA_POR_EMISOR", clock.instant())
                : PaymentTransaction.approved(command.merchant(), command.idempotencyKey(), command.orderReference(),
                        command.amount(), currency, method, reference,
                        "PK-%08d".formatted(repository.nextAuthorizationSequence()), clock.instant());
        return new Outcome(AuthorizationResponse.of(repository.saveAndFlush(tx)), true);
    }

    @Transactional(readOnly = true)
    public Optional<AuthorizationResponse> find(String merchant, String idempotencyKey) {
        return repository.findByMerchantAndIdempotencyKey(merchant, idempotencyKey).map(AuthorizationResponse::of);
    }

    @Transactional(readOnly = true)
    public List<AuthorizationResponse> recent() {
        return repository.findTop300ByOrderByCreatedAtDesc().stream().map(AuthorizationResponse::of).toList();
    }

    /** PayKlik solo acepta tokens o referencias; un número de tarjeta o un CVV se rechazan sin guardarse. */
    private static void rejectSensitiveData(String reference) {
        String compact = reference.replace(" ", "").replace("-", "");
        if (compact.matches(".*\\d{13,19}.*") || compact.toLowerCase(Locale.ROOT).contains("cvv")) {
            throw new GatewayException(HttpStatus.BAD_REQUEST, "SENSITIVE_DATA",
                    "PayKlik solo acepta tarjetas tokenizadas o referencias de transferencia; nunca PAN ni CVV.");
        }
    }
}
