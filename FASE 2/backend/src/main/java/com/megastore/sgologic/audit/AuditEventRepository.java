package com.megastore.sgologic.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByOrderIdOrderByOccurredAtAsc(UUID orderId);
    List<AuditEvent> findByTransactionIdOrderByOccurredAtAsc(UUID transactionId);
}
