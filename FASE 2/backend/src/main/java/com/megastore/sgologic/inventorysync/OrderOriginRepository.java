package com.megastore.sgologic.inventorysync;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface OrderOriginRepository extends JpaRepository<OrderOrigin, UUID> {
}
