package com.payklik.gateway.payments;

import com.payklik.gateway.operation.OperationMode;
import com.payklik.gateway.operation.OperationService;
import com.payklik.gateway.operation.OperationSettings;
import com.payklik.gateway.shared.PayKlikProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Portal de PayKlik: transacciones del comercio y estado del servicio. */
@RestController
@RequestMapping("/api/portal")
class PortalController {

    private final AuthorizationService service;
    private final OperationService operation;
    private final PayKlikProperties properties;

    PortalController(AuthorizationService service, OperationService operation, PayKlikProperties properties) {
        this.service = service;
        this.operation = operation;
        this.properties = properties;
    }

    record OperationRequest(@NotNull OperationMode mode, @NotBlank @Size(max = 80) String updatedBy) {
    }

    record OperationView(OperationMode mode, int slowLatencyMs, String updatedBy, Instant updatedAt, String merchant) {
        static OperationView of(OperationSettings settings, String merchant) {
            return new OperationView(settings.getMode(), settings.getSlowLatencyMs(), settings.getUpdatedBy(),
                    settings.getUpdatedAt(), merchant);
        }
    }

    @GetMapping("/transactions")
    List<AuthorizationResponse> transactions() {
        return service.recent();
    }

    @GetMapping("/operation")
    OperationView operation() {
        return OperationView.of(operation.current(), properties.merchant());
    }

    @PutMapping("/operation")
    OperationView changeOperation(@Valid @RequestBody OperationRequest body) {
        return OperationView.of(operation.change(body.mode(), body.updatedBy().trim()), properties.merchant());
    }
}
