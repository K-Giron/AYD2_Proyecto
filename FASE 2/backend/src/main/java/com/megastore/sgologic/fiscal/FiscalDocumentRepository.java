package com.megastore.sgologic.fiscal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface FiscalDocumentRepository extends JpaRepository<FiscalDocument, UUID> {
    Optional<FiscalDocument> findByIdempotencyKey(String idempotencyKey);
    List<FiscalDocument> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
    boolean existsByOrderIdAndStatus(UUID orderId, FiscalStatus status);
}
