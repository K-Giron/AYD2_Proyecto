package com.megastore.inventoryservice.pointofsale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Punto de venta (tienda): tiene su propio identificador y un almacén
 * asignado del que recibe mercadería. Es el destino de cada folio de
 * salida y de su envío correspondiente.
 */
@Entity
@Table(name = "points_of_sale")
public class PointOfSale {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, updatable = false, unique = true, length = 32)
    private String code;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private UUID warehouseId;

    protected PointOfSale() {
        // Requerido por JPA.
    }

    static PointOfSale create(String code, String name, UUID warehouseId) {
        PointOfSale pointOfSale = new PointOfSale();
        pointOfSale.id = UUID.randomUUID();
        pointOfSale.code = code;
        pointOfSale.name = name;
        pointOfSale.warehouseId = warehouseId;
        return pointOfSale;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public UUID getWarehouseId() { return warehouseId; }
}
