package com.megastore.inventoryservice.logistics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findByFolio(String folio);

    List<Shipment> findAllByOrderByCreatedAtDesc();
}
