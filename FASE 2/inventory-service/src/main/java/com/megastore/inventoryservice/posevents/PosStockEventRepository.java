package com.megastore.inventoryservice.posevents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface PosStockEventRepository extends JpaRepository<PosStockEvent, UUID> {

    List<PosStockEvent> findTop100ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            Collection<String> statuses, Instant now);

    List<PosStockEvent> findTop300ByOrderByCreatedAtDesc();

    List<PosStockEvent> findTop300ByPosCodeOrderByCreatedAtDesc(String posCode);
}
