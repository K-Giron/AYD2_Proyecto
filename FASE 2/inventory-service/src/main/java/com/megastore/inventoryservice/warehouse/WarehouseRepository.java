package com.megastore.inventoryservice.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

    List<Warehouse> findAllByOrderByCodeAsc();

    Optional<Warehouse> findByCode(String code);
}
