package com.megastore.sgologic.onlinestore;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface CatalogImageRepository extends JpaRepository<CatalogImage, UUID> {

    /** Qué productos tienen imagen y cuándo cambió (para la URL con caché), sin cargar los bytes. */
    @Query("select i.productId as productId, i.updatedAt as updatedAt from CatalogImage i")
    List<ImageStamp> findStamps();

    interface ImageStamp {
        UUID getProductId();
        Instant getUpdatedAt();
    }
}
