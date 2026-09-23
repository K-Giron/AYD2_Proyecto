package com.megastore.sgologic.inventory;

import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

import java.util.List;
import java.util.UUID;

/**
 * Expone INV-01 (ubicaciones y disponibilidad) e INV-02 (reserva atómica e
 * idempotente, CDU-02). Endpoints bajo {@code /api/v1}, JSON
 * {@code camelCase}, conforme a {@code api-conventions.md}.
 */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryQueryService queryService;
    private final InventoryReservationService reservationService;

    public InventoryController(InventoryQueryService queryService,
                                InventoryReservationService reservationService) {
        this.queryService = queryService;
        this.reservationService = reservationService;
    }

    @GetMapping("/locations")
    public List<InventoryLocationResponse> getLocations() {
        return queryService.getLocations().stream()
                .map(InventoryLocationResponse::from)
                .toList();
    }

    /**
     * Sin {@code locationId}: disponibilidad del producto en todas las
     * ubicaciones donde tiene stock registrado. Con {@code locationId}:
     * disponibilidad en esa ubicación específica.
     */
    @GetMapping("/stock")
    public List<InventoryStockResponse> getStock(
            @RequestParam UUID productId,
            @RequestParam(required = false) UUID locationId) {
        if (locationId != null) {
            return List.of(queryService.getStockByProductAndLocation(productId, locationId));
        }
        return queryService.getStockByProduct(productId);
    }

    /**
     * CDU-02: reserva atómica e idempotente. Puerto que consumirá la Saga
     * (ORD-03). Devuelve {@code 201} en la primera solicitud y también en
     * una repetición idempotente (la reserva ya existía, no se creó una
     * nueva ni se comprometió stock de más). Con {@code locationId}:
     * reserva exactamente ahí. Sin {@code locationId} (el caso del
     * contrato de la Saga, que no lo incluye): aplica el algoritmo de
     * selección de origen de INV-04/ADR-014.
     */
    @PostMapping("/reservations")
    public ResponseEntity<InventoryReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            HttpServletRequest httpRequest) {
        UUID correlationId = correlationId(httpRequest);
        InventoryReservation reservation = request.locationId() != null
                ? reservationService.reserve(
                        request.orderId(), request.productId(), request.locationId(), request.quantity(),
                        correlationId, idempotencyKey)
                : reservationService.reserveAtBestOrigin(
                        request.orderId(), request.productId(), request.quantity(),
                        correlationId, idempotencyKey);
        InventoryLocation location = queryService.getLocationById(reservation.getLocationId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InventoryReservationResponse.from(reservation, location));
    }

    @GetMapping("/reservations/{reservationId}")
    public InventoryReservationResponse getReservation(@PathVariable UUID reservationId) {
        InventoryReservation reservation = reservationService.getReservation(reservationId);
        InventoryLocation location = queryService.getLocationById(reservation.getLocationId());
        return InventoryReservationResponse.from(reservation, location);
    }

    private UUID correlationId(HttpServletRequest request) {
        Object attribute = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        return UUID.fromString(attribute.toString());
    }
}
