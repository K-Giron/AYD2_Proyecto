package com.megastore.sgologic.credit;

import com.megastore.sgologic.credit.CreditProfileCalculator.HistoryAggregates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CreditProfileCalculatorTest {

    private CreditProfileCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CreditProfileCalculator(new CreditProperties());
    }

    @Test
    void limitIsFortyPercentOfWindowPurchases() {
        var values = calculator.calculate(history(4, "3000.00", 0, 0, 0, 0, "0.00"));

        assertThat(values.availableCredit()).isEqualByComparingTo("1200.00");
        assertThat(values.completedPurchases()).isEqualTo(4);
    }

    @Test
    void limitIsCappedAtTheConfiguredMaximum() {
        var values = calculator.calculate(history(10, "50000.00", 0, 0, 0, 0, "0.00"));

        assertThat(values.availableCredit()).isEqualByComparingTo("5000.00");
    }

    @Test
    void outstandingCreditReducesTheAvailableAmountWithoutGoingNegative() {
        var partial = calculator.calculate(history(4, "3000.00", 0, 0, 0, 0, "500.00"));
        var exhausted = calculator.calculate(history(4, "3000.00", 0, 0, 0, 0, "2000.00"));

        assertThat(partial.availableCredit()).isEqualByComparingTo("700.00");
        assertThat(exhausted.availableCredit()).isEqualByComparingTo("0.00");
    }

    @Test
    void ratesAreComputedFromPaymentsAndReturnsInTheWindow() {
        var values = calculator.calculate(history(5, "1000.00", 4, 3, 1, 2, "0.00"));

        assertThat(values.onTimePaymentRate()).isEqualByComparingTo("0.7500");
        assertThat(values.returnRate()).isEqualByComparingTo("0.2000");
        assertThat(values.criticalIncidents()).isEqualTo(2);
    }

    @Test
    void customerWithoutPaymentsOrPurchasesGetsNeutralRatesAndNoLimit() {
        var values = calculator.calculate(history(0, "0.00", 0, 0, 0, 0, "0.00"));

        assertThat(values.onTimePaymentRate()).isEqualByComparingTo("1.0000");
        assertThat(values.returnRate()).isEqualByComparingTo("0.0000");
        assertThat(values.availableCredit()).isEqualByComparingTo("0.00");
    }

    @Test
    void returnRateNeverExceedsOne() {
        var values = calculator.calculate(history(1, "100.00", 0, 0, 3, 0, "0.00"));

        assertThat(values.returnRate()).isEqualByComparingTo("1.0000");
    }

    private static HistoryAggregates history(int purchases, String total, int payments, int onTime,
                                             int returns, int critical, String outstanding) {
        return new HistoryAggregates(purchases, new BigDecimal(total), payments, onTime, returns,
                critical, new BigDecimal(outstanding));
    }
}
