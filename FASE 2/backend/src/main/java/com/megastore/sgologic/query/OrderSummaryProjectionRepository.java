package com.megastore.sgologic.query;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface OrderSummaryProjectionRepository extends JpaRepository<OrderSummaryProjection, UUID> {
}
