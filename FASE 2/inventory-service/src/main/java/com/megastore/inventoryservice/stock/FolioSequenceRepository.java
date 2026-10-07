package com.megastore.inventoryservice.stock;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.UUID;

/** Backing de {@link FolioGenerator}: una secuencia Postgres por tipo de movimiento. */
interface FolioSequenceRepository extends Repository<StockMovement, UUID> {

    @Query(value = "SELECT nextval('seq_folio_entrada')", nativeQuery = true)
    long nextEntradaSequence();

    @Query(value = "SELECT nextval('seq_folio_salida')", nativeQuery = true)
    long nextSalidaSequence();

    @Query(value = "SELECT nextval('seq_folio_ajuste')", nativeQuery = true)
    long nextAjusteSequence();
}
