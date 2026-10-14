package com.megastore.sgologic.credit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "sgo.credit")
class CreditProperties {

    private int minimumCompletedPurchases = 3;
    private BigDecimal minimumOnTimePaymentRate = new BigDecimal("0.80");
    private BigDecimal maximumReturnRate = new BigDecimal("0.30");
    private int maximumCriticalIncidents = 1;
    private BigDecimal maximumLimit = new BigDecimal("5000.00");
    private int termDays = 30;
    private String ruleVersion = "credit-rules-v1";
    private int historyWindowMonths = 12;
    private BigDecimal limitRatio = new BigDecimal("0.40");

    int getMinimumCompletedPurchases() { return minimumCompletedPurchases; }
    void setMinimumCompletedPurchases(int value) { minimumCompletedPurchases = value; }
    BigDecimal getMinimumOnTimePaymentRate() { return minimumOnTimePaymentRate; }
    void setMinimumOnTimePaymentRate(BigDecimal value) { minimumOnTimePaymentRate = value; }
    BigDecimal getMaximumReturnRate() { return maximumReturnRate; }
    void setMaximumReturnRate(BigDecimal value) { maximumReturnRate = value; }
    int getMaximumCriticalIncidents() { return maximumCriticalIncidents; }
    void setMaximumCriticalIncidents(int value) { maximumCriticalIncidents = value; }
    BigDecimal getMaximumLimit() { return maximumLimit; }
    void setMaximumLimit(BigDecimal value) { maximumLimit = value; }
    int getTermDays() { return termDays; }
    void setTermDays(int value) { termDays = value; }
    int getHistoryWindowMonths() { return historyWindowMonths; }
    void setHistoryWindowMonths(int value) { historyWindowMonths = value; }
    BigDecimal getLimitRatio() { return limitRatio; }
    void setLimitRatio(BigDecimal value) { limitRatio = value; }
    String getRuleVersion() { return ruleVersion; }
    void setRuleVersion(String value) { ruleVersion = value; }
}
