package com.megastore.sgologic.payments;

import com.megastore.sgologic.payments.api.PaymentRequest;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * PAY-01 / CDU-03: crea el intento de pago con una referencia segura y
 * expone su consulta. No acepta PAN completo ni CVV.
 */
@RestController
@RequestMapping("/api/v1/payments")
class PaymentController {

    private final PaymentAttemptService paymentService;

    PaymentController(PaymentAttemptService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/attempts")
    ResponseEntity<PaymentAttemptResponse> request(
            @Valid @RequestBody CreatePaymentAttemptRequest body,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            HttpServletRequest httpRequest) {
        UUID correlationId = UUID.fromString(
                httpRequest.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME).toString());
        PaymentAttemptOutcome outcome = paymentService.requestAttempt(new PaymentRequest(
                body.orderId(), body.amount(), body.currency(), body.channel(), body.method(),
                body.paymentReference(), correlationId, idempotencyKey));
        PaymentAttemptResponse response = PaymentAttemptResponse.from(outcome.attempt());
        if (!outcome.created()) {
            return ResponseEntity.ok(response);
        }
        HttpStatus status = outcome.attempt().getStatus() == PaymentAttemptStatus.PENDING
                ? HttpStatus.ACCEPTED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/attempts/{attemptId}")
    PaymentAttemptResponse get(@PathVariable UUID attemptId) {
        return PaymentAttemptResponse.from(paymentService.getAttempt(attemptId));
    }

    @GetMapping("/attempts")
    List<PaymentAttemptResponse> getForOrder(@RequestParam UUID orderId) {
        return paymentService.getAttemptsForOrder(orderId).stream()
                .map(PaymentAttemptResponse::from).toList();
    }

    record CreatePaymentAttemptRequest(
            @NotNull UUID orderId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @NotBlank String channel,
            @NotBlank String method,
            @NotBlank @Size(max = 128) String paymentReference
    ) {
    }
}
