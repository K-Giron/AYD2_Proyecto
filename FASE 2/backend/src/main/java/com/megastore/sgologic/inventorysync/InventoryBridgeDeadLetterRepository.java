package com.megastore.sgologic.inventorysync;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryBridgeDeadLetterRepository extends JpaRepository<InventoryBridgeDeadLetter, UUID> {

    List<InventoryBridgeDeadLetter> findByOrderIdOrderByRegisteredAtDesc(UUID orderId);
}
