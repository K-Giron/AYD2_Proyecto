package com.megastore.inventoryservice.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface StockRepository extends JpaRepository<Stock, UUID> {

    Optional<Stock> findByArticleId(UUID articleId);

    List<Stock> findAllByOrderByArticleId();

    /** Entrada: suma unidades a la existencia. Requiere que la fila ya exista. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Stock s SET s.quantityOnHand = s.quantityOnHand + :quantity, "
            + "s.updatedAt = :now, s.version = s.version + 1 WHERE s.articleId = :articleId")
    int increase(@Param("articleId") UUID articleId, @Param("quantity") int quantity, @Param("now") Instant now);

    /**
     * Salida: descuenta unidades en una sola sentencia {@code UPDATE ... WHERE}
     * para que nunca quede existencia negativa, ni siquiera ante dos salidas
     * concurrentes por el mismo artículo (mismo patrón ya probado en el
     * módulo {@code inventory} del monolito).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Stock s SET s.quantityOnHand = s.quantityOnHand - :quantity, "
            + "s.updatedAt = :now, s.version = s.version + 1 "
            + "WHERE s.articleId = :articleId AND s.quantityOnHand >= :quantity")
    int decrease(@Param("articleId") UUID articleId, @Param("quantity") int quantity, @Param("now") Instant now);
}
