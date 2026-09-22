package com.megastore.sgologic.logistics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ShipmentDeadLetterRepository extends JpaRepository<ShipmentDeadLetter, UUID> {
}
