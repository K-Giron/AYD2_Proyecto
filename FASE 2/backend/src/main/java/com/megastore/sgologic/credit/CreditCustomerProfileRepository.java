package com.megastore.sgologic.credit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

interface CreditCustomerProfileRepository extends JpaRepository<CreditCustomerProfile, UUID> {

    /** Crea de forma idempotente la línea inicial individual del cliente. */
    @Modifying
    @Query(value = """
            INSERT INTO credit_customer_profiles (
                customer_id, completed_purchases, on_time_payment_rate, return_rate,
                critical_incidents, has_overdue_credit, available_credit, observed_at, version
            ) VALUES (:customerId, 0, 1.0000, 0.0000, 0, FALSE, :initialLimit, :observedAt, 0)
            ON CONFLICT (customer_id) DO NOTHING
            """, nativeQuery = true)
    int initializeIfMissing(@Param("customerId") UUID customerId,
                            @Param("initialLimit") BigDecimal initialLimit,
                            @Param("observedAt") Instant observedAt);
}
