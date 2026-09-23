package com.megastore.sgologic.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    List<InventoryItem> findByProductIdOrderByLocationId(UUID productId);

    Optional<InventoryItem> findByProductIdAndLocationId(UUID productId, UUID locationId);
}
