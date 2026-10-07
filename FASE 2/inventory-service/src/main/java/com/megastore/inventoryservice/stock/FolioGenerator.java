package com.megastore.inventoryservice.stock;

import org.springframework.stereotype.Component;

@Component
class FolioGenerator {

    private final FolioSequenceRepository sequences;

    FolioGenerator(FolioSequenceRepository sequences) {
        this.sequences = sequences;
    }

    String next(StockMovementType type) {
        return switch (type) {
            case ENTRADA -> "ENT-" + String.format("%06d", sequences.nextEntradaSequence());
            case SALIDA -> "SAL-" + String.format("%06d", sequences.nextSalidaSequence());
            case AJUSTE -> "AJU-" + String.format("%06d", sequences.nextAjusteSequence());
        };
    }
}
