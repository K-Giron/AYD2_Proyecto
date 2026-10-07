package com.logiexpress.carrier.guides;

import com.logiexpress.carrier.operation.OperationMode;
import com.logiexpress.carrier.operation.OperationService;
import com.logiexpress.carrier.operation.OperationSettings;
import com.logiexpress.carrier.shared.LogiExpressProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Portal de operación de LogiExpress: mensajeros y supervisión del servicio. */
@RestController
@RequestMapping("/api/portal")
class PortalController {

    private final GuideService service;
    private final OperationService operation;
    private final LogiExpressProperties properties;

    PortalController(GuideService service, OperationService operation, LogiExpressProperties properties) {
        this.service = service;
        this.operation = operation;
        this.properties = properties;
    }

    record CourierRequest(@NotBlank @Size(max = 80) String courier, @Size(max = 120) String receivedBy) {
    }

    record OperationRequest(@NotNull OperationMode mode, @NotBlank @Size(max = 80) String updatedBy) {
    }

    record OperationView(OperationMode mode, int slowLatencyMs, String updatedBy, Instant updatedAt, String merchant) {
        static OperationView of(OperationSettings settings, String merchant) {
            return new OperationView(settings.getMode(), settings.getSlowLatencyMs(), settings.getUpdatedBy(),
                    settings.getUpdatedAt(), merchant);
        }
    }

    @GetMapping("/guides")
    List<GuideResponse> guides() {
        return service.recent();
    }

    @GetMapping("/guides/{trackingNumber}")
    GuideResponse guide(@PathVariable String trackingNumber) {
        return service.find(null, trackingNumber.trim().toUpperCase(java.util.Locale.ROOT));
    }

    @PostMapping("/guides/{trackingNumber}/pickup")
    GuideResponse pickUp(@PathVariable String trackingNumber, @Valid @RequestBody CourierRequest body) {
        return service.pickUp(trackingNumber, body.courier().trim());
    }

    @PostMapping("/guides/{trackingNumber}/deliver")
    GuideResponse deliver(@PathVariable String trackingNumber, @Valid @RequestBody CourierRequest body) {
        return service.deliver(trackingNumber, body.courier().trim(), body.receivedBy());
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
