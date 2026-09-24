package com.megastore.sgologic.payments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PaymentReconciliationRepository extends JpaRepository<PaymentReconciliation, UUID> {

    Optional<PaymentReconciliation> findByPaymentAttemptIdAndStatus(UUID paymentAttemptId,
                                                                    PaymentReconciliationStatus status);

    /**
     * Reclama trabajos vencidos con {@code FOR UPDATE SKIP LOCKED}: varios
     * workers pueden convivir sin conciliar dos veces el mismo intento.
     * Debe invocarse dentro de una transacción.
     */
    @Query(value = "SELECT * FROM payment_reconciliations "
            + "WHERE status = 'PENDING' AND scheduled_at <= :now "
            + "ORDER BY scheduled_at ASC LIMIT :batchSize FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<PaymentReconciliation> lockDueBatch(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
