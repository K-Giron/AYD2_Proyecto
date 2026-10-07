package com.payklik.gateway.payments;

import com.payklik.gateway.operation.OperationService;
import com.payklik.gateway.shared.GatewayException;
import com.payklik.gateway.shared.PayKlikProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** API REST de autorizaciones para comercios afiliados (MegaStore / SGO-Logic), con {@code X-Api-Key}. */
@RestController
@RequestMapping("/api/v1/authorizations")
class MerchantAuthorizationController {

    private final AuthorizationService service;
    private final OperationService operation;
    private final PayKlikProperties properties;

    MerchantAuthorizationController(AuthorizationService service, OperationService operation, PayKlikProperties properties) {
        this.service = service;
        this.operation = operation;
        this.properties = properties;
    }

    record AuthorizeRequest(
            @NotBlank @Size(max = 120) String idempotencyKey,
            @NotBlank @Size(max = 80) String orderReference,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @NotBlank @Size(max = 120) String paymentReference) {
    }

    @PostMapping
    ResponseEntity<AuthorizationResponse> authorize(@Valid @RequestBody AuthorizeRequest body) {
        operation.rejectIfOutOfService();
        AuthorizationService.Command command = new AuthorizationService.Command(properties.merchant(),
                body.idempotencyKey().trim(), body.orderReference().trim(), body.amount(), body.currency(),
                body.paymentReference());
        AuthorizationService.Outcome outcome;
        try {
            outcome = service.authorize(command, operation.current().getMode());
        } catch (DataIntegrityViolationException concurrent) {
            // Dos intentos simultáneos con la misma llave: uno cobra, el otro recibe el mismo resultado.
            outcome = service.authorize(command, operation.current().getMode());
        }
        operation.delayIfSlow();
        return ResponseEntity.status(outcome.created() ? HttpStatus.CREATED : HttpStatus.OK).body(outcome.authorization());
    }

    /** Conciliación: el comercio pregunta qué pasó con un cobro cuya respuesta no recibió. */
    @GetMapping("/{idempotencyKey}")
    AuthorizationResponse find(@PathVariable String idempotencyKey) {
        operation.rejectIfOutOfService();
        return service.find(properties.merchant(), idempotencyKey)
                .orElseThrow(() -> new GatewayException(HttpStatus.NOT_FOUND, "AUTHORIZATION_NOT_FOUND",
                        "No hay autorización con esa llave."));
    }
}
