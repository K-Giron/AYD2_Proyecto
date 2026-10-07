package com.megastore.sgologic.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ErpBatchRepository extends JpaRepository<ErpBatch, UUID> {

    java.util.List<ErpBatch> findTop50ByOrderByCreatedAtDesc();

    java.util.List<ErpBatch> findByStatusInAndUpdatedAtBefore(java.util.Collection<ErpBatchStatus> statuses,
                                                             java.time.Instant before);
}
