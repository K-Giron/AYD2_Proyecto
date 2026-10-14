package com.megastore.sgologic.logistics;

import java.time.Instant;
import java.util.List;

/**
 * Rastreo de LogiExpress (CDU-08, paso 4): las guías del comercio que
 * cambiaron desde un corte, con el estado que reporta el transportista
 * (CREADA, EN_TRANSITO, EN_PUNTO_DE_RETIRO, ENTREGADA).
 */
interface LogiExpressTrackingPort {

    List<TrackedGuide> guidesUpdatedSince(Instant since) throws LogiExpressUnavailableException;

    record TrackedGuide(String trackingNumber, String merchantReference, String status, Instant updatedAt) {
    }
}
