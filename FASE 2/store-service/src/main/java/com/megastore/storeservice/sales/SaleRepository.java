package com.megastore.storeservice.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    List<Sale> findBySyncStatusOrderByCreatedAtAsc(SaleSyncStatus syncStatus);

    List<Sale> findTop200ByOrderByCreatedAtDesc();

    List<Sale> findTop1000ByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(Instant from, Instant to);

    long countBySyncStatus(SaleSyncStatus syncStatus);

    @Query(value = "select nextval('seq_ticket')", nativeQuery = true)
    long nextTicketSequence();
}
