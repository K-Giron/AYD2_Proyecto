package com.megastore.sgologic.onlinestore;

import java.time.Instant;
import java.util.List;

/** Un folio de envío del central con sus líneas. {@code status}: NO_ENCONTRADO, EN_TRANSITO o RECIBIDO. */
public record ShipmentResponse(String folio, String status, String origin, Instant sentAt, String receivedBy,
                               Instant receivedAt, int units, List<OnlineReceipt> lines) {

    static ShipmentResponse of(String folio, List<OnlineReceipt> lines) {
        if (lines.isEmpty()) {
            return new ShipmentResponse(folio, "NO_ENCONTRADO", null, null, null, null, 0, lines);
        }
        boolean pending = lines.stream().anyMatch(line -> line.getStatus() == OnlineReceipt.Status.EN_TRANSITO);
        OnlineReceipt first = lines.getFirst();
        return new ShipmentResponse(first.getShipmentFolio(), pending ? "EN_TRANSITO" : "RECIBIDO", first.getOrigin(),
                first.getOccurredAt(), pending ? null : first.getReceivedBy(), pending ? null : first.getReceivedAt(),
                lines.stream().mapToInt(OnlineReceipt::getQuantity).sum(), lines);
    }
}
