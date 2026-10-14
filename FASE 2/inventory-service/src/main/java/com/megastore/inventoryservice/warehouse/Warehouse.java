package com.megastore.inventoryservice.warehouse;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Almacén físico del que depende uno o más puntos de venta. */
@Entity
@Table(name = "warehouses")
public class Warehouse {

    /** Código del único almacén de origen de artículos/existencias/movimientos (sembrado por V5). */
    public static final String CENTRAL_CODE = "ALM-CENTRAL";

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, updatable = false, unique = true, length = 32)
    private String code;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    protected Warehouse() {
        // Requerido por JPA.
    }

    /** Usado por PointOfSaleService: cada punto de venta crea su propio almacén al darse de alta. */
    public static Warehouse create(String code, String name) {
        Warehouse warehouse = new Warehouse();
        warehouse.id = UUID.randomUUID();
        warehouse.code = code;
        warehouse.name = name;
        return warehouse;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}
