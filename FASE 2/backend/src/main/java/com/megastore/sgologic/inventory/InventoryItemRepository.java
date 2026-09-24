package com.megastore.sgologic.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    List<InventoryItem> findByProductIdOrderByLocationId(UUID productId);

    Optional<InventoryItem> findByProductIdAndLocationId(UUID productId, UUID locationId);

    /**
     * CDU-02: intenta comprometer {@code quantity} unidades en una sola
     * sentencia {@code UPDATE ... WHERE disponible >= quantity}, sin leer
     * primero y escribir después. PostgreSQL serializa las escrituras
     * concurrentes sobre la misma fila (bloqueo de fila en READ COMMITTED):
     * si dos solicitudes compiten por la última unidad, la segunda vuelve a
     * evaluar la condición ya con el cambio de la primera confirmado y
     * afecta 0 filas. Devuelve el número de filas afectadas (0 o 1); nunca
     * deja disponible negativo, reforzado además por el CHECK de la
     * migración V1.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE InventoryItem i SET i.quantityReserved = i.quantityReserved + :quantity, "
            + "i.updatedAt = :now, i.version = i.version + 1 "
            + "WHERE i.productId = :productId AND i.locationId = :locationId "
            + "AND (i.quantityOnHand - i.quantityReserved) >= :quantity")
    int tryReserve(@Param("productId") UUID productId, @Param("locationId") UUID locationId,
                   @Param("quantity") int quantity, @Param("now") Instant now);

    /**
     * Libera {@code quantity} unidades previamente reservadas (INV-03,
     * ORD-04). Atómica por la misma razón que {@link #tryReserve}; el CHECK
     * de la migración V1 impide que quede reservado negativo si se invoca
     * más de una vez sobre la misma reserva.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE InventoryItem i SET i.quantityReserved = i.quantityReserved - :quantity, "
            + "i.updatedAt = :now, i.version = i.version + 1 "
            + "WHERE i.productId = :productId AND i.locationId = :locationId "
            + "AND i.quantityReserved >= :quantity")
    int tryRelease(@Param("productId") UUID productId, @Param("locationId") UUID locationId,
                   @Param("quantity") int quantity, @Param("now") Instant now);

    /** Materializa la venta: descuenta existencia y compromiso en una sola escritura. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE InventoryItem i SET i.quantityOnHand = i.quantityOnHand - :quantity, "
            + "i.quantityReserved = i.quantityReserved - :quantity, "
            + "i.updatedAt = :now, i.version = i.version + 1 "
            + "WHERE i.productId = :productId AND i.locationId = :locationId "
            + "AND i.quantityOnHand >= :quantity AND i.quantityReserved >= :quantity")
    int tryConfirm(@Param("productId") UUID productId, @Param("locationId") UUID locationId,
                   @Param("quantity") int quantity, @Param("now") Instant now);

    /**
     * Entrada de mercadería (recepción, reabastecimiento de tienda/POS):
     * suma unidades a la existencia sin afectar lo reservado. Requiere que
     * ya exista una fila para ese producto/ubicación (INV-01/V2 la crea).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE InventoryItem i SET i.quantityOnHand = i.quantityOnHand + :quantity, "
            + "i.updatedAt = :now, i.version = i.version + 1 "
            + "WHERE i.productId = :productId AND i.locationId = :locationId")
    int tryIncreaseOnHand(@Param("productId") UUID productId, @Param("locationId") UUID locationId,
                          @Param("quantity") int quantity, @Param("now") Instant now);

    /**
     * Salida de mercadería fuera del flujo de venta en línea (merma, ajuste
     * de tienda/POS): descuenta existencia sin tocar lo reservado. Atómica
     * igual que {@link #tryReserve}; solo afecta unidades disponibles
     * (no reservadas), reforzado por el mismo CHECK de la migración V1.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE InventoryItem i SET i.quantityOnHand = i.quantityOnHand - :quantity, "
            + "i.updatedAt = :now, i.version = i.version + 1 "
            + "WHERE i.productId = :productId AND i.locationId = :locationId "
            + "AND (i.quantityOnHand - i.quantityReserved) >= :quantity")
    int tryDecreaseOnHand(@Param("productId") UUID productId, @Param("locationId") UUID locationId,
                          @Param("quantity") int quantity, @Param("now") Instant now);
}
