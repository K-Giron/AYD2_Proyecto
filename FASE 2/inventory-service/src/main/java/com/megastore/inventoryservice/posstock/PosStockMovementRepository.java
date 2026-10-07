package com.megastore.inventoryservice.posstock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PosStockMovementRepository extends JpaRepository<PosStockMovement, UUID> {

    List<PosStockMovement> findByPointOfSaleIdOrderByOccurredAtDesc(UUID pointOfSaleId);

    List<PosStockMovement> findAllByOrderByOccurredAtDesc();

    Optional<PosStockMovement> findByExternalReference(String externalReference);

    List<PosStockMovement> findByPointOfSaleIdAndTypeAndOccurredAtGreaterThanEqualOrderByOccurredAtAsc(
            UUID pointOfSaleId, PosStockMovementType type, Instant since);
}
