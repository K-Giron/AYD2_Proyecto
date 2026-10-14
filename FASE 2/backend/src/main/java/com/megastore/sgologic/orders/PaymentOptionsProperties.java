package com.megastore.sgologic.orders;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sgo.orders.payment-options")
class PaymentOptionsProperties {

    private long creditTimeoutMs = 2000;
    private int callPoolSize = 4;

    long getCreditTimeoutMs() { return creditTimeoutMs; }
    void setCreditTimeoutMs(long creditTimeoutMs) { this.creditTimeoutMs = creditTimeoutMs; }
    int getCallPoolSize() { return callPoolSize; }
    void setCallPoolSize(int callPoolSize) { this.callPoolSize = callPoolSize; }
}
