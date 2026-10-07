package com.megastore.inventoryservice.posstock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PosStockRepository extends JpaRepository<PosStock, UUID> {

    Optional<PosStock> findByPointOfSaleIdAndArticleId(UUID pointOfSaleId, UUID articleId);

    List<PosStock> findByPointOfSaleIdOrderByArticleId(UUID pointOfSaleId);

    List<PosStock> findAllByOrderByPointOfSaleId();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PosStock s SET s.quantityOnHand = s.quantityOnHand + :quantity, "
            + "s.updatedAt = :now, s.version = s.version + 1 "
            + "WHERE s.pointOfSaleId = :pointOfSaleId AND s.articleId = :articleId")
    int increase(@Param("pointOfSaleId") UUID pointOfSaleId, @Param("articleId") UUID articleId,
                 @Param("quantity") int quantity, @Param("now") Instant now);

    /** Venta en tienda: una sola sentencia atómica, nunca deja existencia negativa. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PosStock s SET s.quantityOnHand = s.quantityOnHand - :quantity, "
            + "s.updatedAt = :now, s.version = s.version + 1 "
            + "WHERE s.pointOfSaleId = :pointOfSaleId AND s.articleId = :articleId "
            + "AND s.quantityOnHand >= :quantity")
    int decrease(@Param("pointOfSaleId") UUID pointOfSaleId, @Param("articleId") UUID articleId,
                 @Param("quantity") int quantity, @Param("now") Instant now);
}
