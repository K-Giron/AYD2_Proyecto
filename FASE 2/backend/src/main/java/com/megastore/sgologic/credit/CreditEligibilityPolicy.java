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
        BigDecimal limit = profile == null ? BigDecimal.ZERO
                : profile.getAvailableCredit().min(properties.getMaximumLimit());
        if (profile == null) return new PolicyResult(false, limit, "INSUFFICIENT_HISTORY");
        if (profile.hasOverdueCredit()) return new PolicyResult(false, limit, "OVERDUE_CREDIT");
        if (profile.getCompletedPurchases() < properties.getMinimumCompletedPurchases()) {
            return new PolicyResult(false, limit, "INSUFFICIENT_HISTORY");
        }
        if (profile.getOnTimePaymentRate().compareTo(properties.getMinimumOnTimePaymentRate()) < 0) {
            return new PolicyResult(false, limit, "LATE_PAYMENT_HISTORY");
        }
        if (profile.getReturnRate().compareTo(properties.getMaximumReturnRate()) > 0) {
            return new PolicyResult(false, limit, "HIGH_RETURN_RATE");
        }
        if (profile.getCriticalIncidents() > properties.getMaximumCriticalIncidents()) {
            return new PolicyResult(false, limit, "CRITICAL_INCIDENTS");
        }
        if (amount.compareTo(limit) > 0) return new PolicyResult(false, limit, "INSUFFICIENT_LIMIT");
        return new PolicyResult(true, limit, "ELIGIBLE");
    }

    String ruleVersion() { return properties.getRuleVersion(); }
    int termDays() { return properties.getTermDays(); }

    record PolicyResult(boolean eligible, BigDecimal approvedLimit, String reasonCode) {
    }
}
