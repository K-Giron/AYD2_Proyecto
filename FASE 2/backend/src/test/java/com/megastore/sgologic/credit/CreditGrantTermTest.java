package com.megastore.sgologic.credit;

import com.megastore.sgologic.audit.api.AuditRecorder;
import com.megastore.sgologic.credit.api.CreditOperationResult;
import com.megastore.sgologic.credit.api.CreditOperationStatus;
import com.megastore.sgologic.credit.api.CreditRequest;
import com.megastore.sgologic.integration.outbox.PublishEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** UT-05: la concesión fija el vencimiento a 30 días y respeta el monto permitido. */
class CreditGrantTermTest {

    private static final Instant NOW = Instant.parse("2026-10-14T15:30:00Z");

    private final UUID customer = UUID.randomUUID();
    private CreditCustomerProfile profile;
    private CreditService service;

    @BeforeEach
    void setUp() {
        profile = CreditCustomerProfile.of(customer, 6, new BigDecimal("1.0000"), new BigDecimal("0.0500"),
                0, false, new BigDecimal("1000.00"), NOW);
        CreditCustomerProfileRepository profiles = mock(CreditCustomerProfileRepository.class);
        CreditDecisionRepository decisions = mock(CreditDecisionRepository.class);
        CreditOperationRepository operations = mock(CreditOperationRepository.class);
        when(profiles.findById(customer)).thenReturn(Optional.of(profile));
        CreditDecision[] lastDecision = new CreditDecision[1];
        when(decisions.save(any(CreditDecision.class))).thenAnswer(call -> {
            lastDecision[0] = call.getArgument(0);
            return lastDecision[0];
        });
        when(decisions.findById(any(UUID.class))).thenAnswer(call -> Optional.of(lastDecision[0]));
        when(operations.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(operations.findByOrderId(any())).thenReturn(Optional.empty());
        when(operations.save(any(CreditOperation.class))).thenAnswer(call -> call.getArgument(0));
        CreditProperties properties = new CreditProperties();
        service = new CreditService(profiles, decisions, operations,
                new CreditEligibilityPolicy(properties), properties, mock(CreditConcurrencyGuard.class),
                mock(PublishEvent.class), mock(CreditOperationEvents.class), mock(AuditRecorder.class), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void grantedCreditDueDateIsThirtyDaysAfterTheGrant() {
        CreditOperationResult result = service.requestCredit(request("700.00"));

        assertThat(result.status()).isEqualTo(CreditOperationStatus.GRANTED);
        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2026, 11, 13));
    }

    @Test
    void grantedAmountIsDeductedFromTheAvailableLimit() {
        service.requestCredit(request("700.00"));

        assertThat(profile.getAvailableCredit()).isEqualByComparingTo("300.00");
    }

    @Test
    void amountAboveTheLimitIsRejectedWithoutDueDateNorDebt() {
        CreditOperationResult result = service.requestCredit(request("1000.01"));

        assertThat(result.status()).isEqualTo(CreditOperationStatus.REJECTED);
        assertThat(result.dueDate()).isNull();
        assertThat(result.publicReasonCode()).isEqualTo("INSUFFICIENT_LIMIT");
        assertThat(profile.getAvailableCredit()).isEqualByComparingTo("1000.00");
    }

    private CreditRequest request(String amount) {
        return new CreditRequest(UUID.randomUUID(), customer, new BigDecimal(amount), "GTQ",
                UUID.randomUUID(), "key-" + UUID.randomUUID());
    }
}
