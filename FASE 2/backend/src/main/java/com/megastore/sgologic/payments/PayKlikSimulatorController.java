package com.megastore.sgologic.payments;

import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

/** Endpoint de laboratorio para verificar BB-06 sin implementar PAY-01. */
@RestController
@RequestMapping("/api/v1/simulators/payklik")
class PayKlikSimulatorController {

    private final PayKlikGatewayPort gateway;

    PayKlikSimulatorController(PayKlikGatewayPort gateway) {
        this.gateway = gateway;
    }

    @PostMapping("/authorizations")
    PayKlikGatewayPort.AuthorizationResult authorize(
            @Valid @RequestBody AuthorizationRequest body,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            HttpServletRequest request) {
        UUID correlationId = UUID.fromString(request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME).toString());
        return gateway.authorize(new PayKlikGatewayPort.AuthorizationRequest(
                body.orderId(), body.amount(), body.currency(), body.paymentReference(),
                correlationId, idempotencyKey));
    }

    record AuthorizationRequest(
            @NotNull UUID orderId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String currency,
            @NotBlank String paymentReference
    ) {
    }
}
