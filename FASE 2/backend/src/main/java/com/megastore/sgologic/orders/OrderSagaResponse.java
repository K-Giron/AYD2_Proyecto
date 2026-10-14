package com.megastore.sgologic.orders;

import java.time.LocalDate;
import java.util.UUID;

/** Estado observable de la Saga; {@code completed=false} permite reanudarla. */
public record OrderSagaResponse(
        UUID orderId,
        OrderStatus orderStatus,
        String sagaStatus,
        UUID paymentAttemptId,
        UUID creditOperationId,
        LocalDate creditDueDate,
        UUID fiscalDocumentId,
        String fiscalCui,
        String fiscalSeries,
        UUID reservationId,
        UUID shipmentId,
        String detail
) {
}
