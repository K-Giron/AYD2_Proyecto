package com.megastore.sgologic.integration;

import tools.jackson.databind.JsonNode;
import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations/pos")
class PosIntegrationController {

    private final PosIntegrationService service;

    PosIntegrationController(PosIntegrationService service) {
        this.service = service;
    }

    @PostMapping("/events")
    ResponseEntity<PosDeliveryResponse> publish(@Valid @RequestBody PublishPosEventRequest body,
                                                HttpServletRequest request) {
        UUID correlationId = UUID.fromString(request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME).toString());
        PosDelivery delivery = service.publish(body.eventId(), body.eventType(), body.orderId(),
                body.storeNumber(), body.payload().toString(), correlationId);
        return ResponseEntity.accepted().body(PosDeliveryResponse.from(delivery));
    }

    @GetMapping("/deliveries/{deliveryId}")
    PosDeliveryResponse get(@PathVariable UUID deliveryId) {
        return PosDeliveryResponse.from(service.getDelivery(deliveryId));
    }

    @GetMapping("/events/{eventId}")
    List<PosDeliveryResponse> getForEvent(@PathVariable UUID eventId) {
        return service.getDeliveriesForEvent(eventId).stream().map(PosDeliveryResponse::from).toList();
    }

    record PublishPosEventRequest(
            @NotNull UUID eventId,
            @NotBlank String eventType,
            @NotNull UUID orderId,
            @Min(1) @Max(32) int storeNumber,
            @NotNull JsonNode payload
    ) {
    }

    record PosDeliveryResponse(UUID id, UUID eventId, String eventType, UUID orderId, int storeNumber,
                               PosDeliveryStatus status, int attempts, String acknowledgement,
                               String failureMessage, UUID correlationId, Instant createdAt,
                               Instant updatedAt, Instant acknowledgedAt) {
        static PosDeliveryResponse from(PosDelivery delivery) {
            return new PosDeliveryResponse(delivery.getId(), delivery.getEventId(), delivery.getEventType(),
                    delivery.getOrderId(), delivery.getStoreNumber(), delivery.getStatus(), delivery.getAttempts(),
                    delivery.getAcknowledgement(), delivery.getFailureMessage(), delivery.getCorrelationId(),
                    delivery.getCreatedAt(), delivery.getUpdatedAt(), delivery.getAcknowledgedAt());
        }
    }
}
