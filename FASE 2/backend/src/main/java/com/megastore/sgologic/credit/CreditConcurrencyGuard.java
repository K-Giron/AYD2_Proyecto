package com.megastore.sgologic.credit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Serializa concesiones equivalentes durante la transacción de crédito. */
@Component
class CreditConcurrencyGuard {

    private final JdbcTemplate jdbc;

    CreditConcurrencyGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    void lock(UUID customerId, UUID orderId, String idempotencyKey) {
        List.of("credit:customer:" + customerId, "credit:idempotency:" + idempotencyKey,
                        "credit:order:" + orderId)
                .stream().sorted().forEach(this::advisoryTransactionLock);
    }

    /** Serializa la reconstrucción del perfil con las concesiones del mismo cliente. */
    void lockCustomer(UUID customerId) {
        advisoryTransactionLock("credit:customer:" + customerId);
    }

    private void advisoryTransactionLock(String lockKey) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                statement -> statement.setString(1, lockKey), resultSet -> null);
    }
}
