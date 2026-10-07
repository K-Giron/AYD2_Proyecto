package com.megastore.inventoryservice.finance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findAllByOrderByDueDateAsc();

    List<Invoice> findByTypeOrderByDueDateAsc(InvoiceType type);

    List<Invoice> findByStatusOrderByDueDateAsc(InvoiceStatus status);
}
