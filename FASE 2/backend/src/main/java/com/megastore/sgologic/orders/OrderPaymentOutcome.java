package com.megastore.sgologic.orders;

import com.megastore.sgologic.payments.api.PaymentResult;

public record OrderPaymentOutcome(CustomerOrder order, PaymentResult payment) {
}
