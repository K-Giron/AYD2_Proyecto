package com.megastore.sgologic.incidents;

import com.megastore.sgologic.audit.api.AuditEntryRequest;
import com.megastore.sgologic.audit.api.AuditRecorder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** INC-01 / CDU-10: registro, resolución y evidencia auditable (AUD-01). */
class IncidentServiceTest {

    private final IncidentRepository repository = mock(IncidentRepository.class);
    private final AuditRecorder auditRecorder = mock(AuditRecorder.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-18T15:00:00Z"), ZoneOffset.UTC);
    private final IncidentService service = new IncidentService(repository, auditRecorder, clock);

    @Test
    void registerKeepsRelationToOriginalOrderForAPartialReturn() {
        UUID orderId = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Incident incident = service.register(orderId, IncidentType.RETURN, "Talla incorrecta", true,
                List.of(new IncidentLineItem("SKU-1", 1)), UUID.randomUUID());

        assertThat(incident.getOrderId()).isEqualTo(orderId);
        assertThat(incident.isPartial()).isTrue();
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.REGISTERED);

        ArgumentCaptor<AuditEntryRequest> audit = ArgumentCaptor.forClass(AuditEntryRequest.class);
        verify(auditRecorder).record(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("INCIDENT_REGISTERED");
        assertThat(audit.getValue().aggregateId()).isEqualTo(incident.getId());
        assertThat(audit.getValue().orderId()).isEqualTo(orderId);
        assertThat(audit.getValue().result()).isEqualTo("REGISTERED");
    }

    @Test
    void resolveCalculatesRefundAndCannotBeResolvedTwice() {
        Incident incident = Incident.register(UUID.randomUUID(), IncidentType.RETURN, "Producto dañado", false,
                List.of(new IncidentLineItem("SKU-2", 2)), UUID.randomUUID(), clock.instant());
        when(repository.findById(incident.getId())).thenReturn(Optional.of(incident));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Incident resolved = service.resolve(incident.getId(), IncidentStatus.REFUNDED, new BigDecimal("199.99"));

        assertThat(resolved.getStatus()).isEqualTo(IncidentStatus.REFUNDED);
        assertThat(resolved.getRefundAmount()).isEqualByComparingTo("199.99");
        assertThatThrownBy(() -> service.resolve(incident.getId(), IncidentStatus.REJECTED, null))
                .isInstanceOf(IncidentAlreadyResolvedException.class);
    }
}
