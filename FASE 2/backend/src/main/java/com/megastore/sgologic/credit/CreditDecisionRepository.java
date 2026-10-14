package com.megastore.sgologic.credit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface CreditDecisionRepository extends JpaRepository<CreditDecision, UUID> {
}
