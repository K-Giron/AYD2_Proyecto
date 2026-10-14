package com.megastore.sgologic.credit;

import java.util.UUID;

class CreditOperationNotFoundException extends RuntimeException {

    CreditOperationNotFoundException(UUID operationId) {
        super("No existe la operación de crédito " + operationId + ".");
    }
}
