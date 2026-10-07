package com.megastore.sgologic.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ErpSaleRepository extends JpaRepository<ErpSale, UUID> {
    Optional<ErpSale> findByOrderId(UUID orderId);
    List<ErpSale> findByStatusOrderByConfirmedAtAsc(ErpSaleStatus status);
    List<ErpSale> findByBatchId(UUID batchId);

    List<ErpSale> findByConfirmedAtGreaterThanEqualAndConfirmedAtLessThanOrderByConfirmedAtAsc(
            java.time.Instant from, java.time.Instant to);
}
