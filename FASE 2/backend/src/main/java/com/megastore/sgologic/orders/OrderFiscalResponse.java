package com.megastore.sgologic.orders;

import com.megastore.sgologic.fiscal.FiscalStatus;

import java.util.UUID;

public record OrderFiscalResponse(
        OrderResponse order,
        UUID documentId,
        FiscalStatus fiscalStatus,
        String cui,
        String series,
        String satAuthorization,
        String failureCode
) {
    static OrderFiscalResponse from(OrderFiscalOutcome outcome) {
        var fiscal = outcome.fiscalDocument();
        return new OrderFiscalResponse(OrderResponse.from(outcome.order()), fiscal.documentId(), fiscal.status(),
                fiscal.cui(), fiscal.series(), fiscal.satAuthorization(), fiscal.failureCode());
    }
}
