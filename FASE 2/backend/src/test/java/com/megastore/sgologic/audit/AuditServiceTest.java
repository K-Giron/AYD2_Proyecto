package com.megastore.sgologic.audit;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-23T22:30:00Z");

    @Test
    void storesAppendOnlyEvidenceWithSha256Hash() {
        AuditEventRepository events = mock(AuditEventRepository.class);
        AuditEvidenceAccessRepository accesses = mock(AuditEvidenceAccessRepository.class);
        AuditService service = service(events, accesses);
        AuditCommand command = command();
        when(events.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditEvent saved = service.record(command);

        assertThat(saved.getAction()).isEqualTo("ORDER_STATUS_CHANGED");
        assertThat(saved.getOccurredAt()).isEqualTo(NOW);
        assertThat(saved.getEvidenceHash()).matches("[0-9a-f]{64}");
        verify(events).save(saved);
    }

    @Test
    void authorizedSearchReturnsEvidenceAndRegistersItsAccess() {
        AuditEventRepository events = mock(AuditEventRepository.class);
        AuditEvidenceAccessRepository accesses = mock(AuditEvidenceAccessRepository.class);
        AuditService service = service(events, accesses);
        AuditEvent event = AuditEvent.from(command(), NOW, "a".repeat(64));
        UUID orderId = event.getOrderId();
        when(events.findByOrderIdOrderByOccurredAtAsc(orderId)).thenReturn(List.of(event));

        List<AuditEventResponse> result = service.searchByOrder(
                orderId, "finance-user", "FINANCE", false);

        assertThat(result).singleElement().extracting(AuditEventResponse::evidenceHash)
                .isEqualTo("a".repeat(64));
        verify(accesses).save(any(AuditEvidenceAccess.class));
    }

    @Test
    void unauthorizedSearchIsRejectedAndAlsoRegistered() {
        AuditEventRepository events = mock(AuditEventRepository.class);
        AuditEvidenceAccessRepository accesses = mock(AuditEvidenceAccessRepository.class);
        AuditService service = service(events, accesses);

        assertThatThrownBy(() -> service.searchByOrder(
                UUID.randomUUID(), "customer-user", "CUSTOMER", false))
                .isInstanceOf(AuditAccessDeniedException.class);
        verify(accesses).save(any(AuditEvidenceAccess.class));
    }

    private AuditService service(AuditEventRepository events, AuditEvidenceAccessRepository accesses) {
        return new AuditService(events, accesses, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AuditCommand command() {
        UUID orderId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        return new AuditCommand("customer:" + UUID.randomUUID(), "CUSTOMER",
                "ORDER_STATUS_CHANGED", "ORDER", orderId, orderId,
                correlationId, correlationId, Map.of("status", "CREATED"),
                Map.of("status", "CONFIRMED"), AuditResult.SUCCESS,
                Map.of("reason", "Evidencia fiscal aprobada"));
    }
}
