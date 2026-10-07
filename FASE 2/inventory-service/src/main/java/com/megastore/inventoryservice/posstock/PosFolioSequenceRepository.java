package com.megastore.inventoryservice.posstock;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.UUID;

interface PosFolioSequenceRepository extends Repository<PosStockMovement, UUID> {

    @Query(value = "SELECT nextval('seq_pos_folio_entrada')", nativeQuery = true)
    long nextEntradaSequence();

    @Query(value = "SELECT nextval('seq_pos_folio_venta')", nativeQuery = true)
    long nextVentaSequence();
}
