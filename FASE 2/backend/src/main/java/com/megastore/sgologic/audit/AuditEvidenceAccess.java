package com.megastore.sgologic.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_evidence_access")
class AuditEvidenceAccess {
    @Id
    private UUID id;
    @Column(name = "requester_subject_id", nullable = false, updatable = false, length = 160)
    private String requesterSubjectId;
    @Column(name = "requester_role", nullable = false, updatable = false, length = 40)
    private String requesterRole;
    @Column(nullable = false, updatable = false, length = 24)
    private String action;
    @Column(name = "search_criteria_hash", nullable = false, updatable = false, length = 64)
    private String searchCriteriaHash;
    @Column(nullable = false, updatable = false, length = 24)
    private String result;
    @Column(name = "evidence_reference", updatable = false, length = 240)
    private String evidenceReference;
    @Column(name = "evidence_hash", updatable = false, length = 64)
    private String evidenceHash;
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;
    @Column(name = "completed_at", nullable = false, updatable = false)
    private Instant completedAt;

    protected AuditEvidenceAccess() {
    }

    static AuditEvidenceAccess of(String requester, String role, String action, String criteriaHash,
                                  String result, String reference, String evidenceHash, Instant now) {
        AuditEvidenceAccess access = new AuditEvidenceAccess();
        access.id = UUID.randomUUID();
        access.requesterSubjectId = requester;
        access.requesterRole = role;
        access.action = action;
        access.searchCriteriaHash = criteriaHash;
        access.result = result;
        access.evidenceReference = reference;
        access.evidenceHash = evidenceHash;
        access.requestedAt = now;
        access.completedAt = now;
        return access;
    }
}
