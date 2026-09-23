package com.megastore.sgologic.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {

    Optional<InventoryReservation> findByIdempotencyKey(String idempotencyKey);

    List<InventoryReservation> findByOrderId(UUID orderId);

    /** Soporta el barrido de INV-03: reservas activas ya vencidas. */
    List<InventoryReservation> findByStatusAndExpiresAtBefore(InventoryReservationStatus status, Instant cutoff);
}
