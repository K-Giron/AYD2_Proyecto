package com.megastore.inventoryservice.storesync;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.UUID;

interface StoreFolioSequenceRepository extends Repository<StoreInvoice, UUID> {

    @Query(value = "SELECT nextval('seq_store_invoice')", nativeQuery = true)
    long nextInvoiceSequence();

    @Query(value = "SELECT nextval('seq_store_transfer')", nativeQuery = true)
    long nextTransferSequence();
}
