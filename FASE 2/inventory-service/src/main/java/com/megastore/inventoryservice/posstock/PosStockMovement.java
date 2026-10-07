package com.megastore.inventoryservice.posstock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Movimiento de la existencia propia de un punto de venta: una ENTRADA
 * generada automáticamente por el folio de salida del almacén central
 * (correlacionada con ese folio), o una VENTA en la tienda física, que
 * descuenta esa existencia.
 */
@Entity
@Table(name = "pos_stock_movements")
public class PosStockMovement {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "folio", nullable = false, updatable = false, unique = true, length = 24)
    private String folio;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, updatable = false, length = 16)
    private PosStockMovementType type;

    @Column(name = "point_of_sale_id", nullable = false, updatable = false)
    private UUID pointOfSaleId;

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    /** Solo en ENTRADA: folio de salida del almacén que la originó. */
    @Column(name = "origin_warehouse_folio", updatable = false, length = 16)
    private String originWarehouseFolio;

    @Column(name = "external_reference", unique = true, updatable = false, length = 160)
    private String externalReference;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected PosStockMovement() {
        // Requerido por JPA.
    }

    static PosStockMovement register(String folio, UUID pointOfSaleId, UUID articleId, int quantity,
                                      String originWarehouseFolio, Instant now) {
        return create(folio, PosStockMovementType.ENTRADA, pointOfSaleId, articleId, quantity,
                originWarehouseFolio, null, now);
    }

    static PosStockMovement sale(String folio, UUID pointOfSaleId, UUID articleId, int quantity,
                                  String externalReference, Instant now) {
        return create(folio, PosStockMovementType.VENTA, pointOfSaleId, articleId, quantity,
                null, externalReference, now);
    }

    static PosStockMovement transferOut(String transferFolio, UUID pointOfSaleId, UUID articleId, int quantity,
                                         String externalReference, Instant now) {
        return create("OUT-" + transferFolio, PosStockMovementType.TRASLADO, pointOfSaleId, articleId, quantity,
                transferFolio, externalReference, now);
    }

    private static PosStockMovement create(String folio, PosStockMovementType type, UUID pointOfSaleId,
                                            UUID articleId, int quantity, String originWarehouseFolio,
                                            String externalReference, Instant now) {
        PosStockMovement movement = new PosStockMovement();
        movement.id = UUID.randomUUID();
        movement.folio = folio;
        movement.type = type;
        movement.pointOfSaleId = pointOfSaleId;
        movement.articleId = articleId;
        movement.quantity = quantity;
        movement.originWarehouseFolio = originWarehouseFolio;
        movement.externalReference = externalReference;
        movement.occurredAt = now;
        return movement;
    }

    public UUID getId() { return id; }
    public String getFolio() { return folio; }
    public PosStockMovementType getType() { return type; }
    public UUID getPointOfSaleId() { return pointOfSaleId; }
    public UUID getArticleId() { return articleId; }
    public int getQuantity() { return quantity; }
    public String getOriginWarehouseFolio() { return originWarehouseFolio; }
    public String getExternalReference() { return externalReference; }
    public Instant getOccurredAt() { return occurredAt; }
}
