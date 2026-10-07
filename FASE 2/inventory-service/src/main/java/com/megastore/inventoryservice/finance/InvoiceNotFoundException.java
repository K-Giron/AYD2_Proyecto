package com.megastore.inventoryservice.finance;

import java.util.UUID;

public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(UUID invoiceId) {
        super("No existe una cuenta con id " + invoiceId);
    }
}
