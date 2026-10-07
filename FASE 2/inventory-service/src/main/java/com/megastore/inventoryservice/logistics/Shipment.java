package com.megastore.inventoryservice.logistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Seguimiento de un envío: se crea automáticamente a partir de cada folio
 * de salida del almacén (ver {@code StockService.registerMovement}) y
 * avanza de estado hasta la entrega.
 */
@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "folio", nullable = false, updatable = false, unique = true, length = 16)
    private String folio;

    @Column(name = "movement_id", nullable = false, updatable = false)
    private UUID movementId;

    @Column(name = "point_of_sale_id", nullable = false, updatable = false)
    private UUID pointOfSaleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ShipmentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Shipment() {
        // Requerido por JPA.
    }

    static Shipment createForFolio(String folio, UUID movementId, UUID pointOfSaleId, Instant now) {
        Shipment shipment = new Shipment();
        shipment.id = UUID.randomUUID();
        shipment.folio = folio;
        shipment.movementId = movementId;
        shipment.pointOfSaleId = pointOfSaleId;
        shipment.status = ShipmentStatus.PENDIENTE;
        shipment.createdAt = now;
        shipment.updatedAt = now;
        return shipment;
    }

    void advanceTo(ShipmentStatus next, Instant now) {
        if (!isForwardTransition(status, next)) {
            throw new InvalidShipmentTransitionException(folio, status, next);
        }
        status = next;
        updatedAt = now;
    }

    private static boolean isForwardTransition(ShipmentStatus current, ShipmentStatus next) {
        return next.ordinal() == current.ordinal() + 1;
    }

    public UUID getId() { return id; }
    public String getFolio() { return folio; }
    public UUID getMovementId() { return movementId; }
    public UUID getPointOfSaleId() { return pointOfSaleId; }
    public ShipmentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
