package com.megastore.sgologic.payments;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

    Optional<PaymentAttempt> findByIdempotencyKey(String idempotencyKey);

    List<PaymentAttempt> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    /**
     * Intentos que quedaron en PENDING más allá del umbral (caída del
     * proceso o excepción entre la creación y el resultado). El barrido de
     * conciliación los recupera como indeterminados.
     */
    List<PaymentAttempt> findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            PaymentAttemptStatus status, Instant threshold, Pageable pageable);
}
