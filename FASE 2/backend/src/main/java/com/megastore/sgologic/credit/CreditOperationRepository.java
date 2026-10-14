package com.megastore.sgologic.credit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface CreditOperationRepository extends JpaRepository<CreditOperation, UUID> {
    Optional<CreditOperation> findByOrderId(UUID orderId);
    Optional<CreditOperation> findByIdempotencyKey(String idempotencyKey);
}
