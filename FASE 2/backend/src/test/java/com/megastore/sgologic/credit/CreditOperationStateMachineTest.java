package com.megastore.sgologic.credit;

import com.megastore.sgologic.credit.api.CreditOperationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreditOperationStateMachineTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");

    @Test
    void grantedOperationCanBecomeOverdueThenSettled() {
        CreditOperation operation = operation(true);

        operation.markOverdue(NOW);
        assertThat(operation.getStatus()).isEqualTo(CreditOperationStatus.OVERDUE);

        operation.settle(NOW.plusSeconds(60));
        assertThat(operation.getStatus()).isEqualTo(CreditOperationStatus.SETTLED);
    }

    @Test
    void onlyAGrantedOperationCanBeCancelled() {
        CreditOperation overdue = operation(true);
        overdue.markOverdue(NOW);
        CreditOperation settled = operation(true);
        settled.settle(NOW);

        assertThatThrownBy(() -> overdue.cancel(NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> settled.cancel(NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectedOperationIsTerminalFromTheStart() {
        CreditOperation rejected = operation(false);

        assertThat(rejected.getDueDate()).isNull();
        assertThatThrownBy(() -> rejected.settle(NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> rejected.markOverdue(NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> rejected.cancel(NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void settledOrCancelledOperationsCannotMoveAgain() {
        CreditOperation settled = operation(true);
        settled.settle(NOW);
        CreditOperation cancelled = operation(true);
        cancelled.cancel(NOW);

        assertThatThrownBy(() -> settled.markOverdue(NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cancelled.settle(NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cancelled.markOverdue(NOW)).isInstanceOf(IllegalStateException.class);
    }

    private static CreditOperation operation(boolean eligible) {
        CreditDecision decision = CreditDecision.create(UUID.randomUUID(), new BigDecimal("100.00"), "GTQ",
                eligible, new BigDecimal("500.00"), "credit-rules-v1", eligible ? "ELIGIBLE" : "OVERDUE_CREDIT",
                "{}", UUID.randomUUID(), NOW);
        return CreditOperation.create(UUID.randomUUID(), UUID.randomUUID(), decision, new BigDecimal("100.00"),
                "GTQ", LocalDate.of(2026, 11, 13), "key-" + UUID.randomUUID(), UUID.randomUUID(), NOW);
    }
}
