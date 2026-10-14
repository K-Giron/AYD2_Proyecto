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

    @Test
    void ruleBoundariesAreInclusiveAsDocumented() {
        // Exactamente en el umbral: 3 compras, 80 % puntual, 30 % devoluciones, 1 incidente y el límite.
        var atEveryThreshold = policy.evaluate(profile(3, "0.80", "0.30", 1, false, "1000.00"),
                new BigDecimal("1000.00"));

        assertThat(atEveryThreshold.eligible()).isTrue();
        assertThat(atEveryThreshold.reasonCode()).isEqualTo("ELIGIBLE");
    }

    @Test
    void oneStepBeyondEachBoundaryIsRejectedWithItsOwnReason() {
        assertThat(policy.evaluate(profile(2, "1.00", "0.00", 0, false, "1000.00"), BigDecimal.TEN)
                .reasonCode()).isEqualTo("INSUFFICIENT_HISTORY");
        assertThat(policy.evaluate(profile(5, "0.7999", "0.00", 0, false, "1000.00"), BigDecimal.TEN)
                .reasonCode()).isEqualTo("LATE_PAYMENT_HISTORY");
        assertThat(policy.evaluate(profile(5, "1.00", "0.3001", 0, false, "1000.00"), BigDecimal.TEN)
                .reasonCode()).isEqualTo("HIGH_RETURN_RATE");
        assertThat(policy.evaluate(profile(5, "1.00", "0.00", 2, false, "1000.00"), BigDecimal.TEN)
                .reasonCode()).isEqualTo("CRITICAL_INCIDENTS");
        assertThat(policy.evaluate(profile(5, "1.00", "0.00", 0, false, "1000.00"), new BigDecimal("1000.01"))
                .reasonCode()).isEqualTo("INSUFFICIENT_LIMIT");
    }

    @Test
    void decisionKeepsTheInputsAndThresholdsItWasBasedOn() {
        var result = policy.evaluate(profile(5, "0.90", "0.10", 0, false, "1500.00"),
                new BigDecimal("800.00"));

        assertThat(result.inputsSnapshot())
                .contains("\"ruleVersion\":\"credit-rules-v1\"")
                .contains("\"requestedAmount\":800.00")
                .contains("\"completedPurchases\":5")
                .contains("\"onTimePaymentRate\":0.90")
                .contains("\"availableCredit\":1500.00")
                .contains("\"minimumCompletedPurchases\":3")
                .contains("\"maximumLimit\":5000.00");
    }

    @Test
    void snapshotOfAnUnknownCustomerMarksTheMissingProfile() {
        var result = policy.evaluate(null, new BigDecimal("100.00"));

        assertThat(result.inputsSnapshot()).contains("\"profile\":null");
    }

    private CreditCustomerProfile profile(int purchases, String onTimeRate, String returnRate,
                                          int incidents, boolean overdue, String availableCredit) {
        return CreditCustomerProfile.of(UUID.randomUUID(), purchases, new BigDecimal(onTimeRate),
                new BigDecimal(returnRate), incidents, overdue, new BigDecimal(availableCredit), Instant.now());
    }
}
