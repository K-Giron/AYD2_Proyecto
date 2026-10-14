package com.megastore.sgologic.orders;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentOptionResponse(
        PaymentRoute route,
        boolean enabled,
        BigDecimal approvedLimit,
        LocalDate dueDatePreview
) {
    static PaymentOptionResponse payKlik() {
        return new PaymentOptionResponse(PaymentRoute.PAYKLIK, true, null, null);
    }
}
