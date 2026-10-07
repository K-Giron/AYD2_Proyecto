package com.megastore.inventoryservice.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entrada o salida de inventario en el almacén central. El {@code folio}
 * es la referencia humana del movimiento (ENT-000001/SAL-000001); es la
 * única forma en que la existencia de un artículo cambia.
 */
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "folio", nullable = false, updatable = false, unique = true, length = 16)
    private String folio;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, updatable = false, length = 16)
    private StockMovementType type;

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    /** Almacén de origen donde ocurrió el movimiento (siempre el central). */
    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private UUID warehouseId;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    @Column(name = "reason", length = 255)
    private String reason;

    /** Punto de venta destino; solo aplica a salidas (ver {@code StockService}). */
    @Column(name = "point_of_sale_id")
    private UUID pointOfSaleId;

    /**
     * Referencia opcional de quien origina el movimiento desde afuera (ej.
     * una integración), usada para no duplicar el efecto si repite la
     * misma llamada por un reintento de red — ver
     * {@code StockService#registerMovement}.
     */
    @Column(name = "external_reference", unique = true, length = 160)
    private String externalReference;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected StockMovement() {
        // Requerido por JPA.
    }

    static StockMovement register(String folio, StockMovementType type, UUID articleId, UUID warehouseId,
                                   int quantity, String reason, UUID pointOfSaleId, String externalReference,
                                   Instant now) {
        StockMovement movement = new StockMovement();
        movement.id = UUID.randomUUID();
        movement.folio = folio;
        movement.type = type;
        movement.articleId = articleId;
        movement.warehouseId = warehouseId;
        movement.quantity = quantity;
        movement.reason = reason;
        movement.pointOfSaleId = pointOfSaleId;
        movement.externalReference = externalReference;
        movement.occurredAt = now;
        return movement;
    }

    public UUID getId() {
        return id;
    }

    public String getFolio() {
        return folio;
    }

    public StockMovementType getType() {
        return type;
    }

    public UUID getArticleId() {
        return articleId;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getReason() {
        return reason;
    }

    public UUID getPointOfSaleId() {
        return pointOfSaleId;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
