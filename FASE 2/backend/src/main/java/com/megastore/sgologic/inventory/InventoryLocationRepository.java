package com.megastore.sgologic.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface InventoryLocationRepository extends JpaRepository<InventoryLocation, UUID> {

    Optional<InventoryLocation> findByCode(String code);

    List<InventoryLocation> findAllByOrderByCodeAsc();
}
