package com.megastore.sgologic.orders;

import com.megastore.sgologic.fiscal.FiscalProcessingResult;

public record OrderFiscalOutcome(CustomerOrder order, FiscalProcessingResult fiscalDocument) {
}
