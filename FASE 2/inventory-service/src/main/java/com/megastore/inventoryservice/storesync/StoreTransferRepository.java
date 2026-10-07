package com.megastore.inventoryservice.storesync;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface StoreTransferRepository extends JpaRepository<StoreTransfer, UUID> {

    Optional<StoreTransfer> findByCentralFolio(String centralFolio);

    Optional<StoreTransfer> findByOriginPosCodeAndLocalFolio(String originPosCode, String localFolio);

    List<StoreTransfer> findAllByOrderByOccurredAtDesc();
}
