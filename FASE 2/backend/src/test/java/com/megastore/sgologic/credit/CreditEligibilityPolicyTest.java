package com.megastore.sgologic.credit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CreditEligibilityPolicyTest {

    private CreditEligibilityPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new CreditEligibilityPolicy(new CreditProperties());
    }

    @Test
    void approvesCustomerWithEnoughGoodHistoryAndLimit() {
        var result = policy.evaluate(profile(5, "1.00", "0.10", 0, false, "1500.00"),
                new BigDecimal("800.00"));

        assertThat(result.eligible()).isTrue();
        assertThat(result.reasonCode()).isEqualTo("ELIGIBLE");
        assertThat(result.approvedLimit()).isEqualByComparingTo("1500.00");
    }

    @Test
    void rejectsUnknownCustomerUsingFailClosedDecision() {
        var result = policy.evaluate(null, new BigDecimal("100.00"));

        assertThat(result.eligible()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("INSUFFICIENT_HISTORY");
    }

    @Test
    void rejectsOverdueCreditBeforeOtherRules() {
        var result = policy.evaluate(profile(10, "1.00", "0.00", 0, true, "5000.00"),
                new BigDecimal("100.00"));

        assertThat(result.eligible()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("OVERDUE_CREDIT");
    }

    @Test
    void rejectsHighReturnRate() {
        var result = policy.evaluate(profile(10, "1.00", "0.31", 0, false, "5000.00"),
                new BigDecimal("100.00"));

        assertThat(result.reasonCode()).isEqualTo("HIGH_RETURN_RATE");
    }

    @Test
    void rejectsRepeatedCriticalIncidents() {
        var result = policy.evaluate(profile(10, "1.00", "0.00", 2, false, "5000.00"),
                new BigDecimal("100.00"));

        assertThat(result.reasonCode()).isEqualTo("CRITICAL_INCIDENTS");
    }

    @Test
    void rejectsPurchaseAboveAvailableLimit() {
        var result = policy.evaluate(profile(10, "1.00", "0.00", 0, false, "400.00"),
                new BigDecimal("400.01"));

        assertThat(result.reasonCode()).isEqualTo("INSUFFICIENT_LIMIT");
        assertThat(result.approvedLimit()).isEqualByComparingTo("400.00");
    }

    @Test
    void capsExposedLimitAtConfiguredMaximum() {
        var result = policy.evaluate(profile(10, "1.00", "0.00", 0, false, "9000.00"),
                new BigDecimal("100.00"));

        assertThat(result.approvedLimit()).isEqualByComparingTo("5000.00");
    }

    private CreditCustomerProfile profile(int purchases, String onTimeRate, String returnRate,
                                          int incidents, boolean overdue, String availableCredit) {
        return CreditCustomerProfile.of(UUID.randomUUID(), purchases, new BigDecimal(onTimeRate),
                new BigDecimal(returnRate), incidents, overdue, new BigDecimal(availableCredit), Instant.now());
    }
}
