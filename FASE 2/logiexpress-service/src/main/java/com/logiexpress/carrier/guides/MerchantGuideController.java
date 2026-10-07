package com.logiexpress.carrier.guides;

import com.logiexpress.carrier.operation.OperationService;
import com.logiexpress.carrier.shared.LogiExpressProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** API REST para comercios afiliados (MegaStore / SGO-Logic), con {@code X-Api-Key}. */
@RestController
@RequestMapping("/api/v1/guides")
class MerchantGuideController {

    private final GuideService service;
    private final OperationService operation;
    private final LogiExpressProperties properties;

    MerchantGuideController(GuideService service, OperationService operation, LogiExpressProperties properties) {
        this.service = service;
        this.operation = operation;
        this.properties = properties;
    }

    record CreateGuideRequest(
            @NotBlank @Size(max = 80) String merchantReference,
            @Size(max = 80) String orderReference,
            @NotBlank @Size(max = 200) String origin,
            @NotBlank @Size(max = 400) String destination,
            @Size(max = 20) String pickupPoint) {
    }

    @PostMapping
    ResponseEntity<GuideResponse> create(@Valid @RequestBody CreateGuideRequest body) {
        operation.rejectIfOutOfService();
        GuideService.IssueCommand command = new GuideService.IssueCommand(properties.merchant(), body.merchantReference().trim(),
                body.orderReference(), body.origin().trim(), body.destination().trim(), body.pickupPoint());
        GuideService.Issued issued;
        try {
            issued = service.issue(command);
        } catch (DataIntegrityViolationException concurrent) {
            // Dos solicitudes simultáneas con la misma referencia: gana una, la otra recibe la misma guía.
            issued = service.issue(command);
        }
        operation.delayIfSlow();
        return ResponseEntity.status(issued.created() ? HttpStatus.CREATED : HttpStatus.OK).body(issued.guide());
    }

    @GetMapping("/{trackingNumber}")
    GuideResponse get(@PathVariable String trackingNumber) {
        operation.rejectIfOutOfService();
        return service.find(properties.merchant(), trackingNumber);
    }

    /** Rastreo incremental: el comercio pide lo que cambió desde su último corte. */
    @GetMapping
    List<GuideResponse> updatedSince(@RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant updatedSince) {
        operation.rejectIfOutOfService();
        return service.updatedSince(properties.merchant(), updatedSince);
    }
}
