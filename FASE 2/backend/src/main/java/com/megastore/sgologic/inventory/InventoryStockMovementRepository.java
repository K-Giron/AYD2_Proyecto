package com.megastore.sgologic.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface InventoryStockMovementRepository extends JpaRepository<InventoryStockMovement, UUID> {

    List<InventoryStockMovement> findByLocationIdOrderByOccurredAtDesc(UUID locationId);
}
