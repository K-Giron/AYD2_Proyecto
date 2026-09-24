package com.megastore.sgologic.orders;

import com.megastore.sgologic.payments.PaymentProcessingResult;

public record OrderPaymentOutcome(CustomerOrder order, PaymentProcessingResult payment) {
}
