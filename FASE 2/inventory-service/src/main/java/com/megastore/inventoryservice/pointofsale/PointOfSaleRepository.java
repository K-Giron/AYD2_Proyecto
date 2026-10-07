package com.megastore.inventoryservice.pointofsale;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PointOfSaleRepository extends JpaRepository<PointOfSale, UUID> {

    boolean existsByCode(String code);

    Optional<PointOfSale> findByCode(String code);

    List<PointOfSale> findAllByOrderByNameAsc();
}
