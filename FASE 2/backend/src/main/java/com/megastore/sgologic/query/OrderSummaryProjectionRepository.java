package com.megastore.sgologic.query;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface OrderSummaryProjectionRepository extends JpaRepository<OrderSummaryProjection, UUID> {

    /**
     * Crea la fila vacía si no existe. Varios consumidores (orden, pago,
     * despacho) pueden llegar a la vez con la primera noticia de una orden:
     * con ON CONFLICT ninguno falla por clave duplicada.
     */
    @Modifying
    @Query(value = "INSERT INTO order_summary_projections (order_id, updated_at) VALUES (:orderId, :now) "
            + "ON CONFLICT (order_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("orderId") UUID orderId, @Param("now") Instant now);

    /** Bloquea la fila mientras se aplica un hecho: dos consumidores no se pisan los campos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from OrderSummaryProjection p where p.orderId = :orderId")
    Optional<OrderSummaryProjection> findForUpdate(@Param("orderId") UUID orderId);
}
