package com.megastore.sgologic.credit;

import com.megastore.sgologic.credit.api.CreditOperationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CreditOperationRepository extends JpaRepository<CreditOperation, UUID> {
    Optional<CreditOperation> findByOrderId(UUID orderId);
    Optional<CreditOperation> findByIdempotencyKey(String idempotencyKey);
    List<CreditOperation> findByStatusAndDueDateBefore(CreditOperationStatus status, LocalDate date);
    boolean existsByCustomerIdAndStatus(UUID customerId, CreditOperationStatus status);
}
