package com.megastore.sgologic.incidents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface IncidentRepository extends JpaRepository<Incident, UUID> {

    List<Incident> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
