package com.megastore.inventoryservice.auditlog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {

    List<AuditLogEntry> findAllByOrderByOccurredAtDesc();

    List<AuditLogEntry> findByRelatedFolioOrderByOccurredAtDesc(String relatedFolio);

    List<AuditLogEntry> findByPointOfSaleIdOrderByOccurredAtDesc(UUID pointOfSaleId);
}
