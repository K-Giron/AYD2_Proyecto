package com.megastore.sgologic.fiscal;

import java.util.Optional;
import java.util.UUID;

/** Contrato fiscal público para la Saga de Orders. */
public interface FiscalProcessingPort {
    FiscalProcessingResult requestInvoice(FiscalProcessingRequest request);
    Optional<FiscalProcessingResult> latestForOrder(UUID orderId);
}
