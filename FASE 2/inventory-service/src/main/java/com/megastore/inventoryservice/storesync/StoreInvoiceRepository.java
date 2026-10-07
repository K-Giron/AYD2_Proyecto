package com.megastore.inventoryservice.storesync;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface StoreInvoiceRepository extends JpaRepository<StoreInvoice, UUID> {

    Optional<StoreInvoice> findByPointOfSaleCodeAndTicketFolio(String pointOfSaleCode, String ticketFolio);

    List<StoreInvoice> findAllByOrderByIssuedAtDesc();
}
