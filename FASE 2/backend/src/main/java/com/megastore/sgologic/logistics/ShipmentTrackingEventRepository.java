package com.megastore.sgologic.logistics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ShipmentTrackingEventRepository extends JpaRepository<ShipmentTrackingEvent, UUID> {

    List<ShipmentTrackingEvent> findByShipmentIdOrderByOccurredAtAsc(UUID shipmentId);
}
