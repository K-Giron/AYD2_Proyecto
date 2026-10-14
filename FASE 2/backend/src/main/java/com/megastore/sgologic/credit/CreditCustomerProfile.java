package com.megastore.sgologic.credit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Proyección local del historial importado desde ERP; no consulta ERP en checkout. */
@Entity
@Table(name = "credit_customer_profiles")
class CreditCustomerProfile {

    @Id
    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "completed_purchases", nullable = false)
    private int completedPurchases;

    @Column(name = "on_time_payment_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal onTimePaymentRate;

    @Column(name = "return_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal returnRate;

    @Column(name = "critical_incidents", nullable = false)
    private int criticalIncidents;

    @Column(name = "has_overdue_credit", nullable = false)
    private boolean hasOverdueCredit;

    @Column(name = "available_credit", nullable = false, precision = 14, scale = 2)
    private BigDecimal availableCredit;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected CreditCustomerProfile() {
    }

    static CreditCustomerProfile of(UUID customerId, int completedPurchases,
                                    BigDecimal onTimePaymentRate, BigDecimal returnRate,
                                    int criticalIncidents, boolean hasOverdueCredit,
                                    BigDecimal availableCredit, Instant observedAt) {
        CreditCustomerProfile profile = new CreditCustomerProfile();
        profile.customerId = customerId;
        profile.completedPurchases = completedPurchases;
        profile.onTimePaymentRate = onTimePaymentRate;
        profile.returnRate = returnRate;
        profile.criticalIncidents = criticalIncidents;
        profile.hasOverdueCredit = hasOverdueCredit;
        profile.availableCredit = availableCredit;
        profile.observedAt = observedAt;
        return profile;
    }

    UUID getCustomerId() { return customerId; }
    int getCompletedPurchases() { return completedPurchases; }
    BigDecimal getOnTimePaymentRate() { return onTimePaymentRate; }
    BigDecimal getReturnRate() { return returnRate; }
    int getCriticalIncidents() { return criticalIncidents; }
    boolean hasOverdueCredit() { return hasOverdueCredit; }
    BigDecimal getAvailableCredit() { return availableCredit; }
    Instant getObservedAt() { return observedAt; }

    /** Devuelve al saldo el monto de una obligación pagada o anulada. */
    void restoreAvailableCredit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("El monto a restituir debe ser mayor que cero.");
        }
        availableCredit = availableCredit.add(amount);
    }

    void updateOverdueFlag(boolean overdue) {
        hasOverdueCredit = overdue;
    }

    void consumeAvailableCredit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("El monto de crédito debe ser mayor que cero.");
        }
        if (availableCredit.compareTo(amount) < 0) {
            throw new IllegalStateException("El crédito disponible cambió y ya no cubre la compra.");
        }
        availableCredit = availableCredit.subtract(amount);
    }
}
