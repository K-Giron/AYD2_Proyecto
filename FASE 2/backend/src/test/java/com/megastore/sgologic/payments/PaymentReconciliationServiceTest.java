package com.megastore.sgologic.payments;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PAY-03 / CDU-03 E1: la conciliación reutiliza la misma Idempotency-Key
 * (sin segundo cobro), es un no-op si el intento ya está resuelto y termina
 * en la bandeja de fallidos al agotar los reintentos.
 */
class PaymentReconciliationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-23T16:00:00Z");

    private final PaymentReconciliationRepository reconciliationRepository =
            mock(PaymentReconciliationRepository.class);
    private final PaymentAttemptRepository attemptRepository = mock(PaymentAttemptRepository.class);
    private final ResilientPayKlikClient payKlikClient = mock(ResilientPayKlikClient.class);
    private final PaymentAttemptWriter writer = mock(PaymentAttemptWriter.class);
    private final PaymentReconciliationProperties properties = new PaymentReconciliationProperties();
    private final AuditRecorder auditRecorder = mock(AuditRecorder.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final PaymentReconciliationService service = new PaymentReconciliationService(
            reconciliationRepository, attemptRepository, payKlikClient, writer, properties, auditRecorder, clock);

    @Test
    void reconcilesIndeterminateAttemptReusingSameIdempotencyKey() {
        PaymentAttempt attempt = indeterminateAttempt("idem-rec-1");
        PaymentReconciliation reconciliation = dueReconciliation(attempt.getId());
        when(reconciliationRepository.lockDueBatch(any(), anyInt())).thenReturn(List.of(reconciliation));
        when(attemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(payKlikClient.authorize(any())).thenReturn(new PayKlikGatewayPort.AuthorizationResult(
                UUID.randomUUID(), true, "PK-REC-1", null, NOW));

        service.reconcileDue();

        ArgumentCaptor<PayKlikGatewayPort.AuthorizationRequest> request =
                ArgumentCaptor.forClass(PayKlikGatewayPort.AuthorizationRequest.class);
        verify(payKlikClient).authorize(request.capture());
        assertThat(request.getValue().idempotencyKey()).isEqualTo(attempt.getIdempotencyKey());
        assertThat(request.getValue().paymentReference()).isEqualTo("tok_test_123");
        verify(writer).markApproved(eq(attempt.getId()), eq("PK-REC-1"), any());
        assertThat(reconciliation.getStatus()).isEqualTo(PaymentReconciliationStatus.RESOLVED);
        assertThat(reconciliation.getResult()).isEqualTo("APPROVED");
    }

    @Test
    void doesNotCallProviderWhenAttemptIsAlreadyResolved() {
        PaymentAttempt attempt = approvedAttempt("idem-rec-2");
        PaymentReconciliation reconciliation = dueReconciliation(attempt.getId());
        when(reconciliationRepository.lockDueBatch(any(), anyInt())).thenReturn(List.of(reconciliation));
        when(attemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));

        service.reconcileDue();

        verify(payKlikClient, never()).authorize(any());
        assertThat(reconciliation.getStatus()).isEqualTo(PaymentReconciliationStatus.RESOLVED);
        assertThat(reconciliation.getResult()).isEqualTo("APPROVED");
    }

    @Test
    void recoversStalePendingAttempts() {
        PaymentAttempt stale = PaymentAttempt.create(UUID.randomUUID(), UUID.randomUUID(),
                PaymentChannel.MOBILE, PaymentMethod.CARD, new BigDecimal("150.00"), "GTQ",
                UUID.randomUUID(), "idem-stale", "tok_test_123", NOW.minusSeconds(300));
        when(attemptRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(any(), any(), any()))
                .thenReturn(List.of(stale));

        service.reconcileDue();

        verify(writer).markIndeterminate(eq(stale.getId()), eq("STALE_PENDING"), anyString());
    }

    @Test
    void movesToDeadLetterAfterMaxAttempts() {
        properties.setMaxAttempts(1);
        PaymentAttempt attempt = indeterminateAttempt("idem-rec-3");
        PaymentReconciliation reconciliation = dueReconciliation(attempt.getId());
        when(reconciliationRepository.lockDueBatch(any(), anyInt())).thenReturn(List.of(reconciliation));
        when(attemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(payKlikClient.authorize(any()))
                .thenThrow(new PayKlikGatewayPort.PayKlikUnavailableException("PayKlik caído."));

        service.reconcileDue();

        assertThat(reconciliation.getStatus()).isEqualTo(PaymentReconciliationStatus.DEAD_LETTER);
        assertThat(reconciliation.getFailureCode()).isEqualTo("PAYKLIK_UNAVAILABLE");
        verify(writer, never()).markApproved(any(), anyString(), any());

        ArgumentCaptor<AuditEntryRequest> alert = ArgumentCaptor.forClass(AuditEntryRequest.class);
        verify(auditRecorder).record(alert.capture());
        assertThat(alert.getValue().action()).isEqualTo("PAYMENT_RECONCILIATION_ALERT");
        assertThat(alert.getValue().result()).isEqualTo("DEAD_LETTER");
        assertThat(alert.getValue().orderId()).isEqualTo(attempt.getOrderId());
    }

    private PaymentAttempt indeterminateAttempt(String idempotencyKey) {
        PaymentAttempt attempt = PaymentAttempt.create(UUID.randomUUID(), UUID.randomUUID(),
                PaymentChannel.MOBILE, PaymentMethod.CARD, new BigDecimal("150.00"), "GTQ",
                UUID.randomUUID(), idempotencyKey, "tok_test_123", NOW.minusSeconds(60));
        attempt.markIndeterminate("PAYKLIK_TIMEOUT", "sin respuesta", NOW.minusSeconds(60));
        return attempt;
    }

    private PaymentAttempt approvedAttempt(String idempotencyKey) {
        PaymentAttempt attempt = PaymentAttempt.create(UUID.randomUUID(), UUID.randomUUID(),
                PaymentChannel.MOBILE, PaymentMethod.CARD, new BigDecimal("150.00"), "GTQ",
                UUID.randomUUID(), idempotencyKey, "tok_test_123", NOW.minusSeconds(120));
        attempt.markApproved("PK-PREV", UUID.randomUUID(), NOW.minusSeconds(60));
        return attempt;
    }

    private PaymentReconciliation dueReconciliation(UUID attemptId) {
        return PaymentReconciliation.schedule(attemptId, NOW.minusSeconds(5), NOW.minusSeconds(60));
    }
}
