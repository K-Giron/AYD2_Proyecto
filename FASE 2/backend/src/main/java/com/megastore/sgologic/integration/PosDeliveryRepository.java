package com.megastore.sgologic.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PosDeliveryRepository extends JpaRepository<PosDelivery, UUID> {
    Optional<PosDelivery> findByEventIdAndStoreNumber(UUID eventId, int storeNumber);
    List<PosDelivery> findByEventIdOrderByStoreNumberAsc(UUID eventId);

    List<PosDelivery> findByOrderIdOrderByCreatedAtAscStoreNumberAsc(UUID orderId);

    List<PosDelivery> findTop200ByOrderByCreatedAtDesc();
}
