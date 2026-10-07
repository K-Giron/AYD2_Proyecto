package com.megastore.inventoryservice.finance;

import java.util.UUID;

public class InvoiceAlreadyPaidException extends RuntimeException {

    public InvoiceAlreadyPaidException(UUID invoiceId) {
        super("La cuenta " + invoiceId + " ya está pagada.");
    }
}
