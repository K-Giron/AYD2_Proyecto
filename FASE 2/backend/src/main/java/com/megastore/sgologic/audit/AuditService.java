package com.megastore.sgologic.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuditService {

    private static final Set<String> AUTHORIZED_ROLES = Set.of("FINANCE", "SAT_AUDITOR");

    private final AuditEventRepository eventRepository;
    private final AuditEvidenceAccessRepository accessRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuditService(AuditEventRepository eventRepository,
                        AuditEvidenceAccessRepository accessRepository,
                        ObjectMapper objectMapper,
                        Clock clock) {
        this.eventRepository = eventRepository;
        this.accessRepository = accessRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public AuditEvent record(AuditCommand command) {
        String canonical = canonical(command);
        return eventRepository.save(AuditEvent.from(command, clock.instant(), sha256(canonical)));
    }

    @Transactional
    public List<AuditEventResponse> searchByOrder(UUID orderId, String requester, String role, boolean export) {
        authorize(requester, role, export ? "EXPORT" : "SEARCH", "orderId=" + orderId);
        List<AuditEvent> events = eventRepository.findByOrderIdOrderByOccurredAtAsc(orderId);
        recordAccess(requester, role, export ? "EXPORT" : "SEARCH", "orderId=" + orderId, events);
        return events.stream().map(AuditEventResponse::from).toList();
    }

    @Transactional
    public List<AuditEventResponse> searchByTransaction(UUID transactionId, String requester, String role) {
        authorize(requester, role, "SEARCH", "transactionId=" + transactionId);
        List<AuditEvent> events = eventRepository.findByTransactionIdOrderByOccurredAtAsc(transactionId);
        recordAccess(requester, role, "SEARCH", "transactionId=" + transactionId, events);
        return events.stream().map(AuditEventResponse::from).toList();
    }

    private void authorize(String requester, String role, String action, String criteria) {
        if (requester == null || requester.isBlank() || !AUTHORIZED_ROLES.contains(role)) {
            accessRepository.save(AuditEvidenceAccess.of(
                    requester == null || requester.isBlank() ? "anonymous" : requester,
                    role == null ? "UNKNOWN" : role, action, sha256(criteria), "REJECTED",
                    null, null, clock.instant()));
            throw new AuditAccessDeniedException();
        }
    }

    private void recordAccess(String requester, String role, String action,
                              String criteria, List<AuditEvent> events) {
        String result = events.isEmpty() ? "NOT_FOUND" : "SUCCESS";
        String combinedHash = events.isEmpty() ? null
                : sha256(events.stream().map(AuditEvent::getEvidenceHash).reduce("", String::concat));
        accessRepository.save(AuditEvidenceAccess.of(requester, role, action, sha256(criteria), result,
                events.isEmpty() ? null : criteria, combinedHash, clock.instant()));
    }

    private String canonical(AuditCommand command) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("No fue posible serializar la evidencia de auditoría", exception);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }
}
