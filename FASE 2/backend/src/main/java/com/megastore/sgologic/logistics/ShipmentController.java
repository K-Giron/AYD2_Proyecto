package com.megastore.sgologic.logistics;

import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Expone LOG-01 (crear despacho), LOG-02 (tracking) y el flujo de cambio de
 * método de entrega de CDU-07 A1. Endpoints bajo {@code /api/v1}, JSON
 * {@code camelCase}, conforme a {@code api-conventions.md}.
 */
@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    private static final int MAX_CONCURRENT_UPDATE_ATTEMPTS = 3;

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody CreateShipmentRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            HttpServletRequest httpRequest) {
        UUID correlationId = correlationId(httpRequest);
        Shipment shipment = shipmentService.createDispatch(
                request.orderId(), request.origin(), request.originCode(), request.destination(), correlationId,
                idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ShipmentResponse.from(shipment));
    }

    @GetMapping("/{shipmentId}")
    public ShipmentResponse getShipment(@PathVariable UUID shipmentId) {
        return ShipmentResponse.from(shipmentService.getShipment(shipmentId));
    }

    /** Vista por orden, para consolidar despacho junto a las demás áreas (CDU-09). */
    @GetMapping
    public List<ShipmentResponse> getShipmentsForOrder(@RequestParam UUID orderId) {
        return shipmentService.getShipmentsForOrder(orderId).stream()
                .map(ShipmentResponse::from)
                .toList();
    }

    @GetMapping("/{shipmentId}/tracking-events")
    public List<TrackingEventResponse> getTrackingEvents(@PathVariable UUID shipmentId) {
        return shipmentService.getTrackingHistory(shipmentId).stream()
                .map(TrackingEventResponse::from)
                .toList();
    }

    /**
     * Punto de integración simulado de CDU-08 paso 4. Cuando BB-06 entregue
     * el simulador oficial de LogiExpress, este endpoint puede recibirlo
     * como webhook o sustituirse por un consumidor de eventos.
     */
    @PostMapping("/{shipmentId}/tracking-events")
    public ShipmentResponse recordTrackingEvent(
            @PathVariable UUID shipmentId,
            @Valid @RequestBody RecordTrackingEventRequest request) {
        // Tras crear el despacho, otro hilo pide la guía a LogiExpress y actualiza el mismo
        // registro (@Version). Si ambos coinciden, esta transacción pierde el bloqueo optimista:
        // no es un error del cliente, basta reintentar con el estado ya actualizado.
        for (int attempt = 1; ; attempt++) {
            try {
                return ShipmentResponse.from(
                        shipmentService.recordTrackingEvent(shipmentId, request.status(), "CONSOLA"));
            } catch (OptimisticLockingFailureException concurrentUpdate) {
                if (attempt >= MAX_CONCURRENT_UPDATE_ATTEMPTS) throw concurrentUpdate;
            }
        }
    }

    /** LOG-03: vuelve a pedir la guía a LogiExpress tras agotar los reintentos automáticos. */
    @PostMapping("/{shipmentId}/label-retry")
    public ShipmentResponse retryLabel(@PathVariable UUID shipmentId) {
        return ShipmentResponse.from(shipmentService.retryLabel(shipmentId));
    }

    @PatchMapping("/{shipmentId}/delivery-method")
    public ShipmentResponse changeDeliveryMethod(
            @PathVariable UUID shipmentId,
            @Valid @RequestBody ChangeDeliveryMethodRequest request) {
        return ShipmentResponse.from(
                shipmentService.changeDeliveryMethod(shipmentId, request.origin(), request.destination()));
    }

    private UUID correlationId(HttpServletRequest request) {
        Object attribute = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        return UUID.fromString(attribute.toString());
    }
}
