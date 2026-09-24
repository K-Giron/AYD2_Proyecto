package com.megastore.sgologic.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AuditEvidenceAccessRepository extends JpaRepository<AuditEvidenceAccess, UUID> {
}
