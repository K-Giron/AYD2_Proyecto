package com.megastore.sgologic.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ErpBatchRepository extends JpaRepository<ErpBatch, UUID> {
}
