package com.megastore.sgologic.credit;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class CreditEligibilityPolicy {

    private final CreditProperties properties;

    CreditEligibilityPolicy(CreditProperties properties) {
        this.properties = properties;
    }

    PolicyResult evaluate(CreditCustomerProfile profile, BigDecimal amount) {
        Outcome outcome = decide(profile, amount);
        return new PolicyResult(outcome.eligible(), outcome.approvedLimit(), outcome.reasonCode(),
                inputsSnapshot(profile, amount));
    }

    private Outcome decide(CreditCustomerProfile profile, BigDecimal amount) {
        BigDecimal limit = profile == null ? BigDecimal.ZERO
                : profile.getAvailableCredit().min(properties.getMaximumLimit());
        if (profile == null) return new Outcome(false, limit, "INSUFFICIENT_HISTORY");
        if (profile.hasOverdueCredit()) return new Outcome(false, limit, "OVERDUE_CREDIT");
        if (profile.getCompletedPurchases() < properties.getMinimumCompletedPurchases()) {
            return new Outcome(false, limit, "INSUFFICIENT_HISTORY");
        }
        if (profile.getOnTimePaymentRate().compareTo(properties.getMinimumOnTimePaymentRate()) < 0) {
            return new Outcome(false, limit, "LATE_PAYMENT_HISTORY");
        }
        if (profile.getReturnRate().compareTo(properties.getMaximumReturnRate()) > 0) {
            return new Outcome(false, limit, "HIGH_RETURN_RATE");
        }
        if (profile.getCriticalIncidents() > properties.getMaximumCriticalIncidents()) {
            return new Outcome(false, limit, "CRITICAL_INCIDENTS");
        }
        if (amount.compareTo(limit) > 0) return new Outcome(false, limit, "INSUFFICIENT_LIMIT");
        return new Outcome(true, limit, "ELIGIBLE");
    }

    String ruleVersion() { return properties.getRuleVersion(); }
    int termDays() { return properties.getTermDays(); }

    /**
     * Entradas y umbrales con los que se decidió, para reconstruir la decisión
     * sin volver a consultar el perfil (que cambia con el tiempo). Solo contiene
     * cifras de negocio; nunca datos personales ni de pago.
     */
    private String inputsSnapshot(CreditCustomerProfile profile, BigDecimal amount) {
        String profileJson = profile == null ? "null"
                : "{\"completedPurchases\":" + profile.getCompletedPurchases()
                + ",\"onTimePaymentRate\":" + profile.getOnTimePaymentRate().toPlainString()
                + ",\"returnRate\":" + profile.getReturnRate().toPlainString()
                + ",\"criticalIncidents\":" + profile.getCriticalIncidents()
                + ",\"hasOverdueCredit\":" + profile.hasOverdueCredit()
                + ",\"availableCredit\":" + profile.getAvailableCredit().toPlainString()
                + ",\"observedAt\":\"" + profile.getObservedAt() + "\"}";
        return "{\"ruleVersion\":\"" + properties.getRuleVersion().replaceAll("[\\\\\"]", "") + "\""
                + ",\"requestedAmount\":" + amount.toPlainString()
                + ",\"profile\":" + profileJson
                + ",\"thresholds\":{\"minimumCompletedPurchases\":" + properties.getMinimumCompletedPurchases()
                + ",\"minimumOnTimePaymentRate\":" + properties.getMinimumOnTimePaymentRate().toPlainString()
                + ",\"maximumReturnRate\":" + properties.getMaximumReturnRate().toPlainString()
                + ",\"maximumCriticalIncidents\":" + properties.getMaximumCriticalIncidents()
                + ",\"maximumLimit\":" + properties.getMaximumLimit().toPlainString() + "}}";
    }

    private record Outcome(boolean eligible, BigDecimal approvedLimit, String reasonCode) {
    }

    record PolicyResult(boolean eligible, BigDecimal approvedLimit, String reasonCode, String inputsSnapshot) {
    }
}
