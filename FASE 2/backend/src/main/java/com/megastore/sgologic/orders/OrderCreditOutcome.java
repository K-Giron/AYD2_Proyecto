package com.megastore.sgologic.orders;

import com.megastore.sgologic.credit.api.CreditOperationResult;

public record OrderCreditOutcome(CustomerOrder order, CreditOperationResult creditOperation) {
}
