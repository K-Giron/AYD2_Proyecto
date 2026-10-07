package com.megastore.storeservice.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface StoreStockRepository extends JpaRepository<StoreStock, UUID> {

    /** Descuento atómico: 0 filas = no alcanza la existencia (nunca queda negativa). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update StoreStock s set s.quantity = s.quantity - :quantity, s.updatedAt = :now "
            + "where s.articleId = :articleId and s.quantity >= :quantity")
    int decrease(@Param("articleId") UUID articleId, @Param("quantity") int quantity, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update StoreStock s set s.quantity = s.quantity + :quantity, s.updatedAt = :now "
            + "where s.articleId = :articleId")
    int increase(@Param("articleId") UUID articleId, @Param("quantity") int quantity, @Param("now") Instant now);
}
