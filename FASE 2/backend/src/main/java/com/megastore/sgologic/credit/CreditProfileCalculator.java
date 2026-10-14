package com.megastore.sgologic.credit;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reglas que convierten el historial de la ventana en el perfil que evalúa
 * {@link CreditEligibilityPolicy}. No toca la base de datos.
 */
@Component
class CreditProfileCalculator {

    private final CreditProperties properties;

    CreditProfileCalculator(CreditProperties properties) {
        this.properties = properties;
    }

    ProfileValues calculate(HistoryAggregates history) {
        BigDecimal onTimeRate = history.payments() == 0
                ? BigDecimal.ONE.setScale(4)
                : ratio(history.onTimePayments(), history.payments());
        BigDecimal returnRate = history.purchases() == 0
                ? BigDecimal.ZERO.setScale(4)
                : ratio(history.returns(), history.purchases()).min(BigDecimal.ONE.setScale(4));
        BigDecimal limit = history.purchaseTotal().multiply(properties.getLimitRatio())
                .min(properties.getMaximumLimit()).setScale(2, RoundingMode.DOWN);
        BigDecimal available = limit.subtract(history.outstandingCredit()).max(BigDecimal.ZERO);
        return new ProfileValues(history.purchases(), onTimeRate, returnRate,
                history.criticalIncidents(), available);
    }

    private static BigDecimal ratio(int numerator, int denominator) {
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    record HistoryAggregates(int purchases, BigDecimal purchaseTotal, int payments, int onTimePayments,
                             int returns, int criticalIncidents, BigDecimal outstandingCredit) {
    }

    record ProfileValues(int completedPurchases, BigDecimal onTimePaymentRate, BigDecimal returnRate,
                         int criticalIncidents, BigDecimal availableCredit) {
    }
}
