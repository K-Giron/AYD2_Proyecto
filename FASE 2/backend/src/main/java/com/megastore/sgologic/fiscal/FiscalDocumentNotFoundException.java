package com.megastore.sgologic.fiscal;

import java.util.UUID;

class FiscalDocumentNotFoundException extends RuntimeException {
    FiscalDocumentNotFoundException(UUID id) {
        super("No existe el documento fiscal " + id + ".");
    }
}
