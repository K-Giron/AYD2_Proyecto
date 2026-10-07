package com.megastore.inventoryservice.stock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByArticleIdOrderByOccurredAtDesc(UUID articleId);

    List<StockMovement> findAllByOrderByOccurredAtDesc();

    Optional<StockMovement> findByExternalReference(String externalReference);
}
